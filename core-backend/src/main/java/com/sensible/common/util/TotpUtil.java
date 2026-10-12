package com.sensible.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

/**
 * RFC 6238 TOTP (HMAC-SHA1 / 30초 / 6자리) 구현.
 *
 * Google Authenticator·Authy 등 표준 인증 앱이 쓰는 기본 파라미터 그대로다.
 * 외부 라이브러리를 들이지 않고 JDK 표준(javax.crypto)과 이미 의존성에 있는
 * commons-codec(Base32)만 쓴다. 알고리즘 자체는 RFC 6238 부록 테스트 벡터로 검증한다.
 *
 * 시간은 항상 서버 시간을 기준으로 한다. 서버 시간이 틀어지면 정상 번호도 거부되므로
 * 운영 서버는 NTP 동기화가 전제다 (운영 문서 참조).
 */
public final class TotpUtil {

	/** 인증 코드 자리수 */
	public static final int DIGITS = 6;

	/** 시간 단계(초). 인증 앱 기본값과 같아야 한다 */
	public static final int STEP_SECONDS = 30;

	/** 기본 허용 오차: 앞뒤 1단계(±30초) */
	public static final int DEFAULT_WINDOW = 1;

	/** 비밀키 길이(바이트). 160비트 = SHA1 블록과 같은 권장 길이 */
	private static final int SECRET_BYTES = 20;

	private static final int[] DIGITS_POWER = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000 };

	private static final SecureRandom RANDOM = new SecureRandom();

	private TotpUtil() {
	}

	/** 새 비밀키를 Base32(패딩 없음)로 만든다. 인증 앱에 수동 입력할 수 있는 형태다 */
	public static String generateSecretBase32() {
		byte[] raw = new byte[SECRET_BYTES];
		RANDOM.nextBytes(raw);
		return encodeBase32(raw);
	}

	public static String encodeBase32(byte[] raw) {
		return new Base32().encodeToString(raw).replace("=", "");
	}

	/** Base32 문자열을 바이트로. 공백·소문자·패딩 유무는 관대하게 받는다 */
	public static byte[] decodeBase32(String secretBase32) {
		if (secretBase32 == null) {
			return new byte[0];
		}
		String normalized = secretBase32.replaceAll("[\\s-]", "").toUpperCase();
		return new Base32().decode(normalized);
	}

	/** 현재 시각(ms)의 시간 단계 번호 */
	public static long timeStep(long nowMillis) {
		return Math.floorDiv(nowMillis / 1000L, (long) STEP_SECONDS);
	}

	/**
	 * 특정 시간 단계의 코드를 만든다. RFC 4226 동적 절삭(dynamic truncation).
	 */
	public static String generateCode(byte[] key, long step) {
		try {
			byte[] msg = new byte[8];
			long value = step;
			for (int i = 7; i >= 0; i--) {
				msg[i] = (byte) (value & 0xff);
				value >>>= 8;
			}
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key, "RAW"));
			byte[] hash = mac.doFinal(msg);

			int offset = hash[hash.length - 1] & 0xf;
			int binary = ((hash[offset] & 0x7f) << 24)
					| ((hash[offset + 1] & 0xff) << 16)
					| ((hash[offset + 2] & 0xff) << 8)
					| (hash[offset + 3] & 0xff);
			int otp = binary % DIGITS_POWER[DIGITS];
			String result = Integer.toString(otp);
			StringBuilder sb = new StringBuilder();
			for (int i = result.length(); i < DIGITS; i++) {
				sb.append('0');
			}
			return sb.append(result).toString();
		} catch (Exception e) {
			throw new IllegalStateException("TOTP generation failed", e);
		}
	}

	/**
	 * 입력 코드를 검증하고, 일치한 시간 단계를 돌려준다. 불일치면 -1.
	 *
	 * 일치한 단계를 돌려주는 이유: 같은 단계의 코드를 두 번 받아주면 안 되기 때문이다
	 * (재사용 방지). 호출 측은 마지막으로 성공한 단계를 저장해 두고 그 이하 단계는 거부한다.
	 *
	 * @param window 앞뒤로 허용할 단계 수. 1이면 ±30초
	 */
	public static long matchStep(byte[] key, String code, long nowMillis, int window) {
		if (key == null || key.length == 0 || code == null) {
			return -1L;
		}
		String input = code.trim().replace(" ", "");
		if (input.length() != DIGITS) {
			return -1L;
		}
		for (int i = 0; i < input.length(); i++) {
			if (!Character.isDigit(input.charAt(i))) {
				return -1L;
			}
		}
		long current = timeStep(nowMillis);
		int w = Math.max(0, window);
		// 현재 단계를 먼저 보고, 그다음 ±1, ±2 … 순으로 본다
		for (int d = 0; d <= w; d++) {
			if (constantTimeEquals(generateCode(key, current + d), input)) {
				return current + d;
			}
			if (d > 0 && constantTimeEquals(generateCode(key, current - d), input)) {
				return current - d;
			}
		}
		return -1L;
	}

	/**
	 * 인증 앱 등록용 otpauth URI.
	 * 형식: otpauth://totp/{issuer}:{account}?secret=…&issuer=…&algorithm=SHA1&digits=6&period=30
	 * 발급자·계정에 들어가는 문자는 URL 인코딩한다 (공백·한글 안전).
	 */
	public static String buildOtpAuthUri(String issuer, String account, String secretBase32) {
		String safeIssuer = urlEncode(issuer == null ? "" : issuer);
		String safeAccount = urlEncode(account == null ? "" : account);
		return "otpauth://totp/" + safeIssuer + ":" + safeAccount
				+ "?secret=" + secretBase32
				+ "&issuer=" + safeIssuer
				+ "&algorithm=SHA1&digits=" + DIGITS + "&period=" + STEP_SECONDS;
	}

	private static String urlEncode(String s) {
		try {
			// 라벨 부분은 공백을 %20으로 쓰는 것이 인증 앱 호환성이 좋다
			return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}

	/** 길이·내용 비교를 시간 일정하게 수행 (타이밍 공격 방지) */
	public static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null) {
			return false;
		}
		byte[] x = a.getBytes();
		byte[] y = b.getBytes();
		int result = x.length ^ y.length;
		for (int i = 0; i < x.length && i < y.length; i++) {
			result |= x[i] ^ y[i];
		}
		return result == 0;
	}
}

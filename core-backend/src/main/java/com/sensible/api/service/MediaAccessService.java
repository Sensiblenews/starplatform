package com.sensible.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * 검수 대기 이미지에 대한 작성자 본인 접근 토큰 (2-26차).
 *
 * 작성자에게는 승인 전에도 자기 이미지를 보여준다. 그런데 파일이 공개 디렉터리 밖에 있어
 * 서버를 거쳐야 하고, &lt;img src&gt;는 브라우저가 직접 보내는 요청이라 인증 헤더를 붙일 수 없다.
 * 스타 계정은 세션 쿠키도 쓰지 않아(localStorage의 starToken 기반) 쿠키 방식도 안 된다.
 * starToken을 쿼리 문자열에 넣으면 접근 로그·리퍼러에 남으므로 그것도 안 된다.
 *
 * 그래서 목록 응답에 단기 서명 토큰을 실어 보내고, 이미지 요청은 서명만 검증한다.
 * 서명 형식은 LandingVisitService와 같다: base64url(payload) + "." + base64url(HMAC-SHA256)
 *
 * 중요: 서명이 유효해도 호출부가 DB 상태를 다시 확인한다.
 * 그래서 관리자가 거절한 순간부터 이미 발급된 토큰도 통하지 않는다 —
 * S3 Presigned URL과 달리 만료를 기다릴 필요가 없다.
 */
@Service("mediaAccessService")
public class MediaAccessService {

	/**
	 * 토큰 서명 시크릿. LandingVisitService와 같은 이유로 기동 시 생성한다.
	 * 재배포하면 리셋되지만 유효창이 10분이고 앱이 목록을 다시 받으면 새 토큰이 오므로 영향이 없다.
	 * 다중 인스턴스로 확장하면 globals.properties의 공유 시크릿으로 이관할 것.
	 */
	private static final byte[] SECRET = new byte[32];

	private static final SecureRandom RANDOM = new SecureRandom();

	static {
		RANDOM.nextBytes(SECRET);
	}

	/** 토큰 유효창(ms). 짧게 두고 목록을 다시 받을 때마다 갱신한다 */
	public static final long TOKEN_TTL_MS = 10L * 60 * 1000;

	/** 검증 결과. 유효하지 않으면 null을 돌려준다 */
	public static class Grant {
		public final String targetType;
		public final String targetId;

		Grant(String targetType, String targetId) {
			this.targetType = targetType;
			this.targetId = targetId;
		}
	}

	/**
	 * 작성자 본인에게 내려줄 이미지 접근 토큰을 발급한다.
	 * 호출부가 소유자임을 이미 확인한 뒤에만 부를 것.
	 *
	 * @param targetType MEMBER_CONTENT | STAR_FEED
	 * @param targetId   게시물 ID
	 */
	public String issueToken(String targetType, String targetId) {
		if (targetType == null || targetId == null) {
			return null;
		}

		// 구분자(|)가 값에 섞이면 페이로드가 어긋난다. 우리가 만드는 값에는 없지만 방어해 둔다
		if (targetType.indexOf('|') >= 0 || targetId.indexOf('|') >= 0) {
			return null;
		}

		String payload = targetType + "|" + targetId + "|" + System.currentTimeMillis();
		String payloadB64 = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		return payloadB64 + "." + sign(payloadB64);
	}

	/**
	 * 토큰을 검증한다. 서명이 맞고 만료되지 않았으면 대상 정보를 돌려준다.
	 * 상태(PENDING 여부)와 실제 소유 관계는 호출부가 DB에서 다시 확인해야 한다.
	 */
	public Grant verify(String token) {
		if (token == null) {
			return null;
		}

		int dot = token.indexOf('.');
		if (dot <= 0 || dot == token.length() - 1) {
			return null;
		}

		String payloadB64 = token.substring(0, dot);
		String sig = token.substring(dot + 1);

		// 상수 시간 비교 — 서명 앞부분부터 맞춰가는 공격을 막는다
		if (!MessageDigest.isEqual(sign(payloadB64).getBytes(StandardCharsets.UTF_8),
				sig.getBytes(StandardCharsets.UTF_8))) {
			return null;
		}

		String targetType;
		String targetId;
		long issuedAt;
		try {
			String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
			String[] parts = payload.split("\\|");
			if (parts.length != 3) {
				return null;
			}
			targetType = parts[0];
			targetId = parts[1];
			issuedAt = Long.parseLong(parts[2]);
		} catch (Exception e) {
			return null;
		}

		long elapsed = System.currentTimeMillis() - issuedAt;
		if (elapsed < 0 || elapsed > TOKEN_TTL_MS) {
			return null;
		}

		return new Grant(targetType, targetId);
	}

	private String sign(String payloadB64) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
			byte[] raw = mac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		} catch (Exception e) {
			// HmacSHA256은 JRE 필수 알고리즘이라 사실상 도달 불가
			throw new IllegalStateException("HMAC signing failed", e);
		}
	}
}

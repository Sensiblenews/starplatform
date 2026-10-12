package com.sensible.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 저장용 비밀값(OTP 비밀키 등) 암·복호화. AES-GCM.
 *
 * 키 재료는 운영자가 서버 설정(globals.properties 또는 환경변수)에 두는 임의 문자열이고,
 * 여기서 SHA-256으로 고정 길이 키를 뽑는다. 저장소(Git)에는 키가 없다.
 *
 * 출력 형식: "a2:" 또는 "a1:" 접두어 + Base64(IV 12바이트 ‖ 암호문+태그).
 *  - a2 = AES-256. JCE 정책이 256비트를 허용하는 JDK(8u161+ 기본)에서 쓴다.
 *  - a1 = AES-128. 구형 JDK 8의 제한 정책 환경 폴백. 복호화는 접두어로 키 길이를 고른다.
 * IV는 매번 새로 뽑으므로 같은 평문도 저장값이 다르다.
 */
public final class SecretCryptoUtil {

	private static final String PREFIX_256 = "a2:";
	private static final String PREFIX_128 = "a1:";
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;

	private static final SecureRandom RANDOM = new SecureRandom();

	private SecretCryptoUtil() {
	}

	/** 설정 문자열에서 32바이트 키를 유도한다 */
	public static byte[] deriveKey(String keyMaterial) {
		if (keyMaterial == null || keyMaterial.trim().isEmpty()) {
			throw new IllegalArgumentException("encryption key material is empty");
		}
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			return sha.digest(keyMaterial.trim().getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}

	/** 이 JVM이 AES-256을 허용하는지 */
	static boolean aes256Allowed() {
		try {
			return Cipher.getMaxAllowedKeyLength("AES") >= 256;
		} catch (Exception e) {
			return false;
		}
	}

	public static String encrypt(String plain, byte[] key32) {
		if (plain == null) {
			return null;
		}
		boolean strong = aes256Allowed();
		byte[] key = strong ? key32 : Arrays.copyOf(key32, 16);
		try {
			byte[] iv = new byte[IV_BYTES];
			RANDOM.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
			byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			byte[] out = new byte[iv.length + ct.length];
			System.arraycopy(iv, 0, out, 0, iv.length);
			System.arraycopy(ct, 0, out, iv.length, ct.length);
			return (strong ? PREFIX_256 : PREFIX_128) + Base64.getEncoder().encodeToString(out);
		} catch (Exception e) {
			throw new IllegalStateException("encrypt failed", e);
		}
	}

	/**
	 * 복호화. 키가 다르거나 저장값이 변조됐으면 예외(GCM 태그 불일치)를 던진다 —
	 * 조용히 빈 값을 돌려주면 호출 측이 "미등록"으로 오판할 수 있어서다.
	 */
	public static String decrypt(String stored, byte[] key32) {
		if (stored == null) {
			return null;
		}
		byte[] key;
		String body;
		if (stored.startsWith(PREFIX_256)) {
			key = key32;
			body = stored.substring(PREFIX_256.length());
		} else if (stored.startsWith(PREFIX_128)) {
			key = Arrays.copyOf(key32, 16);
			body = stored.substring(PREFIX_128.length());
		} else {
			throw new IllegalArgumentException("unknown ciphertext format");
		}
		try {
			byte[] all = Base64.getDecoder().decode(body);
			if (all.length <= IV_BYTES) {
				throw new IllegalArgumentException("ciphertext too short");
			}
			byte[] iv = Arrays.copyOfRange(all, 0, IV_BYTES);
			byte[] ct = Arrays.copyOfRange(all, IV_BYTES, all.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("decrypt failed (wrong key or tampered value)", e);
		}
	}
}

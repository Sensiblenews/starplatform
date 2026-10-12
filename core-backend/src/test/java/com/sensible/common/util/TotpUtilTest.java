package com.sensible.common.util;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * RFC 6238 부록 B 테스트 벡터(HMAC-SHA1, 비밀키 "12345678901234567890")로 알고리즘을 검증한다.
 * 부록은 8자리 값이므로 뒤 6자리가 우리 결과와 같아야 한다.
 */
public class TotpUtilTest {

	private static final byte[] RFC_KEY = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

	@Test
	public void rfc6238_테스트벡터와_일치한다() {
		assertEquals("287082", TotpUtil.generateCode(RFC_KEY, TotpUtil.timeStep(59L * 1000)));          // 94287082
		assertEquals("081804", TotpUtil.generateCode(RFC_KEY, TotpUtil.timeStep(1111111109L * 1000)));  // 07081804
		assertEquals("050471", TotpUtil.generateCode(RFC_KEY, TotpUtil.timeStep(1111111111L * 1000)));  // 14050471
		assertEquals("005924", TotpUtil.generateCode(RFC_KEY, TotpUtil.timeStep(1234567890L * 1000)));  // 89005924
		assertEquals("279037", TotpUtil.generateCode(RFC_KEY, TotpUtil.timeStep(2000000000L * 1000)));  // 69279037
	}

	@Test
	public void 현재_단계_코드는_일치하고_단계_번호를_돌려준다() {
		long now = 1234567890L * 1000;
		long step = TotpUtil.timeStep(now);
		assertEquals(step, TotpUtil.matchStep(RFC_KEY, "005924", now, 1));
	}

	@Test
	public void 허용_오차_1단계_앞뒤_코드는_받아준다() {
		long now = 1234567890L * 1000;
		long step = TotpUtil.timeStep(now);
		String prev = TotpUtil.generateCode(RFC_KEY, step - 1);
		String next = TotpUtil.generateCode(RFC_KEY, step + 1);
		assertEquals(step - 1, TotpUtil.matchStep(RFC_KEY, prev, now, 1));
		assertEquals(step + 1, TotpUtil.matchStep(RFC_KEY, next, now, 1));
	}

	@Test
	public void 허용_오차_밖의_코드는_거부한다() {
		long now = 1234567890L * 1000;
		long step = TotpUtil.timeStep(now);
		String twoAgo = TotpUtil.generateCode(RFC_KEY, step - 2);
		assertEquals(-1L, TotpUtil.matchStep(RFC_KEY, twoAgo, now, 1));
		// 오차를 0으로 두면 바로 앞 단계도 거부
		String prev = TotpUtil.generateCode(RFC_KEY, step - 1);
		assertEquals(-1L, TotpUtil.matchStep(RFC_KEY, prev, now, 0));
	}

	@Test
	public void 형식이_틀린_입력은_거부한다() {
		long now = 1234567890L * 1000;
		assertEquals(-1L, TotpUtil.matchStep(RFC_KEY, null, now, 1));
		assertEquals(-1L, TotpUtil.matchStep(RFC_KEY, "", now, 1));
		assertEquals(-1L, TotpUtil.matchStep(RFC_KEY, "12345", now, 1));
		assertEquals(-1L, TotpUtil.matchStep(RFC_KEY, "12345a", now, 1));
		assertEquals(-1L, TotpUtil.matchStep(new byte[0], "005924", now, 1));
	}

	@Test
	public void 공백이_섞인_입력은_정리해서_받아준다() {
		long now = 1234567890L * 1000;
		assertTrue(TotpUtil.matchStep(RFC_KEY, " 005 924 ", now, 1) >= 0);
	}

	@Test
	public void base32_비밀키는_왕복하고_앱_입력형식에_맞는다() {
		String secret = TotpUtil.generateSecretBase32();
		assertEquals(32, secret.length()); // 20바이트 → 32자, 패딩 없음
		assertFalse(secret.contains("="));
		assertTrue(secret.matches("[A-Z2-7]+"));
		assertArrayEquals(TotpUtil.decodeBase32(secret), TotpUtil.decodeBase32(secret.toLowerCase()));
		assertArrayEquals(TotpUtil.decodeBase32(secret), TotpUtil.decodeBase32(secret.substring(0, 8) + " " + secret.substring(8)));
	}

	@Test
	public void 비밀키는_매번_다르다() {
		assertFalse(TotpUtil.generateSecretBase32().equals(TotpUtil.generateSecretBase32()));
	}

	@Test
	public void otpauth_uri는_발급자와_계정을_인코딩한다() {
		String uri = TotpUtil.buildOtpAuthUri("Star Platform", "admin@kr", "ABCDEFGH");
		assertEquals("otpauth://totp/Star%20Platform:admin%40kr?secret=ABCDEFGH&issuer=Star%20Platform&algorithm=SHA1&digits=6&period=30", uri);
	}

	@Test
	public void 상수시간_비교() {
		assertTrue(TotpUtil.constantTimeEquals("123456", "123456"));
		assertFalse(TotpUtil.constantTimeEquals("123456", "123457"));
		assertFalse(TotpUtil.constantTimeEquals("12345", "123456"));
		assertFalse(TotpUtil.constantTimeEquals(null, "123456"));
	}
}

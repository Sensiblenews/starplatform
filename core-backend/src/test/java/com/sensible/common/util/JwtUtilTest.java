package com.sensible.common.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JwtUtilTest {

	// base64 64바이트 (openssl rand -base64 64 형식)
	private static final String KEY_A = "q3Zr0b3o0m4m2VJm5m3WqY9e2l9u1X2bq0hJm8Wf2p0jzYlT4a8e0cVn6xq1y5kq2Qd7o3K8s9S0x2Ym6Wq3Lg==";
	private static final String KEY_B = "Zm9vYmFyYmF6cXV4cXV1eGZvb2JhcmJhenF1eHF1dXhmb29iYXJiYXpxdXhxdXV4Zm9vYmFyYmF6cXV4cXV1eA==";

	@Before
	public void setUp() {
		System.clearProperty(JwtUtil.SYSPROP_KEY);
		JwtUtil.resetForTest();
	}

	@After
	public void tearDown() {
		System.clearProperty(JwtUtil.SYSPROP_KEY);
		JwtUtil.resetForTest();
	}

	@Test
	public void 설정된_키로_발급한_토큰은_검증되고_ID와_권한이_나온다() {
		System.setProperty(JwtUtil.SYSPROP_KEY, KEY_A);
		String token = JwtUtil.generateToken("admin1", "SM");
		assertTrue(JwtUtil.validateToken(token));
		assertEquals("admin1", JwtUtil.getUserIdFromToken(token));
		assertEquals("SM", JwtUtil.getAuthFromToken(token));
		assertTrue(JwtUtil.getIssuedAtMillis(token) > 0);
	}

	@Test
	public void 키를_바꾸면_이전_토큰은_전부_무효() {
		System.setProperty(JwtUtil.SYSPROP_KEY, KEY_A);
		String token = JwtUtil.generateToken("admin1", "SM");

		System.setProperty(JwtUtil.SYSPROP_KEY, KEY_B);
		JwtUtil.resetForTest();
		assertFalse(JwtUtil.validateToken(token));
		assertNull(JwtUtil.getUserIdFromToken(token));
		assertEquals(-1L, JwtUtil.getIssuedAtMillis(token));
	}

	@Test
	public void 키가_없으면_발급은_예외_검증은_false() {
		// 테스트 클래스패스에는 globals.properties 가 없을 수도, 있을 수도 있다.
		// 시스템 속성을 공백으로 두면 trimToNull 로 무시되고, 그래도 globals 에 키가 있으면 이 케이스는 건너뛴다
		System.setProperty(JwtUtil.SYSPROP_KEY, "   ");
		if (JwtUtil.secretKey() != null) {
			return;
		}
		try {
			JwtUtil.generateToken("admin1", "SM");
			fail("키 없이 발급되면 안 된다");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains(JwtUtil.CONFIG_KEY));
		}
		assertFalse(JwtUtil.validateToken("anything"));
		assertNull(JwtUtil.getAuthFromToken("anything"));
	}

	@Test
	public void 위조_토큰은_거부() {
		System.setProperty(JwtUtil.SYSPROP_KEY, KEY_A);
		String token = JwtUtil.generateToken("admin1", "SM");
		String forged = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";
		assertFalse(JwtUtil.validateToken(forged));
		assertNull(JwtUtil.getUserIdFromToken(forged));
	}
}

package com.sensible.common.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class SecretCryptoUtilTest {

	private final byte[] key = SecretCryptoUtil.deriveKey("local-test-key-material");

	@Test
	public void 암호화_복호화_왕복() {
		String enc = SecretCryptoUtil.encrypt("JBSWY3DPEHPK3PXP", key);
		assertFalse("JBSWY3DPEHPK3PXP".equals(enc));
		assertTrue(enc.startsWith("a2:") || enc.startsWith("a1:"));
		assertEquals("JBSWY3DPEHPK3PXP", SecretCryptoUtil.decrypt(enc, key));
	}

	@Test
	public void 같은_평문도_저장값은_매번_다르다() {
		assertFalse(SecretCryptoUtil.encrypt("same", key).equals(SecretCryptoUtil.encrypt("same", key)));
	}

	@Test(expected = IllegalStateException.class)
	public void 다른_키로는_복호화되지_않는다() {
		String enc = SecretCryptoUtil.encrypt("secret", key);
		SecretCryptoUtil.decrypt(enc, SecretCryptoUtil.deriveKey("another-key"));
	}

	@Test(expected = IllegalStateException.class)
	public void 변조된_값은_거부한다() {
		String enc = SecretCryptoUtil.encrypt("secret-value-long-enough", key);
		// 접두어 뒤 본문 한가운데 글자를 바꾼다 (끝 글자를 바꾸면 Base64 자체가 깨져 다른 예외가 난다)
		int pos = enc.length() / 2;
		char c = enc.charAt(pos);
		String tampered = enc.substring(0, pos) + (c == 'A' ? 'B' : 'A') + enc.substring(pos + 1);
		SecretCryptoUtil.decrypt(tampered, key);
	}

	@Test(expected = IllegalArgumentException.class)
	public void 접두어가_없는_값은_거부한다() {
		SecretCryptoUtil.decrypt("plain-text", key);
	}

	@Test(expected = IllegalArgumentException.class)
	public void 빈_키_재료는_거부한다() {
		SecretCryptoUtil.deriveKey("  ");
	}

	@Test
	public void 키_유도는_결정적이다() {
		assertArrayEquals(SecretCryptoUtil.deriveKey("abc"), SecretCryptoUtil.deriveKey(" abc "));
		assertEquals(32, SecretCryptoUtil.deriveKey("abc").length);
	}
}

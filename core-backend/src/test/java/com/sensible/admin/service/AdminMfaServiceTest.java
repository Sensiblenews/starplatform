package com.sensible.admin.service;

import static org.junit.Assert.*;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/** DB 없이 검증 가능한 순수 규칙: 복구 코드 생성·정리·판별·해시 */
public class AdminMfaServiceTest {

	@Test
	public void 복구_코드는_5개_형식은_XXXXX_XXXXX() {
		List<String> codes = AdminMfaService.generateBackupCodes(new SecureRandom(), AdminMfaService.BACKUP_CODE_COUNT);
		assertEquals(5, codes.size());
		Set<String> unique = new HashSet<String>(codes);
		assertEquals(5, unique.size());
		for (String c : codes) {
			assertTrue(c, c.matches("[A-HJ-NP-Z2-9]{5}-[A-HJ-NP-Z2-9]{5}"));
		}
	}

	@Test
	public void 헷갈리는_문자_I_O_0_1은_쓰지_않는다() {
		assertFalse(AdminMfaService.BACKUP_ALPHABET.contains("I"));
		assertFalse(AdminMfaService.BACKUP_ALPHABET.contains("O"));
		assertFalse(AdminMfaService.BACKUP_ALPHABET.contains("0"));
		assertFalse(AdminMfaService.BACKUP_ALPHABET.contains("1"));
		assertEquals(32, AdminMfaService.BACKUP_ALPHABET.length());
	}

	@Test
	public void 입력_정리는_소문자_하이픈_공백을_흡수한다() {
		assertEquals("ABCDEFGHJK", AdminMfaService.normalizeBackupCode(" abcde-fghjk "));
		assertEquals("", AdminMfaService.normalizeBackupCode(null));
	}

	@Test
	public void 복구_코드와_OTP를_구분한다() {
		assertTrue(AdminMfaService.looksLikeBackupCode(AdminMfaService.normalizeBackupCode("ABCDE-FGHJK")));
		assertFalse(AdminMfaService.looksLikeBackupCode(AdminMfaService.normalizeBackupCode("123456")));
		assertFalse(AdminMfaService.looksLikeBackupCode("1234567890")); // 숫자 10자리는 복구 코드가 아니다
		assertFalse(AdminMfaService.looksLikeBackupCode("ABCDEFGHJ")); // 9자
	}

	@Test
	public void 해시는_계정별로_다르고_정리된_코드_기준으로_같다() {
		String a = AdminMfaService.hashBackupCode("admin", "ABCDEFGHJK");
		String b = AdminMfaService.hashBackupCode("Admin", "ABCDEFGHJK"); // 대소문자 무시
		String c = AdminMfaService.hashBackupCode("other", "ABCDEFGHJK");
		assertEquals(64, a.length());
		assertEquals(a, b);
		assertFalse(a.equals(c));
		assertEquals(a, AdminMfaService.hashBackupCode("admin", AdminMfaService.normalizeBackupCode("abcde-fghjk")));
	}

	@Test
	public void DB_값_변환_헬퍼() {
		assertTrue(AdminMfaService.truthy(1));
		assertTrue(AdminMfaService.truthy("1"));
		assertTrue(AdminMfaService.truthy(Boolean.TRUE));
		assertFalse(AdminMfaService.truthy(0));
		assertFalse(AdminMfaService.truthy(null));
		assertEquals(5, AdminMfaService.toInt("5", 0));
		assertEquals(3, AdminMfaService.toInt(null, 3));
		assertEquals(9L, AdminMfaService.toLong(Long.valueOf(9), -1));
		assertEquals(-1L, AdminMfaService.toLong("x", -1));
	}

	@Test
	public void 기본_정책값() {
		assertEquals(5, AdminMfaService.MAX_FAILS);
		assertEquals(10, AdminMfaService.LOCK_MINUTES);
		assertEquals(5, AdminMfaService.BACKUP_CODE_COUNT);
	}
}

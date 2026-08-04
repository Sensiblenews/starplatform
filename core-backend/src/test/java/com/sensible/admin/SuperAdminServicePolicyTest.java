package com.sensible.admin;

import static org.junit.Assert.*;

import org.junit.Test;

import com.sensible.admin.service.SuperAdminService;

/**
 * 약관/개인정보처리방침 제목 구분 규칙 검증.
 * 웹 /terms·/privacy 매칭과 슈퍼 어드민 등록 검증이 모두 이 규칙을 쓴다.
 */
public class SuperAdminServicePolicyTest {

	@Test
	public void privacy_영문_제목을_개인정보처리방침으로_판정한다() {
		assertTrue(SuperAdminService.isPrivacyTitle("Privacy Policy"));
		assertTrue(SuperAdminService.isPrivacyTitle("PRIVACY POLICY"));
	}

	@Test
	public void 개인정보_한글_제목을_개인정보처리방침으로_판정한다() {
		assertTrue(SuperAdminService.isPrivacyTitle("개인정보 처리방침"));
		assertTrue(SuperAdminService.isPrivacyTitle("WITCH 개인정보처리방침"));
	}

	@Test
	public void 이용약관_제목은_개인정보처리방침이_아니다() {
		assertFalse(SuperAdminService.isPrivacyTitle("Terms of Service"));
		assertFalse(SuperAdminService.isPrivacyTitle("이용약관"));
	}

	@Test
	public void null과_빈_제목은_개인정보처리방침이_아니다() {
		assertFalse(SuperAdminService.isPrivacyTitle(null));
		assertFalse(SuperAdminService.isPrivacyTitle(""));
	}
}

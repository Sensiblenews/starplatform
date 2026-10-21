package com.sensible.common.util;

import static org.junit.Assert.*;

import org.junit.Test;

/** 로그인 뒤 돌아갈 주소 — 경보 화면만, 외부로 새는 형태는 거부 (2-35차) */
public class AdminSessionUtilReturnUrlTest {

	@Test
	public void 경보_화면은_허용() {
		assertTrue(AdminSessionUtil.isSafeReturnUrl("/super/alert/detail.do?id=42"));
		assertTrue(AdminSessionUtil.isSafeReturnUrl("/super/alert/center.do?alertId=42"));
	}

	@Test
	public void 다른_화면과_외부_주소는_거부() {
		assertFalse(AdminSessionUtil.isSafeReturnUrl(null));
		assertFalse(AdminSessionUtil.isSafeReturnUrl("/super/admin/list.do"));
		assertFalse(AdminSessionUtil.isSafeReturnUrl("https://evil.com/super/alert/detail.do"));
		assertFalse(AdminSessionUtil.isSafeReturnUrl("//evil.com/super/alert/detail.do"));
		assertFalse(AdminSessionUtil.isSafeReturnUrl("/super/alert/detail.do?id=1//evil.com"));
		assertFalse(AdminSessionUtil.isSafeReturnUrl("/super/alert/detail.do?x=javascript:alert(1)"));
	}
}

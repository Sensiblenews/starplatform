package com.sensible.admin;

import static org.junit.Assert.*;

import org.junit.Test;

import com.sensible.admin.service.SuperAdminService;

/**
 * 채팅 신고 조치 매핑 검증 (2-28차).
 * 어드민 버튼이 보내는 action과 WH_DM_REPORT.STATUS가 어긋나면
 * 처리한 신고가 목록에 계속 남거나, 정지 없이 닫힌 건이 정지된 것처럼 보인다.
 */
public class SuperAdminServiceDmReportTest {

	@Test
	public void 정지_조치는_처리완료_상태가_된다() {
		assertEquals("RESOLVED", SuperAdminService.reportActionToStatus("SUSPEND"));
	}

	@Test
	public void 조치완료와_기각은_그대로_매핑된다() {
		assertEquals("RESOLVED", SuperAdminService.reportActionToStatus("RESOLVED"));
		assertEquals("DISMISSED", SuperAdminService.reportActionToStatus("DISMISSED"));
	}

	@Test
	public void 모르는_조치는_null이다() {
		// 컨트롤러는 이 null을 보고 "알 수 없는 처리입니다"로 막는다
		assertNull(SuperAdminService.reportActionToStatus("DELETE"));
		assertNull(SuperAdminService.reportActionToStatus("suspend"));
		assertNull(SuperAdminService.reportActionToStatus(""));
		assertNull(SuperAdminService.reportActionToStatus(null));
	}

	@Test
	public void 계정을_내리는_조치는_정지뿐이다() {
		assertTrue(SuperAdminService.isSuspendAction("SUSPEND"));
		assertFalse(SuperAdminService.isSuspendAction("RESOLVED"));
		assertFalse(SuperAdminService.isSuspendAction("DISMISSED"));
		assertFalse(SuperAdminService.isSuspendAction(null));
	}

	@Test
	public void 허용_조치_목록은_매핑과_일치한다() {
		for (String action : SuperAdminService.DM_REPORT_ACTIONS) {
			assertNotNull(action, SuperAdminService.reportActionToStatus(action));
		}
	}
}

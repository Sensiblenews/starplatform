package com.sensible.admin.scheduler;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sensible.admin.scheduler.AlertRules.Severity;

public class AlertRulesTest {

	@Test
	public void 디스크_기본_임계값_75_85_95() {
		assertEquals(Severity.NORMAL, AlertRules.percentSeverity(74.9, 75, 85, 95));
		assertEquals(Severity.WARNING, AlertRules.percentSeverity(75, 75, 85, 95));
		assertEquals(Severity.DANGER, AlertRules.percentSeverity(85, 75, 85, 95));
		assertEquals(Severity.CRITICAL, AlertRules.percentSeverity(95, 75, 85, 95));
		assertEquals(Severity.CRITICAL, AlertRules.percentSeverity(100, 75, 85, 95));
	}

	@Test
	public void CPU는_긴급_단계가_없다() {
		assertEquals(Severity.DANGER, AlertRules.percentSeverity(99, 80, 90, 0));
	}

	@Test
	public void 측정_불가_음수는_정상으로_본다() {
		assertEquals(Severity.NORMAL, AlertRules.percentSeverity(-1, 75, 85, 95));
	}

	@Test
	public void http5xx는_건수와_비율을_둘_다_넘어야_위험() {
		// 요청 2건 중 1건 실패(50%)여도 건수가 적으면 정상
		assertEquals(Severity.NORMAL, AlertRules.http5xxSeverity(1, 2, 20, 5));
		// 건수는 넘지만 비율이 낮으면 정상
		assertEquals(Severity.NORMAL, AlertRules.http5xxSeverity(20, 10000, 20, 5));
		// 둘 다 넘으면 위험
		assertEquals(Severity.DANGER, AlertRules.http5xxSeverity(20, 300, 20, 5));
		assertEquals(Severity.NORMAL, AlertRules.http5xxSeverity(0, 0, 20, 5));
	}

	@Test
	public void 로그인_실패와_연결상태() {
		assertEquals(Severity.NORMAL, AlertRules.loginFailSeverity(19, 20));
		assertEquals(Severity.DANGER, AlertRules.loginFailSeverity(20, 20));
		assertEquals(Severity.NORMAL, AlertRules.loginFailSeverity(-1, 20));
		assertEquals(Severity.NORMAL, AlertRules.upDownSeverity(true, Severity.CRITICAL));
		assertEquals(Severity.CRITICAL, AlertRules.upDownSeverity(false, Severity.CRITICAL));
	}

	@Test
	public void 설정값은_기본값_위에_덮인다_그리고_깨진_값은_기본값() {
		Map<String, String> s = new HashMap<String, String>();
		assertEquals(75, AlertRules.intOf(s, AlertRules.S_DISK_WARN));
		s.put(AlertRules.S_DISK_WARN, "60");
		assertEquals(60, AlertRules.intOf(s, AlertRules.S_DISK_WARN));
		s.put(AlertRules.S_DISK_WARN, "abc");
		assertEquals(75, AlertRules.intOf(s, AlertRules.S_DISK_WARN));
		assertEquals("DANGER", AlertRules.strOf(s, AlertRules.S_MAIL_MIN_SEVERITY));
	}

	@Test
	public void 등급_비교와_파싱() {
		assertTrue(Severity.CRITICAL.atLeast(Severity.DANGER));
		assertTrue(Severity.DANGER.atLeast(Severity.DANGER));
		assertFalse(Severity.WARNING.atLeast(Severity.DANGER));
		assertEquals(Severity.DANGER, Severity.parse("danger", Severity.NORMAL));
		assertEquals(Severity.NORMAL, Severity.parse("nope", Severity.NORMAL));
		assertEquals("긴급", Severity.CRITICAL.label());
	}
}

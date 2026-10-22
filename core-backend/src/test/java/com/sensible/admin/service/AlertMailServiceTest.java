package com.sensible.admin.service;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class AlertMailServiceTest {

	@Test
	public void 수신자_목록은_쉼표_세미콜론_공백을_모두_구분자로_본다() {
		List<String> r = AlertMailService.parseRecipients("a@x.com, b@y.com;c@z.com  a@x.com");
		assertEquals(3, r.size());
		assertEquals("a@x.com", r.get(0));
		assertEquals("c@z.com", r.get(2));
	}

	@Test
	public void 골뱅이_없는_토큰은_버린다() {
		assertTrue(AlertMailService.parseRecipients("not-an-email, ").isEmpty());
		assertTrue(AlertMailService.parseRecipients(null).isEmpty());
	}

	@Test
	public void 경보_제목_형식() {
		assertEquals("[긴급] StarPlatform 7001 — 디스크 사용률 발생",
				AlertMailService.alertSubject("긴급", "7001", "디스크 사용률", "발생"));
	}

	@Test
	public void 경보_본문은_요건_항목을_모두_담는다() {
		String body = AlertMailService.alertBody("발생", "위험", "7001", "디스크 사용률", "87% — / 사용 87%",
				"2026-10-12 10:00:00", "2026-10-12 10:05:00", 42L, "정리하세요", "https://x/super/alert/center.do");
		assertTrue(body.contains("경보 등급 : 위험"));
		assertTrue(body.contains("플랫폼    : 7001"));
		assertTrue(body.contains("현재 수치 : 87%"));
		assertTrue(body.contains("발생 시각 : 2026-10-12 10:00:00"));
		assertTrue(body.contains("확인 시각 : 2026-10-12 10:05:00"));
		assertFalse(body.contains("알림 시각"));
		assertTrue(body.contains("발송 시각 : " + AlertMailService.SENT_AT_TOKEN));
		assertTrue(body.contains("경보 ID   : 42"));
		assertTrue(body.contains("정리하세요"));
		assertTrue(body.contains("https://x/super/alert/center.do"));
	}

	@Test
	public void DB_미기록_경보는_ID_자리에_표시가_들어간다() {
		String body = AlertMailService.alertBody("발생", "위험", "W", "x", "v", "a", "b", 0L, "r", null);
		assertTrue(body.contains("(DB 미기록)"));
		assertFalse(body.contains("어드민 경보 센터"));
	}

	@Test
	public void 복구_본문과_지속시간() {
		String body = AlertMailService.recoveryBody("WITCH", "DB 연결", "긴급", "10:00", "11:35", 95 * 60000L, "정상", 7L);
		assertTrue(body.contains("지속 시간   : 1시간 35분"));
		assertTrue(body.contains("경보 ID     : 7"));
		assertEquals("0분", AlertMailService.durationText(-5));
		assertEquals("12분", AlertMailService.durationText(12 * 60000L));
	}

	// ===== 2026-10-23: 발송 시각은 전송 직전에 채운다 =====

	@Test
	public void 발송_시각_토큰은_전송_시도_시각으로_바뀐다() {
		String body = AlertMailService.alertBody("발생", "위험", "W", "x", "v", "2026-10-23 09:00:00", "2026-10-23 09:05:00", 1L, "r", null);
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(2026, java.util.Calendar.OCTOBER, 23, 9, 5, 12);
		String stamped = AlertMailService.stampSentAt(body, c.getTime());
		assertTrue(stamped.contains("발송 시각 : 2026-10-23 09:05:12"));
		assertFalse(stamped.contains(AlertMailService.SENT_AT_TOKEN));
		// 발생·확인 시각은 그대로
		assertTrue(stamped.contains("발생 시각 : 2026-10-23 09:00:00"));
		assertTrue(stamped.contains("확인 시각 : 2026-10-23 09:05:00"));
	}

	@Test
	public void 복구_본문에도_발송_시각_토큰이_있고_토큰_없는_본문은_그대로다() {
		String body = AlertMailService.recoveryBody("W", "x", "긴급", "a", "b", 0L, "v", 7L);
		assertTrue(body.contains("발송 시각   : " + AlertMailService.SENT_AT_TOKEN));
		assertEquals("plain", AlertMailService.stampSentAt("plain", new java.util.Date()));
		assertNull(AlertMailService.stampSentAt(null, new java.util.Date()));
	}

	// ===== 2-35차: 메일 → 경보 상세 바로가기 =====

	@Test
	public void 경보_센터_주소는_상세_주소로_바뀐다() {
		assertEquals("https://x/super/alert/detail.do?id=42", AlertMailService.detailUrl("https://x/super/alert/center.do", 42L));
		assertEquals("https://x/ops?alertId=42", AlertMailService.detailUrl("https://x/ops", 42L));
		assertEquals("https://x/ops?a=1&alertId=42", AlertMailService.detailUrl("https://x/ops?a=1", 42L));
		// DB 미기록 경보는 목록 그대로
		assertEquals("https://x/super/alert/center.do", AlertMailService.detailUrl("https://x/super/alert/center.do", 0L));
	}

	@Test
	public void 복구_본문에_상세_링크가_들어간다() {
		String body = AlertMailService.recoveryBody("WITCH", "DB 연결", "긴급", "10:00", "11:35", 95 * 60000L, "정상", 7L,
				"https://x/super/alert/detail.do?id=7");
		assertTrue(body.contains("https://x/super/alert/detail.do?id=7"));
		assertFalse(AlertMailService.recoveryBody("W", "x", "긴급", "a", "b", 0L, "v", 7L).contains("▶"));
	}
}


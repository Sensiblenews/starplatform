package com.sensible.admin;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.sensible.admin.service.SuperAdminService;

/**
 * 로비 Latest Contents 편성 입력 검증 (2-33차).
 * 운영 설정 범위, 게시 기간, 표시 제목, 업로드 파일 사전 검증이 어드민 폼과 같은 기준으로 걸러져야 한다.
 */
public class SuperAdminServiceLatestContentTest {

	@Test
	public void 설정_빈값은_기본값_10개_4초() {
		Map<String, Object> r = SuperAdminService.normalizeLatestContentSetting(null, null);
		assertEquals(10, r.get("displayCount"));
		assertEquals(4, r.get("intervalSec"));
		r = SuperAdminService.normalizeLatestContentSetting("", "abc");
		assertEquals(10, r.get("displayCount"));
		assertEquals(4, r.get("intervalSec"));
	}

	@Test
	public void 설정_범위_밖은_경계값으로_자른다() {
		Map<String, Object> r = SuperAdminService.normalizeLatestContentSetting("0", "99");
		assertEquals(1, r.get("displayCount"));
		assertEquals(10, r.get("intervalSec"));
		r = SuperAdminService.normalizeLatestContentSetting("999", "1");
		assertEquals(30, r.get("displayCount"));
		assertEquals(3, r.get("intervalSec"));
	}

	@Test
	public void 설정_정상값은_그대로() {
		Map<String, Object> r = SuperAdminService.normalizeLatestContentSetting(" 15 ", "7");
		assertEquals(15, r.get("displayCount"));
		assertEquals(7, r.get("intervalSec"));
	}

	@Test
	public void 기간_빈값은_null_즉시_무기한() throws Exception {
		Map<String, Object> r = SuperAdminService.validateLatestContentPeriod("", null);
		assertNull(r.get("startAt"));
		assertNull(r.get("endAt"));
		r = SuperAdminService.validateLatestContentPeriod("null", "");
		assertNull(r.get("startAt"));
		assertNull(r.get("endAt"));
	}

	@Test
	public void 기간_datetime_local_형식을_받는다() throws Exception {
		Map<String, Object> r = SuperAdminService.validateLatestContentPeriod("2026-10-12T10:00", "2026-10-13 09:30:00");
		assertNotNull(r.get("startAt"));
		assertNotNull(r.get("endAt"));
		java.util.Date start = (java.util.Date) r.get("startAt");
		java.util.Date end = (java.util.Date) r.get("endAt");
		assertTrue(end.after(start));
	}

	@Test
	public void 기간_종료가_시작보다_앞이면_거부() throws Exception {
		try {
			SuperAdminService.validateLatestContentPeriod("2026-10-13T10:00", "2026-10-12T10:00");
			fail("종료 < 시작은 거부되어야 한다");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("종료"));
		}
		try {
			SuperAdminService.validateLatestContentPeriod("2026-10-13T10:00", "2026-10-13T10:00");
			fail("종료 == 시작도 거부되어야 한다");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("종료"));
		}
	}

	@Test
	public void 기간_형식이_틀리면_거부() {
		try {
			SuperAdminService.validateLatestContentPeriod("2026/10/12", null);
			fail("잘못된 형식은 거부되어야 한다");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("시작 일시"));
		}
	}

	@Test
	public void 표시제목_공백은_null_100자_초과는_거부() throws Exception {
		assertNull(SuperAdminService.normalizeTitleOverride(null));
		assertNull(SuperAdminService.normalizeTitleOverride("   "));
		assertEquals("Hello", SuperAdminService.normalizeTitleOverride("  Hello  "));
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 100; i++) sb.append('a');
		assertEquals(100, SuperAdminService.normalizeTitleOverride(sb.toString()).length());
		try {
			SuperAdminService.normalizeTitleOverride(sb.toString() + "a");
			fail("101자는 거부되어야 한다");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("100"));
		}
	}

	@Test
	public void yn은_Y만_Y() {
		assertEquals("Y", SuperAdminService.yn("Y"));
		assertEquals("Y", SuperAdminService.yn(" y "));
		assertEquals("N", SuperAdminService.yn("N"));
		assertEquals("N", SuperAdminService.yn("true"));
		assertEquals("N", SuperAdminService.yn(null));
	}

	@Test
	public void 업로드_검증_확장자_판정() throws Exception {
		assertEquals(".mov", SuperAdminService.validateLatestContentUpload("video/quicktime", "clip.MOV", 1024));
		assertEquals(".mp4", SuperAdminService.validateLatestContentUpload("video/mp4", "clip.mp4", 1024));
		assertEquals(".webm", SuperAdminService.validateLatestContentUpload("", "clip.webm", 1024));
	}

	@Test
	public void 업로드_검증_빈파일_큰파일_비영상은_거부() {
		try {
			SuperAdminService.validateLatestContentUpload("video/mp4", "a.mp4", 0);
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("선택"));
		}
		try {
			SuperAdminService.validateLatestContentUpload("video/mp4", "a.mp4", SuperAdminService.LC_UPLOAD_MAX_BYTES + 1);
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("MB"));
		}
		try {
			SuperAdminService.validateLatestContentUpload("image/png", "a.png", 1024);
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("영상"));
		}
	}
}

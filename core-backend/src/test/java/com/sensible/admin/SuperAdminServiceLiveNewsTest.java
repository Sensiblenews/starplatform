package com.sensible.admin;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.sensible.admin.service.SuperAdminService;

/**
 * 로비 LIVE NEWS 문구 입력 검증 (2-29차).
 * 어드민 폼과 서비스가 같은 기준(코드포인트 50자, 타겟 종류·형식)으로 걸러야 한다.
 */
public class SuperAdminServiceLiveNewsTest {

	@Test
	public void 정상_입력은_trim된_문구와_타겟으로_정규화된다() throws Exception {
		Map<String, Object> r = SuperAdminService.normalizeLiveNews("  GLOBAL CHALLENGE START!  ", "star", "abc123");
		assertEquals("GLOBAL CHALLENGE START!", r.get("message"));
		assertEquals("STAR", r.get("targetType"));
		assertEquals("abc123", r.get("targetValue"));
	}

	@Test
	public void 타겟_없음이면_값은_null이_된다() throws Exception {
		Map<String, Object> r = SuperAdminService.normalizeLiveNews("Hello", null, "ignored");
		assertEquals("NONE", r.get("targetType"));
		assertNull(r.get("targetValue"));
	}

	@Test
	public void 빈_타겟종류는_NONE으로_본다() throws Exception {
		assertEquals("NONE", SuperAdminService.normalizeLiveNews("Hello", "", "").get("targetType"));
		assertEquals("NONE", SuperAdminService.normalizeLiveNews("Hello", "null", "").get("targetType"));
	}

	@Test
	public void 딱_50자는_통과하고_51자는_거부된다() throws Exception {
		String fifty = "12345678901234567890123456789012345678901234567890";
		assertEquals(50, fifty.length());
		assertEquals(fifty, SuperAdminService.normalizeLiveNews(fifty, "NONE", null).get("message"));
		try {
			SuperAdminService.normalizeLiveNews(fifty + "1", "NONE", null);
			fail("51자는 거부되어야 한다");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("50"));
		}
	}

	@Test
	public void 이모지는_한_글자로_센다() throws Exception {
		// 🔥(U+1F525)는 UTF-16으로 2 char지만 코드포인트는 1개. 49자 + 이모지 = 50자여야 통과
		String body = "1234567890123456789012345678901234567890123456789"; // 49자
		String withEmoji = "🔥" + body;
		assertEquals(51, withEmoji.length());
		assertEquals(withEmoji, SuperAdminService.normalizeLiveNews(withEmoji, "NONE", null).get("message"));
	}

	@Test
	public void 빈_문구는_거부된다() {
		try {
			SuperAdminService.normalizeLiveNews("   ", "NONE", null);
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("문구"));
		}
	}

	@Test
	public void 허용되지_않은_타겟종류는_거부된다() {
		try {
			SuperAdminService.normalizeLiveNews("Hello", "RANKING", "x");
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("타겟"));
		}
	}

	@Test
	public void 타겟이_있는데_값이_비면_거부된다() {
		try {
			SuperAdminService.normalizeLiveNews("Hello", "STAR", "");
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("타겟 값"));
		}
	}

	@Test
	public void VS_타겟은_숫자_ID만_받는다() throws Exception {
		assertEquals("12", SuperAdminService.normalizeLiveNews("Hello", "VS", " 12 ").get("targetValue"));
		try {
			SuperAdminService.normalizeLiveNews("Hello", "VS", "abc");
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("VS"));
		}
	}

	@Test
	public void URL_타겟은_http_또는_https만_받는다() throws Exception {
		assertEquals("https://example.com/event",
				SuperAdminService.normalizeLiveNews("Hello", "URL", "https://example.com/event").get("targetValue"));
		try {
			SuperAdminService.normalizeLiveNews("Hello", "URL", "javascript:alert(1)");
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("http"));
		}
	}
}

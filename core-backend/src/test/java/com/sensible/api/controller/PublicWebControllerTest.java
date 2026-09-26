package com.sensible.api.controller;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

/**
 * 공개 포스트 목록의 페이지 파라미터·URL 조립 검증.
 * 컨트롤러 내부 헬퍼라 리플렉션으로 호출한다.
 */
public class PublicWebControllerTest {

	private final PublicWebController controller = new PublicWebController();

	private int parsePage(String raw) throws Exception {
		Method m = PublicWebController.class.getDeclaredMethod("parsePage", String.class);
		m.setAccessible(true);
		return (Integer) m.invoke(controller, raw);
	}

	private String pageUrl(String baseUrl, int page) throws Exception {
		Method m = PublicWebController.class.getDeclaredMethod("pageUrl", String.class, int.class);
		m.setAccessible(true);
		return (String) m.invoke(controller, baseUrl, page);
	}

	@Test
	public void 정상_페이지_번호는_그대로_쓴다() throws Exception {
		assertEquals(1, parsePage("1"));
		assertEquals(7, parsePage("7"));
		assertEquals(3, parsePage("  3  "));
	}

	@Test
	public void 잘못된_페이지_파라미터는_1페이지로_본다() throws Exception {
		// 사람이 주소를 고쳐 넣거나 봇이 아무 값이나 붙여도 500이 나면 안 된다
		assertEquals(1, parsePage(null));
		assertEquals(1, parsePage(""));
		assertEquals(1, parsePage("abc"));
		assertEquals(1, parsePage("0"));
		assertEquals(1, parsePage("-5"));
		assertEquals(1, parsePage("99999999999999999999"));
	}

	@Test
	public void 첫_페이지_URL에는_page_파라미터를_붙이지_않는다() throws Exception {
		// [2-29차 후속] 목록은 홈(/)이다. / 와 /?page=1 이 각각 색인되면 같은 내용이 중복 URL로 잡힌다
		assertEquals("https://witch-hunting.com/", pageUrl("https://witch-hunting.com", 1));
		assertEquals("https://witch-hunting.com/?page=2", pageUrl("https://witch-hunting.com", 2));
	}

	@Test
	public void 옛_목록_URL은_홈_목록으로_영구_이동한다() throws Exception {
		// /posts?page=N 은 이미 색인된 주소라 새 목록 주소로 301 시킨다. 잘못된 값은 1페이지(루트)로
		assertEquals("https://witch-hunting.com/?page=2",
				PublicWebController.legacyPostsRedirectUrl("https://witch-hunting.com", "2"));
		assertEquals("https://witch-hunting.com/",
				PublicWebController.legacyPostsRedirectUrl("https://witch-hunting.com", "1"));
		assertEquals("https://witch-hunting.com/",
				PublicWebController.legacyPostsRedirectUrl("https://witch-hunting.com", "abc"));
	}

	@Test
	public void 마지막_페이지_번호는_글_수로_구한다() throws Exception {
		assertEquals(1, PublicWebController.lastPage(0));
		assertEquals(1, PublicWebController.lastPage(PublicWebController.POSTS_PER_PAGE));
		assertEquals(2, PublicWebController.lastPage(PublicWebController.POSTS_PER_PAGE + 1));
	}

	@Test
	public void 검색어가_있으면_q를_붙인_목록_URL을_만든다() {
		assertEquals("https://witch-hunting.com/?q=flower", PublicWebController.pageUrl("https://witch-hunting.com", 1, "flower"));
		assertEquals("https://witch-hunting.com/?q=flower&page=2", PublicWebController.pageUrl("https://witch-hunting.com", 2, "flower"));
		assertEquals("https://witch-hunting.com/?q=%EA%BD%83+%EC%82%AC%EC%A7%84", PublicWebController.pageUrl("https://witch-hunting.com", 1, "꽃 사진"));
		assertEquals("https://witch-hunting.com/?page=2", PublicWebController.pageUrl("https://witch-hunting.com", 2, null));
	}

	@Test
	public void 검색어는_공백을_다듬고_빈_값은_null이다() {
		assertNull(PublicWebController.normalizeQuery(null));
		assertNull(PublicWebController.normalizeQuery("   "));
		assertEquals("flower", PublicWebController.normalizeQuery("  flower "));
		assertEquals(60, PublicWebController.normalizeQuery(new String(new char[80]).replace('\0', 'a')).length());
	}

	@Test
	public void 쿼리스트링의_한글_검색어를_UTF8로_직접_푼다() {
		assertEquals("꽃 사진", PublicWebController.queryParamUtf8("q=%EA%BD%83+%EC%82%AC%EC%A7%84&page=2", "q", "broken"));
		assertEquals("flower", PublicWebController.queryParamUtf8("page=2&q=flower", "q", null));
		// 없으면 컨테이너 값, 깨진 인코딩도 컨테이너 값
		assertEquals("fb", PublicWebController.queryParamUtf8("page=2", "q", "fb"));
		assertEquals("fb", PublicWebController.queryParamUtf8("q=%E", "q", "fb"));
		assertNull(PublicWebController.queryParamUtf8(null, "q", null));
	}

	@Test
	public void 카테고리_필터는_허용_코드만_통과한다() {
		assertEquals("STAR", PublicWebController.normalizeCategory("star"));
		assertEquals("UNIV", PublicWebController.normalizeCategory(" UNIV "));
		assertNull(PublicWebController.normalizeCategory("HACKER"));
		assertNull(PublicWebController.normalizeCategory(null));
	}

	@Test
	public void 카테고리와_검색어를_유지한_페이지_URL을_만든다() {
		assertEquals("https://witch-hunting.com/?category=STAR", PublicWebController.pageUrl("https://witch-hunting.com", 1, null, "STAR"));
		assertEquals("https://witch-hunting.com/?category=STAR&page=3", PublicWebController.pageUrl("https://witch-hunting.com", 3, null, "STAR"));
		assertEquals("https://witch-hunting.com/?q=flower&category=CITY&page=2", PublicWebController.pageUrl("https://witch-hunting.com", 2, "flower", "CITY"));
	}

	@Test
	public void 카테고리_타일은_시안의_5개_직군을_순서대로_만들고_사진이_없으면_빈_값이다() {
		java.util.Map<String, java.util.Map<String, Object>> covers = new java.util.HashMap<>();
		java.util.Map<String, Object> star = new java.util.HashMap<>();
		star.put("image", "/img/a.jpg");
		covers.put("STAR", star);
		java.util.List<java.util.Map<String, Object>> tiles = PublicWebController.buildCategoryTiles(covers, "https://witch-hunting.com");
		assertEquals(5, tiles.size());
		assertEquals("STAR", tiles.get(0).get("code"));
		assertEquals("Star", tiles.get(0).get("label"));
		assertEquals("https://witch-hunting.com/img/a.jpg", tiles.get(0).get("image"));
		assertEquals("CITY", tiles.get(4).get("code"));
		assertEquals("", tiles.get(4).get("image"));
	}
}

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
		// /posts 와 /posts?page=1 이 각각 색인되면 같은 내용이 중복 URL로 잡힌다
		assertEquals("https://witch-hunting.com/posts", pageUrl("https://witch-hunting.com", 1));
		assertEquals("https://witch-hunting.com/posts?page=2", pageUrl("https://witch-hunting.com", 2));
	}
}

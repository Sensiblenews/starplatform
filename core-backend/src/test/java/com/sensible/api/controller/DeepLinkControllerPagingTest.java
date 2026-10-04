package com.sensible.api.controller;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 2-31차 후속: 스타 페이지 Posts 탭 "Load more" 페이징의 순수 규칙.
 * 글이 20건을 넘는 스타(MSK)는 옛 영상 글이 21번째부터라 화면에서 사라졌다.
 */
public class DeepLinkControllerPagingTest {

	@Test
	public void 페이지_파라미터는_1_미만이나_숫자_아님이면_1이다() {
		assertEquals(1, DeepLinkController.parsePage(null));
		assertEquals(1, DeepLinkController.parsePage(""));
		assertEquals(1, DeepLinkController.parsePage("abc"));
		assertEquals(1, DeepLinkController.parsePage("0"));
		assertEquals(1, DeepLinkController.parsePage("-3"));
		assertEquals(2, DeepLinkController.parsePage(" 2 "));
	}

	@Test
	public void 한_페이지는_기존_20건_그대로다() {
		assertEquals(20, DeepLinkController.STAR_POSTS_PER_PAGE);
	}

	@Test
	public void 다음_페이지_주소는_더_있을_때만_만든다() {
		assertEquals("https://witch-hunting.com/star/SP-1?page=2",
				DeepLinkController.nextPostsUrl("https://witch-hunting.com", "SP-1", 1, true));
		assertEquals("https://witch-hunting.com/star/SP-1?page=3",
				DeepLinkController.nextPostsUrl("https://witch-hunting.com", "SP-1", 2, true));
		assertNull(DeepLinkController.nextPostsUrl("https://witch-hunting.com", "SP-1", 1, false));
	}
}

package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 스타 상세 API의 피드 페이징 파라미터 보정 규칙 검증 (2-26차).
 *
 * limit/offset은 앱이 JSON 본문으로 보내면 Integer로, 폼 파라미터로 오면 String으로 들어온다.
 * 두 경우 모두 같은 값이 나와야 하고, 값을 보내지 않는 기존 호출(웹 랜딩 등)도 그대로 동작해야 한다.
 */
public class SuperAppServiceFeedPagingTest {

	@Test
	public void 값을_보내지_않으면_기본값을_쓴다() {
		assertEquals(SuperAppService.DEFAULT_FEED_LIMIT, SuperAppService.normalizeFeedLimit(null));
		assertEquals(0, SuperAppService.normalizeFeedOffset(null));
	}

	@Test
	public void 숫자로_와도_문자로_와도_같은_값을_돌려준다() {
		assertEquals(10, SuperAppService.normalizeFeedLimit(Integer.valueOf(10)));
		assertEquals(10, SuperAppService.normalizeFeedLimit("10"));
		assertEquals(30, SuperAppService.normalizeFeedOffset(Integer.valueOf(30)));
		assertEquals(30, SuperAppService.normalizeFeedOffset("30"));
	}

	@Test
	public void 앞뒤_공백이_있는_문자도_읽는다() {
		assertEquals(10, SuperAppService.normalizeFeedLimit(" 10 "));
		assertEquals(20, SuperAppService.normalizeFeedOffset(" 20 "));
	}

	@Test
	public void 숫자가_아니면_기본값으로_되돌린다() {
		assertEquals(SuperAppService.DEFAULT_FEED_LIMIT, SuperAppService.normalizeFeedLimit("abc"));
		assertEquals(0, SuperAppService.normalizeFeedOffset("abc"));
	}

	@Test
	public void limit이_0_이하면_기본값을_쓴다() {
		// 0을 그대로 넘기면 빈 목록이 내려가 화면이 비어버린다
		assertEquals(SuperAppService.DEFAULT_FEED_LIMIT, SuperAppService.normalizeFeedLimit(Integer.valueOf(0)));
		assertEquals(SuperAppService.DEFAULT_FEED_LIMIT, SuperAppService.normalizeFeedLimit(Integer.valueOf(-5)));
	}

	@Test
	public void limit은_상한을_넘지_못한다() {
		// 상한이 없으면 예전처럼 스타의 전체 피드가 한 번에 내려간다
		assertEquals(SuperAppService.MAX_FEED_LIMIT, SuperAppService.normalizeFeedLimit(Integer.valueOf(100000)));
		assertEquals(SuperAppService.MAX_FEED_LIMIT,
				SuperAppService.normalizeFeedLimit(Integer.valueOf(SuperAppService.MAX_FEED_LIMIT + 1)));
	}

	@Test
	public void offset은_음수가_되지_않는다() {
		assertEquals(0, SuperAppService.normalizeFeedOffset(Integer.valueOf(-1)));
	}

	@Test
	public void 기본값은_상한_이하여야_한다() {
		assertTrue(SuperAppService.DEFAULT_FEED_LIMIT <= SuperAppService.MAX_FEED_LIMIT);
	}
}

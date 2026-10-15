package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 게시물 조회수·저장 입력 정규화 (2-34차 1-4).
 * 조회는 같은 기기·게시물을 30분 구간에 1번만 세고, 게시물 ID 는 양의 정수만 받는다.
 */
public class SuperAppServiceContentStatsTest {

	private static final long MIN = 60_000L;

	@Test
	public void 같은_30분_구간은_같은_번호() {
		long start = 1_800_000L * 1000; // 구간 경계 (epoch 초가 1800 의 배수)
		int bucket = SuperAppService.contentViewBucket(start);
		assertEquals(bucket, SuperAppService.contentViewBucket(start + 29 * MIN));
		assertEquals(bucket, SuperAppService.contentViewBucket(start + 30 * MIN - 1));
	}

	@Test
	public void 경계를_넘으면_다음_번호() {
		long start = 1_800_000L * 1000;
		int bucket = SuperAppService.contentViewBucket(start);
		assertEquals(bucket + 1, SuperAppService.contentViewBucket(start + 30 * MIN));
		assertEquals(bucket - 1, SuperAppService.contentViewBucket(start - 1));
	}

	@Test
	public void 현재_시각_구간은_INT_범위() {
		int bucket = SuperAppService.contentViewBucket(System.currentTimeMillis());
		assertTrue(bucket > 0);
	}

	@Test
	public void 게시물ID는_양의_정수만() {
		assertEquals(42, SuperAppService.normalizeContentId(42));
		assertEquals(42, SuperAppService.normalizeContentId(" 42 "));
		assertEquals(0, SuperAppService.normalizeContentId(0));
		assertEquals(0, SuperAppService.normalizeContentId(-3));
		assertEquals(0, SuperAppService.normalizeContentId("abc"));
		assertEquals(0, SuperAppService.normalizeContentId(null));
	}
}

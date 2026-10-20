package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 스타 게시물 수정·본문 상한 (2-35차).
 * 본문 상한은 20,000자이며 이모지(서로게이트 쌍)를 한 글자로 센다.
 */
public class SuperAppServiceFeedEditTest {

	private static String repeat(String unit, int count) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			sb.append(unit);
		}
		return sb.toString();
	}

	@Test
	public void 상한은_2만자() {
		assertEquals(20000, SuperAppService.FEED_BODY_MAX_CHARS);
	}

	@Test
	public void 상한까지는_허용하고_넘으면_거부() {
		assertFalse(SuperAppService.isFeedBodyTooLong(null));
		assertFalse(SuperAppService.isFeedBodyTooLong(repeat("가", 20000)));
		assertTrue(SuperAppService.isFeedBodyTooLong(repeat("가", 20001)));
	}

	@Test
	public void 이모지는_한_글자로_센다() {
		// 🌍 은 UTF-16 으로 2칸이지만 한 글자다. 20,000개면 String.length() 는 40,000
		String emojis = repeat("🌍", 20000);
		assertEquals(40000, emojis.length());
		assertFalse(SuperAppService.isFeedBodyTooLong(emojis));
		assertTrue(SuperAppService.isFeedBodyTooLong(emojis + "a"));
	}

	@Test
	public void 상한_안내_문구는_영어() {
		assertEquals("Posts can be up to 20,000 characters.", SuperAppService.feedBodyTooLongMessage());
	}

	@Test
	public void 거절_블라인드_글은_수정_불가() {
		assertTrue(SuperAppService.isEditableModerationStatus(null));
		assertTrue(SuperAppService.isEditableModerationStatus("APPROVED"));
		assertTrue(SuperAppService.isEditableModerationStatus("PENDING"));
		assertFalse(SuperAppService.isEditableModerationStatus("REJECTED"));
		assertFalse(SuperAppService.isEditableModerationStatus("HIDDEN"));
	}
}

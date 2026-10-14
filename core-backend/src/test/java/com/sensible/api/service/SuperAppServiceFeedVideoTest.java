package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 스타 피드 동영상 업로드 사전 검증 (2-34차 1-3, 상한 95MB).
 * 앱에 그대로 보이는 메시지라 영어 문장이어야 한다.
 */
public class SuperAppServiceFeedVideoTest {

	private static final long MB = 1024L * 1024;

	@Test
	public void 상한은_95MB() {
		assertEquals(95 * MB, SuperAppService.FEED_VIDEO_MAX_BYTES);
	}

	@Test
	public void 상한_이하_영상은_원본_확장자를_돌려준다() throws Exception {
		assertEquals(".mov", SuperAppService.validateFeedVideo("video/quicktime", "IMG_0001.MOV", 80 * MB));
		assertEquals(".mp4", SuperAppService.validateFeedVideo("video/mp4", "a.mp4", 95 * MB));
		// MIME 이 비어 오면(일부 Android 파일 선택기) 파일명으로 판정
		assertEquals(".m4v", SuperAppService.validateFeedVideo("", "clip.M4V", 10 * MB));
	}

	@Test
	public void 상한을_넘으면_거부() {
		try {
			SuperAppService.validateFeedVideo("video/mp4", "a.mp4", 95 * MB + 1);
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("95MB"));
		}
	}

	@Test
	public void 빈_파일과_영상_아닌_파일은_거부() {
		try {
			SuperAppService.validateFeedVideo("video/mp4", "a.mp4", 0);
			fail();
		} catch (Exception expected) { }
		try {
			SuperAppService.validateFeedVideo("image/png", "a.png", MB);
			fail();
		} catch (Exception expected) { }
	}
}

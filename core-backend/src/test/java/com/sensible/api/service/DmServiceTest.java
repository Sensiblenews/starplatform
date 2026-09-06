package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Test;

import com.sensible.common.util.ImageModerationUtil;

/**
 * 1:1 메신저 순수 로직 검증 (2-29차).
 * 폭파 시간 상수(레거시 동일), 텍스트·영상 검사, 파일명·토큰 규칙.
 */
public class DmServiceTest {

	@Test
	public void 폭파_시간은_레거시와_같다() {
		assertEquals(300, DmService.EXPIRE_AFTER_SEND_SEC);
		assertEquals(60, DmService.EXPIRE_AFTER_READ_SEC);
	}

	@Test
	public void 텍스트는_trim되고_빈_값은_거부된다() {
		assertEquals("hi", DmService.normalizeText("  hi  "));
		try {
			DmService.normalizeText("   ");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("message"));
		}
	}

	@Test
	public void 텍스트_상한을_넘으면_거부된다() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < DmService.TEXT_MAX_LENGTH; i++) sb.append('a');
		assertEquals(DmService.TEXT_MAX_LENGTH, DmService.normalizeText(sb.toString()).length());
		try {
			DmService.normalizeText(sb.append('b').toString());
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("too long"));
		}
	}

	@Test
	public void 영상은_30MB까지만_받는다() {
		assertEquals("mp4", DmService.validateVideo(DmService.VIDEO_MAX_BYTES, "video/mp4"));
		try {
			DmService.validateVideo(DmService.VIDEO_MAX_BYTES + 1, "video/mp4");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("30MB"));
		}
	}

	@Test
	public void 영상_MIME은_화이트리스트만_통과한다() {
		assertEquals("mov", DmService.videoExtensionOf("video/quicktime"));
		assertEquals("webm", DmService.videoExtensionOf("video/webm; codecs=vp9"));
		assertEquals("3gp", DmService.videoExtensionOf("VIDEO/3GPP"));
		assertNull(DmService.videoExtensionOf("video/x-msvideo"));
		assertNull(DmService.videoExtensionOf("image/png"));
		assertNull(DmService.videoExtensionOf(null));
		try {
			DmService.validateVideo(10, "video/x-msvideo");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("MP4"));
		}
	}

	@Test
	public void 저장_파일명은_UUID이고_썸네일은_thumb_접미사다() {
		String name = DmService.newFileName("mp4");
		assertTrue(name.matches("^[0-9a-f]{32}\\.mp4$"));
		assertEquals(name.substring(0, 32) + "_thumb.jpg", DmService.thumbNameOf(name));
		assertTrue(DmService.isSafeFileName(name));
	}

	@Test
	public void 경로_조작_파일명은_거부된다() {
		assertFalse(DmService.isSafeFileName("../etc/passwd"));
		assertFalse(DmService.isSafeFileName("a/b.jpg"));
		assertFalse(DmService.isSafeFileName(""));
		assertFalse(DmService.isSafeFileName(null));
	}

	@Test
	public void 파일_토큰_종류는_DM_전용만_인정한다() {
		assertTrue(DmService.isFileGrant(DmService.GRANT_FILE));
		assertTrue(DmService.isFileGrant(DmService.GRANT_THUMB));
		assertFalse(DmService.isFileGrant("STAR_FEED"));
		assertFalse(DmService.isFileGrant(null));
	}

	@Test
	public void 확장자별_Content_Type() {
		assertEquals("image/jpeg", DmService.contentTypeOf("a.jpg"));
		assertEquals("video/mp4", DmService.contentTypeOf("a.mp4"));
		assertEquals("video/quicktime", DmService.contentTypeOf("a.mov"));
		assertEquals("application/octet-stream", DmService.contentTypeOf("a.exe"));
	}

	@Test
	public void 이미지_거부_사유_문구() {
		assertTrue(DmService.imageRejectionMessage(ImageModerationUtil.Rejection.TOO_LARGE).contains("10MB"));
		assertEquals("Only JPG, PNG and WebP images are allowed.",
				DmService.imageRejectionMessage(ImageModerationUtil.Rejection.SIGNATURE));
		assertEquals("Upload failed.", DmService.imageRejectionMessage(null));
	}
}

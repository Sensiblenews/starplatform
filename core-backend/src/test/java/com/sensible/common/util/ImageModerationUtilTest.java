package com.sensible.common.util;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.sensible.common.util.ImageModerationUtil.Rejection;
import com.sensible.common.util.ImageModerationUtil.Result;

/**
 * 업로드 이미지 검증 규칙 검증 (2-26차).
 *
 * 이 검증은 "파일이 정상적인 이미지인가"만 본다. 내용의 유해성은 관리자 검수가 판단한다.
 * 확장자·MIME은 클라이언트가 마음대로 붙일 수 있으므로 매직 바이트가 실질 판정이다.
 */
public class ImageModerationUtilTest {

	/** 실제로 디코딩되는 최소 JPEG를 만든다 */
	private static byte[] realJpeg(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", out);
		return out.toByteArray();
	}

	private static byte[] realPng(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	// ===================== 매직 바이트 =====================

	@Test
	public void JPEG_시그니처를_알아본다() throws Exception {
		assertEquals("jpg", ImageModerationUtil.detectImageType(realJpeg(4, 4)));
	}

	@Test
	public void PNG_시그니처를_알아본다() throws Exception {
		assertEquals("png", ImageModerationUtil.detectImageType(realPng(4, 4)));
	}

	@Test
	public void WebP_시그니처를_알아본다() {
		// RIFF....WEBP — ImageIO가 못 읽어도 시그니처로는 판별한다
		byte[] webp = new byte[] { 'R', 'I', 'F', 'F', 0x20, 0, 0, 0, 'W', 'E', 'B', 'P' };
		assertEquals("webp", ImageModerationUtil.detectImageType(webp));
	}

	@Test
	public void 이미지가_아니면_형식을_돌려주지_않는다() {
		byte[] php = "<?php system($_GET['c']); ?>            ".getBytes();
		assertNull(ImageModerationUtil.detectImageType(php));
	}

	@Test
	public void 시그니처보다_짧은_파일은_거부한다() {
		assertNull(ImageModerationUtil.detectImageType(new byte[] { (byte) 0xFF, (byte) 0xD8 }));
	}

	// ===================== 검증 =====================

	@Test
	public void 정상_JPEG는_통과한다() throws Exception {
		Result r = ImageModerationUtil.validate(realJpeg(10, 10), null, "image/jpeg");
		assertTrue(r.isValid());
		assertEquals("jpg", r.extension);
	}

	@Test
	public void 위장_파일은_확장자가_이미지여도_거부한다() {
		// photo.jpg인데 내용은 PHP
		byte[] disguised = "<?php echo 1; ?>                    ".getBytes();
		Result r = ImageModerationUtil.validate(disguised, "photo.jpg", "image/jpeg");
		assertFalse(r.isValid());
		assertEquals(Rejection.SIGNATURE, r.rejection);
	}

	@Test
	public void 저장_확장자는_클라이언트_주장이_아니라_실제_형식을_따른다() throws Exception {
		// 이름은 png인데 내용은 JPEG면 jpg로 저장해야 한다
		Result r = ImageModerationUtil.validate(realJpeg(8, 8), "photo.png", null);
		assertTrue(r.isValid());
		assertEquals("jpg", r.extension);
	}

	@Test
	public void 허용하지_않는_확장자는_거부한다() throws Exception {
		byte[] jpeg = realJpeg(4, 4);
		assertEquals(Rejection.EXTENSION, ImageModerationUtil.validate(jpeg, "a.gif", null).rejection);
		assertEquals(Rejection.EXTENSION, ImageModerationUtil.validate(jpeg, "a.svg", null).rejection);
		assertEquals(Rejection.EXTENSION, ImageModerationUtil.validate(jpeg, "a.php", null).rejection);
		assertEquals(Rejection.EXTENSION, ImageModerationUtil.validate(jpeg, "noext", null).rejection);
	}

	@Test
	public void 파일명이_없으면_확장자를_따지지_않고_시그니처로_판정한다() throws Exception {
		// 게시물 업로드는 base64만 보내 파일명이 없다
		assertTrue(ImageModerationUtil.validate(realJpeg(4, 4), null, null).isValid());
	}

	@Test
	public void 허용하지_않는_MIME은_거부한다() throws Exception {
		Result r = ImageModerationUtil.validate(realJpeg(4, 4), null, "image/gif");
		assertEquals(Rejection.MIME, r.rejection);
	}

	@Test
	public void 빈_파일은_거부한다() {
		assertEquals(Rejection.EMPTY, ImageModerationUtil.validate(new byte[0], null, null).rejection);
		assertEquals(Rejection.EMPTY, ImageModerationUtil.validate(null, null, null).rejection);
	}

	@Test
	public void 용량_상한을_넘으면_거부한다() {
		byte[] tooBig = new byte[ImageModerationUtil.MAX_FILE_BYTES + 1];
		assertEquals(Rejection.TOO_LARGE, ImageModerationUtil.validate(tooBig, null, null).rejection);
	}

	@Test
	public void 헤더만_이미지고_내용이_깨졌으면_거부한다() {
		// JPEG 시그니처만 붙인 쓰레기 데이터
		byte[] broken = new byte[64];
		broken[0] = (byte) 0xFF;
		broken[1] = (byte) 0xD8;
		broken[2] = (byte) 0xFF;
		assertEquals(Rejection.UNREADABLE, ImageModerationUtil.validate(broken, null, null).rejection);
	}

	// ===================== data URI =====================

	@Test
	public void data_URI에서_MIME과_본문을_분리한다() {
		String uri = "data:image/png;base64,AAAA";
		assertEquals("image/png", ImageModerationUtil.mimeFromDataUri(uri));
		assertEquals("AAAA", ImageModerationUtil.base64Payload(uri));
	}

	@Test
	public void 접두어가_없으면_입력_전체를_본문으로_본다() {
		// 회원 콘텐츠는 접두어 없이 순수 base64만 보낸다
		assertNull(ImageModerationUtil.mimeFromDataUri("AAAA"));
		assertEquals("AAAA", ImageModerationUtil.base64Payload("AAAA"));
	}

	// ===================== URL → 파일명 =====================

	@Test
	public void URL에서_파일명을_뽑는다() {
		assertEquals("abc123.jpg",
				ImageModerationUtil.fileNameFromUrl("https://witch-hunting.com/img/abc123.jpg"));
	}

	@Test
	public void 쿼리스트링은_떼어낸다() {
		assertEquals("abc.jpg",
				ImageModerationUtil.fileNameFromUrl("https://witch-hunting.com/img/abc.jpg?v=2"));
	}

	@Test
	public void 경로_조작은_파일명만_남겨_무력화한다() {
		// 마지막 슬래시 뒤만 취하므로 상위 경로로 빠져나갈 수 없다.
		// 남는 값은 검수 보관소 안의 평범한 파일명이라 그 자체로는 위험하지 않다.
		assertEquals("passwd", ImageModerationUtil.fileNameFromUrl("https://x/img/../../etc/passwd"));
		assertEquals("passwd", ImageModerationUtil.fileNameFromUrl("../../etc/passwd"));
	}

	@Test
	public void 파일명을_뽑을_수_없으면_null이다() {
		assertNull(ImageModerationUtil.fileNameFromUrl("https://x/img/"));
		assertNull(ImageModerationUtil.fileNameFromUrl(".."));
		assertNull(ImageModerationUtil.fileNameFromUrl(""));
		assertNull(ImageModerationUtil.fileNameFromUrl(null));
	}

	@Test
	public void 허용하지_않는_문자가_섞인_파일명은_거부한다() {
		// 널 바이트·공백·경로 구분자가 파일 시스템으로 넘어가지 않게 한다
		assertNull(ImageModerationUtil.fileNameFromUrl("https://x/img/a\u0000b.jpg"));
		assertNull(ImageModerationUtil.fileNameFromUrl("https://x/img/a b.jpg"));
	}
}

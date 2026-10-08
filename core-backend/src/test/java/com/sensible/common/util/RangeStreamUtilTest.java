package com.sensible.common.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Range 헤더 해석 규칙 (2-32차 — 어드민 검수 화면의 동영상 플레이어 탐색용).
 * 브라우저가 보내는 세 형태(start-end / start- / -suffix)와 잘못된 범위를 가른다.
 */
public class RangeStreamUtilTest {

	@Test
	public void 범위_헤더가_없으면_전체_전송이다() {
		assertNull(RangeStreamUtil.parseRange(null, 1000));
		assertNull(RangeStreamUtil.parseRange("", 1000));
		assertNull(RangeStreamUtil.parseRange("items=0-10", 1000));
	}

	@Test
	public void 시작과_끝을_둘_다_준_범위() {
		assertArrayEquals(new long[] { 0, 99 }, RangeStreamUtil.parseRange("bytes=0-99", 1000));
		assertArrayEquals(new long[] { 500, 999 }, RangeStreamUtil.parseRange("bytes=500-999", 1000));
	}

	@Test
	public void 끝을_생략하면_파일_끝까지다() {
		assertArrayEquals(new long[] { 200, 999 }, RangeStreamUtil.parseRange("bytes=200-", 1000));
	}

	@Test
	public void 끝이_길이를_넘으면_파일_끝으로_자른다() {
		assertArrayEquals(new long[] { 900, 999 }, RangeStreamUtil.parseRange("bytes=900-5000", 1000));
	}

	@Test
	public void 접미_범위는_마지막_N바이트다() {
		assertArrayEquals(new long[] { 900, 999 }, RangeStreamUtil.parseRange("bytes=-100", 1000));
		// 파일보다 큰 접미는 전체
		assertArrayEquals(new long[] { 0, 999 }, RangeStreamUtil.parseRange("bytes=-5000", 1000));
	}

	@Test
	public void 만족할_수_없는_범위는_빈_배열이다() {
		// 호출부는 이 값을 416 으로 바꾼다
		assertEquals(0, RangeStreamUtil.parseRange("bytes=1000-", 1000).length);
		assertEquals(0, RangeStreamUtil.parseRange("bytes=50-10", 1000).length);
		assertEquals(0, RangeStreamUtil.parseRange("bytes=-0", 1000).length);
	}

	@Test
	public void 다중_범위나_숫자가_아닌_값은_전체_전송으로_떨어진다() {
		assertNull(RangeStreamUtil.parseRange("bytes=0-10,20-30", 1000));
		assertNull(RangeStreamUtil.parseRange("bytes=abc-def", 1000));
		assertNull(RangeStreamUtil.parseRange("bytes=", 1000));
	}

	@Test
	public void 확장자별_content_type() {
		assertEquals("video/mp4", RangeStreamUtil.contentTypeFor("a.mp4"));
		assertEquals("video/quicktime", RangeStreamUtil.contentTypeFor("a.MOV"));
		assertEquals("image/png", RangeStreamUtil.contentTypeFor("a.png"));
		assertEquals("image/jpeg", RangeStreamUtil.contentTypeFor("a.jpg"));
		assertEquals("image/jpeg", RangeStreamUtil.contentTypeFor("noext"));
	}
}

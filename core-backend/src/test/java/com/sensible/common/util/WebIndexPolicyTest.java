package com.sensible.common.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 공개 웹 색인 기준 (2-34차 3-1). 얇은 글·스타 페이지는 색인하지 않는다.
 */
public class WebIndexPolicyTest {

	private static String repeat(String s, int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) sb.append(s);
		return sb.toString();
	}

	@Test
	public void 링크와_태그와_공백은_글자_수에서_뺀다() {
		assertEquals(2, WebIndexPolicy.meaningfulLength("들꽃\n\nhttps://youtube.com/playlist?list=PLCwj4ma_MKfh1dTqbJOWK652jenXmYTmk&si=x"));
		assertEquals(1, WebIndexPolicy.meaningfulLength("<p>  굿 </p>  www.example.com/abc"));
		assertEquals(3, WebIndexPolicy.meaningfulLength("a  b"));
		assertEquals(0, WebIndexPolicy.meaningfulLength(null));
	}

	@Test
	public void 운영의_짧은_글은_색인하지_않는다() {
		assertFalse(WebIndexPolicy.isIndexablePost("굿"));
		assertFalse(WebIndexPolicy.isIndexablePost("ㅠㅠ"));
		assertFalse(WebIndexPolicy.isIndexablePost("들꽃\n\nhttps://youtube.com/playlist?list=" + repeat("x", 200)));
	}

	@Test
	public void 기준_이상이면_색인한다() {
		assertTrue(WebIndexPolicy.isIndexablePost(repeat("가", WebIndexPolicy.POST_MIN_CHARS)));
		assertFalse(WebIndexPolicy.isIndexablePost(repeat("가", WebIndexPolicy.POST_MIN_CHARS - 1)));
	}

	@Test
	public void 스타는_소개문이_충분하거나_색인_가능한_글이_있으면_색인() {
		assertTrue(WebIndexPolicy.isIndexableStar(repeat("a", 100), false));
		assertTrue(WebIndexPolicy.isIndexableStar("", true));
		assertFalse(WebIndexPolicy.isIndexableStar("짧은 소개", false));
		assertFalse(WebIndexPolicy.isIndexableStar(null, false));
	}
}

package com.sensible.api.controller;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

/**
 * 웹 랜딩 최근 게시물 카드의 부가 정보 처리 검증.
 * categoryLabel/thumbAlt는 컨트롤러 내부 헬퍼라 리플렉션으로 호출한다.
 */
public class DeepLinkControllerCardTest {

	private final DeepLinkController controller = new DeepLinkController();

	private String categoryLabel(Object code) throws Exception {
		Method m = DeepLinkController.class.getDeclaredMethod("categoryLabel", Object.class);
		m.setAccessible(true);
		return (String) m.invoke(controller, code);
	}

	private String thumbAlt(String snippet, String author) throws Exception {
		Method m = DeepLinkController.class.getDeclaredMethod("thumbAlt", String.class, String.class);
		m.setAccessible(true);
		return (String) m.invoke(controller, snippet, author);
	}

	@Test
	public void categoryLabel_허용_직군은_영문_라벨로_바꾼다() throws Exception {
		assertEquals("Star", categoryLabel("STAR"));
		assertEquals("Celebrity", categoryLabel("CELEB"));
		assertEquals("Brand", categoryLabel("BRAND"));
		assertEquals("Organization", categoryLabel("ORG"));
		assertEquals("University", categoryLabel("UNIV"));
		assertEquals("City", categoryLabel("CITY"));
		assertEquals("Media", categoryLabel("MEDIA"));
	}

	@Test
	public void categoryLabel_미분류와_미지정은_빈값이라_뱃지가_숨는다() throws Exception {
		// GENERAL은 직군을 고르지 않은 계정이라 카드에 표시할 의미가 없다
		assertEquals("", categoryLabel("GENERAL"));
		assertEquals("", categoryLabel(null));
		assertEquals("", categoryLabel("HACKER"));
		assertEquals("", categoryLabel("star")); // 소문자는 화이트리스트 밖
	}

	@Test
	public void thumbAlt_본문이_있으면_본문을_대체텍스트로_쓴다() throws Exception {
		assertEquals("Beautiful flower", thumbAlt("Beautiful flower", "Emma"));
	}

	@Test
	public void thumbAlt_본문이_비면_작성자_기준_문구로_채운다() throws Exception {
		// 사진만 올린 글이 많아 alt가 비면 크롤러가 읽을 텍스트가 사라진다
		assertEquals("Photo posted by Emma", thumbAlt("", "Emma"));
		assertEquals("Photo posted by Emma", thumbAlt(null, "Emma"));
	}
}

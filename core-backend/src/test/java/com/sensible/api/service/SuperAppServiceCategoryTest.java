package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 스타 직군(카테고리) 화이트리스트 정규화 규칙 검증.
 * 가입(claim/social) 저장 시 이 규칙으로 WH_PRESS.STAR_CATEGORY가 결정된다.
 */
public class SuperAppServiceCategoryTest {

	@Test
	public void 허용된_직군_값은_그대로_통과한다() {
		assertEquals("STAR", SuperAppService.normalizeStarCategory("STAR"));
		assertEquals("CELEB", SuperAppService.normalizeStarCategory("CELEB"));
		assertEquals("BRAND", SuperAppService.normalizeStarCategory("BRAND"));
		assertEquals("UNIV", SuperAppService.normalizeStarCategory("UNIV"));
		assertEquals("CITY", SuperAppService.normalizeStarCategory("CITY"));
		assertEquals("MEDIA", SuperAppService.normalizeStarCategory("MEDIA"));
	}

	@Test
	public void 허용_외_값은_미분류로_정규화한다() {
		assertEquals("GENERAL", SuperAppService.normalizeStarCategory("HACKER"));
		assertEquals("GENERAL", SuperAppService.normalizeStarCategory("star")); // 소문자 불허
		assertEquals("GENERAL", SuperAppService.normalizeStarCategory(""));
	}

	@Test
	public void null과_비문자열_입력도_미분류로_정규화한다() {
		// 구버전 앱은 category 자체를 보내지 않는다 (null)
		assertEquals("GENERAL", SuperAppService.normalizeStarCategory(null));
		assertEquals("GENERAL", SuperAppService.normalizeStarCategory(123));
	}

	@Test
	public void GENERAL은_직접_선택_값으로는_허용하지_않는다() {
		// 가입 화면에는 GENERAL 선택지가 없다 — 전달돼도 결과는 동일하게 GENERAL
		assertEquals("GENERAL", SuperAppService.normalizeStarCategory("GENERAL"));
	}
}

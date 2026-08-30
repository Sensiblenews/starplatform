package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 앱 프로필 수정의 소개문(bio) 파라미터 정규화 규칙 검증 (2-27차).
 * null = 이번 요청은 소개문을 건드리지 않음, 빈 문자열 = 비우기.
 */
public class SuperAppServiceBioTest {

	@Test
	public void null이면_소개문을_건드리지_않는다() {
		assertNull(SuperAppService.normalizeBio(null));
	}

	@Test
	public void 앞뒤_공백은_잘라낸다() {
		assertEquals("Hello fans", SuperAppService.normalizeBio("  Hello fans  "));
	}

	@Test
	public void 공백만_있으면_비우기로_처리한다() {
		assertEquals("", SuperAppService.normalizeBio("   "));
	}

	@Test
	public void 빈_문자열은_비우기로_유지한다() {
		assertEquals("", SuperAppService.normalizeBio(""));
	}

	@Test
	public void 비문자열_입력도_문자열로_정규화한다() {
		// JSON 파싱 결과가 숫자로 들어와도 저장은 문자열 기준
		assertEquals("123", SuperAppService.normalizeBio(123));
	}
}

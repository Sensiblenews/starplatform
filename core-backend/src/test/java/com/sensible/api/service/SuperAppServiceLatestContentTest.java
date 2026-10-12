package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 로비 Latest Contents 이벤트 입력 정규화 (2-33차).
 * 앱이 보낸 값 중 허용 목록 밖의 이벤트 종류·빈 기기 ID 는 저장하지 않아야 한다.
 */
public class SuperAppServiceLatestContentTest {

	@Test
	public void 이벤트_종류는_대소문자_무관하게_허용_목록만() {
		assertEquals("IMPRESSION", SuperAppService.normalizeLatestContentEventType("impression"));
		assertEquals("PROFILE_CLICK", SuperAppService.normalizeLatestContentEventType(" Profile_Click "));
		assertNull(SuperAppService.normalizeLatestContentEventType("COMPLETE"));
		assertNull(SuperAppService.normalizeLatestContentEventType(null));
		assertNull(SuperAppService.normalizeLatestContentEventType(""));
	}

	@Test
	public void 기기ID_공백과_문자열_null은_버리고_긴_값은_100자로_자른다() {
		assertNull(SuperAppService.normalizeLatestContentDeviceId(null));
		assertNull(SuperAppService.normalizeLatestContentDeviceId("  "));
		assertNull(SuperAppService.normalizeLatestContentDeviceId("undefined"));
		assertEquals("abc", SuperAppService.normalizeLatestContentDeviceId(" abc "));
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 150; i++) sb.append('x');
		assertEquals(100, SuperAppService.normalizeLatestContentDeviceId(sb.toString()).length());
	}
}

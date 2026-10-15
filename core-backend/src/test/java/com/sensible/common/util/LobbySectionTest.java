package com.sensible.common.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 로비 편성 구역 정규화 (2-34차).
 * 파라미터 없는 2-33차 호출은 Star Shorts 로, 모르는 값은 캐시 키·SQL 에 들어가지 않아야 한다.
 */
public class LobbySectionTest {

	@Test
	public void 빈_값과_모르는_값은_SHORTS() {
		assertEquals("SHORTS", LobbySection.normalize(null));
		assertEquals("SHORTS", LobbySection.normalize(""));
		assertEquals("SHORTS", LobbySection.normalize("shorts"));
		assertEquals("SHORTS", LobbySection.normalize("LATEST' OR 1=1"));
	}

	@Test
	public void 세로형_가로형은_대소문자_무관() {
		assertEquals("LATEST", LobbySection.normalize(" latest "));
		assertEquals("HOT", LobbySection.normalize("Hot"));
	}

	@Test
	public void 설정_행ID는_기존_1번을_SHORTS가_쓴다() {
		assertEquals(1, LobbySection.settingId("SHORTS"));
		assertEquals(2, LobbySection.settingId("LATEST"));
		assertEquals(3, LobbySection.settingId("HOT"));
	}

	@Test
	public void StarShorts는_항상_영상만() {
		assertEquals("VIDEO", LobbySection.normalizeMediaFilter("SHORTS", "ALL"));
		assertEquals("VIDEO", LobbySection.normalizeMediaFilter("SHORTS", null));
	}

	@Test
	public void 세로형_가로형은_VIDEO_를_고를_때만_영상() {
		assertEquals("VIDEO", LobbySection.normalizeMediaFilter("LATEST", "video"));
		assertEquals("ALL", LobbySection.normalizeMediaFilter("LATEST", "ALL"));
		assertEquals("ALL", LobbySection.normalizeMediaFilter("HOT", null));
		assertEquals("ALL", LobbySection.normalizeMediaFilter("HOT", "photo"));
	}

	@Test
	public void 어드민_메뉴키는_사이드바_키와_같다() {
		assertEquals("latest_contents", LobbySection.activeMenu("SHORTS"));
		assertEquals("latest_feed", LobbySection.activeMenu("LATEST"));
		assertEquals("hot_content", LobbySection.activeMenu("HOT"));
	}
}

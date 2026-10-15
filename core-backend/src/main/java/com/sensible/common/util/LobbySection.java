package com.sensible.common.util;

/**
 * 로비 편성 구역 (WH_LATEST_CONTENT.SECTION, 2-34차).
 * 테이블 이름(LATEST_CONTENT)과 화면 이름이 엇갈리니 주의:
 *   SHORTS = 로비 맨 위 "Star Shorts" 숏폼 (2-33차 "Latest Contents" 의 기존 편성 전부)
 *   LATEST = 첫 네이티브 광고와 Today's TOP 사이 세로형 "Latest Content"
 *   HOT    = 그 아래 가로형 미니 숏폼 "Hot Content"
 * 앱·어드민 파라미터는 전부 여기서 정규화한다 (캐시 키·SQL 에 임의 값이 들어가지 않게).
 */
public final class LobbySection {

	public static final String SHORTS = "SHORTS";
	public static final String LATEST = "LATEST";
	public static final String HOT = "HOT";

	/** 게시물 선택 목록 필터 — 전체 게시물 / 동영상 게시물 */
	public static final String MEDIA_ALL = "ALL";
	public static final String MEDIA_VIDEO = "VIDEO";

	private LobbySection() { }

	/** 구역 정규화 (순수 함수). 모르는 값·빈 값은 SHORTS — 파라미터 없이 부르던 2-33차 호출이 그대로 동작한다 */
	public static String normalize(Object raw) {
		if (raw == null) return SHORTS;
		String value = String.valueOf(raw).trim().toUpperCase();
		if (LATEST.equals(value) || HOT.equals(value)) return value;
		return SHORTS;
	}

	/** 설정 테이블 행 ID (WH_LATEST_CONTENT_SETTING.ID). 1 은 2-33차의 기존 행 */
	public static int settingId(String section) {
		if (LATEST.equals(section)) return 2;
		if (HOT.equals(section)) return 3;
		return 1;
	}

	/** Star Shorts 는 영상만 편성한다 (2-33차 규칙 유지). 세로형·가로형은 사진 글도 받는다 */
	public static boolean videoOnly(String section) {
		return SHORTS.equals(section);
	}

	/**
	 * 게시물 선택 목록 필터 정규화 (순수 함수). Star Shorts 는 항상 VIDEO,
	 * 세로형·가로형은 VIDEO 를 고른 경우만 VIDEO, 나머지는 ALL.
	 */
	public static String normalizeMediaFilter(String section, Object raw) {
		if (videoOnly(section)) return MEDIA_VIDEO;
		if (raw != null && MEDIA_VIDEO.equalsIgnoreCase(String.valueOf(raw).trim())) return MEDIA_VIDEO;
		return MEDIA_ALL;
	}

	/** 어드민 화면 제목·메뉴 키 */
	public static String adminTitle(String section) {
		if (LATEST.equals(section)) return "세로형 자동재생 (Latest Content)";
		if (HOT.equals(section)) return "가로형 미니숏폼 (Hot Content)";
		return "Star Shorts 숏폼";
	}

	public static String activeMenu(String section) {
		if (LATEST.equals(section)) return "latest_feed";
		if (HOT.equals(section)) return "hot_content";
		return "latest_contents";
	}
}

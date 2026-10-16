package com.sensible.common.util;

import java.util.regex.Pattern;

/**
 * 공개 웹 페이지 색인 기준 (2-34차 3-1 AdSense "가치 없는 콘텐츠" 대응).
 *
 * 2026-10-16 운영 점검: 사이트맵의 글 67개 중 64개가 본문 150자 미만("굿", "ㅠㅠ" 등), 스타 페이지 13개 중 11개도
 * 고유 텍스트가 150자 미만인데 전부 색인 대상이었다. 이런 얇은 페이지가 사이트 품질 판정을 끌어내린다.
 * 그래서 고유 텍스트가 기준 미만인 글·스타 페이지는 noindex 를 걸고 사이트맵에서 뺀다.
 *
 * - 판단은 오직 "페이지에 렌더되는 콘텐츠" 기준이다. 사람과 크롤러에게 같은 화면을 보여 주고 색인 여부만 정한다
 *   (UA 분기를 섞으면 클로킹 — 2-27차 원칙 그대로)
 * - 링크(URL)·HTML 태그·공백은 글자 수에서 뺀다. "들꽃 + 유튜브 링크" 같은 글은 얇은 페이지다
 * - 콘텐츠가 길어지면(소개문 작성·긴 글) 자동으로 다시 색인 대상이 된다
 */
public final class WebIndexPolicy {

	/** 글 페이지 색인 최소 글자 수 (링크·태그 제외) */
	public static final int POST_MIN_CHARS = 100;
	/** 스타 페이지 색인 최소 소개문(PRS_BIO) 글자 수 — 소개문이 짧아도 색인 가능한 글이 하나 있으면 색인 */
	public static final int STAR_BIO_MIN_CHARS = 100;

	private static final Pattern URL = Pattern.compile("(?i)(https?://|www\\.)\\S+");
	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	private static final Pattern SPACE = Pattern.compile("\\s+");

	private WebIndexPolicy() {
	}

	/** 링크·HTML 태그를 빼고 공백을 하나로 줄인 뒤의 글자 수 (순수 함수) */
	public static int meaningfulLength(String raw) {
		if (raw == null) return 0;
		String text = URL.matcher(raw).replaceAll(" ");
		text = TAG.matcher(text).replaceAll(" ");
		text = SPACE.matcher(text).replaceAll(" ").trim();
		return text.codePointCount(0, text.length());
	}

	public static boolean isIndexablePost(String body) {
		return meaningfulLength(body) >= POST_MIN_CHARS;
	}

	public static boolean isIndexableStar(String bio, boolean hasIndexablePost) {
		return hasIndexablePost || meaningfulLength(bio) >= STAR_BIO_MIN_CHARS;
	}
}

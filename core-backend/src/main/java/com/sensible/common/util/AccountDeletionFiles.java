package com.sensible.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.sensible.common.Constants;

/**
 * 계정 삭제 때 지울 업로드 파일 경로 계산 (2026-10-17 App Store 심사 5.1.1(v)).
 *
 * DB 의 미디어 URL(https://witch-hunting.com/img/…, /video/…, /video/thumnail/…)을 디스크 경로로 바꾼다.
 * 검수 대기·숨김 상태 파일은 공개 폴더가 아니라 검수 폴더에 있으므로 이미지·영상은 그쪽 후보도 같이 낸다.
 * 파일명은 업로드 때 서버가 만든 UUID 계열이라 영문·숫자·점·밑줄·하이픈만 허용한다 —
 * URL 에 경로 조작(../)이 섞여 있어도 저장 폴더 밖을 가리키지 못하게 하려는 것 (순수 함수)
 */
public final class AccountDeletionFiles {

	private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._-]+");

	private AccountDeletionFiles() {
	}

	/** URL 하나 → 지울 후보 경로들. 우리 서버 URL 이 아니거나 파일명이 안전하지 않으면 빈 목록 */
	public static List<String> candidates(String url) {
		if (url == null) return Collections.emptyList();
		// 썸네일 URL 이 영상 URL 의 하위 경로라 썸네일부터 본다
		if (url.startsWith(Constants._VIDEO_THUMNAIL_FILE_URL)) {
			String name = safeName(url.substring(Constants._VIDEO_THUMNAIL_FILE_URL.length()));
			return name == null ? Collections.<String>emptyList() : Collections.singletonList(Constants._VIDEO_THUMNAIL_SAVE_PATH + name);
		}
		if (url.startsWith(Constants._VIDEO_FILE_URL)) {
			return withModeration(Constants._VIDEO_SAVE_PATH, safeName(url.substring(Constants._VIDEO_FILE_URL.length())));
		}
		if (url.startsWith(Constants._FILE_URL)) {
			return withModeration(Constants._FILE_SAVE_PATH, safeName(url.substring(Constants._FILE_URL.length())));
		}
		return Collections.emptyList();
	}

	private static List<String> withModeration(String publicDir, String name) {
		if (name == null) return Collections.emptyList();
		List<String> paths = new ArrayList<>();
		paths.add(publicDir + name);
		paths.add(Constants._PENDING_SAVE_PATH + name);
		paths.add(Constants._HIDDEN_SAVE_PATH + name);
		return paths;
	}

	/** 하위 폴더·쿼리 없는 단일 파일명만 통과. ".", ".." 는 거부 */
	static String safeName(String name) {
		if (name == null || name.isEmpty()) return null;
		if (!SAFE_NAME.matcher(name).matches()) return null;
		if (name.equals(".") || name.equals("..")) return null;
		return name;
	}
}

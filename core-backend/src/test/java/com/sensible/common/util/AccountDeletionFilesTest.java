package com.sensible.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.sensible.common.Constants;

public class AccountDeletionFilesTest {

	@Test
	public void 이미지는_공개_검수대기_숨김_폴더_후보를_낸다() {
		List<String> paths = AccountDeletionFiles.candidates(Constants._FILE_URL + "abc_123.jpg");
		assertEquals(3, paths.size());
		assertEquals(Constants._FILE_SAVE_PATH + "abc_123.jpg", paths.get(0));
		assertEquals(Constants._PENDING_SAVE_PATH + "abc_123.jpg", paths.get(1));
		assertEquals(Constants._HIDDEN_SAVE_PATH + "abc_123.jpg", paths.get(2));
	}

	@Test
	public void 썸네일은_썸네일_폴더만() {
		List<String> paths = AccountDeletionFiles.candidates(Constants._VIDEO_THUMNAIL_FILE_URL + "v1_thumb.jpg");
		assertEquals(1, paths.size());
		assertEquals(Constants._VIDEO_THUMNAIL_SAVE_PATH + "v1_thumb.jpg", paths.get(0));
	}

	@Test
	public void 영상은_영상_폴더부터() {
		List<String> paths = AccountDeletionFiles.candidates(Constants._VIDEO_FILE_URL + "v1.mp4");
		assertEquals(Constants._VIDEO_SAVE_PATH + "v1.mp4", paths.get(0));
	}

	@Test
	public void 경로_조작과_외부_URL은_무시한다() {
		assertTrue(AccountDeletionFiles.candidates(Constants._FILE_URL + "../etc/passwd").isEmpty());
		assertTrue(AccountDeletionFiles.candidates(Constants._FILE_URL + "..").isEmpty());
		assertTrue(AccountDeletionFiles.candidates(Constants._FILE_URL + "a/b.jpg").isEmpty());
		assertTrue(AccountDeletionFiles.candidates(Constants._FILE_URL).isEmpty());
		assertTrue(AccountDeletionFiles.candidates("https://example.com/img/a.jpg").isEmpty());
		assertTrue(AccountDeletionFiles.candidates(null).isEmpty());
	}
}

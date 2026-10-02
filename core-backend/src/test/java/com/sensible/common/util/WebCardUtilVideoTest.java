package com.sensible.common.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * 2-31차: 웹 카드·상세에서 영상 미디어를 구분해 원본 주소를 내리는지 검증.
 */
public class WebCardUtilVideoTest {

	private static final String BASE = "https://witch-hunting.com";

	private Map<String, Object> post(String type, String mediaUrl, String thumbUrl) {
		Map<String, Object> p = new HashMap<>();
		p.put("CON_ID", "C1");
		p.put("PRS_ID", "P1");
		p.put("PRS_NAME", "Emma");
		p.put("CON_BODY", "hello");
		p.put("MEDIA_TYPE", type);
		p.put("MEDIA_URL", mediaUrl);
		p.put("THUMB_URL", thumbUrl);
		return p;
	}

	@Test
	public void 영상_첫미디어_카드는_video_원본주소와_썸네일_image를_갖는다() {
		Map<String, Object> card = WebCardUtil.toPostCard(
				post("VIDEO", "https://witch-hunting.com/video/a.mp4", "https://witch-hunting.com/video/thumnail/a_thumb.jpg"), BASE);
		assertEquals("https://witch-hunting.com/video/a.mp4", card.get("video"));
		// image 는 그대로 포스터 썸네일
		assertEquals("https://witch-hunting.com/video/thumnail/a_thumb.jpg", card.get("image"));
	}

	@Test
	public void 사진_카드에는_video_키가_없다() {
		Map<String, Object> card = WebCardUtil.toPostCard(post("PHOTO", "/img/a.jpg", "/img/a_t.jpg"), BASE);
		assertFalse(card.containsKey("video"));
		assertEquals("https://witch-hunting.com/img/a_t.jpg", card.get("image"));
	}

	@Test
	public void 미디어_없는_글_카드에도_video_키가_없다() {
		Map<String, Object> card = WebCardUtil.toPostCard(post(null, null, null), BASE);
		assertFalse(card.containsKey("video"));
	}

	@Test
	public void videoUrl_상대경로는_절대주소로_바꾸고_영상이_아니거나_주소가_없으면_빈값() {
		assertEquals("https://witch-hunting.com/video/a.mp4", WebCardUtil.videoUrl(post("VIDEO", "/video/a.mp4", null), BASE));
		assertEquals("", WebCardUtil.videoUrl(post("VIDEO", null, "/t.jpg"), BASE));
		assertEquals("", WebCardUtil.videoUrl(post("PHOTO", "/img/a.jpg", null), BASE));
		assertEquals("", WebCardUtil.videoUrl(null, BASE));
	}
}

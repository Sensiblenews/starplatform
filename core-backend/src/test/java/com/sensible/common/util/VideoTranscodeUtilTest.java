package com.sensible.common.util;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

/**
 * 2-31차 후속: 업로드 영상 웹 호환 변환 — 명령 구성과 확장자 판정, 실패 시 원본 보존.
 */
public class VideoTranscodeUtilTest {

	@Test
	public void 아이폰_quicktime_은_mov_로_저장된다() {
		assertEquals(".mov", VideoTranscodeUtil.extensionFromDataUri("data:video/quicktime;base64"));
		assertEquals(".mov", VideoTranscodeUtil.extensionFromDataUri("data:video/mov;base64"));
	}

	@Test
	public void avi_webm_ogg_mp4_판정() {
		assertEquals(".avi", VideoTranscodeUtil.extensionFromDataUri("data:video/x-msvideo;base64"));
		assertEquals(".webm", VideoTranscodeUtil.extensionFromDataUri("data:video/webm;base64"));
		assertEquals(".ogg", VideoTranscodeUtil.extensionFromDataUri("data:video/ogg;base64"));
		assertEquals(".mp4", VideoTranscodeUtil.extensionFromDataUri("data:video/mp4;base64"));
		assertEquals(".mp4", VideoTranscodeUtil.extensionFromDataUri(null));
	}

	@Test
	public void 변환_명령은_H264_AAC_faststart_를_지정한다() {
		List<String> cmd = VideoTranscodeUtil.buildCommand("/in/a.mov", "/out/a.mp4");
		assertEquals("ffmpeg", cmd.get(0));
		assertEquals("/in/a.mov", cmd.get(cmd.indexOf("-i") + 1));
		assertEquals("libx264", cmd.get(cmd.indexOf("-c:v") + 1));
		assertEquals("aac", cmd.get(cmd.indexOf("-c:a") + 1));
		assertEquals("yuv420p", cmd.get(cmd.indexOf("-pix_fmt") + 1));
		assertEquals("+faststart", cmd.get(cmd.indexOf("-movflags") + 1));
		// 출력 경로가 마지막, 덮어쓰기 허용
		assertEquals("/out/a.mp4", cmd.get(cmd.size() - 1));
		assertTrue(cmd.contains("-y"));
	}

	@Test
	public void 변환이_실패하면_원본은_남고_임시파일은_없다() throws Exception {
		// 비어 있는 입력은 ffmpeg 가 거부한다. ffmpeg 가 없는 환경에서는 실행 자체가 실패한다. 둘 다 "실패" 경로다
		Path dir = Files.createTempDirectory("vt");
		Path saved = dir.resolve("src.mov");
		Files.write(saved, new byte[] { 0, 1, 2 });
		Path target = dir.resolve("src.mp4");

		assertFalse(VideoTranscodeUtil.toWebMp4(saved, target));

		assertTrue(Files.exists(saved));
		assertFalse(Files.exists(target));
		assertFalse(Files.exists(dir.resolve("src.mp4.transcoding.mp4")));
	}
}

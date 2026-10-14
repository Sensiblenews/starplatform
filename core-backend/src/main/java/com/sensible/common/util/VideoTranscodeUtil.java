package com.sensible.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 업로드 영상을 웹 호환 MP4(H.264 + AAC, faststart)로 변환한다 (2-31차 후속).
 *
 * 배경: 아이폰 기본 녹화(고효율)는 HEVC 라서 PC Chrome·Firefox·다수 Android 에서 재생되지 않는다.
 * 운영 영상 3개 중 2개가 HEVC 였고, 웹 카드의 자동재생 스크립트는 재생 실패 시 영상을 조용히 치웠다.
 * 서버에는 썸네일 생성용 ffmpeg 가 이미 있으므로 새 의존성 없이 같은 바이너리로 변환한다.
 *
 * 스타 피드 영상(2-34차부터)과 어드민 숏폼 업로드는 작업 큐(LatestContentUploadJobs)에서 비동기로 부른다.
 * 실패하면 원본을 그대로 쓴다 — 변환이 안 된다고 업로드가 막히지는 않는다.
 */
public final class VideoTranscodeUtil {

	/** 재생용 영상 비트레이트 상한 (요청서 권장 2.5~4Mbps 의 위쪽). bufsize 는 상한의 2배 */
	static final String MAX_VIDEO_BITRATE = "4M";
	static final String VIDEO_BUFSIZE = "8M";

	/**
	 * 짧은 변을 1080 이하로 줄이고(세로 영상은 폭, 가로 영상은 높이) 다른 변은 비율대로(-2 = 짝수 맞춤).
	 * 원본이 더 작으면 그대로, 홀수 크기는 짝수로 내림 (yuv420p 는 홀수를 받지 않는다).
	 * 쉼표가 필터 구분자로 읽히지 않게 식을 작은따옴표로 감싼다 (ProcessBuilder 라 셸을 거치지 않는다)
	 */
	static final String SCALE_FILTER =
			"scale='if(lt(iw,ih),trunc(min(1080,iw)/2)*2,-2)':'if(lt(iw,ih),-2,trunc(min(1080,ih)/2)*2)'";

	/** 변환 타임아웃. 넘기면 프로세스를 죽이고 원본을 쓴다 */
	static final long TIMEOUT_SECONDS = 600L;

	private VideoTranscodeUtil() {
	}

	/**
	 * data URI 의 MIME 부분으로 원본 확장자를 정한다.
	 * 예전 코드는 "mov" 문자열만 찾아서 아이폰의 video/quicktime 이 .mp4 로 저장됐다.
	 */
	public static String extensionFromDataUri(String dataUriHead) {
		String head = dataUriHead == null ? "" : dataUriHead.toLowerCase();
		if (head.contains("webm")) return ".webm";
		if (head.contains("ogg")) return ".ogg";
		if (head.contains("quicktime") || head.contains("mov")) return ".mov";
		if (head.contains("x-msvideo") || head.contains("avi")) return ".avi";
		return ".mp4";
	}

	/**
	 * ffmpeg 명령. 순수 함수라 단위 테스트 대상이다.
	 * - libx264 + aac + yuv420p: 가장 넓은 호환성 (MemberService 레거시 변환과 같은 선택)
	 * - scale=trunc(iw/2)*2:...: yuv420p 는 홀수 크기를 받지 않는다
	 * - movflags +faststart: moov 를 앞으로 옮겨 preload="none" 카드가 헤더만 먼저 받게 한다
	 * - crf 23 / preset fast: 화질은 원본에 가깝게, 변환 시간은 업로드 응답 안에
	 * - 짧은 변 1080 상한 + 최대 4Mbps (2-34차 1-3 — 95MB 원본을 재생용으로 줄인다. 작은 영상은 키우지 않는다)
	 */
	public static List<String> buildCommand(String inputPath, String outputPath) {
		return new ArrayList<>(Arrays.asList(
				"ffmpeg", "-i", inputPath,
				"-c:v", "libx264", "-preset", "fast", "-crf", "23",
				"-maxrate", MAX_VIDEO_BITRATE, "-bufsize", VIDEO_BUFSIZE,
				"-pix_fmt", "yuv420p",
				"-vf", SCALE_FILTER,
				"-c:a", "aac", "-b:a", "128k",
				"-movflags", "+faststart",
				"-y", outputPath));
	}

	/**
	 * saved 를 H.264 MP4 로 바꿔 target 에 둔다. 성공하면 saved 는 지운다(같은 경로면 덮어쓴다).
	 *
	 * @return 변환 성공 여부. 실패 시 saved 는 손대지 않는다
	 */
	public static boolean toWebMp4(Path saved, Path target) {
		Path tmp = target.resolveSibling(target.getFileName() + ".transcoding.mp4");
		try {
			if (!runFfmpeg(saved, tmp)) {
				Files.deleteIfExists(tmp);
				return false;
			}
			if (!Files.exists(tmp) || Files.size(tmp) == 0) {
				Files.deleteIfExists(tmp);
				return false;
			}
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			if (!saved.equals(target)) {
				Files.deleteIfExists(saved);
			}
			return true;
		} catch (Exception e) {
			System.out.println("[VIDEO] transcode failed, keeping original: " + e.getMessage());
			try {
				Files.deleteIfExists(tmp);
			} catch (IOException ignore) {
			}
			return false;
		}
	}

	/** 썸네일 추출 타임아웃. 한 프레임만 뽑으므로 변환보다 훨씬 짧다 */
	static final long THUMBNAIL_TIMEOUT_SECONDS = 60L;

	/**
	 * 썸네일(첫 프레임) 추출 ffmpeg 명령. 순수 함수라 단위 테스트 대상이다 (2-33차).
	 * 기존 SuperAppService.generateVideoThumbnail 과 같은 옵션(-ss 1초, 한 프레임, 덮어쓰기)이다.
	 */
	public static List<String> buildThumbnailCommand(String videoPath, String thumbPath, String seekTime) {
		return new ArrayList<>(Arrays.asList(
				"ffmpeg", "-i", videoPath,
				"-ss", seekTime,
				"-vframes", "1",
				"-y", thumbPath));
	}

	/**
	 * video 의 1초 지점 프레임을 thumb(jpg)로 뽑는다. 1초보다 짧은 영상은 프레임이 안 나오므로 0초로 한 번 더 시도한다.
	 *
	 * @return 썸네일 파일이 실제로 생겼는지
	 */
	public static boolean extractThumbnail(Path video, Path thumb) {
		for (String seek : new String[] { "00:00:01", "00:00:00" }) {
			try {
				if (runThumbnail(video, thumb, seek) && Files.exists(thumb) && Files.size(thumb) > 0) {
					return true;
				}
				Files.deleteIfExists(thumb);
			} catch (Exception e) {
				System.out.println("[VIDEO] thumbnail failed (" + seek + "): " + e.getMessage());
			}
		}
		return false;
	}

	private static boolean runThumbnail(Path video, Path thumb, String seek) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(buildThumbnailCommand(video.toString(), thumb.toString(), seek));
		pb.redirectErrorStream(true);
		pb.redirectOutput(new File(System.getProperty("os.name", "").toLowerCase().contains("win") ? "NUL" : "/dev/null"));
		Process process = pb.start();
		if (!process.waitFor(THUMBNAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			System.out.println("[VIDEO] thumbnail timed out: " + video);
			return false;
		}
		return process.exitValue() == 0;
	}

	private static boolean runFfmpeg(Path input, Path output) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(buildCommand(input.toString(), output.toString()));
		pb.redirectErrorStream(true);
		// 출력을 읽어 주지 않으면 ffmpeg 가 파이프 버퍼에 막혀 멈춘다
		pb.redirectOutput(new File(System.getProperty("os.name", "").toLowerCase().contains("win") ? "NUL" : "/dev/null"));
		Process process = pb.start();
		if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			System.out.println("[VIDEO] transcode timed out after " + TIMEOUT_SECONDS + "s: " + input);
			return false;
		}
		int code = process.exitValue();
		if (code != 0) {
			System.out.println("[VIDEO] ffmpeg exit " + code + ": " + input);
		}
		return code == 0;
	}
}

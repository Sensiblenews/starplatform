package com.sensible.common.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import com.sensible.common.Constants;

/**
 * 업로드 이미지 검증 및 검수 상태별 파일 이동 (2-26차).
 *
 * 검증은 "파일이 정상적인 이미지인가"만 본다. 내용의 유해성은 판별하지 않는다 —
 * 그건 관리자 검수의 몫이다. 확장자·MIME은 클라이언트가 마음대로 붙일 수 있으므로
 * 실제 바이너리 시그니처(매직 바이트)까지 확인해야 위장 파일을 막을 수 있다.
 *
 * 파일 배치 규칙:
 *   검수 대기 → Constants._PENDING_SAVE_PATH (웹앱 밖, 외부 접근 불가)
 *   승인      → Constants._FILE_SAVE_PATH    (톰캣 정적 웹앱, 이때부터 공개)
 *   차단      → Constants._HIDDEN_SAVE_PATH  (웹앱 밖, 삭제하지 않고 보관)
 */
public class ImageModerationUtil {

	/** 허용 확장자. 이 목록에 없으면 거부한다 */
	public static final List<String> ALLOWED_EXTENSIONS =
			java.util.Collections.unmodifiableList(Arrays.asList("jpg", "jpeg", "png", "webp"));

	/** 업로드 용량 상한 (10MB) */
	public static final int MAX_FILE_BYTES = 10 * 1024 * 1024;

	/** 해상도 상한. 픽셀 폭탄으로 디코딩 메모리를 터뜨리는 것을 막는다 */
	public static final int MAX_DIMENSION = 10000;

	/** 검증 실패 사유. 사용자에게 내보낼 문구는 호출부에서 정한다 */
	public enum Rejection {
		EMPTY,          // 빈 파일
		EXTENSION,      // 허용하지 않는 확장자
		MIME,           // 허용하지 않는 MIME
		SIGNATURE,      // 실제 내용이 이미지가 아님 (위장 파일)
		TOO_LARGE,      // 용량 초과
		TOO_LARGE_DIMENSION, // 해상도 초과
		UNREADABLE      // 디코딩 실패
	}

	/** 검증 결과. rejection이 null이면 통과 */
	public static class Result {
		public final Rejection rejection;
		public final String extension;

		private Result(Rejection rejection, String extension) {
			this.rejection = rejection;
			this.extension = extension;
		}

		public boolean isValid() {
			return rejection == null;
		}

		public static Result ok(String extension) {
			return new Result(null, extension);
		}

		public static Result reject(Rejection rejection) {
			return new Result(rejection, null);
		}
	}

	/** 우리가 직접 만든 파일명만 통과시킨다 (UUID·숫자 + 확장자) */
	private static final java.util.regex.Pattern SAFE_FILE_NAME =
			java.util.regex.Pattern.compile("^[A-Za-z0-9_.-]{1,120}$");

	private ImageModerationUtil() {
	}

	// ===================== 검증 =====================

	/**
	 * 업로드된 이미지 바이트를 검증한다.
	 *
	 * @param bytes        디코딩된 파일 내용
	 * @param originalName 원본 파일명 (확장자 판단용, null 허용)
	 * @param declaredMime 클라이언트가 알려준 MIME (null 허용 — 있으면 같이 검사한다)
	 */
	public static Result validate(byte[] bytes, String originalName, String declaredMime) {
		if (bytes == null || bytes.length == 0) {
			return Result.reject(Rejection.EMPTY);
		}
		if (bytes.length > MAX_FILE_BYTES) {
			return Result.reject(Rejection.TOO_LARGE);
		}

		// 파일명을 함께 보낸 경로(채팅 업로드 등)에서만 확장자를 본다.
		// 게시물 업로드는 base64만 보내 파일명이 없으므로, 그때는 아래 시그니처 검사가 실질 판정이다.
		if (originalName != null) {
			String ext = extensionOf(originalName);
			if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
				return Result.reject(Rejection.EXTENSION);
			}
		}

		// 클라이언트가 MIME을 보냈다면 그것도 허용 목록 안이어야 한다.
		// 안 보냈다고 거부하지는 않는다 — 어차피 아래 시그니처 검사가 실질 판정이다.
		if (declaredMime != null && !declaredMime.trim().isEmpty()
				&& !isAllowedMime(declaredMime)) {
			return Result.reject(Rejection.MIME);
		}

		// 실제 바이너리가 이미지인지 확인 (위장 파일 차단)
		String actual = detectImageType(bytes);
		if (actual == null) {
			return Result.reject(Rejection.SIGNATURE);
		}

		// 해상도 상한. webp는 ImageIO 기본 지원이 아니라 읽지 못할 수 있는데,
		// 시그니처로 이미 webp임을 확인했으므로 읽기 실패를 거부 사유로 삼지 않는다.
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (image != null) {
				if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
					return Result.reject(Rejection.TOO_LARGE_DIMENSION);
				}
			} else if (!"webp".equals(actual)) {
				// jpg/png인데 디코딩이 안 되면 깨진 파일이다
				return Result.reject(Rejection.UNREADABLE);
			}
		} catch (IOException e) {
			return Result.reject(Rejection.UNREADABLE);
		} catch (OutOfMemoryError e) {
			// 해상도 검사 자체가 메모리를 터뜨리는 파일도 거부 대상이다
			return Result.reject(Rejection.TOO_LARGE_DIMENSION);
		}

		// 저장 확장자는 실제 형식을 따른다. photo.png인데 내용이 JPEG면 jpg로 저장한다
		return Result.ok(actual);
	}

	/** data URI 접두어(data:image/jpeg;base64,)에서 MIME만 뽑는다. 없으면 null */
	public static String mimeFromDataUri(String dataUri) {
		if (dataUri == null) {
			return null;
		}
		int comma = dataUri.indexOf(',');
		if (comma < 0 || !dataUri.startsWith("data:")) {
			return null;
		}
		String head = dataUri.substring(5, comma);
		int semi = head.indexOf(';');
		String mime = semi >= 0 ? head.substring(0, semi) : head;
		return mime.trim().isEmpty() ? null : mime.trim();
	}

	/** data URI에서 base64 본문만 뽑는다. 접두어가 없으면 입력을 그대로 본문으로 본다 */
	public static String base64Payload(String dataUri) {
		if (dataUri == null) {
			return null;
		}
		int comma = dataUri.indexOf(',');
		return comma >= 0 ? dataUri.substring(comma + 1) : dataUri;
	}

	public static boolean isAllowedMime(String mime) {
		if (mime == null) {
			return false;
		}
		String value = mime.trim().toLowerCase();
		return "image/jpeg".equals(value) || "image/jpg".equals(value)
				|| "image/png".equals(value) || "image/webp".equals(value);
	}

	/** 파일명에서 소문자 확장자를 뽑는다. 없으면 null */
	public static String extensionOf(String fileName) {
		if (fileName == null) {
			return null;
		}
		int dot = fileName.lastIndexOf('.');
		if (dot < 0 || dot == fileName.length() - 1) {
			return null;
		}
		return fileName.substring(dot + 1).toLowerCase();
	}

	/**
	 * 매직 바이트로 실제 이미지 형식을 판별한다.
	 * 허용 형식이 아니면 null — 확장자가 무엇이든 거부 대상이다.
	 *
	 * @return "jpg" | "png" | "webp" | null
	 */
	public static String detectImageType(byte[] bytes) {
		if (bytes == null || bytes.length < 12) {
			return null;
		}

		// JPEG: FF D8 FF
		if (u(bytes[0]) == 0xFF && u(bytes[1]) == 0xD8 && u(bytes[2]) == 0xFF) {
			return "jpg";
		}

		// PNG: 89 50 4E 47 0D 0A 1A 0A
		if (u(bytes[0]) == 0x89 && u(bytes[1]) == 0x50 && u(bytes[2]) == 0x4E && u(bytes[3]) == 0x47
				&& u(bytes[4]) == 0x0D && u(bytes[5]) == 0x0A && u(bytes[6]) == 0x1A && u(bytes[7]) == 0x0A) {
			return "png";
		}

		// WebP: "RIFF" ....(길이 4바이트).... "WEBP"
		if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
				&& bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
			return "webp";
		}

		return null;
	}

	private static int u(byte b) {
		return b & 0xFF;
	}

	// ===================== 파일 배치 =====================

	/** 검수 대기 보관소에 저장하고 파일명을 돌려준다 */
	public static String saveToPending(byte[] bytes, String fileName) throws IOException {
		Path target = ensureDir(Constants._PENDING_SAVE_PATH).resolve(fileName);
		Files.write(target, bytes);
		return fileName;
	}

	/** 대기·차단 보관소에 있는 파일을 공개 디렉터리로 옮긴다 (= 승인) */
	public static boolean promote(String fileName) throws IOException {
		return move(fileName, Constants._PENDING_SAVE_PATH, Constants._HIDDEN_SAVE_PATH, Constants._FILE_SAVE_PATH);
	}

	/** 공개 디렉터리에 있는 파일을 차단 보관소로 옮긴다 (= 비공개). 삭제하지 않는다 */
	public static boolean quarantine(String fileName) throws IOException {
		return move(fileName, Constants._FILE_SAVE_PATH, Constants._PENDING_SAVE_PATH, Constants._HIDDEN_SAVE_PATH);
	}

	/**
	 * 여러 후보 위치를 순서대로 찾아 목적지로 옮긴다.
	 * 이미 목적지에 있으면 아무것도 하지 않고 true를 돌려준다(중복 처리 대비).
	 */
	private static boolean move(String fileName, String from, String alsoFrom, String to) throws IOException {
		if (fileName == null || fileName.trim().isEmpty()) {
			return false;
		}
		Path destDir = ensureDir(to);
		Path dest = destDir.resolve(fileName);
		if (Files.exists(dest)) {
			return true;
		}

		for (String dir : new String[] { from, alsoFrom }) {
			Path source = Paths.get(dir, fileName);
			if (Files.exists(source)) {
				Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
				return true;
			}
		}
		return false;
	}

	/** URL에서 파일명만 뽑는다. https://witch-hunting.com/img/abc.jpg → abc.jpg */
	public static String fileNameFromUrl(String url) {
		if (url == null || url.trim().isEmpty()) {
			return null;
		}
		String value = url.trim();
		int query = value.indexOf('?');
		if (query >= 0) {
			value = value.substring(0, query);
		}
		int slash = value.lastIndexOf('/');
		String name = slash >= 0 ? value.substring(slash + 1) : value;

		// DB 값이라도 파일 시스템에 그대로 쓰지 않는다.
		// 마지막 슬래시 뒤만 취하므로 상위 경로 이동(../)은 이 시점에 이미 무력화돼 있다.
		// 남은 위험(널 바이트·유니코드 트릭)은 문자 화이트리스트로 막는다.
		if (name.isEmpty() || name.contains("..") || !SAFE_FILE_NAME.matcher(name).matches()) {
			return null;
		}
		return name;
	}

	private static Path ensureDir(String dir) throws IOException {
		Path path = Paths.get(dir);
		File file = path.toFile();
		if (!file.exists()) {
			file.mkdirs();
		}
		return path;
	}
}

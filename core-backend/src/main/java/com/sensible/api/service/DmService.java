package com.sensible.api.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import com.sensible.common.util.AWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.Notification;
import com.sensible.common.dao.DefaultDAO;
import com.sensible.common.util.ImageModerationUtil;

/**
 * 1:1 메신저 (2-29차) — 스타 페이지 소유자끼리만.
 *
 * 레거시 지구로또 채팅의 화면·흐름·폭파 시간은 그대로 가져오되 백엔드는 현 계정 모델(WH_PRESS)로 다시 썼다.
 *  - 본인 확인: 모든 요청의 starId + starToken (앱이 localStorage에 둔 값은 신뢰하지 않는다)
 *  - 자동폭파: EXPIRE_AT 컬럼 + DmExpireScheduler(1분). 발송 5분 후, 읽음 처리 시 미읽음이던 메시지는 1분 후.
 *    파일·썸네일도 같은 시점에 디스크에서 지운다 (클라이언트 확정)
 *  - 첨부: 이미지 10MB(피드와 동일 검사), 영상 30MB·mp4/mov/webm/3gp. base64 JSON 업로드(레거시 방식)
 *  - 파일 접근: 정적 매핑이 아니라 MediaAccessService 단기 토큰으로만 스트리밍 (대화 당사자 응답에만 토큰을 붙인다)
 */
@Service("dmService")
public class DmService {

	private static final Logger logger = LoggerFactory.getLogger(DmService.class);

	/** 발송 후 폭파까지(초). 레거시 MemberService.deletion5Min과 동일 */
	public static final int EXPIRE_AFTER_SEND_SEC = 300;
	/** 읽음 처리 후 폭파까지(초). 레거시 MemberService.deletion1Min과 동일 */
	public static final int EXPIRE_AFTER_READ_SEC = 60;
	/** 텍스트 본문 상한 (컬럼 VARCHAR(2000)) */
	public static final int TEXT_MAX_LENGTH = 2000;
	/** 영상 첨부 상한 30MB (검토 문서 3-4: Cloudflare 본문 100MB·힙·전송량 기준) */
	public static final long VIDEO_MAX_BYTES = 30L * 1024 * 1024;
	public static final List<String> VIDEO_EXTENSIONS =
			Collections.unmodifiableList(Arrays.asList("mp4", "mov", "webm", "3gp"));

	/** 업로드 디렉터리 설정 키와 기본값. globals.properties에 키가 없어도 기본 경로로 동작한다 */
	public static final String UPLOAD_PATH_KEY = "dm.upload.path";
	public static final String UPLOAD_PATH_DEFAULT = "/var/lib/tomcat7/dm/";

	/** 파일 토큰 종류 (MediaAccessService.Grant.targetType) */
	public static final String GRANT_FILE = "DM_FILE";
	public static final String GRANT_THUMB = "DM_THUMB";

	private static final String PUSH_CHANNEL = "dm_channel";

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	@Resource(name = "firebaseService")
	private FirebaseService firebaseService;

	@Resource(name = "mediaAccessService")
	private MediaAccessService mediaAccessService;

	@Resource(name = "config")
	private Properties config;

	// ===== 공개 API =====

	/** 텍스트 발송 */
	public Map<String, Object> sendText(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		String starId = str(params.get("starId"));
		if (!isOwner(starId, params.get("starToken"))) return fail(result, "Please sign in again.");
		Map<String, Object> peer = findPeer(starId, str(params.get("peerId")));
		if (peer == null) return fail(result, "This star is not available.");

		String text;
		try {
			text = normalizeText(params.get("text"));
		} catch (IllegalArgumentException e) {
			return fail(result, e.getMessage());
		}

		Map<String, Object> row = new HashMap<>();
		row.put("starId", starId);
		row.put("peerId", peer.get("id"));
		row.put("contentType", "TEXT");
		row.put("content", text);
		row.put("thumbNm", null);
		row.put("expireSec", EXPIRE_AFTER_SEND_SEC);
		dao.insert("superapp.insertDm", row);

		notifyPeer(starId, str(peer.get("id")));
		result.put("result", "OK");
		result.put("msgId", row.get("msgId"));
		return result;
	}

	/** 사진·영상 발송. params: kind(IMAGE|VIDEO), base64(data URI 또는 순수 base64), mime(선택) */
	public Map<String, Object> sendFile(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		String starId = str(params.get("starId"));
		if (!isOwner(starId, params.get("starToken"))) return fail(result, "Please sign in again.");
		Map<String, Object> peer = findPeer(starId, str(params.get("peerId")));
		if (peer == null) return fail(result, "This star is not available.");

		String kind = str(params.get("kind")).toUpperCase();
		String base64 = str(params.get("base64"));
		String declaredMime = str(params.get("mime"));
		if (declaredMime.isEmpty()) declaredMime = ImageModerationUtil.mimeFromDataUri(base64);

		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(ImageModerationUtil.base64Payload(base64));
		} catch (IllegalArgumentException e) {
			return fail(result, "Invalid file data.");
		}

		String extension;
		if ("IMAGE".equals(kind)) {
			ImageModerationUtil.Result check = ImageModerationUtil.validate(bytes, null, declaredMime);
			if (!check.isValid()) return fail(result, imageRejectionMessage(check.rejection));
			extension = check.extension;
		} else if ("VIDEO".equals(kind)) {
			try {
				extension = validateVideo(bytes.length, declaredMime);
			} catch (IllegalArgumentException e) {
				return fail(result, e.getMessage());
			}
		} else {
			return fail(result, "Unsupported attachment type.");
		}

		String fileName = newFileName(extension);
		String thumbName = null;
		try {
			File dir = uploadDir();
			Files.write(new File(dir, fileName).toPath(), bytes);
			if ("VIDEO".equals(kind)) {
				// 썸네일은 있으면 좋고 없어도 된다 — 코덱이 안 맞아 실패해도 발송은 진행
				thumbName = thumbNameOf(fileName);
				try {
					writeVideoThumbnail(new File(dir, fileName), new File(dir, thumbName));
				} catch (Throwable t) {
					logger.warn("[DM] thumbnail failed for {}: {}", fileName, t.getMessage());
					thumbName = null;
				}
			}
		} catch (IOException e) {
			logger.error("[DM] file write failed", e);
			return fail(result, "Upload failed. Please try again.");
		}

		Map<String, Object> row = new HashMap<>();
		row.put("starId", starId);
		row.put("peerId", peer.get("id"));
		row.put("contentType", kind);
		row.put("content", fileName);
		row.put("thumbNm", thumbName);
		row.put("expireSec", EXPIRE_AFTER_SEND_SEC);
		dao.insert("superapp.insertDm", row);

		notifyPeer(starId, str(peer.get("id")));
		result.put("result", "OK");
		result.put("msgId", row.get("msgId"));
		return result;
	}

	/** 대화 목록 (살아 있는 메시지가 있는 상대만) + 내 프로필 사진 */
	public Map<String, Object> getRooms(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		String starId = str(params.get("starId"));
		if (!isOwner(starId, params.get("starToken"))) return fail(result, "Please sign in again.");

		Map<String, Object> param = new HashMap<>();
		param.put("starId", starId);
		List<Map<String, Object>> rooms = dao.selectList("superapp.selectDmRooms", param);
		result.put("result", "OK");
		result.put("rooms", rooms == null ? new ArrayList<Map<String, Object>>() : rooms);
		return result;
	}

	/** 대화 내용. 첨부에는 10분짜리 파일 토큰을 붙인다 */
	public Map<String, Object> getMessages(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		String starId = str(params.get("starId"));
		if (!isOwner(starId, params.get("starToken"))) return fail(result, "Please sign in again.");
		String peerId = str(params.get("peerId"));
		Map<String, Object> peer = dao.selectOne("superapp.selectDmPeer", peerId);
		if (peer == null) return fail(result, "This star is not available.");

		Map<String, Object> param = new HashMap<>();
		param.put("starId", starId);
		param.put("peerId", peerId);
		List<Map<String, Object>> rows = dao.selectList("superapp.selectDmMessages", param);
		if (rows == null) rows = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			String type = str(row.get("contentType"));
			if ("IMAGE".equals(type) || "VIDEO".equals(type)) {
				String msgId = String.valueOf(row.get("msgId"));
				row.put("fileUrl", "/api/super/dm/file?t=" + mediaAccessService.issueToken(GRANT_FILE, msgId));
				if (row.get("thumbNm") != null) {
					row.put("thumbUrl", "/api/super/dm/file?t=" + mediaAccessService.issueToken(GRANT_THUMB, msgId));
				}
			}
			// 파일명은 클라이언트가 알 필요 없다
			row.remove("fileNm");
			row.remove("thumbNm");
		}
		result.put("result", "OK");
		result.put("peer", peer);
		result.put("messages", rows);
		result.put("expireAfterSendSec", EXPIRE_AFTER_SEND_SEC);
		result.put("expireAfterReadSec", EXPIRE_AFTER_READ_SEC);
		return result;
	}

	/** 읽음 처리: 상대가 보낸 미읽음을 읽음으로 바꾸고 폭파를 1분 뒤로 당긴다 */
	public Map<String, Object> markRead(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		String starId = str(params.get("starId"));
		if (!isOwner(starId, params.get("starToken"))) return fail(result, "Please sign in again.");

		Map<String, Object> param = new HashMap<>();
		param.put("starId", starId);
		param.put("peerId", str(params.get("peerId")));
		param.put("expireSec", EXPIRE_AFTER_READ_SEC);
		int updated = dao.update("superapp.updateDmRead", param);
		result.put("result", "OK");
		result.put("updated", updated);
		return result;
	}

	/**
	 * 앱 안 점 표시용 미읽음 수 + 내 프로필 사진(로비 헤더 아바타).
	 * 프로필 사진은 스타 페이지에 공개된 정보라 토큰 없이도 돌려준다(매직 로그인은 starToken이 없다).
	 * 미읽음 수는 토큰이 맞을 때만 세고, DM 테이블 조회가 실패해도 사진은 나가야 하므로 따로 감싼다.
	 */
	public Map<String, Object> getUnreadCount(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		String starId = str(params.get("starId"));
		if (starId.isEmpty()) return fail(result, "Please sign in again.");

		Map<String, Object> me = null;
		try {
			me = dao.selectOne("superapp.selectDmPeer", starId);
		} catch (Exception e) {
			logger.warn("[DM] profile lookup failed: {}", e.getMessage());
		}

		boolean signedIn = isOwner(starId, params.get("starToken"));
		int unread = 0;
		if (signedIn) {
			try {
				Integer count = dao.selectOne("superapp.selectDmUnreadCount", starId);
				unread = count == null ? 0 : count;
			} catch (Exception e) {
				// DDL 미적용 등. 사진은 정상 응답하고 미읽음만 0으로 둔다
				logger.warn("[DM] unread count failed: {}", e.getMessage());
			}
		}
		result.put("result", "OK");
		result.put("signedIn", signedIn);
		result.put("unread", unread);
		result.put("myImage", me == null ? null : me.get("image"));
		return result;
	}

	/** 파일 토큰 → 실제 파일. 만료됐거나 없으면 null */
	public File resolveFile(String grantType, String targetId) {
		long msgId;
		try {
			msgId = Long.parseLong(targetId);
		} catch (NumberFormatException e) {
			return null;
		}
		Map<String, Object> row = dao.selectOne("superapp.selectDmFile", msgId);
		if (row == null) return null;
		String name = GRANT_THUMB.equals(grantType) ? str(row.get("thumbNm")) : str(row.get("fileNm"));
		if (name.isEmpty() || !isSafeFileName(name)) return null;
		File file = new File(uploadDirPath(), name);
		return file.isFile() ? file : null;
	}

	/** 스케줄러: 만료 행의 파일을 지운 뒤 행을 지운다. 삭제한 행 수 반환 */
	public int purgeExpired() {
		List<Map<String, Object>> expired = dao.selectList("superapp.selectDmExpired");
		if (expired == null || expired.isEmpty()) return 0;

		List<Object> ids = new ArrayList<>();
		String dir = uploadDirPath();
		for (Map<String, Object> row : expired) {
			ids.add(row.get("msgId"));
			String type = str(row.get("contentType"));
			if ("IMAGE".equals(type) || "VIDEO".equals(type)) {
				deleteQuietly(dir, str(row.get("content")));
				deleteQuietly(dir, str(row.get("thumbNm")));
			}
		}
		return dao.delete("superapp.deleteDmByIds", ids);
	}

	// ===== 순수 로직 (단위 테스트 대상) =====

	/** 텍스트 본문 정규화. 비었거나 상한 초과면 IllegalArgumentException (메시지는 앱 노출용 영어) */
	public static String normalizeText(Object textObj) {
		String text = textObj == null ? "" : String.valueOf(textObj).trim();
		if (text.isEmpty()) throw new IllegalArgumentException("Please enter a message.");
		if (text.length() > TEXT_MAX_LENGTH) {
			throw new IllegalArgumentException("Message is too long (max " + TEXT_MAX_LENGTH + " characters).");
		}
		return text;
	}

	/** 영상 검사: 크기 상한·MIME 화이트리스트. 통과하면 저장 확장자 반환 */
	public static String validateVideo(long bytes, String declaredMime) {
		if (bytes <= 0) throw new IllegalArgumentException("Invalid file data.");
		if (bytes > VIDEO_MAX_BYTES) {
			throw new IllegalArgumentException("Video is too large. Please choose a file under 30MB.");
		}
		String ext = videoExtensionOf(declaredMime);
		if (ext == null) throw new IllegalArgumentException("Only MP4, MOV and WebM videos are allowed.");
		return ext;
	}

	/** video/mp4 → mp4, video/quicktime → mov, video/webm → webm, video/3gpp → 3gp. 그 외 null */
	public static String videoExtensionOf(String mime) {
		if (mime == null) return null;
		String m = mime.trim().toLowerCase();
		int semi = m.indexOf(';');
		if (semi >= 0) m = m.substring(0, semi).trim();
		if ("video/mp4".equals(m)) return "mp4";
		if ("video/quicktime".equals(m) || "video/mov".equals(m)) return "mov";
		if ("video/webm".equals(m)) return "webm";
		if ("video/3gpp".equals(m) || "video/3gp".equals(m)) return "3gp";
		return null;
	}

	/** 저장 파일명: UUID + 확장자 (클라이언트 파일명은 쓰지 않는다 — 레거시의 경로 조작 여지 제거) */
	public static String newFileName(String extension) {
		return UUID.randomUUID().toString().replace("-", "") + "." + extension;
	}

	public static String thumbNameOf(String fileName) {
		int dot = fileName.lastIndexOf('.');
		return (dot > 0 ? fileName.substring(0, dot) : fileName) + "_thumb.jpg";
	}

	/** 디스크에서 열어도 되는 이름인지 — 스케줄러·스트리밍 둘 다 이 검사를 거친다 */
	public static boolean isSafeFileName(String name) {
		return name != null && name.matches("^[A-Za-z0-9_.-]{1,120}$") && !name.contains("..");
	}

	public static boolean isFileGrant(String grantType) {
		return GRANT_FILE.equals(grantType) || GRANT_THUMB.equals(grantType);
	}

	public static String contentTypeOf(String fileName) {
		String ext = ImageModerationUtil.extensionOf(fileName);
		if (ext == null) return "application/octet-stream";
		switch (ext) {
			case "jpg": case "jpeg": return "image/jpeg";
			case "png": return "image/png";
			case "webp": return "image/webp";
			case "mp4": return "video/mp4";
			case "mov": return "video/quicktime";
			case "webm": return "video/webm";
			case "3gp": return "video/3gpp";
			default: return "application/octet-stream";
		}
	}

	/** 이미지 거부 사유 → 앱 노출 문구 (피드 업로드와 같은 정책: 종류 판별 실패는 한 문장으로 묶는다) */
	public static String imageRejectionMessage(ImageModerationUtil.Rejection rejection) {
		if (rejection == null) return "Upload failed.";
		switch (rejection) {
			case TOO_LARGE: return "Image is too large. Please upload a file under 10MB.";
			case TOO_LARGE_DIMENSION: return "Image resolution is too large. Please upload a smaller image.";
			case EMPTY: return "Invalid file data.";
			default: return "Only JPG, PNG and WebP images are allowed.";
		}
	}

	// ===== 내부 =====

	private boolean isOwner(String starId, Object starToken) {
		if (starId == null || starId.trim().isEmpty()
				|| !(starToken instanceof String) || ((String) starToken).trim().isEmpty()) {
			return false;
		}
		try {
			Map<String, Object> param = new HashMap<>();
			param.put("starId", starId);
			param.put("starToken", starToken);
			Integer valid = dao.selectOne("superapp.checkStarToken", param);
			return valid != null && valid > 0;
		} catch (Exception e) {
			// 확인 실패는 소유자 아님으로 본다 (fail-closed)
			logger.warn("[DM] owner check failed: {}", e.getMessage());
			return false;
		}
	}

	/** 상대 스타 확인. 자기 자신에게는 보낼 수 없다 */
	private Map<String, Object> findPeer(String starId, String peerId) {
		if (peerId == null || peerId.isEmpty() || peerId.equals(starId)) return null;
		return dao.selectOne("superapp.selectDmPeer", peerId);
	}

	/** 수신자 푸시. 본문은 도착 사실만 (클라이언트 확정). 실패해도 발송은 성공으로 본다 */
	private void notifyPeer(String senderId, String receiverId) {
		try {
			Map<String, Object> target = dao.selectOne("superapp.selectPushTargetInfo", receiverId);
			if (target == null || !"Y".equals(target.get("pushYn"))) return;
			String fcmToken = str(target.get("fcmToken"));
			if (fcmToken.isEmpty()) return;

			Notification notification = Notification.builder()
					.setTitle("New message")
					.setBody("You have a new message.")
					.build();
			Map<String, String> data = new HashMap<>();
			data.put("type", "DM");
			data.put("peerId", senderId);
			firebaseService.sendDataNotification(fcmToken, notification, data, PUSH_CHANNEL);
		} catch (Exception e) {
			logger.warn("[DM] push failed: {}", e.getMessage());
		}
	}

	private String uploadDirPath() {
		String path = UPLOAD_PATH_DEFAULT;
		try {
			if (config != null) {
				String v = config.getProperty(UPLOAD_PATH_KEY);
				if (v != null && !v.trim().isEmpty()) path = v.trim();
			}
		} catch (Throwable t) {
			// 설정을 못 읽으면 기본 경로
		}
		return path.endsWith("/") ? path : path + "/";
	}

	private File uploadDir() throws IOException {
		File dir = new File(uploadDirPath());
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("cannot create upload dir: " + dir);
		}
		return dir;
	}

	private void deleteQuietly(String dir, String name) {
		if (name == null || name.isEmpty() || !isSafeFileName(name)) return;
		try {
			Files.deleteIfExists(new File(dir, name).toPath());
		} catch (Exception e) {
			logger.warn("[DM] file delete failed {}: {}", name, e.getMessage());
		}
	}

	/**
	 * 영상 첫 프레임을 JPEG로 저장 (JCodec, 레거시 MemberService와 같은 방식).
	 * 채팅 풍선 미리보기와 전체 보기의 포스터로 쓰인다 — 클라이언트 요청으로 1초 지점이 아니라 첫 프레임.
	 * 첫 프레임을 못 뽑는 파일은 1초 지점으로 한 번 더 시도한다.
	 */
	private void writeVideoThumbnail(File video, File thumb) throws Exception {
		Picture picture = null;
		try {
			FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(video));
			grab.seekToFramePrecise(0);
			picture = grab.getNativeFrame();
		} catch (Throwable t) {
			logger.debug("[DM] first-frame grab failed, retry at 1s: {}", t.getMessage());
		}
		if (picture == null) {
			FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(video));
			grab.seekToSecondPrecise(1);
			picture = grab.getNativeFrame();
		}
		if (picture == null) throw new IOException("no frame");
		BufferedImage image = AWTUtil.toBufferedImage(picture);
		if (!ImageIO.write(image, "jpg", thumb)) throw new IOException("thumbnail write failed");
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static Map<String, Object> fail(Map<String, Object> result, String msg) {
		result.put("result", "FAIL");
		result.put("msg", msg);
		return result;
	}
}

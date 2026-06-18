package com.sensible.api.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.firebase.messaging.Notification;
import com.sensible.common.Constants;
import com.sensible.common.dao.DefaultDAO;

@Service("superAppService")
public class SuperAppService {

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	@Autowired(required = false)
	private JavaMailSender mailSender;

	@Autowired
	private FirebaseService firebaseService;

	public Map<String, Object> getLobbyData(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		String country = (String) map.get("country");
		if (country == null || country.trim().isEmpty()) {
			map.put("country", "KR");
		}

		// 인기 스타 (가로)
		List<Map<String, Object>> popularStars = dao.selectList("superapp.selectPopularStars", map);
		// 전체 스타 (세로)
		List<Map<String, Object>> allStars = dao.selectList("superapp.selectAllStars", map);
		
		List<Map<String, Object>> newCreators = dao.selectList("superapp.selectNewCreators", map);

		resultMap.put("popularStars", popularStars);
		resultMap.put("allStars", allStars);
		resultMap.put("newCreators", newCreators);
		resultMap.put("result", "OK");
		return resultMap;
	}

	public Map<String, Object> getStarDetail(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		// 스타 기본 정보
		Map<String, Object> starInfo = dao.selectOne("superapp.selectStarDetail", map);

		if (starInfo != null) {
			// 스타의 갤러리 이미지들 (WH_CONTENT 재사용)
			List<Map<String, Object>> photos = dao.selectList("superapp.selectStarGallery", map);
			starInfo.put("photos", photos);

			resultMap.put("starInfo", starInfo);
			resultMap.put("result", "OK");
		} else {
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	public Map<String, Object> insertAdLog(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			// 1. 기존 장부(WH_AD_LOG)에 시청 기록 저장
			dao.insert("superapp.insertAdLog", map);

			// 2. 🌟 [신규] IMPRESSION(조회) 발생 시 푸시 알림 발송 로직
			String action = (String) map.get("ACTION");
			String starId = (String) map.get("PRS_ID");

			// 프론트에서 60초 쿨다운을 거치고 넘어온 IMPRESSION일 때만 발송
			if ("IMPRESSION".equals(action) && starId != null) {

				// 이전에 추가했던 '스타의 푸시 설정 및 토큰 조회' 쿼리 호출
				Map<String, Object> targetInfo = dao.selectOne("superapp.selectPushTargetInfo", starId);
				System.out.println("푸시 로깅 시작");
				// PUSH_YN이 'Y'인지 확인
				if (targetInfo != null && "Y".equals(targetInfo.get("pushYn"))) {
					String fcmToken = (String) targetInfo.get("fcmToken");
					System.out.println("fcmToken: " + fcmToken);

					// 토큰이 존재하면 푸시 발송
					if (fcmToken != null && !fcmToken.trim().isEmpty()) {
						System.out.println("sending notifications");

						// Firebase 팝업에 띄울 메시지 구성
						Notification notification = Notification.builder()
								.setTitle("🎉 New visitor alert!")
								.setBody("Someone just visited your Star Page.")
								.build();

						// 발송 중 에러가 나도 로깅만 하고, 메인 로직(방문자 수 증가)을 방해하지 않도록 try-catch
						try {
							firebaseService.sendPersonalNotification(fcmToken, notification);
						} catch (Exception e) {
							System.out.println("FCM 푸시 발송 실패: " + e.getMessage());
						}
					}
				}
			}

			resultMap.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
		}

		return resultMap;
	}

	public Map<String, Object> getFeedDetail(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		// 프론트엔드가 넘긴 ID (예: "5" 또는 "A1")
		String conIdStr = String.valueOf(map.get("conId"));

		// 🌟 1. ID가 'A'로 시작하면 관리자 공지 피드로 처리!
		if (conIdStr.startsWith("A")) {
			// 'A'를 떼어내고 진짜 숫자(DB ID)만 꺼냄
			map.put("realConId", conIdStr.substring(1));

			Map<String, Object> content = dao.selectOne("superapp.selectAdminContentDetail", map);

			if (content != null) {
				// 관리자는 미디어(사진) 테이블이 따로 없으므로, 프론트엔드 규격에 맞춰 수동으로 medias 배열을 만들어 줍니다.
				List<Map<String, Object>> medias = new ArrayList<>();
				if (content.get("IMAGE_URL") != null) {
					Map<String, Object> media = new HashMap<>();
					media.put("MEDIA_TYPE", "PHOTO");
					media.put("MEDIA_URL", content.get("IMAGE_URL"));
					medias.add(media);
				}

				resultMap.put("content", content);
				resultMap.put("medias", medias);
				resultMap.put("result", "OK");
			} else {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "This notice has been deleted or does not exist.");
			}
			return resultMap;
		}

		// 🌟 2. 기존 로직: A로 시작하지 않으면 일반 스타 피드로 처리
		Map<String, Object> content = dao.selectOne("superapp.selectContentDetail", map);

		if (content != null) {
			List<Map<String, Object>> medias = dao.selectList("superapp.selectContentMedias", map);
			resultMap.put("content", content);
			resultMap.put("medias", medias);
			resultMap.put("result", "OK");
		} else {
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "This post has been deleted or does not exist.");
		}
		return resultMap;
	}

	public Map<String, Object> toggleFollow(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			boolean isAdd = Boolean.parseBoolean(String.valueOf(map.get("isAdd")));
			map.put("amount", isAdd ? 1 : -1);
			map.put("actionType", isAdd ? "FOLLOW" : "UNFOLLOW");

			// 1. 기존 카운트 증감
			dao.update("superapp.updateFollowerCount", map);

			// 2. 🌟 신규 이력(Log) 저장 (deviceId 파라미터가 프론트에서 넘어와야 함)
			if (map.get("deviceId") != null) {
				dao.insert("superapp.insertFollowerLog", map);
			}

			resultMap.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	// ==========================================
	// [신규] 비로그인 (Guest) 로직 구현
	// ==========================================

	// 댓글 목록 조회
	public Map<String, Object> getCommentList(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		List<Map<String, Object>> comments = dao.selectList("superapp.selectCommentList", map);
		resultMap.put("comments", comments);
		resultMap.put("result", "OK");
		return resultMap;
	}

	// 댓글 등록
	public Map<String, Object> addComment(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			String deviceId = (String) map.get("deviceId");

			// 🛡️ 1차 방어: 글로벌 차단(앱 전체 금지) 여부 확인
			int isGloballyBlocked = (int) dao.selectOne("superapp.checkGlobalBlock", deviceId);
			if (isGloballyBlocked > 0) {
				resultMap.put("result", "FAIL");
				resultMap.put("msg",
						"This device is permanently banned from commenting due to a severe violation of our terms of service.");
				return resultMap; // 즉시 튕겨냄
			}

			// 🛡️ 2차 방어: 로컬 차단(해당 스타 블랙리스트) 여부 확인
			// (프론트에서 넘어온 prsId와 deviceId를 함께 검사)
			int isLocallyBlocked = (int) dao.selectOne("superapp.checkBlacklist", map);
			if (isLocallyBlocked > 0) {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Commenting has been restricted by the creator.");
				return resultMap; // 즉시 튕겨냄
			}

			dao.insert("superapp.insertComment", map);
			map.put("amount", 1); // 댓글 수 1 증가
			dao.update("superapp.updateCommentCount", map); // 본체 테이블(STAR/FEED) 업데이트
			resultMap.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "A database error occurred.");
		}
		return resultMap;
	}

	public Map<String, Object> deleteStarFeed(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			String starId = (String) params.get("starId");
			String starToken = (String) params.get("starToken");

			// 1. 토큰 유효성 검증 (해킹 방지)
			Map<String, Object> tokenParam = new HashMap<>();
			tokenParam.put("starId", starId);
			tokenParam.put("starToken", starToken);
			int validTokenCount = dao.selectOne("superapp.checkStarToken", tokenParam);

			if (validTokenCount == 0) {
				result.put("result", "FAIL");
				result.put("msg", "Your session has expired. Please log in again.");
				return result;
			}

			// 2. 삭제 실행 (기존 웹 관리자용 쿼리 재활용)
			String conId = String.valueOf(params.get("conId"));
			dao.delete("super.deleteFeedMaster", conId);

			result.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "Failed to delete the post.");
		}
		return result;
	}

	// 댓글 삭제 (비밀번호 검증)
	public Map<String, Object> deleteComment(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		boolean isAdminCheck = map.containsKey("isAdminCheck") && (boolean) map.get("isAdminCheck");
		boolean isStarCheck = map.containsKey("isStarCheck") && (boolean) map.get("isStarCheck");

		// 🌟 1. 페이지 주인(스타) 프리패스 체크
		if (isStarCheck) {
			String starId = (String) map.get("starId");
			String starToken = (String) map.get("starToken");

			// 토큰 검증
			Map<String, Object> tokenParam = new HashMap<>();
			tokenParam.put("starId", starId);
			tokenParam.put("starToken", starToken);
			int validTokenCount = dao.selectOne("superapp.checkStarToken", tokenParam);

			if (validTokenCount > 0) {
				dao.update("superapp.deleteComment", map); // 상태를 DELETED로 변경
				map.put("amount", -1);
				dao.update("superapp.updateCommentCount", map); // 카운트 깎기
				resultMap.put("result", "OK");
				return resultMap;
			} else {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Session expired.");
				return resultMap;
			}
		}

		// 🌟 2. 기존 관리자 프리패스 로직 유지
		if (isAdminCheck) {
			dao.update("superapp.deleteComment", map);
			map.put("amount", -1);
			dao.update("superapp.updateCommentCount", map);
			resultMap.put("result", "OK");
			return resultMap;
		}

		// 🌟 3. 일반 유저 기존 로직 (비밀번호 확인) 유지
		String dbPassword = (String) dao.selectOne("superapp.checkCommentPassword", map);
		String inputPassword = (String) map.get("password");

		if (dbPassword != null && dbPassword.equals(inputPassword)) {
			dao.update("superapp.deleteComment", map);
			map.put("amount", -1);
			dao.update("superapp.updateCommentCount", map);
			resultMap.put("result", "OK");
		} else {
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Incorrect password.");
		}
		return resultMap;
	}

	// 좋아요 토글 (등록/취소)
	public Map<String, Object> toggleGuestLike(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			int likeCount = (int) dao.selectOne("superapp.checkGuestLike", map);

			if (likeCount > 0) {
				// 좋아요 취소
				dao.delete("superapp.deleteGuestLike", map);
				map.put("amount", -1);
				map.put("actionType", "UNLIKE");
				resultMap.put("isLiked", false);
			} else {
				// 좋아요 등록
				dao.insert("superapp.insertGuestLike", map);
				map.put("amount", 1);
				map.put("actionType", "LIKE");
				resultMap.put("isLiked", true);
			}

			// 1. 기존 본체 테이블 업데이트
			dao.update("superapp.updateLikeCount", map);

			// 2. 🌟 신규 이력(Log) 저장
			// 대상이 스타 페이지('STAR')일 때만 로그 기록 (피드 좋아요는 제외)
			if ("STAR".equals(map.get("targetType"))) {
				dao.insert("superapp.insertLikeLog", map);
			}

			resultMap.put("result", "OK");

		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	// 공유 횟수 증가
	public Map<String, Object> addShareCount(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			dao.update("superapp.updateShareCount", map);
			resultMap.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	// 댓글 신고 처리
	public Map<String, Object> reportComment(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			dao.insert("superapp.insertGuestReport", map);
			resultMap.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Failed to submit the report. Please try again.");
		}
		return resultMap;
	}

	/**
	 * [신규] 실시간 조회수 폴링 데이터 조회 및 시간 갱신 로직
	 */
	public Map<String, Object> pollAdViews(Map<String, Object> params) throws Exception {
		Map<String, Object> result = new HashMap<>();

		try {
			String lastCheckTime = (String) params.get("lastCheckTime");

			// 1. 최초 로드 시점: 지난번 시간이 없으면 현재 DB 시간만 찍어서 돌려보냄
			if (lastCheckTime == null || lastCheckTime.trim().isEmpty()) {
				String currentTime = dao.selectOne("superapp.selectDbCurrentTime");
				result.put("currentTime", currentTime);
				result.put("data", new ArrayList<>()); // 데이터는 빈 배열
				result.put("result", "OK");
				return result;
			}

			// 2. 지난번 확인 시간(lastCheckTime) 이후에 증가한 스타별 조회수(NEW_VIEWS) 가져오기
			List<Map<String, Object>> newData = dao.selectList("superapp.selectPolledAdViews", params);

			// 3. 다음번 폴링의 기준이 될 최신 DB 시간 갱신
			String currentTime = dao.selectOne("superapp.selectDbCurrentTime");

			result.put("data", newData);
			result.put("currentTime", currentTime);
			result.put("result", "OK");

		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "Failed to sync real-time data.");
		}

		return result;
	}

	/**
	 * 🌟 [신규] 특정 스타(내 페이지) 전용 조회수 폴링 데이터 조회
	 */
	public Map<String, Object> pollMyVisitors(Map<String, Object> params) throws Exception {
		Map<String, Object> result = new HashMap<>();

		try {
			String lastCheckTime = (String) params.get("lastCheckTime");
			String starId = (String) params.get("starId");

			// 1. 필수 파라미터 누락 방어
			if (starId == null || starId.trim().isEmpty()) {
				result.put("result", "FAIL");
				result.put("msg", "Invalid Star ID.");
				return result;
			}

			// 2. 최초 로드 시점 처리
			if (lastCheckTime == null || lastCheckTime.trim().isEmpty()) {
				String currentTime = dao.selectOne("superapp.selectDbCurrentTime");
				result.put("currentTime", currentTime);
				result.put("newViews", 0); // 처음엔 증가량이 없음
				result.put("result", "OK");
				return result;
			}

			// 3. 지정된 시간 이후, 특정 스타(starId)에게 발생한 조회수(AD_TYPE) 합계 조회
			// (반환값이 null일 수 있으므로 Integer로 캐스팅 후 방어)
			Integer newViews = dao.selectOne("superapp.selectMyNewViewsCount", params);

			// 4. 기준 시간(currentTime) 갱신
			String currentTime = dao.selectOne("superapp.selectDbCurrentTime");

			result.put("newViews", newViews != null ? newViews : 0);
			result.put("currentTime", currentTime);
			result.put("result", "OK");

		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "Failed to sync real-time visitor data.");
		}

		return result;
	}

	// ==========================================
	// [신규] 온보딩 (Page First & Magic Link) 로직
	// ==========================================

	/**
	 * 1. 가입 없이 빈 페이지 즉시 생성
	 */
	public Map<String, Object> createAutoPage(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			String pageId = "SP-" + System.currentTimeMillis();
			map.put("pageId", pageId);
			map.put("name", map.getOrDefault("name", "New Creator"));
			map.put("country", map.getOrDefault("country", "KR")); // 글로벌 서비스 시 클라이언트에서 넘겨준 국가코드 사용 권장

			dao.insert("superapp.insertAutoPage", map);

			resultMap.put("result", "OK");
			resultMap.put("pageId", pageId);
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Failed to create page.");
		}
		return resultMap;
	}

	/**
	 * 4. 페이지 소유권 청구 (매직 링크 발송 및 토큰 저장)
	 */
	public Map<String, Object> requestClaimLink(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			// 1. 고유 토큰 생성 및 DB 저장
			String token = UUID.randomUUID().toString().replace("-", "");
			map.put("token", token);

			dao.insert("superapp.insertClaimToken", map);

			// 2. 매직 링크 URL 조립 (루트 도메인 사용)
			String magicLink = "https://witch-hunting.com/api/super/page/claim/verify?token=" + token;
			// String magicLink =
			// "http://localhost:8080/witch/api/super/page/claim/verify?token=" + token;

			// 3. 매직 링크 알림 메일 발송
			if (mailSender != null) {
				SimpleMailMessage message = new SimpleMailMessage();
				String targetEmail = String.valueOf(map.get("email"));

				message.setTo(targetEmail);
				message.setSubject("👑 [Star Platform] Claim Your Page - Admin Access Link");

				String content = String.format("Welcome to Star Platform!\n\n"
						+ "Click the link below to instantly gain admin access to this page.\n"
						+ "For your security, this link is only valid for 15 minutes.\n\n"
						+ "■ Admin Access Link (Magic Link) :\n%s\n\n"
						+ "* If you did not request this, please ignore this email.", magicLink);

				message.setText(content);
				mailSender.send(message);

				System.out.println("매직 링크 발송 완료: " + targetEmail);
			} else {
				System.out.println("알림: JavaMailSender가 설정되지 않아 메일 발송 생략 (DB 저장은 완료)");
			}

			// 4. 프론트엔드로 성공 응답
			resultMap.put("result", "OK");
			resultMap.put("msg", "Magic link has been sent to your email.");

		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Failed to send claim link.");
		}
		return resultMap;
	}

	/**
	 * 3. 매직 링크 검증 및 소유권 이전 (자동 로그인 처리)
	 */
	public Map<String, Object> verifyClaimToken(String token) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			// 1. 토큰 조회
			Map<String, Object> tokenInfo = dao.selectOne("superapp.selectClaimToken", token);
			if (tokenInfo == null || "1".equals(String.valueOf(tokenInfo.get("USED")))) {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Invalid or already used link.");
				return resultMap;
			}

			// DB에 저장해둔 대상 꺼내기 (기존 페이지 ID이거나, 신규 이름이 들어있음)
			String targetIdOrName = (String) tokenInfo.get("PAGE_ID");
			String email = (String) tokenInfo.get("EMAIL");
			String password = (String) tokenInfo.get("TEMP_PWD");

			String country = (String) tokenInfo.get("COUNTRY");
			if (country == null || country.trim().isEmpty() || country.equals("GLOBAL")) {
				country = "KR";
			}

			String finalStarId = "";

			// 🌟 2. 핵심 로직: targetIdOrName이 "star_"로 시작하는지 확인하여 분기
			if (targetIdOrName != null && targetIdOrName.startsWith("star_")) {
				// 👉 [케이스 A] 기존 페이지 소유권 획득 (UPDATE)
				finalStarId = targetIdOrName;

				Map<String, Object> updateParam = new HashMap<>();
				updateParam.put("pageId", finalStarId);
				updateParam.put("email", email);
				updateParam.put("password", password);

				dao.update("superapp.updatePageOwner", updateParam); // 예전에 만들어둔 기존 쿼리 사용

			} else {
				Map<String, Object> updateParam = new HashMap<>();
				updateParam.put("reqName", targetIdOrName);
				updateParam.put("country", country);
				updateParam.put("email", email);
				updateParam.put("password", password);

				// INSERT 쿼리 대신 UPDATE 쿼리 실행
				int affectedRows = dao.update("superapp.claimEmailPage", updateParam);

				if (affectedRows == 0) {
					// 이메일 인증하는 3분 사이에 다른 사람이 채간 경우
					resultMap.put("result", "FAIL");
					resultMap.put("msg", "Page was taken while verifying email.");
					return resultMap;
				}
			}

			// 🌟 4. Auto-Login Logic: Generate token for Magic Link user
			String starToken = UUID.randomUUID().toString();
			Map<String, Object> tokenParam = new HashMap<>();
			tokenParam.put("starId", finalStarId);
			tokenParam.put("starToken", starToken);
			dao.update("superapp.updateStarToken", tokenParam);

			// 5. Return data (including new token)
			resultMap.put("result", "OK");
			resultMap.put("starId", finalStarId);
			resultMap.put("email", email);
			resultMap.put("starToken", starToken);

		} catch (Exception e) {
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	/**
	 * 🌟 [신규] 무한 트래픽 순환용 추천 페이지 (Next Page) 조회
	 */
	public Map<String, Object> getDiscoverPages(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			List<Map<String, Object>> list = dao.selectList("superapp.selectDiscoverPages", map);
			resultMap.put("result", "OK");
			resultMap.put("list", list);
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Failed to load recommended pages.");
		}
		return resultMap;
	}

	// SuperAppService.java 내부에 추가

	/**
	 * 🌟 [신규] 1:1 메시지 발송 (유저/관리자 공용)
	 */
	public Map<String, Object> sendMessage(Map<String, Object> params) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			dao.insert("superapp.insertMessage", params);
			resultMap.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	/**
	 * 🌟 [신규] 메시지 목록 조회
	 */
	public Map<String, Object> getMessageList(Map<String, Object> params) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			// 특정 페이지의 대화 내역을 시간순으로 가져옴
			List<Map<String, Object>> messages = dao.selectList("superapp.selectMessageList", params);

			// 읽음 처리 (유저가 확인했으므로 관리자가 보낸 메시지들을 읽음으로 업데이트)
			if ("USER".equals(params.get("readerType"))) {
				dao.update("superapp.updateMessageReadStatus", params);
			}

			resultMap.put("list", messages);
			resultMap.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	public Map<String, Object> checkClaimStatus(String token) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		// 1. 토큰 정보 조회 (기존 selectClaimToken 쿼리 사용)
		Map<String, Object> tokenInfo = dao.selectOne("superapp.selectClaimToken", token);

		// 2. 이미 사용된 토큰(USED='1')이고, 만료되지 않았다면 '인증 성공'으로 간주
		// (이전에 verifyClaimToken API에서 성공 시 USED를 1로 바꾼다고 가정)
		if (tokenInfo != null && "1".equals(String.valueOf(tokenInfo.get("USED")))) {
			resultMap.put("result", "OK");
			resultMap.put("starId", tokenInfo.get("PAGE_ID"));
			resultMap.put("email", tokenInfo.get("EMAIL"));
		} else {
			resultMap.put("result", "WAIT"); // 아직 클릭 안 했거나 실패
		}

		return resultMap;
	}

	public Map<String, Object> getLeaderboard() throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		// 수익(revenue) 기준 내림차순 상위 100명 조회
		List<Map<String, Object>> list = dao.selectList("superapp.selectLeaderboard");

		resultMap.put("result", "OK");
		resultMap.put("list", list);

		return resultMap;
	}

	public Map<String, Object> getRevenueLeaderboard() throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		// 수익(revenue) 기준 내림차순 상위 100명 조회
		List<Map<String, Object>> list = dao.selectList("superapp.selectRevenueLeaderboard");

		resultMap.put("result", "OK");
		resultMap.put("list", list);

		return resultMap;
	}

	// ==========================================
	// [신규] 명예의 전당 (Hall of Fame) 서비스
	// ==========================================

	// 1. 역대 오늘의 왕 (일자별 1위)
	public Map<String, Object> getDailyKings() throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		List<Map<String, Object>> list = dao.selectList("superapp.selectDailyKings");
		resultMap.put("result", "OK");
		resultMap.put("list", list);
		return resultMap;
	}

	// 2. 역대 TOP 100 (현재 통합 랭킹 쿼리 재사용)
	public Map<String, Object> getHallOfFameTop100() throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		// 기존 selectLeaderboard 쿼리를 그대로 호출하여 반환
		List<Map<String, Object>> list = dao.selectList("superapp.selectLeaderboard");
		resultMap.put("result", "OK");
		resultMap.put("list", list);
		return resultMap;
	}

	// 3. 월간 챔피언
	public Map<String, Object> getMonthlyChampions(Map<String, Object> params) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		// params 안에는 targetMonth (예: "2026-06") 값이 들어있어야 함
		List<Map<String, Object>> list = dao.selectList("superapp.selectMonthlyChampions", params);
		resultMap.put("result", "OK");
		resultMap.put("list", list);
		return resultMap;
	}

	// 4. 연간 챔피언
	public Map<String, Object> getYearlyChampions(Map<String, Object> params) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		// params 안에는 targetYear (예: "2026") 값이 들어있어야 함
		List<Map<String, Object>> list = dao.selectList("superapp.selectYearlyChampions", params);
		resultMap.put("result", "OK");
		resultMap.put("list", list);
		return resultMap;
	}

	public Map<String, Object> getDailyLeaderboard() throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		// 오늘 발생한 조회수 기반 상위 100명 조회
		List<Map<String, Object>> list = dao.selectList("superapp.selectDailyLeaderboard");

		resultMap.put("result", "OK");
		resultMap.put("list", list);

		return resultMap;
	}

	public Map<String, Object> requestLazyClaimLink(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			int emailCount = dao.selectOne("superapp.checkEmailDuplicate", map.get("email"));
			if (emailCount > 0) {
				resultMap.put("result", "FAIL");
				// 프론트엔드 알럿에 띄워줄 명확한 안내 메시지
				resultMap.put("msg", "This email already owns a page. Please log in instead.");
				return resultMap;
			}

			String country = (String) map.get("country");
			if (country == null || country.trim().isEmpty() || country.equals("GLOBAL")) {
				map.put("country", "KR");
			}

			// 1. 토큰 생성 및 임시 저장 (PAGE_ID 컬럼에 reqName을 임시 보관)
			String token = UUID.randomUUID().toString().replace("-", "");
			map.put("token", token);
			dao.insert("superapp.insertLazyClaimToken", map);

			// 2. 메일 발송 로직
			String magicLink = "https://witch-hunting.com/api/super/page/claim/verify?token=" + token;
			if (mailSender != null) {
				SimpleMailMessage message = new SimpleMailMessage();
				message.setTo(String.valueOf(map.get("email")));
				message.setSubject("👑 [Star Platform] Claim Your Page - " + map.get("reqName"));
				message.setText("Click the link below to instantly create and own your page:\n" + magicLink);
				mailSender.send(message);
			}

			resultMap.put("result", "OK");
		} catch (Exception e) {
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	public Map<String, Object> getAvailablePages(String country) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			List<Map<String, Object>> list = dao.selectList("superapp.selectAvailablePages", country);

			// (선택) 여기에 Redis 등을 붙여 list 각 항목에 'viewers: 랜덤숫자' 를 추가하면 완벽합니다.

			resultMap.put("result", "OK");
			resultMap.put("list", list);
		} catch (Exception e) {
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	public Map<String, Object> claimSocialPage(Map<String, Object> params) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			// 1. 1인 1페이지 방어 검사 (이전에 만든 checkEmailDuplicate 재활용)
			int emailCount = dao.selectOne("superapp.checkEmailDuplicate", params.get("email"));
			if (emailCount > 0) {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "This email already owns a page.");
				return resultMap;
			}

			// 2. 소유권 획득 실행 (UPDATE)
			int affectedRows = dao.update("superapp.claimSocialPage", params);

			// 🌟 0.1초 차이로 다른 사람이 먼저 누른 경우 방어
			if (affectedRows == 0) {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Sorry, this page was just taken by someone else!");
				return resultMap;
			}

			Map<String, Object> starInfo = dao.selectOne("superapp.selectStarByEmail", params.get("email"));

			if (starInfo != null) {
				// Get the DB-generated or existing ID
				String prsId = String.valueOf(starInfo.get("starId")); // Check your XML for the exact alias (e.g.,
																		// PRS_ID or starId)

				// 🌟 4. Auto-Login Logic: Generate token immediately
				String starToken = UUID.randomUUID().toString();

				Map<String, Object> tokenParam = new HashMap<>();
				tokenParam.put("starId", prsId);
				tokenParam.put("starToken", starToken);

				// Save the newly generated token to DB
				dao.update("superapp.updateStarToken", tokenParam);

				// Return EVERYTHING the frontend needs to log in
				resultMap.put("result", "OK");
				resultMap.put("starId", prsId); // 👈 Frontend learns the ID here!
				resultMap.put("starToken", starToken); // 👈 Frontend gets the token here!
			} else {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Account created, but failed to retrieve info.");
			}
		} catch (Exception e) {
			resultMap.put("result", "FAIL");
		}
		return resultMap;
	}

	/**
	 * 기존 크리에이터 소셜 로그인 (파이어베이스 보안 적용)
	 */
	public Map<String, Object> loginSocialStar(Map<String, Object> params) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			// 🌟 수정됨: email 뿐만 아니라 uid까지 모두 일치하는지 DB에서 확인 (selectStarByEmailAndUid)
			Map<String, Object> starInfo = dao.selectOne("superapp.selectStarByEmailAndUid", params);

			if (starInfo != null) {
				String token = UUID.randomUUID().toString(); // 🌟 세션 토큰 생성
				System.out.println("토큰: " + token);
				params.put("starId", starInfo.get("starId"));
				params.put("starToken", token);

				// DB에 토큰 및 만료일(예: 30일) 저장
				dao.update("superapp.updateStarToken", params);

				resultMap.put("result", "OK");
				resultMap.put("starId", starInfo.get("starId"));
				resultMap.put("starToken", token); // 🌟 클라이언트에 토큰 반환
			} else {
				// 🌟 정보가 없거나 uid가 일치하지 않으면 차단!
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Account not found or invalid access. Please create a page first.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Login failed due to a server error.");
		}
		return resultMap;
	}

	/**
	 * 기존 ID/PW 스타 로그인 처리 및 토큰 발급
	 */
	public Map<String, Object> mobileStarLogin(Map<String, Object> params) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			// 프론트에서 넘어온 id, pw 파라미터 매핑
			Map<String, Object> loginParams = new HashMap<>();
			loginParams.put("PRS_ID", params.get("id"));
			loginParams.put("PRS_PWD", params.get("pw"));

			// 1. ID / PW 검증 (기존 superAdminService.loginCheck 대체 쿼리)
			Map<String, Object> starUser = dao.selectOne("superapp.selectStarByIdPw", loginParams);

			if (starUser != null && "ST".equals(starUser.get("PRS_AUTH"))) {
				// 2. 🌟 인증 성공 시 세션 토큰 생성 (소셜 유저와 동일한 규격)
				String starToken = UUID.randomUUID().toString();

				Map<String, Object> tokenParam = new HashMap<>();
				tokenParam.put("starId", starUser.get("PRS_ID"));
				tokenParam.put("starToken", starToken);

				// DB에 발급한 토큰 업데이트
				dao.update("superapp.updateStarToken", tokenParam);

				resultMap.put("result", "OK");
				resultMap.put("starId", starUser.get("PRS_ID"));
				resultMap.put("starToken", starToken); // 🌟 클라이언트에 토큰 전달
				resultMap.put("msg", "Login successful.");
			} else {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Invalid ID or password.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Internal server error.");
		}
		return resultMap;
	}

	/**
	 * 🌟 [원상복구] 스타 피드 작성 및 파일 저장 통합 로직 (JSON Base64 지원)
	 */
	public Map<String, Object> addStarFeed(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			String prsId = (String) params.get("prsId");
			String starToken = (String) params.get("starToken");
			String feedText = (String) params.get("feedText");

			// Base64 문자열로 수신
			String imageBase64 = (String) params.get("imageBase64");
			String videoBase64 = (String) params.get("videoBase64");
			String youtubeUrl = (String) params.get("youtubeUrl");

			// 1. 토큰 유효성 검증
			Map<String, Object> tokenParam = new HashMap<>();
			tokenParam.put("starId", prsId);
			tokenParam.put("starToken", starToken);
			int validTokenCount = dao.selectOne("superapp.checkStarToken", tokenParam);

			if (validTokenCount == 0) {
				result.put("result", "FAIL");
				result.put("msg", "인증 세션이 만료되었습니다. 다시 로그인해 주세요.");
				return result;
			}

			// 2. 피드 마스터 (본문 & 유튜브 URL) Insert
			Map<String, Object> masterParam = new HashMap<>();
			masterParam.put("PRS_ID", prsId);
			masterParam.put("CON_BODY", feedText);
			masterParam.put("YOUTUBE_URL", youtubeUrl != null ? youtubeUrl : "");

			dao.insert("superapp.insertContentMaster", masterParam);
			Object conId = masterParam.get("CON_ID");

			int sortOrder = 0;

			// 3. Base64 이미지 디코딩 및 저장
			if (imageBase64 != null && !imageBase64.isEmpty()) {
				String[] parts = imageBase64.split(",");
				String base64Data = parts.length > 1 ? parts[1] : parts[0];

				String ext = ".jpg";
				if (parts[0].contains("png"))
					ext = ".png";
				else if (parts[0].contains("gif"))
					ext = ".gif";

				byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
				String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
				Path targetPath = Paths.get(Constants._FILE_SAVE_PATH + fileName);
				Files.write(targetPath, decodedBytes);

				// 미디어 테이블 Insert
				Map<String, Object> mediaParam = new HashMap<>();
				mediaParam.put("CON_ID", conId);
				mediaParam.put("MEDIA_TYPE", "PHOTO");
				mediaParam.put("MEDIA_URL", Constants._FILE_URL + fileName);
				mediaParam.put("THUMB_URL", Constants._FILE_URL + fileName);
				mediaParam.put("SORT_ORDER", sortOrder++);

				dao.insert("superapp.insertContentMedia", mediaParam);
			}

			// 4. Base64 동영상 디코딩 및 저장
			if (videoBase64 != null && !videoBase64.isEmpty()) {
				String[] parts = videoBase64.split(",");
				String base64Data = parts.length > 1 ? parts[1] : parts[0];

				String ext = ".mp4"; // 기본값
				if (parts[0].contains("webm"))
					ext = ".webm";
				else if (parts[0].contains("ogg"))
					ext = ".ogg";
				else if (parts[0].contains("mov"))
					ext = ".mov";
				else if (parts[0].contains("avi"))
					ext = ".avi";

				byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
				String uuid = UUID.randomUUID().toString().replace("-", "");
				String videoName = uuid + ext;
				String thumbName = uuid + "_thumb.jpg";

				// 동영상 파일 저장
				Path targetPath = Paths.get(Constants._VIDEO_SAVE_PATH + videoName);
				Files.write(targetPath, decodedBytes);

				// 썸네일 생성 (FFmpeg 호출)
				generateVideoThumbnail(targetPath.toString(), Constants._VIDEO_THUMNAIL_SAVE_PATH + thumbName);

				// 미디어 테이블 Insert
				Map<String, Object> mediaParam = new HashMap<>();
				mediaParam.put("CON_ID", conId);
				mediaParam.put("MEDIA_TYPE", "VIDEO");
				mediaParam.put("MEDIA_URL", Constants._VIDEO_FILE_URL + videoName);
				mediaParam.put("THUMB_URL", Constants._VIDEO_THUMNAIL_FILE_URL + thumbName);
				mediaParam.put("SORT_ORDER", sortOrder++);

				dao.insert("superapp.insertContentMedia", mediaParam);
			}

			result.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "Failed to save the post. Please try again.");
		}
		return result;
	}

	/**
	 * 🌟 [신규 추가] 동영상 썸네일 추출 헬퍼 메소드 (StarController에서 이전됨)
	 */
	private void generateVideoThumbnail(String videoPath, String thumbPath) {
		try {
			// 리눅스 서버 기준: /usr/bin/ffmpeg (로컬 윈도우 환경이면 환경변수에 등록된 ffmpeg가 실행됨)
			String[] cmd = {
					"ffmpeg", "-i", videoPath,
					"-ss", "00:00:01", // 1초 지점 캡처
					"-vframes", "1",
					"-y", // 덮어쓰기 허용
					thumbPath
			};

			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			process.waitFor();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("썸네일 생성 실패: " + e.getMessage());
		}
	}

	// 헬퍼 메소드: 토큰 검증
	public boolean verifyStarToken(String starId, String token) {
		Map<String, Object> param = new HashMap<>();
		param.put("starId", starId);
		param.put("starToken", token);
		int count = dao.selectOne("superapp.checkStarToken", param);
		return count > 0;
	}

	public int countEmptyPages(String country) throws Exception {
		return dao.selectOne("superapp.countEmptyPages", country);
	}

	// SuperAppService.java 내부에 추가

	/**
	 * 스타 프로필 수정 (닉네임, 사진, 비밀번호)
	 */
	public Map<String, Object> updateStarProfile(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			String starId = (String) params.get("starId");
			String starToken = (String) params.get("starToken");
			String imageBase64 = (String) params.get("imageBase64");

			// 1. 토큰 유효성 검증 (보안)
			if (!this.verifyStarToken(starId, starToken)) {
				result.put("result", "FAIL");
				result.put("msg", "인증 세션이 만료되었습니다.");
				return result;
			}

			// 2. 이미지 파일 처리 (이미지가 넘어온 경우만)
			if (imageBase64 != null && !imageBase64.isEmpty()) {
				String[] parts = imageBase64.split(",");
				String base64Data = parts.length > 1 ? parts[1] : parts[0];
				String ext = parts[0].contains("png") ? ".png" : ".jpg";

				byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
				String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
				Path targetPath = Paths.get(Constants._FILE_SAVE_PATH + fileName);
				Files.write(targetPath, decodedBytes);

				// DB에 저장할 URL 경로 세팅
				params.put("profileImageUrl", Constants._FILE_URL + fileName);
			}

			// 3. DB 업데이트 실행
			dao.update("superapp.updateProfile", params);

			result.put("result", "OK");
			result.put("msg", "Profile updated successfully.");

			// 변경된 이미지 URL이 있다면 응답에 포함
			if (params.containsKey("profileImageUrl")) {
				result.put("newProfileImage", params.get("profileImageUrl"));
			}

		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "An error occurred while updating.");
		}
		return result;
	}

	/**
	 * 🌟 [신규] FCM 토큰 저장/초기화 (로그인/로그아웃 시)
	 */
	public Map<String, Object> updateFcmToken(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			dao.update("superapp.updateFcmToken", params);
			result.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
		}
		return result;
	}

	/**
	 * 🌟 [신규] 푸시 알림 수신 상태(PUSH_YN) 토글
	 */
	public Map<String, Object> togglePushSetting(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			dao.update("superapp.updatePushSetting", params);
			result.put("result", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
		}
		return result;
	}

	/**
	 * 🌟 [신규] 스타(크리에이터) 검색 및 페이징 로직
	 */
	public Map<String, Object> searchStars(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			// 1. 검색어 처리 (trim)
			String query = (String) params.get("query");
			if (query == null)
				query = "";
			params.put("query", query.trim());

			// 2. 페이징 파라미터 안전 변환 (Jackson 파싱 대비)
			int page = 1;
			int pageSize = 30;
			if (params.get("page") != null) {
				page = Integer.parseInt(String.valueOf(params.get("page")));
			}
			if (params.get("pageSize") != null) {
				pageSize = Integer.parseInt(String.valueOf(params.get("pageSize")));
			}

			// offset 계산 (MyBatis LIMIT #{offset}, #{limit} 용)
			int offset = (page - 1) * pageSize;
			params.put("limit", pageSize);
			params.put("offset", offset);

			// 3. 데이터 조회 및 총 갯수 카운트
			List<Map<String, Object>> list = dao.selectList("superapp.searchStars", params);
			int total = dao.selectOne("superapp.countSearchStars", params);

			// 4. Paging 메타데이터 조립
			Map<String, Object> paging = new HashMap<>();
			paging.put("page", page);
			paging.put("pageSize", pageSize);
			paging.put("total", total);

			result.put("result", "OK");
			result.put("list", list);
			result.put("paging", paging);

		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "Search failed due to a server error.");
		}
		return result;
	}

	/**
	 * 🌟 [신규] 컨텐츠(게시글) 본문 검색 및 페이징 로직
	 */
	public Map<String, Object> searchContents(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			// 1. 검색어 처리 (공백 제거)
			String query = (String) params.get("query");
			if (query == null)
				query = "";
			params.put("query", query.trim());

			// 2. 페이징 파라미터 변환 및 방어 코드
			int page = 1;
			int pageSize = 30;
			if (params.get("page") != null) {
				page = Integer.parseInt(String.valueOf(params.get("page")));
			}
			if (params.get("pageSize") != null) {
				pageSize = Integer.parseInt(String.valueOf(params.get("pageSize")));
			}

			// offset 산출 (LIMIT #{offset}, #{limit} 바인딩용)
			int offset = (page - 1) * pageSize;
			params.put("limit", pageSize);
			params.put("offset", offset);

			// 3. MyBatis 데이터 조회 실행
			List<Map<String, Object>> list = dao.selectList("superapp.searchContents", params);
			int total = dao.selectOne("superapp.countSearchContents", params);

			// 4. 명세 양식에 맞춰 페이징 데이터 빌드
			Map<String, Object> paging = new HashMap<>();
			paging.put("page", page);
			paging.put("pageSize", pageSize);
			paging.put("total", total);

			result.put("result", "OK");
			result.put("list", list);
			result.put("paging", paging);

		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "Content search failed due to a server error.");
		}
		return result;
	}

	// ==========================================
	// [신규] 유저 간 댓글 차단 기능 (Guest)
	// ==========================================

	/**
	 * 유저 차단 등록
	 */
	public Map<String, Object> addCommentBlock(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			// 이미 차단했는지 확인
			int exists = dao.selectOne("superapp.checkCommentBlockExists", params);
			if (exists > 0) {
				result.put("result", "FAIL");
				result.put("msg", "Already blocked");
				return result;
			}

			// 차단 등록
			dao.insert("superapp.insertCommentBlock", params);
			result.put("result", "OK");

		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "Failed to block user due to a server error.");
		}
		return result;
	}

	/**
	 * 유저 차단 해제
	 */
	public Map<String, Object> removeCommentBlock(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			// 삭제 실행 후 적용된 행(Row)의 갯수를 반환받음
			int affectedRows = dao.delete("superapp.deleteCommentBlock", params);

			if (affectedRows > 0) {
				result.put("result", "OK");
			} else {
				// 지운 내역이 없으면 (차단된 적이 없으면)
				result.put("result", "FAIL");
				result.put("msg", "Not found");
			}

		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "Failed to unblock user due to a server error.");
		}
		return result;
	}

	/**
	 * 내가 차단한 유저 목록 조회
	 */
	public Map<String, Object> getCommentBlockList(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (params.get("blockerDeviceId") == null
					|| String.valueOf(params.get("blockerDeviceId")).trim().isEmpty()) {
				result.put("result", "FAIL");
				result.put("msg", "Invalid deviceId");
				return result;
			}

			List<Map<String, Object>> list = dao.selectList("superapp.selectCommentBlockList", params);

			result.put("result", "OK");
			result.put("list", list);

		} catch (Exception e) {
			e.printStackTrace();
			result.put("result", "FAIL");
			result.put("msg", "Failed to retrieve block list.");
		}
		return result;
	}

	public Map<String, Object> getMyInsight(Map<String, Object> params) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			String starId = (String) params.get("starId");
			String starToken = (String) params.get("starToken");

			if (!this.verifyStarToken(starId, starToken)) {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Session expired. Please log in again.");
				return resultMap;
			}

			Map<String, Object> stats = dao.selectOne("superapp.selectMyInsight", starId);

			if (stats != null) {
				Map<String, Object> today = new HashMap<>();
				today.put("visitors", stats.get("todayVisitors"));
				today.put("likes", stats.get("todayLikes")); // 🌟 실제 DB 값 연결!
				today.put("favorites", stats.get("todayFavorites")); // 🌟 실제 DB 값 연결!

				Map<String, Object> month = new HashMap<>();
				month.put("visitors", stats.get("monthVisitors"));
				month.put("likes", stats.get("monthLikes")); // 🌟 실제 DB 값 연결!
				month.put("favorites", stats.get("monthFavorites")); // 🌟 실제 DB 값 연결!

				Map<String, Object> total = new HashMap<>();
				total.put("visitors", stats.get("totalVisitors"));
				total.put("likes", stats.get("totalLikes"));
				total.put("favorites", stats.get("totalFavorites"));

				resultMap.put("today", today);
				resultMap.put("month", month);
				resultMap.put("total", total);
				resultMap.put("result", "OK");
			} else {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Data not found.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Internal server error.");
		}
		return resultMap;
	}

	// 🌟 [신규] 랭킹 메뉴용 내 누적 수익 조회
	public Map<String, Object> getMyRankingRevenue(Map<String, Object> params) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			String starId = (String) params.get("starId");
			String starToken = (String) params.get("starToken");

			// 1. 토큰 유효성 검증 (금융/수익 데이터이므로 필수)
			if (!this.verifyStarToken(starId, starToken)) {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Session expired. Please log in again.");
				return resultMap;
			}

			// 2. 총 방문자 수 조회
			Long totalVisits = dao.selectOne("superapp.selectTotalViewsForRevenue", starId);
			if (totalVisits == null) {
				totalVisits = 0L;
			}

			// 3. 수익 계산 로직 적용
			double revenueUsd = totalVisits * 0.0000667;
			double revenueKrw = totalVisits * 0.1;

			resultMap.put("totalVisits", totalVisits);
			resultMap.put("revenueUsd", revenueUsd);
			resultMap.put("revenueKrw", revenueKrw);
			resultMap.put("result", "OK");

		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Failed to calculate revenue.");
		}
		return resultMap;
	}

	// 🌟 [신규] My Daily Insight - 활동 유저 500명 리스트 조회
	public Map<String, Object> getInsightUserLogs(Map<String, Object> params) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			String starId = (String) params.get("starId");
			String starToken = (String) params.get("starToken");
			String type = (String) params.get("type"); // VISITOR, LIKE, FAVORITE

			// 1. 보안 검증 (본인만 조회 가능)
			if (!this.verifyStarToken(starId, starToken)) {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Session expired.");
				return resultMap;
			}

			// 2. 타입에 따라 알맞은 쿼리 호출
			List<Map<String, Object>> list = new ArrayList<>();
			if ("VISITOR".equals(type)) {
				list = dao.selectList("superapp.selectRecentVisitors", starId);
			} else if ("LIKE".equals(type)) {
				list = dao.selectList("superapp.selectRecentLikes", starId);
			} else if ("FAVORITE".equals(type)) {
				list = dao.selectList("superapp.selectRecentFavorites", starId);
			} else {
				resultMap.put("result", "FAIL");
				resultMap.put("msg", "Invalid type parameter.");
				return resultMap;
			}

			resultMap.put("result", "OK");
			resultMap.put("list", list);

		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Failed to load user logs.");
		}
		return resultMap;
	}

	// 🌟 [신규] 즐겨찾기 목록 상세 조회
	public Map<String, Object> getFavoriteStars(Map<String, Object> params) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		try {
			// 클라이언트가 보낸 starIds 배열 추출
			List<String> starIds = (List<String>) params.get("starIds");

			// starIds가 없거나 비어있으면 쿼리를 실행하지 않고 빈 배열 즉시 반환
			if (starIds == null || starIds.isEmpty()) {
				resultMap.put("result", "OK");
				resultMap.put("list", new ArrayList<>());
				return resultMap;
			}

			// 조건에 맞는 스타 목록 조회
			List<Map<String, Object>> list = dao.selectList("superapp.selectFavoriteStars", params);

			resultMap.put("result", "OK");
			resultMap.put("list", list);

		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("result", "FAIL");
			resultMap.put("msg", "Failed to load favorites.");
		}

		return resultMap;
	}
}
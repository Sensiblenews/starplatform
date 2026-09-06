package com.sensible.api.controller;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sensible.api.service.LandingVisitService;
import com.sensible.api.service.MediaAccessService;
import com.sensible.api.service.SuperAppService;
import com.sensible.common.Constants;
import com.sensible.common.util.ImageModerationUtil;
import com.sensible.common.domain.CommandMap;

@Controller
public class SuperAppController {

	private static final Logger logger = LoggerFactory.getLogger(SuperAppController.class);

	@Resource(name = "dmService")
	private com.sensible.api.service.DmService dmService;

	@Resource(name = "superAppService")
	private SuperAppService superAppService;

	@Resource(name = "mediaAccessService")
	private MediaAccessService mediaAccessService;

	@Resource(name = "landingVisitService")
	private LandingVisitService landingVisitService;

	@RequestMapping(value = "/api/super/policy.do")
	public String privacyPolicy(HttpServletRequest request) {
		// 관리자 로그인이 필수가 아니라면 별도의 권한 체크 없이 바로 반환합니다.
		return "super/privacy_policy";
	}

	// 1. 로비 데이터 조회 (인기 스타 + 전체 리스트)
	// [Redis 캐시/읽기 전용] GET 전환. POST는 구버전 앱 호환용 (이전 완료 후 제거)
	@RequestMapping(value = "/api/super/lobby", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getLobbyData(CommandMap commandMap) throws Exception {
		return superAppService.getLobbyData(commandMap.getMap());
	}

	// 2. 스타 상세 정보 조회 (프로필 + 갤러리)
	// 피드는 limit/offset으로 나눠 받는다(2-26차). 두 값을 보내지 않으면
	// 서비스가 기본값으로 보정하므로 기존 호출(웹 랜딩 등)은 그대로 동작한다.
	@RequestMapping(value = "/api/super/star/{starId}", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getStarDetail(@PathVariable("starId") String starId, CommandMap commandMap)
			throws Exception {
		commandMap.put("starId", starId);
		return superAppService.getStarDetail(commandMap.getMap());
	}

	/**
	 * 검수 대기 이미지 — 작성자 본인 전용 (2-26차).
	 *
	 * &lt;img src&gt;는 인증 헤더를 붙일 수 없으므로 목록 응답에 실어 보낸 단기 서명 토큰으로 판정한다.
	 * 서명이 유효해도 서비스가 현재 상태를 다시 확인하므로, 관리자가 거절한 순간부터는
	 * 이미 발급된 토큰도 통하지 않는다. 응답은 캐시하지 않는다.
	 *
	 * 경로가 /api/super/ 아래인 이유: witch-servlet.xml의 InterceptorAppAPI·InterceptorAPI가
	 * /api/**에 걸려 app_authorization 헤더를 요구하는데, 브라우저가 직접 보내는 이미지 요청에는
	 * 헤더를 붙일 수 없어 403이 된다. 두 인터셉터 모두 /api/super/**를 제외하고 있고
	 * 앱이 쓰는 다른 API도 전부 그 아래에 있다.
	 */
	@RequestMapping(value = "/api/super/media/pending", method = RequestMethod.GET)
	public void getPendingMedia(@RequestParam("t") String token, HttpServletResponse response) throws Exception {
		MediaAccessService.Grant grant = mediaAccessService.verify(token);
		if (grant == null) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		java.io.File file = superAppService.resolvePendingMedia(grant.targetType, grant.targetId);
		if (file == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String ext = ImageModerationUtil.extensionOf(file.getName());
		response.setContentType("png".equals(ext) ? "image/png"
				: "webp".equals(ext) ? "image/webp" : "image/jpeg");
		response.setContentLength((int) file.length());
		// 거절 후에도 브라우저 캐시에 남으면 곤란하다
		response.setHeader("Cache-Control", "no-store");

		java.io.InputStream in = null;
		try {
			in = new java.io.FileInputStream(file);
			java.io.OutputStream out = response.getOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			out.flush();
		} finally {
			if (in != null) { try { in.close(); } catch (Exception e) { } }
		}
	}

	// 3. [핵심] 광고 시청 로그 기록 (돈 계산용)
	// 앱에서 광고를 본 후 이 API를 호출하면 장부에 기록됩니다.
	@RequestMapping(value = "/api/ad/log", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> recordAdLog(CommandMap commandMap) throws Exception {
		// 파라미터: MEM_ID(시청자), PRS_ID(스타), AD_TYPE(광고종류)
		logger.info("Ad Log Record: " + commandMap.getMap());
		// 앱 진입점은 IMPRESSION 1초 연타 가드를 거친다 (2-23차)
		return superAppService.insertAdLogFromApp(commandMap.getMap());
	}

	// 4. 피드 상세 조회 (게시글 + 미디어 리스트)
	@RequestMapping(value = "/api/super/feed/{conId}", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getFeedDetail(@PathVariable("conId") String conId, CommandMap commandMap)
			throws Exception {
		commandMap.put("conId", conId);
		return superAppService.getFeedDetail(commandMap.getMap());
	}

	@RequestMapping(value = "/api/super/star/toggleFollow", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> toggleFollow(CommandMap commandMap) throws Exception {
		logger.info("Toggle Follow Request: " + commandMap.getMap());
		return superAppService.toggleFollow(commandMap.getMap());
	}

	// ==========================================
	// [신규] 비로그인 (Guest) 기능 API 모음
	// ==========================================

	// ==========================================
	// [신규] 비로그인 (Guest) 기능 API 모음 (수정됨)
	// ==========================================

	// 5. 댓글 목록 조회
	@RequestMapping(value = "/api/super/comment/list", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getCommentList(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.getCommentList(params);
	}

	// 6. 비로그인 댓글 작성
	@RequestMapping(value = "/api/super/comment/add", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> addComment(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.addComment(params);
	}

	// 7. 비로그인 댓글 본인 삭제 (비밀번호 확인)
	@RequestMapping(value = "/api/super/comment/delete", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteComment(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.deleteComment(params);
	}

	// 8. 비로그인 좋아요 토글 (기기 ID 기반)
	@RequestMapping(value = "/api/super/like/toggle", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> toggleGuestLike(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.toggleGuestLike(params);
	}

	// 9. 공유하기 횟수 증가
	@RequestMapping(value = "/api/super/share/add", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> addShareCount(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.addShareCount(params);
	}

	// 🌟 10. 댓글 신고 API (누락되었던 부분 추가!)
	@RequestMapping(value = "/api/super/comment/report", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> reportComment(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.reportComment(params);
	}

	// 🌟 실시간 조회수 폴링 API
	@RequestMapping(value = "/api/super/lobby/poll", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> pollAdViews(@RequestBody Map<String, Object> params) throws Exception {
		// 비즈니스 로직은 철저하게 Service로 위임!
		return superAppService.pollAdViews(params);
	}

	// 🌟 [신규] 내 페이지 맞춤형 실시간 조회수 폴링 API
	@RequestMapping(value = "/api/super/star/my-visitors-poll", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> pollMyVisitors(@RequestBody Map<String, Object> params) throws Exception {
		// 파라미터로 starId와 lastCheckTime이 넘어옵니다.
		return superAppService.pollMyVisitors(params);
	}

	// 🌟 [신규] 웹 랜딩 방문 카운트 API — 서버 발급 HMAC 토큰(2.5초 체류 하한 포함) 검증 후 WH_AD_LOG에 IMPRESSION 기록
	@RequestMapping(value = "/api/super/landing/visit", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> recordLandingVisit(@RequestBody Map<String, Object> params,
			HttpServletRequest request) {
		return landingVisitService.recordVisit(params, request);
	}

	// 🌟 [신규] FCM 기기 토큰 저장/삭제 API
	@RequestMapping(value = "/api/super/star/push/token", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateFcmToken(@RequestBody Map<String, Object> params) {
		return superAppService.updateFcmToken(params);
	}

	// 🌟 [신규] 푸시 알림 수신 설정 조회 API — 프로필 팝오버가 서버 값으로 토글을 초기화할 때 사용
	// 주의: 사용자별 데이터이므로 Cloudflare 캐시 규칙에 추가하지 말 것 (docs/ads-txt-runbook.md 참고)
	@RequestMapping(value = "/api/super/star/push/status", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getPushSetting(@RequestParam("starId") String starId) {
		return superAppService.getPushSetting(starId);
	}

	// 🌟 [신규] 프로필 팝업 메뉴의 푸시 알림 ON/OFF 설정 API
	@RequestMapping(value = "/api/super/star/push/toggle", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> togglePushSetting(@RequestBody Map<String, Object> params) {
		return superAppService.togglePushSetting(params);
	}

	// ==========================================
	// [신규] 온보딩 (Page First & Magic Link) API
	// ==========================================

	// 1. 페이지 즉시 생성
	@RequestMapping(value = "/api/super/page/create", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> createAutoPage(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.createAutoPage(params);
	}

	// 2. 소유권 청구 (매직 링크 발송 요청)
	@RequestMapping(value = "/api/super/page/claim", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> requestClaimLink(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.requestClaimLink(params);
	}

	// 3. 매직 링크 클릭 시 검증 및 자동 로그인 (웹 브라우저 접근을 위해 GET 사용)
	@RequestMapping(value = "/api/super/page/claim/verify", method = RequestMethod.GET)
	public String verifyClaimToken(@RequestParam("token") String token, Model model) throws Exception {
		try {
			// 1. 서비스 로직 호출 (토큰 검증 및 DB 업데이트)
			Map<String, Object> result = superAppService.verifyClaimToken(token);

			if ("OK".equals(result.get("result"))) {
				String starId = String.valueOf(result.get("starId"));
				String email = String.valueOf(result.get("email"));
				String pw = String.valueOf(result.get("pw"));

				// 🌟 [핵심] 기존 URL 형식과 동일하게 구성하되, 경로만 'claim-verify'로 설정
				// 앱이 이 URL을 감지하면 자동으로 열리게 됩니다.
				String webAppUrl = "witchhunting://claim-verify?starId=" + starId + "&email=" + email + "&pw=" + pw;

				model.addAttribute("ogTitle", "🎉 Verification Success!");
				model.addAttribute("ogDesc", "Redirecting to your Star Page...");
				model.addAttribute("targetAppUrl", webAppUrl); // JSP에서 location.href로 사용

				return "/common/deeplink_redirect";
			} else {
				model.addAttribute("msg", result.get("msg"));
				return "/common/deeplink_error";
			}
		} catch (Exception e) {
			return "/common/error";
		}
	}

	// 🌟 [신규] 무한 트래픽 추천 페이지 조회 API
	@RequestMapping(value = "/api/super/page/discover", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getDiscoverPages(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.getDiscoverPages(params);
	}

	// SuperAppController.java 내부에 추가

	@RequestMapping(value = "/api/super/message/send", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendMessage(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.sendMessage(params);
	}

	@RequestMapping(value = "/api/super/message/list", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getMessageList(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.getMessageList(params);
	}

	@RequestMapping(value = "/api/super/page/claim/status", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> checkClaimStatus(@RequestParam("token") String token) throws Exception {
		return superAppService.checkClaimStatus(token);
	}

	// [Redis 캐시/읽기 전용] GET 전환. POST는 구버전 앱 호환용 (이전 완료 후 제거)
	@RequestMapping(value = "/api/super/leaderboard", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getLeaderboard() throws Exception {
		return superAppService.getLeaderboard();
	}

	// 🌟 [신규] VS 배틀필드 카드 목록 (로비 상단 캐러셀). 프론트 3초 폴링 / Redis 2초 캐시
	@RequestMapping(value = "/api/super/lobby/vs-cards", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getVsCards() throws Exception {
		return superAppService.getVsCards();
	}

	// ==========================================
	// 🌟 [신규] 1:1 메신저 (2-29차) — 스타 소유자끼리만. 모든 요청은 body의 starId + starToken으로 본인 확인
	// ==========================================

	@RequestMapping(value = "/api/super/dm/send", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> dmSend(@RequestBody Map<String, Object> params) {
		return dmService.sendText(params);
	}

	@RequestMapping(value = "/api/super/dm/upload", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> dmUpload(@RequestBody Map<String, Object> params) {
		return dmService.sendFile(params);
	}

	@RequestMapping(value = "/api/super/dm/rooms", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> dmRooms(@RequestBody Map<String, Object> params) {
		return dmService.getRooms(params);
	}

	@RequestMapping(value = "/api/super/dm/messages", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> dmMessages(@RequestBody Map<String, Object> params) {
		return dmService.getMessages(params);
	}

	@RequestMapping(value = "/api/super/dm/read", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> dmRead(@RequestBody Map<String, Object> params) {
		return dmService.markRead(params);
	}

	@RequestMapping(value = "/api/super/dm/unread-count", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> dmUnreadCount(@RequestBody Map<String, Object> params) {
		return dmService.getUnreadCount(params);
	}

	// 첨부 파일 스트리밍. 대화 내용 응답에 붙은 단기 토큰(t)으로만 접근 — URL만으로는 열리지 않는다
	@RequestMapping(value = "/api/super/dm/file", method = RequestMethod.GET)
	public void dmFile(@RequestParam("t") String token, HttpServletResponse response) throws Exception {
		MediaAccessService.Grant grant = mediaAccessService.verify(token);
		if (grant == null || !com.sensible.api.service.DmService.isFileGrant(grant.targetType)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		java.io.File file = dmService.resolveFile(grant.targetType, grant.targetId);
		if (file == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		response.setContentType(com.sensible.api.service.DmService.contentTypeOf(file.getName()));
		response.setContentLength((int) file.length());
		response.setHeader("Cache-Control", "no-store");
		try (java.io.InputStream in = new java.io.FileInputStream(file);
				java.io.OutputStream out = response.getOutputStream()) {
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) != -1) {
				out.write(buf, 0, n);
			}
			out.flush();
		}
	}

	// 🌟 [신규] 로비 LIVE 티커 어드민 문구 목록 (2-29차). Redis 30초 캐시, 어드민 저장 시 즉시 evict
	@RequestMapping(value = "/api/super/lobby/live-news", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getLiveNews() throws Exception {
		return superAppService.getLiveNews();
	}

	// 🌟 [신규] 카테고리별 TOP100 (로비 탭 리스트)
	// 파라미터: rankType=GLOBAL|DAILY, category=GLOBAL|STAR|CELEB|BRAND|ORG|UNIV|CITY|MEDIA
	@RequestMapping(value = "/api/super/leaderboard/category", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getCategoryLeaderboard(CommandMap commandMap) throws Exception {
		Map<String, Object> params = commandMap.getMap();
		// Redis 캐시 키가 파라미터 기반이므로 허용값 외에는 기본값으로 정규화 (키 오염 방지)
		params.put("rankType", "DAILY".equals(params.get("rankType")) ? "DAILY" : "GLOBAL");
		String category = String.valueOf(params.get("category"));
		if (!"GLOBAL".equals(category) && !Constants.STAR_CATEGORIES.contains(category)) {
			category = "GLOBAL";
		}
		params.put("category", category);
		return superAppService.getCategoryLeaderboard(params);
	}

	// [Redis 캐시/읽기 전용] GET 전환. POST는 구버전 앱 호환용 (이전 완료 후 제거)
	@RequestMapping(value = "/api/super/leaderboard/revenue", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getRevenueLeaderboard() throws Exception {
		return superAppService.getRevenueLeaderboard();
	}

	// [Redis 캐시/읽기 전용] GET 전환. 파라미터(date)는 쿼리스트링으로 수신.
	// CommandMap이 GET 쿼리파라미터/JSON 바디를 모두 처리(CustomMapArgumentResolver)
	@RequestMapping(value = "/api/super/leaderboard/daily", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getDailyLeaderboard(CommandMap commandMap) throws Exception {
	    return superAppService.getDailyLeaderboard(commandMap.getMap());
	}

	// ==========================================
	// [신규] 명예의 전당 (Hall of Fame) API 모음
	// ==========================================

	// 1. 역대 오늘의 왕
	// [Redis 캐시/읽기 전용] GET 전환. POST는 구버전 앱 호환용 (이전 완료 후 제거)
	@RequestMapping(value = "/api/super/hall-of-fame/daily-kings", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getDailyKings(CommandMap commandMap) throws Exception {
		return superAppService.getDailyKings(commandMap.getMap());
	}

	// 2. 역대 TOP 100 (명예의 전당 탭 용)
	// [Redis 캐시/읽기 전용] GET 전환. POST는 구버전 앱 호환용 (이전 완료 후 제거)
	@RequestMapping(value = "/api/super/hall-of-fame/top100", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getHallOfFameTop100() throws Exception {
		return superAppService.getHallOfFameTop100();
	}

	// 3. 월간 챔피언 (targetMonth="2026-06" 를 쿼리스트링으로 전달)
	// [Redis 캐시/읽기 전용] GET 전환. POST는 구버전 앱 호환용 (이전 완료 후 제거)
	@RequestMapping(value = "/api/super/hall-of-fame/monthly", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getMonthlyChampions(CommandMap commandMap) throws Exception {
		return superAppService.getMonthlyChampions(commandMap.getMap());
	}

	// 4. 연간 챔피언 (targetYear="2026" 을 쿼리스트링으로 전달)
	// [Redis 캐시/읽기 전용] GET 전환. POST는 구버전 앱 호환용 (이전 완료 후 제거)
	@RequestMapping(value = "/api/super/hall-of-fame/yearly", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getYearlyChampions(CommandMap commandMap) throws Exception {
		return superAppService.getYearlyChampions(commandMap.getMap());
	}

	/**
	 * [API] 지연 생성 매직 링크 요청 (앱 모달에서 호출)
	 */
	@RequestMapping(value = "/api/super/page/claim/lazy", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> requestLazyClaimLink(@RequestBody Map<String, Object> params) {
		try {
			return superAppService.requestLazyClaimLink(params);
		} catch (Exception e) {
			Map<String, Object> result = new HashMap<>();
			result.put("result", "FAIL");
			return result;
		}
	}

	@RequestMapping(value = "/api/super/page/available", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getAvailablePages(@RequestParam(defaultValue = "KR") String country) {
		return superAppService.getAvailablePages(country);
	}

	@RequestMapping(value = "/api/super/page/claim/social", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> claimSocialPage(@RequestBody Map<String, Object> params) {
		return superAppService.claimSocialPage(params);
	}

	// 기존 크리에이터 소셜 로그인 (애플/구글)
	@RequestMapping(value = "/api/super/star/login/social", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> loginSocialStar(@RequestBody Map<String, Object> params) {
		return superAppService.loginSocialStar(params);
	}

	@RequestMapping(value = "/api/super/star/login", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> mobileStarLogin(@RequestBody Map<String, Object> params) {
		// 비즈니스 로직은 철저하게 Service로 위임!
		return superAppService.mobileStarLogin(params);
	}

	// 🌟 [원상복구] 스타 피드 작성 (JSON Base64 방식)
	@RequestMapping(value = "/api/super/star/feed/add", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> addStarFeed(@RequestBody Map<String, Object> params) {
		// 비즈니스 로직과 파일 저장은 모두 Service에서 전담합니다.
		return superAppService.addStarFeed(params);
	}

	// SuperAppController.java 내부 추가

	@RequestMapping(value = "/api/super/star/feed/delete", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteStarFeed(@RequestBody Map<String, Object> params) {
		// 비즈니스 로직과 권한 체크(Token)는 Service에서 전담
		return superAppService.deleteStarFeed(params);
	}

	@RequestMapping(value = "/api/apple/callback", method = { RequestMethod.POST, RequestMethod.GET })
	public void appleCallback(@RequestParam Map<String, String> params, HttpServletResponse response) throws Exception {

		// 1. 애플이 쏴준 데이터 꺼내기
		String idToken = params.get("id_token");
		String code = params.get("code");
		String user = params.get("user"); // (최초 1회 가입 시에만 이름/이메일이 JSON으로 들어옴)

		// 2. 안드로이드 앱을 다시 깨우기 위한 딥링크(Deep Link) 주소 조립
		// (Capacitor 애플 플러그인이나 앱 내 라우팅 설정에 맞춘 스킴을 사용합니다)
		// 예시: witchhunting://apple-login?id_token=어쩌구저쩌구...
		String appDeepLink = "witchhunting://apple-auth"
				+ "?id_token=" + (idToken != null ? idToken : "")
				+ "&user=" + (user != null ? java.net.URLEncoder.encode(user, "UTF-8") : "");

		// 3. 열려있던 웹 브라우저를 닫고, 우리 안드로이드 앱으로 강제 이동(Redirect)!
		response.sendRedirect(appDeepLink);
	}

	// SuperAppController.java 내부에 추가

	/**
	 * [API] 스타 프로필 수정 (닉네임 / 사진 / 비번)
	 */
	@RequestMapping(value = "/api/super/star/profile/update", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateStarProfile(@RequestBody Map<String, Object> params) {
		// 이제 비즈니스 로직은 Service에서 전담합니다.
		return superAppService.updateStarProfile(params);
	}

	// 🌟 [신규] 스타 검색 API
	@RequestMapping(value = "/api/super/star/search", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> searchStars(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.searchStars(params);
	}

	// 🌟 [신규] 컨텐츠(게시글) 검색 API
	@RequestMapping(value = "/api/super/content/search", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> searchContents(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.searchContents(params);
	}

	// ==========================================
	// [신규] 유저 간 댓글 차단(Block) API 모음
	// ==========================================

	// 차단 등록
	@RequestMapping(value = "/api/super/block/add", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> addCommentBlock(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.addCommentBlock(params);
	}

	// 차단 해제
	@RequestMapping(value = "/api/super/block/remove", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> removeCommentBlock(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.removeCommentBlock(params);
	}

	// 차단 목록 조회
	@RequestMapping(value = "/api/super/block/list", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getCommentBlockList(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.getCommentBlockList(params);
	}

	// 🌟 [신규] 내 스타페이지 상세 통계 (My Insight)
	@RequestMapping(value = "/api/super/my-insight", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getMyInsight(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.getMyInsight(params);
	}

	// 🌟 [신규] 랭킹 메뉴 - 내 누적 수익 조회
	// [Redis 캐시/읽기 전용] GET 전환. starId를 쿼리스트링으로 전달.
	// ⚠ 사용자별 데이터이므로 공용 CDN(Cloudflare) 캐싱 대상에서 제외할 것 (origin Redis 캐시만)
	@RequestMapping(value = "/api/super/ranking/my-revenue", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> getMyRankingRevenue(CommandMap commandMap) throws Exception {
		return superAppService.getMyRankingRevenue(commandMap.getMap());
	}

	// 🌟 [신규] My Daily Insight - 최근 활동 유저 500명 리스트
	@RequestMapping(value = "/api/super/my-insight/logs", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getInsightUserLogs(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.getInsightUserLogs(params);
	}

	// 🌟 [신규] 즐겨찾기 목록 별도 조회
	@RequestMapping(value = "/api/super/star/favorites", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getFavoriteStars(@RequestBody Map<String, Object> params) throws Exception {
		return superAppService.getFavoriteStars(params);
	}
}

package com.sensible.api.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.sensible.admin.service.ChatService;
import com.sensible.api.service.ContentService;
import com.sensible.api.service.MemberService;
import com.sensible.common.Constants;
import com.sensible.common.domain.CommandMap;

@Controller
public class ApiController {
	
	private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
	
	@Resource(name = "memberService")
	private MemberService memberService;
	
	@Resource(name = "contentService")
	private ContentService contentService;
	
	@Resource(name = "chatService")
	private ChatService chatService;
	
	@RequestMapping(value = "/app/hello", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object hello() {
		return "hello world";
	}
	
	//가입
	@RequestMapping(value = "/app/userJoin", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object user(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		
		Map<String, Object> resultMap = memberService.user(commandMap.getMap());
		return resultMap;
	}

	// 17.05.09.
	@RequestMapping(value = "/app/user", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object userChk(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		// $2a$10$8Ti3Ul510z0FVF2HcogLyuOeqEMkgFhGiCzDmZwg8C.y5HMNZ3XQe
		// 내 token
		Map<String, Object> resultMap = memberService.userChk(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/app/profile", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object getProfile(CommandMap commandMap, HttpServletRequest request) throws Exception {
		Map<String, Object> resultMap = memberService.profile(commandMap.getMap());
		return resultMap;
	}
	
	
	//@RequestMapping(value = "/api/user", method = RequestMethod.GET, produces = "application/json")
	@RequestMapping(value = "/api/user")
	@ResponseBody
	public Object getApiUser(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.getApiUser(commandMap.getMap());
		return resultMap;
	}

	@RequestMapping(value = "/api/user", method = RequestMethod.PUT, produces = "application/json")
	@ResponseBody
	public Object putApiUser(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.putApiUser(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/api/user", method = RequestMethod.DELETE, produces = "application/json")
	@ResponseBody
	public Object deleteApiUser(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.deleteApiUser(commandMap.getMap());
		System.out.println("[CTSOFT] 유저를 삭제합니다 : "+ commandMap.get("MEM_ID"));
		return resultMap;
	}
	
	@RequestMapping(value = "/api/content",  produces = "application/json")
	@ResponseBody
	public Object content(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);				
		Map<String, Object> resultMap = contentService.content(commandMap.getMap());
		
		return resultMap;
	}
	
	@RequestMapping(value = "/api/heart", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object heart(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.heart(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/api/heart/sender", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object heartSender(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.heartSender(commandMap.getMap());
		return resultMap;
	}
	/**
	 * 요정(언론사) 0
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/app/fairyContents",  produces = "application/json")
	@ResponseBody
	public Object fairyContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.fairyContents(commandMap.getMap());
		return resultMap;
	}
	
	
	/**
	 * 몬스터 (학원출판) 1
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/app/monsterContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object monsterContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.monsterContents(commandMap.getMap());
		return resultMap;
	}
	
	
	/**
	 * 마녀(기업/단체)  8
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/app/witchContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object witchContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.witchContents(commandMap.getMap());
		return resultMap;
	}
	
	
	/**
	 * Exclusive (마녀 2)  8
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/app/exclusiveContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object exclusiveContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.witchContents(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/api/subscribe", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Map<String, Object> subscribe(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.subscribe(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/app/mySubscriptions", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Map<String, Object> mySubscriptionsMap(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.mySubscriptions(commandMap.getMap());
		return resultMap;
	}
	
	
	/**
	 * 스토리 6
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/app/storyContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object storyContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);		
		Map<String, Object> resultMap = contentService.storyContents(commandMap.getMap());
		return resultMap;
	}
	
	
	/**
	 * 도깨비  9
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/app/goblinContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object goblinContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		String memId = request.getParameter("MEM_ID");
		commandMap.put("MEM_ID", memId);
		Map<String, Object> resultMap = contentService.goblinContents(commandMap.getMap());
		return resultMap;
	}

	@RequestMapping(value = "/app/promotionContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object promotionContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.promotionContents(commandMap.getMap());
		return resultMap;
	}

	@RequestMapping(value = "/app/FAQContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object FAQContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.FAQContents(commandMap.getMap());
		return resultMap;
	}

	@RequestMapping(value = "/app/SearchResultContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object SearchResultContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.SearchResultContents(commandMap.getMap());
		return resultMap;
	}


	@RequestMapping(value = "/app/popup", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object popup(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.popup(commandMap.getMap());
		return resultMap;
	}
	
	/**
	 * 논리 구구단
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/app/NonRiContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object NonRiContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.NonRiContents(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/app/StarContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object StarContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.StarContents(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/api/appInstDetail", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object appInstDetail(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.appInstDetail(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/app/policyDetail", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object policyDetail(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.policyDetail(commandMap.getMap());
		return resultMap;
	}

	@RequestMapping(value = "/api/comment", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object postComment(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		List<Map<String, Object>> resultMap = contentService.postComment(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/api/commentMod", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object putComment(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		List<Map<String, Object>> resultMap = contentService.putComment(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/api/comment", method = RequestMethod.DELETE, produces = "application/json")
	@ResponseBody
	public Object deleteComment(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		List<Map<String, Object>> resultMap = contentService.deleteComment(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/api/comments", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object getComments(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		List<Map<String, Object>> resultMap = contentService.getComments(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/api/purchase", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object purchase(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		int star = contentService.purchase(commandMap.getMap());
		return star;
	}
	
	@RequestMapping(value = "/app/sensibleEvent", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object sensibleEvent(CommandMap commandMap, HttpServletRequest request) throws Exception {
		Map<String, Object> resultMap = contentService.sensibleEvent(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value = "/api/sensibleEvent", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiSensibleEvent(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.apiSensibleEvent(commandMap.getMap());
		return resultMap;
	}
		
	/**
	 * 일반(도깨비 등록)
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/api/insertContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object insertContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);

		Map<String, Object> resultMap = contentService.insertContents(request, commandMap.getMap());
		return resultMap;
	}
	
	
	/**
	 * 일반 수정
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/api/updateContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object updateContents(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.updateContents(request, commandMap.getMap());
		return resultMap;
	}
	
	public void sessionInfoGetAndPut(CommandMap commandMap, HttpServletRequest request) {
		HttpSession session = request.getSession();
		String appId = String.valueOf(session.getAttribute(Constants._APP_SESSION_KEY));
		String memId = String.valueOf(session.getAttribute("MEM_ID"));
		//String memId = "2024012700001";
		System.out.println("[APP_ID] ::::::::::::::::::: "+appId);
		System.out.println("[MEM_ID] ::::::::::::::::::: "+memId);
		
		logger.error("[APP_ID] ::::::::::::::::::: "+appId);
		logger.error("[MEM_ID] ::::::::::::::::::: "+memId);
		
		commandMap.put("APP_ID", appId);
		commandMap.put("MEM_ID", memId);
	}
	
	/**
	 * 내 콘텐츠 가져오기(도깨비)
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/api/myContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiSensibleMyContent(CommandMap commandMap, HttpServletRequest request) throws Exception {
		sessionInfoGetAndPut(commandMap, request);
		commandMap.put("APP_ID", "1");
		commandMap.put("CON_TYPE", "9");
		Map<String, Object> resultMap = contentService.apiSensibleMyContent(commandMap.getMap());
		return resultMap;
	}
	
	/**
	 * 프로필 콘텐츠 가져오기(도깨비)
	 * @param commandMap
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/api/profileContents", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiProfileContent(CommandMap commandMap, HttpServletRequest request) throws Exception {
		commandMap.put("APP_ID", "1");
		commandMap.put("CON_TYPE", "9");
		Map<String, Object> resultMap = contentService.profileContent(commandMap.getMap());
		return resultMap;
	}
	
	/**
	 * 유저차단
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value = "/api/blockUser", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiBlockUser(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = contentService.blockUser(commandMap.getMap());
		return resultMap;
	}
	
	/**
	 * 유저 닉네임 변경
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/modifyNickname", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiModifyNickname(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.modifyNickname(commandMap.getMap());
		return resultMap;		
	}
	
	/**
	 * 유저 한줄소개 변경
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/modifyIntroduction", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiModifyIntroduction(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.modifyIntroduction(commandMap.getMap());
		return resultMap;		
	}
	
	/**
	 * 유저 팔로우
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/followUser", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiFollowUser(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.followUser(commandMap.getMap());
		return resultMap;
	}
	
	
	
	/**
	 * 유저 언팔로우
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/unFollowUser", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiUnFollowUser(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.unFollowUser(commandMap.getMap());
		return resultMap;
	}
	
	/**
	 * 마녀 페이지 팔로워/팔로잉 카운트 정보
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/witchCount", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiWitchCount(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.followCount(commandMap.getMap());
		return resultMap;
	}
	
	/**
	 * 팔로우 리스트 (검색)
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/followsSearch", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiFollowsSearch(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		List<Map<String, Object>> resultMap = memberService.follows(commandMap.getMap(), true);
		return resultMap;
	}
	
	/**
	 * 팔로우 리스트
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/follows", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiFollows(CommandMap commandMap, HttpServletRequest request) throws Exception{
		List<Map<String, Object>> resultMap = memberService.follows(commandMap.getMap(), false);
		return resultMap;
	}
	
	/**
	 * 팔로워 리스트 (검색)
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/followersSearch", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiFollowersSearch(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		List<Map<String, Object>> resultMap = memberService.followers(commandMap.getMap(), true);
		return resultMap;
	}
	
	/**
	 * 팔로워 리스트
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/followers", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiFollowers(CommandMap commandMap, HttpServletRequest request) throws Exception{
		List<Map<String, Object>> resultMap = memberService.followers(commandMap.getMap(), false);
		return resultMap;
	}
	
	/**
	 * 게시글 조회자 조회
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/viewers", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiViewers(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		List<Map<String, Object>> resultMap = contentService.viewers(commandMap.getMap());
		return resultMap;
	}
	
	/**
	 * 프로필 이미지 변경
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/modifyProfileImage", method=RequestMethod.POST, produces="application/json")
	@ResponseBody
	public Object modifyProfileImage(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.modifyProfileImage(commandMap.getMap());
		return resultMap;
	}
	
	/**
	 * 도깨비 최근 게시글 100건 기준 사용자 리스트
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/goblinPrsList", method=RequestMethod.POST, produces="application/json")
	@ResponseBody
	public Object apiGoblinPrsList(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		List<Map<String, Object>> resultMap = contentService.goblinPrsList(commandMap.getMap());
		return resultMap;
	}
	
	/**
	 * 사용자에게 온 쪽지 리스트 조회
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/newMessage", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiCheckReceivedMessage(CommandMap commandMap, HttpServletRequest request) throws Exception{
		logger.info("commandMap", commandMap);
		logger.info("yahoo");
		Map<String, Object> resultMap = memberService.checkReceivedMessage(commandMap.getMap());
		return resultMap;
	}


	/**
	 * 사용자에게 온 쪽지 리스트 조회
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/messageList", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object apiMessageList(CommandMap commandMap, HttpServletRequest request) throws Exception{
		logger.info("commandMap", commandMap);
		List<Map<String, Object>> resultMap = memberService.messageList(commandMap.getMap());
		return resultMap;
	}

	/**
	 * 사용자 쪽지 등록
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/addMessage", method=RequestMethod.POST, produces="application/json")
	@ResponseBody
	public Object addMessage(CommandMap commandMap, HttpServletRequest request) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.addMessage(commandMap.getMap());
		return resultMap;
	}

	/**
	 * 사용자 쪽지 읽음 처리
	 * @param commandMap
	 * @param request
	 */
	@RequestMapping(value="/api/readMessage", method=RequestMethod.POST, produces="application/json")
	@ResponseBody
	public Object readMessage(
			CommandMap commandMap,
			HttpServletRequest request
	) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.readMessage(commandMap.getMap());
		return resultMap;
	}
	
	@RequestMapping(value="/api/uploadChatFile", method=RequestMethod.POST, produces="application/json")
	@ResponseBody
	public Object uploadChatFile(
			CommandMap commandMap,
			HttpServletRequest request
	) throws Exception{
		sessionInfoGetAndPut(commandMap, request);
		Map<String, Object> resultMap = memberService.uploadFile(commandMap.getMap());
		return resultMap;
	}


}  
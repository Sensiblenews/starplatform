package com.sensible.admin.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import jbit.core.common.util.ParamUtil;
import jbit.core.domain.DefaultVO;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.Notification;
import com.sensible.admin.domain.ComMessageVO;
import com.sensible.admin.domain.ContentVO;
import com.sensible.admin.domain.EventVO;
import com.sensible.admin.domain.UserVO;
import com.sensible.admin.service.AdminService;
import com.sensible.api.service.FirebaseService;
import com.sensible.common.Constants;
import com.sensible.common.domain.CommandMap;
import com.sensible.common.util.SessionUtil;

@Controller
public class AdminController {
	protected Log log = LogFactory.getLog(AdminController.class);

	@Resource(name = "adminService")
	private AdminService adminService;
	
	@Resource(name = "firebaseService")
	private FirebaseService firebaseService;
	
	private static final String REDIRECT_PREFIX = "redirect:";
	private static final String FORWARD_PREFIX = "forward:";
	private static String getRedirectUrl(String url, String paramStr){
		return REDIRECT_PREFIX+url + (paramStr != null ? "?" + paramStr : "");
	}
	private static String getForwardUrl(String url, String paramStr){
		return FORWARD_PREFIX+url + (paramStr != null ? "?" + paramStr : "");
	}
	/**
	 * 메뉴 이동
	 * @param linkPage
	 * @param session
	 * @param baseMenuId
	 * @param menuId
	 * @return
	 */
	@RequestMapping(value="/pageLink/PageLink.do")
	public String moveToPage(@RequestParam("link") String linkPage, HttpSession session, @RequestParam(value="topMenuId", required=false) String topMenuId
			, @RequestParam(value="baseMenuId", required=false)	String baseMenuId, @RequestParam(value="menuId", required=false) String menuId){

		String link = linkPage;
		if (linkPage==null || linkPage.equals("")){
			link = getRedirectUrl(Constants.DEFAULT_RETURN_URL,null);
		}else{
			if(link.indexOf(",")>-1){
			    link=link.substring(0,link.indexOf(","));
			}
		}
		// 선택된 메뉴정보를 세션으로 등록한다.
		if (menuId!=null && !menuId.equals("") && !menuId.equals("null")){
			session.setAttribute("baseMenuId",baseMenuId);
			session.setAttribute("menuId",menuId);
		}
		return link;
	}


	@RequestMapping(value = "/adminChk", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object adminChk(CommandMap commandMap, HttpServletRequest request) throws Exception {
		String id = adminService.adminChk(commandMap.getMap());		
		if (id != null && id.equals("admin")) {
			HttpSession session = request.getSession(true);
			session.setAttribute("ID", id);
			session.setMaxInactiveInterval(1000 * 60 * 120);
		}
		return id;
	}
	
	
	/**
	 * 대쉬보드_통계
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/statusMain")
	public Object statusMain(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {		
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		
		commandMap.put("PRS_AUTH", userVO.getPRS_AUTH());
		commandMap.put("PRS_ID", userVO.getPRS_ID());			
		
		if(Constants.AUTH_SM.equals(userVO.getPRS_AUTH()))
		{
			commandMap.put("APP_ID", "1"); //샌시블로 셋팅
		}else{
			commandMap.put("APP_ID", userVO.getAPP_ID());
		}
		
		
		Map<String, Object> memberStat = adminService.selectCompMemberStatus(commandMap.getMap());
		Map<String, Object> startStat = adminService.selectCompStarStatus(commandMap.getMap());
		Map<String, Object> heartStat = adminService.selectCompHeartStatus(commandMap.getMap());
		
		List<Map<String, Object>> graphDaily = adminService.getGraphDataDaily(commandMap.getMap());
		List<Map<String, Object>> graphMonthly = adminService.getGraphDataMonthly(commandMap.getMap());
		
		ObjectMapper mapper = new ObjectMapper();
		String graphDailyJson = mapper.writeValueAsString(graphDaily);
		String graphMonthlyJson = mapper.writeValueAsString(graphMonthly);
		
		model.addAttribute("memberStat", memberStat);
		model.addAttribute("startStat", startStat);		
		model.addAttribute("heartStat", heartStat);
		model.addAttribute("graphDaily", graphDailyJson);
		model.addAttribute("graphMonthly", graphMonthlyJson);
		model.addAttribute("paramVO", commandMap.getMap());	
		
		return "/stat/statMain.default";
	}
	
	
	/**
	 * 회원관리
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/memberList")
	public Object memberList(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {		
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		
		commandMap.put("PRS_AUTH", userVO.getPRS_AUTH());
		commandMap.put("PRS_ID", userVO.getPRS_ID());	
		commandMap.put("APP_ID", userVO.getAPP_ID());
		
		List<Map<String, Object>> mList = adminService.memberList(commandMap.getMap());
		model.addAttribute("mList", mList);		
		model.addAttribute("paramVO", commandMap.getMap());	
		return "/member/memberList.default";
	}
	
	@RequestMapping(value = "/adm/memberDetail.do")
	public Object memberDetail(CommandMap commandMap, Model model) throws Exception {		
		Map<String, Object> mMap = adminService.memberEdit(commandMap.getMap());
		model.addAttribute("mMap", mMap);
		model.addAttribute("paramVO", commandMap.getMap());	
		return "/member/memberDetail";
	}
	
	
	/**
	 * 회원 수정 저장
	 * @param request
	 * @param model
	 * @param commandMap
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/memberUpdate.do")
	public String memberUpdate(HttpServletRequest request, ModelMap model, CommandMap commandMap) throws Exception {
	
		try {			
			
			adminService.memberUpdate(commandMap.getMap());	
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "저장 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}

		return "jsonView";
	}

	
	/**
	 * 업체관리
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/compList")
	public Object pressList(HttpServletRequest request,CommandMap commandMap,Model model) throws Exception {
		
		HashMap paramVO = ParamUtil.getReqParamHashMap(request);
		List<Map<String, Object>> list = adminService.pressList(commandMap.getMap());
		
		model.addAttribute("list", list);
		model.addAttribute("paramVO", paramVO);
		
		return "/comp/compList.default";
	}
	
	
	/**
	 * 업체 상세 - 등록,폼
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/compInsertForm.do")
	public Object compInsertForm(HttpServletRequest request,CommandMap commandMap, Model model) throws Exception {
		
		HashMap<String,Object> paramVO = ParamUtil.getReqParamHashMap(request);	
		
		model.addAttribute("paramVO", paramVO);
		return "/comp/compInsertForm";		
	}
	
	
	/**
	 * 업체 등록 저장
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/compInsertSave.do")	
	public Object compInsertSave(HttpServletRequest request,CommandMap commandMap, Model model) throws Exception {
		
		try {			
			
			adminService.pressRegit(commandMap.getMap());
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "저장 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}

		
		return "jsonView";
	}
	
	
	/**
	 * 업세 수정 폼
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/compUpdateForm.do")
	public Object compUpdateForm(CommandMap commandMap, Model model) throws Exception {
		Map<String, Object> detail = adminService.pressEdit(commandMap.getMap());
		
		model.addAttribute("detail", detail);
		model.addAttribute("paramVO", commandMap.getMap());
		
		return "/comp/compUpdateForm";		
	}
	
	
	/**
	 * 업체 수정 저장
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/compUpdateSave.do")		
	public Object compUpdateSave(CommandMap commandMap, Model model) throws Exception {
				
		try 
		{			
			adminService.pressUpdate(commandMap.getMap());
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "수정 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}
		
		return "jsonView";
	}
	

	
	
	@RequestMapping("/adm/pressResetPwd.do")
    public String pressResetPwd(HttpServletRequest request, ModelMap model,CommandMap commandMap)throws Exception { 

    	try
    	{
    		commandMap.put("PRS_PWD", "111111");
    		
    		adminService.pressUpdate(commandMap.getMap());
			model.addAttribute("status", "success");
			model.addAttribute("serviceMessage", "초기화 되었습니다.");
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    		model.addAttribute("status", "fail");
    		model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
    	}
    	
      	return "jsonView";
    } 
	

	
	/**
	 * 이벤트 관리
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/eventList")
	public Object eventList(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {
		
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		
		commandMap.put("PRS_AUTH", userVO.getPRS_AUTH());
		commandMap.put("PRS_ID", userVO.getPRS_ID());	
		commandMap.put("APP_ID", userVO.getAPP_ID());
				
		
		List<Map<String, Object>> eList = adminService.eventList(request, commandMap.getMap());
		model.addAttribute("eList", eList);
		
		EventVO c = new EventVO();
		BeanUtils.populate(c, commandMap.getMap());
		
		model.addAttribute("paramVO", c);
		
		return "/event/eventList.default";
	}
	
	
	/**
	 * 이벤트 등록 폼
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/eventInsertForm.do")
	public Object eventInsertForm(CommandMap commandMap, Model model) throws Exception {
		
		List<Map<String, Object>> prsList = adminService.selectAppList(commandMap.getMap());
		model.addAttribute("prsList", prsList);
		
		return "/event/eventInsertForm";
	}
	
	
	@RequestMapping(value = "/adm/eventInsertSave.do")	
	public Object eventInsertSave(HttpServletRequest request,CommandMap commandMap, Model model) throws Exception {
		
		try 
		{			
			adminService.eventRegit(request, commandMap.getMap());
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "저장 되었습니다.");
			
			// FCM Push Send
			
			String title = commandMap.get("EVT_BTN_TEXT").toString();
			String content = commandMap.get("EVT_TEXT").toString();
			int prsID = Integer.parseInt(commandMap.get("PRS_ID").toString());
			int chgYN = Integer.parseInt(commandMap.get("EVT_CHG_YN").toString());
			int useYN = Integer.parseInt(commandMap.get("EVT_USE_YN").toString());
			
			// 푸시조건: 앱은 센서블, 유료/무료 체크: N, 게시여부 체크: Y
			if(prsID == 1 && chgYN == 0 && useYN == 1){
				System.out.println("======================[Firebase Cloud Messaging] Sending Push ==============================");
				System.out.println("title: " + title);
				System.out.println("content: " + content);
				
				Notification notification = Notification.builder()
						.setTitle(title)
						.setBody(content)
						.build();		
				firebaseService.notify("WitchHuntingPush", notification);
			}
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}
		
		return "jsonView";
	}
	
	
	
	/**
	 * 이벤트 수정 폼
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/eventUpdateForm.do")
	public Object eventUpdateForm(CommandMap commandMap, Model model) throws Exception {
		
		Map<String, Object> detail = adminService.eventEdit(commandMap.getMap());
		model.addAttribute("detail", detail);
		
		List<Map<String, Object>> prsList = adminService.selectAppList(commandMap.getMap());
		model.addAttribute("prsList", prsList);
		
		return "/event/eventUpdateForm";
	}
	
	/**
	 * 이벤트 수정 저장
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/eventUpdateSave.do")
	public Object eventUpdate(CommandMap commandMap, Model model) throws Exception {
		
		try 
		{			
			adminService.eventUpdate(commandMap.getMap());
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "저장 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}
		
		return "jsonView";
	}
	
	
	
	
		
	
	
	/**
	 * 컨텐츠 관리
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/adm/contentList")
	public Object contentList(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		
		commandMap.put("PRS_AUTH", userVO.getPRS_AUTH());
		commandMap.put("PRS_ID", userVO.getPRS_ID());	
		commandMap.put("APP_ID", userVO.getAPP_ID());
				
		
		//로그인한 사용자의 cont type setting
		/** CON_TYPE 정의
		 * 	0: 언론사(요정)
			1: 학원/출판(몬스터)
			2: FAQ
			3: 홍보마당
			4: 공지사항
			5: 사용설명서
			6 : 스토리
			7 : 이용약관
			8: 기업/단체(마녀)
			9: 일반(도깨비)
			11:논리 구구단
		 */
		
		if(commandMap.get("CON_TYPE") == null)
		{
			commandMap.put("CON_TYPE","0");		
		}
		
		if(Constants.AUTH_GE.equals(userVO.getPRS_AUTH()))
		{
			//언론사 1 --> CON_TYPE : 0
			if("1".equals(userVO.getPRS_TYPE()))
			{
				commandMap.put("CON_TYPE","0");
			
			}
			//학원출판 2 --> CON_TYPE : 1
			if("2".equals(userVO.getPRS_TYPE()))
			{
				commandMap.put("CON_TYPE","1");
				
			}
			//기업/단체 3 --> CON_TYPE : 8
			if("3".equals(userVO.getPRS_TYPE()))
			{
				commandMap.put("CON_TYPE","8");			
			}				
		}
		

		List<Map<String, Object>> cList = adminService.cotentList(commandMap.getMap());		
		Map<String, Object> categoryList = adminService.categoryList(commandMap.getMap());

		List<Map<String, Object>> resultList = new ArrayList<>();
		List<Integer> typeValues = Arrays.asList(0, 1, 8, 9);
		
		if (categoryList == null) {
		    List<String> defaultCategories = Arrays.asList("요정", "몬스터", "마녀", "도깨비");
		    for (int i = 0; i < defaultCategories.size(); i++) {
		        Map<String, Object> item = new HashMap<>();
		        item.put("category", defaultCategories.get(i));
		        item.put("type", typeValues.get(i));
		        resultList.add(item);
		    }
		} else {
		    String categoryTag = (String) categoryList.get("CATEGORY_TAG");
		    String[] tags = categoryTag.split(",");
		    
		    for (int i = 0; i < tags.length; i++) {
		        Map<String, Object> item = new HashMap<>();
		        item.put("category", tags[i]);
		        item.put("type", typeValues.get(i));
		        resultList.add(item);
		    }
		}


		String conType = (String) commandMap.getMap().get("CON_TYPE");
		System.out.println("컨텐츠 conType :: "+conType);		
		
		model.addAttribute("CON_TYPE", conType);
		model.addAttribute("cList", cList);		
		model.addAttribute("categoryList", resultList);
		
		ContentVO c = new ContentVO();
		BeanUtils.populate(c, commandMap.getMap());
		
		model.addAttribute("paramVO", c);
		
		return "/content/contentList.default";
	}
	
	
	/**
	 * 컨텐츠 등록 폼
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/contentInsertForm.do")
	public Object contentInsertForm(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {

		String conType = (String) commandMap.getMap().get("CON_TYPE");
		
		if("0".equals(conType)) commandMap.put("PRS_TYPE", "1");
		if("1".equals(conType)) commandMap.put("PRS_TYPE", "2");
		if("8".equals(conType)) commandMap.put("PRS_TYPE", "3");
		
		
		List<Map<String, Object>> prsList = adminService.selectPressList(commandMap.getMap());
		model.addAttribute("prsList", prsList);
		
		int freeCnt=0;
		UserVO userVO = SessionUtil.getSessionUserInfo(request);	

		
		//업체일 경우 해당 업체의 무료별,무료하트 가져오기
		if(!Constants.AUTH_SM.equals(userVO.getPRS_AUTH()))
		{
			freeCnt = adminService.getFreeStartHeart(userVO.getPRS_ID());
		}
		
		model.addAttribute("FREE_CNT", freeCnt);
		model.addAttribute("CON_TYPE", conType);
		
		return "/content/contentInsertForm";
	}
	
	/**
	 * 컨텐츠 등록 폼
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/categoryUpdateForm.do")
	public Object categoryUpdateForm(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {

		String conType = (String) commandMap.getMap().get("CON_TYPE");
		
		if("0".equals(conType)) commandMap.put("PRS_TYPE", "1");
		if("1".equals(conType)) commandMap.put("PRS_TYPE", "2");
		if("8".equals(conType)) commandMap.put("PRS_TYPE", "3");
		
		Map<String, Object> map = commandMap.getMap();
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		map.put("APP_ID", userVO.getAPP_ID());
		
		Map<String, Object> categoryList = adminService.categoryList(map);

		List<Map<String, Object>> resultList = new ArrayList<>();
		List<Integer> typeValues = Arrays.asList(0, 1, 8, 9);
		
		if (categoryList == null) {
		    List<String> defaultCategories = Arrays.asList("요정", "몬스터", "마녀", "도깨비");
		    for (int i = 0; i < defaultCategories.size(); i++) {
		        Map<String, Object> item = new HashMap<>();
		        item.put("category", defaultCategories.get(i));
		        item.put("type", typeValues.get(i));
		        resultList.add(item);
		    }
		} else {
		    String categoryTag = (String) categoryList.get("CATEGORY_TAG");
		    String[] tags = categoryTag.split(",");
		    
		    for (int i = 0; i < tags.length; i++) {
		        Map<String, Object> item = new HashMap<>();
		        item.put("category", tags[i]);
		        item.put("type", typeValues.get(i));
		        resultList.add(item);
		    }
		}
		
		model.addAttribute("categoryList", resultList);
		
		return "/content/categoryUpdateForm";
	}
	
	
	/**
	 * 콘텐츠 등록 저장
	 * @param commandMap
	 * @param model
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/contentInsertSave.do")
	public Object contentInsertSave(HttpServletRequest request ,CommandMap commandMap, Model model) throws Exception {

		try 
		{			
			int conId = adminService.contentRegit(request, commandMap.getMap());
			contentRegitFileChk(conId, request, commandMap.getMap());
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "저장 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}		
	
		return "jsonView";
	}
	
	
	/**
	 * 컨텐츠 수정 폼
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/contentUpdateForm.do")
	public Object contentUpdateForm(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {
		
		String conType = (String) commandMap.getMap().get("CON_TYPE");
		
		if("0".equals(conType)) commandMap.put("PRS_TYPE", "1");
		if("1".equals(conType)) commandMap.put("PRS_TYPE", "2");
		if("8".equals(conType)) commandMap.put("PRS_TYPE", "3");
		
		List<Map<String, Object>> prsList = adminService.selectPressList(commandMap.getMap());
		model.addAttribute("prsList", prsList);		
		
		Map<String, Object> detail = adminService.getContent(commandMap.getMap());
		
		model.addAttribute("detail", detail);		
		
		int freeCnt=0;
		UserVO userVO = SessionUtil.getSessionUserInfo(request);	
		
		//업체일 경우 해다 업체의 무료별,무료하트 가져오기
		if(!Constants.AUTH_SM.equals(userVO.getPRS_AUTH()))
		{
			freeCnt = adminService.getFreeStartHeart(userVO.getPRS_ID());
		}
		
		model.addAttribute("FREE_CNT", freeCnt);
		model.addAttribute("CON_TYPE", conType);
		
		return "/content/contentUpdateForm";
	}
	
	
	/**
	 * 컨텐츠 수정 저장
	 * @param commandMap
	 * @param model
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/contentUpdateSave.do")
	public Object contentUpdateSave(CommandMap commandMap, Model model, HttpServletRequest request) throws Exception {

		try 
		{			
			adminService.contentUpdate(request, commandMap.getMap());
			contentUpdateFileChk(request, commandMap);
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "수정 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}		
	
		return "jsonView";		

	}
	
	/**
	 * 카테고리 수정 저장
	 * @param commandMap
	 * @param model
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/categoryUpdateSave.do")
	public Object categoryUpdateSave(CommandMap commandMap, Model model, HttpServletRequest request) throws Exception {
		try 
		{			
			adminService.categoryUpdate(request, commandMap.getMap());
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "저장 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}		
	
		return "jsonView";		

	}
	
	
	/**
	 * 컨텐츠 소트
	 * @param commandMap
	 * @param model
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/contentSort", method = RequestMethod.POST, produces = "application/json")
	public Object contentSort(CommandMap commandMap, Model model, HttpServletRequest request) throws Exception {
		
		adminService.contentSort(commandMap.getMap());
		
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		
		commandMap.put("PRS_AUTH", userVO.getPRS_AUTH());
		commandMap.put("PRS_ID", userVO.getPRS_ID());

		List<Map<String, Object>> cList = adminService.cotentList(commandMap.getMap());		
		
		String conType = (String) commandMap.getMap().get("CON_TYPE");		
		
		model.addAttribute("CON_TYPE", conType);
		model.addAttribute("cList", cList);		
		
		ContentVO c = new ContentVO();
		BeanUtils.populate(c, commandMap.getMap());
		model.addAttribute("paramVO", c);

		
		return "/content/contentList.default";
	}


	
	
	/**
	 * 최상위 관라자에서 업체별 무료별하트 가져오기
	 * @param request
	 * @param model
	 * @param commandMap
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/freeStarHeart.do")
	public String freeStarHeart(HttpServletRequest request, ModelMap model, CommandMap commandMap) throws Exception {

		int freeCnt = adminService.getFreeStartHeart((String) commandMap.get("PRS_ID"));
			
		model.addAttribute("FREE_CNT", freeCnt);

		return "jsonView";
	}

	
	/**
	 * 댓글 팝업 폼
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/commentUpdateForm.do")
	public Object commentUpdateForm(HttpServletRequest request,CommandMap commandMap, Model model) throws Exception {
		String conId = (String) commandMap.getMap().get("CON_ID");
		List<Map<String, Object>> list = adminService.commentEdit(commandMap.getMap());
		model.addAttribute("CON_ID", conId);
		model.addAttribute("list", list);
		
		return "/content/commentUpdateForm";
	}

	@RequestMapping(value = "/adm/commentUpdate", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Object commentUpdate(HttpServletRequest request,CommandMap commandMap, Model model) throws Exception {
		adminService.commentUpdate(commandMap.getMap());
		return "";
	}

	public void contentRegitFileChk(int conId, HttpServletRequest request, Map<String, Object> commandMap)
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String tOrgUrl = (String) commandMap.get("CON_ORIGIN_URL");
		String conIdStr = String.valueOf(conId);
		map.put("CON_ID", conIdStr);
		map.put("CON_ORIGIN_URL", tOrgUrl);

		// 테마 이미지 체크
		MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
		MultipartFile multipartFile = multipartHttpServletRequest.getFile("CON_THUMNAIL");
		if (multipartFile != null && multipartFile.isEmpty() == false) {
			// local 업로드
			log.debug("filename : " + multipartFile.getOriginalFilename());
			String fileNm = conIdStr + "_1.jpg";
			map.put("CON_THUMNAIL", Constants._FILE_URL + fileNm);
			File imgFile = new File(Constants._FILE_SAVE_PATH + fileNm);
			try {
				multipartFile.transferTo(imgFile);
			} catch (Exception e) {
				e.printStackTrace();
				log.debug(e.getMessage());
				log.debug("img upload fail!!!!!!!!!!!!");
			}
			adminService.contentImgUpdate(map);
		}
		
		MultipartFile videoFile = multipartHttpServletRequest.getFile("CON_VIDEO");
		if (videoFile != null && !videoFile.isEmpty()) {
			log.debug("video filename : " + videoFile.getOriginalFilename());
			
			String originalFileName = videoFile.getOriginalFilename();
			String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
			String videoFileName = conIdStr + "_video" + extension;
			
			map.put("CON_VIDEO", Constants._VIDEO_FILE_URL + videoFileName);
			File videoTarget = new File(Constants._VIDEO_SAVE_PATH + videoFileName);
			
			String thumbnail = conIdStr + "_video_thumb.jpg";
	        String thumbPath = Constants._VIDEO_THUMNAIL_SAVE_PATH + thumbnail;
	        map.put("CON_VIDEO_THUMNAIL", Constants._VIDEO_THUMNAIL_FILE_URL + thumbnail);
	        log.info("썸네일 정보");
	        log.info(thumbnail);
	        log.info(thumbPath);
	        
			try {
				videoFile.transferTo(videoTarget);
		        generateVideoThumbnail(videoTarget.getAbsolutePath(), thumbnail);
			} catch (Exception e) {
				log.debug("video upload fail!!!!!!!!!!!");
			}
			
			adminService.contentVideoUpdate(map);
		}
		
	}

	public void contentUpdateFileChk(HttpServletRequest request, CommandMap commandMap) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String tOrgUrl = (String) commandMap.get("CON_ORIGIN_URL");
		String conIdStr = (String) commandMap.getMap().get("CON_ID");
		map.put("CON_ID", conIdStr);
		map.put("CON_ORIGIN_URL", tOrgUrl);

		// 테마 이미지 체크
		MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
		MultipartFile multipartFile = multipartHttpServletRequest.getFile("CON_THUMNAIL");
		if (multipartFile != null && multipartFile.isEmpty() == false) {
			// local 업로드
			log.debug("filename : " + multipartFile.getOriginalFilename());
			String fileNm = conIdStr + "_1.jpg";			
			
			map.put("CON_THUMNAIL", Constants._FILE_URL + fileNm);			
			File imgFile = new File(Constants._FILE_SAVE_PATH + fileNm);
			try {
				multipartFile.transferTo(imgFile);
			} catch (Exception e) {
				e.printStackTrace();
				log.debug(e.getMessage());
				log.debug("img upload fail!!!!!!!!!!!!");
			}
			adminService.contentImgUpdate(map);
		}
		
		// 메인 동영상 파일 처리
		MultipartFile videoFile = multipartHttpServletRequest.getFile("CON_VIDEO");
		if (videoFile != null && !videoFile.isEmpty()) {
			log.debug("video filename : " + videoFile.getOriginalFilename());
			
			String originalFileName = videoFile.getOriginalFilename();
			String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
			String videoFileName = conIdStr + "_video" + extension;
			
			map.put("CON_VIDEO", Constants._VIDEO_FILE_URL + videoFileName);
			File videoTarget = new File(Constants._VIDEO_SAVE_PATH + videoFileName);
			
			String thumbnail = conIdStr + "_video_thumb.jpg";
	        String thumbPath = Constants._VIDEO_THUMNAIL_SAVE_PATH + thumbnail;
	        map.put("CON_VIDEO_THUMNAIL", Constants._VIDEO_THUMNAIL_FILE_URL + thumbnail);
	        log.info("썸네일 정보");
	        log.info(thumbnail);
	        log.info(thumbPath);
	        
			try {
				videoFile.transferTo(videoTarget);
		        generateVideoThumbnail(videoTarget.getAbsolutePath(), thumbnail);
			} catch (Exception e) {
				log.debug("video upload fail!!!!!!!!!!!");
			}
			
			adminService.contentVideoUpdate(map);
		}
	}
	
	private void generateVideoThumbnail(String videoPath, String thumbnailPath) throws IOException, InterruptedException {
		log.info("썸네일 생성 시작");
	    List<String> command = Arrays.asList(
	        "ffmpeg",
	        "-i", videoPath,
	        "-ss", "00:00:01",
	        "-vframes", "1",
	        "-q:v", "2",
	        "-update", "1",
	        Constants._VIDEO_THUMNAIL_SAVE_PATH + thumbnailPath
	    );
	    log.info("FFmpeg Command: " + String.join(" ", command));

	    ProcessBuilder pb = new ProcessBuilder(command);
	    pb.redirectErrorStream(true);
	    Process process = pb.start();

	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            System.out.println(line);
	        }
	    }

	    int exitCode = process.waitFor();
	    if (exitCode != 0) {
	        throw new RuntimeException("FFmpeg 썸네일 생성 실패 (exit code: " + exitCode + ")");
	    }
	}
	
	
	/**
	 * 앱설정 관리
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/settingList")
	public Object settingList(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {
				
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		
		commandMap.put("PRS_AUTH", userVO.getPRS_AUTH());
		commandMap.put("PRS_ID", userVO.getPRS_ID());
		commandMap.put("APP_ID", userVO.getAPP_ID());

		List<Map<String, Object>> cList = adminService.cotentList(commandMap.getMap());
		

		String conType = (String) commandMap.getMap().get("CON_TYPE");		
		System.out.println("conType ::: "+conType);

		model.addAttribute("CON_TYPE", conType);
		model.addAttribute("cList", cList);
		
		
		ContentVO c = new ContentVO();
		BeanUtils.populate(c, commandMap.getMap());
		
		model.addAttribute("paramVO", c);
		
		return "/app/settingList.default";
	}
	
	
	
	/**
	 * 앱설정 등록 폼
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/settingInsertForm.do")
	public Object settingInsertForm(CommandMap commandMap, Model model) throws Exception {
		
		String conType = (String) commandMap.getMap().get("CON_TYPE");
		
		List<Map<String, Object>> prsList = adminService.selectAppList(commandMap.getMap());
		
		model.addAttribute("prsList", prsList);		
		model.addAttribute("CON_TYPE", conType);
		
		return "/app/settingInsertForm";
	}
	
	
	/**
	 * 앱 설정 저장
	 * @param commandMap
	 * @param model
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/settingInsertSave.do")
	public Object settingInsertSave(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {
				
		try 
		{			
			int conId = adminService.insertSettingSave(request, commandMap.getMap());
			contentRegitFileChk(conId, request, commandMap.getMap());
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "저장 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}		
	
		return "jsonView";
		
	}
	

	
	/**
	 * 앱 수정 폼
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/settingUpdateForm.do")
	public Object settingUpdateForm(HttpServletRequest request, CommandMap commandMap, Model model) throws Exception {
				
		String conType = (String) commandMap.getMap().get("CON_TYPE");
		
		List<Map<String, Object>> prsList = adminService.selectAppList(commandMap.getMap());
		
		Map<String, Object> detail = adminService.getContent(commandMap.getMap());
		
		model.addAttribute("detail", detail);
		model.addAttribute("prsList", prsList);		
		model.addAttribute("CON_TYPE", conType);
		
		return "/app/settingUpdateForm";		
		
	}

	
	
	/**
	 * 앱 수정 저장
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/settingUpdateSave.do")
	public Object settingUpdateSave(HttpServletRequest request, CommandMap commandMap, Model model ) throws Exception {
				
		try 
		{			
			adminService.updateSettingSave(request, commandMap.getMap());
			contentUpdateFileChk(request, commandMap);
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "저장 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}		
	
		return "jsonView";

	}


	@RequestMapping(value = "/adm/logout", method = RequestMethod.POST, produces = "application/json")
	public Object logout(Model model, HttpServletRequest request) throws Exception {
	
		HttpSession session = request.getSession(false);
		session.invalidate();
		return getRedirectUrl(Constants.DEFAULT_RETURN_URL,null); 
		
	}

	@RequestMapping(value = "/appUrl",  produces = "application/json")
	public Object appUrl(CommandMap commandMap, Model model, HttpServletRequest request) throws Exception {
		String device = request.getHeader("User-Agent");
		log.debug("############################ device : " + device);
		
		String deviceToLower = device.toLowerCase();
		String type = "";
		if (deviceToLower.indexOf("iphone") > -1) {
			type = "IOS";
		} else if (device.indexOf("android") > -1) {
			type = "ANDROID";
		} else {
			type = "ANDROID";
		}
		String url = adminService.appUrl(type);
		log.debug("############################ url : " + url);
		model.addAttribute("url", url);
		return "/app/appUrl";
	}
	
	
	
	/**
	 * 계정수정 팝업
	 * @param request
	 * @param model
	 * @return String
	 */
	@RequestMapping(value = "/adm/myPagePop.do")
	public String resetPassword(HttpServletRequest request, ModelMap model,CommandMap commandMap) throws Exception {
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		commandMap.put("PRS_ID", userVO.getPRS_ID());
		Map<String, Object> pMap = adminService.pressEdit(commandMap.getMap());
		model.addAttribute("pMap", pMap);
		return "/mypage/myPagePop";
	}
	
	
	/**
	 * 계정수정 저장
	 * @param request
	 * @param model
	 * @return
	 */	
	@RequestMapping(value = "/adm/updateMypage.do")
	public String updateMypage(HttpServletRequest request, ModelMap model, CommandMap commandMap) throws Exception {

		try {			
			UserVO userVO = SessionUtil.getSessionUserInfo(request);
			commandMap.put("PRS_ID", userVO.getPRS_ID());
			adminService.pressUpdate(commandMap.getMap());
			
			Map<String, Object> map = new HashMap<String, Object>();
			
			String tOrgUrl = (String) commandMap.get("ORIG_FILE_NM");
			String PRS_ID = (String) commandMap.getMap().get("PRS_ID");
			

			// 테마 이미지 체크
			MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
			MultipartFile multipartFile = multipartHttpServletRequest.getFile("ORIG_FILE_NM");
			if (multipartFile != null && multipartFile.isEmpty() == false) {
				// local 업로드
				log.debug("filename : " + multipartFile.getOriginalFilename());
				String fileNm = PRS_ID + ".jpg";			
				map.put("ORIG_FILE_NM",  multipartFile.getOriginalFilename());	
				map.put("STORED_FILE_NM", Constants._FILE_URL + multipartFile.getOriginalFilename());			
				File imgFile = new File(Constants._FILE_SAVE_PATH + multipartFile.getOriginalFilename());
				try {
					multipartFile.transferTo(imgFile);
					
					map.put("PRS_ID",PRS_ID);
					adminService.pressUpdate(map);
				} catch (Exception e) {
					e.printStackTrace();
					log.debug(e.getMessage());
					log.debug("img upload fail!!!!!!!!!!!!");
				}
			}

			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "수정이 완료 되었습니다.");
			
		} catch( Exception e ) {
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}

		return "jsonView";
	}
	
	
	/**
	 * 앱 관리
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/appList")
	public Object appList(HttpServletRequest request,CommandMap commandMap,Model model) throws Exception {

		List<Map<String, Object>> list = adminService.selectApplyList(commandMap.getMap());
		
		model.addAttribute("list", list);
		model.addAttribute("paramVO", commandMap.getMap());
		
		return "/app/appList.default";
	}
	
	
	/**
	 * 앱 신청하기
	 * @param request
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/applyList")
	public Object applyList(HttpServletRequest request,CommandMap commandMap,Model model) throws Exception {

		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		commandMap.put("PRS_ID", userVO.getPRS_ID());
		
		Map<String, Object> detail = adminService.pressEdit(commandMap.getMap());
		
		model.addAttribute("detail", detail);
		model.addAttribute("paramVO", commandMap.getMap());
		
		return "/app/applyList.default";
	}
	
	
	/**
	 * 앱 신청 저장
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/updateAppApply.do")		
	public Object updateAppApply(HttpServletRequest request,CommandMap commandMap, Model model) throws Exception {
				
		try 
		{
			UserVO userVO = SessionUtil.getSessionUserInfo(request);
			commandMap.put("PRS_ID", userVO.getPRS_ID());
			
			adminService.updateAppApply(commandMap.getMap());
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "발급 신청 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}
		
		return "jsonView";
	}
	
	
	/**
	 * 앱 발급하기
	 * @param commandMap
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/adm/appIssused.do")		
	public Object appIssused(HttpServletRequest request,CommandMap commandMap, Model model) throws Exception {
				
		try 
		{			
			adminService.updateAppkey(commandMap.getMap());
			
			model.addAttribute("status", "success");			
			model.addAttribute("serviceMessage", "발급 되었습니다.");
			
		} catch( Exception e ) {
			e.printStackTrace();
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}
		
		return "jsonView";
	}
	
	
	
	/**
	 * 404 Error
	 * @return
	 */
	@RequestMapping(value="/error/404Error.do", method = RequestMethod.GET)
    public String notFoundError(HttpServletRequest request, ModelMap model){	
		return "error/404Error.main";
    }
	
	@RequestMapping(value="/error/404Error.do", method = RequestMethod.POST)
    public String notFoundError2(HttpServletRequest request, ModelMap model){	
		return "error/404Error.main";
    }

	/**
	 * 500 Error
	 * @return
	 */
	@RequestMapping("/error/500Error.do")
    public String serverError(){
		return "error/500Error.main"; 
    }

	/**
	 * Error
	 * @return
	 */
	@RequestMapping("/error/exception.do")
    public String Error(){
		return "error/error.main";
    }
	
	@RequestMapping("/main/viewSessionChk.do")
    public String sessiongChk(){
		return "common/viewSessionChk";
    }

	/**
	 * Info 앱 다운로드 페이지
	 */
	@RequestMapping("/info")
	public String Info(){
		return "info/info.main";
	}
}
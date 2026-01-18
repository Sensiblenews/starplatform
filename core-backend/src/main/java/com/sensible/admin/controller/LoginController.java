package com.sensible.admin.controller;

import java.util.HashMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jbit.core.common.util.ParamUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sensible.admin.domain.UserVO;
import com.sensible.admin.service.LoginService;
import com.sensible.common.Constants;
import com.sensible.common.util.SessionUtil;

@Controller
public class LoginController {

	protected Log log = LogFactory.getLog(LoginController.class);

	@Resource(name = "loginService")
	private LoginService service;
	
	private static final String REDIRECT_PREFIX = "redirect:";
	private static final String FORWARD_PREFIX = "forward:";
	private static String getRedirectUrl(String url, String paramStr){
		return REDIRECT_PREFIX+url + (paramStr != null ? "?" + paramStr : "");
	}
	private static String getForwardUrl(String url, String paramStr){
		return FORWARD_PREFIX+url + (paramStr != null ? "?" + paramStr : "");
	}
	
	
	/**
	 * 메인화면 - 로그인 전이면 로그인 페이지로 이동한다.
	 * @param request
	 * @param model
	 * @param paramVO
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/main/main.do")
	public String main(HttpServletRequest request, HttpServletResponse response, ModelMap model, UserVO paramVO) throws Exception {		
		

		System.out.println("================ main ======================");
		if(!SessionUtil.isSessionAttribute(request, Constants._SESSION_KEY))
		{			
			return "/login/login.main";
		}		
		
		UserVO	sessionVO =  SessionUtil.getSessionUserInfo(request);	
		return getForwardUrl("/adm/statusMain",null);
		//최종 관리자면 회원목록으로 일반관리자면 컨텐츠로
		/*if(Constants.AUTH_GE.equals(sessionVO.getPRS_AUTH()))
		{
			return getForwardUrl("/adm/contentList",null);
			
		}else{
			return getForwardUrl("/adm/statusMain",null);			
		}*/
	}
	
	
	
	@RequestMapping(value="/login/loginPrc.do", method = RequestMethod.GET)
	public String loginPrc(HttpServletRequest request, ModelMap model) throws Exception
	{
    	return getRedirectUrl(Constants.DEFAULT_RETURN_URL,null); 
	}
	
	/**
	 * 로그인 처리
	 * @param request
	 * @param model
	 * @param login
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/login/loginPrc.do", method = RequestMethod.POST)
	public String loginPrc(HttpServletRequest request, ModelMap model, UserVO login) throws Exception
	{
		
		if(SessionUtil.isSessionAttribute(request, Constants._SESSION_KEY))
		{
    		return getRedirectUrl(Constants.DEFAULT_RETURN_URL,null);
		}
			

		String returnUrl = getRedirectUrl(Constants.DEFAULT_RETURN_URL,null);				
		
		
		UserVO userVO = service.selectLogin(login);
		
		// 로그인 정보 일치 확인 및 세션 생성 
		if(userVO != null)
		{
			
			//미인증시
			if("N".equals(userVO.getPRS_AUTH_YN()))
			{
				model.addAttribute("userId", userVO.getPRS_ID());
				model.addAttribute("resultCode", "A");
				return getForwardUrl(Constants.DEFAULT_RETURN_URL,null);
			}
			
			
			//세션 저장 
			SessionUtil.setSessionAttribute(request, Constants._SESSION_KEY, userVO);						
			
			//메인페이지
			returnUrl = getRedirectUrl(Constants.DEFAULT_RETURN_URL,null);

		}
		else
		{
			model.addAttribute("resultCode", "F");
			return getForwardUrl(Constants.DEFAULT_RETURN_URL,null);
		}

		return returnUrl;
	}	
	
	
	/**
	 * 비밀번호 변경 팝업
	 * @param request
	 * @param model
	 * @return String
	 */
	@RequestMapping(value = "/login/changePasswordPop.do")
	public String resetPassword(HttpServletRequest request, ModelMap model) throws Exception {
		return "/login/changePasswordPop";
	}
	
	
	/**
	 * 비밀번호 변경
	 * @param request
	 * @param model
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/login/setNewPassword.do")
	public String setNewPassword(HttpServletRequest request, ModelMap model, UserVO paramVO) throws Exception {
		
		String loginId = paramVO.getPRS_ID();
		System.out.println("loginId ::: "+loginId);		
		HashMap<String,Object> hm = ParamUtil.getReqParamHashMap(request);
		
		try {			
			
			service.setNewPassword(hm);			
			
			model.addAttribute("status", "success");
			model.addAttribute("loginId", loginId);
			model.addAttribute("serviceMessage", "비밀번호가 변경되었습니다.");
			
		} catch( Exception e ) {
			model.addAttribute("status", "fail");
			model.addAttribute("serviceMessage", "처리 중 오류가 발생하였습니다.");
		}

		return "jsonView";
	}
	
	
	/**
	 * 로그아웃 처리
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping("/login/logout.do")
	public String logout(HttpServletRequest request, ModelMap model)throws Exception
	{
	
		HttpSession session = request.getSession();		
		session.invalidate();
	
		return "jsonView";
	}

}

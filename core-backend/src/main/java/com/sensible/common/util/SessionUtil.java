package com.sensible.common.util;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.sensible.admin.domain.UserVO;
import com.sensible.admin.service.LoginService;
import com.sensible.common.Constants;
import com.sensible.common.domain.CommandMap;

/**
* session Util
* - Spring에서 제공하는 RequestContextHolder 를 이용하여
* request 객체를 service까지 전달하지 않고 사용할 수 있게 해줌
* 
*/
public class SessionUtil {
	
	
	@Resource(name = "loginService")
	private LoginService service;

	/**
	* attribute 값을 가져 오기 위한 method
	* 
	* @param String  attribute key name 
	* @return Object attribute obj
	*/
	public static Object getAttribute(String name) throws Exception {
		return (Object)RequestContextHolder.getRequestAttributes().getAttribute(name, RequestAttributes.SCOPE_SESSION);
	}
	
	/**
	* attribute 설정 method
	* 
	* @param String  attribute key name 
	* @param Object  attribute obj
	* @return void
	*/
	public static void setAttribute(String name, Object object) throws Exception {
		RequestContextHolder.getRequestAttributes().setAttribute(name, object, RequestAttributes.SCOPE_SESSION);
	}
	
	/**
	* 설정한 attribute 삭제 
	* 
	* @param String  attribute key name 
	* @return void
	*/
	public static void removeAttribute(String name) throws Exception {
		RequestContextHolder.getRequestAttributes().removeAttribute(name, RequestAttributes.SCOPE_SESSION);
	}
	
	/**
	* session id 
	* 
	* @param void
	* @return String SessionId 값
	*/
	public static String getSessionId() throws Exception  {
		return RequestContextHolder.getRequestAttributes().getSessionId();
	}
	
	
	/**
	 * HttpSession에 주어진 키 값으로 세션 객체를 생성하는 기능
	 * @param request
	 * @param keyStr	- 세션 키
	 * @param obj		- 세션 값
	 * @throws Exception
	 */
	public static void setSessionAttribute(HttpServletRequest request, String keyStr, Object obj) throws ServletException
	{
		HttpSession session = request.getSession();
		session.setAttribute(keyStr, obj);
	}

	/**
	 * HttpSession에 존재하는 주어진 키 값에 해당하는 세션 값을 얻어오는 기능
	 * @param request
	 * @param keyStr	- 세션 키
	 * @return
	 * @throws Exception
	 */
	public static Object getSessionAttribute(HttpServletRequest request, String keyStr) throws ServletException
	{
		HttpSession session = request.getSession();
		return session.getAttribute(keyStr);
	}

	/**
     * HttpSession에 존재하는 세션을 주어진 키 값으로 삭제하는 기능
     * 
     * @param request
     * @param keyStr 	- 세션 키
     * @throws Exception
     */
    public static void removeSessionAttribute(HttpServletRequest request, String keyStr) throws ServletException
    {
		HttpSession session = request.getSession();
		session.removeAttribute(keyStr);
    }
	
	
	 /**
     * 세션 존재 유무
     * @param request
     * @param keyStr	- 세션 키
     * @return
     * @throws Exception
     */
    public static boolean isSessionAttribute(HttpServletRequest request, String keyStr) throws ServletException
    {
    	Object	result	= getSessionAttribute(request, keyStr);

    	if(result != null)
    		return true;

    	return false;
    }
        

    public static UserVO getSessionUserInfo(HttpServletRequest request) throws ServletException
    {
    	Object	result	= getSessionAttribute(request, Constants._SESSION_KEY);

    	return (UserVO)result;
    }

}



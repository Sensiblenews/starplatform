package com.sensible.common.filter;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.sensible.api.service.MemberService;
import com.sensible.common.Constants;
import com.sensible.common.util.SessionUtil;



public class InterceptorAppAPI extends HandlerInterceptorAdapter{

	private static final Logger logger = LoggerFactory.getLogger(InterceptorAppAPI.class);
	
	@Resource(name = "memberService")
	private MemberService memberService;
	
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)throws Exception
	{
		 
		System.out.println("=====================APP AUTH KEY SESSION CHECK =================");	
		
		String appAuthKey = request.getHeader("app_authorization");		
		System.out.println("[appAuthKey] =========== "+appAuthKey);
	
		if (appAuthKey != null && appAuthKey.length() > 0) 
		{
			
			if(SessionUtil.isSessionAttribute(request, Constants._APP_SESSION_KEY))
			{
				return super.preHandle(request, response, handler);
			}
			else
			{
				String appId = memberService.selectAppInfo(appAuthKey);
				SessionUtil.setSessionAttribute(request, Constants._APP_SESSION_KEY, appId);
				
				return super.preHandle(request, response, handler);
			}
		} 
		else 
		{
			if (request.getRequestURI().contains("chatfile")) return super.preHandle(request, response, handler);
			
			response.setStatus(403);
			return false;
		}
		
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) throws Exception {
		logger.debug("HTTP.{} - {} END", request.getMethod(), request.getRequestURI());
		MDC.clear();
	}	
	
}


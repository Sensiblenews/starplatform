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
		 
		String appAuthKey = request.getHeader("app_authorization");

		// 매 요청마다 구분선 한 줄 + 인증키 값을 stdout 에 찍고 있었다.
		// 인증키는 그대로 들고 다니면 세션을 가져올 수 있는 값이라 로그에 남기지 않는다.
		// 키 유무만으로도 이 지점의 분기는 추적된다.
		if (logger.isDebugEnabled()) {
			logger.debug("app auth key {}", (appAuthKey != null && appAuthKey.length() > 0) ? "present" : "absent");
		}
	
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


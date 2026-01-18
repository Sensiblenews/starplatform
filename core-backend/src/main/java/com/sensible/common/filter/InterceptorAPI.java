package com.sensible.common.filter;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.sensible.api.service.MemberService;
import com.sensible.common.Constants;
import com.sensible.common.util.SessionUtil;

public class InterceptorAPI extends HandlerInterceptorAdapter{

	private static final Logger logger = LoggerFactory.getLogger(InterceptorAPI.class);
	
	
	@Resource(name = "memberService")
	private MemberService memberService;
	
	/**
	 * 전처리
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		boolean bool = memberAccessTokenCheck(request);
		
		logger.info("prehandle");
		
		if (!bool) {
			response.setStatus(403);
			return false;
		}
		
		return super.preHandle(request, response, handler);
	}
	
	/**
	 * 후처리
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		if (logger.isDebugEnabled()) {
		}
		
		logger.info("posthandle");
		
		logger.debug("HTTP.{} - {} END", request.getMethod(), request.getRequestURI());
		MDC.clear();
	}
	
	public boolean memberAccessTokenCheck(HttpServletRequest request) throws Exception {
		boolean result = true;
		
		if (request.getRequestURI().contains("chatfile")) return true;
		
		String token = request.getHeader("authorization");
		logger.info(" TOKEN \t\t:  " + token);
		System.out.println("[TOKEN] =========== "+token);
		
		if (token != null && token.length() > 0) {
			try {
				Map<String, Object> map = memberService.memberAccessTokenCheck(token);
				if (map != null) {
					String memId = (String) map.get("MEM_ID");
					HttpSession session = request.getSession();
	                session.setAttribute("MEM_ID", memId);
					
	                logger.info(" MEM_ID \t\t:  " + memId);
					result = true;
				} else {
					logger.info(" MEM_ID \t\t:  NULL");
					result = false;
				}
			} catch (Exception e) {
				logger.info(" Error:  \t\t" + e.getStackTrace());
			}
		} else {
			result = false;
		}
		
		return result;
	}
	
}

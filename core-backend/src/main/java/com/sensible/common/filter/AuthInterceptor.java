package com.sensible.common.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ModelAndViewDefiningException;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.sensible.common.Constants;
import com.sensible.common.util.SessionUtil;


@Controller("authInterceptor")
public class AuthInterceptor extends HandlerInterceptorAdapter{

	private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)throws Exception
	{
		
		boolean authChk = false;
 
		System.out.println("=====================SESSION CHECK =================");

		/**
		 * 세션처리
		*/
				
		if(!SessionUtil.isSessionAttribute(request, Constants._SESSION_KEY))
		{
	
			for(String urlPattern : Constants.SESSION_ALLOW_KEY_WORD.split(","))
			{
				if(request.getRequestURI().indexOf(urlPattern) > -1)
				{					
					return true;
				}
			}
			
			if(!authChk)
			{
				if(request.getHeader("AJAX")!= null && request.getHeader("AJAX").equals("true"))
				{
					response.setStatus(999);
					return false;					
				}
				else
				{
					throw new ModelAndViewDefiningException(new ModelAndView("common/viewSessionChk"));
				}	
			}
			
		}
		
		return true;	

	}
	
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) throws Exception {
		logger.debug("HTTP.{} - {} END", request.getMethod(), request.getRequestURI());
		MDC.clear();
	}
}

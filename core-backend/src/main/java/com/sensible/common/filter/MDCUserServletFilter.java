package com.sensible.common.filter;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import jbit.core.common.util.EgovStringUtil;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sensible.admin.domain.UserVO;
import com.sensible.common.Constants;
import com.sensible.common.util.SessionUtil;

public class MDCUserServletFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(MDCUserServletFilter.class);

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		StringBuffer strGetUrlBuf = new StringBuffer();
		HttpServletRequest httprequest = (HttpServletRequest) request;
		//String		localAddr	= httprequest.getLocalAddr();
		String		remoteIp	= httprequest.getRemoteAddr();
		//HttpSession	session		= httprequest.getSession(true);
		UserVO		userVO		= SessionUtil.getSessionUserInfo(httprequest);

		logger.debug("안녕하세요..");	
		
		String usercode = "";
		String username = "";
		String userip = "";
		
		if(userVO != null){
			usercode = userVO.getPRS_ID();
			username = userVO.getPRS_NAME();
		}
		else{
			usercode = Constants._SYSTEM_USER;
			username = Constants._SYSTEM_USER;
		}
		userip = remoteIp;		
				
		ThreadContext.put("LogPrimaryKey", usercode + "/" + username + "/" + userip);
		
		logger.info("\n");
		logger.info("*********************************** Service START ***********************************");
		try {
			logger.info("Referer => " 		+ httprequest.getHeader("referer"));
			logger.info("Requested URL => " 	+ httprequest.getRequestURL());

			for (Enumeration names = httprequest.getHeaderNames(); names.hasMoreElements();) {
				String name = (String) names.nextElement();
				String val = httprequest.getHeader(name);
				logger.debug(name +" => "+ val);
			}
			
			if(httprequest.getAttribute("javax.servlet.include.request_uri") != null) {
				String strIncludeReqURI = httprequest.getAttribute("javax.servlet.include.request_uri").toString();
				if(!strIncludeReqURI.isEmpty()) {
					logger.info("Requested Include URL => " 	+ strIncludeReqURI);
					logger.info("Requested Include Query => " + request.getAttribute("javax.servlet.include.query_string").toString());
				}
			}
			//logger.info("SessionId => " 	+ request.getSession().getId());
			//logger.info("Is Session Exist => "+ request.getSession().isNew());
			
			// url 정보 출력
			strGetUrlBuf.append(httprequest.getScheme());
			strGetUrlBuf.append("://");
			strGetUrlBuf.append(httprequest.getServerName());
			strGetUrlBuf.append(":"+httprequest.getServerPort());
			strGetUrlBuf.append(httprequest.getRequestURI());
			strGetUrlBuf.append("?");
			
			//   Parameter에 개인 정보가 너무 많이 남아있어서 Parameter는 찍지 않도록 변경한다.
			for (Enumeration names = request.getParameterNames(); names.hasMoreElements();) {
				String name = (String) names.nextElement();
				//String val = strUtil.getNullToEmpty(request.getParameter(name));
				//String val = EgovStringUtil.nullConvert(request.getParameter(name));
				String val = EgovStringUtil.nullConvert(StringEscapeUtils.escapeHtml(request.getParameter(name)));
	
				strGetUrlBuf.append(name);
				strGetUrlBuf.append("=");
				if (name.indexOf("loginPw") != -1 || name.indexOf("oldLoginPw") != -1 || name.indexOf("PASSPORT") != -1){
					strGetUrlBuf.append("******");
				}else {
					strGetUrlBuf.append(val);
				}
				strGetUrlBuf.append("&");
				logger.debug("Requested parameter => [" + name + "]:[" + val+"]");
			}
			
			if(strGetUrlBuf.toString().length() > 0) {
				String strUrlWithParameter = strGetUrlBuf.toString().substring(0, strGetUrlBuf.toString().length() - 1);
				logger.info("GET URL = " + strUrlWithParameter);
			}
			
			// Continue processing the rest of the filter chain.
			chain.doFilter(request, response);
		} finally {
			logger.info("*********************************** Service END ***********************************");
			
			try {
				ThreadContext.remove("LogPrimaryKey");
			} catch (Exception ex) {
				logger.error("",ex);
			}
		}
		
	}

	@Override
	public void destroy() {
		
	}

}
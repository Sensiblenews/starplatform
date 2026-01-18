package com.sensible.common.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sensible.common.Constants;

public class HTTPSFilter implements Filter{

	private static final Logger logger = LoggerFactory.getLogger(HTTPSFilter.class);

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		String method      = req.getMethod();
		String uri         = req.getRequestURI();
		String getProtocol = req.getScheme();
		String getDomain   = Constants.domain;
		String getPort     = Integer.toString(req.getServerPort());
		String referer     = req.getHeader("referer");

		
		String strPage = "";
		if(req.getServletPath().lastIndexOf("/") != -1) {
			strPage = req.getServletPath().substring(req.getServletPath().lastIndexOf("/")+1);
		} else {
			strPage = req.getServletPath();
		}
		logger.info("Use Page ===========>  "+ strPage );		
		
		
		if (getProtocol.toLowerCase().equals("http")) {
			
			
			String httpsPath = "https" + "://" + getDomain  + uri;
			logger.info("httpsPath=====>"+httpsPath);
			

			String site = new String(httpsPath);
			res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY); // 요청된 리소스가 임시로 새위치로 이동, 새로운 위치는 Location Header에 포함 
			res.setHeader("Location", site);
			res.sendRedirect(site);

		}
		
		chain.doFilter(req, res);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
}

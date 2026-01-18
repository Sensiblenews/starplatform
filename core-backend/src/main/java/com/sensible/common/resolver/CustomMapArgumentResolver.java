package com.sensible.common.resolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensible.common.domain.CommandMap;

/**
 * CumstomMap 으로 파라미터 받음. servlet 정의.
 * 
 * @author hhcho
 *
 */
public class CustomMapArgumentResolver implements HandlerMethodArgumentResolver {
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return CommandMap.class.isAssignableFrom(parameter.getParameterType());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		CommandMap commandMap = new CommandMap();

		HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

		String contentType = webRequest.getHeader("Content-Type");

		if (contentType != null && contentType.equalsIgnoreCase("application/json")) {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(Feature.AUTO_CLOSE_SOURCE, true);
			
			String body = getBody(request);
			System.out.println("body ::::::: "+body);
			//HashMap<String, Object> result = new ObjectMapper().readValue(body, HashMap.class);
			HashMap<String, Object> result = objectMapper.readValue(body, HashMap.class);
			Iterator<String> keys = result.keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next();
				commandMap.put(key, result.get(key));
			}
			return commandMap;
		} else {
			Enumeration<?> enumeration = request.getParameterNames();
			String key = null;
			String[] values = null;
			while (enumeration.hasMoreElements()) {
				key = (String) enumeration.nextElement();
				values = request.getParameterValues(key);
				if (values != null) {
					commandMap.put(key, (values.length > 1) ? values : values[0]);
				}
			}
			return commandMap;
		}
	}

	public static String getBody(HttpServletRequest request) throws IOException {
		String body = null;
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;

		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {
					throw ex;
				}
			}
		}
		body = stringBuilder.toString();
		return body;
	}
}
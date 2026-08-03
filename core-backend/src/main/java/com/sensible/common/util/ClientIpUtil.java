package com.sensible.common.util;

import javax.servlet.http.HttpServletRequest;

/**
 * 클라이언트 실제 IP 추출 유틸.
 * 운영 트래픽은 Cloudflare 엣지를 거쳐 들어오므로(docs/cloudflare-cache-rules.md 참고)
 * getRemoteAddr()만 쓰면 엣지 IP가 잡힌다. CF-Connecting-IP → X-Forwarded-For → getRemoteAddr 순으로 폴백한다.
 */
public class ClientIpUtil {

	private ClientIpUtil() {
	}

	public static String getClientIp(HttpServletRequest request) {
		// 1. Cloudflare가 부여하는 원 클라이언트 IP
		String ip = request.getHeader("CF-Connecting-IP");
		if (isValid(ip)) {
			return ip.trim();
		}

		// 2. 일반 프록시 체인: 첫 번째 값이 원 클라이언트
		ip = request.getHeader("X-Forwarded-For");
		if (isValid(ip)) {
			int comma = ip.indexOf(',');
			return (comma > -1 ? ip.substring(0, comma) : ip).trim();
		}

		// 3. 직접 연결
		return request.getRemoteAddr();
	}

	private static boolean isValid(String ip) {
		return ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim());
	}
}

package com.sensible.common.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sensible.admin.domain.UserVO;

/**
 * 슈퍼 어드민 로그인 세션·토큰 발급을 한 곳에 모은다.
 *
 * 로그인 컨트롤러(비이중인증 경로)와 OTP 컨트롤러(2차 인증 성공)가 같은 방식으로
 * 세션과 SUPER_TOKEN 쿠키를 만들어야 하므로 분리했다. "최종 관리자 권한"은 이 메서드를
 * 통과한 뒤에만 생긴다.
 */
public final class AdminSessionUtil {

	public static final String SESSION_USER = "SUPER_USER_SESSION";
	public static final String TOKEN_COOKIE = "SUPER_TOKEN";

	/** JwtUtil.EXPIRATION_TIME(2시간)과 맞춘 쿠키 수명(초) */
	private static final int TOKEN_MAX_AGE_SEC = 7200;

	/**
	 * 계정별 "이 시각 이전에 발급된 토큰은 무효" 기준점.
	 * JWT 는 서버에 상태가 없어 그냥은 회수할 수 없다. OTP 재설정처럼 기존 세션을 끊어야 하는 조치가 있을 때
	 * 여기 시각을 찍어 두고 인터셉터가 발급 시각(iat)과 비교한다.
	 * JVM 메모리에만 있으므로 WAS 재기동 시 사라진다 — 토큰 수명이 2시간이라 재기동 후 최대 2시간 창이 남는다.
	 */
	private static final java.util.concurrent.ConcurrentHashMap<String, Long> REVOKED_BEFORE =
			new java.util.concurrent.ConcurrentHashMap<String, Long>();

	private AdminSessionUtil() {
	}

	/** 지금 이전에 발급된 이 계정의 토큰을 전부 무효화한다 */
	public static void revokeTokens(String prsId) {
		if (prsId != null) {
			REVOKED_BEFORE.put(prsId, System.currentTimeMillis());
		}
	}

	/** 토큰 발급 시각이 회수 기준점 이전이면 true */
	public static boolean isRevoked(String prsId, long issuedAtMillis) {
		if (prsId == null) {
			return false;
		}
		Long before = REVOKED_BEFORE.get(prsId);
		// iat 는 초 단위로 잘려 저장되므로 같은 초 안의 발급은 무효로 본다 (안전한 쪽으로)
		return before != null && issuedAtMillis >= 0 && issuedAtMillis < before + 1000L;
	}

	/** 세션 저장 + JWT 쿠키 발급. 반환값은 권한별 첫 화면 */
	public static String establish(HttpServletRequest request, HttpServletResponse response, UserVO user) {
		request.getSession().setAttribute(SESSION_USER, user);
		String token = JwtUtil.generateToken(user.getPRS_ID(), user.getPRS_AUTH());
		// SameSite=Lax: 외부 사이트에서 시작된 POST 에 쿠키가 실리지 않게 한다 (CSRF 완화)
		response.addHeader("Set-Cookie", TOKEN_COOKIE + "=" + token
				+ "; Path=/; Max-Age=" + TOKEN_MAX_AGE_SEC + "; HttpOnly; SameSite=Lax");
		return "ST".equals(user.getPRS_AUTH()) ? "/star/mypage.do" : "/super/dashboard.do";
	}

	/** 세션 무효화 + 쿠키 제거 */
	public static void clear(HttpServletRequest request, HttpServletResponse response) {
		try {
			request.getSession().invalidate();
		} catch (IllegalStateException ignore) {
			// 이미 무효화됨
		}
		response.addHeader("Set-Cookie", TOKEN_COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
	}

	public static UserVO currentUser(HttpServletRequest request) {
		Object u = request.getSession().getAttribute(SESSION_USER);
		return (u instanceof UserVO) ? (UserVO) u : null;
	}
}

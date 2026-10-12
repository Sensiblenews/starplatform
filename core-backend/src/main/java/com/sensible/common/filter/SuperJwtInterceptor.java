package com.sensible.common.filter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession; // 추가됨

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.sensible.admin.domain.UserVO; // 추가됨
import com.sensible.common.util.JwtUtil;

public class SuperJwtInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        String requestURI = request.getRequestURI();

        // 예외 경로 (.do 포함)
        if (requestURI.endsWith("/super/login.do") ||
            requestURI.endsWith("/super/loginPrc.do") ||
            requestURI.endsWith("/super/logout.do") ||
            requestURI.endsWith("/super/test.do")) {
            return true;
        }

        // OTP 2차 인증·등록 경로. 토큰은 아직 없고(1차 인증만 통과) 세션의 MfaPendingVO 로만 식별한다.
        // 대기 상태 검증은 SuperMfaController 가 한다. 관리자 데이터에 닿는 경로는 여기 없다.
        if (requestURI.endsWith("/super/mfa/verify.do") ||
            requestURI.endsWith("/super/mfa/verifyPrc.do") ||
            requestURI.endsWith("/super/mfa/setup.do") ||
            requestURI.endsWith("/super/mfa/setupStart.do") ||
            requestURI.endsWith("/super/mfa/setupConfirm.do")) {
            return true;
        }

        // 1. 쿠키에서 토큰 찾기
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("SUPER_TOKEN".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        // 2. 토큰 검증 및 세션 복구
        if (token != null && JwtUtil.validateToken(token)) {

            // OTP 재설정 등으로 회수된 토큰이면 세션까지 비우고 로그인으로 보낸다
            String tokenUser = JwtUtil.getUserIdFromToken(token);
            if (com.sensible.common.util.AdminSessionUtil.isRevoked(tokenUser, JwtUtil.getIssuedAtMillis(token))) {
                com.sensible.common.util.AdminSessionUtil.clear(request, response);
                response.sendRedirect("/witch/super/login.do");
                return false;
            }

            // [핵심 로직] 세션이 날아갔으면, 토큰을 보고 다시 살려낸다! (Session Restore)
            HttpSession session = request.getSession();
            UserVO user = (UserVO) session.getAttribute("SUPER_USER_SESSION");
            
            if (user == null) {
                String userId = JwtUtil.getUserIdFromToken(token);
                String auth = JwtUtil.getAuthFromToken(token); // JwtUtil에 이 메소드가 있어야 함
                
                // 임시 유저 객체 생성 및 세션 주입
                UserVO restoredUser = new UserVO();
                restoredUser.setPRS_ID(userId);
                restoredUser.setPRS_AUTH(auth);
                
                session.setAttribute("SUPER_USER_SESSION", restoredUser);
                System.out.println(">>> [Interceptor] 세션이 복구되었습니다: " + userId);
            }
            
            return true; // 통과
        }

        // 3. 실패 시 로그인으로 리다이렉트
        response.sendRedirect("/witch/super/login.do"); 
        return false; 
    }
}
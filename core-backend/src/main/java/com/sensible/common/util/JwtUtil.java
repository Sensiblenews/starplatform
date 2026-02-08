package com.sensible.common.util;

import java.util.Date;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtil {
    // 비밀 키 (실무에선 복잡하게 설정하고 절대 노출 금지)
    private static final String SECRET_KEY = "WitchHuntingSuperAppSecretKeyWitchHuntingSuperAppSecretKey";
    
    // 토큰 유효 시간 (예: 2시간)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 2; 

    // 1. 토큰 생성 (로그인 성공 시 호출)
    public static String generateToken(String userId, String auth) {
        return Jwts.builder()
                .setSubject(userId) // 누구 토큰인지
                .claim("auth", auth) // 권한 정보 (SM 등)
                .setIssuedAt(new Date()) // 언제 만들었는지
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 언제 만료되는지
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY) // 서명
                .compact();
    }

    // 2. 토큰 검증 (인터셉터가 호출)
    public static boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // 유효기간 만료, 위조된 토큰 등 모든 에러 처리
            return false;
        }
    }

    // 3. 토큰에서 ID 꺼내기
    public static String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // [추가됨] 토큰에서 권한(Auth) 꺼내기 (세션 복구용 필수 메서드)
    public static String getAuthFromToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
            return claims.get("auth", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
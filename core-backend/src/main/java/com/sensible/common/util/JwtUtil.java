package com.sensible.common.util;

import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * 어드민(SM/LC/ST) 토큰 발급·검증.
 *
 * 서명 키는 소스에 두지 않고 설정에서 읽는다 (2026-10-13 — 이전 상수 키는 공개 저장소 이력에 남아 있어 교체했다).
 * 읽는 순서: 환경변수 WITCH_JWT_SECRET → 시스템 속성 witch.jwt.secret → globals.properties 의 security.jwt.secret.
 * 값은 base64 문자열이어야 한다 (jjwt 0.9 의 signWith(alg, String) 은 base64 로 해석). 생성: openssl rand -base64 64
 *
 * 키가 없으면 발급은 예외, 검증은 false 다 (fail-closed). 키 없이 기본값으로 서명하면 같은 취약점이 되살아나므로
 * 관리자 로그인이 막히는 쪽을 택했다. 배포 순서는 "globals 에 키 추가 → WAR". 키를 바꾸면 기존 관리자 토큰은 전부 무효다.
 */
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    public static final String CONFIG_KEY = "security.jwt.secret";
    public static final String ENV_KEY = "WITCH_JWT_SECRET";
    public static final String SYSPROP_KEY = "witch.jwt.secret";
    private static final String GLOBALS_PATH = "META-INF/props/globals.properties";

    // 토큰 유효 시간 (2시간)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 2;

    /** 로드된 키. null 이면 아직 안 읽었거나 설정이 없다. 설정이 없을 때 매 호출마다 다시 읽지 않도록 missing 을 따로 둔다 */
    private static volatile String secret;
    private static volatile boolean missing;

    /** 설정에서 키를 읽는다. 없으면 null. 한 번 읽은 결과는 유지한다 */
    static String secretKey() {
        String s = secret;
        if (s != null) {
            return s;
        }
        if (missing) {
            return null;
        }
        synchronized (JwtUtil.class) {
            if (secret != null) {
                return secret;
            }
            String loaded = load();
            if (loaded == null) {
                missing = true;
                logger.error("[JWT] " + CONFIG_KEY + " 미설정 — 관리자 토큰을 발급·검증할 수 없어 관리자 로그인이 실패한다. "
                        + "globals.properties 에 " + CONFIG_KEY + " (base64, openssl rand -base64 64) 를 넣고 재기동할 것");
                return null;
            }
            secret = loaded;
            return loaded;
        }
    }

    private static String load() {
        String v = trimToNull(System.getenv(ENV_KEY));
        if (v == null) {
            v = trimToNull(System.getProperty(SYSPROP_KEY));
        }
        if (v == null) {
            Properties p = new Properties();
            InputStream in = null;
            try {
                in = JwtUtil.class.getClassLoader().getResourceAsStream(GLOBALS_PATH);
                if (in != null) {
                    p.load(in);
                    v = trimToNull(p.getProperty(CONFIG_KEY));
                }
            } catch (Exception e) {
                logger.warn("[JWT] globals.properties 읽기 실패: {}", e.toString());
            } finally {
                if (in != null) {
                    try { in.close(); } catch (Exception ignore) { }
                }
            }
        }
        return v;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 테스트 전용: 캐시를 비워 다음 호출에서 다시 읽게 한다 */
    static void resetForTest() {
        synchronized (JwtUtil.class) {
            secret = null;
            missing = false;
        }
    }

    // 1. 토큰 생성 (로그인 성공 시 호출). 키가 없으면 예외 — 호출부가 로그인 실패로 처리한다
    public static String generateToken(String userId, String auth) {
        String key = secretKey();
        if (key == null) {
            throw new IllegalStateException("JWT signing key is not configured (" + CONFIG_KEY + ")");
        }
        return Jwts.builder()
                .setSubject(userId) // 누구 토큰인지
                .claim("auth", auth) // 권한 정보 (SM 등)
                .setIssuedAt(new Date()) // 언제 만들었는지
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 언제 만료되는지
                .signWith(SignatureAlgorithm.HS512, key) // 서명
                .compact();
    }

    private static Claims parse(String token) {
        String key = secretKey();
        if (key == null) {
            return null;
        }
        return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
    }

    // 2. 토큰 검증 (인터셉터가 호출)
    public static boolean validateToken(String token) {
        try {
            return parse(token) != null;
        } catch (Exception e) {
            // 유효기간 만료, 위조된 토큰 등 모든 에러 처리
            return false;
        }
    }

    // 3. 토큰에서 ID 꺼내기
    public static String getUserIdFromToken(String token) {
        try {
            Claims claims = parse(token);
            return (claims != null) ? claims.getSubject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // 토큰 발급 시각(ms). OTP 재설정 등으로 "이 시각 이전 토큰은 무효" 판정에 쓴다. 파싱 실패 시 -1
    public static long getIssuedAtMillis(String token) {
        try {
            Claims claims = parse(token);
            Date iat = (claims != null) ? claims.getIssuedAt() : null;
            return (iat != null) ? iat.getTime() : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    // [추가됨] 토큰에서 권한(Auth) 꺼내기 (세션 복구용 필수 메서드)
    public static String getAuthFromToken(String token) {
        try {
            Claims claims = parse(token);
            return (claims != null) ? claims.get("auth", String.class) : null;
        } catch (Exception e) {
            return null;
        }
    }
}

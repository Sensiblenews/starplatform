package com.sensible.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.sensible.common.util.ClientIpUtil;

/**
 * 웹 랜딩(content_landing.jsp) 방문 카운팅 서비스.
 *
 * 앱 미설치자가 SNS 공유 링크로 랜딩에 진입해 2.5초 이상 실제 체류한 경우에만
 * WH_AD_LOG에 IMPRESSION을 기록한다(앱 방문자 수·어드민 광고 시청 수와 동일 집계).
 * SNS 썸네일 크롤러와 봇·재사용·중복 요청은 서버에서 걸러낸다.
 *
 * 검증 구조:
 *  - 토큰: 랜딩 렌더링 시 서버가 PRS_ID를 바인딩한 HMAC 토큰을 발급하고,
 *    제출 시 서명·발급 경과시간(2.5초 이상 ~ 30분 이내)을 검증한다.
 *  - Redis: 토큰 1회성(nonce), IP 분당 rate limit, 방문자 24시간 중복을 차단한다.
 *    Redis 장애 시에는 기존 insertAdLog의 Redis 실패 무시 정책과 동일하게 fail-open으로
 *    카운트를 진행한다(서명·시간 검증은 Redis와 무관하게 유지되는 최소 방어선).
 */
@Service("landingVisitService")
public class LandingVisitService {

	@Resource(name = "redisTemplate")
	private RedisTemplate<String, Object> redisTemplate;

	@Resource(name = "superAppService")
	private SuperAppService superAppService;

	/**
	 * 토큰 서명 시크릿. 단일 인스턴스 운영 전제로 기동 시 생성한다(설정 파일 수정 불필요).
	 * 재배포 시 리셋되지만 토큰 유효창이 30분이라 영향은 미소비 토큰 극소수 무효화에 그친다.
	 * 다중 인스턴스로 확장하면 globals.properties의 공유 시크릿으로 이관할 것.
	 */
	private static final byte[] SECRET = new byte[32];

	private static final SecureRandom RANDOM = new SecureRandom();

	static {
		RANDOM.nextBytes(SECRET);
	}

	/** 최소 체류 시간(ms) — 클라이언트는 2600ms 후 제출하므로 정상 유저는 항상 통과 */
	private static final long MIN_DWELL_MS = 2500L;

	/** 토큰 유효창(ms) */
	private static final long TOKEN_TTL_MS = 30L * 60 * 1000;

	/** 동일 IP 분당 허용 요청 수 */
	private static final long RATE_LIMIT_PER_MIN = 30L;

	/**
	 * SNS 썸네일 크롤러·봇 판정 정규식.
	 * "bot" 서브스트링이 일부 기기명(Cubot 등)을 오탐할 수 있으나 극소수 과소집계로 허용한다.
	 */
	private static final Pattern BOT_UA = Pattern.compile(
			"(?i)(bot|crawl|spider|slurp|scraper|facebookexternalhit|meta-externalagent"
					+ "|kakaotalk-scrap|telegrambot|whatsapp|slackbot|pinterestbot|linkedinbot"
					+ "|line-poker|discordbot|vkshare|embedly|skypeuripreview|preview)");

	private static final Pattern VISITOR_ID_FORMAT = Pattern.compile("[0-9a-fA-F\\-]{8,64}");

	/** UA가 없거나 크롤러/봇 패턴이면 true */
	public static boolean isCrawler(String userAgent) {
		return userAgent == null || BOT_UA.matcher(userAgent).find();
	}

	/**
	 * 랜딩 렌더링 시 발급하는 방문 토큰.
	 * 형식: base64url(prsId|issuedAt|nonce) + "." + base64url(HMAC-SHA256)
	 * 문자셋이 base64url + '.'로 한정되므로 JSP EL로 출력해도 이스케이프가 필요 없다.
	 */
	public String issueToken(String prsId) {
		byte[] nonceBytes = new byte[12];
		RANDOM.nextBytes(nonceBytes);
		String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

		String payload = prsId + "|" + System.currentTimeMillis() + "|" + nonce;
		String payloadB64 = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		return payloadB64 + "." + sign(payloadB64);
	}

	/**
	 * 방문 카운트 요청 검증 및 기록.
	 * 거부 사유와 무관하게 200 + result:"IGNORED"로 응답해 공격자에게 판정 신호를 주지 않는다.
	 */
	public Map<String, Object> recordVisit(Map<String, Object> params, HttpServletRequest request) {
		Map<String, Object> ignored = new HashMap<>();
		ignored.put("result", "IGNORED");

		// 1. UA 봇 필터
		if (isCrawler(request.getHeader("User-Agent"))) {
			return ignored;
		}

		Object tokenObj = params.get("token");
		Object visitorIdObj = params.get("visitorId");
		if (!(tokenObj instanceof String) || !(visitorIdObj instanceof String)) {
			return ignored;
		}
		String token = (String) tokenObj;
		String visitorId = (String) visitorIdObj;

		if (!VISITOR_ID_FORMAT.matcher(visitorId).matches()) {
			return ignored;
		}

		// 2. 토큰 서명 검증
		int dot = token.indexOf('.');
		if (dot <= 0 || dot == token.length() - 1) {
			return ignored;
		}
		String payloadB64 = token.substring(0, dot);
		String sig = token.substring(dot + 1);
		if (!MessageDigest.isEqual(sign(payloadB64).getBytes(StandardCharsets.UTF_8),
				sig.getBytes(StandardCharsets.UTF_8))) {
			return ignored;
		}

		String prsId;
		long issuedAt;
		String nonce;
		try {
			String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
			String[] parts = payload.split("\\|");
			if (parts.length != 3) {
				return ignored;
			}
			prsId = parts[0];
			issuedAt = Long.parseLong(parts[1]);
			nonce = parts[2];
		} catch (Exception e) {
			return ignored;
		}

		// 3. 발급 경과시간 = 서버가 보증하는 체류 하한 (2.5초 미만이면 봇/직접 호출)
		long elapsed = System.currentTimeMillis() - issuedAt;
		if (elapsed < MIN_DWELL_MS || elapsed > TOKEN_TTL_MS) {
			return ignored;
		}

		// 4~6. Redis 검증 (장애 시 fail-open)
		try {
			// 4. 토큰 1회성: 동일 토큰 재제출 차단
			Boolean firstUse = redisTemplate.opsForValue().setIfAbsent("landing:token:" + nonce, "1");
			if (Boolean.FALSE.equals(firstUse)) {
				return ignored;
			}
			redisTemplate.expire("landing:token:" + nonce, TOKEN_TTL_MS, TimeUnit.MILLISECONDS);

			// 5. IP rate limit (분당)
			String ipKey = "landing:ip:" + ClientIpUtil.getClientIp(request);
			Long count = redisTemplate.opsForValue().increment(ipKey, 1L);
			if (count != null && count == 1L) {
				redisTemplate.expire(ipKey, 60, TimeUnit.SECONDS);
			}
			if (count != null && count > RATE_LIMIT_PER_MIN) {
				return ignored;
			}

			// 6. 동일 방문자 24시간 중복 차단 (IP 병행 키는 쓰지 않는다 —
			//    캐리어 NAT에서 서로 다른 방문자가 같은 IP로 잡혀 과소집계되는 위험이
			//    localStorage 초기화 어뷰징 위험보다 크고, 상한은 5번 rate limit이 담당)
			Boolean firstVisit = redisTemplate.opsForValue()
					.setIfAbsent("landing:visit:" + prsId + ":" + visitorId, "1");
			if (Boolean.FALSE.equals(firstVisit)) {
				return ignored;
			}
			redisTemplate.expire("landing:visit:" + prsId + ":" + visitorId, 24, TimeUnit.HOURS);
		} catch (Exception e) {
			System.out.println("Landing visit Redis check skipped (fail-open): " + e.getMessage());
		}

		// 7. 기존 집계 경로 재사용 — 앱 방문자 수/어드민 광고 시청 수/스타 푸시/랭킹 ZSET에 동일 반영
		Map<String, Object> adLog = new HashMap<>();
		adLog.put("MEM_ID", "WEB");
		adLog.put("PRS_ID", prsId);
		adLog.put("AD_TYPE", "LANDING");
		adLog.put("OS", "WEB");
		adLog.put("ACTION", "IMPRESSION");
		try {
			superAppService.insertAdLog(adLog);
		} catch (Exception e) {
			e.printStackTrace();
			return ignored;
		}

		Map<String, Object> result = new HashMap<>();
		result.put("result", "OK");
		return result;
	}

	private String sign(String payloadB64) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
			byte[] raw = mac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		} catch (Exception e) {
			// HmacSHA256은 JRE 필수 알고리즘이라 사실상 도달 불가
			throw new IllegalStateException("HMAC signing failed", e);
		}
	}
}

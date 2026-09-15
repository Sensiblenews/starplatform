package com.sensible.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * 캐시 오류를 삼키고 원본 메서드로 내려가게 하는 핸들러 (2-29차).
 *
 * 기본 동작(SimpleCacheErrorHandler)은 캐시 접근 예외를 그대로 던진다. 이 프로젝트는
 * 캐시가 Redis에 있으므로, Redis가 잠깐만 끊겨도 @Cacheable 이 붙은 모든 API가
 * 500을 낸다. 랭킹·로비처럼 사람이 제일 먼저 보는 화면이 전부 여기에 해당한다.
 *
 * 캐시는 속도를 위한 사본이고 원본은 DB다. 사본을 못 읽으면 원본을 읽으면 되는데,
 * 지금까지는 요청 자체가 실패했다. 이 핸들러를 끼우면 캐시 실패 시 로그만 남기고
 * 원본 메서드를 실행한다 — 느려지지만 동작한다.
 *
 * 대가: Redis 장애 구간에는 캐시가 막아주던 쿼리가 전부 DB로 내려간다. 500을 내는
 * 것보다는 낫다는 판단이고, 장애가 길어지면 DB 부하를 함께 봐야 한다.
 *
 * 로그는 남기되 스택트레이스는 WARN 한 줄로 줄인다. Redis가 끊긴 구간에는 모든 요청이
 * 여기를 지나므로 스택을 다 찍으면 그게 또 로그 폭주가 된다.
 */
public class CacheFailSoftHandler implements CacheErrorHandler {

	private static final Logger logger = LoggerFactory.getLogger(CacheFailSoftHandler.class);

	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		logger.warn("[CACHE] 조회 실패 — 원본으로 내려간다 (cache=" + cacheName(cache)
				+ ", key=" + key + "): " + exception.getMessage());
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
		logger.warn("[CACHE] 저장 실패 — 결과는 정상 반환한다 (cache=" + cacheName(cache)
				+ ", key=" + key + "): " + exception.getMessage());
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		// 무효화 실패는 오래된 값이 남는다는 뜻이라 조회 실패보다 위험하다
		logger.warn("[CACHE] 무효화 실패 — 오래된 값이 남을 수 있다 (cache=" + cacheName(cache)
				+ ", key=" + key + "): " + exception.getMessage());
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		logger.warn("[CACHE] 전체 비우기 실패 (cache=" + cacheName(cache) + "): " + exception.getMessage());
	}

	private String cacheName(Cache cache) {
		return cache == null ? "?" : cache.getName();
	}
}

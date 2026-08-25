package com.sensible.api.service;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sensible.common.dao.DefaultDAO;

/**
 * 전체 스타 글로벌 순위표 캐시 (2-26차).
 *
 * 스타 상세 API가 매 호출마다 WH_AD_LOG 전체를 GROUP BY하고 전체 스타를 ROW_NUMBER로
 * 매긴 뒤 한 행만 꺼내고 있었다. 스타 페이지를 열 때마다 이 비용이 들어 Android에서
 * 첫 화면이 눈에 띄게 늦었다. 순위표를 한 번에 뽑아 Redis에 캐시해 재사용한다.
 *
 * SuperAppService 안이 아니라 별도 빈으로 둔 이유:
 * 같은 빈 안에서 this로 호출하면 프록시를 타지 않아 @Cacheable이 무시된다.
 */
@Service("starRankService")
public class StarRankService {

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	/**
	 * 전체 스타의 순위·조회수 목록. TTL 60초 (context-redis.xml의 starRank).
	 * 캐시 미스일 때만 순위표를 다시 계산한다.
	 */
	@Cacheable(value = "starRank", key = "'map'", unless = "#result == null")
	public List<Map<String, Object>> getGlobalRankMap() {
		return dao.selectList("superapp.selectGlobalRankMap");
	}

	/**
	 * 캐시를 거치지 않고 직접 계산한다.
	 * Redis 장애로 캐시 조회가 실패했을 때의 폴백 — 비용은 캐시 도입 이전과 같다.
	 */
	public List<Map<String, Object>> getGlobalRankMapUncached() {
		return dao.selectList("superapp.selectGlobalRankMap");
	}
}

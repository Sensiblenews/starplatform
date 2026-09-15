package com.sensible.api.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.sensible.common.dao.DefaultDAO;

/**
 * 오늘 랭킹 실시간 반영 (2-29차).
 *
 * 문제: 오늘 클릭수 랭킹(dailyLeaderboard)은 TTL 600초, 역대 오늘의 왕(hallOfFame)은
 * TTL 43200초(12시간) 캐시에 들어가 있었고, 방문·클릭이 캐시를 비우지 않았다.
 * 그래서 "클릭하면 바로 순위에 반영"이 구조적으로 불가능했다.
 * 기존에 insertAdLog가 `leaderboard:global` ZSET에 점수를 가산하고 있었지만
 * 그 키를 읽는 코드가 어디에도 없어 쓰기만 버려지고 있었다.
 *
 * 해결: 날짜별 ZSET(rank:daily:yyyy-MM-dd)을 오늘 집계의 실시간 사본으로 쓴다.
 *  - 쓰기: IMPRESSION 이 DB에 들어간 직후 ZINCRBY (insertAdLog)
 *  - 읽기: ZSET 상위 N + 스타 메타데이터(PK IN 조회)로 응답을 조립
 *  - 원본은 어디까지나 DB(WH_AD_LOG)다. ZSET 이 없거나 비면 DB 집계로 다시 채운다.
 *
 * 정합성: DB 입력이 ZINCRBY보다 먼저 일어나므로, DB 기준으로 다시 맞추면(rebuild)
 * 그때까지의 모든 증가분이 이미 DB에 반영돼 있다. 따라서 절대값 ZADD로 덮어써도
 * 값이 줄지 않는다 — 어긋남이 생겨도 다음 rebuild에서 스스로 회복한다.
 * (SELECT와 ZADD 사이 1초 미만의 증가분만 유실 가능하고, 다음 rebuild가 메운다)
 *
 * Redis 장애: 모든 메서드가 null 또는 무동작으로 빠지고, 호출부는 기존 DB 경로를
 * 그대로 쓴다. 즉 최악의 경우 이 기능 도입 이전과 같은 동작이 된다.
 */
@Service("dailyRankService")
public class DailyRankService {

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	@Resource(name = "redisTemplate")
	private RedisTemplate<String, Object> redisTemplate;

	/** 날짜별 랭킹 ZSET 키 접두어 */
	static final String KEY_PREFIX = "rank:daily:";
	/** 시딩 완료 표식 접미어. 이게 없으면 읽기 시점에 DB로 채운다 */
	static final String SEEDED_SUFFIX = ":seeded";

	/** ZSET·표식 보관 기간. 오늘 것만 쓰므로 이틀이면 충분하고 자동으로 사라진다 */
	private static final int TTL_HOURS = 48;

	/** 재동기화 최소 간격. 이 간격보다 자주 요청되면 건너뛴다 */
	private static final int RESYNC_INTERVAL_SEC = 300;

	// ==========================================
	// 키 조립 (부수효과 없어 그대로 단위 테스트한다)
	// ==========================================

	static String dailyKey(String date) {
		return KEY_PREFIX + date;
	}

	static String seededKey(String date) {
		return KEY_PREFIX + date + SEEDED_SUFFIX;
	}

	/** 서버 기준 오늘 날짜. DB의 CURDATE()와 같은 타임존을 전제로 한다 */
	static String today() {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	}

	/**
	 * 요청된 날짜가 오늘인지. 값이 비어 있으면 오늘로 본다
	 * (selectDailyLeaderboard가 date 미지정 시 CURDATE() 기준이므로 동일 규칙).
	 */
	static boolean isToday(Object rawDate) {
		if (rawDate == null) {
			return true;
		}
		String date = String.valueOf(rawDate).trim();
		return date.isEmpty() || date.equals(today());
	}

	// ==========================================
	// 쓰기
	// ==========================================

	/**
	 * 오늘 집계에 1을 더한다. IMPRESSION 이 DB에 저장된 뒤에 호출할 것.
	 * 실패는 삼킨다 — 원본은 DB이고, 다음 rebuild가 값을 맞춘다.
	 */
	public void incrementToday(String starId) {
		if (starId == null || starId.trim().isEmpty()) {
			return;
		}
		try {
			String date = today();
			String key = dailyKey(date);
			redisTemplate.opsForZSet().incrementScore(key, starId, 1.0);
			// 키가 처음 생긴 경우를 대비해 매번 만료를 재설정한다 (ZINCRBY는 TTL을 붙이지 않는다)
			redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
		} catch (Exception e) {
			System.out.println("[RANK] 오늘 랭킹 가산 실패 (DB는 정상): " + e.getMessage());
		}
	}

	// ==========================================
	// 읽기
	// ==========================================

	/**
	 * 오늘 랭킹 상위 목록. 프런트가 쓰는 필드(id·name·image·displayViewCount·globalRank)를
	 * 갖춘 맵 목록을 돌려준다. 정렬은 ZSET이, 나머지 정보는 DB가 채운다.
	 *
	 * @return Redis를 쓸 수 없으면 null — 호출부는 기존 DB 집계로 폴백한다
	 */
	public List<Map<String, Object>> getTodayRanking(int limit) {
		if (limit <= 0) {
			return new ArrayList<Map<String, Object>>();
		}

		LinkedHashMap<String, Double> scores = readTopScores(limit);
		if (scores == null) {
			return null;
		}
		if (scores.isEmpty()) {
			// 오늘 방문이 아직 없는 정상 상태
			return new ArrayList<Map<String, Object>>();
		}

		List<Map<String, Object>> metaList;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("ids", new ArrayList<String>(scores.keySet()));
			metaList = dao.selectList("superapp.selectStarsByIds", param);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return assembleRanking(scores, metaList);
	}

	/**
	 * ZSET 점수와 스타 메타데이터를 합쳐 랭킹 목록을 만든다.
	 * 순위는 ZSET 순서 그대로 1부터 매긴다. 메타데이터가 없는 id(비활성 전환 등)는 건너뛰므로
	 * 순위에 구멍이 생기지 않는다.
	 *
	 * 부수효과가 없어 그대로 단위 테스트한다.
	 */
	static List<Map<String, Object>> assembleRanking(LinkedHashMap<String, Double> scores,
			List<Map<String, Object>> metaList) {
		Map<String, Map<String, Object>> metaById = new HashMap<String, Map<String, Object>>();
		if (metaList != null) {
			for (Map<String, Object> meta : metaList) {
				Object id = meta.get("id");
				if (id != null) {
					metaById.put(String.valueOf(id), meta);
				}
			}
		}

		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		int rank = 0;
		for (Map.Entry<String, Double> entry : scores.entrySet()) {
			Map<String, Object> meta = metaById.get(entry.getKey());
			if (meta == null) {
				continue;
			}
			rank++;

			Map<String, Object> row = new HashMap<String, Object>(meta);
			long viewCount = (long) Math.floor(entry.getValue() == null ? 0d : entry.getValue());
			row.put("viewCount", viewCount);
			// 프런트(daily-ranking-modal)가 읽는 필드명
			row.put("displayViewCount", viewCount);
			row.put("globalRank", rank);
			// 기존 응답과의 하위 호환 (대문자 키)
			row.put("GLOBAL_RANK", rank);
			result.add(row);
		}
		return result;
	}

	/**
	 * 오늘의 1위. 역대 오늘의 왕 목록 맨 앞에 얹는다.
	 *
	 * @return 오늘 방문이 없으면 빈 목록, Redis를 쓸 수 없으면 null
	 */
	public Map<String, Object> getTodayKing() {
		List<Map<String, Object>> top = getTodayRanking(1);
		if (top == null) {
			return null;
		}
		if (top.isEmpty()) {
			return new HashMap<String, Object>();
		}

		Map<String, Object> first = top.get(0);
		Map<String, Object> king = new HashMap<String, Object>();
		// 역대 오늘의 왕 화면이 쓰는 필드만 맞춘다 (rankDate·id·name·image·viewCount)
		king.put("rankDate", today());
		king.put("id", first.get("id"));
		king.put("name", first.get("name"));
		king.put("image", first.get("image"));
		king.put("viewCount", first.get("viewCount"));
		return king;
	}

	/**
	 * 어제까지의 오늘의 왕. 이미 지난 날짜의 결과는 더 바뀌지 않으므로 12시간 캐시가 안전하다.
	 * (오늘 행은 getTodayKing이 실시간으로 얹는다)
	 *
	 * 별도 빈에 둔 이유는 StarRankService와 같다 — 같은 빈에서 this로 부르면
	 * 프록시를 타지 않아 @Cacheable이 무시된다.
	 */
	@Cacheable(value = "hallOfFame", key = "'dailyKingsPast:' + #params.toString()", unless = "#result == null")
	public List<Map<String, Object>> getPastDailyKings(Map<String, Object> params) {
		return dao.selectList("superapp.selectDailyKingsBeforeToday", params);
	}

	/**
	 * 오늘의 왕을 DB에서 직접 뽑는다. Redis를 쓸 수 없을 때만 쓰는 폴백이다.
	 * 이게 없으면 Redis 장애 구간에 오늘 행이 목록에서 통째로 빠져, 변경 전보다 나빠진다.
	 */
	public Map<String, Object> getTodayKingFromDb() {
		try {
			List<Map<String, Object>> rows = dao.selectList("superapp.selectTodayKing");
			if (rows == null || rows.isEmpty()) {
				return new HashMap<String, Object>();
			}
			return rows.get(0);
		} catch (Exception e) {
			e.printStackTrace();
			return new HashMap<String, Object>();
		}
	}

	/**
	 * 지난 날짜의 클릭수 랭킹. 이미 지난 날은 결과가 바뀌지 않으므로 기존 캐시(TTL 600초)를
	 * 그대로 쓴다. 오늘 분은 getTodayRanking이 실시간으로 처리한다.
	 *
	 * 이 메서드가 SuperAppService가 아니라 여기 있는 이유: 같은 빈 안에서 this로 호출하면
	 * 프록시를 타지 않아 @Cacheable이 조용히 무시된다 (StarRankService의 주석과 같은 이유).
	 */
	@Cacheable(value = "dailyLeaderboard", key = "'dailyLeaderboard:' + #params.toString()", unless = "#result == null")
	public List<Map<String, Object>> getDailyLeaderboardByDate(Map<String, Object> params) {
		return dao.selectList("superapp.selectDailyLeaderboard", params);
	}

	/**
	 * 캐시를 거치지 않는 집계. 오늘 분의 폴백 전용이다.
	 *
	 * 오늘 값을 캐시 경로로 폴백시키면, ZSET이 잠깐 실패한 사이 최대 10분 묵은 값이
	 * 나가고 그게 또 10분간 굳는다. 오늘만큼은 느려도 방금 값을 주는 쪽이 맞다.
	 * (StarRankService.getGlobalRankMapUncached와 같은 취지)
	 */
	public List<Map<String, Object>> getTodayLeaderboardUncached(Map<String, Object> params) {
		return dao.selectList("superapp.selectDailyLeaderboard", params);
	}

	// ==========================================
	// 시딩 / 재동기화
	// ==========================================

	/**
	 * ZSET 상위 점수를 읽는다. 시딩이 안 됐으면 DB로 채운 뒤 읽는다.
	 *
	 * @return Redis 실패 시 null
	 */
	private LinkedHashMap<String, Double> readTopScores(int limit) {
		String date = today();
		try {
			ensureSeeded(date);
			// DB 기준 재동기화. 자체 간격 제한(5분)이 있어 매 요청마다 돌지 않는다.
			// 스케줄러 크론으로 두지 않은 이유: 스프링 기본 스케줄러는 스레드 풀이 1개라
			// 기존 수집 작업(SystemMetricsCollector·PageGeneratorScheduler)과 겹치면 서로 밀린다.
			// 읽는 사람이 있을 때만 도는 지연 방식이 이 규모에는 충분하다.
			resyncToday();

			Set<ZSetOperations.TypedTuple<Object>> tuples =
					redisTemplate.opsForZSet().reverseRangeWithScores(dailyKey(date), 0, limit - 1);
			LinkedHashMap<String, Double> scores = new LinkedHashMap<String, Double>();
			if (tuples == null) {
				return scores;
			}
			for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
				if (tuple.getValue() != null) {
					scores.put(String.valueOf(tuple.getValue()), tuple.getScore());
				}
			}
			return scores;
		} catch (Exception e) {
			System.out.println("[RANK] 오늘 랭킹 조회 실패 — DB 집계로 폴백: " + e.getMessage());
			return null;
		}
	}

	/**
	 * 오늘 ZSET이 아직 채워지지 않았으면 DB 집계로 채운다.
	 * 표식 키를 SETNX로 잡아 동시 요청이 겹쳐도 한 번만 채운다.
	 */
	private void ensureSeeded(String date) {
		String marker = seededKey(date);
		Boolean first = redisTemplate.opsForValue().setIfAbsent(marker, "1");
		if (!Boolean.TRUE.equals(first)) {
			return; // 이미 채워져 있다
		}
		// 표식을 먼저 잡았으므로 만료를 붙인 뒤 채운다.
		// 채우는 중 실패하면 표식만 남아 다음 재동기화까지 비어 보일 수 있으므로,
		// 실패 시 표식을 지워 다음 요청이 다시 시도하게 한다.
		try {
			redisTemplate.expire(marker, TTL_HOURS, TimeUnit.HOURS);
			seedFromDatabase(date);
		} catch (Exception e) {
			try {
				redisTemplate.delete(marker);
			} catch (Exception ignore) {
				// 표식 삭제까지 실패하면 다음 resync가 정리한다
			}
			throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
		}
	}

	/** DB의 오늘 집계를 ZSET에 절대값으로 써넣는다 */
	private void seedFromDatabase(String date) {
		List<Map<String, Object>> counts = dao.selectList("superapp.selectTodayImpressionCounts");
		String key = dailyKey(date);
		if (counts == null || counts.isEmpty()) {
			return;
		}
		for (Map<String, Object> row : counts) {
			Object starId = row.get("PRS_ID");
			Object viewCount = row.get("viewCount");
			if (starId == null || viewCount == null) {
				continue;
			}
			redisTemplate.opsForZSet().add(key, String.valueOf(starId), ((Number) viewCount).doubleValue());
		}
		redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
	}

	/**
	 * 오늘 ZSET을 DB 기준으로 다시 맞춘다. Redis 장애 구간에 놓친 증가분을 회복시킨다.
	 *
	 * 자체 간격 제한이 있어 자주 불려도 실제 재동기화는 RESYNC_INTERVAL_SEC 마다 한 번이다.
	 * 읽기 경로에서 호출하므로 랭킹을 보는 사람이 없으면 아무 비용도 들지 않는다.
	 */
	public void resyncToday() {
		try {
			String date = today();
			String guard = KEY_PREFIX + date + ":resync";
			Boolean first = redisTemplate.opsForValue().setIfAbsent(guard, "1");
			if (!Boolean.TRUE.equals(first)) {
				return;
			}
			redisTemplate.expire(guard, RESYNC_INTERVAL_SEC, TimeUnit.SECONDS);

			seedFromDatabase(date);
			// 시딩 표식도 같이 세워 둔다 (아직 한 번도 읽히지 않은 날일 수 있다)
			redisTemplate.opsForValue().set(seededKey(date), "1");
			redisTemplate.expire(seededKey(date), TTL_HOURS, TimeUnit.HOURS);
		} catch (Exception e) {
			System.out.println("[RANK] 오늘 랭킹 재동기화 실패: " + e.getMessage());
		}
	}
}

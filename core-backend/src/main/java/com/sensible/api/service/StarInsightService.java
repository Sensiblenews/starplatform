package com.sensible.api.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.sensible.common.dao.DefaultDAO;

/**
 * 스타 인사이트 (2-34차 2-1·2-2) — 글로벌 랭킹·방문자·즐겨찾기·좋아요·게시물 5종을 기간별로 묶는다.
 *
 * 원본은 하루 한 번 집계해 둔 WH_STAR_DAILY_STAT 이고(StarDailyStatScheduler), 오늘 몫만 원본 로그에서 센다.
 * 앱 별 메뉴 I 탭·스타 본인 어드민·글로벌/국가 어드민이 같은 계산을 쓴다.
 *
 * 기간 (요청서 "매일 매주 매달 3개월 6개월 1년" — 각 탭이 서로 다른 그래프가 되도록 범위·단위를 정했다):
 *   DAY 최근 14일(일) · WEEK 최근 12주(주) · MONTH 최근 12개월(월) · M3 최근 90일(일) · M6 최근 26주(주) · Y1 최근 52주(주)
 * 즐겨찾기는 새로 추가된 수(해제는 빼지 않는다 — 기존 My Insight 의 "이번 달 즐겨찾기"와 같은 기준).
 * 랭킹은 구간 마지막 날의 순위(작을수록 위). 스냅샷이 없는 구간은 null — 그래프에서 끊어 그린다.
 */
@Service("starInsightService")
public class StarInsightService {

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	@Resource(name = "starRankService")
	private StarRankService starRankService;

	// ===== 기간 =====

	public enum Period {
		DAY(Unit.DAY, 14), WEEK(Unit.WEEK, 12), MONTH(Unit.MONTH, 12), M3(Unit.DAY, 90), M6(Unit.WEEK, 26), Y1(Unit.WEEK, 52);

		final Unit unit;
		final int count;

		Period(Unit unit, int count) {
			this.unit = unit;
			this.count = count;
		}

		/** 모르는 값은 WEEK (순수 함수) */
		public static Period parse(Object raw) {
			if (raw == null) return WEEK;
			String v = String.valueOf(raw).trim().toUpperCase();
			for (Period p : values()) {
				if (p.name().equals(v)) return p;
			}
			return WEEK;
		}
	}

	enum Unit { DAY, WEEK, MONTH }

	/** 그래프 한 칸 = [start, end) */
	public static final class Bucket {
		public final LocalDate start;
		public final LocalDate end;
		public final String label;

		Bucket(LocalDate start, LocalDate end, String label) {
			this.start = start;
			this.end = end;
			this.label = label;
		}
	}

	private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("M/d");
	private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy.MM");

	/** 오늘을 마지막 칸에 포함하는 구간 목록 (순수 함수 — 단위 테스트 대상). 주는 월요일 시작 */
	public static List<Bucket> buildBuckets(Period period, LocalDate today) {
		List<Bucket> buckets = new ArrayList<>();
		LocalDate lastStart;
		switch (period.unit) {
		case WEEK:
			lastStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			for (int i = period.count - 1; i >= 0; i--) {
				LocalDate s = lastStart.minusWeeks(i);
				buckets.add(new Bucket(s, s.plusWeeks(1), s.format(DAY_LABEL)));
			}
			break;
		case MONTH:
			lastStart = today.withDayOfMonth(1);
			for (int i = period.count - 1; i >= 0; i--) {
				LocalDate s = lastStart.minusMonths(i);
				buckets.add(new Bucket(s, s.plusMonths(1), s.format(MONTH_LABEL)));
			}
			break;
		default:
			for (int i = period.count - 1; i >= 0; i--) {
				LocalDate s = today.minusDays(i);
				buckets.add(new Bucket(s, s.plusDays(1), s.format(DAY_LABEL)));
			}
		}
		return buckets;
	}

	/** 지표 이름 — 앱·어드민 응답 키 */
	public static final String[] COUNT_METRICS = { "visits", "follows", "likes", "posts" };

	/**
	 * 일별 행을 구간별로 합친다 (순수 함수). rows 의 statDate 는 "yyyy-MM-dd".
	 * 합계 지표는 구간 합, 랭킹은 구간 안에서 가장 늦은 날의 값(없으면 null).
	 */
	public static Map<String, List<Object>> bucketize(List<Map<String, Object>> rows, List<Bucket> buckets) {
		Map<String, List<Object>> series = new LinkedHashMap<>();
		for (String m : COUNT_METRICS) {
			List<Object> zeros = new ArrayList<>();
			for (int i = 0; i < buckets.size(); i++) zeros.add(0L);
			series.put(m, zeros);
		}
		List<Object> ranks = new ArrayList<>(Collections.nCopies(buckets.size(), (Object) null));
		LocalDate[] rankDates = new LocalDate[buckets.size()];
		series.put("rank", ranks);
		if (rows == null) return series;

		for (Map<String, Object> row : rows) {
			LocalDate d;
			try {
				d = LocalDate.parse(String.valueOf(row.get("statDate")));
			} catch (Exception e) {
				continue;
			}
			int idx = indexOf(buckets, d);
			if (idx < 0) continue;
			for (String m : COUNT_METRICS) {
				long prev = ((Number) series.get(m).get(idx)).longValue();
				series.get(m).set(idx, prev + toLong(row.get(m)));
			}
			Object rank = row.get("globalRank");
			if (rank != null && (rankDates[idx] == null || d.isAfter(rankDates[idx]))) {
				ranks.set(idx, toLong(rank));
				rankDates[idx] = d;
			}
		}
		return series;
	}

	private static int indexOf(List<Bucket> buckets, LocalDate d) {
		for (int i = 0; i < buckets.size(); i++) {
			Bucket b = buckets.get(i);
			if (!d.isBefore(b.start) && d.isBefore(b.end)) return i;
		}
		return -1;
	}

	private static long toLong(Object v) {
		if (v == null) return 0L;
		if (v instanceof Number) return ((Number) v).longValue();
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	// ===== 한 스타의 그래프 =====

	/**
	 * 앱 I 탭·스타 어드민·글로벌 어드민 공용. 인증은 호출하는 쪽이 끝낸 뒤 부른다.
	 * 응답: { period, labels[], series{rank,visits,follows,likes,posts}, totals{...}, rankNow }
	 */
	public Map<String, Object> getSeries(String prsId, Object periodRaw) throws Exception {
		Period period = Period.parse(periodRaw);
		LocalDate today = LocalDate.now();
		List<Bucket> buckets = buildBuckets(period, today);

		Map<String, Object> param = new HashMap<>();
		param.put("prsId", prsId);
		param.put("fromDate", buckets.get(0).start.toString());
		param.put("toDate", today.toString()); // 오늘 몫은 아래에서 원본으로
		List<Map<String, Object>> rows = new ArrayList<>(dao.<Map<String, Object>>selectList("superapp.selectStarDailyStats", param));

		Map<String, Object> todayRow = dao.selectOne("superapp.selectStarTodayStats", param);
		Long rankNow = currentRank(prsId);
		Map<String, Object> live = new HashMap<>();
		live.put("statDate", today.toString());
		if (todayRow != null) live.putAll(todayRow);
		live.put("globalRank", rankNow);
		rows.add(live);

		Map<String, List<Object>> series = bucketize(rows, buckets);
		Map<String, Object> totals = new LinkedHashMap<>();
		for (String m : COUNT_METRICS) {
			long sum = 0;
			for (Object v : series.get(m)) sum += ((Number) v).longValue();
			totals.put(m, sum);
		}

		List<String> labels = new ArrayList<>();
		for (Bucket b : buckets) labels.add(b.label);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("result", "OK");
		result.put("period", period.name());
		result.put("labels", labels);
		result.put("series", series);
		result.put("totals", totals);
		result.put("rankNow", rankNow);
		return result;
	}

	/** 지금 글로벌 순위 (앱과 같은 순위표 캐시). 순위표에 없으면 null */
	private Long currentRank(String prsId) {
		List<Map<String, Object>> map;
		try {
			map = starRankService.getGlobalRankMap();
		} catch (Exception e) {
			map = starRankService.getGlobalRankMapUncached();
		}
		if (map == null) return null;
		for (Map<String, Object> row : map) {
			if (prsId.equals(String.valueOf(row.get("PRS_ID")))) return toLong(row.get("GLOBAL_RANK"));
		}
		return null;
	}

	// ===== 어드민 인사이트 목록 (2-2) =====

	/** 어드민 목록 지표 (순수 함수). 모르는 값은 RANK */
	public static String normalizeMetric(Object raw) {
		String v = raw == null ? "" : String.valueOf(raw).trim().toUpperCase();
		if ("VISIT".equals(v) || "FOLLOW".equals(v) || "LIKE".equals(v) || "POST".equals(v)) return v;
		return "RANK";
	}

	public static final int RANKING_PAGE_SIZE = 50;

	/**
	 * 지표 높은 순 스타 목록. country 가 있으면 그 국가만(LC). 합계 지표는 어제까지의 기간 합,
	 * RANK 는 지금 글로벌 순위(앱과 같은 순위표) + 기간 시작 무렵 대비 변동.
	 * 응답: { list[{RANK_NO, PRS_ID, PRS_NAME, STORED_FILE_NM, PRS_COUNTRY, TOTAL, START_RANK}], total, page, pageSize }
	 */
	public Map<String, Object> getRanking(Object metricRaw, Object periodRaw, String country, int page) throws Exception {
		String metric = normalizeMetric(metricRaw);
		Period period = Period.parse(periodRaw);
		LocalDate today = LocalDate.now();
		List<Bucket> buckets = buildBuckets(period, today);
		int safePage = Math.max(1, page);
		int start = (safePage - 1) * RANKING_PAGE_SIZE;

		Map<String, Object> param = new HashMap<>();
		param.put("metric", metric);
		param.put("country", country);
		param.put("fromDate", buckets.get(0).start.toString());
		param.put("toDate", today.toString());
		param.put("start", start);
		param.put("length", RANKING_PAGE_SIZE);

		List<Map<String, Object>> list;
		int total;
		if ("RANK".equals(metric)) {
			List<Map<String, Object>> all = rankedStars(param);
			total = all.size();
			list = new ArrayList<>(all.subList(Math.min(start, total), Math.min(start + RANKING_PAGE_SIZE, total)));
		} else {
			list = new ArrayList<>(dao.<Map<String, Object>>selectList("super.selectInsightRankingList", param));
			total = dao.selectOne("super.selectInsightStarCount", param);
			for (int i = 0; i < list.size(); i++) list.get(i).put("RANK_NO", start + i + 1);
		}

		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("total", total);
		result.put("page", safePage);
		result.put("pageSize", RANKING_PAGE_SIZE);
		result.put("metric", metric);
		result.put("period", period.name());
		return result;
	}

	/** 지금 순위표 순서대로 (국가 필터 후 1 부터 다시 번호). TOTAL = 글로벌 순위, START_RANK = 기간 시작 무렵 순위 */
	private List<Map<String, Object>> rankedStars(Map<String, Object> param) {
		Map<String, Map<String, Object>> basics = new HashMap<>();
		for (Map<String, Object> row : dao.<Map<String, Object>>selectList("super.selectInsightStarBasics", param)) {
			basics.put(String.valueOf(row.get("PRS_ID")), row);
		}
		Map<String, Object> startRanks = new HashMap<>();
		for (Map<String, Object> row : dao.<Map<String, Object>>selectList("super.selectInsightStartRanks", param)) {
			startRanks.put(String.valueOf(row.get("PRS_ID")), row.get("GLOBAL_RANK"));
		}
		List<Map<String, Object>> map;
		try {
			map = starRankService.getGlobalRankMap();
		} catch (Exception e) {
			map = starRankService.getGlobalRankMapUncached();
		}
		List<Map<String, Object>> sorted = new ArrayList<>(map == null ? Collections.<Map<String, Object>>emptyList() : map);
		sorted.sort((a, b) -> Long.compare(toLong(a.get("GLOBAL_RANK")), toLong(b.get("GLOBAL_RANK"))));
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> r : sorted) {
			String id = String.valueOf(r.get("PRS_ID"));
			Map<String, Object> info = basics.get(id);
			if (info == null) continue; // 다른 국가 (LC) 또는 비활성
			Map<String, Object> row = new HashMap<>(info);
			row.put("RANK_NO", out.size() + 1);
			row.put("TOTAL", toLong(r.get("GLOBAL_RANK")));
			row.put("START_RANK", startRanks.get(id));
			out.add(row);
		}
		return out;
	}

	/** LC 어드민이 다른 국가 스타를 열지 못하게 — 스타의 국가 (없으면 null) */
	public String starCountry(String prsId) {
		return dao.selectOne("super.selectInsightStarCountry", prsId);
	}

	public Map<String, Object> starHeader(String prsId) {
		return dao.selectOne("super.selectInsightStarHeader", prsId);
	}

	/** 어드민 인사이트 스타 화면 — 게시물 전체 한 페이지 (2-34 3차 5번, 검수 상태 무관) */
	public List<Map<String, Object>> starPosts(String prsId, int page, int size) {
		Map<String, Object> param = new HashMap<>();
		param.put("prsId", prsId);
		param.put("offset", (Math.max(1, page) - 1) * size);
		param.put("size", size);
		return dao.selectList("super.selectInsightStarPosts", param);
	}

	public int starPostCount(String prsId) {
		Integer count = dao.selectOne("super.countInsightStarPosts", prsId);
		return count == null ? 0 : count;
	}

	// ===== 하루 집계 (스케줄러) =====

	/** [fromDate, toDate) 범위의 방문·즐겨찾기·좋아요·게시물을 다시 쓴다 */
	public void aggregateCounts(LocalDate fromDate, LocalDate toDate) {
		Map<String, Object> param = new HashMap<>();
		param.put("fromDate", fromDate.toString());
		param.put("toDate", toDate.toString());
		dao.insert("superapp.upsertDailyVisits", param);
		dao.insert("superapp.upsertDailyFollows", param);
		dao.insert("superapp.upsertDailyUnfollows", param);
		dao.insert("superapp.upsertDailyLikes", param);
		dao.insert("superapp.upsertDailyPosts", param);
	}

	/** 지금 순위표를 statDate 의 스냅샷으로 저장한다. 500행씩 나눠 넣는다 */
	public int snapshotRanks(LocalDate statDate) {
		List<Map<String, Object>> map = starRankService.getGlobalRankMapUncached();
		if (map == null || map.isEmpty()) return 0;
		int saved = 0;
		for (int i = 0; i < map.size(); i += 500) {
			List<Map<String, Object>> chunk = map.subList(i, Math.min(map.size(), i + 500));
			Map<String, Object> param = new HashMap<>();
			param.put("statDate", statDate.toString());
			param.put("rows", chunk);
			dao.insert("superapp.upsertDailyRanks", param);
			saved += chunk.size();
		}
		return saved;
	}
}

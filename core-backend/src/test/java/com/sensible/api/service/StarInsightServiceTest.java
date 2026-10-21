package com.sensible.api.service;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.sensible.api.service.StarInsightService.Bucket;
import com.sensible.api.service.StarInsightService.Period;

/**
 * 인사이트 기간·구간·합산 (2-34차 2-1·2-2).
 * 그래프 칸이 오늘을 마지막에 포함하고, 일별 행이 맞는 칸에 더해지는지 본다.
 */
public class StarInsightServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 15); // 목요일

	@Test
	public void 모르는_기간은_WEEK() {
		assertEquals(Period.WEEK, Period.parse(null));
		assertEquals(Period.WEEK, Period.parse("decade"));
		assertEquals(Period.M3, Period.parse(" m3 "));
		assertEquals(Period.Y1, Period.parse("Y1"));
	}

	@Test
	public void DAY_는_오늘까지_14일() {
		List<Bucket> b = StarInsightService.buildBuckets(Period.DAY, TODAY);
		assertEquals(14, b.size());
		assertEquals(LocalDate.of(2026, 10, 2), b.get(0).start);
		assertEquals(TODAY, b.get(13).start);
		assertEquals(TODAY.plusDays(1), b.get(13).end);
		assertEquals("10/15", b.get(13).label);
	}

	@Test
	public void WEEK_는_월요일_시작_12주() {
		List<Bucket> b = StarInsightService.buildBuckets(Period.WEEK, TODAY);
		assertEquals(12, b.size());
		assertEquals(LocalDate.of(2026, 10, 12), b.get(11).start); // 이번 주 월요일
		assertEquals(LocalDate.of(2026, 10, 19), b.get(11).end);
		assertEquals(LocalDate.of(2026, 7, 27), b.get(0).start);
	}

	@Test
	public void MONTH_는_이번_달까지_12개월() {
		List<Bucket> b = StarInsightService.buildBuckets(Period.MONTH, TODAY);
		assertEquals(12, b.size());
		assertEquals(LocalDate.of(2025, 11, 1), b.get(0).start);
		assertEquals(LocalDate.of(2026, 10, 1), b.get(11).start);
		assertEquals("2026.10", b.get(11).label);
	}

	@Test
	public void 기간별_칸_수() {
		assertEquals(90, StarInsightService.buildBuckets(Period.M3, TODAY).size());
		assertEquals(26, StarInsightService.buildBuckets(Period.M6, TODAY).size());
		assertEquals(52, StarInsightService.buildBuckets(Period.Y1, TODAY).size());
	}

	private static Map<String, Object> row(String date, int visits, int likes, Integer rank) {
		Map<String, Object> m = new HashMap<>();
		m.put("statDate", date);
		m.put("visits", visits);
		m.put("follows", 1);
		m.put("likes", likes);
		m.put("posts", 0);
		m.put("globalRank", rank);
		return m;
	}

	@Test
	public void 일별_행은_주_칸에_더해지고_순위는_그_주_마지막_날_값() {
		List<Bucket> b = StarInsightService.buildBuckets(Period.WEEK, TODAY);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row("2026-10-12", 10, 2, 30)); // 이번 주 월
		rows.add(row("2026-10-14", 5, 1, 25));  // 이번 주 수 — 더 늦은 날이라 순위는 25
		rows.add(row("2026-10-13", 1, 0, null));
		rows.add(row("2026-10-05", 7, 0, 40));  // 지난주
		rows.add(row("2025-01-01", 99, 99, 1)); // 범위 밖 — 무시
		rows.add(row("not-a-date", 99, 99, 1)); // 깨진 행 — 무시
		Map<String, List<Object>> s = StarInsightService.bucketize(rows, b);
		assertEquals(16L, s.get("visits").get(11));
		assertEquals(3L, s.get("likes").get(11));
		assertEquals(3L, s.get("follows").get(11));
		assertEquals(25L, s.get("rank").get(11));
		assertEquals(7L, s.get("visits").get(10));
		assertEquals(40L, s.get("rank").get(10));
		assertNull(s.get("rank").get(0));
		assertEquals(0L, s.get("visits").get(0));
	}

	@Test
	public void 행이_없으면_전부_0_순위는_null() {
		Map<String, List<Object>> s = StarInsightService.bucketize(null, StarInsightService.buildBuckets(Period.DAY, TODAY));
		assertEquals(14, s.get("posts").size());
		assertEquals(0L, s.get("posts").get(5));
		assertNull(s.get("rank").get(5));
	}

	@Test
	public void 어드민_지표는_화이트리스트() {
		assertEquals("VISIT", StarInsightService.normalizeMetric("visit"));
		assertEquals("POST", StarInsightService.normalizeMetric("POST"));
		assertEquals("RANK", StarInsightService.normalizeMetric("VISIT_CNT); DROP"));
		assertEquals("RANK", StarInsightService.normalizeMetric(null));
	}

	@Test
	public void 대시보드_합산_기간합계는_구간을_모두_더한다() {
		List<StarInsightService.Bucket> b = StarInsightService.buildBuckets(Period.WEEK, TODAY);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row("2026-10-13", 10, 2, 0));
		rows.add(row("2026-10-05", 7, 1, 0));
		Map<String, List<Object>> s = StarInsightService.bucketize(rows, b);
		Map<String, Object> totals = StarInsightService.sumTotals(s);
		assertEquals(17L, totals.get("visits"));
		assertEquals(3L, totals.get("likes"));
		assertFalse(totals.containsKey("rank"));
	}
}

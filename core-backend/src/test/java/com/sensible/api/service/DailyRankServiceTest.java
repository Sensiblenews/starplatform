package com.sensible.api.service;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * 오늘 랭킹 실시간 반영의 순수 판정부 검증.
 *
 * Redis·DB에 의존하는 경로는 단위 테스트 대상이 아니다.
 * 키 조립, 오늘 판정, ZSET 점수와 메타데이터 결합만 여기서 본다.
 */
public class DailyRankServiceTest {

	private String today() {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	}

	@Test
	public void 키는_날짜별로_분리된다() {
		// 날짜가 키에 들어가야 자정을 넘겨도 어제 집계가 섞이지 않는다
		assertEquals("rank:daily:2026-09-16", DailyRankService.dailyKey("2026-09-16"));
		assertEquals("rank:daily:2026-09-17", DailyRankService.dailyKey("2026-09-17"));
		assertEquals("rank:daily:2026-09-16:seeded", DailyRankService.seededKey("2026-09-16"));
	}

	@Test
	public void today는_yyyy_MM_dd_형식이다() {
		assertTrue(DailyRankService.today().matches("\\d{4}-\\d{2}-\\d{2}"));
	}

	@Test
	public void date가_비면_오늘로_본다() {
		// selectDailyLeaderboard가 date 미지정 시 CURDATE() 기준이므로 같은 규칙이어야 한다
		assertTrue(DailyRankService.isToday(null));
		assertTrue(DailyRankService.isToday(""));
		assertTrue(DailyRankService.isToday("   "));
		assertTrue(DailyRankService.isToday(today()));
	}

	@Test
	public void 지난_날짜는_오늘이_아니다() {
		assertFalse(DailyRankService.isToday("2020-01-01"));
		assertFalse(DailyRankService.isToday("2026-01-01"));
	}

	@Test
	public void 순위는_ZSET_순서대로_1부터_매긴다() {
		LinkedHashMap<String, Double> scores = new LinkedHashMap<String, Double>();
		scores.put("star_a", 30.0);
		scores.put("star_b", 20.0);
		scores.put("star_c", 10.0);

		List<Map<String, Object>> result = DailyRankService.assembleRanking(scores, meta("star_c", "star_a", "star_b"));

		assertEquals(3, result.size());
		// 메타데이터 조회 순서가 뒤섞여 있어도 ZSET 순서를 따라야 한다
		assertEquals("star_a", result.get(0).get("id"));
		assertEquals("star_b", result.get(1).get("id"));
		assertEquals("star_c", result.get(2).get("id"));
		assertEquals(1, result.get(0).get("globalRank"));
		assertEquals(2, result.get(1).get("globalRank"));
		assertEquals(3, result.get(2).get("globalRank"));
	}

	@Test
	public void 조회수는_정수로_내려간다() {
		// ZSET 점수는 double이지만 화면에는 건수로 보여야 한다
		LinkedHashMap<String, Double> scores = new LinkedHashMap<String, Double>();
		scores.put("star_a", 7.0);
		List<Map<String, Object>> result = DailyRankService.assembleRanking(scores, meta("star_a"));

		assertEquals(7L, result.get(0).get("viewCount"));
		// 프런트(daily-ranking-modal)가 읽는 필드
		assertEquals(7L, result.get(0).get("displayViewCount"));
	}

	@Test
	public void 메타데이터가_없는_스타는_건너뛰고_순위에_구멍을_남기지_않는다() {
		// 비활성 전환된 스타가 ZSET에 남아 있을 수 있다
		LinkedHashMap<String, Double> scores = new LinkedHashMap<String, Double>();
		scores.put("star_a", 30.0);
		scores.put("gone", 20.0);
		scores.put("star_b", 10.0);

		List<Map<String, Object>> result = DailyRankService.assembleRanking(scores, meta("star_a", "star_b"));

		assertEquals(2, result.size());
		assertEquals("star_a", result.get(0).get("id"));
		assertEquals(1, result.get(0).get("globalRank"));
		assertEquals("star_b", result.get(1).get("id"));
		// 2위가 비어 3위로 뛰지 않아야 한다
		assertEquals(2, result.get(1).get("globalRank"));
	}

	@Test
	public void 이름과_이미지는_메타데이터에서_가져온다() {
		LinkedHashMap<String, Double> scores = new LinkedHashMap<String, Double>();
		scores.put("star_a", 5.0);
		List<Map<String, Object>> result = DailyRankService.assembleRanking(scores, meta("star_a"));

		assertEquals("name_star_a", result.get(0).get("name"));
		assertEquals("img_star_a", result.get(0).get("image"));
	}

	@Test
	public void 빈_입력은_빈_목록을_돌려준다() {
		assertTrue(DailyRankService.assembleRanking(new LinkedHashMap<String, Double>(), meta()).isEmpty());
		assertTrue(DailyRankService.assembleRanking(new LinkedHashMap<String, Double>(), null).isEmpty());
	}

	@Test
	public void targetMonth가_이번_달이거나_비면_오늘이_범위에_든다() {
		// 역대 오늘의 왕 목록에 오늘 행을 얹을지 판단한다
		String thisMonth = new SimpleDateFormat("yyyy-MM").format(new Date());
		assertTrue(SuperAppService.includesToday(null));
		assertTrue(SuperAppService.includesToday(""));
		assertTrue(SuperAppService.includesToday(thisMonth));
		assertFalse(SuperAppService.includesToday("2020-01"));
	}

	/** id 목록으로 메타데이터 행을 만든다 */
	private List<Map<String, Object>> meta(String... ids) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		for (String id : ids) {
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("id", id);
			row.put("name", "name_" + id);
			row.put("image", "img_" + id);
			row.put("FOLLOWER_CNT", 1);
			list.add(row);
		}
		return list;
	}
}

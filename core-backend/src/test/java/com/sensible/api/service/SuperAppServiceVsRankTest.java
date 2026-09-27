package com.sensible.api.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * VS 카드 좌·우에 글로벌 순위를 붙이는 규칙 검증 (2026-09-26 요청: 조회수 줄 → 🌍 #순위).
 * 순위표 밖 스타는 null 이어야 프런트가 순위를 지어내지 않는다.
 */
public class SuperAppServiceVsRankTest {

	private static Map<String, Object> row(String prsId, int rank) {
		Map<String, Object> m = new HashMap<>();
		m.put("PRS_ID", prsId);
		m.put("GLOBAL_RANK", rank);
		return m;
	}

	private static Map<String, Object> side(String id) {
		Map<String, Object> m = new HashMap<>();
		m.put("id", id);
		m.put("name", "n");
		return m;
	}

	@Test
	public void 순위표를_PRS_ID별_색인으로_만든다() {
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row("star_1", 1));
		rows.add(row("star_7", 15));
		Map<String, Object> idx = SuperAppService.indexGlobalRank(rows);
		assertEquals(15, idx.get("star_7"));
		assertEquals(1, idx.get("star_1"));
	}

	@Test
	public void 순위표가_없으면_빈_색인이다() {
		assertTrue(SuperAppService.indexGlobalRank(null).isEmpty());
	}

	@Test
	public void 순위표에_있는_스타는_globalRank를_받는다() {
		Map<String, Object> idx = SuperAppService.indexGlobalRank(java.util.Collections.singletonList(row("star_7", 15)));
		Map<String, Object> s = side("star_7");
		SuperAppService.attachGlobalRank(s, idx);
		assertEquals(15, s.get("globalRank"));
	}

	@Test
	public void 순위표_밖_스타는_null이다() {
		Map<String, Object> idx = SuperAppService.indexGlobalRank(java.util.Collections.singletonList(row("star_7", 15)));
		Map<String, Object> s = side("star_999");
		SuperAppService.attachGlobalRank(s, idx);
		assertTrue(s.containsKey("globalRank"));
		assertNull(s.get("globalRank"));
	}

	@Test
	public void 도전자_대기_카드의_right_null은_무시한다() {
		// 예외 없이 지나가야 한다
		SuperAppService.attachGlobalRank(null, new HashMap<String, Object>());
	}
}

package com.sensible.api.service;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;

/**
 * 로비 My Global Ranking 카드의 Global Score 계산·숫자 변환 규칙 검증 (2-29차).
 * 가중치는 superapp.xml의 selectGlobalRankMap / selectVsCards 정렬식(조회 ×1 + 좋아요 ×3 + 즐겨찾기 ×5)과 같아야 한다.
 */
public class SuperAppServiceGlobalScoreTest {

	@Test
	public void 지표가_모두_0이면_점수도_0이다() {
		assertEquals(0L, SuperAppService.computeGlobalScore(0, 0, 0));
	}

	@Test
	public void 조회_좋아요_즐겨찾기에_1_3_5_가중치를_준다() {
		// 100 + 10*3 + 2*5 = 140
		assertEquals(140L, SuperAppService.computeGlobalScore(100, 10, 2));
	}

	@Test
	public void 가중치_상수는_SQL_정렬식과_같다() {
		assertEquals(1, SuperAppService.SCORE_WEIGHT_VIEW);
		assertEquals(3, SuperAppService.SCORE_WEIGHT_LIKE);
		assertEquals(5, SuperAppService.SCORE_WEIGHT_FOLLOWER);
	}

	@Test
	public void VS_카드_예시와_같은_점수가_나온다() {
		// 클라이언트 목업: 조회 5 · 좋아요 0 · 즐겨찾기 2 → 15 pts, 조회 3 · 0 · 2 → 13 pts
		assertEquals(15L, SuperAppService.computeGlobalScore(5, 0, 2));
		assertEquals(13L, SuperAppService.computeGlobalScore(3, 0, 2));
	}

	@Test
	public void null은_0으로_바꾼다() {
		assertEquals(0L, SuperAppService.toLong(null));
	}

	@Test
	public void MyBatis가_돌려주는_숫자_타입을_모두_받는다() {
		assertEquals(5L, SuperAppService.toLong(Integer.valueOf(5)));
		assertEquals(6L, SuperAppService.toLong(Long.valueOf(6L)));
		assertEquals(7L, SuperAppService.toLong(BigInteger.valueOf(7)));
		assertEquals(8L, SuperAppService.toLong(new BigDecimal("8")));
	}

	@Test
	public void 숫자_문자열은_파싱하고_비숫자는_0이다() {
		assertEquals(9L, SuperAppService.toLong(" 9 "));
		assertEquals(0L, SuperAppService.toLong("abc"));
		assertEquals(0L, SuperAppService.toLong(""));
	}
}

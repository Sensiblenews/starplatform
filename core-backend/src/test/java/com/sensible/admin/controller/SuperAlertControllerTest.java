package com.sensible.admin.controller;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * 경보 상세 (2-35차) — 권장 조치 단계 분리·그래프 데이터·원클릭 조치 화이트리스트.
 */
public class SuperAlertControllerTest {

	@Test
	public void 권장_조치_문구를_단계로_나눈다() {
		List<String> steps = SuperAlertController.recommendationSteps(
				"1) 신규 배포·대용량 작업 중단  2) df -h, du 로 원인 확인  3) 로그 로테이션·임시 파일 정리 (DB·업로드 파일은 삭제 금지)  4) 공간 확보 후 서비스·DB 정상 확인");
		assertEquals(4, steps.size());
		assertEquals("신규 배포·대용량 작업 중단", steps.get(0));
		assertEquals("로그 로테이션·임시 파일 정리 (DB·업로드 파일은 삭제 금지)", steps.get(2));
		assertTrue(SuperAlertController.recommendationSteps(null).isEmpty());
	}

	@Test
	public void 그래프_데이터는_시각과_값_배열() {
		List<double[]> points = new ArrayList<double[]>();
		points.add(new double[] { 1000, 87.26 });
		points.add(new double[] { 2000, 90 });
		assertEquals("[[1000,87.3],[2000,90.0]]", SuperAlertController.historyJson(points));
		assertEquals("[]", SuperAlertController.historyJson(new ArrayList<double[]>()));
	}

	@Test
	public void 원클릭_조치는_정해진_것만() {
		assertTrue(SuperAlertController.ACTIONS.contains("RECHECK"));
		assertTrue(SuperAlertController.ACTIONS.contains("GC"));
		assertTrue(SuperAlertController.ACTIONS.contains("NOTE"));
		assertFalse(SuperAlertController.ACTIONS.contains("RESTART"));
		assertFalse(SuperAlertController.ACTIONS.contains("rm -rf"));
	}
}

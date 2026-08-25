package com.sensible.common.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 요청 통계 카운터의 경로 정규화·상태코드 분류·누적 동작 검증.
 * 정적 저장소라 테스트 간 값이 누적되므로 스냅샷 차이로 비교한다.
 */
public class RequestMetricsTest {

	@Test
	public void normalizePath_숫자_세그먼트를_id로_치환한다() {
		assertEquals("/api/content/{id}", RequestMetrics.normalizePath("/api/content/12345"));
		assertEquals("/api/star/{id}/feed", RequestMetrics.normalizePath("/api/star/77/feed"));
	}

	@Test
	public void normalizePath_긴_식별자_세그먼트를_id로_치환한다() {
		assertEquals("/api/file/{id}",
				RequestMetrics.normalizePath("/api/file/a1b2c3d4e5f60718293a4b5c"));
		// 길지만 숫자가 없는 단어는 식별자로 보지 않는다
		assertEquals("/api/notificationsettings",
				RequestMetrics.normalizePath("/api/notificationsettings"));
	}

	@Test
	public void normalizePath_짧은_영문_세그먼트는_유지한다() {
		assertEquals("/api/member/login", RequestMetrics.normalizePath("/api/member/login"));
	}

	@Test
	public void normalizePath_세그먼트_상한을_넘으면_뒤를_잘라낸다() {
		assertEquals("/a/b/c/d/*", RequestMetrics.normalizePath("/a/b/c/d/e/f/g"));
	}

	@Test
	public void normalizePath_쿼리스트링과_path_parameter를_제거한다() {
		assertEquals("/api/content", RequestMetrics.normalizePath("/api/content?page=3&size=20"));
		assertEquals("/adm/main", RequestMetrics.normalizePath("/adm/main;jsessionid=ABC123DEF456"));
	}

	@Test
	public void normalizePath_비어있는_입력은_루트로_처리한다() {
		assertEquals("/", RequestMetrics.normalizePath(null));
		assertEquals("/", RequestMetrics.normalizePath(""));
		assertEquals("/", RequestMetrics.normalizePath("/"));
	}

	@Test
	public void statusBucket_상태코드를_구간으로_나눈다() {
		assertEquals(1, RequestMetrics.statusBucket(200));
		assertEquals(1, RequestMetrics.statusBucket(204));
		assertEquals(2, RequestMetrics.statusBucket(302));
		assertEquals(3, RequestMetrics.statusBucket(404));
		assertEquals(4, RequestMetrics.statusBucket(500));
		// 규격을 벗어난 값은 기타 구간
		assertEquals(5, RequestMetrics.statusBucket(0));
		assertEquals(5, RequestMetrics.statusBucket(999));
	}

	@Test
	public void record_요청수와_응답바이트를_누적한다() {
		RequestMetrics.Snapshot before = RequestMetrics.snapshot();

		RequestMetrics.record("/api/test/case1", 200, 1000L);
		RequestMetrics.record("/api/test/case1", 200, 500L);
		RequestMetrics.record("/api/test/case1", 500, 0L);

		RequestMetrics.Snapshot after = RequestMetrics.snapshot();

		assertEquals(3, after.requests - before.requests);
		assertEquals(1500L, after.bytes - before.bytes);
		assertEquals(2, after.status[1] - before.status[1]);
		assertEquals(1, after.status[4] - before.status[4]);

		long[] path = after.paths.get("/api/test/case1");
		assertNotNull(path);
		assertEquals(3L, path[0]);
		assertEquals(1500L, path[1]);
	}

	@Test
	public void record_바이트가_음수여도_누적을_깨뜨리지_않는다() {
		RequestMetrics.Snapshot before = RequestMetrics.snapshot();
		RequestMetrics.record("/api/test/case2", 200, -1L);
		RequestMetrics.Snapshot after = RequestMetrics.snapshot();

		assertEquals(1, after.requests - before.requests);
		assertEquals(0L, after.bytes - before.bytes);
	}
}

package com.sensible.admin.service;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sensible.admin.service.LatestContentUploadJobs.Job;
import com.sensible.admin.service.LatestContentUploadJobs.State;

public class LatestContentUploadJobsTest {

	private LatestContentUploadJobs jobs;

	@Before
	public void setUp() {
		jobs = new LatestContentUploadJobs();
	}

	@After
	public void tearDown() {
		jobs.shutdown();
	}

	@Test
	public void 성공한_작업은_DONE_과_편성ID를_돌려준다() throws Exception {
		Job job = jobs.submit("a.mp4", new Callable<Integer>() {
			@Override
			public Integer call() {
				return 42;
			}
		});
		assertTrue(job.await(5000));
		assertEquals(State.DONE, job.state);
		assertEquals(42, job.lcId);

		Map<String, Object> status = jobs.status(job.id, System.currentTimeMillis());
		assertEquals("DONE", status.get("state"));
		assertEquals(42, status.get("lcId"));
		assertEquals("a.mp4", status.get("fileName"));
	}

	@Test
	public void 예외가_나면_FAILED_와_메시지를_돌려준다() throws Exception {
		Job job = jobs.submit("b.mp4", new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				throw new Exception("영상에서 썸네일을 만들지 못했습니다.");
			}
		});
		assertTrue(job.await(5000));
		assertEquals(State.FAILED, job.state);

		Map<String, Object> status = jobs.status(job.id, System.currentTimeMillis());
		assertEquals("FAILED", status.get("state"));
		assertEquals("영상에서 썸네일을 만들지 못했습니다.", status.get("msg"));
		assertNull(status.get("lcId"));
	}

	@Test
	public void 모르는_작업은_UNKNOWN() {
		assertNull(jobs.get("nope"));
		assertNull(jobs.get(null));
		assertEquals("UNKNOWN", jobs.status("nope", 0L).get("state"));
	}

	@Test
	public void 끝난_작업은_보관기간이_지나면_지워지고_진행중은_남는다() throws Exception {
		Job finished = jobs.submit("c.mp4", new Callable<Integer>() {
			@Override
			public Integer call() {
				return 1;
			}
		});
		assertTrue(finished.await(5000));

		// 두 번째 작업은 신호를 줄 때까지 끝나지 않는다
		final CountDownLatch release = new CountDownLatch(1);
		Job running = jobs.submit("d.mp4", new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				release.await();
				return 2;
			}
		});

		long later = finished.finishedAt + LatestContentUploadJobs.RETENTION_MS + 1;
		assertEquals(1, jobs.evictFinished(later));
		assertNull(jobs.get(finished.id));
		assertNotNull(jobs.get(running.id));

		// 보관 기간 안이면 지우지 않는다
		assertEquals(0, jobs.evictFinished(finished.finishedAt + 1));

		release.countDown();
		assertTrue(running.await(5000));
		assertEquals(State.DONE, running.state);
	}

	@Test
	public void 작업은_한_번에_하나씩_순서대로_돈다() throws Exception {
		final StringBuilder order = new StringBuilder();
		final CountDownLatch firstStarted = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		Job first = jobs.submit("e.mp4", new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				firstStarted.countDown();
				release.await();
				order.append("1");
				return 1;
			}
		});
		Job second = jobs.submit("f.mp4", new Callable<Integer>() {
			@Override
			public Integer call() {
				order.append("2");
				return 2;
			}
		});
		firstStarted.await();
		assertEquals(State.RUNNING, first.state);
		assertEquals(State.QUEUED, second.state);

		release.countDown();
		assertTrue(second.await(5000));
		assertEquals("12", order.toString());
	}
}

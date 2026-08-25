package com.sensible.admin.scheduler;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * 구간 지표 수집기의 순수 계산 로직 검증.
 * /proc/net/dev 파싱, 누적 카운터 델타, 디렉터리 스캔 상한이 대상이다.
 */
public class SystemMetricsCollectorTest {

	private static final String HEADER_1 = "Inter-|	  Receive												 |	Transmit";
	private static final String HEADER_2 = " face |bytes	packets errs drop fifo frame compressed multicast|bytes	   packets";

	/** rx/tx 사이에 채워 넣는 나머지 컬럼 (packets errs drop fifo frame compressed multicast) */
	private static String devLine(String name, long rx, long tx) {
		return "  " + name + ": " + rx + " 10 0 0 0 0 0 0 " + tx + " 20 0 0 0 0 0 0";
	}

	@Test
	public void parseProcNetDev_송수신_바이트를_읽는다() {
		List<String> lines = Arrays.asList(HEADER_1, HEADER_2, devLine("eth0", 1000L, 2000L));
		long[] result = SystemMetricsCollector.parseProcNetDev(lines, "auto");

		assertEquals(1000L, result[0]);
		assertEquals(2000L, result[1]);
	}

	@Test
	public void parseProcNetDev_auto는_루프백을_제외하고_합산한다() {
		List<String> lines = Arrays.asList(
				HEADER_1, HEADER_2,
				devLine("lo", 999999L, 999999L),
				devLine("eth0", 100L, 200L),
				devLine("eth1", 30L, 40L));
		long[] result = SystemMetricsCollector.parseProcNetDev(lines, "auto");

		assertEquals(130L, result[0]);
		assertEquals(240L, result[1]);
	}

	@Test
	public void parseProcNetDev_인터페이스를_지정하면_해당_값만_읽는다() {
		List<String> lines = Arrays.asList(
				HEADER_1, HEADER_2,
				devLine("eth0", 100L, 200L),
				devLine("eth1", 30L, 40L));
		long[] result = SystemMetricsCollector.parseProcNetDev(lines, "eth1");

		assertEquals(30L, result[0]);
		assertEquals(40L, result[1]);
	}

	@Test
	public void parseProcNetDev_읽을_수_있는_줄이_없으면_미지원으로_표시한다() {
		long[] onlyHeader = SystemMetricsCollector.parseProcNetDev(Arrays.asList(HEADER_1, HEADER_2), "auto");
		assertEquals(-1L, onlyHeader[0]);
		assertEquals(-1L, onlyHeader[1]);

		long[] onlyLoopback = SystemMetricsCollector.parseProcNetDev(
				Arrays.asList(HEADER_1, HEADER_2, devLine("lo", 1L, 1L)), "auto");
		assertEquals(-1L, onlyLoopback[0]);
	}

	@Test
	public void parseProcNetDev_깨진_줄은_건너뛴다() {
		List<String> lines = new ArrayList<String>();
		lines.add(HEADER_1);
		lines.add(null);
		lines.add("	 broken: not a number here");
		lines.add("	 short: 1 2 3");
		lines.add(devLine("eth0", 500L, 700L));
		long[] result = SystemMetricsCollector.parseProcNetDev(lines, "auto");

		assertEquals(500L, result[0]);
		assertEquals(700L, result[1]);
	}

	@Test
	public void positiveDelta_증가분을_계산한다() {
		assertEquals(300L, SystemMetricsCollector.positiveDelta(1300L, 1000L, true));
		assertEquals(0L, SystemMetricsCollector.positiveDelta(1000L, 1000L, true));
	}

	@Test
	public void positiveDelta_카운터가_되감기면_0으로_본다() {
		// 재부팅·인터페이스 리셋으로 NIC 카운터가 작아진 경우
		assertEquals(0L, SystemMetricsCollector.positiveDelta(50L, 9000L, true));
	}

	@Test
	public void positiveDelta_측정불가거나_음수면_0을_돌려준다() {
		assertEquals(0L, SystemMetricsCollector.positiveDelta(1300L, 1000L, false));
		assertEquals(0L, SystemMetricsCollector.positiveDelta(-1L, 1000L, true));
		assertEquals(0L, SystemMetricsCollector.positiveDelta(1300L, -1L, true));
	}

	@Test
	public void scanDirectory_하위_디렉터리까지_용량을_합산한다() throws Exception {
		File root = createTempDir();
		try {
			writeFile(new File(root, "a.txt"), 100);
			File sub = new File(root, "sub");
			assertTrue(sub.mkdir());
			writeFile(new File(sub, "b.txt"), 250);

			SystemMetricsCollector.DirStat stat =
					SystemMetricsCollector.scanDirectory(root, 1000, 10000L);

			assertTrue(stat.exists);
			assertEquals(2, stat.fileCount);
			assertEquals(350L, stat.bytes);
			assertFalse(stat.truncated);
		} finally {
			deleteRecursively(root);
		}
	}

	@Test
	public void scanDirectory_파일수_상한에_걸리면_truncated로_표시한다() throws Exception {
		File root = createTempDir();
		try {
			for (int i = 0; i < 5; i++) {
				writeFile(new File(root, "f" + i + ".txt"), 10);
			}
			SystemMetricsCollector.DirStat stat =
					SystemMetricsCollector.scanDirectory(root, 2, 10000L);

			assertTrue(stat.truncated);
			assertTrue(stat.fileCount <= 5);
		} finally {
			deleteRecursively(root);
		}
	}

	@Test
	public void scanDirectory_없는_경로는_exists_false를_돌려준다() {
		SystemMetricsCollector.DirStat stat = SystemMetricsCollector.scanDirectory(
				new File("/definitely/not/exists/witch-metrics"), 100, 1000L);

		assertFalse(stat.exists);
		assertEquals(0L, stat.bytes);
	}

	@Test
	public void scanDirectory_null_경로도_예외없이_처리한다() {
		SystemMetricsCollector.DirStat stat = SystemMetricsCollector.scanDirectory(null, 100, 1000L);
		assertFalse(stat.exists);
	}

	@Test
	public void round2_소수점_둘째자리로_반올림한다() {
		assertEquals(1.23, SystemMetricsCollector.round2(1.2345), 0.0001);
		assertEquals(0.0, SystemMetricsCollector.round2(Double.NaN), 0.0001);
		assertEquals(0.0, SystemMetricsCollector.round2(Double.POSITIVE_INFINITY), 0.0001);
	}

	@Test
	public void sumWindow_시간_창_안의_델타만_더한다() {
		long base = 1700000000000L;
		List<SystemMetricsCollector.Sample> samples = new ArrayList<SystemMetricsCollector.Sample>();
		// 2시간 전 / 30분 전 / 5분 전 / 지금
		samples.add(sample(base - 2 * 3600000L, 500L, 50L));
		samples.add(sample(base - 1800000L, 100L, 10L));
		samples.add(sample(base - 300000L, 200L, 20L));
		samples.add(sample(base, 300L, 30L));

		long hourTx = SystemMetricsCollector.sumWindow(samples, 3600000L, SystemMetricsCollector.Field.TX);
		// 1시간 창이므로 2시간 전 표본(500)은 빠진다
		assertEquals(600L, hourTx);

		long hourReq = SystemMetricsCollector.sumWindow(samples, 3600000L, SystemMetricsCollector.Field.REQUESTS);
		assertEquals(60L, hourReq);
	}

	@Test
	public void sumWindow_수집이_밀려도_개수가_아니라_시각으로_자른다() {
		long base = 1700000000000L;
		List<SystemMetricsCollector.Sample> samples = new ArrayList<SystemMetricsCollector.Sample>();
		// 스케줄러가 밀려 표본이 3개뿐이지만 구간은 3시간에 걸쳐 있다
		samples.add(sample(base - 3 * 3600000L, 1000L, 100L));
		samples.add(sample(base - 90 * 60000L, 1000L, 100L));
		samples.add(sample(base, 1000L, 100L));

		assertEquals(1000L, SystemMetricsCollector.sumWindow(samples, 3600000L, SystemMetricsCollector.Field.TX));
		assertEquals(2000L, SystemMetricsCollector.sumWindow(samples, 2 * 3600000L, SystemMetricsCollector.Field.TX));
	}

	@Test
	public void sumWindow_표본이_없으면_0이다() {
		assertEquals(0L, SystemMetricsCollector.sumWindow(
				new ArrayList<SystemMetricsCollector.Sample>(), 3600000L, SystemMetricsCollector.Field.TX));
	}

	@Test
	public void lastIntervalMinutes_마지막_두_표본의_실제_간격을_쓴다() {
		long base = 1700000000000L;
		List<SystemMetricsCollector.Sample> samples = new ArrayList<SystemMetricsCollector.Sample>();
		samples.add(sample(base - 900000L, 0L, 0L));
		samples.add(sample(base, 0L, 0L));

		// 15분 밀린 경우 5분이 아니라 15분으로 나눠야 한다
		assertEquals(15.0, SystemMetricsCollector.lastIntervalMinutes(samples), 0.001);
	}

	@Test
	public void lastIntervalMinutes_표본이_부족하면_기본_주기를_쓴다() {
		List<SystemMetricsCollector.Sample> samples = new ArrayList<SystemMetricsCollector.Sample>();
		assertEquals(5.0, SystemMetricsCollector.lastIntervalMinutes(samples), 0.001);
		samples.add(sample(1700000000000L, 0L, 0L));
		assertEquals(5.0, SystemMetricsCollector.lastIntervalMinutes(samples), 0.001);
	}

	/** 시각·송신 델타·요청 델타만 채운 표본 */
	private SystemMetricsCollector.Sample sample(long time, long txDelta, long reqDelta) {
		SystemMetricsCollector.Sample s = new SystemMetricsCollector.Sample();
		s.time = time;
		s.netTxDelta = txDelta;
		s.requestDelta = reqDelta;
		return s;
	}

	// ===== 헬퍼 =====

	private File createTempDir() throws Exception {
		File dir = File.createTempFile("witch-metrics", "");
		assertTrue(dir.delete());
		assertTrue(dir.mkdir());
		return dir;
	}

	private void writeFile(File f, int bytes) throws Exception {
		FileOutputStream out = new FileOutputStream(f);
		try {
			out.write(new byte[bytes]);
		} finally {
			out.close();
		}
	}

	private void deleteRecursively(File f) {
		if (f == null || !f.exists()) {
			return;
		}
		File[] children = f.listFiles();
		if (children != null) {
			for (File c : children) {
				deleteRecursively(c);
			}
		}
		f.delete();
	}
}

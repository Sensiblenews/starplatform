package com.sensible.admin.scheduler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sensible.admin.scheduler.AlertRules.Severity;
import com.sensible.admin.scheduler.AlertStateMachine.Kind;
import com.sensible.admin.scheduler.AlertStateMachine.State;
import com.sensible.admin.scheduler.AlertStateMachine.Transition;
import com.sensible.admin.service.AdminAuditService;
import com.sensible.admin.service.AlertMailService;
import com.sensible.admin.service.SystemAlertService;
import com.sensible.common.Constants;
import com.sensible.common.util.RequestMetrics;

/**
 * 서버 위험 경보 감시기. 5분마다 디스크·RAM·CPU·JVM 힙·DB·Redis·HTTP 5xx·로그인 실패를 읽어
 * 등급을 매기고, 상태기계(AlertStateMachine)가 낸 전이에 따라 DB 기록·메일 발송을 한다.
 *
 * 한계 (요건 문서도 인지하는 부분): 이 감시기는 WAS 안에서 돈다. 톰캣 프로세스가 죽으면
 * 이 감시기도 같이 죽어 "프로세스 다운" 경보는 못 낸다. 그건 외부 생존 감시(별도 서비스)의 몫이다.
 *
 * 메일은 AlertMailService 가 별도 스레드에서 보낸다. SMTP 가 멈춰도 여기(스케줄러 스레드)는 막히지 않는다.
 * DB 가 끊겨도 메모리 상태로 감시·메일은 계속되고, 기록만 빠진다.
 */
@Component
public class SystemAlertMonitor {

	private static final Logger logger = LoggerFactory.getLogger(SystemAlertMonitor.class);

	/** 로그인 실패 집계 창(분) */
	private static final int LOGIN_FAIL_WINDOW_MIN = 15;

	private static final long GB = 1024L * 1024L * 1024L;

	@Resource(name = "config")
	private Properties config;

	@Resource(name = "systemAlertService")
	private SystemAlertService store;

	@Resource(name = "alertMailService")
	private AlertMailService mailer;

	@Resource(name = "adminAuditService")
	private AdminAuditService audit;

	@Autowired(required = false)
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired(required = false)
	private DataSource dataSource;

	private final AlertStateMachine machine = new AlertStateMachine();

	private volatile boolean restored;
	private RequestMetrics.Snapshot prevSnapshot;
	private volatile List<Reading> lastReadings = Collections.emptyList();
	private volatile long lastEvaluatedAt;
	private volatile boolean lastEnabled = true;

	/** 항목별 최근 24시간 측정값 (경보 상세 그래프용, 2-35차). 5분 주기 × 288 = 24시간. 재기동하면 비어서 다시 쌓인다 */
	static final int HISTORY_SIZE = 288;
	private final Map<String, Deque<double[]>> history = new HashMap<String, Deque<double[]>>();

	/** 측정 1건 */
	public static class Reading {
		public String key;
		public String label;
		public double value;
		public String unit;
		public Severity severity;
		public String message;
		public String recommendation;
		public boolean available = true;

		Reading(String key, String label) {
			this.key = key;
			this.label = label;
		}

		public String valueText() {
			if (!available) {
				return "측정 불가";
			}
			if (unit == null) {
				return String.valueOf(Math.round(value));
			}
			return Math.round(value) + unit;
		}
	}

	// ==========================================
	// 스케줄
	// ==========================================

	/** 지표 수집기(0/5분)와 2분 어긋나게 둔다 (2-35차부터 스케줄러는 스레드 풀이라 다른 작업에 밀리지 않는다) */
	@Scheduled(cron = "0 2/5 * * * *")
	public void evaluate() {
		try {
			evaluateOnce(System.currentTimeMillis());
		} catch (Throwable t) {
			logger.warn("[Alert] 평가 실패: {}", t.toString());
		}
	}

	/** 어드민 "지금 점검" 버튼용 */
	public List<Reading> evaluateNow() {
		evaluateOnce(System.currentTimeMillis());
		return lastReadings;
	}

	synchronized void evaluateOnce(long now) {
		if (!restored) {
			restoreFromDb();
		}
		Map<String, String> settings = store.settings();
		List<Reading> readings = readAll(settings);
		lastReadings = readings;
		lastEvaluatedAt = now;
		recordHistory(readings, now);
		boolean enabled = "Y".equalsIgnoreCase(AlertRules.strOf(settings, AlertRules.S_ENABLED));
		lastEnabled = enabled;
		if (!enabled) {
			return; // 표시용 측정만 하고 경보·메일은 내지 않는다
		}
		int confirm = AlertRules.intOf(settings, AlertRules.S_CONFIRM_COUNT);
		int recover = AlertRules.intOf(settings, AlertRules.S_RECOVER_COUNT);
		long remindMs = AlertRules.intOf(settings, AlertRules.S_REMIND_MINUTES) * 60000L;
		int remindMax = AlertRules.intOf(settings, AlertRules.S_REMIND_MAX);

		for (Reading r : readings) {
			List<Transition> transitions = machine.apply(r.key, r.severity, r.value, r.message, now, confirm, recover, remindMs, remindMax);
			for (Transition t : transitions) {
				handle(t, r, settings, now);
			}
		}
	}

	private void restoreFromDb() {
		List<Map<String, Object>> open = store.openAlerts();
		if (open == null) {
			return; // DB 불가 — 다음 주기에 다시 시도
		}
		for (Map<String, Object> row : open) {
			String key = String.valueOf(row.get("ALERT_KEY"));
			Severity sev = Severity.parse(String.valueOf(row.get("SEVERITY")), Severity.WARNING);
			long openedAt = toLong(row.get("OPENED_MS"), System.currentTimeMillis());
			long id = toLong(row.get("ALERT_ID"), 0L);
			boolean acked = "Y".equals(String.valueOf(row.get("ACK_YN")));
			boolean mailSent = "Y".equals(String.valueOf(row.get("MAIL_SENT_YN")));
			int notify = (int) toLong(row.get("NOTIFY_COUNT"), 0L);
			machine.restore(key, sev, openedAt, id, acked, mailSent, notify);
		}
		restored = true;
		if (!open.isEmpty()) {
			logger.info("[Alert] 열린 경보 {}건 복원", open.size());
		}
	}

	// ==========================================
	// 전이 처리
	// ==========================================

	private void handle(final Transition t, Reading r, Map<String, String> settings, long now) {
		String platform = mailer.platformId();
		Severity mailMin = Severity.parse(AlertRules.strOf(settings, AlertRules.S_MAIL_MIN_SEVERITY), Severity.DANGER);
		List<String> to = AlertMailService.parseRecipients(AlertRules.strOf(settings, AlertRules.S_MAIL_TO));

		switch (t.kind) {
			case OPEN: {
				// 발생 시각은 상태 머신이 준 첫 초과 관측 시각 — DB·메일·상세 화면이 같은 값을 쓴다 (2026-10-23)
				long id = store.openAlert(t.key, t.severity.name(), r.label, t.message, t.value, platform, t.openedAt);
				machine.bindAlertId(t.key, id);
				store.event(id, t.key, "OPEN", t.severity.name(), t.message, "SYSTEM");
				logger.warn("[Alert] OPEN {} {} {}", new Object[] { t.severity, t.key, t.message });
				if (t.severity.atLeast(mailMin)) {
					sendAlertMail(t.key, id, "발생", t.severity, r, t.openedAt, now, to, platform);
				}
				break;
			}
			case ESCALATE: {
				long id = machine.state(t.key).alertId;
				store.updateAlert(id, t.severity.name(), t.message, t.value);
				store.event(id, t.key, "ESCALATE", t.severity.name(), t.previousSeverity.label() + " → " + t.severity.label()
						+ " (첫 관측 " + AlertMailService.format(t.sinceAt) + ") : " + t.message, "SYSTEM");
				logger.warn("[Alert] ESCALATE {} {} {}", new Object[] { t.severity, t.key, t.message });
				if (t.severity.atLeast(mailMin)) {
					sendAlertMail(t.key, id, "등급 상승", t.severity, r, t.openedAt, now, to, platform);
				}
				break;
			}
			case REMIND: {
				store.event(t.alertId, t.key, "REMIND", t.severity.name(), "미확인 긴급 경보 재알림: " + t.message, "SYSTEM");
				sendAlertMail(t.key, t.alertId, "재알림 (미확인)", t.severity, r, t.openedAt, now, to, platform);
				break;
			}
			case RESOLVE: {
				store.resolveAlert(t.alertId, t.value);
				store.event(t.alertId, t.key, "RESOLVE", t.previousSeverity.name(), "복구: " + t.message, "SYSTEM");
				logger.info("[Alert] RESOLVE {} {}", t.key, t.message);
				// 경보 메일이 나갔거나, 메일 대상 등급이었는데 발송이 실패했던 경보도 복구 메일을 보낸다 (2-35차 —
				// 예전에는 경보 메일이 3회 모두 실패하면 복구 메일도 영영 나가지 않았다)
				if (t.mailSent || t.previousSeverity.atLeast(mailMin)) {
					boolean stillAbnormal = t.severity != null && t.severity != Severity.NORMAL;
					String valueText = r.valueText() + (stillAbnormal ? " (현재 " + t.severity.label() + " 수준 — 화면에서 계속 관찰)" : "");
					String body = AlertMailService.recoveryBody(platform, r.label, t.previousSeverity.label(),
							AlertMailService.format(t.openedAt), AlertMailService.format(now), now - t.openedAt, valueText, t.alertId,
							AlertMailService.detailUrl(adminUrl(), t.alertId));
					String subject = AlertMailService.alertSubject("복구", platform, r.label, stillAbnormal ? "완화 (" + t.severity.label() + " 수준)" : "정상화");
					final long id = t.alertId;
					final String key = t.key;
					mailer.sendAsync(to, subject, body, new AlertMailService.Callback() {
						@Override
						public void done(boolean success, String error, int attempts) {
							store.event(id, key, success ? "MAIL_SENT" : "MAIL_FAIL", "NORMAL",
									success ? "복구 메일 발송 (" + attempts + "회차)" : "복구 메일 실패: " + error, "SYSTEM");
						}
					});
				}
				break;
			}
			default:
				break;
		}
	}

	private void sendAlertMail(final String key, final long alertId, String kindLabel, Severity severity, Reading r,
			long openedAt, final long now, List<String> to, String platform) {
		String subject = AlertMailService.alertSubject(severity.label(), platform, r.label, kindLabel);
		String body = AlertMailService.alertBody(kindLabel, severity.label(), platform, r.label, r.valueText() + " — " + r.message,
				AlertMailService.format(openedAt), AlertMailService.format(now), alertId, r.recommendation,
				AlertMailService.detailUrl(adminUrl(), alertId));
		mailer.sendAsync(to, subject, body, new AlertMailService.Callback() {
			@Override
			public void done(boolean success, String error, int attempts) {
				if (success) {
					machine.markMailSent(key, now);
					store.markMailSent(alertId);
					store.event(alertId, key, "MAIL_SENT", null, "경보 메일 발송 (" + attempts + "회차)", "SYSTEM");
				} else {
					store.event(alertId, key, "MAIL_FAIL", null, "경보 메일 실패: " + error, "SYSTEM");
				}
			}
		});
	}

	private String adminUrl() {
		String v = (config != null) ? config.getProperty("alert.admin.url") : null;
		if (v != null && !v.trim().isEmpty()) {
			return v.trim();
		}
		return "https://" + Constants.domain + "/super/alert/center.do";
	}

	// ==========================================
	// 측정
	// ==========================================

	List<Reading> readAll(Map<String, String> s) {
		return readAll(s, null);
	}

	/** only 가 있으면 그 항목만 읽는다 (HTTP 5xx 는 peek 에서 따로 처리) */
	private List<Reading> readAll(Map<String, String> s, String only) {
		List<Reading> out = new ArrayList<Reading>();
		if (only != null) {
			if (AlertRules.KEY_DISK.equals(only)) out.add(readDisk(s));
			else if (AlertRules.KEY_RAM.equals(only)) out.add(readRam(s));
			else if (AlertRules.KEY_CPU.equals(only)) out.add(readCpu(s));
			else if (AlertRules.KEY_HEAP.equals(only)) out.add(readHeap(s));
			else if (AlertRules.KEY_DB.equals(only)) out.add(readDb());
			else if (AlertRules.KEY_REDIS.equals(only)) out.add(readRedis());
			else if (AlertRules.KEY_LOGIN_FAIL.equals(only)) out.add(readLoginFail(s));
			return out;
		}
		out.add(readDisk(s));
		out.add(readRam(s));
		out.add(readCpu(s));
		out.add(readHeap(s));
		out.add(readDb());
		out.add(readRedis());
		out.add(readHttp5xx(s, true));
		out.add(readLoginFail(s));
		return out;
	}

	private Reading readDisk(Map<String, String> s) {
		Reading r = new Reading(AlertRules.KEY_DISK, "디스크 사용률");
		r.unit = "%";
		r.recommendation = "1) 신규 배포·대용량 작업 중단  2) df -h, du 로 원인 확인  3) 로그 로테이션·임시 파일 정리 (DB·업로드 파일은 삭제 금지)  4) 공간 확보 후 서비스·DB 정상 확인";
		try {
			String path = cfg("monitor.disk.path", "/");
			File disk = new File(path);
			long total = disk.getTotalSpace();
			long usable = disk.getUsableSpace();
			if (total <= 0) {
				r.available = false;
				r.severity = Severity.NORMAL;
				r.message = "측정 불가";
				return r;
			}
			long used = total - usable;
			r.value = used * 100.0 / total;
			r.severity = AlertRules.percentSeverity(r.value, AlertRules.intOf(s, AlertRules.S_DISK_WARN),
					AlertRules.intOf(s, AlertRules.S_DISK_DANGER), AlertRules.intOf(s, AlertRules.S_DISK_CRITICAL));
			r.message = String.format("%s 사용 %.1f%% (%.1f/%.1f GB, 여유 %.1f GB)", path, r.value,
					used / (double) GB, total / (double) GB, usable / (double) GB);
		} catch (Throwable t) {
			unavailable(r, t);
		}
		return r;
	}

	/**
	 * RAM 사용률. Linux 는 /proc/meminfo 의 MemAvailable 기준(캐시는 여유로 본다).
	 * 그 외 환경은 OperatingSystemMXBean 의 free physical 로 근사한다.
	 */
	private Reading readRam(Map<String, String> s) {
		Reading r = new Reading(AlertRules.KEY_RAM, "RAM 사용률");
		r.unit = "%";
		r.recommendation = "1) top/htop 으로 상위 프로세스 확인  2) JVM 힙 추이와 함께 보고 누수 의심 시 힙 덤프  3) 스왑 증가·OOM 로그 확인  4) 승인된 절차로 문제 프로세스 재시작";
		try {
			long[] mem = readMemInfo();
			long total = mem[0];
			long available = mem[1];
			if (total <= 0) {
				java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
				if (os instanceof com.sun.management.OperatingSystemMXBean) {
					com.sun.management.OperatingSystemMXBean sun = (com.sun.management.OperatingSystemMXBean) os;
					total = sun.getTotalPhysicalMemorySize();
					available = sun.getFreePhysicalMemorySize();
				}
			}
			if (total <= 0) {
				r.available = false;
				r.severity = Severity.NORMAL;
				r.message = "측정 불가";
				return r;
			}
			long used = total - available;
			r.value = used * 100.0 / total;
			r.severity = AlertRules.percentSeverity(r.value, AlertRules.intOf(s, AlertRules.S_RAM_WARN),
					AlertRules.intOf(s, AlertRules.S_RAM_DANGER), AlertRules.intOf(s, AlertRules.S_RAM_CRITICAL));
			String swap = (mem[2] > 0) ? String.format(", 스왑 %.0f%%", (mem[2] - mem[3]) * 100.0 / mem[2]) : "";
			r.message = String.format("사용 %.1f%% (%.1f/%.1f GB%s)", r.value, used / (double) GB, total / (double) GB, swap);
		} catch (Throwable t) {
			unavailable(r, t);
		}
		return r;
	}

	/** {MemTotal, MemAvailable, SwapTotal, SwapFree} 바이트. 파일 없으면 전부 -1 */
	static long[] readMemInfo() {
		long[] out = { -1, -1, -1, -1 };
		File f = new File("/proc/meminfo");
		if (!f.exists()) {
			return out;
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				int idx = -1;
				if (line.startsWith("MemTotal:")) {
					idx = 0;
				} else if (line.startsWith("MemAvailable:")) {
					idx = 1;
				} else if (line.startsWith("SwapTotal:")) {
					idx = 2;
				} else if (line.startsWith("SwapFree:")) {
					idx = 3;
				}
				if (idx >= 0) {
					out[idx] = parseMemInfoKb(line) * 1024L;
				}
			}
		} catch (Throwable ignore) {
			// 측정 불가
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Throwable ignore) {
				}
			}
		}
		return out;
	}

	static long parseMemInfoKb(String line) {
		String[] parts = line.split("\\s+");
		for (int i = 1; i < parts.length; i++) {
			try {
				return Long.parseLong(parts[i]);
			} catch (NumberFormatException ignore) {
				// 라벨 토큰
			}
		}
		return -1;
	}

	private Reading readCpu(Map<String, String> s) {
		Reading r = new Reading(AlertRules.KEY_CPU, "CPU 사용률");
		r.unit = "%";
		r.recommendation = "1) 상위 프로세스·최근 배포 확인  2) Cloudflare 요청 제한·API 동시 처리 제한 적용  3) 비핵심 기능(통계·추천) 제한  4) 정상 지표 확인 후 단계적 복원";
		try {
			java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
			double load = -1;
			if (os instanceof com.sun.management.OperatingSystemMXBean) {
				load = ((com.sun.management.OperatingSystemMXBean) os).getSystemCpuLoad();
			}
			if (load < 0) {
				r.available = false;
				r.severity = Severity.NORMAL;
				r.message = "측정 불가";
				return r;
			}
			r.value = load * 100.0;
			r.severity = AlertRules.percentSeverity(r.value, AlertRules.intOf(s, AlertRules.S_CPU_WARN),
					AlertRules.intOf(s, AlertRules.S_CPU_DANGER), 0);
			r.message = String.format("시스템 CPU %.0f%% (load avg %.2f)", r.value, os.getSystemLoadAverage());
		} catch (Throwable t) {
			unavailable(r, t);
		}
		return r;
	}

	private Reading readHeap(Map<String, String> s) {
		Reading r = new Reading(AlertRules.KEY_HEAP, "JVM 힙 사용률");
		r.unit = "%";
		r.recommendation = "1) 시스템 패널에서 GC 횟수·시간 추이 확인  2) 힙이 계속 오르면 누수 의심 — 힙 덤프 후 분석  3) OOM 전이면 승인된 절차로 톰캣 재시작 검토";
		try {
			java.lang.management.MemoryUsage heap = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
			if (heap.getMax() <= 0) {
				r.available = false;
				r.severity = Severity.NORMAL;
				r.message = "측정 불가";
				return r;
			}
			r.value = heap.getUsed() * 100.0 / heap.getMax();
			r.severity = AlertRules.percentSeverity(r.value, AlertRules.intOf(s, AlertRules.S_HEAP_WARN),
					AlertRules.intOf(s, AlertRules.S_HEAP_DANGER), 0);
			r.message = String.format("힙 %.0f%% (%d/%d MB)", r.value, heap.getUsed() / (1024 * 1024), heap.getMax() / (1024 * 1024));
		} catch (Throwable t) {
			unavailable(r, t);
		}
		return r;
	}

	private Reading readDb() {
		Reading r = new Reading(AlertRules.KEY_DB, "DB 연결");
		r.recommendation = "1) DB 서버 프로세스·디스크·연결 수 확인  2) 커넥션 풀 고갈이면 느린 쿼리·잠금 확인  3) DB 재시작은 승인 절차 후에만  4) 복구 후 앱 로그인·랭킹 조회 확인";
		if (dataSource == null) {
			r.available = false;
			r.severity = Severity.NORMAL;
			r.message = "dataSource 없음";
			return r;
		}
		java.sql.Connection conn = null;
		java.sql.Statement stmt = null;
		java.sql.ResultSet rs = null;
		long started = System.currentTimeMillis();
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.setQueryTimeout(5);
			rs = stmt.executeQuery("SELECT 1");
			boolean up = rs.next();
			long ms = System.currentTimeMillis() - started;
			r.value = up ? 1 : 0;
			r.severity = AlertRules.upDownSeverity(up, Severity.CRITICAL);
			r.message = up ? ("정상 (응답 " + ms + "ms)") : "SELECT 1 결과 없음";
		} catch (Throwable t) {
			r.value = 0;
			r.severity = Severity.CRITICAL;
			r.message = "연결 실패: " + t.getClass().getSimpleName();
		} finally {
			closeQuietly(rs);
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		return r;
	}

	private Reading readRedis() {
		Reading r = new Reading(AlertRules.KEY_REDIS, "Redis 연결");
		r.recommendation = "1) redis-server 프로세스·메모리 확인  2) 앱은 캐시 실패 시 DB 폴백으로 동작하므로 응답 지연·DB 부하를 함께 확인  3) 설정 변경 대신 HQ 에 Incident 등록";
		if (redisTemplate == null) {
			r.available = false;
			r.severity = Severity.NORMAL;
			r.message = "redisTemplate 없음";
			return r;
		}
		RedisConnection c = null;
		try {
			c = redisTemplate.getConnectionFactory().getConnection();
			String pong = c.ping();
			r.value = 1;
			r.severity = Severity.NORMAL;
			r.message = "정상 (" + (pong == null ? "PONG" : pong) + ")";
		} catch (Throwable t) {
			r.value = 0;
			r.severity = Severity.DANGER;
			r.message = "연결 실패: " + t.getClass().getSimpleName();
		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (Throwable ignore) {
				}
			}
		}
		return r;
	}

	/**
	 * 직전 평가 이후 구간의 5xx 건수·비율.
	 * commit=false 면 구간 기준점(prevSnapshot)을 옮기지 않는다 — 경보 상세의 "다시 측정"이 다음 정기 평가의 구간을 깎지 않게
	 */
	private Reading readHttp5xx(Map<String, String> s, boolean commit) {
		Reading r = new Reading(AlertRules.KEY_HTTP5XX, "HTTP 5xx 오류");
		r.unit = "건";
		r.recommendation = "1) catalina.out 최근 예외 확인  2) DB·Redis 경보가 함께 떠 있는지 확인  3) 최근 배포가 있었으면 롤백 검토  4) 특정 경로 집중이면 시스템 패널 상위 경로로 범위 좁히기";
		try {
			RequestMetrics.Snapshot cur = RequestMetrics.snapshot();
			RequestMetrics.Snapshot prev = prevSnapshot;
			if (commit) {
				prevSnapshot = cur;
			}
			if (prev == null) {
				r.value = 0;
				r.severity = Severity.NORMAL;
				r.message = "집계 시작 (다음 주기부터 판정)";
				return r;
			}
			long total = Math.max(0, cur.requests - prev.requests);
			long err = Math.max(0, cur.status[4] - prev.status[4]);
			r.value = err;
			r.severity = AlertRules.http5xxSeverity(err, total, AlertRules.intOf(s, AlertRules.S_HTTP5XX_MIN), AlertRules.intOf(s, AlertRules.S_HTTP5XX_PERCENT));
			r.message = String.format("구간 요청 %d건 중 5xx %d건 (%.1f%%)", total, err, total > 0 ? err * 100.0 / total : 0.0);
		} catch (Throwable t) {
			unavailable(r, t);
		}
		return r;
	}

	/** 최근 15분 관리자 로그인 실패(비밀번호·OTP) 건수 */
	private Reading readLoginFail(Map<String, String> s) {
		Reading r = new Reading(AlertRules.KEY_LOGIN_FAIL, "관리자 로그인 실패");
		r.unit = "건";
		r.recommendation = "1) 감사 로그에서 대상 ID·IP 확인  2) 특정 IP 집중이면 Cloudflare WAF/Rate Limit 으로 차단  3) 해당 관리자 비밀번호 변경·OTP 상태 확인";
		int pw = audit.countRecentFailures(AdminAuditService.LOGIN_FAIL, LOGIN_FAIL_WINDOW_MIN);
		int otp = audit.countRecentFailures(AdminAuditService.OTP_LOGIN_FAIL, LOGIN_FAIL_WINDOW_MIN);
		if (pw < 0 && otp < 0) {
			r.available = false;
			r.severity = Severity.NORMAL;
			r.message = "감사 로그 조회 불가";
			return r;
		}
		int total = Math.max(0, pw) + Math.max(0, otp);
		r.value = total;
		r.severity = AlertRules.loginFailSeverity(total, AlertRules.intOf(s, AlertRules.S_LOGIN_FAIL_MIN));
		r.message = String.format("최근 %d분 실패 %d건 (비밀번호 %d, OTP %d)", LOGIN_FAIL_WINDOW_MIN, total, Math.max(0, pw), Math.max(0, otp));
		return r;
	}

	// ==========================================
	// 조회 (컨트롤러용)
	// ==========================================

	public List<Reading> lastReadings() {
		return lastReadings;
	}

	public long lastEvaluatedAt() {
		return lastEvaluatedAt;
	}

	public boolean lastEnabled() {
		return lastEnabled;
	}

	public List<State> states() {
		return machine.states();
	}

	/** 열린 경보 수와 최고 등급 (사이드바 배지용) */
	public Map<String, Object> openSummary() {
		int count = 0;
		Severity max = Severity.NORMAL;
		for (State st : machine.states()) {
			if (st.open != Severity.NORMAL) {
				count++;
				if (st.open.atLeast(max)) {
					max = st.open;
				}
			}
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("open", count);
		m.put("maxSeverity", max.name());
		m.put("maxSeverityLabel", max.label());
		m.put("lastEvaluatedAt", lastEvaluatedAt);
		return m;
	}

	public void ack(String key) {
		machine.ack(key);
	}

	// ==========================================
	// 경보 상세 (2-35차)
	// ==========================================

	/** 마지막 정기 평가의 해당 항목 측정값 (권장 조치 문구 포함). 없으면 null */
	public Reading lastReading(String key) {
		for (Reading r : lastReadings) {
			if (r.key.equals(key)) {
				return r;
			}
		}
		return null;
	}

	/**
	 * 한 항목만 지금 다시 측정한다. 상태기계·DB·메일은 건드리지 않는다(조회 전용) —
	 * 운영자가 버튼을 여러 번 눌러도 경보가 열리거나 닫히지 않고, 정기 평가 리듬이 흔들리지 않는다
	 */
	public Reading peek(String key) {
		Map<String, String> settings = store.settings();
		if (AlertRules.KEY_HTTP5XX.equals(key)) {
			return readHttp5xx(settings, false);
		}
		for (Reading r : readAll(settings, key)) {
			return r;
		}
		return null;
	}

	/** 항목의 최근 24시간 [시각(ms), 값] 목록 (오래된 순) */
	public List<double[]> history(String key) {
		synchronized (history) {
			Deque<double[]> q = history.get(key);
			return q == null ? new ArrayList<double[]>() : new ArrayList<double[]>(q);
		}
	}

	private void recordHistory(List<Reading> readings, long now) {
		synchronized (history) {
			for (Reading r : readings) {
				if (!r.available) {
					continue;
				}
				Deque<double[]> q = history.get(r.key);
				if (q == null) {
					q = new ArrayDeque<double[]>();
					history.put(r.key, q);
				}
				q.addLast(new double[] { now, r.value });
				while (q.size() > HISTORY_SIZE) {
					q.removeFirst();
				}
			}
		}
	}

	// ==========================================
	// 헬퍼
	// ==========================================

	private void unavailable(Reading r, Throwable t) {
		r.available = false;
		r.severity = Severity.NORMAL;
		r.message = "측정 실패: " + t.getClass().getSimpleName();
	}

	private String cfg(String key, String def) {
		try {
			String v = (config != null) ? config.getProperty(key) : null;
			return (v == null || v.trim().isEmpty()) ? def : v.trim();
		} catch (Throwable t) {
			return def;
		}
	}

	private static long toLong(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number) {
			return ((Number) v).longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v));
		} catch (Exception e) {
			return def;
		}
	}

	private static void closeQuietly(AutoCloseable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Throwable ignore) {
			}
		}
	}
}

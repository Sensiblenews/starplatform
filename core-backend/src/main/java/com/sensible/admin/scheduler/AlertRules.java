package com.sensible.admin.scheduler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 경보 등급 판정 규칙. 순수 함수만 두어 테스트가 쉽게 한다.
 *
 * 등급은 클라이언트 요건의 4단계: 정상 / 주의(WARNING) / 위험(DANGER) / 긴급(CRITICAL).
 * 기본 임계값도 요건 그대로: 디스크 75/85/95, RAM 80/90/95, CPU 80/90.
 * 실제 서버 사양에 맞춘 조정은 어드민 경보 센터의 설정에서 한다.
 */
public final class AlertRules {

	private AlertRules() {
	}

	/** 등급. 순서가 곧 심각도라서 compareTo 로 비교한다 */
	public enum Severity {
		NORMAL("정상"), WARNING("주의"), DANGER("위험"), CRITICAL("긴급");

		private final String label;

		Severity(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}

		public boolean atLeast(Severity other) {
			return this.ordinal() >= other.ordinal();
		}

		public static Severity parse(String s, Severity def) {
			if (s == null) {
				return def;
			}
			try {
				return Severity.valueOf(s.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				return def;
			}
		}
	}

	/** 감시 항목 키. 열려 있는 경보는 항목당 하나다 */
	public static final String KEY_DISK = "disk";
	public static final String KEY_RAM = "ram";
	public static final String KEY_CPU = "cpu";
	public static final String KEY_HEAP = "heap";
	public static final String KEY_DB = "db";
	public static final String KEY_REDIS = "redis";
	public static final String KEY_HTTP5XX = "http5xx";
	public static final String KEY_LOGIN_FAIL = "loginfail";

	/** 설정 키와 기본값. 어드민 설정 화면·DB 설정 테이블·코드가 같은 목록을 본다 */
	public static final String S_ENABLED = "alert.enabled";
	public static final String S_MAIL_TO = "alert.mail.to";
	public static final String S_MAIL_MIN_SEVERITY = "alert.mail.min.severity";
	public static final String S_DISK_WARN = "alert.disk.warn";
	public static final String S_DISK_DANGER = "alert.disk.danger";
	public static final String S_DISK_CRITICAL = "alert.disk.critical";
	public static final String S_RAM_WARN = "alert.ram.warn";
	public static final String S_RAM_DANGER = "alert.ram.danger";
	public static final String S_RAM_CRITICAL = "alert.ram.critical";
	public static final String S_CPU_WARN = "alert.cpu.warn";
	public static final String S_CPU_DANGER = "alert.cpu.danger";
	public static final String S_HEAP_WARN = "alert.heap.warn";
	public static final String S_HEAP_DANGER = "alert.heap.danger";
	public static final String S_HTTP5XX_MIN = "alert.http5xx.min.count";
	public static final String S_HTTP5XX_PERCENT = "alert.http5xx.percent";
	public static final String S_LOGIN_FAIL_MIN = "alert.loginfail.min.count";
	public static final String S_CONFIRM_COUNT = "alert.confirm.count";
	public static final String S_RECOVER_COUNT = "alert.recover.count";
	public static final String S_REMIND_MINUTES = "alert.remind.minutes";
	public static final String S_REMIND_MAX = "alert.remind.max";

	public static Map<String, String> defaults() {
		Map<String, String> d = new LinkedHashMap<String, String>();
		d.put(S_ENABLED, "Y");
		d.put(S_MAIL_TO, "");
		d.put(S_MAIL_MIN_SEVERITY, "DANGER");
		d.put(S_DISK_WARN, "75");
		d.put(S_DISK_DANGER, "85");
		d.put(S_DISK_CRITICAL, "95");
		d.put(S_RAM_WARN, "80");
		d.put(S_RAM_DANGER, "90");
		d.put(S_RAM_CRITICAL, "95");
		d.put(S_CPU_WARN, "80");
		d.put(S_CPU_DANGER, "90");
		d.put(S_HEAP_WARN, "85");
		d.put(S_HEAP_DANGER, "95");
		d.put(S_HTTP5XX_MIN, "20");
		d.put(S_HTTP5XX_PERCENT, "5");
		d.put(S_LOGIN_FAIL_MIN, "20");
		d.put(S_CONFIRM_COUNT, "2");
		d.put(S_RECOVER_COUNT, "2");
		d.put(S_REMIND_MINUTES, "60");
		d.put(S_REMIND_MAX, "3");
		return d;
	}

	/**
	 * 사용률(%) 3단계 판정. critical 을 0 이하로 주면 2단계(주의/위험)만 쓴다.
	 * 측정 불가(음수)는 정상으로 본다 — 측정 실패를 장애로 알리면 오경보가 된다.
	 */
	public static Severity percentSeverity(double value, int warn, int danger, int critical) {
		if (value < 0) {
			return Severity.NORMAL;
		}
		if (critical > 0 && value >= critical) {
			return Severity.CRITICAL;
		}
		if (danger > 0 && value >= danger) {
			return Severity.DANGER;
		}
		if (warn > 0 && value >= warn) {
			return Severity.WARNING;
		}
		return Severity.NORMAL;
	}

	/**
	 * HTTP 5xx: 구간 내 5xx 건수가 최소 건수 이상이고 전체 요청 대비 비율도 넘을 때 위험.
	 * 건수 조건이 있어야 요청 2건 중 1건 실패(50%) 같은 저트래픽 오경보가 안 난다.
	 */
	public static Severity http5xxSeverity(long count5xx, long total, int minCount, int percent) {
		if (count5xx < 0 || total <= 0) {
			return Severity.NORMAL;
		}
		if (count5xx >= minCount && (count5xx * 100.0 / total) >= percent) {
			return Severity.DANGER;
		}
		return Severity.NORMAL;
	}

	/** 반복 로그인 실패(최근 15분): 최소 건수 이상이면 위험 (계정 공격 징후) */
	public static Severity loginFailSeverity(int failures, int minCount) {
		if (failures < 0) {
			return Severity.NORMAL;
		}
		return (failures >= minCount) ? Severity.DANGER : Severity.NORMAL;
	}

	/** 연결 상태형 항목(DB·Redis): 끊기면 지정 등급, 정상이면 NORMAL */
	public static Severity upDownSeverity(boolean up, Severity whenDown) {
		return up ? Severity.NORMAL : whenDown;
	}

	public static int intOf(Map<String, String> settings, String key) {
		String v = settings.get(key);
		if (v == null) {
			v = defaults().get(key);
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (Exception e) {
			return Integer.parseInt(defaults().get(key));
		}
	}

	public static String strOf(Map<String, String> settings, String key) {
		String v = settings.get(key);
		return (v == null) ? defaults().get(key) : v;
	}
}

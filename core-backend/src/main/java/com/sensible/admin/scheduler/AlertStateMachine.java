package com.sensible.admin.scheduler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sensible.admin.scheduler.AlertRules.Severity;

/**
 * 항목별 경보 상태 전이. 순수 자료구조 — 시간은 밖에서 넣어 준다.
 *
 * 핵심 규칙 (알림 피로도·플래핑 방지):
 *  - 같은 등급이 confirmCount 번 연속 관측돼야 경보를 연다 (기본 2회 = 5분 주기 × 2 = 10분 지속).
 *  - 열린 뒤 더 높은 등급이 confirmCount 번 연속이면 ESCALATE 한 번만 알린다. 낮아지는 건 그냥 유지.
 *  - 정상이 recoverCount 번 연속이면 RESOLVE.
 *  - 긴급(CRITICAL) 이고 확인(ack) 안 된 경보는 remindMinutes 마다 최대 remindMax 회 REMIND.
 *  - 항목당 열린 경보는 하나. 같은 장애로 메일이 반복되지 않는다.
 */
public class AlertStateMachine {

	/** 전이 종류 */
	public enum Kind { OPEN, ESCALATE, RESOLVE, REMIND }

	/** 한 번의 평가에서 생긴 전이 */
	public static class Transition {
		public final String key;
		public final Kind kind;
		public final Severity severity;
		/** RESOLVE 때는 "닫힌 경보의 최고 등급" */
		public final Severity previousSeverity;
		public final double value;
		public final String message;
		public final long openedAt;
		/** 전이 시점의 DB 경보 ID (RESOLVE 는 상태가 지워지기 전 값) */
		public final long alertId;
		/** 이 경보로 메일이 나간 적이 있는지 (RESOLVE 때 복구 메일 발송 판단) */
		public final boolean mailSent;

		Transition(String key, Kind kind, Severity severity, Severity previousSeverity, double value, String message,
				long openedAt, long alertId, boolean mailSent) {
			this.key = key;
			this.kind = kind;
			this.severity = severity;
			this.previousSeverity = previousSeverity;
			this.value = value;
			this.message = message;
			this.openedAt = openedAt;
			this.alertId = alertId;
			this.mailSent = mailSent;
		}

		@Override
		public String toString() {
			return kind + ":" + key + ":" + severity;
		}
	}

	/** 항목 하나의 현재 상태 */
	public static class State {
		public String key;
		/** 열린 경보 등급. NORMAL 이면 열린 경보 없음 */
		public Severity open = Severity.NORMAL;
		/** 열린 경보가 겪은 최고 등급 (복구 메일 본문용) */
		public Severity peak = Severity.NORMAL;
		public long openedAt;
		/** 영속화된 경보 ID (DB). 없으면 0 */
		public long alertId;
		public boolean acked;
		/** 이 경보에 대해 메일이 한 번이라도 나갔는지 (복구 메일 발송 판단) */
		public boolean mailSent;
		public long lastNotifiedAt;
		public int remindCount;
		/** 마지막 관측값·등급·메시지 */
		public double lastValue;
		public Severity lastObserved = Severity.NORMAL;
		public String lastMessage = "";
		public long lastEvaluatedAt;

		/** 후보 등급과 연속 관측 횟수 (열기·격상용) */
		Severity candidate = Severity.NORMAL;
		int candidateCount;
		/** 정상 연속 관측 횟수 (복구용) */
		int normalCount;
	}

	private final Map<String, State> states = new LinkedHashMap<String, State>();

	public synchronized State state(String key) {
		State s = states.get(key);
		if (s == null) {
			s = new State();
			s.key = key;
			states.put(key, s);
		}
		return s;
	}

	public synchronized List<State> states() {
		return new ArrayList<State>(states.values());
	}

	/** 재기동 후 DB의 열린 경보를 복원한다 (중복 OPEN 메일 방지) */
	public synchronized void restore(String key, Severity open, long openedAt, long alertId, boolean acked, boolean mailSent, int notifyCount) {
		State s = state(key);
		s.open = open;
		s.peak = open;
		s.openedAt = openedAt;
		s.alertId = alertId;
		s.acked = acked;
		s.mailSent = mailSent;
		s.remindCount = Math.max(0, notifyCount - 1);
		s.lastNotifiedAt = openedAt;
	}

	public synchronized void ack(String key) {
		State s = states.get(key);
		if (s != null) {
			s.acked = true;
		}
	}

	/**
	 * 관측값 1건을 반영하고 전이 목록을 돌려준다 (없으면 빈 목록).
	 *
	 * @param confirmCount 열기·격상에 필요한 연속 관측 수 (1 이상)
	 * @param recoverCount 복구에 필요한 연속 정상 관측 수 (1 이상)
	 * @param remindMs     긴급 미확인 재알림 간격(ms). 0 이하면 재알림 없음
	 * @param remindMax    재알림 최대 횟수
	 */
	public synchronized List<Transition> apply(String key, Severity observed, double value, String message, long now,
			int confirmCount, int recoverCount, long remindMs, int remindMax) {
		List<Transition> out = new ArrayList<Transition>();
		State s = state(key);
		s.lastValue = value;
		s.lastObserved = observed;
		s.lastMessage = (message == null) ? "" : message;
		s.lastEvaluatedAt = now;
		int confirm = Math.max(1, confirmCount);
		int recover = Math.max(1, recoverCount);

		if (observed == Severity.NORMAL) {
			s.candidate = Severity.NORMAL;
			s.candidateCount = 0;
			if (s.open != Severity.NORMAL) {
				s.normalCount++;
				if (s.normalCount >= recover) {
					out.add(new Transition(key, Kind.RESOLVE, Severity.NORMAL, s.peak, value, s.lastMessage, s.openedAt, s.alertId, s.mailSent));
					s.open = Severity.NORMAL;
					s.peak = Severity.NORMAL;
					s.openedAt = 0;
					s.alertId = 0;
					s.acked = false;
					s.mailSent = false;
					s.lastNotifiedAt = 0;
					s.remindCount = 0;
					s.normalCount = 0;
				}
			}
			return out;
		}

		// 비정상 관측
		s.normalCount = 0;
		if (observed.atLeast(s.open) && observed != s.open) {
			// 열린 등급보다 높다 → 후보로 연속 집계
			if (s.candidate == observed) {
				s.candidateCount++;
			} else {
				s.candidate = observed;
				s.candidateCount = 1;
			}
			if (s.candidateCount >= confirm) {
				Kind kind = (s.open == Severity.NORMAL) ? Kind.OPEN : Kind.ESCALATE;
				Severity before = s.open;
				if (kind == Kind.OPEN) {
					s.openedAt = now;
					s.acked = false;
					s.remindCount = 0;
				}
				s.open = observed;
				if (observed.atLeast(s.peak)) {
					s.peak = observed;
				}
				s.lastNotifiedAt = now;
				s.candidate = Severity.NORMAL;
				s.candidateCount = 0;
				out.add(new Transition(key, kind, observed, before, value, s.lastMessage, s.openedAt, s.alertId, s.mailSent));
				return out;
			}
		} else {
			// 열린 등급 이하의 비정상: 유지. 격상 후보는 리셋
			s.candidate = Severity.NORMAL;
			s.candidateCount = 0;
		}

		// 긴급 미확인 재알림
		if (s.open == Severity.CRITICAL && !s.acked && s.mailSent && remindMs > 0
				&& s.remindCount < remindMax && now - s.lastNotifiedAt >= remindMs) {
			s.remindCount++;
			s.lastNotifiedAt = now;
			out.add(new Transition(key, Kind.REMIND, s.open, s.open, value, s.lastMessage, s.openedAt, s.alertId, s.mailSent));
		}
		return out;
	}

	/** 메일이 실제로 나간 뒤 호출 — 복구 메일·재알림 판단에 쓴다 */
	public synchronized void markMailSent(String key, long now) {
		State s = state(key);
		s.mailSent = true;
		s.lastNotifiedAt = now;
	}

	public synchronized void bindAlertId(String key, long alertId) {
		state(key).alertId = alertId;
	}
}

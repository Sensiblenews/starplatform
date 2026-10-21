package com.sensible.admin.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sensible.admin.domain.UserVO;
import com.sensible.admin.scheduler.AlertRules;
import com.sensible.admin.scheduler.AlertRules.Severity;
import com.sensible.admin.scheduler.AlertStateMachine.State;
import com.sensible.admin.scheduler.SystemAlertMonitor;
import com.sensible.admin.scheduler.SystemAlertMonitor.Reading;
import com.sensible.admin.service.AdminAuditService;
import com.sensible.admin.service.AlertMailService;
import com.sensible.admin.service.SystemAlertService;
import com.sensible.common.util.AdminSessionUtil;

/**
 * 경보 센터 (SM 전용): 서버 현황 등급·열린 경보·이력·설정·시험 메일.
 */
@Controller
public class SuperAlertController {

	private static final Logger logger = LoggerFactory.getLogger(SuperAlertController.class);

	/** 시험 메일 결과를 기다리는 상한(ms). SMTP 타임아웃(10초)보다 여유 있게 */
	private static final long TEST_MAIL_WAIT_MS = 25000L;

	@Resource(name = "systemAlertService")
	private SystemAlertService store;

	@Resource(name = "alertMailService")
	private AlertMailService mailer;

	@Resource(name = "adminAuditService")
	private AdminAuditService audit;

	@Resource(name = "systemAlertMonitor")
	private SystemAlertMonitor monitor;

	/** [화면] 경보 센터. alertId 가 붙어 오면(메일 링크 설정이 다른 주소일 때) 그 경보의 상세로 보낸다 */
	@RequestMapping(value = "/super/alert/center.do")
	public String center(HttpServletRequest request, Model model,
			@RequestParam(value = "alertId", required = false) Long alertId) {
		UserVO user = sm(request);
		if (user == null) {
			return "redirect:/super/dashboard.do";
		}
		if (alertId != null && alertId > 0) {
			return "redirect:/super/alert/detail.do?id=" + alertId;
		}
		Map<String, String> settings = store.settings();
		model.addAttribute("activeMenu", "alert_center");
		model.addAttribute("settings", settings);
		model.addAttribute("defaults", AlertRules.defaults());
		model.addAttribute("mailAvailable", mailer.isAvailable());
		model.addAttribute("platformId", mailer.platformId());
		model.addAttribute("recipients", AlertMailService.parseRecipients(AlertRules.strOf(settings, AlertRules.S_MAIL_TO)));
		model.addAttribute("lastEvaluatedAt", monitor.lastEvaluatedAt());
		model.addAttribute("monitorEnabled", monitor.lastEnabled());
		model.addAttribute("readings", readingRows());
		try {
			model.addAttribute("openAlerts", store.openAlerts());
			model.addAttribute("recentAlerts", store.recentAlerts(30));
			model.addAttribute("recentEvents", store.recentEvents(60));
		} catch (Exception e) {
			logger.error("[Alert] center data error: {}", e.toString());
			model.addAttribute("storageError", true);
		}
		return "super/alert_center";
	}

	/** 측정값 + 상태기계 상태를 화면용 행으로 합친다 */
	private List<Map<String, Object>> readingRows() {
		Map<String, State> states = new HashMap<String, State>();
		for (State s : monitor.states()) {
			states.put(s.key, s);
		}
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		for (Reading r : monitor.lastReadings()) {
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			row.put("key", r.key);
			row.put("label", r.label);
			row.put("valueText", r.valueText());
			row.put("severity", r.severity != null ? r.severity.name() : "NORMAL");
			row.put("severityLabel", r.severity != null ? r.severity.label() : "정상");
			row.put("message", r.message);
			row.put("available", r.available);
			State st = states.get(r.key);
			row.put("open", st != null && st.open != Severity.NORMAL ? st.open.name() : null);
			row.put("openLabel", st != null && st.open != Severity.NORMAL ? st.open.label() : null);
			row.put("acked", st != null && st.acked);
			row.put("alertId", st != null ? st.alertId : 0L);
			row.put("openedAt", st != null ? st.openedAt : 0L);
			rows.add(row);
		}
		return rows;
	}

	// ==========================================
	// 경보 상세 + 원클릭 조치 (2-35차)
	// 메일 링크 → 로그인·OTP → 이 화면. 현재 상태·24시간 그래프·권장 조치·경과를 한 화면에 두고,
	// 서버 상태를 망가뜨릴 수 없는 조치만 버튼으로 둔다 (재시작·차단·백업 실행은 의도적으로 없음).
	// ==========================================

	/** 원클릭 조치 화이트리스트. 브라우저가 보낸 다른 값은 거부한다 */
	static final List<String> ACTIONS = java.util.Arrays.asList("RECHECK", "GC", "NOTE");

	/** 조치 메모 최대 길이 (이력 MESSAGE 컬럼 1000자 안) */
	static final int NOTE_MAX = 500;

	/** [화면] 경보 상세 */
	@RequestMapping(value = "/super/alert/detail.do")
	public String detail(HttpServletRequest request, Model model, @RequestParam("id") long alertId) {
		UserVO user = sm(request);
		if (user == null) {
			return "redirect:/super/dashboard.do";
		}
		Map<String, Object> alert;
		try {
			alert = store.alert(alertId);
		} catch (Exception e) {
			logger.error("[Alert] detail error: {}", e.toString());
			alert = null;
		}
		if (alert == null) {
			return "redirect:/super/alert/center.do";
		}
		String key = String.valueOf(alert.get("ALERT_KEY"));
		Reading reading = monitor.lastReading(key);
		Map<String, String> settings = store.settings();

		model.addAttribute("activeMenu", "alert_center");
		model.addAttribute("alert", alert);
		model.addAttribute("alertKey", key);
		model.addAttribute("reading", reading);
		model.addAttribute("steps", recommendationSteps(reading != null ? reading.recommendation : null));
		model.addAttribute("thresholds", thresholds(key, settings));
		model.addAttribute("historyJson", historyJson(monitor.history(key)));
		model.addAttribute("mailAvailable", mailer.isAvailable());
		model.addAttribute("recipients", AlertMailService.parseRecipients(AlertRules.strOf(settings, AlertRules.S_MAIL_TO)));
		try {
			model.addAttribute("events", store.eventsOf(alertId));
		} catch (Exception e) {
			logger.error("[Alert] detail events error: {}", e.toString());
			model.addAttribute("events", new ArrayList<Map<String, Object>>());
		}
		return "super/alert_detail";
	}

	/**
	 * [API] 원클릭 조치. action = RECHECK(이 항목 다시 측정) | GC(메모리 정리) | NOTE(조치 메모)
	 * 확인 처리·시험 메일은 기존 ack.do·testMail.do 를 그대로 쓴다.
	 * 모든 조치는 경보 이력(ACTION)과 감사 로그(ALERT_ACTION)에 실행자·시각·결과가 남는다.
	 */
	@RequestMapping(value = "/super/alert/action.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> action(HttpServletRequest request, @RequestParam("alertId") long alertId,
			@RequestParam("action") String action, @RequestParam(value = "note", required = false) String note) {
		Map<String, Object> result = new HashMap<String, Object>();
		UserVO user = sm(request);
		if (user == null) {
			return denied(result);
		}
		if (!ACTIONS.contains(action)) {
			result.put("status", "fail");
			result.put("msg", "허용되지 않은 조치입니다.");
			return result;
		}
		Map<String, Object> alert;
		try {
			alert = store.alert(alertId);
		} catch (Exception e) {
			alert = null;
		}
		if (alert == null) {
			result.put("status", "fail");
			result.put("msg", "경보를 찾을 수 없습니다.");
			return result;
		}
		String key = String.valueOf(alert.get("ALERT_KEY"));
		String severity = String.valueOf(alert.get("SEVERITY"));
		String message;
		try {
			if ("RECHECK".equals(action)) {
				Reading r = monitor.peek(key);
				if (r == null) {
					result.put("status", "fail");
					result.put("msg", "이 항목은 다시 측정할 수 없습니다.");
					return result;
				}
				message = "다시 측정: " + r.valueText() + " (" + (r.severity != null ? r.severity.label() : "정상") + ") " + r.message;
				result.put("valueText", r.valueText());
				result.put("severity", r.severity != null ? r.severity.name() : "NORMAL");
				result.put("severityLabel", r.severity != null ? r.severity.label() : "정상");
				result.put("detail", r.message);
			} else if ("GC".equals(action)) {
				java.lang.management.MemoryMXBean mx = java.lang.management.ManagementFactory.getMemoryMXBean();
				long before = mx.getHeapMemoryUsage().getUsed();
				System.gc();
				Thread.sleep(150);
				long after = mx.getHeapMemoryUsage().getUsed();
				long freedMb = Math.max(0, (before - after) / (1024 * 1024));
				message = "메모리 정리(GC): 약 " + freedMb + "MB 해제 (힙 " + (after / (1024 * 1024)) + "MB 사용 중)";
			} else {
				String text = note == null ? "" : note.trim();
				if (text.isEmpty()) {
					result.put("status", "fail");
					result.put("msg", "조치 내용을 입력하세요.");
					return result;
				}
				if (text.length() > NOTE_MAX) {
					text = text.substring(0, NOTE_MAX);
				}
				message = "조치 메모: " + text;
			}
		} catch (Exception e) {
			logger.error("[Alert] action {} error: {}", action, e.toString());
			store.event(alertId, key, "ACTION", severity, action + " 실패: " + e.getClass().getSimpleName(), user.getPRS_ID());
			audit.log(user.getPRS_ID(), AdminAuditService.ALERT_ACTION, false, action, null, request, "경보 #" + alertId + " " + action + " 실패");
			result.put("status", "error");
			result.put("msg", "조치 실행 중 오류가 발생했습니다.");
			return result;
		}
		store.event(alertId, key, "ACTION", severity, message, user.getPRS_ID());
		audit.log(user.getPRS_ID(), AdminAuditService.ALERT_ACTION, true, null, null, request, "경보 #" + alertId + " (" + key + ") " + message);
		result.put("status", "success");
		result.put("msg", message);
		return result;
	}

	/** "1) …  2) …" 권장 조치 문구를 단계 목록으로 (순수 함수) */
	static List<String> recommendationSteps(String recommendation) {
		List<String> steps = new ArrayList<String>();
		if (recommendation == null || recommendation.trim().isEmpty()) {
			return steps;
		}
		for (String part : recommendation.trim().split("\\s+(?=\\d+\\)\\s)")) {
			String step = part.replaceFirst("^\\d+\\)\\s*", "").trim();
			if (!step.isEmpty()) {
				steps.add(step);
			}
		}
		return steps;
	}

	/** 그래프에 그을 기준선 (주의·위험·긴급 %). 비율 항목이 아니면 빈 맵 */
	private static Map<String, Integer> thresholds(String key, Map<String, String> settings) {
		Map<String, Integer> t = new LinkedHashMap<String, Integer>();
		String prefix = null;
		if (AlertRules.KEY_DISK.equals(key)) prefix = "alert.disk";
		else if (AlertRules.KEY_RAM.equals(key)) prefix = "alert.ram";
		else if (AlertRules.KEY_CPU.equals(key)) prefix = "alert.cpu";
		else if (AlertRules.KEY_HEAP.equals(key)) prefix = "alert.heap";
		if (prefix == null) {
			return t;
		}
		for (String level : new String[] { "warn", "danger", "critical" }) {
			String k = prefix + "." + level;
			if (AlertRules.defaults().containsKey(k)) {
				t.put(level, AlertRules.intOf(settings, k));
			}
		}
		return t;
	}

	/** [[시각ms, 값], …] JSON (숫자만이라 손으로 만든다) */
	static String historyJson(List<double[]> points) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < points.size(); i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append('[').append((long) points.get(i)[0]).append(',').append(Math.round(points.get(i)[1] * 10) / 10.0).append(']');
		}
		return sb.append(']').toString();
	}

	/** [API] 사이드바 배지용 열린 경보 요약. SM 외에는 0 */
	@RequestMapping(value = "/super/alert/count.json", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> count(HttpServletRequest request) {
		if (sm(request) == null) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("open", 0);
			return m;
		}
		return monitor.openSummary();
	}

	/** [API] 지금 점검 (스케줄을 기다리지 않고 1회 평가) */
	@RequestMapping(value = "/super/alert/evaluate.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> evaluateNow(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<String, Object>();
		if (sm(request) == null) {
			return denied(result);
		}
		try {
			monitor.evaluateNow();
			result.put("status", "success");
			result.put("readings", readingRows());
			result.put("summary", monitor.openSummary());
		} catch (Exception e) {
			logger.error("[Alert] evaluateNow error: {}", e.toString());
			result.put("status", "error");
			result.put("msg", "점검 실행 중 오류: " + e.getClass().getSimpleName());
		}
		return result;
	}

	/** [API] 담당자 확인 (복구와 별개 — 긴급 재알림이 멈춘다) */
	@RequestMapping(value = "/super/alert/ack.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> ack(HttpServletRequest request, @RequestParam("alertId") long alertId) {
		Map<String, Object> result = new HashMap<String, Object>();
		UserVO user = sm(request);
		if (user == null) {
			return denied(result);
		}
		try {
			Map<String, Object> alert = store.alert(alertId);
			if (alert == null) {
				result.put("status", "fail");
				result.put("msg", "경보를 찾을 수 없습니다.");
				return result;
			}
			int n = store.ackAlert(alertId, user.getPRS_ID());
			String key = String.valueOf(alert.get("ALERT_KEY"));
			monitor.ack(key);
			store.event(alertId, key, "ACK", String.valueOf(alert.get("SEVERITY")), "담당자 확인", user.getPRS_ID());
			audit.log(user.getPRS_ID(), AdminAuditService.ALERT_ACK, true, null, null, request, "경보 #" + alertId + " (" + key + ") 확인");
			result.put("status", "success");
			result.put("msg", n > 0 ? "확인 처리되었습니다." : "이미 확인된 경보입니다.");
		} catch (Exception e) {
			logger.error("[Alert] ack error: {}", e.toString());
			result.put("status", "error");
			result.put("msg", "확인 처리 중 오류가 발생했습니다.");
		}
		return result;
	}

	/** [API] 설정 저장. 변경된 키만 저장하고 전후 값을 이력·감사 로그에 남긴다 */
	@RequestMapping(value = "/super/alert/settings.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> saveSettings(HttpServletRequest request, @RequestParam Map<String, String> params) {
		Map<String, Object> result = new HashMap<String, Object>();
		UserVO user = sm(request);
		if (user == null) {
			return denied(result);
		}
		Map<String, String> current = store.settings();
		List<String> errors = new ArrayList<String>();
		List<String> changes = new ArrayList<String>();
		try {
			for (String key : AlertRules.defaults().keySet()) {
				if (!params.containsKey(key)) {
					continue;
				}
				String raw = params.get(key) == null ? "" : params.get(key).trim();
				String normalized = validate(key, raw, errors);
				if (normalized == null) {
					continue;
				}
				String before = current.get(key);
				if (!normalized.equals(before)) {
					store.saveSetting(key, normalized, user.getPRS_ID());
					changes.add(key + ": " + mask(key, before) + " → " + mask(key, normalized));
				}
			}
		} catch (Exception e) {
			logger.error("[Alert] settings save error: {}", e.toString());
			result.put("status", "error");
			result.put("msg", "설정 저장 중 오류가 발생했습니다. 설정 테이블(sys-alert.sql) 적용 여부를 확인하세요.");
			return result;
		}
		if (!changes.isEmpty()) {
			String summary = join(changes, "; ");
			store.event(0, null, "SETTING_CHANGED", null, summary, user.getPRS_ID());
			audit.log(user.getPRS_ID(), AdminAuditService.ALERT_SETTING_CHANGED, true, null, null, request, summary);
		}
		if (!errors.isEmpty()) {
			result.put("status", "fail");
			result.put("msg", "일부 값이 올바르지 않아 저장하지 않았습니다: " + join(errors, ", ")
					+ (changes.isEmpty() ? "" : " (나머지 " + changes.size() + "건은 저장됨)"));
			return result;
		}
		result.put("status", "success");
		result.put("msg", changes.isEmpty() ? "변경된 항목이 없습니다." : changes.size() + "개 항목을 저장했습니다.");
		result.put("changes", changes);
		return result;
	}

	/** 키별 값 검증. 통과하면 정규화된 값, 실패면 null + errors 에 추가 */
	static String validate(String key, String raw, List<String> errors) {
		if (AlertRules.S_ENABLED.equals(key)) {
			return "Y".equalsIgnoreCase(raw) ? "Y" : "N";
		}
		if (AlertRules.S_MAIL_TO.equals(key)) {
			List<String> list = AlertMailService.parseRecipients(raw);
			if (!raw.isEmpty() && list.isEmpty()) {
				errors.add("수신자 형식");
				return null;
			}
			return join(list, ",");
		}
		if (AlertRules.S_MAIL_MIN_SEVERITY.equals(key)) {
			Severity s = Severity.parse(raw, null);
			if (s == null || s == Severity.NORMAL) {
				errors.add("메일 최소 등급");
				return null;
			}
			return s.name();
		}
		try {
			int v = Integer.parseInt(raw);
			if (key.endsWith(".warn") || key.endsWith(".danger") || key.endsWith(".critical") || key.endsWith(".percent")) {
				if (v < 0 || v > 100) {
					errors.add(key + " (0~100)");
					return null;
				}
			} else if (v < 0 || v > 100000) {
				errors.add(key + " (0 이상)");
				return null;
			}
			return String.valueOf(v);
		} catch (NumberFormatException e) {
			errors.add(key + " (숫자)");
			return null;
		}
	}

	/** [API] 시험 메일. 실제 수신 확인은 운영자 몫 — 결과는 이력에 TEST_MAIL 로 남긴다 */
	@RequestMapping(value = "/super/alert/testMail.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> testMail(HttpServletRequest request,
			@RequestParam(value = "to", required = false) String to) {
		Map<String, Object> result = new HashMap<String, Object>();
		UserVO user = sm(request);
		if (user == null) {
			return denied(result);
		}
		List<String> recipients = AlertMailService.parseRecipients(to);
		if (recipients.isEmpty()) {
			recipients = AlertMailService.parseRecipients(AlertRules.strOf(store.settings(), AlertRules.S_MAIL_TO));
		}
		if (recipients.isEmpty()) {
			result.put("status", "fail");
			result.put("msg", "수신자를 먼저 설정하세요.");
			return result;
		}
		String error = mailer.sendTestMail(recipients, TEST_MAIL_WAIT_MS);
		boolean ok = (error == null);
		store.event(0, null, "TEST_MAIL", null, (ok ? "시험 메일 발송 → " : "시험 메일 실패 → ") + join(recipients, ",") + (ok ? "" : " : " + error), user.getPRS_ID());
		audit.log(user.getPRS_ID(), AdminAuditService.ALERT_TEST_MAIL, ok, ok ? null : "SMTP", null, request,
				"수신자 " + recipients.size() + "명" + (ok ? "" : " / " + error));
		result.put("status", ok ? "success" : "fail");
		result.put("msg", ok ? "발송 요청이 SMTP 서버에 접수되었습니다. 휴대전화 메일 앱에서 실제 수신을 확인하세요." : "발송 실패: " + error);
		return result;
	}

	// ==========================================
	// 헬퍼
	// ==========================================

	private UserVO sm(HttpServletRequest request) {
		UserVO u = AdminSessionUtil.currentUser(request);
		return (u != null && "SM".equals(u.getPRS_AUTH())) ? u : null;
	}

	private static Map<String, Object> denied(Map<String, Object> result) {
		result.put("status", "fail");
		result.put("msg", "권한이 없습니다.");
		return result;
	}

	/** 수신자 주소는 이력에 그대로 남겨도 되지만 길면 줄인다 */
	private static String mask(String key, String v) {
		if (v == null) {
			return "(없음)";
		}
		return v.length() > 60 ? v.substring(0, 60) + "…" : v;
	}

	private static String join(List<String> list, String sep) {
		StringBuilder sb = new StringBuilder();
		for (String s : list) {
			if (sb.length() > 0) {
				sb.append(sep);
			}
			sb.append(s);
		}
		return sb.toString();
	}
}

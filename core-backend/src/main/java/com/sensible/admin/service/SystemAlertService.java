package com.sensible.admin.service;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sensible.admin.scheduler.AlertRules;
import com.sensible.common.dao.DefaultDAO;

/**
 * 경보·이력·설정 영속화 (WH_SYS_ALERT / WH_SYS_ALERT_EVENT / WH_SYS_ALERT_SETTING).
 *
 * DB 가 끊긴 상황도 경보 대상이므로, 여기 메서드는 실패해도 예외를 밖으로 내지 않고
 * 로그만 남긴다. 감시·메일 발송은 메모리 상태로 계속 간다.
 */
@Service("systemAlertService")
public class SystemAlertService {

	private static final Logger logger = LoggerFactory.getLogger(SystemAlertService.class);

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	/** 마지막으로 성공적으로 읽은 설정 (DB 장애 시 사용) */
	private volatile Map<String, String> settingsCache = new LinkedHashMap<String, String>(AlertRules.defaults());

	// ==========================================
	// 설정
	// ==========================================

	/** 기본값 위에 DB 값을 덮어 돌려준다. DB 실패 시 직전 캐시 */
	public Map<String, String> settings() {
		try {
			List<Map<String, Object>> rows = dao.selectList("sysalert.selectSettings", null);
			Map<String, String> merged = new LinkedHashMap<String, String>(AlertRules.defaults());
			if (rows != null) {
				for (Map<String, Object> r : rows) {
					Object k = r.get("SETTING_KEY");
					Object v = r.get("SETTING_VALUE");
					if (k != null && v != null) {
						merged.put(String.valueOf(k), String.valueOf(v));
					}
				}
			}
			settingsCache = merged;
			return merged;
		} catch (Throwable t) {
			logger.warn("[Alert] 설정 조회 실패, 캐시 사용: {}", t.toString());
			return settingsCache;
		}
	}

	public void saveSetting(String key, String value, String updatedBy) throws Exception {
		Map<String, Object> p = new HashMap<String, Object>();
		p.put("SETTING_KEY", key);
		p.put("SETTING_VALUE", value == null ? "" : value);
		p.put("UPDATED_BY", updatedBy);
		dao.update("sysalert.upsertSetting", p);
	}

	// ==========================================
	// 경보
	// ==========================================

	/**
	 * 열린 경보 생성. 생성된 ID 반환, 실패 시 0
	 *
	 * @param openedAt 발생 시각(ms) — 상태 머신이 계산한 첫 초과 관측 시각. 0 이하면 지금 (2026-10-23)
	 */
	public long openAlert(String key, String severity, String title, String message, double value, String platformId,
			long openedAt) {
		try {
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("ALERT_KEY", key);
			p.put("SEVERITY", severity);
			p.put("TITLE", title);
			p.put("MESSAGE", message);
			p.put("METRIC_VALUE", value);
			p.put("PLATFORM_ID", platformId);
			p.put("OPENED_DATE", new Date(openedAt > 0 ? openedAt : System.currentTimeMillis()));
			dao.insert("sysalert.insertAlert", p);
			Object id = p.get("ALERT_ID");
			return (id instanceof Number) ? ((Number) id).longValue() : 0L;
		} catch (Throwable t) {
			logger.warn("[Alert] 경보 저장 실패 key={}: {}", key, t.toString());
			return 0L;
		}
	}

	public void updateAlert(long alertId, String severity, String message, double value) {
		if (alertId <= 0) {
			return;
		}
		try {
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("ALERT_ID", alertId);
			p.put("SEVERITY", severity);
			p.put("MESSAGE", message);
			p.put("METRIC_VALUE", value);
			dao.update("sysalert.updateAlert", p);
		} catch (Throwable t) {
			logger.warn("[Alert] 경보 갱신 실패 id={}: {}", alertId, t.toString());
		}
	}

	public void resolveAlert(long alertId, double value) {
		if (alertId <= 0) {
			return;
		}
		try {
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("ALERT_ID", alertId);
			p.put("METRIC_VALUE", value);
			dao.update("sysalert.resolveAlert", p);
		} catch (Throwable t) {
			logger.warn("[Alert] 경보 복구 저장 실패 id={}: {}", alertId, t.toString());
		}
	}

	public void markMailSent(long alertId) {
		if (alertId <= 0) {
			return;
		}
		try {
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("ALERT_ID", alertId);
			dao.update("sysalert.markMailSent", p);
		} catch (Throwable t) {
			logger.warn("[Alert] 메일 플래그 저장 실패 id={}: {}", alertId, t.toString());
		}
	}

	/** 담당자 확인. 성공 1 */
	public int ackAlert(long alertId, String by) throws Exception {
		Map<String, Object> p = new HashMap<String, Object>();
		p.put("ALERT_ID", alertId);
		p.put("ACK_BY", by);
		return dao.update("sysalert.ackAlert", p);
	}

	public List<Map<String, Object>> openAlerts() {
		try {
			return dao.selectList("sysalert.selectOpenAlerts", null);
		} catch (Throwable t) {
			logger.warn("[Alert] 열린 경보 조회 실패: {}", t.toString());
			return null;
		}
	}

	public List<Map<String, Object>> recentAlerts(int limit) throws Exception {
		Map<String, Object> p = new HashMap<String, Object>();
		p.put("limit", limit);
		return dao.selectList("sysalert.selectRecentAlerts", p);
	}

	public Map<String, Object> alert(long alertId) throws Exception {
		Map<String, Object> p = new HashMap<String, Object>();
		p.put("ALERT_ID", alertId);
		return dao.selectOne("sysalert.selectAlert", p);
	}

	// ==========================================
	// 이력
	// ==========================================

	public void event(long alertId, String key, String eventType, String severity, String message, String actor) {
		try {
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("ALERT_ID", alertId > 0 ? alertId : null);
			p.put("ALERT_KEY", key);
			p.put("EVENT_TYPE", eventType);
			p.put("SEVERITY", severity);
			p.put("MESSAGE", AdminAuditService.truncate(message, 1000));
			p.put("ACTOR", actor);
			dao.insert("sysalert.insertEvent", p);
		} catch (Throwable t) {
			logger.warn("[Alert] 이력 저장 실패 type={}: {}", eventType, t.toString());
		}
	}

	public List<Map<String, Object>> recentEvents(int limit) throws Exception {
		Map<String, Object> p = new HashMap<String, Object>();
		p.put("limit", limit);
		return dao.selectList("sysalert.selectRecentEvents", p);
	}

	/** 경보 한 건의 경과 (발생·격상·메일·확인·조치·복구) — 오래된 순 (2-35차 경보 상세) */
	public List<Map<String, Object>> eventsOf(long alertId) throws Exception {
		Map<String, Object> p = new HashMap<String, Object>();
		p.put("ALERT_ID", alertId);
		return dao.selectList("sysalert.selectEventsOfAlert", p);
	}
}

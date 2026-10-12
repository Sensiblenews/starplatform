package com.sensible.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sensible.common.dao.DefaultDAO;
import com.sensible.common.util.ClientIpUtil;

/**
 * 관리자 보안 감사 로그 (WH_ADMIN_AUDIT_LOG).
 *
 * 기록 원칙:
 *  - 비밀키·OTP 번호·복구 코드 원문·비밀번호·토큰은 어떤 칸에도 넣지 않는다.
 *    detail 에는 사유 코드와 사람이 읽을 짧은 설명만 둔다.
 *  - 기록 실패가 본 기능(로그인 등)을 막으면 안 되므로 예외를 밖으로 던지지 않는다.
 */
@Service("adminAuditService")
public class AdminAuditService {

	private static final Logger logger = LoggerFactory.getLogger(AdminAuditService.class);

	// 이벤트 종류 — 클라이언트 요건의 목록 + 비밀번호 단계 로그인 결과
	public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
	public static final String LOGIN_FAIL = "LOGIN_FAIL";
	public static final String OTP_SETUP_STARTED = "OTP_SETUP_STARTED";
	public static final String OTP_REGISTER = "OTP_REGISTER";
	public static final String OTP_LOGIN_SUCCESS = "OTP_LOGIN_SUCCESS";
	public static final String OTP_LOGIN_FAIL = "OTP_LOGIN_FAIL";
	public static final String OTP_LOCKED = "OTP_LOCKED";
	public static final String OTP_BACKUP_CODE_USED = "OTP_BACKUP_CODE_USED";
	public static final String OTP_BACKUP_CODE_REISSUED = "OTP_BACKUP_CODE_REISSUED";
	public static final String OTP_RESET = "OTP_RESET";
	public static final String OTP_DISABLED = "OTP_DISABLED";
	public static final String ADMIN_MFA_POLICY_CHANGED = "ADMIN_MFA_POLICY_CHANGED";
	public static final String ALERT_SETTING_CHANGED = "ALERT_SETTING_CHANGED";
	public static final String ALERT_ACK = "ALERT_ACK";
	public static final String ALERT_TEST_MAIL = "ALERT_TEST_MAIL";

	private static final int MAX_DETAIL = 500;
	private static final int MAX_UA = 300;

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	/**
	 * @param prsId     행위 주체 관리자 ID (로그인 실패처럼 확인 안 된 ID도 그대로 기록)
	 * @param eventType 위 상수 중 하나
	 * @param success   성공/실패
	 * @param reasonCode 실패 사유 코드 (성공이면 null 가능)
	 * @param targetId  대상 관리자 ID (재설정 등 타인 대상 작업일 때)
	 * @param request   IP·UA 추출용 (null 허용)
	 * @param detail    사람이 읽을 짧은 설명. 비밀값 금지
	 */
	public void log(String prsId, String eventType, boolean success, String reasonCode,
			String targetId, HttpServletRequest request, String detail) {
		try {
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("PRS_ID", truncate(prsId, 100));
			row.put("EVENT_TYPE", eventType);
			row.put("RESULT", success ? "S" : "F");
			row.put("REASON_CODE", truncate(reasonCode, 40));
			row.put("TARGET_ID", truncate(targetId, 100));
			row.put("CLIENT_IP", (request != null) ? truncate(ClientIpUtil.getClientIp(request), 45) : null);
			row.put("USER_AGENT", (request != null) ? truncate(request.getHeader("User-Agent"), MAX_UA) : null);
			row.put("DETAIL", truncate(detail, MAX_DETAIL));
			dao.insert("security.insertAuditLog", row);
		} catch (Throwable t) {
			// 감사 테이블 미생성 등. 기능은 계속 가야 하지만 흔적은 서버 로그에 남긴다
			// slf4j 구버전: 인자 3개는 Object[] 로 넘겨야 Marker 오버로드와 섞이지 않는다
			logger.error("[Audit] 기록 실패 event={} prsId={} : {}", new Object[] { eventType, prsId, t.toString() });
		}
	}

	public List<Map<String, Object>> list(int start, int length, String prsId, String eventType) throws Exception {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("start", start);
		params.put("length", length);
		params.put("prsId", emptyToNull(prsId));
		params.put("eventType", emptyToNull(eventType));
		return dao.selectList("security.selectAuditLogs", params);
	}

	public int count(String prsId, String eventType) throws Exception {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("prsId", emptyToNull(prsId));
		params.put("eventType", emptyToNull(eventType));
		Integer n = dao.selectOne("security.countAuditLogs", params);
		return (n == null) ? 0 : n;
	}

	/** 최근 N분 내 특정 이벤트 실패 횟수 (로그인 반복 실패 감지용) */
	public int countRecentFailures(String eventType, int minutes) {
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("eventType", eventType);
			params.put("minutes", minutes);
			Integer n = dao.selectOne("security.countRecentFailures", params);
			return (n == null) ? 0 : n;
		} catch (Throwable t) {
			return -1;
		}
	}

	static String truncate(String s, int max) {
		if (s == null) {
			return null;
		}
		return (s.length() > max) ? s.substring(0, max) : s;
	}

	private static String emptyToNull(String s) {
		return (s == null || s.trim().isEmpty()) ? null : s.trim();
	}
}

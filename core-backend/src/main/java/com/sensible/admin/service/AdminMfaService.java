package com.sensible.admin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sensible.admin.domain.UserVO;
import com.sensible.common.dao.DefaultDAO;
import com.sensible.common.util.SecretCryptoUtil;
import com.sensible.common.util.TotpUtil;

/**
 * 관리자(SM·LC) Google OTP(TOTP) 이중인증.
 *
 * 저장: WH_ADMIN_MFA(계정당 1행) + WH_ADMIN_MFA_BACKUP_CODE(복구 코드 해시) + WH_ADMIN_AUDIT_LOG.
 * 비밀키는 AES-GCM 으로 암호화해 저장하고, 암호화 키는 서버 설정에서만 읽는다.
 *
 * 설정 키 (globals.properties, 전부 생략 가능):
 *  - otp.encryption.key : 비밀키 암호화 키 재료. 환경변수 WITCH_OTP_KEY, 시스템 속성 witch.otp.key 도 인정.
 *                         **없으면 이중인증 전체가 비활성**(기존 로그인 그대로) + ERROR 로그 + 어드민 배너.
 *                         설정 없이 배포했을 때 관리자 전원이 잠기는 사고를 막기 위한 선택이다.
 *  - otp.mode           : required(기본) / optional / off
 *  - otp.required.auth  : 의무 대상 권한. 기본 "SM,LC" (스타 ST 는 대상 아님)
 *  - otp.issuer         : 인증 앱에 표시되는 발급자명. 기본 "StarPlatform Admin"
 */
@Service("adminMfaService")
public class AdminMfaService {

	private static final Logger logger = LoggerFactory.getLogger(AdminMfaService.class);

	/** 세션 키: 1차 인증 통과 후 OTP 대기 상태 */
	public static final String SESSION_PENDING = "SUPER_MFA_PENDING";

	public static final String MODE_REQUIRED = "required";
	public static final String MODE_OPTIONAL = "optional";
	public static final String MODE_OFF = "off";

	/** 연속 실패 상한 → 잠금 */
	public static final int MAX_FAILS = 5;
	/** 잠금 시간(분) */
	public static final int LOCK_MINUTES = 10;
	/** 등록 시작 후 코드 확인까지 허용 시간(분) */
	public static final int PENDING_MINUTES = 10;
	/** 1차 인증 후 OTP 입력 대기 세션 TTL(ms) */
	public static final long LOGIN_PENDING_TTL_MS = 5L * 60 * 1000;
	/** 복구 코드 개수 */
	public static final int BACKUP_CODE_COUNT = 5;
	/** 복구 코드 길이 (하이픈 제외) */
	public static final int BACKUP_CODE_LENGTH = 10;

	/** 복구 코드 문자 집합. 헷갈리는 I·O·0·1 제외 */
	static final String BACKUP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private static final SecureRandom RANDOM = new SecureRandom();

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	@Resource(name = "config")
	private Properties config;

	@Resource(name = "adminAuditService")
	private AdminAuditService audit;

	@Resource(name = "superAdminService")
	private SuperAdminService superAdminService;

	private volatile byte[] encryptionKey;

	@PostConstruct
	public void init() {
		String material = configValue("otp.encryption.key", null);
		if (material == null) {
			material = System.getenv("WITCH_OTP_KEY");
		}
		if (material == null || material.trim().isEmpty()) {
			material = System.getProperty("witch.otp.key");
		}
		if (material != null && !material.trim().isEmpty()) {
			encryptionKey = SecretCryptoUtil.deriveKey(material);
			logger.info("[MFA] 관리자 이중인증 활성 (mode={}, required={})", mode(), requiredAuths());
		} else if (!MODE_OFF.equals(mode())) {
			logger.error("[MFA] otp.encryption.key 미설정 — 관리자 이중인증이 비활성 상태로 기동됨. "
					+ "globals.properties 에 otp.encryption.key 를 넣고 재기동할 것");
		}
	}

	// ==========================================
	// 정책
	// ==========================================

	public String mode() {
		String m = configValue("otp.mode", MODE_REQUIRED).toLowerCase();
		if (!MODE_REQUIRED.equals(m) && !MODE_OPTIONAL.equals(m) && !MODE_OFF.equals(m)) {
			return MODE_REQUIRED;
		}
		return m;
	}

	/** 암호화 키가 있고 mode 가 off 가 아닐 때만 동작 */
	public boolean isAvailable() {
		return encryptionKey != null && !MODE_OFF.equals(mode());
	}

	/** 비활성 사유 (어드민 배너용). 정상이면 null */
	public String unavailableReason() {
		if (MODE_OFF.equals(mode())) {
			return "otp.mode=off";
		}
		if (encryptionKey == null) {
			return "otp.encryption.key 미설정";
		}
		return null;
	}

	public Set<String> requiredAuths() {
		Set<String> set = new HashSet<String>();
		for (String a : configValue("otp.required.auth", "SM,LC").split(",")) {
			if (!a.trim().isEmpty()) {
				set.add(a.trim().toUpperCase());
			}
		}
		return set;
	}

	/** 이 권한이 이중인증 적용 대상인지 (대상 아니면 로그인 흐름이 그대로) */
	public boolean isSubject(String auth) {
		return isAvailable() && auth != null && requiredAuths().contains(auth.toUpperCase());
	}

	/** 미등록 상태로는 관리자 기능에 못 들어가게 할 권한인지 */
	public boolean isEnrollmentRequired(String auth) {
		return isSubject(auth) && MODE_REQUIRED.equals(mode());
	}

	public String issuer() {
		return configValue("otp.issuer", "StarPlatform Admin");
	}

	// ==========================================
	// 조회
	// ==========================================

	public Map<String, Object> getStatus(String prsId) throws Exception {
		return dao.selectOne("security.selectMfa", prsId);
	}

	public boolean isEnrolled(String prsId) throws Exception {
		Map<String, Object> row = getStatus(prsId);
		return row != null && "Y".equals(String.valueOf(row.get("OTP_ENABLED_YN")));
	}

	public int countUnusedBackupCodes(String prsId) throws Exception {
		Integer n = dao.selectOne("security.countUnusedBackupCodes", prsId);
		return (n == null) ? 0 : n;
	}

	/** SM 화면용: 관리자(SM·LC) 전원의 등록 상태 */
	public List<Map<String, Object>> listAdminStatus() throws Exception {
		return dao.selectList("security.selectAdminMfaStatus", null);
	}

	/** 비밀번호 재확인. 기존 로그인 쿼리를 그대로 써서 해시 방식을 한 곳에 둔다 */
	public boolean checkPassword(String prsId, String password) {
		if (prsId == null || password == null || password.isEmpty()) {
			return false;
		}
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("PRS_ID", prsId);
			params.put("PRS_PWD", password);
			UserVO u = superAdminService.loginCheck(params);
			// 로그인 쿼리는 OWNER_EMAIL 로도 매칭되므로 ID 가 정확히 같은지 한 번 더 본다
			return u != null && prsId.equals(u.getPRS_ID());
		} catch (Exception e) {
			return false;
		}
	}

	// ==========================================
	// 등록
	// ==========================================

	/**
	 * 등록 시작: 임시 비밀키를 만들어 암호화 저장한다. 아직 OTP_ENABLED_YN 은 바뀌지 않는다.
	 * 반환 {secret, uri}. 비밀키 원문은 이 응답(등록 화면)에만 나가고 어디에도 기록하지 않는다.
	 */
	public Map<String, String> beginEnrollment(String prsId, String accountLabel, HttpServletRequest request) throws Exception {
		requireAvailable();
		String secret = TotpUtil.generateSecretBase32();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("PRS_ID", prsId);
		params.put("PENDING_SECRET_ENC", SecretCryptoUtil.encrypt(secret, encryptionKey));
		params.put("minutes", PENDING_MINUTES);
		dao.update("security.upsertPending", params);
		audit.log(prsId, AdminAuditService.OTP_SETUP_STARTED, true, null, null, request, "등록 시작 (유효 " + PENDING_MINUTES + "분)");

		Map<String, String> out = new HashMap<String, String>();
		out.put("secret", secret);
		out.put("uri", TotpUtil.buildOtpAuthUri(issuer(), accountLabel != null ? accountLabel : prsId, secret));
		return out;
	}

	/**
	 * 등록 확인: 앱의 6자리가 임시 비밀키와 맞으면 확정하고 복구 코드 5개를 발급한다.
	 * 복구 코드 원문은 반환값으로 한 번만 나간다.
	 */
	public List<String> confirmEnrollment(String prsId, String code, HttpServletRequest request) throws Exception {
		requireAvailable();
		Map<String, Object> row = getStatus(prsId);
		String pendingEnc = (row != null) ? (String) row.get("PENDING_SECRET_ENC") : null;
		if (pendingEnc == null) {
			audit.log(prsId, AdminAuditService.OTP_REGISTER, false, "NO_PENDING", null, request, "등록 시작 기록 없음");
			throw new MfaException("NO_PENDING", "등록 절차가 시작되지 않았거나 이미 끝났습니다. 처음부터 다시 진행하세요.");
		}
		if (!truthy(row.get("PENDING_VALID"))) {
			clearPending(prsId);
			audit.log(prsId, AdminAuditService.OTP_REGISTER, false, "PENDING_EXPIRED", null, request, "등록 유효시간 초과");
			throw new MfaException("PENDING_EXPIRED", "등록 유효시간(" + PENDING_MINUTES + "분)이 지났습니다. QR 코드를 다시 발급받으세요.");
		}
		String secret = SecretCryptoUtil.decrypt(pendingEnc, encryptionKey);
		long step = TotpUtil.matchStep(TotpUtil.decodeBase32(secret), code, System.currentTimeMillis(), TotpUtil.DEFAULT_WINDOW);
		if (step < 0) {
			audit.log(prsId, AdminAuditService.OTP_REGISTER, false, "CODE_MISMATCH", null, request, "등록 확인 코드 불일치");
			throw new MfaException("CODE_MISMATCH", "인증 코드가 일치하지 않습니다. 앱의 현재 6자리를 다시 입력하세요. (서버·휴대폰 시간이 맞는지도 확인)");
		}

		// 확정: 임시 → 정식, 활성화, 실패 카운터 초기화
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("PRS_ID", prsId);
		params.put("OTP_SECRET_ENC", SecretCryptoUtil.encrypt(secret, encryptionKey));
		params.put("LAST_USED_STEP", step);
		dao.update("security.activate", params);

		List<String> codes = issueBackupCodes(prsId);
		audit.log(prsId, AdminAuditService.OTP_REGISTER, true, null, null, request, "OTP 등록 완료, 복구 코드 " + codes.size() + "개 발급");
		return codes;
	}

	public void clearPending(String prsId) {
		try {
			dao.update("security.clearPending", prsId);
		} catch (Throwable ignore) {
			// 정리 실패는 다음 등록 시도에서 덮어쓰기로 해결된다
		}
	}

	// ==========================================
	// 2차 인증
	// ==========================================

	/** 검증 결과 */
	public static class VerifyResult {
		public boolean ok;
		public String reasonCode;
		public String message;
		public boolean usedBackupCode;
		public int remainingBackupCodes = -1;
		public int lockRemainSeconds;

		static VerifyResult fail(String code, String message) {
			VerifyResult r = new VerifyResult();
			r.ok = false;
			r.reasonCode = code;
			r.message = message;
			return r;
		}
	}

	/**
	 * 6자리 OTP 또는 복구 코드를 검증한다.
	 *  - 잠금 중이면 즉시 거부 (DB 시간 기준)
	 *  - OTP: ±1단계 허용, 마지막 성공 단계 이하의 코드는 재사용으로 거부
	 *  - 복구 코드: 해시 일치 + 미사용 → 사용 처리(원자적 UPDATE)
	 *  - 실패 누적 5회 → 10분 잠금. 성공하면 카운터 초기화
	 */
	public VerifyResult verify(String prsId, String input, HttpServletRequest request) throws Exception {
		requireAvailable();
		Map<String, Object> row = getStatus(prsId);
		if (row == null || !"Y".equals(String.valueOf(row.get("OTP_ENABLED_YN"))) || row.get("OTP_SECRET_ENC") == null) {
			audit.log(prsId, AdminAuditService.OTP_LOGIN_FAIL, false, "NOT_ENROLLED", null, request, "OTP 미등록 계정의 검증 시도");
			return VerifyResult.fail("NOT_ENROLLED", "이 계정은 OTP 가 등록되어 있지 않습니다.");
		}
		int lockRemain = toInt(row.get("LOCK_REMAIN_SEC"), 0);
		if (lockRemain > 0) {
			audit.log(prsId, AdminAuditService.OTP_LOGIN_FAIL, false, "LOCKED", null, request, "잠금 중 시도 (" + lockRemain + "초 남음)");
			VerifyResult r = VerifyResult.fail("LOCKED", "연속 실패로 잠겨 있습니다. " + ((lockRemain + 59) / 60) + "분 후 다시 시도하세요.");
			r.lockRemainSeconds = lockRemain;
			return r;
		}

		String normalized = normalizeBackupCode(input);
		if (looksLikeBackupCode(normalized)) {
			return verifyBackupCode(prsId, normalized, row, request);
		}

		String secret = SecretCryptoUtil.decrypt((String) row.get("OTP_SECRET_ENC"), encryptionKey);
		long step = TotpUtil.matchStep(TotpUtil.decodeBase32(secret), input, System.currentTimeMillis(), TotpUtil.DEFAULT_WINDOW);
		if (step < 0) {
			return recordFailure(prsId, row, "CODE_MISMATCH", "인증 코드가 일치하지 않습니다.", request);
		}
		long lastStep = toLong(row.get("LAST_USED_STEP"), -1L);
		if (lastStep >= 0 && step <= lastStep) {
			// 같은(또는 더 이전) 단계 코드 재사용. 실패로 세지만 사유는 구분해 둔다
			return recordFailure(prsId, row, "CODE_REUSED", "이미 사용한 인증 코드입니다. 다음 코드가 뜰 때까지 기다리세요.", request);
		}

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("PRS_ID", prsId);
		params.put("LAST_USED_STEP", step);
		dao.update("security.recordSuccess", params);
		audit.log(prsId, AdminAuditService.OTP_LOGIN_SUCCESS, true, null, null, request, "OTP 검증 성공");

		VerifyResult r = new VerifyResult();
		r.ok = true;
		r.remainingBackupCodes = countUnusedBackupCodes(prsId);
		return r;
	}

	private VerifyResult verifyBackupCode(String prsId, String normalized, Map<String, Object> row, HttpServletRequest request) throws Exception {
		String hash = hashBackupCode(prsId, normalized);
		List<Map<String, Object>> unused = dao.selectList("security.selectUnusedBackupCodes", prsId);
		Long matchedId = null;
		for (Map<String, Object> c : unused) {
			if (TotpUtil.constantTimeEquals(hash, String.valueOf(c.get("CODE_HASH")))) {
				matchedId = toLong(c.get("CODE_ID"), -1L);
				break;
			}
		}
		if (matchedId == null || matchedId < 0) {
			return recordFailure(prsId, row, "BACKUP_MISMATCH", "복구 코드가 일치하지 않거나 이미 사용되었습니다.", request);
		}
		Map<String, Object> useParams = new HashMap<String, Object>();
		useParams.put("CODE_ID", matchedId);
		int updated = dao.update("security.useBackupCode", useParams);
		if (updated != 1) {
			// 같은 코드로 동시에 두 요청이 들어온 경우 — 한쪽만 통과
			return recordFailure(prsId, row, "BACKUP_RACE", "복구 코드가 이미 사용되었습니다.", request);
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("PRS_ID", prsId);
		params.put("LAST_USED_STEP", null);
		dao.update("security.recordSuccess", params);

		int remaining = countUnusedBackupCodes(prsId);
		audit.log(prsId, AdminAuditService.OTP_BACKUP_CODE_USED, true, null, null, request, "복구 코드 사용, 남은 코드 " + remaining + "개");

		VerifyResult r = new VerifyResult();
		r.ok = true;
		r.usedBackupCode = true;
		r.remainingBackupCodes = remaining;
		return r;
	}

	private VerifyResult recordFailure(String prsId, Map<String, Object> row, String reason, String message, HttpServletRequest request) throws Exception {
		int fails = toInt(row.get("FAIL_COUNT"), 0) + 1;
		boolean lock = fails >= MAX_FAILS;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("PRS_ID", prsId);
		params.put("FAIL_COUNT", lock ? 0 : fails);
		params.put("lockMinutes", lock ? LOCK_MINUTES : null);
		dao.update("security.updateFailState", params);

		audit.log(prsId, AdminAuditService.OTP_LOGIN_FAIL, false, reason, null, request, "실패 " + fails + "/" + MAX_FAILS);
		if (lock) {
			audit.log(prsId, AdminAuditService.OTP_LOCKED, true, reason, null, request, LOCK_MINUTES + "분 잠금");
			VerifyResult r = VerifyResult.fail("LOCKED", "연속 " + MAX_FAILS + "회 실패로 " + LOCK_MINUTES + "분간 잠겼습니다.");
			r.lockRemainSeconds = LOCK_MINUTES * 60;
			return r;
		}
		return VerifyResult.fail(reason, message + " (남은 시도 " + (MAX_FAILS - fails) + "회)");
	}

	// ==========================================
	// 복구 코드 · 재설정
	// ==========================================

	/** 복구 코드 재발급. 이전 코드는 전부 무효. 호출 전 비밀번호+OTP 재확인은 컨트롤러 책임 */
	public List<String> regenerateBackupCodes(String prsId, HttpServletRequest request) throws Exception {
		requireAvailable();
		if (!isEnrolled(prsId)) {
			throw new MfaException("NOT_ENROLLED", "OTP 가 등록된 계정만 복구 코드를 재발급할 수 있습니다.");
		}
		List<String> codes = issueBackupCodes(prsId);
		audit.log(prsId, AdminAuditService.OTP_BACKUP_CODE_REISSUED, true, null, null, request, "복구 코드 재발급 " + codes.size() + "개, 이전 코드 무효화");
		return codes;
	}

	/**
	 * OTP 재설정(SM 전용 승인 작업). 비밀키·복구 코드를 전부 지운다.
	 * 대상 관리자는 다음 로그인에서 새로 등록해야 한다(required 모드). DB 직접 수정 대신 이 경로를 쓴다.
	 */
	public void reset(String targetId, String actorId, String reason, HttpServletRequest request) throws Exception {
		dao.delete("security.deleteBackupCodes", targetId);
		dao.delete("security.deleteMfa", targetId);
		// 대상의 기존 토큰·세션을 끊는다 (기기 분실 시 탈취된 세션이 살아 있을 수 있다)
		com.sensible.common.util.AdminSessionUtil.revokeTokens(targetId);
		audit.log(actorId, AdminAuditService.OTP_RESET, true, null, targetId, request,
				"OTP 재설정 — 사유: " + (reason == null ? "" : reason) + " / 기존 토큰 무효화");
	}

	private List<String> issueBackupCodes(String prsId) throws Exception {
		dao.delete("security.deleteBackupCodes", prsId);
		List<String> codes = generateBackupCodes(RANDOM, BACKUP_CODE_COUNT);
		for (String code : codes) {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("PRS_ID", prsId);
			params.put("CODE_HASH", hashBackupCode(prsId, normalizeBackupCode(code)));
			dao.insert("security.insertBackupCode", params);
		}
		return codes;
	}

	// ==========================================
	// 순수 헬퍼 (테스트 대상)
	// ==========================================

	/** 복구 코드 n개. 표시 형식 XXXXX-XXXXX */
	static List<String> generateBackupCodes(SecureRandom random, int n) {
		List<String> out = new ArrayList<String>(n);
		Set<String> seen = new HashSet<String>();
		while (out.size() < n) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
				if (i == BACKUP_CODE_LENGTH / 2) {
					sb.append('-');
				}
				sb.append(BACKUP_ALPHABET.charAt(random.nextInt(BACKUP_ALPHABET.length())));
			}
			String code = sb.toString();
			if (seen.add(code)) {
				out.add(code);
			}
		}
		return out;
	}

	/** 입력 정리: 대문자화, 영숫자 외 제거 */
	static String normalizeBackupCode(String input) {
		if (input == null) {
			return "";
		}
		return input.toUpperCase().replaceAll("[^A-Z0-9]", "");
	}

	/** 정리된 입력이 복구 코드 모양인지 (10자, 숫자만은 아님). 6자리 숫자는 OTP 로 본다 */
	static boolean looksLikeBackupCode(String normalized) {
		return normalized != null && normalized.length() == BACKUP_CODE_LENGTH && !normalized.matches("\\d+");
	}

	/** 저장 해시: SHA-256(prsId ":" code). 코드 자체가 50비트 난수라 별도 솔트 없이도 사전 공격이 안 된다 */
	static String hashBackupCode(String prsId, String normalizedCode) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] d = sha.digest((prsId.toLowerCase() + ":" + normalizedCode).getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : d) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void requireAvailable() throws MfaException {
		if (!isAvailable()) {
			throw new MfaException("UNAVAILABLE", "이중인증이 비활성 상태입니다 (" + unavailableReason() + ").");
		}
	}

	private String configValue(String key, String def) {
		try {
			String v = (config != null) ? config.getProperty(key) : null;
			return (v == null || v.trim().isEmpty()) ? def : v.trim();
		} catch (Throwable t) {
			return def;
		}
	}

	static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean) {
			return (Boolean) v;
		}
		if (v instanceof Number) {
			return ((Number) v).intValue() != 0;
		}
		String s = String.valueOf(v);
		return "1".equals(s) || "Y".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s);
	}

	static int toInt(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number) {
			return ((Number) v).intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v));
		} catch (Exception e) {
			return def;
		}
	}

	static long toLong(Object v, long def) {
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

	/** 사용자에게 보여줄 메시지와 사유 코드를 함께 가진 예외 */
	public static class MfaException extends Exception {
		private static final long serialVersionUID = 1L;
		private final String code;

		public MfaException(String code, String message) {
			super(message);
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	/** 테스트용: 설정 없이 키를 직접 꽂는다 */
	void setEncryptionKeyForTest(String material) {
		this.encryptionKey = SecretCryptoUtil.deriveKey(material);
	}

	static List<String> alphabetForTest() {
		return Arrays.asList(BACKUP_ALPHABET.split(""));
	}
}

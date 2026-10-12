package com.sensible.admin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sensible.admin.domain.MfaPendingVO;
import com.sensible.admin.domain.UserVO;
import com.sensible.admin.service.AdminAuditService;
import com.sensible.admin.service.AdminMfaService;
import com.sensible.admin.service.AdminMfaService.MfaException;
import com.sensible.admin.service.AdminMfaService.VerifyResult;
import com.sensible.common.util.AdminSessionUtil;

/**
 * 관리자 OTP 이중인증 화면·API.
 *
 * 경로 두 묶음:
 *  1) 로그인 중(1차 인증 통과, 세션에 MfaPendingVO 만 있음) — verify/setup 계열.
 *     SuperJwtInterceptor 예외 경로다. 컨트롤러가 대기 상태를 직접 확인한다.
 *  2) 로그인 후(정식 세션) — manage/reenroll/regenerateCodes/reset/audit.
 *     인터셉터가 토큰을 검사하고 여기서는 권한만 본다.
 */
@Controller
public class SuperMfaController {

	private static final Logger logger = LoggerFactory.getLogger(SuperMfaController.class);

	/** 한 대기 세션에서 허용하는 입력 시도 수. DB 잠금(5회/10분)과 별개의 세션 상한 */
	private static final int MAX_PENDING_ATTEMPTS = 10;

	@Resource(name = "adminMfaService")
	private AdminMfaService mfaService;

	@Resource(name = "adminAuditService")
	private AdminAuditService audit;

	// ==========================================
	// 1) 로그인 중 — 2차 인증
	// ==========================================

	/** [화면] OTP 입력 */
	@RequestMapping(value = "/super/mfa/verify.do", method = RequestMethod.GET)
	public String verifyPage(HttpServletRequest request, Model model) {
		MfaPendingVO pending = pending(request, MfaPendingVO.PURPOSE_VERIFY);
		if (pending == null) {
			return "redirect:/super/login.do";
		}
		model.addAttribute("adminId", pending.getUser().getPRS_ID());
		model.addAttribute("adminName", pending.getUser().getPRS_NAME());
		return "super/mfa_verify";
	}

	/** [API] OTP / 복구 코드 검증 → 성공 시 최종 세션·토큰 발급 */
	@RequestMapping(value = "/super/mfa/verifyPrc.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> verifyProcess(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "code", required = false) String code) {
		Map<String, Object> result = new HashMap<String, Object>();
		MfaPendingVO pending = pending(request, MfaPendingVO.PURPOSE_VERIFY);
		if (pending == null) {
			return expired(result);
		}
		if (pending.incrementAttempts() > MAX_PENDING_ATTEMPTS) {
			request.getSession().removeAttribute(AdminMfaService.SESSION_PENDING);
			audit.log(pending.getUser().getPRS_ID(), AdminAuditService.OTP_LOGIN_FAIL, false, "SESSION_ATTEMPTS", null, request, "대기 세션 시도 상한 초과");
			return expired(result);
		}
		UserVO user = pending.getUser();
		try {
			VerifyResult vr = mfaService.verify(user.getPRS_ID(), code, request);
			if (!vr.ok) {
				result.put("status", "fail");
				result.put("msg", vr.message);
				result.put("reason", vr.reasonCode);
				result.put("locked", "LOCKED".equals(vr.reasonCode));
				return result;
			}
			request.getSession().removeAttribute(AdminMfaService.SESSION_PENDING);
			String redirect = AdminSessionUtil.establish(request, response, user);
			result.put("status", "success");
			result.put("redirectUrl", redirect);
			result.put("usedBackupCode", vr.usedBackupCode);
			result.put("remainingBackupCodes", vr.remainingBackupCodes);
			return result;
		} catch (MfaException e) {
			result.put("status", "fail");
			result.put("msg", e.getMessage());
			return result;
		} catch (Exception e) {
			logger.error("[MFA] verify error for {}: {}", user.getPRS_ID(), e.toString());
			result.put("status", "error");
			result.put("msg", "이중인증 처리 중 오류가 발생했습니다. 관리자에게 문의하세요.");
			return result;
		}
	}

	// ==========================================
	// 1) 로그인 중 또는 재등록 — 등록
	// ==========================================

	/** [화면] OTP 등록 (QR) */
	@RequestMapping(value = "/super/mfa/setup.do", method = RequestMethod.GET)
	public String setupPage(HttpServletRequest request, Model model) {
		MfaPendingVO pending = pending(request, MfaPendingVO.PURPOSE_SETUP);
		if (pending == null) {
			return "redirect:/super/login.do";
		}
		model.addAttribute("adminId", pending.getUser().getPRS_ID());
		model.addAttribute("adminName", pending.getUser().getPRS_NAME());
		model.addAttribute("reenroll", pending.isReenroll());
		model.addAttribute("issuer", mfaService.issuer());
		model.addAttribute("pendingMinutes", AdminMfaService.PENDING_MINUTES);
		return "super/mfa_setup";
	}

	/** [API] 임시 비밀키 발급 → QR 용 URI 와 수동 입력용 키 */
	@RequestMapping(value = "/super/mfa/setupStart.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> setupStart(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<String, Object>();
		MfaPendingVO pending = pending(request, MfaPendingVO.PURPOSE_SETUP);
		if (pending == null) {
			return expired(result);
		}
		UserVO user = pending.getUser();
		try {
			String label = user.getPRS_ID() + (user.getPRS_COUNTRY() != null ? " (" + user.getPRS_COUNTRY() + ")" : "");
			Map<String, String> out = mfaService.beginEnrollment(user.getPRS_ID(), label, request);
			result.put("status", "success");
			result.put("uri", out.get("uri"));
			result.put("secret", out.get("secret"));
			return result;
		} catch (MfaException e) {
			result.put("status", "fail");
			result.put("msg", e.getMessage());
			return result;
		} catch (Exception e) {
			logger.error("[MFA] setupStart error for {}: {}", user.getPRS_ID(), e.toString());
			result.put("status", "error");
			result.put("msg", "등록을 시작할 수 없습니다. 이중인증 테이블(admin-mfa-audit.sql) 적용 여부를 확인하세요.");
			return result;
		}
	}

	/** [API] 6자리 확인 → 활성화 + 복구 코드 1회 노출 (+ 로그인 중이면 최종 세션 발급) */
	@RequestMapping(value = "/super/mfa/setupConfirm.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> setupConfirm(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "code", required = false) String code) {
		Map<String, Object> result = new HashMap<String, Object>();
		MfaPendingVO pending = pending(request, MfaPendingVO.PURPOSE_SETUP);
		if (pending == null) {
			return expired(result);
		}
		if (pending.incrementAttempts() > MAX_PENDING_ATTEMPTS) {
			request.getSession().removeAttribute(AdminMfaService.SESSION_PENDING);
			mfaService.clearPending(pending.getUser().getPRS_ID());
			return expired(result);
		}
		UserVO user = pending.getUser();
		try {
			List<String> codes = mfaService.confirmEnrollment(user.getPRS_ID(), code, request);
			request.getSession().removeAttribute(AdminMfaService.SESSION_PENDING);
			String redirect;
			if (pending.isReenroll()) {
				redirect = "/super/mfa/manage.do";
			} else {
				redirect = AdminSessionUtil.establish(request, response, user);
			}
			result.put("status", "success");
			result.put("backupCodes", codes);
			result.put("redirectUrl", redirect);
			return result;
		} catch (MfaException e) {
			result.put("status", "fail");
			result.put("msg", e.getMessage());
			result.put("reason", e.getCode());
			return result;
		} catch (Exception e) {
			logger.error("[MFA] setupConfirm error for {}: {}", user.getPRS_ID(), e.toString());
			result.put("status", "error");
			result.put("msg", "등록 확인 중 오류가 발생했습니다.");
			return result;
		}
	}

	// ==========================================
	// 2) 로그인 후 — 관리
	// ==========================================

	/** [화면] 내 이중인증 상태 (+ SM: 관리자 전원 현황·재설정) */
	@RequestMapping(value = "/super/mfa/manage.do")
	public String managePage(HttpServletRequest request, Model model) throws Exception {
		UserVO user = adminUser(request);
		if (user == null) {
			return "redirect:/super/login.do";
		}
		model.addAttribute("activeMenu", "mfa_manage");
		model.addAttribute("mfaAvailable", mfaService.isAvailable());
		model.addAttribute("mfaUnavailableReason", mfaService.unavailableReason());
		model.addAttribute("mfaMode", mfaService.mode());
		model.addAttribute("isSubject", mfaService.isSubject(user.getPRS_AUTH()));
		model.addAttribute("isSM", "SM".equals(user.getPRS_AUTH()));
		model.addAttribute("me", user);
		try {
			model.addAttribute("myStatus", mfaService.getStatus(user.getPRS_ID()));
			model.addAttribute("backupLeft", mfaService.countUnusedBackupCodes(user.getPRS_ID()));
			if ("SM".equals(user.getPRS_AUTH())) {
				model.addAttribute("adminList", mfaService.listAdminStatus());
			}
		} catch (Exception e) {
			// 테이블 미생성 등. 화면은 뜨되 안내만 바꾼다
			logger.error("[MFA] manage page data error: {}", e.toString());
			model.addAttribute("storageError", true);
		}
		return "super/mfa_manage";
	}

	/** [API] 재등록 시작 — 비밀번호 재확인 후 SETUP 대기 상태로 전환 */
	@RequestMapping(value = "/super/mfa/reenroll.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> reenroll(HttpServletRequest request,
			@RequestParam(value = "password", required = false) String password) {
		Map<String, Object> result = new HashMap<String, Object>();
		UserVO user = adminUser(request);
		if (user == null) {
			return denied(result);
		}
		if (!mfaService.isSubject(user.getPRS_AUTH())) {
			result.put("status", "fail");
			result.put("msg", "이 계정 권한은 이중인증 대상이 아니거나 이중인증이 비활성 상태입니다.");
			return result;
		}
		if (!mfaService.checkPassword(user.getPRS_ID(), password)) {
			audit.log(user.getPRS_ID(), AdminAuditService.OTP_SETUP_STARTED, false, "PASSWORD_MISMATCH", null, request, "재등록 전 비밀번호 재확인 실패");
			result.put("status", "fail");
			result.put("msg", "비밀번호가 일치하지 않습니다.");
			return result;
		}
		request.getSession().setAttribute(AdminMfaService.SESSION_PENDING, new MfaPendingVO(user, MfaPendingVO.PURPOSE_SETUP, true));
		result.put("status", "success");
		result.put("redirectUrl", "/super/mfa/setup.do");
		return result;
	}

	/** [API] 복구 코드 재발급 — 비밀번호 + 현재 OTP 재확인 */
	@RequestMapping(value = "/super/mfa/regenerateCodes.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> regenerateCodes(HttpServletRequest request,
			@RequestParam(value = "password", required = false) String password,
			@RequestParam(value = "code", required = false) String code) {
		Map<String, Object> result = new HashMap<String, Object>();
		UserVO user = adminUser(request);
		if (user == null) {
			return denied(result);
		}
		if (!mfaService.checkPassword(user.getPRS_ID(), password)) {
			audit.log(user.getPRS_ID(), AdminAuditService.OTP_BACKUP_CODE_REISSUED, false, "PASSWORD_MISMATCH", null, request, "재발급 전 비밀번호 재확인 실패");
			result.put("status", "fail");
			result.put("msg", "비밀번호가 일치하지 않습니다.");
			return result;
		}
		try {
			VerifyResult vr = mfaService.verify(user.getPRS_ID(), code, request);
			if (!vr.ok || vr.usedBackupCode) {
				result.put("status", "fail");
				result.put("msg", vr.ok ? "재발급에는 복구 코드가 아니라 인증 앱의 6자리를 입력하세요." : vr.message);
				return result;
			}
			List<String> codes = mfaService.regenerateBackupCodes(user.getPRS_ID(), request);
			result.put("status", "success");
			result.put("backupCodes", codes);
			return result;
		} catch (MfaException e) {
			result.put("status", "fail");
			result.put("msg", e.getMessage());
			return result;
		} catch (Exception e) {
			logger.error("[MFA] regenerate error for {}: {}", user.getPRS_ID(), e.toString());
			result.put("status", "error");
			result.put("msg", "복구 코드 재발급 중 오류가 발생했습니다.");
			return result;
		}
	}

	/** [API] OTP 재설정 (SM 전용, 사유 필수, 본인 비밀번호 재확인) */
	@RequestMapping(value = "/super/mfa/reset.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> reset(HttpServletRequest request,
			@RequestParam(value = "targetId", required = false) String targetId,
			@RequestParam(value = "reason", required = false) String reason,
			@RequestParam(value = "password", required = false) String password) {
		Map<String, Object> result = new HashMap<String, Object>();
		UserVO user = adminUser(request);
		if (user == null || !"SM".equals(user.getPRS_AUTH())) {
			return denied(result);
		}
		if (targetId == null || targetId.trim().isEmpty() || reason == null || reason.trim().length() < 5) {
			result.put("status", "fail");
			result.put("msg", "대상 관리자와 재설정 사유(5자 이상)를 입력하세요.");
			return result;
		}
		if (!mfaService.checkPassword(user.getPRS_ID(), password)) {
			audit.log(user.getPRS_ID(), AdminAuditService.OTP_RESET, false, "PASSWORD_MISMATCH", targetId.trim(), request, "재설정 전 비밀번호 재확인 실패");
			result.put("status", "fail");
			result.put("msg", "비밀번호가 일치하지 않습니다.");
			return result;
		}
		try {
			mfaService.reset(targetId.trim(), user.getPRS_ID(), reason.trim(), request);
			result.put("status", "success");
			result.put("msg", "재설정되었습니다. 대상 관리자는 다음 로그인에서 OTP 를 새로 등록합니다.");
			return result;
		} catch (Exception e) {
			logger.error("[MFA] reset error target={} : {}", targetId, e.toString());
			result.put("status", "error");
			result.put("msg", "재설정 중 오류가 발생했습니다.");
			return result;
		}
	}

	/** [화면] 보안 감사 로그 (SM 전용) */
	@RequestMapping(value = "/super/mfa/audit.do")
	public String auditPage(HttpServletRequest request, Model model,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "prsId", required = false, defaultValue = "") String prsId,
			@RequestParam(value = "eventType", required = false, defaultValue = "") String eventType) throws Exception {
		UserVO user = adminUser(request);
		if (user == null || !"SM".equals(user.getPRS_AUTH())) {
			return "redirect:/super/dashboard.do";
		}
		int length = 50;
		if (page < 1) {
			page = 1;
		}
		int start = (page - 1) * length;
		try {
			int totalCount = audit.count(prsId, eventType);
			model.addAttribute("logs", audit.list(start, length, prsId, eventType));
			model.addAttribute("totalCount", totalCount);
			int totalPages = (int) Math.ceil((double) totalCount / length);
			model.addAttribute("totalPages", totalPages == 0 ? 1 : totalPages);
		} catch (Exception e) {
			logger.error("[Audit] list error: {}", e.toString());
			model.addAttribute("storageError", true);
			model.addAttribute("totalCount", 0);
			model.addAttribute("totalPages", 1);
		}
		model.addAttribute("currentPage", page);
		model.addAttribute("prsId", prsId);
		model.addAttribute("eventType", eventType);
		model.addAttribute("eventTypes", java.util.Arrays.asList(
				AdminAuditService.LOGIN_SUCCESS, AdminAuditService.LOGIN_FAIL,
				AdminAuditService.OTP_SETUP_STARTED, AdminAuditService.OTP_REGISTER,
				AdminAuditService.OTP_LOGIN_SUCCESS, AdminAuditService.OTP_LOGIN_FAIL, AdminAuditService.OTP_LOCKED,
				AdminAuditService.OTP_BACKUP_CODE_USED, AdminAuditService.OTP_BACKUP_CODE_REISSUED, AdminAuditService.OTP_RESET,
				AdminAuditService.ALERT_SETTING_CHANGED, AdminAuditService.ALERT_ACK, AdminAuditService.ALERT_TEST_MAIL));
		model.addAttribute("activeMenu", "audit_log");
		return "super/audit_log";
	}

	// ==========================================
	// 헬퍼
	// ==========================================

	/** 유효한(만료 안 된, 목적이 맞는) 대기 상태. 없으면 null + 세션 정리 */
	private MfaPendingVO pending(HttpServletRequest request, String purpose) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object o = session.getAttribute(AdminMfaService.SESSION_PENDING);
		if (!(o instanceof MfaPendingVO)) {
			return null;
		}
		MfaPendingVO p = (MfaPendingVO) o;
		if (p.getUser() == null || !purpose.equals(p.getPurpose())
				|| p.isExpired(System.currentTimeMillis(), AdminMfaService.LOGIN_PENDING_TTL_MS)) {
			session.removeAttribute(AdminMfaService.SESSION_PENDING);
			return null;
		}
		return p;
	}

	/** 정식 로그인된 관리자(SM·LC). 스타(ST)는 대상이 아니다 */
	private UserVO adminUser(HttpServletRequest request) {
		UserVO u = AdminSessionUtil.currentUser(request);
		if (u == null || !("SM".equals(u.getPRS_AUTH()) || "LC".equals(u.getPRS_AUTH()))) {
			return null;
		}
		return u;
	}

	private static Map<String, Object> expired(Map<String, Object> result) {
		result.put("status", "expired");
		result.put("msg", "인증 대기 시간이 지났거나 세션이 없습니다. 다시 로그인하세요.");
		result.put("redirectUrl", "/super/login.do");
		return result;
	}

	private static Map<String, Object> denied(Map<String, Object> result) {
		result.put("status", "fail");
		result.put("msg", "권한이 없습니다.");
		return result;
	}
}

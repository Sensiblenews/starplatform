package com.sensible.admin.controller;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sensible.admin.domain.MfaPendingVO;
import com.sensible.admin.domain.UserVO;
import com.sensible.admin.service.AdminAuditService;
import com.sensible.admin.service.AdminMfaService;
import com.sensible.admin.service.SuperAdminService;
import com.sensible.common.util.AdminSessionUtil;

@Controller
public class SuperLoginController {

    private static final Logger logger = LoggerFactory.getLogger(SuperLoginController.class);

    @Resource(name = "superAdminService")
    private SuperAdminService superAdminService;

    @Resource(name = "adminMfaService")
    private AdminMfaService mfaService;

    @Resource(name = "adminAuditService")
    private AdminAuditService audit;

    @RequestMapping(value = "/super/test.do")
    @ResponseBody
    public String test() { return "Controller is Alive!"; }

    /**
     * [화면] 로그인 페이지
     */
    @RequestMapping(value = "/super/login.do")
    public String loginPage(HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserVO user = (UserVO) session.getAttribute("SUPER_USER_SESSION");

        // [수정] SM(총괄) 또는 LC(지역관리자)라면 대시보드로 이동
        if (user != null) {
            String auth = user.getPRS_AUTH();
            if ("SM".equals(auth) || "LC".equals(auth)) {
                 return "redirect:/super/dashboard.do";
            }
        }

        return "/super/login";
    }

    /**
     * [API] 로그인 처리 (1차: 아이디+비밀번호)
     *
     * 이중인증 대상(SM·LC, 설정 활성)이면 여기서는 세션·토큰을 주지 않고
     * OTP 대기 상태(MfaPendingVO)만 세션에 두고 2차 인증 화면으로 보낸다.
     * 최종 관리자 권한은 SuperMfaController 가 OTP 검증 뒤에 발급한다.
     */
    @RequestMapping(value = "/super/loginPrc.do")
    @ResponseBody
    public Map<String, Object> loginProcess(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        String inputId = String.valueOf(params.get("PRS_ID"));
        try {
            UserVO user = superAdminService.loginCheck(params);

            if (user != null) {
                String auth = user.getPRS_AUTH();

                // [수정] SM 또는 LC 권한 허용
                if ("SM".equals(auth) || "LC".equals(auth) || "ST".equals(auth)) {
                    audit.log(user.getPRS_ID(), AdminAuditService.LOGIN_SUCCESS, true, null, null, request, "비밀번호 인증 성공 (" + auth + ")");

                    // 이중인증 대상이면 2차 인증으로 분기
                    if (mfaService.isSubject(auth)) {
                        boolean enrolled;
                        try {
                            enrolled = mfaService.isEnrolled(user.getPRS_ID());
                        } catch (Exception e) {
                            // 테이블 미생성 등 저장소 오류. 우회 경로를 만들지 않고 실패로 막는다 (fail-closed)
                            logger.error("[MFA] 상태 조회 실패 — DDL(admin-mfa-audit.sql) 적용 여부 확인: {}", e.toString());
                            audit.log(user.getPRS_ID(), AdminAuditService.LOGIN_FAIL, false, "MFA_STORAGE", null, request, "이중인증 저장소 오류");
                            result.put("status", "fail");
                            result.put("msg", "이중인증 저장소 오류로 로그인할 수 없습니다. 서버 관리자에게 DB 적용 여부를 확인하세요.");
                            return result;
                        }
                        if (enrolled) {
                            request.getSession().setAttribute(AdminMfaService.SESSION_PENDING,
                                    new MfaPendingVO(user, MfaPendingVO.PURPOSE_VERIFY, false));
                            result.put("status", "success");
                            result.put("mfa", "verify");
                            result.put("redirectUrl", "/super/mfa/verify.do");
                            return result;
                        }
                        if (mfaService.isEnrollmentRequired(auth)) {
                            request.getSession().setAttribute(AdminMfaService.SESSION_PENDING,
                                    new MfaPendingVO(user, MfaPendingVO.PURPOSE_SETUP, false));
                            result.put("status", "success");
                            result.put("mfa", "setup");
                            result.put("redirectUrl", "/super/mfa/setup.do");
                            return result;
                        }
                        // optional 모드 + 미등록: 기존 로그인 그대로
                    }

                    // 1. 세션 저장 + 2. JWT 토큰 발급 (PRS_COUNTRY 포함된 VO가 저장됨)
                    String redirect = AdminSessionUtil.establish(request, response, user);
                    result.put("status", "success");
                    result.put("msg", "로그인 성공");
                    result.put("redirectUrl", redirect);
                } else {
                    audit.log(user.getPRS_ID(), AdminAuditService.LOGIN_FAIL, false, "NO_AUTH", null, request, "권한 없음 (" + auth + ")");
                    result.put("status", "fail");
                    result.put("msg", "접근 권한이 없습니다. (관리자 전용)");
                }
            } else {
                audit.log(inputId, AdminAuditService.LOGIN_FAIL, false, "BAD_CREDENTIALS", null, request, "아이디 또는 비밀번호 불일치");
                result.put("status", "fail");
                result.put("msg", "아이디 또는 비밀번호를 확인해주세요.");
            }
        } catch (Exception e) {
            logger.error("[Login] error: {}", e.toString());
            result.put("status", "error");
            result.put("msg", "에러: " + e.getMessage());
        }
        return result;
    }

    @RequestMapping(value = "/super/logout.do")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        AdminSessionUtil.clear(request, response);
        return "redirect:/super/login.do";
    }
}

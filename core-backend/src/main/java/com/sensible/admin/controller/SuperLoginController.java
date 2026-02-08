package com.sensible.admin.controller;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sensible.admin.domain.UserVO;
import com.sensible.admin.service.SuperAdminService;
import com.sensible.common.util.JwtUtil;

@Controller
public class SuperLoginController {

    @Resource(name = "superAdminService")
    private SuperAdminService superAdminService;

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
     * [API] 로그인 처리
     */
    @RequestMapping(value = "/super/loginPrc.do")
    @ResponseBody
    public Map<String, Object> loginProcess(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserVO user = superAdminService.loginCheck(params);
            
            if (user != null) {
                String auth = user.getPRS_AUTH();
                
                // [수정] SM 또는 LC 권한 허용
                if ("SM".equals(auth) || "LC".equals(auth) || "ST".equals(auth)) { 
                    
                    // 1. 세션 저장 (PRS_COUNTRY 포함된 VO가 저장됨)
                    request.getSession().setAttribute("SUPER_USER_SESSION", user);
                    
                    // 2. JWT 토큰 발급
                    String token = JwtUtil.generateToken(user.getPRS_ID(), auth);
                    String cookieHeader = "SUPER_TOKEN=" + token + "; Path=/; Max-Age=7200; HttpOnly"; 
                    response.addHeader("Set-Cookie", cookieHeader);
                    
                    result.put("status", "success");
                    result.put("msg", "로그인 성공");
                    
                    if ("ST".equals(auth)) {
                        result.put("redirectUrl", "/witch/star/mypage.do"); // 스타는 마이페이지로
                    } else {
                        result.put("redirectUrl", "/witch/super/dashboard.do"); // 관리자는 대시보드로
                    }
                } else {
                    result.put("status", "fail");
                    result.put("msg", "접근 권한이 없습니다. (관리자 전용)");
                }
            } else {
                result.put("status", "fail");
                result.put("msg", "아이디 또는 비밀번호를 확인해주세요.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "error");
            result.put("msg", "에러: " + e.getMessage());
        }
        return result;
    }
    
    @RequestMapping(value = "/super/logout.do")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().invalidate();
        String cookieHeader = "SUPER_TOKEN=; Path=/; Max-Age=0"; 
        response.addHeader("Set-Cookie", cookieHeader);
        return "redirect:/super/login.do";
    }
}
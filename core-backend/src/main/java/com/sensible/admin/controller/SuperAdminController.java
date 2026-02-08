package com.sensible.admin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.sensible.admin.domain.UserVO;
import com.sensible.admin.service.SuperAdminService;

@Controller
public class SuperAdminController {

    @Resource(name = "superAdminService")
    private SuperAdminService superAdminService;

    // 헬퍼 메소드: 로그인 유저 확인
    private UserVO getLoginUser(HttpServletRequest request) {
        return (UserVO) request.getSession().getAttribute("SUPER_USER_SESSION");
    }

    // 헬퍼 메소드: 필터용 국가 코드 추출 (SM이면 null, LC면 국가코드 반환)
    private String getFilterCountry(UserVO user) {
        return "LC".equals(user.getPRS_AUTH()) ? user.getPRS_COUNTRY() : null;
    }

    /**
     * [대시보드] 권한에 따라 보여주는 데이터가 다름
     */
    @RequestMapping(value = "/super/dashboard.do")
    public String dashboard(HttpServletRequest request, Model model) throws Exception {
        UserVO user = getLoginUser(request);
        if (user == null) return "redirect:/super/login.do";

        // LC면 자기 국가만, SM이면 전체(null)
        String filterCountry = getFilterCountry(user);
        Map<String, Object> stats = superAdminService.getDashboardStats(filterCountry);
        
        model.addAttribute("stats", stats);
        return "super/dashboard_global"; 
    }

    /**
     * [목록] 권한에 따라 필터링
     */
    @RequestMapping(value = "/super/star/list.do")
    public String starList(HttpServletRequest request, Model model) throws Exception {
        UserVO user = getLoginUser(request);
        if (user == null) return "redirect:/super/login.do";

        String filterCountry = getFilterCountry(user);
        List<Map<String, Object>> starList = superAdminService.getStarList(filterCountry);
        
        model.addAttribute("starList", starList);
        return "super/star_list";
    }

    /**
     * [화면] 스타 등록 폼
     */
    @RequestMapping(value = "/super/star/create.do")
    public String createStarForm(HttpServletRequest request, Model model) {
        UserVO user = getLoginUser(request);
        if (user == null) return "redirect:/super/login.do";
        return "super/star_form";
    }

    /**
     * [API] 스타 등록 (LC는 강제로 자기 국가로 등록)
     */
    @RequestMapping(value = "/super/star/insert.do")
    @ResponseBody
    public Map<String, Object> insertStar(HttpServletRequest request, @RequestParam Map<String, Object> params) throws Exception {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        
        if (user == null) {
             result.put("status", "fail"); result.put("msg", "로그인 필요"); return result;
        }

        try {
            // [중요] LC는 선택권 없음. 무조건 자기 국가로 귀속.
            if("LC".equals(user.getPRS_AUTH())) {
                params.put("PRS_COUNTRY", user.getPRS_COUNTRY());
            }

            String rawPw = (String) params.get("PRS_PWD");
            if (rawPw == null || rawPw.isEmpty()) throw new Exception("비밀번호는 필수입니다.");
            params.put("PRS_PWD", rawPw);
            
            superAdminService.insertStar(params);
            
            result.put("status", "success");
            result.put("msg", "등록 완료");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "실패: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * [화면] 지역 관리자 생성 (SM 전용)
     */
    @RequestMapping(value = "/super/local/create.do")
    public String createLocalAdminForm(HttpServletRequest request) {
        UserVO user = getLoginUser(request);
        // SM만 접근 가능
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            return "redirect:/super/dashboard.do";
        }
        return "super/local_admin_form"; // 신규 JSP
    }

    /**
     * [API] 지역 관리자 생성 (SM 전용)
     */
    @RequestMapping(value = "/super/local/insert.do")
    @ResponseBody
    public Map<String, Object> insertLocalAdmin(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
             result.put("status", "fail"); result.put("msg", "권한이 없습니다."); return result;
        }

        try {
            superAdminService.insertLocalAdmin(params);
            result.put("status", "success");
            result.put("msg", "지역 관리자가 생성되었습니다.");
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", "생성 실패: " + e.getMessage());
        }
        return result;
    }

    // ... toggleStatus 등 기타 메소드는 그대로 유지 ...
    @RequestMapping(value = "/super/star/toggleStatus.do")
    @ResponseBody
    public Map<String, Object> toggleStatus(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        // 기존 코드와 동일
        Map<String, Object> result = new HashMap<>();
        try {
            UserVO user = getLoginUser(request);
            if (user == null) throw new Exception("로그인 필요");
            superAdminService.updateStarStatus(params); 
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail"); result.put("msg", e.getMessage());
        }
        return result;
    }
    
    @RequestMapping(value = "/super/star/togglePopular.do")
    @ResponseBody
    public Map<String, Object> togglePopular(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);

        if (user == null) {
            result.put("status", "fail"); 
            result.put("msg", "로그인이 필요합니다."); 
            return result;
        }

        try {
            // 서비스에게 "이 스타의 인기 상태를 변경해줘"라고 요청
            // (6명이 넘는지 체크하는 로직은 서비스 안에 있음)
            result = superAdminService.togglePopularStatus(params);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "시스템 오류: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * [API] 특정 스타의 피드 목록 조회 (모달용)
     */
    @RequestMapping(value = "/super/star/feedList.do")
    @ResponseBody
    public Map<String, Object> getStarFeedList(@RequestParam("starId") String starId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> list = superAdminService.getStarFeedList(starId);
            result.put("status", "success");
            result.put("list", list);
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * [API] 피드 고정/해제 토글
     */
    @RequestMapping(value = "/super/star/togglePin.do")
    @ResponseBody
    public Map<String, Object> togglePin(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null) { result.put("status", "fail"); return result; }

        try {
            superAdminService.toggleFeedPin(params);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail"); result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * [API] 피드 삭제
     */
    @RequestMapping(value = "/super/star/deleteFeed.do")
    @ResponseBody
    public Map<String, Object> deleteFeed(HttpServletRequest request, @RequestParam("conId") String conId) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null) { result.put("status", "fail"); return result; }

        try {
            superAdminService.deleteFeed(conId);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail"); result.put("msg", e.getMessage());
        }
        return result;
    }
    
    @RequestMapping(value = "/api/super/feed/like")
    public @ResponseBody Map<String, Object> toggleFeedLike(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            // params에는 conId, memId가 들어있어야 합니다.
            superAdminService.toggleFeedLike(params);
            
            // 최신 하트 개수와 상태를 반환
            int newCount = superAdminService.getHeartCount(params);
            boolean isLiked = superAdminService.checkUserLike(params);
            
            result.put("result", "OK");
            result.put("heartCount", newCount);
            result.put("isLiked", isLiked);
            
        } catch (Exception e) {
            result.put("result", "FAIL");
            result.put("msg", e.getMessage());
        }
        return result;
    }
    
    /**
     * [API] 전체 최신 피드 목록 조회 (최근 100개)
     */
    @RequestMapping(value = "/super/star/allFeedList.do")
    @ResponseBody
    public Map<String, Object> getAllFeedList(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        
        if (user == null) {
            result.put("status", "fail");
            result.put("msg", "로그인 필요");
            return result;
        }

        try {
            // SM이면 null(전체), LC면 국가코드 자동 적용
            String filterCountry = getFilterCountry(user);
            
            List<Map<String, Object>> list = superAdminService.getRecentFeedList(filterCountry);
            
            result.put("status", "success");
            result.put("list", list);
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }
    
    @RequestMapping(value = "/super/stats/download.do")
    public void downloadStatsCsv(HttpServletRequest request, HttpServletResponse response, 
                                 @RequestParam Map<String, Object> params) throws Exception {
        
        UserVO user = getLoginUser(request);
        if (user == null) return;

        String filterCountry = getFilterCountry(user);
        params.put("country", filterCountry);
        
        // CSV 내용 생성
        String csvContent = superAdminService.generateStatsCsv(params);
        
        // [수정] 파일명 분기 처리
        String targetMonth = (String) params.get("targetMonth");
        String fileName;
        
        if (targetMonth != null && !targetMonth.isEmpty()) {
            // 월별 다운로드
            fileName = "AdStats_" + targetMonth + ".csv";
        } else {
            // 전체 다운로드
            fileName = "AdStats_Total_AllTime.csv";
        }
        
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        
        response.getWriter().write(csvContent);
    }
}
package com.sensible.admin.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sensible.admin.domain.UserVO;
import com.sensible.admin.service.SuperAdminService;
import com.sensible.common.Constants;

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
        if (user == null)
            return "redirect:/super/login.do";

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
        if (user == null)
            return "redirect:/super/login.do";

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
        if (user == null)
            return "redirect:/super/login.do";
        return "super/star_form";
    }

    /**
     * [API] 스타 등록 (LC는 강제로 자기 국가로 등록)
     */
    @RequestMapping(value = "/super/star/insert.do")
    @ResponseBody
    public Map<String, Object> insertStar(HttpServletRequest request, @RequestParam Map<String, Object> params)
            throws Exception {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);

        if (user == null) {
            result.put("status", "fail");
            result.put("msg", "로그인 필요");
            return result;
        }

        try {
            // [중요] LC는 선택권 없음. 무조건 자기 국가로 귀속.
            if ("LC".equals(user.getPRS_AUTH())) {
                params.put("PRS_COUNTRY", user.getPRS_COUNTRY());
            }

            String rawPw = (String) params.get("PRS_PWD");
            if (rawPw == null || rawPw.isEmpty())
                throw new Exception("비밀번호는 필수입니다.");
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
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
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
            if (user == null)
                throw new Exception("로그인 필요");
            superAdminService.updateStarStatus(params);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", e.getMessage());
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
        if (user == null) {
            result.put("status", "fail");
            return result;
        }

        try {
            superAdminService.toggleFeedPin(params);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", e.getMessage());
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
        if (user == null) {
            result.put("status", "fail");
            return result;
        }

        try {
            superAdminService.deleteFeed(conId);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", e.getMessage());
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
    /**
     * [API] 전체 최신 피드 목록 조회 (무한 스크롤 페이징 적용)
     */
    @RequestMapping(value = "/super/star/allFeedList.do")
    @ResponseBody
    public Map<String, Object> getAllFeedList(HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "1") int page) {

        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);

        if (user == null) {
            result.put("status", "fail");
            result.put("msg", "로그인 필요");
            return result;
        }

        try {
            String filterCountry = getFilterCountry(user);

            // 한 번에 불러올 게시물 수 설정 (예: 20개)
            int limit = 20;
            int offset = (page - 1) * limit;

            List<Map<String, Object>> list = superAdminService.getRecentFeedList(filterCountry, offset, limit);

            result.put("status", "success");
            result.put("list", list);
            // 프론트엔드에서 더 불러올 데이터가 있는지 판단하기 위해 현재 페이지 번호 반환
            result.put("page", page);
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
        if (user == null)
            return;

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

    // [신규] 모달에 뿌려줄 데이터 가져오기
    @RequestMapping(value = "/super/star/getOrderSettings.do", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getOrderSettings(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null) {
            result.put("status", "fail");
            return result;
        }

        try {
            String filterCountry = getFilterCountry(user);
            result = superAdminService.getOrderSettings(filterCountry);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail");
        }
        return result;
    }

    // [신규] 저장하기
    @RequestMapping(value = "/super/star/saveOrderSettings.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveOrderSettings(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null) {
            result.put("status", "fail");
            return result;
        }

        try {
            superAdminService.saveOrderSettings(user, payload);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    // 🌟 [신규] 신청 목록 조회 API (Ajax로 테이블에 뿌릴 때 사용)
    @RequestMapping(value = "/super/ipo/getList.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getIpoRequestList(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 권한 체크 로직 필요 시 추가 (UserVO user = getLoginUser(request); 등)
            List<Map<String, Object>> list = superAdminService.getAdminIpoList();
            result.put("list", list);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail");
        }
        return result;
    }

    // 🌟 [신규] 승인 / 반려 처리 API
    @RequestMapping(value = "/super/ipo/process.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> processIpoRequest(HttpServletRequest request, @RequestParam Map<String, Object> param) {
        Map<String, Object> result = new HashMap<>();
        try {
            superAdminService.processIpoStatus(param);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * [화면] 신고 관리 리스트 페이지 (SM 전용)
     */
    @RequestMapping(value = "/super/report/list.do")
    public String reportList(HttpServletRequest request, Model model) throws Exception {
        UserVO user = getLoginUser(request);
        // SM(총괄 관리자)만 접근 가능
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            return "redirect:/super/dashboard.do";
        }

        List<Map<String, Object>> reportList = superAdminService.getReportList();
        model.addAttribute("reportList", reportList);

        return "super/report_list"; // 신규 JSP
    }

    /**
     * [API] 신고 관리 액션 (댓글 삭제 or 글로벌 차단)
     */
    @RequestMapping(value = "/super/report/action.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> handleReportAction(HttpServletRequest request,
            @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }

        try {
            String action = (String) params.get("action");

            if ("BLIND".equals(action)) {
                // 기존에 만들어둔 댓글 블라인드 쿼리 재활용
                params.put("STATUS", "BLIND");
                superAdminService.updateCommentStatus(params); // 주의: 서비스 메서드 이름 맞춰서 호출 (updateCommentStatus)
            } else if ("GLOBAL_BLOCK".equals(action)) {
                // 플랫폼 전체 영구 차단
                params.put("reason", "누적 신고에 의한 최고 관리자 직권 차단");
                superAdminService.executeGlobalBlock(params);
            }

            result.put("status", "success");
            result.put("msg", "처리되었습니다.");
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", "처리 실패: " + e.getMessage());
        }
        return result;
    }

    /**
     * 🌟 [API] 모바일 앱 전용 관리자 숨겨진 로그인 (이스터에그)
     */
    @RequestMapping(value = "/api/super/admin/login", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> mobileAdminLogin(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 프론트엔드에서 보낸 { id: '...', pw: '...' } 규격을 DB 쿼리용으로 변환
            Map<String, Object> loginParams = new HashMap<>();
            loginParams.put("PRS_ID", params.get("id"));
            loginParams.put("PRS_PWD", params.get("pw"));

            // 기존에 만들어둔 로그인 체크 서비스 재활용
            UserVO user = superAdminService.loginCheck(loginParams);

            if (user != null) {
                String auth = user.getPRS_AUTH();

                // SM(총괄)은 전역(GLOBAL), LC(지역)은 지역(LOCAL) 권한 부여
                if ("SM".equals(auth)) {
                    result.put("result", "OK");
                    result.put("level", "GLOBAL");
                } else if ("LC".equals(auth)) {
                    result.put("result", "OK");
                    result.put("level", "LOCAL");
                } else {
                    result.put("result", "FAIL");
                    result.put("msg", "앱 관리자 권한이 없습니다.");
                }
            } else {
                result.put("result", "FAIL");
                result.put("msg", "아이디 또는 비밀번호를 확인해주세요.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", "FAIL");
            result.put("msg", "서버 에러가 발생했습니다.");
        }
        return result;
    }

    @RequestMapping(value = "/api/super/admin/feed/add", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> addAdminFeed(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String adminId = (String) params.get("adminId");
            String adminPw = (String) params.get("adminPw");
            String adminLevel = (String) params.get("adminLevel");
            String content = (String) params.get("content");
            String imageBase64 = (String) params.get("imageBase64");

            // 1. 프리패스 인증 검증 (보안)
            Map<String, Object> loginParams = new HashMap<>();
            loginParams.put("PRS_ID", adminId);
            loginParams.put("PRS_PWD", adminPw);
            UserVO adminUser = superAdminService.loginCheck(loginParams);

            if (adminUser == null) {
                result.put("result", "FAIL");
                result.put("msg", "관리자 인증에 실패했습니다.");
                return result;
            }

            // 2. 파라미터 조립
            Map<String, Object> param = new HashMap<>();
            param.put("ADMIN_ID", adminId);
            param.put("ADMIN_LEVEL", adminLevel);
            param.put("ADMIN_COUNTRY", adminUser.getPRS_COUNTRY());
            param.put("CON_BODY", content);

            // 3. 🌟 Base64 이미지 디코딩 및 파일 저장 처리
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                // 프론트에서 넘어온 "data:image/jpeg;base64,....." 포맷에서 진짜 데이터만 분리
                String[] parts = imageBase64.split(",");
                String base64Data = parts.length > 1 ? parts[1] : parts[0];

                // 확장자 유추 (간단히 처리)
                String ext = ".jpg";
                if (parts[0].contains("png"))
                    ext = ".png";
                else if (parts[0].contains("gif"))
                    ext = ".gif";

                // Base64 디코딩 (byte 배열로 변환)
                byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

                // 고유 파일명 생성 및 서버 물리 경로에 저장
                String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
                Path targetPath = Paths.get(Constants._FILE_SAVE_PATH + fileName);
                Files.write(targetPath, decodedBytes);

                // DB에 넣을 URL 주소 세팅
                param.put("IMAGE_URL", Constants._FILE_URL + fileName);
            }

            // 4. DB Insert
            superAdminService.insertAdminFeed(param);

            result.put("result", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", "FAIL");
            result.put("msg", "서버 저장 중 오류가 발생했습니다.");
        }
        return result;
    }

    @RequestMapping(value = "/api/super/admin/feed/delete", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> deleteAdminFeed(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String adminId = (String) params.get("adminId");
            String adminPw = (String) params.get("adminPw");
            String conIdStr = (String) params.get("conId"); // 예: "A5"

            // 1. 프리패스 인증 검증 (보안)
            Map<String, Object> loginParams = new HashMap<>();
            loginParams.put("PRS_ID", adminId);
            loginParams.put("PRS_PWD", adminPw);
            UserVO adminUser = superAdminService.loginCheck(loginParams);

            if (adminUser == null) {
                result.put("result", "FAIL");
                result.put("msg", "관리자 인증에 실패했습니다.");
                return result;
            }

            // 2. ID 파싱 및 삭제 실행
            if (conIdStr != null && conIdStr.startsWith("A")) {
                String realConId = conIdStr.substring(1); // 'A' 떼어내기
                superAdminService.deleteAdminFeed(realConId);
                result.put("result", "OK");
            } else {
                result.put("result", "FAIL");
                result.put("msg", "잘못된 관리자 게시글 ID입니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", "FAIL");
            result.put("msg", "서버 에러가 발생했습니다.");
        }
        return result;
    }

    // 🌟 [API] 안 읽은 메시지 개수 (폴링용)
    @RequestMapping(value = "/super/message/unreadCount.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getUnreadCount(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = superAdminService.getTotalUnreadMessages();
            result.put("status", "success");
            result.put("count", count);
        } catch (Exception e) {
            result.put("status", "fail");
        }
        return result;
    }

    // 🌟 [API] 채팅방 목록 조회
    @RequestMapping(value = "/super/message/threads.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getMessageThreads(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> list = superAdminService.getMessageThreads();
            result.put("status", "success");
            result.put("list", list);
        } catch (Exception e) {
            result.put("status", "fail");
        }
        return result;
    }

    // 🌟 [신규] 상단고정 글로벌 프로모션 정보 조회 API
    @RequestMapping(value = "/super/promotion/get.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getActivePromotion(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> promotion = superAdminService.getActivePromotion();
            result.put("status", "success");
            result.put("promotion", promotion);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "Failed to retrieve promotion links.");
        }
        return result;
    }

    // 🌟 [신규] 상단고정 글로벌 프로모션 정보 등록/수정 API
    @RequestMapping(value = "/super/promotion/save.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> savePromotion(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String adminId = (String) params.get("adminId");
            String adminPw = (String) params.get("adminPw");

            // 1. 관리자 권한 확인 (보안 검증)
            Map<String, Object> loginParams = new HashMap<>();
            loginParams.put("PRS_ID", adminId);
            loginParams.put("PRS_PWD", adminPw);

            UserVO adminUser = superAdminService.loginCheck(loginParams);
            if (adminUser == null || !("SM".equals(adminUser.getPRS_AUTH()) || "LC".equals(adminUser.getPRS_AUTH()))) {
                result.put("status", "fail");
                result.put("msg", "Invalid credentials or insufficient permissions.");
                return result;
            }

            // 2. 프로모션 정보 저장
            superAdminService.savePromotion(params);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "Failed to save promotion links.");
        }
        return result;
    }
}
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
import com.sensible.common.util.ImageModerationUtil;

@Controller
public class SuperAdminController {

    @Resource(name = "superAdminService")
    private SuperAdminService superAdminService;

    // 시스템 모니터링용 기존 빈 재사용 주입 (신규 연결 생성 금지)
    @Resource(name = "config")
    private java.util.Properties config;

    @Resource(name = "redisTemplate")
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "dataSource")
    private javax.sql.DataSource dataSource;

    // 5분 간격 구간 지표 수집기. 루트 컨텍스트에 없더라도 기존 순간값 지표는 그대로 나와야 하므로
    // 필수 주입(@Resource)이 아니라 선택 주입으로 둔다.
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.sensible.admin.scheduler.SystemMetricsCollector metricsCollector;

    // 헬퍼 메소드: 로그인 유저 확인
    private UserVO getLoginUser(HttpServletRequest request) {
        return (UserVO) request.getSession().getAttribute("SUPER_USER_SESSION");
    }

    // 헬퍼 메소드: 필터용 국가 코드 추출 (SM이면 null, LC면 국가코드 반환)
    private String getFilterCountry(UserVO user) {
        return "LC".equals(user.getPRS_AUTH()) ? user.getPRS_COUNTRY() : null;
    }

    // 헬퍼 메소드: GET 파라미터의 ISO-8859-1 한글 깨짐 디코딩
    private String decodeGetParameter(String value) {
        if (value == null || value.isEmpty()) return value;
        try {
            boolean isIso = true;
            for (int i = 0; i < value.length(); i++) {
                if (value.charAt(i) > 255) {
                    isIso = false;
                    break;
                }
            }
            if (isIso) {
                return new String(value.getBytes("ISO-8859-1"), "UTF-8");
            }
        } catch (Exception e) {
            // 디코딩 실패 시 원본 반환
        }
        return value;
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
    public String starList(HttpServletRequest request,
                           @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                           @RequestParam(value = "searchKeyword", required = false, defaultValue = "") String searchKeyword,
                           Model model) throws Exception {
        UserVO user = getLoginUser(request);
        if (user == null)
            return "redirect:/super/login.do";

        // GET 요청일 경우 ISO-8859-1 한글 깨짐 처리
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            searchKeyword = decodeGetParameter(searchKeyword);
        }

        String filterCountry = getFilterCountry(user);
        
        int length = 30; // 30 items per page
        int start = (page - 1) * length;
        if (start < 0) start = 0;

        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("country", filterCountry);
        params.put("searchKeyword", searchKeyword.trim());
        params.put("start", start);
        params.put("length", length);

        int totalCount = superAdminService.getStarListCount(params);
        List<Map<String, Object>> starList = superAdminService.getStarList(params);

        int totalPages = (int) Math.ceil((double) totalCount / length);
        if (totalPages == 0) totalPages = 1;

        // Calculate pagination sliding window (max 10 pages displayed)
        int startPage = Math.max(1, page - 4);
        int endPage = Math.min(totalPages, startPage + 9);
        if (endPage - startPage < 9) {
            startPage = Math.max(1, endPage - 9);
        }

        model.addAttribute("starList", starList);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("searchKeyword", searchKeyword);
        model.addAttribute("activeMenu", "star_list");

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

    // ===== 이미지 검수 (2-26차) =====

    /** 검수 대기 목록. 기본은 PENDING, 차단 목록도 같은 화면에서 본다 */
    @RequestMapping(value = "/super/moderation/list.do")
    public String moderationList(HttpServletRequest request, Model model,
            @RequestParam(value = "status", required = false) String status) throws Exception {
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            return "redirect:/super/dashboard.do";
        }

        String target = ("HIDDEN".equals(status) || "REJECTED".equals(status) || "APPROVED".equals(status))
                ? status : "PENDING";
        model.addAttribute("status", target);
        model.addAttribute("queue", superAdminService.getModerationQueue(target));
        model.addAttribute("counts", superAdminService.getModerationCounts());

        return "super/moderation_list";
    }

    /**
     * 검수 대기 이미지 미리보기.
     *
     * 대기 중 파일은 공개 디렉터리(/img)에 없으므로 웹으로 직접 열 수 없다.
     * 관리자만 볼 수 있도록 여기서 직접 내보낸다.
     */
    @RequestMapping(value = "/super/moderation/preview.do")
    public void moderationPreview(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("file") String file) throws Exception {
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // DB에서 온 값이라도 파일 시스템에 그대로 쓰지 않는다 (경로 조작 차단)
        String fileName = ImageModerationUtil.fileNameFromUrl(file);
        if (fileName == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        java.io.File found = null;
        for (String dir : new String[] { Constants._PENDING_SAVE_PATH, Constants._HIDDEN_SAVE_PATH,
                Constants._FILE_SAVE_PATH }) {
            java.io.File candidate = new java.io.File(dir, fileName);
            if (candidate.exists() && candidate.isFile()) {
                found = candidate;
                break;
            }
        }
        if (found == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String ext = ImageModerationUtil.extensionOf(fileName);
        response.setContentType("png".equals(ext) ? "image/png"
                : "webp".equals(ext) ? "image/webp" : "image/jpeg");
        response.setContentLength((int) found.length());
        // 검수 대상 이미지는 캐시하지 않는다. 차단 후에도 브라우저에 남으면 곤란하다
        response.setHeader("Cache-Control", "no-store");

        java.io.InputStream in = null;
        java.io.OutputStream out = null;
        try {
            in = new java.io.FileInputStream(found);
            out = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            if (in != null) { try { in.close(); } catch (Exception e) { } }
        }
    }

    /** 승인 / 차단 처리 */
    @RequestMapping(value = "/super/moderation/action.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> moderationAction(HttpServletRequest request,
            @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }

        try {
            String action = String.valueOf(params.get("action"));
            String targetType = String.valueOf(params.get("targetType"));
            String targetId = String.valueOf(params.get("targetId"));
            String reason = (String) params.get("reason");

            // REJECTED = 검수에서 거절, HIDDEN = 사후 신고로 블라인드.
            // 접근 차단 동작은 같고 이력·통계에서 원인을 구분하기 위해 나눈다(2-26차)
            if (!"APPROVED".equals(action) && !"REJECTED".equals(action) && !"HIDDEN".equals(action)) {
                result.put("status", "fail");
                result.put("msg", "알 수 없는 처리입니다.");
                return result;
            }

            boolean applied = superAdminService.applyModeration(
                    targetType, targetId, action, user.getPRS_ID(), reason);

            result.put("status", "success");
            result.put("applied", applied);
            result.put("msg", applied ? "처리되었습니다." : "이미 처리된 건입니다.");
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

    /**
     * 🌟 [화면] 약관/개인정보처리방침 수정 (SM 전용)
     */
    @RequestMapping(value = "/super/policy/edit.do")
    public String policyEditForm(HttpServletRequest request) {
        UserVO user = getLoginUser(request);
        if (user == null)
            return "redirect:/super/login.do";
        if (!"SM".equals(user.getPRS_AUTH())) {
            return "redirect:/super/dashboard.do";
        }
        return "super/policy_form";
    }

    /**
     * 🌟 [API] 약관 목록 조회 (SM 전용)
     */
    @RequestMapping(value = "/super/policy/list.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getPolicyList(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }
        try {
            result.put("status", "success");
            result.put("list", superAdminService.getPolicyList());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "약관 목록 조회에 실패했습니다.");
        }
        return result;
    }

    /**
     * 🌟 [API] 약관 본문 저장 (SM 전용)
     */
    @RequestMapping(value = "/super/policy/save.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> savePolicyContent(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }
        try {
            Object conId = params.get("CON_ID");
            Object body = params.get("CON_BODY");
            if (conId == null || body == null || String.valueOf(body).trim().isEmpty()) {
                result.put("status", "fail");
                result.put("msg", "본문이 비어 있습니다.");
                return result;
            }
            superAdminService.savePolicyContent(params);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            // 원인 불명의 "저장 실패"로 뭉개지 않도록 실제 사유를 그대로 노출한다 (SM 전용 화면)
            result.put("msg", e.getMessage() != null ? e.getMessage() : "약관 저장에 실패했습니다.");
        }
        return result;
    }

    /**
     * 🌟 [API] 약관 신규 등록 (SM 전용)
     * KIND=terms|privacy 구분을 받는다. 웹 /terms·/privacy가 제목의 privacy/개인정보 포함 여부로
     * 문서를 구분하므로, 등록 시 제목 규칙과 같은 구분의 중복 등록을 서버에서 검증한다.
     */
    @RequestMapping(value = "/super/policy/create.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> createPolicyContent(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }
        try {
            String kind = params.get("KIND") == null ? "" : String.valueOf(params.get("KIND"));
            String title = params.get("CON_TITLE") == null ? "" : String.valueOf(params.get("CON_TITLE")).trim();
            String body = params.get("CON_BODY") == null ? "" : String.valueOf(params.get("CON_BODY")).trim();

            boolean isPrivacy = "privacy".equals(kind);
            if (!isPrivacy && !"terms".equals(kind)) {
                result.put("status", "fail");
                result.put("msg", "약관 구분이 올바르지 않습니다.");
                return result;
            }
            if (title.isEmpty() || body.isEmpty()) {
                result.put("status", "fail");
                result.put("msg", "제목과 본문을 입력해 주세요.");
                return result;
            }
            if (isPrivacy != SuperAdminService.isPrivacyTitle(title)) {
                result.put("status", "fail");
                result.put("msg", isPrivacy
                        ? "개인정보처리방침 제목에는 'privacy' 또는 '개인정보'가 포함되어야 합니다."
                        : "이용약관 제목에는 'privacy'·'개인정보'를 포함할 수 없습니다.");
                return result;
            }
            for (Map<String, Object> row : superAdminService.getPolicyList()) {
                String rowTitle = row.get("CON_TITLE") == null ? "" : row.get("CON_TITLE").toString();
                if (SuperAdminService.isPrivacyTitle(rowTitle) == isPrivacy) {
                    result.put("status", "fail");
                    result.put("msg", "이미 등록된 약관입니다. 기존 항목을 수정해 주세요.");
                    return result;
                }
            }

            Map<String, Object> insertMap = new HashMap<>();
            insertMap.put("PRS_ID", user.getPRS_ID());
            insertMap.put("APP_ID", user.getAPP_ID());
            insertMap.put("CON_TITLE", title);
            insertMap.put("CON_BODY", body);
            superAdminService.createPolicyContent(insertMap);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "약관 등록에 실패했습니다.");
        }
        return result;
    }

    /**
     * [화면] 스타 일괄 등록 폼 (SM 전용)
     */
    @RequestMapping(value = "/super/star/bulk-create.do")
    public String createStarBulkForm(HttpServletRequest request, Model model) {
        UserVO user = getLoginUser(request);
        if (user == null)
            return "redirect:/super/login.do";
        if (!"SM".equals(user.getPRS_AUTH())) {
            return "redirect:/super/dashboard.do";
        }
        return "super/star_bulk_form";
    }

    /**
     * [API] 스타 일괄 등록 (SM 전용)
     */
    @RequestMapping(value = "/super/star/bulk-insert.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> insertStarBulk(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);

        if (user == null) {
            result.put("status", "fail");
            result.put("msg", "로그인 필요");
            return result;
        }

        if (!"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }

        try {
            String rawText = (String) params.get("bulkText");
            String pwd = (String) params.get("PRS_PWD");
            String country = (String) params.get("PRS_COUNTRY");

            if (rawText == null || rawText.trim().isEmpty()) {
                throw new Exception("등록할 텍스트를 입력해 주세요.");
            }
            if (pwd == null || pwd.trim().isEmpty()) {
                pwd = "123"; // 기본 초기 비밀번호 123
            }
            if (country == null || country.trim().isEmpty()) {
                throw new Exception("기본 국가 코드를 선택해 주세요.");
            }

            // 개행 문자로 라인 분할 및 파싱 진행
            java.util.List<String> names = new java.util.ArrayList<>();
            String[] lines = rawText.split("\\r?\\n");
            // [수정] 영문자 접두사(예: S-)가 붙어있는 패턴도 매칭되도록 정규식 보완
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^\\s*(?:[a-zA-Z]+-)?\\d+\\.\\s*(.+)$");

            for (String line : lines) {
                java.util.regex.Matcher matcher = pattern.matcher(line.trim());
                if (matcher.find()) {
                    String name = matcher.group(1).trim();
                    // [수정] 뒤의 괄호에 표기된 메타 정보들(예: "(TWS / ...)")을 걷어내고 순수 이름만 추출
                    int bracketIdx = name.indexOf("(");
                    if (bracketIdx != -1) {
                        name = name.substring(0, bracketIdx).trim();
                    }
                    if (!name.isEmpty()) {
                        names.add(name);
                    }
                }
            }

            if (names.isEmpty()) {
                throw new Exception("유효한 스타 목록 패턴 '(번호). (이름)'을 찾을 수 없습니다.");
            }

            Map<String, Object> bulkResult = superAdminService.insertStarBulk(names, pwd, country);
            result.put("status", "success");
            result.put("msg", bulkResult.get("successCount") + "명의 스타가 성공적으로 일괄 등록되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "일괄 등록 실패: " + e.getMessage());
        }
        return result;
    }

    /**
     * [API] 기존 비밀번호가 비어있는 스타(star_*) 비밀번호 123으로 일괄 변경 (SM 전용)
     */
    @RequestMapping(value = "/super/star/reset-empty-pwd.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> resetEmptyPwd(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }

        try {
            int updatedCount = superAdminService.resetEmptyPasswords();
            result.put("status", "success");
            result.put("msg", "비밀번호가 비어있던 스타 " + updatedCount + "명의 비밀번호를 123으로 변경 완료했습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "오류 발생: " + e.getMessage());
        }
        return result;
    }

    /**
     * [API] 특정 스타 비밀번호 초기화 (123으로 재설정)
     */
    @RequestMapping(value = "/super/star/resetPwd.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> resetStarPwd(HttpServletRequest request,
                                            @RequestParam("PRS_ID") String prsId) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null) {
            result.put("status", "fail");
            result.put("msg", "로그인 필요");
            return result;
        }
        try {
            int updated = superAdminService.resetStarPassword(prsId);
            if (updated > 0) {
                result.put("status", "success");
            } else {
                result.put("status", "fail");
                result.put("msg", "대상 스타를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * [화면] 시스템 관리 패널 (SM 전용)
     */
    @RequestMapping(value = "/super/system/panel.do")
    public String systemPanel(HttpServletRequest request, Model model) {
        UserVO user = getLoginUser(request);
        if (user == null)
            return "redirect:/super/login.do";
        if (!"SM".equals(user.getPRS_AUTH())) {
            return "redirect:/super/dashboard.do";
        }
        model.addAttribute("activeMenu", "system_panel");

        // global.properties 튜닝값을 프론트로 전달 (하드코딩 제거). 값이 없거나 파싱 실패 시 안전 기본값 사용
        model.addAttribute("pollingInterval", getConfigInt("monitor.polling.interval", 5000));
        model.addAttribute("cpuWarn", getConfigInt("monitor.cpu.threshold.warn", 60));
        model.addAttribute("cpuDanger", getConfigInt("monitor.cpu.threshold.danger", 80));
        model.addAttribute("diskWarn", getConfigInt("monitor.disk.threshold.warn", 70));
        model.addAttribute("diskDanger", getConfigInt("monitor.disk.threshold.danger", 85));

        return "super/system_panel";
    }

    // global.properties(config 빈)에서 정수 설정값을 안전하게 읽는 헬퍼
    private int getConfigInt(String key, int defaultValue) {
        try {
            if (config == null)
                return defaultValue;
            String v = config.getProperty(key);
            if (v == null || v.trim().isEmpty())
                return defaultValue;
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * [API] 실시간 JVM 및 시스템 상태 조회 (SM 전용)
     */
    @RequestMapping(value = "/super/system/status.json", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getSystemStatus(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);

        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }

        try {
            java.lang.management.MemoryMXBean memoryMXBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            java.lang.management.MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
            java.lang.management.MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
            
            java.lang.management.ThreadMXBean thread = java.lang.management.ManagementFactory.getThreadMXBean();
            java.lang.management.RuntimeMXBean runtime = java.lang.management.ManagementFactory.getRuntimeMXBean();
            java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();

            long mb = 1024 * 1024;

            // Heap memory status
            result.put("heapInit", heap.getInit() / mb);
            result.put("heapUsed", heap.getUsed() / mb);
            result.put("heapCommitted", heap.getCommitted() / mb);
            result.put("heapMax", heap.getMax() / mb);
            result.put("heapPercent", heap.getMax() > 0 ? (int) ((double) heap.getUsed() / heap.getMax() * 100) : 0);

            // Non-Heap memory status
            result.put("nonHeapInit", nonHeap.getInit() / mb);
            result.put("nonHeapUsed", nonHeap.getUsed() / mb);
            result.put("nonHeapCommitted", nonHeap.getCommitted() / mb);
            result.put("nonHeapMax", nonHeap.getMax() / mb);

            // Threads status
            result.put("threadCount", thread.getThreadCount());
            result.put("peakThreadCount", thread.getPeakThreadCount());
            result.put("totalStartedThreadCount", thread.getTotalStartedThreadCount());

            // OS & JVM Info
            result.put("availableProcessors", os.getAvailableProcessors());
            result.put("systemLoadAverage", os.getSystemLoadAverage());
            result.put("osName", os.getName());
            result.put("osArch", os.getArch());
            result.put("jvmName", runtime.getVmName());
            result.put("jvmVersion", System.getProperty("java.version"));
            
            long uptimeMs = runtime.getUptime();
            long uptimeSec = uptimeMs / 1000;
            long uptimeMin = uptimeSec / 60;
            long uptimeHour = uptimeMin / 60;
            String uptimeStr = String.format("%d시간 %d분 %d초", uptimeHour, uptimeMin % 60, uptimeSec % 60);
            result.put("jvmUptime", uptimeStr);

            // ===== [신규] CPU / Disk / Redis / DB / Queue =====
            // 각 지표는 독립적으로 격리 수집. 하나가 실패해도 나머지 지표와 엔드포인트는 정상 반환.

            // 1) CPU 사용률(%) - com.sun.management API 우선, 미지원/실패 시 load average 폴백
            result.put("cpuAvailable", false);
            result.put("cpuSystemPercent", 0);
            result.put("cpuProcessPercent", 0);
            result.put("systemLoadAverage", os.getSystemLoadAverage());
            try {
                if (os instanceof com.sun.management.OperatingSystemMXBean) {
                    com.sun.management.OperatingSystemMXBean sunOs = (com.sun.management.OperatingSystemMXBean) os;
                    double sysLoad = sunOs.getSystemCpuLoad();   // 0.0 ~ 1.0 (음수면 아직 미측정)
                    double procLoad = sunOs.getProcessCpuLoad(); // 0.0 ~ 1.0
                    if (sysLoad >= 0) {
                        result.put("cpuSystemPercent", (int) Math.round(sysLoad * 100));
                        result.put("cpuAvailable", true);
                    }
                    if (procLoad >= 0) {
                        result.put("cpuProcessPercent", (int) Math.round(procLoad * 100));
                    }
                }
            } catch (Throwable t) {
                // com.sun API 미지원 환경 등: load average 폴백값이 이미 채워져 있음
                result.put("cpuAvailable", false);
            }

            // 2) Disk 사용량 - 설정된 경로(기본 "/")의 파일시스템 용량
            result.put("diskAvailable", false);
            result.put("diskTotalGb", 0);
            result.put("diskUsedGb", 0);
            result.put("diskFreeGb", 0);
            result.put("diskPercent", 0);
            try {
                String diskPath = (config != null) ? config.getProperty("monitor.disk.path", "/") : "/";
                if (diskPath == null || diskPath.trim().isEmpty())
                    diskPath = "/";
                java.io.File disk = new java.io.File(diskPath.trim());
                long total = disk.getTotalSpace();
                long usable = disk.getUsableSpace();
                if (total > 0) {
                    long gb = 1024L * 1024L * 1024L;
                    long used = total - usable;
                    result.put("diskTotalGb", total / gb);
                    result.put("diskUsedGb", used / gb);
                    result.put("diskFreeGb", usable / gb);
                    result.put("diskPercent", (int) ((double) used / total * 100));
                    result.put("diskAvailable", true);
                }
            } catch (Throwable t) {
                result.put("diskAvailable", false);
            }

            // 3) Redis - 기존 redisTemplate 연결로 PING (로컬 미연결은 정상 시나리오 → DOWN 표기)
            result.put("redisStatus", "DOWN");
            result.put("redisMsg", "연결되지 않음");
            result.put("redisUsedMemoryMb", -1);
            result.put("redisPeakMemoryMb", -1);
            result.put("redisClients", "-");
            result.put("redisEvictedKeys", "-");
            result.put("redisKeyCount", -1);
            org.springframework.data.redis.connection.RedisConnection redisConn = null;
            try {
                redisConn = redisTemplate.getConnectionFactory().getConnection();
                String pong = redisConn.ping();
                result.put("redisStatus", "UP");
                result.put("redisMsg", (pong != null ? pong : "PONG"));

                // INFO / DBSIZE는 같은 연결을 재사용한다 (신규 연결 생성 금지)
                try {
                    java.util.Properties info = redisConn.info();
                    if (info != null) {
                        long redisUsed = parseLongSafe(info.getProperty("used_memory"), -1);
                        long redisPeak = parseLongSafe(info.getProperty("used_memory_peak"), -1);
                        result.put("redisUsedMemoryMb", (redisUsed >= 0) ? redisUsed / mb : -1);
                        result.put("redisPeakMemoryMb", (redisPeak >= 0) ? redisPeak / mb : -1);
                        result.put("redisClients", nvl(info.getProperty("connected_clients")));
                        result.put("redisEvictedKeys", nvl(info.getProperty("evicted_keys")));
                    }
                } catch (Throwable ignore) {
                    // INFO 미지원·권한 제한 등은 부가 정보 손실로만 처리
                }
                try {
                    Long dbSize = redisConn.dbSize();
                    if (dbSize != null) {
                        result.put("redisKeyCount", dbSize);
                    }
                } catch (Throwable ignore) {
                    // 키 개수는 부가 정보
                }
            } catch (Throwable t) {
                result.put("redisStatus", "DOWN");
                result.put("redisMsg", "연결되지 않음");
            } finally {
                if (redisConn != null) {
                    try {
                        redisConn.close();
                    } catch (Throwable ignore) {
                    }
                }
            }

            // 4) DB - 기존 dataSource로 SELECT 1 경량 헬스체크 + 커넥션 풀 현황
            result.put("dbStatus", "DOWN");
            result.put("dbMsg", "연결 실패");
            result.put("dbActive", -1);
            result.put("dbIdle", -1);
            try (java.sql.Connection conn = dataSource.getConnection();
                 java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    result.put("dbStatus", "UP");
                    result.put("dbMsg", "정상");
                }
            } catch (Throwable t) {
                result.put("dbStatus", "DOWN");
                result.put("dbMsg", "연결 실패");
            }
            // DBCP 풀 현황. 주입된 dataSource는 log4jdbc 프록시(Log4jdbcProxyDataSource)가
            // BasicDataSource를 감싸고 있어 직접 캐스팅이 안 된다 → 내부 필드를 찾아 꺼낸다.
            result.put("dbMaxActive", -1);
            try {
                org.apache.commons.dbcp.BasicDataSource bds = unwrapBasicDataSource(dataSource, 0);
                if (bds != null) {
                    result.put("dbActive", bds.getNumActive());
                    result.put("dbIdle", bds.getNumIdle());
                    result.put("dbMaxActive", bds.getMaxActive());
                }
            } catch (Throwable t) {
                // 풀 통계는 부가 정보이므로 실패 무시
            }

            // 5) Queue - 프로젝트에 메시지 큐 미도입 → N/A 자리표시자
            result.put("queueStatus", "N/A");
            result.put("queueMsg", "메시지 큐 미도입");

            // 6) GC 통계 - GC 실행 버튼만 있고 GC 추이를 볼 수 없던 부분 보완
            result.put("gcAvailable", false);
            result.put("gcTotalCount", 0);
            result.put("gcTotalTimeMs", 0);
            result.put("gcTimePercent", 0);
            try {
                List<Map<String, Object>> gcList = new java.util.ArrayList<Map<String, Object>>();
                long gcCount = 0;
                long gcTime = 0;
                for (java.lang.management.GarbageCollectorMXBean gc :
                        java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
                    Map<String, Object> row = new HashMap<String, Object>();
                    row.put("name", gc.getName());
                    row.put("count", Math.max(0, gc.getCollectionCount()));
                    row.put("timeMs", Math.max(0, gc.getCollectionTime()));
                    gcList.add(row);
                    if (gc.getCollectionCount() > 0) {
                        gcCount += gc.getCollectionCount();
                    }
                    if (gc.getCollectionTime() > 0) {
                        gcTime += gc.getCollectionTime();
                    }
                }
                result.put("gcCollectors", gcList);
                result.put("gcTotalCount", gcCount);
                result.put("gcTotalTimeMs", gcTime);
                // JVM 가동시간 대비 GC가 점유한 비율
                long up = runtime.getUptime();
                result.put("gcTimePercent", (up > 0) ? Math.round((double) gcTime / up * 10000.0) / 100.0 : 0);
                result.put("gcAvailable", true);
            } catch (Throwable t) {
                result.put("gcAvailable", false);
            }

            // 7) 톰캣 스레드풀 / 활성 세션 - JMX. 배포 형태에 따라 도메인이 Catalina 또는 Tomcat이다
            result.put("tomcatAvailable", false);
            result.put("tomcatThreadsBusy", -1);
            result.put("tomcatThreadsMax", -1);
            result.put("tomcatThreadPercent", 0);
            result.put("tomcatActiveSessions", -1);
            try {
                javax.management.MBeanServer mbs = java.lang.management.ManagementFactory.getPlatformMBeanServer();
                int busy = 0;
                int maxThreads = 0;
                int sessions = 0;
                boolean poolFound = false;
                boolean sessionFound = false;
                String[] domains = { "Catalina", "Tomcat" };
                for (String domain : domains) {
                    for (javax.management.ObjectName on :
                            mbs.queryNames(new javax.management.ObjectName(domain + ":type=ThreadPool,*"), null)) {
                        Object b = mbs.getAttribute(on, "currentThreadsBusy");
                        Object m = mbs.getAttribute(on, "maxThreads");
                        if (b instanceof Number) {
                            busy += ((Number) b).intValue();
                        }
                        if (m instanceof Number) {
                            maxThreads += ((Number) m).intValue();
                        }
                        poolFound = true;
                    }
                    for (javax.management.ObjectName on :
                            mbs.queryNames(new javax.management.ObjectName(domain + ":type=Manager,*"), null)) {
                        Object a = mbs.getAttribute(on, "activeSessions");
                        if (a instanceof Number) {
                            sessions += ((Number) a).intValue();
                            sessionFound = true;
                        }
                    }
                }
                if (poolFound) {
                    result.put("tomcatAvailable", true);
                    result.put("tomcatThreadsBusy", busy);
                    result.put("tomcatThreadsMax", maxThreads);
                    result.put("tomcatThreadPercent",
                            (maxThreads > 0) ? (int) ((double) busy / maxThreads * 100) : 0);
                }
                if (sessionFound) {
                    result.put("tomcatActiveSessions", sessions);
                }
            } catch (Throwable t) {
                result.put("tomcatAvailable", false);
            }

            // 8) 열린 파일 디스크립터 - 누수 조기 감지용
            result.put("fdAvailable", false);
            result.put("fdOpen", -1);
            result.put("fdMax", -1);
            result.put("fdPercent", 0);
            try {
                if (os instanceof com.sun.management.UnixOperatingSystemMXBean) {
                    com.sun.management.UnixOperatingSystemMXBean unixOs =
                            (com.sun.management.UnixOperatingSystemMXBean) os;
                    long openFd = unixOs.getOpenFileDescriptorCount();
                    long maxFd = unixOs.getMaxFileDescriptorCount();
                    if (openFd >= 0 && maxFd > 0) {
                        result.put("fdOpen", openFd);
                        result.put("fdMax", maxFd);
                        result.put("fdPercent", (int) ((double) openFd / maxFd * 100));
                        result.put("fdAvailable", true);
                    }
                }
            } catch (Throwable t) {
                result.put("fdAvailable", false);
            }

            // 9) 구간 지표(트래픽·요청량·디스크 추세·디렉터리 용량)
            // 순간값이 아니라 이전 스냅샷과의 델타가 필요한 값이라 5분 수집기가 계산해 둔 것을 읽기만 한다.
            // 디렉터리 용량은 무거운 작업이라 수집기가 별도 주기로 캐시해 둔 값이다 (폴링마다 재계산하지 않음).
            result.put("collectorAvailable", metricsCollector != null);
            try {
                if (metricsCollector != null) {
                    result.putAll(metricsCollector.getTrafficSummary());
                    result.putAll(metricsCollector.getRequestSummary());
                    result.putAll(metricsCollector.getDiskTrend());
                    result.putAll(metricsCollector.getDirectorySizes());
                }
            } catch (Throwable t) {
                // 구간 지표 실패가 기존 순간값 응답을 막지 않도록 무시
            }

            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "상태 조회 실패: " + e.getMessage());
        }

        return result;
    }

    // Redis INFO 문자열을 안전하게 long으로 변환
    private long parseLongSafe(String v, long defaultValue) {
        try {
            if (v == null || v.trim().isEmpty()) {
                return defaultValue;
            }
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // null이면 "-" 로 표기
    private String nvl(String v) {
        return (v == null || v.trim().isEmpty()) ? "-" : v.trim();
    }

    // log4jdbc 프록시가 감싼 BasicDataSource를 찾아 꺼낸다. 못 찾으면 null
    private org.apache.commons.dbcp.BasicDataSource unwrapBasicDataSource(javax.sql.DataSource ds, int depth) {
        if (ds == null || depth > 3) {
            return null;
        }
        if (ds instanceof org.apache.commons.dbcp.BasicDataSource) {
            return (org.apache.commons.dbcp.BasicDataSource) ds;
        }
        Class<?> clazz = ds.getClass();
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                if (!javax.sql.DataSource.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object inner = f.get(ds);
                    if (inner instanceof javax.sql.DataSource && inner != ds) {
                        org.apache.commons.dbcp.BasicDataSource found =
                                unwrapBasicDataSource((javax.sql.DataSource) inner, depth + 1);
                        if (found != null) {
                            return found;
                        }
                    }
                } catch (Throwable ignore) {
                    // 접근 불가 필드는 건너뛴다
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * [API] 5분 간격 구간 지표 이력 조회 (SM 전용)
     * 차트용이라 status.json과 분리한다. 5초 폴링 대상이 아니다.
     */
    @RequestMapping(value = "/super/system/history.json", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getSystemHistory(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);

        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }

        try {
            if (metricsCollector == null) {
                result.put("status", "success");
                result.put("available", false);
                result.put("samples", new java.util.ArrayList<Object>());
                return result;
            }
            List<Map<String, Object>> samples = metricsCollector.getHistory();
            result.put("status", "success");
            result.put("available", true);
            result.put("intervalMinutes", 5);
            result.put("samples", samples);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "이력 조회 실패: " + e.getMessage());
        }
        return result;
    }

    /**
     * [API] 메모리 정리(GC) 실행 (SM 전용)
     */
    @RequestMapping(value = "/super/system/clean.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> executeMemoryCleanup(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);

        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }

        try {
            java.lang.management.MemoryMXBean memoryMXBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            long beforeUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            
            // Explicit System GC execution
            System.gc();
            
            // Wait briefly for GC thread to complete work
            Thread.sleep(150);
            
            long afterUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long cleanedMemory = (beforeUsed - afterUsed) / (1024 * 1024);

            result.put("status", "success");
            result.put("cleanedMemoryMb", Math.max(0, cleanedMemory));
            result.put("msg", "가비지 컬렉션(GC)을 실행하여 약 " + Math.max(0, cleanedMemory) + "MB의 메모리가 해제되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "메모리 정리 실패: " + e.getMessage());
        }

        return result;
    }

    // ==========================================
    // 🌟 [신규] VS 배틀필드 관리 (SM 전용 — 카드는 전 국가 공통 노출)
    // ==========================================

    /**
     * [화면] VS 카드 목록 (AUTO 14종 + 커스텀)
     */
    @RequestMapping(value = "/super/vs/list.do")
    public String vsCardList(HttpServletRequest request, Model model) throws Exception {
        UserVO user = getLoginUser(request);
        if (user == null)
            return "redirect:/super/login.do";
        if (!"SM".equals(user.getPRS_AUTH())) {
            return "redirect:/super/dashboard.do";
        }

        model.addAttribute("vsList", superAdminService.getVsCardList());
        model.addAttribute("activeMenu", "vs_list");
        return "super/vs_list";
    }

    /**
     * [API] VS 카드 고정(Pin) 토글
     */
    @RequestMapping(value = "/super/vs/pin.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> toggleVsPin(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }
        try {
            superAdminService.toggleVsPin(params);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * [API] VS 카드 노출/비노출 토글 (기본 매치 16장·커스텀 매치 공통)
     */
    @RequestMapping(value = "/super/vs/visible.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> toggleVsVisible(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }
        try {
            superAdminService.toggleVsVisible(params);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * [API] VS 카드 순서 저장 (드래그 결과 — 카드 ID 배열 순서대로 PIN_ORDER 재부여)
     */
    @RequestMapping(value = "/super/vs/order.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveVsOrder(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }
        try {
            @SuppressWarnings("unchecked")
            List<Object> vsIds = (List<Object>) payload.get("vsIds");
            if (vsIds == null || vsIds.isEmpty()) {
                throw new Exception("저장할 순서 정보가 없습니다.");
            }
            superAdminService.saveVsOrder(vsIds);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * [API] 커스텀 VS(특별전) 등록
     */
    @RequestMapping(value = "/super/vs/custom/insert.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> insertVsCustom(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }
        try {
            superAdminService.insertVsCustom(params);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * [API] 커스텀 VS 삭제 (AUTO 카드는 매퍼에서 CARD_KIND='CUSTOM' 조건으로 방어)
     */
    @RequestMapping(value = "/super/vs/custom/delete.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> deleteVsCustom(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }
        try {
            superAdminService.deleteVsCustom(params);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * [API] 커스텀 VS 등록용 스타 검색
     */
    @RequestMapping(value = "/super/vs/starSearch.do")
    @ResponseBody
    public Map<String, Object> searchVsStars(HttpServletRequest request,
            @RequestParam(value = "searchKeyword", required = false, defaultValue = "") String searchKeyword) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null || !"SM".equals(user.getPRS_AUTH())) {
            result.put("status", "fail");
            result.put("msg", "권한이 없습니다.");
            return result;
        }
        try {
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                searchKeyword = decodeGetParameter(searchKeyword);
            }
            Map<String, Object> params = new HashMap<>();
            params.put("searchKeyword", searchKeyword.trim());
            result.put("status", "success");
            result.put("list", superAdminService.searchVsStars(params));
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    // ==========================================
    // 🌟 [신규] 스타 직군(카테고리) 수동 분류 (SM 전체 / LC 자국)
    // ==========================================

    /**
     * [화면] 스타 카테고리 분류 목록
     */
    @RequestMapping(value = "/super/star/category.do")
    public String starCategoryList(HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "searchKeyword", required = false, defaultValue = "") String searchKeyword,
            @RequestParam(value = "filterCategory", required = false, defaultValue = "") String filterCategory,
            Model model) throws Exception {
        UserVO user = getLoginUser(request);
        if (user == null)
            return "redirect:/super/login.do";

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            searchKeyword = decodeGetParameter(searchKeyword);
        }

        String filterCountry = getFilterCountry(user);

        int length = 30;
        int start = (page - 1) * length;
        if (start < 0) start = 0;

        Map<String, Object> params = new HashMap<>();
        params.put("country", filterCountry);
        params.put("searchKeyword", searchKeyword.trim());
        params.put("filterCategory", filterCategory.trim());
        params.put("start", start);
        params.put("length", length);

        int totalCount = superAdminService.getStarCategoryListCount(params);
        List<Map<String, Object>> starList = superAdminService.getStarCategoryList(params);

        int totalPages = (int) Math.ceil((double) totalCount / length);
        if (totalPages == 0) totalPages = 1;

        int startPage = Math.max(1, page - 4);
        int endPage = Math.min(totalPages, startPage + 9);
        if (endPage - startPage < 9) {
            startPage = Math.max(1, endPage - 9);
        }

        model.addAttribute("starList", starList);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("searchKeyword", searchKeyword);
        model.addAttribute("filterCategory", filterCategory);
        model.addAttribute("activeMenu", "star_category");
        return "super/star_category";
    }

    /**
     * [API] 스타 카테고리 저장 (LC는 자국 스타만 — 매퍼의 country 조건으로 방어)
     */
    @RequestMapping(value = "/super/star/updateCategory.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updateStarCategory(HttpServletRequest request,
            @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null) {
            result.put("status", "fail");
            result.put("msg", "로그인 필요");
            return result;
        }
        try {
            params.put("country", getFilterCountry(user));
            superAdminService.updateStarCategory(params);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    // ==========================================
    // 🌟 [신규] 스타 소개(Bio) 관리 (2-27차, SM 전체 / LC 자국)
    // 웹 랜딩(/star/{id}) About 섹션에 노출할 소개문을 입력한다
    // ==========================================

    /**
     * [화면] 스타 소개문 관리 목록
     */
    @RequestMapping(value = "/super/star/bio.do")
    public String starBioList(HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "searchKeyword", required = false, defaultValue = "") String searchKeyword,
            @RequestParam(value = "filterBio", required = false, defaultValue = "") String filterBio,
            Model model) throws Exception {
        UserVO user = getLoginUser(request);
        if (user == null)
            return "redirect:/super/login.do";

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            searchKeyword = decodeGetParameter(searchKeyword);
        }

        String filterCountry = getFilterCountry(user);

        int length = 30;
        int start = (page - 1) * length;
        if (start < 0) start = 0;

        Map<String, Object> params = new HashMap<>();
        params.put("country", filterCountry);
        params.put("searchKeyword", searchKeyword.trim());
        params.put("filterBio", filterBio.trim());
        params.put("start", start);
        params.put("length", length);

        int totalCount = superAdminService.getStarBioListCount(params);
        List<Map<String, Object>> starList = superAdminService.getStarBioList(params);

        int totalPages = (int) Math.ceil((double) totalCount / length);
        if (totalPages == 0) totalPages = 1;

        int startPage = Math.max(1, page - 4);
        int endPage = Math.min(totalPages, startPage + 9);
        if (endPage - startPage < 9) {
            startPage = Math.max(1, endPage - 9);
        }

        model.addAttribute("starList", starList);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("searchKeyword", searchKeyword);
        model.addAttribute("filterBio", filterBio);
        model.addAttribute("bioMaxLength", SuperAdminService.STAR_BIO_MAX_LENGTH);
        model.addAttribute("activeMenu", "star_bio");
        return "super/star_bio";
    }

    /**
     * [API] 스타 소개문 저장 (LC는 자국 스타만 — 매퍼의 country 조건으로 방어)
     */
    @RequestMapping(value = "/super/star/updateBio.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updateStarBio(HttpServletRequest request,
            @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginUser(request);
        if (user == null) {
            result.put("status", "fail");
            result.put("msg", "로그인 필요");
            return result;
        }
        try {
            params.put("country", getFilterCountry(user));
            superAdminService.updateStarBio(params);
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", e.getMessage());
        }
        return result;
    }
}
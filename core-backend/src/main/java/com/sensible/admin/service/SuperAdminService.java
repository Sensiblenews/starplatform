package com.sensible.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import com.sensible.admin.domain.UserVO;
import com.sensible.common.dao.DefaultDAO;

@Service("superAdminService")
public class SuperAdminService {

    @Resource(name="DefaultDAO")
    private DefaultDAO dao;
    
    public Map<String, Object> getDashboardStats(String country) throws Exception {
        Map<String, Object> stats = new HashMap<>();

        // 1. CEO Dashboard 4대 핵심 지표
        stats.put("DAU", dao.selectOne("super.selectDAU", country));
        stats.put("MAU", dao.selectOne("super.selectActiveUserCount", country)); 
        stats.put("newSignups", dao.selectOne("super.selectNewSignups", country));
        stats.put("retentionRate", dao.selectOne("super.selectRetentionRate", country));

        // 🌟 [신규 추가] 2. 서버 상태 (JVM 메모리 및 DB 상태)
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        
        int memoryUsagePercent = (int) ((double) usedMemory / maxMemory * 100);
        
        stats.put("serverUsedMem", usedMemory);
        stats.put("serverMaxMem", maxMemory);
        stats.put("serverMemPercent", memoryUsagePercent);
        stats.put("dbStatus", "ON"); // 여기까지 에러 없이 도달했다면 DB는 정상 연결 상태

        // 3. 기존 지표 유지
        stats.put("starCount", dao.selectOne("super.selectStarCount", country));
        stats.put("totalAdViews", dao.selectOne("super.selectTotalAdViews", country));
        stats.put("monthlyStats", dao.selectList("super.selectMonthlyAdStats", country));

        if (country == null) {
            stats.put("countryStats", dao.selectList("super.selectStarCountByCountry"));
        }

        return stats;
    }
    /**
     * [신규] 지역 관리자 생성
     */
    public void insertLocalAdmin(Map<String, Object> params) throws Exception {
        // ID 중복 체크
        int exists = dao.selectOne("super.checkIdDuplicate", params.get("PRS_ID"));
        if (exists > 0) throw new Exception("이미 사용 중인 아이디입니다.");
     // 🌟 [신규] 총 안 읽은 메시지 수

        dao.insert("super.insertLocalAdmin", params);
    }

    // [수정] 스타 생성 (ID 자동생성)
    public void insertStar(Map<String, Object> params) throws Exception {
        int nextSeq = dao.selectOne("super.selectNextStarSeq");
        String nextId = "star_" + nextSeq;
        params.put("PRS_ID", nextId);
        params.put("APP_ID", nextId);
        params.put("PRS_USE_YN", "Y");
        params.put("PRS_AUTH", "ST");
        params.put("PRS_TYPE", "4");

        dao.insert("super.insertStar", params);
    }
    
    // [수정] 목록 조회 (국가 필터)
    public List<Map<String, Object>> getStarList(String country) throws Exception {
        return dao.selectList("super.selectStarList", country);
    }
    
    public UserVO loginCheck(Map<String, Object> params) throws Exception {
        return dao.selectOne("super.selectSuperLogin", params);
    }

    public void updateStarStatus(Map<String, Object> params) throws Exception {
        dao.update("super.updateStarStatus", params);
    }
    
    public void updateCommentStatus(Map<String, Object> params) throws Exception {
        dao.update("super.updateCommentStatus", params);
    }
    
    // [수정] 인기 스타(Top 6) 토글 및 제한 체크
    public Map<String, Object> togglePopularStatus(Map<String, Object> params) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        String targetStatus = (String) params.get("TARGET_STATUS"); // 'Y' or 'N'
        String country = (String) params.get("PRS_COUNTRY");       // 국가 코드

        // 1. [방어 로직] 'Y'로 켜려는 경우, 이미 6명이 찼는지 검사
        if ("Y".equals(targetStatus)) {
            // super.xml의 카운트 쿼리 호출
            int currentCount = dao.selectOne("super.countPopularStars", country);
            
            if (currentCount >= 16) {
                // 16명 꽉 찼으면 즉시 실패 리턴
                result.put("status", "fail");
                result.put("msg", "인기 스타(Top 16)는 국가별 최대 16명까지만 지정 가능합니다.\n기존 스타를 해제한 후 다시 시도해주세요.");
                return result;
            }
        }

        // 2. 문제 없으면 업데이트 실행
        params.put("IS_POPULAR", targetStatus);
        dao.update("super.updatePopularStatus", params);
        
        result.put("status", "success");
        return result;
    } // 여기서 메소드를 확실히 닫습니다.

    /**
     * [신규] 스타 피드 목록 가져오기
     */
    public List<Map<String, Object>> getStarFeedList(String starId) throws Exception {
        return dao.selectList("super.selectStarFeedList", starId);
    }

    /**
     * [신규] 피드 고정 토글 (Pin/Unpin)
     */
    public void toggleFeedPin(Map<String, Object> params) throws Exception {
        dao.update("super.updateFeedPin", params);
    }

    /**
     * [신규] 피드 삭제
     */
    public void deleteFeed(String conId) throws Exception {
        // FK 제약조건(ON DELETE CASCADE) 고려하여 마스터 삭제
        dao.delete("super.deleteFeedMaster", conId);
    }
    
 // 좋아요 토글 로직
    public void toggleFeedLike(Map<String, Object> params) throws Exception {
        int exists = dao.selectOne("super.checkUserLike", params);
        
        if (exists > 0) {
            // 이미 눌렀으면 -> 취소 (삭제)
            dao.delete("super.deleteLike", params);
        } else {
            // 안 눌렀으면 -> 추가
            dao.insert("super.insertLike", params);
        }
    }

    // 갱신된 하트 개수 가져오기 헬퍼
    public int getHeartCount(Map<String, Object> params) throws Exception {
        return dao.selectOne("super.selectHeartCount", params); // 쿼리는 간단해서 생략 (SELECT COUNT(*) ...)
    }

    public boolean checkUserLike(Map<String, Object> params) throws Exception {
        return (int) dao.selectOne("super.checkUserLike", params) > 0;
    }
    
 // [수정] 무한 스크롤을 위한 페이징 파라미터 추가
    public List<Map<String, Object>> getRecentFeedList(String country, int offset, int limit) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("country", country);
        params.put("offset", offset);
        params.put("limit", limit);
        return dao.selectList("super.selectRecentFeedList", params);
    }
    
    /**
     * [신규] CSV 파일 생성 및 다운로드 데이터 준비
     */
    public String generateStatsCsv(Map<String, Object> params) throws Exception {
        // [수정] 쿼리 ID 변경 (selectMonthlyStarStats -> selectAdStats)
        List<Map<String, Object>> list = dao.selectList("super.selectAdStats", params);
        
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // BOM
        
        // 헤더
        sb.append("스타ID,이름,국가,총 노출수,Android 노출,iOS 노출\n");
        
        // 데이터
        for (Map<String, Object> row : list) {
            sb.append(row.get("PRS_ID")).append(",");
            sb.append(row.get("PRS_NAME")).append(",");
            sb.append(row.get("PRS_COUNTRY")).append(",");
            sb.append(row.get("TOTAL_CNT")).append(",");
            sb.append(row.get("ANDROID_CNT")).append(",");
            sb.append(row.get("IOS_CNT")).append("\n");
        }
        
        return sb.toString();
    }
    
 // [신규] 현재 순서 설정 불러오기
    public Map<String, Object> getOrderSettings(String country) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 선택 가능한 스타 목록 (LC는 자기 나라만, SM은 전체)
        result.put("allStars", dao.selectList("super.selectAvailableStarsForOrder", country));
        
        // 2. 현재 지정된 세팅값 가져오기
        List<Map<String, Object>> currentList = dao.selectList("super.selectCurrentOrderSettings", country);
        
        Map<Integer, String> popular = new HashMap<>();
        Map<Integer, String> listOrder = new HashMap<>();
        
        for(Map<String, Object> row : currentList) {
            int pOrder = (Integer) row.get("POPULAR_ORDER");
            int lOrder = (Integer) row.get("LIST_ORDER");
            String prsId = (String) row.get("PRS_ID");
            
            if(pOrder > 0) popular.put(pOrder, prsId);
            if(lOrder > 0) listOrder.put(lOrder, prsId);
        }
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("popular", popular);
        settings.put("list", listOrder);
        result.put("currentSettings", settings);
        
        return result;
    }

    // [신규] 순서 저장 로직
    public void saveOrderSettings(UserVO user, Map<String, Object> payload) throws Exception {
        Map<String, Object> popular = (Map<String, Object>) payload.get("popular");
        Map<String, Object> list = (Map<String, Object>) payload.get("list");
        
        // 1. 초기화 (SM이면 1~4 및 popular 전체 0으로, LC면 해당 국가의 5~16만 0으로)
        if ("SM".equals(user.getPRS_AUTH())) {
            dao.update("super.resetSMOrder");
        } else if ("LC".equals(user.getPRS_AUTH())) {
            dao.update("super.resetLCOrder", user.getPRS_COUNTRY());
        }

        // 2. 새로운 값 Update (반복문으로 하나씩 업데이트)
        // 가로 스와이프 (SM 전용)
        if ("SM".equals(user.getPRS_AUTH()) && popular != null) {
            for (Map.Entry<String, Object> entry : popular.entrySet()) {
                Map<String, Object> param = new HashMap<>();
                param.put("PRS_ID", entry.getValue());
                param.put("POPULAR_ORDER", Integer.parseInt(entry.getKey()));
                dao.update("super.updatePopularOrder", param);
            }
        }
        
        // 세로 리스트 (SM, LC 공통)
        if (list != null) {
            for (Map.Entry<String, Object> entry : list.entrySet()) {
                Map<String, Object> param = new HashMap<>();
                param.put("PRS_ID", entry.getValue());
                param.put("LIST_ORDER", Integer.parseInt(entry.getKey()));
                dao.update("super.updateListOrder", param);
            }
        }
    }
    
 // 🌟 [신규] 목록 가져오기
    public List<Map<String, Object>> getAdminIpoList() throws Exception {
        return dao.selectList("super.selectAdminIpoList");
    }

    // 🌟 [신규] 상태 변경 및 승인 시 스타 자동 발급 로직
    public void processIpoStatus(Map<String, Object> param) throws Exception {
        String status = (String) param.get("status");
        
        // 1. 상태 업데이트 (pending -> approved 또는 rejected)
        dao.update("super.updateIpoStatus", param);

        // 2. 만약 '승인(approved)' 처리라면, 기존 스타 발급 시스템 호출!
        if ("approved".equals(status)) {
            // TODO: 기존에 구현해두신 "스타 추가(Insert)" 메서드를 여기서 호출합니다.
            // param 안에는 name, category, sns_link 등의 정보가 있으니, 
            // 이를 WH_PRESS 테이블 구조에 맞게 매핑해서 넣어주시면 됩니다.
            
            // 예시: 
            // Map<String, Object> starData = new HashMap<>();
            // starData.put("PRS_NAME", param.get("name"));
            // starData.put("IS_STAR", "Y");
            // ... 생략 ...
            // this.insertStar(starData); // (기존 로직 재활용)
            
            System.out.println("스타페이지 개설 승인 완료 및 스타 권한 부여: " + param.get("name"));
        }
    }
    
    // 🌟 [신규] 신고 목록 조회
    public List<Map<String, Object>> getReportList() throws Exception {
        return dao.selectList("super.selectReportList");
    }

    // 🌟 [신규] 글로벌 영구 차단 실행
    public void executeGlobalBlock(Map<String, Object> params) throws Exception {
        dao.insert("super.insertGlobalBlock", params);
    }
    
    public void insertAdminFeed(Map<String, Object> params) throws Exception {
        dao.insert("super.insertAdminFeed", params);
    }
    
    public void deleteAdminFeed(String realConId) throws Exception {
        dao.delete("super.deleteAdminFeed", realConId);
    }
    
    public void insertStarFeedMaster(Map<String, Object> params) throws Exception {
        // useGeneratedKeys="true" 설정으로 인해 쿼리 실행 후 params에 CON_ID가 담깁니다.
        dao.insert("star.insertContentMaster", params);
    }

    public void insertStarFeedMedia(Map<String, Object> params) throws Exception {
        dao.insert("star.insertContentMedia", params);
    }
    
    public int getTotalUnreadMessages() throws Exception {
        return dao.selectOne("super.selectTotalUnreadMessages");
    }

    public List<Map<String, Object>> getMessageThreads() throws Exception {
        return dao.selectList("super.selectMessageThreads");
    }

    // 🌟 [신규] 상단고정 글로벌 프로모션 정보 조회
    public Map<String, Object> getActivePromotion() throws Exception {
        Map<String, Object> promotion = dao.selectOne("superapp.selectActivePromotion");
        if (promotion == null) {
            promotion = new HashMap<>();
            promotion.put("PIN_LINK_TEXT1", "");
            promotion.put("PIN_LINK_URL1", "");
            promotion.put("PIN_LINK_TEXT2", "");
            promotion.put("PIN_LINK_URL2", "");
        }
        return promotion;
    }

    // 🌟 [신규] 상단고정 글로벌 프로모션 정보 등록/수정
    public void savePromotion(Map<String, Object> params) throws Exception {
        dao.insert("superapp.insertOrUpdatePromotion", params);
    }
    
    
}
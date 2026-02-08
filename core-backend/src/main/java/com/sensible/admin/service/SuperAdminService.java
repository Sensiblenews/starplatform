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
    
    /**
     * [수정] 대시보드 통계 (country가 null이면 전체, 있으면 해당 국가만)
     */
    public Map<String, Object> getDashboardStats(String country) throws Exception {
        Map<String, Object> stats = new HashMap<>();

        // 각 쿼리에 country 파라미터 전달
        stats.put("starCount", dao.selectOne("super.selectStarCount", country));
        stats.put("totalAdViews", dao.selectOne("super.selectTotalAdViews", country));
        stats.put("activeUserCount", dao.selectOne("super.selectActiveUserCount", country));
        stats.put("monthlyStats", dao.selectList("super.selectMonthlyAdStats", country));

        // 국가별 분포는 SM(총괄)만 보므로 필터 없이 조회
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
    
    // [수정] 인기 스타(Top 6) 토글 및 제한 체크
    public Map<String, Object> togglePopularStatus(Map<String, Object> params) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        String targetStatus = (String) params.get("TARGET_STATUS"); // 'Y' or 'N'
        String country = (String) params.get("PRS_COUNTRY");       // 국가 코드

        // 1. [방어 로직] 'Y'로 켜려는 경우, 이미 6명이 찼는지 검사
        if ("Y".equals(targetStatus)) {
            // super.xml의 카운트 쿼리 호출
            int currentCount = dao.selectOne("super.countPopularStars", country);
            
            if (currentCount >= 6) {
                // 6명 꽉 찼으면 즉시 실패 리턴
                result.put("status", "fail");
                result.put("msg", "인기 스타(Top 6)는 국가별 최대 6명까지만 지정 가능합니다.\n기존 스타를 해제한 후 다시 시도해주세요.");
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
    
    public List<Map<String, Object>> getRecentFeedList(String country) throws Exception {
        return dao.selectList("super.selectRecentFeedList", country);
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
}
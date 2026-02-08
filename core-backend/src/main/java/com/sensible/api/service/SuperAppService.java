package com.sensible.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import com.sensible.common.dao.DefaultDAO;

@Service("superAppService")
public class SuperAppService {

    @Resource(name = "DefaultDAO")
    private DefaultDAO dao;

    public Map<String, Object> getLobbyData(Map<String, Object> map) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        
        // 인기 스타 (가로)
        List<Map<String, Object>> popularStars = dao.selectList("superapp.selectPopularStars", map);
        // 전체 스타 (세로)
        List<Map<String, Object>> allStars = dao.selectList("superapp.selectAllStars", map);

        resultMap.put("popularStars", popularStars);
        resultMap.put("allStars", allStars);
        resultMap.put("result", "OK");
        return resultMap;
    }

    public Map<String, Object> getStarDetail(Map<String, Object> map) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        
        // 스타 기본 정보
        Map<String, Object> starInfo = dao.selectOne("superapp.selectStarDetail", map);
        
        if (starInfo != null) {
            // 스타의 갤러리 이미지들 (WH_CONTENT 재사용)
            List<Map<String, Object>> photos = dao.selectList("superapp.selectStarGallery", map);
            starInfo.put("photos", photos);
            
            resultMap.put("starInfo", starInfo);
            resultMap.put("result", "OK");
        } else {
            resultMap.put("result", "FAIL");
        }
        return resultMap;
    }

    public Map<String, Object> insertAdLog(Map<String, Object> map) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            // WH_AD_LOG 테이블에 INSERT
            dao.insert("superapp.insertAdLog", map);
            resultMap.put("result", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", "FAIL");
        }
        return resultMap;
    }
    
    public Map<String, Object> getFeedDetail(Map<String, Object> map) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        
        // 1. 게시글 본문 정보 가져오기
        Map<String, Object> content = dao.selectOne("superapp.selectContentDetail", map);
        
        if (content != null) {
            // 2. 미디어(사진/영상) 리스트 가져오기
            List<Map<String, Object>> medias = dao.selectList("superapp.selectContentMedias", map);
            
            // 3. 작성자(스타) 정보도 같이 내려주면 좋음 (프로필 사진 등)
            // (쿼리에서 조인해서 가져오는 게 정석이지만, 편의상 여기서 맵핑하거나 기존 정보 활용)
            
            resultMap.put("content", content);
            resultMap.put("medias", medias);
            resultMap.put("result", "OK");
        } else {
            resultMap.put("result", "FAIL");
            resultMap.put("msg", "삭제되거나 존재하지 않는 게시물입니다.");
        }
        return resultMap;
    }
}
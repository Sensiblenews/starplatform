package com.sensible.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sensible.api.service.MarketApiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MarketApiController {

    @Autowired
    private MarketApiService marketApiService; // (아래에서 만들 서비스 클래스)

    // 1. 스타페이지 개설 신청 API
    @RequestMapping(value = "/super/ipo/request", method = RequestMethod.POST)
    public Map<String, Object> requestIpo(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = new HashMap<>();
        try {
            marketApiService.insertIpoRequest(param);
            result.put("result", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", "FAIL");
            result.put("msg", "신청 접수 중 오류가 발생했습니다.");
        }
        return result;
    }

    // 2. 리스트 보드 조회 API
    @RequestMapping(value = "/super/ipo/list", method = RequestMethod.GET)
    public Map<String, Object> getIpoList(@RequestParam(defaultValue = "recent") String sortType) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("sortType", sortType);
            
            List<Map<String, Object>> list = marketApiService.getIpoList(param);
            result.put("result", "OK");
            result.put("list", list);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", "FAIL");
        }
        return result;
    }

    // 3. 기업 문의 제안 API
    @RequestMapping(value = "/super/purchase/request", method = RequestMethod.POST)
    public Map<String, Object> requestPurchase(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = new HashMap<>();
        try {
            marketApiService.insertPurchaseRequest(param);
            result.put("result", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", "FAIL");
            result.put("msg", "문의 접수 중 오류가 발생했습니다.");
        }
        return result;
    }
}
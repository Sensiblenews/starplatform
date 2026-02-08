package com.sensible.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sensible.api.service.SuperAppService;
import com.sensible.common.domain.CommandMap;

@Controller
public class SuperAppController {

	private static final Logger logger = LoggerFactory.getLogger(SuperAppController.class);

	@Resource(name = "superAppService")
	private SuperAppService superAppService;

	// 1. 로비 데이터 조회 (인기 스타 + 전체 리스트)
	@RequestMapping(value = "/api/super/lobby", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getLobbyData(CommandMap commandMap) throws Exception {
		return superAppService.getLobbyData(commandMap.getMap());
	}

	// 2. 스타 상세 정보 조회 (프로필 + 갤러리)
	@RequestMapping(value = "/api/super/star/{starId}", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getStarDetail(@PathVariable("starId") String starId, CommandMap commandMap)
			throws Exception {
		commandMap.put("starId", starId);
		return superAppService.getStarDetail(commandMap.getMap());
	}

	// 3. [핵심] 광고 시청 로그 기록 (돈 계산용)
	// 앱에서 광고를 본 후 이 API를 호출하면 장부에 기록됩니다.
	@RequestMapping(value = "/api/ad/log", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> recordAdLog(CommandMap commandMap) throws Exception {
		// 파라미터: MEM_ID(시청자), PRS_ID(스타), AD_TYPE(광고종류)
		logger.info("Ad Log Record: " + commandMap.getMap());
		return superAppService.insertAdLog(commandMap.getMap());
	}
	
	// 4. 피드 상세 조회 (게시글 + 미디어 리스트)
    @RequestMapping(value = "/api/super/feed/{conId}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getFeedDetail(@PathVariable("conId") String conId, CommandMap commandMap) throws Exception {
        commandMap.put("conId", conId);
        return superAppService.getFeedDetail(commandMap.getMap());
    }
}
package com.sensible.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import jbit.core.common.util.DateUtil;
import jbit.core.common.util.EgovFileScrty;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Service;

import com.sensible.admin.domain.UserVO;
import com.sensible.common.Constants;
import com.sensible.common.dao.DefaultDAO;
import com.sensible.common.util.SessionUtil;

@Service("adminService")
public class AdminService {
	
	@Resource(name="DefaultDAO")
    private DefaultDAO dao;


	public String adminChk(Map<String, Object> map) throws Exception {
		return dao.selectOne("admin.adminChk", map);
	}
	
	public List<Map<String, Object>> memberList(Map<String, Object> map) throws Exception {
		return dao.selectList("admin.memberList",map);
	}
	
	public Map<String, Object> memberEdit(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String memId = String.valueOf(map.get("MEM_ID"));
		Map<String, Object> mMap = dao.selectOne("member.selectMember", memId);
		List<Map<String, Object>> hList = dao.selectList("admin.memberStarHist", memId);
		List<Map<String, Object>> heartList = dao.selectList("admin.memberStarHeartHist", memId);
		resultMap.put("mMap", mMap);
		resultMap.put("hList", hList);
		resultMap.put("heartList", heartList);
		return resultMap;
	}
	
	public void memberUpdate(Map<String, Object> map) throws Exception {
		String memAgree = String.valueOf(map.get("MEM_AGREEMENT"));
		String memUseYn = String.valueOf(map.get("MEM_USE_YN"));
				
		if (memAgree != null && memAgree.equals("on")) {
			map.put("MEM_AGREEMENT", "1");
		} else {
			map.put("MEM_AGREEMENT", "0");
		}
		
		if (memUseYn != null && memUseYn.equals("on")) {
			map.put("MEM_USE_YN", "1");
		} else {
			map.put("MEM_USE_YN", "0");
		}
		
		dao.update("member.memberUpdate", map);
	}
	
	public List<Map<String, Object>> pressList(Map<String, Object> map) throws Exception {
		return dao.selectList("admin.pressList", map);
	}
	
	public Map<String, Object> pressEdit(Map<String, Object> map) throws Exception {
		return dao.selectOne("admin.selectPress", map);
	}
	
	public void pressUpdate(Map<String, Object> map) throws Exception {
		dao.update("admin.pressUpdate", map);
	}
	
	/**
	 * app 신청하기
	 * @param map
	 * @throws Exception
	 */
	public void updateAppApply(Map<String, Object> map) throws Exception {		
		dao.update("admin.updateAppApply", map);
	}
	
	
	/**
	 * app 발급하기
	 * @param map
	 * @throws Exception
	 */
	public void updateAppkey(Map<String, Object> map) throws Exception {
		map.put("APP_AUTH_KEY", EgovFileScrty.encryptPassword(String.valueOf(map.get("PRS_ID"))));
		dao.update("admin.updateAppkey", map);
	}
	
	public void pressRegit(Map<String, Object> map) throws Exception {
		
		//map.put("APP_ID", EgovFileScrty.encryptPassword(String.valueOf(map.get("PRS_ID"))));
		map.put("APP_ID", map.get("PRS_ID"));
		dao.insert("admin.pressRegit", map);
	}
	
	public List<Map<String, Object>> cotentList(Map<String, Object> map) throws Exception {					
		return dao.selectMapPageList("admin.cotentList", map);
	}
	
	public Map<String, Object> categoryList(Map<String, Object> map) throws Exception {
		return dao.selectOne("admin.categoryList", map);
	}
	
	
	public int contentRegit(HttpServletRequest request, Map<String, Object> map) throws Exception {
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		
		if(!Constants.AUTH_SM.equals(userVO.getPRS_AUTH()))
		{
			map.put("PRS_ID", userVO.getPRS_ID());	
			map.put("APP_ID", userVO.getAPP_ID());
		}else{
			map.put("APP_ID", dao.selectOne("admin.selectAppPress",map));
		}
		
		Map<String, Object> cMap = dao.selectOne("content.contentNextConId");
		Long conId = (Long) cMap.get("CON_ID");
		map.put("CON_ID", conId);

		String conMetaYn = (String) map.get("CON_META_YN");
		String conUseYn = (String) map.get("CON_USE_YN");
		String conTopYn = (String) map.get("CON_TOP_YN");
		String conCtgyMod = (String) map.get("CON_CATEGORY_MOD");
		
		if (conMetaYn != null && conMetaYn.equals("on")) {
			map.put("CON_META_YN", "1");
		} else {
			map.put("CON_META_YN", "0");
		}
		
		if (conUseYn != null && conUseYn.equals("on")) {
			map.put("CON_USE_YN", "1");
		} else {
			map.put("CON_USE_YN", "0");
		}
		
		if (conTopYn != null && conTopYn.equals("on")) {
			map.put("CON_TOP_YN", "1");
			map.put("CON_TOP_DATE", DateUtil.getSysdate());
		} else {
			map.put("CON_TOP_YN", "0");
			map.put("CON_TOP_DATE", null);
		}
		
		if (conCtgyMod != null && conCtgyMod.length() > 0) {
			map.put("CON_CATEGORY", conCtgyMod);
		}
				
		
		dao.insert("content.contentRegit", map);
		return conId.intValue();
	}
	
	public void categoryUpdate(HttpServletRequest request, Map<String, Object> map) throws Exception {
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		map.put("APP_ID", userVO.getAPP_ID());
		
		String category0 = (String) map.get("CATEGORY_0");
		String category1 = (String) map.get("CATEGORY_1");
		String category8 = (String) map.get("CATEGORY_8");
		String category9 = (String) map.get("CATEGORY_9");
		
		StringBuilder sb = new StringBuilder();

		sb.append(category0);
		sb.append(",");
		sb.append(category1);
		sb.append(",");
		sb.append(category8);
		sb.append(",");
		sb.append(category9);

		String result = sb.toString();
		
		map.put("CATEGORY_TAG", result);
		
		
		dao.insert("content.updateCategory", map);
	}
		
	
	/**
	 * 컨텐츠 상세
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> getContent(Map<String, Object> map) throws Exception {
		
		Map<String, Object> cMap = dao.selectOne("admin.getContents", map);
				
		// html escape
		String str = (String) cMap.get("CON_TITLE");
		String strHtml = StringEscapeUtils.escapeHtml4(str);
		cMap.put("CON_TITLE", strHtml);
	
		return cMap;
	}
	
	public void contentUpdate(HttpServletRequest request, Map<String, Object> map) throws Exception {
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);		
		map.put("PRS_AUTH", userVO.getPRS_AUTH());
		
		if(!Constants.AUTH_SM.equals(userVO.getPRS_AUTH()))
		{
			map.put("PRS_ID", userVO.getPRS_ID());	
			map.put("APP_ID", userVO.getAPP_ID());
		}else{
			map.put("APP_ID", dao.selectOne("admin.selectAppPress",map));
		}
		
		
		String conMetaYn = (String) map.get("CON_META_YN");
		String conUseYn = (String) map.get("CON_USE_YN");
		String conTopYn = (String) map.get("CON_TOP_YN");
		String conCtgyMod = (String) map.get("CON_CATEGORY_MOD");
		
		if (conMetaYn != null && conMetaYn.equals("on")) {
			map.put("CON_META_YN", "1");
		} else {
			map.put("CON_META_YN", "0");
		}
		
		if (conUseYn != null && conUseYn.equals("on")) {
			map.put("CON_USE_YN", "1");
		} else {
			map.put("CON_USE_YN", "0");
		}
		
		if (conTopYn != null && conTopYn.equals("on")) {
			map.put("CON_TOP_YN", "1");
			map.put("CON_TOP_DATE", DateUtil.getSysdate());
		} else {
			map.put("CON_TOP_YN", "0");
			map.put("CON_TOP_DATE", null);
		}
		
		if (conCtgyMod != null && conCtgyMod.length() > 0) {
			map.put("CON_CATEGORY", conCtgyMod);
		}
		
		dao.update("content.contentUpdate", map);
	}
	
	public void contentSort(Map<String, Object> map) throws Exception {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		String conType = (String) map.get("CON_TYPE");
		String type = (String) map.get("type");
		String now = (String) map.get("now");
		
		paramMap.put("CON_TYPE", conType);
		paramMap.put("now", now);
		
		if (type.equals("prevUp")) {
			Map<String, Object> prevMap = dao.selectOne("content.prevContent", paramMap);
			if (prevMap != null && prevMap.get("CON_ID") != null) {
				String conId = String.valueOf(prevMap.get("CON_ID"));
				String conPriot = String.valueOf(prevMap.get("CON_PRIOT"));
				

				paramMap.put("object", now);
				paramMap.put("change", conPriot);
				dao.update("content.updateContentPriot", paramMap);
				
				paramMap.put("conId", conId);
				paramMap.put("change", now);
				dao.update("content.updateContentPriotConId", paramMap);
			}
		} else if (type.equals("moveUp")) {
			String prev = (String) map.get("prev");
			
			Map<String, Object> prevMap = dao.selectOne("content.selectContentPriot", paramMap);
			if (prevMap != null && prevMap.get("CON_ID") != null) {
				String conId = String.valueOf(prevMap.get("CON_ID"));
				String conPriot = String.valueOf(prevMap.get("CON_PRIOT"));
				
				paramMap.put("object", prev);
				paramMap.put("change", now);
				dao.update("content.updateContentPriot", paramMap);
				
				paramMap.put("conId", conId);
				paramMap.put("change", prev);
				dao.update("content.updateContentPriotConId", paramMap);
			}
		} else if (type.equals("moveDown")) {
			String next = (String) map.get("next");
			
			Map<String, Object> prevMap = dao.selectOne("content.selectContentPriot", paramMap);
			if (prevMap != null && prevMap.get("CON_ID") != null) {
				String conId = String.valueOf(prevMap.get("CON_ID"));
				String conPriot = String.valueOf(prevMap.get("CON_PRIOT"));
				

				paramMap.put("object", next);
				paramMap.put("change", now);
				dao.update("content.updateContentPriot", paramMap);
				
				paramMap.put("conId", conId);
				paramMap.put("change", next);
				dao.update("content.updateContentPriotConId", paramMap);
			}
		} else if (type.equals("nextDown")) {
			Map<String, Object> nextMap = dao.selectOne("content.nextContent", paramMap);
			if (nextMap != null && nextMap.get("CON_ID") != null) {
				String conId = String.valueOf(nextMap.get("CON_ID"));
				String conPriot = String.valueOf(nextMap.get("CON_PRIOT"));
				
				paramMap.put("object", now);
				paramMap.put("change", conPriot);
				dao.update("content.updateContentPriot", paramMap);
				
				paramMap.put("conId", conId);
				paramMap.put("change", now);
				dao.update("content.updateContentPriotConId", paramMap);
			}
		} 
	}
	
	
	
	public List<Map<String, Object>> commentEdit(Map<String, Object> map) throws Exception {
		// comments
		List<Map<String, Object>> cmMap = dao.selectList("admin.getComments", map);
		return cmMap;
	}
	
	public void commentUpdate(Map<String, Object> map) throws Exception {
		dao.update("admin.commentUpdate", map);
	}
	
	public void contentImgUpdate(Map<String, Object> map) throws Exception {
		dao.update("admin.contentImgUpdate", map);
	}
	
	public void contentVideoUpdate(Map<String, Object> map) throws Exception {
		dao.update("admin.contentVideoUpdate", map);
	}
	
	public void contentImgSubInsert(Map<String, Object> map) throws Exception {
		dao.insert("admin.contentImgSubInsert", map);
	}
	
	public void contentImgSubUpdate(Map<String, Object> map) throws Exception {
		dao.update("admin.contentImgSubUpdate", map);
	}
	
	public void contentImgSubUrlUpdate(Map<String, Object> map) throws Exception {
		dao.update("admin.contentImgSubUrlUpdate", map);
	}
	

	
	public int insertSettingSave(HttpServletRequest request, Map<String, Object> map) throws Exception {
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		
		if(!Constants.AUTH_SM.equals(userVO.getPRS_AUTH()))
		{
			map.put("PRS_ID", userVO.getPRS_ID());	
			map.put("APP_ID", userVO.getAPP_ID());
		}else{
			map.put("APP_ID", dao.selectOne("admin.selectAppPress",map));
		}
		
		Map<String, Object> cMap = dao.selectOne("content.contentNextConId");
		Long conId = (Long) cMap.get("CON_ID");
		map.put("CON_ID", conId);
		
		String conUseYn = (String) map.get("CON_USE_YN");
		
		if (conUseYn != null && conUseYn.equals("on")) {
			map.put("CON_USE_YN", "1");
		} else {
			map.put("CON_USE_YN", "0");
		}
		
		dao.insert("content.insertAppSetting", map);
		return conId.intValue();
	}
	
		
	/**
	 * 앱 수정 저장
	 * @param request
	 * @param map
	 * @throws Exception
	 */
	public void updateSettingSave(HttpServletRequest request, Map<String, Object> map) throws Exception {
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);		
		
		if(!Constants.AUTH_SM.equals(userVO.getPRS_AUTH()))
		{
			map.put("PRS_ID", userVO.getPRS_ID());	
			map.put("APP_ID", userVO.getAPP_ID());
		}else{
			map.put("APP_ID", dao.selectOne("admin.selectAppPress",map));
		}
		
		String conUseYn = (String) map.get("CON_USE_YN");
		
		if (conUseYn != null && conUseYn.equals("on")) {
			map.put("CON_USE_YN", "1");
		} else {
			map.put("CON_USE_YN", "0");
		}
		
		dao.update("content.updateAppSetting", map);

	}
	
	public void policyUpdate(Map<String, Object> map) throws Exception {
		dao.update("content.policyUpdate", map);
	}
	
	public String appUrl(String type) throws Exception {
		return dao.selectOne("admin.appUrl", type);
	}
	
	public Map<String, Object> contentTotalPrice(String conId) throws Exception {
		return dao.selectOne("content.contentTotalPrice", conId);
	}
	
	public Map<String, Object> contentTotalHeart(String conId) throws Exception {
		return dao.selectOne("content.contentTotalHeart", conId);
	}
	
	public List<Map<String, Object>> selectPressList(Map<String, Object> map) throws Exception {
		return dao.selectList("admin.selectPressList", map);
	}
	
	public List<Map<String, Object>> selectAppList(Map<String, Object> map) throws Exception {
		return dao.selectList("admin.selectAppList", map);
	}
	
	public Map<String, Object> pressStarHst(String prsId) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<String, Object> priceMap = dao.selectOne("admin.totalPrice", prsId);
		Map<String, Object> heartMap = dao.selectOne("admin.totalHeart", prsId);
		resultMap.put("priceMap", priceMap);
		resultMap.put("heartMap", heartMap);
		return resultMap;
	}
	
	public int getFreeStartHeart(String prsId)throws Exception{
		Map<String, Object> compSMap = dao.selectOne("admin.totalPrice", prsId);
		Map<String, Object> compHMap = dao.selectOne("admin.totalHeart", prsId);
		int freeStar = Integer.parseInt(String.valueOf(compSMap.get("PRICE")));
		int freeHeart = Integer.parseInt(String.valueOf(compHMap.get("HEART")));
					
		int compStar = freeStar + freeHeart;	
		
		return compStar;
	}
	
	public List<Map<String, Object>> eventList(HttpServletRequest request, Map<String, Object> map) throws Exception {
		
		return dao.selectMapPageList("admin.eventList", map);
	}
	
	public Map<String, Object> eventEdit(Map<String, Object> map) throws Exception {
		return dao.selectOne("admin.selectEvent", map);
	}
	
	public void eventUpdate(Map<String, Object> map) throws Exception {
		String chgYn = (String) map.get("EVT_CHG_YN");
		String useYn = (String) map.get("EVT_USE_YN");
		
		if (chgYn != null && chgYn.equals("on")) {
			map.put("EVT_CHG_YN", "1");
		} else {
			map.put("EVT_CHG_YN", "0");
		}
		
		if (useYn != null && useYn.equals("on")) {
			map.put("EVT_USE_YN", "1");
		} else {
			map.put("EVT_USE_YN", "0");
		}
		
		dao.update("admin.eventUpdate", map);
	}
	
	public void eventRegit(HttpServletRequest request, Map<String, Object> map) throws Exception {
		
		UserVO userVO = SessionUtil.getSessionUserInfo(request);
		
		if(!Constants.AUTH_SM.equals(userVO.getPRS_AUTH()))
		{
			map.put("PRS_ID", userVO.getPRS_ID());	
			map.put("APP_ID", userVO.getAPP_ID());
		}else{
			map.put("APP_ID", dao.selectOne("admin.selectAppPress",map));
		}
		
		Map<String, Object> eMap = dao.selectOne("admin.eventNextPrsId");
		String evtId = String.valueOf(eMap.get("EVT_ID"));
		
		String chgYn = (String) map.get("EVT_CHG_YN");
		String useYn = (String) map.get("EVT_USE_YN");
		
		if (chgYn != null && chgYn.equals("on")) {
			map.put("EVT_CHG_YN", "1");
		} else {
			map.put("EVT_CHG_YN", "0");
		}
		
		if (useYn != null && useYn.equals("on")) {
			map.put("EVT_USE_YN", "1");
		} else {
			map.put("EVT_USE_YN", "0");
		}
		
		map.put("EVT_ID", evtId);
		dao.insert("admin.eventRegit", map);
	}
	
	
	/**
	 * 앱 관리
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> selectApplyList(Map<String, Object> map) throws Exception {

		return dao.selectList("admin.selectApplyList", map);
	}
	
	
	/**
	 * 업체별 회원 현황
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> selectCompMemberStatus(Map<String, Object> map) throws Exception {
		return dao.selectOne("admin.selectCompMemberStatus", map);
	}
	
	/**
	 * 업체별 별 현황
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> selectCompStarStatus(Map<String, Object> map) throws Exception {
		return dao.selectOne("admin.selectCompStarStatus", map);
	}
	
	/**
	 * 업체별 하트 현황
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> selectCompHeartStatus(Map<String, Object> map) throws Exception {
		return dao.selectOne("admin.selectCompHeartStatus", map);
	}
	
	public List<Map<String, Object>> getGraphDataDaily(Map<String, Object> map) throws Exception {
		return dao.selectList("admin.getGraphDataDaily", map);
	}
	
	public List<Map<String, Object>> getGraphDataMonthly(Map<String, Object> map) throws Exception {
		return dao.selectList("admin.getGraphDataMonthly", map);
	}
}
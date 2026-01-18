package com.sensible.api.service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import com.sensible.common.util.AWTUtil;
import com.sensible.common.util.PictureToBufferedImageConverter;
import jbit.core.common.util.DateUtil;
import org.apache.commons.codec.binary.Base64;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.springframework.stereotype.Service;

import com.owlike.genson.convert.DefaultConverters.PrimitiveConverterFactory.intConverter;
import com.sensible.common.Constants;
import com.sensible.common.dao.DefaultDAO;

@Service("contentService")
public class ContentService {
	
	@Resource(name="DefaultDAO")
    private DefaultDAO dao;
	
	

	public Map<String, Object> content(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<String, Object> cMap = dao.selectOne("content.getContents", map);
		int price = (int) cMap.get("CON_PRICE");		
		String memId = (String) map.get("MEM_ID");
		String writeId = String.valueOf(cMap.get("PRS_ID"));//작성자
		map.put("PRS_ID", writeId);
		
		Map<String, Object> mMap = dao.selectOne("member.selectMember", memId);
		Map<String, Object> hMap = dao.selectOne("content.contentHistoryChk", map);

		//System.out.println("[memID] ================== "+memId);
		//System.out.println("[price] ================== "+price);
		
		// 유료 무료 체크
		if (price == 0) {
			
			mMap.put("isSuccess", 1);
			cMap.put("CON_EXPD", 0);
			// 기존 봤던 내역 있으면 쌓지 않음
			if (hMap == null || hMap.get("MEM_ID") == null) {
				dao.insert("content.insertContentHistory", map);
			}
		} else if(price > 0) {
			
			// 기 결제 체크. 만료되지 않은 결제 내역 체크.
			Map<String, Object> priMap = dao.selectOne("content.contentPriceChk", map);
			
			
			if (priMap == null || priMap.get("MEM_ID") == null) {				
				int star = (int) mMap.get("MEM_STAR");
				int starChg = (int) mMap.get("MEM_STAR_CHG");
				
				int totalStar = star + starChg; //보유별수
				
				if (totalStar < price) {
					mMap.put("isSuccess", 0);
					// 기존 봤던 내역 있으면 쌓지 않음
					if (hMap == null || hMap.get("MEM_ID") == null) {
						dao.insert("content.insertContentHistory", map);
					}
				} else {
					int resultPrice = 0;
					int resultPriceChg = 0;
					int resultStar = 0;
					int resultStarChg = 0;
					
					if (star < price) {
						resultPrice = star;
						resultPriceChg = price - resultPrice;
						resultStar = 0;
						resultStarChg = starChg - (price - star);
					} else {
						resultPrice = price;
						resultStar = star - price;
						resultStarChg = starChg;
					}
										
					map.put("CON_PRICE", resultPrice);
					map.put("CON_PRICE_CHG", resultPriceChg);
					map.put("MEM_STAR", resultStar);
					map.put("MEM_STAR_CHG", resultStarChg);
					
					dao.insert("content.contentPay", map);
					dao.update("member.minusMemberStar", map);
					
					Map<String, Object> payAfterMap = dao.selectOne("member.selectMember", memId);
					int memStar = (int) payAfterMap.get("MEM_STAR");
					int memStarChg = (int) payAfterMap.get("MEM_STAR_CHG");
					
					mMap.put("MEM_STAR", memStar);
					mMap.put("MEM_STAR_CHG", memStarChg);
					
					mMap.put("isSuccess", 1);
					cMap.put("CON_EXPD", 30);
					if (hMap == null || hMap.get("MEM_ID") == null) {
						dao.insert("content.insertContentHistory", map);
					}
				}
			}
			//이미 결제되었고 유효기간이 남아있다면 패스.
			else 
			{
				//System.out.println("=================이미 결재 ==================");
				mMap.put("isSuccess", 1);
				Map<String, Object> chkMap = dao.selectOne("content.contentExpdChk", map);
				cMap.put("CON_EXPD", chkMap.get("CON_EXPD"));
				if (hMap == null || hMap.get("MEM_ID") == null) {
					dao.insert("content.insertContentHistory", map);
				}
			}
		}
		//-가격일시 사용자에게 별을 돌려준다.
		else if(price < 0)
		{
			/**
			 * 1.해당 컨텐츠의 회원사가 무료별,무료하트가 남아 있는지 체크
			 * 2.남아있으면 지급 없으면 해당 컨텐트의 가격을 0로 업데이트
			 * 3.이미 지급받은 컨텐트라면 유효기간동안 지급하지 않는다.
			 */
			
			//해당 컨텐츠의 회원사의 무료별,무료하트 잔여 여부
			
			Map<String, Object> compSMap = dao.selectOne("admin.totalPrice", (String) cMap.get("PRS_ID"));
			Map<String, Object> compHMap = dao.selectOne("admin.totalHeart", (String) cMap.get("PRS_ID"));
			int freeStar = Integer.parseInt(String.valueOf(compSMap.get("PRICE")));
			int freeHeart = Integer.parseInt(String.valueOf(compHMap.get("HEART")));
						
			int compStar = freeStar + freeHeart;		
			
			
			//price값이 마이너스임
			int payPrice = Math.abs(price);
			
			//지급할 무료별하트가 있다면.
			if(compStar >= payPrice )
			{
				// 기 결제 체크. 만료되지 않은 결제 내역 체크.
				Map<String, Object> priMap = dao.selectOne("content.contentPriceChk", map);
				
				if (priMap == null || priMap.get("MEM_ID") == null) 
				{

					//멤버 플러스, price 마이너스, heart 마이너스 값
					int resultSPrice = 0;	//wh_price
					int resultSPriceChg = 0;
					int resultHPrice = 0;	//wh_heart
					int resultHPriceChg = 0;
					int resultMStar = payPrice;						
					
					
					//별먼저 차감
					if((freeStar - payPrice) >= 0)
					{
						
						resultSPrice = price;
						
						map.put("CON_PRICE", resultSPrice);
						map.put("CON_PRICE_CHG", resultSPriceChg);
						dao.insert("content.contentPay", map);
						
					}else{
						
						//무료별이 남아있으면 별차감후 하트도 차감.
						if(freeStar > 0)
						{
							
							resultSPrice = (-freeStar); //마이너스 값으로 wh_price에 등록하기위함.
							resultHPrice = (-(payPrice - freeStar));	
							
							map.put("CON_PRICE", resultSPrice);
							map.put("CON_PRICE_CHG", resultSPriceChg);
							dao.insert("content.contentPay", map);
							
							map.put("CON_HEART", resultHPrice);
							map.put("CON_HEART_CHG", resultHPriceChg);
							dao.insert("content.contentHeart", map);
							
						}else{
							
							resultHPrice = price;
							
							
							//기결제 체크를 위해 0값으로 셋팅.
							map.put("CON_PRICE", 0);
							map.put("CON_PRICE_CHG", 0);
							dao.insert("content.contentPay", map);
							
							map.put("CON_HEART", resultHPrice);
							map.put("CON_HEART_CHG", resultHPriceChg);
							dao.insert("content.contentHeart", map);
						}
					}
					
					map.put("STAR", resultMStar);
					dao.update("member.plusMemberStar", map);
					
					
					//리턴값 셋팅
					Map<String, Object> payAfterMap = dao.selectOne("member.selectMember", memId);
					int memStar = (int) payAfterMap.get("MEM_STAR");
					int memStarChg = (int) payAfterMap.get("MEM_STAR_CHG");
					
					mMap.put("MEM_STAR", memStar);
					mMap.put("MEM_STAR_CHG", memStarChg);
					
					mMap.put("isSuccess", 1);
					cMap.put("CON_EXPD", 30);
					if (hMap == null || hMap.get("MEM_ID") == null) {
						dao.insert("content.insertContentHistory", map);
					}
					
				}
				else
				{
					//이미 지급받은 컨텐츠
					mMap.put("isSuccess", 1);
					Map<String, Object> chkMap = dao.selectOne("content.contentExpdChk", map);
					cMap.put("CON_EXPD", chkMap.get("CON_EXPD"));
					if (hMap == null || hMap.get("MEM_ID") == null) {
						dao.insert("content.insertContentHistory", map);
					}
				}
				
			}
			else
			{
				//업체의 무료별,하트가 없을시 해당 컨텐츠 가격을 0으로 업데이트
				dao.update("content.updateContentsPrice", map);
				
				mMap.put("isSuccess", 1);
				cMap.put("CON_EXPD", 0);
				// 기존 봤던 내역 있으면 쌓지 않음
				if (hMap == null || hMap.get("MEM_ID") == null) {
					dao.insert("content.insertContentHistory", map);
				}
			}
			
		}

		// comments
		List<Map<String, Object>> cmMap = dao.selectList("content.getComments", map);
		for (int i = 0; i < cmMap.size(); i++) {
			String cmMemId = (String) cmMap.get(i).get("MEM_ID");
			Map<String, Object> cmMemIdMap = dao.selectOne("member.selectMember", cmMemId);
			cmMap.get(i).put("User", cmMemIdMap);
		}
		cMap.put("comments", cmMap);

		// images
		List<Map<String, Object>> imgMap = dao.selectList("content.getImages", map);
		cMap.put("images", imgMap);
		
		// 별 소모, 하트 수익 내역
		String conId = String.valueOf(cMap.get("CON_ID"));
		String prsId = String.valueOf(cMap.get("PRS_ID"));
		Map<String, Object> parammMap = new HashMap<String, Object>();
		parammMap.put("PRS_ID", prsId);
		Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap); 
		cMap.put("press", pMap);
		
		// 기자의 게시글이 아닐경우, 자신 게시물 아닐 경우에만 조회수처리
		if(pMap == null && !memId.equals(prsId)){
			dao.insert("content.view", map);
		}
		
		Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
		Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
		
		String pPriceStr = String.valueOf(priceMap.get("PRICE"));
		String pPriceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
		String pHeartStr = String.valueOf(heartMap.get("HEART"));
		String pHeartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
		
		int pPrice = Integer.parseInt(pPriceStr);
		int pPriceChg = Integer.parseInt(pPriceChgStr);
		int pHeart = Integer.parseInt(pHeartStr);
		int pHeartChg = Integer.parseInt(pHeartChgStr);
		
		cMap.put("PRICE", pPrice);
		cMap.put("PRICE_CHG", pPriceChg);
		cMap.put("HEART", pHeart);
		cMap.put("HEART_CHG", pHeartChg);

		cMap.put("PRS_ID", prsId);		
		
		Map<String, Object> prsMemberMap = dao.selectOne("member.selectMember", prsId);
		if(prsMemberMap != null){
			if(prsMemberMap.containsKey("MEM_NAME")){
				cMap.put("MEM_NAME", prsMemberMap.get("MEM_NAME").toString());
			}
			
			if(prsMemberMap.containsKey("MEM_PICTURE")){
				cMap.put("MEM_PICTURE", prsMemberMap.get("MEM_PICTURE").toString());
			}
		}

		resultMap.put("result", mMap);
		resultMap.put("contents", cMap);
				
		return resultMap;
	}
	
	public Map<String, Object> heart(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		String memId = (String) map.get("MEM_ID");
		Map<String, Object> mMap = dao.selectOne("member.selectMember", memId);
		
		String conId = String.valueOf(map.get("CON_ID"));
		String heartStr = String.valueOf(map.get("HEART"));
		int heart = Integer.parseInt(heartStr);
		
		int star = (int) mMap.get("MEM_STAR");
		int starChg = (int) mMap.get("MEM_STAR_CHG");
		
		int resultPrice = 0;
		int resultPriceChg = 0;
		int resultStar = 0;
		int resultStarChg = 0;
		
		if (star < heart) {
			resultPrice = star;
			resultPriceChg = heart - resultPrice;
			resultStar = 0;
			resultStarChg = starChg - (heart - star);
		} else {
			resultPrice = heart;
			resultStar = star - heart;
			resultStarChg = starChg;
		}
		
		map.put("CON_HEART", resultPrice);
		map.put("CON_HEART_CHG", resultPriceChg);
		map.put("MEM_STAR", resultStar);
		map.put("MEM_STAR_CHG", resultStarChg);
		
		dao.insert("content.contentHeart", map);
		dao.update("member.minusMemberStar", map);
		
		Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
		
		String resultHeartStr = String.valueOf(heartMap.get("HEART"));
		String resultHeartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
		int resultHeart = Integer.parseInt(resultHeartStr);
		int resultHeartChg = Integer.parseInt(resultHeartChgStr);
		
		resultMap.put("HEART", resultHeart);
		resultMap.put("HEART_CHG", resultHeartChg);
			
		Map<String, Object> payAfterMap = dao.selectOne("member.selectMember", memId);
		int memStar = (int) payAfterMap.get("MEM_STAR");
		int memStarChg = (int) payAfterMap.get("MEM_STAR_CHG");
		
		resultMap.put("MEM_STAR", memStar);
		resultMap.put("MEM_STAR_CHG", memStarChg);
		
		return resultMap;
	}
	
	public Map<String, Object> heartSender(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		List<Map<String, Object>> mcMap = dao.selectList("content.contentHeartSender", map);
		resultMap.put("HEART_SENDER", mcMap);
		return resultMap;
	}
	
	
	/**
	 * 몬스터 1
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> monsterContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "1");
		map.put("PRS_TYPE", "2"); //학원출판 
		map.put("SORT_ITEM", "CON_PRIOT");
		map.put("SORT_ORDER", "DESC");
		List<Map<String, Object>> mcMap = dao.selectList("content.mainContents", map);
		
		for (int i = 0; i < mcMap.size(); i++) {
			String conId = String.valueOf(mcMap.get(i).get("CON_ID"));
			String prsId = String.valueOf(mcMap.get(i).get("PRS_ID"));
			Map<String, Object> parammMap = new HashMap<String, Object>();
			parammMap.put("PRS_ID", prsId);
			Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap);
			mcMap.get(i).put("press", pMap);
			
			Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
			Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
			
			String priceStr = String.valueOf(priceMap.get("PRICE"));
			String priceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
			String heartStr = String.valueOf(heartMap.get("HEART"));
			String heartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
			
			int price = Integer.parseInt(priceStr);
			int priceChg = Integer.parseInt(priceChgStr);
			int heart = Integer.parseInt(heartStr);
			int heartChg = Integer.parseInt(heartChgStr);
			
			mcMap.get(i).put("PRICE", price);
			mcMap.get(i).put("PRICE_CHG", priceChg);
			mcMap.get(i).put("HEART", heart);
			mcMap.get(i).put("HEART_CHG", heartChg);
		}
		
		resultMap.put("contents", mcMap);
		return resultMap;
	}
	
	/**
	 * 요정 목록
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> fairyContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "0");
		map.put("PRS_TYPE", "1"); //언론사 요정
		map.put("SORT_ITEM", "CON_PRIOT");
		map.put("SORT_ORDER", "DESC");
		List<Map<String, Object>> fcMap = dao.selectList("content.mainContents", map);
		
		for (int i = 0; i < fcMap.size(); i++) {
			String conId = String.valueOf(fcMap.get(i).get("CON_ID"));
			String prsId = String.valueOf(fcMap.get(i).get("PRS_ID"));
			Map<String, Object> parammMap = new HashMap<String, Object>();
			parammMap.put("PRS_ID", prsId);
			Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap);
			fcMap.get(i).put("press", pMap);
			
			Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
			Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
			
			String priceStr = String.valueOf(priceMap.get("PRICE"));
			String priceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
			String heartStr = String.valueOf(heartMap.get("HEART"));
			String heartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
			
			int price = Integer.parseInt(priceStr);
			int priceChg = Integer.parseInt(priceChgStr);
			int heart = Integer.parseInt(heartStr);
			int heartChg = Integer.parseInt(heartChgStr);
			
			fcMap.get(i).put("PRICE", price);
			fcMap.get(i).put("PRICE_CHG", priceChg);
			fcMap.get(i).put("HEART", heart);
			fcMap.get(i).put("HEART_CHG", heartChg);
		}
		
		resultMap.put("contents", fcMap);
		return resultMap;
	}

	
	/**
	 * 마녀
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> witchContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "8");
		map.put("PRS_TYPE", "3");
		map.put("SORT_ITEM", "CON_PRIOT");
		map.put("SORT_ORDER", "DESC");
		List<Map<String, Object>> wcMap = dao.selectList("content.mainContents", map);
		
		for (int i = 0; i < wcMap.size(); i++) {
			String conId = String.valueOf(wcMap.get(i).get("CON_ID"));
			String prsId = String.valueOf(wcMap.get(i).get("PRS_ID"));
			Map<String, Object> parammMap = new HashMap<String, Object>();
			parammMap.put("PRS_ID", prsId);
			Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap);
			wcMap.get(i).put("press", pMap);
			
			Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
			Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
			
			String priceStr = String.valueOf(priceMap.get("PRICE"));
			String priceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
			String heartStr = String.valueOf(heartMap.get("HEART"));
			String heartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
			
			int price = Integer.parseInt(priceStr);
			int priceChg = Integer.parseInt(priceChgStr);
			int heart = Integer.parseInt(heartStr);
			int heartChg = Integer.parseInt(heartChgStr);
			
			wcMap.get(i).put("PRICE", price);
			wcMap.get(i).put("PRICE_CHG", priceChg);
			wcMap.get(i).put("HEART", heart);
			wcMap.get(i).put("HEART_CHG", heartChg);
		}
		
		resultMap.put("contents", wcMap);
		return resultMap;
	}
	
	/**
	 * 마녀 2 (Exclusive)
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> exclusiveContents(Map<String, Object> map) throws Exception {
	    Map<String, Object> resultMap = new HashMap<>();
	    
	    // 1. 파라미터 설정 (이전 witchContents 참고)
	    map.put("CON_TYPE", "8"); // Exclusive 전용 타입 (필요시 변경)
	    // 페이징 파라미터 (offset, limit)은 commandMap에 이미 들어있다고 가정
	    
	    // 2. 작성자 목록 조회 (페이지 단위)
	    List<Map<String, Object>> authorList = dao.selectList("content.selectExclusiveAuthors", map);
	    List<Map<String, Object>> resultList = new ArrayList<>();

	    for (Map<String, Object> author : authorList) {
	        String prsId = String.valueOf(author.get("PRS_ID"));
	        String myMemId = String.valueOf(map.get("MEM_ID"));
	        
	        // 3. 작성자 기본 정보 조회 (기존 admin.selectPress 활용)
	        // 만약 일반 유저도 작성 가능하다면 wh_member 조회 로직도 필요 (mainContents 로직 참고)
	        Map<String, Object> paramMap = new HashMap<>();
	        paramMap.put("PRS_ID", prsId);
	        
	        Map<String, Object> subParam = new HashMap<>();
	        subParam.put("MEM_ID", myMemId);
	        subParam.put("TARGET_ID", prsId);
	        
	        // 작성자 정보 가져오기 (언론사/유저 분기 로직은 기존 mainContents 참조하여 통합)
	        Map<String, Object> pressInfo = dao.selectOne("admin.selectPress", paramMap);
	        
	        // 만약 pressInfo가 null이면 member 테이블에서 조회 (기존 로직 방어코드)
	        if(pressInfo == null) {
	             // (필요시 WH_MEMBER 조회 로직 추가)
	             pressInfo = new HashMap<>();
	             pressInfo.put("PRS_NAME", "Unknown"); 
	        }

	        // 4. 해당 작성자의 최신 글 3개 조회
	        paramMap.put("CON_TYPE", "8");
	        List<Map<String, Object>> threeContents = dao.selectList("content.selectAuthorThreeContents", paramMap);
	        
	        // 5. 데이터 구조핑 (Frontend Content 인터페이스 구조에 맞춤)
	        Map<String, Object> contentGroup = new HashMap<>();
	        
	        // 5-1. 작성자 정보 매핑
	        contentGroup.put("PRS_ID", prsId);
	        contentGroup.put("MEM_NAME", pressInfo.get("PRS_NAME")); // 혹은 MEM_NAME
	        contentGroup.put("MEM_PICTURE", pressInfo.get("stored_file_nm")); // 프로필 이미지 경로 확인 필요
	        contentGroup.put("press", pressInfo); // 상세 정보 포함
	        
	        // 5-2. 3개의 이미지를 'images' 배열에 매핑
	        // Frontend의 ContentImage 인터페이스: { IMG_URL, IMG_ORIGIN_URL }
	        contentGroup.put("images", threeContents); 
	        
	        // 5-3. 구독/잠금 로직을 위한 대표값 설정 (가장 최신 글 기준)
	        if (threeContents.size() > 0) {
	            contentGroup.put("CON_ID", threeContents.get(0).get("CON_ID")); // 대표 ID
	            contentGroup.put("CON_CREATE_DATE", threeContents.get(0).get("CON_CREATE_DATE"));
	        }
	        
	        int isSubscribed = dao.selectOne("content.checkSubscription", subParam); 

	        	// 3. 결과 맵에 추가
	        contentGroup.put("IS_SUBSCRIBED", isSubscribed);

	        resultList.add(contentGroup);
	    }
	    
	    resultMap.put("contents", resultList);
	    return resultMap;
	}
	
	public Map<String, Object> subscribe(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		try {
	        // paramMap에는 MEM_ID, PRS_ID, TRAN_ID가 들어옴
	        
	        // 2. DB에 저장
	        // Service단을 거치는 것이 정석이지만, 여기선 DAO 바로 호출 예시
	        dao.insert("content.insertSubscription", map);
	        
	        resultMap.put("success", true);
	        resultMap.put("message", "Subscription activated");
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        resultMap.put("success", false);
	        resultMap.put("message", "Database Error");
	    }
	    
	    return resultMap;
	}
	
	public Map<String, Object> mySubscriptions(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
	    
	    try {
	        List<Map<String, Object>> list = dao.selectList("content.selectMySubscriptions", map);
	        resultMap.put("list", list);
	        resultMap.put("success", true);
	    } catch (Exception e) {
	        e.printStackTrace();
	        resultMap.put("success", false);
	    }
	    
	    return resultMap;
	}
	
	
	public Map<String, Object> storyContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "6");
		//map.put("CON_CATEGORY", " AND CON_CATEGORY = " + map.get("CON_CATEGORY"));
		map.put("SORT_ITEM", "CON_TITLE");
		map.put("SORT_ORDER", "ASC");
		/*map.put("SORT_ITEM", "CON_PRIOT");
		map.put("SORT_ORDER", "DESC");*/
		List<Map<String, Object>> wcMap = dao.selectList("content.mainContents", map);
		
		for (int i = 0; i < wcMap.size(); i++) {
			String conId = String.valueOf(wcMap.get(i).get("CON_ID"));
			String prsId = String.valueOf(wcMap.get(i).get("PRS_ID"));
			Map<String, Object> parammMap = new HashMap<String, Object>();
			parammMap.put("PRS_ID", prsId);
			Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap);
			wcMap.get(i).put("press", pMap);
			
			Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
			Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
			
			String priceStr = String.valueOf(priceMap.get("PRICE"));
			String priceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
			String heartStr = String.valueOf(heartMap.get("HEART"));
			String heartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
			
			int price = Integer.parseInt(priceStr);
			int priceChg = Integer.parseInt(priceChgStr);
			int heart = Integer.parseInt(heartStr);
			int heartChg = Integer.parseInt(heartChgStr);
			
			wcMap.get(i).put("PRICE", price);
			wcMap.get(i).put("PRICE_CHG", priceChg);
			wcMap.get(i).put("HEART", heart);
			wcMap.get(i).put("HEART_CHG", heartChg);
		}
		
		resultMap.put("contents", wcMap);
		return resultMap;
	}
	

	public Map<String, Object> goblinContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "9");
		map.put("SORT_ITEM", "CON_ID");
		map.put("SORT_ORDER", "DESC");
		List<Map<String, Object>> mcMap = dao.selectList("content.mainContents", map);
		String myMemId = String.valueOf(map.get("MEM_ID"));
		
		for (int i = 0; i < mcMap.size(); i++) {
			String conId = String.valueOf(mcMap.get(i).get("CON_ID"));
			String prsId = String.valueOf(mcMap.get(i).get("PRS_ID"));
			Map<String, Object> parammMap = new HashMap<String, Object>();
			parammMap.put("PRS_ID", prsId);
			Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap);
			mcMap.get(i).put("press", pMap);
			
			Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
			Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
			
			String priceStr = String.valueOf(priceMap.get("PRICE"));
			String priceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
			String heartStr = String.valueOf(heartMap.get("HEART"));
			String heartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
			
			int price = Integer.parseInt(priceStr);
			int priceChg = Integer.parseInt(priceChgStr);
			int heart = Integer.parseInt(heartStr);
			int heartChg = Integer.parseInt(heartChgStr);
			
			mcMap.get(i).put("PRICE", price);
			mcMap.get(i).put("PRICE_CHG", priceChg);
			mcMap.get(i).put("HEART", heart);
			mcMap.get(i).put("HEART_CHG", heartChg);
			
			// [추가된 로직] 3. 구독 여부 확인
	        Map<String, Object> subParam = new HashMap<>();
	        subParam.put("MEM_ID", myMemId);
	        subParam.put("TARGET_ID", prsId);
	        
	        // content.xml에 추가했던 checkSubscription 쿼리 호출
	        int isSubscribed = dao.selectOne("content.checkSubscription", subParam);
	        
	        mcMap.get(i).put("IS_SUBSCRIBED", isSubscribed);
		}
		
		resultMap.put("contents", mcMap);
		return resultMap;
	}
	
	/* TODO
	 * 2022-09-22
	 * 내 콘텐츠(도깨비) 가져오기
	 * Mapper:myContents 추가
	 * 작업자 : 송준호
	 * */
	public Map<String, Object> apiSensibleMyContent(Map<String, Object> map) throws Exception {
		System.out.println("[CTSOFT] : " + map.toString());
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "9");
		map.put("SORT_ITEM", "CON_ID");
		map.put("SORT_ORDER", "DESC");
		List<Map<String, Object>> mcMap = dao.selectList("content.myContents", map);
		
		for (int i = 0; i < mcMap.size(); i++) {
			String conId = String.valueOf(mcMap.get(i).get("CON_ID"));
			String prsId = String.valueOf(mcMap.get(i).get("PRS_ID"));
			Map<String, Object> parammMap = new HashMap<String, Object>();
			parammMap.put("PRS_ID", prsId);
			Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap);
			mcMap.get(i).put("press", pMap);
			
			Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
			Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
			
			String priceStr = String.valueOf(priceMap.get("PRICE"));
			String priceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
			String heartStr = String.valueOf(heartMap.get("HEART"));
			String heartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
			
			int price = Integer.parseInt(priceStr);
			int priceChg = Integer.parseInt(priceChgStr);
			int heart = Integer.parseInt(heartStr);
			int heartChg = Integer.parseInt(heartChgStr);
			
			mcMap.get(i).put("PRICE", price);
			mcMap.get(i).put("PRICE_CHG", priceChg);
			mcMap.get(i).put("HEART", heart);
			mcMap.get(i).put("HEART_CHG", heartChg);
		}

		resultMap.put("contents", mcMap);
		return resultMap;
	}
	
	public Map<String, Object> profileContent(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("SORT_ITEM", "CON_ID");
		map.put("SORT_ORDER", "DESC");
		int page = Integer.parseInt(String.valueOf(map.get("PAGE")));
		map.put("SKIP", (page - 1) * 30);
		List<Map<String, Object>> mcMap = dao.selectList("content.profileContents", map);
		Map<String, Object> cntMap = dao.selectOne("content.profileContentsCount", map);
		
		for (int i = 0; i < mcMap.size(); i++) {
			String conId = String.valueOf(mcMap.get(i).get("CON_ID"));
			String prsId = String.valueOf(mcMap.get(i).get("PRS_ID"));
			Map<String, Object> parammMap = new HashMap<String, Object>();
			parammMap.put("PRS_ID", prsId);
			Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap);
			mcMap.get(i).put("press", pMap);
			
			Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
			Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
			
			String priceStr = String.valueOf(priceMap.get("PRICE"));
			String priceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
			String heartStr = String.valueOf(heartMap.get("HEART"));
			String heartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
			
			int price = Integer.parseInt(priceStr);
			int priceChg = Integer.parseInt(priceChgStr);
			int heart = Integer.parseInt(heartStr);
			int heartChg = Integer.parseInt(heartChgStr);
			
			
			mcMap.get(i).put("PRICE", price);
			mcMap.get(i).put("PRICE_CHG", priceChg);
			mcMap.get(i).put("HEART", heart);
			mcMap.get(i).put("HEART_CHG", heartChg);
		}
		int contentsCnt = Integer.parseInt(String.valueOf(cntMap.get("CNT")));

		resultMap.put("contents", mcMap);
		resultMap.put("contentsCount", contentsCnt);
		return resultMap;
	}
	

	public Map<String, Object> promotionContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "3");
		map.put("SORT_ITEM", "CON_IMPORTANCE DESC, CON_CREATE_DATE ");
		map.put("SORT_ORDER", "DESC");
		List<Map<String, Object>> pcMap = dao.selectList("content.mainContents", map);
		resultMap.put("contents", pcMap);
		return resultMap;
	}

	public Map<String, Object> FAQContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "2");
		map.put("SORT_ITEM", "CON_IMPORTANCE DESC, CON_CREATE_DATE ");
		map.put("SORT_ORDER", "DESC");
		List<Map<String, Object>> fcMap = dao.selectList("content.mainContents", map);
		resultMap.put("contents", fcMap);
		return resultMap;
	}

	public Map<String, Object> SearchResultContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
//		map.put("CON_TYPE", "1");
		
		String keyword = (String) map.get("keyword");
		String keywordChg = URLDecoder.decode(keyword, "UTF-8");
		
		if (keywordChg != null && keywordChg.length() > 0) {
			map.put("keyword", keywordChg);
		} else {
			map.put("keyword", "not keyword");
		}
		
		List<Map<String, Object>> scMap = dao.selectList("content.searchContents", map);
		for(int i=0; i<scMap.size(); i++){
			String conId = String.valueOf(scMap.get(i).get("CON_ID"));
			String prsId = String.valueOf(scMap.get(i).get("PRS_ID"));
			Map<String, Object> parammMap = new HashMap<String, Object>();
			parammMap.put("PRS_ID", prsId);
			Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap);
			scMap.get(i).put("press", pMap);
			
			Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
			Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);

			String priceStr = String.valueOf(priceMap.get("PRICE"));
			String priceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
			String heartStr = String.valueOf(heartMap.get("HEART"));
			String heartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
			
			int price = Integer.parseInt(priceStr);
			int priceChg = Integer.parseInt(priceChgStr);
			int heart = Integer.parseInt(heartStr);
			int heartChg = Integer.parseInt(heartChgStr);
			
			scMap.get(i).put("PRICE", price);
			scMap.get(i).put("PRICE_CHG", priceChg);
			scMap.get(i).put("HEART", heart);
			scMap.get(i).put("HEART_CHG", heartChg);
		}
		
		resultMap.put("contents", scMap);
		return resultMap;
	}

	public Map<String, Object> popup(Map<String, Object> map) throws Exception {
		map.put("CON_TYPE", "4");
		return dao.selectOne("content.popupContents", map);
	}

	public Map<String, Object> NonRiContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "11");
		map.put("SORT_ITEM", "CON_TITLE");
		map.put("SORT_ORDER", "ASC");
		/*map.put("SORT_ITEM", "CON_PRIOT");
		map.put("SORT_ORDER", "DESC");*/
		List<Map<String, Object>> mcMap = dao.selectList("content.mainContents", map);
		
		for (int i = 0; i < mcMap.size(); i++) {
			String conId = String.valueOf(mcMap.get(i).get("CON_ID"));
			String prsId = String.valueOf(mcMap.get(i).get("PRS_ID"));
			Map<String, Object> parammMap = new HashMap<String, Object>();
			parammMap.put("PRS_ID", prsId);
			Map<String, Object> pMap = dao.selectOne("admin.selectPress", parammMap);
			mcMap.get(i).put("press", pMap);
			
			Map<String, Object> priceMap = dao.selectOne("content.contentTotalPrice", conId);
			Map<String, Object> heartMap = dao.selectOne("content.contentTotalHeart", conId);
			
			String priceStr = String.valueOf(priceMap.get("PRICE"));
			String priceChgStr = String.valueOf(priceMap.get("PRICE_CHG"));
			String heartStr = String.valueOf(heartMap.get("HEART"));
			String heartChgStr = String.valueOf(heartMap.get("HEART_CHG"));
			
			int price = Integer.parseInt(priceStr);
			int priceChg = Integer.parseInt(priceChgStr);
			int heart = Integer.parseInt(heartStr);
			int heartChg = Integer.parseInt(heartChgStr);
			
			mcMap.get(i).put("PRICE", price);
			mcMap.get(i).put("PRICE_CHG", priceChg);
			mcMap.get(i).put("HEART", heart);
			mcMap.get(i).put("HEART_CHG", heartChg);
		}
		
		resultMap.put("contents", mcMap);
		return resultMap;
	}
	
	public Map<String, Object> StarContents(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		map.put("CON_TYPE", "6");
		map.put("SORT_ITEM", "CON_IMPORTANCE DESC, CON_UPDATE_DATE ");
		map.put("SORT_ORDER", "DESC");
		List<Map<String, Object>> fcMap = dao.selectList("content.mainContents", map);
		resultMap.put("contents", fcMap);
		return resultMap;
	}
	
	public Map<String, Object> appInstDetail(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<String, Object> cMap = dao.selectOne("content.appInstDetail", map);
		
		String memId = (String) map.get("MEM_ID");
		Map<String, Object> mMap = dao.selectOne("member.selectMember", memId);
//		Map<String, Object> hMap = dao.contentHistoryChk(map);

		mMap.put("isSuccess", 1);
		cMap.put("CON_EXPD", 0);
		
//		if (hMap == null || hMap.get("MEM_ID") == null) {
//			dao.insert("content.insertContentHistory", map);
//		}
		
		// comments
		List<Map<String, Object>> cmMap = dao.selectList("content.getComments", map);
		for (int i = 0; i < cmMap.size(); i++) {
			String cmMemId = (String) cmMap.get(i).get("MEM_ID");
			Map<String, Object> cmMemIdMap = dao.selectOne("member.selectMember", cmMemId);
			cmMap.get(i).put("User", cmMemIdMap);
		}
		cMap.put("comments", cmMap);

		// images
		List<Map<String, Object>> imgMap = dao.selectList("content.getImages", map);
		cMap.put("images", imgMap);

		resultMap.put("result", mMap);
		resultMap.put("contents", cMap);
		return resultMap;
	}
	
	public Map<String, Object> policyDetail(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<String, Object> cMap = dao.selectOne("content.policyDetail", map);
		
//		String memId = (String) map.get("MEM_ID");
		Map<String, Object> mMap = new HashMap<String, Object>();
//		Map<String, Object> mMap = memberDAO.selectOne("member.selectMember", memId);
//		Map<String, Object> hMap = dao.contentHistoryChk(map);

		mMap.put("isSuccess", 1);
		cMap.put("CON_EXPD", 0);
		
//		if (hMap == null || hMap.get("MEM_ID") == null) {
//			dao.insert("content.insertContentHistory", map);
//		}
		
		// comments
//		List<Map<String, Object>> cmMap = dao.selectList("content.getComments", map);
//		for (int i = 0; i < cmMap.size(); i++) {
//			String cmMemId = (String) cmMap.get(i).get("MEM_ID");
//			Map<String, Object> cmMemIdMap = memberDAO.selectOne("member.selectMember", cmMemId);
//			cmMap.get(i).put("User", cmMemIdMap);
//		}
//		cMap.put("comments", cmMap);

		// images
//		List<Map<String, Object>> imgMap = dao.selectList("content.getImages", map);
//		cMap.put("images", imgMap);

		resultMap.put("result", mMap);
		resultMap.put("contents", cMap);
		return resultMap;
	}

	public List<Map<String, Object>> postComment(Map<String, Object> map) throws Exception {
		Map<String, Object> cmMap = dao.selectOne("content.commentCntChk", map);
		Long comId = (Long) cmMap.get("COM_ID");
		map.put("COM_ID", comId);
		dao.insert("content.insertComment", map);
		List<Map<String, Object>> comList = dao.selectList("content.getComments", map);
		for (int i = 0; i < comList.size(); i++) {
			String memId = (String) comList.get(i).get("MEM_ID");
			Map<String, Object> mMap = dao.selectOne("member.selectMember", memId);
			comList.get(i).put("User", mMap);
		}
		return comList;
	}

	public List<Map<String, Object>> putComment(Map<String, Object> map) throws Exception {
		dao.update("content.updateComment", map);
		List<Map<String, Object>> comList = dao.selectList("content.getComments", map);
		for (int i = 0; i < comList.size(); i++) {
			String memId = (String) comList.get(i).get("MEM_ID");
			Map<String, Object> mMap = dao.selectOne("member.selectMember", memId);
			comList.get(i).put("User", mMap);
		}
		return comList;
	}

	public List<Map<String, Object>> deleteComment(Map<String, Object> map) throws Exception {
		dao.update("content.deleteComment", map);
		List<Map<String, Object>> comList = dao.selectList("content.getComments", map);
		for (int i = 0; i < comList.size(); i++) {
			String memId = (String) comList.get(i).get("MEM_ID");
			Map<String, Object> mMap = dao.selectOne("member.selectMember", memId);
			comList.get(i).put("User", mMap);
		}
		return comList;
	}

	public List<Map<String, Object>> getComments(Map<String, Object> map) throws Exception {
		List<Map<String, Object>> comList = dao.selectList("content.getComments", map);
		for (int i = 0; i < comList.size(); i++) {
			String memId = (String) comList.get(i).get("MEM_ID");
			Map<String, Object> mMap = dao.selectOne("member.selectMember", memId);
			comList.get(i).put("User", mMap);
		}
		return comList;
	}
	
	public int purchase(Map<String, Object> map) {
		Map<String, Object> mMap = new HashMap<String, Object>();
		String type = (String) map.get("receipt");
		String starStr = String.valueOf(map.get("chargedStarNum"));
		starStr = starStr.replace(",", "").replace("개", "");
		int star = Integer.parseInt(starStr);
		String memId = (String) map.get("MEM_ID");
		int starInt = star;
		int memStarInt = 0;
		int memStarChgInt = 0;
		map.put("STAR", starInt);
		if (type != null && type.equals("free")) {
			map.put("CHG_TYPE", 1);
		} else {
			map.put("CHG_TYPE", 2);
		}
		try {
			dao.insert("content.insertChage", map);
			if (type != null && type.equals("free")) {
				dao.update("member.plusMemberStar", map);
			} else {
				dao.update("member.plusMemberStarChg", map);
			}
			mMap = dao.selectOne("member.selectMember", memId);
			int memStar = (int) mMap.get("MEM_STAR");
			int memStarChg = (int) mMap.get("MEM_STAR_CHG");
			memStarInt = memStar;
			memStarChgInt = memStarChg;
		} catch (Exception e) {
			memStarInt = -1;
		}
		
		if (type != null && type.equals("free")) {
			return memStarInt;
		} else {
			return memStarChgInt;
		}
	}
	
	public Map<String, Object> sensibleEvent(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		Map<String, Object> eMap = dao.selectOne("content.selectEventLast");
		if (eMap != null && eMap.get("EVT_ID") != null) {
			String evtId = String.valueOf(eMap.get("EVT_ID"));
			map.put("EVT_ID", evtId);
			
			Map<String, Object> ehMap = dao.selectOne("content.selectEventHst", map);
			if (ehMap != null && ehMap.get("EVT_ID") != null) {
				resultMap.put("RESULT", "FAIL");
			} else {
				resultMap.put("item", eMap);
				resultMap.put("RESULT", "OK");
			}
		} else {
			resultMap.put("RESULT", "FAIL");
		}
		
		return resultMap;
	}
	
	public Map<String, Object> apiSensibleEvent(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		String memId = (String) map.get("MEM_ID");
		Map<String, Object> eMap = dao.selectOne("content.selectEvent", map);
		String chgType = String.valueOf(eMap.get("EVT_CHG_YN"));
		String star = String.valueOf(eMap.get("EVT_STAR"));
		map.put("STAR", star);
		
		Map<String, Object> ehMap = dao.selectOne("content.selectEventHst", map);
		if (ehMap != null && ehMap.get("EVT_ID") != null) {
			resultMap.put("RESULT", "FAIL");
		} else {
			dao.insert("content.insertChageEvt", map);
			dao.insert("content.insertChageEvtHst", map);
			if (chgType.equals("1")) {
				dao.update("member.plusMemberStarChg", map);
			} else {
				dao.update("member.plusMemberStar", map);
			}
			
			int memStarInt = 0;
			int memStarChgInt = 0;
			Map<String, Object> mMap = dao.selectOne("member.selectMember", memId);
			int memStar = (int) mMap.get("MEM_STAR");
			int memStarChg = (int) mMap.get("MEM_STAR_CHG");
			memStarInt = memStar;
			memStarChgInt = memStarChg;

			resultMap.put("MEM_STAR", memStarInt);
			resultMap.put("MEM_STAR_CHG", memStarChgInt);
			resultMap.put("RESULT", "OK");
		}
		return resultMap;
	}
	
	
	public Map<String, Object> insertContents(HttpServletRequest request,Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		try
		{
			
			Map<String, Object> cMap = dao.selectOne("content.contentNextConId");
			Long conId = (Long) cMap.get("CON_ID");
			map.put("CON_ID", conId);
			map.put("CON_TYPE", "9");
			
			map.put("PRS_ID", map.get("MEM_ID"));			
			map.put("APP_ID", dao.selectOne("content.selectAppMember", map));
			
			dao.insert("content.contentRegit", map);

			// 영상 등록
			if ((String) map.get("CON_VIDEO") != null && !"".equals((String) map.get("CON_VIDEO"))) {
				String fileNm = String.valueOf(conId.intValue()) + ".mp4"; // .mp4 확장자 설정
				saveVideoFile((String) map.get("CON_VIDEO"), fileNm);

				// 썸네일 파일 이름 설정
				String thumbnailFileNm = String.valueOf(conId.intValue()) + ".jpg";
				generateVideoThumbnailWithJCodec(Constants._VIDEO_SAVE_PATH + fileNm, Constants._FILE_SAVE_PATH + thumbnailFileNm);


				map.put("CON_ORIGIN_URL", Constants._VIDEO_FILE_URL + fileNm);
				map.put("CON_THUMNAIL", Constants._FILE_URL + thumbnailFileNm); // 썸네일 경로

				dao.update("admin.contentImgUpdate", map); // DB Update (영상 경로 저장)
			}

			//이미지 등록
			if((String)map.get("CON_THUMNAIL") !=null || !"".equals((String)map.get("CON_THUMNAIL")) )
			{
				String fileNm = String.valueOf(conId.intValue()) + ".jpg";				
				makeImgFile((String)map.get("CON_THUMNAIL"),fileNm);
				
				map.put("CON_THUMNAIL", Constants._FILE_URL + fileNm);

				dao.update("admin.contentImgUpdate", map);
			}
			
			resultMap.put("RESULT", "OK");
			
		}catch(Exception e){
			e.printStackTrace();
			resultMap.put("RESULT", "FAIL");
		}		
		
		return resultMap;
	}
	
	/*private void makeImgFile(String base64, String savename) throws Exception {
		
		try {
			byte decode[] = Base64.decodeBase64(base64);
			FileOutputStream fos;
			try{
				String target_path = "/var/lib/tomcat7/webapps/img/";
				File target = new File(target_path  + savename);
				target.createNewFile();
				System.out.println(target.exists());
				fos = new FileOutputStream(target);
				fos.write(decode);
				fos.close();
			}catch(Exception e){
				e.printStackTrace();
			}
			System.out.println("DONE");
			
		} catch (Exception e) {
			throw e;
		}
	}*/

	/** 영상 파일에서 썸네일 이미지를 저장한다. */
	private void generateVideoThumbnailWithJCodec(String videoPath, String thumbnailPath) throws IOException, JCodecException {
		File videoFile = new File(videoPath);
		FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile));

		// 특정 시간으로 이동 (예: 1초)
		grab.seekToSecondPrecise(1);

		Picture picture = grab.getNativeFrame();
		if (picture == null) {
			throw new RuntimeException("프레임을 추출할 수 없습니다. 비디오 파일을 확인하세요.");
		}

		// YUV 데이터를 BufferedImage로 변환
		BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);

		// 썸네일 저장
		File outputFile = new File(thumbnailPath);
		if (!ImageIO.write(bufferedImage, "jpg", outputFile)) {
			throw new RuntimeException("이미지 파일 저장에 실패했습니다: " + thumbnailPath);
		}

		System.out.println("썸네일 생성 완료: " + thumbnailPath);

		if (!ImageIO.write(bufferedImage, "jpg", outputFile)) {
			throw new RuntimeException("이미지 파일 저장에 실패했습니다. 경로를 확인하세요: " + thumbnailPath);
		}

		ImageIO.write(bufferedImage, "jpg", outputFile); // 썸네일 저장
	}

	/** 영상 파일을 등록한다.*/
	public void saveVideoFile(String videobase64, String savename) throws Exception {
		// 디렉토리 확인 및 생성
		File directory = new File(Constants._VIDEO_SAVE_PATH);
		if (!directory.exists()) {
			directory.mkdirs(); // 디렉토리가 없으면 생성
		}

		// Base64 디코딩
		byte[] videoBytes = Base64.decodeBase64(videobase64);

		// 임시 파일로 저장
		File tempFile = new File(Constants._VIDEO_SAVE_PATH + "temp_video.mov");
		try (FileOutputStream fos = new FileOutputStream(tempFile)) {
			fos.write(videoBytes);
		}

		// MIME 타입 확인
		String mimeType = Files.probeContentType(tempFile.toPath());
		if (mimeType != null && mimeType.equals("video/quicktime")) {
			// QuickTime 파일이라면 변환
			File mp4File = new File(Constants._VIDEO_SAVE_PATH + savename);
			convertQuickTimeToMP4(tempFile, mp4File);
			// MP4 파일 저장 후 삭제
			tempFile.delete();
		} else {
			// QuickTime 파일이 아니라면 그대로 저장
			File outputFile = new File(Constants._VIDEO_SAVE_PATH + savename);
			try (FileOutputStream fos = new FileOutputStream(outputFile)) {
				fos.write(videoBytes);
			}
		}
	}

	// QuickTime을 MP4로 변환
	private void convertQuickTimeToMP4(File inputFile, File outputFile) throws IOException, InterruptedException {
		// FFmpeg 명령어 경로 설정 (절대 경로 사용)
		String ffmpegPath = "/usr/bin/ffmpeg";  // ffmpeg가 설치된 경로로 수정
		String command = ffmpegPath + " -i " + inputFile.getAbsolutePath() + " -vcodec h264 -acodec aac -strict -2 " + outputFile.getAbsolutePath();

		// FFmpeg 실행
		Process process = Runtime.getRuntime().exec(command);

		// 프로세스 출력 확인
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);  // FFmpeg의 출력 로그
			}
		}

		// 프로세스 종료 대기
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new RuntimeException("FFmpeg 오류: 동영상 변환 실패");
		}

		System.out.println("QuickTime 파일을 MP4로 변환 완료: " + outputFile.getAbsolutePath());
	}


	/** 이미지 파일을 등록한다.*/
	private void makeImgFile(String imgbase64, String savename) throws Exception {
		
		try {
			// create a buffered image
			BufferedImage image = null;

			//String[] base64Arr = imgbase64.split(","); // image/png;base64, 이 부분 버리기 위한 작업
			byte[] imageByte = Base64.decodeBase64(imgbase64); // base64 to byte array로 변경
			
			
			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			image = ImageIO.read(bis);
			bis.close();

			// write the image to a file			
			File outputfile = new File(Constants._FILE_SAVE_PATH + savename);
			
			ImageIO.write(image, "jpg", outputfile); // 파일생성
			
		} catch (IOException e) {
			throw e;
		}
	}
	
	
	public Map<String, Object> updateContents(HttpServletRequest request, Map<String, Object> map) throws Exception {
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		try
		{
			map.put("PRS_ID", map.get("MEM_ID"));
			
			dao.update("content.contentUpdate", map);
			
			//이미지 등록
			if((String)map.get("CON_THUMNAIL") !=null || !"".equals((String)map.get("CON_THUMNAIL")) )
			{
				String fileNm = String.valueOf(map.get("CON_ID")) +"_"+DateUtil.getSysdate() +".jpg";				
				makeImgFile((String)map.get("CON_THUMNAIL"),fileNm);
				
				map.put("CON_THUMNAIL", Constants._FILE_URL + fileNm);

				dao.update("admin.contentImgUpdate", map);
			}			
			
			resultMap.put("RESULT", "OK");
			
			
		}catch(Exception e){
			e.printStackTrace();
			resultMap.put("RESULT", "FAIL");
		}
		
		return resultMap;
	}
	
	public Map<String, Object> blockUser(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();		
		
		try
		{
			dao.insert("insertBlockUser", map);
			
			resultMap.put("RESULT", "OK");
		}catch(Exception e){
			e.printStackTrace();
			resultMap.put("RESULT", "FAIL");
		}
		
		return resultMap;
	}
	
	public List<Map<String, Object>> viewers(Map<String, Object> map) throws Exception{
		List<Map<String, Object>> resultMap = dao.selectList(
				"content.viewers", map);
		return resultMap;
	}
	
	public List<Map<String, Object>> goblinPrsList(Map<String, Object> map) throws Exception{
		List<Map<String, Object>> resultMap = dao.selectList(
				"content.goblinPrsList", map);
		return resultMap;
	}
}
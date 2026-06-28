package com.sensible.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sensible.api.service.SuperAppService;

@Controller
public class DeepLinkController {

	@Resource(name = "superAppService")
	private SuperAppService superAppService;

	private String getBaseUrl(HttpServletRequest request) {
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		String scheme = request.getScheme();

		if (serverName.equals("localhost") || serverName.equals("127.0.0.1") || serverName.startsWith("192.168.")) {
			String portStr = "";
			if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
				portStr = ":" + serverPort;
			}
			return scheme + "://" + serverName + portStr;
		}

		return "https://witch-hunting.com";
	}

	// 🌟 앱에서 공유하기로 생성되는 링크 주소들을 모두 이곳으로 연결
	@RequestMapping(value = { "/post/{id}", "/star/{id}", "/feed-detail/{id}" })
	public String deeplinkTrampoline(@PathVariable String id, HttpServletRequest request, Model model) {
		System.out.println("DeepLink Triggered");

		String uri = request.getRequestURI();
		String baseUrl = getBaseUrl(request);
		
		String userAgent = request.getHeader("User-Agent");
		boolean isFacebookBot = userAgent != null && userAgent.toLowerCase().contains("facebookexternalhit");

		try {
			// 🌟 1. 스타 페이지 공유 링크인 경우
			if (uri.contains("/star/")) {

				Map<String, Object> param = new HashMap<>();
				param.put("starId", id);
				param.put("deviceId", "BOT");

				Map<String, Object> response = superAppService.getStarDetail(param);

				if ("OK".equals(response.get("result"))) {
					@SuppressWarnings("unchecked")
					Map<String, Object> starInfo = (Map<String, Object>) response.get("starInfo");
					
					// 🌟 [핵심 수정] 이미지 경로가 상대경로면 무조건 절대경로(http...)로 변환 (트위터 호환성)
					String imageUrl = (String) starInfo.get("image");
					if (imageUrl != null && !imageUrl.startsWith("http")) {
					    // DB에서 'assets/...' 처럼 슬래시 없이 넘어올 경우를 대비해 슬래시 추가
					    if (!imageUrl.startsWith("/")) {
					        imageUrl = "/" + imageUrl;
					    }
					    imageUrl = baseUrl + imageUrl;
					}

					// JSP로 데이터 넘겨주기
					model.addAttribute("ogTitle", starInfo.get("name") + " | StarPlatform");
					model.addAttribute("ogDesc",
							"Global Rank #" + starInfo.get("GLOBAL_RANK") + " | Visitors " + starInfo.get("viewCount"));
					
					// 🌟 원본 데이터 대신 절대경로로 조립된 imageUrl 변수를 사용합니다.
					model.addAttribute("ogImage", imageUrl); 
					model.addAttribute("ogUrl", baseUrl + uri);
				}
			}
			// 🌟 2. 피드(게시글) 공유 링크인 경우
			else if (uri.contains("/feed-detail/") || uri.contains("/post/")) {

				Map<String, Object> param = new HashMap<>();
				param.put("conId", id);

				// 기존에 만들어둔 피드 상세 조회 서비스 호출
				Map<String, Object> response = superAppService.getFeedDetail(param);

				if ("OK".equals(response.get("result"))) {
					@SuppressWarnings("unchecked")
					Map<String, Object> content = (Map<String, Object>) response.get("content");
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> medias = (List<Map<String, Object>>) response.get("medias");

					String starName = (String) content.get("PRS_NAME"); // 작성자 이름
					String body = (String) content.get("CON_BODY"); // 글 본문

					// 💡 글 내용이 너무 길면 미리보기 카드가 깨질 수 있으므로 적당히 자름
					if (body != null && body.length() > 50) {
						body = body.substring(0, 50) + "...";
					}

					// 기본 썸네일 이미지 주소
					String imageUrl = baseUrl + "/resources/img/icon.png";

					// 💡 첨부된 미디어(사진)가 있다면 첫 번째 사진을 썸네일로 교체!
					if (medias != null && !medias.isEmpty()) {
						Map<String, Object> firstMedia = medias.get(0);
						// THUMB_URL을 우선으로 쓰고, 없으면 원본 MEDIA_URL을 씀
						String thumbUrl = (String) firstMedia.get("THUMB_URL");
						String mediaUrl = (String) firstMedia.get("MEDIA_URL");

						String targetUrl = (thumbUrl != null) ? thumbUrl : (mediaUrl != null ? mediaUrl : imageUrl);

						// 카카오톡 봇은 무조건 절대경로(http)를 요구하므로, 상대경로일 경우 도메인 붙여주기
						if (targetUrl.startsWith("/")) {
							targetUrl = baseUrl + targetUrl;
						}
						imageUrl = targetUrl;
					}

					model.addAttribute("ogTitle", starName + "'s Post | StarPlatform");
					model.addAttribute("ogDesc", body != null ? body : "Check out the latest updates.");
					model.addAttribute("ogImage", imageUrl);
					model.addAttribute("ogUrl", baseUrl + uri);
				}
			}
			// 🌟 3. 그 외 알 수 없는 링크일 경우 기본값
			else {
				model.addAttribute("ogTitle", "StarPlatform SuperApp");
				String defaultTitle = "StarPlatform SuperApp";
				if (isFacebookBot) {
					defaultTitle += " | Everyone Can Earn";
				}
				model.addAttribute("ogDesc", "Everyone Can Earn");
				model.addAttribute("ogImage", baseUrl + "/resources/img/icon.png");
				model.addAttribute("ogUrl", baseUrl + uri);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 🌟 4. 화면에 그릴 내용은 없고 스크립트만 있는 jsp 파일로 연결 (데이터 포함)
		return "/common/deeplink_redirect";
	}
	
	@RequestMapping(value = "/")
    public String rootTrampoline(HttpServletRequest request, Model model) {
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "";
        userAgent = userAgent.toLowerCase();
        
        // 1. 모바일 기기(안드로이드, 아이폰, 아이패드)인지 확인
        boolean isMobile = userAgent.contains("android") || userAgent.contains("iphone") || userAgent.contains("ipad");
        
        // 🌟 2. 카카오톡 등 미리보기 스크랩 봇인지 확인 추가
     // 🌟 2. 카카오톡 등 미리보기 스크랩 봇인지 확인 (트위터/X 추가)
        boolean isBot = userAgent.contains("bot") 
                     || userAgent.contains("scrap") 
                     || userAgent.contains("facebookexternalhit") 
                     || userAgent.contains("kakao")
                     || userAgent.contains("twitter")
                     || userAgent.contains("xbot");
        
     // 🌟 [추가] 특정해서 페이스북 봇인지 확인
        boolean isFacebookBot = userAgent.contains("facebookexternalhit");

        // 💻 3. 모바일도 아니고 봇도 아닌 찐 PC 환경인 경우 -> 사내 관리자 로그인 페이지로
        if (!isMobile && !isBot) {
            return "/common/landing";
        }

        // 📱 4. 모바일 환경이거나 미리보기 수집 봇인 경우 -> 브릿지 페이지로 연결 (OG태그 노출)
        String uri = request.getRequestURI();
        String baseUrl = getBaseUrl(request);
        
     // 🌟 페이스북 봇일 경우에만 제목 뒤에 문구 추가
        String defaultTitle = "StarPlatform SuperApp";
        if (isFacebookBot) {
            defaultTitle += " | Everyone Can Earn";
        }
        
        model.addAttribute("ogTitle", defaultTitle);
        model.addAttribute("ogDesc", "Everyone Can Earn");
        model.addAttribute("ogImage", baseUrl + "/resources/img/icon.png"); 
        model.addAttribute("ogUrl", baseUrl + uri);
        
        return "/common/deeplink_redirect"; 
    }
	
	// 🌟 2. 개인정보 처리방침 페이지 매핑
    @RequestMapping(value = "/privacy")
    public String privacyPolicy() {
        return "/common/privacy-policy"; // (/WEB-INF/jsp/landing/privacy.jsp)
    }

    // 🌟 3. 이용약관 페이지 매핑
    @RequestMapping(value = "/terms")
    public String termsOfService() {
        return "/common/terms"; // (/WEB-INF/jsp/landing/terms.jsp)
    }
}
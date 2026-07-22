package com.sensible.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

	private String encodeUrlParams(String urlStr) {
		if (urlStr == null) return null;
		if (!urlStr.contains("api.dicebear.com")) return urlStr;
		try {
			int queryIdx = urlStr.indexOf("?");
			if (queryIdx == -1) return urlStr;
			
			String baseUrlPart = urlStr.substring(0, queryIdx);
			String queryString = urlStr.substring(queryIdx + 1);
			
			String[] pairs = queryString.split("&");
			StringBuilder newQuery = new StringBuilder();
			for (String pair : pairs) {
				if (newQuery.length() > 0) {
					newQuery.append("&");
				}
				int eqIdx = pair.indexOf("=");
				if (eqIdx != -1) {
					String key = pair.substring(0, eqIdx);
					String val = pair.substring(eqIdx + 1);
					newQuery.append(key).append("=").append(java.net.URLEncoder.encode(val, "UTF-8"));
				} else {
					newQuery.append(pair);
				}
			}
			return baseUrlPart + "?" + newQuery.toString();
		} catch (Exception e) {
			return urlStr;
		}
	}

	@RequestMapping(value = "/image/proxy", method = RequestMethod.GET)
	public void imageProxy(@RequestParam("url") String targetUrl, HttpServletResponse response) {
		if (targetUrl == null || !targetUrl.contains("api.dicebear.com")) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		java.io.InputStream in = null;
		java.io.OutputStream out = null;
		java.net.HttpURLConnection conn = null;
		try {
			java.net.URL url = new java.net.URL(targetUrl);
			conn = (java.net.HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
			
			int responseCode = conn.getResponseCode();
			if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
				response.setContentType(conn.getContentType());
				response.setContentLength(conn.getContentLength());
				
				in = conn.getInputStream();
				out = response.getOutputStream();
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
				out.flush();
			} else {
				response.setStatus(responseCode);
			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} finally {
			if (in != null) { try { in.close(); } catch (Exception e) {} }
			if (out != null) { try { out.close(); } catch (Exception e) {} }
			if (conn != null) { conn.disconnect(); }
		}
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
					if (imageUrl != null) {
					    if (!imageUrl.startsWith("http")) {
					        // DB에서 'assets/...' 처럼 슬래시 없이 넘어올 경우를 대비해 슬래시 추가
					        if (!imageUrl.startsWith("/")) {
					            imageUrl = "/" + imageUrl;
					        }
					        imageUrl = baseUrl + imageUrl;
					    }
					    // 🌟 [추가] http://witch-hunting.com 으로 시작한다면 강제로 https:// 로 변경 (트위터 HTTPS 요구 대응)
					    if (imageUrl.startsWith("http://witch-hunting.com")) {
					        imageUrl = imageUrl.replace("http://", "https://");
					    }
					    // 🌟 [추가] DiceBear SVG 아바타는 크롤러(카톡/페이스북/슬랙 등)가 지원하지 않으므로 PNG로 변환 및 페이스북 크기 조건(200x200 이상) 충족
					    if (imageUrl.contains("api.dicebear.com") && imageUrl.contains("/svg")) {
					        imageUrl = imageUrl.replace("/svg", "/png");
					        if (!imageUrl.contains("size=")) {
					            imageUrl = imageUrl + (imageUrl.contains("?") ? "&" : "?") + "size=256";
					        }
					        // 🌟 [추가] 투명 배경 PNG 렌더링 버그 방지를 위해 불투명 배경색(흰색) 지정
					        if (!imageUrl.contains("backgroundColor=")) {
					            imageUrl = imageUrl + "&backgroundColor=ffffff";
					        }
					    }
					    // 🌟 [추가] DiceBear URL 내 한글 seed 파라미터 등을 퍼센트 인코딩 처리 (페이스북 등 크롤러 호환)
					    imageUrl = encodeUrlParams(imageUrl);
					    // 🌟 [추가] Dicebear 이미지는 동일 도메인(Proxy)을 통해 제공하여 모바일 크롤러 차단 우회
					    if (imageUrl.contains("api.dicebear.com")) {
					        try {
					            imageUrl = baseUrl + request.getContextPath() + "/image/proxy?url=" + java.net.URLEncoder.encode(imageUrl, "UTF-8");
					        } catch (Exception e) {
					            // ignore
					        }
					    }
					    // 🌟 [추가] 트위터의 강력한 이미지 수집 실패 캐시를 파괴하기 위한 타임스탬프 쿼리스트링 삽입 (파라미터 중복 방어)
					    if (imageUrl.contains("?")) {
					        imageUrl = imageUrl + "&t=" + System.currentTimeMillis();
					    } else {
					        imageUrl = imageUrl + "?t=" + System.currentTimeMillis();
					    }
					}

					// JSP로 데이터 넘겨주기 (X/트위터 UI가 카드 설명란을 노출하지 않는 이슈 대응하여 제목에 랭크 직접 표기)
					String displayTitle = starInfo.get("name") + " (Global Rank #" + starInfo.get("GLOBAL_RANK") + ") | StarPlatform";
					model.addAttribute("ogTitle", displayTitle);
					model.addAttribute("ogDesc",
							"Global Rank #" + starInfo.get("GLOBAL_RANK") + " | Visitors " + starInfo.get("viewCount"));
					
					// 🌟 원본 데이터 대신 절대경로 및 캐시버스팅이 조립된 imageUrl 변수를 사용합니다.
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

						// 카카오톡/트위터 봇 호환을 위해 상대경로일 경우 도메인 붙여주기
						if (targetUrl.startsWith("/")) {
							targetUrl = baseUrl + targetUrl;
						}
						// 🌟 [추가] http://witch-hunting.com 으로 시작한다면 강제로 https:// 로 변경 (트위터 HTTPS 대응)
						if (targetUrl.startsWith("http://witch-hunting.com")) {
							targetUrl = targetUrl.replace("http://", "https://");
						}
						// 🌟 [추가] 캐시 버스팅을 위한 타임스탬프 쿼리스트링 삽입 (파라미터 중복 방어)
						if (targetUrl.contains("?")) {
							targetUrl = targetUrl + "&t=" + System.currentTimeMillis();
						} else {
							targetUrl = targetUrl + "?t=" + System.currentTimeMillis();
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
				
				// 🌟 [추가] 기본 이미지에도 HTTPS 대응 및 캐시 버스팅 적용
				String defaultIcon = baseUrl + "/resources/img/icon.png";
				if (defaultIcon.startsWith("http://witch-hunting.com")) {
				    defaultIcon = defaultIcon.replace("http://", "https://");
				}
				defaultIcon = defaultIcon + "?t=" + System.currentTimeMillis();
				
				model.addAttribute("ogImage", defaultIcon);
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
        // 🌟 [추가] 기본 이미지에도 HTTPS 대응 및 캐시 버스팅 적용
        String defaultIcon = baseUrl + "/resources/img/icon.png";
        if (defaultIcon.startsWith("http://witch-hunting.com")) {
            defaultIcon = defaultIcon.replace("http://", "https://");
        }
        defaultIcon = defaultIcon + "?t=" + System.currentTimeMillis();
        
        model.addAttribute("ogImage", defaultIcon);
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
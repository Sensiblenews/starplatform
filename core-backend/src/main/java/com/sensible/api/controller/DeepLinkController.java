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

import com.sensible.admin.service.SuperAdminService;
import com.sensible.api.service.LandingVisitService;
import com.sensible.api.service.SuperAppService;

@Controller
public class DeepLinkController {

	@Resource(name = "superAppService")
	private SuperAppService superAppService;

	@Resource(name = "landingVisitService")
	private LandingVisitService landingVisitService;

	// 약관/개인정보처리방침 DB 렌더링용 (getPolicyList 재사용)
	@Resource(name = "superAdminService")
	private SuperAdminService superAdminService;

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

	// HTML 특수문자 이스케이프 (JSP에 미리보기 텍스트를 출력하기 전 서버에서 처리)
	private String escapeHtml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	// 관련 콘텐츠 카드용 요약: 본문 앞부분만 코드포인트 기준으로 잘라 이스케이프해 반환
	private String snippet(String body, int maxCodePoints) {
		if (body == null) return "";
		String trimmed = body.trim();
		if (trimmed.isEmpty()) return "";
		int total = trimmed.codePointCount(0, trimmed.length());
		if (total <= maxCodePoints) {
			return escapeHtml(trimmed);
		}
		int endIndex = trimmed.offsetByCodePoints(0, maxCodePoints);
		return escapeHtml(trimmed.substring(0, endIndex)) + "...";
	}

	// JSON-LD description용: 본문 앞부분을 코드포인트 기준으로 잘라 반환 (이스케이프는 escapeJson에서 별도 수행)
	private String cutPlain(String s, int maxCodePoints) {
		if (s == null) return "";
		String trimmed = s.trim();
		int total = trimmed.codePointCount(0, trimmed.length());
		if (total <= maxCodePoints) return trimmed;
		return trimmed.substring(0, trimmed.offsetByCodePoints(0, maxCodePoints)) + "...";
	}

	// JSON 문자열 이스케이프. '/'를 '\/'로 바꿔 본문에 '</script>'가 있어도 script 태그가 조기 종료되지 않게 한다
	private String escapeJson(String s) {
		if (s == null) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"': sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '/': sb.append("\\/"); break;
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				default:
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}

	// 웹 랜딩 하단 관련 콘텐츠 카드(최근 게시물 최대 20건) 모델 구성.
	// AdSense 정책(콘텐츠 없는 화면 광고 금지) 대응: 페이지당 콘텐츠량과 내부 링크를 늘린다.
	private void addRelatedPosts(Model model, String starId, String excludeConId, String baseUrl) {
		List<Map<String, Object>> posts = superAppService.getRecentStarPosts(starId, excludeConId);
		List<Map<String, Object>> cards = new java.util.ArrayList<>();
		for (Map<String, Object> post : posts) {
			Map<String, Object> card = new HashMap<>();
			card.put("conId", post.get("CON_ID"));
			card.put("snippet", snippet((String) post.get("CON_BODY"), 90));

			String image = (String) (post.get("THUMB_URL") != null ? post.get("THUMB_URL") : post.get("MEDIA_URL"));
			if (image != null && image.startsWith("/")) {
				image = baseUrl + image;
			}
			card.put("image", image != null ? image : "");
			cards.add(card);
		}
		model.addAttribute("relatedPosts", cards);
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
	// "/witch/..." 매핑은 하위호환용: ROOT 컨텍스트 단독 배포에서도 기존에 공유된 /witch/... 링크가 404가 되지 않게 한다
	@RequestMapping(value = { "/post/{id}", "/star/{id}", "/feed-detail/{id}",
			"/witch/post/{id}", "/witch/star/{id}", "/witch/feed-detail/{id}" })
	public String deeplinkTrampoline(@PathVariable String id, HttpServletRequest request, Model model) {
		System.out.println("DeepLink Triggered");

		String uri = request.getRequestURI();
		String baseUrl = getBaseUrl(request);
		
		String userAgent = request.getHeader("User-Agent");
		boolean isFacebookBot = userAgent != null && userAgent.toLowerCase().contains("facebookexternalhit");

		// 기본은 기존 트램폴린 유지, 콘텐츠 조회에 성공하면 웹 랜딩(미리보기 + 광고)으로 전환
		String view = "/common/deeplink_redirect";

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

					// 🌟 웹 랜딩 미리보기 데이터 (앱 미설치 유저용)
					Object starNameObj = starInfo.get("name");
					model.addAttribute("landingType", "star");
					model.addAttribute("previewTitle", escapeHtml(starNameObj != null ? starNameObj.toString() : "StarPlatform"));
					model.addAttribute("previewMeta",
							"Global Rank #" + starInfo.get("GLOBAL_RANK") + " | Visitors " + starInfo.get("viewCount"));
					model.addAttribute("previewImage", imageUrl != null ? imageUrl : "");

					// 🌟 canonical: 레거시(/witch/star/..)·중복 경로가 모두 대표 URL 하나로 수렴하게 한다
					String canonicalUrl = baseUrl + "/star/" + id;
					model.addAttribute("canonicalUrl", canonicalUrl);

					// 🌟 JSON-LD(ProfilePage) 서버 렌더링: Googlebot이 소스보기만으로 읽을 수 있어야 함
					String starNameRaw = starNameObj != null ? starNameObj.toString() : "StarPlatform";
					StringBuilder ld = new StringBuilder();
					ld.append("{\"@context\":\"https://schema.org\",\"@type\":\"ProfilePage\"");
					ld.append(",\"mainEntity\":{\"@type\":\"Person\",\"name\":\"").append(escapeJson(starNameRaw)).append("\"");
					if (imageUrl != null && !imageUrl.isEmpty()) {
						ld.append(",\"image\":\"").append(escapeJson(imageUrl)).append("\"");
					}
					ld.append("}");
					ld.append(",\"description\":\"").append(escapeJson(
							"Global Rank #" + starInfo.get("GLOBAL_RANK") + " | Visitors " + starInfo.get("viewCount"))).append("\"");
					ld.append(",\"url\":\"").append(escapeJson(canonicalUrl)).append("\"}");
					model.addAttribute("jsonLd", ld.toString());

					// 🌟 방문 카운트 토큰: 실제 브라우저에게만 발급 (크롤러는 OG만 수집하고 카운트 제외)
					// 경로 변수 id가 곧 starId이므로 그대로 바인딩한다
					if (!LandingVisitService.isCrawler(userAgent)) {
						model.addAttribute("visitToken", landingVisitService.issueToken(id));
					}
					// 🌟 관련 콘텐츠 카드: 이 스타의 최근 게시물 2건 (AdSense Thin Content 대응)
					addRelatedPosts(model, id, null, baseUrl);
					view = "/common/content_landing";
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
					String fullBody = (String) content.get("CON_BODY"); // 글 본문 원문 (랜딩 미리보기 컷 용도)

					// 💡 OG 설명용: 글 내용이 너무 길면 미리보기 카드가 깨질 수 있으므로 적당히 자름
					String body = fullBody;
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

					// 🌟 웹 랜딩 본문: 전체 노출 (AdSense '콘텐츠 없는 화면 광고' 정책 위반 판정에 따라
					// 기존 50% 컷 + 프리뷰 잠금을 제거하고 전문을 내려준다 — 클라이언트 확정)
					model.addAttribute("landingType", "post");
					model.addAttribute("previewTitle",
							escapeHtml(starName != null ? starName + "'s Post" : "StarPlatform Post"));
					model.addAttribute("previewBody", escapeHtml(fullBody != null ? fullBody.trim() : ""));
					// 첨부 미디어가 없으면 히어로 이미지를 렌더링하지 않도록 빈 값 전달 (기본 아이콘은 OG 전용)
					model.addAttribute("previewImage", (medias != null && !medias.isEmpty()) ? imageUrl : "");

					// 🌟 canonical: /feed-detail/·/witch/post/ 등 중복 경로가 대표 URL(/post/{id}) 하나로 수렴하게 한다
					String canonicalUrl = baseUrl + "/post/" + id;
					model.addAttribute("canonicalUrl", canonicalUrl);

					// 🌟 JSON-LD(BlogPosting) 서버 렌더링: Googlebot이 소스보기만으로 읽을 수 있어야 함
					String createdDate = content.get("CREATED_DATE") != null ? String.valueOf(content.get("CREATED_DATE")) : "";
					String datePublished = createdDate.length() >= 10 ? createdDate.substring(0, 10) : "";
					StringBuilder ld = new StringBuilder();
					ld.append("{\"@context\":\"https://schema.org\",\"@type\":\"BlogPosting\"");
					ld.append(",\"headline\":\"").append(escapeJson(
							starName != null ? starName + "'s Post" : "StarPlatform Post")).append("\"");
					if (!datePublished.isEmpty()) {
						ld.append(",\"datePublished\":\"").append(escapeJson(datePublished)).append("\"");
					}
					ld.append(",\"author\":{\"@type\":\"Person\",\"name\":\"").append(escapeJson(
							starName != null ? starName : "StarPlatform")).append("\"}");
					if (medias != null && !medias.isEmpty()) {
						ld.append(",\"image\":[\"").append(escapeJson(imageUrl)).append("\"]");
					}
					ld.append(",\"description\":\"").append(escapeJson(cutPlain(fullBody, 150))).append("\"");
					ld.append(",\"publisher\":{\"@type\":\"Organization\",\"name\":\"StarPlatform\"")
							.append(",\"logo\":{\"@type\":\"ImageObject\",\"url\":\"")
							.append(escapeJson(baseUrl + "/resources/img/icon.png")).append("\"}}");
					ld.append(",\"mainEntityOfPage\":\"").append(escapeJson(canonicalUrl)).append("\"}");
					model.addAttribute("jsonLd", ld.toString());

					// 🌟 방문 카운트 토큰: 스타 피드만 작성자 스타에게 귀속해 발급.
					// 관리자 공지(FEED_TYPE='ADMIN')는 PRS_ID 자리에 ADMIN_ID가 담겨 있어 스타 귀속이 불가하므로 카운트 제외
					Object feedPrsId = content.get("PRS_ID");
					if ("STAR".equals(content.get("FEED_TYPE")) && feedPrsId != null) {
						if (!LandingVisitService.isCrawler(userAgent)) {
							model.addAttribute("visitToken", landingVisitService.issueToken(String.valueOf(feedPrsId)));
						}
						// 🌟 관련 콘텐츠 카드: 같은 스타의 다른 게시물 2건 (현재 글 제외)
						addRelatedPosts(model, String.valueOf(feedPrsId), String.valueOf(content.get("CON_ID")), baseUrl);
					}
					view = "/common/content_landing";
				}
			}
			// 🌟 3. 그 외 알 수 없는 링크일 경우 기본값
			else {
				model.addAttribute("ogTitle", "StarPlatform SuperApp");
				
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

		// 🌟 4. 콘텐츠 조회 성공 시 웹 랜딩(content_landing), 실패·미지원 링크는 기존 트램폴린(deeplink_redirect)
		return view;
	}
	
	// 🌟 루트: 서브 라우트 없이 진입한 전원(크롤러 포함)에게 콘텐츠 허브를 서버 렌더링한다.
	// UA별 분기(PC=마케팅, 봇·모바일=빈 트램폴린)는 클로킹 오해 소지가 있고
	// AdSense 심사 크롤러가 빈 페이지를 보게 되는 원인이라 제거 — 클라이언트 확정.
	// 앱 전환은 자동 실행 없이 Open in App 버튼 클릭으로만 시도한다.
	@RequestMapping(value = "/")
	public String rootHub(HttpServletRequest request, Model model) {
		String baseUrl = getBaseUrl(request);
		model.addAttribute("canonicalUrl", baseUrl + "/");

		try {
			// 최근 포스트 카드: 크롤러가 광고·콘텐츠가 있는 /post/*를 발견하는 내부 링크 경로
			List<Map<String, Object>> posts = superAppService.getHomeRecentPosts();
			List<Map<String, Object>> postCards = new java.util.ArrayList<>();
			for (Map<String, Object> post : posts) {
				Map<String, Object> card = new HashMap<>();
				card.put("conId", post.get("CON_ID"));
				card.put("author", escapeHtml(String.valueOf(post.get("PRS_NAME"))));
				card.put("snippet", snippet((String) post.get("CON_BODY"), 90));
				String image = (String) (post.get("THUMB_URL") != null ? post.get("THUMB_URL") : post.get("MEDIA_URL"));
				card.put("image", toAbsoluteUrl(image, baseUrl));
				postCards.add(card);
			}
			model.addAttribute("recentPosts", postCards);

			// 인기 스타 카드: /star/* 내부 링크 경로
			List<Map<String, Object>> stars = superAppService.getHomeTopStars();
			List<Map<String, Object>> starCards = new java.util.ArrayList<>();
			for (Map<String, Object> star : stars) {
				Map<String, Object> card = new HashMap<>();
				card.put("id", star.get("PRS_ID"));
				card.put("name", escapeHtml(String.valueOf(star.get("PRS_NAME"))));
				card.put("image", toAbsoluteUrl((String) star.get("STORED_FILE_NM"), baseUrl));
				card.put("followerCnt", star.get("FOLLOWER_CNT"));
				starCards.add(card);
			}
			model.addAttribute("topStars", starCards);
		} catch (Exception e) {
			// 목록 조회가 실패해도 허브 골격(히어로·소개·푸터)은 렌더링한다
			e.printStackTrace();
		}

		return "/common/landing";
	}

	// 🌟 sitemap.xml 동적 생성: 실존하는 스타·포스트 URL을 나열한다.
	// 기존 정적 파일은 존재하지 않는 /post/123을 담은 스텁이라 제거하고 이 매핑으로 교체 (mvc:resources 매핑도 제거)
	@RequestMapping(value = "/sitemap.xml")
	public void sitemap(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String baseUrl = getBaseUrl(request);
		List<Map<String, Object>> stars = superAppService.getSitemapStars();
		List<Map<String, Object>> posts = superAppService.getSitemapPosts();

		response.setContentType("application/xml");
		response.setCharacterEncoding("UTF-8");
		// 콘텐츠 등록 주기를 고려한 1시간 캐시 (엣지 캐시 회복 주기는 ads.txt 매핑과 동일 기준)
		response.setHeader("Cache-Control", "public, max-age=3600");
		response.getWriter().write(buildSitemapXml(baseUrl, stars, posts));
	}

	// 사이트맵 XML 조립. lastmod는 CREATED_DATE 앞 10자리(yyyy-MM-dd)만 사용
	private String buildSitemapXml(String baseUrl, List<Map<String, Object>> stars, List<Map<String, Object>> posts) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
		appendSitemapUrl(sb, baseUrl + "/", null);
		if (stars != null) {
			for (Map<String, Object> star : stars) {
				Object id = star.get("PRS_ID");
				if (id != null) {
					appendSitemapUrl(sb, baseUrl + "/star/" + id, null);
				}
			}
		}
		if (posts != null) {
			for (Map<String, Object> post : posts) {
				Object id = post.get("CON_ID");
				if (id == null) continue;
				String created = post.get("CREATED_DATE") != null ? String.valueOf(post.get("CREATED_DATE")) : "";
				String lastmod = created.length() >= 10 ? created.substring(0, 10) : null;
				appendSitemapUrl(sb, baseUrl + "/post/" + id, lastmod);
			}
		}
		sb.append("</urlset>\n");
		return sb.toString();
	}

	private void appendSitemapUrl(StringBuilder sb, String loc, String lastmod) {
		sb.append("  <url><loc>").append(escapeHtml(loc)).append("</loc>");
		if (lastmod != null && !lastmod.isEmpty()) {
			sb.append("<lastmod>").append(escapeHtml(lastmod)).append("</lastmod>");
		}
		sb.append("</url>\n");
	}

	// 상대 경로 이미지 URL을 절대 경로로 변환 (허브 카드용. null이면 빈 문자열)
	private String toAbsoluteUrl(String url, String baseUrl) {
		if (url == null || url.trim().isEmpty()) return "";
		if (url.startsWith("http://") || url.startsWith("https://")) return url;
		if (url.startsWith("/")) return baseUrl + url;
		return baseUrl + "/" + url;
	}
	
	// 🌟 2. 개인정보 처리방침 페이지 매핑
    @RequestMapping(value = "/privacy")
    public String privacyPolicy(Model model) {
        return renderPolicy(model, true);
    }

    // 🌟 3. 이용약관 페이지 매핑
    @RequestMapping(value = "/terms")
    public String termsOfService(Model model) {
        return renderPolicy(model, false);
    }

    // 약관/개인정보처리방침을 DB(WH_CONTENT, CON_TYPE=7)에서 렌더링한다.
    // 글로벌 어드민 '약관 수정' 탭에서 저장하면 앱(/app/policyDetail)과 이 웹 화면에 동시 반영된다.
    // DB 조회 실패·해당 행 없음이면 기존 하드코딩 JSP로 폴백해 항상 무언가는 보여준다.
    private String renderPolicy(Model model, boolean isPrivacy) {
        String fallbackView = isPrivacy ? "/common/privacy-policy" : "/common/terms";
        try {
            List<Map<String, Object>> policies = superAdminService.getPolicyList();
            if (policies == null || policies.isEmpty()) {
                return fallbackView;
            }

            Map<String, Object> matched = null;
            for (Map<String, Object> row : policies) {
                String title = row.get("CON_TITLE") != null ? row.get("CON_TITLE").toString() : "";
                String lower = title.toLowerCase();
                boolean looksPrivacy = lower.contains("privacy") || title.contains("개인정보");
                if (looksPrivacy == isPrivacy) {
                    matched = row;
                    break;
                }
            }
            if (matched == null || matched.get("CON_BODY") == null
                    || matched.get("CON_BODY").toString().trim().isEmpty()) {
                return fallbackView;
            }

            model.addAttribute("policyTitle", escapeHtml(String.valueOf(matched.get("CON_TITLE"))));
            model.addAttribute("policyBody", matched.get("CON_BODY"));
            model.addAttribute("policyUpdated", matched.get("CON_UDATE"));
            return "/common/policy_view";
        } catch (Exception e) {
            e.printStackTrace();
            return fallbackView;
        }
    }
}

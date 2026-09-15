package com.sensible.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * 공개 웹 페이지(루트 허브·포스트 목록)에서 공통으로 쓰는 뷰 가공 헬퍼.
 *
 * 원래 DeepLinkController 안의 private 헬퍼였는데, 포스트 목록 페이지(/posts)가
 * 같은 카드를 그리게 되면서 두 컨트롤러가 나눠 쓴다. 카드 모양이 달라지면
 * 여기 한 곳만 고치면 되도록 모아 둔다. DeepLinkController 의 동명 헬퍼들은
 * 이 클래스로 넘기는 얇은 위임이다.
 */
public final class WebCardUtil {

	private WebCardUtil() {
	}

	/**
	 * 절대 URL의 기준이 되는 오리진.
	 * 로컬 개발 주소가 아니면 운영 도메인으로 고정한다 — 프록시 뒤에서 request 의
	 * serverName 이 내부 호스트로 잡히면 공유 링크·OG 이미지가 전부 깨지기 때문이다.
	 */
	public static String getBaseUrl(HttpServletRequest request) {
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

	/** HTML 특수문자 이스케이프 (JSP에 출력하기 전 서버에서 처리) */
	public static String escapeHtml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	/** 카드용 요약: 본문 앞부분만 코드포인트 기준으로 잘라 이스케이프해 반환 */
	public static String snippet(String body, int maxCodePoints) {
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

	/** 스타 직군 코드 -> 카드에 노출할 영문 라벨. 미분류(GENERAL)와 미지정은 빈 문자열로 돌려 뱃지를 숨긴다 */
	public static String categoryLabel(Object rawCode) {
		if (rawCode == null) return "";
		String code = String.valueOf(rawCode);
		if ("STAR".equals(code)) return "Star";
		if ("CELEB".equals(code)) return "Celebrity";
		if ("BRAND".equals(code)) return "Brand";
		if ("ORG".equals(code)) return "Organization";
		if ("UNIV".equals(code)) return "University";
		if ("CITY".equals(code)) return "City";
		if ("MEDIA".equals(code)) return "Media";
		return "";
	}

	/**
	 * 썸네일 대체 텍스트. 본문이 있으면 본문을, 없으면 작성자 기준 문구를 쓴다.
	 * 사진만 올라온 글이 많아 alt가 비면 크롤러가 읽을 텍스트가 사라진다.
	 */
	public static String thumbAlt(String escapedSnippet, String escapedAuthor) {
		if (escapedSnippet != null && !escapedSnippet.isEmpty()) return escapedSnippet;
		return "Photo posted by " + escapedAuthor;
	}

	/** DATETIME 앞 10자(yyyy-MM-dd)만 남긴다 */
	public static String toDatePart(Object rawDate) {
		if (rawDate == null) {
			return "";
		}
		String s = String.valueOf(rawDate);
		return s.length() >= 10 ? s.substring(0, 10) : "";
	}

	/** 상대 경로를 오리진 기준 절대 URL로 */
	public static String toAbsoluteUrl(String url, String baseUrl) {
		if (url == null || url.trim().isEmpty()) return "";
		if (url.startsWith("http://") || url.startsWith("https://")) return url;
		if (url.startsWith("/")) return baseUrl + url;
		return baseUrl + "/" + url;
	}

	/**
	 * 포스트 한 건을 카드 모델로 가공한다.
	 * 루트 허브와 /posts 목록이 같은 카드 마크업을 쓰므로 키 이름도 같아야 한다.
	 */
	public static Map<String, Object> toPostCard(Map<String, Object> post, String baseUrl) {
		Map<String, Object> card = new HashMap<>();
		String author = escapeHtml(String.valueOf(post.get("PRS_NAME")));
		String body = snippet((String) post.get("CON_BODY"), 90);

		card.put("conId", post.get("CON_ID"));
		card.put("starId", post.get("PRS_ID"));
		card.put("author", author);
		card.put("snippet", body);
		// 본문이 한 줄뿐인 사진 글이 대부분이라, 카드가 읽을 거리를 갖도록 부가 정보를 함께 내린다
		card.put("date", toDatePart(post.get("CREATED_DATE")));
		card.put("category", categoryLabel(post.get("STAR_CATEGORY")));
		card.put("likeCnt", post.get("LIKE_CNT"));
		card.put("commentCnt", post.get("COMMENT_CNT"));
		card.put("followerCnt", post.get("FOLLOWER_CNT"));
		card.put("mediaCnt", post.get("MEDIA_CNT"));
		card.put("alt", thumbAlt(body, author));

		String image = (String) (post.get("THUMB_URL") != null ? post.get("THUMB_URL") : post.get("MEDIA_URL"));
		card.put("image", toAbsoluteUrl(image, baseUrl));
		return card;
	}

	/** 포스트 목록을 카드 목록으로 */
	public static List<Map<String, Object>> toPostCards(List<Map<String, Object>> posts, String baseUrl) {
		List<Map<String, Object>> cards = new ArrayList<>();
		if (posts == null) {
			return cards;
		}
		for (Map<String, Object> post : posts) {
			cards.add(toPostCard(post, baseUrl));
		}
		return cards;
	}
}

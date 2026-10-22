package com.sensible.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	/**
	 * 링크로 인정하는 최상위 도메인 — 앱의 core-frontend/src/app/pipes/linkify.ts(LINKIFY_TLDS)와 동기화.
	 * 아무 "단어.단어"나 잡으면 파일명(photo.jpg)·버전(v2.0)·약어(e.g.)까지 링크가 되므로 자주 쓰는 것만 둔다.
	 * 2단계 국가 도메인(co.kr, co.jp …)은 앞 라벨 반복으로 잡히므로 국가 코드만 있으면 된다.
	 */
	static final String[] LINKIFY_TLDS = {
		// 일반
		"com", "net", "org", "edu", "gov", "mil", "int", "info", "biz", "name", "pro", "mobi",
		"io", "co", "me", "tv", "ai", "app", "dev", "page", "xyz", "site", "online", "store", "shop",
		"blog", "news", "live", "link", "club", "tech", "media", "world", "today", "space", "website",
		"ly", "gl", "gg", "to", "fm", "cc", "ws",
		// 국가
		"kr", "jp", "cn", "tw", "hk", "sg", "vn", "th", "ph", "id", "my", "in", "au", "nz",
		"us", "ca", "mx", "br", "ar", "cl", "uk", "ie", "de", "fr", "it", "es", "pt", "nl", "be",
		"ch", "at", "se", "no", "dk", "fi", "pl", "cz", "ru", "ua", "tr", "gr", "il", "ae", "sa", "za", "eu",
	};

	/**
	 * 주소 패턴 — linkify.ts 의 LINK_RE 와 동기화.
	 * 1) http(s):// 로 시작하는 주소  2) www. 로 시작하는 주소
	 * 3) 앞이 영문자·숫자·@·.·/·- 가 아닌 자리에서 시작하는 "라벨.라벨.최상위도메인[:포트][/경로]" (이메일·경로 중간은 제외)
	 * 앱과 같은 결과를 내도록 lookbehind 대신 앞 글자를 그룹(2)으로 잡아 그대로 돌려준다.
	 * 입력이 escape 된 문자열이라 '<' 는 '&lt;' 로 들어오며, 그 엔티티는 아래 TRAILING_ENTITY 로 주소에서 뗀다.
	 */
	private static final Pattern LINK_RE = Pattern.compile(
			"(https?://[^\\s<\\u00A0]+)"
			+ "|(^|[^\\w@./-])("
			+ "www\\.[^\\s<\\u00A0]+"
			+ "|(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:" + String.join("|", LINKIFY_TLDS) + ")(?::\\d{1,5})?(?:[/?#][^\\s<\\u00A0]*)?(?![\\w-])"
			+ ")",
			Pattern.CASE_INSENSITIVE);

	/** 주소 뒤에 붙은 따옴표·꺾쇠의 escape 엔티티 — 여기서부터는 주소가 아니다 */
	private static final Pattern TRAILING_ENTITY = Pattern.compile("&(quot|#39|lt|gt);");

	/** 문장 끝 구두점·닫는 괄호 — 주소에서 뺀다 ("...example.com." → 링크는 example.com) */
	private static final Pattern TRAILING_PUNCT = Pattern.compile("[.,!?;:)\\]]+$");

	/**
	 * 이미 escape 된 본문·소개문 안의 주소를 새 탭으로 열리는 링크로 바꾼다 (2026-10-23, 앱 linkifyText 와 같은 규칙).
	 *
	 * 입력은 반드시 {@link #escapeHtml(String)} 을 거친 문자열이어야 한다. 이 메서드는 입력을 다시 escape 하지 않고
	 * 주소 구간만 {@code <a>} 로 감싸므로, 원문을 그대로 넣으면 사용자 입력이 HTML 로 실행된다.
	 * 주소 안의 '&amp;' 같은 엔티티는 href 에도 그대로 두며(속성 안에서 올바른 표기), 줄바꿈 문자는 건드리지 않는다
	 * (JSP 쪽 white-space: pre-line 이 처리).
	 *
	 * @param escapedText escapeHtml 을 거친 문자열. null 이면 빈 문자열
	 * @return 주소가 {@code <a href target="_blank" rel="noopener noreferrer nofollow">} 로 감싸진 문자열
	 */
	public static String linkify(String escapedText) {
		if (escapedText == null || escapedText.isEmpty()) return "";
		Matcher m = LINK_RE.matcher(escapedText);
		StringBuffer sb = new StringBuffer(escapedText.length() + 64);
		while (m.find()) {
			String httpUrl = m.group(1);
			String raw = httpUrl != null ? httpUrl : m.group(3);
			String prefix = httpUrl != null ? "" : (m.group(2) != null ? m.group(2) : "");

			String url = raw;
			String rest = "";
			Matcher entity = TRAILING_ENTITY.matcher(raw);
			if (entity.find()) {
				url = raw.substring(0, entity.start());
				rest = raw.substring(entity.start());
			}
			Matcher trail = TRAILING_PUNCT.matcher(url);
			if (trail.find()) {
				rest = trail.group() + rest;
				url = url.substring(0, trail.start());
			}
			if (url.isEmpty() || "www.".equalsIgnoreCase(url)) {
				m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
				continue;
			}
			String lower = url.toLowerCase();
			String href = (lower.startsWith("http://") || lower.startsWith("https://")) ? url : "https://" + url;
			String anchor = prefix
					+ "<a href=\"" + href + "\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">" + url + "</a>"
					+ rest;
			m.appendReplacement(sb, Matcher.quoteReplacement(anchor));
		}
		m.appendTail(sb);
		return sb.toString();
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
		// 모바일 홈 큰 카드의 작성자 아바타 (없으면 빈 문자열 → 회색 원)
		card.put("authorImage", toAbsoluteUrl((String) post.get("STORED_FILE_NM"), baseUrl));

		String image = (String) (post.get("THUMB_URL") != null ? post.get("THUMB_URL") : post.get("MEDIA_URL"));
		card.put("image", toAbsoluteUrl(image, baseUrl));
		// 2-31차: 첫 미디어가 영상이면 원본 주소를 따로 내린다 (image 는 그대로 포스터 썸네일)
		String video = videoUrl(post, baseUrl);
		if (!video.isEmpty()) {
			card.put("video", video);
		}
		return card;
	}

	/**
	 * 미디어 행이 영상이면 원본 영상의 절대 주소, 아니면 빈 문자열.
	 * 목록 쿼리(첫 미디어 조인)와 상세의 미디어 행 모두 MEDIA_TYPE·MEDIA_URL 키가 같아 함께 쓴다.
	 */
	public static String videoUrl(Map<String, Object> media, String baseUrl) {
		if (media == null || !"VIDEO".equals(media.get("MEDIA_TYPE"))) return "";
		return toAbsoluteUrl((String) media.get("MEDIA_URL"), baseUrl);
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

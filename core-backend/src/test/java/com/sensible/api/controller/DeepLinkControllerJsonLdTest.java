package com.sensible.api.controller;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

/**
 * 웹 랜딩 JSON-LD 서버 렌더링에 쓰이는 문자열 처리 검증.
 * escapeJson/cutPlain은 컨트롤러 내부 헬퍼라 리플렉션으로 호출한다.
 */
public class DeepLinkControllerJsonLdTest {

	private final DeepLinkController controller = new DeepLinkController();

	private String escapeJson(String s) throws Exception {
		Method m = DeepLinkController.class.getDeclaredMethod("escapeJson", String.class);
		m.setAccessible(true);
		return (String) m.invoke(controller, s);
	}

	private String cutPlain(String s, int max) throws Exception {
		Method m = DeepLinkController.class.getDeclaredMethod("cutPlain", String.class, int.class);
		m.setAccessible(true);
		return (String) m.invoke(controller, s, max);
	}

	@Test
	public void escapeJson_기본_특수문자를_이스케이프한다() throws Exception {
		assertEquals("say \\\"hi\\\"", escapeJson("say \"hi\""));
		assertEquals("a\\\\b", escapeJson("a\\b"));
		assertEquals("line1\\nline2", escapeJson("line1\nline2"));
		assertEquals("tab\\there", escapeJson("tab\there"));
	}

	@Test
	public void escapeJson_슬래시를_이스케이프해_script_조기종료를_막는다() throws Exception {
		// 본문에 </script>가 있어도 <script type="application/ld+json"> 태그가 닫히면 안 됨
		String escaped = escapeJson("</script><script>alert(1)</script>");
		assertFalse(escaped.contains("</script>"));
		assertEquals("<\\/script>", escapeJson("</script>"));
	}

	@Test
	public void escapeJson_제어문자는_유니코드_이스케이프한다() throws Exception {
		assertEquals("a\\u0001b", escapeJson("a" + (char) 1 + "b"));
	}

	@Test
	public void escapeJson_null은_빈문자열을_반환한다() throws Exception {
		assertEquals("", escapeJson(null));
	}

	@Test
	public void cutPlain_길이_초과분만_말줄임_처리한다() throws Exception {
		assertEquals("짧은 글", cutPlain("짧은 글", 150));
		assertEquals("12345...", cutPlain("1234567890", 5));
		assertEquals("", cutPlain(null, 150));
	}

	private String toAbsoluteUrl(String url, String baseUrl) throws Exception {
		Method m = DeepLinkController.class.getDeclaredMethod("toAbsoluteUrl", String.class, String.class);
		m.setAccessible(true);
		return (String) m.invoke(controller, url, baseUrl);
	}

	@Test
	public void toAbsoluteUrl_상대경로만_baseUrl을_붙인다() throws Exception {
		String base = "https://witch-hunting.com";
		assertEquals("https://witch-hunting.com/img/a.jpg", toAbsoluteUrl("/img/a.jpg", base));
		assertEquals("https://witch-hunting.com/img/a.jpg", toAbsoluteUrl("img/a.jpg", base));
		assertEquals("https://cdn.example.com/a.jpg", toAbsoluteUrl("https://cdn.example.com/a.jpg", base));
		assertEquals("http://cdn.example.com/a.jpg", toAbsoluteUrl("http://cdn.example.com/a.jpg", base));
		assertEquals("", toAbsoluteUrl(null, base));
		assertEquals("", toAbsoluteUrl("  ", base));
	}

	@SuppressWarnings("unchecked")
	private String buildSitemapXml(String baseUrl, Object stars, Object posts) throws Exception {
		Method m = DeepLinkController.class.getDeclaredMethod("buildSitemapXml",
				String.class, java.util.List.class, java.util.List.class);
		m.setAccessible(true);
		return (String) m.invoke(controller, baseUrl, stars, posts);
	}

	@Test
	public void buildSitemapXml_루트_스타_포스트_URL을_나열한다() throws Exception {
		java.util.List<java.util.Map<String, Object>> stars = new java.util.ArrayList<>();
		java.util.Map<String, Object> star = new java.util.HashMap<>();
		star.put("PRS_ID", "SP-100");
		stars.add(star);

		java.util.List<java.util.Map<String, Object>> posts = new java.util.ArrayList<>();
		java.util.Map<String, Object> post = new java.util.HashMap<>();
		post.put("CON_ID", 7);
		post.put("CREATED_DATE", "2026-08-03 12:00:00.0");
		posts.add(post);

		String xml = buildSitemapXml("https://witch-hunting.com", stars, posts);
		assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
		assertTrue(xml.contains("<loc>https://witch-hunting.com/</loc>"));
		assertTrue(xml.contains("<loc>https://witch-hunting.com/star/SP-100</loc>"));
		assertTrue(xml.contains("<loc>https://witch-hunting.com/post/7</loc><lastmod>2026-08-03</lastmod>"));
		assertTrue(xml.trim().endsWith("</urlset>"));
	}

	@Test
	public void buildSitemapXml_목록이_비거나_null이어도_루트는_포함한다() throws Exception {
		String xml = buildSitemapXml("https://witch-hunting.com", null, new java.util.ArrayList<>());
		assertTrue(xml.contains("<loc>https://witch-hunting.com/</loc>"));
		assertFalse(xml.contains("/star/"));
		assertFalse(xml.contains("/post/"));
	}

	@Test
	public void cutPlain_서로게이트_쌍을_코드포인트_기준으로_자른다() throws Exception {
		// 이모지(서로게이트 쌍)가 중간에서 잘려 깨진 문자가 생기면 안 됨
		String emoji = "😀😀😀😀"; // 코드포인트 4개
		assertEquals("😀😀...", cutPlain(emoji, 2));
	}

	private String toDatePart(Object rawDate) throws Exception {
		Method m = DeepLinkController.class.getDeclaredMethod("toDatePart", Object.class);
		m.setAccessible(true);
		return (String) m.invoke(controller, rawDate);
	}

	@Test
	public void toDatePart_날짜_부분만_잘라낸다() throws Exception {
		assertEquals("2026-08-03", toDatePart("2026-08-03 12:00:00.0"));
		assertEquals("2026-08-03", toDatePart("2026-08-03"));
		// DB 드라이버가 Timestamp 등 비문자열을 돌려줘도 문자열 변환 후 앞 10자리를 쓴다
		assertEquals("2026-08-03", toDatePart(java.sql.Timestamp.valueOf("2026-08-03 12:00:00")));
	}

	@Test
	public void toDatePart_null이거나_짧으면_빈문자열() throws Exception {
		assertEquals("", toDatePart(null));
		assertEquals("", toDatePart("2026"));
		assertEquals("", toDatePart(""));
	}
}

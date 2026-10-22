package com.sensible.common.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 2026-10-23: 공개 웹 본문·소개문 자동 링크(WebCardUtil.linkify)가 앱 linkify.ts 와 같은 규칙으로 동작하는지 검증.
 * 입력은 escapeHtml 을 거친 문자열이라는 전제를 그대로 따른다.
 */
public class WebCardUtilLinkifyTest {

	private static final String ATTR = "\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">";

	/** href 와 표시 텍스트로 기대 앵커 문자열을 만든다 */
	private String a(String href, String text) {
		return "<a href=\"" + href + ATTR + text + "</a>";
	}

	private String linkify(String raw) {
		return WebCardUtil.linkify(WebCardUtil.escapeHtml(raw));
	}

	@Test
	public void https_주소는_그대로_href가_된다() {
		assertEquals("see " + a("https://example.com/a/b", "https://example.com/a/b") + " now",
				linkify("see https://example.com/a/b now"));
	}

	@Test
	public void http_주소도_스킴을_유지한다() {
		assertEquals(a("http://example.com", "http://example.com"), linkify("http://example.com"));
	}

	@Test
	public void www_주소는_https를_붙인다() {
		assertEquals("go " + a("https://www.naver.com/x", "www.naver.com/x"),
				linkify("go www.naver.com/x"));
	}

	@Test
	public void 맨_도메인과_경로를_잡는다() {
		assertEquals("visit " + a("https://example.com/path", "example.com/path") + " today",
				linkify("visit example.com/path today"));
	}

	@Test
	public void 이단계_국가_도메인과_포트를_잡는다() {
		assertEquals(a("https://abc.co.kr:8080/p?q=1", "abc.co.kr:8080/p?q=1"),
				linkify("abc.co.kr:8080/p?q=1"));
	}

	@Test
	public void 문장_끝_마침표_쉼표_괄호는_주소에서_뺀다() {
		assertEquals("Go to " + a("https://example.com", "example.com") + ".",
				linkify("Go to example.com."));
		assertEquals(a("https://example.com", "example.com") + ", then",
				linkify("example.com, then"));
		assertEquals("(" + a("https://example.com/x", "example.com/x") + ")",
				linkify("(example.com/x)"));
	}

	@Test
	public void 따옴표로_감싼_주소는_엔티티_앞까지만_링크다() {
		assertEquals("&quot;" + a("https://naver.com", "naver.com") + "&quot;",
				linkify("\"naver.com\""));
	}

	@Test
	public void escape된_앰퍼샌드가_든_쿼리스트링은_href에_그대로_남는다() {
		String url = "https://example.com/s?a=1&b=2";
		String escaped = "https://example.com/s?a=1&amp;b=2";
		assertEquals(a(escaped, escaped), linkify(url));
	}

	@Test
	public void 이메일_주소는_링크가_아니다() {
		assertEquals("mail a@b.com please", linkify("mail a@b.com please"));
	}

	@Test
	public void 목록에_없는_접미사는_링크가_아니다() {
		assertEquals("foo.bar and photo.jpg v2.0", linkify("foo.bar and photo.jpg v2.0"));
	}

	@Test
	public void 경로_중간이나_TLD_뒤에_글자가_이어지는_도메인은_잡지_않는다() {
		assertEquals("a/example.com", linkify("a/example.com"));
		assertEquals("-example.com", linkify("-example.com"));
		assertEquals("example.community", linkify("example.community"));
	}

	@Test
	public void 줄바꿈은_그대로_보존한다() {
		assertEquals("first " + a("https://example.com", "example.com") + "\nsecond\r\n" + a("https://www.x.org", "www.x.org"),
				linkify("first example.com\nsecond\r\nwww.x.org"));
	}

	@Test
	public void 태그_문자는_escape_상태로_남고_HTML로_실행되지_않는다() {
		String out = linkify("<script>alert(1)</script> example.com");
		assertEquals("&lt;script&gt;alert(1)&lt;/script&gt; " + a("https://example.com", "example.com"), out);
		assertFalse(out.contains("<script>"));
	}

	@Test
	public void 꺾쇠_뒤에_붙은_주소는_엔티티_앞에서_끊는다() {
		assertEquals("&lt;" + a("https://example.com", "https://example.com") + "&gt;",
				linkify("<https://example.com>"));
	}

	@Test
	public void 대소문자를_가리지_않는다() {
		assertEquals(a("HTTPS://Example.COM/A", "HTTPS://Example.COM/A"), linkify("HTTPS://Example.COM/A"));
		assertEquals(a("https://Naver.Com", "Naver.Com"), linkify("Naver.Com"));
	}

	@Test
	public void null과_빈문자열은_빈문자열() {
		assertEquals("", WebCardUtil.linkify(null));
		assertEquals("", WebCardUtil.linkify(""));
	}

	@Test
	public void TLD_목록은_앱과_같은_개수를_가진다() {
		// linkify.ts 의 LINKIFY_TLDS 와 동기화 — 한쪽만 바꾸면 여기서 걸린다 (2026-10-23 기준 89개)
		assertEquals(89, WebCardUtil.LINKIFY_TLDS.length);
	}
}

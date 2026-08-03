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

	@Test
	public void cutPlain_서로게이트_쌍을_코드포인트_기준으로_자른다() throws Exception {
		// 이모지(서로게이트 쌍)가 중간에서 잘려 깨진 문자가 생기면 안 됨
		String emoji = "😀😀😀😀"; // 코드포인트 4개
		assertEquals("😀😀...", cutPlain(emoji, 2));
	}
}

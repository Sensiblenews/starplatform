package com.sensible.admin;

import static org.junit.Assert.*;

import org.junit.Test;

import com.sensible.admin.service.SuperAdminService;

/**
 * 약관 본문 출력 정규화 규칙 검증.
 * 어드민에서 평문을 붙여넣어도 앱·웹에서 줄바꿈·들여쓰기가 유지되어야 한다.
 * HTML로 작성된 기존 본문(개인정보처리방침 등)은 손대지 않는다.
 */
public class SuperAdminServicePolicyBodyTest {

	@Test
	public void 평문의_줄바꿈은_br로_변환된다() {
		String result = SuperAdminService.renderPolicyBody("1. Purpose\nThis is the body.");
		assertEquals("1. Purpose<br>This is the body.", result);
	}

	@Test
	public void 줄_앞_들여쓰기는_nbsp로_유지된다() {
		String result = SuperAdminService.renderPolicyBody("Title\n    indented");
		assertEquals("Title<br>&nbsp;&nbsp;&nbsp;&nbsp;indented", result);
	}

	@Test
	public void 빈_줄도_유지되어_문단_간격이_남는다() {
		String result = SuperAdminService.renderPolicyBody("first\n\nsecond");
		assertEquals("first<br><br>second", result);
	}

	@Test
	public void 윈도우_줄바꿈도_같은_규칙으로_처리한다() {
		assertEquals("a<br>b", SuperAdminService.renderPolicyBody("a\r\nb"));
		assertEquals("a<br>b", SuperAdminService.renderPolicyBody("a\rb"));
	}

	@Test
	public void 평문에_포함된_꺾쇠는_이스케이프된다() {
		// 태그로 오인될 문자열이 아니어야 화면에 그대로 보인다
		assertEquals("5 &lt; 10 &amp;&amp; 10 &gt; 5",
				SuperAdminService.renderPolicyBody("5 < 10 && 10 > 5"));
	}

	@Test
	public void HTML로_작성된_본문은_그대로_통과한다() {
		String html = "<p>Hello</p>\n<h2>Section</h2>";
		assertEquals(html, SuperAdminService.renderPolicyBody(html));
	}

	@Test
	public void br만_들어간_본문도_HTML로_보고_통과시킨다() {
		String html = "line one<br>line two";
		assertEquals(html, SuperAdminService.renderPolicyBody(html));
	}

	@Test
	public void null과_빈_본문은_빈_문자열이_된다() {
		assertEquals("", SuperAdminService.renderPolicyBody(null));
		assertEquals("", SuperAdminService.renderPolicyBody("   "));
	}
}

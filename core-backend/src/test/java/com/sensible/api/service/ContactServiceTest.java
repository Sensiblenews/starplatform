package com.sensible.api.service;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.sensible.api.service.ContactService.Type;

/**
 * 문의 폼 입력 정리·유형 검증.
 *
 * submit()은 DAO·Redis·메일에 의존해 단위 테스트 대상이 아니다.
 * 부수효과 없는 판정부(sanitize, isKnownType)만 여기서 검증한다.
 */
public class ContactServiceTest {

	@Test
	public void script_태그는_내용까지_통째로_제거한다() {
		// 태그만 벗기면 스크립트 본문이 그대로 남는다
		assertEquals("hello", ContactService.sanitize("hello<script>alert('x')</script>"));
		assertEquals("hello", ContactService.sanitize("hello<SCRIPT SRC=\"//evil\">a</SCRIPT>"));
		assertEquals("hello", ContactService.sanitize("hello<style>body{}</style>"));
	}

	@Test
	public void 여러_줄에_걸친_script도_제거한다() {
		String input = "before<script>\nvar a = 1;\nalert(a);\n</script>after";
		String cleaned = ContactService.sanitize(input);
		assertFalse(cleaned.contains("alert"));
		assertTrue(cleaned.startsWith("before"));
		assertTrue(cleaned.endsWith("after"));
	}

	@Test
	public void 남은_태그는_벗기고_텍스트는_남긴다() {
		assertEquals("bold text", ContactService.sanitize("<b>bold</b> text"));
		assertEquals("click", ContactService.sanitize("<a href=\"http://x\" onclick=\"y()\">click</a>"));
		// 닫히지 않은 태그도 텍스트로 새어 나오지 않게 한다
		assertEquals("", ContactService.sanitize("<img src=x onerror=alert(1)"));
	}

	@Test
	public void 줄바꿈은_살리고_제어문자만_지운다() {
		// 여러 문단으로 쓴 문의가 한 줄로 뭉개지면 읽기 어렵다
		String cleaned = ContactService.sanitize("line one\n\nline two");
		assertTrue(cleaned.contains("\n"));
		assertEquals("line one\n\nline two", cleaned);

		// 제어문자는 지우지 않고 공백으로 바꾼다 — 없애면 앞뒤 단어가 붙어버린다
		assertEquals("a b", ContactService.sanitize("a\u0007b"));
	}

	@Test
	public void 앞뒤_공백을_없애고_빈_입력은_빈_문자열이_된다() {
		assertEquals("hi", ContactService.sanitize("   hi   "));
		assertEquals("", ContactService.sanitize(null));
		assertEquals("", ContactService.sanitize("   "));
	}

	@Test
	public void 목록에_있는_유형만_통과한다() {
		assertTrue(ContactService.isKnownType("GENERAL"));
		assertTrue(ContactService.isKnownType("COPYRIGHT"));
		assertFalse(ContactService.isKnownType("general"));
		assertFalse(ContactService.isKnownType("DROP TABLE"));
		assertFalse(ContactService.isKnownType(null));
		assertFalse(ContactService.isKnownType(""));
	}

	@Test
	public void 유형_코드는_중복되지_않고_라벨이_모두_있다() {
		Set<String> codes = new HashSet<String>();
		for (Type type : ContactService.types()) {
			assertTrue("유형 코드 중복: " + type.getCode(), codes.add(type.getCode()));
			assertFalse(type.getLabel().trim().isEmpty());
		}
		// 푸터가 ?type= 으로 미리 골라주는 두 유형은 반드시 있어야 한다
		assertTrue(codes.contains("ACCOUNT"));
		assertTrue(codes.contains("PARTNERSHIP"));
	}
}

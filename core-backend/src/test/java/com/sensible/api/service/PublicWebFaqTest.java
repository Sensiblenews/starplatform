package com.sensible.api.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.sensible.api.service.PublicWebFaq.Item;
import com.sensible.api.service.PublicWebFaq.Section;

/**
 * 공개 웹 FAQ 데이터 검증.
 *
 * 핵심은 "화면에 보이는 문항과 FAQPage 구조화 데이터가 같은가"다. 둘이 어긋나면
 * 구조화 데이터 스팸으로 판정될 수 있어, 한쪽만 고치는 실수를 여기서 잡는다.
 */
public class PublicWebFaqTest {

	@Test
	public void 모든_문항이_JSON_LD에_그대로_들어간다() {
		String json = PublicWebFaq.toJsonLd(PublicWebFaq.sections());

		for (Section section : PublicWebFaq.sections()) {
			for (Item item : section.getItems()) {
				assertTrue("질문이 구조화 데이터에 없다: " + item.getQuestion(),
					json.contains(PublicWebFaq.escapeJson(item.getQuestion())));
				assertTrue("답변이 구조화 데이터에 없다: " + item.getQuestion(),
					json.contains(PublicWebFaq.escapeJson(item.getAnswer())));
			}
		}
	}

	@Test
	public void JSON_LD의_문항_수가_화면_문항_수와_같다() {
		int visible = 0;
		for (Section section : PublicWebFaq.sections()) {
			visible += section.getItems().size();
		}

		// "@type":"Question" 등장 횟수 = 문항 수
		int inJson = countOccurrences(PublicWebFaq.toJsonLd(PublicWebFaq.sections()), "\"@type\":\"Question\"");
		assertEquals(visible, inJson);
	}

	@Test
	public void JSON_LD는_FAQPage로_시작해_유효한_형태로_닫힌다() {
		String json = PublicWebFaq.toJsonLd(PublicWebFaq.sections());
		assertTrue(json.startsWith("{\"@context\":\"https://schema.org\",\"@type\":\"FAQPage\""));
		assertTrue(json.endsWith("]}"));
	}

	@Test
	public void 슬래시를_이스케이프해_script_태그가_조기_종료되지_않는다() {
		// 답변에 </script> 가 우연히 들어가도 스크립트 블록이 거기서 끊기면 안 된다
		assertEquals("a<\\/script>b", PublicWebFaq.escapeJson("a</script>b"));
		assertFalse(PublicWebFaq.toJsonLd(PublicWebFaq.sections()).contains("</script>"));
	}

	@Test
	public void 큰따옴표와_역슬래시를_이스케이프한다() {
		assertEquals("say \\\"hi\\\"", PublicWebFaq.escapeJson("say \"hi\""));
		assertEquals("a\\\\b", PublicWebFaq.escapeJson("a\\b"));
		assertEquals("", PublicWebFaq.escapeJson(null));
	}

	@Test
	public void 섹션_id는_중복되지_않는다() {
		// id는 목차와 푸터가 앵커로 쓴다. 겹치면 엉뚱한 위치로 점프한다
		Set<String> seen = new HashSet<String>();
		for (Section section : PublicWebFaq.sections()) {
			assertTrue("섹션 id 중복: " + section.getId(), seen.add(section.getId()));
			assertFalse("섹션 id가 비었다", section.getId().isEmpty());
		}
	}

	@Test
	public void 푸터가_링크하는_앵커가_실제로_존재한다() {
		// web-footer.jsp 가 /faq#safety, #advertising, #content-policy 로 보낸다.
		// 앵커를 지우면 푸터 링크가 FAQ 맨 위로 떨어진다
		List<String> anchors = new ArrayList<String>();
		for (Section section : PublicWebFaq.sections()) {
			anchors.add(section.getId());
			for (Item item : section.getItems()) {
				if (item.getId() != null) {
					anchors.add(item.getId());
				}
			}
		}

		assertTrue(anchors.contains("safety"));
		assertTrue(anchors.contains("advertising"));
		assertTrue(anchors.contains("content-policy"));
	}

	@Test
	public void 모든_문항에_질문과_답변이_채워져_있다() {
		for (Section section : PublicWebFaq.sections()) {
			assertFalse("빈 섹션: " + section.getId(), section.getItems().isEmpty());
			for (Item item : section.getItems()) {
				assertFalse(item.getQuestion().trim().isEmpty());
				// 한 문장짜리 답변은 저품질 콘텐츠로 잡힌다. 최소 길이를 지켜본다
				assertTrue("답변이 너무 짧다: " + item.getQuestion(), item.getAnswer().length() >= 80);
			}
		}
	}

	private int countOccurrences(String haystack, String needle) {
		int count = 0;
		int from = 0;
		while (true) {
			int at = haystack.indexOf(needle, from);
			if (at < 0) {
				return count;
			}
			count++;
			from = at + needle.length();
		}
	}
}

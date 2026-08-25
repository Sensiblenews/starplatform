package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.sensible.api.service.MediaAccessService.Grant;

/**
 * 검수 대기 이미지 접근 토큰 검증 (2-26차).
 *
 * 이 토큰은 &lt;img src&gt;에 그대로 실려 나가므로 위조·바꿔치기·재사용에 견뎌야 한다.
 * 다만 토큰이 유효해도 실제 노출 여부는 호출부가 DB 상태를 다시 보고 정한다 —
 * 여기서 검증하는 것은 "이 토큰이 우리가 발급한 것이고 아직 만료되지 않았는가"까지다.
 */
public class MediaAccessServiceTest {

	private MediaAccessService service;

	@Before
	public void setUp() {
		service = new MediaAccessService();
	}

	@Test
	public void 발급한_토큰은_같은_대상으로_되돌아온다() {
		String token = service.issueToken("STAR_FEED", "123");

		Grant grant = service.verify(token);

		assertNotNull(grant);
		assertEquals("STAR_FEED", grant.targetType);
		assertEquals("123", grant.targetId);
	}

	@Test
	public void 회원_콘텐츠와_스타_피드를_구분한다() {
		Grant member = service.verify(service.issueToken("MEMBER_CONTENT", "5"));
		Grant star = service.verify(service.issueToken("STAR_FEED", "5"));

		assertEquals("MEMBER_CONTENT", member.targetType);
		assertEquals("STAR_FEED", star.targetType);
	}

	@Test
	public void 서명을_고치면_거부한다() {
		String token = service.issueToken("STAR_FEED", "123");
		String tampered = token.substring(0, token.indexOf('.') + 1) + "AAAAAAAAAAAAAAAAAAAAAA";

		assertNull(service.verify(tampered));
	}

	@Test
	public void 페이로드를_다른_게시물로_바꿔치기하면_거부한다() {
		// 남의 게시물 ID로 바꿔 끼워도 서명이 맞지 않는다
		String mine = service.issueToken("STAR_FEED", "1");
		String other = service.issueToken("STAR_FEED", "999");

		String swapped = other.substring(0, other.indexOf('.'))
				+ mine.substring(mine.indexOf('.'));

		assertNull(service.verify(swapped));
	}

	@Test
	public void 형식이_어긋나면_거부한다() {
		assertNull(service.verify(null));
		assertNull(service.verify(""));
		assertNull(service.verify("서명없음"));
		assertNull(service.verify(".onlySignature"));
		assertNull(service.verify("onlyPayload."));
	}

	@Test
	public void 만료된_토큰은_거부한다() {
		// 유효창을 넘긴 발급 시각을 직접 만들어 검증한다
		long expired = System.currentTimeMillis() - MediaAccessService.TOKEN_TTL_MS - 1000L;
		String token = forgeWithOurOwnSignature(service, "STAR_FEED", "123", expired);

		assertNull(service.verify(token));
	}

	@Test
	public void 유효창_안이면_통과한다() {
		long recent = System.currentTimeMillis() - 1000L;
		String token = forgeWithOurOwnSignature(service, "STAR_FEED", "123", recent);

		assertNotNull(service.verify(token));
	}

	@Test
	public void 대상값이_없으면_발급하지_않는다() {
		assertNull(service.issueToken(null, "1"));
		assertNull(service.issueToken("STAR_FEED", null));
	}

	@Test
	public void 구분자가_섞인_값은_발급하지_않는다() {
		// 페이로드가 어긋나 다른 대상으로 해석될 수 있다
		assertNull(service.issueToken("STAR_FEED|X", "1"));
		assertNull(service.issueToken("STAR_FEED", "1|2"));
	}

	/**
	 * 발급 시각만 바꾼 토큰을 만든다.
	 * 서명은 서비스 자신의 시크릿으로 해야 하므로 issueToken이 만든 서명 대신
	 * 같은 페이로드를 넣고 다시 발급받는 방식이 불가능하다 — 리플렉션으로 sign을 부른다.
	 */
	private static String forgeWithOurOwnSignature(MediaAccessService service,
			String targetType, String targetId, long issuedAt) {
		try {
			String payload = targetType + "|" + targetId + "|" + issuedAt;
			String payloadB64 = java.util.Base64.getUrlEncoder().withoutPadding()
					.encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

			java.lang.reflect.Method sign =
					MediaAccessService.class.getDeclaredMethod("sign", String.class);
			sign.setAccessible(true);
			return payloadB64 + "." + sign.invoke(service, payloadB64);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}

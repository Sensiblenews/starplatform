package com.sensible.api.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 2-31차 후속: 푸시 채널 id 와 iOS 배지 카운터의 순수 규칙.
 * FirebaseMessaging 은 실제 발송 없이 null 로 둔다 — 여기서 send 는 호출하지 않는다.
 */
public class FirebaseServiceTest {

	@Test
	public void 채널_id_는_소리_없는_옛_채널과_다르다() {
		assertFalse("star_visitor_channel".equals(FirebaseService.CHANNEL_VISITOR));
		assertFalse("dm_channel".equals(FirebaseService.CHANNEL_DM));
		assertFalse("500".equals(FirebaseService.CHANNEL_BROADCAST));
	}

	@Test
	public void 배지_키는_토큰별로_나뉜다() {
		assertEquals("push:badge:tokA", FirebaseService.badgeKey("tokA"));
		assertFalse(FirebaseService.badgeKey("tokA").equals(FirebaseService.badgeKey("tokB")));
	}

	@Test
	public void Redis_가_없으면_배지는_예전처럼_1이다() {
		FirebaseService svc = new FirebaseService(null);
		assertEquals(1, svc.nextBadge("tokA"));
		assertEquals(1, svc.nextBadge(null));
		assertEquals(1, svc.nextBadge(""));
	}

	@Test
	public void Redis_가_없으면_리셋은_실패로_보고한다() {
		FirebaseService svc = new FirebaseService(null);
		assertFalse(svc.resetBadge("tokA"));
		assertFalse(svc.resetBadge(null));
	}
}

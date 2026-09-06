package com.sensible.admin.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sensible.api.service.DmService;

/**
 * 1:1 메신저 자동폭파 (2-29차).
 * 레거시는 서버 메모리(ScheduledExecutorService)에 삭제를 예약해 재시작하면 예약이 사라지고
 * 파일은 디스크에 남았다. 여기서는 EXPIRE_AT 컬럼을 1분마다 훑어 파일을 지운 뒤 행을 지운다.
 */
@Component
public class DmExpireScheduler {

	private static final Logger logger = LoggerFactory.getLogger(DmExpireScheduler.class);

	@Autowired
	private DmService dmService;

	@Scheduled(cron = "0 * * * * *")
	public void purgeExpired() {
		try {
			int deleted = dmService.purgeExpired();
			if (deleted > 0) {
				logger.info("[DM] expired messages purged: {}", deleted);
			}
		} catch (Exception e) {
			// 한 번 실패해도 다음 분에 다시 돈다
			logger.warn("[DM] purge failed: {}", e.getMessage());
		}
	}
}

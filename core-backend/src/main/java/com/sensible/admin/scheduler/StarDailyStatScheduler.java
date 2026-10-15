package com.sensible.admin.scheduler;

import java.time.LocalDate;

import javax.annotation.Resource;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sensible.api.service.StarInsightService;

/**
 * 스타 인사이트 일별 집계 (2-34차). 매일 00:10 에 전날 행을 쓴다.
 *
 * - 방문·즐겨찾기·좋아요·게시물: 그저께~어제 이틀을 다시 쓴다 (자정 직전에 들어온 로그가 늦게 반영되는 경우 보정)
 * - 글로벌 순위: 지금 순위표를 "어제" 스냅샷으로 저장한다 (어제 하루가 끝난 직후의 순위)
 *
 * 스프링 기본 스케줄러는 스레드 하나라(context-common.xml task:annotation-driven) 다른 작업과 겹치지 않는 시각에 둔다.
 * 00:00 SystemMetricsCollector 일 작업과 10분 띄웠다. 실패해도 다음 날 다시 쓰므로 로그만 남긴다.
 */
@Component
public class StarDailyStatScheduler {

	@Resource(name = "starInsightService")
	private StarInsightService starInsightService;

	@Scheduled(cron = "0 10 0 * * *")
	public void aggregateYesterday() {
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		long started = System.currentTimeMillis();
		try {
			starInsightService.aggregateCounts(yesterday.minusDays(1), today);
		} catch (Exception e) {
			System.out.println("[INSIGHT] daily counts failed: " + e.getMessage());
		}
		try {
			int ranks = starInsightService.snapshotRanks(yesterday);
			System.out.println("[INSIGHT] daily stat " + yesterday + " ranks=" + ranks
					+ " in " + (System.currentTimeMillis() - started) + "ms");
		} catch (Exception e) {
			System.out.println("[INSIGHT] rank snapshot failed: " + e.getMessage());
		}
	}
}

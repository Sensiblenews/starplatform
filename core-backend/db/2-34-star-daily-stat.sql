-- =========================================================
-- 스타 인사이트 일별 통계 (2-34차 2-1·2-2, 2026-10-15)
--
-- 배경: 유저 인사이트(앱 별 메뉴 I 탭·스타 본인 어드민)와 글로벌·국가 어드민 인사이트 메뉴가
--       랭킹·방문자·즐겨찾기·좋아요·게시물 5종을 일·주·월·3개월·6개월·1년 단위로 그린다.
--       1년 범위를 원본 로그(WH_AD_LOG 등)에서 매번 GROUP BY 하면 무거우므로 하루 한 번 집계해 둔다.
--       매일 00:10 StarDailyStatScheduler 가 전날(과 그 전날 — 늦게 들어온 로그 보정) 행을 다시 쓴다.
--
--       글로벌 랭킹은 과거 기록이 저장된 적이 없다(순위는 누적 카운터로 매번 계산).
--       GLOBAL_RANK 는 이 테이블이 생긴 날부터 매일 스냅샷으로 쌓이고, 그 전 날짜는 NULL 이다.
--       나머지 4종은 로그에 일시가 있어 2-34-star-daily-stat-backfill.sql 로 과거분을 채울 수 있다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       미실행 상태로 배포하면 인사이트 화면·API 와 00:10 집계만 실패한다 (다른 화면은 영향 없음).
-- =========================================================

CREATE TABLE WH_STAR_DAILY_STAT (
	STAT_DATE    DATE         NOT NULL,
	PRS_ID       VARCHAR(50)  NOT NULL,
	VISIT_CNT    INT          NOT NULL DEFAULT 0 COMMENT '방문 (WH_AD_LOG IMPRESSION)',
	FOLLOW_CNT   INT          NOT NULL DEFAULT 0 COMMENT '즐겨찾기 추가 (WH_FOLLOWER_LOG FOLLOW)',
	UNFOLLOW_CNT INT          NOT NULL DEFAULT 0 COMMENT '즐겨찾기 해제 (WH_FOLLOWER_LOG UNFOLLOW)',
	LIKE_CNT     INT          NOT NULL DEFAULT 0 COMMENT '받은 좋아요 (WH_LIKE_LOG LIKE)',
	POST_CNT     INT          NOT NULL DEFAULT 0 COMMENT '작성 게시물 (WH_STAR_CONTENT 생성일)',
	GLOBAL_RANK  INT          NULL COMMENT '그날 끝 무렵(다음날 00:10) 글로벌 순위 스냅샷. 스냅샷 전 날짜는 NULL',
	UPDATED_DATE DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (STAT_DATE, PRS_ID),
	KEY IDX_WH_STAR_DAILY_STAT_PRS (PRS_ID, STAT_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='스타 일별 인사이트 집계';


-- =========================================================
-- 검증 쿼리
--   SELECT STAT_DATE, COUNT(*), SUM(VISIT_CNT) FROM WH_STAR_DAILY_STAT GROUP BY STAT_DATE ORDER BY STAT_DATE DESC LIMIT 10;
--
-- 롤백
--   DROP TABLE WH_STAR_DAILY_STAT;
-- =========================================================

-- =========================================================
-- 로비 LIVE 티커 — 어드민 LIVE NEWS 문구 (2-29차, 2026-09-06)
--
-- 배경: 로비 티커 Normal Loop의 ① 구간(관리자 입력 문구)을 담는다.
--       ②(카테고리별 #1)·③(VS 상태)은 기존 VS 카드 데이터를 재사용하므로
--       테이블이 필요 없다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       미실행 상태로 배포하면 /api/super/lobby/live-news 와
--       어드민 /super/live-news/list.do 가 SQL 에러로 실패한다.
--       (로비 자체는 티커가 문구 없이 ②·③만 순환하므로 장애는 아니다)
-- =========================================================

CREATE TABLE WH_LIVE_NEWS (
	NEWS_ID       INT          NOT NULL AUTO_INCREMENT,
	MESSAGE       VARCHAR(200) NOT NULL COMMENT '티커 노출 문구 (50자 제한은 서비스에서 강제)',
	TARGET_TYPE   VARCHAR(10)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE | STAR | VS | URL',
	TARGET_VALUE  VARCHAR(500) NULL COMMENT 'STAR=PRS_ID, VS=VS_ID, URL=외부 주소',
	SORT_ORDER    INT          NOT NULL DEFAULT 0,
	USE_YN        CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT 'ON/OFF 토글',
	DEL_YN        CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '소프트 삭제',
	CREATED_DATE  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	UPDATED_DATE  DATETIME     NULL,
	PRIMARY KEY (NEWS_ID)
) DEFAULT CHARSET=utf8mb4;

CREATE INDEX IDX_WH_LIVE_NEWS_ACTIVE ON WH_LIVE_NEWS (DEL_YN, USE_YN, SORT_ORDER);


-- =========================================================
-- 검증 쿼리
--   SHOW CREATE TABLE WH_LIVE_NEWS;
--
-- 롤백 (문제 발생 시)
--   DROP TABLE WH_LIVE_NEWS;
-- =========================================================

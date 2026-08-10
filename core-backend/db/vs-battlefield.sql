-- =========================================================
-- VS 배틀필드 (로비 VS 카드 + 카테고리 랭킹) DDL
-- 대상 DB: MariaDB (witch)
-- 주의: 운영 DB에 수동으로 적용한다. 애플리케이션 배포 전에 실행할 것.
-- =========================================================

-- 1. 스타 직군(카테고리) 컬럼
--    GENERAL = 미분류. 랭킹·VS 카드에서 후순위로 노출된다.
--    허용값: STAR / CELEB / BRAND / UNIV / CITY / MEDIA / GENERAL
ALTER TABLE WH_PRESS
	ADD COLUMN STAR_CATEGORY VARCHAR(20) NOT NULL DEFAULT 'GENERAL';

CREATE INDEX IDX_WH_PRESS_STAR_CATEGORY ON WH_PRESS (STAR_CATEGORY);

-- 기존 데이터는 컬럼 DEFAULT('GENERAL')로 일괄 미분류 처리된다.
-- 분류는 슈퍼 어드민 > Star Category 화면에서 수동으로 진행한다.

-- 2. VS 카드 설정 테이블
--    AUTO   : 랭킹 기반 자동 매치업 (RANK_TYPE + CATEGORY 조합, 아래 14행 시드)
--    CUSTOM : 어드민이 직접 등록한 특별전 (LEFT/RIGHT_PRS_ID)
--    노출 순서: IS_PINNED='Y' 우선, PIN_ORDER 오름차순, VS_ID 오름차순
CREATE TABLE WH_VS_CARD (
	VS_ID         INT          NOT NULL AUTO_INCREMENT,
	CARD_KIND     VARCHAR(10)  NOT NULL DEFAULT 'AUTO',  -- AUTO | CUSTOM
	RANK_TYPE     VARCHAR(10)  NULL,                     -- AUTO 전용: GLOBAL(누적 노출) | DAILY(오늘 노출)
	CATEGORY      VARCHAR(20)  NULL,                     -- AUTO 전용: GLOBAL | STAR | CELEB | BRAND | UNIV | CITY | MEDIA
	LEFT_PRS_ID   VARCHAR(50)  NULL,                     -- CUSTOM 전용
	RIGHT_PRS_ID  VARCHAR(50)  NULL,                     -- CUSTOM 전용
	TITLE         VARCHAR(100) NULL,                     -- CUSTOM 카드 표시명 (선택)
	IS_PINNED     CHAR(1)      NOT NULL DEFAULT 'N',
	PIN_ORDER     INT          NOT NULL DEFAULT 0,
	USE_YN        CHAR(1)      NOT NULL DEFAULT 'Y',
	CREATED_DATE  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (VS_ID)
) DEFAULT CHARSET=utf8mb4;

-- 3. AUTO 카드 14종 시드 (Global Ranking 7 + Daily King 7)
INSERT INTO WH_VS_CARD (CARD_KIND, RANK_TYPE, CATEGORY, PIN_ORDER) VALUES
	('AUTO', 'GLOBAL', 'GLOBAL',  1),
	('AUTO', 'GLOBAL', 'STAR',    2),
	('AUTO', 'GLOBAL', 'CELEB',   3),
	('AUTO', 'GLOBAL', 'BRAND',   4),
	('AUTO', 'GLOBAL', 'UNIV',    5),
	('AUTO', 'GLOBAL', 'CITY',    6),
	('AUTO', 'GLOBAL', 'MEDIA',   7),
	('AUTO', 'DAILY',  'GLOBAL',  8),
	('AUTO', 'DAILY',  'STAR',    9),
	('AUTO', 'DAILY',  'CELEB',  10),
	('AUTO', 'DAILY',  'BRAND',  11),
	('AUTO', 'DAILY',  'UNIV',   12),
	('AUTO', 'DAILY',  'CITY',   13),
	('AUTO', 'DAILY',  'MEDIA',  14);

-- =========================================================
-- 로비 Latest Contents 숏폼 편성 (2-33차, 2026-10-12)
--
-- 배경: 로비 헤더와 VS 카드 사이에 운영진이 편성한 숏폼 영상 캐러셀을 넣는다.
--       원본 게시물(WH_STAR_CONTENT / STAR_CONTENT_MEDIA)은 복제하지 않고
--       CON_ID 만 참조한다. 게시자 이름·프로필·영상 주소는 조회 시 조인한다.
--       WH_STAR_CONTENT 는 하드 삭제라 FK 를 걸지 않는다 — 원본이 지워지거나
--       검수에서 차단되면 로비 조회(INNER JOIN)에서 자동으로 빠진다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       미실행 상태로 배포하면 /api/super/lobby/latest-contents 와
--       어드민 /super/latest-contents/list.do 가 SQL 에러로 실패한다.
--       (앱은 조회 실패 시 섹션을 숨기므로 로비 자체는 열린다)
-- =========================================================

-- 편성 목록
CREATE TABLE WH_LATEST_CONTENT (
	LC_ID          INT          NOT NULL AUTO_INCREMENT,
	CON_ID         INT          NOT NULL COMMENT 'WH_STAR_CONTENT.CON_ID (참조만)',
	DISPLAY_ORDER  INT          NOT NULL DEFAULT 0 COMMENT '노출 순서 (1부터)',
	IS_FEATURED    CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '운영진 추천 표시',
	USE_YN         CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '게시/숨김 토글',
	DEL_YN         CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '소프트 삭제',
	START_AT       DATETIME     NULL COMMENT '노출 시작 (NULL=즉시, 서버 시간 KST 기준)',
	END_AT         DATETIME     NULL COMMENT '노출 종료 (NULL=무기한)',
	TITLE_OVERRIDE VARCHAR(100) NULL COMMENT '표시 제목 재정의 (NULL=원본 본문)',
	CREATED_BY     VARCHAR(50)  NULL COMMENT '등록 관리자 PRS_ID',
	UPDATED_BY     VARCHAR(50)  NULL,
	CREATED_DATE   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	UPDATED_DATE   DATETIME     NULL,
	PRIMARY KEY (LC_ID)
) DEFAULT CHARSET=utf8mb4;

CREATE INDEX IDX_WH_LATEST_CONTENT_ACTIVE ON WH_LATEST_CONTENT (DEL_YN, USE_YN, DISPLAY_ORDER);
CREATE INDEX IDX_WH_LATEST_CONTENT_CON ON WH_LATEST_CONTENT (CON_ID, DEL_YN);

-- 운영 설정 (단일 행 ID=1). 노출 개수·자동 이동 간격은 어드민에서 바꾼다
CREATE TABLE WH_LATEST_CONTENT_SETTING (
	ID             INT          NOT NULL,
	DISPLAY_COUNT  INT          NOT NULL DEFAULT 10 COMMENT '로비 노출 개수 (1~30)',
	INTERVAL_SEC   INT          NOT NULL DEFAULT 4 COMMENT '자동 이동 간격 초 (3~10)',
	UPDATED_BY     VARCHAR(50)  NULL,
	UPDATED_DATE   DATETIME     NULL,
	PRIMARY KEY (ID)
) DEFAULT CHARSET=utf8mb4;

INSERT INTO WH_LATEST_CONTENT_SETTING (ID, DISPLAY_COUNT, INTERVAL_SEC) VALUES (1, 10, 4);

-- 앱 이벤트 집계 (노출·재생·클릭·프로필 클릭). 기기·편성·종류·일자 단위로 1건만 남긴다(INSERT IGNORE)
CREATE TABLE WH_LATEST_CONTENT_EVENT (
	EVT_ID         BIGINT       NOT NULL AUTO_INCREMENT,
	LC_ID          INT          NOT NULL,
	EVENT_TYPE     VARCHAR(20)  NOT NULL COMMENT 'IMPRESSION | PLAY | CLICK | PROFILE_CLICK',
	DEVICE_ID      VARCHAR(100) NOT NULL COMMENT '기기 ID 또는 로그인 스타 ID',
	EVENT_DATE     DATE         NOT NULL,
	CREATED_DATE   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (EVT_ID),
	UNIQUE KEY UK_WH_LATEST_CONTENT_EVENT (LC_ID, EVENT_TYPE, DEVICE_ID, EVENT_DATE)
) DEFAULT CHARSET=utf8mb4;

-- 운영자 변경 이력
CREATE TABLE WH_LATEST_CONTENT_LOG (
	LOG_ID         BIGINT       NOT NULL AUTO_INCREMENT,
	LC_ID          INT          NULL COMMENT '설정 변경은 NULL',
	ACTION         VARCHAR(20)  NOT NULL COMMENT 'INSERT | UPLOAD | UPDATE | TOGGLE | ORDER | DELETE | SETTING',
	ADMIN_ID       VARCHAR(50)  NULL,
	DETAIL         VARCHAR(500) NULL,
	CREATED_DATE   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (LOG_ID)
) DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- 검증 쿼리
--   SHOW CREATE TABLE WH_LATEST_CONTENT;
--   SELECT * FROM WH_LATEST_CONTENT_SETTING;
--
-- 롤백 (문제 발생 시)
--   DROP TABLE WH_LATEST_CONTENT_LOG;
--   DROP TABLE WH_LATEST_CONTENT_EVENT;
--   DROP TABLE WH_LATEST_CONTENT_SETTING;
--   DROP TABLE WH_LATEST_CONTENT;
-- =========================================================

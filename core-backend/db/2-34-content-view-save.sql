-- =========================================================
-- 게시물 조회수 · 저장 (2-34차 1-4, 2026-10-15)
--
-- 배경: 숏폼 전체 화면 플레이어 오른쪽에 조회수·좋아요·댓글·저장·공유와 숫자를 붙인다.
--       좋아요·댓글·공유는 WH_STAR_CONTENT 의 기존 카운터를 쓰고, 조회수·저장만 새로 만든다.
--       회원 로그인이 없는 앱이라 시청자·저장자는 좋아요(WH_GUEST_LIKE)와 같은 기기 ID 로 구분한다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       미실행 상태로 배포하면 /api/super/content/stats·view·save 가 실패한다
--       (플레이어는 숫자를 숨기고 재생은 계속된다. 다른 화면은 영향 없음).
-- =========================================================

-- 게시물 카운터 (좋아요·댓글·공유 카운터 옆)
ALTER TABLE WH_STAR_CONTENT
	ADD COLUMN VIEW_CNT INT NOT NULL DEFAULT 0 COMMENT '조회수 (기기·30분 단위 중복 제거)',
	ADD COLUMN SAVE_CNT INT NOT NULL DEFAULT 0 COMMENT '저장 수';

-- 조회 중복 방지: 같은 기기가 같은 게시물을 30분 안에 다시 봐도 1번만 센다.
-- BUCKET = UNIX_TIMESTAMP / 1800. INSERT IGNORE 로 1행이 들어갔을 때만 VIEW_CNT 를 올린다
CREATE TABLE WH_CONTENT_VIEW (
	CON_ID       INT          NOT NULL,
	DEVICE_ID    VARCHAR(100) NOT NULL,
	BUCKET       INT          NOT NULL COMMENT '30분 단위 시간 구간',
	CREATED_DATE DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (CON_ID, DEVICE_ID, BUCKET),
	KEY IDX_WH_CONTENT_VIEW_DATE (CREATED_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='게시물 조회 기록 (중복 집계 방지)';

-- 기기별 저장 목록
CREATE TABLE WH_CONTENT_SAVE (
	CON_ID       INT          NOT NULL,
	DEVICE_ID    VARCHAR(100) NOT NULL,
	CREATED_DATE DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (CON_ID, DEVICE_ID),
	KEY IDX_WH_CONTENT_SAVE_DEVICE (DEVICE_ID, CREATED_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='게시물 저장 (기기 단위)';

-- 조회 기록은 중복 판정에만 쓰므로 오래된 행은 지워도 된다 (예: 30일 지난 행, 운영 판단으로 수동 실행)
-- DELETE FROM WH_CONTENT_VIEW WHERE CREATED_DATE < NOW() - INTERVAL 30 DAY;

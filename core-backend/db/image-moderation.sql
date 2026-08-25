-- =========================================================
-- 이미지 사전 검수 (2-26차, 2026-08-24)
--
-- 배경: 클라이언트 확정 사항은 "PENDING → 관리자 승인 → 공개"다.
--       AI 판정은 도입하지 않고 사람이 직접 승인한다.
--
-- 노출 규칙 (클라이언트 최종 확정):
--       작성자 본인 → 검수 대기 중에도 자기 이미지를 본다 (서명 토큰 경유)
--       타인       → 게시물은 보이되 이미지 자리에 "검토 중"
--       거절·블라인드 → 게시물 자체가 목록에서 빠진다
--
--       상태 컬럼은 미디어가 아니라 게시물 테이블에 둔다.
--       미디어 조인마다 조건을 붙이지 않아도 되고, 조회 비용도 컬럼 비교로 끝난다.
--
-- 대상: 회원 콘텐츠(WH_CONTENT, CON_TYPE='9')와 스타 피드(WH_STAR_CONTENT).
--       어드민 공지는 운영진이 직접 쓰므로 검수 대상이 아니다.
--
-- 상태값: PENDING(검수 대기) / APPROVED(공개) / REJECTED(검수 거절) / HIDDEN(신고 블라인드)
--       REJECTED와 HIDDEN은 접근 차단 동작이 같고 파일도 같은 곳으로 격리된다.
--       나누는 이유는 이력·통계에서 원인을 구분하기 위해서다.
--       (검수에서 걸렀는가 / 공개 후 신고로 내렸는가)
--
--       컬럼 타입이 VARCHAR(10)이라 값이 하나 늘어도 스키마 변경은 없다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       기본값이 'APPROVED'라 기존 게시물의 노출 상태는 변하지 않는다.
--       (이미 공개된 수만 건을 소급 검수하는 것은 불가능하므로 전량 승인 처리)
-- =========================================================

-- 1. 회원 콘텐츠 -------------------------------------------------
ALTER TABLE WH_CONTENT
	ADD COLUMN MDR_STATUS VARCHAR(10) NOT NULL DEFAULT 'APPROVED',
	ADD COLUMN MDR_DATE   DATETIME    NULL,
	ADD COLUMN MDR_ADMIN  VARCHAR(50) NULL;

-- 검수 대기 목록 조회용. 목록은 MDR_STATUS로 걸러 오래된 순으로 본다
CREATE INDEX IDX_WH_CONTENT_MDR ON WH_CONTENT (MDR_STATUS, CON_ID);


-- 2. 스타 피드 ---------------------------------------------------
ALTER TABLE WH_STAR_CONTENT
	ADD COLUMN MDR_STATUS VARCHAR(10) NOT NULL DEFAULT 'APPROVED',
	ADD COLUMN MDR_DATE   DATETIME    NULL,
	ADD COLUMN MDR_ADMIN  VARCHAR(50) NULL;

CREATE INDEX IDX_WH_STAR_CONTENT_MDR ON WH_STAR_CONTENT (MDR_STATUS, CON_ID);


-- 3. 처리 이력 ---------------------------------------------------
-- 누가 언제 무엇을 왜 했는지 남긴다.
-- 스토어 심사·분쟁 대응에서 "신고에 적시 대응했다"를 증명하는 근거가 된다.
CREATE TABLE WH_MEDIA_MODERATION_LOG (
	LOG_ID       BIGINT       NOT NULL AUTO_INCREMENT,
	TARGET_TYPE  VARCHAR(20)  NOT NULL COMMENT 'MEMBER_CONTENT | STAR_FEED',
	TARGET_ID    VARCHAR(50)  NOT NULL COMMENT '게시물 ID',
	ACTION       VARCHAR(20)  NOT NULL COMMENT 'PENDING | APPROVED | HIDDEN',
	REASON       VARCHAR(255)     NULL,
	ADMIN_ID     VARCHAR(50)      NULL COMMENT '자동 등록이면 NULL',
	CREATED_DATE DATETIME     NOT NULL,
	PRIMARY KEY (LOG_ID),
	KEY IDX_MDR_LOG_TARGET (TARGET_TYPE, TARGET_ID, CREATED_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- =========================================================
-- 검증 쿼리
--
-- 기존 게시물이 전부 APPROVED인지 (PENDING이 0건이어야 한다)
--   SELECT MDR_STATUS, COUNT(*) FROM WH_CONTENT      GROUP BY MDR_STATUS;
--   SELECT MDR_STATUS, COUNT(*) FROM WH_STAR_CONTENT GROUP BY MDR_STATUS;
--
-- 배포 후 상태별 건수
--   SELECT MDR_STATUS, COUNT(*) FROM WH_CONTENT      GROUP BY MDR_STATUS;
--   SELECT MDR_STATUS, COUNT(*) FROM WH_STAR_CONTENT GROUP BY MDR_STATUS;
--
-- 롤백 (문제 발생 시)
--   ALTER TABLE WH_CONTENT      DROP COLUMN MDR_STATUS, DROP COLUMN MDR_DATE, DROP COLUMN MDR_ADMIN;
--   ALTER TABLE WH_STAR_CONTENT DROP COLUMN MDR_STATUS, DROP COLUMN MDR_DATE, DROP COLUMN MDR_ADMIN;
--   DROP TABLE WH_MEDIA_MODERATION_LOG;
-- =========================================================

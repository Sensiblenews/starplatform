-- =========================================================
-- 1:1 메신저 (DM) — 스타 페이지 소유자 간 대화 (2-29차, 2026-09-06)
--
-- 배경: 레거시 지구로또 채팅(WH_USER_MESSAGE ⋈ WH_MEMBER)은 운영 DB에
--       WH_MEMBER가 없어 그대로 쓸 수 없다. 계정은 WH_PRESS(PRS_ID) 기준으로
--       다시 잡고, 자동폭파는 서버 메모리 예약 대신 EXPIRE_AT 컬럼 + 1분 주기
--       스케줄러(DmExpireScheduler)로 영속화한다.
--
-- 폭파 규칙(클라이언트 확정, 레거시와 동일):
--       발송 5분 후 삭제. 읽음 처리 시 그 시점 미읽음이던 메시지는 1분 후 삭제.
--       사진·영상 파일과 썸네일도 같은 시점에 디스크에서 삭제. 행은 물리 삭제.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       업로드 디렉터리(globals.properties dm.upload.path, 기본 /var/lib/tomcat7/dm/)도
--       배포 전에 만들고 Tomcat 쓰기 권한을 줘야 한다.
-- =========================================================

CREATE TABLE WH_DM_MESSAGE (
	MSG_ID        BIGINT       NOT NULL AUTO_INCREMENT,
	SEND_PRS_ID   VARCHAR(50)  NOT NULL COMMENT '발신 스타 (WH_PRESS.PRS_ID)',
	RECV_PRS_ID   VARCHAR(50)  NOT NULL COMMENT '수신 스타 (WH_PRESS.PRS_ID)',
	CONTENT_TYPE  VARCHAR(10)  NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT | IMAGE | VIDEO',
	CONTENT       VARCHAR(2000) NOT NULL COMMENT '텍스트 본문 또는 저장 파일명',
	THUMB_NM      VARCHAR(255) NULL COMMENT '영상 썸네일 파일명',
	SEND_DATE     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	READ_DATE     DATETIME     NULL,
	EXPIRE_AT     DATETIME     NOT NULL COMMENT '폭파 예정 시각',
	PRIMARY KEY (MSG_ID)
) DEFAULT CHARSET=utf8mb4;

-- 미읽음 수·수신함 조회
CREATE INDEX IDX_WH_DM_RECV ON WH_DM_MESSAGE (RECV_PRS_ID, READ_DATE, EXPIRE_AT);
-- 발신함(대화 목록) 조회
CREATE INDEX IDX_WH_DM_SEND ON WH_DM_MESSAGE (SEND_PRS_ID, EXPIRE_AT);
-- 폭파 스케줄러
CREATE INDEX IDX_WH_DM_EXPIRE ON WH_DM_MESSAGE (EXPIRE_AT);


-- =========================================================
-- 검증 쿼리
--   SHOW CREATE TABLE WH_DM_MESSAGE;
--
-- 롤백 (문제 발생 시)
--   DROP TABLE WH_DM_MESSAGE;
-- =========================================================

-- =========================================================
-- 1:1 메신저 신고 (2-28차, 2026-09-11)
--
-- 배경: WH_DM_MESSAGE는 발송 5분·읽음 1분 뒤 DmExpireScheduler(1분 주기)가
--       행과 첨부 파일을 물리 삭제한다. 신고가 들어와도 관리자가 화면을 열 시점엔
--       원문이 이미 사라져 있다.
--       따라서 이 테이블은 원본을 가리키는 참조가 아니라 신고 시점의 스냅샷이다.
--
-- 규칙: 텍스트는 CONTENT_SNAPSHOT에 본문을 복사한다.
--       사진·영상은 신고 전용 디렉터리(Constants._DM_REPORT_SAVE_PATH)로 파일을
--       복사하고 그 파일명을 FILE_NM에 둔다. 원본 파일명(UUID)을 그대로 쓴다
--       — 디렉터리가 다르므로 충돌이 없고 로그 대조가 쉽다.
--
--       첨부 복사에 실패해도(스케줄러가 방금 지운 정상 경합) 신고 자체는 접수한다.
--       이때 FILE_NM은 NULL이고 어드민 화면은 "확보 실패"로 표시한다.
--       누가·누구를·언제·왜가 남으면 상습범 정지에는 충분하다.
--
-- 외래키: 두지 않는다.
--       WH_DM_MESSAGE 행은 5분 뒤 사라지므로 FK가 있으면 증거가 같이 지워지거나
--       폭파가 막힌다. WH_PRESS에도 두지 않는다(계정이 지워져도 증거는 남겨야 한다).
--       기존 테이블(WH_DM_MESSAGE, WH_GUEST_REPORT, WH_MEDIA_MODERATION_LOG)도
--       모두 FK가 없다.
--
-- 중복 신고: (MSG_ID, REPORTER_PRS_ID) 유니크. 같은 메시지를 두 번 신고할 수 없다.
--
-- 문자셋: utf8mb4 필수. WH_DM_MESSAGE가 utf8mb4이고 DM 본문에 이모지가 흔하다.
--       utf8로 만들면 스냅샷 INSERT가 깨지거나 잘린다.
--
-- 제재: 어드민은 영구 정지만 한다 (WH_PRESS.PRS_USE_YN = 'N').
--       기간제 정지가 없으므로 WH_PRESS에 새 컬럼을 붙이지 않는다.
--       처리 이력은 기존 WH_MEDIA_MODERATION_LOG를 재사용한다
--       (TARGET_TYPE='DM_REPORT', ACTION='SUSPEND'|'RESOLVED'|'DISMISSED').
--       두 컬럼 모두 VARCHAR(20)이라 추가 DDL이 필요 없다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       신고 증거 디렉터리도 배포 전에 만들고 Tomcat 쓰기 권한을 줘야 한다.
--         sudo mkdir -p /var/lib/tomcat7/moderation/dm-report
--         sudo chown tomcat7:tomcat7 /var/lib/tomcat7/moderation/dm-report
--         sudo chmod 750 /var/lib/tomcat7/moderation/dm-report
--       이 경로는 반드시 webapps/ 밖이어야 한다. 접근 제어가 통째로
--       "공개 디렉터리에 없으면 웹으로 404"라는 성질에 의존한다 (2-26차와 동일).
-- =========================================================

CREATE TABLE WH_DM_REPORT (
	RPT_ID           BIGINT        NOT NULL AUTO_INCREMENT,
	MSG_ID           BIGINT        NOT NULL COMMENT '원본 WH_DM_MESSAGE.MSG_ID (FK 아님 — 원본은 곧 폭파된다)',
	REPORTER_PRS_ID  VARCHAR(50)   NOT NULL COMMENT '신고자 (WH_PRESS.PRS_ID)',
	TARGET_PRS_ID    VARCHAR(50)   NOT NULL COMMENT '피신고자 = 메시지 발신자',
	TARGET_PRS_NAME  VARCHAR(100)      NULL COMMENT '신고 시점 이름 스냅샷 (계정이 지워져도 화면에 남긴다)',
	REASON           VARCHAR(20)   NOT NULL COMMENT 'ABUSE | SEXUAL | SPAM | FRAUD | THREAT | OTHER',
	CONTENT_TYPE     VARCHAR(10)   NOT NULL COMMENT 'TEXT | IMAGE | VIDEO',
	CONTENT_SNAPSHOT VARCHAR(2000)     NULL COMMENT 'TEXT일 때 본문 사본 (WH_DM_MESSAGE.CONTENT와 같은 폭)',
	FILE_NM          VARCHAR(255)      NULL COMMENT '신고 디렉터리로 복사한 첨부 파일명. 복사 실패 시 NULL',
	THUMB_NM         VARCHAR(255)      NULL COMMENT '영상 썸네일 사본. 원본에 없을 수 있다',
	MSG_SEND_DATE    DATETIME          NULL COMMENT '원본 발송 시각',
	STATUS           VARCHAR(10)   NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN | RESOLVED | DISMISSED',
	ADMIN_ID         VARCHAR(50)       NULL COMMENT '처리한 관리자',
	ADMIN_MEMO       VARCHAR(255)      NULL,
	HANDLED_DATE     DATETIME          NULL,
	CREATED_DATE     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (RPT_ID),
	UNIQUE KEY UK_WH_DM_REPORT_MSG (MSG_ID, REPORTER_PRS_ID)
) DEFAULT CHARSET=utf8mb4;

-- 어드민 목록: 상태 탭별로 최신순
CREATE INDEX IDX_WH_DM_REPORT_STATUS ON WH_DM_REPORT (STATUS, CREATED_DATE);
-- 누적 신고 확인 (같은 스타가 몇 번 신고됐나)
CREATE INDEX IDX_WH_DM_REPORT_TARGET ON WH_DM_REPORT (TARGET_PRS_ID, CREATED_DATE);
-- 도배 방지: 신고자 1인 24시간 건수
CREATE INDEX IDX_WH_DM_REPORT_REPORTER ON WH_DM_REPORT (REPORTER_PRS_ID, CREATED_DATE);


-- =========================================================
-- 검증 쿼리
--   SHOW CREATE TABLE WH_DM_REPORT;
--
--   -- 상태별 건수
--   SELECT STATUS, COUNT(*) FROM WH_DM_REPORT GROUP BY STATUS;
--
--   -- 첨부를 확보하지 못한 신고 (스케줄러와의 경합. 소수면 정상, 전부면 디렉터리 권한 의심)
--   SELECT COUNT(*) FROM WH_DM_REPORT WHERE CONTENT_TYPE <> 'TEXT' AND FILE_NM IS NULL;
--
--   -- 누적 신고 상위 스타
--   SELECT TARGET_PRS_ID, TARGET_PRS_NAME, COUNT(*) AS CNT
--     FROM WH_DM_REPORT GROUP BY TARGET_PRS_ID, TARGET_PRS_NAME
--    ORDER BY CNT DESC LIMIT 20;
--
--
-- 보존 정책 (수동)
--   증거이므로 자동 삭제하지 않는다. 대신 분기마다 아래 순서로 정리한다.
--   1) 지울 대상의 파일명을 먼저 뽑는다
--      SELECT RPT_ID, FILE_NM, THUMB_NM FROM WH_DM_REPORT
--       WHERE STATUS <> 'OPEN' AND HANDLED_DATE < DATE_SUB(NOW(), INTERVAL 180 DAY);
--   2) /var/lib/tomcat7/moderation/dm-report/ 에서 해당 파일을 지운다
--   3) 행을 지운다
--      DELETE FROM WH_DM_REPORT
--       WHERE STATUS <> 'OPEN' AND HANDLED_DATE < DATE_SUB(NOW(), INTERVAL 180 DAY);
--   순서를 바꾸면 파일명을 잃어버려 고아 파일이 남는다.
--
--
-- 롤백 (문제 발생 시)
--   DROP TABLE WH_DM_REPORT;
--   sudo rm -rf /var/lib/tomcat7/moderation/dm-report
-- =========================================================

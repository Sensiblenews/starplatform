-- =========================================================
-- 공개 웹 문의 접수 (2-29차, 2026-09-16)
--
-- 배경: witch-hunting.com 공개 사이트에 문의 폼(/contact)을 추가한다.
--       지금까지 문의 경로는 앱 FAQ 화면의 "문의하기"(메일 앱 실행)뿐이라
--       웹 방문자에게는 연락 수단이 없었다.
--
--       접수분은 이 테이블에 남기고 관리자에게 메일로도 알린다.
--       메일 발송이 실패해도 접수 자체는 남아야 하므로 DB 저장이 원본이다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       미실행 상태로 배포하면 /contact 의 폼 제출이 SQL 에러로 실패한다
--       (읽기 전용인 /contact 페이지 노출과 다른 공개 페이지에는 영향 없음).
-- =========================================================

CREATE TABLE IF NOT EXISTS WH_WEB_CONTACT (
	CONTACT_ID    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '접수 번호',
	CONTACT_TYPE  VARCHAR(30)  NOT NULL                COMMENT '문의 유형 코드 (GENERAL/ACCOUNT/REPORT/COPYRIGHT/ADVERTISING/BRAND/INSTITUTION/PARTNERSHIP/PRIVACY/OTHER)',
	SENDER_NAME   VARCHAR(100) NOT NULL                COMMENT '이름 또는 닉네임',
	SENDER_EMAIL  VARCHAR(255) NOT NULL                COMMENT '회신 이메일',
	SUBJECT       VARCHAR(200) NOT NULL                COMMENT '제목',
	MESSAGE_BODY  TEXT         NOT NULL                COMMENT '문의 내용 (HTML 태그 제거 후 저장)',
	CLIENT_IP     VARCHAR(45)  NULL                    COMMENT '접수 IP (IPv6 대응 45자)',
	USER_AGENT    VARCHAR(500) NULL                    COMMENT '접수 UA (스팸 판별 참고용)',
	STATUS        VARCHAR(20)  NOT NULL DEFAULT 'NEW'  COMMENT '처리 상태 (NEW/DONE)',
	MAIL_SENT_YN  CHAR(1)      NOT NULL DEFAULT 'N'    COMMENT '관리자 알림 메일 발송 여부',
	CREATED_DATE  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '접수 일시',
	PRIMARY KEY (CONTACT_ID),
	-- 어드민 목록은 최신순 + 미처리 우선으로 본다
	KEY IDX_WH_WEB_CONTACT_CREATED (CREATED_DATE),
	KEY IDX_WH_WEB_CONTACT_STATUS (STATUS, CREATED_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공개 웹 문의 접수';

-- =========================================================
-- 개인정보 보관 정책
--
-- 이름·이메일이 들어가므로 무기한 보관하지 않는다. 처리 완료 후
-- 보관 기간이 정해지면 아래 형태의 정리 작업을 스케줄러에 추가할 것.
-- (기간 미확정 상태라 지금은 실행하지 않는다 — 주석으로만 남긴다)
--
--   DELETE FROM WH_WEB_CONTACT
--    WHERE STATUS = 'DONE' AND CREATED_DATE < DATE_SUB(NOW(), INTERVAL 1 YEAR);
-- =========================================================

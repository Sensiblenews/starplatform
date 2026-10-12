-- =========================================================
-- 서버 위험 경보 · 자동 이메일 알림 (2026-10-12)
--
-- 배경: 디스크·RAM·CPU·JVM 힙·DB·Redis·HTTP 5xx·관리자 로그인 실패를 5분마다 평가해
--       주의/위험/긴급 등급을 매기고, 위험 이상은 SMTP 메일로 운영자에게 알린다.
--       같은 장애는 경보 하나로 관리하고(중복 발송 방지), 복구되면 복구 메일을 보낸다.
--
-- 감시 상태의 원본은 WAS 메모리다. 이 테이블들은 "기록·조회·재기동 복원"용이다.
-- DB 가 끊긴 상황도 경보 대상이므로, 테이블이 없거나 DB 가 죽어도 감시·메일은 계속된다
-- (이력만 빠진다). 따라서 미실행 상태로 배포해도 로그인 등 다른 기능은 영향이 없다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 전 실행 권장.
-- =========================================================

-- 경보 (항목당 열린 경보는 하나)
CREATE TABLE IF NOT EXISTS WH_SYS_ALERT (
	ALERT_ID            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '경보 ID (메일에 표기)',
	ALERT_KEY           VARCHAR(30)   NOT NULL                COMMENT '항목: disk/ram/cpu/heap/db/redis/http5xx/loginfail',
	SEVERITY            VARCHAR(10)   NOT NULL                COMMENT '현재 등급: WARNING/DANGER/CRITICAL',
	STATUS              VARCHAR(10)   NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / RESOLVED',
	TITLE               VARCHAR(100)  NOT NULL                COMMENT '항목 표시명',
	MESSAGE             VARCHAR(500)  NULL                    COMMENT '최근 측정 설명',
	METRIC_VALUE        DOUBLE        NULL                    COMMENT '최근 측정값',
	PLATFORM_ID         VARCHAR(20)   NULL                    COMMENT '플랫폼 ID (globals platform.id, 기본 WITCH)',
	OPENED_DATE         DATETIME      NOT NULL                COMMENT '발생 시각',
	UPDATED_DATE        DATETIME      NOT NULL                COMMENT '갱신 시각',
	RESOLVED_DATE       DATETIME      NULL                    COMMENT '복구 시각',
	ACK_YN              CHAR(1)       NOT NULL DEFAULT 'N'    COMMENT '담당자 확인 여부 (복구와 별개)',
	ACK_BY              VARCHAR(50)   NULL                    COMMENT '확인한 관리자',
	ACK_DATE            DATETIME      NULL                    COMMENT '확인 시각',
	MAIL_SENT_YN        CHAR(1)       NOT NULL DEFAULT 'N'    COMMENT '경보 메일이 1회 이상 나갔는지',
	NOTIFY_COUNT        INT           NOT NULL DEFAULT 0      COMMENT '메일 발송 횟수 (최초+격상+재알림)',
	LAST_NOTIFIED_DATE  DATETIME      NULL                    COMMENT '마지막 메일 발송 시각',
	PRIMARY KEY (ALERT_ID),
	KEY IDX_WH_SYS_ALERT_STATUS (STATUS, SEVERITY, OPENED_DATE),
	KEY IDX_WH_SYS_ALERT_KEY (ALERT_KEY, OPENED_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='서버 위험 경보';

-- 이력 (발생·격상·재알림·복구·확인·메일 성공/실패·시험 메일·설정 변경)
CREATE TABLE IF NOT EXISTS WH_SYS_ALERT_EVENT (
	EVENT_ID      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '이력 번호',
	ALERT_ID      BIGINT        NULL                    COMMENT '관련 경보 (설정 변경·시험 메일은 NULL)',
	ALERT_KEY     VARCHAR(30)   NULL                    COMMENT '항목',
	EVENT_TYPE    VARCHAR(20)   NOT NULL                COMMENT 'OPEN/ESCALATE/REMIND/RESOLVE/ACK/MAIL_SENT/MAIL_FAIL/TEST_MAIL/SETTING_CHANGED',
	SEVERITY      VARCHAR(10)   NULL                    COMMENT '당시 등급',
	MESSAGE       VARCHAR(1000) NULL                    COMMENT '설명 (SMTP 인증정보 등 비밀값 금지)',
	ACTOR         VARCHAR(50)   NULL                    COMMENT 'SYSTEM 또는 관리자 ID',
	CREATED_DATE  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발생 시각',
	PRIMARY KEY (EVENT_ID),
	KEY IDX_WH_SYS_ALERT_EVENT_ALERT (ALERT_ID),
	KEY IDX_WH_SYS_ALERT_EVENT_DATE (CREATED_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='서버 경보 이력';

-- 설정 (어드민 경보 센터에서 수정. 없는 키는 코드 기본값 사용)
--   alert.enabled=Y, alert.mail.to=<쉼표 구분 수신자>, alert.mail.min.severity=DANGER,
--   alert.disk.warn/danger/critical=75/85/95, alert.ram.*=80/90/95, alert.cpu.warn/danger=80/90,
--   alert.heap.warn/danger=85/95, alert.http5xx.min.count=20, alert.http5xx.percent=5,
--   alert.loginfail.min.count=20, alert.confirm.count=2, alert.recover.count=2,
--   alert.remind.minutes=60, alert.remind.max=3
-- SMTP 호스트·계정·비밀번호는 여기 두지 않는다 (서버 설정 파일 전용).
CREATE TABLE IF NOT EXISTS WH_SYS_ALERT_SETTING (
	SETTING_KEY    VARCHAR(50)  NOT NULL COMMENT '설정 키',
	SETTING_VALUE  VARCHAR(500) NOT NULL COMMENT '설정 값',
	UPDATED_BY     VARCHAR(50)  NULL     COMMENT '변경한 관리자',
	UPDATED_DATE   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경 시각',
	PRIMARY KEY (SETTING_KEY)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='서버 경보 설정';

-- =========================================================
-- 보관 정책 (미확정 — 지금은 실행하지 않는다)
--   DELETE FROM WH_SYS_ALERT_EVENT WHERE CREATED_DATE < DATE_SUB(NOW(), INTERVAL 1 YEAR);
--   DELETE FROM WH_SYS_ALERT WHERE STATUS = 'RESOLVED' AND RESOLVED_DATE < DATE_SUB(NOW(), INTERVAL 1 YEAR);
--
-- 롤백: 세 테이블 DROP. 감시기는 테이블 없이도 동작한다(기록만 실패 로그).
-- =========================================================

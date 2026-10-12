-- =========================================================
-- 관리자 Google OTP 이중인증 + 보안 감사 로그 (2026-10-12)
--
-- 배경: 슈퍼 어드민(SM)·지역 관리자(LC) 로그인을 "아이디+비밀번호 → OTP 6자리(또는 복구 코드)"
--       2단계로 바꾼다. OTP 검증 전에는 세션·SUPER_TOKEN 쿠키를 발급하지 않는다.
--
-- 계정 테이블은 WH_PRESS(PRS_ID) 다. WH_PRESS 에 컬럼을 추가하지 않고 1:1 보조 테이블로 분리한다
-- (스타 계정과 섞인 넓은 테이블에 비밀값 컬럼을 늘리지 않기 위함).
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       미실행 상태로 배포하면 SM·LC 로그인이 "이중인증 저장소 오류"로 실패한다
--       (의도된 fail-closed. 스타 ST 로그인과 앱 API 는 영향 없음).
--
-- 선행 설정: globals.properties 에 otp.encryption.key=<임의의 긴 문자열> 을 넣고 WAS 재기동.
--            키가 없으면 이중인증은 비활성(기존 로그인 그대로)이고 어드민에 경고 배너가 뜬다.
--            키를 바꾸면 기존 비밀키를 복호화할 수 없으므로 전원 재등록이 필요하다.
-- =========================================================

CREATE TABLE IF NOT EXISTS WH_ADMIN_MFA (
	PRS_ID              VARCHAR(50)  NOT NULL                COMMENT '관리자 ID (WH_PRESS.PRS_ID)',
	OTP_ENABLED_YN      CHAR(1)      NOT NULL DEFAULT 'N'    COMMENT 'OTP 활성 여부',
	OTP_SECRET_ENC      VARCHAR(512) NULL                    COMMENT '정식 비밀키 (AES-GCM 암호문, 원문 저장 금지)',
	PENDING_SECRET_ENC  VARCHAR(512) NULL                    COMMENT '등록 진행 중 임시 비밀키 (암호문)',
	PENDING_EXPIRES     DATETIME     NULL                    COMMENT '임시 비밀키 만료 시각',
	FAIL_COUNT          INT          NOT NULL DEFAULT 0      COMMENT '연속 실패 횟수 (5회 → 잠금)',
	LOCKED_UNTIL        DATETIME     NULL                    COMMENT '잠금 해제 시각 (10분)',
	LAST_VERIFIED_DATE  DATETIME     NULL                    COMMENT '마지막 2차 인증 성공 시각',
	LAST_USED_STEP      BIGINT       NULL                    COMMENT '마지막으로 성공한 TOTP 시간 단계 (재사용 방지)',
	ENROLLED_DATE       DATETIME     NULL                    COMMENT '등록 완료 시각',
	UPDATED_DATE        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '갱신 시각',
	PRIMARY KEY (PRS_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='관리자 OTP 이중인증';

CREATE TABLE IF NOT EXISTS WH_ADMIN_MFA_BACKUP_CODE (
	CODE_ID       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '복구 코드 번호',
	PRS_ID        VARCHAR(50) NOT NULL                COMMENT '관리자 ID',
	CODE_HASH     CHAR(64)    NOT NULL                COMMENT 'SHA-256(prsId:code) 16진수. 원문 저장 금지',
	USED_YN       CHAR(1)     NOT NULL DEFAULT 'N'    COMMENT '사용 여부 (일회용)',
	USED_DATE     DATETIME    NULL                    COMMENT '사용 시각',
	CREATED_DATE  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급 시각',
	PRIMARY KEY (CODE_ID),
	KEY IDX_WH_ADMIN_MFA_BACKUP_PRS (PRS_ID, USED_YN)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='관리자 OTP 복구 코드 (해시)';

-- 감사 로그. 비밀키·OTP 번호·복구 코드 원문·비밀번호·토큰은 어떤 컬럼에도 넣지 않는다.
CREATE TABLE IF NOT EXISTS WH_ADMIN_AUDIT_LOG (
	LOG_ID        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '로그 번호',
	PRS_ID        VARCHAR(100) NULL                    COMMENT '행위 주체 (로그인 실패 시 입력 ID 그대로)',
	EVENT_TYPE    VARCHAR(40)  NOT NULL                COMMENT 'LOGIN_SUCCESS/LOGIN_FAIL/OTP_SETUP_STARTED/OTP_REGISTER/OTP_LOGIN_SUCCESS/OTP_LOGIN_FAIL/OTP_LOCKED/OTP_BACKUP_CODE_USED/OTP_BACKUP_CODE_REISSUED/OTP_RESET/ALERT_SETTING_CHANGED/ALERT_ACK/ALERT_TEST_MAIL',
	RESULT        CHAR(1)      NOT NULL                COMMENT 'S 성공 / F 실패',
	REASON_CODE   VARCHAR(40)  NULL                    COMMENT '실패 사유 코드',
	TARGET_ID     VARCHAR(100) NULL                    COMMENT '대상 관리자 (재설정 등)',
	CLIENT_IP     VARCHAR(45)  NULL                    COMMENT '요청 IP',
	USER_AGENT    VARCHAR(300) NULL                    COMMENT '요청 UA',
	DETAIL        VARCHAR(500) NULL                    COMMENT '설명 (비밀값 금지)',
	CREATED_DATE  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발생 시각',
	PRIMARY KEY (LOG_ID),
	KEY IDX_WH_ADMIN_AUDIT_PRS (PRS_ID, CREATED_DATE),
	KEY IDX_WH_ADMIN_AUDIT_EVENT (EVENT_TYPE, CREATED_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='관리자 보안 감사 로그';

-- =========================================================
-- 보관 정책 (미확정 — 지금은 실행하지 않는다)
--   DELETE FROM WH_ADMIN_AUDIT_LOG WHERE CREATED_DATE < DATE_SUB(NOW(), INTERVAL 1 YEAR);
--
-- 롤백: 세 테이블 DROP. 애플리케이션은 otp.mode=off 로 두면 테이블 없이도 기동·로그인된다.
-- =========================================================

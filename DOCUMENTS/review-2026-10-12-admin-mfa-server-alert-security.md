# 요청서 검토 + 구현 결과 — 어드민 Google OTP 이중인증 · 서버 위험 경보(SMTP) · 보안 15×15 점검 · 시스템 관리 지침 (2026-10-12)

클라이언트 메일 묶음 5건(어드민 OTP 지시서 2종, 서버 경보 20단계 + 보완 지적, 15×15 통합 매뉴얼, 시스템 관리 31~47항)을 한 번에 다룬다.
요청서의 Node.js/Express/PM2/Nodemailer/speakeasy 전제와 "N단계" 순서는 참고로만 보고, 현 스택(Spring 4.1 + 전자정부프레임워크 + MyBatis/MariaDB + Redis + 톰캣 7, 기존 `mailSender` SMTP 빈)으로 다시 설계했다.
사용자 지시: "7001~7010 확장판 — 실운영(서버·Cloudflare·외부 계정)에 필요한 것만 빼고 전부".

---

## 1. 요구사항 목록

| # | 요구사항 | 출처 | 상태 |
|---|---|---|---|
| R1 | SM·LC 등 핵심 관리자 계정에 Google Authenticator(TOTP) 이중인증. OTP 검증 전 관리자 토큰 미발급 | OTP 지시서 | ✅ 구현 |
| R2 | 등록 플로우: 비밀번호 재확인 → 임시 비밀키(만료) → QR → 6자리 확인 → 활성화 → 복구 코드 5개 1회 노출 | OTP 지시서 | ✅ |
| R3 | 비밀키 암호화 저장, 암호화 키는 저장소 밖(환경 설정) | OTP 지시서 | ✅ AES-GCM, `otp.encryption.key` |
| R4 | 복구 코드: 난수·해시 저장·일회용·사용 이력·재발급 시 이전 무효·소진 시 재발급 | OTP 지시서 | ✅ |
| R5 | 2단계 로그인 분리, 임시 상태는 OTP 전용 경로에서만 유효·짧은 수명·재사용 방지 | OTP 지시서 | ✅ 세션 `MfaPendingVO`(5분, 시도 상한) |
| R6 | 미등록 고권한 관리자는 등록 전 관리자 기능 접근 불가, 우회 경로 금지 | OTP 지시서 | ✅ `otp.mode=required` |
| R7 | 시간 오차 window 1(±30초), 서버 NTP 동기화 | OTP 지시서 | ✅ 코드 / NTP 는 🔧 운영 |
| R8 | OTP 5회 연속 실패 → 10분 잠금, 복구 코드 대입 공격 함께 제한, 공격자가 무기한 잠그지 못하게 | OTP 지시서 | ✅ (잠금은 10분 자동 해제, 세션 시도 상한 10회) |
| R9 | 감사 로그 이벤트 9종 + 기록 항목(ID·종류·시각·결과·IP·사유·승인자), 비밀값 미기록 | OTP 지시서 | ✅ `WH_ADMIN_AUDIT_LOG` (OTP_DISABLED·POLICY_CHANGED 는 상수만 — 해당 기능 없음) |
| R10 | 분실·재설정 절차: 복구 코드 → 본인 확인 → 보안 담당자 승인 재설정 → 기존 무효화 → 재등록 → 세션 만료, DB 직접 수정 금지 | OTP 지시서 | ✅ SM 재설정 + 토큰 무효화 + 안내문 |
| R11 | 총운영사(복구 가능) 어드민에서 실제 테스트, 사용 안내문·테스트 결과·DB 변경·캡처 제출 | OTP 메일 | 🟡 안내문·DDL·테스트 12항목 체크표 제공. **실제 어드민 테스트는 DDL 적용 후 운영에서** (아래 6절) |
| R12 | 디스크·RAM·CPU(·JVM·DB·Redis·API) 감시, 어드민 경보 표시(정상/주의/위험/긴급, 수치·시각) | 경보 20단계 | ✅ 경보 센터 |
| R13 | 임계값: 디스크 75/85/95, RAM 80/90/95, CPU 80/90 지속, 서버 사양에 맞춰 조정 가능 | 경보 20단계 | ✅ 기본값 + 어드민 설정 |
| R14 | SMTP 설정 안전 보관, 시험 메일 `[TEST] StarPlatform 서버 알림` 플랫폼 ID, 실수신 확인 | 경보 20단계 | ✅ 시험 메일 버튼 / 실수신은 🔧 운영 |
| R15 | 위험 메일 내용(등급·플랫폼·항목·수치·시각·권장 조치·경보 ID), 중복 방지, 격상 시 재알림, 미확인 긴급 재알림(간격·횟수) | 경보 20단계 | ✅ |
| R16 | 복구 메일(같은 경보 ID, 복구 시각·지속 시간·복구 후 값), 발송 실패 기록·제한 재시도, 메일 실패가 감시를 멈추지 않게, 비동기 | 경보 20단계 + 보완 | ✅ 전용 스레드·10초 타임아웃·3회 재시도 |
| R17 | 경보·확인·조치·복구 이력 저장, 비밀정보 미기록 | 경보 20단계 | ✅ `WH_SYS_ALERT_EVENT` |
| R18 | 안전한 자동 조치만(로그 로테이션), 데이터 삭제 금지, 재시작·재부팅 무조건 자동 금지 | 경보 20단계 | ✅ 자동 조치 **없음**(의도) / 로테이션은 🔧 서버 |
| R19 | 외부 생존 감시(서버 밖), 실제 검증 | 경보 20단계 | 🔧 운영 (방법은 runbook 3절) |
| R20 | 장애·복구 시험(디스크·CPU·메모리·프로세스·DB·홈페이지·SMTP·복구) 운영 데이터 훼손 없이 | 경보 20단계 | 📄 시험표 제공, 실행은 운영 |
| R21 | 7001 검증 후 7002~7010 확대, 호기별 설정·수신자 분리 | 경보 20단계 | ✅ 설계(`platform.id`·설정 테이블) / 적용은 🔧 |
| R22 | 감시 주기 명시, 주의는 대시보드만·위험부터 메일, 플래핑 억제 | 보완 지적 | ✅ 5분 주기, 2회 연속 확인, 메일 최소 등급 설정 |
| R23 | 15×15 통합 체크리스트 체크 → 보관·제출, 단시간 가능 작업 수행 | 통합 매뉴얼 | ✅ `checklist-15x15-2026-10-12.md` |
| R24 | Cloudflare 무료 설정 점검 후 빠진 것 보완, 종합 제출 | 보안조치 메일 | 🟡 외부 관측 + 점검표. 대시보드는 🔧 |
| R25 | 랜섬웨어·DDoS 사전 대비(기존 기능 활용, 최소 개발) | 보안조치 메일 | 📄 체크리스트 03 + 지침 43~45 |
| R26 | 시스템 관리 31~46항 텍스트 보관·제출, 47항은 별도 보안 조치로 보관·제출 | 31~47 메일 | ✅ `system-management-guide.md` / 47은 gitignore 로컬 문서 |

## 2. 구현 방안 — 실제 반영 내용

### 2-A. core-backend (전부 백엔드·어드민 JSP, core-frontend 변경 없음)

**이중인증**
- `TotpUtil` — RFC 6238(HMAC-SHA1·30초·6자리) 자체 구현. 외부 라이브러리 없이 JDK + 기존 commons-codec(Base32). RFC 테스트 벡터 5개 통과.
- `SecretCryptoUtil` — AES-GCM(256, 구형 JCE 정책이면 128 폴백) + 랜덤 IV. 키 재료는 `otp.encryption.key`(globals) / env `WITCH_OTP_KEY`.
- `AdminMfaService` — 등록 시작/확인, 검증(OTP·복구 코드), 잠금(5회/10분, DB 시간 기준), 재사용 방지(마지막 성공 시간 단계 저장), 복구 코드 재발급, SM 재설정(+토큰 무효화), 관리자 현황.
- `AdminAuditService` — 감사 로그. 기록 실패가 기능을 막지 않음.
- `SuperLoginController` — 비밀번호 통과 후 SM/LC 는 세션·토큰 없이 `MfaPendingVO` 만 두고 `/super/mfa/verify.do` 또는 `/super/mfa/setup.do` 로. 비밀번호 단계 성공/실패 감사.
- `SuperJwtInterceptor` — OTP 경로 5개 예외 + **회수된 토큰 거부**(`AdminSessionUtil.isRevoked`).
- `AdminSessionUtil` — 세션·JWT 쿠키 발급 일원화(`SameSite=Lax` 추가), 토큰 회수 기준점.
- `SuperMfaController` + JSP 4개(`mfa_verify`, `mfa_setup`(QR 은 브라우저에서 qrcodejs CDN 으로 렌더 — 비밀키가 서버 로그·URL 에 남지 않음), `mfa_manage`, `audit_log`).
- 매퍼 `security.xml`, DDL `db/admin-mfa-audit.sql` (WH_ADMIN_MFA · WH_ADMIN_MFA_BACKUP_CODE · WH_ADMIN_AUDIT_LOG).

**서버 경보**
- `AlertRules`(등급 규칙·기본값·설정 키), `AlertStateMachine`(연속 확인·격상·복구·재알림 상태 전이 — 순수 클래스, 13개 테스트), `SystemAlertMonitor`(5분 cron `0 2/5 * * * *`, 측정 8항목, 재기동 시 DB 열린 경보 복원), `AlertMailService`(기존 `mailSender` 재사용, 전용 스레드, 타임아웃 주입, 재시도), `SystemAlertService`(DB 기록·설정, 실패해도 감시 지속).
- `SuperAlertController` + `alert_center.jsp`(현황·열린 경보·확인·설정·시험 메일·이력·지금 점검), 사이드바 배지 + 위험 이상 상단 띠(60초 폴링).
- 매퍼 `sysalert.xml`, DDL `db/sys-alert.sql` (WH_SYS_ALERT · WH_SYS_ALERT_EVENT · WH_SYS_ALERT_SETTING).

**설정 키(globals.properties, 전부 선택·기본값 있음)**: `otp.encryption.key`(없으면 MFA 비활성+ERROR 로그+배너), `otp.mode`, `otp.required.auth`, `otp.issuer`, `alert.mail.from`, `alert.admin.url`, `platform.id`, `monitor.disk.path`(기존).

### 2-B. 문서 (DOCUMENTS/security-ops/)
- `otp-operator-guide.md` — 총운영사 사용 안내 + 분실·재설정 절차 + 서버 배포 체크리스트
- `server-alert-runbook.md` — 경보 운영 매뉴얼, 시험 방법, 외부 생존 감시, 7001 확대 적용
- `checklist-15x15-2026-10-12.md` — 225 항목 판정표 + 즉시 조치 6건
- `cloudflare-free-plan-checklist.md` — 외부 실측(헤더·DNS) + 대시보드 점검 18항 + 무료 플랜 한계
- `system-management-guide.md` — 31~46항을 실제 화면 기준으로 재작성, 47 포인터
- (저장소 밖) `core-backend/docs/ops/secrets-inventory-47.md` — 비밀정보 위치·조치. **별도 채널로 전달**

## 3. 충돌·불가 항목과 대안

| 요청서 내용 | 현 구조와의 차이 | 대안(적용함) |
|---|---|---|
| `admins` 테이블에 `otp_*` 컬럼 추가 | 관리자 계정은 스타와 같은 `WH_PRESS` 다. 넓은 공용 테이블에 비밀값 컬럼을 늘리지 않음 | 1:1 보조 테이블 `WH_ADMIN_MFA` + 복구 코드 테이블 |
| `temp_token` 발행 | 어드민 인증은 세션 + `SUPER_TOKEN` JWT 쿠키. 별도 토큰을 만들면 인터셉터가 두 체계를 봐야 함 | 세션 속성 `MfaPendingVO`(5분, 목적 구분, 시도 상한). 토큰·세션은 OTP 성공 뒤 `AdminSessionUtil.establish` 에서만 발급 |
| speakeasy/otplib, Nodemailer, os/child_process, setInterval | Node 전제 | JDK 표준 + commons-codec, 기존 `JavaMailSenderImpl`, MXBean + `/proc/meminfo`, Spring `@Scheduled` |
| 3분 주기 | 기존 지표 수집기가 5분, 스프링 스케줄러 스레드 1개 | 5분(수집기와 2분 어긋남). "지속" 판정은 2회 연속으로 |
| "Node.js 프로세스 중단 감지" | 톰캣이 곧 감시기 → 자기 다운은 못 봄(요청서도 인지) | 외부 생존 감시로 분리(🔧) |
| 메모리 캐시로 1시간 1회 중복 방지 | 재기동 시 중복 발송 | 항목당 열린 경보 1개 + DB 복원. 격상·긴급 재알림만 추가 발송 |
| 자동 재시작·보호 모드(07 대항목) | 요청서 자체가 "무조건 실행 금지" | 자동 조치 미구현(의도). 경보 → 사람 판단. 보호 모드는 Cost Guard 전제(비필수 목록) 확정 뒤 |
| 패스키·보안 키(04-02) | 어드민은 JSP 서버 렌더링, WebAuthn 은 별도 과제 | TOTP 만. 요청서도 "향후 고려" |
| OTP 재설정 후 기존 세션 무효화 | JWT 는 무상태 | 계정별 회수 시각(JVM 메모리) — 재기동 후 최대 2시간 창 존재. 더 엄격히 하려면 DB 조회가 요청마다 필요 |
| 15×15 중 서버·Cloudflare·외부 계정 항목 | 개발 측 접근 권한 없음 | 🔧 로 표기하고 기대 설정·검증 방법을 적어 운영 측에 넘김 |

## 4. 승인이 필요한 항목 (이번에 실행하지 않음)

| # | 항목 | 이유 |
|---|---|---|
| A1 | 운영 DB DDL 2종 실행 (`db/admin-mfa-audit.sql`, `db/sys-alert.sql`) | DB 마이그레이션. **MFA DDL 은 배포 전 필수** — 미실행이면 SM/LC 로그인이 fail-closed |
| A2 | `globals.properties` 에 `otp.encryption.key`(+`platform.id`) 추가, WAS 재기동 | 설정 파일. 없으면 MFA 비활성(기존 로그인 그대로) |
| A3 | **SMTP 앱 비밀번호 폐기·재발급** + `context-common.xml` 의 평문 자격증명을 globals 로 이전, `mail.debug=false` | 저장소(public)에 비밀번호가 있음. 보안 즉시 조치. XML 변경은 설정 파일 승인 |
| A4 | **JWT 서명 키** 를 소스 상수에서 globals 로 이전 + 교체 | 저장소에 있음 → SM 토큰 위조 가능. OTP 로도 못 막는 경로 |
| A5 | `web.xml` 세션 쿠키 `Secure`, 보안 헤더(또는 CF Transform Rule) | 설정 파일 / Cloudflare |
| A6 | Cloudflare: HSTS·최소 TLS 1.2·Rate Limit(관리자 로그인)·Bot Fight·Access(관리자 경로)·원본 IP 차단 | 외부 서비스 설정 |
| A7 | 외부 업타임 서비스 계약·등록 | 외부 서비스 |
| A8 | 어드민 경보 센터에 수신자 저장 + 시험 메일 실수신 | 운영자 연락처 |

## 5. 미결 질문 (클라이언트)

1. 경보 메일 **수신자 주소**(운영자 휴대전화 메일)와 발신 계정 — 현재 발신은 Gmail 개인 계정(`ztwo5595@gmail.com`)이며 A3 와 함께 회사 SMTP 로 바꿀지.
2. OTP 재설정의 **본인 확인 절차** 세부(전화·대면·서면)와 승인자 지정. 비상용 SM 계정을 따로 둘지.
3. 서버 **SSH/root 권한** 보유 여부(2-23차부터 미회신) — logrotate·방화벽·NTP·백업은 전부 이 전제.
4. **Cloudflare 계정 접근** 또는 운영 측 점검 담당자 — 점검표 18항목.
5. KT 서버 과금 방식 — 트래픽 임계값 경보·보호 모드(07) 착수 전제(Cost Guard 미결 동일).
6. 감사 로그·경보 이력 **보관 기간**(DDL 에 1년 예시만 주석).
7. 7001~7010 호기별 `platform.id`·수신자·암호화 키를 누가 생성·보관할지(Config Factory 의 `TODO_` 항목과 묶어서).

## 6. 검증 결과

| 항목 | 결과 |
|---|---|
| 컴파일 | `./mvnw.sh compile` 성공 |
| 단위 테스트 | `./mvnw.sh test` 32개 클래스 268건 전체 통과 — 신규 6개 클래스 50건(TOTP RFC 벡터 10·암복호화 7·복구 코드 규칙 7·등급 규칙 7·상태 전이 13·메일 본문 6) 포함, 기존 실패 0 |
| JSP 문법 | 신규 5개 + 수정 2개(sidebar·login)를 Jasper(Tomcat 7.0.47)로 번역·컴파일 — 전부 통과. Tomcat 7 EL 2.2 비호환(리스트 리터럴) 1건 발견·수정 |
| Spring 컨텍스트 기동 | 로컬 톰캣 8080 (아래 6-1) |
| 외부 실측 | `curl`/`dig` 로 Cloudflare 헤더·DNS 확인 → CF 점검표 1절 |
| **미검증** | DB 가 있어야 하는 E2E(등록→로그인→복구 코드→잠금, 경보 OPEN→메일→복구). 로컬은 운영 DB 에 붙어 있어 신규 테이블을 만들 수 없고(마이그레이션 승인 사항), 어드민 SM 자격증명도 개발 측에 없다. **DDL 적용 뒤 운영에서 아래 12항목을 실행해 기록** |

### 6-1. 로컬 기동 확인 (톰캣 8080, 운영 DB 연결, 신규 테이블·`otp.encryption.key` 없음 = 배포 전 상태 재현)

| 확인 | 결과 |
|---|---|
| Spring 컨텍스트 | 정상 기동. 신규 빈 6개 주입·매퍼 XML 2개 파싱 OK |
| 키 미설정 안전장치 | `[MFA] otp.encryption.key 미설정 — 관리자 이중인증이 비활성 상태로 기동됨` ERROR 1줄, 로그인은 기존대로 |
| `/super/login.do` | 200 |
| `/super/mfa/verify.do`, `/super/mfa/setup.do` (대기 상태 없이) | 302 → 로그인 |
| `/super/mfa/manage.do`, `/super/alert/center.do`, `/super/alert/count.json` (토큰 없이) | 302 → 로그인 (인터셉터) |
| `POST /super/mfa/verifyPrc.do`, `setupStart.do` (대기 상태 없이) | `{"status":"expired", redirectUrl:"/super/login.do"}` — 데이터 접근 없음 |
| `POST /super/loginPrc.do` 틀린 자격증명 | `{"status":"fail"}`, 감사 테이블 없음은 `[Audit] 기록 실패` 경고로만 처리(500 아님) |
| 경보 감시기 | 5분 주기 기동. 설정·이력 테이블 없음은 경고 후 기본값으로 계속(fail-soft) |

### 6-2. OTP 최종 테스트 12항목 (요청서 9절) — 운영 적용 후 실행용

| # | 시험 | 기대 |
|---|---|---|
| 1 | 올바른 비밀번호 + 올바른 OTP | 대시보드 진입, 감사 `OTP_LOGIN_SUCCESS` |
| 2 | 올바른 비밀번호 + 틀린 OTP | "남은 시도 N회", `OTP_LOGIN_FAIL` |
| 3 | 만료(5분 경과)·재사용된 대기 상태 | `expired` → 로그인 화면 |
| 4 | OTP 등록 전 `/super/dashboard.do` 직접 호출 | 로그인으로 리다이렉트(토큰 없음) |
| 5 | 등록 중단 후 10분 경과 → 확인 | "유효시간 지났다" + QR 재발급 |
| 6 | 복구 코드 최초 사용 | 로그인 성공, 남은 개수 안내, `OTP_BACKUP_CODE_USED` |
| 7 | 같은 복구 코드 재사용 | 실패 카운트 증가 |
| 8 | 재발급 후 이전 코드 | 실패 |
| 9 | 5회 연속 실패 | 10분 잠금, `OTP_LOCKED`, 잠금 중 입력은 즉시 거부 |
| 10 | 서버 시간 동기화 상태 | `timedatectl` NTP yes, 앱 번호 일치 |
| 11 | SM 재설정 후 대상의 기존 탭 새로고침 | 로그인 화면으로 (토큰 회수) |
| 12 | 로그·DB 에 비밀정보 | `catalina.out`·감사 로그·`WH_ADMIN_MFA` 에 비밀키·OTP·복구 코드 원문 없음 (암호문·해시만) |

## 7. 배포 순서

1. (A3·A4) 비밀정보 교체는 이 배포와 독립적으로 **먼저**
2. 운영 DB: `db/admin-mfa-audit.sql` → `db/sys-alert.sql`
3. `globals.properties`: `otp.encryption.key=…`, `platform.id=WITCH`
4. WAR 배포·재기동 → 로그 `[MFA] 관리자 이중인증 활성` 확인
5. SM 로그인 → OTP 등록 → 6-2 표 실행 → 경보 센터 수신자 저장 → 시험 메일 실수신
6. LC 관리자 등록 안내(`otp-operator-guide.md` 전달)
7. 외부 생존 감시 등록, Cloudflare 점검표 적용

## 8. 변경 파일

- 신규 Java 13: `common/util/TotpUtil`, `SecretCryptoUtil`, `AdminSessionUtil`; `admin/domain/MfaPendingVO`; `admin/service/AdminMfaService`, `AdminAuditService`, `AlertMailService`, `SystemAlertService`; `admin/scheduler/AlertRules`, `AlertStateMachine`, `SystemAlertMonitor`; `admin/controller/SuperMfaController`, `SuperAlertController`
- 수정 Java 3: `SuperLoginController`, `SuperJwtInterceptor`, `JwtUtil`
- 매퍼 2: `security.xml`, `sysalert.xml` · DDL 2: `db/admin-mfa-audit.sql`, `db/sys-alert.sql`
- JSP 신규 5 / 수정 1(`sidebar.jsp`)
- 테스트 6: `TotpUtilTest`, `SecretCryptoUtilTest`, `AdminMfaServiceTest`, `AlertRulesTest`, `AlertStateMachineTest`, `AlertMailServiceTest`
- 문서 5 + 로컬 1

# 시스템 관리 운영 지침 (요청서 31~47항 보관본, 2026-10-12)

클라이언트가 전달한 "31. System Management ~ 47. 비밀번호/Secret" 을 **이 프로젝트의 실제 화면과 기능 기준으로** 다시 적은 보관본이다.
원문의 Node.js/PM2 전제는 톰캣(Java)·MariaDB·Redis 로 바꿔 읽었다. 47번은 지시대로 별도 보안 채널 문서로 분리했다(끝 참조).

---

## 31. System Management — 어디서 보나

| 원문 항목 | 실제 위치 | 비고 |
|---|---|---|
| JVM Heap / Non-Heap / Active Threads / JVM 정보 / Garbage Collection / Thread Pool | 어드민 **시스템 관리** (`/super/system/panel.do`, SM 전용) — 5초 폴링 `status.json` | 2026-08 Cost Guard 지표 확장 |
| System CPU / Disk / Disk Trend / Directory Size | 같은 화면 (`SystemMetricsCollector` 5분 수집, 디렉터리는 30분) | 디렉터리는 `monitor.dir.*` 설정 필요 |
| Redis / Database(커넥션 풀) | 같은 화면 | PING·SELECT 1·DBCP 풀 |
| Outbound Traffic / API Request / Response Route | 같은 화면 (`/proc/net/dev`, 요청 카운터 필터, 상위 경로 10개) | Linux 전용 지표는 macOS 로컬에서 숨김 |
| Queue | **해당 없음** — 이 시스템에는 작업 큐가 없다 (푸시·집계는 동기 호출/스케줄러) | 도입 시 별도 과제 |
| **경보·알림** | 어드민 **경보 센터** (`/super/alert/center.do`) — 등급·열린 경보·메일·이력·설정 | 2026-10-12 신설 |
| **보안 감사** | 어드민 **감사 로그** (`/super/mfa/audit.do`), **보안 · 이중인증** (`/super/mfa/manage.do`) | 2026-10-12 신설 |

## 32. 시스템 모니터링 원칙

총운영사는 상태를 **보고 보고한다**. Core Infrastructure 는 바꾸지 않는다.

- 운영자가 보는 것: 정상/비정상, 장애 여부, 디스크 증가, CPU 이상, DB 이상, Redis 이상, API 오류(5xx), 트래픽 급증. → 시스템 관리 + 경보 센터
- HQ가 담당하는 것: Core Server, DB/Redis 아키텍처, API Core, 보안·WAF(Cloudflare), 인프라, 배포, 코어 패치.
- 경보 센터의 "확인" 버튼은 **봤다는 표시**다. 설정 변경(임계값·수신자)은 SM 만 가능하고 감사 로그에 남는다.
- 어드민의 "메모리 정리(GC)" 버튼 외에 서버 상태를 바꾸는 기능은 어드민에 **없다**. 재시작·설정 변경은 HQ 작업이다.

## 33. JVM Memory

| 상태 | 판단 기준 (시스템 관리 + 경보 센터) |
|---|---|
| 정상 | 힙 증가가 일시적이고 GC 뒤 내려옴. CPU·응답 정상 |
| 경고 | 힙이 계속 오르고(GC 뒤에도 바닥이 높아짐) GC 횟수·시간 증가, 스레드 증가 → 경보 센터 **JVM 힙 주의(85%)** |
| 장애 | OutOfMemory, 톰캣 재시작 반복, API 전체 장애 → **JVM 힙 위험(95%)** + HTTP 5xx 경보 |

조치 순서: GC 지표 확인 → 힙 덤프(HQ) → 최근 배포 확인 → 승인 후 재시작.

## 34. Disk Management

Total / Used / Free 는 시스템 관리에, 추세(GB/시간, 소진 예상일)는 Disk Trend 에, 임계값 경보는 경보 센터에 있다.
감시 대상 디렉터리: 업로드(`/var/lib/tomcat7/webapps/img`, `/video`), 로그(`catalina.out`, `log4j`), 임시 파일, 백업, 캐시.
2-29차에 로그 폭주 원인(log4j2 설정 버그, 요청당 43KB)을 고쳤으므로 현재 가장 큰 증가 요인은 **업로드 영상**이다.

## 35. Disk Guard

디스크 100% 도달을 막는다. **자동 삭제는 구현하지 않았다** — 요청서 16·35항의 "검증된 로그 로테이션과 보존기간 지난 지정 로그 정리만 허용" 원칙에 따라, 서버 쪽 logrotate(루트 권한)로 처리한다(로컬 `core-backend/docs/ops/logrotate-tomcat`).

- 자동관리 대상(서버 logrotate): 오래된 catalina 로그, 임시 파일, 캐시, 오래된 모니터링·내보내기 파일
- **삭제 금지**: 사용자 콘텐츠(업로드 이미지·영상), 거래·정산 자료(`WH_AD_LOG`), 법률상 보존 자료, DB 파일
- 디스크 95% 긴급 경보 시 첫 조치는 `server-alert-runbook.md` 8절

## 36. Redis

시스템 관리에서 UP/DOWN, used/peak memory, clients, keys, evicted 를 본다. 적중률(Hit/Miss)은 INFO 의 keyspace_hits/misses 로 추가 가능(미구현).
Redis 장애 시 앱은 **DB 폴백**으로 동작한다(2-29 CacheErrorHandler). 총운영사는 설정을 바꾸지 말고 HQ 에 Incident 를 등록한다. 경보 센터 등급은 **위험**(긴급 아님).

## 37. Database

Connection(풀 active/idle/max), 응답(SELECT 1), 오류는 시스템 관리·경보 센터에서. Slow Query·Lock·Storage·Replication 은 **DB 서버 쪽 설정이 필요**해 어드민에 없다(단일 DB, 복제 없음).
운영자가 DB 를 직접 수정하는 것은 원칙적으로 금지. OTP 우회 목적의 DB 수정도 금지(04 대항목).

## 38. API Monitoring

Request Count, 상태코드 분포(2xx/3xx/4xx/5xx), 상위 경로(응답 바이트 기준 10개)는 시스템 관리에 있다. 응답 시간(latency) 분포는 **미구현**.
핵심 경로: `/post/{id}`, `/star/{id}`(공개 웹), `/api/super/*`(앱), `/super/*`(어드민). 경보 센터는 5분 구간 5xx 가 20건·5% 이상이면 위험으로 알린다.

## 39. HTTP Status 관리

- 2xx 정상 / 4xx 사용자 요청·권한·입력 문제 / 5xx 서버 오류
- 5xx 급증 → 경보 센터가 Incident 후보를 만든다(위험). 담당자가 "확인" 하고 catalina.out 과 DB·Redis 경보를 함께 본다.

## 40. Outbound Traffic

NIC 송신 바이트(5분·1시간·24시간·당일·월 예상)와 앱 응답 바이트 비중이 시스템 관리에 있다.
급증 시 확인: 영상 다운로드(상위 경로에 `/video/*`), 이미지, 봇·크롤링(Cloudflare Security Events), 대용량 파일, API 남용, 외부 공격.
트래픽 **임계값 경보는 미구현** — KT 서버 과금 방식이 확인되면 추가(Cost Guard 미결).

## 41. Directory Monitoring

Upload / Log / Temp 디렉터리 용량은 30분마다 스캔해 시스템 관리에 표시(`monitor.dir.upload|log|temp` 설정 시). 파일 수·시간 상한이 있어 큰 디렉터리는 `truncated` 로 표시된다. Backup·Monitoring 디렉터리는 설정 키 추가로 확장 가능.

## 42. Garbage Collection

GC Count, Total GC Time, 빈도, 힙 변화는 시스템 관리에 있다. GC 시간이 비정상적으로 늘면 33항 절차로 JVM/메모리 분석을 한다.

## 43. 서버 장애 대응 — Incident Process

Detection(경보 센터/외부 감시) → Classification(44항 등급) → Notification(경보 메일·운영 채널) → Containment(Cloudflare Rate Limit·기능 제한) → Diagnosis(로그·지표) → Recovery → Verification(앱·웹·어드민 회귀) → Root Cause Analysis → Post-Incident Report(경보 ID·이력 첨부)

## 44. 장애 등급

| 등급 | 정의 | 예 | 경보 센터 대응 |
|---|---|---|---|
| P1 Critical | 전체 서비스 장애 | App/Web Down, DB 연결 실패 | 긴급(CRITICAL) — 재알림 |
| P2 Major | 핵심 기능 장애 | Login/Ranking, Redis 다운 + 지연 | 위험(DANGER) |
| P3 Normal | 일부 기능 장애 | 특정 메뉴, 5xx 소폭 증가 | 주의/위험 |
| P4 Minor | 경미한 문제 | UI/텍스트 | 경보 아님 |

실제 Response/Recovery 시간은 Annex 6 및 Commercial Order Form 의 SLA 를 따른다(계약 문서).

## 45. 보안사고

즉시: 이상행위 확인(감사 로그) → 계정 보호(비밀번호 변경·OTP 재설정·토큰 무효화) → 접근 제한(Cloudflare) → 증적 보존(catalina.out·감사 로그·경보 이력 복사) → HQ 통보 → 확산 차단.
개인정보 침해 의심 시 Annex 4 Incident Response 적용.

## 46. Admin 보안

모든 관리자 권장: 강력한 비밀번호, **MFA(이번 적용 — SM·LC 의무)**, 개별 계정, 계정 공유 금지, 최소권한(SM/LC 분리), 정기 권한 검토(감사 로그 + 관리자 현황 화면), 퇴사자 즉시 권한 회수(`PRS_USE_YN` 비활성 + OTP 재설정), API Secret 공유 금지.

## 47. 비밀번호 / Secret

원칙: DB Password, SSH Key, API Secret, OAuth Secret, Cloud Credential, Payment Credential, Encryption Key 를 메신저·문서에 평문으로 공유하지 않는다.
**현황 점검 결과와 조치 목록은 별도 보안 채널 문서**로 제출한다 (저장소 밖: `core-backend/docs/ops/secrets-inventory-47.md`, gitignore). 즉시 조치가 필요한 항목이 2건 있다(SMTP 앱 비밀번호, 관리자 토큰 서명 키).

# 05 호기별 자원 분리 매트릭스 (40·41·42단계)

`factory/platforms/platform-700x.json` 과 비밀 묶음 `platform-security-configs-2026-10-09.zip`(별도 전달, 방법 C) 의 값이 같은 규칙으로 생성돼 서로 맞는다.

| 호기 | 저장소 | DB 이름 | DB 계정 | Redis | 업로드 루트 | 도메인 | Android 패키지(제안) | iOS 번들(제안) |
|---|---|---|---|---|---|---|---|---|
| 7001 | platform-7001 | starplatform_7001 | sp7001_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7001 | kr.co.sensiblenews.platform7001 | kr.co.sensiblenews.platform7001 |
| 7002 | platform-7002 | starplatform_7002 | sp7002_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7002 | kr.co.sensiblenews.platform7002 | kr.co.sensiblenews.platform7002 |
| 7003 | platform-7003 | starplatform_7003 | sp7003_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7003 | kr.co.sensiblenews.platform7003 | kr.co.sensiblenews.platform7003 |
| 7004 | platform-7004 | starplatform_7004 | sp7004_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7004 | kr.co.sensiblenews.platform7004 | kr.co.sensiblenews.platform7004 |
| 7005 | platform-7005 | starplatform_7005 | sp7005_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7005 | kr.co.sensiblenews.platform7005 | kr.co.sensiblenews.platform7005 |
| 7006 | platform-7006 | starplatform_7006 | sp7006_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7006 | kr.co.sensiblenews.platform7006 | kr.co.sensiblenews.platform7006 |
| 7007 | platform-7007 | starplatform_7007 | sp7007_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7007 | kr.co.sensiblenews.platform7007 | kr.co.sensiblenews.platform7007 |
| 7008 | platform-7008 | starplatform_7008 | sp7008_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7008 | kr.co.sensiblenews.platform7008 | kr.co.sensiblenews.platform7008 |
| 7009 | platform-7009 | starplatform_7009 | sp7009_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7009 | kr.co.sensiblenews.platform7009 | kr.co.sensiblenews.platform7009 |
| 7010 | platform-7010 | starplatform_7010 | sp7010_app@localhost | 127.0.0.1:6379 (requirepass, 호기별 상이) | /var/lib/tomcat7/webapps | TODO_DOMAIN_7010 | kr.co.sensiblenews.platform7010 | kr.co.sensiblenews.platform7010 |

## 비밀 묶음 내용 (호기당 4파일, ZIP 에는 넣지 않음)

| 파일 | 내용 | 비밀 |
|---|---|---|
| `globals.properties` | 위 DB·Redis + Apple 자리표시자 + 모니터링 + 서명 비밀키 | DB·Redis 비밀번호, 서명키 |
| `.env` | 같은 값 + 관리자 초기 비밀번호 + 백업 암호화 구절 + Android 키스토어 비밀번호 | 전부 |
| `mysql-init.sql` | DB·계정 생성(localhost·127.0.0.1 만) | DB 비밀번호 |
| `redis.conf.snippet` | requirepass·bind·위험 명령 비활성 | Redis 비밀번호 |

생성 방식: `openssl rand` 난수. Master 운영 값은 읽지도 복사하지도 않았다. 호기 간 중복 값 없음(난수 길이 24~32).

## 판정

설계·설정은 준비됐지만 인스턴스가 없어 40·41·42 는 **NOT VERIFIED**. 서버가 생기면 `architecture.md` 의 "검증 방법" 열을 순서대로 실행하고 결과를 `05_Infrastructure/isolation-test-700x.log` 로 남긴다.

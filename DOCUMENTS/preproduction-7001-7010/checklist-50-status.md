# 50단계 상태표 (2026-10-11, 기준 master-baseline-v1.0 = cb7c05e)

판정 집계: **PASS 13 · IN PROGRESS 22 · FAIL 0 · NOT VERIFIED 15** / 50

판정 규칙은 원문 7절 그대로 적용했다. 설정·문서만 있는 항목은 PASS 로 올리지 않았고, 실운영 서버·호기 도메인·단말·스토어 계정이 있어야 확인되는 항목은 NOT VERIFIED 로 두었다. 증빙 경로는 제출 ZIP 기준이다.

## PART 1. Master 보호 및 기준 확정

| # | 단계 | 판정 | 증빙 | 비고 |
|---|---|---|---|---|
| 01 | 정상 운영 Master Source 확인 | **PASS** | `01_Master_Baseline/baseline.md` | Sensiblenews/starplatform, 운영 브랜치 feature/2-31-web-video-followup, cb7c05e, 운영 버전표(앱 10.0.72/100072·WAR 1.0.0) |
| 02 | 핵심 기능 Baseline 테스트 | **IN PROGRESS** | `06_QA_Test_Results/test-backend.log, test-frontend.log, screenshots/` | 백엔드 90 테스트·앱 42 스펙 통과, 광고·스타페이지·피드 실기기 캡처. Login·Chat·Commerce·Ranking·VS 는 수동 QA 미실시 |
| 03 | Master Source Backup | **PASS** | `07_Backup_Rollback/master-source-backup.log` | git bundle 별도 위치 생성 → clone 복구 → tree 해시 일치(3168 파일) |
| 04 | Master Repository 확정 | **PASS** | `02_GitHub_Repository/repositories.md, permissions.md` | 원본 1 + 사본 10 URL, 협업자·권한 표 |
| 05 | Production Branch 확정 | **PASS** | `01_Master_Baseline/protection-record.md` | 운영 기준 브랜치 확정 + main 보호 규칙 적용(PR 필수·force push·삭제 금지·관리자 포함) 적용 전후 기록 |
| 06 | 변경관리 규칙 설정 | **PASS** | `01_Master_Baseline/master-protection-policy.md` | 브랜치 보호 + PR 리뷰 절차 + 변경관리 규칙 문서. HQ 승인란 공란 |
| 07 | Baseline Git Tag 생성 | **PASS** | `01_Master_Baseline/baseline.md` | 태그 master-baseline-v1.0 = cb7c05e, 원격 푸시 완료 |
| 08 | Database Baseline 확정 | **IN PROGRESS** | `01_Master_Baseline/baseline.md §DB` | 저장소 DDL 이력(2-21~2-29 차수 문서) 목록. 라이브 스키마 덤프·마이그레이션 도구 없음 → 서버 작업 |
| 09 | Server·API·Redis 기준 확인 | **IN PROGRESS** | `05_Infrastructure/healthcheck-master.log, 04_Build_Artifacts/build-report.md` | 공개 엔드포인트 9개 200·TLS·Cloudflare 확인, 런타임 버전표. 서버 내부(톰캣·MariaDB·Redis 버전) 는 SSH 접근 뒤 |
| 10 | Master 보호 기준 승인 | **IN PROGRESS** | `01_Master_Baseline/master-protection-policy.md` | 문서 작성 완료, HQ 승인 대기 |

## PART 2. 플랫폼별 변경값 분리

| # | 단계 | 판정 | 증빙 | 비고 |
|---|---|---|---|---|
| 11 | 하드코딩 조사 | **PASS** | `03_Config_Factory/hardcoded-inventory.md` | 도메인 34파일·앱명 12·광고 ID·식별자·경로·브랜딩 자산 전수표 + 재현 명령 |
| 12 | Platform ID 분리 | **IN PROGRESS** | `03_Config_Factory/platforms/, 04_Build_Artifacts/build-web-7001.log` | platformId 설정 관리 + 7001 웹 번들 반영 확인. 앱(AAB·IPA) 빌드 반영은 미완 |
| 13 | App Name 분리 | **NOT VERIFIED** | `03_Config_Factory/inject-7001-run.log` | appName·strings.xml·title 주입 확인. 실제 앱 화면은 단말 설치 필요 |
| 14 | Domain 분리 | **NOT VERIFIED** | `—` | 호기 도메인 미확보(TODO_). DNS 조회 불가 |
| 15 | API URL 분리 | **IN PROGRESS** | `04_Build_Artifacts/build-web-7001.log` | 번들에 7001 apiBaseUrl 주입, Master 도메인 0건. 네트워크 로그는 실행 환경 필요 |
| 16 | Android Package ID 분리 | **IN PROGRESS** | `03_Config_Factory/inject-7001-run.log, 04_Build_Artifacts/build-aab.log` | trapeze PACKAGE_NAME 주입. 7001 AAB 는 미빌드(Master AAB 만 빌드) |
| 17 | iOS Bundle ID 분리 | **IN PROGRESS** | `03_Config_Factory/inject-7001-run.log` | trapeze BUNDLE_ID·capacitor appId 주입. Xcode 설정 확인은 Mac 서명 환경 |
| 18 | Scheme·Deep Link 분리 | **NOT VERIFIED** | `—` | 단말 테스트 필요. 설정은 trapeze·AASA·assetlinks 주입 |
| 19 | Branding·Assets 분리 | **IN PROGRESS** | `03_Config_Factory/hardcoded-inventory.md §7` | 자산 위치·교체 방식 조사 완료. 자동 교체 미구현(아이콘 도구 Node 24 불가) |
| 20 | 광고·버전·스토어 정보 분리 | **IN PROGRESS** | `03_Config_Factory/validate-run.log` | 광고 ID·버전·식별자를 Config 로 분리, 검증 통과. 스토어 메타데이터(설명·스크린샷) 는 범위 밖 |

## PART 3. Config Factory 구축

| # | 단계 | 판정 | 증빙 | 비고 |
|---|---|---|---|---|
| 21 | Config 표준 정의 | **PASS** | `03_Config_Factory/platform.config.schema.json` | 필수 항목·자료형·허용값·형식 규칙 + 교차 검증 규칙 |
| 22 | 7001 Config 작성 | **IN PROGRESS** | `03_Config_Factory/validate-run.log` | platform-7001.json 검증 PASS(경고). TODO_ 22건은 총운영사 자료 대기 |
| 23 | 7002~7010 Config 작성 | **IN PROGRESS** | `03_Config_Factory/validate-run.log` | 10개 파일 전부 존재·누락 0·중복 0. TODO_ 동일 |
| 24 | Config Validation 구현 | **PASS** | `03_Config_Factory/selftest.log, test-fixtures/` | 오류 픽스처 5종 FAIL, strict 완전 설정 PASS, 누락·URL·중복·자료형·production 미입력 탐지 |
| 25 | Config 변경 테스트 | **PASS** | `03_Config_Factory/config-change-test.log` | 7001 displayName·versionCode 변경 → 7001 출력 9파일만 변경, 7002 변화 없음 |

## PART 4. Build Injector·Native 자동화

| # | 단계 | 판정 | 증빙 | 비고 |
|---|---|---|---|---|
| 26 | Build Injector 구축 | **PASS** | `04_Build_Artifacts/build-web-7001.log` | Injector 적용 worktree 에서 ng build 성공, 번들에 주입값 확인, Master 체크아웃 불변 |
| 27 | Logo 자동 교체 | **IN PROGRESS** | `03_Config_Factory/hardcoded-inventory.md §7` | 미구현 — 로고 파일 매핑표부터 |
| 28 | Icon 자동 교체 | **IN PROGRESS** | `03_Config_Factory/hardcoded-inventory.md §7` | 미구현 — cordova-res 불가, @capacitor/assets 도입은 package.json 변경 승인 필요 |
| 29 | Splash 자동 교체 | **IN PROGRESS** | `03_Config_Factory/hardcoded-inventory.md §7` | 미구현 — 아이콘과 같은 도구 |
| 30 | Theme·App Name 자동 변경 | **IN PROGRESS** | `03_Config_Factory/inject-7001-run.log` | 앱 이름은 주입됨. 색상·테마는 토큰화 선행 필요 |
| 31 | Android ID 자동 변경 | **IN PROGRESS** | `03_Config_Factory/inject-7001-run.log` | applicationId 주입. 7001 AAB 메타데이터는 미빌드 |
| 32 | iOS ID 자동 변경 | **NOT VERIFIED** | `—` | 서명 설정은 Apple 계정·인증서 필요 |
| 33 | Scheme·Deep Link 자동 변경 | **NOT VERIFIED** | `—` | 단말 테스트 필요 |
| 34 | Universal Link·App Links 검증 | **NOT VERIFIED** | `05_Infrastructure/healthcheck-master.log` | Master AASA·assetlinks 200 확인. 호기 도메인·서명 지문 없음 |
| 35 | AdMob 설정 주입 | **IN PROGRESS** | `03_Config_Factory/README.md §admob.mode` | test/production 구분 주입 구현·검증기 차단 확인. 테스트 광고 실제 노출은 단말 |

## PART 5. Build·Infrastructure Factory

| # | 단계 | 판정 | 증빙 | 비고 |
|---|---|---|---|---|
| 36 | Web Build 자동화 | **PASS** | `04_Build_Artifacts/build-web.log, build-web-7001.log` | Master·7001 웹 번들 빌드 성공. CI 는 없음(초안만, ci/github-actions.example.yml) |
| 37 | Android Build 자동화 | **IN PROGRESS** | `04_Build_Artifacts/build-aab.log` | Master 미서명 AAB 빌드 성공(JDK 17). 서명·7001 반영·CI 미완 |
| 38 | iOS Build 자동화 | **NOT VERIFIED** | `04_Build_Artifacts/ios-archive-procedure.md` | 서명 자격·Xcode 아카이브 필요 |
| 39 | Server 독립 생성 | **NOT VERIFIED** | `05_Infrastructure/status.md` | 호기 서버 없음 |
| 40 | DB 독립 생성 | **NOT VERIFIED** | `05_Infrastructure/separation-matrix.md` | DB명·계정 설계 + mysql-init.sql(비밀 묶음) 준비. 인스턴스 없음 |
| 41 | Redis 독립 구성 | **NOT VERIFIED** | `05_Infrastructure/separation-matrix.md` | redis.conf.snippet 준비. 인스턴스 없음 |
| 42 | Storage·Auth·Secrets 분리 | **NOT VERIFIED** | `05_Infrastructure/separation-matrix.md` | 비밀 묶음 호기별 분리 생성. 접근 거부 테스트는 서버 |
| 43 | Domain·SSL·CDN·WAF 검증 | **NOT VERIFIED** | `05_Infrastructure/status.md` | 도메인 없음 |

## PART 6. QA·Rollback·Factory Acceptance

| # | 단계 | 판정 | 증빙 | 비고 |
|---|---|---|---|---|
| 44 | Config Validation Test | **PASS** | `03_Config_Factory/selftest.log` | 정상·오류 Config 자동 테스트 PASS, 잘못된 설정 차단 확인 |
| 45 | Build Validation Test | **IN PROGRESS** | `04_Build_Artifacts/build-report.md` | Web PASS·Android 미서명 PASS·iOS 미검증 |
| 46 | Infrastructure Validation Test | **NOT VERIFIED** | `05_Infrastructure/status.md` | 서버 없음 |
| 47 | Identity·Link Validation | **IN PROGRESS** | `06_QA_Test_Results/identity-matrix.md` | Config 에서 생성한 식별자 표(도메인·API·패키지·번들·딥링크). 실동작 확인은 단말·도메인 |
| 48 | Branding·Advertising Validation | **NOT VERIFIED** | `—` | 최종 화면 필요 |
| 49 | QA·Rollback Test | **IN PROGRESS** | `07_Backup_Rollback/rollback-test.log, backup-restore-dryrun.log` | 코드 롤백(이전 태그 WAR 재빌드) PASS. 서버·DB 복구 테스트 NOT VERIFIED |
| 50 | Factory Acceptance Test | **NOT VERIFIED** | `08_Manuals_Handover/fat-results.md` | 필수 항목 미해결(인프라·단말) → 승인 불가 상태 |

## NOT VERIFIED 15건의 공통 선행 조건

- 호기 서버 10대(또는 1대 선행) + 도메인 + Cloudflare 존재: 14·39·40·41·42·43·46
- 실기기(Android·iOS) 설치 테스트: 13·18·33·48
- Apple Developer·Google Play 계정·서명 자산: 32·38·34(지문)
- HQ 승인: 50

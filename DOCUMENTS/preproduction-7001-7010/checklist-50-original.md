# 사전 생산준비 50단계 — 클라이언트 원문 요지 (2026-10-11 수신)

총운영사(HQ)가 전달한 체크리스트의 단계·증빙 요건을 그대로 옮긴 것이다. 판정과 증빙 매핑은 `checklist-50-status.md` 에 있다.

## 기본 원칙 (원문 10항)

1. 기존 GitHub 계정 유지, 새 계정 만들지 않음
2. Repository 는 7001~7010 호기별로 분리
3. 정상 운영 중인 Master Source 보호
4. 공통 코드·Factory 템플릿은 활용하되 플랫폼마다 독립 빌드·배포·운영
5. Server·DB·Auth·Secrets·Storage 등 Production 자원 독립
6. Config 기반 자동 주입과 Build Factory 구축
7. 실제 테스트와 증빙이 확인된 항목만 완료 처리
8. 7001 Factory 검증이 끝나도 7002~7010 개별 검증 생략 금지
9. 개발 완료 / QA 완료 / 총운영사 공급 승인을 구분

## 판정 기준 (원문 7절)

| 판정 | 뜻 |
|---|---|
| PASS | 실제 테스트 통과 + 필수 조건 충족 + 증빙 확보 + 결과 기록 |
| IN PROGRESS | 작업 진행 중, 테스트 또는 증빙 미완료 |
| FAIL | 테스트 실패 또는 필수 조건 미충족, 재작업 필요 |
| NOT VERIFIED | 설정·문서만 존재, 실제 동작 확인 미완료, 증빙 부족 |

※ 설정 파일이나 문서가 존재한다는 이유만으로 PASS 처리하지 않는다.

## PART 1. Master 보호 및 기준 확정

| # | 단계 | 증빙 요건 |
|---|---|---|
| 01 | 정상 운영 Master Source 확인 | Repository URL, Commit SHA, 운영 버전표 |
| 02 | 핵심 기능 Baseline 테스트 | Login·StarPage·Ranking·VS·Content·Video·Chat·광고·Commerce 기능별 QA 결과 |
| 03 | Master Source Backup | 백업 기록, 복구 테스트 결과 |
| 04 | Master Repository 확정 | Repository URL, 권한 목록 |
| 05 | Production Branch 확정 | Branch 및 보호 규칙 |
| 06 | 변경관리 규칙 설정 | 변경 승인 절차 |
| 07 | Baseline Git Tag 생성 | Tag 이름, Commit SHA |
| 08 | Database Baseline 확정 | Schema 버전, Migration 목록 |
| 09 | Server·API·Redis 기준 확인 | 버전표, 헬스체크 결과 |
| 10 | Master 보호 기준 승인 | Master 보호 기준 문서 |

## PART 2. 플랫폼별 변경값 분리

| # | 단계 | 증빙 요건 |
|---|---|---|
| 11 | 하드코딩 조사 | 검색 결과 및 수정 목록 |
| 12 | Platform ID 분리 | Config 및 빌드 결과 |
| 13 | App Name 분리 | 실제 앱 화면 |
| 14 | Domain 분리 | 도메인 설정 및 DNS 조회 결과 |
| 15 | API URL 분리 | 네트워크 로그 |
| 16 | Android Package ID 분리 | Gradle 설정 및 APK/AAB 정보 |
| 17 | iOS Bundle ID 분리 | Xcode Build Settings |
| 18 | Scheme·Deep Link 분리 | 실제 단말 테스트 |
| 19 | Branding·Assets 분리 | 앱 화면 및 리소스 목록 |
| 20 | 광고·버전·스토어 정보 분리 | Config 검증 결과 |

## PART 3. Config Factory 구축

| # | 단계 | 증빙 요건 |
|---|---|---|
| 21 | Config 표준 정의 | Config Schema |
| 22 | 7001 Config 작성 | Config Validation 로그 |
| 23 | 7002~7010 Config 작성 | 파일 목록 및 누락 검사 결과 |
| 24 | Config Validation 구현 | 정상·오류 Config 테스트 결과 |
| 25 | Config 변경 테스트 | 변경 전후 비교 결과 |

## PART 4. Build Injector·Native 자동화

| # | 단계 | 증빙 요건 |
|---|---|---|
| 26 | Build Injector 구축 | 빌드 로그 및 산출물 |
| 27 | Logo 자동 교체 | 앱 화면 |
| 28 | Icon 자동 교체 | 실제 설치 앱 |
| 29 | Splash 자동 교체 | 단말 캡처 |
| 30 | Theme·App Name 자동 변경 | 실제 앱 화면 |
| 31 | Android ID 자동 변경 | AAB 메타데이터 |
| 32 | iOS ID 자동 변경 | Archive 설정 및 서명 검사 |
| 33 | Scheme·Deep Link 자동 변경 | 단말 테스트 결과 |
| 34 | Universal Link·App Links 검증 | 설정 파일과 단말 테스트 결과 |
| 35 | AdMob 설정 주입 | 광고 설정 검사 및 테스트 광고 결과 |

## PART 5. Build·Infrastructure Factory

| # | 단계 | 증빙 요건 |
|---|---|---|
| 36 | Web Build 자동화 | CI 로그 및 산출물 |
| 37 | Android Build 자동화 | CI 로그 및 AAB |
| 38 | iOS Build 자동화 | CI 로그 및 Archive |
| 39 | Server 독립 생성 | 배포 설정 및 헬스체크 |
| 40 | DB 독립 생성 | 연결 대상 및 격리 테스트 |
| 41 | Redis 독립 구성 | 연결 설정 및 격리 테스트 |
| 42 | Storage·Auth·Secrets 분리 | 권한표 및 접근 거부 테스트 |
| 43 | Domain·SSL·CDN·WAF 검증 | DNS 조회, TLS 검사, 헬스체크 |

## PART 6. QA·Rollback·Factory Acceptance

| # | 단계 | 증빙 요건 |
|---|---|---|
| 44 | Config Validation Test | 자동 테스트 결과 |
| 45 | Build Validation Test | CI 결과 및 산출물 |
| 46 | Infrastructure Validation Test | 통합 테스트 결과 |
| 47 | Identity·Link Validation | 플랫폼별 검증표 |
| 48 | Branding·Advertising Validation | 최종 화면 및 설정 검사 |
| 49 | QA·Rollback Test | 복구 전후 비교 및 로그 |
| 50 | Factory Acceptance Test | FAT 결과 및 승인 기록 |

## 7001호기 최종 승인 관문 (원문 8절, 10항목)

1. Master Baseline 및 복구 기준 확정
2. Repository 분리 및 권한 검증
3. Config 기반 설정 주입 검증
4. Web·Android·iOS 빌드 검증
5. Server·DB·Redis·Storage·Auth 독립성 검증
6. Domain·API·Deep Link·앱 식별자 검증
7. Branding·광고·스토어 설정 검증
8. 핵심 기능 통합 QA 통과
9. Rollback 및 복구 후 정상 동작 확인
10. 필수 증빙 및 최종 승인 기록 완료

7001 FAT 통과 = Factory 생산공정 승인. 7002~7010 도 각각 Config·빌드·인프라 독립성·QA 를 별도 검증.

## 운영 원칙 (원문 9·10절)

작업 완료 ≠ 검증 완료 ≠ 공급 승인. 단계·담당자 지정 → 구현 → 실제 테스트 → 로그·스크린샷·산출물 확보 → 개발·검증 담당자 확인 → 증빙 있는 항목만 PASS → FAIL 은 원인·담당·재검증 기한 기록 → 선행 조건 미충족 시 다음 승인 단계로 넘어가지 않음.

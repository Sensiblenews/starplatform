# 08 FAT (Factory Acceptance Test) 결과 — v1.0, 2026-10-11

범위: 7001호기 Factory 공정. 원문 8절 관문 10항목은 `gate-7001.md`, 50단계 상세는 `checklist-50-status.md`.

| 시험 | 방법 | 결과 | 증빙 |
|---|---|---|---|
| F-01 Config 스키마·검증기 | 11개 설정 + 오류 픽스처 5 + strict 1 | PASS | `03_Config_Factory/validate-run.log`, `selftest.log` |
| F-02 Injector 자가 검증 | master.json 주입 → 원본과 바이트 비교 | PASS (51파일, 차이 0) | `selftest.log` |
| F-03 7001 주입 | platform-7001.json → 51파일 치환 + globals 생성 | PASS | `inject-7001-run.log` |
| F-04 변경 격리 | 7001 값 변경 → 7001 산출물만 변함 | PASS | `config-change-test.log` |
| F-05 Web 빌드(7001) | worktree 주입 → ng build → 번들 검사 | PASS | `04_Build_Artifacts/build-web-7001.log` |
| F-06 WAR 빌드(Master) | ./mvnw.sh build + 테스트 90 | PASS | `build-war.log`, `06_QA_Test_Results/test-backend.log` |
| F-07 Android AAB(Master) | gradle bundleRelease (JDK 17) | PASS(미서명) | `build-aab.log` |
| F-08 iOS Archive | — | NOT VERIFIED | `ios-archive-procedure.md` |
| F-09 앱 단위 테스트 | Karma 42 스펙 | PASS | `06_QA_Test_Results/test-frontend.log` |
| F-10 Master 소스 백업·복구 | git bundle → clone → tree 해시 | PASS | `07_Backup_Rollback/master-source-backup.log` |
| F-11 코드 롤백 | 이전 태그 WAR 재빌드 | PASS | `rollback-test.log` |
| F-12 DB·파일 백업·복구 | 스크립트 dry-run | NOT VERIFIED(실행은 서버) | `backup-restore-dryrun.log` |
| F-13 저장소 분리·권한 | API 조회 10개 | PASS | `02_GitHub_Repository/repositories.md` |
| F-14 Master 보호 | 보호 규칙 적용 전후 | PASS | `01_Master_Baseline/protection-record.md` |
| F-15 인프라 격리 | — | NOT VERIFIED(서버 없음) | `05_Infrastructure/status.md` |
| F-16 단말 식별자·딥링크·브랜딩 | — | NOT VERIFIED | — |

**FAT 판정: 보완 요청(조건부).** Factory 코드·절차(F-01~F-07, F-09~F-11, F-13·14)는 통과했고, 인프라·단말·서명 의존 시험(F-08·F-12·F-15·F-16)은 호기 자원이 생긴 뒤 같은 패키지 v1.1 로 재제출한다.

| 역할 | 이름 | 판정 | 일자 |
|---|---|---|---|
| 개발진 자체 검증 | gkrkdals | PASS(범위 내) | 2026-10-11 |
| HQ 검수 | | ☐ PASS ☐ FAIL ☐ 보완 요청 | |

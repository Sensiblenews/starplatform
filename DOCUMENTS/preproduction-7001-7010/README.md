# 7001~7010호기 사전 생산준비 — 제출 패키지 문서 (DOCUMENTS/preproduction-7001-7010)

클라이언트의 「개발 결과물 제출 및 인수인계 표준」(2026-10-11) 에 맞춘 문서 원본이다. 제출 ZIP `SP_PreProduction_7001-7010_v1.0.zip` 은 이 문서들과 `factory/` 코드, 빌드·테스트 로그를 8개 폴더로 묶은 것이며 `MANIFEST.txt` 의 Commit SHA 가 이 저장소 커밋과 같다.

| 문서 | ZIP 위치 | 단계 |
|---|---|---|
| baseline.md, master-protection-policy.md, protection-record.md | 01_Master_Baseline | 01·05·06·07·08·09·10 |
| repositories.md, permissions.md, repo-report.txt, resync-snapshots.sh | 02_GitHub_Repository | 04·42 |
| (factory/ 코드), factory/hardcoded-inventory.md | 03_Config_Factory | 11·21~26·35·44 |
| build-report.md, ios-archive-procedure.md | 04_Build_Artifacts | 36·37·38·45 |
| architecture.md, separation-matrix.md, infra-status.md | 05_Infrastructure | 39~43·46 |
| checklist-50-original.md, checklist-50-status.md/.csv, gate-7001.md, identity-matrix.md, checklist-3sets-status.md/.csv, qa-fail-actions.md | 06_QA_Test_Results | 02·47·전체 |
| backup-policy.md, rollback-procedure.md | 07_Backup_Rollback | 03·49 |
| manual.md, handover-report.md, fat-results.md, submission-form.md | 08_Manuals_Handover / 루트 | 50 |

패키지 조립 스크립트와 로그는 저장소에 두지 않는다(빌드 산출물). 재조립은 `factory/factory.sh` 로 빌드·검증 로그를 다시 만들고 위 문서를 복사하면 된다.

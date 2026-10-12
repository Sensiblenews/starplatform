# 07 백업 정책 (⑥ Backup 증빙, 03·49단계)

## 대상과 주기

| 대상 | 내용 | 주기 | 보존 | 도구 |
|---|---|---|---|---|
| 소스 | Master 저장소 전체(태그 포함) | 태그 생성 시 | 영구(태그별) | `git bundle` → 별도 저장소. 이번 패키지: `master-source-backup.log` |
| DB | 호기 DB 전체 덤프(`--single-transaction --routines --triggers`) | 매일 03:00 | 일 7 · 주 4 · 월 6 | `factory/ops/backup.sh` |
| 업로드 파일 | `/img`, `/video`, `/video/thumnail` | 매일(DB 와 같은 아카이브) | 동일 | 〃 |
| 검수 디렉터리 | `moderation/pending`, `hidden`, `dm-report` | 매일 | 동일 | 〃 |
| 설정 | `globals.properties`, 배포 중인 `witch.war` | 매일 | 동일 | 〃 |
| 서명 자산 | Android 키스토어·비밀번호, Apple `.p8`·인증서, `google-services.json`, `GoogleService-Info.plist` | 변경 시 | 영구 | **백업 아카이브에 넣지 않는다.** 비밀정보 관리 시스템(또는 오프라인 금고)에 별도 보관 |

## 보호

- 아카이브는 `openssl enc -aes-256-cbc -pbkdf2` 로 암호화. 구절은 호기 `.env` 의 `BACKUP_ENCRYPTION_PASSPHRASE`(호기별 상이).
- 아카이브와 `.sha256` 을 함께 보관. 복구 전 해시 검증(`restore.sh` 가 자동 수행).
- 오프사이트: 서버 밖(다른 리전의 오브젝트 스토리지 또는 HQ 보관소)에 사본 1부. 삭제 방지(Object Lock/버전 관리) 적용 — 보안 체크리스트 12·13번과 연결.
- 백업 파일에 개인정보(회원 DB)가 포함되므로 접근 권한은 운영사 책임자 + HQ 로 제한하고 제출 ZIP 에는 절대 넣지 않는다.

## 복구 목표

- RPO 24시간(일 1회 백업). 더 짧게 필요하면 binlog 보존을 추가한다.
- RTO 2시간(서버 1대 재구성 + restore.sh + healthcheck).

## 현 판정

- 소스 백업·복구 테스트: **PASS**(`master-source-backup.log`).
- DB·파일 백업 실행·복구 테스트: **NOT VERIFIED** — Master 운영 서버에 개발진 SSH 가 없고, 운영 DB 에 대고 테스트하지 않는 원칙. 테스트 서버(또는 7001 서버)가 생기면 `backup.sh → restore.sh → healthcheck.sh` 를 실행하고 로그를 `07_Backup_Rollback/backup-restore-test-700x.log` 로 남긴다. 명령 확인용 dry-run 은 `backup-restore-dryrun.log`.

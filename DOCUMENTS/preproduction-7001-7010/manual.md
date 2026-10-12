# 08 개발·운영 매뉴얼 (⑦) — StarPlatform Master / 7001~7010호기

## 1. 설치·실행 (개발 환경)

| 구성 | 요구 | 명령 |
|---|---|---|
| 공통 | git, Node 20~24, JDK 8(백엔드)·JDK 17(Android gradle), Android Studio, Xcode(iOS, macOS) | |
| 프런트 | `cd core-frontend && npm i` | `ionic serve` (로컬), `ionic serve --external --public-host=<host>` |
| 백엔드 | JDK 8 자동 탐지 래퍼 | `cd core-backend && ./mvnw.sh run` (톰캣 포트 80) |
| 설정 | `core-backend/src/main/resources/META-INF/props/globals.properties`(미추적) | 호기 비밀 묶음에서 배치 |

새 clone 에서 빌드 전에 복사해야 하는 미추적 파일: `04_Build_Artifacts/build-report.md` "새 clone 에서 빌드하려면" 표.

## 2. 빌드·배포 명령

| 대상 | 명령 | 주의 |
|---|---|---|
| Web 번들 | `cd core-frontend && ionic build` (= `ng build`) | AOT(`--configuration=production`) 는 기존 NG6007 로 깨져 있음 — 팀 관례는 JIT |
| Android | `ionic capacitor build android` → Android Studio 에서 서명·AAB | **`npm run build-android`·`clean-build-*` 금지**(android/ 삭제) · 릴리스 전 `trapeze-android.yml` 버전 상향·커밋 · gradle 은 JDK 17 |
| iOS | `ionic build && npx cap copy ios` → Xcode Archive | `trapeze-ios.yml` 버전 상향 · trapeze 는 1회만 |
| WAR | `cd core-backend && ./mvnw.sh build` → `target/witch.war` | 테스트 포함: `./mvnw.sh build:test` |
| 배포(WAR) | 이전 WAR 보존 → 톰캣 중지 → `webapps/witch` 삭제 → 새 WAR 복사 → 시작 → `factory/ops/healthcheck.sh <domain>` | DDL 이 있으면 먼저 적용(차수 문서) |
| 호기 생산 | `factory/factory.sh validate --strict` → `factory.sh package 700x <out> <.env>` → 앱은 주입된 체크아웃에서 위 Android/iOS 명령 | `factory/README.md` |

## 3. 환경변수·설정

- 비밀 아닌 값: `factory/platforms/platform-700x.json`(스키마 `platform.config.schema.json`).
- 비밀: 호기 `.env`(비밀 묶음) → `inject.js --secrets` 가 `globals.properties` 로 변환. 서버에는 `globals.properties` 만 올라가고 `.env` 는 백업 스크립트용으로 600 권한 보관.
- 키 목록: `DB_PASSWORD`, `DB_ROOT_PASSWORD`, `REDIS_PASSWORD`, `ADMIN_INITIAL_PASSWORD`, `MEDIA_TOKEN_SECRET`, `LANDING_TOKEN_SECRET`, `BACKUP_ENCRYPTION_PASSPHRASE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, (Apple) `APPLE_KEY_ID`, `APPLE_SERVICE_ID`.
- `MEDIA_TOKEN_SECRET`·`LANDING_TOKEN_SECRET` 은 예약 키 — 현 코드가 설정에서 읽도록 바뀐 뒤 유효.

## 4. 장애 대응·복구

| 증상 | 1차 확인 | 조치 |
|---|---|---|
| 사이트 5xx / 앱 API 실패 | `healthcheck.sh`, 톰캣 `catalina.out`, 디스크(검수 pending 에 영상 최대 140MB 누적) | 톰캣 재시작 → 안 되면 `rollback-procedure.md` |
| 로그인 불가 | Apple/Google/Kakao 콜백 도메인·키 만료 | `globals.properties` apple.*·콘솔 확인 |
| 광고 미노출 | AdMob 콘솔 상태, `adb logcat -s AdMobVideo` | 콘솔(단위 유형·미디에이션) — 코드 외 |
| 검수 대기 글이 안 보임 | 어드민 "미디어 검수" 대기열 | 승인/차단 처리(의도된 동작) |
| 데이터 손상 | 직전 백업 해시 확인 | `restore.sh --db-only` → 기능 확인 |
| ads.txt 비어 있음(재시동 공백) | Cloudflare Worker `x-ads-fallback` 헤더 | Worker 동작 확인 |

## 5. 운영사 인계 절차 (호기당)

1. HQ: 호기 Config 확정(`TODO_` 0건, `validate --strict` PASS) + 비밀 묶음 전달(보안 채널).
2. 개발진: `factory.sh package` → Web·WAR + 앱 2종 빌드 → `04_Build_Artifacts` 형식으로 제출.
3. 운영사/HQ: 서버 준비(mysql-init → redis.conf → globals 배치 → WAR) → `healthcheck.sh`.
4. 격리 테스트(`architecture.md` 검증 방법 열) → 결과 로그 제출.
5. 스토어 등록(운영사 계정) → 단계적 출시.
6. 백업 스케줄 등록(`backup.sh` cron 03:00) + 첫 복구 테스트.
7. 인수인계서 서명 → `handover-report.md` 양식.

## 6. 참고 문서

`CLAUDE.md`(저장소 규칙), `core-backend/README.md`(빌드 상세), `factory/README.md`, `DOCUMENTS/release-notes-*.md`, `DOCUMENTS/runbook-video-reencode-2026-10-04.md`.

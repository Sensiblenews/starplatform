# 01 Master Baseline (01·05·07·08·09단계)

## 기준 버전

| 항목 | 값 |
|---|---|
| Repository | https://github.com/Sensiblenews/starplatform (public) |
| 운영 배포 기준 Branch | `feature/2-31-web-video-followup` (차수별 기능 브랜치가 여기로 스택 머지되고, `main` 은 따라잡기 PR 로 맞춘다) |
| Baseline Tag | `master-baseline-v1.0` (annotated) |
| Commit SHA | `cb7c05e09c390b0ff1eb4f0bb454094e95d31fc8` — PR #32 머지 커밋 |
| `main` | `7757a701aacf7b39e91798d51f5c83ff5c09997f` — PR #30 머지(2 PR 뒤처짐, 따라잡기 PR 예정) |
| 이전 기준 | `baseline-2026-10` = `08141c4` (platform-7001~7010 사본이 현재 이 기준) |
| 태그 생성·푸시 | 2026-10-11, `git push origin master-baseline-v1.0` |

## 운영 버전표

| 구성 | 버전 | 출처 |
|---|---|---|
| Android 앱 | 10.0.72 / versionCode 100072 (trapeze 선언값, 소스 오브 트루스) · 로컬 build.gradle 은 10.0.81/100081 (테스트 빌드 흔적) | `core-frontend/trapeze-android.yml` |
| iOS 앱 | 10.0.72 / build 100072 | `core-frontend/trapeze-ios.yml` |
| 백엔드 WAR | witch 1.0.0-BUILD-SNAPSHOT | `core-backend/pom.xml` |
| 프런트 스택 | Angular 16.2 · Ionic 8.2 · Capacitor 6 · TypeScript 5.1 | `core-frontend/package.json` |
| 백엔드 스택 | Java 8 · Spring 4.1.1 · 전자정부 3.5.0 · MyBatis 3.2.2 · Tomcat 7 | `core-backend/pom.xml` |
| 데이터 | MariaDB(MySQL Connector 8.0.11) · Redis(Jedis) | `globals.properties`(서버) |
| 공개 웹 | witch-hunting.com, Cloudflare 프록시, Let's Encrypt (만료 2026-11-23) | `05_Infrastructure/healthcheck-master.log` |

## baseline-2026-10 → master-baseline-v1.0 변경 (7 커밋, 10 파일, +308/−80)

```
cb7c05e Merge pull request #32 from Sensiblenews/fix/pending-video-owner-no-badge
551cc72 Merge pull request #31 from Sensiblenews/fix/android-native-ad-card-size
3ecfd73 fix(app): 검수 대기 영상의 작성자 본인 화면에서 Under review 표시 제거 — 사진과 동일하게 썸네일만
814b323 fix(android): 네이티브 광고 위 드래그가 웹뷰 스크롤을 막던 회귀 수정 — 탭/드래그 라우팅
6e5a4dd docs: Android 네이티브 광고 동영상 로그 수집 안내 (AdMobVideo 태그 읽는 법)
772be4e fix(android): 네이티브 광고 진단 로그 확장 — 채운 네트워크, 영상 표면 실측 크기
5511985 fix(android): 네이티브 광고 카드 — 스타 페이지는 iOS 와 같이 화면 폭+85, 로비는 가로 크리에이티브만 요청
```

```
A	DOCUMENTS/android-native-ad-video-log-guide-2026-10-09.md
A	core-frontend/platformConfigurement/android/app/src/main/java/kr/co/sensiblenews/witchHuntingVU2D7F2P7E/AdTouchRouterLayout.java
M	core-frontend/platformConfigurement/android/app/src/main/java/kr/co/sensiblenews/witchHuntingVU2D7F2P7E/MainActivity.java
M	core-frontend/platformConfigurement/android/app/src/main/java/kr/co/sensiblenews/witchHuntingVU2D7F2P7E/NativeBridge.java
M	core-frontend/src/app/pages/feed-detail/feed-detail.page.html
M	core-frontend/src/app/pages/feed-detail/feed-detail.page.scss
M	core-frontend/src/app/pages/star-page/star-page.page.html
M	core-frontend/src/app/pages/star-page/star-page.page.scss
M	core-frontend/src/app/pages/star-page/star-page.page.spec.ts
M	core-frontend/src/app/pages/star-page/star-page.page.ts
```

## DB Baseline (08단계 — IN PROGRESS)

- 스키마 마이그레이션 도구(Flyway 등)는 없다. 차수별 DDL 은 운영 DB 에 수동 적용했고 이력은 차수 문서에 있다: 2-21(WH_VS_CARD·STAR_CATEGORY), 2-25(VISIBLE_YN), 2-26(검수 MDR_STATUS), 2-27(PRS_BIO), 2-28(신고·차단), 2-29(티커·메신저·web-contact).
- 라이브 스키마 덤프(`mysqldump --no-data`)는 서버에서 DBA 권한으로 떠서 `db-schema-baseline.sql` 로 보관해야 한다 — 개발 PC 에서 운영 DB 자격으로 덤프하지 않았다(운영 비밀 취급 원칙).
- 호기 DB 는 이 스키마를 `mysql-init.sql`(비밀 묶음) 로 만든 빈 DB 에 적용해 시작한다.

## Server·API·Redis 기준 (09단계 — IN PROGRESS)

- 공개 엔드포인트 9개 전부 200, TLS 정상, Cloudflare 경유 확인: `05_Infrastructure/healthcheck-master.log`.
- 서버 내부 버전(톰캣 패치·MariaDB·Redis·OS)은 SSH 접근이 아직 개발진에 없어 미기록(보안 체크리스트 3번과 같은 미결).

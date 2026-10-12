# 04 빌드 보고 (③ 빌드 및 자동화 결과) — 2026-10-11, 기준 cb7c05e

CI/CD 는 없다(수동 빌드). 아래는 개발 PC(macOS, Apple Silicon)에서 실행한 로컬 빌드 기록이며 CI 초안은 `03_Config_Factory/ci/github-actions.example.yml`(비활성).

| 산출물 | 명령 | 결과 | 소요 | 해시(SHA-256) / 위치 |
|---|---|---|---|---|
| Web 번들 (Master) | `cd core-frontend && npx ng build` | PASS (경고: CommonJS 의존성 1건, 기존) | 2.4s(증분) | www/ tar: `77b34d48653c0671bcbc16590968b7b44fd06b9d8510c7a3e16b8910b18f2d2f` · 로그 `build-web.log` |
| Web 번들 (7001 주입) | worktree 주입 → `npx ng build` | PASS — 번들에 7001 apiBaseUrl 포함, Master 도메인 0건, 제목 치환 | — | `build-web-7001.log` |
| 백엔드 WAR | `cd core-backend && ./mvnw.sh build` | PASS (테스트 90건 포함 통과) | 4.8s(증분) | `1552c214929392a57c60004be363367c0cca6930c99e18c9e8c9d2ce45104e1b` · 62,978,318 B · 파일은 방법 C(대용량) |
| Android AAB (Master) | `cd core-frontend/android && JAVA_HOME=<JDK17> ./gradlew :app:bundleRelease --offline` | PASS — **미서명** (release signingConfig 없음, 서명은 Android Studio 에서 키스토어로) | 11s(증분) | `4e622f30155969aad6d01971651b668953ad0b0e0b35078ba3068bfd1a6901d8` · 31,156,661 B · `build-aab.log` |
| iOS Archive | Xcode > Product > Archive | NOT VERIFIED — 서명 자격·배포 인증서 필요 | — | 절차 `ios-archive-procedure.md` |

## 버전 기록

| 항목 | 값 | 비고 |
|---|---|---|
| trapeze-android.yml | 10.0.72 / 100072 | 릴리스 소스 오브 트루스 |
| android/app/build.gradle (로컬) | 10.0.81 / 100081 | 테스트 빌드 흔적. AAB 메타데이터도 이 값. 릴리스 전 yml 을 Play Console 최고값+1 로 올리고 커밋 |
| trapeze-ios.yml | 10.0.72 / 100072 | |
| JDK | 백엔드 Zulu 8 / Android Gradle Corretto 17 | 기본 JDK 21 로는 `Unknown Kotlin JVM target: 21`(kakao 플러그인) — 첫 시도 실패 기록 `build-aab.log` 의 재시도 전 오류 |

## 오류 기록

| 시도 | 오류 | 조치 |
|---|---|---|
| AAB 1차 | `Unknown Kotlin JVM target: 21` (gradle 이 JDK 21 선택) | JAVA_HOME 을 JDK 17 로 지정해 재실행 → 성공 |
| Karma 1차 | `should create` 스캐폴드 스펙 5건 실패(기존 파손, 2026-08 메모) | 실제 로직 스펙(star-page 21 + deep-link 21)만 실행 → 42 통과 |

## 새 clone 에서 빌드하려면 추가로 필요한 파일 (저장소 미추적)

| 파일 | 이유 | 출처 |
|---|---|---|
| `core-frontend/src/app/constants/Keys/AdMob.ts` | `.gitignore`(`**/AdMob.ts`). 없으면 ng build 실패 | 운영본 복사 또는 Injector 가 생성하도록 개선(권고: 비밀 아니므로 추적 전환) |
| `core-frontend/android/` 전체 | 미추적 작업본(`google-services.json`, 런처 아이콘, tick.mp3 포함) | `android.zip` 백업 + `platformConfigurement/android` |
| `core-backend/src/main/resources/META-INF/props/globals.properties` | 비밀 | 호기 비밀 묶음 |
| `core-backend/src/main/resources/META-INF/message/` | `.gitignore` 대상. 없으면 메시지 번들 누락 | 운영본 복사 — 추적 전환 권고 |
| `core-backend/src/main/resources/apple/`, `firebase/` | Apple 키·Firebase 서비스 계정 | 호기별 계정에서 발급 |

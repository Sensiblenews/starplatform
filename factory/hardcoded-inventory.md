# 하드코딩 전수 조사 — 플랫폼 고유값 (11단계 증빙)

기준: `master-baseline-v1.0` (cb7c05e), 2026-10-11. 검색 명령은 맨 아래. "Injector" 열은 `factory/inject.js` 가 지금 바꿔 주는지, "후속" 열은 코드 수정이 더 필요한지다.

## 1. 도메인 `witch-hunting.com` — 34개 파일

| 위치 | 건수 | 용도 | Injector | 후속 |
|---|---|---|---|---|
| `core-frontend/src/environments/environment.prod.ts`, `environment.ts` | 1+1 | apiBaseURL | ✅ | 둘 다 `production: true` 인 구조는 그대로 |
| `core-frontend/src/app/` TS 8개 (app.component, login-modal, detail-page, feed-detail, market-menu-popover, star-page, deep-link.service, helper.service) | 14 | 공유 URL·딥링크 해석·앱 다운로드 안내 | ✅ | `environment.apiBaseURL` 로 모아 쓰면 치환 지점이 1곳으로 줄어든다 |
| `core-frontend/trapeze-android.yml` DEEPLINK_HOST, `trapeze-ios.yml` applinks | 1+1 | App Links / Universal Links 호스트 | ✅ | — |
| `core-backend/.../common/Constants.java` | 4 | `domain`, `_FILE_URL`, `_VIDEO_FILE_URL`, `_VIDEO_THUMNAIL_FILE_URL` | ✅ | 설정(globals) 로 빼면 WAR 재빌드 없이 호기 전환 가능 |
| 백엔드 Java 7개 (SuperAdminController, AppleController, DeepLinkController, SuperAppService, FileUtil, ImageModerationUtil, WebCardUtil) | 15 | OG 메타·Apple 리다이렉트·딥링크·파일 URL | ✅ | Constants.domain 참조로 통일 권장 |
| JSP 10개 (about, contact, content_landing, deeplink_redirect, faq, home, posts, info, super/sidebar, super/web_contact) | 22 | canonical·OG·JSON-LD·문의 메일·관리자 링크 | ✅ | — |
| 테스트 5개 (`DeepLinkController*Test`, `PublicWebControllerTest`, `ImageModerationUtilTest`, `WebCardUtilVideoTest`) | — | 기대값 | ✖(의도적) | 도메인을 바꾸면 테스트 기대값도 Constants 참조로 바꿔야 한다 |

## 2. 앱 이름 / 표기

| 위치 | 값 | Injector | 후속 |
|---|---|---|---|
| `capacitor.config.ts` appName | Star Platform | ✅ | |
| `platformConfigurement/android/.../strings.xml` app_name·title_activity_main | Star Platform | ✅ | `android/` 작업본도 있으면 함께 |
| `trapeze-ios.yml` displayName·FacebookDisplayName | 스타플랫폼 | ✅ | productName `earthLotto` 는 레거시 이름, 그대로 |
| `src/index.html` `<title>` | 스타플랫폼 | ✅ | |
| 화면 문자열 11개 파일 ("Star Platform"/"스타플랫폼") | | ✅ | 안내문 문장 안에 들어 있어 i18n 키로 빼는 것이 정석 |
| `constants/Keys/FacebookAPI.ts` APP_NAME | earthlotto | ✖ | Facebook 콘솔 표시명과 무관, 유지 |

## 3. 앱 식별자

| 항목 | 위치 | Injector | 후속 |
|---|---|---|---|
| Android applicationId | `trapeze-android.yml` PACKAGE_NAME → build.gradle | ✅ | Java 패키지 디렉터리 `kr/co/sensiblenews/witchHuntingVU2D7F2P7E` 와 `proguard-rules.pro`·`GlobalApplication` 참조는 수동(별도 작업) |
| Android custom scheme·package_name | `strings.xml` | ✅ | |
| iOS bundleId | `trapeze-ios.yml` BUNDLE_ID, `capacitor.config.ts` appId | ✅ | `project.pbxproj` 는 trapeze 가 씀 |
| iOS 커스텀 스킴 `witchhunting` | `trapeze-ios.yml` CFBundleURLSchemes | ✅ | |
| AASA appID (`VU2D7F2P7E.kr.co.sensiblenews.witchHunting`) | `.well-known/apple-app-site-association` | ✅ | 호기 Apple Team ID 필요 |
| assetlinks package_name | `.well-known/assetlinks.json` | ✅ | **서명 지문 2개는 호기 키스토어 생성 뒤 수동** |
| 인앱 상품 ID `kr.co.sensiblenews.witchhunting.inapp.*` | `constants/StoreItems.ts`, `store.service.ts` | ✖ | 스토어 상품을 호기별로 만든 뒤 매핑(Commerce 분리 과제) |
| Apple 로그인 clientId·teamId | `globals.properties` | ✅(템플릿) | `.env` 의 APPLE_* |

## 4. 광고

| 항목 | 위치 | Injector | 후속 |
|---|---|---|---|
| AdMob App ID (Android·iOS) | `trapeze-android.yml`·`trapeze-ios.yml` ADMOB_APP_ID | ✅ | |
| 배너·전면 단위 | `constants/Keys/AdMob.ts` (4개) | ✅ | **파일이 `.gitignore`(`**/AdMob.ts`) 대상 → 새 clone·스냅샷 저장소에는 없다. 빌드하려면 운영본에서 복사해 넣어야 한다.** 비밀이 아니므로 추적 대상으로 바꾸는 것을 권고 |
| 전면 단위(로비 AdMobService 초기화) | `app.component.ts` adId | ✅ | AdMob.ts 와 중복 — 한 곳으로 |
| 네이티브 단위 | Android `MainActivity.java`, iOS `MainViewController.swift` | ✅ | 네이티브 코드에 박혀 있음. 플러그인 인자로 받게 바꾸면 웹 설정으로 통일 |
| 테스트 단위 | `ad-mob.service.ts` TEST_* 상수 | ✖ | 구글 공개 값, 호기 무관 |
| AdSense 게시자 `ca-pub-9109251900558498` | JSP 6개 | ✅ | 호기별 AdSense 계정이 생기면 값 교체. `ads.txt` 는 Cloudflare Worker 폴백과 함께 호기별 재설정 |

## 5. SNS 로그인·푸시·분석

| 항목 | 위치 | Injector | 후속 |
|---|---|---|---|
| Kakao 네이티브 키·JS 키 | `trapeze-*.yml`, `constants/Keys/KakaoAPI.ts`, iOS `Info.plist`·`AppDelegate.swift` | ✅(yml·ts) / ✖(iOS 소스는 trapeze 가 plist 에 씀) | |
| Facebook App ID·Client Token | `trapeze-*.yml`, `constants/Keys/FacebookAPI.ts`, iOS `Info.plist` | ✅ | |
| Google 로그인 clientId | `capacitor.config.ts`, `strings.xml` | ✅ | `google-services.json`(Android, 미추적)·`GoogleService-Info.plist`(iOS) 는 호기 Firebase 프로젝트에서 받아 직접 배치 |
| Firebase 서버 키 | `core-backend/src/main/resources/firebase/` (미추적) | ✖ | 호기별 서비스 계정 파일을 서버에 배치 |
| Apple 키 `.p8` | `core-backend/src/main/resources/apple/`, 서버 `/var/lib/tomcat7/keys` | ✖ | 호기별 Apple 계정 |

## 6. 서버·저장소 경로

| 항목 | 위치 | Injector | 후속 |
|---|---|---|---|
| 업로드·영상·썸네일·검수·DM 신고 디렉터리 (`/var/lib/tomcat7/...`) | `Constants.java` 6개 상수, `ContentService`·`DmService` | ✖ | 호기별 서버가 같은 경로를 쓰면 문제 없음(서버가 분리되므로). 한 서버에 두 호기를 올릴 때만 설정화 필요 |
| DB·Redis 접속 | `globals.properties`(미추적) | ✅(템플릿 + `.env`) | |
| 모니터링 경로 | `globals.properties` monitor.dir.* | ✅(템플릿) | |
| `META-INF/message/`, `META-INF/props/`, `resources/apple|firebase/` | `.gitignore` 대상 | — | **새 clone 에서 WAR 빌드·기동에 필요한 파일이 저장소에 없다.** 인계 시 "서버에서 복사해야 하는 파일 목록" 으로 관리(`DOCUMENTS/preproduction-7001-7010/manual.md` 참조) |

## 7. 브랜딩 자산·테마 (27~30단계, Injector 범위 밖)

| 항목 | 위치 | 상태 |
|---|---|---|
| 런처 아이콘 | `android/app/src/main/res/mipmap-*/ic_launcher*.webp`(미추적), iOS `Assets.xcassets` | 자동 생성 도구 `cordova-res` 가 Node 24 에서 sharp 오류로 불가. 대체(`@capacitor/assets`) 는 `package.json` 변경이라 승인 필요 |
| 스플래시 | `android/.../drawable*/splash.png`, iOS `Splash.imageset`, `resources/splash.png` | 동일 |
| 로비 로고·히어로 이미지 | `src/assets/img/` (0101.svg 등 번호 파일명) | 파일 교체 방식. 매핑표 필요 |
| 색상 테마 | `src/theme/variables.scss` Ionic 변수 54개 + 화면별 직접 지정 색 | 토큰화 선행 |
| 스플래시 배경색 `#111013` | `capacitor.config.ts` 2곳, Android `colors.xml` | 토큰화 대상 |

## 검색 명령 (재현)

```bash
grep -rln 'witch-hunting\.com' core-frontend/src core-backend/src --include='*.ts' --include='*.java' --include='*.jsp' --include='*.xml' --include='*.html'
grep -rln "Star Platform\|스타플랫폼" core-frontend/src --include='*.ts' --include='*.html'
grep -rhoE 'ca-app-pub-[0-9]{16}[/~][0-9]{10}' core-frontend | sort | uniq -c
grep -rhoE 'ca-pub-[0-9]{16}' core-backend/src | sort | uniq -c
grep -rln 'kr.co.sensiblenews' core-frontend core-backend/src --exclude-dir=node_modules --exclude-dir=android --exclude-dir=ios
git status --short --ignored core-frontend/src core-backend/src | grep '^!!'
```

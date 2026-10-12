# Config Factory (7001~7010호기 사전 생산준비)

Master(현 StarPlatform 운영 소스)에 박힌 플랫폼 고유값을 `platform.config.json` 하나로 바꿔 끼우는 도구다. 의존성 없이 Node 18+ 와 zsh 만 쓴다(`package.json` 변경 없음).

| 파일 | 역할 |
|---|---|
| `platform.config.schema.json` | 설정 표준(JSON Schema draft-07). 필수 항목·자료형·허용값·형식 |
| `platforms/master.json` | 현 운영본 값. Injector 자가 검증 기준 |
| `platforms/platform-7001.json` … `7010.json` | 호기별 설정. 총운영사 자료가 와야 하는 값은 `TODO_` 자리표시자 |
| `validate.js` | 스키마 + 파일 간 중복(패키지명·번들 ID·도메인·DB명) + 교차 일관성 검사. 종료코드 0/1 |
| `inject.js` | Build Injector. 설정값을 51개 파일에 주입하고 `globals.properties` 를 템플릿에서 생성 |
| `globals.properties.template` | 백엔드 설정 템플릿. 비밀값은 `.env` 에서 |
| `test-fixtures/` | 검증기 오류 픽스처 5종 + strict 통과 예시 |
| `ci/github-actions.example.yml` | CI 초안(비활성) |
| `factory.sh` | 위 전부를 묶은 실행 래퍼 |

## 실행 명령

```bash
factory/factory.sh validate                 # 11개 설정 검증 (TODO_ 는 경고)
factory/factory.sh validate --strict        # TODO_ 도 오류 — 생산 직전 게이트
factory/factory.sh selftest                 # 정상·오류 픽스처 + Injector 자가 검증
factory/factory.sh inject 7001 /tmp/out-7001 ~/secrets/platform-7001/.env   # 체크아웃은 그대로, 결과만 out 에
factory/factory.sh package 7001 /tmp/pkg-7001 ~/secrets/platform-7001/.env  # worktree 에 주입 → web+war 빌드 → 해시
```

`inject.js --apply <checkout>` 은 체크아웃을 직접 바꾼다. 이때도 `globals.properties` 는 덮지 않고 `factory/.generated/` 에만 생성한다(운영 설정 파일은 승인 없이 수정 금지). Android/iOS 앱은 주입된 체크아웃에서 `ionic capacitor build android|ios` 로 빌드한다. `npm run build-android` 류는 `android/` 를 지우므로 쓰지 않는다.

## 비밀값

설정 파일에는 비밀이 없다. `secretsRef.keys` 에 적힌 키를 `.env`(KEY=VALUE) 로 넘긴다. 호기별 `.env` 는 별도 전달된 보안 설정 묶음(`platform-security-configs-*.zip`, 저장소 밖)에 있다. `--allow-missing-secrets` 는 `${KEY}` 자리표시자를 남긴 채 진행하는 개발용 옵션이다.

## admob.mode

`test` 면 구글 공개 테스트 단위 ID 로 주입되고, `production` 이면 설정의 단위 ID 를 쓴다. `production` 인데 `TODO_` 가 남아 있으면 검증기가 막는다(35단계).

## 주입 대상 (51개 파일)

- 앱 식별: `trapeze-android.yml`(패키지명·버전·딥링크 호스트·AdMob App ID·Kakao·Facebook), `trapeze-ios.yml`(번들 ID·버전·표시명·applinks·커스텀 스킴), `capacitor.config.ts`(appId·appName·Google 클라이언트), `strings.xml`
- 네트워크: `environment.ts`·`environment.prod.ts` 의 apiBaseURL, 백엔드 `Constants.java` 의 domain·파일 URL, 도메인이 박힌 Java 7·JSP 10·TS 8 파일
- 광고: `AdMob.ts`, `app.component.ts`, Android `MainActivity.java`·iOS `MainViewController.swift` 네이티브 단위, JSP 6개의 AdSense 게시자 ID
- 딥링크 검증 파일: `apple-app-site-association` appID, `assetlinks.json` package_name
- 표기명: `index.html` 제목, 화면 문자열 11개 파일의 "Star Platform"/"스타플랫폼"
- 백엔드 설정: `globals.properties`(템플릿 생성)

## 한계 (다음 단계 범위)

- **Java 패키지 디렉터리**(`kr/co/sensiblenews/witchHuntingVU2D7F2P7E`)는 바꾸지 않는다. applicationId 만 바뀌어도 빌드·설치는 되지만 패키지 경로 정리는 별도 작업이다.
- **assetlinks.json 의 서명 지문**은 호기 키스토어를 만든 뒤 수동으로 넣는다.
- **로고·아이콘·스플래시·색상 테마**는 교체하지 않는다(27~30단계). 아이콘 생성 도구 `cordova-res` 가 Node 24 에서 깨져 있어 대체 도구 승인이 먼저다.
- **기능 플래그(`features`)** 는 설정에만 있고 코드 분기는 아직 없다.
- **`google-services.json`·`GoogleService-Info.plist`·키스토어·p8** 은 다루지 않는다. 호기별 Firebase/Apple 계정에서 받아 직접 놓는다.
- 저장소 디렉터리 경로(`/var/lib/tomcat7/...`)는 `Constants.java` 에 상수로 박혀 있어 설정의 `uploadRoot` 는 `globals.properties` 의 모니터링 경로에만 반영된다.

전수 조사 결과는 `hardcoded-inventory.md` 참조.

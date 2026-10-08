# 2-32차 요청서 검토 (2026-10-08)

요청서 3건 중 **3. 생산 팩토리 사전 준비는 다음 작업으로 보류**. 아래는 1·2번만 다룬다.
요청서에 섞여 온 코드·CSS·AndroidManifest 예시·"STEP N" 순서는 참고 자료로만 보고 스펙으로 쓰지 않았다.

---

## 1. 요구사항 목록

| # | 요구사항 | 출처 |
|---|---|---|
| R1 | PC 웹·모바일 웹 AdSense 광고 자리 픽셀을 첨부 규격(PC 5종·모바일 5종)에 맞춰 고정 | 1번 + 규격 메일 |
| R2 | 광고 로딩 전 레이아웃 밀림(CLS) 방지를 위해 브레이크포인트별 최소 높이 확보 | 규격 메일 3절 |
| R3 | 어드민 사전 검수가 사진만 다루는 것을 동영상까지 확장 | 2번 |
| R4 | iOS는 네이티브·전면 광고에서 동영상 광고가 나오는데 Android는 안 나옴 → Android도 나오게 | 2번 + 별도 메일 |
| R5 | (별도 메일) Android 진단 시 로그 5종 확보: 하드웨어 가속 / NativeAd 로드 / mediaContent / hasVideoContent / MediaView 실측 크기 | 별도 메일 |
| R6 | (별도 메일) 구글 테스트 광고 단위로 먼저 확인해 "앱 구현 문제 vs 인벤토리 문제" 분리 | 별도 메일 |

---

## 2. 코드 분석 결과

### 2-A. Android 동영상 광고 — 네이티브는 **코드상 동영상이 나올 수 없는 구조**, 전면은 코드 문제 아님

별도 메일의 진단 순서(하드웨어 가속 → MediaView → hasVideoContent → 레이어 → 인벤토리)를 코드로 따라가 본 결과다.

| 점검 항목 | 결과 | 근거 |
|---|---|---|
| 하드웨어 가속 | **정상**. `<application android:hardwareAccelerated="true">`, MainActivity 는 별도 지정 없이 상속 | `android/app/src/main/AndroidManifest.xml:16` |
| MediaView 존재 | **없음**. 실제 쓰이는 `ad_layout.xml` 의 미디어 자리가 `ImageView` 다. `MediaView` 가 있는 `native_ad_layout.xml` 은 아무 데서도 inflate 하지 않는 죽은 파일 | `android/app/src/main/res/layout/ad_layout.xml` (`<ImageView android:id="@+id/ad_media">`), `MainActivity.java:146` (`R.layout.ad_layout`) |
| NativeAdView 구성 | **규격 위반**. `NativeAdView` 가 카드 전체가 아니라 CTA 버튼 하나만 감싼다. `setMediaView` / `setHeadlineView` / `setIconView` 등록이 전혀 없고 `setCallToActionView` 만 있다 | `ad_layout.xml` 하단 `<NativeAdView android:id="@+id/ad_call_to_action">`, `MainActivity.java:178-192` |
| 미디어 바인딩 | 동영상 광고가 와도 `getMediaContent().getMainImage()` 의 정지 이미지를 `ImageView` 에 넣는다. SDK 가 동영상을 그릴 곳이 없다 | `MainActivity.java:155-163` |
| hasVideoContent | 호출은 하지만 **mute 만** 하고 재생 대상(MediaView)이 없다 | `MainActivity.java:194-196`, `triggerAdPlayback` |
| VideoOptions | 미지정(기본값 음소거 자동재생). 메일 지적대로 1순위 원인은 아님 | `MainActivity.java:175` |
| 다중 로드 | `adLoader.loadAds(request, 2)` 사용. **구글 문서: 미디에이션이 설정된 광고 단위에서는 loadAds() 가 동작하지 않는다.** 2026-09-13/26 에 Liftoff·Meta·Pangle 미디에이션을 붙였으므로 지금 Android 네이티브는 미디에이션 수요를 아예 못 받고 있을 가능성이 있다 (iOS 는 단건 로드) | `MainActivity.java:198` |
| iOS 비교 | iOS 는 `GADNativeAdView.mediaView = adMediaView` 로 정상 바인딩 → 메일의 "iOS 는 나오고 Android 만 안 나온다"와 정확히 일치 | `ios/App/App/controllers/MainViewController.swift:202` |

**결론(네이티브)**: 메일의 진단표에서 "hasVideoContent = true 인데 화면에 안 나온다 → 렌더링/MediaView 문제"에 해당하며, 더 정확히는 *MediaView 자체가 없어서* 어떤 동영상 광고도 그려질 수 없다. 이 결함은 2026-08-12 분석 때 백로그로 기록돼 있었다(이번에 고친다).

**결론(전면)**: Android 전면 광고는 `@capacitor-community/admob` 6.x 의 `InterstitialAd.load() → show()` 그대로이고 iOS 와 같은 플러그인·같은 TS 호출 경로다. 앱 코드에 "동영상이냐 이미지냐"를 가르는 지점이 없다. 전면에서만 동영상이 안 나오면 원인은 AdMob 콘솔(Android 전면 광고 단위의 광고 유형에서 "동영상" 체크 여부, Android 미디에이션 그룹 매핑) 또는 수요 측이다. 참고로 Android 는 2026-09-17 실기기에서 Liftoff 광고 소스의 App ID 오기입을 고쳐 정상화된 이력이 있다(5ca03e6) — 클라이언트가 그 이전 빌드로 본 "예전 버그"일 수 있다.

### 2-B. 어드민 사전 검수 — 동영상은 **게이트를 완전히 우회**하고 있고, 회원 글 동영상 업로드는 실패 응답까지 낸다

| 경로 | 현재 동작 | 근거 |
|---|---|---|
| 스타 피드 동영상 (`addStarFeed`) | 원본·변환본·썸네일을 **곧바로 공개 디렉터리**(`/video`, `/video/thumnail`)에 쓴다. 이미지가 같이 없으면 `MDR_STATUS` 를 PENDING 으로 바꾸지 않아 즉시 공개 | `SuperAppService.java:1615-1652` |
| 회원 글 동영상 (`ContentService`) | 영상은 공개 `/video`, 썸네일은 공개 `/img` 에 바로 저장. PENDING 전환 없음 | `ContentService.java:1187-1199` |
| 회원 글 동영상 — 추가 결함 | 영상 분기가 `CON_THUMNAIL` 에 **공개 URL** 을 넣은 뒤, 이어지는 이미지 분기가 같은 키를 base64 로 다시 읽어 시그니처 검증 → `RESULT=FAIL, REASON=SIGNATURE` 를 돌려준다. 그 시점엔 DB 행과 파일은 이미 만들어진 뒤라 **앱은 "Upload failed" 를 띄우는데 글은 공개 상태로 올라가 있다**. 2-26차 검수 도입 때 생긴 회귀로 보인다. 실기기 재현 필요 | `ContentService.java:1197` → `1209-1227` |
| 어드민 대기열 SQL | 스타 피드는 `D.MEDIA_TYPE = 'PHOTO'` 로 **동영상 글을 목록에서 제외**. 첫 미디어(`SORT_ORDER=0`) 하나만 본다 | `super.xml:725-761` |
| 승인/차단 처리 | `selectModerationTarget` 이 미디어 1건의 URL 만 돌려주고, `promote/quarantine` 의 공개 목적지가 `/img` 로 고정 — 동영상(`/video`)·동영상 썸네일(`/video/thumnail`) 을 옮길 수단이 없다 | `SuperAdminService.java:386-395`, `ImageModerationUtil.java` 하단 |
| 어드민 미리보기 | `preview.do` 가 jpg/png/webp 만 content-type 지정, Range 미지원 → `<video>` 재생 불가 | `SuperAdminController.java:629-632` |
| 작성자 본인 노출 | `/api/super/media/pending` 도 이미지 전용(content-type, Range 없음). iOS WKWebView 는 Range 없는 동영상 응답을 재생하지 않는다 | `SuperAppController.java:85-110` |
| 앱 화면 | 피드 상세 `getVideoList()` 가 `MEDIA_URL` 없는 항목을 버려서, 검수 대기 동영상은 "Under review" 대신 **그냥 사라진다**. 스타 페이지는 `isUnderReview` 플레이스홀더가 동영상도 덮지만 본인용 토큰이 mp4 를 가리키면 `image/jpeg` 로 내려와 깨진다 | `feed-detail.page.ts:146-147`, `star-page.page.ts:517-532` |
| SQL 게이트 | 스타 갤러리는 `IF(APPROVED, MEDIA_URL, NULL)` 로 동영상 URL 도 가린다(OK). 회원 글 목록은 `CON_THUMNAIL` 만 가리고 `CON_ORIGIN_URL`(영상 주소) 은 그대로 내려간다 | `superapp.xml:417-421`, `content.xml:16-20` |

스키마는 손댈 것이 없다. 상태 컬럼은 게시물 단위고, 미디어 테이블에 `MEDIA_TYPE` 이 이미 있다.

### 2-C. 웹 AdSense 자리 — 현재 배치 vs 요청 규격

광고 코드(`<ins class="adsbygoogle">`)는 여전히 심사용 임시 제거 상태이고, 지금 있는 것은 크기만 잡은 점선 플레이스홀더 10곳이다. 승인 전이라 이번 작업도 **플레이스홀더 크기·위치·브레이크포인트** 조정이다.

현재 (home.jsp / content_landing.jsp):

| 페이지 | 슬롯 | PC | 모바일 | 규격 메일 대비 |
|---|---|---|---|---|
| 홈 | hero-leaderboard | 728×90 (히어로 우측) | 숨김 | 리더보드 ✓ (위치는 GNB 직하단이 아니라 히어로 안) |
| 홈 | sidebar-halfpage | 300×600 sticky* | 숨김 | 하프 페이지 ✓ |
| 홈 | list-leaderboard | 728×90 (페이지네이션 아래) | 숨김 | 규격 메일엔 "본문 끝 = 336×280" |
| 홈 | mobile-top | — | 320×100 | 대형 모바일 배너 ✓ |
| 홈 | mobile-infeed | — | 300×250 (6장마다) | 중형 사각형 ✓ |
| 홈 | mobile-bottom | — | 320×100 (탭바 위) | 규격엔 320×50 앵커 또는 336×280 |
| 스타 | star-a | 728×90 (통계 아래) | 320×100 | ✓ |
| 스타 | star-b | 300×600 (sticky 아님) | 숨김 | ✓ (sticky 여부만 홈·포스트와 다름) |
| 스타 | star-infeed | 300×250 (6장마다) | 숨김 | PC 규격 목록에 300×250 없음 → 336×280 후보 |
| 스타 | star-mobile-c | — | 300×250 | ✓ |
| 포스트 | post-d | 728×90 (본문 끝) | 300×250 | PC 규격: 본문 끝 336×280 / 모바일 규격: 336×280 |
| 포스트 | post-e | 300×600 sticky* | 숨김 | ✓ |
| 포스트 | post-mobile-b | — | 320×100 | ✓ |
| 포스트 | post-mobile-c | — | 300×250 | ✓ |

\* sticky 는 **2026-10-06 메일에 따라 작업 트리에 수정만 돼 있고 아직 커밋되지 않은 상태**(`home.jsp`, `content_landing.jsp` 변경분). 2-31 후속 브랜치 위에 있다.

브레이크포인트: 규격은 PC ≥1024 / 모바일 ≤768 인데 현재는 홈 그리드·히어로 광고 1100, 홈 모바일 섹션 820, 스타·포스트 광고 전환 1000 으로 제각각이다.

---

## 3. 구현 방안

### 3-A. Android 네이티브 광고 MediaView 전환 (core-frontend/android + platformConfigurement/android 동시 수정)

1. `ad_layout.xml`: 루트를 `NativeAdView` 로 바꿔 카드 전체를 감싸고, `ad_media` 를 `MediaView` 로 교체. CTA 를 감싸던 안쪽 `NativeAdView` 제거. 컨테이너 315dp 안에서 미디어가 남는 높이를 먹는 제약은 유지.
2. `MainActivity.populateNativeAdView`: `setMediaView` → `mediaView.setMediaContent(nativeAd.getMediaContent())` → headline/body/icon/CTA 등록 → 마지막에 `setNativeAd`. `NativeAdOptions` 에 `VideoOptions.startMuted(true)` 지정하고 수동 `mute(true)` 제거.
3. `loadAds(…, 2)` → `loadAd(...)`. 미디에이션 광고 단위에서 다중 로드는 공식적으로 미지원이고, 현재 두 번째 응답이 첫 번째를 덮어쓰는 구조라 이득도 없다.
4. 메일이 요구한 로그 5종을 `Log.d("AdMobVideo", …)` 로 추가: hardwareAccelerated(`getWindow().getAttributes().flags`), 로드 성공, mediaContent null 여부, `hasVideoContent()/getAspectRatio()/getDuration()`, `mediaView.post()` 에서 실측 width×height. 운영 빌드에도 남겨도 무해한 수준.
5. 검증: 에뮬레이터에서 구글 **네이티브 동영상 테스트 단위**(`ca-app-pub-3940256099942544/1044960115`)로 동영상 재생 확인 → 운영 단위로 되돌린 뒤 실기기 Ad Inspector. 컨테이너 크기·클리핑은 로비/스타 페이지 둘 다 본다(로비는 `ad_loaded` 이벤트 경로).
6. `android/` 는 git 미추적이므로 **같은 수정을 `platformConfigurement/android` 사본에도 반영**해 커밋한다 (두 파일 현재 동일함을 diff 로 확인).

전면 광고는 코드 수정 없음. 콘솔 확인 항목을 클라이언트에 전달한다(§6).

### 3-B. 동영상 사전 검수 (core-backend → core-frontend)

**백엔드** (컨트롤러 → 서비스 → 매퍼 순)

1. `ImageModerationUtil`: `promote/quarantine` 에 **공개 목적지 디렉터리 인자**를 받는 오버로드 추가(영상 `/video`, 영상 썸네일 `/video/thumnail`). 대기·격리 보관소는 기존 `moderation/pending`·`hidden` 을 그대로 쓴다(파일명이 UUID 라 충돌 없음).
2. `SuperAppService.addStarFeed`: 동영상 원본 저장·ffmpeg 변환·썸네일 추출을 **대기 디렉터리에서** 수행. `MEDIA_URL`/`THUMB_URL` 은 승인 후 최종 주소를 미리 기록(이미지와 같은 방식). 동영상만 있는 글도 PENDING + 로그.
3. `ContentService`(회원 글): 영상·썸네일을 대기 디렉터리로, PENDING 전환. 영상 분기가 `CON_THUMNAIL` 을 URL 로 덮어 이미지 분기가 다시 읽는 회귀를 분기 분리로 수정(이 수정만으로 "Upload failed" 오표시가 사라진다).
4. `super.xml`: `selectModerationQueue` 에서 `MEDIA_TYPE='PHOTO'` 조건 제거, `MEDIA_TYPE`·`THUMB_URL`·`CON_ORIGIN_URL` 을 행에 추가. `selectModerationTarget` 을 **게시물의 미디어 전부**를 돌려주는 목록 조회로 변경.
5. `SuperAdminService.applyModeration`: 미디어 목록을 돌며 타입별 목적지로 이동. 하나라도 실패하면 기존처럼 상태 롤백.
6. `SuperAdminController.preview.do`: 확장자별 content-type(mp4 → `video/mp4`), **HTTP Range(206)** 지원, 탐색 대상에 `/video` 계열 디렉터리 추가. 어드민 화면에서 `<video controls>` 로 봐야 하므로 필수.
7. `content.xml` 회원 글 목록: `CON_ORIGIN_URL` 도 `IF(APPROVED, …, NULL)` 로 가린다.
8. 작성자 본인 노출은 아래 결정 사항(§5-③)에 따라 둘 중 하나:
   - **A안(권장)**: 대기 중엔 본인도 **썸네일 + "Under review" 표시**만. `/api/super/media/pending` 이 동영상 글에는 썸네일 파일을 돌려주도록 `selectPendingMediaTarget` 을 타입 인지형으로 변경. 코드가 적고 140MB 영상을 Spring 으로 스트리밍하지 않는다.
   - B안: 본인에게 동영상 재생까지 제공. 같은 엔드포인트에 Range 지원 + `video/mp4` 를 추가하고 토큰 TTL(10분) 안에 재생이 끝나지 않을 때의 재발급 처리가 필요.

**프런트엔드**

9. `feed-detail`: `getVideoList()` 가 PENDING 동영상을 버리지 않게 하고 "Under review" 블록을 동영상 카드에도 적용. 본인이면 썸네일 포스터(A안) 표시.
10. `star-page`: `isUnderReview`/`pendingImageUrl` 계산에서 `MEDIA_TYPE='VIDEO'` 를 분기해 토큰이 썸네일을 가리키도록 정리.
11. `content-list`(회원 글): PENDING 이면 `CON_ORIGIN_URL` 재생 경로를 막고 기존 `app-moderated-image` 로 통일.
12. `moderation_list.jsp`: `MEDIA_TYPE='VIDEO'` 행은 `<video controls preload="metadata">` + 썸네일 포스터로 렌더. 제목을 "이미지 검수" → "미디어 검수".

**테스트**: `VideoTranscodeUtil`·`ImageModerationUtil` 단위 테스트에 목적지 분기 케이스 추가, `ContentService` 영상+이미지 분기 분리 테스트. 프런트는 `tsc --noEmit` + 관련 spec.

### 3-C. AdSense 자리 규격 고정 (core-backend JSP 2개)

사용자 지시대로 요청 규격에 맞춰 **고정 픽셀**로 통일한다. 변경안:

| 자리 | 현재 | 변경 | 비고 |
|---|---|---|---|
| 홈 PC 히어로 | 728×90 | 728×90 유지 | 리더보드 규격 그대로 |
| 홈 PC 목록 끝 | 728×90 | **336×280** | "본문 하단 = 대형 사각형" 규격 |
| 홈 PC 사이드바 | 300×600 sticky | 유지 | 10-06 메일분 커밋 포함 |
| 홈 모바일 하단 | 320×100 | 320×100 유지 | 앵커(320×50 sticky footer)는 모바일 하단 탭바와 겹침 → 미적용 권고(§6) |
| 스타 PC 피드 중간 | 300×250 | **336×280** | PC 규격에 300×250 없음 |
| 스타 PC 사이드바 | 300×600 | 유지 (+ 홈·포스트와 같이 sticky 로 통일할지 §6) | |
| 포스트 PC 본문 끝 | 728×90 | **336×280** | 규격 "본문 마무리" |
| 포스트 모바일 본문 끝 | 300×250 | **336×280 (≥375px) / 300×250 (<375px)** | 360px 폭 Android 에서 336 은 잘린다 |
| 모바일 피드 중간 | 300×250 | 유지 | In-feed(fluid) 는 콘솔에서 별도 광고 단위를 만들어야 해 승인 후 후속 |
| 970×250 빌보드 | 없음 | **미적용** | 본문 열 폭이 1400 컨테이너 기준 1008px 이라 뷰포트 ≈1390px 미만에서 잘림. 필요하면 ≥1400 전용으로만 |
| 160×600 | 없음 | 미적용 | 사이드바가 300/320px 이라 하프 페이지가 맞음 |

- 브레이크포인트: 광고 전환을 **PC ≥1024 / 모바일 ≤768** 로 통일. 769–1023 은 한 열이므로 모바일 세트를 쓴다. 홈 사이드바가 나타나는 1100 도 1024 로 내려 3열 그리드가 깨지지 않는지 브라우저에서 확인한다.
- CLS: 모든 자리에 `min-height` 를 브레이크포인트별로 선언(현재 `height` 고정이라 이미 밀림은 없지만, 승인 후 `ins` 를 넣을 때 규칙이 그대로 쓰이도록 정리).
- 승인 후 `ins` 삽입 시 AdSense 공식 "고급 반응형(미디어 쿼리로 크기 지정)" 방식을 쓰면 위 고정 픽셀이 그대로 유효하다.

---

## 4. 충돌·불가 항목과 대안

| 항목 | 문제 | 대안 |
|---|---|---|
| 모바일 앵커 320×50 (sticky footer) | 모바일 웹은 이미 하단 탭바가 고정돼 있어 앵커가 탭바를 덮거나 탭바 위에 띄워야 함. AdSense 앵커는 자동 광고 기능이라 수동 배치 단위도 아님 | 하단 320×100 유지, 앵커는 승인 후 자동 광고 설정에서 판단 |
| 970×250 빌보드 | 본문 열 폭 부족(위 표) | 미적용 또는 ≥1400px 전용 |
| Fluid in-feed | 콘솔에서 In-feed 광고 단위(레이아웃 키) 생성 필요, 승인 전엔 불가 | 300×250 유지, 후속 |
| Android 전면 동영상 | 앱 코드에 결정 지점이 없음 | 콘솔 점검 항목 전달 + 실기기 Ad Inspector |
| 작성자 본인 동영상 재생(B안) | Spring 으로 대용량 Range 스트리밍 + 토큰 TTL 문제 | A안(썸네일 + Under review) 권장 |
| 기존 공개 동영상 | 기본값 APPROVED 라 소급 검수 없음 (2-26 과 동일 정책) | 그대로 |

---

## 5. 승인 필요 항목

1. **android/ 네이티브 코드 수정 + platformConfigurement 동기화** (의존성 추가 없음, `play-services-ads` 25.4.0 그대로). 검증 중 테스트 광고 단위로 임시 교체 후 원복.
2. **작성자 본인 노출 방식**: A안(썸네일만) / B안(재생까지).
3. **작업 트리의 10-06 sticky 변경분**(home.jsp·content_landing.jsp, 미커밋) 을 이번 2-32 에 묶어 커밋할지, 2-31 후속에 넣을지.
4. 스타 페이지 사이드바 300×600 도 홈·포스트처럼 sticky 로 통일할지 (10-06 메일은 홈·포스트만 언급).
5. 회원 글 동영상 "Upload failed" 회귀 수정을 이번 범위에 포함 (범위 밖이지만 같은 코드 블록을 고치게 됨).
6. DB 마이그레이션·설정 파일 수정·새 서버 디렉터리: **없음**. 다만 `moderation/pending` 에 동영상(최대 140MB)이 쌓이므로 디스크 여유 확인 요청.

---

## 6. 미결 질문 (클라이언트 회신 필요)

1. Android 동영상 미노출을 **어느 빌드(버전코드)·어느 화면**에서 봤는가? 네이티브(스타 페이지/로비)와 전면 각각 "이미지 광고는 나오는가"? 2026-09-17 Liftoff 설정 수정 이전 빌드라면 전면은 이미 해결됐을 수 있다.
2. AdMob 콘솔 확인 요청: Android **전면 광고 단위 광고 유형에 "동영상"** 이 켜져 있는지, Android 네이티브 단위에 **동영상 미디어 허용**이 켜져 있는지, Android 미디에이션 그룹의 Liftoff/Meta/Pangle 매핑 상태. 스크린샷 요청.
3. 970×250 빌보드와 320×50 앵커를 위 사유로 빼도 되는지.
4. 태블릿 구간(769–1023px)은 모바일 세트로 처리해도 되는지.
5. 검수 대기 중 작성자 본인이 자기 동영상을 **재생**해야 하는지, 썸네일 + "Under review" 로 충분한지.
6. 동영상 검수 도입 후 대기열이 늘어난다 — 검수 담당·대응 시간은 여전히 미정(2-26 미결 그대로).

---

## 참고

- 구글 문서: 미디에이션 광고 단위에서 `loadAds()` 다중 로드 미지원 — https://developers.google.com/admob/android/native
- 2-26차 검수 설계 메모: `DOCUMENTS/moderation-counter-proposal-2026-08-24.md`, 네이티브 광고 분석(2026-08-12) 백로그에 "Android 템플릿이 MediaView 대신 ImageView" 로 기록돼 있었음

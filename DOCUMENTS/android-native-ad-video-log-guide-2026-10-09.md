# Android 네이티브 광고 동영상 로그 수집 안내 (2026-10-09)

클라이언트 보고: 네이티브 광고 동영상이 광고 프레임을 채우지 않고 작게 보인다(피드 상세 화면 스크린샷).
원인을 추측으로 고치지 않기 위해 앱이 찍는 진단 로그를 받는다. PR #31 빌드부터 들어 있다.

## 수집 방법

1. 폰을 USB 로 연결하고 개발자 옵션의 USB 디버깅을 켠다.
2. 아래 명령을 띄워 둔 채 앱을 열고 **로비 → 아무 스타 페이지 → 글 하나 눌러 피드 상세** 순으로 이동한다. 각 화면에서 광고가 보일 때까지 2~3초 기다린다.

```bash
adb logcat -c && adb logcat -s AdMobVideo
```

3. 출력 전체를 텍스트로 저장해 보낸다. 스크린샷을 같이 찍어 주면 어느 줄이 어느 화면인지 맞추기 쉽다.

## 로그 한 묶음의 뜻

```
hardwareAccelerated=true                               ← 전제 조건. false 면 여기서 끝
loaded container=… headline=… mediaContent=true        ← 광고가 왔다
hasVideoContent=true aspectRatio=0.5625 duration=15000 ← 동영상 여부·비율(0.56=세로, 1.78=가로)·길이
network=com.google.ads.mediation.vungle.… responseId=… ← 어느 네트워크가 채웠는가 (null 이면 AdMob 직접)
mediaView size=1040x520 shown=true                     ← 미디어 상자 실측(px)
tree container=… 1056x840 card=1056x840 mediaView=1040x520
tree  TextureView 292x520 at(374,0) vis=0              ← 영상 표면이 실제로 차지한 사각형
```

## 읽는 법

| 관찰 | 뜻 | 조치 |
|---|---|---|
| `aspectRatio` 가 세로(≈0.56)인데 `TextureView` 폭이 좁다 | 세로 영상을 가로 상자에 맞춰 넣은 것. 정상 동작 | 로비는 가로만 요청(PR #31), 스타는 카드 확대(PR #31) |
| `mediaView size` 는 정상인데 `TextureView` 높이만 작다 | SDK/네트워크 렌더러가 표면 크기를 잘못 잡음 | `network=` 값으로 특정 네트워크 여부 확인 → 해당 어댑터 버전·설정 점검 |
| `mediaView size` 자체가 작다(예: 높이 100 대) | 우리 레이아웃 제약 문제 | 카드 구조 수정 |
| `hasVideoContent=false` | 이번 응답은 이미지 광고 | 동영상 여부는 수급 문제, 코드 아님 |
| `native ad failed code=3` | no fill | 광고 단위·미디에이션 설정 |

## 2026-10-09 실기기(Galaxy S23) 확인 결과

- 하드웨어 가속 켜져 있음(윈도우 플래그·GPU 렌더 통계로 확인). 초기 로그의 `false` 는 onCreate 시점이 일러 잘못 읽은 것 → 뷰가 붙은 뒤 재도록 수정.
- 광고 47건·동영상 10건 모두 영상 표면이 미디어 상자와 같은 크기. 띠 모양은 재현되지 않음(전부 AdMob 직접 수급, 미디에이션 영상은 0건).
- "이미지가 상자 가운데 작게 보임" 은 광고 자산(3600×1881) 안에 흰 여백이 들어 있는 것. 스케일은 FIT_CENTER 로 폭을 꽉 채우고 있음 → 자산 문제, 코드로 할 것 없음.
- 광고 위에서 드래그하면 스크롤이 안 되던 회귀(2-32차 변경으로 카드 전체가 클릭 자산이 되면서 발생) → `AdTouchRouterLayout` 로 탭/드래그 분리해 수정, 실기기 확인.

## 전면 광고

전면은 구글이 전체 화면을 직접 그리므로 크기 문제가 없다. 동영상이 나오는지만 보려면
`core-frontend/src/app/services/ad-mob.service.ts` 의 `USE_TEST_INTERSTITIAL` 을 `true` 로 두고
테스트 단위를 전면 동영상 테스트 단위 `ca-app-pub-3940256099942544/8691691433` 으로 바꿔 빌드한다(확인 후 원복).

# 앱 스토어 릴리스 노트 — 2026-10-04 (브랜치 feature/2-31-web-video-followup, PR #28)

앱(iOS·Android)에 들어간 변경만 담았다. 웹(영상 변환·전체화면 뷰어·프로필 Load more)은 스토어 노트 대상이 아니다.

앱 변경 요약
1. Android: 방문자·메시지 알림의 소리와 진동이 울리지 않던 문제 수정 (알림 채널 재생성)
2. iOS: 앱 아이콘 배지 숫자가 1에서 멈추던 문제 수정 — 알림이 올 때마다 1, 2, 3으로 올라간다 (서버 동시 배포 필요)
3. 앱을 켠 뒤 첫 터치·스크롤에서 의도치 않은 효과음이 나던 문제 수정 (iOS). 로비(첫 화면)는 효과음 없이 진동만
4. 진동(햅틱) 세기 상향 — Android Light·iOS Medium → 둘 다 Heavy
5. iOS: 앱 실행 직후 흰 화면이 한 번 비치던 깜빡임 수정
6. 스타 페이지 하단 추천 목록을 병렬로 받아 진입이 조금 빨라짐

주의
- 크래시 수정은 이번 빌드에 없다. 아래 노트에도 그렇게 쓰지 않았다.
- 2번(배지)은 서버(witch.war)도 같이 올라가야 동작한다. 앱만 먼저 나가면 배지는 예전처럼 1로 온다. 서버만 먼저 나가면 구 앱에서 숫자가 계속 쌓이고 Android 구 앱은 알림 채널이 없어 무음이 된다 → **앱 심사 통과 시점에 서버를 배포**한다.
- 1번(Android 알림 소리)은 앱 업데이트 후 첫 실행에서 새 알림 채널이 만들어져야 적용된다. 설정 화면의 알림 카테고리 이름이 바뀐다(Visitor Alerts / Messages / Announcements).

---

## 1. 스토어 "새로운 기능" (Apple What's New / Google Play 출시 노트, 500자 이내)

### 한국어
```
• 알림 수정: 방문자·메시지 알림의 소리와 진동이 울리지 않던 문제를 고쳤습니다. 앱 아이콘의 알림 숫자도 이제 알림이 올 때마다 올라갑니다.
• 앱을 켠 직후 화면을 터치하거나 스크롤할 때 나던 효과음을 없앴습니다.
• 진동 피드백을 더 또렷하게 바꿨습니다.
• 앱 실행 시 화면이 깜빡이던 현상을 수정했습니다.
• 스타 페이지 진입 속도를 개선했습니다.
```

### English
```
• Notification fixes: visitor and message alerts now play their sound and vibrate as expected, and the app icon badge counts up with each new alert instead of staying at 1.
• Removed the unintended sound that played on the first tap or scroll after opening the app.
• Stronger, clearer vibration feedback.
• Fixed a brief flicker when launching the app.
• Star pages open a little faster.
```

### Deutsch
```
• Benachrichtigungen repariert: Besucher- und Nachrichtenhinweise spielen jetzt wie vorgesehen Ton und Vibration ab, und die Zahl auf dem App-Symbol zählt mit jeder neuen Benachrichtigung hoch, statt bei 1 stehen zu bleiben.
• Der unbeabsichtigte Ton beim ersten Tippen oder Scrollen nach dem Start wurde entfernt.
• Deutlichere Vibrationsrückmeldung.
• Kurzes Flackern beim Start der App behoben.
• Star-Seiten öffnen sich etwas schneller.
```

### 日本語
```
• 通知の修正：訪問者・メッセージ通知の音とバイブレーションが鳴らなかった問題を修正しました。アプリアイコンのバッジ数も、通知が届くたびに増えるようになりました。
• アプリ起動直後に画面をタップ・スクロールしたときに鳴っていた効果音をなくしました。
• バイブレーションをよりはっきり感じられるようにしました。
• 起動時に画面が一瞬ちらつく現象を修正しました。
• スターページの表示が少し速くなりました。
```

### Français
```
• Notifications corrigées : les alertes de visiteurs et de messages jouent désormais leur son et vibrent comme prévu, et le badge de l'icône augmente à chaque nouvelle alerte au lieu de rester à 1.
• Suppression du son intempestif joué au premier appui ou défilement après l'ouverture de l'app.
• Retour haptique plus net et plus marqué.
• Correction d'un bref scintillement au lancement de l'app.
• Les pages Star s'ouvrent un peu plus vite.
```

### Android 전용 추가 문구 (Google Play 노트 끝에 붙이기)
| 언어 | 문구 |
|---|---|
| 한국어 | • 알림 카테고리가 새로 만들어집니다. 알림이 계속 울리지 않으면 설정 > 알림에서 Star Platform의 알림이 켜져 있는지 확인해 주세요. |
| English | • Notification categories have been recreated. If alerts stay silent, check that Star Platform notifications are enabled in Settings > Notifications. |
| Deutsch | • Die Benachrichtigungskategorien wurden neu angelegt. Bleiben Hinweise stumm, prüfen Sie unter Einstellungen > Benachrichtigungen, ob Star Platform aktiviert ist. |
| 日本語 | • 通知カテゴリを作り直しました。通知が鳴らない場合は、設定 > 通知でStar Platformの通知がオンになっているか確認してください。 |
| Français | • Les catégories de notification ont été recréées. Si les alertes restent silencieuses, vérifiez que Star Platform est activé dans Réglages > Notifications. |

---

## 2. 심사자용 메모 (App Store Connect "App Review Information → Notes")

`[ ]` 부분은 제출 전에 채운다.

### English (Apple 심사는 영어로 쓰는 것이 처리에 유리)
```
This is a bug-fix update for notification feedback. No new permissions, no new SDKs, and no changes to sign-in, purchase, or ad flows. The privacy manifest and the App Tracking Transparency prompt are unchanged.

What changed
1. Notification badge: the app icon badge was stuck at "1" no matter how many alerts arrived. The server now sends an incrementing count and the app resets it when opened.
2. Unintended audio: a short click sound played on the user's first tap or scroll after launch. Cause: a one-time "silent" audio warm-up used volume=0, which iOS ignores. It no longer runs on iOS/Android.
3. Haptics: in-app vibration feedback was too weak to notice on some devices. Impact strength raised to Heavy. The home screen (lobby) now gives vibration only, no sound.
4. Launch: removed a one-frame white flash between the launch screen and the first screen by matching the launch and web view background colors.
5. Star page: the "recommended pages" list is requested in parallel with the page itself (minor speed-up).
(Android-only in the same release: notification channels were recreated so visitor/message alerts play sound and vibrate.)

How to test
- Browsing requires no account. Lobby and star pages are public.
- Haptics and the first-tap fix: open the app, tap or scroll on the first screen — no sound; tapping a star card vibrates.
- Badge: sign in with the review account [Apple ID / Google account], which owns a StarPage. Visit https://witch-hunting.com/star/[id] from a desktop browser to generate a visitor alert; each alert increments the badge; opening the app clears it. (Requires the matching server release, deployed on [date].)
- Notification sound/vibration on iOS follows the device's Sound & Haptics settings; the app's permission request includes alert, sound and badge.

Contact for questions: [email]
```

### 한국어 (내부 확인용 번역)
```
알림 피드백 버그 수정 업데이트입니다. 새 권한·새 SDK 없음, 로그인·결제·광고 흐름 변경 없음. 개인정보 매니페스트와 ATT 안내는 그대로입니다.

변경 내용
1. 배지: 알림이 몇 건 와도 아이콘 숫자가 "1"에 멈춰 있었습니다. 서버가 올라가는 숫자를 보내고 앱을 열면 리셋합니다.
2. 의도치 않은 소리: 실행 후 첫 터치·스크롤에 짧은 클릭음이 났습니다. 원인은 volume=0 으로 돌리던 1회성 오디오 워밍업을 iOS 가 무시한 것. iOS·Android 에서는 더 이상 돌지 않습니다.
3. 햅틱: 앱 안 진동이 일부 기기에서 느껴지지 않을 만큼 약했습니다. Heavy 로 상향. 첫 화면(로비)은 소리 없이 진동만.
4. 실행: 런치 화면과 첫 화면 사이에 흰 프레임이 한 번 비치던 것을 배경색을 맞춰 제거.
5. 스타 페이지: 추천 목록을 페이지와 병렬로 요청(소폭 속도 개선).
(Android 전용: 알림 채널을 다시 만들어 방문자·메시지 알림에 소리·진동이 납니다.)

테스트 방법
- 열람은 계정 불필요. 로비·스타 페이지는 공개입니다.
- 햅틱·첫 터치: 앱을 열고 첫 화면을 터치·스크롤 — 소리 없음. 스타 카드를 누르면 진동.
- 배지: 스타페이지가 있는 심사용 계정 [계정]으로 로그인. PC 브라우저에서 https://witch-hunting.com/star/[id] 를 방문하면 방문자 알림이 오고, 알림마다 배지가 올라가며 앱을 열면 지워집니다. (서버 릴리스 동시 배포 전제, 배포일 [날짜].)
- iOS 알림 소리·진동은 기기의 사운드 및 햅틱 설정을 따릅니다. 앱의 권한 요청은 알림·소리·배지를 포함합니다.
```

---

## 3. 빠른 심사(Expedited Review) 요청문

Apple 의 빠른 심사는 "라이브 앱의 치명적 버그 수정" 또는 "시간이 정해진 이벤트"에 허용된다. 이번 빌드는 크래시 수정이 아니므로 **크래시라고 쓰지 않는다.** 대신 사실인 것만 적는다: 알림 기능(소리·진동·배지)이 사용자에게 제대로 전달되지 않는 버그이고, 실행 직후 의도치 않은 소리가 나는 버그다. 승인 여부는 Apple 이 판단하며, 거절돼도 일반 심사로 넘어간다. Google Play 에는 빠른 심사 제도가 없다.

요청 폼: App Store Connect → Contact Us → App Review → Request Expedited Review.

### English (요청 폼에 붙여넣기)
```
App: Star Platform (bundle id kr.co.sensiblenews.witchHunting)
Version: [10.0.xx] (build [1000xx])

Reason: critical bug fix affecting notifications for all users of the live app.

Problem in the current App Store version
1. The app icon badge is stuck at "1" regardless of how many visitor or message alerts arrive, so users cannot tell new alerts from old ones.
2. In-app vibration feedback is too weak to be noticed on many devices, so users miss the visitor-count updates that are the core of the app.
3. An unintended click sound plays on the user's first tap or scroll after launching the app. Users are reporting this as the app "making noise on its own".

What this build fixes
- Badge count now increments per alert and resets when the app is opened.
- Haptic feedback strength raised; the home screen gives vibration only, without sound.
- The unintended first-tap sound is removed.
- A one-frame white flash at launch is removed.

No new permissions, SDKs, or changes to sign-in, purchases, or ads. Full details and test steps are in the App Review notes for this version.

We would be grateful for an expedited review so affected users receive the fix as soon as possible. Thank you.
```

### 한국어 (내부 확인용 번역)
```
앱: Star Platform (kr.co.sensiblenews.witchHunting)
버전: [10.0.xx] (빌드 [1000xx])

사유: 라이브 앱의 모든 사용자에게 영향을 주는 알림 기능 버그 수정.

현재 스토어 버전의 문제
1. 방문자·메시지 알림이 몇 건 와도 아이콘 배지가 "1"에 멈춰 새 알림과 지난 알림을 구분할 수 없음.
2. 앱 안 진동 피드백이 여러 기기에서 느껴지지 않을 만큼 약해, 앱의 핵심인 방문자 수 변화를 놓침.
3. 앱 실행 후 첫 터치·스크롤에 의도치 않은 클릭음이 남. 사용자들이 "앱이 혼자 소리를 낸다"고 보고.

이 빌드의 수정
- 배지가 알림마다 올라가고 앱을 열면 리셋.
- 햅틱 세기 상향, 첫 화면은 소리 없이 진동만.
- 첫 터치 소리 제거.
- 실행 시 흰 프레임 깜빡임 제거.

새 권한·SDK 없음, 로그인·결제·광고 변경 없음. 상세와 테스트 절차는 이 버전의 심사 메모에 있습니다.
```

---

## 4. 제출 체크리스트

1. `core-frontend/trapeze-android.yml` 의 versionName/versionCode 와 `trapeze-ios.yml` 의 version/buildNumber 를 올리고 커밋 (현재 10.0.72 / 100072).
2. `ionic capacitor build ios` / `ionic capacitor build android` 로만 빌드한다 (clean-build 스크립트 금지).
3. iOS: `ios/` 는 gitignore 라 `LaunchScreen.storyboard` 가 로컬 변경 그대로인지 확인. 다른 PC 에서 빌드하면 `npm run copy-assets-ios` 로 `platformConfigurement/ios` 사본을 먼저 복사.
4. 서버(witch.war)는 **앱 심사 통과 시점**에 배포. 배포 전 `DOCUMENTS/runbook-video-reencode-2026-10-04.md` 의 영상 재인코딩도 함께.
5. 제출 후 실기기 확인: Android 알림 소리·진동(백그라운드 상태에서), iOS 배지 1→2, 첫 터치 무음, 실행 깜빡임.

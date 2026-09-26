# 앱 스토어 릴리스 노트 — 2026-09-27 (브랜치 feature/2-29-followup-vs-rank-web-pangle)

앱(iOS·Android)에 들어간 변경만 담았다. 웹사이트 개편은 스토어 노트 대상이 아니다.

앱 변경 요약
1. 로비 VS 카드의 이름 아래 지표를 조회수에서 글로벌 순위(🌍 #N)로 교체
2. 비로그인 사용자용 Global Ranking 배너 신설 — Create Your StarPage / Re-Login(Apple·Google 하단 시트)
3. 광고 미디에이션 파트너 추가(Pangle). 앱 코드 동작 변화 없음
4. Android: 최소 지원 버전 7.0(API 24)으로 상향

주의: 크래시 수정은 이번 빌드에 없다. 아래 노트에도 그렇게 쓰지 않았다.

---

## 1. 스토어 "새로운 기능" (Apple What's New / Google Play 출시 노트, 500자 이내)

### 한국어
```
• 로비 VS 카드에 각 스타의 글로벌 순위를 표시합니다.
• 로그인 전 화면에 Global Ranking 배너가 추가되었습니다. 바로 스타페이지를 만들거나 Apple·Google 계정으로 다시 로그인할 수 있습니다.
• 광고 시스템을 업데이트했습니다.
```

### English
```
• VS cards in the lobby now show each star's Global Rank.
• New Global Ranking banner for signed-out users: create your StarPage or re-login with Apple or Google in one tap.
• Updated the advertising system.
```

### Deutsch
```
• VS-Karten in der Lobby zeigen jetzt den globalen Rang jedes Stars.
• Neues Global-Ranking-Banner für nicht angemeldete Nutzer: StarPage erstellen oder mit Apple bzw. Google erneut anmelden – mit einem Tipp.
• Werbesystem aktualisiert.
```

### 日本語
```
• ロビーのVSカードに各スターのグローバルランキングを表示するようになりました。
• 未ログイン時にGlobal Rankingバナーを追加しました。ワンタップでStarPageの作成、またはApple・Googleアカウントでの再ログインができます。
• 広告システムを更新しました。
```

### Français
```
• Les cartes VS du lobby affichent désormais le classement mondial de chaque star.
• Nouvelle bannière Global Ranking pour les utilisateurs non connectés : créez votre StarPage ou reconnectez-vous avec Apple ou Google en un geste.
• Système publicitaire mis à jour.
```

### Android 전용 추가 문구 (Google Play 노트 끝에 붙이기)
| 언어 | 문구 |
|---|---|
| 한국어 | • 이 버전부터 Android 7.0 이상에서 사용할 수 있습니다. |
| English | • This version requires Android 7.0 or later. |
| Deutsch | • Diese Version erfordert Android 7.0 oder neuer. |
| 日本語 | • このバージョンはAndroid 7.0以降で利用できます。 |
| Français | • Cette version nécessite Android 7.0 ou une version ultérieure. |

---

## 2. 심사자용 메모 (App Store Connect "App Review Information → Notes")

`[ ]` 부분은 제출 전에 채운다.

### English (Apple 심사는 영어로 쓰는 것이 처리에 유리)
```
This update contains UI changes only. No new permissions and no changes to sign-in or purchase flows.

What changed
1. Lobby VS cards: the line under each star's name now shows the star's Global Rank instead of a view count.
2. Signed-out lobby: a new "Global Ranking" banner with two tap areas — "Create Your StarPage" (opens the existing page-creation flow) and "Re-Login" (opens the existing Sign in with Apple / Google sheet).
3. Added one more ad mediation partner (Pangle) to the existing Google AdMob mediation. Ads still respect App Tracking Transparency; the ATT prompt and privacy manifest are unchanged.

How to test
- Browsing the lobby requires no account.
- To test "Re-Login", use the review account: [Apple ID / Google account for review] — sign in with Apple is available on any device; the account must already have a StarPage.
- The Global Ranking banner is shown only while signed out; after signing in it is replaced by the "My Global Ranking" card.

Contact for questions: [email]
```

### 한국어 (내부 확인용 번역)
```
이번 업데이트는 UI 변경만 포함합니다. 새 권한 요청이 없고 로그인·결제 흐름 변경도 없습니다.

변경 내용
1. 로비 VS 카드: 스타 이름 아래 줄이 조회수 대신 글로벌 순위를 보여줍니다.
2. 비로그인 로비: "Global Ranking" 배너 신설. "Create Your StarPage"는 기존 페이지 생성 흐름, "Re-Login"은 기존 Apple/Google 로그인 시트를 엽니다.
3. 기존 Google AdMob 미디에이션에 광고 파트너(Pangle) 1개 추가. ATT 안내와 개인정보 매니페스트는 그대로입니다.

테스트 방법
- 로비 열람은 계정이 필요 없습니다.
- "Re-Login" 테스트는 심사용 계정 [계정]으로. 해당 계정에 스타페이지가 있어야 합니다.
- 배너는 로그아웃 상태에서만 보이고, 로그인하면 "My Global Ranking" 카드로 바뀝니다.
```

---

## 3. 빠른 심사(Expedited Review)에 대해

이번 빌드는 크래시 수정이 아니다. "UI 크래시 수정, 긴급"이라고 적으면 사실과 다른 진술이 된다.
- Apple의 빠른 심사는 "치명적 버그 수정" 또는 "시간이 정해진 이벤트"에만 허용되고, 사실이 아닌 요청이 확인되면 이후 요청이 거절될 수 있다고 명시돼 있다. 심사자는 빌드를 직접 실행하므로 크래시가 없다는 것이 바로 드러난다.
- Google Play에는 빠른 심사 제도 자체가 없다.
- 실제로 심사 시간을 줄이는 방법은 위 심사자 메모처럼 "권한 변경 없음, 로그인 흐름 변경 없음, 테스트 계정 제공"을 분명히 적는 것이다. 이런 UI-only 업데이트는 보통 24시간 안에 처리된다.

# 요청서 검토 — VS 카드 글로벌랭킹 · 비로그인 배너 Re-Login · 웹 화이트 모드 개편 · Pangle 미디에이션

검토일: 2026-09-26 · 대상 브랜치 기준: `feature/2-29-lobby-my-ranking` (HEAD 945f55c)
차수: 메일에 번호 없음. 항목 2는 "2-29 연장선"으로 표기됨. 임의 채번하지 않음.
첨부 이미지: 1 = 비로그인 배너(2000×700 webp, 145KB) · 2 = PC 홈 시안 · 3 = PC 스타 프로필 시안 · 4 = PC 콘텐츠 상세 시안 · 5 = 모바일 홈 시안

---

## 1. 요구사항 목록

### 항목 1 — VS 카드
- R1-1. VS 카드 좌·우 스타 이름 아래의 `👁️ 조회수` 줄을 `🌍 #15` (Today's TOP 행과 같은 지구 아이콘 + 글로벌 순위)로 교체한다.

### 항목 2 — 비로그인 My Global Ranking 카드 (2-29 연장)
- R2-1. 비로그인 카드는 첨부 이미지 1장(지구·인물·왕관·문구·버튼이 모두 그려진 것)을 그대로 표시한다. 이미지 재가공 없음.
- R2-2. 이미지 위 `Create Your StarPage` 자리에 투명 클릭 영역 → 기존 StarPage 생성 흐름.
- R2-3. 이미지 위 `Re-Login` 자리에 투명 클릭 영역 → 스타페이지 ✏️(연필) 버튼과 같은 방식으로 Apple·Google 로그인이 화면 하단에서 올라온다.
- R2-4. (조건부) 로그인 사용자 화면 상단 우측에 지구 그림. "복잡하면 취소" — 클라이언트가 스스로 보류 가능성을 열어 둠.

### 항목 3 — 웹 랜딩·웹사이트 디자인 개선
- R3-1. 모든 웹 화면을 다크 모드가 아닌 일반 화이트 모드로.
- R3-2. `Home`과 `Public Posts` 자리를 맞바꾼다. Home = 기존 Public Posts 화면, Posts = 기존 Home 화면. PC·모바일 동일.
- R3-3. 새 Home(포스트 목록)에서 상단 "Public posts" 제목·설명문·건수 표기를 없앤다. PC는 시안(이미지 2)의 홈 화면으로 교체.
- R3-4. 새 Home 하단의 FAQ를 뺀다.
- R3-5. 스타 프로필 페이지는 그대로 두되 하단 FAQ 글귀만 제거. (단, 시안 이미지 3 참고)
- R3-6. 콘텐츠 상세 페이지도 그대로 두되 하단 FAQ 글귀만 제거. (단, 시안 이미지 4 참고)
- R3-7. 상단 좌측 로고 → 바뀐 Home, 상단 우측 Posts → 이동 배치된 Posts.
- R3-8. 작업 완료 후 1회 수정 라운드 포함.

### 항목 4 — Pangle 미디에이션
- R4-1. AdMob Bidding 소스로 Pangle을 추가한다. 1차는 Pangle ROW로 Android/iOS × Native/Interstitial 4개 단위.
- R4-2. 미국 트래픽용 Pangle US 는 Interstitial만 별도 매핑(Native 미지원). Pangle KR은 Closed Beta 권한이 있을 때만.
- R4-3. 어댑터는 1개만 설치, 지역 분리는 AdMob 콘솔 매핑으로.
- R4-4. Meta: 첫 메일은 "정상 참여", 둘째 메일은 "격리 후 재활성화" — 서로 다름 (미결 질문 참조).
- R4-5. Pangle 계정은 "기존 이메일 주소·패스워드 동일"로 생성 (→ 계정 생성·자격증명 입력은 개발자가 대행하지 않음, 4절 참조).
- R4-6. 검증은 Ad Inspector 단독 소스 테스트 → 4사 통합 순.

---

## 2. 현 아키텍처 기준 구현 방안

### 항목 1 — VS 카드 (프런트 + 백엔드 소폭)
현재 `VsCardSide`(vs-carousel.component.ts:5)에 `globalRank`가 없다. 백엔드 `getVsCards`(SuperAppService.java:1124)는
- 랭킹형 카드: `superapp.selectVsTop2` → 카테고리 내 1·2위 행. 여기 오는 순위는 "카테고리 안 1·2위"이지 글로벌 순위가 아님.
- CUSTOM 카드: `superapp.selectVsStarWithScore` → id/name/image/score/viewCount 등만.

**백엔드**: `getVsCards`에서 좌·우 각 스타 id에 대해 이미 캐시된 글로벌 순위표(my-rank·Today's TOP이 쓰는 `GLOBAL_RANK` 맵, SuperAppService.java:2171 부근 로직)를 조회해 `globalRank`를 side 맵에 추가. SQL 변경 없음, DDL 없음. 순위표 밖 페이지는 null.
**프런트**: `VsCardSide.globalRank?: number` 추가, `stat-line`을 `🌍 #{{ globalRank }}`로 교체. null이면 줄을 비우거나 `—`. 아이콘은 클라이언트 지정대로 Today's TOP 행과 동일한 🌍 이모지(lobby.page.html:440). VS 폴링 3초 주기는 그대로.

### 항목 2 — 비로그인 배너 (프런트만)
- `my-ranking-card.component.html`의 `#guestTpl`(CSS로 그린 지구·왕관·별 장식 + 버튼)을 `<img>` 1장 + 투명 `<button>` 2개로 교체. 이미지는 `src/assets/img/global-ranking.webp`(첨부 1.webp 그대로, 2000×700).
- Create → 이미 연결된 `createPage` 출력 → `openAvailablePageModal()` (lobby.page.ts:1111). 변경 없음.
- Re-Login → 새 출력 `reLogin` → `CreatorLoginService.promptLogin()`. 이것이 스타페이지 ✏️ FAB(`handleWriteButtonClick`)이 쓰는 ActionSheet(하단 시트, "Continue with Apple / Continue with Google")라 요청의 "몽당 연필 클릭 때와 같이 하단에서 올라오는 방식"과 정확히 일치. 로그인 성공 시 로비의 `openCreatorLogin()`과 같은 후처리(isStar·starId 갱신, my-rank 폴링 시작). **`CallLoginViewService`(가운데 뜨는 구글/애플/카카오/이메일 모달)는 쓰지 않는다.**
- 클릭 영역 좌표는 메일의 % 값을 그대로 쓰지 않고 실제 이미지에서 측정해 넣는다(메일 값은 데이터). 투명 버튼에 `aria-label` 필수(이미지 안 글자는 스크린리더가 못 읽음).
- 카드 높이: 현재 비로그인 카드는 로그인 카드와 "같은 셸·같은 높이"(2-29 확정)이고 66% 축소 상태. 이미지는 20:7 비율이라 폭 기준 높이가 정해진다 → 로그인 카드와 높이가 달라짐. **2-29 결정 번복이므로 확인 필요**(5절 Q2).
- R2-4(로그인 사용자 상단 우측 지구 그림)는 대상 위치가 불명확 → 클라이언트 말대로 보류 권고(5절 Q3).

### 항목 3 — 웹 (백엔드 JSP + 컨트롤러)
관련 파일: `DeepLinkController.java:522`(`/` → `/common/landing`), `PublicWebController.java:84`(`/posts` → `/common/posts`), `common/landing.jsp`, `common/posts.jsp`, `common/content_landing.jsp`(스타 프로필·콘텐츠 상세 겸용, `landingType`으로 분기), `common/include/web-base-style.jsp`·`web-chrome-style.jsp`·`web-nav.jsp`·`web-footer.jsp`.

- **화이트 모드(R3-1)**: 다크 팔레트(#0f172a/#1e293b/#e2e8f0)가 `web-base-style.jsp`·`web-chrome-style.jsp`·`landing.jsp` 인라인 CSS에 하드코딩. 공통 include 2개 + landing.jsp + about/faq/contact/posts/web_404/policy_view의 인라인 색상을 흰 배경 팔레트로 교체. `content_landing.jsp`는 이미 흰 배경(#fafafa/#fff)이라 대상 아님. `prefers-color-scheme` 분기는 없으므로 "다크 모드 끄기"가 아니라 색상 상수 교체 작업.
- **Home/Posts 맞바꾸기(R3-2, R3-7)**: 뷰 매핑만 교차한다. `/` 핸들러가 포스트 목록 데이터+`posts.jsp`를, `/posts` 핸들러가 기존 landing 데이터+`landing.jsp`를 렌더. `activeNav`도 교차. 로고·Posts 링크는 이미 `/`·`/posts`라 추가 수정 없음. 따라오는 정리: canonical, sitemap(`DeepLinkController.java:578~588`), FAQPage JSON-LD(landing.jsp:32 — /posts로 같이 이동), 페이지네이션 URL(`/posts?page=N` → `/?page=N`), `posts.jsp`의 "home page로 가라"는 빈 상태 안내문.
- **새 Home 정리(R3-3)**: `posts.jsp`의 `page-title`·`page-lead`·`list-meta` 제거. PC 시안(이미지 2)의 상단 히어로(지구 배경 + "Your Page. Your World." + Explore 버튼)와 하단 3개 카드(Join/Create/Grow)는 신규 마크업. 히어로 배경 이미지는 시안에서 잘라 쓸 수 없으니 **원본 이미지 별도 수령 필요**.
- **FAQ 제거(R3-4~6)**: `content_landing.jsp:584~622` `faq-section` 제거(스타/포스트 두 분기 모두). 새 Home(`posts.jsp`)에는 FAQ 블록 자체가 없다 → "하단 FAQ"가 푸터의 FAQ 링크 열(`web-footer.jsp:22~42`)을 뜻하는지 확인 필요(5절 Q5).
- **모바일 시안(이미지 5)**: 하단 탭바(Home/About/Posts/FAQ/Contact), 기능 칩 4개, "Explore by Category"(STAR/CELEB/BRAND/UNIVERSITY/CITY — 실재하는 STAR_CATEGORY 코드와 일치), "203개국" 배너는 전부 신규 섹션. 카테고리 카드의 대표 이미지·스타 수 데이터는 별도 쿼리 필요(super.xml/superapp.xml에 카테고리별 집계 추가, DDL 없음).
- **광고 슬롯**: 시안의 AdSense 자리(728×90, 300×600)는 현재 "AdSense 승인 대기"로 코드가 임시 제거된 상태. 승인 전이면 자리만 비워 두고 슬롯 코드는 넣지 않는다.

### 항목 4 — Pangle (네이티브 설정 + 콘솔)
코드 변경은 전부 `platformConfigurement/` 아래(android/·ios/ 직접 수정 금지).
- Android: `build.gradle`에 `com.google.ads.mediation:pangle:8.3.0.3.0`(현재 `play-services-ads:25.4.0`과 짝; 8.3.0.4.0은 25.5.0 요구) + Maven 저장소 `https://artifact.bytedance.com/repository/pangle` 추가. proguard `-keep`은 기존 `com.google.ads.mediation.**`로 커버, Pangle SDK용 규칙은 AAR 동봉 여부 확인. 앱 광고 호출 코드는 변경 없음(Liftoff·Meta와 동일).
- iOS: Podfile에 `GoogleMobileAdsMediationPangle` — **GMA 11.3.0 핀 때문에 5.8.0.7.0이 상한**(3절 참조). SKAdNetwork: Pangle 공개 ID 중 `22mmun2rn5`는 Info.plist에 이미 있음, `238da6jt44`는 없음 → Info.plist(trapeze yml 아님)에 추가.
- 콘솔(개발자 코드 아님): Pangle 앱 2개·Placement 4개(+US용 Interstitial 2개) 생성, AdMob 미디에이션 그룹 4개에 Pangle ROW(/US) 소스 추가·매핑, app-ads.txt Pangle 판매자 라인(WAR + Cloudflare Worker 폴백 갱신), 개인정보 처리방침 광고 파트너 목록에 Pangle 추가.
- 검증 순서는 요청서대로(단독 소스 → 4사 통합). Ad Inspector는 실기기 필요.

---

## 3. 충돌·실현 불가 항목 + 대안

| # | 충돌 | 대안 |
|---|---|---|
| C1 | **Pangle Android는 minSdk 24 요구**(Google 공식 문서 "Android API level 24 or later"). 앱은 `minSdkVersion = 23`. AppLovin 때와 같은 하드 제약. | Android 6.0 단말을 버리는 제품 결정이 선행돼야 함. `tools:overrideLibrary` 우회는 런타임 크래시로 바뀌므로 불가. |
| C2 | **16KB 페이지 정렬**: Pangle SDK는 네이티브 `.so`를 동봉하는 것으로 알려져 있고, 이 앱은 AGP 8.2.1이라 zip 정렬(AGP 8.5.1+)이 안 된다. AppLovin은 이 이유로 Play 업로드 거부 → revert 이력. | AGP ≥ 8.5.1 상향을 선행 과제로 분리(R8과 따로 검증). 실제 `.so` 포함 여부는 어댑터를 붙인 빌드에서 `zipalign -c -P 16 -v 4`로 실측 확정. |
| C3 | **iOS 어댑터 상한**: `@capacitor-community/admob@6`이 GMA를 11.3.0에 핀 → Pangle iOS 어댑터는 5.8.0.7.0(Pangle SDK 5.8.0.7, 2024년 초)까지만 설치 가능. Pangle Bidding(US/KR/ROW 분리)이 이 구 SDK에서 입찰에 참여하는지 보장 없음. Liftoff·Meta와 같은 사정. | 우선 5.8.0.7.0으로 붙이고 Ad Inspector에서 Bid Response 여부 실측. 안 되면 admob v7 + Capacitor 7 메이저 업그레이드가 선행 과제. |
| C4 | 요청서의 "GMA **Next-Gen** SDK 5.5.0.4.0 이상" 전제는 이 앱에 해당 없음. 앱은 레거시 `play-services-ads`이고 Google 문서도 레거시 어댑터(8.3.x)를 안내. | 레거시 어댑터 8.3.0.3.0 사용. |
| C5 | Pangle 계정 생성 "기존 이메일·패스워드 동일": 계정 생성과 자격증명 입력은 개발자가 대행하지 않는다. | 클라이언트가 직접 생성 후 App ID·Placement ID·app-ads.txt 라인만 전달. 코드에는 ID가 들어가지 않는다(Bidding은 콘솔 매핑). |
| C6 | 웹 스타 프로필·콘텐츠 상세: 텍스트는 "그대로(FAQ만 제거)"인데 시안 이미지 3·4는 전면 재디자인(커버 이미지, 통계 바, Posts/Photos/About 탭, Follow·좋아요·공유·신고 버튼, 사이드바). 웹은 비로그인 익명 열람이라 Follow·좋아요·신고는 동작할 수 없다. | 텍스트 지시대로 FAQ 제거만 하고, 시안은 후속 라운드 여부를 별도 확인(Q4). 액션 버튼은 넣어도 앱 딥링크(`spOpenApp`)로만 연결 가능. |
| C7 | 시안 이미지 5·2에 한국어 부제("지구 80억 인구 랭킹시스템", "203개국")가 있는데 사이트는 영어 단일이고 CLAUDE.md 규칙도 노출 텍스트 영어. | 영어로 작성(Q6). |
| C8 | 스타·포스트 페이지 FAQ는 2-27차에 AdSense 대응(페이지 고유 텍스트·h2/h3 계층, 광고를 첫 화면 아래로 밀기)으로 넣은 것. 제거하면 글이 적은 스타 페이지는 본문이 얇아진다. | 제거는 하되 리스크를 회신에 명시. |
| C9 | Home/Posts 교차는 SEO 자산 이동을 동반: FAQPage JSON-LD·"Create. Grow. Earn." 히어로가 `/posts`로 가고, `/`는 목록이 된다. 이미 색인된 `/posts?page=N`이 바뀐다. | 매핑 교차 + canonical/sitemap/pager 동기 수정. 필요 시 `/posts?page=N` → `/?page=N` 301. |
| C10 | 비로그인 배너: 2-29에서 "로그인 카드와 같은 셸·같은 높이"로 확정했던 것을 이미지 1장으로 번복. 이미지 20:7 비율이라 높이가 달라진다. 이미지 안 텍스트는 번역·접근성 대상이 안 된다. | 이미지 방식 채택(클라이언트 확정), aria-label로 보완. 높이 차이는 Q2. |
| C11 | Meta 처리: 첫 메일 "정상 참여", 둘째 메일 "격리 후 재활성화". | 콘솔 작업이라 코드 무관. 어느 쪽인지 확정(Q9). |

---

## 4. 승인 필요 항목

1. **Android minSdk 23 → 24 상향**(Android 6.0 지원 종료 — 제품 결정). Pangle 착수 전제.
2. **AGP 8.2.1 → 8.5.1+ 상향**(16KB 정렬). 별도 태스크로 분리, R8과 분리 검증.
3. **Android 의존성·저장소 추가**: `com.google.ads.mediation:pangle`, Maven `artifact.bytedance.com/repository/pangle` (platformConfigurement/android/build.gradle).
4. **iOS Pod 추가**: `GoogleMobileAdsMediationPangle` 5.8.0.7.0 + Info.plist SKAdNetwork 1건 추가.
5. **앱 용량 증가** 수용(Pangle SDK는 Meta보다 큼 — 빌드 후 실측 보고).
6. **app-ads.txt 수정 + WAR 재배포 + Cloudflare Worker 폴백 갱신**(Liftoff·Meta 라인도 Worker 미반영 상태).
7. **개인정보 처리방침 광고 파트너 목록**에 Pangle 추가(정책 문서 변경).
8. **웹 URL 구조 변경**(`/`↔`/posts` 콘텐츠 교차, 페이지네이션 URL) — SEO 영향 있는 변경.
9. **웹 신규 이미지 에셋 수령**: 홈 히어로 배경, 카테고리 대표 이미지(시안 캡처는 원본이 아니라 사용 불가).
10. **앱 에셋 추가**: `assets/img/global-ranking.webp`(145KB).
11. `package.json`·DDL 변경: 없음. `globals.properties` 변경: 없음.

---

## 5. 미결 질문 (클라이언트 회신 필요)

- **Q1 (VS)** DAILY 유형 카드에도 글로벌 순위(#)를 보이나, 아니면 일간 순위인가? 요청은 "글로벌랭킹(#15)"이라 글로벌로 가정. 순위표 밖 스타(글로벌 순위 없음)는 빈칸 처리해도 되는가?
- **Q2 (배너)** 비로그인 배너를 이미지 비율(20:7)대로 두면 로그인 카드보다 낮아진다. 높이 불일치를 허용하는가, 아니면 이미지 양옆을 잘라 맞추는가?
- **Q3 (배너)** "로그인 사용자 상단 우측 지구 그림"의 위치가 로비 헤더인지 My Global Ranking 카드인지 불명확. 메일대로 이번 차수에서 제외해도 되는가?
- **Q4 (웹)** 스타 프로필·콘텐츠 상세는 "그대로 + FAQ만 제거"인가, 시안 이미지 3·4대로 재디자인인가? 재디자인이면 Follow·좋아요·공유·신고 버튼은 웹에서 동작 불가(앱 열기로만 연결) — 그래도 넣는가?
- **Q5 (웹)** 새 Home(포스트 목록)에는 FAQ 블록이 없다. "하단 FAQ 빼주기"가 푸터의 FAQ 링크 열을 뜻하는가?
- **Q6 (웹)** 시안의 한국어 부제는 영어로 바꾸는가? (사이트는 영어 단일)
- **Q7 (웹)** 모바일 시안(이미지 5)의 하단 탭바·기능 칩·Explore by Category·203개국 배너는 이번 범위인가, "1회 수정"에 포함인가? "203개국" 수치의 근거는?
- **Q8 (웹)** 시안의 AdSense 슬롯 2곳: 승인 전이라 코드는 넣지 않고 자리만 비워 두는 것으로 진행해도 되는가?
- **Q9 (Pangle)** Meta는 "정상 참여 유지"인가 "격리 후 재활성화"인가?
- **Q10 (Pangle)** minSdk 24(Android 6.0 지원 종료)와 AGP 상향을 수용하는가? AppLovin 때 같은 이유로 보류된 건이다.
- **Q11 (Pangle)** Pangle KR Closed Beta 권한 신청 여부 · Pangle US 매핑을 1차에 넣을지.
- **Q12 (Pangle)** Pangle 계정은 클라이언트가 직접 생성하고 ID만 전달하는 방식으로 진행 가능한가?
- **Q13 (공통)** 착수 순서: 항목 1·2(앱, 소규모) → 항목 3(웹, 중규모) → 항목 4(선행 조건 해소 후)로 제안. 동의하는가?

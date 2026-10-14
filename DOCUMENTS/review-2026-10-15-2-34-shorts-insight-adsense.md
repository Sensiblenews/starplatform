# 2-34차 요청서 검토 — 숏폼 조정·로비 콘텐츠란 추가·용량/사운드 / 인사이트 / AdSense (2026-10-15)

요청서 = 클라이언트 메모(1-1~3-2, 12개 항목) + 외부 AI 문서 4종(Star Shorts UI, 조회수·저장, 용량·인코딩, 자동 사운드, AdSense 거절 원인) + 유튜브 쇼츠 스크린샷 1장.
문서 안의 Express·MySQL 코드, `shorts`/`short_saves` 테이블 예시, `/api/shorts/:id/*` 경로, MainActivity·capacitor.config 예시,
Cloud Run·Lambda, "N단계" 순서는 참고 자료로만 보고 현 구조(Spring 4.1 + MyBatis + JSP, Ionic 8 + Capacitor 6) 기준으로 다시 설계했다.

메일 말미의 "총 12 / 킥오프 5 / 완료 5 / 1차 수정완료 2" 는 대금 조건으로 보이며 기술 검토 대상이 아니다(회신은 사용자 판단).

---

## 0. 현 구조에서 확인한 사실 (코드·운영 실측)

| 항목 | 현재 |
|---|---|
| 로비 순서 | ① Latest Contents 숏폼 → ② VS → ③ LIVE NEWS 티커 → ④ My Global Ranking → ⑤ 네이티브 광고 1 → ⑥ Today's TOP → Trending → 광고 2 → Discover → 탭바 → TOP100 → 광고 3 → New Creators (`lobby.page.html:355~`) |
| 숏폼 카드 | `pages/lobby/components/latest-contents-carousel/`. 제목 "Latest Contents"(18px) + 빨간 LIVE 배지(13px) / 왼쪽 메인 영상 + 오른쪽 "Next" 썸네일(라벨 15px) / 하단 아바타·이름(16px)·"StarPage >"(12px)·`5 / 6` 인덱스(12px). VS 카드 머리글 글씨는 11~12px |
| 전체 화면 플레이어 | `latest-contents-player.component.ts` — 닫기·"Tap for sound"·하단 프로필만. **우측 액션 버튼·숫자 없음** |
| 좋아요·댓글·공유 | 이미 있음. `/api/super/like/toggle`(WH_STAR_CONTENT.LIKE_CNT), `/api/super/comment/*`(COMMENT_CNT, 댓글 모달 `comment-modal.component.ts`), `/api/super/share/add`(SHARE_CNT). 스타 페이지에서 쓰는 것과 같은 API |
| 조회수·저장 | **없음.** 게시물 단위 조회수 컬럼 없음(AD_VIEW_CNT 는 프로필 단위). 저장/북마크 개념 전무. 숏폼 전용 통계 `WH_LATEST_CONTENT_EVENT`(노출·재생·클릭·프로필 클릭, 기기·일 단위 유니크)는 있음 |
| 숏폼 어드민 | `/super/latest-contents/*` + `latest_contents.jsp`: 기존 게시물 선택·직접 업로드(비동기)·개수(1~30)·간격(3~10초)·순서(드래그+숫자)·게시 기간·Featured·숨김. **선택 목록은 영상 글만**(`selectLatestContentCandidates` MEDIA_TYPE='VIDEO', 최대 50건). 사이드바 "로비 편성" 그룹 = Latest Contents / VS / LIVE NEWS |
| 스타 페이지 광고 삽입 | `star-page.page.ts:987` `insertAdSlots()` — 3·9·16번째 글 뒤 고정 위치(“N개마다” 규칙 아님) |
| 영상 용량 | 스타 글쓰기(`admin-write-modal`) **15MB, 클라이언트 검사만**. 영상은 **base64 JSON**으로 `/api/super/star/feed/add` 에 실려 가고 서버는 **요청 안에서 동기 ffmpeg 변환**(`SuperAppService:1714`). 회원 글쓰기(`write-modal`)는 영상 상한도 변환도 없음. DM 30MB·변환 없음. 어드민 숏폼 직접 업로드만 multipart + 100MB + 비동기 작업 큐 |
| 사운드 | 로비 카드는 `muted` 고정, 전체 화면 플레이어는 소리 켠 재생 시도 → 거부 시 음소거 + "Tap for sound". Android `setMediaPlaybackRequiresUserGesture` 미설정, capacitor.config iOS 미디어 설정 없음 |
| 별 아이콘 메뉴 | 로비 헤더 별 → 탭 **M(My Favorites)·F(Followers)·V(Visitors)**, 스타 로그인 시만 탭 노출 (`lobby.page.html:132`). 인사이트는 스타 페이지의 `my-insight-modal`(오늘·이번 달·누적 숫자만)에 따로 있음 |
| My Favorites 0 | 목록이 **기기 localStorage(`favorite_stars`)에만** 있고, 비어 있으면 API 를 부르지 않는다. 서버(`WH_FOLLOWER_LOG`)에는 기록이 있으나 목록 복원에 쓰지 않음. 재설치·기기 변경·**스타 로그인(deviceId 가 starId 로 바뀜)** 시 0 |
| 통계 원천 데이터 | 방문 `WH_AD_LOG`(일시 있음) / 즐겨찾기 `WH_FOLLOWER_LOG`(FOLLOW·UNFOLLOW, 일시) / 좋아요 `WH_LIKE_LOG`(일시) / 게시물 `WH_STAR_CONTENT.CREATED_DATE`. **글로벌 랭킹 이력 테이블은 없음**(순위는 누적 카운터로 매번 계산, 일간은 Redis 48시간) |
| 차트 | 앱(package.json)에 차트 라이브러리 없음. 어드민 JSP 는 Chart.js CDN 선례 있음(`stat/statMain.jsp`) |
| 어드민 권한 | SM(글로벌) / LC(국가) / ST(스타 본인, `/star/*` 마이페이지). **SM 이 특정 스타의 어드민으로 "들어가는" 기능은 없음**, 스타별 통계 화면도 없음 |
| 상단고정·판매채널 | "상단 고정"은 **게시물 단위**(IS_PINNED + 홍보 링크, `star_list.jsp` 게시물 액션). 스타 단위는 "인기"(IS_POPULAR)·순서 슬롯. **"글로벌 판매채널"은 코드에 없음**(2-29 /about 작성 때도 미확인 항목으로 남김) |
| AdSense | 6개 JSP 에 소유권 스크립트·메타만 있고 `<ins>` 광고 단위는 0개(점선 플레이스홀더 14곳). 광고 게이트 스크립트는 심사용으로 빼둔 상태. 카카오 애드핏 흔적 없음 |
| 웹 딥링크 | 콘텐츠·홈 페이지 자동 앱 이동은 **이미 제거됨**(클릭 시에만 `spOpenAppRoute`). 자동 이동은 조회 실패 시 폴백 `deeplink_redirect.jsp`(noindex) 하나뿐. 0.5초 타이머·market:// 없음. 다만 `assetlinks.json` 에 `handle_all_urls` 가 있어 **앱 설치 기기에서는 Android OS 가 witch-hunting.com 링크를 바로 앱으로 연다(App Links)** — 클라이언트가 본 "0.5초 만에 앱으로"는 이것으로 추정 |
| 크롤러 시점 HTML | Googlebot 모바일 UA 로 운영 실측(2026-10-15): `/` `/about` `/faq` `/posts` 모두 200, 태그 제거 후 텍스트 25~51KB. JS 없이도 본문이 HTML 에 있음(JSP 서버 렌더링). 요청서의 "CSR 빈 화면" 전제는 2-29 때와 같은 이유로 해당 없음 |

---

## 1. 요구사항 목록

### 1-1 Star Shorts 카드 (로비 상단 숏폼)
- R1. 제목 "Latest Contents" → **"Star Shorts"**. 크기는 VS 카드 제목 정도로 작게.
- R2. "Next" 텍스트 제거, LIVE 배지 제거(실제 라이브 방송 기능은 없음 → 그냥 제거).
- R3. 메인 영상이 카드 가로·세로를 최대한 사용(오른쪽 빈 공간 제거), 원본 비율 유지.
- R4. 다음 숏폼 미리보기는 현재 크기의 가로·세로 약 50%, 메인 영상 오른쪽 상단에 겹쳐 배치(조금 위로). 터치 시 메인으로 전환, 스와이프·자동 전환 시 함께 갱신.
- R5. 게시자 원형 프로필(24~30px) + 이름(10~12px)을 메인 영상 하단 왼쪽에 오버레이(반투명 배경 가능), 터치 → StarPage. "StarPage >" 도 작게.
- R6. 오른쪽 하단 `5 / 6` 인덱스 제거.
- R7. 기존 동작 유지: 활성 1개만 자동재생, 이탈 시 정지, 수동 스와이프 우선, 실패 시 포스터+재생 버튼.

### 1-2 로비 콘텐츠란 추가 + 어드민
- R8. **첫 번째 네이티브 광고와 Today's TOP 사이**에 세로형 자동재생 콘텐츠란 **"Latest Content"** 신설. 콘텐츠 3·6·7번째 뒤에 네이티브 광고 1개씩 자동 삽입(**확정** — 메모 문구 그대로, 최대 3개).
- R9. 그 아래 가로형 미니 숏폼란 **"Hot Content"** 신설. 한 화면에 1.5~2개.
- R10. 빨간 LIVE 뉴스 티커를 Star Shorts 와 VS 카드 사이로 이동(현재는 VS 아래).
- R11. 어드민 "로비 편성"의 LIVE NEWS 아래에 "세로형 자동재생", "가로형 미니숏폼" 메뉴 추가. 기능은 기존 숏폼 어드민과 완전 동일(게시물 선택·직접 게시·게시 개수·시간·순서). 게시 개수는 어드민 설정 **1~30(확정)**.
- R12. 단, 기존 게시물 선택 시 **전체 게시물 / 동영상 게시물** 구분 필터 추가.

### 1-3 용량·사운드
- R13. 동영상 게시 용량 15MB → 50MB(우선) ~ 100MB(검증 후), 서버 압축(H.264/AAC MP4). **숏폼뿐 아니라 모든 동영상**(로비 등 모든 페이지·카테고리).
- R14. 업로드 진행률·실패 원인·재시도. 인코딩 완료 전에는 게시 처리하지 않음, 실패·임시 파일 정리.
- R15. 숏폼·세로형·가로형 자동재생 시 **자동 사운드**. 차단되면 첫 터치에 소리 켜기, 그래도 안 되면 소리 켜기 버튼.

### 1-4 숏폼 전체 화면 플레이어
- R16. 전체 화면 확대 시 오른쪽 중간~하단에 세로 아이콘 열: **조회수 · 좋아요 · 댓글 · 저장 · 공유** + 각 숫자.
- R17. 좋아요·댓글·공유는 기존 로직 재사용. 조회수·저장은 신규(서버 집계, 중복 방지, 사용자별 저장 상태).

### 2-1 유저 인사이트 (앱 + 스타 본인 어드민)
- R18. 별 아이콘 메뉴에 **I(Insight)** 탭 신설, 탭 순서 **I · V · F · M**.
- R19. 그래프 5종: 글로벌 랭킹 · 방문자 · 즐겨찾기 · 좋아요 · 게시물. 기간: 일·주·월·3개월·6개월·1년, 한눈에 보기.
- R20. 같은 인사이트를 각 유저 어드민(스타 본인 `/star/*`)에도 표시.
- R21. My Favorites 가 0 으로 나오는 버그 수정.

### 2-2 글로벌·국가 어드민 인사이트
- R22. 좌측 메뉴에 대분류 **인사이트** + 소분류 랭킹·방문자수·즐겨찾기·좋아요·게시물.
- R23. 각 소분류 = 모든 유저를 해당 지표 높은 순으로 표, 기간 선택(일~1년). SM 은 전체, LC 는 자기 국가.
- R24. 유저 클릭 → 그 유저의 인사이트(R19 그래프) 바로 보기.
- R25. 랭킹 목록에서 각 유저에 "상단고정"과 "글로벌 판매채널"을 바로 지정(기존 방식).

### 3 웹 광고·문서
- R26. AdSense 광고 단위 실제 삽입(PC웹 + 모바일웹), 카카오 애드핏은 코드 발급 후 추가.
- R27. AdSense "가치 없는 콘텐츠" 대응: ① 크롤러 접속 시 자동 앱 이동 차단(클라이언트가 1번안 선택) ② JS 미실행 시 빈 화면 점검.
- R28. 자동생산·디자인 자동화 메일을 체크리스트 형태로 정리(별도 메일 — **이번 요청서에 본문 없음**).

---

## 2. 구현 방안

### 1-1 Star Shorts (core-frontend 만)
- `latest-contents-carousel` html/scss 수정: 제목 문자열·크기(VS 머리글 12px 대), LIVE·Next·인덱스 제거, 메인 영상 `width:100%` 로 카드 전체, 미리보기는 `position:absolute; top; right` 로 현재 크기의 50% 썸네일(영상이 아니라 포스터 이미지 — 동시 재생 방지), 하단 프로필 오버레이(26px/11px, 반투명 그라데이션).
- 로직(`latest-contents-logic.ts`)·어드민·API 변경 없음. 섹션 이름만 바뀌고 내부 이름(`latestContents`)은 유지.

### 1-2 로비 신규 2개 섹션 (backend → frontend)
- **백엔드**: 기존 숏폼 편성 구조를 "편성 구역" 단위로 일반화한다. `WH_LATEST_CONTENT` 와 `_SETTING` 에 `SECTION`(SHORTS / LATEST / HOT) 컬럼을 추가하고 기존 행은 SHORTS 로 채운다(테이블 복제보다 어드민·API·통계 코드 재사용이 쉬움). 어드민 컨트롤러는 같은 핸들러에 `section` 파라미터, 사이드바에는 메뉴 2개를 같은 JSP 로 연결(`activeMenu` 만 다름). 앱 API `/api/super/lobby/latest-contents?section=` 추가(기존 무파라미터 = SHORTS 로 유지 → 기존 엔드포인트 보존). 캐시 키에 section 포함.
- 게시물 선택 목록: `selectLatestContentCandidates` 의 VIDEO 조건을 `mediaFilter`(ALL/VIDEO) 로 분기. 이미지 글도 고를 수 있게 되므로 앱 응답에 MEDIA_TYPE·이미지 URL 포함.
- **프런트**: 
  - 세로형 "Latest Content": 세로 카드 목록, 화면 중앙 카드 1개만 IntersectionObserver 로 재생(스타 페이지 패턴). 이미지 글은 이미지 카드. 광고는 `insertAdSlots` 와 같은 방식으로 지정 위치에 `{isAd}` 항목 삽입.
  - 가로형 "Hot Content": 카드 폭 55~65%(1.5~2장) 가로 스크롤, 화면에 가장 많이 보이는 카드 1개만 재생.
  - 티커 위치 이동: `lobby.page.html` 블록 순서만 변경.
  - 로비 생명주기(ionViewDidEnter/WillLeave/appStateChange)에 두 섹션의 재생 정지 묶기.
- **네이티브 광고**: 로비 광고는 네이티브 오버레이 방식이라 슬롯이 늘면 오버레이도 늘어난다(현재 3개, `lobby-ad-slots.ts`). 세로형에 3개를 더하면 로비에 6개 — 스크롤 동기화·클리핑 비용과 AdMob 노출 정책을 같이 봐야 함(→ C4).

### 1-3 용량·인코딩·사운드
- **업로드 경로 재설계(핵심)**: 스타 글쓰기는 영상을 base64 JSON 으로 보내고 서버가 같은 요청 안에서 변환한다. 50MB 영상이면 본문 약 67MB, 100MB 영상이면 약 133MB 로 **Cloudflare 본문 한도(100MB)를 넘고**, 변환 시간이 CF 100초 타임아웃을 넘는다(2-33 어드민 업로드에서 실제로 겪은 문제).
  → 영상만 multipart 로 따로 올리는 엔드포인트를 **추가**(기존 `/feed/add` 는 유지)하고, 어드민 숏폼 업로드의 `LatestContentUploadJobs` 비동기 큐 + 상태 폴링 패턴을 공용화해 재사용. 글은 "처리 중"으로 저장 → 변환 완료 시 검수 대기(MDR) 흐름으로 넘김 → 실패 시 상태 표시·재시도.
  - 앱: `admin-write-modal`(스타), `write-modal`(회원) 상한 50MB → 검증 후 100MB, 진행률(HttpClient reportProgress), 실패 메시지·재시도.
  - 변환 옵션: 현재 `toWebMp4`(libx264/aac/faststart)에 해상도 상한(긴 변 1080 또는 720)·비트레이트 상한 추가.
  - 100MB 를 넘기려면 청크 업로드가 필요하나 요청서 상한이 100MB 라 이번엔 불필요. 단 정확히 100MB 파일 + multipart 헤더는 한도를 살짝 넘으므로 **앱 상한은 95MB 정도로** 잡는다.
  - DM(30MB)은 "모든 동영상" 범위인지 확인 필요(Q9).
- **사운드**:
  - Android: `MainActivity` 에서 WebView `setMediaPlaybackRequiresUserGesture(false)` (android/ 는 gitignore → `platformConfigurement/android` 사본 동시 수정). 이러면 앱 실행 직후 소리 자동재생이 가능해진다.
  - iOS: WKWebView 는 앱 실행 직후 소리 자동재생을 막는다(Capacitor 설정 노출 여부는 실기기 실측 필요). 첫 터치에 활성 영상 `muted=false` + `play()` 재시도로 해제, 해제 상태를 앱 전역(서비스)에 보관해 이후 Star Shorts·세로형·가로형 모두 소리로 재생.
  - 항상 **소리 나는 영상은 화면 전체에서 1개**(세 섹션 공용 재생 관리 서비스). 각 카드에 음소거 토글 아이콘.
  - 2-29~2-31 때 iOS 첫 터치 틱 소리 이슈가 있었으므로 unlock 은 `TickSoundService` 와 충돌 없게 한 곳으로 모은다.

### 1-4 플레이어 우측 액션 열 (backend → frontend)
- **백엔드**: 
  - 조회수: `WH_STAR_CONTENT.VIEW_CNT` 컬럼 + 중복 방지 테이블(`CON_ID, DEVICE_ID, 30분 버킷` UNIQUE, INSERT IGNORE → 1행 들어갈 때만 +1). API `POST /api/super/content/view`. 회원 인증이 없는 앱이라 시청자 식별은 기존과 같은 deviceId(좋아요와 동일 수준의 신뢰도).
  - 저장: `WH_CONTENT_SAVE(CON_ID, DEVICE_ID, CREATED_DATE, UNIQUE)` + `SAVE_CNT` 컬럼. API 저장/취소/상태. 로그인 요구 없음(좋아요·즐겨찾기와 같은 기기 단위) — 요청서의 "로그인 안내"는 현 구조에 회원 로그인이 없어 해당 없음.
  - 숏폼 목록 API 응답에 LIKE/COMMENT/SHARE/VIEW/SAVE_CNT + 내 좋아요·저장 여부 포함(스와이프마다 추가 호출 없음).
- **프런트**: 플레이어 오른쪽에 세로 버튼 5개(아이콘 + 축약 숫자 1.2K 형식). 좋아요 = `/like/toggle`, 댓글 = 기존 `comment-modal` 열기, 공유 = `/share/add` + Capacitor Share. 조회수는 재생 시작 시 1회 보고.

### 2-1 인사이트 (backend → frontend, 스타 어드민 JSP)
- **집계 테이블**: `WH_STAR_DAILY_STAT(STAT_DATE, PRS_ID, VISIT_CNT, FOLLOW_ADD, FOLLOW_DEL, LIKE_CNT, POST_CNT, GLOBAL_RANK, SCORE)`. 매일 새벽 스케줄러가 전날분을 로그에서 집계·순위 스냅샷 저장. 1년 범위를 원본 로그(`WH_AD_LOG` 등)로 매번 GROUP BY 하면 무겁다.
  - 방문·즐겨찾기·좋아요·게시물은 로그에 일시가 있어 **과거분 백필 가능**.
  - **글로벌 랭킹은 과거 기록이 없다.** 순위는 누적 카운터로 매번 계산했기 때문에 과거 날짜의 순위를 정확히 되살릴 수 없다 → 스냅샷 시작일부터 쌓이고, 과거는 로그 기반 추정치로 채울지 결정 필요(Q13).
  - 주·월·3/6/12개월은 일별 테이블을 합산(순위는 기간 말일 값 또는 기간 평균 — Q14).
- **API**: `/api/super/my-insight/series?metric&period` (starToken 필요, 기존 my-insight 와 같은 인증).
- **앱**: 별 메뉴 탭에 I 추가, 순서 I·V·F·M, 기본 탭 I. 그래프는 **차트 라이브러리 추가(승인 필요)** 또는 SVG 로 직접 그리기(의존성 없음, 막대/꺾은선 5개 수준이면 충분) 중 선택(A2).
- **스타 어드민**: `/star/mypage` 에 인사이트 섹션, Chart.js CDN(기존 statMain.jsp 선례, npm 의존성 아님).
- **My Favorites 0**: 서버에 기기별 즐겨찾기 목록 API(`WH_FOLLOWER_LOG` 의 기기별 최종 상태가 FOLLOW 인 스타) 추가 → localStorage 가 비었으면 서버에서 복원·병합. 스타 로그인 시 deviceId 가 바뀌는 문제는 로그인 전 기기 ID 로도 조회해 합친다.

### 2-2 글로벌·국가 어드민 인사이트 (backend JSP)
- 사이드바에 "인사이트" 그룹 + 소분류 5개(아코디언 규칙대로 `nav-sub`). 한 컨트롤러·한 JSP 에 `metric` 파라미터.
- 목록: `WH_STAR_DAILY_STAT` 기간 합산 내림차순, 페이징, LC 는 국가 조건(기존 `star_list` 의 국가 필터 재사용).
- 유저 클릭 → SM/LC 용 **스타 인사이트 상세 페이지**(`/super/insight/star.do?id=`, 2-1 과 같은 그래프). 스타 본인 어드민으로 로그인 전환(대리 로그인)은 권한·감사 로그 문제가 있어 권장하지 않음(C7).
- 랭킹 목록 행 액션: "인기(상단) 지정"은 기존 `togglePopular` 재사용 가능. "글로벌 판매채널"은 대상 기능이 없어 정의 필요(Q16).

### 3 웹 (backend JSP)
- AdSense: 플레이스홀더 14곳에 `<ins class="adsbygoogle">` 삽입 + 빼둔 광고 게이트(본문 짧으면 미노출·무충전 시 접기) 복원. **슬롯 ID 가 3314400335 하나뿐**이라 위치별 단위를 AdSense 콘솔에서 더 만들지(권장: 위치별 리포트 가능), 하나를 반복할지 결정 필요. 2-32 규격(PC ≥1024 / 모바일 ≤768 브레이크포인트 통일)도 같이 정리.
- 카카오 애드핏: 코드 수령 후 같은 슬롯 구조에 네트워크별 분기(국가·no-fill 기준은 Q19).
- 크롤러 자동 이동 차단(1번안): 자동 이동이 남은 곳은 폴백 `deeplink_redirect.jsp` 하나 → 기존 `LandingVisitService.isCrawler` 를 써서 크롤러면 즉시 intent 를 생략하고 정적 안내를 보여준다. 작업량 작음. 단 이것으로 AdSense 판정이 바뀔 가능성은 낮다(C9).
- JS 미실행 빈 화면: 실측상 해당 없음 — 회신용 근거로 정리만.

---

## 3. 충돌·불가 항목과 대안

- **C1. 자동 사운드 vs 음소거 시작.** 클라이언트 메모(1-3)는 자동 사운드, 같은 묶음의 Star Shorts 문서(6절)는 "음소거 자동재생". 또 2-31 때 "로비는 소리 끔"으로 정했었다. → 1-3 메모(최신)를 따르되 음소거 토글 제공을 권장. 확인 필요(Q7).
- **C2. iOS 는 앱 실행 즉시 소리 자동재생을 보장할 수 없다.** WKWebView 정책. 첫 터치 해제가 현실적 최대치(문서 스스로도 인정). Android 는 네이티브 설정으로 가능.
- **C3. 50~100MB 는 클라이언트 상한만 올리면 안 된다.** base64 JSON 경로 + 동기 변환은 Cloudflare 본문 100MB·응답 100초 한도에 걸린다. 업로드 경로를 multipart + 비동기 변환으로 바꾸는 것이 전제(2절). 서버 CPU·디스크 여유는 KT 서버 SSH 접근이 없어 확인 불가(cost-guard 때부터 미결) — 100MB 확정 전 실측 필요.
- **C4. 로비 네이티브 광고 6개.** 현재 네이티브 오버레이 3개도 Android 스크롤·클리핑 이슈를 거쳐 안정화했다(2-33). 세로형 안에 3개를 더하면 오버레이 수·메모리·AdMob "콘텐츠 대비 광고 과다" 정책 위험 증가. 대안: 세로형 안 광고는 1~2개로 시작, 또는 세로형 노출 개수 대비 비율 제한.
- **C5. "세로형 자동재생"에 이미지 글.** 전체 게시물 선택을 허용하면 이미지 글이 섞인다 — 이미지 카드로 표시(자동재생 없음). 이게 의도인지 확인(Q4).
- **C6. 글로벌 랭킹 그래프의 과거 구간.** 랭킹 이력이 저장된 적이 없어 1년 그래프는 스냅샷 시작일 이후만 정확하다. 나머지 4종은 로그 백필 가능(로그 시작 시점 이후).
- **C7. "해당 유저 어드민으로 이동".** SM/LC 가 스타 계정 세션으로 들어가는 구조는 없다. 대리 로그인은 보안·감사 문제 → 어드민 쪽에 스타별 인사이트 상세 페이지를 두는 것으로 대체 제안.
- **C8. "글로벌 판매채널 바로 찍어주기 (기존 방식)".** 코드에 해당 기능이 없다. "상단고정"도 기존엔 게시물 단위다. 스타 단위로 무엇을 지정하는 것인지 정의가 필요(Q16).
- **C9. AdSense 거절 원인 진단이 현 구조와 맞지 않는다.**
  - 크롤러가 받는 HTML 에 본문이 이미 있다(실측 25~51KB 텍스트). "CSR 빈 화면"·"Prerender/SSR 필요"는 해당 없음(2-29 때와 같은 결론).
  - 콘텐츠 페이지에 자동 앱 이동 스크립트가 없다(2-27 에서 제거). 클라이언트가 본 즉시 앱 전환은 **Android App Links(OS 동작)** 로 추정되며, Googlebot 은 앱이 없으므로 영향 없음.
  - 따라서 1번안은 폴백 페이지에 적용해 두되, "가치 없는 콘텐츠" 원인은 다른 쪽(콘텐츠 있는 스타 13명 수준의 얇은 콘텐츠, 자동 생성 빈 국가 페이지 등)일 가능성이 높다 — 별도 점검 필요.
- **C10. "업로드 진행 중엔 정상 게시로 처리하지 않음"** 은 2-32 검수 흐름과 합쳐야 한다(변환 완료 → 검수 대기 → 승인 후 공개). 상태가 하나 늘어남(처리 중/실패).

## 4. 승인 필요 항목

- **DB 변경(DDL, 운영 수동 선실행)**
  - `WH_LATEST_CONTENT`·`_SETTING` 에 `SECTION` 컬럼(+기존 행 SHORTS)
  - `WH_STAR_CONTENT` 에 `VIEW_CNT`·`SAVE_CNT`, 신규 `WH_CONTENT_VIEW`(중복 방지)·`WH_CONTENT_SAVE`
  - 영상 처리 상태 컬럼(처리 중/실패) — 업로드 비동기화용
  - `WH_STAR_DAILY_STAT` + 과거분 백필 쿼리(운영 DB 부하 시간대 고려)
- **의존성**: 앱 그래프용 차트 라이브러리(chart.js 등) 추가 여부 — 미승인 시 SVG 직접 구현(A2).
- **네이티브 설정 파일**: Android `MainActivity` 미디어 제스처 해제(android/ + platformConfigurement 사본), 필요 시 iOS WebView 설정.
- **스케줄러 추가**: 일별 통계 집계(기존 스케줄러 스레드 1개 — 실행 시간대 조정).
- **서버 확인**: KT 서버 CPU·디스크·ffmpeg 여유(SSH 권한), Cloudflare 본문 한도 플랜.
- **AdSense 콘솔**: 위치별 광고 단위 추가 생성(클라이언트 계정 작업).
- **앱 업데이트**: 1-1~2-1 은 앱 빌드·스토어 배포 필요(trapeze 버전 올림).

## 5. 미결 질문 (클라이언트 회신 필요)

**확정 사항 (2026-10-15, 사용자)**
- ~~Q3~~ 세로형·가로형 노출 개수 = 어드민 설정, 1~30 (기존 숏폼과 동일).
- ~~Q5~~ 광고 위치는 메모 그대로 3·6·7번째 콘텐츠 뒤.
- ~~Q7~~ 자동 사운드 + 음소거 버튼.
- ~~Q8~~ 업로드 상한은 처음부터 95MB (100MB 는 Cloudflare 본문 한도에 걸려 95).
- 앱 그래프는 차트 라이브러리 없이 SVG 직접 구현, 로비 네이티브 광고 슬롯은 3→6 으로 확장.

**1-1 / 1-2**
- Q1. Star Shorts 제목 크기 기준 "VS 카드 제목" = VS 카드 머리글(배지 옆 12px 글자)인가?
- Q2. 다음 숏폼 미리보기는 정지 썸네일이면 되는가(영상 2개 동시 재생 방지 목적)?
- Q3. 세로형 "Latest Content" 는 한 번에 몇 개 노출? 무한 스크롤인가, 어드민 게시 개수까지인가?
- Q4. 세로형·가로형에 이미지 글도 들어가는가(전체 게시물 선택 허용의 의미)? 영상 터치 시 동작은 Star Shorts 와 같은 전체 화면 플레이어인가?
- Q5. 광고 위치 "3개 이후, 6개 이후, 7개 이후"는 오타인가? (3·6·9 처럼 3개마다? 스타 페이지는 실제로 3·9·16번째 뒤)
- Q6. "Hot Content" 도 어드민 수동 편성만인가, 아니면 좋아요·조회수 기반 자동 선정이 필요한가?

**1-3**
- Q7. 자동 사운드가 최종인가? (같은 날 문서에는 "음소거 자동재생"으로 되어 있음) 음소거 토글 버튼은 둬도 되는가?
- Q8. 용량은 50MB 로 먼저 배포 후 100MB 로 올리는 2단계에 동의하는가?
- Q9. "모든 동영상"에 DM(현재 30MB)과 회원 글쓰기도 포함되는가?
- Q10. 기존에 올라간 영상도 재압축 대상인가? (서버 접근 필요)

**1-4**
- Q11. 저장한 영상은 어디서 다시 보는가? (저장 목록 화면이 요청서에 없음 — 숫자·상태만이면 화면 추가 불필요)
- Q12. 조회수는 "재생 시작" 기준이면 되는가, 몇 초 이상 시청 기준인가? 같은 기기 재집계 간격(문서 예시 30분) 확정.

**2-1 / 2-2**
- Q13. 글로벌 랭킹 그래프는 오늘부터 쌓이는 것으로 충분한가? (과거 랭킹은 저장된 적이 없음)
- Q14. 주·월 단위 랭킹 값은 기간 말일 순위인가, 기간 중 최고 순위인가?
- Q15. I 탭은 지금처럼 스타 로그인 시에만 보이면 되는가(일반 사용자에겐 메뉴 탭 자체가 없음)?
- Q16. "글로벌 판매채널 찍어주기"는 정확히 무엇을 지정하는 기능인가? "상단고정"은 스타를 로비·목록 상단에 고정하는 것(현 "인기" 기능)인가, 게시물 고정인가?
- Q17. "해당 유저 어드민 이동"은 어드민 화면에서 그 유저의 인사이트 그래프를 보는 것으로 충분한가? (스타 계정으로 대리 로그인은 권장하지 않음)
- Q18. My Favorites 0 은 어떤 상황에서 재현되는가? (스타 로그인 후 / 재설치 후 / 다른 기기) — "예전 버그 동일"의 이전 보고 시점.

**3**
- Q19. AdSense 와 카카오 애드핏은 같은 자리를 나눠 쓰는가(국내=애드핏, 해외=AdSense 등), 다른 자리인가?
- Q20. AdSense 승인 상태: 현재 재심사 대기인가, 거절 상태인가? 광고 단위 삽입은 승인 후 진행인가?
- Q21. 클라이언트가 "0.5초 만에 앱으로 이동"을 본 정확한 URL과 경로(카카오톡 링크·크롬 주소창 등)는?
- Q22. 3-2 자동생산·디자인 자동화 "별도 메일" 본문 수령 필요.

---

## 참고
- 이전 검토: `review-2026-10-12-2-33-shortform-latest-contents.md`(숏폼 원설계), `review-2026-10-08-2-32-adsense-video-ad-moderation.md`(AdSense 자리 규격·검수), `review-2026-10-04-web-video-followup.md`(영상 변환·사운드)
- 요청서 코드(Express 라우트, `shorts` 테이블 DDL, MainActivity/capacitor.config 예시, isGoogleBot 스니펫)는 채택하지 않았다.

---

## 6. 구현 메모 (2026-10-15)

worktree `.claude/worktrees/2-34`, 브랜치 5개를 차례로 쌓았다(base origin/main 581f2ae). 아직 푸시하지 않았다.

| 브랜치 | 커밋 | 내용 |
|---|---|---|
| feature/2-34-star-shorts | 9b56de3 | 1-1 Star Shorts 개편, 1-4 플레이어 조회수·좋아요·댓글·저장·공유 |
| feature/2-34-lobby-sections | d9ce31b | 1-2 세로형 Latest Content·가로형 Hot Content·어드민 구역 편성·티커 이동·네이티브 슬롯 6 |
| feature/2-34-video-upload-sound | ec5bc2d | 1-3 스타 영상 95MB multipart + 비동기 압축, 로비 자동 사운드 |
| feature/2-34-insight | 2372ac1 | 2-1·2-2 인사이트(앱 I 탭·스타 어드민·글로벌/국가 어드민), My Favorites 0 |
| feature/2-34-web-ads | 648d14a | 3-1 AdSense 단위 스크립트(승인 전 OFF), 크롤러 자동 이동 차단 |

**배포 전 운영 DB 수동 실행 (순서대로)**
1. `core-backend/db/2-34-content-view-save.sql`
2. `core-backend/db/2-34-latest-content-section.sql` — 미실행 시 Star Shorts 까지 조회 실패
3. `core-backend/db/2-34-star-daily-stat.sql` (+ 선택: `2-34-star-daily-stat-backfill.sql`, WH_AD_LOG 인덱스 확인 후 한가한 시간에)

**구현 중 확인한 사실**
- Capacitor 가 이미 Android `setMediaPlaybackRequiresUserGesture(false)`, iOS `mediaTypesRequiringUserActionForPlayback = []` 를 설정한다. 로비가 무음이던 원인은 웹 코드의 `muted` 뿐이라 네이티브 설정 변경은 하지 않았다.
- 영상이 붙는 글쓰기 경로는 스타 글(`/api/super/star/feed/add`)뿐이다. 관리자 피드 API 는 영상을 무시하고, 회원 글쓰기(`write-modal`)는 레거시 화면 전용이다 → 95MB 경로는 스타 글에만 적용.
- 로비 네이티브 슬롯 3개는 웹 상수·템플릿·Android/iOS 컨테이너에 모두 하드코딩돼 있었다. 슬롯 번호를 `data-slot` 으로 바꿔 세로형 목록이 짧아 일부 슬롯이 없을 때도 번호가 밀리지 않게 했다.
- 세로형·가로형은 사진 글도 편성할 수 있다(전체/동영상 구분 요청). 사진 카드를 누르면 게시물 상세, 영상은 Star Shorts 와 같은 전체 화면 플레이어.

**검증한 것**: 백엔드 테스트 310건, JspC(톰캣 7 Jasper)로 바뀐 JSP 9개 컴파일, 프런트 `tsc` + 관련 Karma 47건, 내장 브라우저에서 Star Shorts 레이아웃·플레이어 액션 열(모의 응답)·로비 섹션 순서·I 탭 그래프(모의 응답) DOM 확인, web-ads.js 게이트 시뮬레이션.

**검증 못 한 것 (실기기·운영 필요)**
- Android·iOS 네이티브 슬롯 6개 — platformConfigurement 사본만 고쳤고 빌드하지 않았다. `android/`·`ios/` 로 옮긴 뒤 `ionic capacitor build` 후 실기기에서 슬롯 정렬·클리핑·광고 로드 확인.
- 소리 자동재생 — 내장 브라우저 패널이 숨겨져 있어 영상 재생 자체가 멈췄다. 실기기에서 앱 실행 직후 소리, 음소거 아이콘, 섹션 간 1개만 재생 확인.
- 새 ffmpeg scale 필터(짧은 변 1080·4Mbps) — 로컬에 ffmpeg 가 없다. 실패해도 원본으로 게시되지만 서버에서 한 번 확인 필요.
- 95MB 실제 업로드·Cloudflare 통과, 어드민 화면(로그인 필요), 00:10 집계 스케줄러 첫 실행.

**클라이언트 회신 대기로 남긴 것**: 저장 목록 화면(Q11), 글로벌 판매채널·상단고정의 정의(Q16), AdSense 승인 상태와 위치별 단위 생성(Q19·Q20), 카카오 애드핏 코드, 3-2 자동생산 메일 본문(Q22), DM 용량(Q9).

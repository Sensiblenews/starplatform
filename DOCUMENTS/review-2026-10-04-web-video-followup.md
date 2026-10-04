# 요청서 검토 — 웹 영상 재생 불량 2건 · 클릭 전체화면 뷰어 · 마이너 버그 재보고

검토일: 2026-10-04 · 기준 브랜치 `feature/2-31-web-video-autoplay` (HEAD f0b0a2d, PR #27)
차수: 메일에 번호 없음. 임의 채번하지 않고 "2-31차 웹 영상 후속"으로 표기.
메일 2통: ① 진단 메일(항목 2·3 참고용) ② 전체화면 뷰어 10단계 메일(항목 1 참고용) + 마이너 버그 상태 보고.

요청서는 Angular 프런트·Profile API 전제로 쓰였지만 witch-hunting.com 웹은 witch.war의 JSP 서버 렌더링이다.
문서의 진단 경로(trackBy, serializer, flatMap 등)는 대상이 없다. 아래 원인은 운영 HTML·영상 파일·코드를 직접 대조해 확정했다.

---

## 0. 확정된 원인

### (2) "MSK 강아지는 되고 Emma 여성 영상은 안 됨" → 영상 코덱 차이

운영 홈 2페이지의 영상 카드 3개 파일 헤더를 직접 읽었다. HTTP 응답은 셋 다 동일(200 · video/mp4 · Range 206 · Cloudflare HIT · 약 4MB)이라 네트워크·MIME·CORS 원인은 아니다.

| 글 | 스타 | 코덱 | moov 위치 | 결과 |
|---|---|---|---|---|
| /post/47 (귀요미, 강아지) | MSK | H.264(avc1) 720p | 앞(faststart) | 모든 브라우저 재생 |
| /post/43 | MSK | HEVC(hvc1) 1920 | 끝 | PC Chrome·Firefox·다수 Android Chrome 재생 불가 |
| /post/41 (여성) | Emma | HEVC(hvc1) 1920, 브랜드 mp42(아이폰 녹화) | 끝 | 위와 같음 |

- HEVC는 Safari(iOS/Mac)에서만 확실히 재생된다. 다른 브라우저에서는 `error` 이벤트가 나고, 자동재생 스크립트(`common/include/web-video-autoplay.jsp` onError)가 카드의 `<video>`를 조용히 제거해 썸네일만 남긴다 → "안 되는 영상"으로 보인다. 콘솔 로그가 없어 현장 진단도 안 된다.
- 뿌리는 업로드 경로(`SuperAppService.addStarFeed`): 앱이 보낸 영상을 변환 없이 그대로 저장한다. 아이폰 기본 녹화(고효율=HEVC)가 그대로 올라간다. 부수 버그: data-URI가 `video/quicktime`이면 `"mov"` 매칭에 안 걸려 `.mp4` 확장자로 저장된다.
- 요청서의 muted/playsinline 차이·Observer 등록·trackBy 가설은 전부 해당 없음(마크업 3장 동일, 서버 렌더링).
- 서버에 ffmpeg가 이미 있다(썸네일 생성이 ffmpeg이고 HEVC 파일의 썸네일이 실제로 생성돼 있다) → 새 의존성 없이 변환 가능. libx264 포함 빌드인지는 서버에서 확인 필요.

### (3) "스타 프로필에서 영상이 안 나옴 / 마지막에 2개가 묶임" → 프로필 목록 20건 제한

- 스타 페이지 Posts 탭은 `superapp.selectRecentStarPosts` = `ORDER BY CREATED_DATE DESC LIMIT 20`. 페이징·Load more 없음.
- 운영 MSK 페이지(`/star/SP-1776782161491`) Posts 탭은 글 128…49 정확히 20건. MSK의 영상 글 47·43은 2026-05 작성이라 21·22번째 → 잘려 나간다. 글이 20건 미만인 Emma는 41번 영상이 Posts 탭에 들어 있다(단 HEVC라 Chrome에서는 (2)로 사라진다).
- "마지막에 2개가 묶임"은 별도 버그가 아니다. MSK 영상 2개가 모두 5월 글이라 날짜순 목록(홈 2페이지)의 맨 뒤에 나란히 보이는 것. 요청서가 의심한 "video 배열을 마지막 item에 붙이는 로직"은 코드에 없다(JSP·컨트롤러 `DeepLinkController.addRelatedPosts` 모두 글 단위).
- 부수: Photos 탭은 영상 글 썸네일을 재생 배지 없이 섞어 보여 준다(동작은 함).

### (1) 전체화면 뷰어 → 신규. 웹에는 라이트박스·뷰어가 전혀 없다.

---

## 1. 요구사항 목록

### A. 웹 영상 (메일 ①·②)
- A1. 홈(PC·모바일)에서 영상마다 되고 안 되는 차이를 없앤다. 기존 스크롤 자동재생은 그대로 둔다.
- A2. 스타 프로필 페이지에 그 스타의 영상 글이 보여야 한다. 글마다 자기 영상이 독립적으로 렌더된다.
- A3. 영상 클릭 → PC·모바일 웹 모두 앱 같은 전체화면 뷰어(fixed · 100vw · 100dvh · 검정 배경).
- A4. 뷰어는 같은 영상 URL을 쓰고, 열리면서 바로 재생(사용자 제스처), object-fit: contain.
- A5. ✕ 또는 뒤로가기로 닫으면 원래 자리로 돌아오고 기존 자동재생이 다시 동작한다.
- A6. 자동재생 로직·API·DB·업로드 구조는 바꾸지 않는다(메일 ②). → A1 해결과 충돌, 3절 참조.
- A7. 수정 후 PC Web + Mobile Web에서 홈/프로필 각각 테스트.
- A8. 뷰어는 재생 문제 해결 다음 단계(메일 ① ⑩).

### B. 마이너 버그 재보고
- B1. 첫 화면 로딩 속도 — "좋아진 듯" → 조치 없음.
- B2. Android 프로필 페이지 로딩 — "그대로".
- B3. 알림 시 iOS 진동 안 됨 / Android 사운드 안 됨 — "그대로".
- B4. 앱 켠 뒤 첫 화면에서 클릭·하단 스크롤 시 사운드 제거 — "안드 개선, iOS 그대로".
- B5. iOS 방문자·채팅 알림 배지 숫자가 1에서 안 올라감(안드는 1,2,3) — "그대로".
- B6. iOS 앱 켤 때 순간 깜빡임 — "그대로".

참고: 2-31차 PR #27은 웹 영상만 담았고 B항목은 손대지 않았다. B3 iOS 진동은 2-29차(6d1e252, 9/19)에서 권한 굳음을 고쳤고 9/27 빌드 브랜치에 포함돼 있으나 릴리스 노트에는 적지 않았다 → 테스트 기기 빌드 확인 필요(5절).

---

## 2. 현 아키텍처 기준 구현 방안

### A1. 영상 호환 — core-backend(업로드 변환) + 기존 파일 1회 변환
- `SuperAppService.addStarFeed` 영상 저장 직후 ffmpeg로 H.264/AAC MP4 + `-movflags +faststart`로 변환(원본은 성공 시 폐기). 기존 `generateVideoThumbnail`의 ProcessBuilder 패턴과 레거시 `MemberService.convertVideoToMp4` 헬퍼 재사용. 변환 실패 시 원본 그대로 저장(현행 유지).
- 확장자 매핑 버그는 출력이 항상 .mp4가 되면서 자연히 해소.
- 기존 HEVC 파일 2개(18143c69…, 3fe1c99c…)는 서버에서 1회 재인코딩. 동일 파일명 덮어쓰기라 DB·URL 변경 없음, Cloudflare 캐시 퍼지 필요. 다른 HEVC 글이 더 있는지 `ffprobe` 일괄 점검.
- `web-video-autoplay.jsp` onError에 `console.warn(url, error.code)` 추가(요청서 ④ "play() 오류 확인"). 동작 변화 없음.
- 요청서 ①~⑤ 진단 항목(URL/코덱/MIME/muted/playsinline/Network 비교)은 위 표로 이미 끝났다.

### A2. 스타 프로필 영상 — core-backend(JSP + 쿼리)
- Posts 탭에 Load more 추가: 홈이 이미 쓰는 패턴(`home.jsp`의 `data-next` fetch → append → `spVideoScan`)을 그대로 적용. `selectRecentStarPosts`에 offset/size, `/star/{id}?page=N` 처리(기존 엔드포인트 유지, 파라미터만 추가).
- 대안(단순): LIMIT 20→60. 글이 많은 스타에서 재발하므로 비권장.
- `addRelatedPosts`와 `WebCardUtil.toPostCard` 중복 로직 통합(선택). Photos 탭 영상 썸네일에 재생 배지(선택).

### A3~A5. 전체화면 뷰어 — core-backend(공통 include 1곳)
- 위치: `web-video-autoplay.jsp`(홈·/posts·스타·상세 모두 이미 include). 레이어 + `<video controls autoplay playsinline>` + ✕. CSS `position:fixed; inset:0; height:100vh; height:100dvh; background:#000; object-fit:contain`. ASCII만 출력(Jasper 인코딩).
- 열기: 같은 `data-src`를 뷰어 video에 넣고 `play()`. 카드 영상은 autoPause, `history.pushState` → 뒤로가기 = 닫기. Escape·✕·배경 탭도 닫기.
- 닫기: 뷰어 video pause + src 제거 → 기존 `pick()` 재호출로 카드 자동재생 복귀(메일 ② 9단계).
- 클릭 대상(Q1): 카드 영상은 `<a href="/post/{id}">` 안에 있고 오버레이는 pointer-events:none. 권장 — 영상 카드의 미디어 영역(썸네일+재생 배지) 클릭은 뷰어, 제목·본문 클릭은 글 페이지. 상세 페이지 영상은 controls 클릭과 겹치므로 펼침 버튼으로 연다.
- 소리: 사용자가 클릭했으므로 뷰어는 소리 켜고 시작, controls로 끌 수 있음(Q2).
- iOS Safari는 요소 전체화면 API가 없어 fixed 레이어 방식이 맞다(메일 ② 5단계와 일치).
- 스타 페이지에서 뷰어를 열어도 방문 로그는 페이지 진입 시 1회뿐(변경 없음).

### B. 마이너 버그 — core-frontend(+ 일부 core-backend), 앱 재배포 필요
- B2 Android 프로필 로딩: 2-26차 서버 수정 이후 실측이 없다. 앱에 `perfTrace` 계측(localStorage `perfTrace=1` → console.table)이 있으므로 수치 먼저. 코드상 후보: `loadStarDetail` → `loadRecommendedPages` 직렬 2회 요청(병렬화 가능), 프로필·추천 아바타 이미지 lazy 미적용, 재진입 시 `refreshInBackground` 최대 100건 재조회. 수치 없이 고치지 않는다.
- B3 Android 사운드: 원인 확정. `star_visitor_channel`은 2026-06-06에 소리 없이 만들어졌고 `sound:'tick'`은 2026-09-06에 추가됐다(`app.component.ts`). Android는 이미 생성된 채널의 소리를 앱이 바꿀 수 없다 → 9/6 이전 설치 기기는 영구 무음. 해결: 채널 id를 `*_v2`로 바꾸고 백엔드 `FirebaseService`의 channelId도 함께 변경. 어드민 전체 푸시 채널 `500`(`notifications.service.ts`)은 소리 설정 자체가 없음 → 같은 방식.
- B3 iOS 진동: 코드 수정(6d1e252)은 돼 있다. 테스트 기기가 9/27 빌드인지, 설정>알림>Star Platform 소리 켜짐인지 먼저 확인(Q5). 재현되면 시뮬레이터 `simctl push`로 검증.
- B4 첫 화면 사운드: 로비 틱은 30초 폴링 NEW_VIEWS가 0~29초 랜덤 지연으로 울리는 것(`lobby.page.ts distributePolledViews`)과 카드 탭 `triggerDopamine`. "클릭·스크롤 시"는 체감 타이밍이지 트리거가 아니다. 해결: 로비에서 틱 소리를 끈다(햅틱 유지 여부 Q6). 스타 페이지 틱은 유지.
- B5 iOS 배지: `FirebaseService`가 항상 `setBadge(1)`(주석: 클라이언트가 점만 원함). Android는 런처가 알림 개수를 세는 것. iOS는 서버가 숫자를 줘야 한다 → Redis `badge:{token}` INCR 값을 badge로 전송, 앱 기동 시 `Badge.clear()` 자리에서 리셋 API 호출(신규 `POST /api/super/star/push/badge/reset`). 잠금화면에서 알림만 지우면 앱을 열 때까지 숫자가 남는 건 iOS 한계.
- B6 iOS 깜빡임: 가설 ① `LaunchScreen.storyboard` 배경이 `systemBackgroundColor`(다크 모드면 검정)인데 웹 body는 흰색 → 전환 순간 색 반전. ② `SplashScreen.hide({fadeOutDuration:1000})`가 초기화 체인 끝에 실행. 조치: 런치 배경 흰색 고정, `capacitor.config.ts` SplashScreen `backgroundColor` 명시, WKWebView 배경 통일. 시뮬레이터 다크 모드로 재현 확인 후 적용.

---

## 3. 충돌 항목 + 대안

| 요청서 내용 | 실제 | 대안 |
|---|---|---|
| Angular `*ngFor`/trackBy/Profile API serializer 점검 | 웹은 JSP 서버 렌더링. Angular는 앱 번들 전용 | 해당 없음. 원인은 0절로 확정 |
| "DB 변경·업로드 변경·영상 변환 변경 없음"(메일 ②) | A1의 뿌리가 업로드 시 무변환이라 변환 없이는 Emma 영상이 Chrome에서 영원히 안 된다 | 업로드 변환(서버 ffmpeg, 의존성 추가 없음) + 기존 파일 1회 재인코딩. DB·API는 그대로 |
| "video 배열을 마지막 item에 묶는 버그" | 존재하지 않음. 20건 제한 + 날짜순 | Load more |
| 카드 영상 클릭 → 뷰어 | 2-29 후속에서 "카드 클릭은 글 페이지로"로 정정(f728a68) | 미디어 영역만 뷰어, 나머지는 글 페이지(Q1) |
| 뷰어 `100dvh` | iOS 15.4+/Chrome 108+ 지원 | `100vh` 폴백 1줄 추가 |
| 운영 페이지에 Cloudflare Rocket Loader가 인라인 스크립트를 지연 로드 중(로컬엔 없음) | 현재 MSK 영상이 되므로 치명적이진 않으나 운영 전용 변수 | 그대로 두되 검증은 운영 URL로 |

---

## 4. 승인 필요 항목

1. 업로드 시 ffmpeg 변환 도입 — 새 라이브러리 없음. 단 (a) 서버 ffmpeg의 libx264 포함 확인, (b) 동기 변환이라 영상 업로드 응답이 수 초~수십 초 길어짐(용량 상한 제안 100MB), (c) 톰캣 CPU 사용. 비동기 변환은 상태 컬럼이 필요해 DB 변경이 되므로 1차는 동기.
2. 운영 서버 1회 작업 — 기존 HEVC 2개 재인코딩 + 전수 점검 + CF 캐시 퍼지. SSH 권한 미결이면 런북으로 전달.
3. API 추가 2건(삭제 없음) — `/star/{id}?page=N`, `POST /api/super/star/push/badge/reset`.
4. Redis 키 추가 — `badge:{token}`(TTL 30일). globals.properties 변경 없음.
5. 푸시 채널 id 변경 — 백엔드 payload + 앱 동시 변경. 구 앱은 기본 채널로 떨어짐(소리 없음, 현재와 동일). 앱 스토어 재배포 필수.
6. B 전체가 앱 재배포 전제(versionCode 상향, `trapeze-android.yml`). 웹 A는 WAR 배포만.
7. 범위·대금: 메일 ①은 "뷰어는 다음 단계", 메일 ②는 뷰어를 본 작업으로 쓴다. 한 차수로 묶을지 분리할지.

---

## 5. 미결 질문 (클라이언트)

- Q1. 영상 카드에서 어디를 눌러야 뷰어가 열리나? (권장: 썸네일·재생 배지 = 뷰어, 글 제목·본문 = 글 페이지. 9/26에 "카드 클릭은 글 페이지"로 정정하셨기 때문)
- Q2. 뷰어는 소리 켜고 시작? (권장: 켜고. 사용자가 눌렀으니 자동재생 정책에 걸리지 않음)
- Q3. Emma 영상·MSK 2번째 영상이 아이폰 HEVC 원본이라 PC Chrome에서 안 되는 것. 기존 파일을 H.264로 재인코딩해도 되나(화질 유지, 용량 비슷)? 앞으로 업로드는 서버에서 자동 변환(업로드가 몇 초 길어짐) — 수용?
- Q4. 스타 프로필 Posts 탭 — "더 보기" 20건씩 vs 한 번에 더 많이. (권장: 더 보기)
- Q5. 마이너 버그 테스트 기기의 앱 버전(설정>앱 정보)과 설치 시점. Android는 9/6 이전 설치면 채널 무음이 설명되고, iOS는 9/27 빌드인지에 따라 진동 수정 포함 여부가 갈림. iOS 기기 다크 모드 사용 여부(깜빡임 가설).
- Q6. "첫 화면 사운드 제거" — 로비에서 소리만 끄고 진동은 둘지, 둘 다 끌지. 스타 페이지 틱은 유지?
- Q7. iOS 배지 숫자 = "마지막으로 앱을 연 뒤 받은 알림 수"로 정의해도 되나(Android 동작과 동일).
- Q8. Android 프로필 로딩 — 기기명·네트워크·체감 초. 또는 `perfTrace` 결과 캡처 요청.
- Q9. 이번 메일 묶음의 차수 번호.

---

## 검증 계획(구현 단계용)

- A1: 로컬에서 HEVC 샘플 업로드 → 저장 파일 `ffprobe`로 avc1 확인; 운영 재인코딩 후 `curl -r 0-65535`로 moov 위치·avc1 확인.
- A2: 로컬 톰캣(운영 DB)에서 `/star/SP-1776782161491` Posts 탭 Load more로 47·43 노출 확인. 스타 페이지 열람은 JS가 방문을 보고하므로 curl로만(브라우저로 열면 MSK에게 방문 푸시가 간다).
- A3~A5: 내장 브라우저(패널이 화면 안에 있어야 자동재생 IO가 돈다)에서 클릭→뷰어→뒤로가기→자동재생 복귀, 모바일 375px·100dvh.
- B3/B5: iOS 시뮬레이터 `simctl push` 페이로드로 badge 숫자·소리; Android 에뮬레이터에 구 빌드 설치 후 새 빌드 덮어 채널 v2 소리 확인.
- B6: 시뮬레이터 다크 모드 실행 영상 전후 비교.
- 테스트: `WebCardUtilVideoTest` 확장, 백엔드 `./mvnw.sh test`, 프런트 `npx tsc --noEmit` + 관련 spec.

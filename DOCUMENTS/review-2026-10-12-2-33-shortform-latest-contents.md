# 2-33차 2번 항목 검토 — 로비 숏폼(Latest Contents) + 어드민 편성 + 동영상 용량 3배 (2026-10-12)

요청서(외부 AI 생성 3종 + 클라이언트 메모·시안 3장)에서 요구사항만 추출했다. 문서 안의 Angular 예시 코드·Swiper 선택·
`/api/v1/...` 경로·20단계 순서·`latest_contents` 컬럼 예시는 참고 자료로만 보고 현 구조 기준으로 다시 설계했다.

시안 해석: 1·2번은 1/3 높이 안(폐기), 3번이 최종(1/2 높이, 메인 카드 + 오른쪽 다음 카드 일부, 하단 프로필+이름, LIVE 뱃지).

## 0. 현 구조에서 확인한 사실

| 항목 | 현재 |
|---|---|
| 로비 순서 | VS → 티커 → My Ranking → 광고1 → Today's TOP → Trending → 광고2 → Discover → 탭바 → TOP100 → 광고3 → New Creators. **요청서 순서와 이미 일치**(1번 항목에서 완료). 숏폼만 헤더–VS 사이에 삽입하면 됨 |
| 캐러셀 선례 | `vs-carousel`(setInterval 자동 이동 + touchstart/touchend 스와이프, 자체 구현). Swiper 8.4.7은 package.json에 있으나 로비에서 미사용 |
| 영상 자동재생 선례 | 스타 페이지 `IntersectionObserver`(threshold 0.8) + `muted playsinline autoplay loop` — iOS/Android 동작 확인됨 |
| 로비 생명주기 | `ionViewDidEnter/WillLeave` + `appStateChange`로 VS 폴링·티커를 켜고 끔. 숏폼 타이머도 같은 자리에 묶으면 됨 |
| 어드민 선례 | `/super/vs/*`(list·pin·visible·order·custom insert·starSearch) + `vs_list.jsp`(드래그 행 + 숫자 입력 + 자동 재번호). `/super/live-news/*`는 캐시 무효화(@CacheEvict) 선례. 메뉴는 `super/sidebar.jsp`, 권한은 `PRS_AUTH='SM'` |
| 앱 API·캐시 | `/api/super/lobby/vs-cards`·`live-news` 모두 `@Cacheable` + 어드민 변경 시 evict. 같은 패턴 적용 |
| 영상 데이터 | `WH_STAR_CONTENT`(CON_ID, PRS_ID, CON_BODY, MDR_STATUS…) + `STAR_CONTENT_MEDIA`(MEDIA_TYPE='VIDEO', MEDIA_URL, THUMB_URL) + `WH_PRESS`(PRS_NAME, STORED_FILE_NM). 게시물 상세 화면 `/feed-detail/:conId` 존재 |
| 검수 정책 | 영상도 2-32차부터 검수 대기(`MDR_STATUS`). 승인 전 글은 URL 자체를 내려주지 않음 → 숏폼도 `APPROVED`만 노출해야 함 |
| 동영상 업로드 상한 | **스타 글쓰기 실제 경로는 `admin-write-modal` → 5MB** (안내문 "Video size must be less than 5MB. Current size: …"). `video.service.ts`의 140MB/1분 검사는 레거시 `write-modal`용이라 현 플로우에 안 걸림. DM은 30MB 별도. 서버는 base64 JSON(`/api/super/star/feed/add`)로 받고 크기 상한 없음(multipart도 -1) |

## 1. 요구사항 목록

### 로비 숏폼 영역
- R1. 헤더와 VS 카드 사이에 숏폼 가로 캐러셀 섹션 삽입. 다른 섹션 순서는 유지.
- R2. 높이 = 화면 높이의 약 1/2(최종안). 메인 카드가 가로의 약 70%, 오른쪽에 다음 카드 일부(≈1.5장 노출).
- R3. 세로 9:16 기준, 카드에 `cover`로 채움. 가로형 영상도 깨지지 않게 표시.
- R4. 자동 이동 4초, 마지막→첫 카드 순환, 수동 스와이프 후 6초간 자동 이동 정지, 로비 활성 상태에서만 동작, 백그라운드·페이지 이탈 시 타이머 정리.
- R5. 화면에 보이는 카드 1개만 음소거 자동재생, 나머지 정지. 포스터(썸네일) 먼저 표시, 영상은 지연 로딩. 자동재생 차단 시 포스터+재생 버튼.
- R6. 카드 하단에 게시자 원형 프로필 + 닉네임(한 클릭 영역). 터치 시 해당 StarPage로 이동. 영상 터치는 별도(전체 화면/상세). 프로필 없으면 기본 이미지, 긴 이름 말줄임.
- R7. 섹션 상단에 카테고리 제목은 넣지 않음(클라이언트 메모). 시안에는 "Latest Contents · LIVE"가 있어 확인 필요(Q1).
- R8. 콘텐츠 0개·영상 실패·프로필 없음·게시 기간 종료 시에도 로비 정상 동작.

### 어드민 편성
- R9. 어드민 메뉴 `Latest Contents` 추가(기존 로그인·권한 재사용).
- R10. 기존 게시물 검색·선택이 기본(검색 조건: 게시자 닉네임, StarPage, 등록일, 게시 상태). 선택 시 영상·썸네일·게시자·프로필·StarPage 자동 연결.
- R11. 노출 순서 변경(드래그 + 숫자 입력, 중복 자동 정리).
- R12. 게시/숨김 상태, 노출 시작·종료 일시(예약·자동 만료).
- R13. 미리보기, 수정, 삭제.
- R14. 노출 개수(기본 10)와 자동 이동 간격(기본 4초, 3~10초) 운영 설정.
- R15. Featured 표시(추천 여부와 순서는 별도 필드).
- R16. 어드민 직접 업로드·외부 URL 등록은 보조 기능(→ 충돌 C3).
- R17. 저장 즉시 로비 반영(캐시 무효화), 로비는 스와이프마다 서버 호출하지 않음.

### 통계·검증
- R18. 노출·재생 시작·시청 완료율·콘텐츠 클릭·프로필 클릭 집계 + 운영자 변경 이력.
- R19. 게시자 권한·저작권·공개 상태 검증.

### 동영상 용량
- R20. 동영상 게재 용량 상한을 현재의 3배로(5MB → 15MB) + 안내문도 함께 수정.

## 2. 구현 방안

### core-frontend
- 새 컴포넌트 `pages/lobby/components/latest-contents-carousel/` (vs-carousel과 같은 위치·패턴).
  - 레이아웃: 가로 스크롤 트랙 + `scroll-snap`(카드 폭 70%, 오른쪽 다음 카드 일부). 높이는 `ion-content` 가용 높이의 50%(`--lobby-content-height` CSS 변수 또는 `50vh - 헤더`로 계산).
  - 자동 이동: `setInterval(4s)` + 수동 터치 후 6초 보류(vs-carousel의 touchstart/touchend 로직 재사용). 간격 값은 API 응답에 포함.
  - 재생 제어: `IntersectionObserver`(threshold 0.5, root=트랙)로 활성 카드만 `play()`, 나머지 `pause()`. 활성 카드와 양옆 1장만 `<video src>` 바인딩, 나머지는 `poster`만(지연 로딩). `play()` 거부 시 재생 버튼 오버레이.
  - 클릭: 영상 → `/feed-detail/:conId`(기존 상세), 프로필 → `goToStarPage(PRS_ID)`(기존 함수). `stopPropagation`으로 분리.
  - 생명주기: `lobby.page.ts`의 `ionViewDidEnter`(로드+타이머 시작) / `ionViewWillLeave` / `appStateChange`에 VS·티커와 나란히 묶음. 폴링은 안 함(진입 시 1회 + 로비 복귀 시 재조회).
- `lobby.page.html`: `<app-vs-carousel>` 바로 위에 삽입. 0개면 섹션 자체 숨김.
- `lobby.service`(또는 기존 http 호출부)에 `GET /api/super/lobby/latest-contents` 추가.
- 통계: `POST /api/super/lobby/latest-contents/event` (IMPRESSION/PLAY/CLICK/PROFILE_CLICK, 카드당 세션 1회 dedupe) — 범위 확정 시(Q5).
- 용량: `admin-write-modal.component.ts`의 `maxSizeInMB = 5 → 15`, 안내문 `Video size must be less than 15MB`. 선택 화면 안내 문구가 있으면 같이.
- 테스트: 캐러셀 인덱스·자동 이동·보류 로직은 순수 함수로 분리해 spec 작성(lobby-ad-slots 방식). `npx tsc --noEmit`.

### core-backend
- DDL `db/latest-contents.sql`(운영 DB 수동 적용):
  - `WH_LATEST_CONTENT`: LC_ID PK, CON_ID(WH_STAR_CONTENT 참조), DISPLAY_ORDER, IS_FEATURED('Y/N'), VISIBLE_YN, START_AT, END_AT, TITLE_OVERRIDE(선택), CREATED_BY/UPDATED_BY(PRS_ID), CREATED_DATE/UPDATED_DATE. 원본 복제 없이 참조만.
  - 설정(노출 개수·자동 이동 간격)은 기존 시스템 패널 저장 방식을 확인해 같은 곳에 키 2개 추가. 없으면 `WH_LATEST_CONTENT_SETTING` 1행.
  - 통계 확정 시 `WH_LATEST_CONTENT_EVENT`(LC_ID, EVENT_TYPE, DEVICE_ID, CREATED_DATE) + 운영 로그 `WH_LATEST_CONTENT_LOG`.
- 어드민(`SuperAdminController` → `SuperAdminService` → `super.xml`): `/super/latest-contents/list.do`, `search.do`(승인된 영상 글만, 닉네임·StarPage·기간 필터, `/super/vs/starSearch.do` 패턴), `insert.do`, `update.do`(순서·기간·featured·숨김), `order.do`, `delete.do`, `setting.do`. JSP `super/latest_contents.jsp`(vs_list.jsp 드래그 행·숫자 입력·재번호 스크립트 재사용 + 검색 모달 + 썸네일·영상 미리보기), `sidebar.jsp` 메뉴. SM 권한.
- 앱 API(`SuperAppController` → `SuperAppService` → `superapp.xml`): `GET /api/super/lobby/latest-contents` — `WH_LATEST_CONTENT × WH_STAR_CONTENT(MDR_STATUS='APPROVED') × STAR_CONTENT_MEDIA(VIDEO 첫 장) × WH_PRESS`, `VISIBLE_YN='Y' AND (START_AT IS NULL OR START_AT<=NOW()) AND (END_AT IS NULL OR END_AT>NOW())`, `ORDER BY DISPLAY_ORDER LIMIT 설정값`. 응답에 `intervalSec` 포함. `@Cacheable("latestContents")` TTL 60초, 어드민 모든 변경 메서드에 `@CacheEvict`(live-news 패턴). 캐시 TTL 등록은 Spring XML 캐시 설정에 한 줄 추가.
- 테스트: 서비스 단위 테스트(기간 필터·LIMIT), `./mvnw.sh test`.

## 3. 충돌·실현 불가 항목과 대안

- C1. **PC 웹·모바일 웹 검수 항목**: 로비는 앱(Ionic) 전용 화면. 공개 웹사이트는 JSP라 별도 작업 → 이번 범위에서 제외 제안.
- C2. **6단계 상태(Draft→Pending Review→…)·별도 승인자**: 어드민은 SM 단일 권한이고 게시물 검수(MDR_STATUS)가 이미 있음 → 상태는 `게시/숨김` + 시작·종료 시각으로 축소(예약·만료는 시각으로 자동 판정, 목록에는 "예약/게시 중/만료" 표시만). 저작권·권한 "검증"은 자동화 불가 → 운영자 확인 체크박스 수준.
- C3. **어드민 직접 업로드·외부 URL**: 카드는 `<video>` 직접 재생이라 유튜브 등 외부 URL은 자동재생 불가 → 제외. 어드민 글(`WH_ADMIN_CONTENT`)은 게시자 StarPage가 없어 프로필 클릭 목적지가 없음 → 직접 업로드가 꼭 필요하면 해당 스타 계정으로 글을 올린 뒤 선택하는 방식으로 통일 제안.
- C4. **검수 미승인 영상**: 2-26/2-32 정책상 승인 전 URL 비노출 → 어드민 검색·로비 API 모두 `APPROVED`만. 편성 후 차단되면 자동 제외.
- C5. **통계 범위**: 완료율은 세션별 진행률 추적이 필요해 범위가 큼 → 1차는 노출·재생·클릭·프로필클릭 4종 카운트만, 완료율 제외 제안. 또는 통계 전체를 2차로.
- C6. **가로형 영상**: 요청서가 "중앙 크롭 또는 원본 비율"을 허용 → `cover` 중앙 크롭 단일 처리.
- C7. **1/2 높이의 부작용**: 첫 화면에서 VS·티커·My Ranking이 아래로 밀림(클라이언트도 인지). 헤더 포함 실제 가용 높이 기준 50%로 잡고 실기기에서 조정.
- C8. **PC 마우스·트랙패드 조작, prefers-reduced-motion**: 앱 전용이라 불필요. 터치 스와이프만.
- C9. **Swiper.js**: 이미 의존성에 있으나 로비 VS 캐러셀은 자체 구현. 일관성·Ionic 8 호환 리스크 때문에 자체 구현 유지(의존성 추가 없음).
- C10. **용량 3배의 서버 영향**: base64 JSON 15MB → 본문 약 20MB. JVM·Cloudflare(100MB) 모두 여유. 다만 서버가 업로드 응답 전에 ffmpeg 변환·썸네일 생성을 동기로 하므로 큰 파일은 응답 지연이 3배로 늘어남(이미 존재하는 구조, 이번엔 수치만 변경).

## 4. 승인 필요 항목

- A1. DDL: `WH_LATEST_CONTENT`(필수) + 설정·통계 테이블(범위 확정 시). 운영 DB 수동 적용, 배포 전 선실행.
- A2. 새 API 엔드포인트 추가(기존 삭제 없음): `/api/super/lobby/latest-contents`(+ `/event`), `/super/latest-contents/*`.
- A3. 어드민 `sidebar.jsp` 메뉴 추가, Spring 캐시 설정 XML에 캐시 이름·TTL 추가.
- A4. 업로드 상한 5MB → 15MB(앱 안내문 포함). DM 30MB는 그대로.
- A5. 의존성 추가 없음. `globals.properties`·`.env` 변경 없음.

## 5. 미결 질문 (클라이언트 회신용)

- Q1. 섹션 제목: 메모는 "카테고리 제목 불필요", 시안은 "Latest Contents · LIVE". 제목 없이 영상만 둘지, 시안대로 둘지.
- Q2. 자동 이동 4초 고정인지, 영상이 4초보다 길면 끝까지 재생 후 넘길지(기본: 4초 고정).
- Q3. 영상 터치 → 기존 게시물 상세(댓글 포함)로 이동하면 되는지, 전용 전체 화면 플레이어가 필요한지(기본: 기존 상세).
- Q4. 어드민 직접 업로드가 꼭 필요한지, 필요하다면 게시자(StarPage)를 어떻게 지정할지(C3).
- Q5. 통계 범위: 4종 카운트만 1차로 할지, 전부 2차로 미룰지(C5).
- Q6. 노출 개수·자동 이동 간격을 어드민 설정으로 둘지, 10개·4초 고정으로 시작할지.
- Q7. 숏폼 후보는 영상 게시물만인지(사진 게시물 제외).
- Q8. 용량 3배 대상 확인: 스타 글쓰기 5MB → 15MB가 맞는지. (실측 상한은 5MB, 140MB는 미사용 코드)
- Q9. 웹(PC·모바일 웹) 로비는 범위 밖으로 봐도 되는지(C1).
- Q10. 시작·종료 일시의 기준 시간대(서버 KST 고정으로 가정).

## 6. 구현 메모 (2026-10-12, 브랜치 `feature/2-33-shortform-latest-contents`)

사용자 확정: 섹션 제목 표기 / 영상 터치는 기존 상세 / 어드민 직접 업로드 포함(게시자 스타 지정 필수) / 통계 4종 어드민 표시.

### 배포 전 필수
- `core-backend/db/latest-contents.sql` 운영 DB **선실행** (테이블 4개 + 설정 1행). 미실행 시 앱은 섹션만 숨기고, 어드민 메뉴는 500.
- Redis 캐시 `latestContents`(60초) — `context-redis.xml`에 추가됨. 어드민 변경 시 즉시 evict.

### 변경 파일
- 백엔드: `VideoTranscodeUtil`(썸네일 추출 공용화), `SuperAppService/Controller`(`/api/super/lobby/latest-contents`, `/event`), `SuperAdminService/Controller`(`/super/latest-contents/*` 9개), `super.xml`·`superapp.xml`, `latest_contents.jsp`, `sidebar.jsp`, 테스트 3개.
- 프런트: `latest-contents-logic.ts`(+spec 16건), `components/latest-contents-carousel/`, `lobby.page.{ts,html}`·`lobby.module.ts` 연결, `admin-write-modal` 5MB→15MB + 힌트.

### 검증한 것
- 백엔드 `./mvnw.sh test` 233건 통과, 프런트 `tsc` 통과, Karma 로비 spec 79건 통과.
- 로컬 `ionic serve` + 내장 브라우저에서 API 응답을 모의로 넣어 확인: 섹션 제목·LIVE 뱃지·`n / N`, 높이 = (콘텐츠 − 헤더)/2, 1.5장 노출, 4초 자동 이동·순환, 수동 스와이프 후 6초 보류 뒤 재개, 활성 ±1장만 src 바인딩, 활성 1개만 재생, 노출·재생·클릭·프로필 클릭 이벤트 전송, 프로필 → `/star/:id`, 영상 → `/feed-detail/:conId`.

### 검증하지 못한 것 (실기기·운영 DB 필요)
- 어드민 화면 렌더·직접 업로드(ffmpeg 변환·공개 디렉터리 이동): 로컬 톰캣은 운영 DB에 붙고 DDL 미적용이라 실행 불가. 배포 후 `/super/latest-contents/list.do`에서 설정 저장·게시물 선택(중복 거부)·업로드·드래그 순서·토글·기간·삭제 확인.
- iOS/Android 실기기: 음소거 자동재생(저전력 모드 → 재생 버튼), 가로 스와이프와 세로 스크롤 충돌, 화면 이탈·백그라운드 시 정지(내장 브라우저는 Ionic 생명주기 이벤트가 안 떠서 확인 불가).
- 글쓰기 모달 15MB 힌트 표시(스타 로그인 필요).

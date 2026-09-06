# 2-29차 검토 ① — 로비 LIVE 티커 업그레이드 + Global Admin LIVE NEWS 관리

2026-09-06 · 클라이언트 표기 "2-27차 최종안" 중 티커·어드민 부분. 코드 작성 전 검토 산출물.

## 요약

티커는 2-28차에 이미 납품된 `LiveNewsTickerComponent`(marquee, VS 카드 3초 폴링 데이터)를 **개편**하는 작업이다. 신규는 어드민 LIVE NEWS 관리 화면과 그 문구를 앱에 내려주는 API 하나이며, 이 둘은 **새 테이블 1개**가 필요하다. 기존 Interrupt(NEW KING·LEAD CHANGE 즉시 표출)는 그대로 두고 추가 개발만 생략한다(사용자 결정).

---

## 1. 요구사항 목록

| # | 요구사항 | 출처 |
|---|---|---|
| T1 | Normal Loop 3항목을 **순차 순환**: ① 어드민 문구 → ② 카테고리별 #1 현황 → ③ 현재 VS 카드 상태 요약 | 최종안 |
| T2 | 데이터 없는 카테고리는 건너뛴다 | 최종안 |
| T3 | 문장과 문장 사이 간격을 현재의 **절반**으로 | 최종안 |
| T4 | 티커 터치 시 항목별 랜딩: 스타 항목 → 스타 페이지, VS 항목 → VS 카드 포커스, 어드민 문구 → 지정 타겟 | 최종안 |
| T5 | Interrupt(순위 역전 속보) 추가 개발은 생략. 기존 구현은 유지 | 최종안 + 사용자 결정 |
| A1 | Global Admin에 **LIVE NEWS 관리** 메뉴 신설 | 최종안 |
| A2 | 입력 필드: 문구(최대 30자, 글자수 카운터) · 랜딩 타겟(None / 스타 ID / VS Card / 외부 URL) · 노출 순서 · ON/OFF · 저장 | 최종안 |
| X1 | Today's TOP 클릭 순서 로직, 랭킹·VS·Daily King 계산 로직, 기존 API는 건드리지 않는다 | 최종안 (강조) |

## 2. 현 아키텍처 기준 구현 방안

### 2-1. core-backend

**신규 테이블 `WH_LIVE_NEWS`** (DDL은 `core-backend/db/live-news.sql`로 제공, 운영 DB 수동 적용 — 승인 항목)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| NEWS_ID | INT AUTO_INCREMENT PK | |
| MESSAGE | VARCHAR(120) | 앱 노출 문구. 길이 상한은 서비스 상수로 강제 |
| TARGET_TYPE | VARCHAR(10) | NONE · STAR · VS · URL |
| TARGET_VALUE | VARCHAR(500) | STAR=PRS_ID, VS=VS_ID, URL=외부 주소, NONE=NULL |
| SORT_ORDER | INT DEFAULT 0 | 오름차순 노출 |
| USE_YN | CHAR(1) DEFAULT 'Y' | ON/OFF 토글 |
| DEL_YN | CHAR(1) DEFAULT 'N' | 소프트 삭제 |
| CREATED_DATE / UPDATED_DATE | DATETIME | |

**어드민 화면** — VS 배틀필드 관리 화면과 같은 패턴을 그대로 따른다.
- 컨트롤러 `SuperAdminController`: `/super/live-news/list.do`(목록), `insert.do` · `update.do` · `toggle.do` · `order.do` · `delete.do`(POST, `@ResponseBody` Map). SM 권한 체크는 `/super/vs/list.do`와 동일.
- 서비스 `SuperAdminService`: `LIVE_NEWS_MAX_LENGTH = 30` 상수, 저장 시 서버에서도 길이·타겟 유효성 검증(STAR면 `WH_PRESS` 존재 확인, URL이면 http(s)만 허용).
- 매퍼 `super.xml`: `selectLiveNewsAdminList` / `insertLiveNews` / `updateLiveNews` / `updateLiveNewsUseYn` / `updateLiveNewsOrder` / `deleteLiveNews`.
- JSP `super/live_news.jsp`: `vs_list.jsp` 구조 복제. 문구 입력란 아래 글자수 카운터, 타겟 선택 셀렉트(STAR 선택 시 기존 `/super/vs/starSearch.do` 재사용해 스타 검색).
- `sidebar.jsp`에 메뉴 항목 추가(`activeMenu eq 'live_news'`).

**앱 API** — `SuperAppController`에 `GET /api/super/lobby/live-news` 추가. `superapp.xml`에 `selectLiveNewsActiveList`(USE_YN='Y' AND DEL_YN='N', SORT_ORDER ASC). 응답은 `[{newsId, message, targetType, targetValue}]`. 기존 `vsCards`처럼 Redis 캐시(TTL 30초 정도)로 감싼다. 카테고리별 #1 데이터는 **새 API 없이** 기존 `/api/super/lobby/vs-cards` 응답을 그대로 쓴다.

### 2-2. core-frontend

**`LiveNewsTickerComponent` 개편**
- 입력 추가: `@Input() news: LiveNewsItem[]` (로비가 `live-news` API로 채움).
- 문장 모델을 문자열에서 `{ text, target }` 객체로 바꾼다. `target`은 `{kind: 'STAR'|'VS'|'URL'|'NONE', starId?, vsId?, url?}`.
- `buildDefaults()`를 3구간 순차 큐로 재구성:
  1. 어드민 문구(ON, SORT_ORDER 순) — 타겟은 저장값.
  2. 카테고리별 #1 — VS 카드 중 `type === 'GLOBAL'`인 AUTO 카드의 1위 측을 한 줄씩. `🌎 GLOBAL #1 · {name}`, `⭐ STAR #1 · …`, `👤 CELEB`, `🏢 BRAND`, `🏛️ ORG`, `🎓 UNIV`, `🌆 CITY`, `📺 MEDIA`. Daily King 카드는 `👑 DAILY KING #1 · {name}` 한 줄. 카드가 없거나 `left`가 비면 건너뜀(T2). 타겟은 해당 스타.
  3. VS 상태 요약 — 현재 `⚔️ … · GAP n` 문구 유지. 타겟은 해당 VS 카드.
- 순환 인덱스는 구간을 넘어 이어지며, 어드민 문구가 0건이면 ②부터 시작한다.
- 기존 `detectEvents` 인터럽트 경로와 `pendingEvents` 처리는 손대지 않는다(T5).

**간격 절반(T3)** — 현재는 문장이 화면 밖으로 완전히 빠진 뒤 다음 문장이 오른쪽 끝에서 시작하므로 빈 구간이 화면 폭 하나만큼 생긴다. `marquee-text` span을 2개 두고, 앞 문장의 꼬리가 화면 오른쪽에서 **화면 폭의 절반** 지점을 지날 때 다음 문장을 출발시킨다. 속도(`SPEED_PX_PER_SEC`)와 최소 체류(`MIN_PASS_MS`)는 유지. 인터럽트 문장은 지금처럼 진행 중 문장을 끊는다.

**터치 랜딩(T4)** — `focusRequest` 이벤트를 `EventEmitter<TickerTarget>`으로 바꾼다. 로비 `onTickerFocus(target)`:
- STAR → Today's TOP 카드가 쓰는 것과 같은 스타 페이지 이동 경로 재사용.
- VS → `app-vs-carousel`로 스크롤 후 해당 `vsId` 카드로 이동(캐러셀에 `jumpTo(vsId)` 공개 메서드 추가).
- URL → `HelperService.openURL` (인앱 브라우저, 이미 사용 중).
- NONE → 지금처럼 캐러셀 스크롤만.

**로비 페이지** — `ionViewDidEnter`에서 `live-news` 1회 조회 + 60초 주기 갱신(3초 VS 폴링에 얹지 않는다). `ionViewWillLeave`·AppState 정지 시 타이머 해제(기존 패턴).

**테스트** — `live-news-ticker.component.spec.ts`에 큐 생성 순수 함수(`buildQueue`) 스펙 추가: 3구간 순서, 빈 카테고리 스킵, 어드민 0건, 타겟 매핑. 기존 `detectEvents` 스펙 8건 유지.

## 3. 충돌 항목과 대안

| 문서 주장 | 실제 | 대안 |
|---|---|---|
| "16개 카테고리" | 카테고리는 7개(STAR·CELEB·BRAND·ORG·UNIV·CITY·MEDIA) + GENERAL. 로비 VS 카드도 이 7개 기준 | 7개 + GLOBAL + DAILY KING으로 ② 구간을 구성. 카테고리 추가는 별도 요청으로 |
| "새 DB 구조 건드리지 않음" | 공지·배너 성격의 테이블이 없다 | `WH_LIVE_NEWS` 1개 신규. 기존 테이블은 변경 없음 |
| "16개 분야 #1을 로비 진입 시 1회 캐싱" | 이미 3초 폴링 중인 VS 카드 응답에 카테고리별 #1·#2가 들어 있다 | 별도 API·캐싱 없이 그 데이터를 재사용 |
| 뒷부분 AI 문서의 Slide-Up + Fade, 3~4초 체류 | 2-28에서 클라이언트가 marquee 확정 | marquee 유지, 간격만 절반 |
| Interrupt 우선순위 9단계 | 앞부분에서 "Interrupt 생략" 명시 | 기존 3단계(역전·추격·점수변화) 유지, 확장 안 함 |
| "한글 기준 30자" | 노출 텍스트는 영어 규칙 | 30자 = 문자 수 기준으로 통일. 영어 문장 기준 너무 짧으면 상한 상향 제안(미결 Q1) |
| 로비 순서 다이어그램 | 현재 순서(VS → 티커 → Today's TOP → AD → 랭킹)와 동일 | 변경 없음 |

## 4. 승인 필요 항목

1. **DDL**: `WH_LIVE_NEWS` 테이블 생성. 애플리케이션 배포 **전** 운영 DB에 수동 적용(2-21·2-27과 같은 절차).
2. **Redis 캐시 키 추가**: `live-news` 30초 TTL. 설정 파일(`context-redis.xml`) 변경 없이 기존 캐시 매니저에 이름만 추가 가능 — 기존 캐시 TTL 표와 충돌 여부 구현 시 재확인.
3. 새 의존성 없음. `package.json` · `globals.properties` 변경 없음.

## 5. 미결 질문 (클라이언트)

1. 문구 30자 제한은 **문자 수** 기준으로 통일해도 되는가? 영어 문장이면 30자는 5~6단어라 상한을 50자 정도로 올리길 권한다.
2. 카테고리 #1 구간에 **Daily King #1**과 **Global #1**도 포함하는가? (앞부분 최종안에는 없고 뒷부분 문서에는 있음. 포함하는 쪽으로 가정)
3. 어드민 문구가 여러 건 ON이면 **모두 순서대로** 한 바퀴에 내보내는가, 아니면 바퀴마다 1건씩 돌아가며 내보내는가? (모두 순서대로로 가정)
4. 외부 URL 타겟은 인앱 브라우저로 여는 것으로 충분한가?
5. 티커 터치 시 스타 페이지로 이동하면 Today's TOP 클릭 카운트와 **별개**로 두는 것이 맞는가? (별개로 가정. Today's TOP 로직은 건드리지 않음)

## 6. 작업 규모 감

백엔드(테이블·어드민 CRUD·API) 1.5일, 프런트(큐 재구성·간격·타겟 랜딩·캐러셀 점프·스펙) 1.5일, 검증 0.5일.

---

## 7. 구현 메모 (2026-09-06 구현 완료)

미결 질문 5건은 2026-09-06 사용자 답변으로 확정했고 구현에 반영했다.

| 질문 | 확정 | 바꾸는 곳 |
|---|---|---|
| Q1 30자 단위 | **50자**로 확정. 문자 수(코드포인트) 기준, 이모지 1개 = 1자 | `SuperAdminService.LIVE_NEWS_MAX_LENGTH` (JSP 카운터는 이 값을 받아 씀) |
| Q2 Daily King·Global #1 포함 | 포함. `🌎 GLOBAL #1`, `👑 DAILY KING #1` 각 한 줄 | `LiveNewsTickerComponent.buildCategoryLeaderItems` |
| Q3 어드민 문구 여러 건 | 한 바퀴에 모두 순서대로 | `buildQueue` |
| Q4 외부 URL | **기기 기본 브라우저**(크롬·사파리)로 앱 밖에서 연다. `HelperService.openExternalURL`(InAppBrowser `_system`) | `LobbyPage.onTickerFocus` |
| Q5 Today's TOP 카운트 | 별개. 스타 페이지 라우팅만 | — |

변경 파일
- 백엔드: `db/live-news.sql`(DDL, **배포 전 수동 적용**), `super.xml`·`superapp.xml` 매퍼, `SuperAdminService`(검증·CRUD·`@CacheEvict`), `SuperAdminController`(`/super/live-news/*`), `super/live_news.jsp`, `sidebar.jsp`, `SuperAppService.getLiveNews`(`liveNews` 캐시 30초), `SuperAppController`(`GET /api/super/lobby/live-news`), `context-redis.xml`(TTL 항목).
- 프런트: `live-news-ticker.component.{ts,html,spec.ts}`(Normal Loop 3구간·슬롯 2개 교차 진입·타겟), `vs-carousel.component.ts`(`jumpTo`), `lobby.page.{ts,html}`(문구 60초 갱신·타겟 랜딩).

검증
- `./mvnw.sh test` 110건 통과(신규 `SuperAdminServiceLiveNewsTest` 10건 포함, 50자 기준), `xmllint` 매퍼 2종 정상.
- `npx tsc --noEmit` src 오류 0, 티커 스펙 16건 통과(기존 8 + 신규 8), `npx ng build`(JIT) 성공.
- 실기기 확인 항목: 문장 교차 진입 간격, 어드민 ON/OFF 즉시 반영(캐시 evict), 티커 탭 랜딩 3종.

# 2-29차 검토 ② — 1:1 메신저 이식 (레거시 witch 채팅 → 스타플랫폼)

2026-09-06 · 클라이언트 표기 "2-27차 최종안" 1번 항목. 코드 작성 전 검토 산출물. 같은 날 확정 답변 반영.

## 요약

클라이언트가 말하는 "지구로또 메신저"는 이 저장소 안의 레거시 `pages/list/witch` 채팅이다. 텍스트·사진·영상 전송, 영상 썸네일, 읽음 표시, FCM 푸시, 자동폭파까지 실제 코드가 있어 **UI와 흐름은 재사용**할 수 있다. 그러나 이 코드는 운영 DB에 없는 `WH_MEMBER` 계정 모델 위에 있고, 자동폭파는 서버 메모리 예약이라 재시작하면 사라지며, 파일 경로가 서버 절대경로로 박혀 있다. 따라서 **"그대로 붙이기"는 불가능**하고, 화면·흐름은 옮기되 백엔드는 현 계정 모델(`WH_PRESS`) 기준으로 다시 쓴다. 폭파 시간과 뱃지·푸시 동작은 레거시와 동일하게 맞춘다.

---

## 0. 확정 사항 (2026-09-06)

| 항목 | 확정 |
|---|---|
| 대화 형태 | **1:1 전용**, 그룹 없음 |
| 참여자 | **스타 페이지 소유자끼리만**. 비로그인 방문자(게스트)는 보낼 수 없음 |
| 자동폭파 | 레거시 그대로 **발송 5분 후 삭제, 읽음 처리 시 미읽음이던 메시지 1분 후 삭제**. 사진·영상 파일·썸네일도 같은 시점에 디스크에서 삭제 |
| 진입점 | **타인 프로필 페이지 헤더 영역의 채팅 아이콘**. 내 페이지에는 없음. 대화 목록은 **로비 헤더 💬 아이콘**(🔍 💬 ⊕ ☆ 🏆 👤 순, 미읽음 점 포함). 수신자는 푸시 탭으로 대화 직접 진입도 가능 |
| 로비 헤더 | 스타 소유자는 프로필 사진, 스타가 아닌 사용자는 현행대로 아이콘 없음 |
| 앱 아이콘 뱃지 | **점 표기**(숫자 아님). 앱을 열면 사라짐 |
| 푸시 문구 | 발신자·내용 없이 **"You have a new message."** 만 |
| 첨부 용량 | 아래 3-4 계산값 적용 |

## 1. 요구사항 목록

| # | 요구사항 |
|---|---|
| M1 | 스타 소유자 간 1:1 메신저: 텍스트·사진·영상 전송, 읽음 처리 |
| M2 | 로그인한 스타가 **다른** 스타 페이지에 들어갔을 때 프로필 헤더 영역에 채팅 아이콘. Global Ranking pill(빨간 테두리)은 폭 절반 |
| M3 | 로비 헤더 우상단 👤를 본인 프로필 사진으로 교체 (스타 소유자만) |
| M4 | 메시지 수신 시 앱 아이콘에 점. 앱 열면 제거. 방문자 수 푸시는 기존 유지 |
| M5 | 자동폭파(발송 5분 / 읽음 1분) + 파일 동시 삭제 |
| M6 | 푸시 본문은 도착 사실만 |

## 2. 레거시 코드 실측

| 계층 | 위치 | 재사용 | 비고 |
|---|---|---|---|
| 채팅 화면 | `core-frontend/src/app/pages/list/witch/chat-write/` | **가능** | 말풍선·첨부·미리보기·영상 포스터·이미지 뷰어. 라벨이 한국어라 영어로 교체 |
| 미읽음 감지 | `CheckMessageService` + `app.component.ts`의 `/api/newMessage` 호출 | 가능 | 트리거 방식 유지, 식별자 교체 |
| API | `ApiController` `/api/newMessage` `/api/messageList` `/api/addMessage` `/api/readMessage` `/api/uploadChatFile` | 참고만 | `member.xml`이 `WH_MEMBER` 조인. 운영 DB에 없는 테이블 |
| 자동폭파 | `MemberService.addMessage/readMessage` — `ScheduledExecutorService`로 발송 5분 후, 읽음 1분 후 소프트삭제 | 시간만 | 서버 재시작 시 예약 소실. 파일은 디스크에 잔존 |
| 파일 저장 | `MemberService.uploadFile` — `/home/ubuntu/uploads` 하드코딩, JCodec 썸네일 | 로직만 | 경로는 설정으로. `/chatfiles/` 정적 매핑이 서블릿 설정에 없음 |
| 푸시 | `MemberService.sendNotificationToReceiver` → `FirebaseService.sendPersonalNotification` (APNs badge=1 고정) | **가능** | 점 표기 확정이라 badge=1 그대로. 토큰만 `WH_PRESS.FCM_TOKEN`으로 |
| 뱃지 | `@capawesome/capacitor-badge`, 앱 복귀 시 `Badge.clear()` | **그대로** | "앱 열면 없앰"이 이미 구현된 동작 |

의존성: 프런트 `@capawesome/capacitor-file-picker`, `@capawesome/capacitor-badge`, `VideoService`, 백엔드 JCodec 모두 이미 있음. **새 의존성 없음.**

## 3. 현 아키텍처 기준 구현 방안

### 3-1. 참여자 모델

메신저 계정은 `WH_PRESS`(스타 페이지 소유자)로 한정한다. 게스트는 기기 ID뿐이고 푸시 토큰이 없다. 채팅 아이콘은 `isStar && starId !== 보고 있는 페이지 소유자`일 때만 렌더하고, API도 발신·수신 양쪽이 유효한 PRS_ID인지 검증한다.

### 3-2. core-backend

**신규 테이블 `WH_DM_MESSAGE`** (DDL `core-backend/db/dm-message.sql`, 수동 적용)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| MSG_ID | BIGINT AUTO_INCREMENT PK | |
| SEND_PRS_ID / RECV_PRS_ID | VARCHAR(50) | `WH_PRESS.PRS_ID` |
| CONTENT_TYPE | VARCHAR(10) | TEXT · IMAGE · VIDEO |
| CONTENT | VARCHAR(2000) | 텍스트 본문 또는 저장 파일명 |
| THUMB_NM | VARCHAR(255) | 영상 썸네일 파일명 |
| SEND_DATE | DATETIME | |
| READ_DATE | DATETIME NULL | |
| EXPIRE_AT | DATETIME | 발송 시 +5분. 읽음 처리 시 그 시점 미읽음이던 메시지는 min(기존, +1분) |

인덱스: (RECV_PRS_ID, READ_DATE), (EXPIRE_AT). 만료 행은 파일 삭제 후 **물리 삭제**(소프트삭제 컬럼 불필요).

**API** — `SuperAppController`에 `/api/super/dm/*` (기존 `/api/super/message/*`는 스타↔어드민 문의 스레드라 이름 충돌 회피). 인증은 기존 스타 토큰.
- `POST /api/super/dm/send` — 텍스트. 저장 후 상대 `FCM_TOKEN`으로 푸시(PUSH_YN 존중).
- `POST /api/super/dm/upload` — 사진·영상(base64 JSON, 레거시 방식). 서버에서 용량·MIME·시그니처 검사(피드 업로드의 `TOO_LARGE` 거부 패턴 재사용). 영상은 JCodec 썸네일 생성.
- `GET /api/super/dm/rooms` — 대화 상대 목록(최근 메시지·미읽음 수). 수신자가 발신자를 찾는 용도.
- `GET /api/super/dm/messages?peerId=` — 대화 내용(만료 제외).
- `POST /api/super/dm/read` — 읽음 처리 + EXPIRE_AT 단축.
- `GET /api/super/dm/unread-count` — 앱 **안** 점 표시용(로비 아바타). 앱 아이콘 뱃지에는 쓰지 않음.
- 매퍼는 `superapp.xml`에만 추가.

**자동폭파** — `EXPIRE_AT` + `@Scheduled(cron = "0 * * * * *")` 스케줄러(`admin/scheduler/` 패턴). 만료 행의 파일·썸네일을 지운 뒤 행을 삭제. 시간값은 서비스 상수 고정(발송 300초, 읽음 60초). 스프링 기본 스케줄러가 단일 스레드라 기존 `PageGeneratorScheduler`(10분)와 겹칠 때 최대 수 초 지연될 수 있음 — 허용 범위.

**파일 서빙** — 정적 매핑 대신 `GET /api/super/dm/file/{msgId}` 로 내려주고 대화 당사자 토큰만 통과시킨다. URL만 알면 누구나 보는 구조를 막는다.

**푸시** — 레거시 `sendPersonalNotification` 재사용. 제목 없음, 본문 `You have a new message.` 고정. APNs badge=1(점) 유지, Android는 채널 `dm_channel` 추가(알림이 남아 있으면 런처가 점을 표시). 데이터 페이로드에 `type=DM`, `peerId`를 실어 탭 시 해당 대화로 진입.

### 3-3. core-frontend

- **채팅 화면**: `chat-write-modal`을 `modals/dm-chat/`로 이동, API·식별자(`PRS_ID`)·라벨(영어) 교체. 이미지 뷰어 동반 이동. 영상 선택은 `FilePicker` 대신 이미 길이 검사를 갖춘 `VideoService` 재사용.
- **채팅 아이콘(M2)**: `star-page.page.html` 프로필 헤더의 `.info-rank-line` 행 오른쪽 끝. 헤더 높이는 네이티브 광고 dp 하드코딩 때문에 늘릴 수 없으므로 한 줄 안에 배치. 본인 페이지의 순위 pill은 폭 절반.
- **수신자 진입(확정)**: 푸시 탭 → `peerId`로 대화 직접 진입. 앱 안에서는 로비 아바타에 미읽음 점이 켜지고, 탭하면 미읽음 대화 목록(`rooms`) → 대화. 푸시에 발신자 이름이 없으므로 이 목록이 없으면 수신자가 상대를 찾을 수 없다.
- **로비 아바타(M3)**: 👤를 `<ion-avatar>`로 교체, 이미지는 로그인 시 받는 스타 프로필에서 캐시. 미읽음 점은 `unread-count`로 앱 복귀·푸시 수신·채팅 모달 닫힘 시에만 갱신(3초 VS 폴링에 얹지 않음).
- **앱 아이콘 뱃지(M4)**: 추가 코드 없음. iOS는 APNs badge=1로 점, 앱 복귀 시 기존 `Badge.clear()`가 지움. Android는 알림 잔존 시 런처 점, 앱 열 때 기존 알림 정리 로직이 지움.
- **레거시 정리**: `pages/list/witch/` 채팅 3종과 `chat.service.ts`는 이식 후 제거 후보. 기존 엔드포인트는 삭제하지 않음.

### 3-4. 첨부 용량 상한 (계산)

전제: base64 JSON 업로드라 본문은 원본의 약 1.37배, 서버는 본문 문자열과 디코드 바이트를 동시에 들어 원본의 약 3배 힙을 순간 사용. 로컬 JVM 설정 Xmx768m. 도메인은 Cloudflare 경유(요청 본문 100MB 상한). 파일은 5분 안에 지워지므로 디스크가 아니라 **전송량과 힙**이 제약. Cost Guard 검토 때 KT 트래픽 과금 여부가 미결이라 전송량은 보수적으로 잡는다.

| 종류 | 상한 | 근거 |
|---|---|---|
| 이미지 | **10MB** | 피드 업로드와 동일. 서버 `TOO_LARGE` 검사 재사용 |
| 영상 | **30MB · 60초** | 720p H.264 60초가 약 20~30MB. JSON 본문 약 41MB로 Cloudflare 여유, 힙 순간 사용 약 90MB로 동시 2~3건까지 안전. 발신+수신 전송량 최대 60MB/건 |

피드 영상의 현행 140MB 상한을 그대로 쓰지 않는 이유: 메신저는 5분 뒤 사라질 파일에 190MB JSON 본문을 태우는 셈이고, Cloudflare 본문 상한도 넘긴다. 클라이언트 측에서 선택 즉시 크기·길이를 검사해 영어 안내를 띄우고, 서버도 같은 값으로 재검사한다.

## 4. 충돌 항목과 대안

| 문서 주장 | 실제 | 대안 |
|---|---|---|
| "코드와 로직 모두 그대로" | `WH_MEMBER` 없음, 인메모리 폭파, 절대경로 | 화면·흐름·시간값 이식 + 백엔드 재작성 |
| "각 유저의 프로필 사진" | 로그인 주체는 스타 소유자뿐 | 스타 소유자만 아바타. 게스트는 현행 유지(확정) |
| "자동폭파 → 용량 절감" | 레거시는 파일 잔존 | 파일까지 삭제하는 DB 기반 스케줄러(확정) |
| Global Ranking 줄에 버튼 | 헤더 높이 고정 | 한 줄 내 우측 배치, pill 폭 절반 |
| 영상 140MB(피드) | Cloudflare 100MB 본문 상한 초과 | 메신저는 30MB·60초 |

## 5. 승인 필요 항목

1. **DDL**: `WH_DM_MESSAGE` 생성 + 인덱스. 배포 전 수동 적용.
2. **`globals.properties`**: `dm.upload.path` 키 1개. (설정 파일 수정 금지 항목이라 사전 승인)
3. **서버 디렉터리**: 업로드 경로와 `thumbnail/` 생성, Tomcat 쓰기 권한. 2-26 이미지 검수와 같은 배포 전 작업.
4. **Android 알림 채널 추가**(앱 업데이트 필요). APNs payload는 레거시와 동일(badge=1)이라 변경 없음.
5. **레거시 witch 채팅 코드 제거** 여부(엔드포인트는 유지).
6. 새 npm·Maven 의존성 없음.

## 6. 남은 확인

없음. 수신자 진입 경로는 푸시 탭(대화 직접 진입)과 로비 헤더 💬 아이콘(대화 목록)으로 확정(2026-09-06, 아바타 점 방식에서 변경). 검토 종료, 티커 건 이후 별도 회차로 구현.

## 7. 작업 규모 감

백엔드(테이블·API 7종·업로드·파일 서빙·스케줄러·푸시) 3일, 프런트(채팅 화면 이식·아이콘·아바타·대화 목록·용량 검사) 2.5일, 서버 작업·검증 1일. 티커 건과 별도 회차로 진행 권장.

---

## 8. 구현 메모 (2026-09-06 구현 완료, 미커밋)

**백엔드**
- `core-backend/db/dm-message.sql` — `WH_DM_MESSAGE` DDL. **배포 전 수동 적용.**
- `DmService`(api/service) — 본인 확인(starId+starToken, fail-closed), 텍스트/첨부 발송, 대화 목록·내용, 읽음(EXPIRE_AT를 1분 뒤로 단축), 미읽음 수, 파일 스트리밍 해석, 만료 정리. 폭파 상수 300초/60초. 첨부는 UUID 파일명, 이미지는 `ImageModerationUtil.validate`(10MB), 영상은 30MB·mp4/mov/webm/3gp, JCodec 썸네일(실패해도 발송).
- `DmExpireScheduler`(admin/scheduler) — 1분 cron, 파일·썸네일 삭제 후 행 물리 삭제.
- `FirebaseService.sendDataNotification` — 데이터 페이로드(`type=DM, peerId`)·채널 `dm_channel`·badge=1. 본문 "You have a new message." 고정, 제목 "New message".
- `SuperAppController` — `POST /api/super/dm/{send,upload,rooms,messages,read,unread-count}`, `GET /api/super/dm/file?t=`(MediaAccessService 10분 토큰, 대화 내용 응답에만 붙음).
- 업로드 경로: `globals.properties` `dm.upload.path`(없으면 `/var/lib/tomcat7/dm/`). 디렉터리와 Tomcat 쓰기 권한은 배포 전 서버 작업.
- 테스트 `DmServiceTest` 10건. `./mvnw.sh test` 120건 통과.

**프런트**
- `services/dm.service.ts` — API·미읽음 수·내 프로필 사진 상태. `modals/dm-chat/`(채팅, 3초 폴링, 사진=PhotoService·영상=VideoService, 미리보기, 전체 보기, 자동폭파 안내), `modals/dm-rooms/`(대화 목록) — standalone, 열기 헬퍼 `openDmChat`/`openDmRooms`.
- 스타 페이지: 순위 줄 맨 오른쪽 채팅 아이콘(로그인한 스타가 타인 페이지를 볼 때만). 본인 순위 pill은 `max-width: 50%`.
- 로비 헤더: 검색 옆에 💬 아이콘(대화 목록, 미읽음 점) 추가 — 순서 🔍 💬 ⊕ ☆ 🏆 👤. 👤는 내 프로필 사진(`unread-count` 응답의 myImage)으로 바뀌고 탭은 내 페이지 진입만. 미읽음은 진입·복귀·로그인 직후 갱신.
- `app.component.ts`: Android 채널 `dm_channel` 추가, 포그라운드 DM 푸시 → 점 갱신, 푸시 탭 → 해당 대화 직접 진입. 앱 아이콘 뱃지는 기존 `Badge.clear()` 그대로.
- `tsc` src 오류 0, `npx ng build` 성공, 스펙 20건 통과(티커 16 + DM 4).

**배포 전 체크리스트**
1. `dm-message.sql` 실행 (+ 티커 건 `live-news.sql`).
2. `globals.properties`에 `dm.upload.path` (선택) — 없으면 기본 경로. 디렉터리 생성 + 쓰기 권한.
3. Android 채널은 앱 업데이트에 포함됨. iOS는 기존 APNs 설정 그대로.
4. 실기기 확인: 타인 페이지 아이콘 노출·본인 페이지 미노출, 사진/영상 전송, 5분·1분 폭파와 파일 삭제, 푸시 탭 진입, 헤더 점.

**남긴 것**
- 레거시 `pages/list/witch/` 채팅과 `chat.service.ts`는 삭제하지 않음(승인 항목 5). 기존 `/api/messageList` 등 엔드포인트 유지.

# 📋 Admin Write Modal API 명세서 (v2.0)

## 1️⃣ **관리자 피드 추가** (Global Notice / Local Feed)

### Endpoint
```
POST /api/super/admin/feed/add
```

### Request Header
```json
{
  "Content-Type": "application/json"
}
```

### Request Body (이미지, 동영상, YouTube 독립적 선택 가능)

#### 📸 **이미지만 포함**
```json
{
  "adminId": "admin_001",
  "adminPw": "hashed_password",
  "adminLevel": "GLOBAL",
  "content": "공지사항 텍스트 내용\n줄바꿈도 지원됩니다",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEA...",
  "videoBase64": null,
  "youtubeUrl": null
}
```

#### 🎥 **동영상 파일만 포함** (최대 5MB)
```json
{
  "adminId": "admin_001",
  "adminPw": "hashed_password",
  "adminLevel": "GLOBAL",
  "content": "동영상이 첨부된 공지사항",
  "imageBase64": null,
  "videoBase64": "data:video/mp4;base64,AAAAIGZ0eXBpc29tAAACAGisuoN...",
  "youtubeUrl": null
}
```

#### 🎬 **YouTube URL만 포함**
```json
{
  "adminId": "admin_001",
  "adminPw": "hashed_password",
  "adminLevel": "GLOBAL",
  "content": "유튜브 영상 소개",
  "imageBase64": null,
  "videoBase64": null,
  "youtubeUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
```

#### 📸 + 🎥 **이미지 + 동영상 함께 포함**
```json
{
  "adminId": "admin_001",
  "adminPw": "hashed_password",
  "adminLevel": "GLOBAL",
  "content": "공지사항에 이미지와 동영상 모두 포함",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEA...",
  "videoBase64": "data:video/mp4;base64,AAAAIGZ0eXBpc29tAAACAGisuoN...",
  "youtubeUrl": null
}
```

#### 📸 + 🎬 **이미지 + YouTube URL 함께 포함**
```json
{
  "adminId": "admin_001",
  "adminPw": "hashed_password",
  "adminLevel": "GLOBAL",
  "content": "이미지와 유튜브 링크를 함께 공유합니다",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEA...",
  "videoBase64": null,
  "youtubeUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
```

#### 🎥 + 🎬 **동영상 + YouTube URL 함께 포함**
```json
{
  "adminId": "admin_001",
  "adminPw": "hashed_password",
  "adminLevel": "GLOBAL",
  "content": "직접 촬영한 영상과 유튜브 추천 영상",
  "imageBase64": null,
  "videoBase64": "data:video/mp4;base64,AAAAIGZ0eXBpc29tAAACAGisuoN...",
  "youtubeUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
```

#### 📸 + 🎥 + 🎬 **모두 포함**
```json
{
  "adminId": "admin_001",
  "adminPw": "hashed_password",
  "adminLevel": "GLOBAL",
  "content": "이미지, 동영상, 유튜브 링크 모두 포함한 공지사항",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEA...",
  "videoBase64": "data:video/mp4;base64,AAAAIGZ0eXBpc29tAAACAGisuoN...",
  "youtubeUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
```

### Request Parameters 설명

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `adminId` | string | ✅ | 관리자 ID |
| `adminPw` | string | ✅ | 관리자 비밀번호 (암호화된 형식) |
| `adminLevel` | string | ✅ | `GLOBAL` (전체 공지) \| `LOCAL` (지역피드) |
| `content` | string | ✅ | 포스팅 내용 (최소 1글자) |
| `imageBase64` | string | ❌ | 이미지 Base64 인코딩 (JPEG, PNG, GIF 지원) |
| `videoBase64` | string | ❌ | 동영상 Base64 인코딩 (mp4, webm, ogg, mov, avi 지원, 최대 5MB) |
| `youtubeUrl` | string | ❌ | YouTube URL (`https://youtube.com/watch?v=...` 또는 `https://youtu.be/...`) |

### ✅ 특징
- **미디어 독립적 선택**: `imageBase64`, `videoBase64`, `youtubeUrl`을 원하는 조합으로 선택 가능
- **전부 생략 가능**: 세 가지 모두 null/빈값이어도 됨 (텍스트만 포스팅)
- **중복 첨부 가능**: 모든 미디어 함께 첨부 가능

### Response (Success)
```json
{
  "result": "OK",
  "feedId": "feed_20260510_001",
  "timestamp": "2026-05-10T15:30:45Z",
  "msg": "Feed posted successfully"
}
```

### Response (Failure)
```json
{
  "result": "FAIL",
  "msg": "관리자 인증 실패",
  "errorCode": "AUTH_001"
}
```

---

## 2️⃣ **스타 피드 추가** (Creator Post)

### Endpoint
```
POST /api/super/star/feed/add
```

### Request Header
```json
{
  "Content-Type": "application/json"
}
```

### Request Body (이미지, 동영상, YouTube 독립적 선택 가능)

#### 📸 **이미지만 포함**
```json
{
  "prsId": "star_12345",
  "starPw": "hashed_password",
  "starToken": "jwt_token_abc123...",
  "feedText": "나의 새로운 피드를 공유합니다",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEA...",
  "videoBase64": null,
  "youtubeUrl": null
}
```

#### 🎥 **동영상 파일만 포함** (최대 5MB)
```json
{
  "prsId": "star_12345",
  "starPw": "hashed_password",
  "starToken": "jwt_token_abc123...",
  "feedText": "내 동영상을 보세요!",
  "imageBase64": null,
  "videoBase64": "data:video/mp4;base64,AAAAIGZ0eXBpc29tAAACAGisuoN...",
  "youtubeUrl": null
}
```

#### 🎬 **YouTube URL만 포함**
```json
{
  "prsId": "star_12345",
  "starPw": "hashed_password",
  "starToken": "jwt_token_abc123...",
  "feedText": "제 채널 영상입니다",
  "imageBase64": null,
  "videoBase64": null,
  "youtubeUrl": "https://youtu.be/dQw4w9WgXcQ"
}
```

#### 📸 + 🎥 + 🎬 **모두 포함**
```json
{
  "prsId": "star_12345",
  "starPw": "hashed_password",
  "starToken": "jwt_token_abc123...",
  "feedText": "팬 여러분, 제 새로운 콘텐츠들입니다",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEA...",
  "videoBase64": "data:video/mp4;base64,AAAAIGZ0eXBpc29tAAACAGisuoN...",
  "youtubeUrl": "https://youtu.be/dQw4w9WgXcQ"
}
```

### Request Parameters 설명

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `prsId` | string | ✅ | 스타 프로필 ID |
| `starPw` | string | ✅ | 스타 비밀번호 (암호화된 형식) |
| `starToken` | string | ✅ | 스타 인증 토큰 (JWT) |
| `feedText` | string | ✅ | 포스팅 내용 (최소 1글자) |
| `imageBase64` | string | ❌ | 이미지 Base64 인코딩 |
| `videoBase64` | string | ❌ | 동영상 Base64 인코딩 (최대 5MB) |
| `youtubeUrl` | string | ❌ | YouTube URL |

### ✅ 특징
- **자유로운 조합**: 3가지 미디어를 원하는 대로 조합 가능
- **토큰 검증**: `starToken` 필수 (만료 여부 확인)
- **중복 첨부 가능**: 모든 미디어 동시 첨부 가능

### Response (Success)
```json
{
  "result": "OK",
  "feedId": "feed_20260510_002",
  "createdAt": "2026-05-10T15:35:20Z",
  "msg": "Star feed posted successfully"
}
```

### Response (Failure)
```json
{
  "result": "FAIL",
  "msg": "스타 토큰 만료",
  "errorCode": "TOKEN_EXPIRED"
}
```

---

## 3️⃣ **Error Codes**

| Error Code | 설명 |
|-----------|------|
| `AUTH_001` | 관리자 인증 실패 |
| `AUTH_002` | 스타 인증 실패 |
| `TOKEN_EXPIRED` | 스타 토큰 만료 |
| `CONTENT_EMPTY` | 콘텐츠 필수입력 |
| `VIDEO_TOO_LARGE` | 동영상 파일 5MB 초과 |
| `VIDEO_FORMAT_INVALID` | 지원하지 않는 동영상 형식 |
| `YOUTUBE_URL_INVALID` | 유효하지 않은 YouTube URL |
| `ADMIN_LEVEL_INVALID` | 잘못된 관리자 레벨 |
| `SERVER_ERROR` | 서버 오류 |

---

## 📝 Frontend 통신 흐름

```
TS Component
    ↓
submitPost() 메서드
    ↓
payload 조성:
  • imageBase64: 이미지 있으면 포함, 없으면 null
  • videoBase64: 동영상 있으면 포함, 없으면 null
  • youtubeUrl: YouTube URL 있으면 포함, 없으면 null
    ↓
this.http.post(targetUrl, payload)
    ↓
API 요청 (이미지 + 동영상 + YouTube 모두 지원)
    ↓
Response: { result: "OK" }
    ↓
modalCtrl.dismiss({ success: true })
```

---

## 🎯 주요 개선사항 (v1.0 → v2.0)

| 항목 | v1.0 | v2.0 |
|------|------|------|
| 미디어 선택 | 하나만 선택 | 완전 자유 조합 |
| 이미지 | ✅ | ✅ |
| 동영상 | ✅ | ✅ (5MB 제한) |
| YouTube | ✅ | ✅ |
| 모두 함께 | ❌ | ✅ |
| 제거 기능 | 통합 제거 | 개별 제거 |

---

## 🛠️ 구현 세부사항

### Frontend (Angular/Ionic)
- **컴포넌트**: `AdminWriteModalComponent`
- **템플릿**: `admin-write-modal.component.html`
- **스타일**: `admin-write-modal.component.scss`
- **동영상 파일 크기**: 최대 5MB
- **동영상 형식**: mp4, webm, ogg, mov, avi
- **YouTube 임베드**: 정규식으로 자동 변환 (`safe-url.pipe.ts` 활용)

### Backend 요구사항
- Base64 인코딩된 이미지/동영상 파일 처리
- 5MB 크기 제한 검증
- 동영상 파일 형식 검증
- YouTube URL 유효성 검증
- 관리자/스타 인증 처리
- 미디어 데이터 저장소 (DB 또는 클라우드 스토리지)

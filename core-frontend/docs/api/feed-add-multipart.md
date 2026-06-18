# 피드 작성 API (multipart/form-data)

관리자/스타 피드 작성 시 미디어 파일을 첨부하는 API 문서입니다.
**기존 Base64 JSON 방식에서 `multipart/form-data` 방식으로 전환**되었습니다.

## 변경 배경
- 기존: 이미지/동영상을 Base64 인코딩하여 JSON body에 포함 → 용량 33% 증가, 직렬화/역직렬화 부하
- 변경: `multipart/form-data`로 File 객체를 바이너리 그대로 전송 → 전송 속도 및 서버 부하 대폭 개선

---

## 1. 관리자 피드 작성

### 엔드포인트
- Method: `POST`
- URL: `/api/super/admin/feed/add`
- 클라이언트 위치: `AdminWriteModalComponent.submitPost()`

### 요청 헤더
```
Content-Type: multipart/form-data; boundary=...  (브라우저가 자동 설정)
app_authorization: {APP_KEY}
authorization: {userToken}
Accept: application/json
```

> ⚠️ `Content-Type`은 클라이언트가 명시하지 않습니다. `FormData`를 전송하면 브라우저가 `boundary`를 포함해 자동 설정합니다.

### 요청 필드 (form-data)

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `adminId` | `string` | ✅ | 관리자 ID |
| `adminPw` | `string` | ✅ | 관리자 비밀번호 |
| `adminLevel` | `string` | ✅ | 관리자 레벨 (`GLOBAL` 또는 `LOCAL`) |
| `content` | `string` | ✅ | 피드 텍스트 내용 (줄바꿈 포함 가능) |
| `image` | `File` | ❌ | 첨부 이미지 파일 (JPEG, PNG 등) |
| `video` | `File` | ❌ | 첨부 동영상 파일 (mp4, webm, ogg, mov, avi / 최대 5MB) |
| `youtubeUrl` | `string` | ❌ | YouTube 영상 URL |

### 예시
```bash
curl -X POST \
  -H "app_authorization: {APP_KEY}" \
  -H "authorization: {userToken}" \
  -H "Accept: application/json" \
  -F "adminId=admin01" \
  -F "adminPw=pass1234" \
  -F "adminLevel=GLOBAL" \
  -F "content=오늘의 공지사항입니다." \
  -F "image=@/path/to/photo.jpg" \
  https://your-domain.example.com/api/super/admin/feed/add
```

---

## 2. 스타 피드 작성

### 엔드포인트
- Method: `POST`
- URL: `/api/super/star/feed/add`
- 클라이언트 위치: `AdminWriteModalComponent.submitPost()` (`isStar=true`)

### 요청 헤더
관리자 피드와 동일합니다.

### 요청 필드 (form-data)

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `prsId` | `string` | ✅ | 스타 고유 ID |
| `starPw` | `string` | ✅ | 스타 본인 인증 비밀번호 |
| `feedText` | `string` | ✅ | 피드 텍스트 내용 (줄바꿈 포함 가능) |
| `starToken` | `string` | ✅ | 스타 인증 토큰 |
| `image` | `File` | ❌ | 첨부 이미지 파일 (JPEG, PNG 등) |
| `video` | `File` | ❌ | 첨부 동영상 파일 (mp4, webm, ogg, mov, avi / 최대 5MB) |
| `youtubeUrl` | `string` | ❌ | YouTube 영상 URL |

### 예시
```bash
curl -X POST \
  -H "app_authorization: {APP_KEY}" \
  -H "authorization: {userToken}" \
  -H "Accept: application/json" \
  -F "prsId=star_001" \
  -F "starPw=starpass" \
  -F "feedText=새로운 포스트입니다!" \
  -F "starToken=eyJhbGciOi..." \
  -F "image=@/path/to/photo.jpg" \
  -F "video=@/path/to/clip.mp4" \
  https://your-domain.example.com/api/super/star/feed/add
```

---

## 응답

### 성공
```json
{
  "result": "OK"
}
```

### 실패
```json
{
  "result": "FAIL",
  "msg": "에러 메시지"
}
```

---

## 기존 → 신규 필드 매핑 (서버 마이그레이션 참고)

| 기존 (JSON Base64) | 신규 (multipart) | 비고 |
|--------------------|------------------|------|
| `imageBase64` (data:image/...;base64,...) | `image` (File) | 바이너리 직접 수신 |
| `videoBase64` (data:video/...;base64,...) | `video` (File) | 바이너리 직접 수신 |
| `youtubeUrl` (string) | `youtubeUrl` (string) | 변경 없음 |
| `content` / `feedText` (string) | `content` / `feedText` (string) | 변경 없음 |

---

## 서버 구현 참고 (Spring Boot 예시)

```java
@PostMapping("/api/super/admin/feed/add")
public ResponseEntity<?> addAdminFeed(
    @RequestParam("adminId") String adminId,
    @RequestParam("adminPw") String adminPw,
    @RequestParam("adminLevel") String adminLevel,
    @RequestParam("content") String content,
    @RequestParam(value = "image", required = false) MultipartFile image,
    @RequestParam(value = "video", required = false) MultipartFile video,
    @RequestParam(value = "youtubeUrl", required = false) String youtubeUrl
) {
    // image.getBytes(), image.getOriginalFilename() 등으로 파일 처리
    // 기존 Base64 디코딩 로직 제거
}
```

> 기존에 `@RequestBody`로 JSON을 받던 부분을 `@RequestParam` + `MultipartFile`로 변경하면 됩니다.
> `application.properties`에 파일 크기 제한도 확인: `spring.servlet.multipart.max-file-size=10MB`

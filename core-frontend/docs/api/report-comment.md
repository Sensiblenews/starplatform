# 신고(리포트) API

댓글 신고 기능에서 사용하는 API 문서입니다.

## 개요
- 목적: 특정 댓글을 신고 사유와 함께 전송합니다.
- 클라이언트 위치: `CommentModalComponent.reportComment()`

## 엔드포인트
- Method: `POST`
- URL: `/api/super/comment/report`

## 요청
### 요청 헤더
- `Content-Type: application/json`

### 요청 바디
```json
{
  "cmtId": "string | number",
  "deviceId": "string",
  "reason": "SPAM | ABUSE | AD"
}
```

### 필드 설명
- `cmtId`: 신고 대상 댓글 ID
- `deviceId`: 신고 요청 단말 식별자
- `reason`: 신고 사유 코드
  - `SPAM`: Inappropriate Content
  - `ABUSE`: Swearing/Insulting Statements
  - `AD`: Advertising Post

## 응답
- 서버 구현에 따라 다릅니다.
- 현재 클라이언트에서는 응답 코드/바디를 검사하지 않습니다.

## 에러 처리
- 현재 클라이언트는 에러 응답을 구분하지 않고 성공 메시지를 표시합니다.
- 실패 응답 형식 및 메시지는 서버 스펙을 확인해야 합니다.

## 예시
### 요청 예시
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"cmtId":123,"deviceId":"device-abc","reason":"SPAM"}' \
  https://your-domain.example.com/api/super/comment/report
```

### 응답 예시
```json
{
  "result": "OK"
}
```

> 응답 구조는 서버 구현에 따라 다를 수 있습니다.

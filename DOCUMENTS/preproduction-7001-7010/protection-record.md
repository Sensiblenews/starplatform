# main 브랜치 보호 규칙 적용 기록 (05단계)

적용 일시: 2026-10-11 17:29:48 · 계정: Sensiblenews(소유자) · 대상: Sensiblenews/starplatform main

## 적용 전
```
# before
{"message":"Branch not protected","documentation_url":"https://docs.github.com/rest/branches/branch-protection#get-branch-protection","status":"404"}gh: Branch not protected (HTTP 404)

```

## 적용 요청 본문
```json
{
  "required_status_checks": null,
  "enforce_admins": true,
  "required_pull_request_reviews": { "required_approving_review_count": 0, "dismiss_stale_reviews": true },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_conversation_resolution": false,
  "required_linear_history": false
}
```

## 적용 후 (API 응답 요약)
```json
{
  "url": "https://api.github.com/repos/Sensiblenews/starplatform/branches/main/protection",
  "required_pull_request_reviews": {
    "required_approving_review_count": 0,
    "dismiss_stale_reviews": true
  },
  "enforce_admins": true,
  "allow_force_pushes": false,
  "allow_deletions": false
}
```

검증 명령: `gh api repos/Sensiblenews/starplatform/branches/main/protection` (소유자 계정)

# 권한 확인표 (04·42단계)

| 저장소 | 계정 | 역할 | 비고 |
|---|---|---|---|
| starplatform | Sensiblenews | admin (소유자) | 개인 계정. Organization 이 아니라 팀·읽기 전용 등급을 만들 수 없다 |
| starplatform | gkrkdals | write | 개발진. main 은 보호 규칙으로 PR 필수 |
| Sensiblenews/platform-7001 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |
| Sensiblenews/platform-7002 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |
| Sensiblenews/platform-7003 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |
| Sensiblenews/platform-7004 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |
| Sensiblenews/platform-7005 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |
| Sensiblenews/platform-7006 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |
| Sensiblenews/platform-7007 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |
| Sensiblenews/platform-7008 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |
| Sensiblenews/platform-7009 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |
| Sensiblenews/platform-7010 | Sensiblenews | admin | 소유자 외 접근자 없음. 운영사 계정은 호기 계약 뒤 read 로 초대 |

## 설계 원칙

- Core(Master) 는 HQ 전용 권한으로 보호: 소유자 계정이 HQ 로 넘어가거나 HQ 계정을 admin 으로 초대하고, 개발진은 write + PR 필수.
- 호기 사본은 운영사별로 read(열람) 또는 triage 로 초대. 운영사가 코드를 바꾸지 않는 구조(Config 만 HQ 가 관리).
- 개인 계정의 한계: 저장소마다 협업자를 개별 초대해야 하고, 감사 로그·SSO·팀 권한이 없다. Organization(무료) 으로 옮기면 `Core` 팀(admin)/`Dev` 팀(write)/`Operator-700x` 팀(read) 구조가 가능하다 — 이전은 웹 UI 에서만 할 수 있다.

## 미적용 항목과 명령

| 항목 | 상태 | 명령 |
|---|---|---|
| 원본 저장소 private 전환 | 미적용(사용자 결정 보류) | `gh repo edit Sensiblenews/starplatform --visibility private --accept-visibility-change-consequences` |
| 운영 배포 브랜치 보호 | 미적용(HQ 결정) | `gh api -X PUT repos/Sensiblenews/starplatform/branches/feature%2F2-31-web-video-followup/protection --input protection.json` |
| 사본 main 보호 | 미적용 | 같은 명령을 platform-700x 에 반복. Config 변경도 PR 로 받을 때 적용 |

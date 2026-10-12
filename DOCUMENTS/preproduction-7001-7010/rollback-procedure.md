# 07 Rollback 절차 (49단계)

## 1. 백엔드 WAR

| 단계 | 명령/행동 | 확인 |
|---|---|---|
| 0 배포 전 | `cp webapps/witch.war backup/witch-<버전>-<날짜>.war` — 이전 WAR 를 항상 보존 | 파일 존재·해시 기록 |
| 1 문제 감지 | 헬스체크 실패·오류 로그 급증·핵심 기능 불가 | 모니터링 어드민/로그 |
| 2 롤백 결정 | DDL 변경이 있었던 배포인지 확인(차수 문서) | 있으면 3b |
| 3a 코드만 롤백 | 톰캣 중지 → `webapps/witch` 삭제 → 이전 WAR 복사 → 톰캣 시작 | `healthcheck.sh <domain>` 전부 200 |
| 3b 코드+DB 롤백 | 3a + `restore.sh <.env> <직전 백업> <작업 디렉터리> --db-only` | 복구 후 로그인·스타페이지·피드·업로드 수동 확인 |
| 4 설정 | `globals.properties` 를 바꿨다면 백업본으로 되돌림 | 톰캣 재시작 |
| 5 기록 | 롤백 시각·버전·원인·담당자를 release-notes 에 기록 | — |

코드 롤백이 실제로 가능한지(이전 태그로 WAR 재빌드) 는 `rollback-test.log` 에서 확인했다 — 롤백 WAR 는 재빌드 대신 보존해 둔 이전 WAR 를 쓰는 것이 원칙이고, 재빌드는 보존본이 없을 때의 대안이다.

## 2. 앱 (Android / iOS)

- 스토어는 바이너리 되돌리기가 없다. **단계적 출시(Play: Staged rollout, App Store: Phased release)** 를 쓰고 문제 시 출시 중단 → 이전 버전 번호보다 높은 versionCode 로 이전 소스를 재빌드해 핫픽스 올림.
- 서버 API 호환성: 앱 롤백 시 서버가 구버전 앱을 거부하지 않도록 기존 엔드포인트 삭제 금지 원칙 유지.

## 3. Factory 산출물

- Injector 는 Master 를 바꾸지 않으므로(worktree/`--out`) Factory 롤백은 "호기 Config 파일을 이전 커밋으로 되돌리고 재주입" 이다. 사본 저장소 갱신도 태그 단위라 이전 태그로 재푸시하면 된다.

## 4. 현 판정

코드 롤백 빌드 **PASS**, 서버·DB 롤백 실행 **NOT VERIFIED**(서버 없음). 복구 후 기능 테스트 시나리오: 로그인(Google·Kakao·Apple 중 1) → 로비 랭킹 표시 → 스타페이지 글 보기 → 글 작성(이미지) → 어드민 검수 승인 → 공개 확인.

# Master 보호 기준 (06·10단계) — HQ 승인 요청 문서

## 1. 보호 대상

- Repository `Sensiblenews/starplatform` 의 `main` 브랜치와 운영 배포 기준 브랜치 `feature/2-31-web-video-followup`.
- 태그 `master-baseline-v1.0` 이후의 모든 운영 반영은 PR 로만 들어간다.

## 2. 적용된 기술적 보호 (2026-10-11, Sensiblenews 계정으로 적용)

| 규칙 | 값 |
|---|---|
| Pull Request 필수 | 켜짐 (직접 push 거부) |
| 필수 승인 수 | 0 (협업자 2명뿐이라 셀프 머지 허용. 운영사·HQ 계정이 들어오면 1 이상으로 올린다) |
| 오래된 리뷰 무효화 | 켜짐 |
| force push | 금지 |
| 브랜치 삭제 | 금지 |
| 관리자 포함(enforce_admins) | 켜짐 — 소유자도 규칙을 우회하지 못한다 |

적용 전·후 API 응답은 `protection-record.md`. 운영 배포 기준 브랜치에도 같은 규칙을 적용할지는 HQ 결정 사항(현재는 기능 브랜치 스택 머지 관례 때문에 미적용).

## 3. 변경관리 규칙

1. 모든 변경은 `feature/<차수>-<주제>` 또는 `fix/<주제>` 브랜치에서 시작한다.
2. PR 제목은 Conventional Commits 접두사(`feat:`·`fix:`·`docs:`), 본문에 요청서 차수·검증 방법·배포 영향(DDL·설정·디렉터리)을 적는다.
3. 운영 반영 전 체크: `./mvnw.sh test` 통과, 앱 변경 시 `npx ng build` 성공, 서명·비밀 파일 미포함(`git status --ignored` 로 확인).
4. 머지 뒤 운영 배포는 수동(WAR 교체·스토어 업로드). 배포 시각·버전·담당자를 `DOCUMENTS/release-notes-*.md` 에 남긴다.
5. Factory 작업(`factory/`)은 Master 코드와 같은 저장소에 두되 Master 동작을 바꾸지 않는다 — Injector 는 `--out` 또는 별도 worktree 에만 쓰고, Master 체크아웃에 `--apply` 하지 않는다.
6. 호기 사본(`platform-700x`)은 Master 태그 단위로만 갱신한다(`02_GitHub_Repository/resync-snapshots.sh`). 사본에서 Master 로 역방향 머지는 하지 않는다.
7. 금지: `main` 직접 push, `.env`·`globals.properties` 승인 없는 수정, 기존 API 엔드포인트 삭제, `package.json` 의존성 변경, 확인 없는 DB 마이그레이션, 서명 키·자격 증명 파일 이동.

## 4. 승인

| 역할 | 이름 | 승인 | 일자 |
|---|---|---|---|
| 개발진 | gkrkdals | 작성·자체 검증 | 2026-10-11 |
| HQ 승인권자 | | ☐ 승인 ☐ 보완 요청 | |

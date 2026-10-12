# 02 Repository 목록 (04단계) — 2026-10-11 GitHub API 조회

| 저장소 | 공개 | 기본 브랜치 | HEAD | 태그 | 최근 push | 비고 |
|---|---|---|---|---|---|---|
| [Sensiblenews/starplatform](https://github.com/Sensiblenews/starplatform) (Master) | **public** | main (보호 규칙 적용) | cb7c05e (운영 브랜치) / 7757a70 (main) | master-baseline-v1.0, baseline-2026-10 | 2026-10-11 | 원본. private 전환은 미적용(명령은 permissions.md) |
| [Sensiblenews/platform-7001](https://github.com/Sensiblenews/platform-7001) | private | main | f8f9880 | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |
| [Sensiblenews/platform-7002](https://github.com/Sensiblenews/platform-7002) | private | main | 80f6380 | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |
| [Sensiblenews/platform-7003](https://github.com/Sensiblenews/platform-7003) | private | main | ba879f0 | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |
| [Sensiblenews/platform-7004](https://github.com/Sensiblenews/platform-7004) | private | main | a8ff813 | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |
| [Sensiblenews/platform-7005](https://github.com/Sensiblenews/platform-7005) | private | main | 69184ac | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |
| [Sensiblenews/platform-7006](https://github.com/Sensiblenews/platform-7006) | private | main | 878bbde | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |
| [Sensiblenews/platform-7007](https://github.com/Sensiblenews/platform-7007) | private | main | 7e90900 | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |
| [Sensiblenews/platform-7008](https://github.com/Sensiblenews/platform-7008) | private | main | 6737650 | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |
| [Sensiblenews/platform-7009](https://github.com/Sensiblenews/platform-7009) | private | main | 74ee5ba | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |
| [Sensiblenews/platform-7010](https://github.com/Sensiblenews/platform-7010) | private | main | fc89e65 | baseline-2026-10@10008ee | 2026-10-08 | baseline-2026-10 + README 1커밋. `resync-snapshots.sh` 로 master-baseline-v1.0 갱신 예정 |

사본 10개의 HEAD 는 모두 `baseline-2026-10`(08141c4) 위에 호기별 README 커밋 하나를 얹은 상태다. Master 가 PR #31·#32 로 2커밋 앞섰으므로 제출 뒤 `resync-snapshots.sh`(사용자 터미널 실행) 로 `master-baseline-v1.0` 을 머지·푸시한다.

원시 조회 결과: `repo-report.txt`. 재조회: `gh auth switch --user Sensiblenews && for n in $(seq 7001 7010); do gh api repos/Sensiblenews/platform-$n --jq '.full_name+" "+.pushed_at'; done`

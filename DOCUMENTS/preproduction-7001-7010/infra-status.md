# 05 인프라 현황 (④ Production 인프라 분리 증빙) — 2026-10-11

| 항목 | 현황 | 판정 | 증빙 |
|---|---|---|---|
| 플랫폼별 Server 구성 | Master 1대(KT, Cloudflare 경유)만 존재. 호기 서버 0대 | NOT VERIFIED | `architecture.md`(목표 구성) |
| DB 분리 | 호기별 DB명·계정·비밀번호 설계 + 생성 SQL 준비 | NOT VERIFIED | `separation-matrix.md`, 비밀 묶음 |
| Redis 분리 | 호기별 requirepass 설정 준비 | NOT VERIFIED | 비밀 묶음 `redis.conf.snippet` |
| Storage 분리 | 서버 분리 전제. 경로는 코드 고정 | NOT VERIFIED | `03_Config_Factory/hardcoded-inventory.md §6` |
| Auth·권한 분리 | 호기별 OAuth·Apple·Firebase 값은 Config `TODO_` | NOT VERIFIED | `03_Config_Factory/platforms/` |
| 도메인·SSL·CDN·WAF | Master: Cloudflare 프록시, Let's Encrypt(만료 2026-11-23), WAF 규칙은 계정 확인 필요. 호기 도메인 없음 | Master 확인 / 호기 NOT VERIFIED | `healthcheck-master.log` |
| 백업·복구 정책 | 정책·스크립트 작성, dry-run | 문서 PASS / 실행 NOT VERIFIED | `07_Backup_Rollback/` |
| 헬스체크 | 스크립트 작성, Master 1회 실행 | PASS(Master) | `healthcheck.sh`, `healthcheck-master.log` |

선행 결정(HQ): 클라우드·호스팅 선택(현 Master 는 KT 서버), 호기당 서버 1대 여부, Cloudflare 계정 구조(존 분리 vs 서브도메인), 도메인 10개.

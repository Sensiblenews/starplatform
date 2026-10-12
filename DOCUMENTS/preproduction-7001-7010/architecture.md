# 05 호기별 독립 인프라 구성도 (39~43단계 설계)

실제 호기 서버는 아직 없다. 아래는 Master 운영 구성을 그대로 복제하는 목표 구성과, Config Factory 가 채우는 값의 위치다.

```
                 운영사 N (platform-700N)
 ┌───────────────────────────────────────────────────────────────┐
 │  DNS: <domain-700N>  ──► Cloudflare (프록시·WAF·DDoS·ads.txt Worker) │
 │                                 │ 443 (Cloudflare IP 만 허용)        │
 │  ┌──────────────────────────────▼───────────────────────────┐  │
 │  │  Server-700N (Ubuntu, 1대)                                 │  │
 │  │   Tomcat 7  ── witch.war (Injector 로 도메인·광고 ID 주입)  │  │
 │  │     └ globals.properties (호기 DB·Redis·Apple·서명키)       │  │
 │  │     └ /var/lib/tomcat7/webapps/img|video|video/thumnail    │  │
 │  │     └ /var/lib/tomcat7/moderation/pending|hidden|dm-report  │  │
 │  │     └ /var/lib/tomcat7/keys/AuthKey_<KEYID>.p8             │  │
 │  │   MariaDB   ── DB starplatform_700N / user sp700N_app       │  │
 │  │   Redis     ── requirepass, bind 127.0.0.1, FLUSH* 비활성   │  │
 │  │   백업: ops/backup.sh → 암호화 아카이브 → 오프사이트          │  │
 │  └──────────────────────────────────────────────────────────┘  │
 │  앱: Android <packageName-700N> / iOS <bundleId-700N>            │
 │      Firebase 프로젝트-700N · AdMob 앱-700N · Apple 팀-700N        │
 └───────────────────────────────────────────────────────────────┘
          ×10 (7001~7010). 호기 간 공유 자원 없음.

 HQ: Sensiblenews/starplatform (Master, 보호) ──태그──► platform-700N 저장소 (사본)
                                              └─ factory/ (Config·Injector) ─► 호기 빌드
```

## 공유하지 않는 것 (독립성 체크 항목)

| 자원 | 호기별 독립 | 설정 위치 | 검증 방법(서버 생긴 뒤) |
|---|---|---|---|
| 서버·배포 대상 | 각 1대(초기) | 배포 스크립트 인자 | `healthcheck.sh <domain>` 호기별 실행 |
| DB 인스턴스·계정·데이터 | DB `starplatform_<n>`, 계정 `sp<n>_app`(localhost 전용) | `mysql-init.sql`, `globals.properties` | 다른 호기 계정으로 접속 시도 → 거부 확인 |
| Redis | 인스턴스 1개/서버, 비밀번호 상이 | `redis.conf.snippet`, `globals.properties` | 다른 호기 비밀번호로 AUTH → 실패 확인 |
| 업로드 스토리지 | 서버 로컬 디렉터리(서버가 다르므로 분리) | `Constants.java`(경로 고정) | 호기 A 의 URL 로 호기 B 파일 접근 → 404 |
| Auth(Apple·Google·Kakao·Facebook) | 호기별 앱·클라이언트 ID | `platform.config.json` → trapeze·capacitor·globals | 로그인 콜백 도메인 일치 확인 |
| Secrets | 호기별 `.env`(난수 생성, 비밀 묶음) | 서버 600 권한 파일 | 묶음 간 값 중복 0 확인(생성 시 난수) |
| 도메인·TLS·CDN·WAF | 호기별 Cloudflare 존(또는 서브도메인) | Cloudflare | DNS 조회·TLS 발급자·`cf-ray` 헤더 |
| 스토어·광고 계정 | 호기별 AdMob 앱 ID·AdSense 게시자 | Config `admob`·`adsense` | 콘솔 확인 |

## 1대 서버에 여러 호기를 올리는 경우(비권장)

`Constants.java` 의 업로드·검수 경로가 `/var/lib/tomcat7/...` 로 고정되어 있어 같은 서버에 두 호기를 올리면 파일이 섞인다. 톰캣 인스턴스 분리 + 경로 설정화(하드코딩 조사 §6)가 선행돼야 한다. 초기에는 호기당 서버 1대를 전제한다.

# Cloudflare 무료 플랜 설정 점검표 (2026-10-12)

클라이언트 요청 "현재 Cloudflare 무료 버전의 설정을 먼저 점검한 뒤 빠진 항목만 보완".
Cloudflare 대시보드 계정은 개발 측에 없으므로 **대시보드 항목은 운영 측이 직접 확인**해야 한다 (NEEDS OPS).
개발 측에서 바깥에서 관측 가능한 것만 먼저 확인했다 (1절).

---

## 1. 외부에서 관측한 현재 상태 (2026-10-12, curl/dig)

| 항목 | 관측 | 판정 |
|---|---|---|
| 프록시 | `server: cloudflare`, `cf-ray` 응답 — 루트 도메인이 Cloudflare 를 거친다 | ✅ |
| HTTP→HTTPS | `http://witch-hunting.com/` → 301 | ✅ |
| 캐시 | HTML 은 `cf-cache-status: DYNAMIC` (정상, 동적 페이지) | ✅ |
| HSTS | `strict-transport-security` 헤더 **없음** | ❌ 보완 (대시보드 SSL/TLS → Edge Certificates → HSTS) |
| 브라우저 보안 헤더 | `x-frame-options`, `x-content-type-options`, `referrer-policy`, `content-security-policy` **없음** | ❌ 보완 (아래 3절) |
| 세션 쿠키 | `JSESSIONID … HttpOnly` 만 있고 **`Secure` 없음**. 공개 페이지에도 세션이 만들어짐 | ❌ 보완 (아래 3절) |
| 관리자 경로 | `/super/login.do` 가 공개 인터넷에서 그대로 열림 (200). 보호는 앱의 비밀번호+OTP 만 | 🟡 CF Access 또는 IP 제한 권장 |
| DNS | 루트·www 모두 Cloudflare IP(104.21.x / 172.67.x), NS 는 Cloudflare. MX 없음. `mail`·`api`·`dev`·`origin` 서브도메인 없음 → 공개 DNS 에서 원본 IP 노출 **없음** | ✅ (대시보드에서 비공개 레코드도 Proxied 인지 재확인) |

> 원본 서버 IP(`211.253.26.237`, globals.properties 의 DB 호스트와 동일 대역)가 DNS 어디에도 직접 노출되지 않아야 한다.
> 노출되어 있으면 Cloudflare 를 우회해 직접 공격이 가능하다.

## 2. 대시보드에서 확인할 것 (무료 플랜에서 가능한 것만)

| # | 항목 | 위치 | 기대 설정 | 요청서 매핑 |
|---|---|---|---|---|
| 1 | SSL/TLS 모드 | SSL/TLS → Overview | **Full (strict)**. 원본에 유효한 인증서(Cloudflare Origin CA 무료) 필요 | 01-04 |
| 2 | Always Use HTTPS | SSL/TLS → Edge Certificates | On | 01-09 |
| 3 | HSTS | 같은 곳 | On, max-age 6개월, includeSubDomains 는 모든 서브도메인이 HTTPS 일 때만 | 01-09 |
| 4 | 최소 TLS 버전 | 같은 곳 | **TLS 1.2** (기본값 1.0 이면 올릴 것) | 01-09 |
| 5 | WAF 관리형 규칙 | Security → WAF → Managed rules | 무료 플랜은 "Cloudflare Managed Ruleset (Free)" 만 제공. On 확인 | 01-05 |
| 6 | DDoS | Security → DDoS | HTTP DDoS 보호는 전 플랜 자동. 민감도 기본값 확인. Notifications 에서 "HTTP DDoS Attack Alert" 켜기 | 01-06, 03-02 |
| 7 | Rate Limiting | Security → WAF → Rate limiting rules | 무료 1개 규칙. **`/super/loginPrc.do` 와 `/super/mfa/verifyPrc.do`** 에 IP 당 10회/분 → 차단 10분 권장 | 01-07, 02-12 |
| 8 | Bot Fight Mode | Security → Bots | On (무료). 정상 검색 봇은 Verified Bots 로 자동 허용 | 02-02~06 |
| 9 | Security Level / Browser Integrity Check | Security → Settings | Medium 이상 / On | 02-06 |
| 10 | 원본 IP 노출 | DNS → Records | A/AAAA/CNAME 전부 **Proxied(주황 구름)**. MX 가 같은 서버를 가리키면 메일은 별도 호스트로 | 01-02, 01-10 |
| 11 | 원본 직접 접근 차단 | 원본 서버 방화벽 | 80/443 은 Cloudflare IP 대역만 허용 (https://www.cloudflare.com/ips/) | 01-02, 01-10 |
| 12 | 관리 포트 | 원본 서버 방화벽 | SSH(22)·DB(3306)·Redis(6379) 는 인터넷에 열지 않음. 운영 IP 또는 VPN 만 | 01-03 |
| 13 | 관리자 경로 보호 | Zero Trust → Access (무료 50명) | `witch-hunting.com/super/*` 에 Access 정책(허용 이메일 + OTP) 추가 → 앱 로그인 앞에 한 겹 더 | 01-11 |
| 14 | 보안 헤더 | Rules → Transform Rules → Modify Response Header | 아래 3절 헤더 추가 (무료 플랜 규칙 수 제한 안에서) | 01-09 |
| 15 | 캐시 규칙 | Caching → Cache Rules | 정적(`/resources/*`, 이미지·영상)만 캐시, `/api/*`·`/super/*`·개인화 응답은 Bypass. (로컬 `core-backend/docs/cloudflare-cache-rules.md` 참고) | 01-08, 06-06 |
| 16 | API 토큰 | My Profile → API Tokens | Global API Key 사용 금지. Worker 배포 등 용도별 토큰·최소 권한·만료 | 01-13 |
| 17 | 변경 이력 | Manage Account → Audit Log | 월 1회 확인. 규칙 변경은 이 문서 또는 운영 일지에 기록 | 01-14, 01-15 |
| 18 | 계정 MFA | My Profile → Authentication | Cloudflare 계정에 2FA 적용 | 03-07 |

## 3. 보안 헤더 · 쿠키 보완안

**Cloudflare Transform Rule (권장, 코드 변경 없음)** — 응답 헤더 추가:
```
Strict-Transport-Security: max-age=15552000            (HSTS 설정 메뉴로 대신 가능)
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN                            (공개 랜딩을 외부에 iframe 으로 넣는 곳이 있으면 제외)
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```
CSP 는 어드민이 jsDelivr·cdnjs·jQuery CDN 과 인라인 스크립트를 쓰고 있어 바로 걸면 깨진다. 먼저 `Content-Security-Policy-Report-Only` 로 관측 후 적용.

**세션 쿠키 Secure** — 둘 중 하나:
- 애플리케이션: `web.xml` 에 `<session-config><cookie-config><secure>true</secure></cookie-config>` (Servlet 3.0 스키마 필요, 로컬 http 개발이 깨지므로 운영 전용 설정으로) — **설정 파일 변경이라 승인 항목**
- Cloudflare: Transform Rule 로 `Set-Cookie` 에 `; Secure` 추가는 불가(응답 헤더 값 편집 제한). 애플리케이션 쪽이 맞다.
- 참고: 이번 작업의 관리자 토큰 쿠키(`SUPER_TOKEN`)에는 `SameSite=Lax` 를 추가했다. `Secure` 는 원본이 HTTP 로 받는 구조(CF 종단)라 `request.isSecure()` 로 판단할 수 없어 보류 — Full(strict) 전환 후 함께 적용.

## 4. 요청서 항목 중 무료 플랜에서 안 되는 것

| 항목 | 상태 | 대안 |
|---|---|---|
| 로그인·검색·댓글·API 별 Rate Limit 여러 개 | 무료는 규칙 1개 | 가장 위험한 관리자 로그인 1개만 CF 에, 나머지는 앱(Redis 버스트 가드·OTP 잠금)으로 |
| OWASP Core Ruleset 전체 | Pro 이상 | 무료 Managed Ruleset + 앱 입력 검증 |
| Zone Lockdown / IP Access Rules 세밀 제어 | 제한적 | Access(Zero Trust) 무료 범위로 관리자 경로 보호 |
| Health Checks / 외부 생존 감시 | Pro 이상 | 외부 업타임 서비스 사용 (`server-alert-runbook.md` 3절) |
| Logpush / Security Events 장기 보관 | 유료 | 대시보드 Security Events 24h~72h 범위 내 수동 확인 |

## 5. 완료 기준 (요청서 01 대항목)

- 원본 IP 직접 접근이 차단되고(11번 실측), Cloudflare 경유 정상 트래픽(웹·앱 API·어드민)이 유지된다.
- 1·2·3·4·7·8·13 적용 후 공개 페이지·앱 로그인·관리자 로그인(OTP 포함)을 다시 확인한다.
- 적용 내역과 실측 결과(curl 헤더, dig)를 이 문서 1절 표에 갱신해 제출한다.

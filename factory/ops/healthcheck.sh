#!/bin/zsh
# 호기 공개 엔드포인트 헬스체크. 사용: healthcheck.sh <domain> [scheme]
# 서버 내부(DB·Redis) 는 보지 않는다 — 그건 서버에서 globals 설정으로 접속 확인.
D=${1:?domain}; SCHEME=${2:-https}; B="$SCHEME://$D"
echo "# healthcheck $B  $(date '+%F %T')"
for p in / /robots.txt /sitemap.xml /ads.txt /.well-known/assetlinks.json /.well-known/apple-app-site-association /about /faq /posts; do
  code=$(curl -s -o /dev/null -w '%{http_code} %{time_total}s %{size_download}B' --max-time 15 "$B$p")
  printf '%-45s %s\n' "$p" "$code"
done
echo "# TLS"
echo | openssl s_client -servername "$D" -connect "$D:443" 2>/dev/null | openssl x509 -noout -subject -issuer -dates 2>/dev/null || echo "TLS 확인 불가"
echo "# DNS"
dig +short "$D" A 2>/dev/null | head -5
echo "# 서버 헤더"
curl -sI --max-time 15 "$B/" | grep -i -E '^(server|cf-ray|cf-cache-status|x-powered-by|strict-transport|content-type):' | head -8

#!/bin/zsh
# 호기 서버 복구 — backup.sh 가 만든 .tar.enc 를 풀어 DB·파일·설정을 되돌린다.
# 사용: restore.sh <.env> <백업.tar.enc> <복구 작업 디렉터리> [--dry-run] [--db-only|--files-only]
# 절차: 1) 톰캣 중지 2) 현재 상태를 backup.sh 로 한 번 더 백업(복구 실패 대비) 3) 이 스크립트 4) 톰캣 시작 5) healthcheck.sh
set -e
ENV=${1:?.env}; ENC=${2:?backup.tar.enc}; WORK=${3:?작업 디렉터리}; shift 3
DRY=""; MODE=all
for a in "$@"; do case $a in --dry-run) DRY=1;; --db-only) MODE=db;; --files-only) MODE=files;; esac; done
set -a; source "$ENV"; set +a
: ${DB_HOST:=127.0.0.1}; : ${DB_PORT:=3306}; : ${UPLOAD_ROOT:=/var/lib/tomcat7/webapps}; : ${MODERATION_ROOT:=/var/lib/tomcat7/moderation}
run() { if [ -n "$DRY" ]; then echo "[dry-run] $*"; else eval "$*"; fi; }
echo "# restore from $ENC (mode=$MODE)"
run "shasum -a 256 -c '${ENC}.sha256'"
run "mkdir -p '$WORK'"
run "openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_ENCRYPTION_PASSPHRASE -in '$ENC' | tar -xf - -C '$WORK'"
NAME=$(basename "$ENC" .tar.enc); D="$WORK/$NAME"
run "(cd '$D' && shasum -a 256 -c SHA256SUMS)"
if [ "$MODE" != files ]; then
  # DB: 비어 있는 호기 DB 에 되돌린다. 기존 데이터가 있으면 먼저 DROP/CREATE 를 운영자가 결정한다
  run "gunzip -c '$D/db-$DB_NAME.sql.gz' | MYSQL_PWD=\"\$DB_PASSWORD\" mysql -h $DB_HOST -P $DB_PORT -u $DB_USER $DB_NAME"
fi
if [ "$MODE" != db ]; then
  run "tar -xzf '$D/uploads.tar.gz' -C '$UPLOAD_ROOT'"
  run "tar -xzf '$D/moderation.tar.gz' -C '$(dirname $MODERATION_ROOT)'"
  echo "# config.tar.gz 는 자동 반영하지 않는다 — globals.properties 는 내용 확인 뒤 수동 배치"
fi
echo "# done. 다음: 톰캣 시작 → factory/ops/healthcheck.sh <domain> → 핵심 기능 점검(로그인·스타페이지·피드·업로드)"

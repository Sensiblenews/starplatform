#!/bin/zsh
# 호기 서버 백업 — DB 덤프 + 업로드/검수 디렉터리 + 설정 파일을 암호화 아카이브로 만든다.
# 사용: backup.sh <.env> <백업 보관 디렉터리> [--dry-run]
#   .env 는 비밀 묶음의 platform-<n>/.env (DB_NAME·DB_USER·DB_PASSWORD·BACKUP_ENCRYPTION_PASSPHRASE 사용)
# 운영 DB 에 대고 실행하는 명령이므로 --dry-run 으로 먼저 명령만 확인한다.
set -e
ENV=${1:?.env 파일}; DEST=${2:?백업 디렉터리}; DRY=${3:-}
set -a; source "$ENV"; set +a
: ${DB_HOST:=127.0.0.1}; : ${DB_PORT:=3306}
: ${UPLOAD_ROOT:=/var/lib/tomcat7/webapps}; : ${MODERATION_ROOT:=/var/lib/tomcat7/moderation}; : ${TOMCAT_HOME:=/var/lib/tomcat7}
TS=$(date +%Y%m%d-%H%M%S); NAME="platform-${PLATFORM_ID}-${TS}"; WORK="$DEST/$NAME"
run() { if [ "$DRY" = "--dry-run" ]; then echo "[dry-run] $*"; else eval "$*"; fi; }
echo "# backup $NAME → $DEST"
run "mkdir -p '$WORK'"
# 1. DB (단일 트랜잭션, 루틴·트리거 포함). 비밀번호는 인자가 아니라 환경변수로 넘긴다
run "MYSQL_PWD=\"\$DB_PASSWORD\" mysqldump -h $DB_HOST -P $DB_PORT -u $DB_USER --single-transaction --routines --triggers --quick $DB_NAME | gzip > '$WORK/db-$DB_NAME.sql.gz'"
# 2. 업로드 파일(img·video·thumnail) 과 검수 디렉터리(pending·hidden·dm-report)
run "tar -czf '$WORK/uploads.tar.gz' -C '$UPLOAD_ROOT' img video"
run "tar -czf '$WORK/moderation.tar.gz' -C '$(dirname $MODERATION_ROOT)' '$(basename $MODERATION_ROOT)'"
# 3. 설정 (globals.properties, 배포 WAR)
run "tar -czf '$WORK/config.tar.gz' -C '$TOMCAT_HOME' webapps/witch/WEB-INF/classes/META-INF/props/globals.properties webapps/witch.war"
# 4. 매니페스트 + 암호화 (비밀 구절은 .env 의 BACKUP_ENCRYPTION_PASSPHRASE)
run "(cd '$WORK' && shasum -a 256 * > SHA256SUMS)"
run "tar -cf - -C '$DEST' '$NAME' | openssl enc -aes-256-cbc -pbkdf2 -salt -pass env:BACKUP_ENCRYPTION_PASSPHRASE -out '$DEST/$NAME.tar.enc'"
run "rm -rf '$WORK'"
run "shasum -a 256 '$DEST/$NAME.tar.enc' > '$DEST/$NAME.tar.enc.sha256'"
echo "# done: $DEST/$NAME.tar.enc (+ .sha256). 오프사이트 저장소로 복사하고 보존 기간(일 7·주 4·월 6)에 따라 정리한다"

#!/usr/bin/env bash
set -euo pipefail
COMPOSE_FILE=/root/deploy/docker-compose.yml
BACKUP_DIR=/root/deploy/db-backups
LOG_FILE=/root/deploy/db-backups/backup.log
MYSQL_CONTAINER=disease_mysql
KEEP_DAILY=30
KEEP_WEEKLY=24

mkdir -p "$BACKUP_DIR"

log(){ echo "[$(date +"%F %T")] $*" | tee -a "$LOG_FILE"; }

extract_compose_value(){
  local key="$1"
  grep -E "^[[:space:]]*$key:" "$COMPOSE_FILE" | awk -F': ' '{print $2}' | tr -d '"' | tr -d "'" | tr -d '[:space:]' | awk 'NR==1{print;exit}'
}

DB_USER="$(extract_compose_value MYSQL_USER)"
DB_PASS="$(extract_compose_value MYSQL_PASSWORD)"
DB_NAME="$(extract_compose_value MYSQL_DATABASE)"

if [[ -z "$DB_USER" || -z "$DB_PASS" || -z "$DB_NAME" ]]; then
  log "ERROR: 未能从 compose 中解析数据库连接信息"
  exit 1
fi

STAMP="$(date +%Y%m%d_%H%M%S)"
DOW="$(date +%u)"
TYPE="daily"
if [[ "$DOW" == "1" ]]; then TYPE="weekly"; fi
OUT_BASE="${TYPE}_${DB_NAME}_${STAMP}.sql"
OUT_SQL="${BACKUP_DIR}/${OUT_BASE}"
OUT_GZ="${OUT_SQL}.gz"

log "开始备份 ${DB_NAME} (${TYPE})"

docker exec "$MYSQL_CONTAINER" mysqldump   -u"$DB_USER" -p"$DB_PASS"   --default-character-set=utf8mb4   --single-transaction --quick --routines --events --triggers   --set-gtid-purged=OFF --no-tablespaces   "$DB_NAME" > "$OUT_SQL"

gzip -f "$OUT_SQL"
sha256sum "$OUT_GZ" > "${OUT_GZ}.sha256"

log "备份完成: ${OUT_GZ} ($(du -h "$OUT_GZ" | awk '{print $1}'))"

a=$(ls -1t "$BACKUP_DIR"/daily_*.sql.gz 2>/dev/null || true)
if [[ -n "$a" ]]; then
  echo "$a" | awk 'NR>'"$KEEP_DAILY"'' | while IFS= read -r f; do
    rm -f "$f" "${f}.sha256"
    log "清理旧日备份: $f"
  done
fi

b=$(ls -1t "$BACKUP_DIR"/weekly_*.sql.gz 2>/dev/null || true)
if [[ -n "$b" ]]; then
  echo "$b" | awk 'NR>'"$KEEP_WEEKLY"'' | while IFS= read -r f; do
    rm -f "$f" "${f}.sha256"
    log "清理旧周备份: $f"
  done
fi

ln -sfn "$OUT_GZ" "$BACKUP_DIR/latest.sql.gz"
ln -sfn "${OUT_GZ}.sha256" "$BACKUP_DIR/latest.sql.gz.sha256"

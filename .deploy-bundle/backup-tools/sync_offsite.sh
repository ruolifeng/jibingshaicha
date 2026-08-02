#!/usr/bin/env bash
set -euo pipefail
BACKUP_DIR=/root/deploy/db-backups
LOG_FILE=/root/deploy/db-backups/backup.log
ENV_FILE=/root/deploy/tools/backup/.env

log(){ echo "[$(date +"%F %T")] $*" | tee -a "$LOG_FILE"; }

if [[ -f "$ENV_FILE" ]]; then
  source "$ENV_FILE"
fi

if ! command -v rclone >/dev/null 2>&1; then
  log "SKIP: 未安装 rclone，跳过云端同步"
  exit 0
fi

TARGET_REMOTE="${BACKUP_REMOTE:-}"
if [[ -z "$TARGET_REMOTE" && -n "${RCLONE_REMOTE:-}" && -n "${BACKUP_REMOTE_PATH:-}" ]]; then
  TARGET_REMOTE="${RCLONE_REMOTE}:${BACKUP_REMOTE_PATH}"
fi

if [[ -z "$TARGET_REMOTE" ]]; then
  log "SKIP: 未配置 BACKUP_REMOTE 或 (RCLONE_REMOTE + BACKUP_REMOTE_PATH)，跳过云端同步"
  exit 0
fi

log "开始同步到云端: ${TARGET_REMOTE}"
rclone copy "$BACKUP_DIR" "$TARGET_REMOTE" --include "*.sql.gz" --include "*.sha256" --transfers 4 --checkers 8
log "云端同步完成"

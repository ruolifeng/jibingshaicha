#!/usr/bin/env bash
set -euo pipefail
COMPOSE_FILE=/root/deploy/docker-compose.yml
BACKUP_DIR=/root/deploy/db-backups
MYSQL_CONTAINER=disease_mysql

extract_compose_value(){
  local key="$1"
  grep -E "^[[:space:]]*$key:" "$COMPOSE_FILE" | awk -F': ' '{print $2}' | tr -d '"' | tr -d "'" | tr -d '[:space:]' | awk 'NR==1{print;exit}'
}

DB_USER="$(extract_compose_value MYSQL_USER)"
DB_PASS="$(extract_compose_value MYSQL_PASSWORD)"
DB_NAME="$(extract_compose_value MYSQL_DATABASE)"

if [[ ! -L "$BACKUP_DIR/latest.sql.gz" ]]; then
  echo "latest.sql.gz 不存在"; exit 1
fi

echo "将恢复: $(readlink -f "$BACKUP_DIR/latest.sql.gz") 到库 ${DB_NAME}"
read -r -p "确认恢复请输入 YES: " ans
[[ "$ans" == "YES" ]] || { echo "已取消"; exit 1; }

gzip -dc "$BACKUP_DIR/latest.sql.gz" | docker exec -i "$MYSQL_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME"
echo "恢复完成"

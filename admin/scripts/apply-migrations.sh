#!/usr/bin/env bash
# 按版本号顺序执行 admin/src/main/resources/migration 下全部 SQL 迁移（可重复执行）
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MIGRATION_DIR="$ROOT_DIR/src/main/resources/migration"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-disease_monitor}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:?请设置 DB_PASSWORD 环境变量}"

MYSQL=(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME")

if [ ! -d "$MIGRATION_DIR" ]; then
  echo "迁移目录不存在: $MIGRATION_DIR" >&2
  exit 1
fi

echo "Applying migrations to ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"

shopt -s nullglob
files=("$MIGRATION_DIR"/V*.sql)
if [ ${#files[@]} -eq 0 ]; then
  echo "未找到迁移文件" >&2
  exit 1
fi

IFS=$'\n' sorted=($(printf '%s\n' "${files[@]}" | sort -V))
for file in "${sorted[@]}"; do
  echo ">> $(basename "$file")"
  "${MYSQL[@]}" < "$file"
done

echo "All migrations applied."

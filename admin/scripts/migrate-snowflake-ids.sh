#!/usr/bin/env bash
# 停机迁移：存量自增主键 → 雪花 ID
#
# 步骤：
#   1. 停止后端与前端服务
#   2. 全库备份（强烈建议）
#   3. 执行本脚本（会以 migrate-snowflake-ids=true 启动一次 Spring Boot）
#   4. 确认日志出现「雪花 ID 存量迁移完成」
#   5. 确认 application-*.yaml 中 app.migrate-snowflake-ids=false 后正常启动服务
#   6. 全员重新登录
#
# 用法：
#   cd admin
#   DB_PASSWORD=xxx ./scripts/migrate-snowflake-ids.sh
#   # 或指定 profile：SPRING_PROFILES_ACTIVE=prod ./scripts/migrate-snowflake-ids.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"

echo "==> [1/3] 建议先备份数据库，例如："
echo "    mysqldump -uroot -p --single-transaction --routines --triggers disease_monitor > backup_before_snowflake.sql"
echo ""
read -r -p "已完成备份？输入 yes 继续: " confirm
if [[ "$confirm" != "yes" ]]; then
  echo "已取消"
  exit 1
fi

echo "==> [2/3] 以 app.migrate-snowflake-ids=true 启动迁移（profile=${PROFILE}）"
MVN=(./mvnw)
if [[ ! -x ./mvnw ]]; then
  MVN=(mvn)
fi

"${MVN[@]}" -DskipTests spring-boot:run \
  -Dspring-boot.run.profiles="${PROFILE}" \
  -Dspring-boot.run.arguments="--app.migrate-snowflake-ids=true"

echo ""
echo "==> [3/3] 迁移进程已结束。请确认："
echo "    1) 日志含「雪花 ID 存量迁移完成」"
echo "    2) yaml 中 app.migrate-snowflake-ids 保持 false"
echo "    3) 正常启动后端与前端，全员重新登录"

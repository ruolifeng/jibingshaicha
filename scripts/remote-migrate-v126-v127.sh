#!/bin/bash
set -euo pipefail
DB_PASS=FwFAdsVs80RDu2PSS7FddfDL
MYSQL=(docker exec -i disease_mysql mysql -udisease_monitor -p"${DB_PASS}" --default-character-set=utf8mb4 disease_monitor)

echo "==> V126"
"${MYSQL[@]}" < /tmp/V126_follow_up_due_same_day_reminder.sql
echo "==> V127"
"${MYSQL[@]}" < /tmp/V127_close_contact_case_preventive_reminder_switches.sql
echo "==> verify"
docker exec disease_mysql mysql -udisease_monitor -p"${DB_PASS}" --default-character-set=utf8mb4 disease_monitor -e \
  "SELECT code,name,enabled FROM sys_message_reminder_config WHERE code LIKE 'follow_up_due' OR code LIKE 'supervision_due' OR code LIKE 'close_contact_case_followup%' ORDER BY code;"
echo MIGRATE_OK

#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEY="$ROOT/it_temp_key"
HOST="root@10.92.69.65"
TS_TAG=$(date +%Y%m%d-%H%M)
FRONTEND_TAR="/tmp/frontend-dist-${TS_TAG}.tar.gz"

echo "==> Upload jar"
scp -i "$KEY" -o StrictHostKeyChecking=no "$ROOT/admin/target/admin-1.0-SNAPSHOT.jar" "$HOST:/root/deploy/backend/app.jar.plain"

echo "==> Pack and upload frontend"
tar czf "$FRONTEND_TAR" -C "$ROOT/dist" .
scp -i "$KEY" -o StrictHostKeyChecking=no "$FRONTEND_TAR" "$HOST:$FRONTEND_TAR"

ssh -i "$KEY" -o StrictHostKeyChecking=no "$HOST" bash -s <<REMOTE
set -euo pipefail
FRONTEND_TAR=$FRONTEND_TAR
TS=\$(date +%Y%m%d_%H%M%S)
cd /root/deploy
cp -a backend/app.jar "backend/app.jar.bak_\${TS}"
[ -d frontend/dist ] && mv frontend/dist "frontend/dist.bak_\${TS}"
mkdir -p frontend/dist
tar xzf "\$FRONTEND_TAR" -C frontend/dist
test -f frontend/dist/index.html
cd /root/deploy/backend
cp -a app.jar.plain admin-to-encrypt.jar
docker run --rm -v /root/deploy/tools:/tools -v /root/deploy/backend:/work -w /work \
  eclipse-temurin:17-jdk java -jar /tools/classfinal-fatjar.jar \
  -file admin-to-encrypt.jar -packages cn.luyou -pwd udCC8v3z6lCODRlCsrH6VYx8yFZ1 -Y
cp -a admin-to-encrypt-encrypted.jar app.jar
cd /root/deploy
docker compose build backend frontend
docker compose up -d --no-deps backend frontend
for i in \$(seq 1 60); do
  docker logs --tail 300 disease_backend 2>&1 | grep -q "Started Application" && break
  sleep 2
done
docker logs --tail 300 disease_backend 2>&1 | grep "Started Application" | tail -1
curl -s -o /dev/null -w "frontend:%{http_code}\n" http://127.0.0.1/
docker ps --format "table {{.Names}}\t{{.Status}}" | grep disease
rm -f "\$FRONTEND_TAR" /root/deploy/backend/app.jar.plain /root/deploy/backend/admin-to-encrypt.jar /root/deploy/backend/admin-to-encrypt-encrypted.jar 2>/dev/null || true
ls -1t /root/deploy/backend/app.jar.bak_* 2>/dev/null | tail -n +3 | xargs -r rm -f
ls -1d /root/deploy/frontend/dist.bak_* 2>/dev/null | tail -n +3 | xargs -r rm -rf
echo DONE
REMOTE

rm -f "$FRONTEND_TAR"
echo "DEPLOY_COMPLETE"

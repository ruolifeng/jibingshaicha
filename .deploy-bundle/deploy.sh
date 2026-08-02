#!/bin/bash
set -e
cd /root/deploy
TS=$(date +%Y%m%d_%H%M%S)
cp backend/app.jar "backend/app.jar.bak_${TS}"
cp -a frontend/dist "frontend/dist.bak_${TS}"
cp /tmp/deploy-bundle/app.jar backend/app.jar
rsync -a --delete /tmp/deploy-bundle/dist/ frontend/dist/
docker compose up -d --build --no-deps frontend backend
docker compose ps

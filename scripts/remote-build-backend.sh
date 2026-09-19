#!/bin/bash
set -euo pipefail
rm -rf /tmp/admin-build
mkdir -p /tmp/admin-build
python3 -c "import zipfile; zipfile.ZipFile('/tmp/admin-src.zip').extractall('/tmp/admin-build'); print('extracted')"
ls -la /tmp/admin-build
echo START_MAVEN
docker run --rm \
  -v /tmp/admin-build:/app \
  -v maven-repo:/root/.m2 \
  -w /app \
  maven:3.9-eclipse-temurin-17 mvn -B clean package -DskipTests
ls -lh /tmp/admin-build/target/*.jar
cp -a /tmp/admin-build/target/admin-1.0-SNAPSHOT.jar /tmp/admin-1.0-SNAPSHOT.jar
echo BUILD_OK

#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

if [ ! -f .env ]; then
  echo "[错误] 当前目录缺少 .env 文件。"
  exit 1
fi

if [ ! -f frontend/dist/index.html ]; then
  echo "[错误] 缺少 frontend/dist，请先在本地执行 ./package-release.sh 并上传最新包。"
  exit 1
fi

if [ ! -f backend/target/platform-0.0.1-SNAPSHOT.jar ]; then
  echo "[错误] 缺少 backend/target/platform-0.0.1-SNAPSHOT.jar，请先在本地执行 ./package-release.sh 并上传最新包。"
  exit 1
fi

echo "[1/3] 停止旧容器..."
docker-compose down --remove-orphans || true

echo "[2/3] 启动新版本..."
docker-compose up -d --build

echo "[3/3] 当前状态"
docker-compose ps

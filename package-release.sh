#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
RELEASE_DIR="$ROOT_DIR/release"
ARCHIVE_NAME="${1:-deploy-built.tar.gz}"

cd "$ROOT_DIR"

if [ ! -f .env ]; then
  echo "[错误] 未找到 .env 文件，请先准备好本地环境变量。"
  exit 1
fi

echo "[1/4] 构建前端..."
cd frontend
npm run build
cd "$ROOT_DIR"

echo "[2/4] 打包后端..."
cd backend
mvn clean package -DskipTests -q
cd "$ROOT_DIR"

echo "[3/4] 生成发布包..."
mkdir -p "$RELEASE_DIR"
rm -f "$RELEASE_DIR/$ARCHIVE_NAME"
tar \
  --exclude='.git' \
  --exclude='.cursor' \
  --exclude='release' \
  --exclude='frontend/node_modules' \
  -czf "$RELEASE_DIR/$ARCHIVE_NAME" .

echo "[4/4] 完成"
echo "发布包路径: $RELEASE_DIR/$ARCHIVE_NAME"
echo "下一步可执行:"
echo "scp \"$RELEASE_DIR/$ARCHIVE_NAME\" root@你的服务器IP:~/"

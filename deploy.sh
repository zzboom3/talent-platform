#!/bin/bash
set -e

echo "=============================="
echo " 京州市软件产业人才公共服务平台"
echo " 一键部署脚本"
echo "=============================="

if [ ! -f .env ]; then
    echo "[错误] 未找到 .env 文件，请先执行: cp .env.example .env 并填写配置"
    exit 1
fi

echo ""
echo "[1/3] 构建前端..."
cd frontend
npm install --silent
npm run build
cd ..
echo "  ✓ 前端构建完成 -> frontend/dist/"

echo ""
echo "[2/3] 打包后端..."
cd backend
mvn clean package -DskipTests -q
cd ..
echo "  ✓ 后端打包完成 -> backend/target/platform-0.0.1-SNAPSHOT.jar"

echo ""
echo "[3/3] 启动 Docker 服务..."
docker-compose down 2>/dev/null || true
docker-compose up -d --build
echo "  ✓ 服务启动完成"

echo ""
echo "=============================="
echo " 部署完成！"
echo " 访问地址: http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'localhost')"
echo " 管理员账号: admin / admin123"
echo " 演示企业账号: jingzhou_tech / demo123"
echo "=============================="

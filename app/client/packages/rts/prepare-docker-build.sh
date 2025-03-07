#!/usr/bin/env bash

set -o errexit

cd "$(dirname "$0")"

echo "检查dist目录是否存在..."
if [ ! -d "dist" ]; then
  echo "dist目录不存在，请先构建RTS服务"
  echo "运行以下命令构建RTS服务："
  echo "  cd $(pwd)"
  echo "  yarn build"
  exit 1
fi

echo "检查node_modules目录是否存在..."
if [ ! -d "node_modules" ]; then
  echo "node_modules目录不存在，请先安装依赖"
  echo "运行以下命令安装依赖："
  echo "  cd $(pwd)"
  echo "  yarn install"
  exit 1
fi

echo "检查start-server.sh文件是否存在..."
if [ ! -f "start-server.sh" ]; then
  echo "start-server.sh文件不存在，创建一个简单的启动脚本"
  cat > start-server.sh << 'EOF'
#!/bin/sh
set -e
exec node --require source-map-support/register dist/bundle/server.js
EOF
  chmod +x start-server.sh
fi

echo "检查.env文件是否存在..."
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
  echo "使用.env.example作为.env文件"
  cp .env.example .env
fi

echo "准备工作完成，现在可以构建Docker镜像了"
echo "运行以下命令构建Docker镜像："
echo "  docker build -t appsmith-rts -f Dockerfile.minimal ." 
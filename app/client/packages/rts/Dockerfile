FROM node:18-alpine

WORKDIR /app

# 安装构建依赖
RUN apk add --no-cache bash

# 复制package.json和yarn.lock
COPY package.json ./
COPY .env.example ./.env

# 复制源代码和构建脚本
COPY src/ ./src/
COPY tsconfig.json ./
COPY build.js ./
COPY build.sh ./
COPY start-server.sh ./
COPY node_modules/ ./node_modules/
COPY dist/ ./dist/
COPY jest.config.js ./
COPY .eslintrc.json ./
COPY .lintstagedrc.json ./
# 设置权限
RUN chmod +x ./build.sh ./start-server.sh

# 构建应用
#RUN ./build.sh

# 暴露端口
EXPOSE 8091

# 启动服务
CMD ["./start-server.sh"] 
# ==================== Stage 1: 构建前端 ====================
FROM node:22-alpine AS builder

WORKDIR /app

# 优先复制依赖文件，利用 Docker 缓存层
COPY package.json pnpm-lock.yaml ./

# 启用 corepack 并安装依赖
RUN corepack enable && pnpm install --frozen-lockfile

# 复制源码并执行生产构建
COPY . .
RUN pnpm build

# ==================== Stage 2: 生产服务 ====================
FROM nginx:1.27-alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

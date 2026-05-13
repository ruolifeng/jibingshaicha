# ==================== Stage 1: Maven 编译 ====================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 优先复制 pom.xml，缓存依赖下载层
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# 复制源码并打包（跳过测试）
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ==================== Stage 2: 生产运行 ====================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装 mysql-client（备份功能通过 mysqldump 命令行工具导出数据库）
RUN apk add --no-cache mysql-client

# 创建上传/备份文件存储目录
RUN mkdir -p /app/uploads /app/backups

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 9652

ENTRYPOINT ["java", "-jar", "app.jar"]

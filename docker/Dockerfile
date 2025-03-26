# 1. 选择基础镜像（带 JDK 的镜像）
FROM openjdk:17-jdk-slim

# 2. 设置工作目录
WORKDIR /app

# 3. 复制 Spring Boot JAR 文件到容器
COPY target/*.jar app.jar

# 4. 运行 Spring Boot 应用mv
CMD ["java", "-jar", "app.jar"]

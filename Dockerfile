# ==============================================================================
# Dockerfile - ThinkAI Backend
# ==============================================================================
# Mục đích: Build Spring Boot application thành Docker image
# Cách build: docker build -t minhtuyetvoi/thinkai-backend:latest .
# ==============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build - Biên dịch ứng dụng
# -----------------------------------------------------------------------------
# Sử dụng Maven + JDK 21 làm base image để build
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Thư mục làm việc trong container
WORKDIR /app

# Copy Maven wrapper và pom.xml trước để tận dụng cache
# Nếu pom.xml không đổi, Docker sẽ reuse layer này
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download tất cả dependencies trước (cache cho lần build sau)
RUN mvn dependency:go-offline -B

# Copy source code vào
COPY src ./src

# Build ứng dụng thành file .jar
# Kết quả: target/*.jar
RUN mvn package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 2: Runtime - Môi trường chạy ứng dụng
# -----------------------------------------------------------------------------
# Sử dụng JRE 21 Alpine (image nhẹ, chỉ chạy không build)
FROM eclipse-temurin:21-jre-alpine

# Thư mục làm việc
WORKDIR /app

# Copy file .jar từ stage 1 (builder) vào stage 2
# Chỉ copy file .jar, không copy source code
COPY --from=builder /app/target/*.jar app.jar

# Khai báo port mà ứng dụng sẽ lắng nghe
# Phải khớp với server.port trong application.yml
EXPOSE 8081

# Lệnh chạy khi container khởi động
# Container nhận biến môi trường từ bên ngoài (docker-compose, Railway,...)
ENTRYPOINT ["java", "-jar", "app.jar"]

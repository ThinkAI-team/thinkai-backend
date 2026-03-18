# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first to cache dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source and build jar
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Port must match server.port in application.yml
EXPOSE 8081

# Environment variables will be injected via docker-compose
ENTRYPOINT ["java", "-jar", "app.jar"]

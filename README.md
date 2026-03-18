# ThinkAI Backend

> Spring Boot backend API cho nền tảng học trực tuyến ThinkAI.

## 📋 Yêu cầu

| Công cụ      | Phiên bản | Kiểm tra                  |
| -------------- | ----------- | -------------------------- |
| Docker         | ≥ 20.x     | `docker --version`       |
| Docker Compose | ≥ 2.x      | `docker compose version` |
| Java           | ≥ 21       | `java -version`          |

## 🚀 Quick Start

```bash
# 1. Clone project
git clone <repository-url>
cd thinkai-backend

# 2. Chạy toàn bộ hệ thống (DB + Backend)
docker compose up --build -d

# 3. Theo dõi logs
docker compose logs -f thinkai-backend
```

Dịch vụ sẽ sẵn sàng tại: `http://localhost:8081`

### Các lệnh hữu ích khác
- `docker compose down`: Dừng hệ thống nhưng giữ lại dữ liệu DB.
- `docker compose down -v`: Reset hoàn toàn hệ thống và xóa dữ liệu DB.
- `docker compose restart thinkai-api`: Restart riêng Backend (sau khi sửa code).
# 3. Chờ MySQL sẵn sàng (~10 giây)
docker logs thinkai-mysql --tail 10

# 4. Chạy Spring Boot
./mvnw spring-boot:run
```

App sẽ chạy tại: `http://localhost:8080`

## 🗄️ Database

- **Host**: localhost
- **Port**: 3306
- **Database**: thinkai_db
- **Username**: root
- **Password**: root

### Truy cập MySQL CLI

```bash
docker exec -it thinkai-mysql mysql -uroot -proot thinkai_db
```

## 📁 Cấu trúc Project

```
src/main/java/com/thinkai/backend/
├── entity/          # JPA Entities (15 files)
├── repository/      # Data Access Layer (coming soon)
├── service/         # Business Logic (coming soon)
├── controller/      # REST API (coming soon)
└── DemoApplication.java
```

## 🔧 Configuration

| Variable     | File                       | Mô tả                       |
| ------------ | -------------------------- | ----------------------------- |
| Database URL | `application.properties` | JDBC connection string        |
| JPA DDL Auto | `application.properties` | `update` (auto sync schema) |

## ⚠️ Troubleshooting

### Port 3306 đã bị chiếm

```bash
# Kiểm tra process đang dùng port
sudo lsof -i :3306

# Hoặc đổi port trong docker-compose.yml
ports:
  - "3307:3306"  # Đổi sang 3307
```

### Permission denied khi chạy mvnw

```bash
chmod +x mvnw
```

### MySQL chưa sẵn sàng

```bash
# Kiểm tra container status
docker ps | grep thinkai-mysql

# Xem logs
docker logs thinkai-mysql
```

## 📚 Documentation

- [Database Schema](../thinkai-docs/DB_Scheme.md)
- [API Specification](../thinkai-docs/API_SPEC.md)
- [Architecture](../thinkai-docs/Architecture.md)

## 🛠️ Tech Stack

- **Framework**: Spring Boot 4.0.2
- **Database**: MySQL 8.0
- **ORM**: Hibernate / JPA
- **Build**: Maven
- **Container**: Docker

## 📄 License

Private - ThinkAI Team

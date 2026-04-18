# ThinkAI VPS Deploy (Backend + MySQL + Redis)

Mục tiêu: thực thi nhanh 5 cải tiến hiệu năng sau khi chuyển từ Aiven MySQL sang MySQL Docker.

## 1) Giảm latency DB (đưa DB về nội bộ Docker)

File compose: `docker-compose.mysql-redis.yml` đã cấu hình backend dùng DB nội bộ:

- `DB_URL=jdbc:mysql://thinkai-db:3306/...`
- Redis nội bộ: `SPRING_DATA_REDIS_HOST=thinkai-redis`

Không dùng `localhost` trong `DB_URL` khi backend chạy trong container.

## 2) Tune pool kết nối (Hikari)

Đã set sẵn trong compose:

- `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10`
- `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=2`
- `SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000`
- `SPRING_DATASOURCE_HIKARI_VALIDATION_TIMEOUT=5000`
- `SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=1200000`
- `SPRING_JPA_OPEN_IN_VIEW=false`

## 3) Tạo index cho query nóng

SQL idempotent: `sql/01_add_performance_indexes.sql`

Apply:

```bash
docker exec -i thinkai-db mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < deploy/vps/sql/01_add_performance_indexes.sql
```

## 4) Bật theo dõi query chậm + tài nguyên

Đã bật `slow query log` trong MySQL command:

- `--slow_query_log=1`
- `--long_query_time=1`
- `--slow_query_log_file=/var/lib/mysql/slow.log`

Lệnh theo dõi:

```bash
docker stats
docker exec -it thinkai-db sh -c "tail -n 200 /var/lib/mysql/slow.log"
docker logs --tail 200 thinkai-backend
```

## 5) Redis cache cho read-heavy endpoint

Redis đã có trong stack (`thinkai-redis`) và backend đã trỏ nội bộ.
Sau khi app ổn định, ưu tiên cache các endpoint đọc nhiều (vd dashboard, unread count, list công khai), tránh cache endpoint ghi.

---

## Quick start

1. Tạo `.env` từ mẫu:

```bash
cp deploy/vps/.env.example .env
```

2. Chạy stack:

```bash
docker compose -f deploy/vps/docker-compose.mysql-redis.yml --env-file .env up -d
```

3. Kiểm tra health:

```bash
docker compose -f deploy/vps/docker-compose.mysql-redis.yml ps
docker logs --tail 100 thinkai-backend
```

4. Nếu cần import dump cũ:

```bash
docker exec -i thinkai-db mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < backup_defaultdb.sql
```

5. Apply performance indexes:

```bash
docker exec -i thinkai-db mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < deploy/vps/sql/01_add_performance_indexes.sql
```

## Rollback nhanh về backend cũ

Chỉ cần dừng stack hiện tại và chạy lại container backend đang dùng trước đó:

```bash
docker compose -f deploy/vps/docker-compose.mysql-redis.yml down
```

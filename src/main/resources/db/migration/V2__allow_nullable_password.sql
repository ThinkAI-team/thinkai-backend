-- Migration: Cho phép password_hash nullable để hỗ trợ Google OAuth users
-- Google users đăng ký không có password, nên cột này phải cho phép NULL

ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL;

-- Test data cho Course Detail API
INSERT IGNORE INTO courses (id, title, description, thumbnail_url, price, instructor_id, is_published, status, created_at, updated_at)
VALUES (1, 'TOEIC Beginner 450+', 'Khóa học luyện thi TOEIC cơ bản từ 0 đến 450+. Bao gồm Listening và Reading.', 'https://example.com/toeic.jpg', 299000, 1, true, 'APPROVED', NOW(), NOW());

INSERT IGNORE INTO lessons (id, course_id, title, type, content_url, duration_seconds, order_index, created_at, updated_at) VALUES
(1, 1, 'Bài 1: Giới thiệu TOEIC', 'VIDEO', 'https://example.com/vid1.mp4', 630, 1, NOW(), NOW()),
(2, 1, 'Bài 2: Listening Part 1 - Photographs', 'VIDEO', 'https://example.com/vid2.mp4', 1200, 2, NOW(), NOW()),
(3, 1, 'Bài 3: Tài liệu Reading Comprehension', 'PDF', 'https://example.com/doc.pdf', 0, 3, NOW(), NOW());

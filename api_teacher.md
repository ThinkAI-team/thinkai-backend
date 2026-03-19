# Hướng Dẫn Test Postman - Teacher Portal APIs

Tài liệu này hướng dẫn cách test các API của Module Teacher Portal trên Postman.

## 1. Môi trường & Authentication
- **Base URL:** `http://localhost:8080/api` (hoặc port bạn đang chạy)
- **Authentication:** Các API này yêu cầu Header `Authorization: Bearer <your_jwt_token>`
  - Đảm bảo tài khoản đăng nhập có role `TEACHER` hoặc `ADMIN` để có quyền truy cập.

---

## 2. API Dashboard Giảng Viên

### GET `/teacher/dashboard`
- **Mục đích:** Lấy thống kê tổng quan (khóa học, học viên, tỷ lệ).
- **Header:** `Authorization: Bearer <token>`
- **Body:** Không có.
- **Expected Response (200 OK):**
```json
{
    "totalCourses": 5,
    "totalStudents": 120,
    "completedStudents": 45,
    "completionRate": 37.5
}
```

---

## 3. Khóa Học (Course Management)

### POST `/teacher/courses` (Tạo Khóa Học)
- **Header:** `Content-Type: application/json`, `Authorization: Bearer <token>`
- **Body 1 (Khóa học TOEIC):**
```json
{
    "title": "TOEIC 700+ Mastery",
    "description": "Khóa học luyện thi TOEIC chuyên sâu trong 2 tháng.",
    "thumbnailUrl": "https://example.com/toeic.png",
    "price": 500000
}
```
- **Body 2 (Khóa học IELTS):**
```json
{
    "title": "IELTS 6.5 Intensive",
    "description": "Luyện thi IELTS tập trung 4 kỹ năng trong 3 tháng.",
    "thumbnailUrl": "https://example.com/ielts.png",
    "price": 1200000
}
```
- **Expected Response:** 201 Created

### GET `/teacher/courses` (Danh sách khóa học của GV)
- **QueryParams:** `?page=0&size=10`
- **Expected Response (200 OK):** Trả về Pageable object các Course.

### PUT `/teacher/courses/{id}` (Cập nhật Khóa Học)
- Tương tự như POST, dùng để cập nhật.

### PUT `/teacher/courses/{id}/publish` (Publish Khóa Học)
- **Header:** `Authorization: Bearer <token>`
- **Mục đích:** Chuyển trạng thái khóa học từ DRAFT sang PENDING (chờ duyệt).
- **Body:** Không có.

---

## 4. Bài Học (Lesson Management)

### POST `/teacher/courses/{courseId}/lessons` (Tạo bài học mới)
- **Header:** `Content-Type: application/json`
- **Body 1 (Video Lesson):**
```json
{
    "title": "Bài 1: Thì Hiện Tại Đơn",
    "type": "VIDEO",
    "durationSeconds": 600,
    "orderIndex": 1
}
```
- **Body 2 (Video Lesson 2):**
```json
{
    "title": "Bài 2: Hiện Tại Tiếp Diễn",
    "type": "VIDEO",
    "durationSeconds": 1200,
    "orderIndex": 2
}
```
- **Body 3 (PDF Lesson):**
```json
{
    "title": "Tài liệu Từ Vựng (PDF)",
    "type": "PDF",
    "contentText": "Vui lòng xem 100 từ vựng TOEIC cơ bản ở file đính kèm.",
    "orderIndex": 3
}
```

### POST `/teacher/courses/{courseId}/lessons/upload` (Upload File)
- **Header:** `Authorization: Bearer <token>`
- **Body (form-data):**
  - Key: `file`, Type: File, Value: Chọn một file (PDF, MP4)
- **Expected Response (200 OK):** Trả về link file đã lưu.

### PUT `/teacher/courses/{courseId}/lessons/order` (Sắp Xếp)
- **Body (JSON):**
```json
{
    "lessonOrders": [
        { "lessonId": 1, "orderIndex": 0 },
        { "lessonId": 5, "orderIndex": 1 }
    ]
}
```

---

## 5. Ngân Hàng Câu Hỏi (Question Bank)

### POST `/teacher/questions/import` (Import CSV)
- **Body (form-data):**
  - Key: `file`, Type: File, Value: (File CSV mẫu: `examType,section,part,content,options,correctAnswer,difficulty`)

### POST `/teacher/questions` (Tạo Câu Hỏi Thủ Công)
- **Body 1 (Câu hỏi Reading):**
```json
{
    "examType": "TOEIC",
    "section": "READING",
    "part": "PART_5",
    "content": "The manager _____ to the meeting yesterday.",
    "options": "[\"go\", \"goes\", \"went\", \"gone\"]",
    "correctAnswer": "went",
    "difficulty": "EASY",
    "tags": ["grammar", "past-tense"]
}
```
- **Body 2 (Câu hỏi Listening):**
```json
{
    "examType": "TOEIC",
    "section": "LISTENING",
    "part": "PART_2",
    "content": "(Audio Plays) Where is the nearest ATM?",
    "options": "[\"A. At 5 PM.\", \"B. Just around the corner.\", \"C. I don't need cash.\"]",
    "correctAnswer": "B. Just around the corner.",
    "difficulty": "EASY",
    "audioUrl": "https://example.com/audio/q2.mp3",
    "tags": ["listening", "location"]
}
```

---

## 6. Bài Thi (Exam Management)

### POST `/teacher/exams`
- **Body 1 (Đề thi TOEIC):**
```json
{
    "courseId": 1,
    "title": "Mock Test TOEIC 01",
    "examType": "TOEIC",
    "description": "Bài thi thử TOEIC Format mới",
    "timeLimitMinutes": 120,
    "passingScore": 60,
    "isRandomOrder": true,
    "partConfig": {
        "PART_1": 6,
        "PART_2": 25,
        "PART_5": 30
    }
}
```
- **Body 2 (Đề thi IELTS Mini):**
```json
{
    "courseId": 2,
    "title": "IELTS Reading Mock 01",
    "examType": "IELTS",
    "description": "Bài kiểm tra kỹ năng đọc IELTS",
    "timeLimitMinutes": 60,
    "passingScore": 25,
    "isRandomOrder": false,
    "partConfig": {
        "PART_1": 13,
        "PART_2": 13,
        "PART_3": 14
    }
}
```

### GET `/teacher/exams`
- Query Params: `?page=0&size=10`
- Trả về danh sách bài thi đã tạo bởi giáo viên.



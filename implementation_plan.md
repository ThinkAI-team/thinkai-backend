# Fix Exam Creation Logic

Hi! Như giải thích của mình, lỗi Bài thi đang rỗng là do Backend thiếu phần kết nối vào kho câu hỏi. Đây là kế hoạch sửa lỗi chi tiết:

## Các thay đổi đề xuất

### 1. [QuestionBankRepository](file:///d:/Hoc_Tap/Do_an/thinkai-backend/src/main/java/com/thinkai/backend/repository/QuestionBankRepository.java#13-24)
Thêm hàm để truy vấn câu hỏi từ kho dễ dàng hơn:
#### [MODIFY] [QuestionBankRepository.java](file:///d:/Hoc_Tap/Do_an/thinkai-backend/src/main/java/com/thinkai/backend/repository/QuestionBankRepository.java)
- Thêm method `List<QuestionBank> findByExamTypeAndPart(ExamType examType, Part part);`

### 2. [ExamService](file:///d:/Hoc_Tap/Do_an/thinkai-backend/src/main/java/com/thinkai/backend/service/ExamService.java#38-440)
Sửa đổi hàm [createExam](file:///d:/Hoc_Tap/Do_an/thinkai-backend/src/main/java/com/thinkai/backend/controller/TeacherExamController.java#33-39) để đọc thông số của giao diện gửi xuống và bốc câu hỏi từ [QuestionBank](file:///d:/Hoc_Tap/Do_an/thinkai-backend/src/main/java/com/thinkai/backend/entity/QuestionBank.java#10-74) nạp vào bài thi.
#### [MODIFY] [ExamService.java](file:///d:/Hoc_Tap/Do_an/thinkai-backend/src/main/java/com/thinkai/backend/service/ExamService.java)
- Import thêm các class cần thiết: `java.util.Collections`, `com.thinkai.backend.entity.enums.Part`, `com.fasterxml.jackson.databind.ObjectMapper`.
- Map `partConfig` từ Request và lưu JSON vào Entity [Exam](file:///d:/Hoc_Tap/Do_an/thinkai-backend/src/main/java/com/thinkai/backend/controller/TeacherExamController.java#40-46).
- Nếu có `partConfig` (vd: `{"PART_5": 10}`), duyệt từng Part, query tới Kho câu hỏi thông qua `findByExamTypeAndPart`.
- Trộn ngẫu nhiên câu hỏi (shuffle).
- Lưu các câu hỏi được chọn vào bảng [Question](file:///d:/Hoc_Tap/Do_an/thinkai-backend/src/main/java/com/thinkai/backend/entity/Question.java#6-44) với `examId` của bài thi vừa tạo mới.

## User Review Required
> [!IMPORTANT]
> Đây là thay đổi can thiệp trực tiếp vào Core Logic phần Bài thi để sửa hoàn toàn lỗi bạn gặp. Bạn vui lòng xem qua và cho mình biết nếu bạn đồng ý chạy Plan này!

## Verification Plan
### Automated Tests
- Khởi động lại Spring Boot backend.
### Manual Verification
- Lên lại phần "Quản lý bài thi", thiết lập partConfig = `{"PART_5": 5}`.
- Bấm "TẠO BÀI THI".
- Vào màn hình thi thử (Student/Luyện thi) và chọn bài thi để xem câu hỏi đã load thành công hay chưa.

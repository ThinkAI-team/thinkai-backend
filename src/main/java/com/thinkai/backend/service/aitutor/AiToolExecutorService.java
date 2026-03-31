package com.thinkai.backend.service.aitutor;

import com.thinkai.backend.entity.*;
import com.thinkai.backend.repository.*;
import com.thinkai.backend.service.CourseService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Service
public class AiToolExecutorService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final ExamRepository examRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final ObjectMapper objectMapper;

    public AiToolExecutorService(
            CourseRepository courseRepository,
            LessonRepository lessonRepository,
            ExamRepository examRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            CourseService courseService,
            ObjectMapper objectMapper) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.examRepository = examRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.objectMapper = objectMapper;
    }

    public ToolExecuteResult execute(String action, JsonNode args, User user) {
        return switch (action) {
            case "enroll_course" -> executeEnrollCourse(args, user);
            case "update_course" -> executeUpdateCourse(args, user);
            case "delete_course" -> executeDeleteCourse(args, user);
            case "create_course" -> executeCreateCourse(args, user);
            case "update_lesson" -> executeUpdateLesson(args, user);
            case "delete_lesson" -> executeDeleteLesson(args, user);
            case "create_lesson" -> executeCreateLesson(args, user);
            case "update_exam" -> executeUpdateExam(args, user);
            case "delete_exam" -> executeDeleteExam(args, user);
            case "create_exam" -> executeCreateExam(args, user);
            case "list_teacher_courses" -> executeListTeacherCourses(args, user);
            case "get_course_analytics" -> executeGetCourseAnalytics(args, user);
            case "create_question" -> executeCreateQuestion(args, user);
            case "bulk_import_questions" -> executeBulkImportQuestions(args, user);
            default -> ToolExecuteResult.failure("Action not supported: " + action);
        };
    }

    private ToolExecuteResult executeEnrollCourse(JsonNode args, User user) {
        if (user.getRole() != User.Role.STUDENT) {
            return ToolExecuteResult.failure("Chỉ STUDENT mới có thể đăng ký khóa học.");
        }

        Long courseId = extractLong(args, "courseId");
        if (courseId == null) {
            return ToolExecuteResult.needInfo("enroll_course", "Thiếu courseId để đăng ký.");
        }

        Optional<Course> optCourse = courseRepository.findById(courseId);
        if (optCourse.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy khóa học với courseId=" + courseId);
        }

        Course course = optCourse.get();
        if (!Boolean.TRUE.equals(course.getIsPublished())) {
            return ToolExecuteResult.failure("Khóa học này chưa được xuất bản.");
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            return ToolExecuteResult.failure("Bạn đã đăng ký khóa học này rồi.");
        }

        Enrollment enrollment = Enrollment.builder()
                .userId(user.getId())
                .courseId(courseId)
                .progressPercent(0)
                .build();
        enrollmentRepository.save(enrollment);

        return ToolExecuteResult.success("enroll_course", "Đăng ký khóa học thành công! Course: " + course.getTitle());
    }

    private ToolExecuteResult executeUpdateCourse(JsonNode args, User user) {
        Long courseId = extractLong(args, "courseId");
        if (courseId == null) {
            return ToolExecuteResult.needInfo("update_course", "Thiếu courseId để cập nhật.");
        }

        Optional<Course> optCourse = courseRepository.findById(courseId);
        if (optCourse.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy khóa học với courseId=" + courseId);
        }

        Course course = optCourse.get();
        if (!hasOwnership(course, user)) {
            return ToolExecuteResult.failure("Bạn không sở hữu khóa học này.");
        }

        if (args.has("title") && !args.get("title").isNull()) {
            course.setTitle(args.get("title").asText());
        }
        if (args.has("description") && !args.get("description").isNull()) {
            course.setDescription(args.get("description").asText());
        }
        if (args.has("price") && !args.get("price").isNull()) {
            course.setPrice(new java.math.BigDecimal(args.get("price").asText()));
        }
        if (args.has("thumbnailUrl") && !args.get("thumbnailUrl").isNull()) {
            course.setThumbnailUrl(args.get("thumbnailUrl").asText());
        }

        courseRepository.save(course);
        return ToolExecuteResult.success("update_course", "Đã cập nhật khóa học courseId=" + courseId);
    }

    private ToolExecuteResult executeDeleteCourse(JsonNode args, User user) {
        Long courseId = extractLong(args, "courseId");
        if (courseId == null) {
            return ToolExecuteResult.needInfo("delete_course", "Thiếu courseId để xóa.");
        }

        Optional<Course> optCourse = courseRepository.findById(courseId);
        if (optCourse.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy khóa học với courseId=" + courseId);
        }

        Course course = optCourse.get();
        if (!hasOwnership(course, user)) {
            return ToolExecuteResult.failure("Bạn không sở hữu khóa học này.");
        }

        courseRepository.delete(course);
        return ToolExecuteResult.success("delete_course", "Đã xóa khóa học courseId=" + courseId);
    }

    private ToolExecuteResult executeCreateCourse(JsonNode args, User user) {
        if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
            return ToolExecuteResult.failure("Bạn cần quyền TEACHER hoặc ADMIN để tạo khóa học.");
        }

        String title = extractText(args, "title");
        if (title == null || title.isBlank()) {
            return ToolExecuteResult.needInfo("create_course", "Thiếu title cho khóa học.");
        }

        String description = extractText(args, "description");
        if (description == null) description = "";

        java.math.BigDecimal price = java.math.BigDecimal.ZERO;
        if (args.has("price") && !args.get("price").isNull()) {
            try {
                price = new java.math.BigDecimal(args.get("price").asText());
            } catch (Exception ignored) {}
        }

        Course course = Course.builder()
                .title(title)
                .description(description)
                .price(price)
                .instructorId(user.getId())
                .isPublished(false)
                .status(Course.Status.DRAFT)
                .build();
        course = courseRepository.save(course);
        return ToolExecuteResult.success("create_course", "Đã tạo khóa học thành công! ID: " + course.getId());
    }

    private ToolExecuteResult executeCreateLesson(JsonNode args, User user) {
        Long courseId = extractLong(args, "courseId");
        if (courseId == null) {
            return ToolExecuteResult.needInfo("create_lesson", "Thiếu courseId cho bài học.");
        }

        Optional<Course> optCourse = courseRepository.findById(courseId);
        if (optCourse.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy khóa học với courseId=" + courseId);
        }

        Course course = optCourse.get();
        if (!hasOwnership(course, user)) {
            return ToolExecuteResult.failure("Bạn không sở hữu khóa học này.");
        }

        String title = extractText(args, "title");
        if (title == null || title.isBlank()) {
            return ToolExecuteResult.needInfo("create_lesson", "Thiếu title cho bài học.");
        }

        long maxOrder = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
                .stream().mapToLong(l -> l.getOrderIndex()).max().orElse(0);

        Lesson lesson = Lesson.builder()
                .courseId(courseId)
                .title(title)
                .type(Lesson.LessonType.VIDEO)
                .orderIndex((int) (maxOrder + 1))
                .build();
        lesson = lessonRepository.save(lesson);
        return ToolExecuteResult.success("create_lesson", "Đã tạo bài học thành công! ID: " + lesson.getId());
    }

    private ToolExecuteResult executeCreateExam(JsonNode args, User user) {
        Long courseId = extractLong(args, "courseId");
        if (courseId == null) {
            return ToolExecuteResult.needInfo("create_exam", "Thiếu courseId cho bài thi.");
        }

        Optional<Course> optCourse = courseRepository.findById(courseId);
        if (optCourse.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy khóa học với courseId=" + courseId);
        }

        Course course = optCourse.get();
        if (!hasOwnership(course, user)) {
            return ToolExecuteResult.failure("Bạn không sở hữu khóa học này.");
        }

        String title = extractText(args, "title");
        if (title == null || title.isBlank()) {
            return ToolExecuteResult.needInfo("create_exam", "Thiếu title cho bài thi.");
        }

        String examTypeStr = extractText(args, "examType");
        com.thinkai.backend.entity.enums.ExamType examType = com.thinkai.backend.entity.enums.ExamType.TOEIC;
        if (examTypeStr != null && examTypeStr.toUpperCase().contains("IELTS")) {
            examType = com.thinkai.backend.entity.enums.ExamType.IELTS;
        }

        Exam exam = Exam.builder()
                .courseId(courseId)
                .title(title)
                .examType(examType)
                .createdBy(user.getId())
                .build();
        exam = examRepository.save(exam);
        return ToolExecuteResult.success("create_exam", "Đã tạo bài thi thành công! ID: " + exam.getId());
    }

    private ToolExecuteResult executeUpdateLesson(JsonNode args, User user) {
        Long lessonId = extractLong(args, "lessonId");
        if (lessonId == null) {
            return ToolExecuteResult.needInfo("update_lesson", "Thiếu lessonId để cập nhật.");
        }

        Optional<Lesson> optLesson = lessonRepository.findById(lessonId);
        if (optLesson.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy bài học với lessonId=" + lessonId);
        }

        Lesson lesson = optLesson.get();
        Optional<Course> optCourse = courseRepository.findById(lesson.getCourseId());
        if (optCourse.isEmpty() || !hasOwnership(optCourse.get(), user)) {
            return ToolExecuteResult.failure("Bạn không sở hữu khóa học chứa bài học này.");
        }

        if (args.has("title") && !args.get("title").isNull()) {
            lesson.setTitle(args.get("title").asText());
        }
        if (args.has("contentText") && !args.get("contentText").isNull()) {
            lesson.setContentText(args.get("contentText").asText());
        }
        if (args.has("contentUrl") && !args.get("contentUrl").isNull()) {
            lesson.setContentUrl(args.get("contentUrl").asText());
        }
        if (args.has("durationSeconds") && !args.get("durationSeconds").isNull()) {
            lesson.setDurationSeconds(args.get("durationSeconds").asInt());
        }
        if (args.has("orderIndex") && !args.get("orderIndex").isNull()) {
            lesson.setOrderIndex(args.get("orderIndex").asInt());
        }

        lessonRepository.save(lesson);
        return ToolExecuteResult.success("update_lesson", "Đã cập nhật bài học lessonId=" + lessonId);
    }

    private ToolExecuteResult executeDeleteLesson(JsonNode args, User user) {
        Long lessonId = extractLong(args, "lessonId");
        if (lessonId == null) {
            return ToolExecuteResult.needInfo("delete_lesson", "Thiếu lessonId để xóa.");
        }

        Optional<Lesson> optLesson = lessonRepository.findById(lessonId);
        if (optLesson.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy bài học với lessonId=" + lessonId);
        }

        Lesson lesson = optLesson.get();
        Optional<Course> optCourse = courseRepository.findById(lesson.getCourseId());
        if (optCourse.isEmpty() || !hasOwnership(optCourse.get(), user)) {
            return ToolExecuteResult.failure("Bạn không sở hữu khóa học chứa bài học này.");
        }

        lessonRepository.delete(lesson);
        return ToolExecuteResult.success("delete_lesson", "Đã xóa bài học lessonId=" + lessonId);
    }

    private ToolExecuteResult executeUpdateExam(JsonNode args, User user) {
        Long examId = extractLong(args, "examId");
        if (examId == null) {
            return ToolExecuteResult.needInfo("update_exam", "Thiếu examId để cập nhật.");
        }

        Optional<Exam> optExam = examRepository.findById(examId);
        if (optExam.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy bài thi với examId=" + examId);
        }

        Exam exam = optExam.get();
        Optional<Course> optCourse = courseRepository.findById(exam.getCourseId());
        if (optCourse.isEmpty() || !hasOwnership(optCourse.get(), user)) {
            return ToolExecuteResult.failure("Bạn không sở hữu khóa học chứa bài thi này.");
        }

        if (args.has("title") && !args.get("title").isNull()) {
            exam.setTitle(args.get("title").asText());
        }
        if (args.has("description") && !args.get("description").isNull()) {
            exam.setDescription(args.get("description").asText());
        }
        if (args.has("timeLimitMinutes") && !args.get("timeLimitMinutes").isNull()) {
            exam.setTimeLimitMinutes(args.get("timeLimitMinutes").asInt());
        }
        if (args.has("passingScore") && !args.get("passingScore").isNull()) {
            exam.setPassingScore(args.get("passingScore").asInt());
        }

        examRepository.save(exam);
        return ToolExecuteResult.success("update_exam", "Đã cập nhật bài thi examId=" + examId);
    }

    private ToolExecuteResult executeDeleteExam(JsonNode args, User user) {
        Long examId = extractLong(args, "examId");
        if (examId == null) {
            return ToolExecuteResult.needInfo("delete_exam", "Thiếu examId để xóa.");
        }

        Optional<Exam> optExam = examRepository.findById(examId);
        if (optExam.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy bài thi với examId=" + examId);
        }

        Exam exam = optExam.get();
        Optional<Course> optCourse = courseRepository.findById(exam.getCourseId());
        if (optCourse.isEmpty() || !hasOwnership(optCourse.get(), user)) {
            return ToolExecuteResult.failure("Bạn không sở hữu khóa học chứa bài thi này.");
        }

        examRepository.delete(exam);
        return ToolExecuteResult.success("delete_exam", "Đã xóa bài thi examId=" + examId);
    }

    private ToolExecuteResult executeListTeacherCourses(JsonNode args, User user) {
        List<Course> courses;
        if (user.getRole() == User.Role.ADMIN) {
            courses = courseRepository.findAll();
        } else {
            courses = courseRepository.findByInstructorId(user.getId());
        }

        if (courses.isEmpty()) {
            return ToolExecuteResult.success("list_teacher_courses", "Bạn chưa tạo khóa học nào.");
        }

        StringBuilder sb = new StringBuilder("Danh sách khóa học của bạn:\n");
        int index = 1;
        for (Course course : courses) {
            sb.append(index++).append(". [courseId=").append(course.getId()).append("] ")
                    .append(course.getTitle())
                    .append(" | published=").append(Boolean.TRUE.equals(course.getIsPublished()))
                    .append(" | status=").append(course.getStatus())
                    .append('\n');
        }
        return ToolExecuteResult.success("list_teacher_courses", sb.toString().trim());
    }

    private ToolExecuteResult executeGetCourseAnalytics(JsonNode args, User user) {
        Long courseId = extractLong(args, "courseId");
        if (courseId == null) {
            return ToolExecuteResult.needInfo("get_course_analytics", "Thiếu courseId để lấy thống kê.");
        }

        Optional<Course> optCourse = courseRepository.findById(courseId);
        if (optCourse.isEmpty()) {
            return ToolExecuteResult.failure("Không tìm thấy khóa học với courseId=" + courseId);
        }

        Course course = optCourse.get();
        if (!hasOwnership(course, user)) {
            return ToolExecuteResult.failure("Bạn không sở hữu khóa học này.");
        }

        long totalEnrollments = enrollmentRepository.countByCourseId(courseId);
        long totalLessons = lessonRepository.countByCourseId(courseId);

        return ToolExecuteResult.success("get_course_analytics",
                "Thống kê khóa học [" + course.getTitle() + "]:\n"
                        + "- Tổng học viên: " + totalEnrollments + "\n"
                        + "- Tổng bài học: " + totalLessons + "\n"
                        + "- Đã publish: " + Boolean.TRUE.equals(course.getIsPublished()));
    }

    private ToolExecuteResult executeCreateQuestion(JsonNode args, User user) {
        String questionText = extractText(args, "questionText");
        if (questionText == null || questionText.isBlank()) {
            return ToolExecuteResult.needInfo("create_question", "Thiếu questionText.");
        }

        String correctAnswer = extractText(args, "correctAnswer");
        if (correctAnswer == null || correctAnswer.isBlank()) {
            return ToolExecuteResult.needInfo("create_question", "Thiếu correctAnswer.");
        }

        return ToolExecuteResult.needInfo("create_question", 
                "Tạo câu hỏi yêu cầu nhiều thông tin hơn. Vui lòng sử dụng giao diện quản lý ngân hàng câu hỏi.");
    }

    private ToolExecuteResult executeBulkImportQuestions(JsonNode args, User user) {
        Long courseId = extractLong(args, "courseId");
        if (courseId == null) {
            return ToolExecuteResult.needInfo("bulk_import_questions", "Thiếu courseId.");
        }

        return ToolExecuteResult.needInfo("bulk_import_questions", 
                "Import câu hỏi hàng loạt yêu cầu file JSON/Excel. Vui lòng sử dụng giao diện quản lý.");
    }

    private boolean hasOwnership(Course course, User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return true;
        }
        return course.getInstructorId() != null && course.getInstructorId().equals(user.getId());
    }

    private Long extractLong(JsonNode args, String field) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return null;
        }
        JsonNode node = args.get(field);
        if (node.isNumber()) {
            return node.asLong();
        }
        try {
            return Long.parseLong(node.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private String extractText(JsonNode args, String field) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return null;
        }
        return args.get(field).asText();
    }

    public static class ToolExecuteResult {
        private final boolean success;
        private final boolean needsMoreInfo;
        private final String action;
        private final String message;

        private ToolExecuteResult(boolean success, boolean needsMoreInfo, String action, String message) {
            this.success = success;
            this.needsMoreInfo = needsMoreInfo;
            this.action = action;
            this.message = message;
        }

        public static ToolExecuteResult success(String action, String message) {
            return new ToolExecuteResult(true, false, action, message);
        }

        public static ToolExecuteResult failure(String message) {
            return new ToolExecuteResult(false, false, "error", message);
        }

        public static ToolExecuteResult needInfo(String action, String message) {
            return new ToolExecuteResult(false, true, action, message);
        }

        public boolean isSuccess() { return success; }
        public boolean isNeedsMoreInfo() { return needsMoreInfo; }
        public String getAction() { return action; }
        public String getMessage() { return message; }
    }
}
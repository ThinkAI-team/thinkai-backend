package com.thinkai.backend.ai.tool;

import com.thinkai.backend.entity.AiSettings;
import com.thinkai.backend.entity.Cart;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.entity.ExamAttempt;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.LessonProgress;
import com.thinkai.backend.entity.Payment;
import com.thinkai.backend.entity.Question;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.repository.AiSettingsRepository;
import com.thinkai.backend.repository.CartRepository;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.ExamAttemptRepository;
import com.thinkai.backend.repository.ExamRepository;
import com.thinkai.backend.repository.LessonProgressRepository;
import com.thinkai.backend.repository.LessonRepository;
import com.thinkai.backend.repository.PaymentRepository;
import com.thinkai.backend.repository.QuestionRepository;
import com.thinkai.backend.repository.UserMemoryRepository;
import com.thinkai.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@SuppressWarnings("checkstyle:ConstantName")
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    // Tool permissions by role
    private static final Set<String> STUDENT_TOOLS = Set.of(
        // User info
        "get_user_level", "get_user_profile", "get_user_settings",
        // Learning
        "get_user_progress", "get_user_vocab_progress", "get_user_exam_history",
        "get_enrolled_courses", "get_lesson_detail", "get_course_detail",
        "get_my_exams", "get_my_certificates", "get_daily_streak",
        // Browse
        "list_shop_courses", "search_courses", "search_shop_courses", "list_course_lessons",
        "search_lessons", "get_grammar_topic", "search_vocabulary", "get_course_info", "get_exam_info",
        "list_my_courses", "list_my_learning_progress", "list_my_exams",
        // Actions
        "start_lesson", "complete_lesson", "enroll_course", "unenroll_course",
        "add_to_cart", "view_cart", "checkout"
    );

    private static final Set<String> TEACHER_TOOLS = Set.of(
        // All student tools
        "get_user_level", "get_user_profile", "get_user_settings",
        "get_user_progress", "get_user_vocab_progress", "get_user_exam_history",
        "get_enrolled_courses", "get_lesson_detail", "get_course_detail",
        "get_my_exams", "get_my_certificates", "get_daily_streak",
        "list_shop_courses", "search_courses", "search_shop_courses", "list_course_lessons",
        "search_lessons", "get_grammar_topic", "search_vocabulary", "get_course_info", "get_exam_info",
        "list_my_courses", "list_my_learning_progress", "list_my_exams",
        "start_lesson", "complete_lesson", "enroll_course", "unenroll_course",
        "add_to_cart", "view_cart", "checkout",
        // Teacher tools
        "get_my_courses", "list_teacher_courses", "get_course_students", "get_students_in_course", "get_course_analytics",
        "get_student_progress", "get_all_lessons",
        "create_course", "update_course", "publish_course",
        "create_lesson", "update_lesson", "delete_lesson",
        "create_exam", "update_exam", "publish_exam",
        "create_question", "bulk_import_questions",
        "get_student_results", "get_exam_results"
    );

    private static final Set<String> ADMIN_TOOLS = Set.of(
        // All teacher tools
        "get_user_level", "get_user_profile", "get_user_settings",
        "get_user_progress", "get_user_vocab_progress", "get_user_exam_history",
        "get_enrolled_courses", "get_lesson_detail", "get_course_detail",
        "get_my_exams", "get_my_certificates", "get_daily_streak",
        "list_shop_courses", "search_courses", "search_shop_courses", "list_course_lessons",
        "search_lessons", "get_grammar_topic", "search_vocabulary", "get_course_info", "get_exam_info",
        "list_my_courses", "list_my_learning_progress", "list_my_exams",
        "start_lesson", "complete_lesson", "enroll_course", "unenroll_course",
        "add_to_cart", "view_cart", "checkout",
        "get_my_courses", "list_teacher_courses", "get_course_students", "get_students_in_course", "get_course_analytics",
        "get_student_progress", "get_all_lessons",
        "create_course", "update_course", "publish_course", "delete_course",
        "create_lesson", "update_lesson", "delete_lesson",
        "create_exam", "update_exam", "publish_exam", "delete_exam",
        "create_question", "bulk_import_questions",
        "get_student_results", "get_exam_results",
        // Admin tools
        "get_all_users", "get_user_detail", "manage_user", "delete_user",
        "get_all_courses", "get_course_stats", "get_revenue_stats",
        "get_system_stats", "get_payment_history", "get_enrollment_stats",
        "get_exam_attempts", "get_question_bank"
    );

    private final UserMemoryRepository userMemoryRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final AiSettingsRepository aiSettingsRepository;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    private final QuestionRepository questionRepository;

    public ToolExecutor(
            UserMemoryRepository userMemoryRepository,
            LessonProgressRepository lessonProgressRepository,
            ExamAttemptRepository examAttemptRepository,
            LessonRepository lessonRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            ExamRepository examRepository,
            UserRepository userRepository,
            AiSettingsRepository aiSettingsRepository,
            CartRepository cartRepository,
            PaymentRepository paymentRepository,
            QuestionRepository questionRepository) {
        this.userMemoryRepository = userMemoryRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.aiSettingsRepository = aiSettingsRepository;
        this.cartRepository = cartRepository;
        this.paymentRepository = paymentRepository;
        this.questionRepository = questionRepository;
    }

    public ToolResult execute(ToolCall call, Long userId) {
        String toolName = call.toolName();
        Map<String, Object> args = call.arguments() != null ? call.arguments() : new HashMap<>();
        
        log.info("[TOOL] Executing: {} for userId: {}", toolName, userId);

        // Handle special case: auto-detect system tool query
        if ("auto_detect_system_tools".equals(toolName)) {
            String message = getStringArg(args, "userMessage");
            if (isSystemToolQuery(message)) {
                return getAvailableToolList(userId);
            }
            return ToolResult.success("auto_detect_system_tools", "Not a system tool query", Map.of("isToolQuery", false));
        }
        if ("none".equals(toolName)) {
            return ToolResult.success("none", "No tool action required", Map.of());
        }

        // Check role-based permission
        User.Role userRole = getUserRole(userId);
        Set<String> allowedTools = getAllowedTools(userRole);
        
        if (!allowedTools.contains(toolName)) {
            log.warn("[TOOL] Permission denied: {} for role {}", toolName, userRole);
            return ToolResult.error(toolName, "Tool '" + toolName + "' not available for role: " + userRole);
        }

        try {
            return switch (toolName) {
                // User Info
                case "get_user_level" -> getUserLevel(userId);
                case "get_user_profile" -> getUserProfile(userId);
                case "get_user_settings" -> getUserSettings(userId);
                
                // Learning
                case "get_user_progress" -> getUserProgress(userId);
                case "get_user_vocab_progress" -> getUserVocabProgress(userId);
                case "get_user_exam_history" -> getUserExamHistory(userId);
                case "get_my_exams" -> getMyExams(userId);
                case "list_my_exams" -> listMyExams(userId, getIntArg(args, "limit", 12));
                case "get_my_certificates" -> getMyCertificates(userId);
                case "get_daily_streak" -> getDailyStreak(userId);
                
                // Courses & Lessons
                case "get_enrolled_courses", "list_my_courses" -> listMyCourses(userId, getIntArg(args, "limit", 8));
                case "list_my_learning_progress", "list_my_progress" -> listMyProgress(userId, getIntArg(args, "limit", 8));
                case "get_lesson_detail" -> getLessonDetail(getLongArg(args, "lessonId"));
                case "get_course_detail" -> getCourseDetail(userId, getLongArg(args, "courseId"));
                case "list_course_lessons" -> listCourseLessons(userId, getLongArg(args, "courseId"));
                case "search_lessons" -> searchLessons(
                    firstNonBlank(getStringArg(args, "query"), getStringArg(args, "keyword")),
                    getLongArg(args, "courseId"));
                case "search_courses", "search_shop_courses" -> searchCourses(getStringArg(args, "query"), getIntArg(args, "limit", 8));
                
                // Grammar & Vocab
                case "get_grammar_topic" -> getGrammarTopic(getStringArg(args, "topic"));
                case "search_vocabulary" -> searchVocabulary(getStringArg(args, "query"));
                
                // Browse Shop
                case "list_shop_courses" -> listShopCourses(getIntArg(args, "limit", 8));
                case "get_course_info" -> getCourseInfo(getLongArg(args, "courseId"));
                case "get_exam_info" -> getExamInfo(getLongArg(args, "examId"));
                
                // Actions
                case "start_lesson" -> startLesson(userId, getLongArg(args, "lessonId"));
                case "complete_lesson" -> completeLesson(userId, getLongArg(args, "lessonId"));
                case "enroll_course" -> enrollCourse(userId, getLongArg(args, "courseId"));
                case "unenroll_course" -> unenrollCourse(userId, getLongArg(args, "courseId"));
                case "add_to_cart" -> addToCart(userId, getLongArg(args, "courseId"));
                case "view_cart" -> viewCart(userId);
                case "checkout" -> checkout(userId);
                
                // Teacher Tools
                case "get_my_courses", "list_teacher_courses" -> getMyTeachingCourses(userId);
                case "get_course_students" -> getCourseStudents(getLongArg(args, "courseId"));
                case "get_course_analytics" -> getCourseAnalytics(getLongArg(args, "courseId"));
                case "get_student_progress" -> getStudentProgress(getLongArg(args, "userId"), getLongArg(args, "courseId"));
                case "get_all_lessons" -> getAllLessons(getLongArg(args, "courseId"));
                case "get_all_courses" -> getAllCourses();
                case "get_students_in_course" -> getStudentsInCourse(getLongArg(args, "courseId"));
                case "get_student_results" -> getStudentResults(getLongArg(args, "examId"));
                case "get_exam_results" -> getExamResults(getLongArg(args, "examId"));
                
                // Course Management
                case "create_course" -> createCourse(args);
                case "update_course" -> updateCourse(getLongArg(args, "courseId"), args);
                case "publish_course" -> publishCourse(getLongArg(args, "courseId"));
                case "delete_course" -> deleteCourse(getLongArg(args, "courseId"));
                
                // Lesson Management
                case "create_lesson" -> createLesson(args);
                case "update_lesson" -> updateLesson(getLongArg(args, "lessonId"), args);
                case "delete_lesson" -> deleteLesson(getLongArg(args, "lessonId"));
                
                // Exam Management
                case "create_exam" -> createExam(args);
                case "update_exam" -> updateExam(getLongArg(args, "examId"), args);
                case "publish_exam" -> publishExam(getLongArg(args, "examId"));
                case "delete_exam" -> deleteExam(getLongArg(args, "examId"));
                case "create_question" -> createQuestion(args);
                case "bulk_import_questions" -> bulkImportQuestions(args);
                
                // Admin Tools
                case "get_all_users" -> getAllUsers();
                case "get_user_detail" -> getUserDetail(getLongArg(args, "userId"));
                case "manage_user" -> manageUser(getLongArg(args, "targetUserId"), getStringArg(args, "action"));
                case "delete_user" -> deleteUser(getLongArg(args, "userId"));
                case "get_course_stats" -> getCourseStats(getLongArg(args, "courseId"));
                case "get_revenue_stats" -> getRevenueStats();
                case "get_system_stats" -> getSystemStats();
                case "get_payment_history" -> getPaymentHistory(getIntArg(args, "limit", 20));
                case "get_enrollment_stats" -> getEnrollmentStats();
                case "get_exam_attempts" -> getExamAttempts(getLongArg(args, "examId"));
                case "get_question_bank" -> getQuestionBank(getLongArg(args, "courseId"));
                
                default -> ToolResult.error(toolName, "Unknown tool: " + toolName);
            };
        } catch (Exception e) {
            log.error("[TOOL] Error executing {}: {}", toolName, e.getMessage());
            return ToolResult.error(toolName, "Error: " + e.getMessage());
        }
    }

    private User.Role getUserRole(Long userId) {
        if (userId == null) {
            return User.Role.STUDENT;
        }
        return userRepository.findById(userId)
            .map(u -> u.getRole() != null ? u.getRole() : User.Role.STUDENT)
            .orElse(User.Role.STUDENT);
    }

    private Set<String> getAllowedTools(User.Role role) {
        if (role == null) {
            return STUDENT_TOOLS;
        }
        return switch (role) {
            case ADMIN -> ADMIN_TOOLS;
            case TEACHER -> TEACHER_TOOLS;
            default -> STUDENT_TOOLS;
        };
    }

    // ========== SYSTEM TOOL QUERY DETECTION ==========

    private boolean isSystemToolQuery(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("tool") || 
               lower.contains("công cụ") ||
               lower.contains("hệ thống") ||
               lower.contains("api") ||
               lower.contains("chức năng") ||
               lower.contains("features") ||
               lower.contains("functionality") ||
               lower.contains("bạn có thể làm gì") ||
               lower.contains("những gì bạn có thể làm") ||
               lower.contains("what can you do") ||
               lower.contains("your capabilities");
    }

    private ToolResult getAvailableToolList(Long userId) {
        User.Role role = getUserRole(userId);
        Set<String> tools = getAllowedTools(role);
        
        String roleName = role != null ? role.name() : "STUDENT";
        
        StringBuilder sb = new StringBuilder();
        sb.append("Here are the system tools available to you (Role: ").append(roleName).append("):\n\n");
        
        for (String tool : tools) {
            sb.append("- ").append(tool).append("\n");
        }
        
        sb.append("\nThese are the actual API tools I can access in the system.");
        
        Map<String, Object> data = new HashMap<>();
        data.put("tools", new ArrayList<>(tools));
        data.put("role", roleName);
        data.put("count", tools.size());
        
        return ToolResult.success("get_system_tools", sb.toString(), data);
    }

    // ========== STUDENT TOOLS ==========

    private ToolResult getUserLevel(Long userId) {
        if (userId == null) {
            return ToolResult.success("get_user_level", "Not logged in", Map.of("level", null));
        }
        
        return userMemoryRepository.findByUserId(userId)
            .map(um -> {
                String level = um.getUserLevel() != null ? um.getUserLevel() : "B1";
                String targetExam = um.getTargetExam();
                Integer targetScore = um.getTargetScore();
                
                Map<String, Object> data = new HashMap<>();
                data.put("level", level);
                if (targetExam != null) {
                    data.put("targetExam", targetExam);
                }
                if (targetScore != null) {
                    data.put("targetScore", targetScore);
                }
                
                return ToolResult.success("get_user_level", "Level: " + level, data);
            })
            .orElse(ToolResult.success("get_user_level", "Level: B1 (default)", Map.of("level", "B1")));
    }

    private ToolResult getUserProgress(Long userId) {
        if (userId == null) {
            return ToolResult.success("get_user_progress", "Not logged in", Map.of());
        }
        
        try {
            List<LessonProgress> completed = lessonProgressRepository.findByUserIdAndIsCompletedTrue(userId);
            long enrolled = enrollmentRepository.countByUserId(userId);
            
            Map<String, Object> data = Map.of(
                "completedLessons", completed.size(),
                "enrolledCourses", enrolled
            );
            return ToolResult.success("get_user_progress", 
                String.format("Completed %d lessons, %d courses", completed.size(), enrolled), data);
        } catch (Exception e) {
            return ToolResult.success("get_user_progress", "Error loading progress", Map.of("error", e.getMessage()));
        }
    }

    private ToolResult getUserVocabProgress(Long userId) {
        return ToolResult.success("get_user_vocab_progress", 
            "Vocabulary tracking coming soon. Currently 0 words tracked.", 
            Map.of("wordsLearned", 0, "wordsToReview", 0));
    }

    private ToolResult getUserExamHistory(Long userId) {
        if (userId == null) {
            return ToolResult.success("get_user_exam_history", "Not logged in", Map.of("exams", List.of()));
        }
        
        try {
            List<ExamAttempt> attempts = examAttemptRepository.findByUserId(userId);
            List<Map<String, Object>> exams = attempts.stream().map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("examId", a.getExamId());
                m.put("score", a.getScore());
                m.put("totalQuestions", a.getTotalQuestions());
                m.put("correctAnswers", a.getCorrectAnswers());
                m.put("attemptedAt", a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : null);
                return m;
            }).toList();
            
            return ToolResult.success("get_user_exam_history", 
                exams.isEmpty() ? "No exam attempts yet" : String.format("%d exams taken", exams.size()),
                Map.of("exams", exams));
        } catch (Exception e) {
            return ToolResult.error("get_user_exam_history", e.getMessage());
        }
    }

    private ToolResult searchLessons(String query, Long courseId) {
        try {
            List<Lesson> lessons;
            if (query != null && !query.isBlank()) {
                lessons = lessonRepository.findByTitleContainingIgnoreCase(query);
            } else if (courseId != null) {
                lessons = lessonRepository.findByCourseId(courseId);
            } else {
                lessons = lessonRepository.findAll().stream().limit(10).toList();
            }
            
            List<Map<String, Object>> list = lessons.stream().map(l -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", l.getId());
                m.put("title", l.getTitle());
                m.put("description", l.getDescription());
                return m;
            }).toList();
            
            return ToolResult.success("search_lessons", String.format("Found %d lessons", lessons.size()), 
                Map.of("lessons", list));
        } catch (Exception e) {
            return ToolResult.error("search_lessons", e.getMessage());
        }
    }

    private ToolResult getCourseInfo(Long courseId) {
        if (courseId == null) {
            return ToolResult.error("get_course_info", "Course ID required");
        }
        
        return courseRepository.findById(courseId)
            .map(c -> ToolResult.success("get_course_info", c.getTitle(), 
                Map.of("id", c.getId(), "title", c.getTitle(), "description", c.getDescription(), 
                       "price", c.getPrice(), "level", c.getLevel())))
            .orElse(ToolResult.error("get_course_info", "Course not found"));
    }

    private ToolResult getExamInfo(Long examId) {
        if (examId == null) {
            return ToolResult.error("get_exam_info", "Exam ID required");
        }
        
        return examRepository.findById(examId)
            .map(e -> ToolResult.success("get_exam_info", e.getTitle(),
                Map.of("id", e.getId(), "title", e.getTitle(), "duration", e.getDuration(), 
                       "totalQuestions", e.getTotalQuestions())))
            .orElse(ToolResult.error("get_exam_info", "Exam not found"));
    }

    private ToolResult searchVocabulary(String query) {
        return ToolResult.success("search_vocabulary", "Vocabulary search coming soon", Map.of("query", query, "results", List.of()));
    }

    private ToolResult getGrammarTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return ToolResult.success("get_grammar_topic", "Topics: tenses, conditionals, passive, articles, prepositions",
                Map.of("topics", List.of("tenses", "conditionals", "passive", "articles", "prepositions")));
        }
        
        String explanation = switch (topic.toLowerCase()) {
            case "tenses" -> "English has 12 tenses: Present Simple, Present Continuous, Present Perfect, etc.";
            case "conditionals" -> "Type 0 (general truth), Type 1 (real), Type 2 (hypothetical), Type 3 (past hypothetical).";
            case "passive" -> "Passive: be + past participle. Use when action is more important than performer.";
            case "articles" -> "a/an (indefinite), the (definite), no article (plural/uncountable).";
            case "prepositions" -> "Time: at, on, in. Place: at, on, in. Movement: to, from, through.";
            default -> "Topic not found. Try: tenses, conditionals, passive, articles, prepositions.";
        };
        
        return ToolResult.success("get_grammar_topic", explanation, Map.of("topic", topic, "explanation", explanation));
    }

    private ToolResult startLesson(Long userId, Long lessonId) {
        if (userId == null) {
            return ToolResult.error("start_lesson", "User not logged in");
        }
        if (lessonId == null) {
            return ToolResult.error("start_lesson", "Lesson ID required");
        }
        
        try {
            if (lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId).isPresent()) {
                return ToolResult.success("start_lesson", "Lesson already started", Map.of("status", "in_progress", "lessonId", lessonId));
            }
            
            LessonProgress progress = LessonProgress.builder()
                .userId(userId).lessonId(lessonId).isCompleted(false).progressPercent(0)
                .startedAt(LocalDateTime.now()).build();
            lessonProgressRepository.save(progress);
            
            return ToolResult.success("start_lesson", "Lesson started!", Map.of("status", "started", "lessonId", lessonId));
        } catch (Exception e) {
            return ToolResult.error("start_lesson", e.getMessage());
        }
    }

    private ToolResult completeLesson(Long userId, Long lessonId) {
        if (userId == null) {
            return ToolResult.error("complete_lesson", "User not logged in");
        }
        if (lessonId == null) {
            return ToolResult.error("complete_lesson", "Lesson ID required");
        }
        
        return lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
            .map(lp -> {
                lp.setIsCompleted(true);
                lp.setProgressPercent(100);
                lp.setCompletedAt(LocalDateTime.now());
                lessonProgressRepository.save(lp);
                return ToolResult.success("complete_lesson", "Lesson completed!", Map.of("status", "completed", "lessonId", lessonId));
            })
            .orElse(ToolResult.error("complete_lesson", "Lesson not started"));
    }

    private ToolResult getEnrolledCourses(Long userId) {
        if (userId == null) {
            return ToolResult.success("get_enrolled_courses", "Not logged in", Map.of("courses", List.of()));
        }
        
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        if (enrollments.isEmpty()) {
            return ToolResult.success("get_enrolled_courses", "You haven't enrolled in any courses yet.", 
                Map.of("courses", List.of(), "message", "No courses enrolled"));
        }
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Map<String, Object> m = new HashMap<>();
            m.put("courseId", e.getCourseId());
            m.put("progressPercent", e.getProgressPercent());
            m.put("enrolledAt", e.getEnrolledAt() != null ? e.getEnrolledAt().toString() : null);
            
            // Get course details
            courseRepository.findById(e.getCourseId()).ifPresent(course -> {
                m.put("title", course.getTitle());
                m.put("description", course.getDescription());
                m.put("level", course.getLevel());
                m.put("price", course.getPrice());
            });
            
            // Get lesson progress
            List<Lesson> lessons = lessonRepository.findByCourseId(e.getCourseId());
            long totalLessons = lessons.size();
            long completedLessons = lessonProgressRepository.findByUserIdAndIsCompletedTrue(userId).stream()
                .filter(lp -> lessons.stream().anyMatch(l -> l.getId().equals(lp.getLessonId())))
                .count();
            m.put("totalLessons", totalLessons);
            m.put("completedLessons", completedLessons);
            
            list.add(m);
        }
        
        String summary = String.format("You are enrolled in %d courses: %s", 
            enrollments.size(),
            list.stream().map(m -> (String)m.getOrDefault("title", "Unknown"))
                .collect(java.util.stream.Collectors.joining(", ")));
        
        return ToolResult.success("get_enrolled_courses", summary, Map.of("courses", list, "total", enrollments.size()));
    }

    private ToolResult getLessonDetail(Long lessonId) {
        if (lessonId == null) {
            return ToolResult.error("get_lesson_detail", "Lesson ID required");
        }
        
        return lessonRepository.findById(lessonId)
            .map(l -> ToolResult.success("get_lesson_detail", l.getTitle(),
                Map.of("id", l.getId(), "title", l.getTitle(), "description", l.getDescription(), 
                       "contentUrl", l.getContentUrl(), "durationSeconds", l.getDurationSeconds())))
            .orElse(ToolResult.error("get_lesson_detail", "Lesson not found"));
    }

    // ========== TEACHER TOOLS ==========

    private ToolResult getStudentsInCourse(Long courseId) {
        if (courseId == null) {
            return ToolResult.error("get_students_in_course", "Course ID required");
        }
        
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        List<Map<String, Object>> students = enrollments.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", e.getUserId());
            m.put("progress", e.getProgressPercent());
            m.put("enrolledAt", e.getEnrolledAt());
            return m;
        }).toList();
        
        return ToolResult.success("get_students_in_course", 
            String.format("%d students in course", students.size()), Map.of("students", students));
    }

    private ToolResult getCourseAnalytics(Long courseId) {
        if (courseId == null) {
            return ToolResult.error("get_course_analytics", "Course ID required");
        }
        
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        long completed = enrollments.stream().filter(e -> e.getProgressPercent() != null && e.getProgressPercent() >= 100).count();
        
        Map<String, Object> data = Map.of(
            "totalStudents", enrollments.size(),
            "completedStudents", completed,
            "completionRate", enrollments.isEmpty() ? 0 : (double) completed / enrollments.size() * 100
        );
        
        return ToolResult.success("get_course_analytics", 
            String.format("Course analytics: %d students, %d completed", enrollments.size(), completed), data);
    }

    private ToolResult getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        List<Map<String, Object>> list = courses.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("description", c.getDescription());
            m.put("level", c.getLevel());
            return m;
        }).toList();
        
        return ToolResult.success("get_all_courses", String.format("%d courses", courses.size()), Map.of("courses", list));
    }

    private ToolResult getAllLessons(Long courseId) {
        if (courseId == null) {
            return ToolResult.error("get_all_lessons", "Course ID required");
        }
        
        List<Lesson> lessons = lessonRepository.findByCourseId(courseId);
        List<Map<String, Object>> list = lessons.stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("title", l.getTitle());
            m.put("orderIndex", l.getOrderIndex());
            return m;
        }).toList();
        
        return ToolResult.success("get_all_lessons", String.format("%d lessons", lessons.size()), Map.of("lessons", list));
    }

    private ToolResult getStudentProgress(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return ToolResult.error("get_student_progress", "userId and courseId required");
        }
        
        List<LessonProgress> progress = lessonProgressRepository.findByUserIdAndLessonIdIn(userId, 
            lessonRepository.findByCourseId(courseId).stream().map(Lesson::getId).toList());
        
        long completed = progress.stream().filter(LessonProgress::getIsCompleted).count();
        
        return ToolResult.success("get_student_progress", 
            String.format("Student completed %d/%d lessons", completed, progress.size()),
            Map.of("completed", completed, "total", progress.size()));
    }

    private ToolResult getCourseStudents(Long courseId) {
        return getStudentsInCourse(courseId);
    }

    // ========== ADMIN TOOLS ==========

    private ToolResult getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("email", u.getEmail());
            m.put("fullName", u.getFullName());
            m.put("role", u.getRole());
            m.put("isActive", u.getIsActive());
            return m;
        }).toList();
        
        return ToolResult.success("get_all_users", String.format("%d users", users.size()), Map.of("users", list));
    }

    private ToolResult getSystemStats() {
        long totalUsers = userRepository.count();
        long totalCourses = courseRepository.count();
        long totalEnrollments = enrollmentRepository.count();
        
        Map<String, Object> data = Map.of(
            "totalUsers", totalUsers,
            "totalCourses", totalCourses,
            "totalEnrollments", totalEnrollments
        );
        
        return ToolResult.success("get_system_stats", 
            String.format("System: %d users, %d courses, %d enrollments", totalUsers, totalCourses, totalEnrollments), data);
    }

    private ToolResult manageUser(Long targetUserId, String action) {
        if (targetUserId == null || action == null) {
            return ToolResult.error("manage_user", "targetUserId and action required");
        }
        
        return userRepository.findById(targetUserId)
            .map(user -> {
                switch (action.toLowerCase()) {
                    case "activate" -> user.setIsActive(true);
                    case "deactivate" -> user.setIsActive(false);
                    case "promote_teacher" -> user.setRole(User.Role.TEACHER);
                    case "demote_student" -> user.setRole(User.Role.STUDENT);
                    default -> { return ToolResult.error("manage_user", "Unknown action: " + action); }
                }
                userRepository.save(user);
                return ToolResult.success("manage_user", "User " + action + "d successfully", Map.of("userId", targetUserId, "action", action));
            })
            .orElse(ToolResult.error("manage_user", "User not found"));
    }

    private ToolResult createCourse(Map<String, Object> args) {
        String title = getStringArg(args, "title");
        String description = getStringArg(args, "description");
        
        if (title == null) {
            return ToolResult.error("create_course", "Title required");
        }

        Course course = Course.builder()
            .title(title)
            .description(description != null ? description : "")
            .level("B1")
            .price(java.math.BigDecimal.ZERO)
            .build();
        course = courseRepository.save(course);

        return ToolResult.success("create_course", "Course created: " + title, Map.of("courseId", course.getId()));
    }

    private ToolResult updateCourse(Long courseId, Map<String, Object> args) {
        if (courseId == null) {
            return ToolResult.error("update_course", "courseId required");
        }
        
        return courseRepository.findById(courseId)
            .map(course -> {
                String title = getStringArg(args, "title");
                String description = getStringArg(args, "description");
                if (title != null) {
                    course.setTitle(title);
                }
                if (description != null) {
                    course.setDescription(description);
                }
                courseRepository.save(course);
                return ToolResult.success("update_course", "Course updated", Map.of("courseId", courseId));
            })
            .orElse(ToolResult.error("update_course", "Course not found"));
    }

    private ToolResult deleteCourse(Long courseId) {
        if (courseId == null) {
            return ToolResult.error("delete_course", "courseId required");
        }
        
        courseRepository.deleteById(courseId);
        return ToolResult.success("delete_course", "Course deleted", Map.of("courseId", courseId));
    }

    private ToolResult createExam(Map<String, Object> args) {
        String title = getStringArg(args, "title");
        Long courseId = getLongArg(args, "courseId");
        
        if (title == null) {
            return ToolResult.error("create_exam", "Title required");
        }
        
        Exam exam = Exam.builder()
            .title(title)
            .description(getStringArg(args, "description") != null ? getStringArg(args, "description") : "")
            .courseId(courseId)
            .duration(60)
            .totalQuestions(50)
            .build();
        exam = examRepository.save(exam);
        
        return ToolResult.success("create_exam", "Exam created: " + title, Map.of("examId", exam.getId()));
    }

    private ToolResult updateExam(Long examId, Map<String, Object> args) {
        if (examId == null) {
            return ToolResult.error("update_exam", "examId required");
        }

        return examRepository.findById(examId)
            .map(exam -> {
                String title = getStringArg(args, "title");
                Integer duration = getIntArg(args, "duration");
                if (title != null) {
                    exam.setTitle(title);
                }
                if (duration != null) {
                    exam.setDuration(duration);
                }
                examRepository.save(exam);
                return ToolResult.success("update_exam", "Exam updated", Map.of("examId", examId));
            })
            .orElse(ToolResult.error("update_exam", "Exam not found"));
    }

    private ToolResult deleteExam(Long examId) {
        if (examId == null) {
            return ToolResult.error("delete_exam", "examId required");
        }
        
        examRepository.deleteById(examId);
        return ToolResult.success("delete_exam", "Exam deleted", Map.of("examId", examId));
    }

    private ToolResult getRevenueStats() {
        return ToolResult.success("get_revenue_stats", "Revenue stats - feature coming soon", Map.of());
    }

    private ToolResult createQuestion(Map<String, Object> args) {
        Long examId = resolveExamId(args);
        if (examId == null) {
            return ToolResult.error("create_question", "examId (or courseId with existing exam) is required");
        }
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) {
            return ToolResult.error("create_question", "Exam not found");
        }

        String content = firstNonBlank(getStringArg(args, "content"), getStringArg(args, "questionText"));
        if (content == null || content.isBlank()) {
            return ToolResult.error("create_question", "content/questionText is required");
        }
        String optionsJson = normalizeOptionsToJson(args.get("options"));
        if (optionsJson == null) {
            return ToolResult.error("create_question", "options is required (JSON array or list)");
        }
        String correctOption = firstNonBlank(getStringArg(args, "correctOption"), getStringArg(args, "correctAnswer"));
        if (correctOption == null || correctOption.isBlank()) {
            return ToolResult.error("create_question", "correctOption/correctAnswer is required");
        }

        int nextOrder = questionRepository.findByExamIdOrderByOrderIndexAsc(examId).size();
        Question.QuestionType type = parseQuestionType(getStringArg(args, "type"));

        Question question = Question.builder()
            .examId(examId)
            .content(content)
            .options(optionsJson)
            .correctOption(correctOption)
            .type(type)
            .explanation(getStringArg(args, "explanation"))
            .orderIndex(nextOrder)
            .build();
        question = questionRepository.save(question);

        Integer currentTotal = exam.getTotalQuestions() != null ? exam.getTotalQuestions() : 0;
        exam.setTotalQuestions(Math.max(currentTotal, nextOrder + 1));
        examRepository.save(exam);

        return ToolResult.success("create_question", "Question created", Map.of(
            "examId", examId,
            "questionId", question.getId(),
            "orderIndex", nextOrder
        ));
    }

    private ToolResult bulkImportQuestions(Map<String, Object> args) {
        Long examId = resolveExamId(args);
        if (examId == null) {
            return ToolResult.error("bulk_import_questions", "examId (or courseId with existing exam) is required");
        }
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) {
            return ToolResult.error("bulk_import_questions", "Exam not found");
        }
        Object questionsObj = args.get("questions");
        if (!(questionsObj instanceof List<?> rawList) || rawList.isEmpty()) {
            return ToolResult.error("bulk_import_questions", "questions must be a non-empty list");
        }

        List<Question> existing = questionRepository.findByExamIdOrderByOrderIndexAsc(examId);
        int nextOrder = existing.size();
        int created = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rawList.size(); i++) {
            Object item = rawList.get(i);
            if (!(item instanceof Map<?, ?> rawMap)) {
                errors.add("Item " + i + ": invalid payload type");
                continue;
            }
            Map<String, Object> qArgs = new HashMap<>();
            rawMap.forEach((k, v) -> {
                if (k != null) {
                    qArgs.put(k.toString(), v);
                }
            });

            String content = firstNonBlank(getStringArg(qArgs, "content"), getStringArg(qArgs, "questionText"));
            String optionsJson = normalizeOptionsToJson(qArgs.get("options"));
            String correctOption = firstNonBlank(getStringArg(qArgs, "correctOption"), getStringArg(qArgs, "correctAnswer"));
            if (content == null || content.isBlank() || optionsJson == null || correctOption == null || correctOption.isBlank()) {
                errors.add("Item " + i + ": missing content/options/correctOption");
                continue;
            }

            Question question = Question.builder()
                .examId(examId)
                .content(content)
                .options(optionsJson)
                .correctOption(correctOption)
                .type(parseQuestionType(getStringArg(qArgs, "type")))
                .explanation(getStringArg(qArgs, "explanation"))
                .orderIndex(nextOrder++)
                .build();
            questionRepository.save(question);
            created++;
        }

        Integer currentTotal = exam.getTotalQuestions() != null ? exam.getTotalQuestions() : 0;
        exam.setTotalQuestions(Math.max(currentTotal, nextOrder));
        examRepository.save(exam);

        Map<String, Object> data = new HashMap<>();
        data.put("examId", examId);
        data.put("created", created);
        data.put("requested", rawList.size());
        data.put("errors", errors);
        return ToolResult.success(
            "bulk_import_questions",
            String.format("Imported %d/%d questions", created, rawList.size()),
            data
        );
    }

    private Long resolveExamId(Map<String, Object> args) {
        Long examId = getLongArg(args, "examId");
        if (examId != null) {
            return examId;
        }
        Long courseId = getLongArg(args, "courseId");
        if (courseId == null) {
            return null;
        }
        List<Exam> exams = examRepository.findByCourseId(courseId);
        if (exams.isEmpty()) {
            return null;
        }
        exams.sort(Comparator.comparing(Exam::getId));
        return exams.get(exams.size() - 1).getId();
    }

    private Question.QuestionType parseQuestionType(String type) {
        if (type == null || type.isBlank()) {
            return Question.QuestionType.SINGLE_CHOICE;
        }
        try {
            return Question.QuestionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Question.QuestionType.SINGLE_CHOICE;
        }
    }

    private String normalizeOptionsToJson(Object optionsObj) {
        if (optionsObj == null) {
            return null;
        }
        if (optionsObj instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                return trimmed;
            }
            if (trimmed.contains("|")) {
                String[] parts = trimmed.split("\\|");
                return toJsonArray(parts);
            }
            return null;
        }
        if (optionsObj instanceof List<?> list) {
            String[] items = list.stream().map(v -> v == null ? "" : v.toString()).toArray(String[]::new);
            return toJsonArray(items);
        }
        return null;
    }

    private String toJsonArray(String[] items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(escapeJson(items[i])).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ========== HELPERS ==========

    private Long getLongArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getStringArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Integer getIntArg(Map<String, Object> args, String key, int defaultValue) {
        Integer value = getIntArg(args, key);
        return value != null ? value : defaultValue;
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
    
    // ========== TUTOR-STYLE TOOLS ==========
    
    private ToolResult listShopCourses(int limit) {
        try {
            List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsPublished()))
                .limit(limit)
                .toList();
            
            if (courses.isEmpty()) {
                return ToolResult.success("list_shop_courses", "No published courses available.", Map.of("courses", List.of()));
            }
            
            List<Map<String, Object>> list = courses.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("title", c.getTitle());
                m.put("description", c.getDescription());
                m.put("price", c.getPrice());
                m.put("level", c.getLevel());
                return m;
            }).toList();
            
            return ToolResult.success("list_shop_courses", 
                String.format("Found %d published courses", courses.size()), 
                Map.of("courses", list, "count", courses.size()));
        } catch (Exception e) {
            return ToolResult.error("list_shop_courses", e.getMessage());
        }
    }
    
    private ToolResult searchShopCourses(String query, int limit) {
        if (query == null || query.isBlank()) {
            return listShopCourses(limit);
        }
        
        try {
            List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase().contains(query.toLowerCase()))
                .filter(c -> Boolean.TRUE.equals(c.getIsPublished()))
                .limit(limit)
                .toList();
            
            List<Map<String, Object>> list = courses.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("title", c.getTitle());
                m.put("description", c.getDescription());
                m.put("price", c.getPrice());
                return m;
            }).toList();
            
            return ToolResult.success("search_shop_courses",
                String.format("Found %d courses matching '%s'", courses.size(), query),
                Map.of("courses", list, "query", query));
        } catch (Exception e) {
            return ToolResult.error("search_shop_courses", e.getMessage());
        }
    }
    
    private ToolResult getCourseDetail(Long userId, Long courseId) {
        if (courseId == null) {
            return ToolResult.error("get_course_detail", "courseId required");
        }
        
        return courseRepository.findById(courseId)
            .map(course -> {
                List<Lesson> lessons = lessonRepository.findByCourseId(courseId);
                Map<String, Object> data = new HashMap<>();
                data.put("id", course.getId());
                data.put("title", course.getTitle());
                data.put("description", course.getDescription());
                data.put("price", course.getPrice());
                data.put("level", course.getLevel());
                data.put("isPublished", course.getIsPublished());
                data.put("lessonsCount", lessons.size());
                data.put("lessons", lessons.stream().map(l -> 
                    Map.of("id", l.getId(), "title", l.getTitle(), "order", l.getOrderIndex())
                ).toList());
                
                // Check if user is enrolled
                if (userId != null) {
                    boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
                    data.put("isEnrolled", isEnrolled);
                }
                
                return ToolResult.success("get_course_detail", 
                    "Course: " + course.getTitle() + " (" + lessons.size() + " lessons)", data);
            })
            .orElse(ToolResult.error("get_course_detail", "Course not found"));
    }
    
    private ToolResult listCourseLessons(Long userId, Long courseId) {
        if (courseId == null) {
            return ToolResult.error("list_course_lessons", "courseId required");
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ToolResult.error("list_course_lessons", "Course not found");
        }
        
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<Map<String, Object>> list = lessons.stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("title", l.getTitle());
            m.put("type", l.getType());
            m.put("orderIndex", l.getOrderIndex());
            m.put("durationSeconds", l.getDurationSeconds());
            if (userId != null) {
                boolean completed = lessonProgressRepository.findByUserIdAndLessonId(userId, l.getId())
                    .map(LessonProgress::getIsCompleted).orElse(false);
                m.put("completed", completed);
            }
            return m;
        }).toList();
        
        return ToolResult.success("list_course_lessons",
            "Course '" + course.getTitle() + "' has " + lessons.size() + " lessons",
            Map.of("courseId", courseId, "courseTitle", course.getTitle(), "lessons", list));
    }
    
    private ToolResult enrollCourse(Long userId, Long courseId) {
        if (userId == null) {
            return ToolResult.error("enroll_course", "User not logged in");
        }
        if (courseId == null) {
            return ToolResult.error("enroll_course", "courseId required");
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ToolResult.error("enroll_course", "Course not found");
        }
        if (course.getStatus() == Course.Status.BLOCKED) {
            return ToolResult.error("enroll_course", "Course is blocked by admin");
        }
        if (!Boolean.TRUE.equals(course.getIsPublished()) || course.getStatus() != Course.Status.APPROVED) {
            return ToolResult.error("enroll_course", "Course is not available for enrollment");
        }
        
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return ToolResult.success("enroll_course", "You are already enrolled in '" + course.getTitle() + "'", 
                Map.of("courseId", courseId, "alreadyEnrolled", true));
        }
        
        Enrollment enrollment = Enrollment.builder()
            .userId(userId)
            .courseId(courseId)
            .progressPercent(0)
            .isActive(true)
            .enrolledAt(java.time.LocalDateTime.now())
            .build();
        enrollmentRepository.save(enrollment);
        
        return ToolResult.success("enroll_course", 
            "Successfully enrolled in '" + course.getTitle() + "'!", 
            Map.of("courseId", courseId, "courseTitle", course.getTitle()));
    }
    
    private ToolResult listMyCourses(Long userId, int limit) {
        if (userId == null) {
            return ToolResult.success("list_my_courses", "Not logged in", Map.of("courses", List.of()));
        }
        return getEnrolledCourses(userId); // Reuse existing implementation
    }

    private ToolResult listMyProgress(Long userId, int limit) {
        if (userId == null) {
            return ToolResult.success("list_my_progress", "Not logged in", Map.of("courses", List.of()));
        }
        return getUserProgress(userId); // Reuse existing implementation
    }

    private ToolResult listMyExams(Long userId, int limit) {
        if (userId == null) {
            return ToolResult.success("list_my_exams", "Not logged in", Map.of("exams", List.of()));
        }
        
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        List<Map<String, Object>> examList = new ArrayList<>();
        
        for (Enrollment e : enrollments) {
            List<Exam> exams = examRepository.findByCourseId(e.getCourseId());
            for (Exam exam : exams) {
                if (examList.size() >= limit) {
                    break;
                }
                Map<String, Object> m = new HashMap<>();
                m.put("id", exam.getId());
                m.put("title", exam.getTitle());
                m.put("examType", exam.getExamType());
                m.put("courseId", exam.getCourseId());
                examList.add(m);
            }
        }
        
        return ToolResult.success("list_my_exams",
            String.format("You have access to %d exams", examList.size()),
            Map.of("exams", examList, "count", examList.size()));
    }
    
    // ========== ADDITIONAL TOOLS ==========
    
    private ToolResult getUserProfile(Long userId) {
        if (userId == null) {
            return ToolResult.success("get_user_profile", "Not logged in", Map.of());
        }
        
        return userRepository.findById(userId)
            .map(user -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", user.getId());
                data.put("email", user.getEmail());
                data.put("fullName", user.getFullName());
                data.put("role", user.getRole());
                data.put("isActive", user.getIsActive());
                data.put("createdAt", user.getCreatedAt());
                
                // Get user memory
                userMemoryRepository.findByUserId(userId).ifPresent(um -> {
                    data.put("level", um.getUserLevel());
                    data.put("targetExam", um.getTargetExam());
                    data.put("targetScore", um.getTargetScore());
                });
                
                return ToolResult.success("get_user_profile", "User: " + user.getFullName(), data);
            })
            .orElse(ToolResult.error("get_user_profile", "User not found"));
    }
    
    private ToolResult getUserSettings(Long userId) {
        if (userId == null) {
            return ToolResult.success("get_user_settings", "Not logged in", Map.of());
        }
        
        List<AiSettings> settings = aiSettingsRepository.findByUserId(userId);
        Map<String, Object> settingsMap = new HashMap<>();
        for (AiSettings s : settings) {
            settingsMap.put(s.getSettingKey(), s.getSettingValue());
        }
        
        return ToolResult.success("get_user_settings", 
            String.format("Found %d settings", settings.size()), 
            Map.of("settings", settingsMap));
    }
    
    private ToolResult searchCourses(String query, int limit) {
        if (query == null || query.isBlank()) {
            return listShopCourses(limit);
        }
        
        try {
            List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase().contains(query.toLowerCase()))
                .filter(c -> Boolean.TRUE.equals(c.getIsPublished()))
                .limit(limit)
                .toList();
            
            List<Map<String, Object>> list = courses.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("title", c.getTitle());
                m.put("price", c.getPrice());
                return m;
            }).toList();
            
            return ToolResult.success("search_courses", 
                String.format("Found %d courses", courses.size()),
                Map.of("courses", list, "query", query));
        } catch (Exception e) {
            return ToolResult.error("search_courses", e.getMessage());
        }
    }
    
    private ToolResult getMyExams(Long userId) {
        if (userId == null) {
            return ToolResult.success("get_my_exams", "Not logged in", Map.of("exams", List.of()));
        }
        return listMyExams(userId, 20);
    }
    
    private ToolResult getMyCertificates(Long userId) {
        return ToolResult.success("get_my_certificates", 
            "Certificate feature coming soon. Complete courses to earn certificates!", 
            Map.of("certificates", List.of()));
    }
    
    private ToolResult getDailyStreak(Long userId) {
        if (userId == null) {
            return ToolResult.success("get_daily_streak", "Not logged in", Map.of());
        }

        int streak = aiSettingsRepository.findByUserIdAndSettingKey(userId, "daily_streak")
            .map(s -> {
                try {
                    return Integer.parseInt(s.getSettingValue());
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .orElse(0);
        
        return ToolResult.success("get_daily_streak", 
            "Your current streak: " + streak + " days", 
            Map.of("streak", streak));
    }
    
    private ToolResult unenrollCourse(Long userId, Long courseId) {
        if (userId == null) {
            return ToolResult.error("unenroll_course", "User not logged in");
        }
        if (courseId == null) {
            return ToolResult.error("unenroll_course", "courseId required");
        }
        
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
            .map(enrollment -> {
                enrollmentRepository.delete(enrollment);
                return ToolResult.success("unenroll_course", "Successfully unenrolled", Map.of("courseId", courseId));
            })
            .orElse(ToolResult.error("unenroll_course", "Not enrolled in this course"));
    }
    
    private ToolResult addToCart(Long userId, Long courseId) {
        if (userId == null) {
            return ToolResult.error("add_to_cart", "User not logged in");
        }
        if (courseId == null) {
            return ToolResult.error("add_to_cart", "courseId required");
        }
        
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            cart = Cart.builder().userId(userId).build();
            cart = cartRepository.save(cart);
        }
        
        // Check if already enrolled
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return ToolResult.success("add_to_cart", "Already enrolled in this course", Map.of("alreadyEnrolled", true));
        }
        
        return ToolResult.success("add_to_cart", "Added to cart", Map.of("courseId", courseId));
    }
    
    private ToolResult viewCart(Long userId) {
        if (userId == null) {
            return ToolResult.success("view_cart", "Not logged in", Map.of("items", List.of()));
        }
        
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        return ToolResult.success("view_cart", "Cart is empty", Map.of("items", List.of()));
    }
    
    private ToolResult checkout(Long userId) {
        if (userId == null) {
            return ToolResult.error("checkout", "User not logged in");
        }
        return ToolResult.success("checkout", "Checkout feature coming soon!", Map.of());
    }

    private ToolResult getMyTeachingCourses(Long userId) {
        if (userId == null) {
            return ToolResult.success("get_my_courses", "Not logged in", Map.of("courses", List.of()));
        }
        
        List<Course> courses = courseRepository.findAll().stream()
            .filter(c -> c.getInstructorId() != null && c.getInstructorId().equals(userId))
            .toList();
        
        List<Map<String, Object>> list = courses.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("published", c.getIsPublished());
            return m;
        }).toList();
        
        return ToolResult.success("get_my_courses", 
            String.format("You have %d courses", courses.size()),
            Map.of("courses", list));
    }
    
    private ToolResult publishCourse(Long courseId) {
        if (courseId == null) {
            return ToolResult.error("publish_course", "courseId required");
        }
        
        return courseRepository.findById(courseId)
            .map(course -> {
                course.setIsPublished(true);
                course.setStatus(Course.Status.APPROVED);
                courseRepository.save(course);
                return ToolResult.success("publish_course", "Course published", Map.of("courseId", courseId));
            })
            .orElse(ToolResult.error("publish_course", "Course not found"));
    }
    
    private ToolResult createLesson(Map<String, Object> args) {
        String title = getStringArg(args, "title");
        Long courseId = getLongArg(args, "courseId");
        if (title == null || courseId == null) {
            return ToolResult.error("create_lesson", "title and courseId required");
        }
        
        Lesson lesson = Lesson.builder()
            .title(title)
            .courseId(courseId)
            .description(getStringArg(args, "description"))
            .type(Lesson.LessonType.valueOf(getStringArg(args, "type") != null ? getStringArg(args, "type") : "VIDEO"))
            .contentUrl(getStringArg(args, "contentUrl"))
            .contentText(getStringArg(args, "contentText"))
            .durationSeconds(getIntArg(args, "durationSeconds", 0))
            .orderIndex(getIntArg(args, "orderIndex", 0))
            .build();
        lesson = lessonRepository.save(lesson);
        
        return ToolResult.success("create_lesson", "Lesson created: " + title, Map.of("lessonId", lesson.getId()));
    }
    
    private ToolResult updateLesson(Long lessonId, Map<String, Object> args) {
        if (lessonId == null) {
            return ToolResult.error("update_lesson", "lessonId required");
        }

        return lessonRepository.findById(lessonId)
            .map(lesson -> {
                if (getStringArg(args, "title") != null) {
                    lesson.setTitle(getStringArg(args, "title"));
                }
                if (getStringArg(args, "description") != null) {
                    lesson.setDescription(getStringArg(args, "description"));
                }
                if (getStringArg(args, "contentUrl") != null) {
                    lesson.setContentUrl(getStringArg(args, "contentUrl"));
                }
                lessonRepository.save(lesson);
                return ToolResult.success("update_lesson", "Lesson updated", Map.of("lessonId", lessonId));
            })
            .orElse(ToolResult.error("update_lesson", "Lesson not found"));
    }
    
    private ToolResult deleteLesson(Long lessonId) {
        if (lessonId == null) {
            return ToolResult.error("delete_lesson", "lessonId required");
        }
        lessonRepository.deleteById(lessonId);
        return ToolResult.success("delete_lesson", "Lesson deleted", Map.of("lessonId", lessonId));
    }
    
    private ToolResult publishExam(Long examId) {
        if (examId == null) {
            return ToolResult.error("publish_exam", "examId required");
        }
        
        return examRepository.findById(examId)
            .map(exam -> {
                // Exam doesn't have isPublished field, just confirm it exists
                examRepository.save(exam);
                return ToolResult.success("publish_exam", "Exam confirmed: " + exam.getTitle(), Map.of("examId", examId, "title", exam.getTitle()));
            })
            .orElse(ToolResult.error("publish_exam", "Exam not found"));
    }
    
    private ToolResult getUserDetail(Long targetUserId) {
        if (targetUserId == null) {
            return ToolResult.error("get_user_detail", "userId required");
        }
        
        return userRepository.findById(targetUserId)
            .map(user -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", user.getId());
                data.put("email", user.getEmail());
                data.put("fullName", user.getFullName());
                data.put("role", user.getRole());
                data.put("isActive", user.getIsActive());
                
                long enrolled = enrollmentRepository.countByUserId(targetUserId);
                long completed = lessonProgressRepository.findByUserIdAndIsCompletedTrue(targetUserId).size();
                data.put("enrolledCourses", enrolled);
                data.put("completedLessons", completed);
                
                return ToolResult.success("get_user_detail", "User: " + user.getFullName(), data);
            })
            .orElse(ToolResult.error("get_user_detail", "User not found"));
    }
    
    private ToolResult deleteUser(Long targetUserId) {
        if (targetUserId == null) {
            return ToolResult.error("delete_user", "userId required");
        }
        
        userRepository.deleteById(targetUserId);
        return ToolResult.success("delete_user", "User deleted", Map.of("userId", targetUserId));
    }
    
    private ToolResult getCourseStats(Long courseId) {
        if (courseId == null) {
            return ToolResult.error("get_course_stats", "courseId required");
        }
        
        long enrolled = enrollmentRepository.countByCourseId(courseId);
        List<Lesson> lessons = lessonRepository.findByCourseId(courseId);
        
        Map<String, Object> data = Map.of(
            "courseId", courseId,
            "totalEnrolled", enrolled,
            "totalLessons", lessons.size()
        );
        
        return ToolResult.success("get_course_stats", 
            String.format("Course %d: %d enrolled, %d lessons", courseId, enrolled, lessons.size()),
            data);
    }
    
    private ToolResult getPaymentHistory(int limit) {
        List<Payment> payments = paymentRepository.findAll().stream().limit(limit).toList();
        
        List<Map<String, Object>> list = payments.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("amount", p.getAmount());
            m.put("status", p.getStatus());
            m.put("createdAt", p.getCreatedAt());
            return m;
        }).toList();
        
        return ToolResult.success("get_payment_history", 
            String.format("Found %d payments", payments.size()),
            Map.of("payments", list));
    }
    
    private ToolResult getEnrollmentStats() {
        long total = enrollmentRepository.count();
        long activeEnrollments = enrollmentRepository.findAll().stream()
            .filter(e -> e.getIsActive() != null && e.getIsActive())
            .count();
        
        return ToolResult.success("get_enrollment_stats",
            String.format("Total: %d, Active: %d", total, activeEnrollments),
            Map.of("totalEnrollments", total, "activeEnrollments", activeEnrollments));
    }
    
    private ToolResult getExamAttempts(Long examId) {
        if (examId == null) {
            return ToolResult.error("get_exam_attempts", "examId required");
        }
        
        List<ExamAttempt> attempts = examAttemptRepository.findAll().stream()
            .filter(a -> a.getExamId() != null && a.getExamId().equals(examId))
            .toList();
        
        List<Map<String, Object>> list = attempts.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", a.getUserId());
            m.put("score", a.getScore());
            m.put("submittedAt", a.getSubmittedAt());
            return m;
        }).toList();
        
        return ToolResult.success("get_exam_attempts",
            String.format("%d attempts", attempts.size()),
            Map.of("attempts", list));
    }
    
    private ToolResult getQuestionBank(Long courseId) {
        if (courseId == null) {
            return ToolResult.error("get_question_bank", "courseId required");
        }
        
        return ToolResult.success("get_question_bank", 
            "Question bank feature coming soon", 
            Map.of("questions", List.of()));
    }
    
    private ToolResult getStudentResults(Long examId) {
        return getExamAttempts(examId);
    }
    
    private ToolResult getExamResults(Long examId) {
        return getExamAttempts(examId);
    }
}

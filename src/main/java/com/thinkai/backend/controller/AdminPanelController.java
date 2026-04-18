package com.thinkai.backend.controller;

import com.thinkai.backend.dto.AdminQuestionBankResponse;
import com.thinkai.backend.dto.AdminQuestionBankRequest;
import com.thinkai.backend.dto.AdminStatsResponse;
import com.thinkai.backend.dto.AiAgentTraceDto;
import com.thinkai.backend.dto.AdminCourseRequest;
import com.thinkai.backend.dto.AdminCourseResponse;
import com.thinkai.backend.dto.AdminUserResponse;
import com.thinkai.backend.dto.AdminAiRuntimeSettingsDto;
import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.entity.QuestionBank;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.CourseReview;
import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.Payment;
import com.thinkai.backend.entity.AdminAuditLog;
import com.thinkai.backend.repository.QuestionBankRepository;
import com.thinkai.backend.repository.ExamRepository;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.CourseReviewRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.PaymentRepository;
import com.thinkai.backend.repository.AdminAuditLogRepository;
import com.thinkai.backend.security.AdminOnly;
import com.thinkai.backend.service.AdminStatsService;
import com.thinkai.backend.service.AiSettingsService;
import com.thinkai.backend.service.AiRuntimeSettingsService;
import com.thinkai.backend.service.aitutor.AiAgentTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/admin", "/admin-panel"})
@RequiredArgsConstructor
public class AdminPanelController {

    private final AdminStatsService adminStatsService;
    private final AiAgentTraceService aiAgentTraceService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final QuestionBankRepository questionBankRepository;
    private final ExamRepository examRepository;
    private final AiSettingsService aiSettingsService;
    private final AiRuntimeSettingsService aiRuntimeSettingsService;
    private final CourseReviewRepository courseReviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    @AdminOnly
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        AdminStatsResponse stats = adminStatsService.getStats();
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalUsers", stats.getTotalUsers());
        dashboard.put("totalCourses", stats.getTotalCourses());
        dashboard.put("totalEnrollments", stats.getTotalEnrollments());
        dashboard.put("aiChatsToday", 0); // TODO: implement if needed
        return ResponseEntity.ok(ApiResponse.success("Dashboard data", dashboard));
    }

    @AdminOnly
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminStatsService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Admin system stats", stats));
    }

    @AdminOnly
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) User.ApprovalStatus approvalStatus) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.searchAdminUsers(normalizedKeyword, role, isActive, approvalStatus, pageRequest);
        
        Page<AdminUserResponse> response = userPage.map(user -> AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .approvalStatus(user.getEffectiveApprovalStatus())
                .build());
        
        return ResponseEntity.ok(ApiResponse.success("User list", response));
    }

    @AdminOnly
    @PutMapping("/users/{userId}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.markApproved();
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("isActive", true);
        result.put("approvalStatus", user.getEffectiveApprovalStatus().name());
        result.put("approved", true);
        return ResponseEntity.ok(ApiResponse.success("User approved", result));
    }

    @AdminOnly
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Boolean isActive = body.get("isActive");
        if (Boolean.TRUE.equals(isActive)) {
            user.markApproved();
        } else {
            user.markBlocked();
        }
        userRepository.save(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("isActive", user.getIsActive());
        result.put("approvalStatus", user.getEffectiveApprovalStatus().name());
        
        return ResponseEntity.ok(ApiResponse.success("User status updated", result));
    }

    @AdminOnly
    @PutMapping("/users/bulk-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUsersBulkStatus(
            @RequestBody Map<String, Object> body) {
        Object userIdsRaw = body.get("userIds");
        Object isActiveRaw = body.get("isActive");

        if (!(userIdsRaw instanceof List<?> userIdListRaw) || userIdListRaw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userIds is required");
        }
        if (!(isActiveRaw instanceof Boolean isActive)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isActive is required");
        }

        List<Long> userIds = userIdListRaw.stream()
                .map(item -> {
                    if (item instanceof Number number) return number.longValue();
                    if (item instanceof String value && !value.isBlank()) {
                        try {
                            return Long.parseLong(value.trim());
                        } catch (NumberFormatException ignored) {
                            return null;
                        }
                    }
                    return null;
                })
                .filter(id -> id != null && id > 0)
                .toList();

        if (userIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userIds is invalid");
        }

        List<User> users = userRepository.findAllById(userIds);
        if (users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No users found");
        }

        for (User user : users) {
            if (isActive) {
                user.markApproved();
            } else {
                user.markBlocked();
            }
        }
        userRepository.saveAll(users);

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", users.size());
        result.put("isActive", isActive);
        result.put("approvalStatus", isActive ? User.ApprovalStatus.APPROVED.name() : User.ApprovalStatus.BLOCKED.name());
        result.put("userIds", users.stream().map(User::getId).toList());
        return ResponseEntity.ok(ApiResponse.success("Users status updated", result));
    }

    @AdminOnly
    @PutMapping("/users/bulk-status-filter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUsersBulkStatusByFilter(
            @RequestBody Map<String, Object> body) {
        Object isActiveRaw = body.get("isActive");
        if (!(isActiveRaw instanceof Boolean isActive)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isActive is required");
        }

        String keyword = null;
        if (body.get("keyword") instanceof String k && !k.isBlank()) {
            keyword = k.trim();
        }

        User.Role role = null;
        if (body.get("role") instanceof String roleText && !roleText.isBlank()) {
            try {
                role = User.Role.valueOf(roleText.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
            }
        }

        Boolean currentIsActive = null;
        if (body.containsKey("currentIsActive")) {
            Object currentIsActiveRaw = body.get("currentIsActive");
            if (currentIsActiveRaw != null && !(currentIsActiveRaw instanceof Boolean)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currentIsActive must be boolean or null");
            }
            currentIsActive = (Boolean) currentIsActiveRaw;
        }

        User.ApprovalStatus currentApprovalStatus = null;
        if (body.get("currentApprovalStatus") instanceof String statusText && !statusText.isBlank()) {
            try {
                currentApprovalStatus = User.ApprovalStatus.valueOf(statusText.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid currentApprovalStatus");
            }
        }

        List<User> users = userRepository.searchAdminUsersNoPage(keyword, role, currentIsActive, currentApprovalStatus);
        if (users.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("updatedCount", 0);
            emptyResult.put("isActive", isActive);
            emptyResult.put("userIds", List.of());
            return ResponseEntity.ok(ApiResponse.success("No users matched filter", emptyResult));
        }

        for (User user : users) {
            if (isActive) {
                user.markApproved();
            } else {
                user.markBlocked();
            }
        }
        userRepository.saveAll(users);

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", users.size());
        result.put("isActive", isActive);
        result.put("approvalStatus", isActive ? User.ApprovalStatus.APPROVED.name() : User.ApprovalStatus.BLOCKED.name());
        result.put("userIds", users.stream().map(User::getId).toList());
        return ResponseEntity.ok(ApiResponse.success("Users status updated by filter", result));
    }

    @AdminOnly
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<Page<AdminCourseResponse>>> getCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Course> coursePage = courseRepository.findAll(pageRequest);
        
        Page<AdminCourseResponse> response = coursePage.map(course -> AdminCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .price(course.getPrice())
                .instructorId(course.getInstructorId())
                .isPublished(course.getIsPublished())
                .status(course.getStatus())
                .build());
        
        return ResponseEntity.ok(ApiResponse.success("Course list", response));
    }

    @AdminOnly
    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCourse(@RequestBody AdminCourseRequest request) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .instructorId(request.getInstructorId())
                .thumbnailUrl(request.getThumbnailUrl())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .status(request.getStatus() != null ? request.getStatus() : Course.Status.DRAFT)
                .build();
        
        course = courseRepository.save(course);
        
        Map<String, Object> result = new HashMap<>();
        result.put("courseId", course.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Course created", result));
    }

    @AdminOnly
    @PutMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<AdminCourseResponse>> updateCourse(
            @PathVariable Long courseId,
            @RequestBody AdminCourseRequest request) {
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice());
        course.setInstructorId(request.getInstructorId());
        course.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getIsPublished() != null) {
            course.setIsPublished(request.getIsPublished());
        }
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }
        
        course = courseRepository.save(course);
        
        return ResponseEntity.ok(ApiResponse.success("Course updated", AdminCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .price(course.getPrice())
                .instructorId(course.getInstructorId())
                .isPublished(course.getIsPublished())
                .status(course.getStatus())
                .build()));
    }

    @AdminOnly
    @PutMapping("/courses/{courseId}/block")
    public ResponseEntity<ApiResponse<Map<String, Object>>> blockCourse(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> body) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean blocked = Boolean.TRUE.equals(body.get("blocked"));

        if (blocked) {
            course.setStatus(Course.Status.BLOCKED);
            course.setIsPublished(false);
        } else {
            Course.Status restoreStatus = Course.Status.PENDING;
            Object restoreStatusRaw = body.get("restoreStatus");
            if (restoreStatusRaw instanceof String statusText && !statusText.isBlank()) {
                try {
                    restoreStatus = Course.Status.valueOf(statusText.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid restoreStatus");
                }
            }
            if (restoreStatus == Course.Status.BLOCKED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "restoreStatus cannot be BLOCKED");
            }
            course.setStatus(restoreStatus);
            course.setIsPublished(restoreStatus == Course.Status.APPROVED);
        }

        courseRepository.save(course);

        Map<String, Object> result = new HashMap<>();
        result.put("courseId", course.getId());
        result.put("status", course.getStatus().name());
        result.put("isPublished", course.getIsPublished());
        result.put("blocked", course.getStatus() == Course.Status.BLOCKED);
        return ResponseEntity.ok(ApiResponse.success("Course block status updated", result));
    }

    @AdminOnly
    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        courseRepository.deleteById(courseId);
        return ResponseEntity.ok(ApiResponse.success("Course deleted", null));
    }

    @AdminOnly
    @PutMapping("/settings/ai-prompts")
    public ResponseEntity<ApiResponse<Boolean>> updateAIPrompts(@RequestBody Map<String, String> body) {
        String tutorSystemPrompt = body.get("tutorSystemPrompt");
        String examGeneratorPrompt = body.get("examGeneratorPrompt");
        
        if (tutorSystemPrompt != null) {
            aiSettingsService.updateSetting("tutorSystemPrompt", tutorSystemPrompt);
        }
        if (examGeneratorPrompt != null) {
            aiSettingsService.updateSetting("examGeneratorPrompt", examGeneratorPrompt);
        }
        
        return ResponseEntity.ok(ApiResponse.success("AI prompts updated", true));
    }

    @AdminOnly
    @GetMapping("/settings/ai-runtime")
    public ResponseEntity<ApiResponse<AdminAiRuntimeSettingsDto>> getAiRuntimeSettings() {
        return ResponseEntity.ok(ApiResponse.success("AI runtime settings", aiRuntimeSettingsService.getSettings()));
    }

    @AdminOnly
    @PutMapping("/settings/ai-runtime")
    public ResponseEntity<ApiResponse<AdminAiRuntimeSettingsDto>> updateAiRuntimeSettings(
            @RequestBody AdminAiRuntimeSettingsDto body,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "unknown-admin";
        AdminAiRuntimeSettingsDto updated = aiRuntimeSettingsService.updateSettings(body, actor);
        return ResponseEntity.ok(ApiResponse.success("AI runtime settings updated", updated));
    }

    @AdminOnly
    @GetMapping("/ai/traces")
    public ResponseEntity<ApiResponse<List<AiAgentTraceDto>>> getAiTraces(
            @RequestParam(required = false) String conversationId) {
        List<AiAgentTraceDto> traces = aiAgentTraceService.getAllTraces(
                conversationId,
                null,
                true
        );
        return ResponseEntity.ok(ApiResponse.success("AI traces", traces));
    }

    @AdminOnly
    @GetMapping("/ai/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiStats() {
        List<AiAgentTraceDto> allTraces = aiAgentTraceService.getAllTraces(null, null, true);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTraces", allTraces.size());
        
        long totalLatency = allTraces.stream()
                .filter(t -> t.getLatencyMs() != null)
                .mapToLong(t -> t.getLatencyMs())
                .sum();
        stats.put("avgLatencyMs", allTraces.isEmpty() ? 0 : totalLatency / allTraces.size());
        
        int totalInputTokens = allTraces.stream()
                .filter(t -> t.getInputTokens() != null)
                .mapToInt(t -> t.getInputTokens())
                .sum();
        stats.put("totalInputTokens", totalInputTokens);
        
        int totalOutputTokens = allTraces.stream()
                .filter(t -> t.getOutputTokens() != null)
                .mapToInt(t -> t.getOutputTokens())
                .sum();
        stats.put("totalOutputTokens", totalOutputTokens);
        
        Map<String, Long> agentUsage = new HashMap<>();
        allTraces.stream()
                .filter(t -> t.getAgentType() != null)
                .forEach(t -> {
                    String key = t.getAgentType().name();
                    agentUsage.put(key, agentUsage.getOrDefault(key, 0L) + 1);
                });
        stats.put("agentUsage", agentUsage);
        
        return ResponseEntity.ok(ApiResponse.success("AI stats", stats));
    }

    @AdminOnly
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AdminAuditLog> logPage = adminAuditLogRepository.search(actor, action, resourceType, pageRequest);

        Page<Map<String, Object>> response = logPage.map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("actor", log.getActor());
            map.put("action", log.getAction());
            map.put("resourceType", log.getResourceType());
            map.put("resourceKey", log.getResourceKey());
            map.put("diffSummary", log.getDiffSummary());
            map.put("beforeSnapshot", log.getBeforeSnapshot());
            map.put("afterSnapshot", log.getAfterSnapshot());
            map.put("createdAt", log.getCreatedAt());
            return map;
        });

        return ResponseEntity.ok(ApiResponse.success("Audit logs", response));
    }

    @AdminOnly
    @GetMapping("/question-banks")
    public ResponseEntity<ApiResponse<Page<AdminQuestionBankResponse>>> getQuestionBanks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<QuestionBank> questionBankPage = questionBankRepository.findAll(pageRequest);
        
        Page<AdminQuestionBankResponse> response = questionBankPage.map(qb -> 
            AdminQuestionBankResponse.builder()
                .id(qb.getId())
                .examType(qb.getExamType())
                .section(qb.getSection())
                .part(qb.getPart())
                .content(qb.getContent())
                .options(qb.getOptions())
                .correctAnswer(qb.getCorrectAnswer())
                .explanation(qb.getExplanation())
                .audioUrl(qb.getAudioUrl())
                .imageUrl(qb.getImageUrl())
                .difficulty(qb.getDifficulty())
                .tags(qb.getTags())
                .createdBy(qb.getCreatedBy())
                .createdAt(qb.getCreatedAt())
                .build()
        );
        
        return ResponseEntity.ok(ApiResponse.success("Question bank list", response));
    }

    @AdminOnly
    @GetMapping("/exams")
    public ResponseEntity<ApiResponse<Page<ExamDto>>> getExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Exam> examPage = examRepository.findAll(pageRequest);
        
        Page<ExamDto> response = examPage.map(exam -> ExamDto.builder()
                .id(exam.getId())
                .examType(exam.getExamType())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .timeLimitMinutes(exam.getTimeLimitMinutes())
                .passingScore(exam.getPassingScore())
                .createdAt(exam.getCreatedAt())
                .build());
        
        return ResponseEntity.ok(ApiResponse.success("Exam list", response));
    }

    // ==================== EXAMS CRUD ====================
    @AdminOnly
    @PostMapping("/exams")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createExam(@RequestBody ExamDto request) {
        Exam exam = Exam.builder()
                .courseId(request.getCourseId())
                .title(request.getTitle())
                .description(request.getDescription())
                .examType(request.getExamType())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passingScore(request.getPassingScore())
                .build();
        
        exam = examRepository.save(exam);
        Map<String, Object> result = new HashMap<>();
        result.put("examId", exam.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exam created", result));
    }

    @AdminOnly
    @PutMapping("/exams/{examId}")
    public ResponseEntity<ApiResponse<ExamDto>> updateExam(
            @PathVariable Long examId,
            @RequestBody ExamDto request) {
        
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setExamType(request.getExamType());
        exam.setTimeLimitMinutes(request.getTimeLimitMinutes());
        exam.setPassingScore(request.getPassingScore());
        
        exam = examRepository.save(exam);
        
        return ResponseEntity.ok(ApiResponse.success("Exam updated", ExamDto.builder()
                .id(exam.getId())
                .courseId(exam.getCourseId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .examType(exam.getExamType())
                .timeLimitMinutes(exam.getTimeLimitMinutes())
                .passingScore(exam.getPassingScore())
                .build()));
    }

    @AdminOnly
    @DeleteMapping("/exams/{examId}")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long examId) {
        if (!examRepository.existsById(examId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        }
        examRepository.deleteById(examId);
        return ResponseEntity.ok(ApiResponse.success("Exam deleted", null));
    }

    // ==================== QUESTION BANKS CRUD ====================
    @AdminOnly
    @PostMapping("/question-banks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createQuestionBank(@RequestBody AdminQuestionBankRequest request) {
        QuestionBank qb = QuestionBank.builder()
                .examType(request.getExamType())
                .section(request.getSection())
                .part(request.getPart())
                .content(request.getContent())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .explanation(request.getExplanation())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .difficulty(request.getDifficulty())
                .tags(request.getTags())
                .build();
        
        qb = questionBankRepository.save(qb);
        Map<String, Object> result = new HashMap<>();
        result.put("questionBankId", qb.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question bank created", result));
    }

    @AdminOnly
    @PutMapping("/question-banks/{questionBankId}")
    public ResponseEntity<ApiResponse<AdminQuestionBankResponse>> updateQuestionBank(
            @PathVariable Long questionBankId,
            @RequestBody AdminQuestionBankRequest request) {
        
        QuestionBank qb = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question bank not found"));
        
        qb.setExamType(request.getExamType());
        qb.setSection(request.getSection());
        qb.setPart(request.getPart());
        qb.setContent(request.getContent());
        qb.setOptions(request.getOptions());
        qb.setCorrectAnswer(request.getCorrectAnswer());
        qb.setExplanation(request.getExplanation());
        qb.setAudioUrl(request.getAudioUrl());
        qb.setImageUrl(request.getImageUrl());
        qb.setDifficulty(request.getDifficulty());
        qb.setTags(request.getTags());
        
        qb = questionBankRepository.save(qb);
        
        return ResponseEntity.ok(ApiResponse.success("Question bank updated", AdminQuestionBankResponse.builder()
                .id(qb.getId())
                .examType(qb.getExamType())
                .section(qb.getSection())
                .part(qb.getPart())
                .content(qb.getContent())
                .options(qb.getOptions())
                .correctAnswer(qb.getCorrectAnswer())
                .explanation(qb.getExplanation())
                .audioUrl(qb.getAudioUrl())
                .imageUrl(qb.getImageUrl())
                .difficulty(qb.getDifficulty())
                .tags(qb.getTags())
                .build()));
    }

    @AdminOnly
    @DeleteMapping("/question-banks/{questionBankId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestionBank(@PathVariable Long questionBankId) {
        if (!questionBankRepository.existsById(questionBankId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question bank not found");
        }
        questionBankRepository.deleteById(questionBankId);
        return ResponseEntity.ok(ApiResponse.success("Question bank deleted", null));
    }

    // ==================== REVIEWS CRUD ====================
    @AdminOnly
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<CourseReview> reviewPage = courseReviewRepository.findAll(pageRequest);
        
        Page<Map<String, Object>> response = reviewPage.map(review -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", review.getId());
            map.put("courseId", review.getCourseId());
            map.put("userId", review.getUserId());
            map.put("rating", review.getRating());
            map.put("comment", review.getReviewText());
            map.put("createdAt", review.getCreatedAt());
            return map;
        });
        
        return ResponseEntity.ok(ApiResponse.success("Review list", response));
    }

    @AdminOnly
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> body) {
        
        CourseReview review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        
        if (body.containsKey("rating")) {
            review.setRating((Integer) body.get("rating"));
        }
        if (body.containsKey("comment")) {
            review.setReviewText((String) body.get("comment"));
        }
        
        review = courseReviewRepository.save(review);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", review.getId());
        result.put("rating", review.getRating());
        result.put("comment", review.getReviewText());
        
        return ResponseEntity.ok(ApiResponse.success("Review updated", result));
    }

    @AdminOnly
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        if (!courseReviewRepository.existsById(reviewId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
        }
        courseReviewRepository.deleteById(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }

    // ==================== ENROLLMENTS ====================
    @AdminOnly
    @GetMapping("/enrollments")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long userId) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Enrollment> enrollmentPage = enrollmentRepository.findAll(pageRequest);
        
        Page<Map<String, Object>> response = enrollmentPage.map(enrollment -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", enrollment.getId());
            map.put("userId", enrollment.getUserId());
            map.put("courseId", enrollment.getCourseId());
            map.put("progressPercent", enrollment.getProgressPercent());
            map.put("enrolledAt", enrollment.getEnrolledAt());
            return map;
        });
        
        return ResponseEntity.ok(ApiResponse.success("Enrollment list", response));
    }

    @AdminOnly
    @PutMapping("/enrollments/{enrollmentId}/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleEnrollment(
            @PathVariable Long enrollmentId) {
        
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        
        boolean newStatus = !enrollment.getIsActive();
        enrollment.setIsActive(newStatus);
        enrollmentRepository.save(enrollment);
        
        Map<String, Object> result = new HashMap<>();
        result.put("enrollmentId", enrollmentId);
        result.put("isActive", newStatus);
        
        return ResponseEntity.ok(ApiResponse.success("Enrollment status toggled", result));
    }

    // ==================== PAYMENTS ====================
    @AdminOnly
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentRepository.findAll(pageRequest);
        
        Page<Map<String, Object>> response = paymentPage.map(payment -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", payment.getId());
            map.put("userId", payment.getUserId());
            map.put("amount", payment.getAmount());
            map.put("status", payment.getStatus());
            map.put("createdAt", payment.getCreatedAt());
            return map;
        });
        
        return ResponseEntity.ok(ApiResponse.success("Payment list", response));
    }

    @AdminOnly
    @PutMapping("/payments/{paymentId}/refund")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refundPayment(
            @PathVariable Long paymentId) {
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        
        // Mark as refunded
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
        
        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", paymentId);
        result.put("status", "CANCELLED");
        
        return ResponseEntity.ok(ApiResponse.success("Payment refunded", result));
    }
}

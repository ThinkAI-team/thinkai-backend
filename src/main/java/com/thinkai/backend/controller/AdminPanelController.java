package com.thinkai.backend.controller;

import com.thinkai.backend.dto.AdminQuestionBankResponse;
import com.thinkai.backend.dto.AdminStatsResponse;
import com.thinkai.backend.dto.AiAgentTraceDto;
import com.thinkai.backend.dto.AdminCourseRequest;
import com.thinkai.backend.dto.AdminCourseResponse;
import com.thinkai.backend.dto.AdminUserResponse;
import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.entity.QuestionBank;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.repository.QuestionBankRepository;
import com.thinkai.backend.repository.ExamRepository;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.security.AdminOnly;
import com.thinkai.backend.service.AdminStatsService;
import com.thinkai.backend.service.AiSettingsService;
import com.thinkai.backend.service.aitutor.AiAgentTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @RequestParam(required = false) Boolean isActive) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageRequest);
        
        Page<AdminUserResponse> response = userPage.map(user -> AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build());
        
        return ResponseEntity.ok(ApiResponse.success("User list", response));
    }

    @AdminOnly
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Boolean isActive = body.get("isActive");
        user.setIsActive(isActive != null ? isActive : true);
        userRepository.save(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("isActive", user.getIsActive());
        
        return ResponseEntity.ok(ApiResponse.success("User status updated", result));
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
}

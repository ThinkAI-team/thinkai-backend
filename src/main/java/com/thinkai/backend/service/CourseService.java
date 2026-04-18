package com.thinkai.backend.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.dto.CourseListResponse;
import com.thinkai.backend.dto.CourseRequest;
import com.thinkai.backend.dto.EnrollmentResponse;
import com.thinkai.backend.dto.LessonResponse;
import com.thinkai.backend.dto.MyCourseResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.CartItemRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.LessonProgressRepository;
import com.thinkai.backend.repository.LessonRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * GET /courses — List published courses with filters, search and pagination.
     */
    public Map<String, Object> getPublishedCourses(
            String keyword,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String sortBy,
            String sortDir,
            int page,
            int size) {
        // Giới hạn size tối đa 50
        size = Math.min(size, 50);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Course> coursePage = courseRepository.findPublishedCourses(
                keyword, priceMin, priceMax, pageable);

        List<CourseListResponse> content = coursePage.getContent().stream()
                .map(this::toCourseListResponse)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("page", coursePage.getNumber());
        response.put("size", coursePage.getSize());
        response.put("totalElements", coursePage.getTotalElements());
        response.put("totalPages", coursePage.getTotalPages());

        return response;
    }

    /**
     * GET /courses/{id} — Course details with lessons and enrollment status.
     */
    public CourseDetailResponse getCourseDetail(Long courseId, Long currentUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId,
                        HttpStatus.NOT_FOUND));

        boolean isInstructor = currentUserId != null
                && course.getInstructorId() != null
                && course.getInstructorId().equals(currentUserId);

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        Boolean isEnrolled = false;
        Integer progressPercent = 0;
        if (currentUserId != null) {
            Enrollment enrollment = enrollmentRepository
                    .findByUserIdAndCourseId(currentUserId, courseId)
                    .orElse(null);
            if (enrollment != null) {
                isEnrolled = true;
                progressPercent = enrollment.getProgressPercent();
            }
        }

        // Public users or non-related users cannot view unpublished courses.
        if (!Boolean.TRUE.equals(course.getIsPublished()) && !isInstructor && !isEnrolled) {
            throw new ApiException("Khóa học chưa được xuất bản", HttpStatus.FORBIDDEN);
        }
        if (course.getStatus() == Course.Status.BLOCKED && !isInstructor) {
            throw new ApiException("Khóa học đã bị khóa bởi quản trị viên", HttpStatus.FORBIDDEN);
        }

        // Get instructor name
        String instructorName = null;
        if (course.getInstructorId() != null) {
            User instructor = userRepository.findById(course.getInstructorId()).orElse(null);
            if (instructor != null) {
                instructorName = instructor.getFullName();
            }
        }

        Set<Long> completedLessonIds = (currentUserId == null || lessons.isEmpty())
                ? Set.of()
                : lessonProgressRepository.findByUserIdAndLessonIdIn(
                        currentUserId,
                        lessons.stream().map(Lesson::getId).toList())
                        .stream()
                        .filter(progress -> Boolean.TRUE.equals(progress.getIsCompleted()))
                        .map(progress -> progress.getLessonId())
                        .collect(Collectors.toSet());

        List<LessonResponse> lessonResponses = lessons.stream()
                .map(lesson -> LessonResponse.builder()
                        .id(lesson.getId())
                        .title(lesson.getTitle())
                        .type(lesson.getType().name())
                        .duration(formatDuration(lesson.getDurationSeconds()))
                        .isCompleted(completedLessonIds.contains(lesson.getId()))
                        .orderIndex(lesson.getOrderIndex())
                        .build())
                .toList();

        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .instructorName(instructorName)
                .price(course.getPrice())
                .isEnrolled(isEnrolled)
                .progressPercent(progressPercent)
                .lessons(lessonResponses)
                .build();
    }

    // ===================== TEACHER OPERATIONS =====================

    public Course createCourse(Long teacherId, CourseRequest request) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .price(request.getPrice())
                .instructorId(teacherId)
                .isPublished(false)
                .status(Course.Status.DRAFT)
                .build();
        Course saved = courseRepository.save(course);
        notificationService.notifyStudentsWhenCourseCreated(teacherId, saved);
        return saved;
    }

    public Page<Course> getCoursesByTeacher(Long teacherId, Pageable pageable) {
        return courseRepository.findByInstructorId(teacherId, pageable);
    }

    public Course getCourseByIdAndTeacher(Long courseId, Long teacherId) {
        return courseRepository.findByIdAndInstructorId(courseId, teacherId)
                .orElseThrow(
                        () -> new ApiException("Không tìm thấy khóa học với ID: " + courseId, HttpStatus.NOT_FOUND));
    }

    public Course updateCourse(Long courseId, Long teacherId, CourseRequest request) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setPrice(request.getPrice());
        return courseRepository.save(course);
    }

    public void deleteCourse(Long courseId, Long teacherId) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);
        courseRepository.delete(course);
    }

    public Course publishCourse(Long courseId, Long teacherId) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);
        course.setIsPublished(true);
        course.setStatus(Course.Status.APPROVED);
        return courseRepository.save(course);
    }

    private static final String UPLOAD_DIR = "uploads/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    @Value("${app.backend-url:}")
    private String backendUrl;

    @Transactional
    public String uploadThumbnail(Long courseId, Long teacherId, org.springframework.web.multipart.MultipartFile file) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);

        if (file.isEmpty()) {
            throw new ApiException("File không được để trống", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            String sizeInMB = String.format("%.0f MB", file.getSize() / (1024.0 * 1024));
            throw new ApiException("File quá lớn (" + sizeInMB + "). Giới hạn tối đa: 10 MB", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ApiException("Chỉ chấp nhận file ảnh (JPEG, PNG, GIF, WEBP)", HttpStatus.BAD_REQUEST);
        }

        try {
            java.io.File uploadDirObj = new java.io.File(UPLOAD_DIR);
            if (!uploadDirObj.exists()) {
                uploadDirObj.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = java.util.UUID.randomUUID().toString() + extension;
            java.nio.file.Path path = java.nio.file.Paths.get(UPLOAD_DIR + newFilename).toAbsolutePath();
            
            // Use copy from stream to bypass MultipartFile temporary path issues
            java.nio.file.Files.copy(file.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String normalizedBackendUrl = backendUrl == null ? "" : backendUrl.trim();
            if (!normalizedBackendUrl.isBlank() && normalizedBackendUrl.endsWith("/")) {
                normalizedBackendUrl = normalizedBackendUrl.substring(0, normalizedBackendUrl.length() - 1);
            }
            String fileUrl = (normalizedBackendUrl.isBlank() ? "" : normalizedBackendUrl) + "/api/files/" + newFilename;
            
            course.setThumbnailUrl(fileUrl);
            courseRepository.save(course);

            return fileUrl;
        } catch (java.io.IOException e) {
            throw new ApiException("Lỗi khi lưu file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST /courses/{id}/enroll — Enroll in a course.
     */
    @Transactional
    public EnrollmentResponse enrollCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId,
                        HttpStatus.NOT_FOUND));

        if (!course.getIsPublished()) {
            throw new ApiException("Khóa học chưa được xuất bản", HttpStatus.BAD_REQUEST);
        }
        if (course.getStatus() == Course.Status.BLOCKED) {
            throw new ApiException("Khóa học đã bị khóa bởi quản trị viên", HttpStatus.BAD_REQUEST);
        }
        if (course.getStatus() != Course.Status.APPROVED) {
            throw new ApiException("Khóa học chưa sẵn sàng để đăng ký", HttpStatus.BAD_REQUEST);
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ApiException("Bạn đã đăng ký khóa học này rồi", HttpStatus.BAD_REQUEST);
        }

        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .courseId(courseId)
                .progressPercent(0)
                .isActive(true)
                .build();

        enrollment = enrollmentRepository.save(enrollment);
        cartItemRepository.deleteByCartUserIdAndCourseId(userId, courseId);

        return EnrollmentResponse.builder()
                .enrollmentId(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }

    /**
     * DELETE /enrollments/{courseId} — Hủy đăng ký khóa học.
     */
    @Transactional
    public void unenrollCourse(Long courseId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ApiException("Bạn chưa đăng ký khóa học này", HttpStatus.BAD_REQUEST));

        enrollmentRepository.delete(enrollment);
    }

    /**
     * GET /users/me/courses — User's enrolled courses.
     */
    public List<MyCourseResponse> getMyCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);

        return enrollments.stream().map(enrollment -> {
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course == null) {
                return null;
            }
            if (course.getStatus() == Course.Status.BLOCKED) {
                return null;
            }

            List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
            String nextLessonTitle = null;
            Long nextLessonId = null;
            if (!lessons.isEmpty()) {
                // Tạm lấy bài học đầu tiên hoặc tính theo progress
                int completedCount = (int) Math.round(
                        lessons.size() * enrollment.getProgressPercent() / 100.0);
                int nextIndex = Math.min(completedCount, lessons.size() - 1);
                Lesson nextLesson = lessons.get(nextIndex);
                nextLessonTitle = nextLesson.getTitle();
                nextLessonId = nextLesson.getId();
            }

            MyCourseResponse.NextLessonInfo nextLessonInfo = null;
            if (nextLessonId != null) {
                nextLessonInfo = MyCourseResponse.NextLessonInfo.builder()
                        .id(nextLessonId)
                        .title(nextLessonTitle)
                        .build();
            }

            return MyCourseResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .thumbnail(course.getThumbnailUrl())
                    .price(course.getPrice())
                    .progressPercent(enrollment.getProgressPercent())
                    .enrolledAt(enrollment.getEnrolledAt())
                    .nextLesson(nextLessonInfo)
                    .build();
        }).filter(r -> r != null).toList();
    }

    // ===================== PRIVATE HELPER =====================

    private CourseListResponse toCourseListResponse(Course course) {
        CourseListResponse.InstructorInfo instructorInfo = null;
        if (course.getInstructorId() != null) {
            User instructor = userRepository.findById(course.getInstructorId()).orElse(null);
            if (instructor != null) {
                instructorInfo = CourseListResponse.InstructorInfo.builder()
                        .id(instructor.getId())
                        .fullName(instructor.getFullName())
                        .build();
            }
        }

        return CourseListResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .thumbnail(course.getThumbnailUrl())
                .price(course.getPrice())
                .instructor(instructorInfo)
                .lessonsCount((int) lessonRepository.countByCourseId(course.getId()))
                .enrolledCount((int) enrollmentRepository.countByCourseId(course.getId()))
                .build();
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds == 0) {
            return null;
        }
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }
}

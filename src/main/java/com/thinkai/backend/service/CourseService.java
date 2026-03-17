package com.thinkai.backend.service;

import com.thinkai.backend.dto.*;
import com.thinkai.backend.entity.*;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;

    /**
     * GET /courses — Danh sách khóa học published + filter/search/paginate
     */
    public Map<String, Object> getPublishedCourses(
            String keyword,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String sortBy,
            String sortDir,
            int page,
            int size
    ) {
        size = Math.min(size, 50);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Course> coursePage = courseRepository.findPublishedCourses(keyword, priceMin, priceMax, pageable);

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
     * Lấy chi tiết khóa học kèm danh sách bài + progress cho sidebar.
     * Auth: Optional (nếu đã login → trả thêm isEnrolled, progressPercent, isCompleted per lesson).
     */
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId, String email) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Khóa học không tồn tại", HttpStatus.NOT_FOUND));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        InstructorDto instructor = null;
        if (course.getInstructorId() != null) {
            instructor = userRepository.findById(course.getInstructorId())
                    .map(u -> InstructorDto.builder()
                            .id(u.getId())
                            .fullName(u.getFullName())
                            .avatarUrl(u.getAvatarUrl())
                            .build())
                    .orElse(null);
        }

        boolean isEnrolled = false;
        int progressPercent = 0;
        Map<Long, LessonProgress> progressMap = Map.of();

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                Enrollment enrollment = enrollmentRepository
                        .findByUserIdAndCourseId(user.getId(), courseId).orElse(null);
                if (enrollment != null) {
                    isEnrolled = true;
                    progressPercent = enrollment.getProgressPercent();

                    List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
                    progressMap = lessonProgressRepository
                            .findByUserIdAndLessonIdIn(user.getId(), lessonIds)
                            .stream()
                            .collect(Collectors.toMap(LessonProgress::getLessonId, p -> p));
                }
            }
        }

        final Map<Long, LessonProgress> finalProgressMap = progressMap;
        List<LessonSummaryDto> lessonDtos = lessons.stream()
                .map(lesson -> {
                    LessonProgress progress = finalProgressMap.get(lesson.getId());
                    return LessonSummaryDto.builder()
                            .id(lesson.getId())
                            .title(lesson.getTitle())
                            .type(lesson.getType().name())
                            .duration(formatDuration(lesson.getDurationSeconds()))
                            .isCompleted(progress != null && Boolean.TRUE.equals(progress.getIsCompleted()))
                            .orderIndex(lesson.getOrderIndex())
                            .build();
                })
                .toList();

        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnail(course.getThumbnailUrl())
                .price(course.getPrice() != null ? course.getPrice().doubleValue() : 0)
                .instructor(instructor)
                .isEnrolled(isEnrolled)
                .progressPercent(progressPercent)
                .lessons(lessonDtos)
                .build();
    }

    /**
     * Đăng ký khóa học cho student.
     */
    @Transactional
    public EnrollResponse enrollCourse(Long courseId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Khóa học không tồn tại", HttpStatus.NOT_FOUND));

        if (!course.getIsPublished()) {
            throw new ApiException("Khóa học chưa được xuất bản", HttpStatus.BAD_REQUEST);
        }

        if (enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId).isPresent()) {
            throw new ApiException("Bạn đã đăng ký khóa học này rồi", HttpStatus.CONFLICT);
        }

        Enrollment enrollment = Enrollment.builder()
                .userId(user.getId())
                .courseId(course.getId())
                .progressPercent(0)
                .build();
        enrollmentRepository.save(enrollment);

        log.info("Student {} enrolled in course {}", email, courseId);

        return EnrollResponse.builder()
                .enrollmentId(enrollment.getId())
                .courseId(course.getId())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }

    /**
     * GET /users/me/courses — Khóa học của tôi
     */
    public List<MyCourseResponse> getMyCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);

        return enrollments.stream().map(enrollment -> {
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course == null) return null;

            List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
            String nextLessonTitle = null;
            Long nextLessonId = null;
            if (!lessons.isEmpty()) {
                int completedCount = (int) Math.round(lessons.size() * enrollment.getProgressPercent() / 100.0);
                int nextIndex = Math.min(completedCount, lessons.size() - 1);
                Lesson nextLesson = lessons.get(nextIndex);
                nextLessonTitle = nextLesson.getTitle();
                nextLessonId = nextLesson.getId();
            }

            return MyCourseResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .thumbnail(course.getThumbnailUrl())
                    .price(course.getPrice())
                    .progressPercent(enrollment.getProgressPercent())
                    .nextLessonTitle(nextLessonTitle)
                    .nextLessonId(nextLessonId)
                    .build();
        }).filter(r -> r != null).toList();
    }

    // ===================== PRIVATE HELPERS =====================

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
        if (seconds == null || seconds == 0) return null;
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }
}

package com.thinkai.backend.service;

import com.thinkai.backend.dto.*;
import com.thinkai.backend.entity.*;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Lấy chi tiết khóa học kèm danh sách bài + progress cho sidebar.
     * Nếu user đã login → trả thêm isEnrolled, progressPercent, isCompleted per lesson.
     */
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId, String email) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Khóa học không tồn tại", HttpStatus.NOT_FOUND));

        // Lấy lessons theo thứ tự
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        // Lấy instructor
        InstructorDto instructor = null;
        if (course.getInstructorId() != null) {
            userRepository.findById(course.getInstructorId()).ifPresent(user ->
                    log.debug("Instructor found: {}", user.getFullName()));
            instructor = userRepository.findById(course.getInstructorId())
                    .map(u -> InstructorDto.builder()
                            .id(u.getId())
                            .fullName(u.getFullName())
                            .avatarUrl(u.getAvatarUrl())
                            .build())
                    .orElse(null);
        }

        // Check enrollment + progress nếu user đã login
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

                    // Batch query: lấy tất cả progress 1 lần
                    List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
                    progressMap = lessonProgressRepository
                            .findByUserIdAndLessonIdIn(user.getId(), lessonIds)
                            .stream()
                            .collect(Collectors.toMap(LessonProgress::getLessonId, p -> p));
                }
            }
        }

        // Build lesson summary list cho sidebar
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

        // Check đã enroll chưa
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
     * Format duration từ seconds → "mm:ss"
     */
    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds == 0) return null;
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }
}

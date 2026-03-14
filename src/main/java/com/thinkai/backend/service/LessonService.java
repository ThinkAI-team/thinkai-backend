package com.thinkai.backend.service;

import com.thinkai.backend.dto.LessonCompleteRequest;
import com.thinkai.backend.dto.LessonCompleteResponse;
import com.thinkai.backend.dto.LessonDetailResponse;
import com.thinkai.backend.entity.*;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;

    /**
     * Lấy nội dung chi tiết bài học (cho video player / PDF viewer).
     * Student phải đã enroll khóa học chứa bài này.
     */
    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonDetail(Long lessonId, String email) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException("Bài học không tồn tại", HttpStatus.NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        // Check enrollment
        enrollmentRepository.findByUserIdAndCourseId(user.getId(), lesson.getCourseId())
                .orElseThrow(() -> new ApiException(
                        "Bạn chưa đăng ký khóa học này", HttpStatus.FORBIDDEN));

        // Lấy course title
        String courseTitle = courseRepository.findById(lesson.getCourseId())
                .map(Course::getTitle)
                .orElse(null);

        // Lấy progress hiện tại
        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(user.getId(), lessonId).orElse(null);

        // Cập nhật last_accessed_at
        if (progress == null) {
            progress = LessonProgress.builder()
                    .userId(user.getId())
                    .lessonId(lessonId)
                    .isCompleted(false)
                    .watchTimeSeconds(0)
                    .lastAccessedAt(LocalDateTime.now())
                    .build();
            lessonProgressRepository.save(progress);
        } else {
            progress.setLastAccessedAt(LocalDateTime.now());
            lessonProgressRepository.save(progress);
        }

        return LessonDetailResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .type(lesson.getType().name())
                .contentUrl(lesson.getContentUrl())
                .contentText(lesson.getContentText())
                .durationSeconds(lesson.getDurationSeconds())
                .orderIndex(lesson.getOrderIndex())
                .courseTitle(courseTitle)
                .courseId(lesson.getCourseId())
                .isCompleted(progress.getIsCompleted())
                .watchTimeSeconds(progress.getWatchTimeSeconds())
                .build();
    }

    /**
     * Đánh dấu bài học đã hoàn thành + tính lại progress % khóa học.
     */
    @Transactional
    public LessonCompleteResponse completeLesson(Long lessonId, String email, LessonCompleteRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException("Bài học không tồn tại", HttpStatus.NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        // Check enrollment
        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(user.getId(), lesson.getCourseId())
                .orElseThrow(() -> new ApiException(
                        "Bạn chưa đăng ký khóa học này", HttpStatus.FORBIDDEN));

        // Upsert progress
        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(user.getId(), lessonId)
                .orElse(LessonProgress.builder()
                        .userId(user.getId())
                        .lessonId(lessonId)
                        .isCompleted(false)
                        .watchTimeSeconds(0)
                        .build());

        progress.setIsCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastAccessedAt(LocalDateTime.now());
        if (request.getWatchTimeSeconds() != null && request.getWatchTimeSeconds() > 0) {
            progress.setWatchTimeSeconds(request.getWatchTimeSeconds());
        }
        lessonProgressRepository.save(progress);

        // Tính lại progress % cho khóa học
        long totalLessons = lessonRepository.countByCourseId(lesson.getCourseId());
        var allLessonIds = lessonRepository.findByCourseIdOrderByOrderIndexAsc(lesson.getCourseId())
                .stream().map(Lesson::getId).toList();
        long completedLessons = lessonProgressRepository
                .findByUserIdAndLessonIdIn(user.getId(), allLessonIds)
                .stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsCompleted()))
                .count();

        double courseProgress = totalLessons > 0
                ? Math.round((double) completedLessons / totalLessons * 100.0 * 10.0) / 10.0
                : 0;

        enrollment.setProgressPercent((int) courseProgress);
        if (courseProgress >= 100) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }
        enrollmentRepository.save(enrollment);

        log.info("Student {} completed lesson {} (course progress: {}%)", email, lessonId, courseProgress);

        return LessonCompleteResponse.builder()
                .lessonId(lessonId)
                .isCompleted(true)
                .courseProgress(courseProgress)
                .build();
    }
}

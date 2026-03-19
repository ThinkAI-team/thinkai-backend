package com.thinkai.backend.service;

import com.thinkai.backend.dto.LessonCompleteRequest;
import com.thinkai.backend.dto.LessonCompleteResponse;
import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.LessonProgress;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.LessonProgressRepository;
import com.thinkai.backend.repository.LessonRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LessonProgressService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Transactional
    public LessonCompleteResponse completeLesson(String email, Long lessonId, LessonCompleteRequest request) {
        // 1. Tìm user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        // 2. Tìm lesson
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException("Không tìm thấy bài học", HttpStatus.NOT_FOUND));

        // 3. Kiểm tra enrollment
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), lesson.getCourseId())
                .orElseThrow(() -> new ApiException("Bạn chưa đăng ký khóa học này", HttpStatus.FORBIDDEN));

        // 4. Upsert lesson progress
        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder()
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

        // 5. Tính lại progress percent
        long totalLessons = lessonRepository.countByCourseId(lesson.getCourseId());
        long completedLessons = lessonProgressRepository
                .countCompletedByUserAndCourse(user.getId(), lesson.getCourseId());

        int progressPercent = totalLessons > 0
                ? (int) Math.round((double) completedLessons / totalLessons * 100)
                : 0;

        enrollment.setProgressPercent(progressPercent);

        // 6. Nếu hoàn thành 100% → set completedAt
        if (progressPercent >= 100 && enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        enrollmentRepository.save(enrollment);

        // 7. Return response
        return LessonCompleteResponse.builder()
                .lessonId(lessonId)
                .isCompleted(true)
                .courseProgress(progressPercent)
                .build();
    }
}

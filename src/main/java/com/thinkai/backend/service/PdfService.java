package com.thinkai.backend.service;

import com.thinkai.backend.dto.PdfLessonResponse;
import com.thinkai.backend.dto.PdfProgressRequest;
import com.thinkai.backend.dto.PdfProgressResponse;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.LessonProgress;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.LessonProgressRepository;
import com.thinkai.backend.repository.LessonRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /**
     * Lấy metadata bài học PDF (URL, tổng trang, thứ tự).
     * Validate: lesson tồn tại + type = PDF + user đã enroll.
     */
    @Transactional(readOnly = true)
    public PdfLessonResponse getPdfLesson(Long lessonId, String email) {
        User user = findUser(email);
        Lesson lesson = findLesson(lessonId);

        validatePdfType(lesson);
        validateEnrollment(user.getId(), lesson.getCourseId());

        return PdfLessonResponse.builder()
                .lessonId(lesson.getId())
                .title(lesson.getTitle())
                .contentUrl(lesson.getContentUrl())
                .totalPages(lesson.getDurationSeconds()) // dùng duration_seconds lưu totalPages cho PDF
                .courseId(lesson.getCourseId())
                .orderIndex(lesson.getOrderIndex())
                .build();
    }

    /**
     * Cập nhật tiến độ đọc PDF.
     * Lưu currentPage vào watch_time_seconds, tính % đọc.
     * Tự động đánh dấu hoàn thành khi đọc hết.
     */
    @Transactional
    public PdfProgressResponse updateReadingProgress(Long lessonId, String email, PdfProgressRequest request) {
        User user = findUser(email);
        Lesson lesson = findLesson(lessonId);

        validatePdfType(lesson);
        validateEnrollment(user.getId(), lesson.getCourseId());

        if (request.getCurrentPage() > request.getTotalPages()) {
            throw new ApiException("currentPage không được lớn hơn totalPages", HttpStatus.BAD_REQUEST);
        }

        // Cập nhật totalPages vào lesson nếu chưa có
        if (lesson.getDurationSeconds() == null || lesson.getDurationSeconds() == 0) {
            lesson.setDurationSeconds(request.getTotalPages());
            lessonRepository.save(lesson);
        }

        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder()
                        .userId(user.getId())
                        .lessonId(lessonId)
                        .isCompleted(false)
                        .watchTimeSeconds(0)
                        .build());

        progress.setWatchTimeSeconds(request.getCurrentPage()); // currentPage → watch_time_seconds
        progress.setLastAccessedAt(LocalDateTime.now());

        boolean isCompleted = request.getCurrentPage().equals(request.getTotalPages());
        if (isCompleted && !progress.getIsCompleted()) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            log.info("Student {} hoàn thành bài PDF lessonId={}", email, lessonId);
        }

        lessonProgressRepository.save(progress);

        double percentage = (request.getCurrentPage().doubleValue() / request.getTotalPages().doubleValue()) * 100;

        return PdfProgressResponse.builder()
                .lessonId(lessonId)
                .currentPage(request.getCurrentPage())
                .totalPages(request.getTotalPages())
                .readingPercentage(Math.round(percentage * 100.0) / 100.0)
                .isCompleted(progress.getIsCompleted())
                .lastAccessedAt(progress.getLastAccessedAt())
                .build();
    }

    /**
     * Khôi phục vị trí đọc cuối cùng khi user quay lại.
     */
    @Transactional(readOnly = true)
    public PdfProgressResponse getReadingProgress(Long lessonId, String email) {
        User user = findUser(email);
        Lesson lesson = findLesson(lessonId);

        validatePdfType(lesson);
        validateEnrollment(user.getId(), lesson.getCourseId());

        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(user.getId(), lessonId)
                .orElse(null);

        int currentPage = (progress != null) ? progress.getWatchTimeSeconds() : 0;
        int totalPages = (lesson.getDurationSeconds() != null) ? lesson.getDurationSeconds() : 0;
        double percentage = (totalPages > 0) ? (currentPage * 100.0 / totalPages) : 0;

        return PdfProgressResponse.builder()
                .lessonId(lessonId)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .readingPercentage(Math.round(percentage * 100.0) / 100.0)
                .isCompleted(progress != null && progress.getIsCompleted())
                .lastAccessedAt(progress != null ? progress.getLastAccessedAt() : null)
                .build();
    }

    // ==================== Private Helpers ====================

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));
    }

    private Lesson findLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException("Bài học không tồn tại", HttpStatus.NOT_FOUND));
    }

    private void validatePdfType(Lesson lesson) {
        if (lesson.getType() != Lesson.LessonType.PDF) {
            throw new ApiException("Bài học này không phải dạng PDF", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateEnrollment(Long userId, Long courseId) {
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ApiException("Bạn chưa đăng ký khóa học này", HttpStatus.FORBIDDEN);
        }
    }
}

package com.thinkai.backend.service;

import com.thinkai.backend.dto.UpdateProgressRequest;
import com.thinkai.backend.dto.UpdateProgressResponse;
import com.thinkai.backend.dto.VideoPreferenceDto;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.LessonProgress;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.entity.VideoPreference;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.LessonProgressRepository;
import com.thinkai.backend.repository.LessonRepository;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.repository.VideoPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoProgressService {

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final VideoPreferenceRepository videoPreferenceRepository;
    private final UserRepository userRepository;

    private static final double AUTO_COMPLETE_THRESHOLD = 0.9;

    /**
     * Lưu tiến độ xem giữa chừng — frontend gọi định kỳ mỗi 10s.
     * Auto-complete khi xem ≥90% video. Tính lại tiến độ khóa học.
     */
    @Transactional
    public UpdateProgressResponse updateProgress(Long lessonId, String email, UpdateProgressRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException("Bài học không tồn tại", HttpStatus.NOT_FOUND));

        // Check enrollment
        var enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), lesson.getCourseId())
                .orElseThrow(() -> new ApiException("Bạn chưa đăng ký khóa học này", HttpStatus.FORBIDDEN));

        // Upsert progress
        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(user.getId(), lessonId)
                .orElse(LessonProgress.builder()
                        .userId(user.getId())
                        .lessonId(lessonId)
                        .isCompleted(false)
                        .watchTimeSeconds(0)
                        .currentTimeSeconds(0)
                        .build());

        progress.setWatchTimeSeconds(request.getWatchTimeSeconds());
        progress.setLastAccessedAt(LocalDateTime.now());

        // Lưu vị trí video hiện tại (cho resume playback)
        if (request.getCurrentTimeSeconds() != null) {
            progress.setCurrentTimeSeconds(request.getCurrentTimeSeconds());
        }

        // Tính % tiến độ bài học
        double lessonPercent = 0.0;
        if (lesson.getDurationSeconds() != null && lesson.getDurationSeconds() > 0) {
            lessonPercent = Math.min(100.0,
                    (double) request.getWatchTimeSeconds() / lesson.getDurationSeconds() * 100);

            // Auto-complete khi xem ≥90%
            if (!Boolean.TRUE.equals(progress.getIsCompleted())
                    && lessonPercent >= AUTO_COMPLETE_THRESHOLD * 100) {
                progress.setIsCompleted(true);
                progress.setCompletedAt(LocalDateTime.now());
                log.info("Auto-completed: lesson={}, user={}, watchPercent={}%",
                        lessonId, email, Math.round(lessonPercent));
            }
        }

        lessonProgressRepository.save(progress);

        // Tính lại tiến độ khóa học = (bài đã complete / tổng bài) * 100
        List<Lesson> allLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(lesson.getCourseId());
        long completedCount = lessonProgressRepository.countCompletedByUserAndCourse(
                user.getId(), lesson.getCourseId());
        double coursePercent = allLessons.isEmpty() ? 0.0
                : (double) completedCount / allLessons.size() * 100;

        // Cập nhật Enrollment.progressPercent
        enrollment.setProgressPercent((int) Math.round(coursePercent));
        if (coursePercent >= 100.0 && enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }
        enrollmentRepository.save(enrollment);

        log.debug("Progress saved: lesson={}, user={}, watchTime={}s, lessonPercent={}%, coursePercent={}%",
                lessonId, email, request.getWatchTimeSeconds(), Math.round(lessonPercent), Math.round(coursePercent));

        return UpdateProgressResponse.builder()
                .lessonId(lessonId)
                .watchTimeSeconds(progress.getWatchTimeSeconds())
                .currentTimeSeconds(progress.getCurrentTimeSeconds())
                .isCompleted(progress.getIsCompleted())
                .lessonProgressPercent(Math.round(lessonPercent * 10.0) / 10.0)
                .courseProgressPercent(Math.round(coursePercent * 10.0) / 10.0)
                .build();
    }

    /**
     * Lấy cài đặt player của user (tốc độ, auto-play, chất lượng).
     */
    @Transactional(readOnly = true)
    public VideoPreferenceDto getPreferences(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        VideoPreference pref = videoPreferenceRepository.findByUserId(user.getId())
                .orElse(VideoPreference.builder()
                        .playbackSpeed(1.0)
                        .autoPlay(true)
                        .quality("auto")
                        .build());

        return VideoPreferenceDto.builder()
                .playbackSpeed(pref.getPlaybackSpeed())
                .autoPlay(pref.getAutoPlay())
                .quality(pref.getQuality())
                .build();
    }

    /**
     * Cập nhật cài đặt player — merge từng field nếu có.
     */
    @Transactional
    public VideoPreferenceDto savePreferences(String email, VideoPreferenceDto dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        VideoPreference pref = videoPreferenceRepository.findByUserId(user.getId())
                .orElse(VideoPreference.builder()
                        .userId(user.getId())
                        .playbackSpeed(1.0)
                        .autoPlay(true)
                        .quality("auto")
                        .build());

        if (dto.getPlaybackSpeed() != null) {
            pref.setPlaybackSpeed(dto.getPlaybackSpeed());
        }
        if (dto.getAutoPlay() != null) {
            pref.setAutoPlay(dto.getAutoPlay());
        }
        if (dto.getQuality() != null) {
            pref.setQuality(dto.getQuality());
        }

        videoPreferenceRepository.save(pref);

        log.info("Preferences saved for user {}: speed={}, autoPlay={}, quality={}",
                email, pref.getPlaybackSpeed(), pref.getAutoPlay(), pref.getQuality());

        return VideoPreferenceDto.builder()
                .playbackSpeed(pref.getPlaybackSpeed())
                .autoPlay(pref.getAutoPlay())
                .quality(pref.getQuality())
                .build();
    }
}

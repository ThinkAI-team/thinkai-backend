package com.thinkai.backend.service;

import com.thinkai.backend.dto.UpdateProgressRequest;
import com.thinkai.backend.dto.UpdateProgressResponse;
import com.thinkai.backend.dto.VideoPreferenceDto;
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
public class VideoProgressService {

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final VideoPreferenceRepository videoPreferenceRepository;
    private final UserRepository userRepository;

    /**
     * Lưu tiến độ xem giữa chừng — frontend gọi định kỳ mỗi 10-15s.
     * Không đánh dấu hoàn thành, chỉ cập nhật watchTimeSeconds + lastAccessedAt.
     */
    @Transactional
    public UpdateProgressResponse updateProgress(Long lessonId, String email, UpdateProgressRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException("Bài học không tồn tại", HttpStatus.NOT_FOUND));

        // Check enrollment
        enrollmentRepository.findByUserIdAndCourseId(user.getId(), lesson.getCourseId())
                .orElseThrow(() -> new ApiException("Bạn chưa đăng ký khóa học này", HttpStatus.FORBIDDEN));

        // Upsert progress
        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(user.getId(), lessonId)
                .orElse(LessonProgress.builder()
                        .userId(user.getId())
                        .lessonId(lessonId)
                        .isCompleted(false)
                        .watchTimeSeconds(0)
                        .build());

        progress.setWatchTimeSeconds(request.getWatchTimeSeconds());
        progress.setLastAccessedAt(LocalDateTime.now());
        lessonProgressRepository.save(progress);

        log.debug("Progress saved: lesson={}, user={}, watchTime={}s",
                lessonId, email, request.getWatchTimeSeconds());

        return UpdateProgressResponse.builder()
                .lessonId(lessonId)
                .watchTimeSeconds(progress.getWatchTimeSeconds())
                .isCompleted(progress.getIsCompleted())
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

        if (dto.getPlaybackSpeed() != null) pref.setPlaybackSpeed(dto.getPlaybackSpeed());
        if (dto.getAutoPlay() != null) pref.setAutoPlay(dto.getAutoPlay());
        if (dto.getQuality() != null) pref.setQuality(dto.getQuality());

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

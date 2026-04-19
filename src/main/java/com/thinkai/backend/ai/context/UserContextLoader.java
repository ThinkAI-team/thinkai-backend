package com.thinkai.backend.ai.context;

import com.thinkai.backend.ai.orchestrator.OrchestratorContextBuilder.UserProfileContext;
import com.thinkai.backend.entity.AiSettings;
import com.thinkai.backend.entity.UserMemory;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.repository.AiSettingsRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.LessonProgressRepository;
import com.thinkai.backend.repository.UserMemoryRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class UserContextLoader {

    private final UserRepository userRepository;
    private final UserMemoryRepository userMemoryRepository;
    private final AiSettingsRepository aiSettingsRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    public UserContextLoader(
            UserRepository userRepository,
            UserMemoryRepository userMemoryRepository,
            AiSettingsRepository aiSettingsRepository,
            EnrollmentRepository enrollmentRepository,
            LessonProgressRepository lessonProgressRepository) {
        this.userRepository = userRepository;
        this.userMemoryRepository = userMemoryRepository;
        this.aiSettingsRepository = aiSettingsRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
    }

    public UserProfileContext load(Long userId) {
        if (userId == null) {
            return defaultProfile();
        }

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return defaultProfile();
            }

            Optional<UserMemory> userMemoryOpt = userMemoryRepository.findByUserId(userId);

            String level;
            String targetExam;
            Integer targetScore;
            List<String> weakPoints;
            List<String> strongPoints;

            if (userMemoryOpt.isPresent()) {
                UserMemory um = userMemoryOpt.get();
                level = um.getUserLevel();
                targetExam = um.getTargetExam();
                targetScore = um.getTargetScore();
                weakPoints = parsePoints(um.getWeakPoints());
                strongPoints = parsePoints(um.getStrongPoints());
            } else {
                level = getUserLevel(userId);
                targetExam = getTargetExam(userId);
                targetScore = getTargetScore(userId);
                weakPoints = getWeakPoints(userId);
                strongPoints = getStrongPoints(userId);
            }

            int completedLessons = (int) lessonProgressRepository.findByUserIdAndIsCompletedTrue(userId).size();
            int streak = calculateStreak(userId);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userName", user.getFullName());
            metadata.put("enrolledCourses", enrollmentRepository.countByUserId(userId));
            if (userMemoryOpt.isPresent()) {
                metadata.put("adaptiveRules", userMemoryOpt.get().getAdaptiveRules());
            }

            return new UserProfileContext(
                level != null ? level : "B1",
                targetExam,
                targetScore,
                weakPoints != null ? weakPoints : List.of(),
                strongPoints != null ? strongPoints : List.of(),
                streak,
                completedLessons,
                metadata
            );

        } catch (Exception e) {
            log.error("Error loading user profile: {}", e.getMessage());
            return defaultProfile();
        }
    }

    private List<String> parsePoints(String points) {
        if (points == null || points.isBlank()) {
            return List.of();
        }
        return Arrays.asList(points.split(","));
    }

    private String getUserLevel(Long userId) {
        return aiSettingsRepository.findByUserIdAndSettingKey(userId, "user_level")
            .map(AiSettings::getSettingValue)
            .orElse("B1");
    }

    private String getTargetExam(Long userId) {
        return aiSettingsRepository.findByUserIdAndSettingKey(userId, "target_exam")
            .map(AiSettings::getSettingValue)
            .orElse(null);
    }

    private Integer getTargetScore(Long userId) {
        return aiSettingsRepository.findByUserIdAndSettingKey(userId, "target_score")
            .map(s -> Integer.parseInt(s.getSettingValue()))
            .orElse(null);
    }

    private List<String> getWeakPoints(Long userId) {
        return aiSettingsRepository.findByUserIdAndSettingKey(userId, "weak_points")
            .map(s -> Arrays.asList(s.getSettingValue().split(",")))
            .orElse(List.of());
    }

    private List<String> getStrongPoints(Long userId) {
        return aiSettingsRepository.findByUserIdAndSettingKey(userId, "strong_points")
            .map(s -> Arrays.asList(s.getSettingValue().split(",")))
            .orElse(List.of());
    }

    private int calculateStreak(Long userId) {
        return aiSettingsRepository.findByUserIdAndSettingKey(userId, "daily_streak")
            .map(s -> {
                try {
                    return Integer.parseInt(s.getSettingValue());
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .orElse(0);
    }

    private UserProfileContext defaultProfile() {
        return new UserProfileContext(
            "B1",
            null,
            null,
            List.of(),
            List.of(),
            0,
            0,
            Map.of()
        );
    }
}

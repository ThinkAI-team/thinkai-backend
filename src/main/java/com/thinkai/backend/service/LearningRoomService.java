package com.thinkai.backend.service;

import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.dto.LessonDetailResponse;
import com.thinkai.backend.dto.LessonResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.LessonProgress;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.LessonProgressRepository;
import com.thinkai.backend.repository.LessonRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LearningRoomService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseWithLessons(String email, Long courseId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Course not found", HttpStatus.NOT_FOUND));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new ApiException("You are not enrolled in this course", HttpStatus.FORBIDDEN));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<LessonProgress> progresses = lessonProgressRepository.findByUserIdAndIsCompletedTrue(user.getId());

        List<LessonResponse> lessonResponses = new ArrayList<>();
        for (Lesson lesson : lessons) {
            boolean isCompleted = progresses.stream()
                    .anyMatch(p -> p.getLessonId().equals(lesson.getId()));

            lessonResponses.add(LessonResponse.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .type(lesson.getType() != null ? lesson.getType().name() : null)
                    .duration(lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() / 60 + " min" : null)
                    .isCompleted(isCompleted)
                    .orderIndex(lesson.getOrderIndex())
                    .build());
        }

        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .price(course.getPrice())
                .isEnrolled(true)
                // Requires instructor mapping, skip if not mapped in entity directly
                // .instructorName(course.getInstructor() != null ? course.getInstructor().getFullName() : null)
                .progressPercent(enrollment.getProgressPercent())
                .lessons(lessonResponses)
                .build();
    }

    @Transactional
    public LessonDetailResponse getLessonDetail(String email, Long lessonId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException("Lesson not found", HttpStatus.NOT_FOUND));

        Course course = courseRepository.findById(lesson.getCourseId())
                .orElseThrow(() -> new ApiException("Course not found", HttpStatus.NOT_FOUND));

        // Verify enrollment
        enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId())
                .orElseThrow(() -> new ApiException("You are not enrolled in this course", HttpStatus.FORBIDDEN));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        
        Long previousLessonId = null;
        Long nextLessonId = null;

        for (int i = 0; i < lessons.size(); i++) {
            if (Objects.equals(lessons.get(i).getId(), lessonId)) {
                if (i > 0) {
                    previousLessonId = lessons.get(i - 1).getId();
                }
                if (i < lessons.size() - 1) {
                    nextLessonId = lessons.get(i + 1).getId();
                }
                break;
            }
        }

        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseGet(() -> {
                    LessonProgress newProgress = LessonProgress.builder()
                            .userId(user.getId())
                            .lessonId(lessonId)
                            .isCompleted(false)
                            .watchTimeSeconds(0)
                            .build();
                    return lessonProgressRepository.save(newProgress);
                });

        // Update last accessed time
        progress.setLastAccessedAt(LocalDateTime.now());
        lessonProgressRepository.save(progress);

        // Tính % tiến độ bài học
        double lessonPercent = 0.0;
        if (lesson.getDurationSeconds() != null && lesson.getDurationSeconds() > 0) {
            lessonPercent = Math.min(100.0,
                    (double) progress.getWatchTimeSeconds() / lesson.getDurationSeconds() * 100);
        }

        return LessonDetailResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .type(lesson.getType() != null ? lesson.getType().name() : null)
                .contentUrl(lesson.getContentUrl())
                .contentText(lesson.getContentText())
                .durationSeconds(lesson.getDurationSeconds())
                .orderIndex(lesson.getOrderIndex())
                .isCompleted(progress.getIsCompleted())
                .watchTimeSeconds(progress.getWatchTimeSeconds())
                .currentTimeSeconds(progress.getCurrentTimeSeconds())
                .lessonProgressPercent(Math.round(lessonPercent * 10.0) / 10.0)
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .previousLessonId(previousLessonId)
                .nextLessonId(nextLessonId)
                .build();
    }
}

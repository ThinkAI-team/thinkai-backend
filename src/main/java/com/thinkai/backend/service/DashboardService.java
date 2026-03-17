package com.thinkai.backend.service;

import com.thinkai.backend.dto.DashboardResponse;
import com.thinkai.backend.dto.EnrolledCourseDto;
import com.thinkai.backend.dto.NextLessonDto;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dashboard Service for student statistics and overview.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    /**
     * Get dashboard data for the authenticated student.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String email) {
        // 1. Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        // 2. Build greeting
        String greeting = buildGreeting(user.getFullName());

        // 3. Get all enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(user.getId());

        if (enrollments.isEmpty()) {
            return DashboardResponse.builder()
                    .greeting(greeting)
                    .totalEnrolledCourses(0)
                    .averageProgress(0)
                    .enrolledCourses(List.of())
                    .nextLesson(null)
                    .build();
        }

        // 4. Get completed lesson IDs for this user (batch query)
        Set<Long> completedLessonIds = lessonProgressRepository
                .findByUserIdAndIsCompletedTrue(user.getId())
                .stream()
                .map(LessonProgress::getLessonId)
                .collect(Collectors.toSet());

        // 5. Build enrolled course list + find next lesson
        List<EnrolledCourseDto> courseDtos = new ArrayList<>();
        NextLessonDto nextLesson = null;
        LocalDateTime latestAccess = null;

        for (Enrollment enrollment : enrollments) {
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course == null) {
                continue;
            }

            List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
            long totalLessons = lessons.size();
            long completedLessons = lessons.stream()
                    .filter(l -> completedLessonIds.contains(l.getId()))
                    .count();

            // Find last accessed time for this course
            LocalDateTime courseLastAccessed = lessons.stream()
                    .map(l -> lessonProgressRepository.findByUserIdAndLessonId(user.getId(), l.getId())
                            .map(LessonProgress::getLastAccessedAt)
                            .orElse(null))
                    .filter(t -> t != null)
                    .max(LocalDateTime::compareTo)
                    .orElse(enrollment.getEnrolledAt());

            courseDtos.add(EnrolledCourseDto.builder()
                    .courseId(course.getId())
                    .title(course.getTitle())
                    .thumbnailUrl(course.getThumbnailUrl())
                    .progressPercent(enrollment.getProgressPercent())
                    .totalLessons(totalLessons)
                    .completedLessons(completedLessons)
                    .lastAccessedAt(courseLastAccessed)
                    .build());

            // Find next incomplete lesson (first by orderIndex)
            if (nextLesson == null || (latestAccess != null && courseLastAccessed != null
                    && courseLastAccessed.isAfter(latestAccess))) {
                for (Lesson lesson : lessons) {
                    if (!completedLessonIds.contains(lesson.getId())) {
                        nextLesson = NextLessonDto.builder()
                                .lessonId(lesson.getId())
                                .lessonTitle(lesson.getTitle())
                                .courseTitle(course.getTitle())
                                .type(lesson.getType().name())
                                .build();
                        latestAccess = courseLastAccessed;
                        break;
                    }
                }
            }
        }

        // 6. Calculate average progress
        double averageProgress = enrollments.stream()
                .mapToInt(Enrollment::getProgressPercent)
                .average()
                .orElse(0);

        // 7. Sort courses by last accessed (most recent first)
        courseDtos.sort((a, b) -> {
            if (a.getLastAccessedAt() == null && b.getLastAccessedAt() == null) {
                return 0;
            }
            if (a.getLastAccessedAt() == null) {
                return 1;
            }
            if (b.getLastAccessedAt() == null) {
                return -1;
            }
            return b.getLastAccessedAt().compareTo(a.getLastAccessedAt());
        });

        return DashboardResponse.builder()
                .greeting(greeting)
                .totalEnrolledCourses(enrollments.size())
                .averageProgress(Math.round(averageProgress * 10.0) / 10.0)
                .enrolledCourses(courseDtos)
                .nextLesson(nextLesson)
                .build();
    }

    private String buildGreeting(String fullName) {
        LocalTime now = LocalTime.now();
        String timeGreeting;
        if (now.isBefore(LocalTime.of(12, 0))) {
            timeGreeting = "Chào buổi sáng";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            timeGreeting = "Chào buổi chiều";
        } else {
            timeGreeting = "Chào buổi tối";
        }
        return timeGreeting + ", " + fullName + "! 👋";
    }
}

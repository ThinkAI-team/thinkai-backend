package com.thinkai.backend.service;

import com.thinkai.backend.dto.MyCourseResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.LessonRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách khóa học đã đăng ký của user
     * API: GET /users/me/courses
     */
    public List<MyCourseResponse> getMyCourses(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy người dùng", HttpStatus.NOT_FOUND
                ));

        List<Enrollment> enrollments = enrollmentRepository
                .findByUserIdOrderByEnrolledAtDesc(user.getId());

        return enrollments.stream()
                .map(enrollment -> {
                    Course course = courseRepository.findById(enrollment.getCourseId())
                            .orElse(null);
                    if (course == null) return null;

                    // Tìm bài học tiếp theo (bài đầu tiên chưa hoàn thành)
                    MyCourseResponse.NextLessonInfo nextLesson = resolveNextLesson(
                            enrollment.getCourseId(), enrollment.getProgressPercent()
                    );

                    return MyCourseResponse.builder()
                            .id(course.getId())
                            .title(course.getTitle())
                            .thumbnail(course.getThumbnailUrl())
                            .price(course.getPrice())
                            .progressPercent(enrollment.getProgressPercent())
                            .enrolledAt(enrollment.getEnrolledAt())
                            .nextLesson(nextLesson)
                            .build();
                })
                .filter(r -> r != null)
                .toList();
    }

    /**
     * Tìm bài học tiếp theo dựa trên progress
     */
    private MyCourseResponse.NextLessonInfo resolveNextLesson(Long courseId, Integer progressPercent) {
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        if (lessons.isEmpty()) return null;

        if (progressPercent == null || progressPercent == 0) {
            Lesson first = lessons.get(0);
            return MyCourseResponse.NextLessonInfo.builder()
                    .id(first.getId())
                    .title(first.getTitle())
                    .build();
        }

        // Tính bài tiếp theo dựa trên % progress
        int completedCount = (int) Math.floor(progressPercent * lessons.size() / 100.0);
        if (completedCount >= lessons.size()) return null; // Đã hoàn thành hết

        Lesson next = lessons.get(completedCount);
        return MyCourseResponse.NextLessonInfo.builder()
                .id(next.getId())
                .title(next.getTitle())
                .build();
    }
}

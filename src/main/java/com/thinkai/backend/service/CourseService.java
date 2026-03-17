package com.thinkai.backend.service;

import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.dto.LessonResponse;
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
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /**
     * Lấy chi tiết khóa học + danh sách bài học + trạng thái enrollment
     * API: GET /courses/{courseId}
     */
    public CourseDetailResponse getCourseDetail(Long courseId, Long currentUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId,
                        HttpStatus.NOT_FOUND
                ));

        // Lấy danh sách bài học, sắp xếp theo thứ tự
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        // Lấy thông tin giảng viên
        CourseDetailResponse.InstructorInfo instructorInfo = null;
        if (course.getInstructorId() != null) {
            User instructor = userRepository.findById(course.getInstructorId()).orElse(null);
            if (instructor != null) {
                instructorInfo = CourseDetailResponse.InstructorInfo.builder()
                        .id(instructor.getId())
                        .fullName(instructor.getFullName())
                        .avatarUrl(instructor.getAvatarUrl())
                        .build();
            }
        }

        // Kiểm tra trạng thái enrollment (nếu user đã đăng nhập)
        Boolean isEnrolled = false;
        Integer progressPercent = 0;
        if (currentUserId != null) {
            Enrollment enrollment = enrollmentRepository
                    .findByUserIdAndCourseId(currentUserId, courseId)
                    .orElse(null);
            if (enrollment != null) {
                isEnrolled = true;
                progressPercent = enrollment.getProgressPercent();
            }
        }

        // Map lessons sang DTO
        List<LessonResponse> lessonResponses = lessons.stream()
                .map(lesson -> LessonResponse.builder()
                        .id(lesson.getId())
                        .title(lesson.getTitle())
                        .type(lesson.getType().name())
                        .duration(formatDuration(lesson.getDurationSeconds()))
                        .isCompleted(false) // TODO: tra LessonProgress khi module Learning Room sẵn sàng
                        .orderIndex(lesson.getOrderIndex())
                        .build())
                .toList();

        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnail(course.getThumbnailUrl())
                .price(course.getPrice())
                .instructor(instructorInfo)
                .isEnrolled(isEnrolled)
                .progressPercent(progressPercent)
                .lessons(lessonResponses)
                .build();
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds == 0) {
            return null;
        }
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }
}

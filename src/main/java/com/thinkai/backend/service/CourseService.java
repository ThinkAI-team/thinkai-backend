package com.thinkai.backend.service;

import com.thinkai.backend.dto.EnrollmentResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /**
     * Đăng ký khóa học
     * API: POST /courses/{courseId}/enroll
     * Auth: Bearer Token (STUDENT)
     */
    @Transactional
    public EnrollmentResponse enrollCourse(Long courseId, String userEmail) {
        // 1. Tìm user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy người dùng", HttpStatus.NOT_FOUND
                ));

        // 2. Tìm khóa học
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId, HttpStatus.NOT_FOUND
                ));

        // 3. Kiểm tra khóa học đã published chưa
        if (!course.getIsPublished() || course.getStatus() != Course.Status.APPROVED) {
            throw new ApiException(
                    "Khóa học chưa được công bố", HttpStatus.BAD_REQUEST
            );
        }

        // 4. Kiểm tra đã đăng ký trước đó chưa
        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new ApiException(
                    "Bạn đã đăng ký khóa học này rồi", HttpStatus.CONFLICT
            );
        }

        // 5. Tạo enrollment mới
        Enrollment enrollment = Enrollment.builder()
                .userId(user.getId())
                .courseId(courseId)
                .progressPercent(0)
                .enrolledAt(LocalDateTime.now())
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        // 6. Trả response
        return EnrollmentResponse.builder()
                .enrollmentId(enrollment.getId())
                .courseId(courseId)
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}

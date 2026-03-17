package com.thinkai.backend.service;

import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.dto.CourseListResponse;
import com.thinkai.backend.dto.EnrollmentResponse;
import com.thinkai.backend.dto.LessonResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    /**
     * GET /courses — Danh sách khóa học published + filter/search/paginate
     */
    public Map<String, Object> getPublishedCourses(
            String keyword,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String sortBy,
            String sortDir,
            int page,
            int size
    ) {
        // Giới hạn size tối đa 50
        size = Math.min(size, 50);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Course> coursePage = courseRepository.findPublishedCourses(
                keyword, priceMin, priceMax, pageable
        );

        List<CourseListResponse> content = coursePage.getContent().stream()
                .map(this::toCourseListResponse)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("page", coursePage.getNumber());
        response.put("size", coursePage.getSize());
        response.put("totalElements", coursePage.getTotalElements());
        response.put("totalPages", coursePage.getTotalPages());

        return response;
    }

    /**
     * GET /courses/{id} — Chi tiết khóa học + lessons + enrollment status
     */
    public CourseDetailResponse getCourseDetail(Long courseId, Long currentUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId,
                        HttpStatus.NOT_FOUND
                ));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        // Lấy thông tin instructor
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

        // Check enrollment status (nếu user đã đăng nhập)
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

        List<LessonResponse> lessonResponses = lessons.stream()
                .map(lesson -> LessonResponse.builder()
                        .id(lesson.getId())
                        .title(lesson.getTitle())
                        .type(lesson.getType().name())
                        .duration(formatDuration(lesson.getDurationSeconds()))
                        .isCompleted(false) // TODO: check lesson_progress
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

    /**
     * POST /courses/{id}/enroll — Đăng ký khóa học
     */
    @Transactional
    public EnrollmentResponse enrollCourse(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy người dùng", HttpStatus.NOT_FOUND
                ));

        // Kiểm tra khóa học tồn tại
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId,
                        HttpStatus.NOT_FOUND
                ));

        // Kiểm tra khóa học đã published chưa
        if (!course.getIsPublished()) {
            throw new ApiException("Khóa học chưa được xuất bản", HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra đã đăng ký chưa
        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new ApiException("Bạn đã đăng ký khóa học này rồi", HttpStatus.BAD_REQUEST);
        }

        Enrollment enrollment = Enrollment.builder()
                .userId(user.getId())
                .courseId(courseId)
                .progressPercent(0)
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        return EnrollmentResponse.builder()
                .enrollmentId(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }

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
                    if (course == null) {
                        return null;
                    }

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
        if (lessons.isEmpty()) {
            return null;
        }

        if (progressPercent == null || progressPercent == 0) {
            Lesson first = lessons.get(0);
            return MyCourseResponse.NextLessonInfo.builder()
                    .id(first.getId())
                    .title(first.getTitle())
                    .build();
        }

        // Tính bài tiếp theo dựa trên % progress
        int completedCount = (int) Math.floor(progressPercent * lessons.size() / 100.0);
        if (completedCount >= lessons.size()) {
            return null; // Đã hoàn thành hết
        }

        Lesson next = lessons.get(completedCount);
        return MyCourseResponse.NextLessonInfo.builder()
                .id(next.getId())
                .title(next.getTitle())
                .build();
    }

    // ===================== PRIVATE HELPERS =====================

    private CourseListResponse toCourseListResponse(Course course) {
        // Lấy instructor info
        CourseListResponse.InstructorInfo instructorInfo = null;
        if (course.getInstructorId() != null) {
            User instructor = userRepository.findById(course.getInstructorId()).orElse(null);
            if (instructor != null) {
                instructorInfo = CourseListResponse.InstructorInfo.builder()
                        .id(instructor.getId())
                        .fullName(instructor.getFullName())
                        .build();
            }
        }

        return CourseListResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .thumbnail(course.getThumbnailUrl())
                .price(course.getPrice())
                .instructor(instructorInfo)
                .lessonsCount(lessonRepository.countByCourseId(course.getId()))
                .enrolledCount(enrollmentRepository.countByCourseId(course.getId()))
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


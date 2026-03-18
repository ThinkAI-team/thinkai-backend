package com.thinkai.backend.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.dto.CourseListResponse;
import com.thinkai.backend.dto.CourseRequest;
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
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /**
     * GET /courses — List published courses with filters, search and pagination.
     */
    public Map<String, Object> getPublishedCourses(
            String keyword,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String sortBy,
            String sortDir,
            int page,
            int size) {
        // Giới hạn size tối đa 50
        size = Math.min(size, 50);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Course> coursePage = courseRepository.findPublishedCourses(
                keyword, priceMin, priceMax, pageable);

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
     * GET /courses/{id} — Course details with lessons and enrollment status.
     */
    public CourseDetailResponse getCourseDetail(Long courseId, Long currentUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId,
                        HttpStatus.NOT_FOUND));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        // Get instructor info
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

        // Check enrollment status
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

    // ===================== TEACHER OPERATIONS =====================

    public Course createCourse(Long teacherId, CourseRequest request) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .price(request.getPrice())
                .instructorId(teacherId)
                .isPublished(false)
                .status(Course.Status.DRAFT)
                .build();
        return courseRepository.save(course);
    }

    public Page<Course> getCoursesByTeacher(Long teacherId, Pageable pageable) {
        return courseRepository.findByInstructorId(teacherId, pageable);
    }

    public Course getCourseByIdAndTeacher(Long courseId, Long teacherId) {
        return courseRepository.findByIdAndInstructorId(courseId, teacherId)
                .orElseThrow(() -> new ApiException("Không tìm thấy khóa học với ID: " + courseId, HttpStatus.NOT_FOUND));
    }

    public Course updateCourse(Long courseId, Long teacherId, CourseRequest request) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setPrice(request.getPrice());
        return courseRepository.save(course);
    }

    public void deleteCourse(Long courseId, Long teacherId) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);
        courseRepository.delete(course);
    }

    public Course publishCourse(Long courseId, Long teacherId) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);
        course.setIsPublished(true);
        course.setStatus(Course.Status.APPROVED);
        return courseRepository.save(course);
    }

    /**
     * POST /courses/{id}/enroll — Enroll in a course.
     */
    @Transactional
    public EnrollmentResponse enrollCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId,
                        HttpStatus.NOT_FOUND));

        if (!course.getIsPublished()) {
            throw new ApiException("Khóa học chưa được xuất bản", HttpStatus.BAD_REQUEST);
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ApiException("Bạn đã đăng ký khóa học này rồi", HttpStatus.BAD_REQUEST);
        }

        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
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
     * GET /users/me/courses — User's enrolled courses.
     */
    public List<MyCourseResponse> getMyCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);

        return enrollments.stream().map(enrollment -> {
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course == null) {
                return null;
            }

            List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
            String nextLessonTitle = null;
            Long nextLessonId = null;
            if (!lessons.isEmpty()) {
                // Tạm lấy bài học đầu tiên hoặc tính theo progress
                int completedCount = (int) Math.round(
                        lessons.size() * enrollment.getProgressPercent() / 100.0);
                int nextIndex = Math.min(completedCount, lessons.size() - 1);
                Lesson nextLesson = lessons.get(nextIndex);
                nextLessonTitle = nextLesson.getTitle();
                nextLessonId = nextLesson.getId();
            }

            return MyCourseResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .thumbnail(course.getThumbnailUrl())
                    .price(course.getPrice())
                    .progressPercent(enrollment.getProgressPercent())
                    .nextLessonTitle(nextLessonTitle)
                    .nextLessonId(nextLessonId)
                    .build();
        }).filter(r -> r != null).toList();
    }

    // ===================== PRIVATE HELPER =====================

    private CourseListResponse toCourseListResponse(Course course) {
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
                .lessonsCount((int) lessonRepository.countByCourseId(course.getId()))
                .enrolledCount((int) enrollmentRepository.countByCourseId(course.getId()))
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

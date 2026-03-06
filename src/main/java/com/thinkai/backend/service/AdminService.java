package com.thinkai.backend.service;

import com.thinkai.backend.dto.admin.AdminCourseRequest;
import com.thinkai.backend.dto.admin.AdminCourseResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final CourseRepository courseRepository;

    // ─── POST /admin/courses ──────────────────────────────────────────────────
    @Transactional
    public AdminCourseResponse createCourse(AdminCourseRequest request) {
        Course.Status status = parseStatus(request.getStatus(), Course.Status.DRAFT);

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .price(request.getPrice())
                .instructorId(request.getInstructorId())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .status(status)
                .build();

        course = courseRepository.save(course);
        return toCourseResponse(course);
    }

    // ─── PUT /admin/courses/{courseId} ────────────────────────────────────────
    @Transactional
    public AdminCourseResponse updateCourse(Long courseId, AdminCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với id: " + courseId, HttpStatus.NOT_FOUND));

        Course.Status status = parseStatus(request.getStatus(), course.getStatus());

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setPrice(request.getPrice());
        course.setInstructorId(request.getInstructorId());
        if (request.getIsPublished() != null) {
            course.setIsPublished(request.getIsPublished());
        }
        course.setStatus(status);

        course = courseRepository.save(course);
        return toCourseResponse(course);
    }

    // ─── DELETE /admin/courses/{courseId} ─────────────────────────────────────
    @Transactional
    public void deleteCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ApiException(
                    "Không tìm thấy khóa học với id: " + courseId, HttpStatus.NOT_FOUND);
        }
        courseRepository.deleteById(courseId);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private Course.Status parseStatus(String statusStr, Course.Status defaultStatus) {
        if (statusStr == null || statusStr.isBlank())
            return defaultStatus;
        try {
            return Course.Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    "Status không hợp lệ. Giá trị cho phép: DRAFT, PENDING, APPROVED, REJECTED",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private AdminCourseResponse toCourseResponse(Course course) {
        return AdminCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .price(course.getPrice())
                .instructorId(course.getInstructorId())
                .isPublished(course.getIsPublished())
                .status(course.getStatus().name())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}

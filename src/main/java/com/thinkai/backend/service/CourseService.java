package com.thinkai.backend.service;

import com.thinkai.backend.dto.CourseRequest;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public Course createCourse(Long teacherId, CourseRequest request) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .price(request.getPrice())
                .instructorId(teacherId)
                .status(Course.Status.DRAFT)
                .isPublished(false)
                .build();
        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public Page<Course> getCoursesByTeacher(Long teacherId, Pageable pageable) {
        return courseRepository.findByInstructorId(teacherId, pageable);
    }

    @Transactional(readOnly = true)
    public Course getCourseByIdAndTeacher(Long courseId, Long teacherId) {
        return courseRepository.findByIdAndInstructorId(courseId, teacherId)
                .orElseThrow(() -> new ApiException("Course not found or access denied", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Course updateCourse(Long courseId, Long teacherId, CourseRequest request) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setPrice(request.getPrice());
        return courseRepository.save(course);
    }

    @Transactional
    public Course publishCourse(Long courseId, Long teacherId) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);
        if (course.getStatus() != Course.Status.DRAFT) {
            throw new ApiException("Only DRAFT courses can be published", HttpStatus.BAD_REQUEST);
        }
        course.setStatus(Course.Status.PENDING); // Pending Admin Approval
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long courseId, Long teacherId) {
        Course course = getCourseByIdAndTeacher(courseId, teacherId);
        courseRepository.delete(course);
    }
}

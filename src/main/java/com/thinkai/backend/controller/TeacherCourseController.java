package com.thinkai.backend.controller;

import com.thinkai.backend.dto.CourseRequest;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/courses")
@RequiredArgsConstructor
public class TeacherCourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    private Long getTeacherId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .map(User::getId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Course> createCourse(Authentication auth, @Valid @RequestBody CourseRequest request) {
        Course course = courseService.createCourse(getTeacherId(auth), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }

    @GetMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Page<Course>> getCourses(Authentication auth, Pageable pageable) {
        Page<Course> courses = courseService.getCoursesByTeacher(getTeacherId(auth), pageable);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Course> getCourseDetail(Authentication auth, @PathVariable Long id) {
        Course course = courseService.getCourseByIdAndTeacher(id, getTeacherId(auth));
        return ResponseEntity.ok(course);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Course> updateCourse(Authentication auth, @PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        Course course = courseService.updateCourse(id, getTeacherId(auth), request);
        return ResponseEntity.ok(course);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(Authentication auth, @PathVariable Long id) {
        courseService.deleteCourse(id, getTeacherId(auth));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Course> publishCourse(Authentication auth, @PathVariable Long id) {
        Course course = courseService.publishCourse(id, getTeacherId(auth));
        return ResponseEntity.ok(course);
    }
}

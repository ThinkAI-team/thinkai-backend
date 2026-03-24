package com.thinkai.backend.controller;

import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.security.TeacherOrAdmin;
import com.thinkai.backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @StudentOnly
    @PostMapping("/{courseId}")
    public ResponseEntity<com.thinkai.backend.dto.EnrollmentResponse> enrollInCourse(
            Authentication auth,
            @PathVariable Long courseId) {
        Long userId = requireCurrentUserId(auth);
        com.thinkai.backend.dto.EnrollmentResponse response = courseService.enrollCourse(courseId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @TeacherOrAdmin
    @GetMapping("/manage")
    public ResponseEntity<List<Enrollment>> manageEnrollments(Authentication auth) {
        User currentUser = getCurrentUser(auth);

        if (currentUser.getRole() == User.Role.ADMIN) {
            return ResponseEntity.ok(enrollmentRepository.findAll());
        }

        List<Long> teacherCourseIds = courseRepository.findByInstructorId(currentUser.getId()).stream()
                .map(com.thinkai.backend.entity.Course::getId)
                .toList();

        if (teacherCourseIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(enrollmentRepository.findByCourseIdIn(teacherCourseIds));
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException("Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private Long requireCurrentUserId(Authentication auth) {
        return getCurrentUser(auth).getId();
    }
}

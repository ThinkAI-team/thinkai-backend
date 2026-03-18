package com.thinkai.backend.controller;

import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.security.TeacherOrAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    @StudentOnly
    @PostMapping("/{courseId}")
    public ResponseEntity<Enrollment> enrollInCourse(@PathVariable Long courseId) {
        // Chỉ Student mới được đăng ký khóa học
        return ResponseEntity.ok(new Enrollment());
    }

    @TeacherOrAdmin
    @GetMapping("/manage")
    public ResponseEntity<List<Enrollment>> manageEnrollments() {
        // Chỉ Teacher hoặc Admin mới được quản lý đăng ký
        return ResponseEntity.ok(List.of());
    }
}

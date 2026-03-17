package com.thinkai.backend.controller;

import com.thinkai.backend.entity.Course;
import com.thinkai.backend.security.TeacherOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        // Mọi người đều có thể xem danh sách khóa học
        return ResponseEntity.ok(List.of());
    }

    @TeacherOnly
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        // Chỉ Teacher mới có quyền tạo khóa học
        return ResponseEntity.ok(course);
    }

    @TeacherOnly
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        // Chỉ Teacher mới có quyền sửa khóa học
        return ResponseEntity.ok(course);
    }
}

package com.thinkai.backend.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ TEACHER mới truy cập được.
 * Sử dụng: @TeacherOnly trên method hoặc class Controller.
 *
 * Ví dụ:
 *   @TeacherOnly
 *   @PostMapping("/teacher/courses")
 *   public ResponseEntity<Course> createCourse(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('TEACHER')")
public @interface TeacherOnly {
}

package com.thinkai.backend.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TEACHER hoặc ADMIN đều truy cập được.
 * Sử dụng: @TeacherOrAdmin trên method hoặc class Controller.
 *
 * Ví dụ:
 *   @TeacherOrAdmin
 *   @GetMapping("/courses/{id}/students")
 *   public ResponseEntity<List<User>> getStudents(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public @interface TeacherOrAdmin {
}

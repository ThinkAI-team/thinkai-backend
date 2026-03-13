package com.thinkai.backend.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ STUDENT mới truy cập được.
 * Sử dụng: @StudentOnly trên method hoặc class Controller.
 *
 * Ví dụ:
 *   @StudentOnly
 *   @PostMapping("/enrollments")
 *   public ResponseEntity<Enrollment> enroll(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('STUDENT')")
public @interface StudentOnly {
}

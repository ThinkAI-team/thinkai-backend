package com.thinkai.backend.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ ADMIN mới truy cập được.
 * Sử dụng: @AdminOnly trên method hoặc class Controller.
 *
 * Ví dụ:
 *   @AdminOnly
 *   @GetMapping("/admin/users")
 *   public ResponseEntity<List<User>> getAllUsers() { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
public @interface AdminOnly {
}

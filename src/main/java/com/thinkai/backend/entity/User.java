package com.thinkai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * ENTITY USER - Đại diện cho bảng "users" trong database
 * ============================================================================
 * 
 * 📚 KIẾN THỨC CƠ BẢN:
 * - @Entity: Đánh dấu class này là 1 Entity (tương ứng 1 bảng DB)
 * - @Table: Chỉ định tên bảng trong DB (nếu khác tên class)
 * - @Id: Đánh dấu Primary Key
 * - @Column: Map thuộc tính với cột trong DB
 * 
 * 📦 LOMBOK ANNOTATIONS:
 * - @Getter/@Setter: Tự động tạo getter/setter cho tất cả fields
 * - @NoArgsConstructor: Tạo constructor không tham số (JPA yêu cầu)
 * - @AllArgsConstructor: Tạo constructor đầy đủ tham số
 * - @Builder: Cho phép tạo object theo pattern Builder
 */

@Entity                                    // ← Đánh dấu là Entity
@Table(name = "users")                     // ← Tên bảng trong DB
@Getter @Setter                            // ← Lombok: tự tạo getter/setter
@NoArgsConstructor                         // ← Lombok: constructor rỗng
@AllArgsConstructor                        // ← Lombok: constructor đầy đủ
@Builder                                   // ← Lombok: Builder pattern
public class User {

    // ========================================================================
    // PRIMARY KEY
    // ========================================================================
    @Id                                    // ← Đánh dấu là Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ← Auto increment
    private Long id;

    // ========================================================================
    // BASIC FIELDS - Các trường cơ bản
    // ========================================================================
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;                  // ← nullable=false → NOT NULL
                                           // ← unique=true → UNIQUE KEY
    
    @Column(name = "password_hash")
    private String passwordHash;           // ← nullable cho Google OAuth users
    
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;              // ← Không có nullable=false → cho phép NULL
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "google_id", unique = true)
    private String googleId;               // ← Cho đăng nhập Google OAuth

    // ========================================================================
    // ENUM FIELD - Trường kiểu Enum
    // ========================================================================
    
    @Enumerated(EnumType.STRING)           // ← Lưu dạng STRING (không phải số)
    @Column(nullable = false)
    private Role role = Role.STUDENT;      // ← Default value
    
    /**
     * Enum Role - Định nghĩa các vai trò người dùng
     * Được khai báo bên trong Entity hoặc file riêng (Role.java)
     */
    public enum Role {
        STUDENT,    // Học viên
        TEACHER,    // Giảng viên
        ADMIN       // Quản trị viên
    }

    // ========================================================================
    // BOOLEAN FIELD
    // ========================================================================
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;       // ← Default = true

    // ========================================================================
    // TIMESTAMP FIELDS - Thời gian tạo/cập nhật
    // ========================================================================
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;       // ← updatable=false: không cho update sau khi tạo
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================================================================
    // LIFECYCLE CALLBACKS - Tự động set giá trị trước khi lưu
    // ========================================================================
    
    @PrePersist                            // ← Chạy TRƯỚC KHI insert vào DB
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate                             // ← Chạy TRƯỚC KHI update
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // RELATIONSHIPS (sẽ thêm sau khi có các Entity khác)
    // ========================================================================
    // 
    // Ví dụ: User có nhiều Course (1-N relationship)
    // @OneToMany(mappedBy = "instructor")
    // private List<Course> courses;
    //
    // Ví dụ: User có nhiều Enrollment (1-N relationship)  
    // @OneToMany(mappedBy = "user")
    // private List<Enrollment> enrollments;
}

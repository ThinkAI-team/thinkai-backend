package com.thinkai.backend.repository;

import com.thinkai.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    // Đếm theo role
    long countByRole(User.Role role);

    // Đếm theo role + isActive
    long countByRoleAndIsActive(User.Role role, Boolean isActive);

    /**
     * Admin: Tìm kiếm user với bộ lọc động
     * - keyword: tìm theo fullName hoặc email (nullable)
     * - role: lọc theo vai trò (nullable)
     * - isActive: lọc trạng thái (nullable)
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:keyword IS NULL
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role     IS NULL OR u.role     = :role)
              AND (:isActive IS NULL OR u.isActive = :isActive)
            """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("role") User.Role role,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}

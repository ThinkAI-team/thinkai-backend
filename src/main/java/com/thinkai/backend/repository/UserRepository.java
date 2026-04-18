package com.thinkai.backend.repository;

import com.thinkai.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    long countByRole(User.Role role);

    List<User> findByRoleAndIsActiveTrue(User.Role role);

    @Query("""
            SELECT u
            FROM User u
            WHERE (:keyword IS NULL OR
                   LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:isActive IS NULL OR u.isActive = :isActive)
              AND (:approvalStatus IS NULL OR u.approvalStatus = :approvalStatus)
            """)
    Page<User> searchAdminUsers(
            @Param("keyword") String keyword,
            @Param("role") User.Role role,
            @Param("isActive") Boolean isActive,
            @Param("approvalStatus") User.ApprovalStatus approvalStatus,
            Pageable pageable
    );

    @Query("""
            SELECT u
            FROM User u
            WHERE (:keyword IS NULL OR
                   LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:isActive IS NULL OR u.isActive = :isActive)
              AND (:approvalStatus IS NULL OR u.approvalStatus = :approvalStatus)
            """)
    List<User> searchAdminUsersNoPage(
            @Param("keyword") String keyword,
            @Param("role") User.Role role,
            @Param("isActive") Boolean isActive,
            @Param("approvalStatus") User.ApprovalStatus approvalStatus
    );
}

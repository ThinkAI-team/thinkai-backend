package com.thinkai.backend.repository;

import com.thinkai.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}

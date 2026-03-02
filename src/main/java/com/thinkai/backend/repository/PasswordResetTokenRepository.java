package com.thinkai.backend.repository;

import com.thinkai.backend.entity.PasswordResetToken;
import com.thinkai.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime after);

    void deleteAllByUser(User user);
}

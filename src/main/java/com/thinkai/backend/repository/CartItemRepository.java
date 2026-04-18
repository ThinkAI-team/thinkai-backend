package com.thinkai.backend.repository;

import com.thinkai.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndCourseId(Long cartId, Long courseId);
    List<CartItem> findAllByCartIdOrderByAddedAtDesc(Long cartId);
    List<CartItem> findAllByCartUserIdOrderByAddedAtDesc(Long userId);
    boolean existsByCartUserIdAndCourseId(Long userId, Long courseId);
    void deleteAllByCartId(Long cartId);
    void deleteByCartIdAndCourseId(Long cartId, Long courseId);
    void deleteAllByCartUserId(Long userId);
    void deleteByCartUserIdAndCourseId(Long userId, Long courseId);
}

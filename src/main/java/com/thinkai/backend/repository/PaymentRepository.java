package com.thinkai.backend.repository;

import com.thinkai.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderCode(Long orderCode);

    Optional<Payment> findByPaymentLinkId(String paymentLinkId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByCourseId(Long courseId);

    List<Payment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, Payment.PaymentStatus status);
}

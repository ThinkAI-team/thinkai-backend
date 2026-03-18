package com.thinkai.backend.repository;

import com.thinkai.backend.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    List<ExamAttempt> findByUserIdAndExamId(Long userId, Long examId);
    List<ExamAttempt> findByUserId(Long userId);
}

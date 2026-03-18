package com.thinkai.backend.repository;

import com.thinkai.backend.entity.QuestionBank;
import com.thinkai.backend.entity.enums.ExamType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {

    Page<QuestionBank> findByCreatedBy(Long createdBy, Pageable pageable);

    Optional<QuestionBank> findByIdAndCreatedBy(Long id, Long createdBy);

    List<QuestionBank> findByExamTypeAndCreatedBy(ExamType examType, Long createdBy);

    List<QuestionBank> findByExamType(ExamType examType);
}

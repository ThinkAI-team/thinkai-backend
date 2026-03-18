package com.thinkai.backend.repository;

import com.thinkai.backend.entity.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByCourseId(Long courseId);

    Page<Exam> findByCreatedBy(Long createdBy, Pageable pageable);

    Optional<Exam> findByIdAndCreatedBy(Long id, Long createdBy);

    long countByCreatedBy(Long createdBy);
}

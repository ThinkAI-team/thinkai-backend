package com.thinkai.backend.repository;

import com.thinkai.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    List<Enrollment> findByUserId(Long userId);

    int countByCourseId(Long courseId);
}



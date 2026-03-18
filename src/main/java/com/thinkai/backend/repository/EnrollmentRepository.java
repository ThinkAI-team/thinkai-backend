package com.thinkai.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thinkai.backend.entity.Enrollment;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByUserId(Long userId);

    long countByUserId(Long userId);

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    int countByCourseId(Long courseId);

    @Query("""
                    SELECT COUNT(DISTINCT e.userId)
                    FROM Enrollment e
                    JOIN Course c ON e.courseId = c.id
                    WHERE c.instructorId = :instructorId
            """)
    long countEnrolledStudentsByInstructorId(@Param("instructorId") Long instructorId);

    @Query("""
                    SELECT COUNT(DISTINCT e.userId)
                    FROM Enrollment e
                    JOIN Course c ON e.courseId = c.id
                    WHERE c.instructorId = :instructorId
                      AND e.completedAt IS NOT NULL
            """)
    long countCompletedStudentsByInstructorId(@Param("instructorId") Long instructorId);
}


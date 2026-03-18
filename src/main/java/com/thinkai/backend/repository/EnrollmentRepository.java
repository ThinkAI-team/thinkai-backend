package com.thinkai.backend.repository;

import com.thinkai.backend.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

<<<<<<< HEAD
    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    Page<Enrollment> findByUserId(Long userId, Pageable pageable);

    long countByCourseId(Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e JOIN Course c ON e.courseId = c.id WHERE c.instructorId = :instructorId")
    long countEnrolledStudentsByInstructorId(@Param("instructorId") Long instructorId);

    @Query("SELECT COUNT(e) FROM Enrollment e JOIN Course c ON e.courseId = c.id WHERE c.instructorId = :instructorId AND e.progressPercent = 100")
    long countCompletedStudentsByInstructorId(@Param("instructorId") Long instructorId);

    @Query("SELECT COUNT(e) FROM Enrollment e JOIN Course c ON e.courseId = c.id WHERE c.id = :courseId AND e.progressPercent = 100")
    long countCompletedStudentsByCourseId(@Param("courseId") Long courseId);
=======
    List<Enrollment> findByUserId(Long userId);

    long countByUserId(Long userId);

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    int countByCourseId(Long courseId);
>>>>>>> origin/develop
}


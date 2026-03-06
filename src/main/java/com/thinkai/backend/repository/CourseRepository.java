package com.thinkai.backend.repository;

import com.thinkai.backend.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

    Optional<Course> findByIdAndInstructorId(Long id, Long instructorId);

    long countByInstructorId(Long instructorId);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.instructorId = :instructorId AND c.status = :status")
    long countByInstructorIdAndStatus(@Param("instructorId") Long instructorId, @Param("status") Course.Status status);
}

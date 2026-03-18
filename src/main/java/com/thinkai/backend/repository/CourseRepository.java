package com.thinkai.backend.repository;

import com.thinkai.backend.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

<<<<<<< HEAD
import java.util.Optional;
=======
import java.math.BigDecimal;
>>>>>>> origin/develop

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

<<<<<<< HEAD
    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

    Optional<Course> findByIdAndInstructorId(Long id, Long instructorId);

    long countByInstructorId(Long instructorId);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.instructorId = :instructorId AND c.status = :status")
    long countByInstructorIdAndStatus(@Param("instructorId") Long instructorId, @Param("status") Course.Status status);
}
=======
    @Query("""
        SELECT c FROM Course c
        WHERE c.isPublished = true
          AND c.status = 'APPROVED'
          AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:priceMin IS NULL OR c.price >= :priceMin)
          AND (:priceMax IS NULL OR c.price <= :priceMax)
    """)
    Page<Course> findPublishedCourses(
            @Param("keyword") String keyword,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax,
            Pageable pageable
    );
}

>>>>>>> origin/develop

package com.thinkai.backend.repository;

import com.thinkai.backend.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    List<CourseReview> findByCourseIdAndIsApprovedTrueOrderByCreatedAtDesc(Long courseId);

    boolean existsByCourseIdAndUserId(Long courseId, Long userId);
}

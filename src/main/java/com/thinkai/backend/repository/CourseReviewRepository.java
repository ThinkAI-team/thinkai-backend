package com.thinkai.backend.repository;

import com.thinkai.backend.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    // Lấy bài đánh giá theo khoá học và có được approved không
    Page<CourseReview> findByCourseIdAndIsApproved(Long courseId, Boolean isApproved, Pageable pageable);

    // Admin lấy tất cả review theo khoá học
    Page<CourseReview> findByCourseId(Long courseId, Pageable pageable);

    // Tìm review của 1 user trên 1 khoá học (do constraint unique course_id,
    // user_id)
    Optional<CourseReview> findByCourseIdAndUserId(Long courseId, Long userId);
}

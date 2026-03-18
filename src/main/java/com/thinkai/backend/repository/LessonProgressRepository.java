package com.thinkai.backend.repository;

import com.thinkai.backend.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<LessonProgress> findByUserIdAndLessonIdIn(Long userId, List<Long> lessonIds);

    List<LessonProgress> findByUserIdAndIsCompletedTrue(Long userId);

    @Query("SELECT COUNT(lp) FROM LessonProgress lp " +
           "JOIN Lesson l ON lp.lessonId = l.id " +
           "WHERE lp.userId = :userId AND l.courseId = :courseId AND lp.isCompleted = true")
    long countCompletedByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);
}

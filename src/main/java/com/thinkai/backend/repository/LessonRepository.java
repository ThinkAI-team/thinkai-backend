package com.thinkai.backend.repository;

import com.thinkai.backend.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    Optional<Lesson> findByIdAndCourseId(Long id, Long courseId);

    Integer countByCourseId(Long courseId);
}

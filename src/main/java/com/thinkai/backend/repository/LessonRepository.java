package com.thinkai.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkai.backend.entity.Lesson;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    long countByCourseId(Long courseId);

    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    Optional<Lesson> findByIdAndCourseId(Long id, Long courseId);
}



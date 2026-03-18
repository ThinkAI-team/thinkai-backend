package com.thinkai.backend.repository;

import com.thinkai.backend.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    long countByCourseId(Long courseId);

    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);
}

package com.thinkai.backend.service;

import com.thinkai.backend.dto.LessonOrderRequest;
import com.thinkai.backend.dto.LessonOrderUpdate;
import com.thinkai.backend.dto.LessonRequest;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    private static final String UPLOAD_DIR = "uploads/";

    @Transactional
    public Lesson createLesson(Long courseId, Long teacherId, LessonRequest request) {
        verifyCourseOwnership(courseId, teacherId);

        Integer currentCount = lessonRepository.countByCourseId(courseId);
        int nextOrderIndex = currentCount != null ? currentCount : 0;

        Lesson lesson = Lesson.builder()
                .courseId(courseId)
                .title(request.getTitle())
                .type(request.getType())
                .contentUrl(request.getContentUrl())
                .contentText(request.getContentText())
                .durationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : nextOrderIndex)
                .build();

        return lessonRepository.save(lesson);
    }

    @Transactional
    public String uploadLessonFile(Long courseId, Long teacherId, MultipartFile file) {
        verifyCourseOwnership(courseId, teacherId);

        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.write(path, file.getBytes());

            // In production, this would be an S3 or GCS URL
            return "http://localhost:8080/api/files/" + filename; 

        } catch (IOException e) {
            throw new ApiException("Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void reorderLessons(Long courseId, Long teacherId, LessonOrderRequest request) {
        verifyCourseOwnership(courseId, teacherId);

        List<Lesson> currentLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        Map<Long, Lesson> lessonMap = currentLessons.stream()
                .collect(Collectors.toMap(Lesson::getId, l -> l));

        for (LessonOrderUpdate update : request.getLessonOrders()) {
            Lesson lesson = lessonMap.get(update.getLessonId());
            if (lesson != null) {
                lesson.setOrderIndex(update.getOrderIndex());
                lessonRepository.save(lesson);
            }
        }
    }

    private void verifyCourseOwnership(Long courseId, Long teacherId) {
        courseRepository.findByIdAndInstructorId(courseId, teacherId)
                .orElseThrow(() -> new ApiException("Course not found or access denied", HttpStatus.NOT_FOUND));
    }
}

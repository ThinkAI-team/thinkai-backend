package com.thinkai.backend.service;

import com.thinkai.backend.dto.LessonOrderRequest;
import com.thinkai.backend.dto.LessonOrderUpdate;
import com.thinkai.backend.dto.LessonRequest;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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

    @Value("${app.backend-url:http://localhost:8081}")
    private String backendUrl;

    @Transactional
    public Lesson createLesson(Long courseId, Long teacherId, LessonRequest request) {
        verifyCourseOwnership(courseId, teacherId);

        long currentCount = lessonRepository.countByCourseId(courseId);
        int nextOrderIndex = (int) currentCount;

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

    private static final long MAX_FILE_SIZE = 1L * 1024 * 1024 * 1024; // 1GB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "video/mp4", "video/webm", "video/ogg", "video/quicktime", "video/x-msvideo",
            "application/pdf"
    );

    @Transactional
    public String uploadLessonFile(Long courseId, Long teacherId, MultipartFile file) {
        verifyCourseOwnership(courseId, teacherId);

        // Validate file rỗng
        if (file.isEmpty()) {
            throw new ApiException("File không được để trống", HttpStatus.BAD_REQUEST);
        }

        // Validate kích thước
        if (file.getSize() > MAX_FILE_SIZE) {
            String sizeInMB = String.format("%.0f MB", file.getSize() / (1024.0 * 1024));
            throw new ApiException(
                    "File quá lớn (" + sizeInMB + "). Giới hạn tối đa: 1 GB",
                    HttpStatus.valueOf(413));
        }

        // Validate loại file
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(
                    "Định dạng file không hợp lệ. Chỉ chấp nhận: video (mp4, webm, ogg, mov, avi) và pdf",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + filename);

            // Streaming: không load toàn bộ file vào RAM
            file.transferTo(path.toFile());

            String normalizedBackendUrl = backendUrl.endsWith("/")
                    ? backendUrl.substring(0, backendUrl.length() - 1)
                    : backendUrl;
            return normalizedBackendUrl + "/api/files/" + filename;

        } catch (IOException e) {
            throw new ApiException("Upload file thất bại: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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

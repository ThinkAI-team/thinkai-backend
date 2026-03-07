package com.thinkai.backend.service;

import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final CourseRepository courseRepository;

    /**
     * Lấy danh sách bài thi theo khóa học.
     * Kiểm tra khóa học tồn tại trước khi truy vấn.
     *
     * @param courseId ID của khóa học
     * @return danh sách ExamDto
     * @throws ApiException nếu khóa học không tồn tại (404)
     */
    public List<ExamDto> getExamsByCourseId(Long courseId) {
        // 1. Kiểm tra khóa học có tồn tại không
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId,
                        HttpStatus.NOT_FOUND));

        // 2. Lấy danh sách bài thi
        List<Exam> exams = examRepository.findByCourseId(courseId);

        // 3. Chuyển đổi Entity -> DTO
        return exams.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi Exam Entity sang ExamDto.
     */
    private ExamDto toDto(Exam exam) {
        return ExamDto.builder()
                .id(exam.getId())
                .examType(exam.getExamType())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .timeLimitMinutes(exam.getTimeLimitMinutes())
                .passingScore(exam.getPassingScore())
                .createdAt(exam.getCreatedAt())
                .build();
    }
}

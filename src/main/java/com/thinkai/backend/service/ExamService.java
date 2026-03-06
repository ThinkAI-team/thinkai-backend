package com.thinkai.backend.service;

import com.thinkai.backend.dto.ExamRequest;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;

    @Transactional
    public Exam createExam(Long teacherId, ExamRequest request) {
        String partConfigJson = "{}";
        try {
            if (request.getPartConfig() != null && !request.getPartConfig().isEmpty()) {
                StringBuilder sb = new StringBuilder("{");
                int count = 0;
                for (Map.Entry<String, Integer> entry : request.getPartConfig().entrySet()) {
                    sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
                    if (++count < request.getPartConfig().size()) sb.append(",");
                }
                sb.append("}");
                partConfigJson = sb.toString();
            }
        } catch (Exception e) {
            partConfigJson = "{}";
        }

        Exam exam = Exam.builder()
                .courseId(request.getCourseId())
                .title(request.getTitle())
                .examType(request.getExamType())
                .description(request.getDescription())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passingScore(request.getPassingScore())
                .isRandomOrder(request.getIsRandomOrder() != null ? request.getIsRandomOrder() : false)
                .partConfig(partConfigJson)
                .createdBy(teacherId)
                .build();

        return examRepository.save(exam);
    }

    @Transactional(readOnly = true)
    public Page<Exam> getExamsByTeacher(Long teacherId, Pageable pageable) {
        return examRepository.findByCreatedBy(teacherId, pageable);
    }
}

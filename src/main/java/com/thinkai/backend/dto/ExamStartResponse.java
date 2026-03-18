package com.thinkai.backend.dto;

import com.thinkai.backend.entity.enums.ExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response trả về khi user bắt đầu làm bài thi.
 * Chứa thông tin phiên thi + danh sách câu hỏi (không có đáp án).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamStartResponse {

    private Long attemptId;
    private Long examId;
    private ExamType examType;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer totalQuestions;
    private LocalDateTime startedAt;
    private List<QuestionDto> questions;
}

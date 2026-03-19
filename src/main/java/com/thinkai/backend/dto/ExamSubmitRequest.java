package com.thinkai.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body khi nộp bài thi.
 * Chứa attemptId và danh sách câu trả lời.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmitRequest {

    @NotNull(message = "attemptId không được để trống")
    private Long attemptId;

    @NotEmpty(message = "Danh sách câu trả lời không được để trống")
    private List<AnswerDto> answers;
}

package com.thinkai.backend.service;

import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.dto.ExamStartResponse;
import com.thinkai.backend.dto.QuestionDto;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.entity.ExamAttempt;
import com.thinkai.backend.entity.Question;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.ExamAttemptRepository;
import com.thinkai.backend.repository.ExamRepository;
import com.thinkai.backend.repository.QuestionRepository;
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
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;

    // ==================== Feature #1: Exam List ====================

    /**
     * Lấy danh sách bài thi theo khóa học.
     * Kiểm tra khóa học tồn tại trước khi truy vấn.
     */
    public List<ExamDto> getExamsByCourseId(Long courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy khóa học với ID: " + courseId,
                        HttpStatus.NOT_FOUND));

        List<Exam> exams = examRepository.findByCourseId(courseId);

        return exams.stream()
                .map(this::toExamDto)
                .collect(Collectors.toList());
    }

    // ==================== Feature #2: Exam Taking ====================

    /**
     * Bắt đầu làm bài thi.
     * 1. Kiểm tra bài thi tồn tại.
     * 2. Lấy danh sách câu hỏi (ẩn đáp án đúng).
     * 3. Tạo phiên thi (ExamAttempt).
     * 4. Trả về thông tin bài thi + câu hỏi.
     *
     * @param examId ID của bài thi
     * @param userId ID của user (tạm thời truyền qua param, sau sẽ lấy từ JWT)
     * @return ExamStartResponse chứa thông tin phiên thi và câu hỏi
     */
    public ExamStartResponse startExam(Long examId, Long userId) {
        // 1. Kiểm tra bài thi tồn tại
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(
                        "Không tìm thấy bài thi với ID: " + examId,
                        HttpStatus.NOT_FOUND));

        // 2. Lấy danh sách câu hỏi theo thứ tự
        List<Question> questions = questionRepository.findByExamIdOrderByOrderIndexAsc(examId);

        if (questions.isEmpty()) {
            throw new ApiException(
                    "Bài thi chưa có câu hỏi nào",
                    HttpStatus.BAD_REQUEST);
        }

        // 3. Tạo phiên thi (ExamAttempt)
        ExamAttempt attempt = ExamAttempt.builder()
                .userId(userId)
                .examId(examId)
                .totalQuestions(questions.size())
                .build();
        attempt = examAttemptRepository.save(attempt);

        // 4. Map câu hỏi sang DTO (ẩn đáp án đúng)
        List<QuestionDto> questionDtos = questions.stream()
                .map(this::toQuestionDto)
                .collect(Collectors.toList());

        // 5. Trả về response
        return ExamStartResponse.builder()
                .attemptId(attempt.getId())
                .examId(exam.getId())
                .examType(exam.getExamType())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .timeLimitMinutes(exam.getTimeLimitMinutes())
                .totalQuestions(questions.size())
                .startedAt(attempt.getStartedAt())
                .questions(questionDtos)
                .build();
    }

    // ==================== Private Mappers ====================

    private ExamDto toExamDto(Exam exam) {
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

    private QuestionDto toQuestionDto(Question question) {
        return QuestionDto.builder()
                .id(question.getId())
                .content(question.getContent())
                .options(question.getOptions())
                .type(question.getType().name())
                .orderIndex(question.getOrderIndex())
                .build();
    }
}

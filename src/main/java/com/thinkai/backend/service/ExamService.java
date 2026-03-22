package com.thinkai.backend.service;

import com.thinkai.backend.dto.AnswerDto;
import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.dto.ExamHistoryDto;
import com.thinkai.backend.dto.ExamRequest;
import com.thinkai.backend.dto.ExamStartResponse;
import com.thinkai.backend.dto.ExamSubmitRequest;
import com.thinkai.backend.dto.ExamSubmitResponse;
import com.thinkai.backend.dto.QuestionDto;
import com.thinkai.backend.dto.AnswerResultDto;
import com.thinkai.backend.dto.ExamResultResponse;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.entity.ExamAnswer;
import com.thinkai.backend.entity.ExamAttempt;
import com.thinkai.backend.entity.Question;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.ExamAnswerRepository;
import com.thinkai.backend.repository.ExamAttemptRepository;
import com.thinkai.backend.repository.ExamRepository;
import com.thinkai.backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

        private final ExamRepository examRepository;
        private final CourseRepository courseRepository;
        private final QuestionRepository questionRepository;
        private final ExamAttemptRepository examAttemptRepository;
        private final ExamAnswerRepository examAnswerRepository;
        private final AITutorService aiTutorService;

        // ==================== Teacher Operations ====================

        public Exam createExam(Long teacherId, ExamRequest request) {
                Exam exam = Exam.builder()
                                .courseId(request.getCourseId())
                                .title(request.getTitle())
                                .examType(request.getExamType())
                                .description(request.getDescription())
                                .timeLimitMinutes(request.getTimeLimitMinutes() != null ? request.getTimeLimitMinutes()
                                                : 120)
                                .passingScore(request.getPassingScore() != null ? request.getPassingScore() : 60)
                                .isRandomOrder(request.getIsRandomOrder() != null ? request.getIsRandomOrder() : false)
                                .createdBy(teacherId)
                                .build();
                return examRepository.save(exam);
        }

        public Page<Exam> getExamsByTeacher(Long teacherId, Pageable pageable) {
                return examRepository.findByCreatedBy(teacherId, pageable);
        }

        // ==================== Feature #1: Exam List ====================

        /**
         * Lấy danh sách bài thi theo khóa học.
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
         */
        public ExamStartResponse startExam(Long examId, Long userId) {
                Exam exam = examRepository.findById(examId)
                                .orElseThrow(() -> new ApiException(
                                                "Không tìm thấy bài thi với ID: " + examId,
                                                HttpStatus.NOT_FOUND));

                List<Question> questions = questionRepository.findByExamIdOrderByOrderIndexAsc(examId);

                if (questions.isEmpty()) {
                        throw new ApiException(
                                        "Bài thi chưa có câu hỏi nào",
                                        HttpStatus.BAD_REQUEST);
                }

                ExamAttempt attempt = ExamAttempt.builder()
                                .userId(userId)
                                .examId(examId)
                                .totalQuestions(questions.size())
                                .build();
                attempt = examAttemptRepository.save(attempt);

                List<QuestionDto> questionDtos = questions.stream()
                                .map(this::toQuestionDto)
                                .collect(Collectors.toList());

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

        // ==================== Feature #3: Exam Submit ====================

        /**
         * Nộp bài thi và tự động chấm điểm.
         *
         * Luồng xử lý:
         * 1. Tìm ExamAttempt → 404 nếu không tìm thấy.
         * 2. Kiểm tra chưa nộp (submittedAt == null) → 400 nếu đã nộp.
         * 3. Lấy danh sách câu hỏi + đáp án đúng từ DB.
         * 4. Duyệt từng câu trả lời: so sánh selectedOption với correctOption.
         * 5. Lưu ExamAnswer vào DB.
         * 6. Tính điểm, cập nhật ExamAttempt.
         * 7. Trả về kết quả chấm.
         */
        @Transactional
        public ExamSubmitResponse submitExam(Long examId, Long currentUserId, ExamSubmitRequest request) {
                // 1. Tìm phiên thi
                ExamAttempt attempt = examAttemptRepository.findById(request.getAttemptId())
                                .orElseThrow(() -> new ApiException(
                                                "Không tìm thấy phiên thi với ID: " + request.getAttemptId(),
                                                HttpStatus.NOT_FOUND));

                if (!attempt.getUserId().equals(currentUserId)) {
                        throw new ApiException("Bạn không có quyền nộp bài cho phiên thi này", HttpStatus.FORBIDDEN);
                }

                if (!attempt.getExamId().equals(examId)) {
                        throw new ApiException("Phiên thi không thuộc bài thi hiện tại", HttpStatus.BAD_REQUEST);
                }

                // 2. Kiểm tra chưa nộp
                if (attempt.getSubmittedAt() != null) {
                        throw new ApiException(
                                        "Bài thi đã được nộp trước đó",
                                        HttpStatus.BAD_REQUEST);
                }

                // 3. Lấy bài thi và danh sách câu hỏi
                Exam exam = examRepository.findById(attempt.getExamId())
                                .orElseThrow(() -> new ApiException(
                                                "Không tìm thấy bài thi",
                                                HttpStatus.NOT_FOUND));

                List<Question> questions = questionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());

                // Tạo map questionId → Question và correctOption
                Map<Long, Question> questionMap = questions.stream()
                                .collect(Collectors.toMap(Question::getId, q -> q));
                Map<Long, String> correctAnswerMap = questions.stream()
                                .collect(Collectors.toMap(Question::getId, Question::getCorrectOption));

                // 4. Chấm từng câu trả lời
                int correctCount = 0;
                StringBuilder wrongAnswersSummary = new StringBuilder();

                for (AnswerDto answer : request.getAnswers()) {
                        String correctOption = correctAnswerMap.get(answer.getQuestionId());

                        boolean isCorrect = correctOption != null
                                        && normalizeOptionForComparison(correctOption)
                                                        .equals(normalizeOptionForComparison(
                                                                        answer.getSelectedOption()));

                        if (isCorrect) {
                                correctCount++;
                        } else {
                                // Thu thập thông tin câu sai cho AI Feedback
                                Question q = questionMap.get(answer.getQuestionId());
                                if (q != null) {
                                        String contentSnippet = q.getContent().length() > 100
                                                        ? q.getContent().substring(0, 100) + "..."
                                                        : q.getContent();
                                        wrongAnswersSummary.append("- Câu: ").append(contentSnippet)
                                                        .append(" | Bạn chọn: ").append(answer.getSelectedOption())
                                                        .append(" | Đáp án đúng: ").append(correctOption);
                                        if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                                                wrongAnswersSummary.append(" | Giải thích: ")
                                                                .append(q.getExplanation().length() > 150
                                                                                ? q.getExplanation().substring(0, 150)
                                                                                        + "..."
                                                                                : q.getExplanation());
                                        }
                                        wrongAnswersSummary.append("\n");
                                }
                        }

                        // 5. Lưu ExamAnswer
                        ExamAnswer examAnswer = ExamAnswer.builder()
                                        .attemptId(attempt.getId())
                                        .questionId(answer.getQuestionId())
                                        .selectedOption(answer.getSelectedOption())
                                        .isCorrect(isCorrect)
                                        .build();
                        examAnswerRepository.save(examAnswer);
                }

                // 6. Tính điểm
                int totalQuestions = attempt.getTotalQuestions();
                BigDecimal score = BigDecimal.ZERO;
                if (totalQuestions > 0) {
                        score = BigDecimal.valueOf(correctCount)
                                        .multiply(BigDecimal.valueOf(100))
                                        .divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP);
                }

                boolean isPassed = score.compareTo(BigDecimal.valueOf(exam.getPassingScore())) >= 0;
                LocalDateTime submittedAt = LocalDateTime.now();

                // Tính thời gian làm bài
                long timeTakenSeconds = Duration.between(attempt.getStartedAt(), submittedAt).getSeconds();

                // 7. Tạo AI Feedback (không làm hỏng submit nếu Gemini lỗi)
                String aiFeedback = null;
                try {
                        aiFeedback = aiTutorService.generateExamFeedback(
                                        exam.getTitle(),
                                        score,
                                        correctCount,
                                        totalQuestions,
                                        isPassed,
                                        wrongAnswersSummary.length() > 0 ? wrongAnswersSummary.toString() : null);
                } catch (Exception e) {
                        // Log nhưng không throw — nộp bài vẫn thành công
                        org.slf4j.LoggerFactory.getLogger(ExamService.class)
                                        .warn("Không thể tạo AI Feedback: {}", e.getMessage());
                }

                // 8. Cập nhật ExamAttempt
                attempt.setScore(score);
                attempt.setCorrectCount(correctCount);
                attempt.setIsPassed(isPassed);
                attempt.setAiFeedback(aiFeedback);
                attempt.setSubmittedAt(submittedAt);
                examAttemptRepository.save(attempt);

                // 9. Trả về kết quả
                return ExamSubmitResponse.builder()
                                .attemptId(attempt.getId())
                                .examTitle(exam.getTitle())
                                .score(score)
                                .correctCount(correctCount)
                                .totalQuestions(totalQuestions)
                                .isPassed(isPassed)
                                .passingScore(exam.getPassingScore())
                                .submittedAt(submittedAt)
                                .timeTakenSeconds(timeTakenSeconds)
                                .build();
        }

        // ==================== Feature #4: Exam Result ====================

        /**
         * Xem chi tiết kết quả bài thi.
         *
         * 1. Tìm ExamAttempt → 404 nếu không tìm thấy.
         * 2. Kiểm tra đã nộp (submittedAt != null) → 400 nếu chưa nộp.
         * 3. Load Exam, Questions, Answers.
         * 4. Map sang AnswerResultDto (kèm correctOption, explanation).
         * 5. Trả về ExamResultResponse.
         */
        public ExamResultResponse getExamResult(Long attemptId, Long currentUserId) {
                // 1. Tìm phiên thi
                ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                                .orElseThrow(() -> new ApiException(
                                                "Không tìm thấy phiên thi với ID: " + attemptId,
                                                HttpStatus.NOT_FOUND));

                if (!attempt.getUserId().equals(currentUserId)) {
                        throw new ApiException("Bạn không có quyền xem kết quả phiên thi này", HttpStatus.FORBIDDEN);
                }

                // 2. Kiểm tra đã nộp
                if (attempt.getSubmittedAt() == null) {
                        throw new ApiException(
                                        "Bài thi chưa được nộp, không thể xem kết quả",
                                        HttpStatus.BAD_REQUEST);
                }

                // 3. Load exam info
                Exam exam = examRepository.findById(attempt.getExamId())
                                .orElseThrow(() -> new ApiException(
                                                "Không tìm thấy bài thi",
                                                HttpStatus.NOT_FOUND));

                // 4. Load questions và answers
                List<Question> questions = questionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
                List<ExamAnswer> answers = examAnswerRepository.findByAttemptId(attemptId);

                // Map answerId → ExamAnswer để tra cứu nhanh
                Map<Long, ExamAnswer> answerMap = answers.stream()
                                .collect(Collectors.toMap(ExamAnswer::getQuestionId, a -> a));

                // 5. Map sang AnswerResultDto
                List<AnswerResultDto> answerResults = questions.stream()
                                .map(q -> {
                                        ExamAnswer ans = answerMap.get(q.getId());
                                        return AnswerResultDto.builder()
                                                        .questionId(q.getId())
                                                        .content(q.getContent())
                                                        .options(q.getOptions())
                                                        .orderIndex(q.getOrderIndex())
                                                        .selectedOption(ans != null ? ans.getSelectedOption() : null)
                                                        .correctOption(q.getCorrectOption())
                                                        .isCorrect(ans != null ? ans.getIsCorrect() : false)
                                                        .explanation(q.getExplanation())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // Tính thời gian làm bài
                long timeTakenSeconds = Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();

                // 6. Trả về response
                return ExamResultResponse.builder()
                                .attemptId(attempt.getId())
                                .examTitle(exam.getTitle())
                                .examType(exam.getExamType())
                                .score(attempt.getScore())
                                .correctCount(attempt.getCorrectCount())
                                .totalQuestions(attempt.getTotalQuestions())
                                .isPassed(attempt.getIsPassed())
                                .passingScore(exam.getPassingScore())
                                .startedAt(attempt.getStartedAt())
                                .submittedAt(attempt.getSubmittedAt())
                                .timeTakenSeconds(timeTakenSeconds)
                                .aiFeedback(attempt.getAiFeedback())
                                .answers(answerResults)
                                .build();
        }

        // ==================== Feature #5: Exam History ====================

        /**
         * Lấy lịch sử thi của user.
         * Chỉ trả về các lần thi đã nộp, sắp xếp mới nhất trước.
         */
        public List<ExamHistoryDto> getExamHistory(Long currentUserId) {
                List<ExamAttempt> attempts = examAttemptRepository.findByUserId(currentUserId);

                return attempts.stream()
                                // Chỉ lấy các lần thi đã nộp
                                .filter(a -> a.getSubmittedAt() != null)
                                // Sắp xếp mới nhất trước
                                .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                                .map(attempt -> {
                                        // Load exam info
                                        Exam exam = examRepository.findById(attempt.getExamId()).orElse(null);
                                        String examTitle = exam != null ? exam.getTitle() : "Bài thi đã bị xóa";

                                        long timeTakenSeconds = Duration.between(
                                                        attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();

                                        return ExamHistoryDto.builder()
                                                        .attemptId(attempt.getId())
                                                        .examId(attempt.getExamId())
                                                        .examTitle(examTitle)
                                                        .examType(exam != null ? exam.getExamType() : null)
                                                        .score(attempt.getScore())
                                                        .correctCount(attempt.getCorrectCount())
                                                        .totalQuestions(attempt.getTotalQuestions())
                                                        .isPassed(attempt.getIsPassed())
                                                        .startedAt(attempt.getStartedAt())
                                                        .submittedAt(attempt.getSubmittedAt())
                                                        .timeTakenSeconds(timeTakenSeconds)
                                                        .build();
                                })
                                .collect(Collectors.toList());
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

        /**
         * Chuẩn hóa option để so sánh: DB lưu "A", frontend gửi "A. Reading".
         * Trích xuất key (A, B, C...) từ format "X. ..." để so khớp đúng.
         */
        private String normalizeOptionForComparison(String option) {
                if (option == null || option.isBlank()) {
                        return "";
                }
                String trimmed = option.trim();
                // Format "A. Reading" hoặc "B. Room 2" -> lấy "A" hoặc "B"
                if (trimmed.matches("^[A-Za-z]\\.\\s+.*")) {
                        return trimmed.substring(0, 1).toUpperCase();
                }
                return trimmed.toUpperCase();
        }
}

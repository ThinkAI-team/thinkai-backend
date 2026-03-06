package com.thinkai.backend.service;

import com.thinkai.backend.dto.QuestionBankRequest;
import com.thinkai.backend.entity.QuestionBank;
import com.thinkai.backend.entity.enums.ExamType;
import com.thinkai.backend.entity.enums.Part;
import com.thinkai.backend.entity.enums.Section;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.QuestionBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionBankService {

    private final QuestionBankRepository questionBankRepository;

    @Transactional
    public QuestionBank createQuestion(Long teacherId, QuestionBankRequest request) {
        String tagsJson = "[]";
        try {
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < request.getTags().size(); i++) {
                    sb.append("\"").append(request.getTags().get(i).replace("\"", "\\\"")).append("\"");
                    if (i < request.getTags().size() - 1) sb.append(",");
                }
                sb.append("]");
                tagsJson = sb.toString();
            }
        } catch (Exception e) {
            tagsJson = "[]";
        }

        QuestionBank question = QuestionBank.builder()
                .examType(request.getExamType())
                .section(request.getSection())
                .part(request.getPart())
                .content(request.getContent())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .explanation(request.getExplanation())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .difficulty(request.getDifficulty())
                .tags(tagsJson)
                .createdBy(teacherId)
                .build();

        return questionBankRepository.save(question);
    }

    @Transactional(readOnly = true)
    public Page<QuestionBank> getQuestionsByTeacher(Long teacherId, Pageable pageable) {
        return questionBankRepository.findByCreatedBy(teacherId, pageable);
    }

    @Transactional(readOnly = true)
    public QuestionBank getQuestionByIdAndTeacher(Long id, Long teacherId) {
        return questionBankRepository.findByIdAndCreatedBy(id, teacherId)
                .orElseThrow(() -> new ApiException("Question not found or access denied", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public List<QuestionBank> importFromCsv(Long teacherId, MultipartFile file) {
        List<QuestionBank> importedQuestions = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (isFirstLine) { // Skip header
                    isFirstLine = false;
                    continue;
                }
                
                String[] data = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); // Split by comma ignoring commas inside quotes
                if (data.length < 7) continue;

                // Simple CSV format mapping: examType,section,part,content,options(JSON),correctAnswer,difficulty
                ExamType examType = ExamType.valueOf(data[0].replace("\"", "").trim());
                Section section = Section.valueOf(data[1].replace("\"", "").trim());
                Part part = Part.valueOf(data[2].replace("\"", "").trim());
                String content = data[3].trim().replace("\"\"", "\"");
                if (content.startsWith("\"") && content.endsWith("\"")) content = content.substring(1, content.length() - 1);
                
                String options = data[4].trim().replace("\"\"", "\""); // handle escaped quotes
                if(options.startsWith("\"") && options.endsWith("\"")) options = options.substring(1, options.length() -1);
                
                String correctAnswer = data[5].trim().replace("\"\"", "\"");
                if (correctAnswer.startsWith("\"") && correctAnswer.endsWith("\"")) correctAnswer = correctAnswer.substring(1, correctAnswer.length() - 1);
                
                QuestionBank.Difficulty difficulty = QuestionBank.Difficulty.valueOf(data[6].replace("\"", "").trim());

                QuestionBank q = QuestionBank.builder()
                        .examType(examType)
                        .section(section)
                        .part(part)
                        .content(content)
                        .options(options)
                        .correctAnswer(correctAnswer)
                        .difficulty(difficulty)
                        .createdBy(teacherId)
                        .build();
                
                importedQuestions.add(q);
            }
            
            return questionBankRepository.saveAll(importedQuestions);

        } catch (Exception e) {
            throw new ApiException("Failed to parse CSV file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}

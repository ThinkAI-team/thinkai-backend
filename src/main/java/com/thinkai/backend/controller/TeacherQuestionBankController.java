package com.thinkai.backend.controller;

import com.thinkai.backend.dto.QuestionBankRequest;
import com.thinkai.backend.entity.QuestionBank;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.service.QuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher/questions")
@RequiredArgsConstructor
public class TeacherQuestionBankController {

    private final QuestionBankService questionBankService;
    private final UserRepository userRepository;

    private Long getTeacherId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .map(User::getId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<QuestionBank> createQuestion(Authentication auth, @Valid @RequestBody QuestionBankRequest request) {
        QuestionBank question = questionBankService.createQuestion(getTeacherId(auth), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(question);
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importQuestions(
            Authentication auth,
            @RequestParam("file") MultipartFile file) {
        List<QuestionBank> imported = questionBankService.importFromCsv(getTeacherId(auth), file);
        return ResponseEntity.ok(Map.of(
            "message", "Import successful",
            "count", imported.size()
        ));
    }

    @GetMapping("/bank")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Page<QuestionBank>> getQuestionBank(Authentication auth, Pageable pageable) {
        Page<QuestionBank> questions = questionBankService.getQuestionsByTeacher(getTeacherId(auth), pageable);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<QuestionBank> getQuestionDetail(Authentication auth, @PathVariable Long id) {
        QuestionBank question = questionBankService.getQuestionByIdAndTeacher(id, getTeacherId(auth));
        return ResponseEntity.ok(question);
    }
}

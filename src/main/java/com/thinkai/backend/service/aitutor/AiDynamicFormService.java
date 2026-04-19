package com.thinkai.backend.service.aitutor;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiDynamicFormService {

    private final AiToolRegistryService toolRegistry;
    private final ObjectMapper objectMapper;

    private static final Map<String, Map<String, String>> FIELD_PROMPTS;
    
    static {
        Map<String, Map<String, String>> prompts = new LinkedHashMap<>();
        
        Map<String, String> courseIdMap = new HashMap<>();
        courseIdMap.put("vi", "Bạn muốn thao tác với khóa học nào? Vui lòng cung cấp courseId (ví dụ: 123)");
        courseIdMap.put("en", "Which course do you want to work with? Please provide courseId (e.g., 123)");
        prompts.put("courseId", courseIdMap);
        
        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("vi", "Tiêu đề là gì?");
        titleMap.put("en", "What is the title?");
        prompts.put("title", titleMap);
        
        Map<String, String> descMap = new HashMap<>();
        descMap.put("vi", "Mô tả nội dung khóa học?");
        descMap.put("en", "Please provide a description for the course.");
        prompts.put("description", descMap);
        
        Map<String, String> priceMap = new HashMap<>();
        priceMap.put("vi", "Giá khóa học là bao nhiêu (VND)?");
        priceMap.put("en", "What is the course price (in VND)?");
        prompts.put("price", priceMap);
        
        Map<String, String> thumbMap = new HashMap<>();
        thumbMap.put("vi", "Bạn muốn dùng ảnh thumbnail nào?");
        thumbMap.put("en", "Which thumbnail image would you like to use?");
        prompts.put("thumbnailUrl", thumbMap);
        
        Map<String, String> lessonIdMap = new HashMap<>();
        lessonIdMap.put("vi", "Bạn muốn thao tác với bài học nào? Vui lòng cung cấp lessonId");
        lessonIdMap.put("en", "Which lesson do you want to work with? Please provide lessonId");
        prompts.put("lessonId", lessonIdMap);
        
        Map<String, String> examIdMap = new HashMap<>();
        examIdMap.put("vi", "Bạn muốn thao tác với bài thi nào? Vui lòng cung cấp examId");
        examIdMap.put("en", "Which exam do you want to work with? Please provide examId");
        prompts.put("examId", examIdMap);
        
        Map<String, String> contentUrlMap = new HashMap<>();
        contentUrlMap.put("vi", "URL nội dung bài học (video/PDF)?");
        contentUrlMap.put("en", "What is the content URL for this lesson (video/PDF)?");
        prompts.put("contentUrl", contentUrlMap);
        
        Map<String, String> contentTextMap = new HashMap<>();
        contentTextMap.put("vi", "Nội dung text cho bài học?");
        contentTextMap.put("en", "What is the text content for this lesson?");
        prompts.put("contentText", contentTextMap);
        
        Map<String, String> durationMap = new HashMap<>();
        durationMap.put("vi", "Thời lượng bài học (giây)?");
        durationMap.put("en", "What is the lesson duration (in seconds)?");
        prompts.put("durationSeconds", durationMap);
        
        Map<String, String> orderMap = new HashMap<>();
        orderMap.put("vi", "Thứ tự bài học trong khóa?");
        orderMap.put("en", "What is the order index for this lesson?");
        prompts.put("orderIndex", orderMap);
        
        Map<String, String> timeLimitMap = new HashMap<>();
        timeLimitMap.put("vi", "Thời gian làm bài (phút)?");
        timeLimitMap.put("en", "What is the time limit for the exam (in minutes)?");
        prompts.put("timeLimitMinutes", timeLimitMap);
        
        Map<String, String> passingMap = new HashMap<>();
        passingMap.put("vi", "Điểm để đạt (%?)");
        passingMap.put("en", "What is the passing score (%)?");
        prompts.put("passingScore", passingMap);
        
        Map<String, String> examTypeMap = new HashMap<>();
        examTypeMap.put("vi", "Loại bài thi (TOEIC/IELTS)?");
        examTypeMap.put("en", "What type of exam (TOEIC/IELTS)?");
        prompts.put("examType", examTypeMap);
        
        Map<String, String> questionTextMap = new HashMap<>();
        questionTextMap.put("vi", "Nội dung câu hỏi?");
        questionTextMap.put("en", "What is the question text?");
        prompts.put("questionText", questionTextMap);
        
        Map<String, String> answerMap = new HashMap<>();
        answerMap.put("vi", "Đáp án đúng?");
        answerMap.put("en", "What is the correct answer?");
        prompts.put("correctAnswer", answerMap);
        
        FIELD_PROMPTS = Collections.unmodifiableMap(prompts);
    }

    public AiDynamicFormService(AiToolRegistryService toolRegistry, ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    public DynamicFormResult analyzeMissingFields(String action, JsonNode args, String language) {
        var optDef = toolRegistry.find(action);
        if (optDef.isEmpty()) {
            return DynamicFormResult.noMissing();
        }

        var def = optDef.get();
        List<String> requiredFields = def.requiredFields();
        
        if (requiredFields == null || requiredFields.isEmpty()) {
            return DynamicFormResult.noMissing();
        }

        List<MissingFieldInfo> missing = new ArrayList<>();
        for (String field : requiredFields) {
            if (args == null || !args.has(field) || args.get(field) == null || args.get(field).isNull()) {
                String prompt = getFieldPrompt(field, language);
                missing.add(new MissingFieldInfo(field, prompt));
            }
        }

        if (missing.isEmpty()) {
            return DynamicFormResult.noMissing();
        }

        return new DynamicFormResult(true, missing);
    }

    private String getFieldPrompt(String field, String language) {
        Map<String, String> fieldPrompts = FIELD_PROMPTS.get(field);
        if (fieldPrompts == null) {
            return "Thiếu thông tin: " + field;
        }

        String lang = language != null && language.toLowerCase().startsWith("vi") ? "vi" : "en";
        return fieldPrompts.getOrDefault(lang, fieldPrompts.get("en"));
    }

    public String buildFollowUpQuestion(DynamicFormResult formResult, String language) {
        if (!formResult.hasMissingFields()) {
            return null;
        }

        List<MissingFieldInfo> missing = formResult.missingFields();
        if (missing.size() == 1) {
            return missing.get(0).prompt();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Cần thêm thông tin:\n");
        for (int i = 0; i < missing.size(); i++) {
            sb.append("- ").append(missing.get(i).prompt()).append("\n");
        }
        return sb.toString().trim();
    }

    public record DynamicFormResult(
            boolean hasMissingFields,
            List<MissingFieldInfo> missingFields) {

        public static DynamicFormResult noMissing() {
            return new DynamicFormResult(false, List.of());
        }
    }

    public record MissingFieldInfo(
            String fieldName,
            String prompt) {}
}
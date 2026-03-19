package com.thinkai.backend.service;

import com.thinkai.backend.dto.AIChatRequest;
import com.thinkai.backend.dto.AIChatResponse;
import com.thinkai.backend.dto.AISummarizeRequest;
import com.thinkai.backend.dto.AISummarizeResponse;
import com.thinkai.backend.entity.AiChatLog;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.dto.AiSettingsDto;
import com.thinkai.backend.repository.AiChatLogRepository;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AITutorService {

    private final RestClient restClient;
    private final AiChatLogRepository aiChatLogRepository;
    private final UserRepository userRepository;
    private final AiSettingsService aiSettingsService;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public AITutorService(RestClient.Builder restClientBuilder,
                          AiChatLogRepository aiChatLogRepository,
                          UserRepository userRepository,
                          AiSettingsService aiSettingsService) {
        this.restClient = restClientBuilder.build();
        this.aiChatLogRepository = aiChatLogRepository;
        this.userRepository = userRepository;
        this.aiSettingsService = aiSettingsService;
    }

    public AIChatResponse chat(AIChatRequest request, String email) {
        User user = null;
        AiSettingsDto settings = null;
        if (email != null) {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
            settings = aiSettingsService.getSettings(email);
        }

        String language = settings != null ? settings.getLanguage() : "English";
        String responseLength = settings != null ? settings.getResponseLength() : "detailed";

        String context = request.getContext() != null ? request.getContext() : "General English";
        String prompt = "You are an AI English Tutor for TOEIC/IELTS students. Your role is strictly to help users learn English. " +
                "If the user asks a question that is NOT related to English learning, grammar, vocabulary, TOEIC, or IELTS, " +
                "you MUST politely refuse to answer and remind them of your role. " +
                "Do not answer general knowledge questions outside the scope of English learning.\n" +
                "IMPORTANT INSTRUCTIONS FROM USER: Please reply in " + language + " language. Ensure your response is " + responseLength + ".\n" +
                "Context of current lesson: " + context + "\n" +
                "Student: " + request.getMessage();
        
        long startTime = System.currentTimeMillis();
        String responseText = callGeminiApi(prompt);
        long responseTimeMs = System.currentTimeMillis() - startTime;

        if (user != null) {
            AiChatLog chatLog = AiChatLog.builder()
                    .userId(user.getId())
                    .userMessage(request.getMessage())
                    .aiResponse(responseText)
                    .responseTimeMs((int) responseTimeMs)
                    .build();
            aiChatLogRepository.save(chatLog);
        }

        return new AIChatResponse(responseText);
    }

    public List<AiChatLog> getChatHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return aiChatLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public AiChatLog getChatById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return aiChatLogRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException("Chat not found or access denied", HttpStatus.NOT_FOUND));
    }

    public AiChatLog submitFeedback(Long id, String email, com.thinkai.backend.dto.AiFeedbackRequest request) {
        AiChatLog chatLog = getChatById(id, email);
        chatLog.setRating(request.getRating());
        return aiChatLogRepository.save(chatLog);
    }

    public void deleteChat(Long id, String email) {
        AiChatLog chatLog = getChatById(id, email);
        aiChatLogRepository.delete(chatLog);
    }

    public AISummarizeResponse summarize(AISummarizeRequest request) {
        String prompt = "You are an AI English Tutor. Please summarize the following lesson content concisely. " +
                "Highlight key grammar rules, vocabulary, or structures to help the student review. " +
                "Only summarize the provided English lesson content. If the content is not related to English learning, refuse to summarize it.\n\n" +
                request.getContent();
        
        String responseText = callGeminiApi(prompt);
        return new AISummarizeResponse(responseText);
    }

    private String callGeminiApi(String prompt) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            return "System Error: Gemini API key is not configured. Please contact the administrator.";
        }

        String requestUrl = geminiApiUrl.contains("?") 
            ? geminiApiUrl + "&key=" + geminiApiKey 
            : geminiApiUrl + "?key=" + geminiApiKey;

        // Gemini API Request Body Structure
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri(requestUrl)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            return extractTextFromResponse(response);
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            e.printStackTrace();
            return "Sorry, I encountered an error while processing your request. Please try again later.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<?, ?> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            return "No valid response from AI.";
        } catch (Exception e) {
            System.err.println("Failed to parse AI response: " + e.getMessage());
            return "Failed to parse AI response.";
        }
    }
}

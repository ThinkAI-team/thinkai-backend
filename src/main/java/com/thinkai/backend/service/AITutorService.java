package com.thinkai.backend.service;

import com.thinkai.backend.dto.AIChatRequest;
import com.thinkai.backend.dto.AIChatResponse;
import com.thinkai.backend.dto.AISummarizeRequest;
import com.thinkai.backend.dto.AISummarizeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AITutorService {

    private final RestClient restClient;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public AITutorService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public AIChatResponse chat(AIChatRequest request) {
        String prompt = "You are an AI English Tutor for TOEIC/IELTS students. Your role is strictly to help users learn English. " +
                "If the user asks a question that is NOT related to English learning, grammar, vocabulary, TOEIC, or IELTS, you MUST politely refuse to answer and remind them of your role. " +
                "Do not answer general knowledge questions outside the scope of English learning.\n" +
                "Context of current lesson: " + (request.getContext() != null ? request.getContext() : "General English") + "\n" +
                "Student: " + request.getMessage();
        
        String responseText = callGeminiApi(prompt);
        return new AIChatResponse(responseText);
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

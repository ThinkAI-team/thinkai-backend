package com.thinkai.backend.ai.llm;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.dto.AdminAiRuntimeSettingsDto;
import com.thinkai.backend.service.AiRuntimeSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class OpenRouterService implements LLMService {

    private final RestTemplate restTemplate;
    private final AiRuntimeSettingsService aiRuntimeSettingsService;

    @Value("${openrouter.api.url}")
    private String openRouterUrl;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.keys:}")
    private String apiKeys;

    @Value("${openrouter.api.models:}")
    private String models;

    @Value("${openrouter.api.model}")
    private String model;

    @Value("${openrouter.api.temperature:0.3}")
    private double temperature;

    @Value("${openrouter.api.max-tokens:700}")
    private int maxTokens;

    @Value("${openrouter.api.top-p:0.9}")
    private double topP;
    private final AtomicInteger apiKeyCursor = new AtomicInteger(0);

    public OpenRouterService(AiRuntimeSettingsService aiRuntimeSettingsService) {
        this.restTemplate = new RestTemplate();
        this.aiRuntimeSettingsService = aiRuntimeSettingsService;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        if (model != null && !model.isBlank()) {
            LLMConfig.setDefaultModel(model);
            log.info("Set default LLM model to: {}", model);
        }
    }

    @Override
    public LLMResponse call(AgentType agent, String userMessage, 
                           List<ChatMessage> history, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();

        List<String> modelPool = resolveModelPool();
        if (modelPool.isEmpty()) {
            long latency = System.currentTimeMillis() - startTime;
            return LLMResponse.builder()
                    .content("AI Harness đang tạm thời không khả dụng do cấu hình model.")
                    .agentUsed(agent.getCode())
                    .latencyMs(latency)
                    .modelUsed("none")
                    .metadata(Map.of("error", "Harness disabled or all models blocked"))
                    .build();
        }

        Map<String, String> modelFailures = new LinkedHashMap<>();

        for (int attempt = 0; attempt < modelPool.size(); attempt++) {
            String currentModel = modelPool.get(attempt);
            List<String> keyPool = resolveApiKeyPool();
            if (keyPool.isEmpty()) {
                long latency = System.currentTimeMillis() - startTime;
                return LLMResponse.builder()
                        .content("AI Harness đang tạm thời không khả dụng do thiếu API key.")
                        .agentUsed(agent.getCode())
                        .latencyMs(latency)
                        .modelUsed(currentModel)
                        .metadata(Map.of("error", "No OpenRouter API key configured"))
                        .build();
            }
            
            try {
                log.info("LLM call: agent={}, model={}, attempt={}/{}", 
                    agent, currentModel, attempt + 1, modelPool.size());
                log.info("OpenRouter key pool size: {}", keyPool.size());
                
                LLMConfig config = LLMConfig.of(agent);
                
                List<ChatMessage> messages = buildMessages(agent, userMessage, history, context);
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", currentModel);
                requestBody.put("messages", messages);
                requestBody.put("temperature", config.temperature());
                requestBody.put("max_tokens", config.maxTokens());
                requestBody.put("top_p", config.topP());
                
                ResponseEntity<Map> response = null;
                Exception lastKeyError = null;

                for (int keyAttempt = 0; keyAttempt < keyPool.size(); keyAttempt++) {
                    String currentKey = keyPool.get(keyAttempt);
                    try {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(currentKey);

                        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                        log.debug("Calling OpenRouter: url={}, model={}, keyAttempt={}/{}",
                                openRouterUrl, currentModel, keyAttempt + 1, keyPool.size());

                        response = restTemplate.exchange(
                                openRouterUrl,
                                HttpMethod.POST,
                                request,
                                Map.class);
                        lastKeyError = null;
                        break;
                    } catch (Exception keyError) {
                        lastKeyError = keyError;
                        log.warn("OpenRouter key failed: model={}, keyAttempt={}/{}, reason={}",
                                currentModel, keyAttempt + 1, keyPool.size(), keyError.getMessage());
                        if (!isRetryable(keyError) || keyAttempt == keyPool.size() - 1) {
                            break;
                        }
                    }
                }

                if (lastKeyError != null) {
                    throw lastKeyError;
                }

                long latency = System.currentTimeMillis() - startTime;
                log.info("LLM call success: agent={}, model={}, latency={}ms", agent, currentModel, latency);

                if (response != null && response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return parseResponse(response.getBody(), agent, latency, currentModel);
                }
                throw new RuntimeException("LLM API returned non-OK or empty body");
                
            } catch (Exception e) {
                log.warn("Model {} failed: {}", currentModel, e.getMessage());
                modelFailures.put(currentModel, describeModelFailure(e));
                
                // If rate limited or server error, try next model
                if (isRetryable(e)) {
                    log.info("Trying next model...");
                    if (attempt < modelPool.size() - 1) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                }
                
                // Last attempt failed
                if (attempt == modelPool.size() - 1) {
                    log.error("All models failed: {}", e.getMessage());
                    long latency = System.currentTimeMillis() - startTime;
                    return LLMResponse.builder()
                        .content(buildHarnessFailureMessage(modelFailures))
                        .agentUsed(agent.getCode())
                        .latencyMs(latency)
                        .modelUsed(currentModel)
                        .metadata(Map.of("error", e.getMessage()))
                        .build();
                }
            }
        }
        
        // Fallback (should not reach here)
        long latency = System.currentTimeMillis() - startTime;
        return LLMResponse.builder()
            .content(buildHarnessFailureMessage(modelFailures))
            .agentUsed(agent.getCode())
            .latencyMs(latency)
            .modelUsed(modelPool.get(0))
            .build();
    }

    private List<String> resolveApiKeyPool() {
        List<String> keys = new ArrayList<>();
        if (apiKeys != null && !apiKeys.isBlank()) {
            for (String raw : apiKeys.split(",")) {
                if (raw != null && !raw.trim().isBlank()) {
                    keys.add(raw.trim());
                }
            }
        }
        if (keys.isEmpty() && apiKey != null && !apiKey.isBlank()) {
            keys.add(apiKey.trim());
        }
        if (keys.size() <= 1) {
            return keys;
        }
        int start = Math.floorMod(apiKeyCursor.getAndIncrement(), keys.size());
        List<String> rotated = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            rotated.add(keys.get((start + i) % keys.size()));
        }
        return rotated;
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof RestClientResponseException httpEx) {
            int status = httpEx.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        String msg = e.getMessage();
        if (msg == null) {
            return true;
        }
        String normalized = msg.toLowerCase();
        return normalized.contains("timeout")
                || normalized.contains("timed out")
                || normalized.contains("connection reset")
                || normalized.contains("connectexception")
                || normalized.contains("i/o error");
    }

    private List<String> resolveModelPool() {
        if (!aiRuntimeSettingsService.isHarnessEnabled()) {
            return List.of();
        }

        AdminAiRuntimeSettingsDto runtimeSettings = aiRuntimeSettingsService.getSettings();
        List<String> configuredPool = runtimeSettings.getHarnessModels() != null
                ? runtimeSettings.getHarnessModels()
                : List.of();

        List<String> source = configuredPool.isEmpty() ? defaultModelPool() : configuredPool;
        Set<String> unique = new LinkedHashSet<>();
        for (String candidate : source) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String normalized = candidate.trim();
            if (aiRuntimeSettingsService.isModelBlocked(normalized)) {
                continue;
            }
            unique.add(normalized);
        }
        return new ArrayList<>(unique);
    }

    private List<String> defaultModelPool() {
        Set<String> defaults = new LinkedHashSet<>();
        if (models != null && !models.isBlank()) {
            for (String raw : models.split(",")) {
                if (raw != null && !raw.trim().isBlank()) {
                    defaults.add(raw.trim());
                }
            }
        }
        if (model != null && !model.isBlank()) {
            defaults.add(model.trim());
        }
        return new ArrayList<>(defaults);
    }

    @Override
    public StreamResponse stream(AgentType agent, String userMessage,
                                  List<ChatMessage> history, Map<String, Object> context) {
        throw new UnsupportedOperationException("Streaming not implemented yet");
    }

    private List<ChatMessage> buildMessages(AgentType agent, String userMessage,
                                            List<ChatMessage> history,
                                            Map<String, Object> context) {
        var messages = new java.util.ArrayList<ChatMessage>();
        
        messages.add(buildSystemMessage(agent, context));
        
        if (history != null && !history.isEmpty()) {
            int maxHistory = 10;
            int start = Math.max(0, history.size() - maxHistory);
            for (int i = start; i < history.size(); i++) {
                messages.add(history.get(i));
            }
        }
        
        messages.add(ChatMessage.user(userMessage));
        
        return messages;
    }

    private ChatMessage buildSystemMessage(AgentType agent, Map<String, Object> context) {
        String systemPrompt = getSystemPrompt(agent);
        
        if (context != null && !context.isEmpty()) {
            StringBuilder contextStr = new StringBuilder();
            
            if (context.containsKey("userLevel")) {
                contextStr.append("User level: ").append(context.get("userLevel")).append(". ");
            }
            if (context.containsKey("weakPoints")) {
                contextStr.append("Weak points: ").append(context.get("weakPoints")).append(". ");
            }
            if (context.containsKey("targetExam")) {
                contextStr.append("Target exam: ").append(context.get("targetExam")).append(". ");
            }
            
            if (contextStr.length() > 0) {
                systemPrompt = systemPrompt + "\n\nContext: " + contextStr;
            }
        }
        
        return ChatMessage.system(systemPrompt);
    }

    private String getSystemPrompt(AgentType agent) {
        return switch (agent) {
            case TOEIC_READING ->
                "You are a TOEIC Reading specialist. Focus on Part 5 (grammar), " +
                "Part 6 (text completion), and Part 7 (reading comprehension). " +
                "Always explain WHY an answer is correct and WHY others are wrong. " +
                "Target: 450+ Reading score.";
            case TOEIC_LISTENING ->
                "You are a TOEIC Listening specialist. Cover Part 1 (photographs), " +
                "Part 2 (question-response), Part 3 (conversations), and Part 4 (talks). " +
                "Teach note-taking strategies. Target: 450+ Listening score.";
            case TOEIC_GRAMMAR ->
                "You are a TOEIC grammar specialist. Focus on high-frequency patterns: " +
                "S-V agreement, tenses, prepositions, articles, and conjunctions. " +
                "Provide clear rules with examples.";
            case TOEIC_VOCABULARY ->
                "You are a TOEIC vocabulary specialist. Focus on business vocabulary: " +
                "meetings, emails, travel, finance, and marketing. " +
                "Teach word usage in context.";
            case IELTS_READING ->
                "You are an IELTS Reading specialist. Teach skimming and scanning techniques. " +
                "Cover Academic and General Training Reading. Target: Band 7.0+.";
            case IELTS_LISTENING ->
                "You are an IELTS Listening specialist. Cover all 4 sections with " +
                "note-taking strategies. Practice form completion, matching, and " +
                "multiple choice. Target: Band 7.0+.";
            case IELTS_WRITING ->
                "You are an IELTS Writing specialist. Cover Task 1 (150+ words, 20 min) " +
                "and Task 2 (250+ words, 40 min). Provide band descriptor feedback. " +
                "Target: Band 7.0+.";
            case IELTS_SPEAKING ->
                "You are an IELTS Speaking mock examiner. Simulate Part 1 (introduction), " +
                "Part 2 (2 min talk), and Part 3 (discussion). " +
                "Give band descriptor feedback. Target: Band 7.0+.";
            case CONVERSATION ->
                "You are BiliBily - a friendly, lively English tutor like a cool friend. " +
                "Write naturally with emojis, casual style, short paragraphs. " +
                "IMPORTANT - You have access to system tools for user-specific data: " +
                "get_user_level (English level, target exam), " +
                "get_user_progress (completed lessons, enrolled courses), " +
                "get_user_exam_history (past exams with scores), " +
                "get_enrolled_courses (list of enrolled courses), " +
                "get_lesson_detail, get_course_info, search_lessons, " +
                "get_grammar_topic, start/complete_lesson. " +
                "When user asks about 'my courses', 'my progress', 'my level', " +
                "'enrolled courses', 'completed lessons', " +
                "'exam scores' -> answer SPECIFICALLY using this tool information! " +
                "Give exact numbers and details! " +
                "Don't be vague or generic!";
            case GRAMMAR ->
                "You are an English grammar teacher. Explain clearly with examples. " +
                "Use simple language for lower levels, more technical for advanced.";
            case VOCABULARY ->
                "You are a vocabulary expansion specialist. Teach words in context, " +
                "show usage patterns, and provide memorable examples.";
            case PRONUNCIATION ->
                "You are a pronunciation coach. Focus on sounds, stress patterns, intonation. Provide audio examples when possible.";
            case EXAM_STRATEGY ->
                "You are a test-taking strategy specialist for TOEIC/IELTS. " +
                "Teach time management, question elimination, and guessing strategies.";
            case MISTAKE_ANALYZER ->
                "You are a mistake analyzer. Categorize errors (grammar, vocabulary, pronunciation) and provide targeted practice.";
            case PROGRESS_TRACKER ->
                "You are a progress tracker. Summarize achievements, suggest next steps, and motivate the learner.";
            case PLATFORM_LEARNING, PLATFORM_COURSE_OPS, PLATFORM_EXAM_OPS ->
                "You are a platform assistant. Help with learning platform operations including courses, lessons, and progress tracking.";
        };
    }

    private LLMResponse parseResponse(Map<String, Object> response, AgentType agent, long latency, String modelUsed) {
        try {
            List choices = (List) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return buildErrorResponse(agent, latency, "No response from LLM", modelUsed);
            }
            
            Map firstChoice = (Map) choices.get(0);
            Map message = (Map) firstChoice.get("message");
            String content = (String) message.get("content");
            
            Map usage = (Map) response.get("usage");
            int inputTokens = usage != null ? (Integer) usage.get("prompt_tokens") : 0;
            int outputTokens = usage != null ? (Integer) usage.get("completion_tokens") : 0;
            int totalTokens = usage != null ? (Integer) usage.get("total_tokens") : 0;
            
            String model = (String) response.get("model");
            
            return LLMResponse.builder()
                .content(content != null ? content : "")
                .agentUsed(agent.getCode())
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .latencyMs(latency)
                .modelUsed(model != null ? model : modelUsed)
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing LLM response: {}", e.getMessage());
            return buildErrorResponse(agent, latency, e.getMessage(), modelUsed);
        }
    }

    private LLMResponse buildErrorResponse(AgentType agent, long latency, String error, String modelUsed) {
        return LLMResponse.builder()
            .content("AI Harness gặp lỗi xử lý phản hồi từ model. Vui lòng thử lại sau ít phút.")
            .agentUsed(agent.getCode())
            .latencyMs(latency)
            .modelUsed(modelUsed != null ? modelUsed : "google/gemini-2.0-flash-001")
            .build();
    }

    private String describeModelFailure(Exception e) {
        if (e instanceof RestClientResponseException httpEx) {
            int status = httpEx.getStatusCode().value();
            String body = httpEx.getResponseBodyAsString();
            String normalized = body == null ? "" : body.toLowerCase(Locale.ROOT);
            if (status == 429 || normalized.contains("rate limit")) {
                return "Rate limit từ provider (429).";
            }
            if (status == 404 && normalized.contains("deprecated")) {
                return "Model đã deprecated/không còn hỗ trợ (404).";
            }
            if (status == 404) {
                return "Model không tồn tại hoặc không truy cập được (404).";
            }
            if (status == 401 || status == 403) {
                return "API key không hợp lệ hoặc không có quyền (401/403).";
            }
            if (status >= 500) {
                return "Provider đang lỗi tạm thời (5xx).";
            }
            return "Lỗi HTTP " + status + ".";
        }
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return "Lỗi kết nối chưa xác định.";
        }
        String normalized = msg.toLowerCase(Locale.ROOT);
        if (normalized.contains("timeout") || normalized.contains("timed out")) {
            return "Timeout khi gọi provider.";
        }
        return "Lỗi kết nối: " + msg;
    }

    private String buildHarnessFailureMessage(Map<String, String> failures) {
        if (failures == null || failures.isEmpty()) {
            return "AI Harness tạm thời không khả dụng. Vui lòng thử lại sau.";
        }
        StringBuilder sb = new StringBuilder("AI Harness tạm thời lỗi khi gọi model. Chi tiết: ");
        boolean first = true;
        for (Map.Entry<String, String> entry : failures.entrySet()) {
            if (!first) {
                sb.append(" | ");
            }
            first = false;
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue());
        }
        sb.append(" Vui lòng thử lại sau hoặc đổi model trong Admin.");
        return sb.toString();
    }
}

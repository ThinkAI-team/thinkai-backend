package com.thinkai.backend.service;

import com.thinkai.backend.dto.AIChatRequest;
import com.thinkai.backend.dto.AIChatResponse;
import com.thinkai.backend.dto.AISummarizeRequest;
import com.thinkai.backend.dto.AISummarizeResponse;
import com.thinkai.backend.dto.AiAgentTraceDto;
import com.thinkai.backend.dto.AiChatConversationDetailDto;
import com.thinkai.backend.dto.AiChatConversationDto;
import com.thinkai.backend.dto.AiFeedbackRequest;
import com.thinkai.backend.dto.AiSettingsDto;
import com.thinkai.backend.dto.AiTutorUiAction;
import com.thinkai.backend.dto.CourseRequest;
import com.thinkai.backend.dto.LessonRequest;
import com.thinkai.backend.entity.AiChatLog;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.entity.enums.ExamType;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.AiChatLogRepository;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.ExamRepository;
import com.thinkai.backend.repository.LessonProgressRepository;
import com.thinkai.backend.repository.LessonRepository;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.service.aitutor.AiAgentRouteDecision;
import com.thinkai.backend.service.aitutor.AiAgentRouterService;
import com.thinkai.backend.service.aitutor.AiAgentTraceService;
import com.thinkai.backend.service.aitutor.AiAgentType;
import com.thinkai.backend.service.aitutor.AiToolPolicyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AITutorService {

    private static final int MAX_HISTORY_TURNS = 10;
    private static final int MAX_TOOL_ROUTER_TURNS = 10;
    private static final int MAX_ATTACHMENT_FILES = 3;
    private static final int MAX_ATTACHMENT_CHARS = 5000;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+");
    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();
    private static final Set<String> MUTATING_ACTIONS = Set.of(
            "enroll_course",
            "create_course",
            "create_exam",
            "publish_course",
            "create_lesson");
    private static final Set<String> CONFIRMATION_KEYWORDS = Set.of(
            "xac nhan", "xác nhận", "dong y", "đồng ý", "confirm", "yes", "ok", "oke", "thuc hien", "thực hiện");
    private static final Set<String> IMAGE_FILE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> TEXT_FILE_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "csv", "json", "xml", "yaml", "yml", "log");
    private static final Set<String> VIETNAMESE_KEYWORDS = Set.of(
            "toi", "tôi", "mình", "minh", "ban", "bạn", "khong", "không", "muon", "muốn",
            "giup", "giúp", "khoa", "khóa", "hoc", "học", "bai", "bài", "thi", "de", "đề",
            "tieng", "tiếng", "viet", "việt", "lam", "làm", "tao", "tạo");

    private final RestClient restClient;
    private final AiChatLogRepository aiChatLogRepository;
    private final UserRepository userRepository;
    private final AiSettingsService aiSettingsService;
    private final AiRuntimeSettingsService aiRuntimeSettingsService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamRepository examRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseService courseService;
    private final AiAgentRouterService aiAgentRouterService;
    private final AiToolPolicyService aiToolPolicyService;
    private final AiAgentTraceService aiAgentTraceService;
    private final ObjectMapper objectMapper;

    public AITutorService(
            RestClient.Builder restClientBuilder,
            AiChatLogRepository aiChatLogRepository,
            UserRepository userRepository,
            AiSettingsService aiSettingsService,
            AiRuntimeSettingsService aiRuntimeSettingsService,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            ExamRepository examRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            CourseService courseService,
            AiAgentRouterService aiAgentRouterService,
            AiToolPolicyService aiToolPolicyService,
            AiAgentTraceService aiAgentTraceService,
            ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.aiChatLogRepository = aiChatLogRepository;
        this.userRepository = userRepository;
        this.aiSettingsService = aiSettingsService;
        this.aiRuntimeSettingsService = aiRuntimeSettingsService;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.examRepository = examRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.courseService = courseService;
        this.aiAgentRouterService = aiAgentRouterService;
        this.aiToolPolicyService = aiToolPolicyService;
        this.aiAgentTraceService = aiAgentTraceService;
        this.objectMapper = objectMapper;
    }

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.keys:}")
    private String apiKeys;

    @Value("${openrouter.api.models:}")
    private String models;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.api.model}")
    private String model;

    @Value("${openrouter.api.fallback-model:}")
    private String fallbackModel;

    @Value("${openrouter.api.temperature:0.3}")
    private double temperature;

    @Value("${openrouter.api.top-p:0.9}")
    private double topP;

    @Value("${openrouter.api.max-tokens:700}")
    private int maxTokens;
    private final AtomicInteger apiKeyCursor = new AtomicInteger(0);

    private static final class ToolDecision {
        private final String action;
        private final JsonNode args;

        private ToolDecision(String action, JsonNode args) {
            this.action = action;
            this.args = args;
        }

        static ToolDecision none(ObjectMapper mapper) {
            try {
                return new ToolDecision("none", mapper.readTree("{}"));
            } catch (Exception ex) {
                return new ToolDecision("none", null);
            }
        }
    }

    private static final class ToolExecutionResult {
        private final String toolName;
        private final String rawOutput;
        private final boolean requiresMoreInfo;
        private final List<AiTutorUiAction> actions;
        private final AiAgentType agentType;

        private ToolExecutionResult(String toolName, String rawOutput, boolean requiresMoreInfo,
                List<AiTutorUiAction> actions, AiAgentType agentType) {
            this.toolName = toolName;
            this.rawOutput = rawOutput;
            this.requiresMoreInfo = requiresMoreInfo;
            this.actions = actions != null ? actions : List.of();
            this.agentType = agentType;
        }

        static ToolExecutionResult success(String toolName, String rawOutput) {
            return new ToolExecutionResult(toolName, rawOutput, false, List.of(), AiAgentType.LEARNING);
        }

        static ToolExecutionResult success(String toolName, String rawOutput, AiAgentType agentType) {
            return new ToolExecutionResult(toolName, rawOutput, false, List.of(), agentType);
        }

        static ToolExecutionResult needInfo(String toolName, String rawOutput, List<AiTutorUiAction> actions) {
            return new ToolExecutionResult(toolName, rawOutput, true, actions, AiAgentType.LEARNING);
        }

        static ToolExecutionResult needInfo(String toolName, String rawOutput, List<AiTutorUiAction> actions, AiAgentType agentType) {
            return new ToolExecutionResult(toolName, rawOutput, true, actions, agentType);
        }
    }

    private static final class UploadedFileRef {
        private final String url;
        private final String fileName;
        private final String extension;

        private UploadedFileRef(String url, String fileName, String extension) {
            this.url = url;
            this.fileName = fileName;
            this.extension = extension;
        }
    }

    private static final class AttachmentContext {
        private final String promptText;
        private final List<String> imageUrls;

        private AttachmentContext(String promptText, List<String> imageUrls) {
            this.promptText = promptText;
            this.imageUrls = imageUrls != null ? imageUrls : List.of();
        }
    }

    public AIChatResponse chat(AIChatRequest request, String email) {
        String rawMessage = request != null && request.getMessage() != null
                ? request.getMessage().trim()
                : "";
        if (rawMessage.isEmpty()) {
            throw new ApiException("Message cannot be empty", HttpStatus.BAD_REQUEST);
        }

        User user = null;
        AiSettingsDto settings = null;
        if (email != null) {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
            settings = aiSettingsService.getSettings(email);
        }

        String requestedConversationId = normalizeConversationId(request.getConversationId());
        String conversationId = resolveConversationIdForChat(user, requestedConversationId);
        List<AiChatLog> previousLogs = user != null
                ? aiChatLogRepository.findByUserIdAndConversationIdOrderByCreatedAtAsc(user.getId(), conversationId)
                : List.of();
        String conversationTitle = resolveConversationTitle(previousLogs, rawMessage);

        String fallbackLanguage = settings != null ? settings.getLanguage() : "English";
        String responseLength = settings != null ? settings.getResponseLength() : "detailed";
        String language = detectPreferredLanguage(rawMessage, fallbackLanguage);
        String lessonContext = request.getContext() != null && !request.getContext().isBlank()
                ? request.getContext()
                : "General English";

        long startTime = System.currentTimeMillis();
        ToolExecutionResult toolExecution = user != null
                ? tryHandlePlatformAction(user, rawMessage, previousLogs, conversationId)
                : null;

        String responseText;
        List<AiTutorUiAction> responseActions = List.of();
        AiAgentType responseAgentType = AiAgentType.TUTOR;

        if (toolExecution != null) {
            responseText = humanizeToolOutputWithModel(
                    rawMessage,
                    language,
                    responseLength,
                    toolExecution.toolName,
                    toolExecution.rawOutput,
                    toolExecution.requiresMoreInfo);
            responseActions = toolExecution.actions;
            responseAgentType = toolExecution.agentType;
        } else {
            String systemPrompt = buildTutorSystemPrompt(language, responseLength, lessonContext, user);
            List<Map<String, Object>> messages = buildChatMessages(systemPrompt, previousLogs, rawMessage);
            responseText = callAiApi(messages);
            responseAgentType = AiAgentType.TUTOR;
        }
        long responseTimeMs = System.currentTimeMillis() - startTime;

        Long messageId = null;
        if (user != null) {
            AiChatLog chatLog = AiChatLog.builder()
                    .userId(user.getId())
                    .conversationId(conversationId)
                    .conversationTitle(conversationTitle)
                    .userMessage(rawMessage)
                    .aiResponse(responseText)
                    .citations(serializeUiActions(responseActions))
                    .responseTimeMs((int) responseTimeMs)
                    .build();
            chatLog = aiChatLogRepository.save(chatLog);
            messageId = chatLog.getId();
        }

        return new AIChatResponse(responseText, conversationId, messageId, responseActions, responseAgentType);
    }

    public List<AiChatConversationDto> getChatHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        List<AiChatLog> logs = aiChatLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        Map<String, AiChatConversationDto> grouped = new LinkedHashMap<>();

        for (AiChatLog log : logs) {
            String conversationId = toConversationKey(log);
            AiChatConversationDto dto = grouped.get(conversationId);
            if (dto == null) {
                dto = AiChatConversationDto.builder()
                        .conversationId(conversationId)
                        .title(resolveTitle(log.getConversationTitle(), log.getUserMessage()))
                        .lastMessagePreview(toPreview(log.getUserMessage()))
                        .lastMessageAt(log.getCreatedAt())
                        .messageCount(0)
                        .build();
                grouped.put(conversationId, dto);
            }
            dto.setMessageCount(dto.getMessageCount() + 1);
        }

        return new ArrayList<>(grouped.values());
    }

    public AiChatConversationDetailDto getChatByConversationId(String conversationId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (isLegacyConversationId(conversationId)) {
            Long messageId = parseLegacyMessageId(conversationId);
            AiChatLog legacy = getChatMessageById(messageId, user.getId());
            return AiChatConversationDetailDto.builder()
                    .conversationId(conversationId)
                    .title(resolveTitle(legacy.getConversationTitle(), legacy.getUserMessage()))
                    .messages(List.of(legacy))
                    .build();
        }

        List<AiChatLog> logs = aiChatLogRepository
                .findByUserIdAndConversationIdOrderByCreatedAtAsc(user.getId(), conversationId);
        if (logs.isEmpty()) {
            throw new ApiException("Chat not found or access denied", HttpStatus.NOT_FOUND);
        }

        String title = logs.stream()
                .map(AiChatLog::getConversationTitle)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElseGet(() -> resolveTitle(null, logs.get(0).getUserMessage()));

        return AiChatConversationDetailDto.builder()
                .conversationId(conversationId)
                .title(title)
                .messages(logs)
                .build();
    }

    public List<AiAgentTraceDto> getConversationTraces(String conversationId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        return aiAgentTraceService.getConversationTraces(conversationId, user.getId(), isAdmin);
    }

    public AiChatConversationDetailDto renameConversation(String conversationId, String title, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty()) {
            throw new ApiException("Title cannot be empty", HttpStatus.BAD_REQUEST);
        }

        if (isLegacyConversationId(conversationId)) {
            Long messageId = parseLegacyMessageId(conversationId);
            AiChatLog legacy = getChatMessageById(messageId, user.getId());
            legacy.setConversationTitle(normalizedTitle);
            aiChatLogRepository.save(legacy);
            return getChatByConversationId(conversationId, email);
        }

        List<AiChatLog> logs = aiChatLogRepository
                .findByUserIdAndConversationIdOrderByCreatedAtAsc(user.getId(), conversationId);
        if (logs.isEmpty()) {
            throw new ApiException("Chat not found or access denied", HttpStatus.NOT_FOUND);
        }

        for (AiChatLog log : logs) {
            log.setConversationTitle(normalizedTitle);
        }
        aiChatLogRepository.saveAll(logs);

        return getChatByConversationId(conversationId, email);
    }

    public AiChatLog submitFeedback(Long id, String email, AiFeedbackRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        AiChatLog chatLog = getChatMessageById(id, user.getId());
        chatLog.setRating(request.getRating());
        return aiChatLogRepository.save(chatLog);
    }

    public void deleteConversation(String conversationId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (isLegacyConversationId(conversationId)) {
            Long messageId = parseLegacyMessageId(conversationId);
            AiChatLog legacy = getChatMessageById(messageId, user.getId());
            aiChatLogRepository.delete(legacy);
            return;
        }

        List<AiChatLog> logs = aiChatLogRepository
                .findByUserIdAndConversationIdOrderByCreatedAtDesc(user.getId(), conversationId);
        if (logs.isEmpty()) {
            throw new ApiException("Chat not found or access denied", HttpStatus.NOT_FOUND);
        }
        aiChatLogRepository.deleteAll(logs);
    }

    public AISummarizeResponse summarize(AISummarizeRequest request) {
        String systemPrompt = "You are an AI English Tutor for lesson post-processing tasks. "
                + "You can produce summaries, key bullet points, or flashcards depending on the user's explicit instruction. "
                + "STRICT FORMAT POLICY: If the user asks for JSON or a specific output schema, return exactly that format and nothing else. "
                + "Do not add markdown fences unless user asks for them. "
                + "If the lesson content is not related to English learning, politely refuse in one short sentence. "
                + "Respond in the same language as the user's input content unless user explicitly requests another language.";

        String responseText = callAiApi(systemPrompt, request.getContent());
        return new AISummarizeResponse(responseText);
    }

    public String generateExamFeedback(String examTitle, BigDecimal score,
            int correctCount, int totalQuestions, boolean isPassed, String wrongAnswersSummary) {
        String systemPrompt = "Bạn là AI Gia sư tiếng Anh cho học viên TOEIC/IELTS. "
                + "Hãy đưa ra phản hồi ngắn gọn, khích lệ và mang tính xây dựng bằng tiếng Việt.";
        String userMessage = "Thông tin kết quả:\n"
                + "- Bài thi: " + examTitle + "\n"
                + "- Điểm: " + score + "%\n"
                + "- Số câu đúng: " + correctCount + "/" + totalQuestions + "\n"
                + "- Kết quả: " + (isPassed ? "Đạt" : "Chưa đạt") + "\n\n"
                + (wrongAnswersSummary != null && !wrongAnswersSummary.isBlank()
                        ? "Các câu sai:\n" + wrongAnswersSummary + "\n\n"
                        : "")
                + "Yêu cầu: 1) Đánh giá tổng quan, 2) Điểm mạnh, "
                + "3) Cần cải thiện, 4) Gợi ý bước tiếp theo. Giới hạn dưới 200 từ.";
        return callAiApi(systemPrompt, userMessage);
    }

    private String buildTutorSystemPrompt(String language, String responseLength, String context, User user) {
        String platformContext = user != null ? buildPlatformContext(user) : "User is not authenticated.";
        return "You are an AI English Tutor for TOEIC/IELTS students.\n"
                + "Scope: only answer English-learning related questions (grammar, vocabulary, pronunciation, TOEIC, IELTS).\n"
                + "If a question is outside this scope, politely refuse and redirect to English learning.\n"
                + "LANGUAGE POLICY (STRICT): Always reply in the same language as the latest user message. "
                + "If conflict happens with profile setting, prioritize latest user message language.\n"
                + "Output quality: ensure response is " + responseLength + ".\n"
                + "Detected reply language: " + language + ".\n"
                + "Lesson context: " + context + ".\n"
                + "Platform context (trusted DB data):\n"
                + platformContext + "\n"
                + "For platform-management requests:\n"
                + "- Users can ask in natural language; do not force command syntax.\n"
                + "- Respect role policy from platform context. STUDENT must be denied create/update operations.\n"
                + "- For any data-changing action (enroll/create), ask for explicit confirmation before executing.\n"
                + "- If information is missing for a platform action, ask concise follow-up questions.\n"
                + "- Keep answers concise when returning tool results.\n";
    }

    private List<Map<String, Object>> buildChatMessages(
            String systemPrompt,
            List<AiChatLog> previousLogs,
            String currentUserMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        int startIndex = Math.max(0, previousLogs.size() - MAX_HISTORY_TURNS);
        for (int i = startIndex; i < previousLogs.size(); i++) {
            AiChatLog log = previousLogs.get(i);
            messages.add(Map.of("role", "user", "content", log.getUserMessage()));
            messages.add(Map.of("role", "assistant", "content", log.getAiResponse()));
        }

        AttachmentContext attachmentContext = buildAttachmentContext(currentUserMessage);
        messages.add(Map.of("role", "user", "content", toUserMessageContent(attachmentContext)));
        return messages;
    }

    private Object toUserMessageContent(AttachmentContext attachmentContext) {
        if (attachmentContext.imageUrls.isEmpty()) {
            return attachmentContext.promptText;
        }

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("type", "text", "text", attachmentContext.promptText));
        for (String imageUrl : attachmentContext.imageUrls) {
            parts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", imageUrl)));
        }
        return parts;
    }

    private AttachmentContext buildAttachmentContext(String message) {
        if (message == null || message.isBlank()) {
            return new AttachmentContext("", List.of());
        }

        List<UploadedFileRef> refs = extractUploadedFileRefs(message);
        if (refs.isEmpty()) {
            return new AttachmentContext(message, List.of());
        }

        StringBuilder contextBlock = new StringBuilder();
        List<String> imageUrls = new ArrayList<>();
        int processed = 0;

        for (UploadedFileRef ref : refs) {
            if (processed >= MAX_ATTACHMENT_FILES) {
                break;
            }
            processed++;

            contextBlock.append("\n[Attached file ").append(processed).append("]\n")
                    .append("url: ").append(ref.url).append('\n')
                    .append("name: ").append(ref.fileName).append('\n')
                    .append("ext: ").append(ref.extension).append('\n');

            if (IMAGE_FILE_EXTENSIONS.contains(ref.extension)) {
                imageUrls.add(ref.url);
                contextBlock.append("note: image attached; analyze the visual content if supported.\n");
                continue;
            }

            if (TEXT_FILE_EXTENSIONS.contains(ref.extension)) {
                String snippet = readTextSnippet(ref.fileName, MAX_ATTACHMENT_CHARS);
                if (snippet != null && !snippet.isBlank()) {
                    contextBlock.append("extracted_text:\n")
                            .append(snippet)
                            .append('\n');
                } else {
                    contextBlock.append("note: unable to extract text content from this file.\n");
                }
                continue;
            }

            contextBlock.append("note: file type is currently not auto-extracted. Use the URL if needed.\n");
        }

        if (contextBlock.isEmpty()) {
            return new AttachmentContext(message, imageUrls);
        }

        String enriched = message + "\n\nAttachment context (trusted extraction):" + contextBlock;
        return new AttachmentContext(enriched, imageUrls);
    }

    private List<UploadedFileRef> extractUploadedFileRefs(String message) {
        Matcher matcher = URL_PATTERN.matcher(message);
        Set<String> seen = new LinkedHashSet<>();
        List<UploadedFileRef> refs = new ArrayList<>();

        while (matcher.find()) {
            String raw = normalizeUrlCandidate(matcher.group());
            if (raw == null || raw.isBlank() || !seen.add(raw)) {
                continue;
            }
            try {
                URI uri = URI.create(raw);
                String path = uri.getPath();
                if (path == null || !path.contains("/api/files/")) {
                    continue;
                }
                String fileName = Paths.get(path).getFileName().toString();
                String extension = fileExtension(fileName);
                refs.add(new UploadedFileRef(raw, fileName, extension));
            } catch (Exception ignored) {
                // Ignore malformed URL and continue.
            }
        }
        return refs;
    }

    private String normalizeUrlCandidate(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim();
        while (cleaned.endsWith(".")
                || cleaned.endsWith(",")
                || cleaned.endsWith(";")
                || cleaned.endsWith(")")
                || cleaned.endsWith("]")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private String fileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot >= fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String readTextSnippet(String fileName, int maxChars) {
        try {
            String safeName = Paths.get(fileName).getFileName().toString();
            Path target = UPLOAD_DIR.resolve(safeName).normalize();
            if (!target.startsWith(UPLOAD_DIR) || !Files.exists(target) || !Files.isRegularFile(target)) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
                char[] buffer = new char[1024];
                int total = 0;
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    int remaining = maxChars - total;
                    if (remaining <= 0) {
                        break;
                    }
                    int toAppend = Math.min(read, remaining);
                    sb.append(buffer, 0, toAppend);
                    total += toAppend;
                    if (total >= maxChars) {
                        break;
                    }
                }
            }

            String text = sb.toString().trim();
            if (text.isBlank()) {
                return null;
            }
            if (text.length() >= maxChars) {
                return text + "\n...[truncated]";
            }
            return text;
        } catch (Exception ignored) {
            return null;
        }
    }

    private ToolExecutionResult tryHandlePlatformAction(
            User user,
            String message,
            List<AiChatLog> previousLogs,
            String conversationId) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/tool")) {
            return handleToolCommand(user, message);
        }

        AiAgentRouteDecision routeDecision = aiAgentRouterService.route(
                user,
                message,
                previousLogs,
                MAX_TOOL_ROUTER_TURNS,
                this::callAiApi);
        if (routeDecision.isNone()) {
            return null;
        }

        aiAgentTraceService.traceRoute(user.getId(), conversationId, message, routeDecision);
        ToolDecision decision = new ToolDecision(routeDecision.action(), routeDecision.args());
        ToolExecutionResult result = executeToolDecision(user, message, decision, false);
        if (result != null) {
            aiAgentTraceService.traceExecution(
                    user.getId(),
                    conversationId,
                    routeDecision.agentType(),
                    routeDecision.action(),
                    result.requiresMoreInfo,
                    result.rawOutput);
        }
        return result;
    }

    private ToolExecutionResult handleToolCommand(User user, String message) {
        String body = message.substring("/tool".length()).trim();
        String normalized = body.toLowerCase(Locale.ROOT);

        if (body.isEmpty() || normalized.equals("help")) {
            return ToolExecutionResult.success("help", toolHelpMessage());
        }

        if (normalized.startsWith("shop-courses")) {
            int limit = extractTrailingInt(body, 8, 1, 20);
            return ToolExecutionResult.success("list_shop_courses", toolListShopCourses(limit));
        }

        if (normalized.startsWith("search-courses")) {
            String keyword = extractSearchKeyword(body);
            if (keyword == null || keyword.isBlank()) {
                return ToolExecutionResult.needInfo(
                        "search_shop_courses",
                        "Bạn muốn tìm khóa học theo từ khóa nào?",
                        List.of());
            }
            int limit = extractTrailingInt(body, 8, 1, 20);
            return ToolExecutionResult.success("search_shop_courses", toolSearchShopCourses(keyword, limit));
        }

        if (normalized.startsWith("course-detail")) {
            Long courseId = extractFirstLong(body);
            if (courseId == null) {
                return ToolExecutionResult.needInfo(
                        "get_course_detail",
                        "Mẫu đúng: /tool course-detail 123",
                        List.of());
            }
            return ToolExecutionResult.success("get_course_detail", toolGetCourseDetail(user, courseId));
        }

        if (normalized.startsWith("course-lessons")) {
            Long courseId = extractFirstLong(body);
            if (courseId == null) {
                return ToolExecutionResult.needInfo(
                        "list_course_lessons",
                        "Mẫu đúng: /tool course-lessons 123",
                        List.of());
            }
            return ToolExecutionResult.success("list_course_lessons", toolListCourseLessons(user, courseId));
        }

        if (normalized.startsWith("my-courses")) {
            int limit = extractTrailingInt(body, 8, 1, 20);
            return ToolExecutionResult.success("list_my_courses", toolListMyCourses(user, limit));
        }

        if (normalized.startsWith("my-progress")) {
            int limit = extractTrailingInt(body, 8, 1, 20);
            return ToolExecutionResult.success("list_my_learning_progress", toolListMyLearningProgress(user, limit));
        }

        if (normalized.startsWith("my-exams")) {
            int limit = extractTrailingInt(body, 12, 1, 30);
            return ToolExecutionResult.success("list_my_exams", toolListMyExams(user, limit));
        }

        if (normalized.startsWith("enroll")) {
            String payload = extractJsonPayload(body);
            Long courseId = null;
            if (payload != null) {
                try {
                    JsonNode json = objectMapper.readTree(payload);
                    courseId = longOf(json, "courseId");
                } catch (Exception ignored) {
                    return ToolExecutionResult.needInfo(
                            "enroll_course",
                            "JSON enroll không hợp lệ. Mẫu: /tool enroll {\"courseId\":123}",
                            List.of());
                }
            }
            if (courseId == null) {
                courseId = extractFirstLong(body);
            }
            if (courseId == null) {
                return ToolExecutionResult.needInfo(
                        "enroll_course",
                        "Bạn muốn đăng ký khóa học nào? Gửi `courseId` (ví dụ: 123).",
                        List.of());
            }
            return ToolExecutionResult.success("enroll_course", toolEnrollCourse(user, courseId));
        }

        if (normalized.startsWith("create-course")) {
            if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
                return ToolExecutionResult.needInfo(
                        "create_course",
                        "Bạn cần quyền TEACHER hoặc ADMIN để tạo khóa học.",
                        List.of());
            }
            String payload = extractJsonPayload(body);
            if (payload == null) {
                return ToolExecutionResult.needInfo(
                        "create_course",
                        "Mẫu đúng: /tool create-course {\"title\":\"...\",\"description\":\"...\",\"price\":199000,\"thumbnailUrl\":\"...\"}",
                        List.of(uploadImageAction("thumbnailUrl")));
            }
            try {
                return executeCreateCourse(user, objectMapper.readTree(payload), message, false);
            } catch (Exception e) {
                return ToolExecutionResult.needInfo(
                        "create_course",
                        "JSON create-course không hợp lệ.",
                        List.of(uploadImageAction("thumbnailUrl")));
            }
        }

        if (normalized.startsWith("create-exam")) {
            if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
                return ToolExecutionResult.needInfo(
                        "create_exam",
                        "Bạn cần quyền TEACHER hoặc ADMIN để tạo bài thi.",
                        List.of());
            }
            String payload = extractJsonPayload(body);
            if (payload == null) {
                return ToolExecutionResult.needInfo(
                        "create_exam",
                        "Mẫu đúng: /tool create-exam {\"courseId\":1,\"title\":\"...\",\"examType\":\"TOEIC\",\"description\":\"...\",\"timeLimitMinutes\":90,\"passingScore\":60,\"isRandomOrder\":false}",
                        List.of());
            }
            try {
                return executeCreateExam(user, objectMapper.readTree(payload), false);
            } catch (Exception e) {
                return ToolExecutionResult.needInfo("create_exam", "JSON create-exam không hợp lệ.", List.of());
            }
        }

        if (normalized.startsWith("publish-course")) {
            if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
                return ToolExecutionResult.needInfo(
                        "publish_course",
                        "Bạn cần quyền TEACHER hoặc ADMIN để publish khóa học.",
                        List.of());
            }
            Long courseId = extractFirstLong(body);
            if (courseId == null) {
                return ToolExecutionResult.needInfo(
                        "publish_course",
                        "Mẫu đúng: /tool publish-course {\"courseId\":123}",
                        List.of());
            }
            try {
                JsonNode payload = objectMapper.readTree("{\"courseId\":" + courseId + "}");
                return executePublishCourse(user, payload, false);
            } catch (Exception e) {
                return ToolExecutionResult.needInfo("publish_course", "Dữ liệu publish-course không hợp lệ.", List.of());
            }
        }

        if (normalized.startsWith("create-lesson")) {
            if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
                return ToolExecutionResult.needInfo(
                        "create_lesson",
                        "Bạn cần quyền TEACHER hoặc ADMIN để tạo bài học.",
                        List.of());
            }
            String payload = extractJsonPayload(body);
            if (payload == null) {
                return ToolExecutionResult.needInfo(
                        "create_lesson",
                        "Mẫu đúng: /tool create-lesson {\"courseId\":1,\"title\":\"...\",\"type\":\"VIDEO\",\"contentUrl\":\"...\",\"contentText\":\"...\",\"durationSeconds\":300,\"orderIndex\":1}",
                        List.of(uploadFileAction("contentUrl", "Chọn file bài học", "image/*,.pdf,.mp4,.mp3,.txt,.md")));
            }
            try {
                return executeCreateLesson(user, objectMapper.readTree(payload), false);
            } catch (Exception e) {
                return ToolExecutionResult.needInfo(
                        "create_lesson",
                        "JSON create-lesson không hợp lệ.",
                        List.of(uploadFileAction("contentUrl", "Chọn file bài học", "image/*,.pdf,.mp4,.mp3,.txt,.md")));
            }
        }

        return ToolExecutionResult.needInfo(
                "help",
                "Lệnh tool không hợp lệ. Dùng `/tool help` để xem danh sách lệnh.",
                List.of());
    }

    private ToolExecutionResult executeToolDecision(
            User user,
            String message,
            ToolDecision decision,
            boolean allowMutatingWithoutConfirmation) {
        String denyReason = aiToolPolicyService.denyReason(user, decision.action);
        if (denyReason != null) {
            return ToolExecutionResult.needInfo(decision.action, denyReason, List.of());
        }

        if (!allowMutatingWithoutConfirmation
                && MUTATING_ACTIONS.contains(decision.action)
                && !hasExecutionConfirmation(message)) {
            return buildConfirmationRequest(user, message, decision);
        }

        switch (decision.action) {
            case "list_shop_courses":
                return ToolExecutionResult.success("list_shop_courses", toolListShopCourses(8));
            case "search_shop_courses": {
                String keyword = textOf(decision.args, "keyword");
                if (keyword == null || keyword.isBlank()) {
                    keyword = extractSearchKeyword(message);
                }
                if (keyword == null || keyword.isBlank()) {
                    return ToolExecutionResult.needInfo(
                            "search_shop_courses",
                            "Bạn muốn tìm khóa học theo từ khóa nào?",
                            List.of());
                }
                return ToolExecutionResult.success("search_shop_courses", toolSearchShopCourses(keyword, 8));
            }
            case "get_course_detail": {
                Long courseId = longOf(decision.args, "courseId");
                if (courseId == null) {
                    courseId = extractFirstLong(message);
                }
                if (courseId == null) {
                    return ToolExecutionResult.needInfo(
                            "get_course_detail",
                            "Bạn muốn xem chi tiết khóa học nào? Vui lòng gửi `courseId`.",
                            List.of());
                }
                return ToolExecutionResult.success("get_course_detail", toolGetCourseDetail(user, courseId));
            }
            case "list_course_lessons": {
                Long courseId = longOf(decision.args, "courseId");
                if (courseId == null) {
                    courseId = extractFirstLong(message);
                }
                if (courseId == null) {
                    return ToolExecutionResult.needInfo(
                            "list_course_lessons",
                            "Bạn muốn xem danh sách bài học của khóa nào? Vui lòng gửi `courseId`.",
                            List.of());
                }
                return ToolExecutionResult.success("list_course_lessons", toolListCourseLessons(user, courseId));
            }
            case "list_my_courses":
                return ToolExecutionResult.success("list_my_courses", toolListMyCourses(user, 8));
            case "list_my_learning_progress":
                return ToolExecutionResult.success("list_my_learning_progress", toolListMyLearningProgress(user, 8));
            case "list_my_exams":
                return ToolExecutionResult.success("list_my_exams", toolListMyExams(user, 12));
            case "enroll_course": {
                Long courseId = longOf(decision.args, "courseId");
                if (courseId == null) {
                    courseId = extractFirstLong(message);
                }
                if (courseId == null) {
                    return ToolExecutionResult.needInfo(
                            "enroll_course",
                            "Bạn muốn đăng ký course nào? Vui lòng gửi `courseId`.",
                            List.of());
                }
                return ToolExecutionResult.success("enroll_course", toolEnrollCourse(user, courseId));
            }
            case "create_course":
                return executeCreateCourse(user, decision.args, message, false);
            case "create_exam":
                return executeCreateExam(user, decision.args, false);
            case "publish_course":
                return executePublishCourse(user, decision.args, false);
            case "create_lesson":
                return executeCreateLesson(user, decision.args, false);
            default:
                return null;
        }
    }

    private ToolExecutionResult buildConfirmationRequest(User user, String message, ToolDecision decision) {
        switch (decision.action) {
            case "enroll_course": {
                Long courseId = longOf(decision.args, "courseId");
                if (courseId == null) {
                    courseId = extractFirstLong(message);
                }
                if (courseId == null) {
                    return ToolExecutionResult.needInfo(
                            "enroll_course",
                            "Bạn muốn đăng ký course nào? Vui lòng gửi `courseId`.",
                            List.of());
                }
                return ToolExecutionResult.needInfo(
                        "enroll_course",
                        "Mình đã hiểu bạn muốn đăng ký courseId=" + courseId
                                + ". Nếu đúng, hãy trả lời: `Xác nhận đăng ký course " + courseId + "`.",
                        List.of());
            }
            case "create_course":
                return executeCreateCourse(user, decision.args, message, true);
            case "create_exam":
                return executeCreateExam(user, decision.args, true);
            case "publish_course":
                return executePublishCourse(user, decision.args, true);
            case "create_lesson":
                return executeCreateLesson(user, decision.args, true);
            default:
                return null;
        }
    }

    private boolean hasExecutionConfirmation(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        for (String keyword : CONFIRMATION_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String toolHelpMessage() {
        return "Bạn có thể hỏi bằng ngôn ngữ tự nhiên hoặc dùng lệnh:\n"
                + "- /tool shop-courses [limit]\n"
                + "- /tool search-courses <keyword> [limit]\n"
                + "- /tool course-detail <courseId>\n"
                + "- /tool course-lessons <courseId>\n"
                + "- /tool my-courses [limit]\n"
                + "- /tool my-progress [limit]\n"
                + "- /tool my-exams [limit]\n"
                + "- /tool enroll {\"courseId\":123}\n"
                + "- /tool create-course {\"title\":\"...\",\"description\":\"...\",\"price\":199000,\"thumbnailUrl\":\"...\"}\n"
                + "- /tool create-exam {\"courseId\":1,\"title\":\"...\",\"examType\":\"TOEIC\",\"description\":\"...\",\"timeLimitMinutes\":90,\"passingScore\":60,\"isRandomOrder\":false}\n"
                + "- /tool publish-course {\"courseId\":123}\n"
                + "- /tool create-lesson {\"courseId\":1,\"title\":\"...\",\"type\":\"VIDEO\",\"contentUrl\":\"...\",\"contentText\":\"...\",\"durationSeconds\":300,\"orderIndex\":1}";
    }

    private String toolListShopCourses(int limit) {
        Page<Course> page = courseRepository.findPublishedCourses(
                null, null, null,
                PageRequest.of(0, Math.max(1, Math.min(limit, 20)), Sort.by(Sort.Direction.DESC, "createdAt")));
        if (page.isEmpty()) {
            return "Shop hiện chưa có khóa học nào đang publish.";
        }
        StringBuilder sb = new StringBuilder("Khóa học trên shop:\n");
        int index = 1;
        for (Course course : page.getContent()) {
            sb.append(index++).append(". [courseId=").append(course.getId()).append("] ")
                    .append(course.getTitle())
                    .append(" | price=").append(formatPrice(course.getPrice()))
                    .append('\n');
        }
        return sb.toString().trim();
    }

    private String toolSearchShopCourses(String keyword, int limit) {
        Page<Course> page = courseRepository.findPublishedCourses(
                keyword, null, null,
                PageRequest.of(0, Math.max(1, Math.min(limit, 20)), Sort.by(Sort.Direction.DESC, "createdAt")));
        if (page.isEmpty()) {
            return "Không tìm thấy khóa học phù hợp với từ khóa: \"" + keyword + "\".";
        }
        StringBuilder sb = new StringBuilder("Kết quả tìm kiếm khóa học:\n");
        int index = 1;
        for (Course course : page.getContent()) {
            sb.append(index++).append(". [courseId=").append(course.getId()).append("] ")
                    .append(course.getTitle())
                    .append(" | price=").append(formatPrice(course.getPrice()))
                    .append('\n');
        }
        return sb.toString().trim();
    }

    private String toolGetCourseDetail(User user, Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return "Không tìm thấy khóa học với courseId=" + courseId + ".";
        }

        boolean isOwner = course.getInstructorId() != null && course.getInstructorId().equals(user.getId());
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        boolean isEnrolled = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId).isPresent();
        if (!Boolean.TRUE.equals(course.getIsPublished()) && !isOwner && !isAdmin && !isEnrolled) {
            return "Bạn không có quyền xem chi tiết khóa học chưa publish này.";
        }

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        return "Chi tiết khóa học:\n"
                + "- courseId: " + course.getId() + "\n"
                + "- title: " + course.getTitle() + "\n"
                + "- description: " + (course.getDescription() == null ? "" : course.getDescription()) + "\n"
                + "- price: " + formatPrice(course.getPrice()) + "\n"
                + "- published: " + Boolean.TRUE.equals(course.getIsPublished()) + "\n"
                + "- status: " + course.getStatus() + "\n"
                + "- lessonsCount: " + lessons.size();
    }

    private String toolListCourseLessons(User user, Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return "Không tìm thấy khóa học với courseId=" + courseId + ".";
        }
        boolean isOwner = course.getInstructorId() != null && course.getInstructorId().equals(user.getId());
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        boolean isEnrolled = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId).isPresent();
        if (!Boolean.TRUE.equals(course.getIsPublished()) && !isOwner && !isAdmin && !isEnrolled) {
            return "Bạn không có quyền xem bài học của khóa học này.";
        }

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        if (lessons.isEmpty()) {
            return "Khóa học này chưa có bài học nào.";
        }
        StringBuilder sb = new StringBuilder("Danh sách bài học:\n");
        int index = 1;
        for (Lesson lesson : lessons) {
            sb.append(index++).append(". [lessonId=").append(lesson.getId()).append("] ")
                    .append(lesson.getTitle())
                    .append(" | type=").append(lesson.getType())
                    .append(" | order=").append(lesson.getOrderIndex())
                    .append('\n');
        }
        return sb.toString().trim();
    }

    private String toolListMyLearningProgress(User user, int limit) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(user.getId());
        if (enrollments.isEmpty()) {
            return "Bạn chưa có khóa học đăng ký.";
        }

        StringBuilder sb = new StringBuilder("Tiến độ học tập của bạn:\n");
        int max = Math.max(1, Math.min(limit, 20));
        int count = 0;
        for (Enrollment enrollment : enrollments) {
            if (count >= max) {
                break;
            }
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course == null) {
                continue;
            }
            long totalLessons = lessonRepository.countByCourseId(course.getId());
            long completedLessons = lessonProgressRepository.countCompletedByUserAndCourse(user.getId(), course.getId());
            sb.append(count + 1).append(". [courseId=").append(course.getId()).append("] ")
                    .append(course.getTitle())
                    .append(" | progress=").append(enrollment.getProgressPercent()).append("%")
                    .append(" | completedLessons=").append(completedLessons).append("/").append(totalLessons)
                    .append('\n');
            count++;
        }
        return sb.toString().trim();
    }

    private String toolListMyCourses(User user, int limit) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(user.getId());
        if (enrollments.isEmpty()) {
            return "Bạn chưa đăng ký khóa học nào.";
        }

        StringBuilder sb = new StringBuilder("Khóa học của bạn:\n");
        int count = 0;
        for (Enrollment enrollment : enrollments) {
            if (count >= Math.max(1, Math.min(limit, 20))) {
                break;
            }
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course == null) {
                continue;
            }
            sb.append(count + 1).append(". [courseId=").append(course.getId()).append("] ")
                    .append(course.getTitle())
                    .append(" | progress=").append(enrollment.getProgressPercent()).append("%\n");
            count++;
        }

        if (count == 0) {
            return "Bạn chưa đăng ký khóa học nào.";
        }
        return sb.toString().trim();
    }

    private String toolListMyExams(User user, int limit) {
        int max = Math.max(1, Math.min(limit, 30));

        if (user.getRole() == User.Role.TEACHER || user.getRole() == User.Role.ADMIN) {
            Page<Exam> myExams = examRepository.findByCreatedBy(
                    user.getId(),
                    PageRequest.of(0, max, Sort.by(Sort.Direction.DESC, "createdAt")));
            if (myExams.isEmpty()) {
                return "Bạn chưa tạo bài thi nào.";
            }
            StringBuilder teacherSb = new StringBuilder("Bài thi bạn đã tạo:\n");
            int index = 1;
            for (Exam exam : myExams.getContent()) {
                teacherSb.append(index++).append(". [examId=").append(exam.getId()).append("] ")
                        .append(exam.getTitle())
                        .append(" | type=").append(exam.getExamType())
                        .append(" | courseId=").append(exam.getCourseId()).append('\n');
            }
            return teacherSb.toString().trim();
        }

        List<Enrollment> enrollments = enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(user.getId());
        if (enrollments.isEmpty()) {
            return "Bạn chưa có khóa học đăng ký, nên chưa có danh sách bài thi liên quan.";
        }

        StringBuilder sb = new StringBuilder("Bài thi khả dụng cho bạn:\n");
        int count = 0;

        for (Enrollment enrollment : enrollments) {
            if (count >= max) {
                break;
            }
            List<Exam> exams = examRepository.findByCourseId(enrollment.getCourseId());
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            String courseLabel = course != null ? course.getTitle() : ("Course " + enrollment.getCourseId());
            for (Exam exam : exams) {
                if (count >= max) {
                    break;
                }
                sb.append(count + 1).append(". [examId=").append(exam.getId()).append("] ")
                        .append(exam.getTitle())
                        .append(" | type=").append(exam.getExamType())
                        .append(" | course=").append(courseLabel).append('\n');
                count++;
            }
        }

        if (count == 0) {
            return "Hiện chưa có bài thi nào cho các khóa học bạn đã đăng ký.";
        }
        return sb.toString().trim();
    }

    private String toolEnrollCourse(User user, Long courseId) {
        if (user.getRole() != User.Role.STUDENT) {
            return "Chỉ STUDENT mới có thể enroll khóa học.";
        }
        try {
            courseService.enrollCourse(courseId, user.getId());
            return "Đăng ký khóa học thành công. courseId=" + courseId;
        } catch (Exception e) {
            return "Không thể đăng ký khóa học: " + e.getMessage();
        }
    }

    private ToolExecutionResult executeCreateCourse(User user, JsonNode payload, String rawMessage, boolean dryRun) {
        if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
            return ToolExecutionResult.needInfo(
                    "create_course",
                    "Bạn cần quyền TEACHER hoặc ADMIN để tạo khóa học.",
                    List.of());
        }

        String title = textOf(payload, "title");
        if (title == null || title.isBlank()) {
            return ToolExecutionResult.needInfo(
                    "create_course",
                    "Thiếu `title`. Bạn muốn đặt tên khóa học là gì?",
                    List.of());
        }

        BigDecimal price = decimalOf(payload, "price");
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            return ToolExecutionResult.needInfo(
                    "create_course",
                    "Thiếu hoặc sai `price`. Vui lòng gửi giá khóa học (ví dụ: 199000).",
                    List.of());
        }

        String thumbnailUrl = textOf(payload, "thumbnailUrl");
        boolean userAskedForImage = rawMessage != null && rawMessage.toLowerCase(Locale.ROOT).matches(
                ".*(thumbnail|hình|ảnh|image|banner).*");
        if ((thumbnailUrl == null || thumbnailUrl.isBlank()) && userAskedForImage) {
            return ToolExecutionResult.needInfo(
                    "create_course",
                    "Bạn chưa cung cấp `thumbnailUrl`. Bạn có thể upload ảnh bằng nút bên dưới rồi gửi lại URL.",
                    List.of(uploadImageAction("thumbnailUrl")));
        }

        CourseRequest req = CourseRequest.builder()
                .title(title.trim())
                .description(textOf(payload, "description"))
                .thumbnailUrl(thumbnailUrl)
                .price(price.setScale(2, RoundingMode.HALF_UP))
                .build();

        if (dryRun) {
            return ToolExecutionResult.needInfo(
                    "create_course",
                    "Mình đã đủ thông tin để tạo khóa học \"" + req.getTitle()
                            + "\" (price=" + formatPrice(req.getPrice())
                            + "). Nếu đúng, hãy trả lời: `Xác nhận tạo khóa học`.",
                    thumbnailUrl == null || thumbnailUrl.isBlank()
                            ? List.of(uploadImageAction("thumbnailUrl"))
                            : List.of());
        }

        try {
            Course created = courseService.createCourse(user.getId(), req);
            return ToolExecutionResult.success("create_course", "Đã tạo khóa học thành công. ID: " + created.getId()
                    + ", tiêu đề: \"" + created.getTitle()
                    + "\". Trạng thái hiện tại: DRAFT.");
        } catch (Exception e) {
            return ToolExecutionResult.success("create_course", "Không thể tạo khóa học: " + e.getMessage());
        }
    }

    private ToolExecutionResult executeCreateExam(User user, JsonNode payload, boolean dryRun) {
        if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
            return ToolExecutionResult.needInfo(
                    "create_exam",
                    "Bạn cần quyền TEACHER hoặc ADMIN để tạo bài thi.",
                    List.of());
        }

        Long courseId = longOf(payload, "courseId");
        if (courseId == null) {
            return ToolExecutionResult.needInfo(
                    "create_exam",
                    "Thiếu `courseId`. Bạn muốn tạo bài thi cho khóa học nào?",
                    List.of());
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ToolExecutionResult.needInfo(
                    "create_exam",
                    "Không tìm thấy khóa học với ID: " + courseId + ". Vui lòng kiểm tra lại.",
                    List.of());
        }
        if (user.getRole() == User.Role.TEACHER
                && (course.getInstructorId() == null || !course.getInstructorId().equals(user.getId()))) {
            return ToolExecutionResult.needInfo(
                    "create_exam",
                    "Bạn chỉ có thể tạo bài thi cho khóa học do bạn sở hữu.",
                    List.of());
        }

        String title = textOf(payload, "title");
        if (title == null || title.isBlank()) {
            return ToolExecutionResult.needInfo(
                    "create_exam",
                    "Thiếu `title`. Bạn muốn đặt tên bài thi là gì?",
                    List.of());
        }

        ExamType examType;
        try {
            examType = ExamType.valueOf(textOf(payload, "examType").toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return ToolExecutionResult.needInfo(
                    "create_exam",
                    "Trường `examType` không hợp lệ. Chỉ chấp nhận TOEIC hoặc IELTS.",
                    List.of());
        }

        Integer timeLimit = intOf(payload, "timeLimitMinutes");
        if (timeLimit == null || timeLimit <= 0) {
            timeLimit = 120;
        }

        Integer passingScore = intOf(payload, "passingScore");
        if (passingScore == null || passingScore < 0 || passingScore > 100) {
            passingScore = 60;
        }

        Boolean randomOrder = boolOf(payload, "isRandomOrder");
        if (randomOrder == null) {
            randomOrder = false;
        }

        String partConfigJson = null;
        if (payload.has("partConfig") && payload.get("partConfig").isObject()) {
            try {
                partConfigJson = objectMapper.writeValueAsString(payload.get("partConfig"));
            } catch (Exception ignored) {
                partConfigJson = null;
            }
        }

        Exam exam = Exam.builder()
                .courseId(courseId)
                .title(title.trim())
                .examType(examType)
                .description(textOf(payload, "description"))
                .timeLimitMinutes(timeLimit)
                .passingScore(passingScore)
                .isRandomOrder(randomOrder)
                .partConfig(partConfigJson)
                .createdBy(user.getId())
                .build();

        if (dryRun) {
            return ToolExecutionResult.needInfo(
                    "create_exam",
                    "Mình đã đủ thông tin để tạo bài thi \"" + exam.getTitle()
                            + "\" cho courseId=" + exam.getCourseId()
                            + ". Nếu đúng, hãy trả lời: `Xác nhận tạo bài thi`.",
                    List.of());
        }

        try {
            exam = examRepository.save(exam);
            return ToolExecutionResult.success("create_exam", "Đã tạo bài thi thành công. ID: " + exam.getId()
                    + ", tiêu đề: \"" + exam.getTitle()
                    + "\", loại: " + exam.getExamType() + ".");
        } catch (Exception e) {
            return ToolExecutionResult.success("create_exam", "Không thể tạo bài thi: " + e.getMessage());
        }
    }

    private ToolExecutionResult executePublishCourse(User user, JsonNode payload, boolean dryRun) {
        if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
            return ToolExecutionResult.needInfo(
                    "publish_course",
                    "Bạn cần quyền TEACHER hoặc ADMIN để publish khóa học.",
                    List.of());
        }

        Long courseId = longOf(payload, "courseId");
        if (courseId == null) {
            return ToolExecutionResult.needInfo(
                    "publish_course",
                    "Thiếu `courseId`. Bạn muốn publish khóa học nào?",
                    List.of());
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ToolExecutionResult.needInfo(
                    "publish_course",
                    "Không tìm thấy khóa học với ID: " + courseId + ".",
                    List.of());
        }

        if (user.getRole() == User.Role.TEACHER
                && (course.getInstructorId() == null || !course.getInstructorId().equals(user.getId()))) {
            return ToolExecutionResult.needInfo(
                    "publish_course",
                    "Bạn chỉ có thể publish khóa học do bạn sở hữu.",
                    List.of());
        }

        if (Boolean.TRUE.equals(course.getIsPublished())) {
            return ToolExecutionResult.success("publish_course", "Khóa học này đã được publish trước đó.");
        }

        if (dryRun) {
            return ToolExecutionResult.needInfo(
                    "publish_course",
                    "Mình đã sẵn sàng publish khóa học \"" + course.getTitle()
                            + "\" (courseId=" + course.getId()
                            + "). Nếu đúng, hãy trả lời: `Xác nhận publish khóa học`.",
                    List.of());
        }

        if (user.getRole() == User.Role.ADMIN) {
            course.setIsPublished(true);
            course.setStatus(Course.Status.APPROVED);
            courseRepository.save(course);
            return ToolExecutionResult.success("publish_course",
                    "Đã publish khóa học thành công. courseId=" + course.getId());
        }

        Course published = courseService.publishCourse(courseId, user.getId());
        return ToolExecutionResult.success("publish_course",
                "Đã publish khóa học thành công. courseId=" + published.getId());
    }

    private ToolExecutionResult executeCreateLesson(User user, JsonNode payload, boolean dryRun) {
        if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
            return ToolExecutionResult.needInfo(
                    "create_lesson",
                    "Bạn cần quyền TEACHER hoặc ADMIN để tạo bài học.",
                    List.of());
        }

        Long courseId = longOf(payload, "courseId");
        if (courseId == null) {
            return ToolExecutionResult.needInfo(
                    "create_lesson",
                    "Thiếu `courseId`. Bạn muốn tạo bài học cho khóa nào?",
                    List.of());
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ToolExecutionResult.needInfo(
                    "create_lesson",
                    "Không tìm thấy khóa học với ID: " + courseId + ".",
                    List.of());
        }

        if (user.getRole() == User.Role.TEACHER
                && (course.getInstructorId() == null || !course.getInstructorId().equals(user.getId()))) {
            return ToolExecutionResult.needInfo(
                    "create_lesson",
                    "Bạn chỉ có thể tạo bài học cho khóa học do bạn sở hữu.",
                    List.of());
        }

        String title = textOf(payload, "title");
        if (title == null || title.isBlank()) {
            return ToolExecutionResult.needInfo(
                    "create_lesson",
                    "Thiếu `title`. Bạn muốn đặt tên bài học là gì?",
                    List.of());
        }

        Lesson.LessonType lessonType;
        try {
            lessonType = Lesson.LessonType.valueOf(textOf(payload, "type").toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return ToolExecutionResult.needInfo(
                    "create_lesson",
                    "Trường `type` không hợp lệ. Chỉ chấp nhận VIDEO, PDF, QUIZ.",
                    List.of());
        }

        String contentUrl = textOf(payload, "contentUrl");
        String contentText = textOf(payload, "contentText");
        if ((contentUrl == null || contentUrl.isBlank())
                && (contentText == null || contentText.isBlank())
                && lessonType != Lesson.LessonType.QUIZ) {
            return ToolExecutionResult.needInfo(
                    "create_lesson",
                    "Thiếu nội dung bài học. Vui lòng cung cấp `contentUrl` hoặc `contentText`.",
                    List.of(uploadFileAction("contentUrl", "Chọn file bài học", "image/*,.pdf,.mp4,.mp3,.txt,.md")));
        }

        Integer durationSeconds = intOf(payload, "durationSeconds");
        if (durationSeconds == null || durationSeconds < 0) {
            durationSeconds = 0;
        }
        Integer orderIndex = intOf(payload, "orderIndex");

        LessonRequest lessonRequest = LessonRequest.builder()
                .title(title.trim())
                .type(lessonType)
                .contentUrl(contentUrl)
                .contentText(contentText)
                .durationSeconds(durationSeconds)
                .orderIndex(orderIndex)
                .build();

        if (dryRun) {
            return ToolExecutionResult.needInfo(
                    "create_lesson",
                    "Mình đã đủ thông tin để tạo bài học \"" + lessonRequest.getTitle()
                            + "\" cho courseId=" + courseId
                            + ". Nếu đúng, hãy trả lời: `Xác nhận tạo bài học`.",
                    List.of());
        }

        long currentCount = lessonRepository.countByCourseId(courseId);
        Lesson lesson = Lesson.builder()
                .courseId(courseId)
                .title(lessonRequest.getTitle())
                .type(lessonRequest.getType())
                .contentUrl(lessonRequest.getContentUrl())
                .contentText(lessonRequest.getContentText())
                .durationSeconds(lessonRequest.getDurationSeconds() != null ? lessonRequest.getDurationSeconds() : 0)
                .orderIndex(lessonRequest.getOrderIndex() != null ? lessonRequest.getOrderIndex() : (int) currentCount)
                .build();
        lesson = lessonRepository.save(lesson);

        return ToolExecutionResult.success(
                "create_lesson",
                "Đã tạo bài học thành công. lessonId=" + lesson.getId()
                        + ", title=\"" + lesson.getTitle()
                        + "\", type=" + lesson.getType() + ".");
    }

    private AiTutorUiAction uploadImageAction(String targetField) {
        return uploadFileAction(targetField, "Chọn ảnh", "image/*");
    }

    private AiTutorUiAction uploadFileAction(String targetField, String label, String accept) {
        return AiTutorUiAction.builder()
                .type("UPLOAD_FILE")
                .label(label)
                .accept(accept)
                .targetField(targetField)
                .hint("Sau khi upload xong, URL sẽ được chèn vào ô nhập.")
                .build();
    }

    private String humanizeToolOutputWithModel(
            String userMessage,
            String language,
            String responseLength,
            String toolName,
            String rawToolOutput,
            boolean needMoreInfo) {
        String systemPrompt = "You are an AI tutor assistant that rewrites tool outputs to be clear for end users.\n"
                + "Always respond in " + language + ". Keep style " + responseLength + ".\n"
                + "If tool result says missing information, ask concise follow-up questions.\n"
                + "If operation succeeds, summarize clearly with actionable next step.\n"
                + "Do not mention internal JSON or implementation details.";
        String userPrompt = "Original user request: " + userMessage + "\n"
                + "Tool name: " + toolName + "\n"
                + "Need more info: " + needMoreInfo + "\n"
                + "Raw tool result:\n" + rawToolOutput;

        String rendered = callAiApi(systemPrompt, userPrompt);
        if (rendered == null || rendered.isBlank() || rendered.startsWith("Sorry, I encountered")) {
            return rawToolOutput;
        }
        return rendered;
    }

    private String serializeUiActions(List<AiTutorUiAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(actions);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPlatformContext(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current user role: ").append(user.getRole()).append('\n');

        Page<Course> shopCourses = courseRepository.findPublishedCourses(
                null, null, null,
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt")));
        sb.append("Shop published courses:\n");
        if (shopCourses.isEmpty()) {
            sb.append("- none\n");
        } else {
            for (Course c : shopCourses.getContent()) {
                sb.append("- [courseId=").append(c.getId()).append("] ")
                        .append(c.getTitle())
                        .append(" | price=").append(formatPrice(c.getPrice()))
                        .append('\n');
            }
        }

        List<Enrollment> enrollments = enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(user.getId());
        sb.append("User enrolled courses:\n");
        if (enrollments.isEmpty()) {
            sb.append("- none\n");
        } else {
            int count = 0;
            for (Enrollment enrollment : enrollments) {
                if (count >= 8) {
                    break;
                }
                Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
                if (course == null) {
                    continue;
                }
                sb.append("- [courseId=").append(course.getId()).append("] ")
                        .append(course.getTitle())
                        .append(" | progress=").append(enrollment.getProgressPercent()).append("%\n");
                count++;
            }
        }

        sb.append("Available exams for enrolled courses:\n");
        if (enrollments.isEmpty()) {
            sb.append("- none\n");
        } else {
            int examCount = 0;
            for (Enrollment enrollment : enrollments) {
                if (examCount >= 12) {
                    break;
                }
                List<Exam> exams = examRepository.findByCourseId(enrollment.getCourseId());
                for (Exam exam : exams) {
                    if (examCount >= 12) {
                        break;
                    }
                    sb.append("- [examId=").append(exam.getId()).append("] ")
                            .append(exam.getTitle())
                            .append(" | type=").append(exam.getExamType())
                            .append(" | courseId=").append(exam.getCourseId()).append('\n');
                    examCount++;
                }
            }
            if (examCount == 0) {
                sb.append("- none\n");
            }
        }

        if (user.getRole() == User.Role.TEACHER || user.getRole() == User.Role.ADMIN) {
            List<Course> myCourses = courseRepository.findByInstructorId(user.getId());
            sb.append("Teacher-owned courses:\n");
            if (myCourses.isEmpty()) {
                sb.append("- none\n");
            } else {
                for (int i = 0; i < Math.min(myCourses.size(), 8); i++) {
                    Course c = myCourses.get(i);
                    sb.append("- [courseId=").append(c.getId()).append("] ")
                            .append(c.getTitle())
                            .append(" | published=").append(Boolean.TRUE.equals(c.getIsPublished())).append('\n');
                }
            }

            Page<Exam> myExams = examRepository.findByCreatedBy(
                    user.getId(),
                    PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt")));
            sb.append("Teacher-created exams:\n");
            if (myExams.isEmpty()) {
                sb.append("- none\n");
            } else {
                for (Exam exam : myExams.getContent()) {
                    sb.append("- [examId=").append(exam.getId()).append("] ")
                            .append(exam.getTitle())
                            .append(" | type=").append(exam.getExamType())
                            .append(" | courseId=").append(exam.getCourseId()).append('\n');
                }
            }
        }

        return sb.toString();
    }

    private String detectPreferredLanguage(String message, String fallbackLanguage) {
        if (message != null) {
            String trimmed = message.trim();
            if (!trimmed.isEmpty()) {
                String normalized = trimmed.toLowerCase(Locale.ROOT);
                if (normalized.matches(".*[àáạảãăằắặẳẵâầấậẩẫèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*")
                        || normalized.contains("khóa học")
                        || normalized.contains("khoa hoc")
                        || normalized.contains("bài thi")
                        || normalized.contains("bai thi")
                        || containsVietnameseKeywords(normalized)) {
                    return "Vietnamese";
                }
                if (normalized.matches(".*[a-z].*")) {
                    return "English";
                }
            }
        }
        if (fallbackLanguage != null && !fallbackLanguage.isBlank()) {
            return fallbackLanguage;
        }
        return "English";
    }

    private boolean containsVietnameseKeywords(String normalized) {
        String[] tokens = normalized.split("[^\\p{L}0-9]+");
        int hits = 0;
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (VIETNAMESE_KEYWORDS.contains(token)) {
                hits++;
                if (hits >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private String resolveConversationIdForChat(User user, String requestedConversationId) {
        if (requestedConversationId == null) {
            return UUID.randomUUID().toString();
        }

        if (!isLegacyConversationId(requestedConversationId) || user == null) {
            return requestedConversationId;
        }

        Long legacyMessageId = parseLegacyMessageId(requestedConversationId);
        AiChatLog legacy = getChatMessageById(legacyMessageId, user.getId());
        if (legacy.getConversationId() != null && !legacy.getConversationId().isBlank()) {
            return legacy.getConversationId();
        }

        String generatedConversationId = UUID.randomUUID().toString();
        legacy.setConversationId(generatedConversationId);
        legacy.setConversationTitle(resolveTitle(legacy.getConversationTitle(), legacy.getUserMessage()));
        aiChatLogRepository.save(legacy);
        return generatedConversationId;
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        String normalized = conversationId.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveConversationTitle(List<AiChatLog> previousLogs, String message) {
        if (!previousLogs.isEmpty()) {
            for (AiChatLog log : previousLogs) {
                if (log.getConversationTitle() != null && !log.getConversationTitle().isBlank()) {
                    return log.getConversationTitle();
                }
            }
            return resolveTitle(null, previousLogs.get(0).getUserMessage());
        }
        return resolveTitle(null, message);
    }

    private String resolveTitle(String preferredTitle, String fallbackMessage) {
        if (preferredTitle != null && !preferredTitle.isBlank()) {
            return preferredTitle.trim();
        }
        String source = fallbackMessage == null ? "" : fallbackMessage.trim().replaceAll("\\s+", " ");
        if (source.isEmpty()) {
            return "Cuộc trò chuyện mới";
        }
        return source.length() <= 60 ? source : source.substring(0, 60) + "...";
    }

    private String toConversationKey(AiChatLog log) {
        if (log.getConversationId() != null && !log.getConversationId().isBlank()) {
            return log.getConversationId();
        }
        return "legacy-" + log.getId();
    }

    private boolean isLegacyConversationId(String conversationId) {
        return conversationId != null && conversationId.startsWith("legacy-");
    }

    private Long parseLegacyMessageId(String conversationId) {
        if (!isLegacyConversationId(conversationId)) {
            throw new ApiException("Invalid conversation id", HttpStatus.BAD_REQUEST);
        }
        try {
            return Long.parseLong(conversationId.substring("legacy-".length()));
        } catch (NumberFormatException ex) {
            throw new ApiException("Invalid conversation id", HttpStatus.BAD_REQUEST);
        }
    }

    private String toPreview(String text) {
        String normalized = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return "Cuộc trò chuyện mới";
        }
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "...";
    }

    private AiChatLog getChatMessageById(Long id, Long userId) {
        return aiChatLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException("Chat not found or access denied", HttpStatus.NOT_FOUND));
    }

    private String extractJsonPayload(String rawMessage) {
        int start = rawMessage.indexOf('{');
        int end = rawMessage.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return rawMessage.substring(start, end + 1);
    }

    private int extractTrailingInt(String raw, int defaultValue, int min, int max) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String[] parts = raw.trim().split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String token = parts[i].trim();
            if (token.matches("\\d+")) {
                try {
                    int parsed = Integer.parseInt(token);
                    return Math.max(min, Math.min(max, parsed));
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private Long extractFirstLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split("[^0-9]+");
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            try {
                return Long.parseLong(part);
            } catch (NumberFormatException ignored) {
                // Ignore and continue scanning.
            }
        }
        return null;
    }

    private String extractSearchKeyword(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String payload = extractJsonPayload(raw);
        if (payload != null) {
            try {
                JsonNode json = objectMapper.readTree(payload);
                String fromKeyword = textOf(json, "keyword");
                if (fromKeyword != null && !fromKeyword.isBlank()) {
                    return fromKeyword;
                }
                String fromQuery = textOf(json, "query");
                if (fromQuery != null && !fromQuery.isBlank()) {
                    return fromQuery;
                }
                String fromQ = textOf(json, "q");
                if (fromQ != null && !fromQ.isBlank()) {
                    return fromQ;
                }
            } catch (Exception ignored) {
                // Ignore invalid JSON and continue with plain-text extraction.
            }
        }

        String normalized = raw
                .replaceFirst("(?i)^/tool\\s*", "")
                .replaceFirst("(?i)^search-courses\\b", "")
                .replaceFirst("(?i)^(tim|tìm|search)\\s+", "")
                .replaceFirst("(?i)^(khoa hoc|khóa học|course|courses)\\s+", "")
                .replaceAll("^[\\-:]+", "")
                .trim();

        String[] tokens = normalized.split("\\s+");
        if (tokens.length > 1 && tokens[tokens.length - 1].matches("\\d+")) {
            normalized = normalized.substring(0, normalized.lastIndexOf(tokens[tokens.length - 1])).trim();
        }

        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > 80) {
            return normalized.substring(0, 80).trim();
        }
        return normalized;
    }

    private String textOf(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return value != null ? value.trim() : null;
    }

    private Long longOf(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        try {
            return node.get(field).asLong();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer intOf(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        try {
            return node.get(field).asInt();
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean boolOf(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        try {
            return node.get(field).asBoolean();
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal decimalOf(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        try {
            return new BigDecimal(node.get(field).asText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0";
        }
        return price.stripTrailingZeros().toPlainString();
    }

    private String callAiApi(String systemPrompt, String userMessage) {
        return callAiApi(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)));
    }

    private String callAiApi(List<Map<String, Object>> messages) {
        if (!aiRuntimeSettingsService.isTutorEnabled()) {
            return "AI Tutor đang tạm thời bị khóa bởi quản trị viên.";
        }

        String configuredModel = aiRuntimeSettingsService.getSettings().getTutorModel();
        List<String> modelPool = resolveTutorModelPool(configuredModel);
        if (modelPool.isEmpty()) {
            return "System Error: Tutor model is not configured. Please set OPENROUTER_API_MODEL.";
        }
        return callAiApiWithModel(messages, modelPool.get(0), true, modelPool);
    }

    private String callAiApiWithModel(
            List<Map<String, Object>> messages,
            String modelToUse,
            boolean allowFallback,
            List<String> modelPool) {
        List<String> keyPool = resolveApiKeyPool();
        if (keyPool.isEmpty()) {
            return "System Error: OpenRouter API key is not configured. Please contact the administrator.";
        }
        if (aiRuntimeSettingsService.isModelBlocked(modelToUse)) {
            if (!allowFallback) {
                return "AI Tutor không khả dụng do model hiện tại đang bị khóa.";
            }
            String fallbackCandidate = resolveTutorFallbackModel(modelToUse, modelPool);
            if (fallbackCandidate == null) {
                return "AI Tutor không khả dụng vì các model đã bị khóa.";
            }
            return callAiApiWithModel(messages, fallbackCandidate, true, modelPool);
        }

        Map<String, Object> requestBody = Map.of(
                "model", modelToUse,
                "temperature", temperature,
                "top_p", topP,
                "max_tokens", maxTokens,
                "messages", messages);

        RestClientResponseException lastHttpError = null;
        Exception lastError = null;

        for (int i = 0; i < keyPool.size(); i++) {
            String currentKey = keyPool.get(i);
            try {
                System.out.println("AITutor OpenRouter call: model=" + modelToUse + ", keyAttempt=" + (i + 1) + "/" + keyPool.size());
                Map<?, ?> response = restClient.post()
                        .uri(apiUrl)
                        .header("Authorization", "Bearer " + currentKey)
                        .header("Content-Type", "application/json")
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);
                return extractTextFromResponse(response);
            } catch (RestClientResponseException e) {
                lastHttpError = e;
                lastError = e;
                String errorBody = e.getResponseBodyAsString();
                System.err.println("Error calling OpenRouter API. model=" + modelToUse + ", keyAttempt="
                        + (i + 1) + "/" + keyPool.size() + ", Status: " + e.getStatusCode() + ", Body: " + errorBody);

                boolean retryable = isRateLimitError(e) || e.getStatusCode().value() == 429 || e.getStatusCode().value() >= 500;
                if (retryable && i < keyPool.size() - 1) {
                    continue;
                }
                break;
            } catch (Exception e) {
                lastError = e;
                System.err.println("Unexpected error calling OpenRouter API: " + e.getMessage());
                if (i < keyPool.size() - 1) {
                    continue;
                }
            }
        }

        if (allowFallback && shouldFallbackFrom(modelToUse, modelPool) && lastHttpError != null) {
            if (isRateLimitError(lastHttpError) || lastHttpError.getStatusCode().value() == 429
                    || lastHttpError.getStatusCode().value() == 400
                    || lastHttpError.getStatusCode().value() == 404
                    || lastHttpError.getStatusCode().value() >= 500) {
                String fallbackCandidate = resolveTutorFallbackModel(modelToUse, modelPool);
                if (fallbackCandidate != null) {
                    System.out.println("Attempting fallback to model: " + fallbackCandidate);
                    return callAiApiWithModel(messages, fallbackCandidate, true, modelPool);
                }
            }
        }

        if (lastHttpError != null) {
            return buildTutorProviderErrorMessage(lastHttpError, modelToUse);
        }
        String msg = lastError != null ? lastError.getMessage() : "Unknown error";
        return "AI Tutor gặp lỗi kết nối model. Chi tiết: " + msg + ". Vui lòng thử lại sau ít phút.";
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

    private boolean hasImageParts(List<Map<String, Object>> messages) {
        for (Map<String, Object> message : messages) {
            Object content = message.get("content");
            if (!(content instanceof List<?> parts)) {
                continue;
            }
            for (Object part : parts) {
                if (!(part instanceof Map<?, ?> partMap)) {
                    continue;
                }
                Object type = partMap.get("type");
                if (type != null && "image_url".equals(String.valueOf(type))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUnsupportedImageError(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return false;
        }
        String normalized = body.toLowerCase(Locale.ROOT);
        return normalized.contains("image_url")
                || normalized.contains("vision")
                || normalized.contains("multimodal")
                || (normalized.contains("image") && normalized.contains("not support"));
    }

    private List<Map<String, Object>> stripImageParts(List<Map<String, Object>> messages) {
        List<Map<String, Object>> stripped = new ArrayList<>(messages.size());
        for (Map<String, Object> message : messages) {
            Object content = message.get("content");
            if (!(content instanceof List<?> parts)) {
                stripped.add(message);
                continue;
            }

            StringBuilder textBuilder = new StringBuilder();
            for (Object part : parts) {
                if (!(part instanceof Map<?, ?> partMap)) {
                    continue;
                }
                Object type = partMap.get("type");
                if (!"text".equals(String.valueOf(type))) {
                    continue;
                }
                Object text = partMap.get("text");
                if (text != null && !text.toString().isBlank()) {
                    if (!textBuilder.isEmpty()) {
                        textBuilder.append('\n');
                    }
                    textBuilder.append(text);
                }
            }
            stripped.add(Map.of(
                    "role", message.get("role"),
                    "content", textBuilder.toString()));
        }
        return stripped;
    }

    private boolean shouldFallbackFrom(String currentModel, List<String> modelPool) {
        String fallbackCandidate = resolveTutorFallbackModel(currentModel, modelPool);
        return fallbackCandidate != null && !fallbackCandidate.equalsIgnoreCase(currentModel);
    }

    private String resolveTutorFallbackModel(String currentModel, List<String> modelPool) {
        if (modelPool == null || modelPool.isEmpty()) {
            return null;
        }
        int currentIndex = -1;
        for (int i = 0; i < modelPool.size(); i++) {
            if (modelPool.get(i).equalsIgnoreCase(currentModel)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) {
            return modelPool.get(0);
        }
        for (int i = currentIndex + 1; i < modelPool.size(); i++) {
            String candidate = modelPool.get(i);
            if (!aiRuntimeSettingsService.isModelBlocked(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private List<String> resolveTutorModelPool(String configuredModel) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        addModelCandidate(ordered, configuredModel);
        addModelCandidate(ordered, aiRuntimeSettingsService.getSettings().getTutorFallbackModel());

        if (models != null && !models.isBlank()) {
            for (String raw : models.split(",")) {
                addModelCandidate(ordered, raw);
            }
        }

        addModelCandidate(ordered, model);
        addModelCandidate(ordered, fallbackModel);

        List<String> result = new ArrayList<>();
        for (String candidate : ordered) {
            if (!aiRuntimeSettingsService.isModelBlocked(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private void addModelCandidate(Set<String> target, String candidate) {
        if (candidate == null) {
            return;
        }
        String normalized = candidate.trim();
        if (!normalized.isBlank()) {
            target.add(normalized);
        }
    }

    private boolean isRateLimitError(RestClientResponseException e) {
        if (e.getStatusCode() != null && e.getStatusCode().value() == 429) {
            return true;
        }
        String body = e.getResponseBodyAsString();
        return body != null && body.toLowerCase(Locale.ROOT).contains("rate limit");
    }

    private String buildTutorProviderErrorMessage(RestClientResponseException e, String modelName) {
        int status = e.getStatusCode().value();
        String body = e.getResponseBodyAsString();
        String normalized = body == null ? "" : body.toLowerCase(Locale.ROOT);

        if (status == 429 || normalized.contains("rate limit")) {
            return "AI Tutor đang bị giới hạn tần suất (429) ở model '" + modelName
                    + "'. Vui lòng thử lại sau 20-60 giây hoặc đổi model/key.";
        }
        if (status == 404 && normalized.contains("deprecated")) {
            return "Model '" + modelName
                    + "' đã deprecated trên OpenRouter (404). Vui lòng đổi model trong cấu hình.";
        }
        if (status == 404) {
            return "Model '" + modelName
                    + "' không tồn tại/không truy cập được (404). Vui lòng kiểm tra tên model.";
        }
        if (status == 401 || status == 403) {
            return "OpenRouter API key không hợp lệ hoặc không đủ quyền (" + status
                    + "). Vui lòng kiểm tra OPENROUTER_API_KEY(S).";
        }
        if (status >= 500) {
            return "OpenRouter/provider đang lỗi tạm thời (" + status
                    + "). Vui lòng thử lại sau ít phút.";
        }
        return "AI Tutor gặp lỗi provider (" + status + ") với model '" + modelName
                + "'. Vui lòng thử lại hoặc đổi model.";
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<?, ?> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
            return "No valid response from AI.";
        } catch (Exception e) {
            System.err.println("Failed to parse AI response: " + e.getMessage());
            return "Failed to parse AI response.";
        }
    }
}

package com.thinkai.backend.service.aitutor;

import com.thinkai.backend.entity.AiChatLog;
import com.thinkai.backend.entity.User;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

@Service
public class AiAgentRouterService {

    private final ObjectMapper objectMapper;
    private final AiToolRegistryService aiToolRegistryService;

    public AiAgentRouterService(ObjectMapper objectMapper, AiToolRegistryService aiToolRegistryService) {
        this.objectMapper = objectMapper;
        this.aiToolRegistryService = aiToolRegistryService;
    }

    public AiAgentRouteDecision route(
            User user,
            String message,
            List<AiChatLog> previousLogs,
            int maxToolRouterTurns,
            AiRouterModelClient modelClient) {
        if (message == null || message.isBlank()) {
            return AiAgentRouteDecision.none(objectMapper);
        }

        String systemPrompt = buildRouterSystemPrompt();
        String userPrompt = buildRouterUserPrompt(user, message, previousLogs, maxToolRouterTurns);

        String routerRaw = modelClient.complete(systemPrompt, userPrompt);
        AiAgentRouteDecision fromModel = parseRouteDecision(routerRaw);
        if (!fromModel.isNone()) {
            return fromModel;
        }

        return fallbackByHeuristic(message);
    }

    private String buildRouterSystemPrompt() {
        StringJoiner actionsJoiner = new StringJoiner(", ");
        for (String action : aiToolRegistryService.knownActions()) {
            actionsJoiner.add(action);
        }

        return "You are a multi-agent router for BiliBily (AI Tutor platform assistant).\n"
                + "Return strict JSON only with schema: {\"action\":\"...\",\"args\":{...}}.\n"
                + "Do not return markdown or explanation.\n"
                + "Available actions: " + actionsJoiner + ".\n"
                + "Agent mapping rules:\n"
                + "- LEARNING: list/search/detail/enroll/my-progress/my-courses.\n"
                + "- COURSE_OPS: create/publish course, create lesson.\n"
                + "- EXAM_OPS: list/create exams.\n"
                + "- TUTOR: non-tool English tutoring questions => action none.\n"
                + "If no tool is needed, return action none.";
    }

    private String buildRouterUserPrompt(User user, String message, List<AiChatLog> previousLogs, int maxToolRouterTurns) {
        StringBuilder historyContext = new StringBuilder();
        int start = Math.max(0, previousLogs.size() - Math.max(1, maxToolRouterTurns));
        for (int i = start; i < previousLogs.size(); i++) {
            AiChatLog log = previousLogs.get(i);
            historyContext.append("User: ").append(log.getUserMessage()).append('\n');
            historyContext.append("Assistant: ").append(log.getAiResponse()).append('\n');
        }

        return "User role: " + user.getRole() + "\n"
                + "Recent conversation:\n" + historyContext + "\n"
                + "Current user message:\n" + message + "\n"
                + "Return JSON now.";
    }

    private AiAgentRouteDecision parseRouteDecision(String routerRaw) {
        String routerJson = extractJsonPayload(routerRaw);
        if (routerJson == null) {
            return AiAgentRouteDecision.none(objectMapper);
        }
        try {
            JsonNode node = objectMapper.readTree(routerJson);
            String action = textOf(node, "action");
            if (action == null) {
                return AiAgentRouteDecision.none(objectMapper);
            }

            action = action.trim().toLowerCase(Locale.ROOT);
            if (!aiToolRegistryService.isKnownAction(action)) {
                return AiAgentRouteDecision.none(objectMapper);
            }

            JsonNode args = node.has("args") && node.get("args").isObject()
                    ? node.get("args")
                    : objectMapper.createObjectNode();
            AiAgentType agentType = aiToolRegistryService.find(action)
                    .map(AiToolDefinition::agentType)
                    .orElse(AiAgentType.TUTOR);
            return new AiAgentRouteDecision(agentType, action, args);
        } catch (Exception ex) {
            return AiAgentRouteDecision.none(objectMapper);
        }
    }

    private AiAgentRouteDecision fallbackByHeuristic(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        String action = null;

        if (normalized.contains("khóa học trên shop")
                || normalized.contains("shop courses")
                || normalized.contains("danh sách khóa học")) {
            action = "list_shop_courses";
        } else if (normalized.contains("tìm khóa học")
                || normalized.contains("search course")
                || normalized.contains("search courses")) {
            action = "search_shop_courses";
        } else if (normalized.contains("chi tiết khóa học")
                || normalized.contains("course detail")) {
            action = "get_course_detail";
        } else if (normalized.contains("danh sách bài học")
                || normalized.contains("course lessons")
                || normalized.contains("lessons of course")) {
            action = "list_course_lessons";
        } else if (normalized.contains("khóa học của tôi")
                || normalized.contains("my courses")) {
            action = "list_my_courses";
        } else if (normalized.contains("tiến độ học")
                || normalized.contains("learning progress")
                || normalized.contains("my progress")) {
            action = "list_my_learning_progress";
        } else if (normalized.contains("bài thi có sẵn")
                || normalized.contains("my exams")
                || normalized.contains("available exams")) {
            action = "list_my_exams";
        } else if (normalized.contains("đăng ký khóa học") || normalized.contains("enroll")) {
            action = "enroll_course";
        } else if (normalized.contains("tạo khóa học") || normalized.contains("create course")) {
            action = "create_course";
        } else if (normalized.contains("tạo bài thi") || normalized.contains("create exam")) {
            action = "create_exam";
        } else if (normalized.contains("publish khóa học")
                || normalized.contains("xuất bản khóa học")
                || normalized.contains("publish course")) {
            action = "publish_course";
        } else if (normalized.contains("tạo bài học")
                || normalized.contains("create lesson")) {
            action = "create_lesson";
        }

        if (action == null) {
            return AiAgentRouteDecision.none(objectMapper);
        }
        AiAgentType agentType = aiToolRegistryService.find(action)
                .map(AiToolDefinition::agentType)
                .orElse(AiAgentType.TUTOR);
        return new AiAgentRouteDecision(agentType, action, objectMapper.createObjectNode());
    }

    private String extractJsonPayload(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return null;
        }
        int start = rawMessage.indexOf('{');
        int end = rawMessage.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return rawMessage.substring(start, end + 1);
    }

    private String textOf(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return value != null ? value.trim() : null;
    }
}


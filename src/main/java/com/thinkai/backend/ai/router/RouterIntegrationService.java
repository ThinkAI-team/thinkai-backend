package com.thinkai.backend.ai.router;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.service.aitutor.AiAgentRouteDecision;
import com.thinkai.backend.service.aitutor.AiAgentType;
import com.thinkai.backend.service.aitutor.AiToolRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Integration service giữa Hybrid Router và hệ thống routing hiện có
 * Chuyển đổi giữa AgentType (new) và AiAgentType (legacy)
 */
@Service
public class RouterIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(RouterIntegrationService.class);

    private final HybridRouter hybridRouter;
    private final AiToolRegistryService toolRegistryService;

    public RouterIntegrationService(HybridRouter hybridRouter, AiToolRegistryService toolRegistryService) {
        this.hybridRouter = hybridRouter;
        this.toolRegistryService = toolRegistryService;
    }

    /**
     * Route user message sử dụng Hybrid Router
     * Trả về AgentType (harness)
     */
    public AgentType routeToHarnessAgent(String message, String userLevel, String targetExam) {
        AiHarnessRequest request = AiHarnessRequest.builder()
            .message(message)
            .userLevel(userLevel != null ? userLevel : "B1")
            .targetExam(targetExam)
            .build();

        HybridRouter.RoutingResult result = hybridRouter.route(request);

        logger.debug("Harness routing: '{}' -> {}, confidence={}",
            message.substring(0, Math.min(50, message.length())),
            result.agent(),
            result.confidence());

        return result.agent();
    }

    /**
     * Convert từ Harness AgentType sang Legacy AiAgentType
     */
    public AiAgentType toLegacyAgentType(AgentType harnessAgent) {
        return switch (harnessAgent) {
            case PLATFORM_LEARNING, PLATFORM_COURSE_OPS, PLATFORM_EXAM_OPS -> AiAgentType.LEARNING;
            case TOEIC_READING, TOEIC_LISTENING, TOEIC_GRAMMAR, TOEIC_VOCABULARY,
                 IELTS_READING, IELTS_LISTENING, IELTS_WRITING, IELTS_SPEAKING,
                 GRAMMAR, VOCABULARY, PRONUNCIATION, CONVERSATION,
                 EXAM_STRATEGY, MISTAKE_ANALYZER, PROGRESS_TRACKER -> AiAgentType.TUTOR;
        };
    }

    /**
     * Check nếu message cần platform tool (tương thích với AiAgentRouterService)
     */
    public boolean needsPlatformTool(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);

        // Platform operation keywords
        return normalized.contains("course") ||
               normalized.contains("khóa học") ||
               normalized.contains("enroll") ||
               normalized.contains("đăng ký") ||
               normalized.contains("progress") ||
               normalized.contains("tiến độ") ||
               normalized.contains("exam") ||
               normalized.contains("bài thi");
    }

    /**
     * Get routing explanation cho debugging
     */
    public String getRoutingExplanation(String message) {
        AiHarnessRequest request = AiHarnessRequest.builder()
            .message(message)
            .build();

        HybridRouter.RoutingExplanation explanation = hybridRouter.explain(request);

        StringBuilder sb = new StringBuilder();
        sb.append("Input: ").append(explanation.input()).append("\n");
        sb.append("Final Agent: ").append(explanation.finalResult().agent()).append("\n");
        sb.append("Confidence: ").append(String.format("%.3f", explanation.finalResult().confidence())).append("\n");
        sb.append("Reason: ").append(explanation.finalResult().reason()).append("\n");

        return sb.toString();
    }

    /**
     * Route và trả về decision cho integration với AITutorService
     */
    public AiAgentRouteDecision routeForTutor(User user, String message) {
        AgentType harnessAgent = routeToHarnessAgent(message, null, null);

        // Chuyển đổi sang legacy type
        AiAgentType legacyType = toLegacyAgentType(harnessAgent);

        // Tạo AiAgentRouteDecision
        // Note: Cần ObjectMapper và tool registry
        return new AiAgentRouteDecision(
            legacyType,
            "none", // action - sẽ được xử lý bởi tool router nếu cần
            null    // args
        );
    }

    /**
     * Get suggested agents cho user dựa trên profile
     */
    public java.util.List<AgentType> getSuggestedAgents(User user, String targetExam) {
        java.util.List<AgentType> suggestions = new java.util.ArrayList<>();

        // Add target exam agents
        if ("TOEIC".equalsIgnoreCase(targetExam)) {
            suggestions.add(AgentType.TOEIC_READING);
            suggestions.add(AgentType.TOEIC_LISTENING);
        } else if ("IELTS".equalsIgnoreCase(targetExam)) {
            suggestions.add(AgentType.IELTS_SPEAKING);
            suggestions.add(AgentType.IELTS_WRITING);
        }

        // Add general agents
        suggestions.add(AgentType.GRAMMAR);
        suggestions.add(AgentType.VOCABULARY);

        return suggestions.stream().distinct().toList();
    }
}

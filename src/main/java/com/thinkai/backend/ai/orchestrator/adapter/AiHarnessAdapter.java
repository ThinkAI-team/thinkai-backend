package com.thinkai.backend.ai.orchestrator.adapter;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import com.thinkai.backend.ai.state.AiHarnessResponse;
import com.thinkai.backend.dto.AIChatRequest;
import com.thinkai.backend.dto.AIChatResponse;
import com.thinkai.backend.service.aitutor.AiAgentType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AiHarnessAdapter {

    public AiHarnessRequest toHarnessRequest(AIChatRequest request, Long userId,
            String userLevel, String targetExam, Integer targetScore,
            List<String> weakPoints, List<String> strongPoints) {

        return AiHarnessRequest.builder()
            .traceId(UUID.randomUUID().toString())
            .userId(userId)
            .conversationId(request.getConversationId())
            .message(request.getMessage())
            .userLevel(userLevel != null ? userLevel : "B1")
            .targetExam(targetExam)
            .targetScore(targetScore)
            .lessonContext(request.getContext() != null ? request.getContext() : "General English")
            .weakPoints(weakPoints)
            .strongPoints(strongPoints)
            .build();
    }

    public AIChatResponse toChatResponse(AiHarnessResponse harnessResponse) {
        if (harnessResponse == null) {
            return new AIChatResponse(
                "I apologize, but I encountered an error.",
                null, null, List.of(), AiAgentType.TUTOR
            );
        }

        // Get thinking steps
        List<Map<String, Object>> thinkingStepsList = getThinkingSteps(harnessResponse);

        AIChatResponse response;

        if (harnessResponse.errorCode() != null) {
            response = new AIChatResponse(
                harnessResponse.errorMessage() != null
                    ? harnessResponse.errorMessage()
                    : "An error occurred. Please try again.",
                harnessResponse.conversationId(),
                harnessResponse.messageId(),
                harnessResponse.uiActions(),
                mapAgentType(harnessResponse.agentType())
            );
        } else {
            response = new AIChatResponse(
                harnessResponse.content(),
                harnessResponse.conversationId(),
                harnessResponse.messageId(),
                harnessResponse.uiActions(),
                mapAgentType(harnessResponse.agentType())
            );
        }

        response.setThinkingSteps(thinkingStepsList);
        return response;
    }

    /**
     * Get thinking steps for frontend display
     */
    public List<Map<String, Object>> getThinkingSteps(AiHarnessResponse harnessResponse) {
        if (harnessResponse == null || harnessResponse.thinkingSteps() == null) {
            return List.of();
        }

        return harnessResponse.thinkingSteps().stream()
            .map(step -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("step", step.step());
                map.put("description", step.description());
                map.put("latencyMs", step.latencyMs());
                map.put("success", step.success());
                return map;
            })
            .toList();
    }

    private AiAgentType mapAgentType(AgentType agentType) {
        if (agentType == null) {
            return AiAgentType.TUTOR;
        }

        return switch (agentType) {
            case TOEIC_READING, TOEIC_LISTENING, TOEIC_GRAMMAR, TOEIC_VOCABULARY -> AiAgentType.LEARNING;
            case IELTS_READING, IELTS_LISTENING, IELTS_WRITING, IELTS_SPEAKING -> AiAgentType.LEARNING;
            case GRAMMAR, VOCABULARY, PRONUNCIATION, CONVERSATION -> AiAgentType.LEARNING;
            case EXAM_STRATEGY, MISTAKE_ANALYZER, PROGRESS_TRACKER -> AiAgentType.LEARNING;
            default -> AiAgentType.TUTOR;
        };
    }

    public Map<String, Object> extractContextHints(AiHarnessResponse response) {
        if (response == null || response.contextHints() == null) {
            return Map.of();
        }
        return response.contextHints();
    }
}

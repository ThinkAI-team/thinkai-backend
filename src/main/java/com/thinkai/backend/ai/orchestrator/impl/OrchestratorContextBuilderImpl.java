package com.thinkai.backend.ai.orchestrator.impl;

import com.thinkai.backend.ai.adaptive.RuleBasedAdaptiveService;
import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.context.ConversationContextLoader;
import com.thinkai.backend.ai.context.UserContextLoader;
import com.thinkai.backend.ai.orchestrator.OrchestratorContextBuilder;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorContextBuilderImpl implements OrchestratorContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorContextBuilderImpl.class);

    private final UserContextLoader userContextLoader;
    private final ConversationContextLoader conversationContextLoader;
    private final RuleBasedAdaptiveService adaptiveService;

    public OrchestratorContextBuilderImpl(
            UserContextLoader userContextLoader,
            ConversationContextLoader conversationContextLoader,
            RuleBasedAdaptiveService adaptiveService) {
        this.userContextLoader = userContextLoader;
        this.conversationContextLoader = conversationContextLoader;
        this.adaptiveService = adaptiveService;
    }

    @Override
    public String build(AiHarnessRequest request, AgentType agent) {
        StringBuilder context = new StringBuilder();
        UserProfileContext profile = null;

        try {
            if (request.userId() != null) {
                profile = userContextLoader.load(request.userId());
                
                context.append("User Level: ").append(profile.level()).append("\n");
                
                if (profile.targetExam() != null) {
                    context.append("Target Exam: ").append(profile.targetExam());
                    if (profile.targetScore() != null) {
                        context.append(" (Target Score: ").append(profile.targetScore()).append(")");
                    }
                    context.append("\n");
                }
                
                if (!profile.weakPoints().isEmpty()) {
                    context.append("Weak Areas: ").append(String.join(", ", profile.weakPoints())).append("\n");
                }
                if (!profile.strongPoints().isEmpty()) {
                    context.append("Strong Areas: ").append(String.join(", ", profile.strongPoints())).append("\n");
                }
                
                context.append("Lessons Completed: ").append(profile.totalLessonsCompleted()).append("\n");
                context.append("Daily Streak: ").append(profile.dailyStreak()).append(" days\n");
            } else {
                profile = new UserProfileContext(
                    request.userLevel(),
                    request.targetExam(),
                    request.targetScore(),
                    request.weakPoints(),
                    request.strongPoints(),
                    0,
                    0,
                    java.util.Map.of()
                );
                context.append("User Level: ").append(request.userLevel()).append("\n");
            }

            if (request.lessonContext() != null && !request.lessonContext().isBlank()) {
                context.append("Lesson Context: ").append(request.lessonContext()).append("\n");
            }

            appendChatPreferences(context, request);

            String adaptiveGuidance = adaptiveService.buildAdaptiveGuidance(profile, agent, request.message());
            if (!adaptiveGuidance.isBlank()) {
                context.append("\n").append(adaptiveGuidance).append("\n");
            }

            if (request.conversationId() != null && request.userId() != null) {
                String convHistory = conversationContextLoader.load(
                    request.userId(), 
                    request.conversationId(), 
                    6
                );
                if (!convHistory.isBlank()) {
                    context.append("\n").append(convHistory).append("\n");
                }
            }

            context.append("\nUser Message: ").append(request.message());

        } catch (Exception e) {
            log.error("Error building context: {}", e.getMessage());
            context.append("User Level: ").append(request.userLevel()).append("\n");
            context.append("User Message: ").append(request.message());
        }

        return context.toString();
    }

    @Override
    public UserProfileContext buildUserProfile(Long userId) {
        return userContextLoader.load(userId);
    }

    @Override
    public String buildConversationContext(String conversationId, int maxTurns) {
        return "";
    }

    private void appendChatPreferences(StringBuilder context, AiHarnessRequest request) {
        if (request == null || request.metadata() == null || request.metadata().isEmpty()) {
            return;
        }
        String language = asText(request.metadata().get("language"));
        String responseLength = asText(request.metadata().get("responseLength"));
        String communicationStyle = asText(request.metadata().get("communicationStyle"));
        String correctionMode = asText(request.metadata().get("correctionMode"));
        String answerFormat = asText(request.metadata().get("answerFormat"));

        if (language.isBlank() && responseLength.isBlank() && communicationStyle.isBlank()
                && correctionMode.isBlank() && answerFormat.isBlank()) {
            return;
        }

        context.append("\nChat Preferences:\n");
        if (!language.isBlank()) {
            context.append("- Reply language: ").append(language).append("\n");
        }
        if (!responseLength.isBlank()) {
            context.append("- Response length: ").append(responseLength).append("\n");
        }
        if (!communicationStyle.isBlank()) {
            context.append("- Communication style: ").append(communicationStyle).append("\n");
        }
        if (!correctionMode.isBlank()) {
            context.append("- Correction mode: ").append(correctionMode).append("\n");
        }
        if (!answerFormat.isBlank()) {
            context.append("- Answer format: ").append(answerFormat).append("\n");
        }
    }

    private String asText(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }
}

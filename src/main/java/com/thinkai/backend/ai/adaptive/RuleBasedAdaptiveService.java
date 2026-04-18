package com.thinkai.backend.ai.adaptive;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.orchestrator.OrchestratorContextBuilder.UserProfileContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RuleBasedAdaptiveService {

    public String buildAdaptiveGuidance(UserProfileContext profile, AgentType agent, String userMessage) {
        if (profile == null) {
            return "";
        }

        List<String> rules = new ArrayList<>();
        String level = normalize(profile.level());
        String message = normalize(userMessage);

        if ("A1".equals(level) || "A2".equals(level)) {
            rules.add("Use very simple words and short sentences.");
            rules.add("Give one clear example before any advanced explanation.");
        } else if ("B1".equals(level) || "B2".equals(level)) {
            rules.add("Balance explanation and practice with moderate detail.");
            rules.add("Provide 2 examples and one quick mini-exercise.");
        } else if ("C1".equals(level) || "C2".equals(level)) {
            rules.add("Use advanced nuance, collocations, and concise correction.");
            rules.add("Include contrast between natural and unnatural phrasing.");
        }

        if (profile.weakPoints() != null && !profile.weakPoints().isEmpty()) {
            rules.add("Prioritize weak points in feedback: " + String.join(", ", profile.weakPoints()) + ".");
        }

        if (profile.strongPoints() != null && !profile.strongPoints().isEmpty()) {
            rules.add("Leverage strong points to scaffold harder tasks: " + String.join(", ", profile.strongPoints()) + ".");
        }

        if (profile.dailyStreak() <= 1) {
            rules.add("Keep first task easy to increase confidence and retention.");
        } else if (profile.dailyStreak() >= 7) {
            rules.add("Increase challenge slightly and add timed practice.");
        }

        if (profile.targetExam() != null && !profile.targetExam().isBlank()) {
            String exam = profile.targetExam().trim();
            if ("TOEIC".equalsIgnoreCase(exam)) {
                rules.add("Format examples in TOEIC style when relevant.");
            } else if ("IELTS".equalsIgnoreCase(exam)) {
                rules.add("Format feedback in IELTS band-focused style when relevant.");
            } else {
                rules.add("Align responses with target exam style: " + exam + ".");
            }
        }

        if (profile.targetScore() != null) {
            if (profile.targetScore() >= 850) {
                rules.add("Focus on high-accuracy corrections and edge cases.");
            } else if (profile.targetScore() >= 650) {
                rules.add("Focus on consistent accuracy across common patterns.");
            } else {
                rules.add("Focus on core grammar and high-frequency vocabulary first.");
            }
        }

        if (agent == AgentType.CONVERSATION && containsAny(message, "speaking", "conversation", "nói", "hội thoại")) {
            rules.add("Respond with natural dialogue turns and one speaking drill.");
        }
        if (containsAny(message, "grammar", "ngữ pháp")) {
            rules.add("Include rule + exception + one corrected learner sentence.");
        }
        if (containsAny(message, "vocab", "từ vựng", "word")) {
            rules.add("Provide pronunciation hint and one collocation.");
        }

        String customRules = extractCustomRules(profile);
        if (!customRules.isBlank()) {
            rules.add("Apply user custom adaptive rules: " + customRules);
        }

        if (rules.isEmpty()) {
            return "";
        }

        return "Adaptive Rules:\n- " + String.join("\n- ", rules);
    }

    private String extractCustomRules(UserProfileContext profile) {
        if (profile == null || profile.metadata() == null) {
            return "";
        }
        Object raw = profile.metadata().get("adaptiveRules");
        if (!(raw instanceof String text)) {
            return "";
        }
        String normalized = text.trim().replace("\n", " | ");
        if (normalized.length() > 500) {
            return normalized.substring(0, 500);
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsAny(String message, String... keywords) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}

package com.thinkai.backend.ai.validator;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SafetyChecker {

    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
        Pattern.compile("<script", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onclick=", Pattern.CASE_INSENSITIVE)
    );

    private static final List<String> BLOCKED_TOPICS = List.of(
        "violence",
        "hate speech",
        "illegal",
        "harmful advice"
    );

    public SafetyResult check(String content) {
        List<String> issues = new ArrayList<>();

        if (content == null || content.isBlank()) {
            issues.add("Content is empty");
            return new SafetyResult(false, issues);
        }

        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(content).find()) {
                issues.add("Potential XSS pattern detected");
            }
        }

        String lowerContent = content.toLowerCase();
        for (String topic : BLOCKED_TOPICS) {
            if (lowerContent.contains(topic)) {
                issues.add("Content contains restricted topic: " + topic);
            }
        }

        boolean safe = issues.isEmpty();
        return new SafetyResult(safe, issues);
    }

    public record SafetyResult(
        boolean safe,
        List<String> issues
    ) {}
}

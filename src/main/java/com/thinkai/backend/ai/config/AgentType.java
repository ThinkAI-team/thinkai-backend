package com.thinkai.backend.ai.config;

/**
 * Các loại Agent chuyên biệt cho TOEIC/IELTS
 * Thay thế/mở rộng từ AiAgentType hiện có
 */
public enum AgentType {
    // Platform agents (giữ từ hệ thống cũ)
    PLATFORM_LEARNING("platform-learning", "Learning platform operations"),
    PLATFORM_COURSE_OPS("platform-course-ops", "Course management for teachers"),
    PLATFORM_EXAM_OPS("platform-exam-ops", "Exam management for teachers"),

    // TOEIC specialized agents
    TOEIC_READING("toeic-reading", "TOEIC Reading comprehension specialist"),
    TOEIC_LISTENING("toeic-listening", "TOEIC Listening strategies specialist"),
    TOEIC_GRAMMAR("toeic-grammar", "TOEIC grammar patterns specialist"),
    TOEIC_VOCABULARY("toeic-vocabulary", "TOEIC vocabulary building specialist"),

    // IELTS specialized agents
    IELTS_READING("ielts-reading", "IELTS Reading strategies specialist"),
    IELTS_LISTENING("ielts-listening", "IELTS Listening specialist"),
    IELTS_WRITING("ielts-writing", "IELTS Writing Task 1 & 2 specialist"),
    IELTS_SPEAKING("ielts-speaking", "IELTS Speaking mock examiner"),

    // General English agents
    CONVERSATION("conversation", "General English conversation partner"),
    GRAMMAR("grammar", "English grammar teacher"),
    VOCABULARY("vocabulary", "Vocabulary expansion specialist"),
    PRONUNCIATION("pronunciation", "Pronunciation coach"),

    // Special agents
    EXAM_STRATEGY("exam-strategy", "Test-taking strategies for TOEIC/IELTS"),
    MISTAKE_ANALYZER("mistake-analyzer", "Analyze and categorize student mistakes"),
    PROGRESS_TRACKER("progress-tracker", "Track and report learning progress");

    private final String code;
    private final String description;

    AgentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Kiểm tra agent có phải TOEIC specialist không
     */
    public boolean isToeicAgent() {
        return code.startsWith("toeic-");
    }

    /**
     * Kiểm tra agent có phải IELTS specialist không
     */
    public boolean isIeltsAgent() {
        return code.startsWith("ielts-");
    }

    /**
     * Kiểm tra agent có phải platform agent không
     */
    public boolean isPlatformAgent() {
        return code.startsWith("platform-");
    }

    /**
     * Lấy exam type tương ứng
     */
    public String getExamType() {
        if (isToeicAgent()) {
            return "TOEIC";
        }
        if (isIeltsAgent()) {
            return "IELTS";
        }
        return null;
    }
}

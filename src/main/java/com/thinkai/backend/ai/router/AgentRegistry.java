package com.thinkai.backend.ai.router;

import com.thinkai.backend.ai.config.AgentType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry cho các agent với metadata và routing patterns
 * Quản lý keywords, embeddings, và scoring weights
 */
@Component
public class AgentRegistry {

    private final Map<AgentType, AgentMetadata> registry = new EnumMap<>(AgentType.class);

    @PostConstruct
    public void init() {
        // Platform agents
        register(AgentType.PLATFORM_LEARNING, new AgentMetadata(
            Set.of("course", "khóa học", "enroll", "đăng ký", "progress", "tiến độ"),
            Map.of("platform", 1.0, "learning", 0.9),
            0.5 // weight
        ));

        register(AgentType.PLATFORM_COURSE_OPS, new AgentMetadata(
            Set.of("create course", "tạo khóa học", "publish", "xuất bản", "update course"),
            Map.of("platform", 1.0, "course_ops", 0.9),
            0.5
        ));

        register(AgentType.PLATFORM_EXAM_OPS, new AgentMetadata(
            Set.of("create exam", "tạo bài thi", "exam", "bài thi"),
            Map.of("platform", 1.0, "exam_ops", 0.9),
            0.5
        ));

        // TOEIC agents
        register(AgentType.TOEIC_READING, new AgentMetadata(
            Set.of("toeic reading", "đọc toeic", "part 5", "part 6", "part 7",
                   "incomplete sentence", "text completion", "reading comprehension"),
            Map.of("toeic", 1.0, "reading", 0.9, "test_prep", 0.8),
            1.0
        ));

        register(AgentType.TOEIC_LISTENING, new AgentMetadata(
            Set.of("toeic listening", "nghe toeic", "part 1", "part 2", "part 3", "part 4",
                   "photograph", "question response", "conversation", "talk"),
            Map.of("toeic", 1.0, "listening", 0.9, "test_prep", 0.8),
            1.0
        ));

        register(AgentType.TOEIC_GRAMMAR, new AgentMetadata(
            Set.of("toeic grammar", "ngữ pháp toeic", "subject verb", "tenses", "preposition",
                   "conjunction", "article", "word form", "part 5"),
            Map.of("toeic", 1.0, "grammar", 0.9, "test_prep", 0.8),
            0.9
        ));

        register(AgentType.TOEIC_VOCABULARY, new AgentMetadata(
            Set.of("toeic vocabulary", "từ vựng toeic", "business word", "office vocabulary",
                   "collocation", "phrase", "part 5", "part 6"),
            Map.of("toeic", 1.0, "vocabulary", 0.9, "test_prep", 0.8),
            0.9
        ));

        // IELTS agents
        register(AgentType.IELTS_READING, new AgentMetadata(
            Set.of("ielts reading", "đọc ielts", "true false", "matching", "gap fill",
                   "heading", "academic reading", "general training", "skim", "scan"),
            Map.of("ielts", 1.0, "reading", 0.9, "test_prep", 0.8),
            1.0
        ));

        register(AgentType.IELTS_LISTENING, new AgentMetadata(
            Set.of("ielts listening", "nghe ielts", "section 1", "section 2", "section 3", "section 4",
                   "form filling", "map labeling", "multiple choice"),
            Map.of("ielts", 1.0, "listening", 0.9, "test_prep", 0.8),
            1.0
        ));

        register(AgentType.IELTS_WRITING, new AgentMetadata(
            Set.of("ielts writing", "viết ielts", "task 1", "task 2", "essay", "graph", "chart",
                   "process", "map", "band score", "cohesive device"),
            Map.of("ielts", 1.0, "writing", 0.9, "test_prep", 0.8),
            1.0
        ));

        register(AgentType.IELTS_SPEAKING, new AgentMetadata(
            Set.of("ielts speaking", "nói ielts", "part 1", "part 2", "part 3", "cue card",
                   "fluency", "pronunciation", "mock test", "speaking test"),
            Map.of("ielts", 1.0, "speaking", 0.9, "test_prep", 0.8),
            1.0
        ));

        // General English agents
        register(AgentType.GRAMMAR, new AgentMetadata(
            Set.of("grammar", "ngữ pháp", "tense", "part of speech", "sentence structure",
                   "clause", "phrase", "syntax", "error correction"),
            Map.of("grammar", 1.0, "general", 0.9),
            0.8
        ));

        register(AgentType.VOCABULARY, new AgentMetadata(
            Set.of("vocabulary", "từ vựng", "word", "meaning", "synonym", "antonym",
                   "definition", "collocation", "idiom", "phrase"),
            Map.of("vocabulary", 1.0, "general", 0.9),
            0.8
        ));

        register(AgentType.CONVERSATION, new AgentMetadata(
            Set.of("chat", "trò chuyện", "talk", "conversation", "speaking practice",
                   "free talk", "dialogue", "daily conversation"),
            Map.of("conversation", 1.0, "general", 0.9),
            0.7
        ));

        register(AgentType.PRONUNCIATION, new AgentMetadata(
            Set.of("pronunciation", "phát âm", "accent", "intonation", "stress",
                   "phonetic", "ipa", "sound", "linking"),
            Map.of("pronunciation", 1.0, "general", 0.9),
            0.8
        ));

        register(AgentType.EXAM_STRATEGY, new AgentMetadata(
            Set.of("strategy", "chiến lược", "tips", "mẹo", "time management", "test technique",
                   "score", "điểm", "target score"),
            Map.of("strategy", 1.0, "test_prep", 0.9),
            0.8
        ));

        register(AgentType.MISTAKE_ANALYZER, new AgentMetadata(
            Set.of("mistake", "lỗi sai", "error", "wrong", "incorrect", "analyze",
                   "common error", "frequent mistake"),
            Map.of("analysis", 1.0, "general", 0.8),
            0.6
        ));

        register(AgentType.PROGRESS_TRACKER, new AgentMetadata(
            Set.of("progress", "tiến bộ", "improvement", "track", "statistics", "report",
                   "performance", "achievement"),
            Map.of("tracking", 1.0, "general", 0.8),
            0.6
        ));
    }

    private void register(AgentType agentType, AgentMetadata metadata) {
        registry.put(agentType, metadata);
    }

    public Optional<AgentMetadata> get(AgentType agentType) {
        return Optional.ofNullable(registry.get(agentType));
    }

    public Set<AgentType> getAllAgents() {
        return registry.keySet();
    }

    public Map<AgentType, AgentMetadata> getAll() {
        return Collections.unmodifiableMap(registry);
    }

    /**
     * Get agents by category (toeic/ielts/general)
     */
    public List<AgentType> getByCategory(String category) {
        return registry.entrySet().stream()
            .filter(e -> e.getValue().tags().containsKey(category))
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Agent metadata
     */
    public record AgentMetadata(
        Set<String> keywords,           // Keywords for matching
        Map<String, Double> tags,       // Category tags with weights
        double routingWeight          // Weight in hybrid scoring
    ) {}
}

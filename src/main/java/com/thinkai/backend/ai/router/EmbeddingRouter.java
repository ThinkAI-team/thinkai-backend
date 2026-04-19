package com.thinkai.backend.ai.router;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.embedding.EmbeddingService;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Embedding-based Router
 * Định tuyến dựa trên semantic similarity với pre-defined queries
 */
@Component
@SuppressWarnings("checkstyle:ConstantName")
public class EmbeddingRouter {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingRouter.class);

    private final EmbeddingService embeddingService;
    private final AgentRegistry agentRegistry;

    // Pre-computed embeddings cho các queries mẫu
    private final Map<AgentType, List<String>> agentQueries = new EnumMap<>(AgentType.class);
    private final Map<String, List<Double>> queryEmbeddings = new HashMap<>();

    // Threshold cho similarity
    private static final double SIMILARITY_THRESHOLD = 0.7;

    public EmbeddingRouter(EmbeddingService embeddingService, AgentRegistry agentRegistry) {
        this.embeddingService = embeddingService;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void init() {
        // Initialize sample queries cho mỗi agent type
        initializeAgentQueries();

        // Pre-compute embeddings
        computeEmbeddings();

        logger.info("EmbeddingRouter initialized with {} agent types", agentQueries.size());
    }

    /**
     * Route request using embedding similarity
     */
    public List<RoutingScore> route(AiHarnessRequest request) {
        String message = request.message().trim().toLowerCase();

        // Generate embedding cho input
        List<Double> queryEmbedding = embeddingService.embed(message);

        if (queryEmbedding.isEmpty()) {
            return List.of();
        }

        Map<AgentType, Double> agentScores = new EnumMap<>(AgentType.class);

        // Calculate similarity với tất cả sample queries
        for (Map.Entry<String, List<Double>> entry : queryEmbeddings.entrySet()) {
            String queryKey = entry.getKey();
            List<Double> candidateEmbedding = entry.getValue();

            double similarity = embeddingService.cosineSimilarity(queryEmbedding, candidateEmbedding);

            // Parse agent từ query key (format: "AGENT_TYPE:sample_query")
            AgentType agent = parseAgentFromKey(queryKey);
            if (agent != null) {
                agentScores.merge(agent, similarity, Math::max);
            }
        }

        // Convert to list and sort
        List<RoutingScore> scores = new ArrayList<>();
        for (Map.Entry<AgentType, Double> entry : agentScores.entrySet()) {
            if (entry.getValue() >= SIMILARITY_THRESHOLD) {
                scores.add(new RoutingScore(
                    entry.getKey(),
                    entry.getValue(),
                    "embedding_similarity"
                ));
            }
        }

        scores.sort((a, b) -> Double.compare(b.score(), a.score()));

        logger.debug("Embedding routing for '{}': top agent={}, score={}",
            request.message().substring(0, Math.min(50, request.message().length())),
            scores.isEmpty() ? "none" : scores.get(0).agent(),
            scores.isEmpty() ? 0 : String.format("%.3f", scores.get(0).score()));

        return scores;
    }

    /**
     * Get top agent from embedding routing
     */
    public Optional<RoutingScore> getTopAgent(AiHarnessRequest request) {
        List<RoutingScore> scores = route(request);
        return scores.isEmpty() ? Optional.empty() : Optional.of(scores.get(0));
    }

    /**
     * Initialize sample queries cho mỗi agent
     */
    private void initializeAgentQueries() {
        // TOEIC queries
        agentQueries.put(AgentType.TOEIC_READING, List.of(
            "help me with toeic reading part 5",
            "how to solve incomplete sentences",
            "reading comprehension strategies",
            "toeic reading practice",
            "đọc hiểu toeic part 7",
            "làm bài đọc toeic"
        ));

        agentQueries.put(AgentType.TOEIC_LISTENING, List.of(
            "toeic listening practice",
            "how to improve listening score",
            "part 3 conversation tips",
            "listening strategies for toeic",
            "nghe toeic part 1",
            "luyện nghe toeic"
        ));

        agentQueries.put(AgentType.TOEIC_GRAMMAR, List.of(
            "toeic grammar questions",
            "subject verb agreement rules",
            "tenses in toeic",
            "prepositions for toeic",
            "ngữ pháp toeic",
            "cấu trúc ngữ pháp thi toeic"
        ));

        agentQueries.put(AgentType.TOEIC_VOCABULARY, List.of(
            "toeic vocabulary list",
            "business words for toeic",
            "office vocabulary",
            "common collocations",
            "từ vựng toeic thông dụng",
            "học từ vựng toeic"
        ));

        // IELTS queries
        agentQueries.put(AgentType.IELTS_READING, List.of(
            "ielts reading tips",
            "skimming and scanning",
            "true false not given",
            "matching headings",
            "đọc ielts academic",
            "chiến thuật đọc ielts"
        ));

        agentQueries.put(AgentType.IELTS_LISTENING, List.of(
            "ielts listening section",
            "note taking strategies",
            "form filling listening",
            "multiple choice listening",
            "nghe ielts section 1",
            "luyện nghe ielts"
        ));

        agentQueries.put(AgentType.IELTS_WRITING, List.of(
            "ielts writing task 1",
            "ielts writing task 2",
            "essay structure",
            "describe a graph",
            "viết ielts task 1",
            "bài viết ielts band 7"
        ));

        agentQueries.put(AgentType.IELTS_SPEAKING, List.of(
            "ielts speaking practice",
            "speaking part 2 cue card",
            "fluency and coherence",
            "pronunciation tips",
            "luyện nói ielts",
            "mock speaking test"
        ));

        // General English queries
        agentQueries.put(AgentType.GRAMMAR, List.of(
            "english grammar help",
            "present perfect tense",
            "conditional sentences",
            "passive voice",
            "ngữ pháp tiếng anh",
            "câu điều kiện"
        ));

        agentQueries.put(AgentType.VOCABULARY, List.of(
            "learn new words",
            "synonyms and antonyms",
            "word definitions",
            "expand vocabulary",
            "học từ vựng tiếng anh",
            "từ đồng nghĩa"
        ));

        agentQueries.put(AgentType.CONVERSATION, List.of(
            "practice speaking",
            "daily conversation",
            "chat in english",
            "free talk practice",
            "trò chuyện tiếng anh",
            "luyện nói hàng ngày"
        ));

        agentQueries.put(AgentType.PRONUNCIATION, List.of(
            "pronunciation guide",
            "english sounds",
            "intonation patterns",
            "stress in words",
            "phát âm tiếng anh",
            "trọng âm từ"
        ));

        // Strategy agents
        agentQueries.put(AgentType.EXAM_STRATEGY, List.of(
            "test taking strategies",
            "time management tips",
            "exam preparation",
            "score improvement",
            "chiến thuật thi",
            "mẹo làm bài thi"
        ));
    }

    /**
     * Pre-compute embeddings cho tất cả sample queries
     */
    private void computeEmbeddings() {
        for (Map.Entry<AgentType, List<String>> entry : agentQueries.entrySet()) {
            AgentType agent = entry.getKey();
            List<String> queries = entry.getValue();

            for (String query : queries) {
                String key = agent + ":" + query;
                List<Double> embedding = embeddingService.embed(query);
                queryEmbeddings.put(key, embedding);
            }
        }
    }

    /**
     * Parse agent type từ query key
     */
    private AgentType parseAgentFromKey(String key) {
        int separatorIndex = key.indexOf(':');
        if (separatorIndex < 0) {
            return null;
        }

        String agentName = key.substring(0, separatorIndex);
        try {
            return AgentType.valueOf(agentName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Routing score record
     */
    public record RoutingScore(
        AgentType agent,
        double score,
        String reason
    ) {}
}

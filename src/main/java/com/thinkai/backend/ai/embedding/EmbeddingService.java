package com.thinkai.backend.ai.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Embedding Service cho semantic similarity
 * Sử dụng OpenRouter API hoặc local embedding model
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    private final RestClient restClient;

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.api.url:}")
    private String apiUrl;

    @Value("${ai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    // Cache để tránh duplicate calls
    private final Map<String, List<Double>> embeddingCache = new java.util.concurrent.ConcurrentHashMap<>();

    public EmbeddingService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Generate embedding cho text using OpenAI API
     *
     * @param text Input text
     * @return Embedding vector (1536 dimensions cho OpenAI)
     */
    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.trim().toLowerCase();

        // Check cache
        if (embeddingCache.containsKey(normalized)) {
            return embeddingCache.get(normalized);
        }

        // Call actual embedding API
        try {
            List<Double> embedding = callEmbeddingApi(normalized);
            embeddingCache.put(normalized, embedding);
            return embedding;
        } catch (Exception e) {
            logger.error("Embedding API call failed: {}", e.getMessage());
            // Fallback: generate deterministic embedding based on text hash
            return generateHashBasedEmbedding(normalized);
        }
    }

    /**
     * Call OpenAI Embedding API
     */
    private List<Double> callEmbeddingApi(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenRouter API key not configured");
        }

        Map<String, Object> requestBody = Map.of(
            "model", embeddingModel,
            "input", text
        );

        try {
            Map<?, ?> response = restClient.post()
                .uri(apiUrl.replace("/chat/completions", "/embeddings"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty() && data.get(0).containsKey("embedding")) {
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    logger.debug("Generated embedding with {} dimensions", embedding.size());
                    return embedding;
                }
            }
            throw new RuntimeException("Invalid embedding response");
        } catch (Exception e) {
            logger.error("Failed to call embedding API: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Generate deterministic embedding based on text hash (fallback)
     */
    private List<Double> generateHashBasedEmbedding(String text) {
        // Tạo pseudo-random embedding dựa trên hash của text
        int size = 128;
        List<Double> embedding = new ArrayList<>(size);

        int hash = text.hashCode();
        java.util.Random random = new java.util.Random(hash);

        for (int i = 0; i < size; i++) {
            embedding.add(random.nextGaussian() * 0.1);
        }

        // Normalize
        double norm = 0.0;
        for (double v : embedding) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < size; i++) {
                embedding.set(i, embedding.get(i) / norm);
            }
        }

        return embedding;
    }

    /**
     * Calculate cosine similarity giữa 2 embeddings
     */
    public double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        if (a.size() != b.size()) {
            logger.warn("Embedding size mismatch: {} vs {}", a.size(), b.size());
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Find k-nearest neighbors
     */
    public List<SimilarityResult> findNearest(List<Double> query, Map<String, List<Double>> candidates, int k) {
        List<SimilarityResult> results = new ArrayList<>();

        for (Map.Entry<String, List<Double>> entry : candidates.entrySet()) {
            double similarity = cosineSimilarity(query, entry.getValue());
            if (similarity > 0) {
                results.add(new SimilarityResult(entry.getKey(), similarity));
            }
        }

        results.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));

        return results.subList(0, Math.min(k, results.size()));
    }


    /**
     * Similarity result record
     */
    public record SimilarityResult(
        String key,
        double similarity
    ) {}
}

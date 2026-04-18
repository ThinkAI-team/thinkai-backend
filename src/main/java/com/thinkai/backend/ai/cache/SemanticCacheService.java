package com.thinkai.backend.ai.cache;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.embedding.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SemanticCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmbeddingService embeddingService;
    private final CacheKeyGenerator keyGenerator;
    private final CacheMetrics metrics;

    private static final double SIMILARITY_THRESHOLD = 0.85;
    private static final Duration DEFAULT_TTL = Duration.ofHours(2);
    private static final int MAX_CACHED_QUERIES = 50;

    public SemanticCacheService(
            RedisTemplate<String, Object> redisTemplate,
            EmbeddingService embeddingService,
            CacheKeyGenerator keyGenerator,
            CacheMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.embeddingService = embeddingService;
        this.keyGenerator = keyGenerator;
        this.metrics = metrics;
    }

    public Optional<CachedResponse> get(String query, AgentType agent, Long userId) {
        try {
            String key = keyGenerator.generateKey(agent, userId);
            List<Object> cachedQueries = getCachedQueries(key);
            
            if (cachedQueries == null || cachedQueries.isEmpty()) {
                metrics.recordMiss();
                return Optional.empty();
            }

            double[] queryEmbedding = toDoubleArray(embeddingService.embed(query));
            
            CachedResponse bestMatch = null;
            double bestSimilarity = 0;
            
            for (Object item : cachedQueries) {
                if (item instanceof Map) {
                    Map<String, Object> entry = (Map<String, Object>) item;
                    String cachedQuery = (String) entry.get("query");
                    Object embeddingObj = entry.get("embedding");
                    
                    if (cachedQuery != null && embeddingObj != null) {
                        double[] cachedEmbedding = convertEmbedding(embeddingObj);
                        double similarity = cosineSimilarity(queryEmbedding, cachedEmbedding);
                        
                        if (similarity > SIMILARITY_THRESHOLD && similarity > bestSimilarity) {
                            bestSimilarity = similarity;
                            bestMatch = new CachedResponse(
                                (String) entry.get("response"),
                                similarity,
                                ((Number) entry.get("timestamp")).longValue(),
                                cachedQuery
                            );
                        }
                    }
                }
            }
            
            if (bestMatch != null) {
                metrics.recordHit();
                log.debug("Cache hit: agent={}, similarity={}", agent, bestSimilarity);
                return Optional.of(bestMatch);
            } else {
                metrics.recordMiss();
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Cache get error: {}", e.getMessage());
            metrics.recordError();
            return Optional.empty();
        }
    }

    public void put(String query, String response, AgentType agent, Long userId) {
        try {
            String key = keyGenerator.generateKey(agent, userId);
            double[] embedding = toDoubleArray(embeddingService.embed(query));
            
            Map<String, Object> entry = new HashMap<>();
            entry.put("query", query);
            entry.put("response", response);
            entry.put("embedding", embedding);
            entry.put("timestamp", System.currentTimeMillis());
            
            List<Object> cachedQueries = getCachedQueries(key);
            if (cachedQueries == null) {
                cachedQueries = new ArrayList<>();
            }
            
            cachedQueries.add(entry);
            
            while (cachedQueries.size() > MAX_CACHED_QUERIES) {
                cachedQueries.remove(0);
            }
            
            redisTemplate.opsForValue().set(key, cachedQueries, DEFAULT_TTL);
            metrics.recordSave();
            
            log.debug("Cache saved: agent={}, query={}", agent, query.substring(0, Math.min(30, query.length())));
            
        } catch (Exception e) {
            log.error("Cache put error: {}", e.getMessage());
            metrics.recordError();
        }
    }

    public void invalidate(AgentType agent, Long userId) {
        try {
            String key = keyGenerator.generateKey(agent, userId);
            redisTemplate.delete(key);
            metrics.recordInvalidate();
            log.debug("Cache invalidated: agent={}, userId={}", agent, userId);
        } catch (Exception e) {
            log.error("Cache invalidate error: {}", e.getMessage());
        }
    }

    public void invalidateAll(Long userId) {
        try {
            Set<String> keys = redisTemplate.keys("cache:*:" + userId);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                metrics.recordInvalidate();
                log.debug("All cache invalidated for userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Cache invalidate all error: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> getCachedQueries(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return null;
    }

    private double[] convertEmbedding(Object embeddingObj) {
        if (embeddingObj instanceof double[]) {
            return (double[]) embeddingObj;
        } else if (embeddingObj instanceof float[]) {
            float[] fEmbed = (float[]) embeddingObj;
            double[] dEmbed = new double[fEmbed.length];
            for (int i = 0; i < fEmbed.length; i++) {
                dEmbed[i] = fEmbed[i];
            }
            return dEmbed;
        } else if (embeddingObj instanceof List) {
            List<Number> list = (List<Number>) embeddingObj;
            double[] dEmbed = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                dEmbed[i] = list.get(i).doubleValue();
            }
            return dEmbed;
        }
        return new double[0];
    }

    private double[] toDoubleArray(List<Double> list) {
        if (list == null || list.isEmpty()) {
            return new double[0];
        }
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length || a.length == 0) {
            return 0;
        }
        
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0 : dotProduct / denominator;
    }

    public record CachedResponse(
        String response,
        double similarity,
        long timestamp,
        String originalQuery
    ) {}
}

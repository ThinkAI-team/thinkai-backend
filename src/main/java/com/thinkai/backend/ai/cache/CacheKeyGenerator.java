package com.thinkai.backend.ai.cache;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class CacheKeyGenerator {

    public String generateKey(com.thinkai.backend.ai.config.AgentType agent, Long userId) {
        String raw = String.format("cache:%s:%d", agent.getCode(), userId);
        return hash(raw);
    }

    public String generateKey(com.thinkai.backend.ai.config.AgentType agent, Long userId, String contextHash) {
        String raw = String.format("cache:%s:%d:%s", agent.getCode(), userId, contextHash);
        return hash(raw);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return input;
        }
    }
}

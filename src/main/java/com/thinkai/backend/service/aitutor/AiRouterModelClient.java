package com.thinkai.backend.service.aitutor;

@FunctionalInterface
public interface AiRouterModelClient {
    String complete(String systemPrompt, String userPrompt);
}


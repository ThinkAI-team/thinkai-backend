package com.thinkai.backend.ai.tool;

import java.util.Map;

public record ToolCall(
    String toolName,
    Map<String, Object> arguments,
    String requestId
) {}

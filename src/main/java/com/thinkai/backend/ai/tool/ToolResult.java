package com.thinkai.backend.ai.tool;

import java.util.Map;

public record ToolResult(
    String toolName,
    boolean success,
    String output,
    Map<String, Object> data,
    String error
) {
    public static ToolResult success(String toolName, String output, Map<String, Object> data) {
        return new ToolResult(toolName, true, output, data, null);
    }

    public static ToolResult error(String toolName, String error) {
        return new ToolResult(toolName, false, null, null, error);
    }
}

package com.thinkai.backend.ai.tool;

import com.thinkai.backend.ai.config.AgentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new HashMap<>();

    public ToolRegistry() {
        registerDefaultTools();
    }

    private void registerDefaultTools() {
        register(ToolDefinition.readOnly(
            "get_user_level",
            "Get user's current English level (A1, A2, B1, B2, C1, C2)",
            AgentType.PLATFORM_LEARNING,
            List.of(new ToolDefinition.ToolParameter("userId", "long", "User ID", true))
        ));

        register(ToolDefinition.readOnly(
            "get_user_progress",
            "Get user's learning progress including completed lessons and courses",
            AgentType.PLATFORM_LEARNING,
            List.of(new ToolDefinition.ToolParameter("userId", "long", "User ID", true))
        ));

        register(ToolDefinition.readOnly(
            "search_lessons",
            "Search for lessons by keyword or topic",
            AgentType.PLATFORM_LEARNING,
            List.of(
                new ToolDefinition.ToolParameter("query", "string", "Search keyword", true),
                new ToolDefinition.ToolParameter("limit", "int", "Maximum results", false)
            )
        ));

        register(ToolDefinition.readOnly(
            "get_course_info",
            "Get information about a specific course",
            AgentType.PLATFORM_LEARNING,
            List.of(new ToolDefinition.ToolParameter("courseId", "long", "Course ID", true))
        ));

        register(ToolDefinition.readOnly(
            "get_exam_info",
            "Get information about a specific exam",
            AgentType.PLATFORM_EXAM_OPS,
            List.of(new ToolDefinition.ToolParameter("examId", "long", "Exam ID", true))
        ));

        register(ToolDefinition.readOnly(
            "get_user_exam_history",
            "Get user's past exam attempts and scores",
            AgentType.PLATFORM_EXAM_OPS,
            List.of(
                new ToolDefinition.ToolParameter("userId", "long", "User ID", true),
                new ToolDefinition.ToolParameter("examId", "long", "Exam ID", false)
            )
        ));

        register(ToolDefinition.readOnly(
            "search_vocabulary",
            "Search vocabulary from the word bank",
            AgentType.VOCABULARY,
            List.of(new ToolDefinition.ToolParameter("word", "string", "Word to search", true))
        ));

        register(ToolDefinition.readOnly(
            "get_user_vocab_progress",
            "Get user's vocabulary learning progress",
            AgentType.VOCABULARY,
            List.of(new ToolDefinition.ToolParameter("userId", "long", "User ID", true))
        ));

        register(ToolDefinition.readOnly(
            "get_grammar_topic",
            "Get grammar explanation for a specific topic",
            AgentType.GRAMMAR,
            List.of(new ToolDefinition.ToolParameter("topic", "string", "Grammar topic", true))
        ));

        register(ToolDefinition.mutating(
            "start_lesson",
            "Start a new lesson for the user",
            AgentType.PLATFORM_COURSE_OPS,
            List.of(
                new ToolDefinition.ToolParameter("userId", "long", "User ID", true),
                new ToolDefinition.ToolParameter("lessonId", "long", "Lesson ID", true)
            )
        ));

        register(ToolDefinition.mutating(
            "complete_lesson",
            "Mark a lesson as completed",
            AgentType.PLATFORM_COURSE_OPS,
            List.of(
                new ToolDefinition.ToolParameter("userId", "long", "User ID", true),
                new ToolDefinition.ToolParameter("lessonId", "long", "Lesson ID", true),
                new ToolDefinition.ToolParameter("score", "int", "Lesson score", false)
            )
        ));
    }

    private void register(ToolDefinition tool) {
        tools.put(tool.name(), tool);
    }

    public Optional<ToolDefinition> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<ToolDefinition> getToolsForAgent(AgentType agent) {
        return tools.values().stream()
            .filter(t -> t.targetAgent() == agent)
            .toList();
    }

    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    public List<ToolDefinition> getToolsForMessage(String message, AgentType currentAgent) {
        String lowerMessage = message.toLowerCase();
        List<ToolDefinition> result = new ArrayList<>();

        if (lowerMessage.contains("level") || lowerMessage.contains("trình độ")) {
            getTool("get_user_level").ifPresent(result::add);
        }
        if (lowerMessage.contains("progress") || lowerMessage.contains("tiến độ") || lowerMessage.contains("completed")) {
            getTool("get_user_progress").ifPresent(result::add);
        }
        if (lowerMessage.contains("search") || lowerMessage.contains("tìm")) {
            getTool("search_lessons").ifPresent(result::add);
        }
        if (lowerMessage.contains("course") || lowerMessage.contains("khóa học")) {
            getTool("get_course_info").ifPresent(result::add);
        }
        if (lowerMessage.contains("exam") || lowerMessage.contains("thi")) {
            getTool("get_exam_info").ifPresent(result::add);
            getTool("get_user_exam_history").ifPresent(result::add);
        }
        if (lowerMessage.contains("vocabulary") || lowerMessage.contains("từ vựng")) {
            getTool("search_vocabulary").ifPresent(result::add);
            getTool("get_user_vocab_progress").ifPresent(result::add);
        }
        if (lowerMessage.contains("grammar") || lowerMessage.contains("ngữ pháp")) {
            getTool("get_grammar_topic").ifPresent(result::add);
        }
        if (lowerMessage.contains("start") || lowerMessage.contains("bắt đầu")) {
            getTool("start_lesson").ifPresent(result::add);
        }
        if (lowerMessage.contains("complete") || lowerMessage.contains("hoàn thành")) {
            getTool("complete_lesson").ifPresent(result::add);
        }

        return result;
    }
}

package com.thinkai.backend.service.aitutor;

import com.thinkai.backend.entity.User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AiToolRegistryService {

    private final Map<String, AiToolDefinition> registry;

    public AiToolRegistryService() {
        Map<String, AiToolDefinition> map = new LinkedHashMap<>();

        register(map, AiToolDefinition.readOnly("none", AiAgentType.TUTOR, Set.of(
                User.Role.STUDENT, User.Role.TEACHER, User.Role.ADMIN)));

        register(map, AiToolDefinition.readOnly("list_shop_courses", AiAgentType.LEARNING, Set.of(
                User.Role.STUDENT, User.Role.TEACHER, User.Role.ADMIN)));
        register(map, AiToolDefinition.readOnly("search_shop_courses", AiAgentType.LEARNING, Set.of(
                User.Role.STUDENT, User.Role.TEACHER, User.Role.ADMIN)));
        register(map, AiToolDefinition.readOnly("get_course_detail", AiAgentType.LEARNING, Set.of(
                User.Role.STUDENT, User.Role.TEACHER, User.Role.ADMIN)));
        register(map, AiToolDefinition.readOnly("list_course_lessons", AiAgentType.LEARNING, Set.of(
                User.Role.STUDENT, User.Role.TEACHER, User.Role.ADMIN)));
        register(map, AiToolDefinition.readOnly("list_my_courses", AiAgentType.LEARNING, Set.of(
                User.Role.STUDENT, User.Role.TEACHER, User.Role.ADMIN)));
        register(map, AiToolDefinition.readOnly("list_my_learning_progress", AiAgentType.LEARNING, Set.of(
                User.Role.STUDENT, User.Role.TEACHER, User.Role.ADMIN)));
        register(map, AiToolDefinition.readOnly("list_my_exams", AiAgentType.EXAM_OPS, Set.of(
                User.Role.STUDENT, User.Role.TEACHER, User.Role.ADMIN)));

        register(map, AiToolDefinition.mutating("enroll_course", AiAgentType.LEARNING, 
                Set.of(User.Role.STUDENT), true, List.of("courseId")));
        
        register(map, AiToolDefinition.mutating("create_course", AiAgentType.COURSE_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("title", "description")));
        register(map, AiToolDefinition.mutatingWithOwnership("update_course", AiAgentType.COURSE_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("courseId"), "courseId"));
        register(map, AiToolDefinition.mutatingWithOwnership("delete_course", AiAgentType.COURSE_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("courseId"), "courseId"));
        register(map, AiToolDefinition.mutating("publish_course", AiAgentType.COURSE_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("courseId")));
        
        register(map, AiToolDefinition.mutating("create_lesson", AiAgentType.COURSE_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("courseId", "title")));
        register(map, AiToolDefinition.mutatingWithOwnership("update_lesson", AiAgentType.COURSE_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("lessonId"), "lessonId"));
        register(map, AiToolDefinition.mutatingWithOwnership("delete_lesson", AiAgentType.COURSE_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("lessonId"), "lessonId"));
        
        register(map, AiToolDefinition.mutating("create_exam", AiAgentType.EXAM_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("courseId", "title")));
        register(map, AiToolDefinition.mutatingWithOwnership("update_exam", AiAgentType.EXAM_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("examId"), "examId"));
        register(map, AiToolDefinition.mutatingWithOwnership("delete_exam", AiAgentType.EXAM_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("examId"), "examId"));

        register(map, AiToolDefinition.readOnly("list_teacher_courses", AiAgentType.COURSE_OPS, Set.of(
                User.Role.TEACHER, User.Role.ADMIN)));
        register(map, AiToolDefinition.readOnly("get_course_analytics", AiAgentType.COURSE_OPS, Set.of(
                User.Role.TEACHER, User.Role.ADMIN)));

        register(map, AiToolDefinition.mutating("create_question", AiAgentType.EXAM_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("questionText", "correctAnswer")));
        register(map, AiToolDefinition.mutating("bulk_import_questions", AiAgentType.EXAM_OPS, 
                Set.of(User.Role.TEACHER, User.Role.ADMIN), true, List.of("courseId", "questions")));

        this.registry = Collections.unmodifiableMap(map);
    }

    private void register(Map<String, AiToolDefinition> map, AiToolDefinition definition) {
        map.put(definition.action(), definition);
    }

    public Optional<AiToolDefinition> find(String action) {
        if (action == null || action.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.get(action.trim().toLowerCase(Locale.ROOT)));
    }

    public boolean isKnownAction(String action) {
        return find(action).isPresent();
    }

    public Set<String> knownActions() {
        return registry.keySet();
    }
}
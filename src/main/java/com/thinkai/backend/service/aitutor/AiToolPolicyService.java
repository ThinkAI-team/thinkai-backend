package com.thinkai.backend.service.aitutor;

import com.thinkai.backend.entity.User;
import org.springframework.stereotype.Service;

@Service
public class AiToolPolicyService {

    private final AiToolRegistryService aiToolRegistryService;

    public AiToolPolicyService(AiToolRegistryService aiToolRegistryService) {
        this.aiToolRegistryService = aiToolRegistryService;
    }

    public String denyReason(User user, String action) {
        if (user == null || action == null || action.isBlank() || "none".equalsIgnoreCase(action)) {
            return null;
        }

        AiToolDefinition definition = aiToolRegistryService.find(action).orElse(null);
        if (definition == null) {
            return "Hành động không hợp lệ hoặc chưa được hỗ trợ.";
        }

        if (!definition.isAllowed(user.getRole())) {
            return switch (action) {
                case "enroll_course" -> "Chỉ STUDENT mới có thể đăng ký khóa học.";
                case "create_course", "publish_course", "create_lesson", "create_exam",
                     "update_course", "delete_course", "update_lesson", "delete_lesson",
                     "update_exam", "delete_exam", "create_question", "bulk_import_questions" ->
                        "Bạn cần quyền TEACHER hoặc ADMIN để thực hiện thao tác này.";
                case "list_teacher_courses", "get_course_analytics" ->
                        "Bạn cần quyền TEACHER hoặc ADMIN để xem thông tin này.";
                default -> "Bạn không có quyền thực hiện thao tác này.";
            };
        }

        return null;
    }

    public String ownershipDenyReason(User user, String action, String ownershipField, Object ownershipValue) {
        if (user.getRole() == User.Role.ADMIN) {
            return null;
        }
        
        if (ownershipValue == null) {
            return null;
        }

        return switch (action) {
            case "update_course", "delete_course" -> 
                "Bạn không sở hữu khóa học này.";
            case "update_lesson", "delete_lesson" ->
                "Bạn không sở hữu bài học này.";
            case "update_exam", "delete_exam" ->
                "Bạn không sở hữu bài thi này.";
            default -> null;
        };
    }
}
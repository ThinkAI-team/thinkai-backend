package com.thinkai.backend.ai.orchestrator;

import com.thinkai.backend.ai.config.AgentType;
import org.springframework.stereotype.Component;

/**
 * Validator Interface - Phase 6 implement
 * Kiểm tra quality của response
 */
@Component
public interface OrchestratorValidator {

    /**
     * Validate response theo agent type rules
     *
     * @param response Raw LLM response
     * @param agent Agent type
     * @return Validated response (có thể sửa đổi)
     * @throws ValidationException nếu không đạt requirements
     */
    String validate(String response, AgentType agent) throws ValidationException;

    /**
     * Validate với detailed result
     */
    ValidationResult validateDetailed(String response, AgentType agent);

    /**
     * Check nhanh (không throw exception)
     */
    boolean isValid(String response, AgentType agent);

    class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    record ValidationResult(
        boolean valid,
        String validatedResponse,
        java.util.List<String> errors,
        java.util.List<String> warnings
    ) {
        public ValidationResult {
            if (errors == null) errors = java.util.List.of();
            if (warnings == null) warnings = java.util.List.of();
        }
    }
}

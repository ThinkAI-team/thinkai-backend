package com.thinkai.backend.ai.orchestrator.impl;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.orchestrator.OrchestratorValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 6: Validator Implementation
 * Kiểm tra quality của response
 */
@Service
public class OrchestratorValidatorImpl implements OrchestratorValidator {

    @Override
    public String validate(String response, AgentType agent) throws ValidationException {
        ValidationResult result = validateDetailed(response, agent);
        if (!result.valid()) {
            throw new ValidationException(String.join(", ", result.errors()));
        }
        return result.validatedResponse();
    }

    @Override
    public ValidationResult validateDetailed(String response, AgentType agent) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check empty
        if (response == null || response.isBlank()) {
            errors.add("Response cannot be empty");
            return new ValidationResult(false, response, errors, warnings);
        }

        // Check min length
        if (response.length() < 10) {
            errors.add("Response too short (min 10 chars)");
        }

        // Agent-specific validation
        switch (agent) {
            case TOEIC_READING, TOEIC_LISTENING, IELTS_READING, IELTS_LISTENING -> {
                // Reading/Listening should have explanations
                if (!response.toLowerCase().contains("explanation") &&
                    !response.toLowerCase().contains("because")) {
                    warnings.add("Should include explanation for answers");
                }
            }
            case IELTS_WRITING -> {
                // Writing should have structure
                if (!response.contains("Introduction") && !response.contains("Body")) {
                    warnings.add("Should have clear structure");
                }
            }
            case IELTS_SPEAKING -> {
                // Speaking should have natural flow
                if (response.length() < 50) {
                    warnings.add("Speaking response should be conversational");
                }
            }
        }

        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, response, errors, warnings);
    }

    @Override
    public boolean isValid(String response, AgentType agent) {
        try {
            validate(response, agent);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }
}

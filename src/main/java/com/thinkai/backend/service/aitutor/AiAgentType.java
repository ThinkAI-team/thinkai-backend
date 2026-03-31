package com.thinkai.backend.service.aitutor;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AiAgentType {
    TUTOR,
    LEARNING,
    COURSE_OPS,
    EXAM_OPS,
    SAFETY_POLICY;

    @JsonValue
    public String toJson() {
        return this.name();
    }
}


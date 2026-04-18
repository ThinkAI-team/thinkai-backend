package com.thinkai.backend.ai.exception;

import com.thinkai.backend.ai.state.AiState;

/**
 * Exception cho AI Harness System
 */
public class AiHarnessException extends RuntimeException {

    private final String traceId;
    private final AiState errorState;
    private final String errorCode;

    public AiHarnessException(String message) {
        super(message);
        this.traceId = null;
        this.errorState = AiState.ERROR;
        this.errorCode = "UNKNOWN";
    }

    public AiHarnessException(String message, String traceId, AiState errorState, String errorCode) {
        super(message);
        this.traceId = traceId;
        this.errorState = errorState;
        this.errorCode = errorCode;
    }

    public AiHarnessException(String message, Throwable cause) {
        super(message, cause);
        this.traceId = null;
        this.errorState = AiState.ERROR;
        this.errorCode = "UNKNOWN";
    }

    public String getTraceId() {
        return traceId;
    }

    public AiState getErrorState() {
        return errorState;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Error codes
     */
    public static final String ROUTING_ERROR = "ROUTING_ERROR";
    public static final String CACHE_ERROR = "CACHE_ERROR";
    public static final String CONTEXT_ERROR = "CONTEXT_ERROR";
    public static final String LLM_ERROR = "LLM_ERROR";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String CRITIC_ERROR = "CRITIC_ERROR";
    public static final String MEMORY_ERROR = "MEMORY_ERROR";
}

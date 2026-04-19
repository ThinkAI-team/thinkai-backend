package com.thinkai.backend.ai.orchestrator;

import com.thinkai.backend.ai.config.AgentConfig;
import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import com.thinkai.backend.ai.state.AiHarnessResponse;
import com.thinkai.backend.ai.state.AiState;
import com.thinkai.backend.ai.state.AiStateTransition;
import com.thinkai.backend.ai.observability.TraceLogger;
import com.thinkai.backend.service.aitutor.AiAgentTraceService;
import com.thinkai.backend.service.aitutor.AiAgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core Orchestrator cho AI Harness English Learning System
 * Dieu phoi flow tu request -> response qua state machine
 *
 * Flow: START -> ROUTED -> CACHE_CHECK -> CONTEXT_BUILT -> LLM_CALLED ->
 *       [TOOL_CALLED] -> VALIDATED -> [CRITIC_CHECK] -> DONE
 */
@SuppressWarnings("checkstyle:ConstantName")
@Component
public class AiOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AiOrchestrator.class);
    private static final int MAX_TOOL_CYCLES = 3;

    // Dependencies (se inject o cac phase sau)
    private final OrchestratorRouter router;
    private final OrchestratorCache cache;
    private final OrchestratorContextBuilder contextBuilder;
    private final OrchestratorLLM llmService;
    private final OrchestratorValidator validator;
    private final OrchestratorCritic critic;
    private final OrchestratorMemory memory;
    private final OrchestratorObservability observability;
    private final com.thinkai.backend.ai.tool.ToolExecutor toolExecutor;
    private final AiAgentTraceService traceService;
    private final TraceLogger traceLogger;

    public AiOrchestrator(
            OrchestratorRouter router,
            OrchestratorCache cache,
            OrchestratorContextBuilder contextBuilder,
            OrchestratorLLM llmService,
            OrchestratorValidator validator,
            OrchestratorCritic critic,
            OrchestratorMemory memory,
            OrchestratorObservability observability,
            com.thinkai.backend.ai.tool.ToolExecutor toolExecutor,
            AiAgentTraceService traceService,
            TraceLogger traceLogger) {
        this.router = router;
        this.cache = cache;
        this.contextBuilder = contextBuilder;
        this.llmService = llmService;
        this.validator = validator;
        this.critic = critic;
        this.memory = memory;
        this.observability = observability;
        this.toolExecutor = toolExecutor;
        this.traceService = traceService;
        this.traceLogger = traceLogger;
    }

    /**
     * Main entry point - xu ly request qua state machine
     */
    public AiHarnessResponse handle(AiHarnessRequest request) {
        String traceId = request.traceId() != null ? request.traceId() : UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        List<AiStateTransition> transitions = new ArrayList<>();
        List<AiHarnessResponse.ThinkingStep> thinkingSteps = new ArrayList<>();
        Map<String, Object> contextHints = new HashMap<>();
        List<Map<String, Object>> runGraph = new ArrayList<>();
        contextHints.put("runGraph", runGraph);
        AiState currentState = AiState.START;
        long stepStartTime = System.currentTimeMillis();

        // Record start
        transitions.add(AiStateTransition.start(traceId));
        traceLogger.startTrace(
            traceId,
            request.userId() != null ? request.userId().toString() : "anonymous",
            request.message());

        try {
            logger.info("[{}] Starting AI Harness flow for user {}", traceId, request.userId());

            // Step 1: Route to appropriate agent
            AgentType agent = routeToAgent(request, traceId, transitions);
            currentState = AiState.ROUTED;
            long routeTime = System.currentTimeMillis() - stepStartTime;
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "1. Routing",
                "Agent: " + agent.getDescription() + " (" + agent.getCode() + ")",
                routeTime,
                true
            ));
            logStateTransition(traceId, currentState, "Agent selected: " + agent);
            appendTransition(
                traceId, transitions, currentState,
                "Agent selected: " + agent.getCode(), routeTime);
            addRunGraphNode(
                runGraph, "route", "router", "ok", routeTime,
                Map.of("agent", agent.getCode()));

            // Step 2: Check semantic cache
            stepStartTime = System.currentTimeMillis();
            Optional<AiHarnessResponse> cachedResponse = checkCache(
                request, agent, traceId, transitions);
            long cacheTime = System.currentTimeMillis() - stepStartTime;
            if (cachedResponse.isPresent()) {
                thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                    "2. Cache Check",
                    "HIT: Tra ve response tu cache",
                    cacheTime,
                    true
                ));
                currentState = AiState.DONE;
                logStateTransition(traceId, currentState, "Cache hit - returning cached response");
                appendTransition(traceId, transitions, currentState, "Cache hit", cacheTime);
                addRunGraphNode(
                    runGraph, "cache", "semantic_cache", "hit",
                    cacheTime, Map.of("agent", agent.getCode()));
                long cacheHitTime = System.currentTimeMillis() - startTime.toEpochMilli();
                traceLogger.endTrace(traceId, currentState.name(), agent.getCode(), cacheHitTime);
                return cachedResponse.get().withThinkingSteps(thinkingSteps);
            }
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "2. Cache Check",
                "MISS: Khong tim thay trong cache",
                cacheTime,
                true
            ));
            currentState = AiState.CACHE_CHECK;
            logStateTransition(traceId, currentState, "Cache miss - proceeding");
            appendTransition(traceId, transitions, currentState, "Cache miss", cacheTime);
            addRunGraphNode(
                runGraph, "cache", "semantic_cache", "miss",
                cacheTime, Map.of("agent", agent.getCode()));

            // Step 3: Build context
            stepStartTime = System.currentTimeMillis();
            String context = buildContext(request, agent, traceId, transitions);
            long contextTime = System.currentTimeMillis() - stepStartTime;
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "3. Context Builder",
                "Xay dung context tu user profile va lich su. Do dai message: "
                    + request.message().length() + " ky tu",
                contextTime,
                true
            ));
            currentState = AiState.CONTEXT_BUILT;
            logStateTransition(traceId, currentState, "Context built");
            appendTransition(
                traceId, transitions, currentState, "Context built", contextTime);
            addRunGraphNode(
                runGraph, "context", "context_builder", "ok", contextTime, Map.of());

            boolean adaptiveApplied = context.contains("Adaptive Rules:");
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "3.8. Adaptive Rules",
                adaptiveApplied
                    ? "Da ap dung rule-based adaptive tu user profile"
                    : "Khong co adaptive rule phu hop",
                0,
                true
            ));
            addRunGraphNode(
                runGraph, "adaptive", "rule_based_adaptive",
                adaptiveApplied ? "applied" : "skipped", 0, Map.of());

            // Step 3.5: Check if this is a tool query (system tools OR user data)
            // - execute BEFORE LLM call
            String toolInfo = "";
            String toolInfoLabel = "";
            stepStartTime = System.currentTimeMillis();
            boolean isToolQuery = isSystemToolQuery(request.message());
            String detectedTool = determineToolForQuery(request.message());
            if (isToolQuery) {
                toolInfo = executeToolBeforeLLM(request, traceId, runGraph);
                toolInfoLabel = determineToolLabel(request.message(), detectedTool);
                logger.info("[{}] Tool query detected: label={}, output length={}",
                    traceId, toolInfoLabel, toolInfo.length());
                if (toolInfo.startsWith("CONFIRMATION_REQUIRED:")) {
                    contextHints.put("confirmationRequired", true);
                    contextHints.put("pendingAction", detectedTool);
                    contextHints.put("pendingArgs",
                        buildToolArgumentsForQuery(
                            detectedTool, request.message(), request.userId()));
                } else if (toolInfo.startsWith("DRY_RUN:")) {
                    contextHints.put("dryRun", true);
                    contextHints.put("pendingAction", detectedTool);
                    contextHints.put("pendingArgs",
                        buildToolArgumentsForQuery(
                            detectedTool, request.message(), request.userId()));
                }
            }
            long toolDetectionTime = System.currentTimeMillis() - stepStartTime;
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "3.5. Tool Detection",
                isToolQuery
                    ? (detectedTool != null
                        ? "Phat hien tool `" + detectedTool + "` (" + toolInfoLabel + ")"
                        : "Phat hien cau hoi ve " + toolInfoLabel + " - lay du lieu")
                    : "Khong phai cau hoi ve tools",
                toolDetectionTime,
                true
            ));

            // Build final context with tool info if available
            String finalContext = context;
            if (!toolInfo.isBlank()) {
                // Format context for better LLM understanding
                finalContext = context + "\n\n=== USER ENROLLMENT DATA ===\n"
                    + toolInfo + "\n=== END DATA ===";
                logger.debug("[{}] Context with tool data length: {}",
                    traceId, finalContext.length());
            }

            // Step 4: Call LLM
            stepStartTime = System.currentTimeMillis();
            AgentConfig agentConfig = loadAgentConfig(agent);
            String llmResponse = callLLM(agent, finalContext, agentConfig, traceId, transitions);
            long llmTime = System.currentTimeMillis() - stepStartTime;
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "4. LLM Call",
                "Goi OpenRouter API. Do dai response: " + llmResponse.length() + " ky tu",
                llmTime,
                llmResponse.contains("I apologize") ? false : true
            ));
            currentState = AiState.LLM_CALLED;
            logStateTransition(traceId, currentState, "LLM responded");
            appendTransition(traceId, transitions, currentState, "LLM responded", llmTime);
            addRunGraphNode(
                runGraph, "llm", "openrouter", "ok", llmTime,
                Map.of("agent", agent.getCode()));

            // Step 4.5: Execute tools if needed (based on LLM response)
            // Note: Primary tool execution already done in Step 3.5 for user data queries
            // This step only handles additional tools if LLM response indicates need
            stepStartTime = System.currentTimeMillis();
            boolean toolsExecutedInStep3 = isToolQuery && !toolInfo.isBlank();
            String contextWithTools = executeToolsIfNeeded(
                request, llmResponse, agent, traceId, transitions, runGraph);
            long toolTime = System.currentTimeMillis() - stepStartTime;
            if (toolsExecutedInStep3 || !contextWithTools.equals(llmResponse)) {
                thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                    "5. Tool Execution",
                    toolsExecutedInStep3
                        ? "Da thuc thi trong step 3.5"
                        : "Planner tool-loop hoan tat",
                    toolTime,
                    true
                ));
            } else {
                thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                    "5. Tool Execution",
                    "Khong can su dung tools",
                    toolTime,
                    true
                ));
            }
            currentState = AiState.TOOL_CALLED;
            logStateTransition(traceId, currentState, "Tool phase completed");
            appendTransition(
                traceId, transitions, currentState, "Tool phase completed", toolTime);

            // Step 5: Validate response
            stepStartTime = System.currentTimeMillis();
            String validatedResponse = validate(
                contextWithTools, agent, traceId, transitions);
            long validationTime = System.currentTimeMillis() - stepStartTime;
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "6. Validation",
                "Kiem tra noi dung: an toan va phu hop",
                validationTime,
                true
            ));
            currentState = AiState.VALIDATED;
            logStateTransition(traceId, currentState, "Response validated");
            appendTransition(
                traceId, transitions, currentState, "Response validated", validationTime);

            // Step 6: Critic review (20% sampling hoac response dai)
            stepStartTime = System.currentTimeMillis();
            boolean wasReviewed = shouldCriticReview(validatedResponse);
            String finalResponse = criticReview(
                validatedResponse, agent, traceId, transitions);
            long criticTime = System.currentTimeMillis() - stepStartTime;
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "7. Critic Review",
                wasReviewed
                        ? "Lay mau 20% hoac phan hoi dai de kiem dinh chat luong."
                        : "Bo qua critic review vi phan hoi ngan.",
                criticTime,
                true
            ));
            currentState = AiState.CRITIC_CHECK;
            logStateTransition(traceId, currentState, "Critic check completed");
            appendTransition(
                traceId, transitions, currentState, "Critic completed", criticTime);

            // Step 7: Save to memory
            stepStartTime = System.currentTimeMillis();
            saveToMemory(request, agent, finalResponse, traceId, transitions);
            long memoryTime = System.currentTimeMillis() - stepStartTime;
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "8. Save Memory",
                "Luu lich su vao database",
                memoryTime,
                true
            ));
            currentState = AiState.DONE;
            logStateTransition(traceId, currentState, "Saved to memory");
            appendTransition(
                traceId, transitions, currentState, "Saved to memory", memoryTime);

            // Build response
            int responseTimeMs = (int) (Instant.now().toEpochMilli()
                - startTime.toEpochMilli());

            // Calculate token usage if available
            int tokensUsed = 0;
            try {
                var tokenUsage = llmService.getLastTokenUsage();
                tokensUsed = tokenUsage.totalTokens();
            } catch (Exception ignored) { }

            AiHarnessResponse response = AiHarnessResponse.builder()
                    .content(finalResponse)
                    .conversationId(request.conversationId())
                    .agentType(agent)
                    .agentName(agent.getDescription())
                    .finalState(currentState)
                    .stateTransitions(transitions)
                    .validated(true)
                    .criticReviewed(wasReviewed)
                    .responseTimeMs(responseTimeMs)
                    .tokensUsed(tokensUsed)
                    .contextHints(contextHints)
                    .thinkingSteps(thinkingSteps)
                    .build();

            // Save semantic cache for repeated/similar queries.
            try {
                cache.save(request, response, agent);
            } catch (Exception e) {
                logger.debug("[{}] Cache save failed: {}", traceId, e.getMessage());
            }

            // Trace to AiAgentTraceService for admin panel
            traceService.traceExecution(
                    request.userId(),
                    request.conversationId(),
                    mapToAiAgentType(agent),
                    "HARNESS_COMPLETE",
                    false,
                    finalResponse,
                    (long) responseTimeMs,
                    tokensUsed,
                    tokensUsed,
                    null
            );

            traceLogger.endTrace(
                traceId, currentState.name(), agent.getCode(), responseTimeMs);
            logger.info("[{}] Flow completed in {}ms - Steps: {}",
                traceId, responseTimeMs, thinkingSteps.size());

            return response;

        } catch (Exception e) {
            logger.error("[{}] Error in AI Harness flow: {}", traceId, e.getMessage(), e);
            currentState = AiState.ERROR;

            // Add error step
            thinkingSteps.add(new AiHarnessResponse.ThinkingStep(
                "Error",
                "Error: " + e.getMessage(),
                0,
                false
            ));

            appendTransition(
                traceId, transitions, currentState,
                "Error: " + e.getMessage(), 0);

            int responseTimeMs = (int) (Instant.now().toEpochMilli()
                - startTime.toEpochMilli());
            AiHarnessResponse errorResponse = AiHarnessResponse.error(
                traceId, "ORCHESTRATOR_ERROR",
                e.getMessage(), responseTimeMs).withThinkingSteps(thinkingSteps);

            traceLogger.endTrace(
                traceId, currentState.name(), "error", responseTimeMs);
            return errorResponse;
        }
    }

    // ========== STEP IMPLEMENTATIONS ==========

    /**
     * Step 1: Route request to appropriate agent
     * Su dung Hybrid Router (keyword + embedding)
     */
    private AgentType routeToAgent(
            AiHarnessRequest request, String traceId,
            List<AiStateTransition> transitions) {
        // Use injected router
        return router.route(request);
    }

    /**
     * Step 2: Check semantic cache
     * Redis-based semantic cache voi embedding similarity
     */
    private Optional<AiHarnessResponse> checkCache(
            AiHarnessRequest request, AgentType agent,
            String traceId, List<AiStateTransition> transitions) {
        // Use injected cache service
        try {
            var cached = cache.check(request, agent);
            if (cached.isPresent()) {
                var entry = cached.get();
                return Optional.of(AiHarnessResponse.builder()
                    .content(entry.response())
                    .agentType(agent)
                    .agentName(agent.getDescription())
                    .cacheHit(true)
                    .finalState(AiState.DONE)
                    .build());
            }
        } catch (Exception e) {
            logger.debug("Cache check failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Step 3: Build context from user profile
     * User profile + conversation history + lesson context
     */
    private String buildContext(
            AiHarnessRequest request, AgentType agent,
            String traceId, List<AiStateTransition> transitions) {
        // Use injected context builder
        return contextBuilder.build(request, agent);
    }

    /**
     * Load agent config from database or use default
     */
    private AgentConfig loadAgentConfig(AgentType agent) {
        return AgentConfig.getDefaultConfig(agent);
    }

    /**
     * Step 4: Call LLM voi agent config
     * Su dung OpenRouter API
     */
    private String callLLM(
            AgentType agent, String context, AgentConfig config,
            String traceId, List<AiStateTransition> transitions) {
        // Use injected LLM service
        try {
            return llmService.call(agent, context, config);
        } catch (Exception e) {
            logger.error("[{}] LLM call failed: {}", traceId, e.getMessage());
            return "I apologize, but I encountered an error processing your request. "
                + "Please try again.";
        }
    }

    /**
     * Step 4.5: Execute tools if needed
     * Check if LLM response contains tool calls and execute them
     */
    private String executeToolsIfNeeded(
            AiHarnessRequest request, String llmResponse,
            AgentType agent, String traceId,
            List<AiStateTransition> transitions,
            List<Map<String, Object>> runGraph) {
        String message = request.message() != null ? request.message() : "";
        String plannerSignal = (llmResponse == null ? "" : llmResponse) + "\n" + message;
        StringBuilder additionalToolContext = new StringBuilder();
        Set<String> executedTools = new HashSet<>();

        try {
            Long userId = request.userId();

            for (int cycle = 1; cycle <= MAX_TOOL_CYCLES; cycle++) {
                String plannedTool = planNextTool(plannerSignal, executedTools);
                if (plannedTool == null) {
                    break;
                }
                Map<String, Object> plannedArgs =
                    buildToolArgumentsForQuery(plannedTool, message, userId);
                List<String> missingArgs = validateToolArgs(plannedTool, plannedArgs);
                if (!missingArgs.isEmpty()) {
                    addRunGraphNode(
                        runGraph, "tool", plannedTool, "invalid_args", 0,
                        Map.of("missing", missingArgs, "cycle", cycle));
                    break;
                }

                long t0 = System.currentTimeMillis();
                var result = toolExecutor.execute(
                    new com.thinkai.backend.ai.tool.ToolCall(
                        plannedTool, plannedArgs, traceId),
                    userId
                );
                long elapsed = System.currentTimeMillis() - t0;
                executedTools.add(plannedTool);
                String status = result.success() ? "ok" : "error";
                addRunGraphNode(
                    runGraph, "tool", plannedTool, status, elapsed,
                    Map.of("cycle", cycle));

                if (result.success() && result.output() != null
                        && !result.output().isBlank()) {
                    additionalToolContext.append("- ").append(plannedTool)
                        .append(": ").append(result.output()).append("\n");
                    plannerSignal = plannerSignal + "\nObservation["
                        + plannedTool + "]: " + result.output();
                } else if (!result.success() && result.error() != null) {
                    additionalToolContext.append("- ").append(plannedTool)
                        .append("_error: ").append(result.error()).append("\n");
                    break;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("[{}] Tool execution failed: {}", traceId, e.getMessage());
            addRunGraphNode(
                runGraph, "tool", "planner_loop", "error", 0,
                Map.of("message", e.getMessage()));
        }

        if (additionalToolContext.isEmpty()) {
            return llmResponse;
        }
        return llmResponse + "\n\n[Additional tool context]\n"
            + additionalToolContext;
    }

    /**
     * Check if the user message is asking about system tools OR user data
     */
    private boolean isSystemToolQuery(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase();

        // System tool queries
        if (lower.contains("tool")
                || lower.contains("cong cu")
                || lower.contains("he thong")
                || lower.contains("api")
                || lower.contains("chuc nang")
                || lower.contains("features")
                || lower.contains("functionality")
                || lower.contains("ban co the lam gi")
                || lower.contains("nhung gi ban co the lam")
                || lower.contains("what can you do")
                || lower.contains("your capabilities")
                || lower.contains("nhung tool")
                || lower.contains("co nhung tool")) {
            return true;
        }

        // User data queries (about their own information)
        if (lower.contains("khoa hoc")
                || lower.contains("dang ky")
                || lower.contains("tien do")
                || lower.contains("bai hoc")
                || lower.contains("level")
                || lower.contains("trinh do")
                || lower.contains("diem")
                || lower.contains("lich su thi")
                || lower.contains("courses")
                || lower.contains("enrolled")
                || lower.contains("my courses")
                || lower.contains("progress")
                || lower.contains("completed")
                || lower.contains("lessons")
                || lower.contains("exam")
                || lower.contains("score")) {
            return true;
        }

        return false;
    }

    /**
     * Execute tool BEFORE LLM call - for both system tools AND user data queries
     * Include tool output in context
     */
    private String executeToolBeforeLLM(
            AiHarnessRequest request, String traceId,
            List<Map<String, Object>> runGraph) {
        try {
            Long userId = request.userId();
            String message = request.message();
            String lower = message != null ? message.toLowerCase() : "";

            // Determine which tool to call based on query type
            String toolName = determineToolForQuery(message);

            if (toolName != null) {
                Map<String, Object> toolArgs =
                    buildToolArgumentsForQuery(toolName, message, userId);
                List<String> missingArgs = validateToolArgs(toolName, toolArgs);
                if (!missingArgs.isEmpty()) {
                    addRunGraphNode(
                        runGraph, "tool", toolName, "invalid_args", 0,
                        Map.of("missing", missingArgs));
                    return "MISSING_ARGS: " + toolName + " requires " + missingArgs;
                }
                if (isMutatingTool(toolName)) {
                    if (isDryRunRequest(request, message)) {
                        return "DRY_RUN: " + toolName + " with args " + toolArgs;
                    }
                    if (!hasMutatingConfirmation(request, message)) {
                        return "CONFIRMATION_REQUIRED: Action `" + toolName
                            + "` needs explicit confirmation. "
                            + "Reply with `xac nhan` (or `confirm`) "
                            + "and include course/exam identifier.";
                    }
                }
                var result = toolExecutor.execute(
                    new com.thinkai.backend.ai.tool.ToolCall(
                        toolName, toolArgs, traceId),
                    userId
                );

                if (result.success()) {
                    logger.info("[{}] Executed tool '{}' for user data query",
                        traceId, toolName);
                    addRunGraphNode(
                        runGraph, "tool", toolName, "ok", 0,
                        Map.of("phase", "pre_llm"));
                    if (isMutatingTool(toolName) && userId != null) {
                        invalidateAllAgentCaches(userId);
                    }
                    return result.output();
                }
                logger.info("[{}] Tool '{}' failed: {}",
                    traceId, toolName, result.error());
                String errorStr = result.error() == null ? "" : result.error();
                addRunGraphNode(
                    runGraph, "tool", toolName, "error", 0,
                    Map.of("phase", "pre_llm", "error", errorStr));
            }

            // Fallback: if message is about system tools, get full tool list
            if (lower.contains("tool") || lower.contains("cong cu")
                    || lower.contains("he thong")) {
                Map<String, Object> fallbackArgs = new HashMap<>();
                if (message != null) {
                    fallbackArgs.put("userMessage", message);
                }
                var result = toolExecutor.execute(
                    new com.thinkai.backend.ai.tool.ToolCall(
                        "auto_detect_system_tools", fallbackArgs, traceId),
                    userId
                );
                if (result.success()) {
                    addRunGraphNode(
                        runGraph, "tool", "auto_detect_system_tools", "ok", 0,
                        Map.of("phase", "pre_llm"));
                    return result.output();
                }
            }

        } catch (Exception e) {
            logger.warn("[{}] Tool execution before LLM failed: {}",
                traceId, e.getMessage());
        }
        return "";
    }

    /**
     * Determine which tool to call based on user query
     */
    private String determineToolForQuery(String message) {
        if (message == null) {
            return null;
        }
        String lower = message.toLowerCase();
        Long courseId = extractFirstLong(message);

        // Unenroll intent must be prioritized before generic "my courses" / "enroll"
        if ((lower.contains("huy dang ky") || lower.contains("huy dang ky")
                || lower.contains("bo dang ky")
                || lower.contains("cancel enrollment")
                || lower.contains("unenroll"))
                && (lower.contains("khoa hoc") || lower.contains("course"))) {
            return "unenroll_course";
        }

        // Explicit enroll action (requires a concrete course id)
        if ((lower.contains("dang ky") || lower.contains("enroll"))
                && (lower.contains("khoa hoc") || lower.contains("course"))
                && courseId != null) {
            return "enroll_course";
        }

        // User's enrolled courses (user's personal courses)
        if (lower.contains("khoa hoc")
                || lower.contains("dang ky") || lower.contains("courses")
                || lower.contains("enrolled") || lower.contains("my courses")
                || lower.contains("cua toi") || lower.contains("cua minh")) {
            return "get_enrolled_courses";
        }

        // User's progress
        if (lower.contains("tien do") || lower.contains("progress")
                || lower.contains("completed") || lower.contains("bai hoc")
                || lower.contains("hoan thanh")) {
            return "get_user_progress";
        }

        // User's level
        if (lower.contains("level") || lower.contains("trinh do")) {
            return "get_user_level";
        }

        // User's exam history
        if (lower.contains("thi") || lower.contains("exam")
                || lower.contains("diem") || lower.contains("score")
                || lower.contains("lich su") || lower.contains("bai thi")) {
            return "get_user_exam_history";
        }

        // Shop courses - all published courses
        if (lower.contains("danh sach khoa hoc") || lower.contains("shop")
                || lower.contains("tat ca khoa hoc")
                || lower.contains("cac khoa hoc")) {
            return "list_shop_courses";
        }

        // Search courses
        if (lower.contains("tim")
                && (lower.contains("khoa hoc") || lower.contains("course"))) {
            return "search_courses";
        }

        // Course detail - when user asks about specific course info
        if ((lower.contains("chi tiet") || lower.contains("thong tin"))
                && (lower.contains("khoa hoc") || lower.contains("course"))) {
            return "get_course_detail";
        }

        // Course lessons - list of lessons in a course
        if (lower.contains("bai hoc") && lower.contains("khoa")) {
            return "list_course_lessons";
        }

        return null;
    }

    private Map<String, Object> buildToolArgumentsForQuery(
            String toolName, String message, Long userId) {
        Map<String, Object> args = new HashMap<>();
        if (userId != null) {
            args.put("userId", userId);
        }

        Long courseId = extractFirstLong(message);
        switch (toolName) {
            case "get_course_detail", "list_course_lessons",
                    "enroll_course", "unenroll_course" -> {
                if (courseId != null) {
                    args.put("courseId", courseId);
                }
            }
            case "search_courses" -> {
                if (message != null && !message.isBlank()) {
                    args.put("query", message);
                }
                args.put("limit", 8);
            }
            case "list_shop_courses" -> args.put("limit", 8);
            default -> {
                // keep default args
            }
        }
        return args;
    }

    private Long extractFirstLong(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\b(\\d{1,18})\\b").matcher(message);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String planNextTool(String plannerSignal, Set<String> executedTools) {
        if (plannerSignal == null) {
            return null;
        }
        String lower = plannerSignal.toLowerCase();
        if ((lower.contains("tool") || lower.contains("cong cu")
                || lower.contains("capabilities"))
                && !executedTools.contains("auto_detect_system_tools")) {
            return "auto_detect_system_tools";
        }
        if ((lower.contains("progress") || lower.contains("tien do")
                || lower.contains("lesson"))
                && !executedTools.contains("get_user_progress")) {
            return "get_user_progress";
        }
        if ((lower.contains("vocab") || lower.contains("tu vung")
                || lower.contains("word"))
                && !executedTools.contains("get_user_vocab_progress")) {
            return "get_user_vocab_progress";
        }
        if ((lower.contains("exam") || lower.contains("thi")
                || lower.contains("score"))
                && !executedTools.contains("get_user_exam_history")) {
            return "get_user_exam_history";
        }
        return null;
    }

    private List<String> validateToolArgs(
            String toolName, Map<String, Object> args) {
        List<String> missing = new ArrayList<>();
        if (toolName == null) {
            return missing;
        }
        if (("get_course_detail".equals(toolName)
                || "list_course_lessons".equals(toolName)
                || "enroll_course".equals(toolName)
                || "unenroll_course".equals(toolName))
                && args.get("courseId") == null) {
            missing.add("courseId");
        }
        if (("create_question".equals(toolName)
                || "bulk_import_questions".equals(toolName))
                && args.get("examId") == null
                && args.get("courseId") == null) {
            missing.add("examId|courseId");
        }
        if ("search_courses".equals(toolName)
                && args.get("query") == null) {
            missing.add("query");
        }
        return missing;
    }

    /**
     * Determine label for thinking process display
     */
    private String determineToolLabel(String message, String detectedTool) {
        if ("unenroll_course".equals(detectedTool)) {
            return "huy dang ky khoa hoc";
        }
        if ("enroll_course".equals(detectedTool)) {
            return "dang ky khoa hoc";
        }
        if (message == null) {
            return "tools";
        }
        String lower = message.toLowerCase();

        if (lower.contains("khoa hoc")
                || lower.contains("dang ky") || lower.contains("courses")
                || lower.contains("enrolled")) {
            return "khoa hoc";
        }
        if (lower.contains("tien do") || lower.contains("progress")
                || lower.contains("completed") || lower.contains("bai hoc")) {
            return "tien do hoc tap";
        }
        if (lower.contains("level") || lower.contains("trinh do")) {
            return "trinh do";
        }
        if (lower.contains("thi") || lower.contains("exam")
                || lower.contains("diem") || lower.contains("score")) {
            return "lich su thi";
        }
        return "tools";
    }

    /**
     * Step 5: Validate response
     * Safety + Quality validation
     */
    private String validate(
            String response, AgentType agent,
            String traceId, List<AiStateTransition> transitions) {
        // Use injected validator
        try {
            return validator.validate(response, agent);
        } catch (Exception e) {
            logger.warn("Validation failed: {}", e.getMessage());
            return response;
        }
    }

    /**
     * Step 6: Critic review
     * Quality scoring voi 20% sampling
     */
    private String criticReview(
            String response, AgentType agent,
            String traceId, List<AiStateTransition> transitions) {
        // Use injected critic
        if (shouldCriticReview(response)) {
            String reviewed = critic.review(response, agent);
            logger.debug("[{}] Critic review completed", traceId);
            return reviewed;
        }
        return response;
    }

    private boolean shouldCriticReview(String response) {
        // 20% sampling hoac response > 300 chars
        return response != null
            && (response.length() > 300 || Math.random() < 0.2);
    }

    /**
     * Step 7: Save to memory
     * Note: Saving to database is handled by AiHarnessController
     */
    private void saveToMemory(
            AiHarnessRequest request, AgentType agent, String response,
            String traceId, List<AiStateTransition> transitions) {
        // Save to memory for conversation tracking
        try {
            memory.saveTurn(
                request.conversationId(), request.userId(),
                request.message(), response, agent);

            // Update conversation summary
            if (request.userId() != null && request.conversationId() != null) {
                String title = generateConversationTitle(request.message());
                String preview = response != null && response.length() > 100
                    ? response.substring(0, 100) + "..."
                    : response;

                // Use reflection or cast to implementation
                if (memory instanceof
                        com.thinkai.backend.ai.orchestrator.impl
                            .OrchestratorMemoryImpl) {
                    ((com.thinkai.backend.ai.orchestrator.impl
                        .OrchestratorMemoryImpl) memory)
                        .updateConversationSummary(
                            request.userId(), request.conversationId(),
                            title, preview);
                }
            }

            logger.debug("[{}] Memory save completed", traceId);
        } catch (Exception e) {
            logger.warn("Memory save failed: {}", e.getMessage());
        }
    }

    private String generateConversationTitle(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return "New Conversation";
        }
        // Use first few words as title
        String[] words = userMessage.split("\\s+");
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < Math.min(5, words.length); i++) {
            if (i > 0) {
                title.append(" ");
            }
            title.append(words[i]);
        }
        return title.length() > 30
            ? title.substring(0, 27) + "..."
            : title.toString();
    }

    // ========== UTILITY METHODS ==========

    private void logStateTransition(
            String traceId, AiState state, String reason) {
        logger.debug("[{}] State: {} - {}", traceId, state, reason);
    }

    private void appendTransition(
            String traceId, List<AiStateTransition> transitions,
            AiState toState, String reason, long latencyMs) {
        AiState fromState = transitions.isEmpty()
            ? null
            : transitions.get(transitions.size() - 1).toState();
        if (fromState == toState) {
            return;
        }
        transitions.add(AiStateTransition.create(
            traceId, fromState, toState, reason, latencyMs));
    }

    private void addRunGraphNode(
            List<Map<String, Object>> runGraph, String kind, String name,
            String status, long latencyMs, Map<String, Object> meta) {
        if (runGraph == null) {
            return;
        }
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("kind", kind);
        node.put("name", name);
        node.put("status", status);
        node.put("latencyMs", latencyMs);
        node.put("ts", System.currentTimeMillis());
        node.put("meta", meta == null ? Map.of() : meta);
        runGraph.add(node);
    }

    private boolean isMutatingTool(String toolName) {
        return "enroll_course".equals(toolName)
                || "unenroll_course".equals(toolName)
                || "create_course".equals(toolName)
                || "update_course".equals(toolName)
                || "publish_course".equals(toolName)
                || "delete_course".equals(toolName)
                || "create_lesson".equals(toolName)
                || "update_lesson".equals(toolName)
                || "delete_lesson".equals(toolName)
                || "create_exam".equals(toolName)
                || "update_exam".equals(toolName)
                || "publish_exam".equals(toolName)
                || "delete_exam".equals(toolName)
                || "create_question".equals(toolName)
                || "bulk_import_questions".equals(toolName);
    }

    private void invalidateAllAgentCaches(Long userId) {
        for (AgentType agentType : AgentType.values()) {
            try {
                cache.invalidate(userId, agentType);
            } catch (Exception e) {
                logger.debug(
                    "Cache invalidate failed for userId={} agent={}: {}",
                    userId, agentType, e.getMessage());
            }
        }
    }

    private boolean hasMutatingConfirmation(
            AiHarnessRequest request, String message) {
        if (request != null && request.metadata() != null) {
            Object confirmed = request.metadata().get("confirmAction");
            if (confirmed instanceof Boolean b && b) {
                return true;
            }
        }
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("xac nhan")
            || lower.contains("confirm")
            || lower.contains("dong y");
    }

    private boolean isDryRunRequest(
            AiHarnessRequest request, String message) {
        if (request != null && request.metadata() != null) {
            Object dryRun = request.metadata().get("dryRun");
            if (dryRun instanceof Boolean b && b) {
                return true;
            }
        }
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("dry run")
            || lower.contains("xem truoc")
            || lower.contains("preview");
    }

    /**
     * Get current state (for monitoring)
     * Returns START by default - can be extended with distributed state tracking
     */
    public AiState getCurrentState(String traceId) {
        return AiState.START;
    }

    /**
     * Force transition to error state (for recovery)
     */
    public void forceErrorState(String traceId, String reason) {
        logger.error("[{}] Forcing error state: {}", traceId, reason);
        // Recovery logic: log error for manual intervention
    }

    /**
     * Map AgentType to AiAgentType for trace service
     */
    private AiAgentType mapToAiAgentType(AgentType agent) {
        if (agent == null) {
            return AiAgentType.TUTOR;
        }
        return switch (agent) {
            case TOEIC_READING, TOEIC_LISTENING, TOEIC_GRAMMAR,
                 TOEIC_VOCABULARY, IELTS_READING, IELTS_LISTENING,
                 IELTS_WRITING, IELTS_SPEAKING, GRAMMAR, VOCABULARY,
                 PRONUNCIATION, CONVERSATION, EXAM_STRATEGY,
                 MISTAKE_ANALYZER, PROGRESS_TRACKER,
                 PLATFORM_LEARNING, PLATFORM_COURSE_OPS,
                 PLATFORM_EXAM_OPS ->
                AiAgentType.LEARNING;
        };
    }
}

package com.thinkai.backend.ai.orchestrator;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import org.springframework.stereotype.Component;

/**
 * Router Interface - Phase 2 sẽ implement Hybrid Router
 * Tích hợp với AiAgentRouterService hiện có
 */
@Component
public interface OrchestratorRouter {

    /**
     * Route request tới appropriate agent
     * Sử dụng hybrid approach: keyword + embedding matching
     *
     * @param request User request
     * @return Selected agent type
     */
    AgentType route(AiHarnessRequest request);

    /**
     * Route với confidence score
     */
    RouteResult routeWithConfidence(AiHarnessRequest request);

    record RouteResult(
        AgentType agentType,
        double confidence,
        String routingReason
    ) {}
}

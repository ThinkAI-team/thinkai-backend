package com.thinkai.backend.ai.orchestrator.impl;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.orchestrator.OrchestratorRouter;
import com.thinkai.backend.ai.router.HybridRouter;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Phase 2: Hybrid Router Implementation
 * Su dung Hybrid Router voi keyword + embedding
 */
@SuppressWarnings("checkstyle:ConstantName")
@Service
public class OrchestratorRouterImpl implements OrchestratorRouter {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorRouterImpl.class);

    private final HybridRouter hybridRouter;

    public OrchestratorRouterImpl(HybridRouter hybridRouter) {
        this.hybridRouter = hybridRouter;
    }

    @Override
    public AgentType route(AiHarnessRequest request) {
        HybridRouter.RoutingResult result = hybridRouter.route(request);
        return result.agent();
    }

    @Override
    public RouteResult routeWithConfidence(AiHarnessRequest request) {
        HybridRouter.RoutingResult result = hybridRouter.route(request);

        logger.debug("Router result: agent={}, confidence={}, reason={}",
            result.agent(), result.confidence(), result.reason());

        return new RouteResult(
            result.agent(),
            result.confidence(),
            String.format("%s (level: %s)", result.reason(), result.confidenceLevel())
        );
    }
}

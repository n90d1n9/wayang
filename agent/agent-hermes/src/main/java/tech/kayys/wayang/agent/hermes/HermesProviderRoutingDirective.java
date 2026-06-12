package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral LLM provider routing instruction for Hermes inference.
 */
public record HermesProviderRoutingDirective(
        boolean active,
        String operation,
        String selectedProvider,
        String requestedProvider,
        String fallbackProvider,
        String model,
        String requestedModel,
        String routingMode,
        boolean localPreferred,
        boolean highContextRequired,
        boolean apiGatewayPreferred,
        boolean toolCallingRequired,
        String requestId,
        String tenantId,
        String sessionId,
        String userId,
        String source,
        String reason) {

    public HermesProviderRoutingDirective {
        operation = HermesDirectiveSupport.clean(operation, active ? "route" : "none");
        selectedProvider = HermesDirectiveSupport.clean(selectedProvider, "auto");
        requestedProvider = HermesDirectiveSupport.clean(requestedProvider, "");
        fallbackProvider = HermesDirectiveSupport.clean(fallbackProvider, "auto");
        model = HermesDirectiveSupport.clean(model, "");
        requestedModel = HermesDirectiveSupport.clean(requestedModel, "");
        routingMode = HermesDirectiveSupport.clean(routingMode, "auto");
        requestId = HermesDirectiveSupport.clean(requestId, "");
        tenantId = HermesDirectiveSupport.clean(tenantId, "default");
        sessionId = HermesDirectiveSupport.clean(sessionId, "");
        userId = HermesDirectiveSupport.clean(userId, "");
        source = HermesDirectiveSupport.clean(source, "none");
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "provider routing requested" : "no provider routing requested");
    }

    public static HermesProviderRoutingDirective from(HermesProviderRoutingPlan plan, AgentRequest request) {
        HermesProviderRoutingPlan effectivePlan = plan == null
                ? new HermesProviderRoutingResolver(HermesAgentModeConfig.defaults()).defaultPlan()
                : plan;
        HermesDirectiveSupport.Identity identity = HermesDirectiveSupport.identity(request);
        boolean active = request != null || hasRoutingSignal(effectivePlan);
        return new HermesProviderRoutingDirective(
                active,
                active ? "route" : "none",
                effectivePlan.selectedProvider(),
                effectivePlan.requestedProvider(),
                effectivePlan.fallbackProvider(),
                effectivePlan.model(),
                effectivePlan.requestedModel(),
                routingMode(effectivePlan),
                effectivePlan.localPreferred(),
                effectivePlan.highContextRequired(),
                effectivePlan.apiGatewayPreferred(),
                effectivePlan.toolCallingRequired(),
                identity.requestId(),
                identity.tenantId(),
                identity.sessionId(),
                identity.userId(),
                effectivePlan.source(),
                active ? effectivePlan.reason() : "default plan only");
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("selectedProvider", selectedProvider);
        metadata.put("requestedProvider", requestedProvider);
        metadata.put("fallbackProvider", fallbackProvider);
        metadata.put("model", model);
        metadata.put("requestedModel", requestedModel);
        metadata.put("routingMode", routingMode);
        metadata.put("localPreferred", localPreferred);
        metadata.put("highContextRequired", highContextRequired);
        metadata.put("apiGatewayPreferred", apiGatewayPreferred);
        metadata.put("toolCallingRequired", toolCallingRequired);
        metadata.put("requestId", requestId);
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("source", source);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }

    private static boolean hasRoutingSignal(HermesProviderRoutingPlan plan) {
        return !"auto".equals(plan.selectedProvider())
                || plan.explicitProvider()
                || plan.explicitModel()
                || plan.localPreferred()
                || plan.highContextRequired()
                || plan.apiGatewayPreferred();
    }

    private static String routingMode(HermesProviderRoutingPlan plan) {
        if (plan.apiGatewayPreferred()) {
            return "api-gateway";
        }
        if (plan.localPreferred()) {
            return "local";
        }
        if (plan.highContextRequired()) {
            return "high-context";
        }
        if (plan.explicitProvider()) {
            return "explicit-provider";
        }
        if (plan.explicitModel()) {
            return "explicit-model";
        }
        return "auto";
    }
}

package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter-neutral sub-agent spawn instruction for Hermes fan-out workstreams.
 */
public record HermesDelegationDirective(
        boolean delegationEnabled,
        boolean requested,
        boolean delegated,
        boolean active,
        String operation,
        String groupId,
        int subAgentCount,
        int maxSubAgents,
        List<String> lanes,
        String isolationMode,
        String parentRequestId,
        String tenantId,
        String sessionId,
        String userId,
        String source,
        String reason) {

    public HermesDelegationDirective {
        operation = HermesDirectiveSupport.clean(operation, active ? "spawn" : "none");
        groupId = HermesDirectiveSupport.clean(groupId, "");
        subAgentCount = Math.max(0, subAgentCount);
        maxSubAgents = Math.max(1, maxSubAgents);
        lanes = HermesText.distinctTrimmedList(lanes);
        isolationMode = HermesDirectiveSupport.clean(isolationMode, active ? "context-isolated" : "none");
        parentRequestId = HermesDirectiveSupport.clean(parentRequestId, "");
        tenantId = HermesDirectiveSupport.clean(tenantId, "default");
        sessionId = HermesDirectiveSupport.clean(sessionId, "");
        userId = HermesDirectiveSupport.clean(userId, "");
        source = HermesDirectiveSupport.clean(source, "none");
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "sub-agent delegation requested" : "no sub-agent delegation requested");
    }

    public static HermesDelegationDirective from(HermesDelegationPlan plan, AgentRequest request) {
        HermesDelegationPlan effectivePlan = plan == null
                ? new HermesDelegationPlanner(HermesAgentModeConfig.defaults()).defaultPlan()
                : plan;
        HermesDirectiveSupport.Identity identity = HermesDirectiveSupport.identity(request);
        boolean requestPresent = request != null;
        boolean active = requestPresent && effectivePlan.active();
        return new HermesDelegationDirective(
                effectivePlan.delegationEnabled(),
                effectivePlan.requested(),
                effectivePlan.delegated(),
                active,
                active ? "spawn" : "none",
                active ? groupId(effectivePlan, identity) : "",
                active ? effectivePlan.suggestedSubAgents() : 0,
                effectivePlan.maxSubAgents(),
                effectivePlan.lanes(),
                active ? effectivePlan.isolationMode() : "none",
                identity.requestId(),
                identity.tenantId(),
                identity.sessionId(),
                identity.userId(),
                effectivePlan.source(),
                reason(requestPresent, effectivePlan));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("delegationEnabled", delegationEnabled);
        metadata.put("requested", requested);
        metadata.put("delegated", delegated);
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("groupId", groupId);
        metadata.put("subAgentCount", subAgentCount);
        metadata.put("maxSubAgents", maxSubAgents);
        metadata.put("lanes", lanes);
        metadata.put("isolationMode", isolationMode);
        metadata.put("parentRequestId", parentRequestId);
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("source", source);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }

    private static String groupId(HermesDelegationPlan plan, HermesDirectiveSupport.Identity identity) {
        String base = identity.requestId();
        if (base == null || base.isBlank()) {
            base = HermesDirectiveSupport.hashBase(
                    plan.lanes(),
                    identity.tenantId(),
                    identity.sessionId());
        }
        return HermesDirectiveSupport.prefixedId("hermes-delegation", base, "group");
    }

    private static String reason(boolean requestPresent, HermesDelegationPlan plan) {
        if (!requestPresent) {
            return "default plan only";
        }
        if (!plan.delegationEnabled()) {
            return "sub-agent delegation disabled";
        }
        if (!plan.delegated()) {
            return plan.reason();
        }
        if (!plan.active()) {
            return "sub-agent count below spawn threshold";
        }
        return plan.reason();
    }
}

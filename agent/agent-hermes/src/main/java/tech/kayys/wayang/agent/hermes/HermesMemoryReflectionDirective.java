package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentMemoryConfig;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral command for memory services to execute Hermes reflection work.
 */
public record HermesMemoryReflectionDirective(
        boolean memoryEnabled,
        boolean requested,
        boolean active,
        String operation,
        String scope,
        String cadence,
        String priority,
        String subjectType,
        String subjectId,
        String memoryNamespace,
        String requestId,
        String tenantId,
        String sessionId,
        String userId,
        String source,
        String reason) {

    public HermesMemoryReflectionDirective {
        operation = HermesDirectiveSupport.clean(operation, active ? "consolidate" : "none");
        scope = HermesDirectiveSupport.clean(scope, "session");
        cadence = HermesDirectiveSupport.clean(cadence, active ? "post-run" : "none");
        priority = HermesDirectiveSupport.clean(priority, "normal");
        subjectType = HermesDirectiveSupport.clean(subjectType, scope);
        subjectId = HermesDirectiveSupport.clean(subjectId, "default");
        memoryNamespace = HermesDirectiveSupport.clean(memoryNamespace, "");
        requestId = HermesDirectiveSupport.clean(requestId, "");
        tenantId = HermesDirectiveSupport.clean(tenantId, "default");
        sessionId = HermesDirectiveSupport.clean(sessionId, "");
        userId = HermesDirectiveSupport.clean(userId, "");
        source = HermesDirectiveSupport.clean(source, "none");
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "memory reflection requested" : "no memory reflection requested");
    }

    public static HermesMemoryReflectionDirective from(
            HermesMemoryReflectionPlan plan,
            AgentRequest request) {
        HermesMemoryReflectionPlan effectivePlan = plan == null
                ? new HermesMemoryReflectionPlan(false, false, false, "session", "none", "normal", "none", "")
                : plan;
        AgentMemoryConfig memoryConfig = request == null ? AgentMemoryConfig.defaults() : request.memoryConfig();
        HermesDirectiveSupport.Identity identity = HermesDirectiveSupport.identity(request);
        String scope = effectivePlan.scope();
        return new HermesMemoryReflectionDirective(
                effectivePlan.memoryEnabled(),
                effectivePlan.requested(),
                effectivePlan.active(),
                effectivePlan.active() ? "consolidate" : "none",
                scope,
                effectivePlan.cadence(),
                effectivePlan.priority(),
                scope,
                subjectId(scope, request),
                memoryConfig == null ? "" : memoryConfig.memoryNamespace(),
                identity.requestId(),
                identity.tenantId(),
                identity.sessionId(),
                identity.userId(),
                effectivePlan.source(),
                effectivePlan.reason());
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("memoryEnabled", memoryEnabled);
        metadata.put("requested", requested);
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("scope", scope);
        metadata.put("cadence", cadence);
        metadata.put("priority", priority);
        metadata.put("subjectType", subjectType);
        metadata.put("subjectId", subjectId);
        metadata.put("memoryNamespace", memoryNamespace);
        metadata.put("requestId", requestId);
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("source", source);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }

    private static String subjectId(String scope, AgentRequest request) {
        if (request == null) {
            return "default";
        }
        return switch (HermesRequestValues.normalize(scope)) {
            case "global" -> "global";
            case "tenant" -> HermesDirectiveSupport.clean(request.tenantId(), "default");
            case "user" -> HermesDirectiveSupport.clean(
                    request.userId(),
                    HermesDirectiveSupport.clean(request.tenantId(), "default"));
            case "conversation", "session" -> HermesDirectiveSupport.clean(
                    request.sessionId(),
                    HermesDirectiveSupport.clean(request.requestId(), "default"));
            case "agent" -> HermesDirectiveSupport.clean(
                    agentId(request),
                    HermesDirectiveSupport.clean(request.tenantId(), "default"));
            default -> HermesDirectiveSupport.clean(
                    request.sessionId(),
                    HermesDirectiveSupport.clean(request.tenantId(), "default"));
        };
    }

    private static String agentId(AgentRequest request) {
        Object value = request.context().get("agentId");
        return value == null ? "" : value.toString();
    }
}

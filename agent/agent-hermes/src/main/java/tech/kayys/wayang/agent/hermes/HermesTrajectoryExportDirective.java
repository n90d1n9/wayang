package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral trajectory export instruction for Hermes observability sinks.
 */
public record HermesTrajectoryExportDirective(
        boolean exportEnabled,
        boolean requested,
        boolean active,
        String operation,
        String exportId,
        String format,
        String destination,
        boolean includePrompts,
        boolean includeToolCalls,
        boolean redactSensitive,
        String requestId,
        String tenantId,
        String sessionId,
        String userId,
        String source,
        String reason) {

    public HermesTrajectoryExportDirective {
        operation = HermesDirectiveSupport.clean(operation, active ? "export" : "none");
        exportId = HermesDirectiveSupport.clean(exportId, "");
        format = HermesDirectiveSupport.clean(format, "jsonl");
        destination = HermesDirectiveSupport.clean(destination, active ? "local" : "none");
        requestId = HermesDirectiveSupport.clean(requestId, "");
        tenantId = HermesDirectiveSupport.clean(tenantId, "default");
        sessionId = HermesDirectiveSupport.clean(sessionId, "");
        userId = HermesDirectiveSupport.clean(userId, "");
        source = HermesDirectiveSupport.clean(source, "none");
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "trajectory export requested" : "no trajectory export requested");
    }

    public static HermesTrajectoryExportDirective from(HermesTrajectoryExportPlan plan, AgentRequest request) {
        HermesTrajectoryExportPlan effectivePlan = plan == null
                ? new HermesTrajectoryExportResolver(HermesAgentModeConfig.defaults()).defaultPlan()
                : plan;
        HermesDirectiveSupport.Identity identity = HermesDirectiveSupport.identity(request);
        boolean requestPresent = request != null;
        boolean active = requestPresent && effectivePlan.active();
        return new HermesTrajectoryExportDirective(
                effectivePlan.exportEnabled(),
                effectivePlan.requested(),
                active,
                active ? "export" : "none",
                active ? exportId(effectivePlan, identity) : "",
                effectivePlan.format(),
                active ? effectivePlan.destination() : "none",
                effectivePlan.includePrompts(),
                effectivePlan.includeToolCalls(),
                effectivePlan.redactSensitive(),
                identity.requestId(),
                identity.tenantId(),
                identity.sessionId(),
                identity.userId(),
                effectivePlan.source(),
                reason(requestPresent, effectivePlan));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("exportEnabled", exportEnabled);
        metadata.put("requested", requested);
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("exportId", exportId);
        metadata.put("format", format);
        metadata.put("destination", destination);
        metadata.put("includePrompts", includePrompts);
        metadata.put("includeToolCalls", includeToolCalls);
        metadata.put("redactSensitive", redactSensitive);
        metadata.put("requestId", requestId);
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("source", source);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }

    private static String exportId(HermesTrajectoryExportPlan plan, HermesDirectiveSupport.Identity identity) {
        String base = identity.requestId();
        if (base == null || base.isBlank()) {
            base = HermesDirectiveSupport.hashBase(
                    plan.destination(),
                    identity.tenantId(),
                    identity.sessionId());
        }
        return HermesDirectiveSupport.prefixedId("hermes-trajectory", base, "export");
    }

    private static String reason(boolean requestPresent, HermesTrajectoryExportPlan plan) {
        if (!requestPresent) {
            return "default plan only";
        }
        if (!plan.exportEnabled()) {
            return "trajectory export disabled";
        }
        if (!plan.export()) {
            return plan.reason();
        }
        if (!plan.active()) {
            return "trajectory export destination unavailable";
        }
        return plan.reason();
    }
}

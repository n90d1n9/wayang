package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral execution dispatch instruction for Hermes runtime backends.
 */
public record HermesExecutionDirective(
        boolean executable,
        boolean backendSupported,
        boolean active,
        String operation,
        String backend,
        String requestedBackend,
        String adapterType,
        String safetyProfile,
        boolean isolationRequired,
        boolean remotePreferred,
        boolean serverlessPreferred,
        String requestId,
        String tenantId,
        String sessionId,
        String userId,
        String reason) {

    public HermesExecutionDirective {
        operation = HermesDirectiveSupport.clean(operation, active ? "dispatch" : "none");
        backend = HermesDirectiveSupport.clean(backend, "none");
        requestedBackend = HermesDirectiveSupport.clean(requestedBackend, "");
        adapterType = HermesDirectiveSupport.clean(adapterType, "none");
        safetyProfile = HermesDirectiveSupport.clean(safetyProfile, "none");
        requestId = HermesDirectiveSupport.clean(requestId, "");
        tenantId = HermesDirectiveSupport.clean(tenantId, "default");
        sessionId = HermesDirectiveSupport.clean(sessionId, "");
        userId = HermesDirectiveSupport.clean(userId, "");
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "execution dispatch requested" : "no execution dispatch requested");
    }

    public static HermesExecutionDirective from(
            HermesExecutionPlan plan,
            AgentRequest request,
            HermesAgentModeConfig config) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesExecutionPlan effectivePlan = plan == null
                ? new HermesExecutionPlanner(effectiveConfig).defaultPlan()
                : plan;
        boolean requestPresent = request != null;
        boolean executable = effectivePlan.executable();
        boolean supported = effectiveConfig.runtimeCapabilities().supportsExecutionBackend(effectivePlan.backend());
        boolean active = requestPresent && executable && supported;
        HermesDirectiveSupport.Identity identity = HermesDirectiveSupport.identity(request);
        return new HermesExecutionDirective(
                executable,
                supported,
                active,
                active ? "dispatch" : "none",
                effectivePlan.backend(),
                effectivePlan.requestedBackend(),
                adapterType(effectivePlan.backend()),
                safetyProfile(effectivePlan),
                effectivePlan.isolationRequired(),
                effectivePlan.remotePreferred(),
                effectivePlan.serverlessPreferred(),
                identity.requestId(),
                identity.tenantId(),
                identity.sessionId(),
                identity.userId(),
                reason(requestPresent, executable, supported, effectivePlan));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("executable", executable);
        metadata.put("backendSupported", backendSupported);
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("backend", backend);
        metadata.put("requestedBackend", requestedBackend);
        metadata.put("adapterType", adapterType);
        metadata.put("safetyProfile", safetyProfile);
        metadata.put("isolationRequired", isolationRequired);
        metadata.put("remotePreferred", remotePreferred);
        metadata.put("serverlessPreferred", serverlessPreferred);
        metadata.put("requestId", requestId);
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }

    private static String adapterType(String backend) {
        return switch (HermesRequestValues.normalize(backend)) {
            case "local" -> "local-terminal";
            case "docker", "singularity" -> "container";
            case "ssh" -> "remote-shell";
            case "modal", "daytona" -> "serverless";
            default -> "none";
        };
    }

    private static String safetyProfile(HermesExecutionPlan plan) {
        if (!plan.executable()) {
            return "none";
        }
        if (plan.serverlessPreferred()) {
            return "serverless-isolated";
        }
        if (plan.remotePreferred()) {
            return "remote-isolated";
        }
        if (plan.isolationRequired()) {
            return "container-isolated";
        }
        return "standard";
    }

    private static String reason(
            boolean requestPresent,
            boolean executable,
            boolean supported,
            HermesExecutionPlan plan) {
        if (!requestPresent) {
            return "default plan only";
        }
        if (!executable) {
            return "no execution backend configured";
        }
        if (!supported) {
            return "execution backend unsupported";
        }
        return plan.reason();
    }
}

package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

final class McpServerLifecycleServiceTestDouble extends McpServerLifecycleService {
    private final McpServerLifecycleResult lifecycleResult;
    private final McpServerLifecycleImpact impactResult;
    private final McpServerRetirementResult retirementResult;
    private String lastRequestId;
    private String lastServerName;
    private Boolean lastEnabled;

    private McpServerLifecycleServiceTestDouble(
            McpServerLifecycleResult lifecycleResult,
            McpServerLifecycleImpact impactResult,
            McpServerRetirementResult retirementResult) {
        this.lifecycleResult = lifecycleResult;
        this.impactResult = impactResult;
        this.retirementResult = retirementResult;
    }

    static McpServerLifecycleServiceTestDouble changing(McpServerLifecycleResult result) {
        return new McpServerLifecycleServiceTestDouble(result, null, null);
    }

    static McpServerLifecycleServiceTestDouble impacting(McpServerLifecycleImpact result) {
        return new McpServerLifecycleServiceTestDouble(null, result, null);
    }

    static McpServerLifecycleServiceTestDouble retiring(McpServerRetirementResult result) {
        return new McpServerLifecycleServiceTestDouble(null, null, result);
    }

    String lastRequestId() {
        return lastRequestId;
    }

    String lastServerName() {
        return lastServerName;
    }

    Boolean lastEnabled() {
        return lastEnabled;
    }

    @Override
    public Uni<McpServerLifecycleResult> setEnabled(
            String requestId,
            String serverName,
            boolean enabled) {
        lastRequestId = requestId;
        lastServerName = serverName;
        lastEnabled = enabled;
        if (lifecycleResult == null) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(lifecycleResult);
    }

    @Override
    public Uni<McpServerLifecycleImpact> impact(String requestId, String serverName) {
        lastRequestId = requestId;
        lastServerName = serverName;
        if (impactResult == null) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(impactResult);
    }

    @Override
    public Uni<McpServerRetirementResult> retire(String requestId, String serverName) {
        lastRequestId = requestId;
        lastServerName = serverName;
        if (retirementResult == null) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(retirementResult);
    }
}

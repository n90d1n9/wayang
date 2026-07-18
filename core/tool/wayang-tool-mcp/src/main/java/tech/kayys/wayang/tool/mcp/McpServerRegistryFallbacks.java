package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.util.List;

final class McpServerRegistryFallbacks {

    private McpServerRegistryFallbacks() {
    }

    static McpServerRegistryInspectionService serverRegistryInspectionService(
            McpServerRegistryInspectionService configured) {
        return configured == null ? new McpServerRegistryInspectionService() {
            @Override
            public Uni<List<McpServerRegistryEntry>> list(
                    String requestId,
                    Boolean enabled,
                    String transport) {
                return Uni.createFrom().item(List.of());
            }

            @Override
            public Uni<McpServerRegistryEntry> get(String requestId, String serverName) {
                return Uni.createFrom().nullItem();
            }
        } : configured;
    }

    static McpServerLifecycleService serverLifecycleService(
            McpServerLifecycleService configured) {
        return configured == null ? new McpServerLifecycleService() {
            @Override
            public Uni<McpServerLifecycleResult> setEnabled(
                    String requestId,
                    String serverName,
                    boolean enabled) {
                return Uni.createFrom().nullItem();
            }

            @Override
            public Uni<McpServerLifecycleImpact> impact(String requestId, String serverName) {
                return Uni.createFrom().nullItem();
            }

            @Override
            public Uni<McpServerRetirementResult> retire(String requestId, String serverName) {
                return Uni.createFrom().nullItem();
            }
        } : configured;
    }
}

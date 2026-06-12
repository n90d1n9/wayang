package tech.kayys.wayang.tool.mcp;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class McpToolRegistrySummaries {

    private McpToolRegistrySummaries() {
    }

    static McpToolRegistrySummary from(List<tech.kayys.wayang.tool.entity.McpTool> tools) {
        RegistrySummaryAccumulator total = new RegistrySummaryAccumulator(null, null);
        Map<String, RegistrySummaryAccumulator> servers = new LinkedHashMap<>();
        for (tech.kayys.wayang.tool.entity.McpTool tool : tools) {
            String serverName = McpToolRegistryMetadata.serverName(tool);
            String endpoint = McpToolRegistryMetadata.registryEndpoint(tool);
            total.add(tool, endpoint);
            servers.computeIfAbsent(summaryKey(serverName, endpoint),
                    ignored -> new RegistrySummaryAccumulator(serverName, endpoint))
                    .add(tool, endpoint);
        }
        return new McpToolRegistrySummary(
                total.total,
                total.lifecycle.enabled(),
                total.lifecycle.disabled(),
                total.lifecycle.stale(),
                total.lifecycle.active(),
                total.lifecycle.serverDisabled(),
                total.lifecycle.retired(),
                total.readOnly,
                total.requiresApproval,
                total.lifecycle.lifecycleStates(),
                servers.values().stream()
                        .sorted(Comparator.comparing(RegistrySummaryAccumulator::sortKey))
                        .map(RegistrySummaryAccumulator::toServerSummary)
                        .toList());
    }

    private static String summaryKey(String serverName, String endpoint) {
        return (serverName == null ? "" : serverName.toLowerCase())
                + "|"
                + (endpoint == null ? "" : endpoint);
    }

    private static final class RegistrySummaryAccumulator {
        private final String serverName;
        private final McpToolLifecycleCounts lifecycle = new McpToolLifecycleCounts();
        private String endpoint;
        private int total;
        private int readOnly;
        private int requiresApproval;

        private RegistrySummaryAccumulator(String serverName, String endpoint) {
            this.serverName = serverName;
            this.endpoint = endpoint;
        }

        private void add(tech.kayys.wayang.tool.entity.McpTool tool, String candidateEndpoint) {
            total++;
            lifecycle.add(tool);
            if (tool.isReadOnly()) {
                readOnly++;
            }
            if (tool.isRequiresApproval()) {
                requiresApproval++;
            }
            if ((endpoint == null || endpoint.isBlank())
                    && candidateEndpoint != null
                    && !candidateEndpoint.isBlank()) {
                endpoint = candidateEndpoint;
            }
        }

        private String sortKey() {
            return serverName == null ? "" : serverName.toLowerCase();
        }

        private McpToolRegistrySummary.ServerSummary toServerSummary() {
            return new McpToolRegistrySummary.ServerSummary(
                    serverName,
                    endpoint,
                    total,
                    lifecycle.enabled(),
                    lifecycle.disabled(),
                    lifecycle.stale(),
                    lifecycle.active(),
                    lifecycle.serverDisabled(),
                    lifecycle.retired(),
                    readOnly,
                    requiresApproval,
                    lifecycle.lifecycleStates());
        }
    }
}

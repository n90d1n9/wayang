package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record McpToolRegistryEntry(
        String toolId,
        String name,
        String description,
        String namespace,
        String serverName,
        String endpoint,
        boolean enabled,
        boolean stale,
        boolean serverDisabled,
        boolean retired,
        String lifecycleState,
        boolean readOnly,
        boolean requiresApproval,
        Set<String> capabilities,
        Set<String> tags,
        Map<String, Object> inputSchema,
        String operationId,
        Instant createdAt,
        Instant updatedAt) {

    public McpToolRegistryEntry {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        inputSchema = McpMaps.copy(inputSchema);
    }
}

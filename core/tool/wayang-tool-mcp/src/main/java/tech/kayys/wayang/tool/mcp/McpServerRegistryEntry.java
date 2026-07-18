package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

public record McpServerRegistryEntry(
        String serverName,
        String transport,
        String endpoint,
        boolean enabled,
        String source,
        String syncSchedule,
        Instant lastSyncAt,
        Instant createdAt,
        Instant updatedAt) {
}

package tech.kayys.wayang.tool.mcp;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryTestFixtures.append;

final class McpToolCallHistoryResourceTestHarness {
    private static final Duration DEFAULT_RETENTION = Duration.ofDays(7);
    private static final Clock HISTORY_CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T00:00:00Z"),
            ZoneOffset.UTC);

    private McpToolCallHistoryResourceTestHarness() {
    }

    static McpToolCallHistoryResource resourceWithHistory(McpToolCallHistoryEntry... entries) {
        McpToolCallHistoryStore store = new InMemoryMcpToolCallHistoryStore(
                DEFAULT_RETENTION,
                HISTORY_CLOCK);
        for (McpToolCallHistoryEntry entry : entries) {
            append(store, entry);
        }
        return resourceWithStore(store);
    }

    static McpToolCallHistoryResource resourceWithStore(McpToolCallHistoryStore store) {
        McpToolCallHistoryResource resource = new McpToolCallHistoryResource();
        resource.toolCallHistoryService = new McpToolCallHistoryService(store);
        return resource;
    }

    static McpToolCallHistoryQueryParamsTestBuilder query(String runId) {
        return McpToolCallHistoryQueryParamsTestBuilder.forRun(runId);
    }

    static McpToolCallHistoryQueryParams runQuery(String runId) {
        return query(runId).build();
    }

    static McpToolCallHistoryQueryParams runQuery(String runId, int limit) {
        return query(runId).withLimit(limit).build();
    }
}

package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.entity.McpTool;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.history;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.server;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.tool;

class McpToolServerHealthSourceTest {

    @Test
    void loadsServersToolsAndHistoryIntoSnapshot() {
        Instant startedAt = Instant.now();
        McpServerRegistry docs = server("tenant-1", "docs");
        McpTool docsTool = tool("tenant-1", "docs.search", true,
                Set.of("mcp", "tool", "mcp:docs"), Set.of("docs"));
        McpToolDiscoverySyncHistoryEntry latest = history("docs", McpToolDiscoverySyncStatuses.ERROR, startedAt);
        McpToolDiscoverySyncHistoryEntry success =
                history("docs", McpToolDiscoverySyncStatuses.SUCCESS, startedAt.minusSeconds(20));
        McpToolDiscoverySyncHistoryEntry error =
                history("docs", McpToolDiscoverySyncStatuses.ERROR, startedAt.minusSeconds(10));
        McpToolDiscoverySyncServiceTestDouble syncService = new McpToolDiscoverySyncServiceTestDouble(
                List.of(latest),
                List.of(success),
                List.of(error),
                List.of(latest, success));

        McpToolServerHealthSource.Snapshot snapshot = McpToolServerHealthSource.load(
                        "tenant-1",
                        new McpServerRegistryRepositoryTestDouble(List.of(docs, server("other", "crm"))),
                        new McpToolRepositoryTestDouble(List.of(docsTool, tool("other", "crm.lookup", true,
                                Set.of("mcp", "tool", "mcp:crm"), Set.of("crm")))),
                        syncService)
                .await().atMost(Duration.ofSeconds(3));

        McpToolServerHealthInputs.ServerInput docsInput = snapshot.inputs().forServer("docs");

        assertEquals(List.of(docs), snapshot.servers());
        assertEquals(1, docsInput.toolCounts().total());
        assertEquals(1, docsInput.toolCounts().active());
        assertEquals(latest, docsInput.latest());
        assertEquals(success, docsInput.latestSuccess());
        assertEquals(error, docsInput.latestError());
        assertEquals(List.of(latest, success), docsInput.recentHistory());
        assertEquals(Arrays.asList(null, McpToolDiscoverySyncStatuses.SUCCESS, McpToolDiscoverySyncStatuses.ERROR),
                syncService.latestStatuses());
        assertEquals(List.of(200, 200, 200), syncService.latestLimits());
        assertEquals(List.of(200), syncService.historyLimits());
    }

    @Test
    void nullSyncServiceUsesEmptyHistory() {
        McpToolServerHealthSource.Snapshot snapshot = McpToolServerHealthSource.load(
                        "tenant-1",
                        new McpServerRegistryRepositoryTestDouble(List.of(server("tenant-1", "docs"))),
                        new McpToolRepositoryTestDouble(List.of(tool("tenant-1", "docs.search", true,
                                Set.of("mcp", "tool", "mcp:docs"), Set.of("docs")))),
                        null)
                .await().atMost(Duration.ofSeconds(3));

        McpToolServerHealthInputs.ServerInput docsInput = snapshot.inputs().forServer("docs");

        assertEquals(1, snapshot.servers().size());
        assertEquals(1, docsInput.toolCounts().total());
        assertNull(docsInput.latest());
        assertNull(docsInput.latestSuccess());
        assertNull(docsInput.latestError());
        assertEquals(List.of(), docsInput.recentHistory());
    }

}

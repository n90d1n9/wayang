package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.history;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.tool;

class McpToolServerHealthInputsTest {

    @Test
    void preparesPerServerToolCountsAndHistoryLookups() {
        Instant startedAt = Instant.now();
        McpToolDiscoverySyncHistoryEntry latest = history("DOCS", McpToolDiscoverySyncStatuses.ERROR, startedAt);
        McpToolDiscoverySyncHistoryEntry success =
                history("docs", McpToolDiscoverySyncStatuses.SUCCESS, startedAt.minusSeconds(20));
        McpToolDiscoverySyncHistoryEntry error =
                history("docs", McpToolDiscoverySyncStatuses.ERROR, startedAt.minusSeconds(10));

        McpToolServerHealthInputs inputs = McpToolServerHealthInputs.from(
                List.of(tool("docs.search", true, Set.of("mcp", "tool", "mcp:docs"), Set.of("DOCS"))),
                List.of(latest),
                List.of(success),
                List.of(error),
                List.of(latest, success));

        McpToolServerHealthInputs.ServerInput docs = inputs.forServer("docs");

        assertEquals(1, docs.toolCounts().total());
        assertEquals(1, docs.toolCounts().active());
        assertEquals(latest, docs.latest());
        assertEquals(success, docs.latestSuccess());
        assertEquals(error, docs.latestError());
        assertEquals(List.of(latest, success), docs.recentHistory());
    }

    @Test
    void missingServerReturnsEmptyCountsAndHistory() {
        McpToolServerHealthInputs inputs = McpToolServerHealthInputs.from(List.of(), List.of(), List.of(), List.of(),
                List.of());

        McpToolServerHealthInputs.ServerInput missing = inputs.forServer("missing");

        assertEquals(0, missing.toolCounts().total());
        assertNull(missing.latest());
        assertNull(missing.latestSuccess());
        assertNull(missing.latestError());
        assertEquals(List.of(), missing.recentHistory());
    }

    @Test
    void nullInputsAreTreatedAsEmptyCollections() {
        McpToolServerHealthInputs inputs = McpToolServerHealthInputs.from(null, null, null, null, null);

        McpToolServerHealthInputs.ServerInput missing = inputs.forServer(null);

        assertEquals(0, missing.toolCounts().total());
        assertNull(missing.latest());
        assertEquals(List.of(), missing.recentHistory());
    }
}

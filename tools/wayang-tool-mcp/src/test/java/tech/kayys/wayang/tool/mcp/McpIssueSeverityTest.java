package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.issueDetail;

class McpIssueSeverityTest {

    @Test
    void normalizesSeverityValues() {
        assertEquals(McpIssueSeverity.CRITICAL, McpIssueSeverity.normalize("critical"));
        assertEquals(McpIssueSeverity.WARNING, McpIssueSeverity.normalize(" warning "));
        assertEquals("NEEDS_REVIEW", McpIssueSeverity.normalize("needs-review"));
        assertNull(McpIssueSeverity.normalize(" "));
    }

    @Test
    void ranksAndComparesKnownSeverities() {
        assertEquals(3, McpIssueSeverity.rank("critical"));
        assertEquals(2, McpIssueSeverity.rank("warning"));
        assertEquals(1, McpIssueSeverity.rank("info"));
        assertEquals(0, McpIssueSeverity.rank("unknown"));
        assertEquals(McpIssueSeverity.CRITICAL, McpIssueSeverity.higher("warning", "critical"));
        assertEquals(McpIssueSeverity.WARNING, McpIssueSeverity.higher("warning", "info"));
        assertNull(McpIssueSeverity.higher("unknown", null));
    }

    @Test
    void findsHighestIssueSeverity() {
        assertEquals(
                McpIssueSeverity.CRITICAL,
                McpIssueSeverity.highest(List.of(
                        issueDetail("SERVER_DISABLED", "INFO", "Server is disabled."),
                        issueDetail("SYNC_ERROR", "CRITICAL", "Latest sync failed."),
                        issueDetail("STALE_TOOLS", "WARNING", "Tools are stale."))));
        assertNull(McpIssueSeverity.highest(List.of()));
    }
}

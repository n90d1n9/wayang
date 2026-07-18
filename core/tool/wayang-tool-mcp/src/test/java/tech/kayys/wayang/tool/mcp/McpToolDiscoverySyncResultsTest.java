package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolDiscoverySyncResultsTest {

    @Test
    void warningBuildsZeroScanResult() {
        McpToolDiscoverySyncResult result = McpToolDiscoverySyncResults.warning("not configured");

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(0, result.stale());
        assertEquals(0, result.reactivated());
        assertEquals(List.of("not configured"), result.warnings());
    }

    @Test
    void singleServerMapsServerResultCounts() {
        McpToolDiscoverySyncResult result = McpToolDiscoverySyncResults.singleServer(
                new McpToolDiscoveryServerSyncResult(
                        2,
                        1,
                        1,
                        List.of("sync warning")));

        assertEquals(1, result.scanned());
        assertEquals(2, result.imported());
        assertEquals(1, result.stale());
        assertEquals(1, result.reactivated());
        assertEquals(List.of("sync warning"), result.warnings());
    }

    @Test
    void accumulatorAggregatesServerResultsAndWarnings() {
        McpToolDiscoverySyncResults.Accumulator accumulator = McpToolDiscoverySyncResults.accumulator();

        accumulator.addWarning("not due");
        accumulator.addServerResult(new McpToolDiscoveryServerSyncResult(
                2,
                1,
                0,
                List.of("docs warning")));
        accumulator.addServerResult(new McpToolDiscoveryServerSyncResult(
                3,
                0,
                2,
                List.of()));

        McpToolDiscoverySyncResult result = accumulator.toResult();
        assertEquals(2, result.scanned());
        assertEquals(5, result.imported());
        assertEquals(1, result.stale());
        assertEquals(2, result.reactivated());
        assertEquals(List.of("not due", "docs warning"), result.warnings());
    }

    @Test
    void registryModeDisabledUsesConsistentWarning() {
        McpToolDiscoverySyncResult result = McpToolDiscoverySyncResults.registryModeDisabled();

        assertEquals(0, result.scanned());
        assertEquals(List.of("Live MCP discovery sync skipped: MCP registry database mode is not enabled."),
                result.warnings());
    }
}

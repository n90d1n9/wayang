package tech.kayys.wayang.tool.mcp;

import java.util.ArrayList;
import java.util.List;

final class McpToolDiscoverySyncResults {

    private McpToolDiscoverySyncResults() {
    }

    static McpToolDiscoverySyncResult registryModeDisabled() {
        return warning(McpToolDiscoverySyncMessages.registryModeDisabled());
    }

    static McpToolDiscoverySyncResult warning(String warning) {
        return new McpToolDiscoverySyncResult(0, 0, List.of(warning));
    }

    static McpToolDiscoverySyncResult singleServer(McpToolDiscoveryServerSyncResult result) {
        return fromServer(1, result);
    }

    static McpToolDiscoverySyncResult serverError(String warning) {
        return new McpToolDiscoverySyncResult(1, 0, 0, 0, List.of(warning));
    }

    static McpToolDiscoverySyncResult fromServer(
            int scanned,
            McpToolDiscoveryServerSyncResult result) {
        return new McpToolDiscoverySyncResult(
                scanned,
                result.imported(),
                result.stale(),
                result.reactivated(),
                result.warnings());
    }

    static Accumulator accumulator() {
        return new Accumulator();
    }

    static final class Accumulator {
        private int scanned;
        private int imported;
        private int stale;
        private int reactivated;
        private final List<String> warnings = new ArrayList<>();

        void addWarning(String warning) {
            warnings.add(warning);
        }

        void addServerResult(McpToolDiscoveryServerSyncResult result) {
            scanned++;
            imported += result.imported();
            stale += result.stale();
            reactivated += result.reactivated();
            warnings.addAll(result.warnings());
        }

        McpToolDiscoverySyncResult toResult() {
            return new McpToolDiscoverySyncResult(
                    scanned,
                    imported,
                    stale,
                    reactivated,
                    warnings);
        }
    }
}

package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.util.Locale;

record McpToolDiscoverySyncHistoryFilters(
        String serverName,
        String status,
        int limit,
        int scanLimit) {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_SCAN_LIMIT = 500;
    private static final int FILTERED_SCAN_MULTIPLIER = 4;
    private static final int LATEST_SCAN_MULTIPLIER = 10;
    private static final McpHistoryPageWindowLimits LIMITS = new McpHistoryPageWindowLimits(
            0,
            0,
            DEFAULT_LIMIT,
            MAX_LIMIT,
            MAX_SCAN_LIMIT);

    static McpToolDiscoverySyncHistoryFilters of(
            String serverName,
            String status,
            int limit) {
        String normalizedServerName = normalizeServerName(serverName);
        String normalizedStatus = normalizeStatus(status);
        int boundedLimit = boundedLimit(limit);
        return new McpToolDiscoverySyncHistoryFilters(
                normalizedServerName,
                normalizedStatus,
                boundedLimit,
                listScanLimit(boundedLimit, normalizedServerName, normalizedStatus));
    }

    static McpToolDiscoverySyncHistoryFilters latest(
            String serverName,
            String status,
            int limit) {
        String normalizedServerName = normalizeServerName(serverName);
        String normalizedStatus = normalizeStatus(status);
        int boundedLimit = boundedLimit(limit);
        return new McpToolDiscoverySyncHistoryFilters(
                normalizedServerName,
                normalizedStatus,
                boundedLimit,
                latestScanLimit(boundedLimit));
    }

    boolean matches(RegistrySyncHistory history) {
        return McpToolDiscoverySyncHistorySource.isMcpToolHistory(history)
                && matchesServerName(history)
                && matchesStatus(history);
    }

    static int boundedLimit(int limit) {
        return McpHistoryPageWindows.boundedLimit(limit, LIMITS);
    }

    static int listScanLimit(int boundedLimit, String serverName, String status) {
        return McpHistoryPageWindows.filteredScanLimit(
                boundedLimit,
                LIMITS,
                FILTERED_SCAN_MULTIPLIER,
                hasFilter(serverName) || hasFilter(status));
    }

    static int latestScanLimit(int boundedLimit) {
        return McpHistoryPageWindows.expandedScanLimit(
                boundedLimit,
                LIMITS,
                LATEST_SCAN_MULTIPLIER);
    }

    private boolean matchesServerName(RegistrySyncHistory history) {
        return serverName == null || serverName.equals(normalizeServerName(history.getSourceRef()));
    }

    private boolean matchesStatus(RegistrySyncHistory history) {
        return status == null || status.equals(normalizeStatus(history.getStatus()));
    }

    private static boolean hasFilter(String value) {
        return McpHistoryFilterSupport.blankToNull(value) != null;
    }

    private static String normalizeServerName(String serverName) {
        String normalized = McpHistoryFilterSupport.blankToNull(serverName);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeStatus(String status) {
        String normalized = McpHistoryFilterSupport.blankToNull(status);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}

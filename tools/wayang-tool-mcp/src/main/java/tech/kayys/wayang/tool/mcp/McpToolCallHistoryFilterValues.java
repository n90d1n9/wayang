package tech.kayys.wayang.tool.mcp;

import java.util.Locale;

final class McpToolCallHistoryFilterValues {
    private McpToolCallHistoryFilterValues() {
    }

    static String normalizeToolId(String toolId) {
        return McpHistoryFilterSupport.blankToNull(toolId);
    }

    static String normalizeStatus(String status) {
        String normalized = McpHistoryFilterSupport.blankToNull(status);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    static String normalizeFailureType(String failureType) {
        String normalized = McpHistoryFilterSupport.blankToNull(failureType);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    static Long normalizeDurationBound(Long value) {
        if (value == null || value < 0) {
            return null;
        }
        return value;
    }
}

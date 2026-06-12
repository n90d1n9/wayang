package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

final class McpToolCallHistorySummaryKeys {
    private McpToolCallHistorySummaryKeys() {
    }

    static Instant sortFinishedAt(McpToolCallHistoryEntry entry) {
        if (entry == null || entry.finishedAt() == null) {
            return Instant.EPOCH;
        }
        return entry.finishedAt();
    }

    static String toolIdentityKey(McpToolCallHistoryEntry entry) {
        return entry == null ? "" : sortToolId(entry);
    }

    static String sortToolId(McpToolCallHistoryEntry entry) {
        return entry == null ? "" : nullSafe(entry.toolId());
    }

    static String defaultKey(String value) {
        return value == null || value.isBlank() ? "(unknown)" : value;
    }

    static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

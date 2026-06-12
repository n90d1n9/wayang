package tech.kayys.wayang.tool.mcp;

import java.util.Locale;

public final class McpServerActionRiskLevel {

    public static final String HIGH = "HIGH";
    public static final String LOW = "LOW";
    public static final String MEDIUM = "MEDIUM";
    public static final String UNKNOWN = "UNKNOWN";

    private McpServerActionRiskLevel() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    static String from(McpToolServerHealth.ActionQueueItem action) {
        String severity = McpIssueSeverity.normalize(action.severity());
        if (severity == null) {
            return UNKNOWN;
        }
        return switch (severity) {
            case McpIssueSeverity.CRITICAL -> HIGH;
            case McpIssueSeverity.WARNING -> MEDIUM;
            case McpIssueSeverity.INFO -> action.safeToAutomate() ? LOW : MEDIUM;
            default -> UNKNOWN;
        };
    }
}

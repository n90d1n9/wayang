package tech.kayys.wayang.tool.mcp;

import java.util.Locale;

public final class McpServerActionExecutionMode {

    public static final String AUTOMATABLE = "AUTOMATABLE";
    public static final String MANUAL = "MANUAL";
    public static final String REVIEW_REQUIRED = "REVIEW_REQUIRED";

    private McpServerActionExecutionMode() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    static String resolve(
            String executionMode,
            boolean safeToAutomate,
            boolean hasStructuredOperation) {
        String normalized = normalize(executionMode);
        if (normalized != null) {
            return normalized;
        }
        if (!hasStructuredOperation) {
            return MANUAL;
        }
        return safeToAutomate ? AUTOMATABLE : REVIEW_REQUIRED;
    }
}

package tech.kayys.wayang.tool.mcp;

import java.util.Locale;

final class McpServerHealthFilterValues {
    private McpServerHealthFilterValues() {
    }

    static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static String normalizeLifecycleState(String value) {
        return normalizeCode(value);
    }

    static String normalizeCode(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.replace('-', '_').toUpperCase(Locale.ROOT);
    }

    static String normalizeMethod(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    static Integer normalizeLimit(Integer value) {
        if (value == null) {
            return null;
        }
        return Math.max(0, value);
    }

    static Integer normalizeOffset(Integer value) {
        if (value == null) {
            return null;
        }
        return Math.max(0, value);
    }
}

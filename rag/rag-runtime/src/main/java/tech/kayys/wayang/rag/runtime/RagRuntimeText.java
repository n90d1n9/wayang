package tech.kayys.wayang.rag.runtime;

import java.util.Locale;

final class RagRuntimeText {

    private RagRuntimeText() {
    }

    static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    static String trimToDefault(String value, String defaultValue) {
        String normalized = trimToEmpty(value);
        return normalized.isBlank() ? defaultValue : normalized;
    }

    static String trimToLowerEmpty(String value) {
        return trimToEmpty(value).toLowerCase(Locale.ROOT);
    }
}

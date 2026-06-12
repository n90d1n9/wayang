package tech.kayys.wayang.a2ui.wayang.support;

/**
 * Small scalar normalization helpers for A2UI record constructors.
 */
public final class RecordValues {

    private RecordValues() {
    }

    public static String text(String value) {
        return value == null ? "" : value.trim();
    }

    public static String textOrDefault(String value, String fallback) {
        String normalized = text(value);
        return normalized.isBlank() ? text(fallback) : normalized;
    }
}

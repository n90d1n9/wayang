package tech.kayys.wayang.gollek.sdk;

/**
 * Small text normalization helpers shared across SDK models and wire envelopes.
 */
final class SdkText {

    private SdkText() {
    }

    static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    static String blankToNull(String value) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }
}

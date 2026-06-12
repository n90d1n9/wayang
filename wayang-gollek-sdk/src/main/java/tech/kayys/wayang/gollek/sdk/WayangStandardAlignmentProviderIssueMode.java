package tech.kayys.wayang.gollek.sdk;

import java.util.Locale;

/**
 * Readiness behavior for standard-alignment provider issues.
 */
public enum WayangStandardAlignmentProviderIssueMode {
    IGNORE,
    WARN,
    BLOCK;

    public static WayangStandardAlignmentProviderIssueMode fromId(String id) {
        return switch (key(id)) {
            case "ignore", "ignored", "off", "none", "disabled", "disable" -> IGNORE;
            case "block", "blocked", "fail", "failure", "error", "required" -> BLOCK;
            default -> WARN;
        };
    }

    public String id() {
        return switch (this) {
            case IGNORE -> "ignore";
            case WARN -> "warn";
            case BLOCK -> "block";
        };
    }

    private static String key(String value) {
        return SdkText.trimToEmpty(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }
}

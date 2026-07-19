package tech.kayys.wayang.tool.mcp;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;

final class McpHistoryRetention {

    static final Duration DEFAULT_RETENTION = Duration.ofDays(7);
    static final int DEFAULT_MAX_ENTRIES = 500;
    static final int MAX_CONFIGURED_ENTRIES = 10_000;

    private McpHistoryRetention() {
    }

    static Duration retentionFromConfig(String retention) {
        return retentionFromConfig(retention, DEFAULT_RETENTION);
    }

    static Duration retentionFromConfig(String retention, Duration defaultRetention) {
        Duration effectiveDefault = normalizeRetention(defaultRetention, DEFAULT_RETENTION);
        if (retention == null || retention.isBlank()) {
            return effectiveDefault;
        }
        String value = retention.trim();
        try {
            return normalizeRetention(Duration.parse(value), effectiveDefault);
        } catch (DateTimeParseException ignored) {
            return normalizeRetention(simpleDuration(value, effectiveDefault), effectiveDefault);
        }
    }

    static Duration normalizeRetention(Duration retention) {
        return normalizeRetention(retention, DEFAULT_RETENTION);
    }

    static Duration normalizeRetention(Duration retention, Duration defaultRetention) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            return defaultRetention == null || defaultRetention.isZero() || defaultRetention.isNegative()
                    ? DEFAULT_RETENTION
                    : defaultRetention;
        }
        return retention;
    }

    static int normalizeMaxEntries(int maxEntries) {
        return normalizeMaxEntries(maxEntries, DEFAULT_MAX_ENTRIES, MAX_CONFIGURED_ENTRIES);
    }

    static int normalizeMaxEntries(
            int maxEntries,
            int defaultMaxEntries,
            int maxConfiguredEntries) {
        int effectiveDefault = defaultMaxEntries <= 0 ? DEFAULT_MAX_ENTRIES : defaultMaxEntries;
        int effectiveCap = maxConfiguredEntries <= 0 ? MAX_CONFIGURED_ENTRIES : maxConfiguredEntries;
        if (maxEntries <= 0) {
            return Math.min(effectiveDefault, effectiveCap);
        }
        return Math.min(maxEntries, effectiveCap);
    }

    private static Duration simpleDuration(String value, Duration defaultRetention) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (normalized.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()));
            }
            if (normalized.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(normalized.substring(0, normalized.length() - 1).trim()));
            }
            if (normalized.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(normalized.substring(0, normalized.length() - 1).trim()));
            }
            if (normalized.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(normalized.substring(0, normalized.length() - 1).trim()));
            }
            if (normalized.endsWith("d")) {
                return Duration.ofDays(Long.parseLong(normalized.substring(0, normalized.length() - 1).trim()));
            }
            return Duration.ofSeconds(Long.parseLong(normalized));
        } catch (NumberFormatException ignored) {
            return defaultRetention;
        }
    }
}

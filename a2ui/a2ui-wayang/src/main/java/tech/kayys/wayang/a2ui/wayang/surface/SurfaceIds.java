package tech.kayys.wayang.a2ui.wayang.surface;

import java.util.Locale;

/**
 * Stable surface-id helpers for Wayang A2UI surfaces.
 */
public final class SurfaceIds {

    private static final String UNKNOWN = "unknown";

    private SurfaceIds() {
    }

    public static String runStatus(String runId) {
        return "wayang-run-" + safe(runId);
    }

    public static String runEvents(String runId) {
        return "wayang-run-events-" + safe(runId);
    }

    public static String runHistory() {
        return "wayang-run-history";
    }

    public static String runHistoryRow(String runId) {
        return runHistory() + "-run-" + safe(runId);
    }

    public static String actionResult(int sequence, String actionName) {
        return "wayang-action-result-" + sequence + "-" + safe(actionName, "action");
    }

    public static String safe(String value) {
        return safe(value, UNKNOWN);
    }

    public static String safe(String value, String fallback) {
        String resolvedFallback = fallback == null || fallback.isBlank() ? UNKNOWN : fallback.trim();
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return resolvedFallback;
        }
        String safe = normalized.replaceAll("[^a-z0-9_-]+", "-");
        return safe.isBlank() ? resolvedFallback : safe;
    }
}

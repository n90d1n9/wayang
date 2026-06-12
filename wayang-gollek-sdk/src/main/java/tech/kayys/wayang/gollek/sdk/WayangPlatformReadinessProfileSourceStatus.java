package tech.kayys.wayang.gollek.sdk;

/**
 * Operator-facing status for one readiness profile source during registry resolution.
 *
 * <p>The status records whether a source was selected, whether fallback was
 * used, and the redacted source diagnostics consumed by platform readiness and
 * preflight reports.</p>
 */
public record WayangPlatformReadinessProfileSourceStatus(
        String sourceId,
        String sourceType,
        String location,
        boolean selected,
        boolean fallback,
        boolean available,
        boolean valid,
        int profileCount,
        int issueCount,
        String message) {

    public WayangPlatformReadinessProfileSourceStatus {
        sourceId = SdkText.trimToDefault(sourceId, "unknown");
        sourceType = SdkText.trimToDefault(sourceType, "unknown");
        location = redact(location);
        profileCount = Math.max(0, profileCount);
        issueCount = Math.max(0, issueCount);
        message = redact(message);
    }

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }
}

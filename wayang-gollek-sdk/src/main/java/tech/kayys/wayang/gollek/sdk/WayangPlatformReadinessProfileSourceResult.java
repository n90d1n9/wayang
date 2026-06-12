package tech.kayys.wayang.gollek.sdk;

import java.util.List;

/**
 * Immutable result produced by a readiness profile source load attempt.
 *
 * <p>The result keeps loaded profiles for registry selection and exposes
 * redacted source diagnostics for readiness reports, preflight output, and CLI
 * payloads.</p>
 */
public record WayangPlatformReadinessProfileSourceResult(
        String sourceId,
        String sourceType,
        String location,
        boolean available,
        List<WayangPlatformReadinessProfileDescriptor> profiles,
        String message) {

    public WayangPlatformReadinessProfileSourceResult {
        sourceId = SdkText.trimToDefault(sourceId, "unknown");
        sourceType = SdkText.trimToDefault(sourceType, "unknown");
        location = redact(location);
        profiles = profiles == null || profiles.isEmpty()
                ? List.of()
                : profiles.stream()
                        .filter(profile -> profile != null)
                        .toList();
        message = redact(message);
    }

    public static WayangPlatformReadinessProfileSourceResult available(
            String sourceId,
            String sourceType,
            String location,
            List<WayangPlatformReadinessProfileDescriptor> profiles,
            String message) {
        return new WayangPlatformReadinessProfileSourceResult(
                sourceId,
                sourceType,
                location,
                true,
                profiles,
                message);
    }

    public static WayangPlatformReadinessProfileSourceResult unavailable(
            String sourceId,
            String sourceType,
            String location,
            String message) {
        return new WayangPlatformReadinessProfileSourceResult(
                sourceId,
                sourceType,
                location,
                false,
                List.of(),
                message);
    }

    public int profileCount() {
        return profiles.size();
    }

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }
}

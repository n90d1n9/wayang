package tech.kayys.wayang.gollek.sdk;

public record WayangPlatformReadinessProfileValidationIssue(
        String kind,
        String message,
        String profileId,
        String readinessId) {

    public WayangPlatformReadinessProfileValidationIssue {
        kind = normalizeRequired("Issue kind", kind);
        message = normalizeRequired("Issue message", message);
        profileId = SdkText.trimToEmpty(profileId);
        readinessId = SdkText.trimToEmpty(readinessId);
    }

    private static String normalizeRequired(String label, String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }
}

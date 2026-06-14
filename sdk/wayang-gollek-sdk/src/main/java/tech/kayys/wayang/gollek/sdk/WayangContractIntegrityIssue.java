package tech.kayys.wayang.gollek.sdk;

public record WayangContractIntegrityIssue(
        String kind,
        String message,
        String schema,
        int version,
        String envelope,
        String commandId) {

    public WayangContractIntegrityIssue {
        kind = normalizeRequired("Issue kind", kind);
        message = normalizeRequired("Issue message", message);
        schema = SdkText.trimToEmpty(schema);
        version = Math.max(0, version);
        envelope = SdkText.trimToEmpty(envelope);
        commandId = SdkText.trimToEmpty(commandId);
    }

    private static String normalizeRequired(String label, String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }
}

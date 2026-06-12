package tech.kayys.wayang.gollek.sdk.storage.s3;

import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Diagnostic report for config-driven S3-compatible credential selection.
 */
public record WayangReadinessProfileS3CredentialsResolution(
        String credentialsRef,
        String provider,
        String selectedCredentialsId,
        String selectedBy,
        boolean available,
        List<String> credentialsIds,
        String message) {

    public WayangReadinessProfileS3CredentialsResolution {
        credentialsRef = normalize(credentialsRef);
        provider = normalize(provider);
        selectedCredentialsId = normalize(selectedCredentialsId);
        selectedBy = selectedBy == null ? "" : selectedBy.trim();
        credentialsIds = credentialsIds == null || credentialsIds.isEmpty()
                ? List.of()
                : credentialsIds.stream()
                        .filter(id -> id != null && !id.isBlank())
                        .map(WayangReadinessProfileS3CredentialsResolution::normalize)
                        .toList();
        message = message == null ? "" : message.trim();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("credentialsRef", redact(credentialsRef));
        values.put("provider", provider);
        values.put("selectedCredentialsId", redact(selectedCredentialsId));
        values.put("selectedBy", selectedBy);
        values.put("available", available);
        values.put("credentialsIds", credentialsIds.stream()
                .map(WayangReadinessProfileS3CredentialsResolution::redact)
                .toList());
        values.put("message", redact(message));
        return java.util.Collections.unmodifiableMap(values);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }
}

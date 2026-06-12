package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Diagnostic report for config-driven readiness profile object-storage service selection.
 */
public record WayangReadinessProfileObjectStorageServiceResolution(
        String credentialsRef,
        String provider,
        String selectedServiceId,
        String selectedBy,
        boolean available,
        List<String> serviceIds,
        String message) {

    public WayangReadinessProfileObjectStorageServiceResolution {
        credentialsRef = normalize(credentialsRef);
        provider = normalize(provider);
        selectedServiceId = normalize(selectedServiceId);
        selectedBy = selectedBy == null ? "" : selectedBy.trim();
        serviceIds = serviceIds == null || serviceIds.isEmpty()
                ? List.of()
                : serviceIds.stream()
                        .filter(id -> id != null && !id.isBlank())
                        .map(WayangReadinessProfileObjectStorageServiceResolution::normalize)
                        .toList();
        message = message == null ? "" : message.trim();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("credentialsRef", redact(credentialsRef));
        values.put("provider", provider);
        values.put("selectedServiceId", redact(selectedServiceId));
        values.put("selectedBy", selectedBy);
        values.put("available", available);
        values.put("serviceIds", serviceIds.stream()
                .map(WayangReadinessProfileObjectStorageServiceResolution::redact)
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

package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provider-level diagnostics for readiness profile object-storage service discovery.
 */
public record WayangReadinessProfileObjectStorageServiceProviderDiagnostics(
        String providerId,
        int priority,
        boolean available,
        List<String> serviceIds,
        Map<String, Object> details,
        String message) {

    public WayangReadinessProfileObjectStorageServiceProviderDiagnostics {
        providerId = trimToDefault(providerId, "unknown").toLowerCase(Locale.ROOT);
        priority = Math.max(0, priority);
        serviceIds = copyServiceIds(serviceIds);
        details = copyDetails(details);
        message = redact(message);
    }

    public int serviceCount() {
        return serviceIds.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerId", providerId);
        values.put("priority", priority);
        values.put("available", available);
        values.put("serviceCount", serviceCount());
        values.put("serviceIds", serviceIds.stream()
                .map(WayangReadinessProfileObjectStorageServiceProviderDiagnostics::redact)
                .toList());
        values.put("message", message);
        values.put("details", details);
        return java.util.Collections.unmodifiableMap(values);
    }

    private static List<String> copyServiceIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(WayangReadinessProfileObjectStorageServiceProviderDiagnostics::trimToEmpty)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static Map<String, Object> copyDetails(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return WayangSecretRedactor.diagnosticMap(values);
    }

    private static String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToEmpty(value);
        return trimmed.isBlank() ? defaultValue : trimmed;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }
}

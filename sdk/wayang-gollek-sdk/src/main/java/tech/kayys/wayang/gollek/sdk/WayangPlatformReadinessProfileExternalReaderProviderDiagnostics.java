package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Redacted provider-level diagnostics for readiness profile external reader discovery.
 *
 * <p>The record keeps SDK and CLI diagnostics provider-neutral while allowing
 * optional database, object-storage, or hybrid modules to attach their own
 * nested detail maps.</p>
 */
public record WayangPlatformReadinessProfileExternalReaderProviderDiagnostics(
        String providerId,
        String providerClass,
        int priority,
        boolean available,
        List<String> readerTypes,
        String message,
        Map<String, Object> details) {

    public WayangPlatformReadinessProfileExternalReaderProviderDiagnostics {
        providerId = SdkText.trimToDefault(providerId, "unknown");
        providerClass = SdkText.trimToEmpty(providerClass);
        priority = Math.max(0, priority);
        readerTypes = copyReaderTypes(readerTypes);
        message = redact(SdkText.trimToEmpty(message));
        details = copyDetails(details);
    }

    public static WayangPlatformReadinessProfileExternalReaderProviderDiagnostics failure(
            String providerId,
            String providerClass,
            int priority,
            Throwable error) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (error != null) {
            details.put("errorType", error.getClass().getName());
        }
        return new WayangPlatformReadinessProfileExternalReaderProviderDiagnostics(
                providerId,
                providerClass,
                priority,
                false,
                List.of(),
                failureMessage(error),
                details);
    }

    public int readerTypeCount() {
        return readerTypes.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerId", providerId);
        values.put("providerClass", providerClass);
        values.put("priority", priority);
        values.put("available", available);
        values.put("readerTypeCount", readerTypeCount());
        values.put("readerTypes", readerTypes);
        values.put("message", message);
        values.put("details", details);
        return SdkMaps.orderedCopy(values);
    }

    private static String failureMessage(Throwable error) {
        if (error == null) {
            return "Readiness profile external reader provider failed.";
        }
        String message = SdkText.trimToEmpty(error.getMessage());
        if (message.isBlank()) {
            return "Readiness profile external reader provider failed: "
                    + error.getClass().getSimpleName()
                    + ".";
        }
        return "Readiness profile external reader provider failed: " + redact(message) + ".";
    }

    private static List<String> copyReaderTypes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(SdkText::trimToEmpty)
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

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }
}

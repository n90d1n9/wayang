package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Diagnostic summary for one discovered coding-agent extension.
 */
public record WayangCodeAgentExtensionDiagnostics(
        String extensionId,
        String extensionClass,
        String name,
        String edition,
        int priority,
        boolean available,
        List<String> capabilityTags,
        String message,
        Map<String, Object> details) {

    public WayangCodeAgentExtensionDiagnostics {
        extensionId = normalizeIdentifier(extensionId, "unknown");
        extensionClass = SdkText.trimToEmpty(extensionClass);
        name = SdkText.trimToDefault(name, extensionId);
        edition = normalizeIdentifier(edition, "oss");
        capabilityTags = normalizeTags(capabilityTags);
        message = SdkText.trimToEmpty(message);
        details = SdkMaps.copy(details);
    }

    public static WayangCodeAgentExtensionDiagnostics failure(
            String extensionId,
            String extensionClass,
            int priority,
            Throwable error) {
        String message = error == null ? "Extension failed." : error.getMessage();
        return new WayangCodeAgentExtensionDiagnostics(
                extensionId,
                extensionClass,
                extensionId,
                "unknown",
                priority,
                false,
                List.of(),
                SdkText.trimToDefault(message, "Extension failed."),
                Map.of("errorType", error == null ? "unknown" : error.getClass().getName()));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("extensionId", extensionId);
        values.put("extensionClass", extensionClass);
        values.put("name", name);
        values.put("edition", edition);
        values.put("priority", priority);
        values.put("available", available);
        values.put("capabilityTags", capabilityTags);
        values.put("message", message);
        values.put("details", details);
        return SdkMaps.orderedCopy(values);
    }

    private static String normalizeIdentifier(String value, String defaultValue) {
        String normalized = SdkText.trimToEmpty(value).toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    private static List<String> normalizeTags(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> tags = new LinkedHashSet<>();
        values.forEach(value -> {
            String tag = normalizeIdentifier(value, "");
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        });
        return tags.isEmpty() ? List.of() : List.copyOf(tags);
    }
}

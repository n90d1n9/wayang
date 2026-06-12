package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record WayangProviderCapabilityDescriptor(
        String id,
        String providerId,
        String providerNamespace,
        String moduleId,
        String capabilityType,
        String name,
        String description,
        WayangProviderCapabilityState state,
        List<String> surfaceIds,
        List<String> standardIds,
        List<String> tags,
        Map<String, Object> metadata) {

    public WayangProviderCapabilityDescriptor {
        id = requireIdentifier("Provider capability id", id);
        providerId = requireIdentifier("Provider id", providerId);
        providerNamespace = normalizeDefaultIdentifier(providerNamespace, "wayang");
        moduleId = normalizeDefaultIdentifier(moduleId, "core");
        capabilityType = normalizeDefaultIdentifier(capabilityType, "general");
        name = requireNonBlank("Provider capability name", name);
        description = SdkText.trimToEmpty(description);
        state = state == null ? WayangProviderCapabilityState.AVAILABLE : state;
        surfaceIds = normalizeIdentifiers(surfaceIds);
        standardIds = normalizeStandardIds(standardIds);
        tags = normalizeIdentifiers(tags);
        metadata = SdkMaps.copy(metadata);
    }

    public boolean available() {
        return state.available();
    }

    public boolean supportsSurface(String surfaceId) {
        String normalized = normalizeIdentifier(surfaceId);
        return normalized.isEmpty() || surfaceIds.isEmpty() || surfaceIds.contains(normalized);
    }

    public boolean supportsStandard(String standardId) {
        String normalized = normalizeStandardId(standardId);
        return normalized.isEmpty() || (!standardIds.isEmpty() && standardIds.contains(normalized));
    }

    public boolean hasTag(String tag) {
        String normalized = normalizeIdentifier(tag);
        return !normalized.isEmpty() && tags.contains(normalized);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("providerId", providerId);
        values.put("providerNamespace", providerNamespace);
        values.put("moduleId", moduleId);
        values.put("capabilityType", capabilityType);
        values.put("name", name);
        values.put("description", description);
        values.put("state", state.id());
        values.put("available", available());
        values.put("surfaceIds", surfaceIds);
        values.put("standardIds", standardIds);
        values.put("tags", tags);
        values.put("metadata", metadata);
        return SdkMaps.orderedCopy(values);
    }

    private static String requireNonBlank(String label, String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }

    private static String requireIdentifier(String label, String value) {
        String normalized = normalizeIdentifier(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }

    private static String normalizeDefaultIdentifier(String value, String defaultValue) {
        String normalized = normalizeIdentifier(value);
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    private static String normalizeIdentifier(String value) {
        return SdkText.trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private static List<String> normalizeIdentifiers(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String item = normalizeIdentifier(value);
            if (!item.isEmpty()) {
                normalized.add(item);
            }
        }
        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    private static List<String> normalizeStandardIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String item = normalizeStandardId(value);
            if (!item.isEmpty()) {
                normalized.add(item);
            }
        }
        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    private static String normalizeStandardId(String value) {
        String normalized = normalizeIdentifier(value);
        return normalized.isEmpty() ? "" : WayangStandardRegistry.canonicalId(normalized);
    }
}

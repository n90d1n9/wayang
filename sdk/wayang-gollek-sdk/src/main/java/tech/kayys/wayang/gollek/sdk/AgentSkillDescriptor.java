package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record AgentSkillDescriptor(
        String id,
        String name,
        String description,
        String category,
        String source,
        String version,
        List<String> surfaceIds,
        List<String> inputKeys,
        List<String> outputKeys,
        List<String> tags,
        Map<String, Object> metadata) {

    public AgentSkillDescriptor {
        id = normalizeIdentifier("Skill id", id);
        name = requireNonBlank("Skill name", name);
        description = SdkText.trimToEmpty(description);
        category = SdkText.trimToDefault(category, "General");
        source = normalizeDefaultIdentifier(source, "builtin");
        version = SdkText.trimToDefault(version, "1.0.0");
        surfaceIds = normalizeIdentifiers(surfaceIds);
        inputKeys = normalizeValues(inputKeys);
        outputKeys = normalizeValues(outputKeys);
        tags = normalizeIdentifiers(tags);
        metadata = SdkMaps.copy(metadata);
    }

    public static AgentSkillDescriptor of(
            String id,
            String name,
            String description,
            String category,
            String source,
            List<String> surfaceIds) {
        return new AgentSkillDescriptor(
                id,
                name,
                description,
                category,
                source,
                "1.0.0",
                surfaceIds,
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    public boolean supportsSurface(String surfaceId) {
        String normalized = normalizeOptionalIdentifier(surfaceId);
        return normalized.isEmpty() || surfaceIds.isEmpty() || surfaceIds.contains(normalized);
    }

    public boolean hasInputKey(String inputKey) {
        return containsIgnoreCase(inputKeys, inputKey);
    }

    public boolean hasOutputKey(String outputKey) {
        return containsIgnoreCase(outputKeys, outputKey);
    }

    public boolean hasTag(String tag) {
        String normalized = normalizeOptionalIdentifier(tag);
        return !normalized.isEmpty() && tags.contains(normalized);
    }

    private static boolean containsIgnoreCase(List<String> values, String value) {
        String normalized = SdkText.trimToEmpty(value);
        return !normalized.isEmpty() && values.stream().anyMatch(item -> item.equalsIgnoreCase(normalized));
    }

    private static String requireNonBlank(String label, String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }

    private static String normalizeIdentifier(String label, String value) {
        String normalized = normalizeOptionalIdentifier(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }

    private static String normalizeDefaultIdentifier(String value, String defaultValue) {
        String normalized = normalizeOptionalIdentifier(value);
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    private static String normalizeOptionalIdentifier(String value) {
        return SdkText.trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private static List<String> normalizeIdentifiers(List<String> values) {
        return normalize(values, true);
    }

    private static List<String> normalizeValues(List<String> values) {
        return normalize(values, false);
    }

    private static List<String> normalize(List<String> values, boolean lowercase) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String item = SdkText.trimToEmpty(value);
            if (lowercase) {
                item = item.toLowerCase(Locale.ROOT);
            }
            if (!item.isEmpty()) {
                normalized.add(item);
            }
        }
        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }
}

package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Object-storage provider setting family used for lightweight config readiness checks.
 */
record SkillManagementObjectStorageProviderConfigFamily(
        SkillManagementObjectStorageProviderKind kind,
        List<String> requiredKeys,
        List<String> optionalKeys) {

    SkillManagementObjectStorageProviderConfigFamily {
        kind = Objects.requireNonNull(kind, "kind");
        requiredKeys = SkillManagementValueSupport.compactStrings(requiredKeys);
        optionalKeys = SkillManagementValueSupport.compactStrings(optionalKeys);
    }

    String id() {
        return kind.configName();
    }

    String label() {
        return kind.readinessLabel();
    }

    String summaryLabel() {
        return kind.summaryLabel();
    }

    boolean configured(Map<String, String> normalized) {
        return requiredKeys.stream().anyMatch(key -> hasValue(normalized, key))
                || optionalKeys.stream().anyMatch(key -> hasValue(normalized, key));
    }

    List<String> missingRequiredKeys(Map<String, String> normalized) {
        return requiredKeys.stream()
                .filter(key -> !hasValue(normalized, key))
                .toList();
    }

    List<String> warnings(Map<String, String> normalized) {
        List<String> missing = missingRequiredKeys(normalized);
        if (missing.isEmpty()) {
            return List.of();
        }
        return List.of(label() + " object-storage provider settings are incomplete: missing "
                + String.join(", ", missing) + ".");
    }

    private static boolean hasValue(Map<String, String> normalized, String key) {
        if (normalized == null || normalized.isEmpty()) {
            return false;
        }
        String value = normalized.get(SkillStoreConfigValues.normalize(key));
        return value != null && !value.isBlank();
    }
}

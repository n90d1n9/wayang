package tech.kayys.wayang.agent.skills.management;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ordered provider key groups for samples and readiness checks.
 */
record SkillManagementObjectStorageProviderConfigKeySet(
        List<SkillManagementObjectStorageProviderConfigKey> sampleKeys,
        List<SkillManagementObjectStorageProviderConfigKey> readinessKeys) {

    SkillManagementObjectStorageProviderConfigKeySet {
        sampleKeys = copy(sampleKeys);
        readinessKeys = copy(readinessKeys);
        requireUniqueProperties("sample", sampleKeys);
        requireUniqueProperties("readiness", readinessKeys);
        requireReadinessKeysInSamples(sampleKeys, readinessKeys);
    }

    List<SkillManagementRuntimeConfigSampleEntry> sampleEntries(boolean environment) {
        return sampleKeys.stream()
                .map(key -> key.sampleEntry(environment))
                .toList();
    }

    List<String> requiredProperties() {
        return properties(readinessKeys.stream()
                .filter(SkillManagementObjectStorageProviderConfigKey::required)
                .toList());
    }

    List<String> optionalProperties() {
        return properties(readinessKeys.stream()
                .filter(key -> !key.required())
                .toList());
    }

    private static List<SkillManagementObjectStorageProviderConfigKey> copy(
            List<SkillManagementObjectStorageProviderConfigKey> keys) {
        return keys == null || keys.isEmpty()
                ? List.of()
                : List.copyOf(keys);
    }

    private static List<String> properties(List<SkillManagementObjectStorageProviderConfigKey> keys) {
        return keys.stream()
                .map(SkillManagementObjectStorageProviderConfigKey::property)
                .toList();
    }

    private static void requireUniqueProperties(
            String label,
            List<SkillManagementObjectStorageProviderConfigKey> keys) {
        Set<String> seen = new LinkedHashSet<>();
        keys.forEach(key -> {
            if (!seen.add(key.property())) {
                throw new IllegalArgumentException(
                        "Duplicate object-storage provider " + label + " key: " + key.property());
            }
        });
    }

    private static void requireReadinessKeysInSamples(
            List<SkillManagementObjectStorageProviderConfigKey> sampleKeys,
            List<SkillManagementObjectStorageProviderConfigKey> readinessKeys) {
        Set<String> sampleProperties = new LinkedHashSet<>(properties(sampleKeys));
        readinessKeys.forEach(key -> {
            if (!sampleProperties.contains(key.property())) {
                throw new IllegalArgumentException(
                        "Readiness object-storage provider key is not part of sample keys: "
                                + key.property());
            }
        });
    }
}

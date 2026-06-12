package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Provider-family readiness assessment for object-storage runtime settings.
 */
public record SkillManagementObjectStorageProviderConfigAssessment(
        List<String> configuredProviders,
        List<String> warnings) {

    public SkillManagementObjectStorageProviderConfigAssessment {
        configuredProviders = SkillManagementValueSupport.compactStrings(configuredProviders);
        warnings = SkillManagementValueSupport.compactStrings(warnings);
    }

    public static SkillManagementObjectStorageProviderConfigAssessment fromRuntime() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.putAll(System.getenv());
        values.putAll(SkillStoreConfigValues.fromProperties(System.getProperties()));
        return fromMap(values);
    }

    public static SkillManagementObjectStorageProviderConfigAssessment fromProperties(Properties properties) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties));
    }

    public static SkillManagementObjectStorageProviderConfigAssessment fromEnvironment(
            Map<String, String> environment) {
        return fromMap(environment);
    }

    public static SkillManagementObjectStorageProviderConfigAssessment fromMap(Map<String, ?> values) {
        Map<String, String> normalized = values == null || values.isEmpty()
                ? Map.of()
                : SkillStoreConfigValues.flattenAndNormalize(values);
        List<SkillManagementObjectStorageProviderConfigFamily> configured =
                SkillManagementObjectStorageProviderConfigFamilies.all().stream()
                .filter(family -> family.configured(normalized))
                .toList();
        List<String> warnings = new ArrayList<>();
        if (configured.isEmpty()) {
            warnings.add("Object-storage persistence is selected but no "
                    + SkillManagementObjectStorageProviderConfigFamilies.summaryLabel()
                    + " provider settings were detected.");
        }
        if (configured.size() > 1) {
            warnings.add("Multiple object-storage provider setting families detected: "
                    + String.join(", ", configured.stream()
                            .map(SkillManagementObjectStorageProviderConfigFamily::id)
                            .toList())
                    + ". Keep only the active provider family configured.");
        }
        configured.forEach(family -> warnings.addAll(family.warnings(normalized)));
        return new SkillManagementObjectStorageProviderConfigAssessment(
                configured.stream()
                        .map(SkillManagementObjectStorageProviderConfigFamily::id)
                        .toList(),
                warnings);
    }

    public boolean hasConfiguredProvider() {
        return !configuredProviders.isEmpty();
    }

    public int warningCount() {
        return warnings.size();
    }
}

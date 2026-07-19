package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Catalog of object-storage provider setting families known to skill management.
 */
final class SkillManagementObjectStorageProviderConfigFamilies {

    private static final List<SkillManagementObjectStorageProviderConfigFamily> FAMILIES = List.of(
            family(
                    SkillManagementObjectStorageProviderKind.S3_RUSTFS,
                    SkillManagementObjectStorageProviderConfigHints.requiredProperties(
                            SkillManagementObjectStorageProviderKind.S3_RUSTFS),
                    SkillManagementObjectStorageProviderConfigHints.optionalProperties(
                            SkillManagementObjectStorageProviderKind.S3_RUSTFS)),
            family(
                    SkillManagementObjectStorageProviderKind.GCS,
                    SkillManagementObjectStorageProviderConfigHints.requiredProperties(
                            SkillManagementObjectStorageProviderKind.GCS),
                    SkillManagementObjectStorageProviderConfigHints.optionalProperties(
                            SkillManagementObjectStorageProviderKind.GCS)),
            family(
                    SkillManagementObjectStorageProviderKind.AZURE,
                    SkillManagementObjectStorageProviderConfigHints.requiredProperties(
                            SkillManagementObjectStorageProviderKind.AZURE),
                    SkillManagementObjectStorageProviderConfigHints.optionalProperties(
                            SkillManagementObjectStorageProviderKind.AZURE)));

    private SkillManagementObjectStorageProviderConfigFamilies() {
    }

    static List<SkillManagementObjectStorageProviderConfigFamily> all() {
        return FAMILIES;
    }

    static String summaryLabel() {
        return readableList(FAMILIES.stream()
                .map(SkillManagementObjectStorageProviderConfigFamily::summaryLabel)
                .toList());
    }

    private static SkillManagementObjectStorageProviderConfigFamily family(
            SkillManagementObjectStorageProviderKind kind,
            List<String> requiredKeys,
            List<String> optionalKeys) {
        return new SkillManagementObjectStorageProviderConfigFamily(
                kind,
                requiredKeys,
                optionalKeys);
    }

    private static String readableList(List<String> labels) {
        if (labels.isEmpty()) {
            return "";
        }
        if (labels.size() == 1) {
            return labels.get(0);
        }
        if (labels.size() == 2) {
            return labels.get(0) + " or " + labels.get(1);
        }
        return String.join(", ", labels.subList(0, labels.size() - 1))
                + ", or "
                + labels.get(labels.size() - 1);
    }
}

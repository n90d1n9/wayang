package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Object-storage provider vocabulary used by runtime config sample generation.
 */
enum SkillManagementRuntimeConfigSampleProvider {
    NONE(null),
    S3_RUSTFS(SkillManagementObjectStorageProviderKind.S3_RUSTFS),
    GCS(SkillManagementObjectStorageProviderKind.GCS),
    AZURE(SkillManagementObjectStorageProviderKind.AZURE);

    private final SkillManagementObjectStorageProviderKind kind;

    SkillManagementRuntimeConfigSampleProvider(SkillManagementObjectStorageProviderKind kind) {
        this.kind = kind;
    }

    String configName() {
        return kind == null ? "" : kind.configName();
    }

    String label() {
        return kind == null ? "" : kind.sampleLabel();
    }

    SkillManagementObjectStorageProviderKind kind() {
        return kind;
    }

    List<SkillManagementRuntimeConfigSampleEntry> sampleEntries(boolean environment) {
        return kind == null
                ? List.of()
                : SkillManagementObjectStorageProviderConfigHints.sampleEntries(kind, environment);
    }

    static SkillManagementRuntimeConfigSampleProvider defaultForProfile(
            SkillManagementServiceProfile profile) {
        if (profile == null) {
            return NONE;
        }
        return switch (profile) {
            case OBJECT_STORAGE, HYBRID_OBJECT_FILE, MIRRORED_OBJECT_FILE -> S3_RUSTFS;
            default -> NONE;
        };
    }
}

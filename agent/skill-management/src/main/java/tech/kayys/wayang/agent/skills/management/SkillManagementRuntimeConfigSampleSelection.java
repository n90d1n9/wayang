package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Resolved runtime config sample target before properties/environment rendering.
 */
record SkillManagementRuntimeConfigSampleSelection(
        SkillManagementServiceProfileDescriptor descriptor,
        SkillManagementRuntimeConfigSampleProvider objectStorageProvider,
        String description) {

    SkillManagementRuntimeConfigSampleSelection {
        descriptor = Objects.requireNonNull(descriptor, "descriptor");
        objectStorageProvider = objectStorageProvider == null
                ? SkillManagementRuntimeConfigSampleProvider.NONE
                : objectStorageProvider;
        description = description == null ? "" : description.trim();
    }
}

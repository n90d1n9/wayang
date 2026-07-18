package tech.kayys.wayang.agent.skills.management;

/**
 * One object-storage provider setting key shared by hints, samples, and readiness checks.
 */
record SkillManagementObjectStorageProviderConfigKey(
        String property,
        String environment,
        String defaultValue,
        String sampleDescription,
        boolean required) {

    SkillManagementObjectStorageProviderConfigKey {
        property = requiredText(property, "property");
        environment = requiredText(environment, "environment");
        defaultValue = defaultValue == null ? "" : defaultValue.trim();
        sampleDescription = sampleDescription == null ? "" : sampleDescription.trim();
    }

    SkillManagementRuntimeConfigSampleEntry sampleEntry(boolean environmentFormat) {
        return new SkillManagementRuntimeConfigSampleEntry(
                environmentFormat ? environment : property,
                defaultValue,
                sampleDescription);
    }

    private static String requiredText(String value, String name) {
        String resolved = value == null ? "" : value.trim();
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return resolved;
    }
}

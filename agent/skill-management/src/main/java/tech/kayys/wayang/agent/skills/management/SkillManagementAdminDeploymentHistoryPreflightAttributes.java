package tech.kayys.wayang.agent.skills.management;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

/**
 * Deployment history preflight fields decoded from event attributes.
 */
record SkillManagementAdminDeploymentHistoryPreflightAttributes(
        boolean available,
        boolean ready,
        boolean deployable,
        int errors,
        int configurationErrors,
        int targetStoreErrors,
        int sourceStoreErrors,
        int capabilityErrors,
        String message) {

    SkillManagementAdminDeploymentHistoryPreflightAttributes {
        errors = SkillManagementAdminValueSupport.nonNegative(errors);
        configurationErrors = SkillManagementAdminValueSupport.nonNegative(configurationErrors);
        targetStoreErrors = SkillManagementAdminValueSupport.nonNegative(targetStoreErrors);
        sourceStoreErrors = SkillManagementAdminValueSupport.nonNegative(sourceStoreErrors);
        capabilityErrors = SkillManagementAdminValueSupport.nonNegative(capabilityErrors);
        message = SkillManagementAdminValueSupport.text(message);
    }

    static SkillManagementAdminDeploymentHistoryPreflightAttributes empty() {
        return from(null);
    }

    static SkillManagementAdminDeploymentHistoryPreflightAttributes from(
            SkillManagementEventAttributeReader attributes) {
        SkillManagementEventAttributeReader reader = SkillManagementEventAttributeReader.orEmpty(attributes);
        return new SkillManagementAdminDeploymentHistoryPreflightAttributes(
                reader.hasPrefix(PREFLIGHT),
                reader.flag(PREFLIGHT_READY),
                reader.flag(PREFLIGHT_DEPLOYABLE),
                reader.count(PREFLIGHT_ERRORS),
                reader.count(PREFLIGHT_CONFIGURATION_ERRORS),
                reader.count(PREFLIGHT_TARGET_STORE_ERRORS),
                reader.count(PREFLIGHT_SOURCE_STORE_ERRORS),
                reader.count(PREFLIGHT_CAPABILITY_ERRORS),
                reader.text(PREFLIGHT_MESSAGE));
    }
}

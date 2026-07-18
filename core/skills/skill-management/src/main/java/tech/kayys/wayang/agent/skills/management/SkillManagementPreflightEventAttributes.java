package tech.kayys.wayang.agent.skills.management;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

/**
 * Event-attribute projection for preflight reports and preflight failures.
 */
final class SkillManagementPreflightEventAttributes {

    private SkillManagementPreflightEventAttributes() {
    }

    static Map<String, String> deployment(SkillManagementDeploymentPreflightReport report) {
        Objects.requireNonNull(report, "report");
        return preflight(report.validation(), report.deployable());
    }

    static Map<String, String> validation(SkillManagementPreflightReport report) {
        SkillManagementPreflightReport validation = SkillManagementPreflightReport.orEmpty(report);
        return preflight(validation, validation.ready());
    }

    static Map<String, String> failure(RuntimeException error) {
        Objects.requireNonNull(error, "error");
        if (error instanceof SkillManagementDeploymentPreflightException deploymentError) {
            return deployment(deploymentError.report());
        }
        if (error instanceof SkillManagementPreflightException preflightError) {
            return validation(preflightError.preflightReport());
        }
        return Map.of();
    }

    private static Map<String, String> preflight(
            SkillManagementPreflightReport validation,
            boolean deployable) {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        attributes.put(PREFLIGHT_READY, String.valueOf(validation.ready()));
        attributes.put(PREFLIGHT_DEPLOYABLE, String.valueOf(deployable));
        attributes.put(PREFLIGHT_ERRORS, String.valueOf(validation.errors().size()));
        putIfPresent(attributes, PREFLIGHT_MESSAGE, validation.errorsMessage());
        validation(attributes, PREFLIGHT_CONFIGURATION, validation.configurationValidation());
        validation(attributes, PREFLIGHT_TARGET_STORE, validation.targetStoreValidation());
        validation(attributes, PREFLIGHT_SOURCE_STORE, validation.sourceStoreValidation());
        validation(attributes, PREFLIGHT_CAPABILITY, validation.capabilityValidation());
        return Map.copyOf(attributes);
    }

    private static void validation(
            LinkedHashMap<String, String> attributes,
            String prefix,
            SkillStoreConfigValidationResult validation) {
        SkillStoreConfigValidationResult resolved = validation == null
                ? SkillStoreConfigValidationResult.valid()
                : validation;
        attributes.put(prefix + "Valid", String.valueOf(resolved.validConfiguration()));
        attributes.put(prefix + "Errors", String.valueOf(resolved.errors().size()));
        putIfPresent(attributes, prefix + "Message", resolved.message());
    }

    private static void putIfPresent(
            LinkedHashMap<String, String> attributes,
            String key,
            String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }
}

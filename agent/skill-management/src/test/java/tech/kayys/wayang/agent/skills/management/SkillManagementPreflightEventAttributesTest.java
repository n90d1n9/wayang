package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementPreflightEventAttributesTest {

    @Test
    void validationProjectsPreflightBuckets() {
        SkillManagementPreflightReport report = new SkillManagementPreflightReport(
                SkillStoreConfigValidationResult.error("missing config"),
                SkillStoreConfigValidationResult.valid(),
                null,
                SkillStoreConfigValidationResult.error("missing capability"));

        Map<String, String> attributes = SkillManagementPreflightEventAttributes.validation(report);

        assertThat(attributes)
                .containsEntry("preflightReady", "false")
                .containsEntry("preflightDeployable", "false")
                .containsEntry("preflightErrors", "2")
                .containsEntry("preflightMessage", "missing config; missing capability")
                .containsEntry("preflightConfigurationValid", "false")
                .containsEntry("preflightConfigurationErrors", "1")
                .containsEntry("preflightConfigurationMessage", "missing config")
                .containsEntry("preflightTargetStoreValid", "true")
                .containsEntry("preflightSourceStoreValid", "true")
                .containsEntry("preflightCapabilityValid", "false")
                .containsEntry("preflightCapabilityErrors", "1")
                .containsEntry("preflightCapabilityMessage", "missing capability");
        assertThatThrownBy(() -> attributes.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failureProjectsPreflightExceptionReport() {
        SkillManagementPreflightReport report = new SkillManagementPreflightReport(
                null,
                SkillStoreConfigValidationResult.error("target unavailable"),
                null,
                null);
        RuntimeException error = new SkillManagementPreflightException(
                SkillManagementEventOperation.MAINTENANCE,
                report);

        Map<String, String> attributes = SkillManagementPreflightEventAttributes.failure(error);

        assertThat(attributes)
                .containsEntry("preflightReady", "false")
                .containsEntry("preflightDeployable", "false")
                .containsEntry("preflightErrors", "1")
                .containsEntry("preflightTargetStoreMessage", "target unavailable");
    }
}

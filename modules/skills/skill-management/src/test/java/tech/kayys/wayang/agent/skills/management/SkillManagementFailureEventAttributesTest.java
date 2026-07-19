package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementFailureEventAttributesTest {

    @Test
    void withContextAddsOperationCorrelationAttributes() {
        SkillManagementOperationContext context =
                new SkillManagementOperationContext("operation-1", "parent-1");

        Map<String, String> attributes =
                SkillManagementFailureEventAttributes.withContext(Map.of("revision", "2"), context);

        assertThat(attributes)
                .containsEntry("revision", "2")
                .containsEntry("operationId", "operation-1")
                .containsEntry("parentOperationId", "parent-1");
        assertThatThrownBy(() -> attributes.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failureAddsCustomAttributesContextAndErrorMetadata() {
        RuntimeException error = new IllegalStateException("store failed");

        Map<String, String> attributes = SkillManagementFailureEventAttributes.failure(
                error,
                Map.of("kind", "resource"),
                SkillManagementOperationContext.of("artifact-1"));

        assertThat(attributes)
                .containsEntry("kind", "resource")
                .containsEntry("operationId", "artifact-1")
                .containsEntry("errorType", "IllegalStateException")
                .containsEntry("error", "store failed");
        assertThatThrownBy(() -> attributes.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failureIncludesPreflightReportAttributes() {
        SkillManagementPreflightReport report = new SkillManagementPreflightReport(
                null,
                SkillStoreConfigValidationResult.error("target unavailable"),
                null,
                null);
        RuntimeException error = new SkillManagementPreflightException(
                SkillManagementEventOperation.MAINTENANCE,
                report);

        Map<String, String> attributes = SkillManagementFailureEventAttributes.failure(error, Map.of());

        assertThat(attributes)
                .containsEntry("preflightReady", "false")
                .containsEntry("preflightErrors", "1")
                .containsEntry("preflightTargetStoreMessage", "target unavailable")
                .containsEntry("errorType", "SkillManagementPreflightException")
                .containsEntry("error", "Skill-management maintenance preflight failed: target unavailable");
    }
}

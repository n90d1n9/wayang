package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementEventAttributesTest {

    @Test
    void artifactDeletedProjectsReferenceAndDeleteFlag() {
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");

        Map<String, String> attributes = SkillManagementEventAttributes.artifactDeleted(reference, true);

        assertThat(attributes)
                .containsEntry("kind", "resource")
                .containsEntry("name", "prompt")
                .containsEntry("version", "v1")
                .containsEntry("qualifiedName", "planner:resource:prompt:v1")
                .containsEntry("deleted", "true");
        assertThatThrownBy(() -> attributes.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void skillRevisionProjectsRevisionOnly() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        SkillLifecycleState state = new SkillLifecycleState(
                "planner",
                SkillLifecycleStatus.ACTIVE,
                now,
                now,
                7);

        assertThat(SkillManagementEventAttributes.skillRevision(state))
                .containsOnly(Map.entry("revision", "7"));
    }

    @Test
    void withContextAddsOperationCorrelationAttributes() {
        SkillManagementOperationContext context =
                new SkillManagementOperationContext("operation-1", "parent-1");

        Map<String, String> attributes =
                SkillManagementEventAttributes.withContext(Map.of("revision", "2"), context);

        assertThat(attributes)
                .containsEntry("revision", "2")
                .containsEntry("operationId", "operation-1")
                .containsEntry("parentOperationId", "parent-1");
        assertThatThrownBy(() -> attributes.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementArtifactEventAttributesTest {

    @Test
    void artifactProjectsReferencePayloadAndSize() {
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifact artifact = SkillArtifact.text(reference, "Plan carefully.");

        Map<String, String> attributes = SkillManagementArtifactEventAttributes.artifact(artifact);

        assertThat(attributes)
                .containsEntry("kind", "resource")
                .containsEntry("name", "prompt")
                .containsEntry("version", "v1")
                .containsEntry("qualifiedName", "planner:resource:prompt:v1")
                .containsEntry("contentType", "text/plain; charset=utf-8")
                .containsEntry("sizeBytes", "15");
        assertThatThrownBy(() -> attributes.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void syncProjectsChangeCountsAndConsistency() {
        SkillArtifactReference copied = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference conflict = SkillArtifactReference.resource("planner", "prompt", "v2");
        SkillArtifactStoreSyncResult result = new SkillArtifactStoreSyncResult(
                true,
                List.of(
                        new SkillArtifactStoreSyncChange(
                                copied,
                                SkillArtifactStoreSyncAction.COPIED,
                                "copied"),
                        new SkillArtifactStoreSyncChange(
                                conflict,
                                SkillArtifactStoreSyncAction.CONFLICT,
                                "conflict")));

        Map<String, String> attributes = SkillManagementArtifactEventAttributes.sync(result);

        assertThat(attributes)
                .containsEntry("dryRun", "true")
                .containsEntry("changed", "1")
                .containsEntry("consistent", "false")
                .containsEntry("copied", "1")
                .containsEntry("updated", "0")
                .containsEntry("unchanged", "0")
                .containsEntry("conflicts", "1")
                .containsEntry("deleted", "0");
    }
}

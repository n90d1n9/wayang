package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminSyncViewsTest {

    @Test
    void mapsDefinitionSyncResultToStableAdminProjection() {
        SkillDefinitionStoreSyncResult result = new SkillDefinitionStoreSyncResult(
                false,
                List.of(
                        new SkillDefinitionStoreSyncChange(
                                "planner",
                                SkillDefinitionStoreSyncAction.COPIED,
                                "created"),
                        new SkillDefinitionStoreSyncChange(
                                "writer",
                                SkillDefinitionStoreSyncAction.CONFLICT,
                                "target differs")));

        SkillManagementAdminDefinitionSyncStatus view =
                SkillManagementAdminSyncViews.definitionSync(result);

        assertThat(view.dryRun()).isFalse();
        assertThat(view.copied()).isEqualTo(1);
        assertThat(view.conflicts()).isEqualTo(1);
        assertThat(view.changed()).isEqualTo(1);
        assertThat(view.changes()).extracting(SkillManagementAdminSyncChange::skillId)
                .containsExactly("planner", "writer");
        assertThat(view.changes()).extracting(SkillManagementAdminSyncChange::action)
                .containsExactly("COPIED", "CONFLICT");
    }

    @Test
    void mapsArtifactSyncResultToStableAdminProjection() {
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactStoreSyncResult result = new SkillArtifactStoreSyncResult(
                true,
                List.of(new SkillArtifactStoreSyncChange(
                        prompt,
                        SkillArtifactStoreSyncAction.UPDATED,
                        "updated bytes")));

        SkillManagementAdminArtifactSyncStatus view =
                SkillManagementAdminSyncViews.artifactSync(result);

        assertThat(view.dryRun()).isTrue();
        assertThat(view.updated()).isEqualTo(1);
        assertThat(view.changed()).isEqualTo(1);
        assertThat(view.changes()).extracting(SkillManagementAdminArtifactSyncChange::artifactReference)
                .containsExactly("planner:resource:prompt:v1");
        assertThat(view.changes()).extracting(SkillManagementAdminArtifactSyncChange::detail)
                .containsExactly("updated bytes");
    }

    @Test
    void definitionSyncStatusDerivesSummaryFromNormalizedChanges() {
        SkillManagementAdminDefinitionSyncStatus view = new SkillManagementAdminDefinitionSyncStatus(
                false,
                99,
                99,
                99,
                99,
                99,
                99,
                java.util.Arrays.asList(
                        new SkillManagementAdminSyncChange("copied", "COPIED", false, ""),
                        new SkillManagementAdminSyncChange("updated", "UPDATED", false, ""),
                        new SkillManagementAdminSyncChange("unchanged", "UNCHANGED", true, ""),
                        new SkillManagementAdminSyncChange("conflict", "CONFLICT", true, ""),
                        new SkillManagementAdminSyncChange("deleted", "DELETED", false, ""),
                        null));

        assertThat(view.copied()).isEqualTo(1);
        assertThat(view.updated()).isEqualTo(1);
        assertThat(view.unchanged()).isEqualTo(1);
        assertThat(view.conflicts()).isEqualTo(1);
        assertThat(view.deleted()).isEqualTo(1);
        assertThat(view.changed()).isEqualTo(3);
        assertThat(view.changes()).hasSize(5);
        assertThat(view.changes()).extracting(SkillManagementAdminSyncChange::changed)
                .containsExactly(true, true, false, false, true);
    }

    @Test
    void artifactSyncStatusDerivesSummaryFromNormalizedChanges() {
        SkillManagementAdminArtifactSyncStatus view = new SkillManagementAdminArtifactSyncStatus(
                false,
                99,
                99,
                99,
                99,
                99,
                99,
                java.util.Arrays.asList(
                        new SkillManagementAdminArtifactSyncChange("planner:resource:prompt:v1", "COPIED", false, ""),
                        new SkillManagementAdminArtifactSyncChange("planner:resource:prompt:v2", "UPDATED", false, ""),
                        new SkillManagementAdminArtifactSyncChange("planner:resource:prompt:v3", "UNCHANGED", true, ""),
                        new SkillManagementAdminArtifactSyncChange("planner:resource:prompt:v4", "CONFLICT", true, ""),
                        new SkillManagementAdminArtifactSyncChange("planner:resource:prompt:v5", "DELETED", false, ""),
                        null));

        assertThat(view.copied()).isEqualTo(1);
        assertThat(view.updated()).isEqualTo(1);
        assertThat(view.unchanged()).isEqualTo(1);
        assertThat(view.conflicts()).isEqualTo(1);
        assertThat(view.deleted()).isEqualTo(1);
        assertThat(view.changed()).isEqualTo(3);
        assertThat(view.changes()).hasSize(5);
        assertThat(view.changes()).extracting(SkillManagementAdminArtifactSyncChange::changed)
                .containsExactly(true, true, false, false, true);
    }
}

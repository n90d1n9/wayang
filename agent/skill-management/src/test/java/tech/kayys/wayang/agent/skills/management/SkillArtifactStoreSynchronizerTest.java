package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillArtifactStoreSynchronizerTest {

    private final SkillArtifactStoreSynchronizer synchronizer = new SkillArtifactStoreSynchronizer();

    @Test
    void bootstrapsMissingArtifactsWithoutOverwritingConflicts() {
        InMemorySkillArtifactStore source = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore target = new InMemorySkillArtifactStore();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("writer", "v1");
        source.putArtifact(SkillArtifact.text(prompt, "source"));
        source.putArtifact(SkillArtifact.text(packageArtifact, "package"));
        target.putArtifact(SkillArtifact.text(prompt, "target"));

        SkillArtifactStoreSyncResult result = synchronizer.sync(
                source,
                target,
                SkillArtifactStoreSyncOptions.bootstrap());

        assertThat(result.copied()).isEqualTo(1);
        assertThat(result.conflicts()).isEqualTo(1);
        assertThat(target.getArtifact(packageArtifact)).isPresent();
        assertThat(text(target.getArtifact(prompt).orElseThrow())).isEqualTo("target");
    }

    @Test
    void mirrorOverwritesChangedArtifactsAndDeletesTargetOrphans() {
        InMemorySkillArtifactStore source = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore target = new InMemorySkillArtifactStore();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference old = SkillArtifactReference.resource("old", "prompt", "v1");
        source.putArtifact(SkillArtifact.text(prompt, "source"));
        target.putArtifact(SkillArtifact.text(prompt, "target"));
        target.putArtifact(SkillArtifact.text(old, "remove"));

        SkillArtifactStoreSyncResult result = synchronizer.sync(
                source,
                target,
                SkillArtifactStoreSyncOptions.mirror());

        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.deleted()).isEqualTo(1);
        assertThat(text(target.getArtifact(prompt).orElseThrow())).isEqualTo("source");
        assertThat(target.getArtifact(old)).isEmpty();
    }

    @Test
    void dryRunReportsChangesWithoutMutatingTarget() {
        InMemorySkillArtifactStore source = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore target = new InMemorySkillArtifactStore();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        source.putArtifact(SkillArtifact.text(prompt, "source"));

        SkillArtifactStoreSyncResult result = synchronizer.sync(
                source,
                target,
                SkillArtifactStoreSyncOptions.mirror().asDryRun());

        assertThat(result.dryRun()).isTrue();
        assertThat(result.copied()).isEqualTo(1);
        assertThat(target.getArtifact(prompt)).isEmpty();
    }

    @Test
    void treatsEquivalentArtifactsAsUnchanged() {
        InMemorySkillArtifactStore source = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore target = new InMemorySkillArtifactStore();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        source.putArtifact(new SkillArtifact(
                prompt,
                "hello".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                Map.of("tenant", "tenant-a")));
        target.putArtifact(new SkillArtifact(
                prompt,
                "hello".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                Map.of("tenant", "tenant-a")));

        SkillArtifactStoreSyncResult result = synchronizer.sync(
                source,
                target,
                SkillArtifactStoreSyncOptions.mirror());

        assertThat(result.unchanged()).isEqualTo(1);
        assertThat(result.changed()).isZero();
    }

    @Test
    void syncResultNormalizesNullChangesAndDetails() {
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("writer", "v1");
        SkillArtifactStoreSyncChange copied = new SkillArtifactStoreSyncChange(
                prompt,
                SkillArtifactStoreSyncAction.COPIED,
                null);
        SkillArtifactStoreSyncChange unchanged = new SkillArtifactStoreSyncChange(
                packageArtifact,
                SkillArtifactStoreSyncAction.UNCHANGED,
                "same");

        SkillArtifactStoreSyncResult result = new SkillArtifactStoreSyncResult(
                false,
                java.util.Arrays.asList(copied, null, unchanged));

        assertThat(result.changes()).containsExactly(copied, unchanged);
        assertThat(result.changes().get(0).detail()).isEmpty();
        assertThat(result.copied()).isEqualTo(1);
        assertThat(result.unchanged()).isEqualTo(1);
        assertThat(result.changed()).isEqualTo(1);
    }

    private static String text(SkillArtifact artifact) {
        return new String(artifact.content(), StandardCharsets.UTF_8);
    }
}

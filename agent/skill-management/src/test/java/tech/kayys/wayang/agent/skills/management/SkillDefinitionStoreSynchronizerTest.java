package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillDefinitionStoreSynchronizerTest {

    private final SkillDefinitionStoreSynchronizer synchronizer = new SkillDefinitionStoreSynchronizer();

    @Test
    void bootstrapsMissingDefinitionsWithoutOverwritingConflicts() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        source.registerSkill(skill("planner", "Plan carefully."));
        source.registerSkill(skill("writer", "Write carefully."));
        target.registerSkill(skill("planner", "Existing target prompt."));

        SkillDefinitionStoreSyncResult result = synchronizer.sync(
                source,
                target,
                SkillDefinitionStoreSyncOptions.bootstrap());

        assertThat(result.copied()).isEqualTo(1);
        assertThat(result.conflicts()).isEqualTo(1);
        assertThat(target.getSkill("writer")).isPresent();
        assertThat(target.getSkill("planner").orElseThrow().systemPrompt())
                .isEqualTo("Existing target prompt.");
    }

    @Test
    void mirrorOverwritesChangedDefinitionsAndDeletesTargetOrphans() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        source.registerSkill(skill("planner", "Plan carefully."));
        target.registerSkill(skill("planner", "Old prompt."));
        target.registerSkill(skill("old", "Remove me."));

        SkillDefinitionStoreSyncResult result = synchronizer.sync(
                source,
                target,
                SkillDefinitionStoreSyncOptions.mirror());

        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.deleted()).isEqualTo(1);
        assertThat(target.getSkill("planner").orElseThrow().systemPrompt()).isEqualTo("Plan carefully.");
        assertThat(target.getSkill("old")).isEmpty();
    }

    @Test
    void dryRunReportsChangesWithoutMutatingTarget() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        source.registerSkill(skill("planner", "Plan carefully."));

        SkillDefinitionStoreSyncResult result = synchronizer.sync(
                source,
                target,
                SkillDefinitionStoreSyncOptions.mirror().asDryRun());

        assertThat(result.dryRun()).isTrue();
        assertThat(result.copied()).isEqualTo(1);
        assertThat(target.getSkill("planner")).isEmpty();
    }

    @Test
    void treatsPropertyCodecEquivalentDefinitionsAsUnchanged() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        SkillDefinition sourceSkill = skill("planner", "Plan carefully.");
        SkillDefinitionPropertiesCodec codec = new SkillDefinitionPropertiesCodec();
        SkillDefinition reloadedTargetSkill = codec.fromBytes(codec.toBytes(sourceSkill), "test");
        source.registerSkill(sourceSkill);
        target.registerSkill(reloadedTargetSkill);

        SkillDefinitionStoreSyncResult result = synchronizer.sync(
                source,
                target,
                SkillDefinitionStoreSyncOptions.mirror());

        assertThat(result.unchanged()).isEqualTo(1);
        assertThat(result.changed()).isZero();
    }

    @Test
    void syncResultNormalizesNullChangesAndDetails() {
        SkillDefinitionStoreSyncChange copied = new SkillDefinitionStoreSyncChange(
                "planner",
                SkillDefinitionStoreSyncAction.COPIED,
                null);
        SkillDefinitionStoreSyncChange unchanged = new SkillDefinitionStoreSyncChange(
                "writer",
                SkillDefinitionStoreSyncAction.UNCHANGED,
                "same");

        SkillDefinitionStoreSyncResult result = new SkillDefinitionStoreSyncResult(
                false,
                java.util.Arrays.asList(copied, null, unchanged));

        assertThat(result.changes()).containsExactly(copied, unchanged);
        assertThat(result.changes().get(0).detail()).isEmpty();
        assertThat(result.copied()).isEqualTo(1);
        assertThat(result.unchanged()).isEqualTo(1);
        assertThat(result.changed()).isEqualTo(1);
    }

    private SkillDefinition skill(String id, String systemPrompt) {
        return TestSkillDefinitions.withSystemPromptAndMetadata(
                id,
                systemPrompt,
                Map.of("enabled", true));
    }

}

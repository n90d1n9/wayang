package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SkillLifecycleStateReconcilerTest {

    private final SkillLifecycleStateReconciler reconciler = new SkillLifecycleStateReconciler();

    @Test
    void inspectOnlyReportsMissingAndOrphanedStateWithoutMutating() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(skill("planner"));
        definitions.registerSkill(skill("writer"));
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        states.save(SkillLifecycleState.created("orphan"));

        SkillLifecycleStateReconcileResult result = reconciler.reconcile(
                definitions,
                states,
                SkillLifecycleStateReconcileOptions.inspectOnly());

        assertThat(result.consistent()).isFalse();
        assertThat(result.definitionSkillIds()).containsExactly("planner", "writer");
        assertThat(result.persistedStateSkillIds()).containsExactly("orphan");
        assertThat(result.missingStateSkillIds()).containsExactly("planner", "writer");
        assertThat(result.orphanedStateSkillIds()).containsExactly("orphan");
        assertThat(result.createdStateSkillIds()).isEmpty();
        assertThat(result.removedStateSkillIds()).isEmpty();
        assertThat(states.snapshot()).containsOnlyKeys("orphan");
    }

    @Test
    void canCreateMissingStatesAndRemoveOrphansExplicitly() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(skill("planner"));
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        states.save(SkillLifecycleState.created("orphan"));

        SkillLifecycleStateReconcileResult result = reconciler.reconcile(
                definitions,
                states,
                SkillLifecycleStateReconcileOptions.createMissingAndRemoveOrphans());

        assertThat(result.consistent()).isTrue();
        assertThat(result.createdStateSkillIds()).containsExactly("planner");
        assertThat(result.removedStateSkillIds()).containsExactly("orphan");
        assertThat(states.snapshot()).containsOnlyKeys("planner");
    }

    @Test
    void managementServiceExposesLifecycleReconciliation() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(skill("planner"));
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        SkillManagementService service = new SkillManagementService(definitions, states);

        SkillLifecycleStateReconcileResult result = service.reconcileLifecycleState(
                SkillLifecycleStateReconcileOptions.createMissing()).await().indefinitely();

        assertThat(result.createdStateSkillIds()).containsExactly("planner");
        assertThat(states.snapshot()).containsOnlyKeys("planner");
    }

    @Test
    void reconcilePlanNormalizesSparseIdsBeforeDiffing() {
        SkillLifecycleStateReconcilePlan plan = SkillLifecycleStateReconcilePlan.from(
                java.util.Arrays.asList("writer", null, " ", "planner", "planner"),
                java.util.Arrays.asList("planner", "old", "old", ""));

        assertThat(plan.definitionSkillIds()).containsExactly("planner", "writer");
        assertThat(plan.persistedStateSkillIds()).containsExactly("old", "planner");
        assertThat(plan.missingStateSkillIds()).containsExactly("writer");
        assertThat(plan.orphanedStateSkillIds()).containsExactly("old");
    }

    @Test
    void reconcileResultNormalizesSparseIds() {
        SkillLifecycleStateReconcileResult result = new SkillLifecycleStateReconcileResult(
                java.util.Arrays.asList("writer", null, "planner", "planner", ""),
                java.util.Arrays.asList("planner", "old", "old", " "),
                List.of("writer"),
                List.of("old"),
                List.of("writer", "writer"),
                java.util.Arrays.asList("old", null, ""));

        assertThat(result.definitionSkillIds()).containsExactly("planner", "writer");
        assertThat(result.persistedStateSkillIds()).containsExactly("old", "planner");
        assertThat(result.createdStateSkillIds()).containsExactly("writer");
        assertThat(result.removedStateSkillIds()).containsExactly("old");
        assertThat(result.consistent()).isTrue();
    }

    private SkillDefinition skill(String id) {
        return TestSkillDefinitions.basic(id);
    }

}

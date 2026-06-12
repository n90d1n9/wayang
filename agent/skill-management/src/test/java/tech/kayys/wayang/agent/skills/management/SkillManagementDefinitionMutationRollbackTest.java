package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementDefinitionMutationRollbackTest {

    @Test
    void createdRollbackRemovesCreatedDefinitionAndReturnsOriginalError() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        SkillDefinition skill = TestSkillDefinitions.named("planner", "Planner");
        definitions.registerSkill(skill);
        RuntimeException error = new IllegalStateException("lifecycle save failed");
        SkillManagementDefinitionMutationRollback rollback = newRollback(
                definitions,
                new InMemorySkillLifecycleStateStore());

        RuntimeException result = rollback.created("planner", error);

        assertThat(result).isSameAs(error);
        assertThat(definitions.getSkill("planner")).isEmpty();
        assertThat(error.getSuppressed()).isEmpty();
    }

    @Test
    void updatedRollbackRestoresPreviousDefinitionAndLifecycleState() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        SkillDefinition previousDefinition = TestSkillDefinitions.named("planner", "Planner");
        SkillDefinition updatedDefinition = TestSkillDefinitions.named("planner", "Updated Planner");
        definitions.registerSkill(updatedDefinition);
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        SkillLifecycleState previousState = state("planner", SkillLifecycleStatus.DISABLED, 3);
        states.save(state("planner", SkillLifecycleStatus.ACTIVE, 4));
        RuntimeException error = new IllegalStateException("lifecycle save failed");
        SkillManagementDefinitionMutationRollback rollback = newRollback(definitions, states);

        RuntimeException result = rollback.updated(
                "planner",
                previousDefinition,
                Optional.of(previousState),
                error);

        assertThat(result).isSameAs(error);
        assertThat(definitions.getSkill("planner")).contains(previousDefinition);
        assertThat(states.get("planner")).contains(previousState);
        assertThat(error.getSuppressed()).isEmpty();
    }

    @Test
    void deletedRollbackRestoresDefinitionAndRemovesCreatedLifecycleState() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        SkillDefinition previousDefinition = TestSkillDefinitions.named("planner", "Planner");
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        states.save(SkillLifecycleState.created("planner"));
        RuntimeException error = new IllegalStateException("lifecycle remove failed");
        SkillManagementDefinitionMutationRollback rollback = newRollback(definitions, states);

        RuntimeException result = rollback.deleted(
                "planner",
                previousDefinition,
                Optional.empty(),
                error);

        assertThat(result).isSameAs(error);
        assertThat(definitions.getSkill("planner")).contains(previousDefinition);
        assertThat(states.get("planner")).isEmpty();
        assertThat(error.getSuppressed()).isEmpty();
    }

    @Test
    void rollbackFailuresAreSuppressedOnOriginalError() {
        RuntimeException error = new IllegalStateException("lifecycle save failed");
        SkillManagementDefinitionMutationRollback rollback = newRollback(
                new FailingUnregisterSkillDefinitionStore(),
                new InMemorySkillLifecycleStateStore());

        RuntimeException result = rollback.created("planner", error);

        assertThat(result).isSameAs(error);
        assertThat(error.getSuppressed()).hasSize(1);
        assertThat(error.getSuppressed()[0]).hasMessage("definition rollback failed");
    }

    private SkillManagementDefinitionMutationRollback newRollback(
            SkillDefinitionStore definitions,
            SkillLifecycleStateStore states) {
        return new SkillManagementDefinitionMutationRollback(definitions, states);
    }

    private SkillLifecycleState state(String skillId, SkillLifecycleStatus status, int revision) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new SkillLifecycleState(skillId, status, now, now, revision);
    }

    private static final class FailingUnregisterSkillDefinitionStore extends TestSkillDefinitionStore {

        @Override
        public boolean unregisterSkill(String skillId) {
            throw new IllegalStateException("definition rollback failed");
        }
    }
}

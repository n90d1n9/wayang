package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementLifecycleRunnerTest {

    @Test
    void stateForExistingReturnsImplicitDefaultWithoutPersisting() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.basic("planner"));
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        SkillManagementLifecycleRunner runner = runner(definitions, states, new InMemorySkillManagementEventSink());

        SkillLifecycleState state = runner.stateForExisting("planner");

        assertThat(state.status()).isEqualTo(SkillLifecycleStatus.ACTIVE);
        assertThat(state.revision()).isEqualTo(1);
        assertThat(states.snapshot()).isEmpty();
    }

    @Test
    void transitionRecordsSuccessEventAndPersistsState() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.basic("planner"));
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementLifecycleRunner runner = runner(definitions, states, eventSink);

        SkillLifecycleState state = runner.transition("planner", SkillLifecycleStatus.DISABLED);

        assertThat(state.status()).isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(states.get("planner")).contains(state);
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.TRANSITION_SKILL);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("status", SkillLifecycleStatus.DISABLED.name())
                .containsEntry("revision", "1")
                .containsKey("operationId");
    }

    @Test
    void snapshotReturnsPersistedLifecycleStates() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.basic("planner"));
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        SkillManagementLifecycleRunner runner = runner(definitions, states, new InMemorySkillManagementEventSink());
        SkillLifecycleState state = SkillLifecycleState.created("planner").withStatus(SkillLifecycleStatus.DISABLED);
        states.save(state);

        assertThat(runner.snapshot()).containsEntry("planner", state);
    }

    @Test
    void transitionRecordsFailureEventBeforeRethrowing() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.basic("planner"));
        FailingSaveLifecycleStateStore states = new FailingSaveLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementLifecycleRunner runner = runner(definitions, states, eventSink);

        assertThatThrownBy(() -> runner.transition("planner", SkillLifecycleStatus.DISABLED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lifecycle save failed");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.TRANSITION_SKILL);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("status", SkillLifecycleStatus.DISABLED.name())
                .containsEntry("error", "lifecycle save failed")
                .containsEntry("errorType", IllegalStateException.class.getSimpleName())
                .containsKey("operationId");
    }

    @Test
    void reconcileRecordsSuccessEventAndResultAttributes() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.basic("planner"));
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementLifecycleRunner runner = runner(definitions, states, eventSink);

        SkillLifecycleStateReconcileResult result =
                runner.reconcile(SkillLifecycleStateReconcileOptions.createMissing());

        assertThat(result.createdStateSkillIds()).containsExactly("planner");
        assertThat(states.snapshot()).containsOnlyKeys("planner");
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.RECONCILE_LIFECYCLE);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("created", "1")
                .containsEntry("consistent", "true")
                .containsKey("operationId");
    }

    @Test
    void reconcileRecordsFailureEventBeforeRethrowing() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementLifecycleRunner runner = runner(
                TestSkillDefinitionStore.failingList("definition list failed"),
                new InMemorySkillLifecycleStateStore(),
                eventSink);

        assertThatThrownBy(() -> runner.reconcile(SkillLifecycleStateReconcileOptions.inspectOnly()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("definition list failed");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.RECONCILE_LIFECYCLE);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("error", "definition list failed")
                .containsEntry("errorType", IllegalStateException.class.getSimpleName())
                .containsKey("operationId");
    }

    private SkillManagementLifecycleRunner runner(
            SkillDefinitionStore definitions,
            SkillLifecycleStateStore states,
            SkillManagementEventSink eventSink) {
        return new SkillManagementLifecycleRunner(
                definitions,
                states,
                new SkillLifecycleStateReconciler(),
                new SkillManagementEventRecorder(eventSink));
    }

    private static final class FailingSaveLifecycleStateStore implements SkillLifecycleStateStore {

        @Override
        public Optional<SkillLifecycleState> get(String skillId) {
            return Optional.empty();
        }

        @Override
        public SkillLifecycleState save(SkillLifecycleState state) {
            throw new IllegalStateException("lifecycle save failed");
        }

        @Override
        public boolean remove(String skillId) {
            return false;
        }

        @Override
        public Map<String, SkillLifecycleState> snapshot() {
            return Map.of();
        }
    }
}

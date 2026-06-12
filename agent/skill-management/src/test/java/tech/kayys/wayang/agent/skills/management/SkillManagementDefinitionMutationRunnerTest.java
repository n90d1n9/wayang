package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementDefinitionMutationRunnerTest {

    @Test
    void createRegistersDefinitionStateAndEvent() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        ToggleLifecycleStateStore states = new ToggleLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementDefinitionMutationRunner runner = runner(definitions, states, eventSink);
        SkillDefinition skill = TestSkillDefinitions.named("planner", "Planner");

        SkillDefinition result = runner.create(skill);

        assertThat(result).isEqualTo(skill);
        assertThat(definitions.getSkill("planner")).contains(skill);
        assertThat(states.get("planner")).isPresent();
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.CREATE_SKILL);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes()).containsKey("operationId");
    }

    @Test
    void updatePersistsRevisionAndEvent() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        SkillDefinition original = TestSkillDefinitions.named("planner", "Planner");
        SkillDefinition updated = TestSkillDefinitions.named("planner", "Updated Planner");
        definitions.registerSkill(original);
        ToggleLifecycleStateStore states = new ToggleLifecycleStateStore();
        states.put(state("planner", SkillLifecycleStatus.DISABLED, 3));
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementDefinitionMutationRunner runner = runner(definitions, states, eventSink);

        SkillDefinition result = runner.update("planner", updated);

        assertThat(result).isEqualTo(updated);
        assertThat(definitions.getSkill("planner")).contains(updated);
        assertThat(states.get("planner").orElseThrow().revision()).isEqualTo(4);
        assertThat(states.get("planner").orElseThrow().status()).isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.UPDATE_SKILL);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("revision", "4")
                .containsKey("operationId");
    }

    @Test
    void deleteRemovesDefinitionStateAndEvent() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.named("planner", "Planner"));
        ToggleLifecycleStateStore states = new ToggleLifecycleStateStore();
        states.put(SkillLifecycleState.created("planner"));
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementDefinitionMutationRunner runner = runner(definitions, states, eventSink);

        boolean deleted = runner.delete("planner");

        assertThat(deleted).isTrue();
        assertThat(definitions.getSkill("planner")).isEmpty();
        assertThat(states.get("planner")).isEmpty();
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.DELETE_SKILL);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes()).containsKey("operationId");
    }

    @Test
    void createRollsBackDefinitionWhenLifecycleInitializationFails() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        ToggleLifecycleStateStore states = new ToggleLifecycleStateStore();
        states.failSaves = true;
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementDefinitionMutationRunner runner = runner(definitions, states, eventSink);

        assertThatThrownBy(() -> runner.create(TestSkillDefinitions.named("planner", "Planner")))
                .isInstanceOf(SkillManagementWriteException.class)
                .hasMessageContaining("Failed to create skill consistently: planner");

        assertThat(definitions.getSkill("planner")).isEmpty();
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.CREATE_SKILL);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("error", "lifecycle save failed")
                .containsEntry("errorType", IllegalStateException.class.getSimpleName())
                .containsKey("operationId");
    }

    @Test
    void updateRestoresPreviousDefinitionWhenLifecycleRevisionFails() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        SkillDefinition original = TestSkillDefinitions.named("planner", "Planner");
        SkillDefinition updated = TestSkillDefinitions.named("planner", "Updated Planner");
        definitions.registerSkill(original);
        ToggleLifecycleStateStore states = new ToggleLifecycleStateStore();
        states.put(state("planner", SkillLifecycleStatus.DISABLED, 3));
        states.failSaves = true;
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementDefinitionMutationRunner runner = runner(definitions, states, eventSink);

        assertThatThrownBy(() -> runner.update("planner", updated))
                .isInstanceOf(SkillManagementWriteException.class)
                .hasMessageContaining("Failed to update skill consistently: planner");

        assertThat(definitions.getSkill("planner")).contains(original);
        assertThat(states.get("planner").orElseThrow().revision()).isEqualTo(3);
        assertThat(states.get("planner").orElseThrow().status()).isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(eventSink.events()).hasSize(1);
        assertThat(eventSink.events().get(0).operation()).isEqualTo(SkillManagementEventOperation.UPDATE_SKILL);
        assertThat(eventSink.events().get(0).success()).isFalse();
    }

    @Test
    void deleteRestoresPreviousDefinitionWhenLifecycleCleanupFails() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        SkillDefinition original = TestSkillDefinitions.named("planner", "Planner");
        definitions.registerSkill(original);
        ToggleLifecycleStateStore states = new ToggleLifecycleStateStore();
        states.put(SkillLifecycleState.created("planner"));
        states.failRemoves = true;
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementDefinitionMutationRunner runner = runner(definitions, states, eventSink);

        assertThatThrownBy(() -> runner.delete("planner"))
                .isInstanceOf(SkillManagementWriteException.class)
                .hasMessageContaining("Failed to delete skill consistently: planner");

        assertThat(definitions.getSkill("planner")).contains(original);
        assertThat(states.get("planner")).isPresent();
        assertThat(eventSink.events()).hasSize(1);
        assertThat(eventSink.events().get(0).operation()).isEqualTo(SkillManagementEventOperation.DELETE_SKILL);
        assertThat(eventSink.events().get(0).success()).isFalse();
    }

    private SkillManagementDefinitionMutationRunner runner(
            SkillDefinitionStore definitions,
            SkillLifecycleStateStore states,
            SkillManagementEventSink eventSink) {
        return new SkillManagementDefinitionMutationRunner(
                definitions,
                states,
                new SkillManagementEventRecorder(eventSink));
    }

    private SkillLifecycleState state(String skillId, SkillLifecycleStatus status, int revision) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new SkillLifecycleState(skillId, status, now, now, revision);
    }

    private static final class ToggleLifecycleStateStore implements SkillLifecycleStateStore {
        private final Map<String, SkillLifecycleState> states = new LinkedHashMap<>();
        private boolean failSaves;
        private boolean failRemoves;

        @Override
        public Optional<SkillLifecycleState> get(String skillId) {
            return Optional.ofNullable(states.get(skillId));
        }

        @Override
        public SkillLifecycleState save(SkillLifecycleState state) {
            if (failSaves) {
                throw new IllegalStateException("lifecycle save failed");
            }
            states.put(state.skillId(), state);
            return state;
        }

        @Override
        public boolean remove(String skillId) {
            if (failRemoves) {
                throw new IllegalStateException("lifecycle remove failed");
            }
            return states.remove(skillId) != null;
        }

        @Override
        public Map<String, SkillLifecycleState> snapshot() {
            return Map.copyOf(states);
        }

        void put(SkillLifecycleState state) {
            states.put(state.skillId(), state);
        }
    }
}

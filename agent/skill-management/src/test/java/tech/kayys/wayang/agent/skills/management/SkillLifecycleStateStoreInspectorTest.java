package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SkillLifecycleStateStoreInspectorTest {

    private final SkillLifecycleStateStoreInspector inspector = new SkillLifecycleStateStoreInspector();

    @Test
    void reportsReadyStoreWithSortedSkillIdsAndStatusCounts() {
        InMemorySkillLifecycleStateStore store = new InMemorySkillLifecycleStateStore();
        store.save(state("writer", SkillLifecycleStatus.DISABLED));
        store.save(state("planner", SkillLifecycleStatus.ACTIVE));
        store.save(state("deprecated", SkillLifecycleStatus.DEPRECATED));

        SkillLifecycleStateStoreInspection inspection = inspector.inspect("runtime", store);

        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.status()).isEqualTo(SkillLifecycleStateStoreHealthStatus.READY);
        assertThat(inspection.name()).isEqualTo("runtime");
        assertThat(inspection.stateCount()).isEqualTo(3);
        assertThat(inspection.skillIds()).containsExactly("deprecated", "planner", "writer");
        assertThat(inspection.statusCounts())
                .containsEntry(SkillLifecycleStatus.ACTIVE, 1)
                .containsEntry(SkillLifecycleStatus.DISABLED, 1)
                .containsEntry(SkillLifecycleStatus.DEPRECATED, 1);
        assertThat(inspection.capabilities().names()).containsExactly("read", "write", "delete", "list");
        assertThat(inspection.failure()).isBlank();
    }

    @Test
    void reportsUnavailableStoreWithoutThrowing() {
        SkillLifecycleStateStoreInspection inspection =
                inspector.inspect("broken", new FailingSkillLifecycleStateStore());

        assertThat(inspection.status()).isEqualTo(SkillLifecycleStateStoreHealthStatus.UNAVAILABLE);
        assertThat(inspection.ready()).isFalse();
        assertThat(inspection.stateCount()).isZero();
        assertThat(inspection.failure()).contains("IllegalStateException").contains("lifecycle unavailable");
    }

    @Test
    void reportsHybridStoreChildren() {
        InMemorySkillLifecycleStateStore primary = new InMemorySkillLifecycleStateStore();
        InMemorySkillLifecycleStateStore fallback = new InMemorySkillLifecycleStateStore();
        primary.save(state("planner", SkillLifecycleStatus.ACTIVE));
        fallback.save(state("writer", SkillLifecycleStatus.DISABLED));

        SkillLifecycleStateStoreInspection inspection = inspector.inspect(
                "hybrid",
                new HybridSkillLifecycleStateStore(primary, fallback));

        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.skillIds()).containsExactly("planner", "writer");
        assertThat(inspection.children()).hasSize(2);
        assertThat(inspection.children().get(0).name()).isEqualTo("primary");
        assertThat(inspection.children().get(0).skillIds()).containsExactly("planner");
        assertThat(inspection.children().get(1).name()).isEqualTo("fallback");
        assertThat(inspection.children().get(1).skillIds()).containsExactly("writer");
        assertThat(inspection.capabilities().names())
                .containsExactly("read", "write", "delete", "list", "primary-fallback");
    }

    @Test
    void managementServiceExposesLifecycleStateStoreInspection() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.basic("planner"));
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        SkillManagementService service = new SkillManagementService(definitions, lifecycle);

        service.disableSkill("planner").await().indefinitely();
        SkillLifecycleStateStoreInspection inspection =
                service.inspectLifecycleStateStore().await().indefinitely();

        assertThat(inspection.name()).isEqualTo("lifecycle");
        assertThat(inspection.skillIds()).containsExactly("planner");
        assertThat(inspection.statusCounts()).containsEntry(SkillLifecycleStatus.DISABLED, 1);
    }

    private SkillLifecycleState state(String skillId, SkillLifecycleStatus status) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new SkillLifecycleState(skillId, status, now, now, 1);
    }

    private static final class FailingSkillLifecycleStateStore implements SkillLifecycleStateStore {
        @Override
        public Optional<SkillLifecycleState> get(String skillId) {
            return Optional.empty();
        }

        @Override
        public SkillLifecycleState save(SkillLifecycleState state) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(String skillId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, SkillLifecycleState> snapshot() {
            throw new IllegalStateException("lifecycle unavailable");
        }
    }
}

package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementInspectorTest {

    private final SkillManagementInspector inspector = new SkillManagementInspector();

    @Test
    void combinesStoreHealthAndLifecycleReconciliation() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(skill("planner"));
        definitions.registerSkill(skill("writer"));
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        lifecycle.save(SkillLifecycleState.created("planner"));
        lifecycle.save(SkillLifecycleState.created("orphan"));
        InMemorySkillArtifactStore artifacts = new InMemorySkillArtifactStore();
        artifacts.putArtifact(SkillArtifact.of(SkillArtifactReference.resource("planner", "prompt", "v1"),
                new byte[] {1}));

        SkillManagementInspection inspection =
                inspector.inspect(definitions, lifecycle, SkillManagementEventReader.empty(), artifacts);

        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.definitionStore().skillIds()).containsExactly("planner", "writer");
        assertThat(inspection.lifecycleStateStore().skillIds()).containsExactly("orphan", "planner");
        assertThat(inspection.eventStore().ready()).isTrue();
        assertThat(inspection.eventStore().matchedEvents()).isZero();
        assertThat(inspection.artifactStore().ready()).isTrue();
        assertThat(inspection.artifactStore().artifactReferences())
                .containsExactly("planner:resource:prompt:v1");
        assertThat(inspection.lifecycleStateConsistent()).isFalse();
        assertThat(inspection.lifecycleStateReconciliation().missingStateSkillIds()).containsExactly("writer");
        assertThat(inspection.lifecycleStateReconciliation().orphanedStateSkillIds()).containsExactly("orphan");
        assertThat(inspection.lifecycleStateReconciliationFailure()).isBlank();
    }

    @Test
    void reportsUnavailableStoreWithoutThrowing() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(skill("planner"));

        SkillManagementInspection inspection = inspector.inspect(definitions, new FailingSkillLifecycleStateStore());

        assertThat(inspection.ready()).isFalse();
        assertThat(inspection.lifecycleStateStore().ready()).isFalse();
        assertThat(inspection.lifecycleStateReconciliationFailure()).contains("Skipped lifecycle reconciliation");
    }

    @Test
    void managementServiceExposesAggregateInspection() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(skill("planner"));
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementService service = new SkillManagementService(
                definitions,
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                events);
        service.createSkill(skill("writer")).await().indefinitely();

        SkillManagementInspection inspection = service.inspectManagement().await().indefinitely();

        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.lifecycleStateConsistent()).isFalse();
        assertThat(inspection.lifecycleStateReconciliation().missingStateSkillIds()).containsExactly("planner");
        assertThat(inspection.eventStore().matchedEvents()).isEqualTo(1);
        assertThat(inspection.eventStore().summary().operationCounts()).containsEntry("CREATE_SKILL", 1);
        assertThat(inspection.artifactStore().ready()).isTrue();
    }

    private SkillDefinition skill(String id) {
        return TestSkillDefinitions.basic(id);
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

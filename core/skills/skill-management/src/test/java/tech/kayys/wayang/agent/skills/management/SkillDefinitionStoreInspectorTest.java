package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SkillDefinitionStoreInspectorTest {

    private final SkillDefinitionStoreInspector inspector = new SkillDefinitionStoreInspector();

    @Test
    void reportsReadyStoreWithSortedSkillIds() {
        TestSkillDefinitionStore store = new TestSkillDefinitionStore();
        store.registerSkill(TestSkillDefinitions.basic("writer"));
        store.registerSkill(TestSkillDefinitions.basic("planner"));

        SkillDefinitionStoreInspection inspection = inspector.inspect("primary", store);

        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.status()).isEqualTo(SkillDefinitionStoreHealthStatus.READY);
        assertThat(inspection.name()).isEqualTo("primary");
        assertThat(inspection.skillCount()).isEqualTo(2);
        assertThat(inspection.skillIds()).containsExactly("planner", "writer");
        assertThat(inspection.capabilities().names()).containsExactly("read", "write", "delete", "list");
        assertThat(inspection.failure()).isBlank();
    }

    @Test
    void reportsUnavailableStoreWithoutThrowing() {
        SkillDefinitionStoreInspection inspection = inspector.inspect("broken", new FailingSkillDefinitionStore());

        assertThat(inspection.status()).isEqualTo(SkillDefinitionStoreHealthStatus.UNAVAILABLE);
        assertThat(inspection.ready()).isFalse();
        assertThat(inspection.skillCount()).isZero();
        assertThat(inspection.failure()).contains("IllegalStateException").contains("store unavailable");
    }

    @Test
    void includesHybridChildStoreInspections() {
        TestSkillDefinitionStore primary = new TestSkillDefinitionStore();
        TestSkillDefinitionStore fallback = new TestSkillDefinitionStore();
        fallback.registerSkill(TestSkillDefinitions.basic("fallback-skill"));
        HybridSkillDefinitionStore hybrid = new HybridSkillDefinitionStore(primary, fallback);

        SkillDefinitionStoreInspection inspection = inspector.inspect("hybrid", hybrid);

        assertThat(inspection.children()).extracting(SkillDefinitionStoreInspection::name)
                .containsExactly("primary", "fallback");
        assertThat(inspection.children().get(0).skillCount()).isZero();
        assertThat(inspection.children().get(1).skillIds()).containsExactly("fallback-skill");
        assertThat(inspection.skillIds()).containsExactly("fallback-skill");
        assertThat(inspection.capabilities().names())
                .containsExactly("read", "write", "delete", "list", "primary-fallback");
    }

    @Test
    void managementServiceExposesStoreInspection() {
        TestSkillDefinitionStore store = new TestSkillDefinitionStore();
        store.registerSkill(TestSkillDefinitions.basic("planner"));
        SkillManagementService service = new SkillManagementService(store);

        SkillDefinitionStoreInspection inspection = service.inspectStore().await().indefinitely();

        assertThat(inspection.name()).isEqualTo("skills");
        assertThat(inspection.skillIds()).containsExactly("planner");
    }

    private static final class FailingSkillDefinitionStore implements SkillDefinitionStore {
        @Override
        public Optional<SkillDefinition> getSkill(String skillId) {
            return Optional.empty();
        }

        @Override
        public List<SkillDefinition> listSkills() {
            throw new IllegalStateException("store unavailable");
        }

        @Override
        public void registerSkill(SkillDefinition skill) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unregisterSkill(String skillId) {
            throw new UnsupportedOperationException();
        }
    }
}

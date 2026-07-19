package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementInspectionReaderTest {

    @Test
    void readsIndividualAndCompositeInspectionsFromManagedStores() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        InMemorySkillArtifactStore artifacts = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        definitions.registerSkill(TestSkillDefinitions.categorized("planner", "REASONING"));
        states.save(SkillLifecycleState.created("planner"));
        artifacts.putArtifact(SkillArtifact.text(prompt, "Plan carefully."));
        events.record(new SkillManagementEvent(
                Instant.now(),
                SkillManagementEventOperation.CREATE_SKILL,
                "planner",
                true,
                Map.of()));
        SkillManagementInspectionReader reader = new SkillManagementInspectionReader(
                definitions,
                new SkillDefinitionStoreInspector(),
                states,
                new SkillLifecycleStateStoreInspector(),
                artifacts,
                new SkillArtifactStoreInspector(),
                events,
                new SkillManagementInspector());

        SkillDefinitionStoreInspection definitionInspection = reader.definitionStore();
        SkillLifecycleStateStoreInspection lifecycleInspection = reader.lifecycleStateStore();
        SkillArtifactStoreInspection artifactInspection = reader.artifactStore();
        SkillManagementInspection managementInspection = reader.management();

        assertThat(definitionInspection.name()).isEqualTo("skills");
        assertThat(definitionInspection.skillIds()).containsExactly("planner");
        assertThat(lifecycleInspection.name()).isEqualTo("lifecycle");
        assertThat(lifecycleInspection.statusCounts())
                .containsEntry(SkillLifecycleStatus.ACTIVE, 1);
        assertThat(artifactInspection.name()).isEqualTo("artifacts");
        assertThat(artifactInspection.artifactReferences())
                .containsExactly(prompt.qualifiedName());
        assertThat(managementInspection.ready()).isTrue();
        assertThat(managementInspection.definitionStore().skillCount()).isEqualTo(1);
        assertThat(managementInspection.eventStore().returnedEvents()).isEqualTo(1);
        assertThat(managementInspection.artifactStore().artifactCount()).isEqualTo(1);
        assertThat(managementInspection.lifecycleStateConsistent()).isTrue();
    }

}

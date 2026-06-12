package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementServiceFactoryDependenciesTest {

    @Test
    void registryCustomStoresAssemblesServiceComponents() {
        TestSkillRegistry registry = new TestSkillRegistry();
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycleStates = new InMemorySkillLifecycleStateStore();
        InMemorySkillArtifactStore artifacts = new InMemorySkillArtifactStore();
        SkillManagementServiceFactoryDependencies dependencies =
                SkillManagementServiceFactoryDependencies.registryCustomStores(
                        registry,
                        null,
                        null,
                        Map.of("definitions", definitions),
                        Map.of("lifecycle", lifecycleStates),
                        Map.of("artifacts", artifacts));
        SkillManagementService service = dependencies.components().service(SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.custom("definitions"),
                SkillLifecycleStateStoreConfig.custom("lifecycle"),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.custom("artifacts"),
                SkillLifecycleStateReconcileOptions.inspectOnly()));
        SkillDefinition skill = TestSkillDefinitions.named("planner", "Planner");
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");

        service.createSkill(skill).await().indefinitely();
        service.putArtifact(SkillArtifact.text(reference, "Plan carefully.")).await().indefinitely();

        assertThat(definitions.getSkill("planner")).contains(skill);
        assertThat(lifecycleStates.get("planner")).isPresent();
        assertThat(artifacts.getArtifact(reference)).isPresent();
    }
}

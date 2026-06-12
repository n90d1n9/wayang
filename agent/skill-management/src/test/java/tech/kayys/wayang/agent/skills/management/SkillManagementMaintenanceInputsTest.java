package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementMaintenanceInputsTest {

    @Test
    void definitionsOnlyRequiresCoreStoresAndSkipsArtifacts() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();

        SkillManagementMaintenanceInputs inputs =
                SkillManagementMaintenanceInputs.definitionsOnly(source, target, lifecycle);

        assertThat(inputs.sourceDefinitions()).isSameAs(source);
        assertThat(inputs.targetDefinitions()).isSameAs(target);
        assertThat(inputs.lifecycleStateStore()).isSameAs(lifecycle);
        assertThat(inputs.sourceArtifacts()).isNull();
        assertThat(inputs.targetArtifacts()).isNull();
        assertThat(inputs.artifactStoresAvailable()).isFalse();
    }

    @Test
    void withArtifactsReportsAvailabilityOnlyWhenBothStoresArePresent() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore targetArtifacts = new InMemorySkillArtifactStore();

        SkillManagementMaintenanceInputs inputs = SkillManagementMaintenanceInputs.withArtifacts(
                source,
                target,
                lifecycle,
                sourceArtifacts,
                targetArtifacts);
        SkillManagementMaintenanceInputs missingTargetArtifacts = SkillManagementMaintenanceInputs.withArtifacts(
                source,
                target,
                lifecycle,
                sourceArtifacts,
                null);

        assertThat(inputs.sourceArtifacts()).isSameAs(sourceArtifacts);
        assertThat(inputs.targetArtifacts()).isSameAs(targetArtifacts);
        assertThat(inputs.artifactStoresAvailable()).isTrue();
        assertThat(missingTargetArtifacts.artifactStoresAvailable()).isFalse();
    }

    @Test
    void rejectsMissingCoreStores() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();

        assertThatThrownBy(() -> SkillManagementMaintenanceInputs.definitionsOnly(null, target, lifecycle))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("sourceDefinitions");
        assertThatThrownBy(() -> SkillManagementMaintenanceInputs.definitionsOnly(source, null, lifecycle))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("targetDefinitions");
        assertThatThrownBy(() -> SkillManagementMaintenanceInputs.definitionsOnly(source, target, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("lifecycleStateStore");
    }
}

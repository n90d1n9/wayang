package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementBootstrapperTest {

    @Test
    void appliesConfiguredLifecycleReconciliationDuringBootstrap() {
        TestSkillRegistry registry = new TestSkillRegistry();
        registry.registerSkill(TestSkillDefinitions.basic("planner"));
        SkillManagementBootstrapper bootstrapper = new SkillManagementBootstrapper(
                new SkillManagementServiceFactory(registry));
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillLifecycleStateReconcileOptions.createMissing());

        SkillManagementBootstrapResult result = bootstrapper.bootstrap(config);

        assertThat(result.initialInspection().lifecycleStateConsistent()).isFalse();
        assertThat(result.initialInspection().lifecycleStateReconciliation().missingStateSkillIds())
                .containsExactly("planner");
        assertThat(result.lifecycleStateReconcileResult().createdStateSkillIds()).containsExactly("planner");
        assertThat(result.lifecycleStateChanged()).isTrue();
        assertThat(result.finalInspection().lifecycleStateConsistent()).isTrue();
        assertThat(result.ready()).isTrue();
        assertThat(result.service().lifecycleSnapshot().await().indefinitely()).containsOnlyKeys("planner");
    }

    @Test
    void inspectOnlyBootstrapDoesNotMutateLifecycleState() {
        TestSkillRegistry registry = new TestSkillRegistry();
        registry.registerSkill(TestSkillDefinitions.basic("planner"));
        SkillManagementBootstrapper bootstrapper = new SkillManagementBootstrapper(
                new SkillManagementServiceFactory(registry));

        SkillManagementBootstrapResult result = bootstrapper.bootstrap(SkillManagementServiceConfig.defaults());

        assertThat(result.lifecycleStateChanged()).isFalse();
        assertThat(result.finalInspection().lifecycleStateConsistent()).isFalse();
        assertThat(result.service().lifecycleSnapshot().await().indefinitely()).isEmpty();
    }

    @Test
    void emitsBootstrapEventWithReadinessSummary() {
        TestSkillRegistry registry = new TestSkillRegistry();
        registry.registerSkill(TestSkillDefinitions.basic("planner"));
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementBootstrapper bootstrapper = new SkillManagementBootstrapper(
                new SkillManagementServiceFactory(registry),
                eventSink);

        bootstrapper.bootstrap(SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillLifecycleStateReconcileOptions.createMissing()));

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.BOOTSTRAP);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("ready", "true")
                .containsEntry("changed", "true")
                .containsEntry("lifecycleConsistent", "true");
        assertThat(event.attributes().get("operationId")).isNotBlank();
        assertThat(event.attributes()).doesNotContainKey("parentOperationId");
    }

}

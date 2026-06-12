package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementWorkflowRunnerTest {

    @Test
    void deploysAndCorrelatesMaintenanceAndDeploymentEvents() {
        TestSkillRegistry targetRegistry = new TestSkillRegistry();
        TestSkillRegistry sourceRegistry = new TestSkillRegistry();
        SkillDefinition sourceSkill = TestSkillDefinitions.basic("planner");
        sourceRegistry.registerSkill(sourceSkill);
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.packageArtifact("planner", "v1");
        sourceArtifacts.putArtifact(SkillArtifact.text(reference, "package"));
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementWorkflowRunner runner = newRunner(
                targetRegistry,
                eventSink,
                Map.of("source-definitions", new RegistrySkillDefinitionStore(sourceRegistry)),
                Map.of("source-artifacts", sourceArtifacts));

        SkillManagementDeploymentResult result = runner.deploy(SkillManagementDeploymentConfig.of(
                serviceConfig(),
                SkillManagementMaintenanceSourceConfig.of(
                        SkillDefinitionStoreConfig.custom("source-definitions"),
                        SkillArtifactStoreConfig.custom("source-artifacts")),
                SkillManagementMaintenancePlan.bootstrap()));

        assertThat(result.changed()).isTrue();
        assertThat(result.service().getSkill("planner").await().indefinitely()).contains(sourceSkill);
        assertThat(result.service().getArtifact(reference).await().indefinitely()).isPresent();
        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.MAINTENANCE,
                        SkillManagementEventOperation.DEPLOYMENT);
        SkillManagementEvent maintenanceEvent = eventSink.events().get(0);
        SkillManagementEvent deploymentEvent = eventSink.events().get(1);
        assertThat(deploymentEvent.attributes().get("operationId")).isNotBlank();
        assertThat(maintenanceEvent.attributes())
                .containsEntry("parentOperationId", deploymentEvent.attributes().get("operationId"));
    }

    @Test
    void runMaintenanceRecordsPreflightFailureThroughOverrideSink() {
        TestSkillRegistry targetRegistry = new TestSkillRegistry();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementWorkflowRunner runner = newRunner(targetRegistry, eventSink, Map.of(), Map.of());

        assertThatThrownBy(() -> runner.runMaintenance(
                serviceConfig(),
                SkillManagementMaintenanceSourceConfig.definitions(
                        SkillDefinitionStoreConfig.custom("missing-source")),
                SkillManagementMaintenancePlan.bootstrap()))
                .isInstanceOf(SkillManagementMaintenancePreflightException.class)
                .hasMessageContaining("No custom skill definition store registered for: missing-source");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.MAINTENANCE);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("errorType", "SkillManagementMaintenancePreflightException")
                .containsEntry("preflightSourceStoreErrors", "1")
                .containsEntry(
                        "preflightSourceStoreMessage",
                        "No custom skill definition store registered for: missing-source");
        assertThat(event.attributes().get("operationId")).isNotBlank();
    }

    private SkillManagementWorkflowRunner newRunner(
            SkillRegistry targetRegistry,
            SkillManagementEventSink eventSink,
            Map<String, SkillDefinitionStore> definitionStores,
            Map<String, SkillArtifactStore> artifactStores) {
        SkillManagementStoreBundleFactory storeBundleFactory = new SkillManagementStoreBundleFactory(
                new SkillDefinitionStoreFactory(
                        targetRegistry,
                        (SkillManagementObjectStore) null,
                        (javax.sql.DataSource) null,
                        definitionStores),
                new SkillLifecycleStateStoreFactory(),
                new SkillManagementEventStoreFactory(),
                new SkillArtifactStoreFactory(artifactStores),
                eventSink);
        SkillManagementPreflightService preflightService =
                new SkillManagementPreflightService(storeBundleFactory);
        SkillManagementServiceAssembler serviceAssembler = new SkillManagementServiceAssembler(
                new SkillDefinitionStoreInspector(),
                new SkillLifecycleStateStoreInspector());
        return new SkillManagementWorkflowRunner(storeBundleFactory, preflightService, serviceAssembler);
    }

    private SkillManagementServiceConfig serviceConfig() {
        return SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.memory(),
                SkillLifecycleStateReconcileOptions.inspectOnly());
    }

}

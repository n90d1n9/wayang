package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementServiceTest {

    @Test
    void managesSkillLifecycleAgainstActiveRegistry() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementService service = new SkillManagementService(registry);

        SkillDefinition skill = skill("planner", "REASONING");
        service.createSkill(skill).await().indefinitely();

        assertThat(service.getSkill("planner").await().indefinitely()).contains(skill);
        assertThat(service.listActiveSkills().await().indefinitely()).extracting(SkillDefinition::id)
                .containsExactly("planner");

        service.disableSkill("planner").await().indefinitely();
        assertThat(service.getLifecycleState("planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(service.listActiveSkills().await().indefinitely()).isEmpty();

        SkillDefinition updated = TestSkillDefinitions.builder("planner")
                .name("Planner")
                .description("Plans tasks with tool context")
                .category("REASONING")
                .systemPrompt("Plan carefully.")
                .tools(List.of("search"))
                .build();
        service.updateSkill("planner", updated).await().indefinitely();

        assertThat(service.getLifecycleState("planner").await().indefinitely().revision()).isEqualTo(2);
        assertThat(service.search("search", "REASONING", true).await().indefinitely())
                .extracting(SkillDefinition::id)
                .containsExactly("planner");

        assertThat(service.deleteSkill("planner").await().indefinitely()).isTrue();
        assertThat(service.getSkill("planner").await().indefinitely()).isEmpty();
        assertThat(service.lifecycleSnapshot().await().indefinitely()).isEmpty();
    }

    @Test
    void preservesLifecycleStateThroughInjectedStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillLifecycleStateStore lifecycleStateStore = new InMemorySkillLifecycleStateStore();
        SkillManagementService firstService = new SkillManagementService(
                new RegistrySkillDefinitionStore(registry),
                lifecycleStateStore);
        firstService.createSkill(skill("planner", "REASONING")).await().indefinitely();
        firstService.disableSkill("planner").await().indefinitely();

        SkillManagementService secondService = new SkillManagementService(
                new RegistrySkillDefinitionStore(registry),
                lifecycleStateStore);

        assertThat(secondService.getLifecycleState("planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(secondService.listActiveSkills().await().indefinitely()).isEmpty();
    }

    @Test
    void lifecycleReadsDoNotPersistImplicitDefaults() {
        TestSkillRegistry registry = new TestSkillRegistry();
        registry.registerSkill(skill("planner", "REASONING"));
        InMemorySkillLifecycleStateStore lifecycleStateStore = new InMemorySkillLifecycleStateStore();
        SkillManagementService service = new SkillManagementService(
                new RegistrySkillDefinitionStore(registry),
                lifecycleStateStore);

        assertThat(service.listActiveSkills().await().indefinitely()).extracting(SkillDefinition::id)
                .containsExactly("planner");
        assertThat(service.getLifecycleState("planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.ACTIVE);
        assertThat(service.lifecycleSnapshot().await().indefinitely()).isEmpty();

        service.disableSkill("planner").await().indefinitely();

        assertThat(service.lifecycleSnapshot().await().indefinitely()).containsOnlyKeys("planner");
        assertThat(service.lifecycleSnapshot().await().indefinitely().get("planner").status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
    }

    @Test
    void failedStatusTransitionDoesNotPersistImplicitDefaultState() {
        TestSkillRegistry registry = new TestSkillRegistry();
        registry.registerSkill(skill("planner", "REASONING"));
        ToggleLifecycleStateStore lifecycleStateStore = new ToggleLifecycleStateStore();
        SkillManagementService service = new SkillManagementService(
                new RegistrySkillDefinitionStore(registry),
                lifecycleStateStore);

        lifecycleStateStore.failSaves = true;

        assertThatThrownBy(() -> service.disableSkill("planner").await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lifecycle save failed");
        assertThat(lifecycleStateStore.snapshot()).isEmpty();
    }

    @Test
    void emitsEventsForSuccessfulLifecycleOperations() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillLifecycleStateStore lifecycleStateStore = new InMemorySkillLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementService service = TestSkillManagementServices.builder(registry)
                .lifecycleStateStore(lifecycleStateStore)
                .eventSink(eventSink)
                .build();

        service.createSkill(skill("planner", "REASONING")).await().indefinitely();
        service.disableSkill("planner").await().indefinitely();
        service.reconcileLifecycleState(SkillLifecycleStateReconcileOptions.inspectOnly()).await().indefinitely();

        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.CREATE_SKILL,
                        SkillManagementEventOperation.TRANSITION_SKILL,
                        SkillManagementEventOperation.RECONCILE_LIFECYCLE);
        assertThat(eventSink.events()).allMatch(SkillManagementEvent::success);
        assertThat(eventSink.events().get(1).attributes())
                .containsEntry("status", SkillLifecycleStatus.DISABLED.name())
                .containsEntry("revision", "1");
        assertThat(eventSink.events().get(2).attributes())
                .containsEntry("consistent", "true")
                .containsEntry("missing", "0")
                .containsEntry("orphaned", "0");
        assertThat(service.eventHistory(SkillManagementEventQuery.latest()).await().indefinitely().events())
                .hasSize(3);
    }

    @Test
    void emitsRevisionAttributeForSkillUpdates() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillLifecycleStateStore lifecycleStateStore = new InMemorySkillLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementService service = TestSkillManagementServices.builder(registry)
                .lifecycleStateStore(lifecycleStateStore)
                .eventSink(eventSink)
                .build();
        service.createSkill(skill("planner", "REASONING")).await().indefinitely();
        SkillDefinition updated = TestSkillDefinitions.builder("planner")
                .name("Updated Planner")
                .description("Plans with updated context")
                .category("REASONING")
                .systemPrompt("Plan with the latest context.")
                .build();

        service.updateSkill("planner", updated).await().indefinitely();

        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.CREATE_SKILL,
                        SkillManagementEventOperation.UPDATE_SKILL);
        assertThat(eventSink.events().get(0).attributes().get("operationId")).isNotBlank();
        assertThat(eventSink.events().get(1).attributes())
                .containsEntry("revision", "2");
        assertThat(eventSink.events().get(1).attributes().get("operationId")).isNotBlank();
        assertThat(eventSink.events().get(1).attributes())
                .doesNotContainKey("parentOperationId");
    }

    @Test
    void runsMaintenanceAgainstManagedStores() {
        TestSkillRegistry sourceRegistry = new TestSkillRegistry();
        TestSkillRegistry targetRegistry = new TestSkillRegistry();
        InMemorySkillLifecycleStateStore lifecycleStateStore = new InMemorySkillLifecycleStateStore();
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore targetArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        sourceRegistry.registerSkill(skill("planner", "REASONING"));
        targetRegistry.registerSkill(skill("old", "GENERAL"));
        lifecycleStateStore.save(SkillLifecycleState.created("old"));
        sourceArtifacts.putArtifact(SkillArtifact.text(prompt, "source"));
        eventSink.record(event(SkillManagementEventOperation.UPDATE_SKILL, "old-event", true));
        eventSink.record(event(SkillManagementEventOperation.UPDATE_SKILL, "kept-event", true));
        SkillManagementService service = TestSkillManagementServices.builder(targetRegistry)
                .lifecycleStateStore(lifecycleStateStore)
                .artifactStore(targetArtifacts)
                .eventSink(eventSink)
                .build();

        SkillManagementMaintenanceResult result = service.runMaintenance(
                        new RegistrySkillDefinitionStore(sourceRegistry),
                        sourceArtifacts,
                        SkillManagementMaintenancePlan.mirrorAndRepair()
                                .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)))
                .await()
                .indefinitely();

        assertThat(result.definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.definitionSyncResult().deleted()).isEqualTo(1);
        assertThat(result.artifactSyncResult().copied()).isEqualTo(1);
        assertThat(result.lifecycleStateReconcileResult().createdStateSkillIds()).containsExactly("planner");
        assertThat(result.lifecycleStateReconcileResult().removedStateSkillIds()).containsExactly("old");
        assertThat(result.eventPruneResult().skipped()).isFalse();
        assertThat(result.eventPruneResult().prunedEvents()).isEqualTo(1);
        assertThat(result.changed()).isTrue();
        assertThat(result.consistent()).isTrue();
        assertThat(targetRegistry.getSkill("planner")).isPresent();
        assertThat(targetRegistry.getSkill("old")).isEmpty();
        assertThat(targetArtifacts.getArtifact(prompt)).isPresent();
        assertThat(lifecycleStateStore.snapshot()).containsOnlyKeys("planner");
        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.UPDATE_SKILL,
                        SkillManagementEventOperation.MAINTENANCE);
        assertThat(eventSink.events().get(0).skillId()).isEqualTo("kept-event");
        assertThat(eventSink.events().get(1).attributes())
                .containsEntry("artifactChanges", "1")
                .containsEntry("eventPruneEnabled", "true")
                .containsEntry("eventPruned", "1");
    }

    @Test
    void eventHistoryIsEmptyForWriteOnlySinks() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementEventSink writeOnlySink = event -> {
        };
        SkillManagementService service = TestSkillManagementServices.builder(registry)
                .eventSink(writeOnlySink)
                .build();

        service.createSkill(skill("planner", "REASONING")).await().indefinitely();

        assertThat(service.eventHistory(SkillManagementEventQuery.latest()).await().indefinitely().events())
                .isEmpty();
    }

    @Test
    void queryableEventSinkFiltersLimitsAndSummarizesEvents() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        eventSink.record(event(SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        eventSink.record(event(SkillManagementEventOperation.UPDATE_SKILL, "planner", true));
        eventSink.record(event(SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        SkillManagementEventPage latestPlannerEvents = eventSink.query(
                SkillManagementEventQuery.forSkill("planner", 2));

        assertThat(latestPlannerEvents.matchedEvents()).isEqualTo(3);
        assertThat(latestPlannerEvents.returnedEvents()).isEqualTo(2);
        assertThat(latestPlannerEvents.truncated()).isTrue();
        assertThat(latestPlannerEvents.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.UPDATE_SKILL,
                        SkillManagementEventOperation.DELETE_SKILL);
        assertThat(latestPlannerEvents.summary().totalEvents()).isEqualTo(2);
        assertThat(latestPlannerEvents.summary().successfulEvents()).isEqualTo(1);
        assertThat(latestPlannerEvents.summary().failedEvents()).isEqualTo(1);
        assertThat(latestPlannerEvents.summary().skillCounts()).containsEntry("planner", 2);

        SkillManagementEventPage failures = eventSink.query(SkillManagementEventQuery.failures(10));

        assertThat(failures.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DELETE_SKILL);
    }

    @Test
    void eventHistoryConvenienceMethodsFilterOperationCorrelation() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementService service = TestSkillManagementServices.builder()
                .eventSink(eventSink)
                .build();
        eventSink.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                true,
                Map.of("operationId", "deployment-1")));
        eventSink.record(event(
                SkillManagementEventOperation.MAINTENANCE,
                "",
                true,
                Map.of(
                        "operationId", "maintenance-1",
                        "parentOperationId", "deployment-1")));
        eventSink.record(event(
                SkillManagementEventOperation.MAINTENANCE,
                "",
                true,
                Map.of(
                        "operationId", "maintenance-2",
                        "parentOperationId", "deployment-2")));

        SkillManagementEventPage root = service.eventHistoryForOperation(" deployment-1 ", 10)
                .await().indefinitely();
        SkillManagementEventPage children = service.eventHistoryForParentOperation("deployment-1", 10)
                .await().indefinitely();
        SkillManagementAdminOperationTrace trace = service.operationTrace("deployment-1", 10)
                .await().indefinitely();
        SkillManagementAdminOperationTracePage tracePage = service.deploymentOperationTraces(10, 10)
                .await().indefinitely();
        SkillManagementAdminOperationTracePage healthyTracePage = service.deploymentOperationTraces(
                        SkillManagementOperationTraceQuery.deploymentsByStatus(
                                10,
                                10,
                                SkillManagementOperationTraceStatus.HEALTHY))
                .await().indefinitely();
        SkillManagementAdminOperationTracePage stringFilteredTracePage =
                service.deploymentOperationTraces(10, 10, " healthy ")
                        .await().indefinitely();

        assertThat(root.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DEPLOYMENT);
        assertThat(children.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.MAINTENANCE);
        assertThat(children.events().get(0).attributes()).containsEntry("operationId", "maintenance-1");
        assertThat(trace.rootEventAvailable()).isTrue();
        assertThat(trace.childEventCount()).isEqualTo(1);
        assertThat(trace.totalEvents()).isEqualTo(2);
        assertThat(tracePage.returnedRootEvents()).isEqualTo(1);
        assertThat(tracePage.traceableRootEvents()).isEqualTo(1);
        assertThat(tracePage.untraceableRootEvents()).isZero();
        assertThat(tracePage.filteredTraces()).isZero();
        assertThat(tracePage.returnedTraces()).isEqualTo(1);
        assertThat(tracePage.healthyTraces()).isEqualTo(1);
        assertThat(tracePage.traces()).extracting(SkillManagementAdminOperationTrace::operationId)
                .containsExactly("deployment-1");
        assertThat(healthyTracePage.statusFilter()).isEqualTo(SkillManagementOperationTraceStatus.HEALTHY.name());
        assertThat(healthyTracePage.filteredTraces()).isZero();
        assertThat(healthyTracePage.returnedTraces()).isEqualTo(1);
        assertThat(healthyTracePage.traces()).extracting(SkillManagementAdminOperationTrace::status)
                .containsExactly(SkillManagementOperationTraceStatus.HEALTHY.name());
        assertThat(stringFilteredTracePage.statusFilter())
                .isEqualTo(SkillManagementOperationTraceStatus.HEALTHY.name());
        assertThat(stringFilteredTracePage.returnedTraces()).isEqualTo(1);
    }

    @Test
    void inMemoryEventSinkRetainsOnlyConfiguredRecentEvents() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink(2);
        eventSink.record(null);
        eventSink.record(event(SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        eventSink.record(event(SkillManagementEventOperation.UPDATE_SKILL, "planner", true));
        eventSink.record(event(SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        SkillManagementEventReader reader = eventSink;

        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.UPDATE_SKILL,
                        SkillManagementEventOperation.DELETE_SKILL);
        assertThat(reader.latest().matchedEvents()).isEqualTo(2);
        assertThat(reader.latest().summary().failedEvents()).isEqualTo(1);

        eventSink.clear();

        assertThat(reader.latest().events()).isEmpty();
    }

    @Test
    void compositeEventSinkFansOutAndIsolatesFailingSinks() {
        InMemorySkillManagementEventSink firstSink = new InMemorySkillManagementEventSink();
        InMemorySkillManagementEventSink secondSink = new InMemorySkillManagementEventSink();
        SkillManagementEvent event = event(SkillManagementEventOperation.CREATE_SKILL, "planner", true);
        SkillManagementEventSink composite = SkillManagementEventSink.composite(
                firstSink,
                ignored -> {
                    throw new IllegalStateException("diagnostic sink failed");
                },
                null,
                secondSink);

        composite.record(event);
        composite.record(null);

        assertThat(firstSink.events()).containsExactly(event);
        assertThat(secondSink.events()).containsExactly(event);
        assertThat(((SkillManagementEventReader) composite).latest().events()).containsExactly(event);
        SkillManagementEventSink.composite(List.of()).record(event);
    }

    @Test
    void eventHistoryReadsThroughCompositeReadableSink() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillManagementEventSink readableSink = new InMemorySkillManagementEventSink();
        SkillManagementEventSink composite = SkillManagementEventSink.composite(
                event -> {
                },
                new FailingReadableEventSink(),
                readableSink);
        SkillManagementService service = TestSkillManagementServices.builder(registry)
                .eventSink(composite)
                .build();

        service.createSkill(skill("planner", "REASONING")).await().indefinitely();

        SkillManagementEventPage history = service.eventHistory(SkillManagementEventQuery.latest())
                .await()
                .indefinitely();
        assertThat(history.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.CREATE_SKILL);
    }

    @Test
    void managesArtifactsThroughConfiguredArtifactStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillArtifactStore artifactStore = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementService service = TestSkillManagementServices.builder(registry)
                .artifactStore(artifactStore)
                .eventSink(eventSink)
                .build();
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifact artifact = SkillArtifact.text(reference, "hello");

        service.putArtifact(artifact).await().indefinitely();

        assertThat(service.getArtifact(reference).await().indefinitely())
                .hasValueSatisfying(reloaded -> assertThat(new String(
                        reloaded.content(),
                        java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("hello"));
        assertThat(service.listArtifacts("planner").await().indefinitely()).containsExactly(reference);

        assertThat(service.deleteArtifact(reference).await().indefinitely()).isTrue();
        assertThat(service.getArtifact(reference).await().indefinitely()).isEmpty();
        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.PUT_ARTIFACT,
                        SkillManagementEventOperation.DELETE_ARTIFACT);
        assertThat(eventSink.events().get(0).attributes())
                .containsEntry("kind", "resource")
                .containsEntry("name", "prompt")
                .containsEntry("version", "v1")
                .containsEntry("sizeBytes", "5");
        assertThat(eventSink.events().get(1).attributes())
                .containsEntry("deleted", "true");
    }

    @Test
    void emitsEventForArtifactSynchronization() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore targetArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        sourceArtifacts.putArtifact(SkillArtifact.text(reference, "hello"));
        SkillManagementService service = TestSkillManagementServices.builder(registry)
                .artifactStore(targetArtifacts)
                .eventSink(eventSink)
                .build();

        SkillArtifactStoreSyncResult result = service.syncArtifacts(
                        sourceArtifacts,
                        SkillArtifactStoreSyncOptions.bootstrap())
                .await()
                .indefinitely();

        assertThat(result.copied()).isEqualTo(1);
        assertThat(targetArtifacts.getArtifact(reference)).isPresent();
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.SYNC_ARTIFACTS);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("copied", "1")
                .containsEntry("changed", "1")
                .containsEntry("consistent", "true")
                .containsEntry("deleted", "0");
    }

    @Test
    void emitsFailureEventWhenArtifactSynchronizationFails() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementService service = TestSkillManagementServices.builder(registry)
                .eventSink(eventSink)
                .build();

        assertThatThrownBy(() -> service.syncArtifacts(
                        new FailingSkillArtifactStore(),
                        SkillArtifactStoreSyncOptions.bootstrap())
                .await()
                .indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("artifact list failed");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.SYNC_ARTIFACTS);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("errorType", "IllegalStateException")
                .containsEntry("error", "artifact list failed");
    }

    @Test
    void exposesArtifactStoreInspection() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillArtifactStore artifactStore = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.mcpDescriptor("planner", "tools", "v1");
        artifactStore.putArtifact(SkillArtifact.text(reference, "tools"));
        SkillManagementService service = TestSkillManagementServices.builder(registry)
                .artifactStore(artifactStore)
                .build();

        SkillArtifactStoreInspection inspection = service.inspectArtifactStore().await().indefinitely();

        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.artifactReferences()).containsExactly("planner:mcp-descriptor:tools:v1");
    }

    @Test
    void rollsBackCreatedDefinitionWhenLifecycleInitializationFails() {
        TestSkillRegistry registry = new TestSkillRegistry();
        ToggleLifecycleStateStore lifecycleStateStore = new ToggleLifecycleStateStore();
        lifecycleStateStore.failSaves = true;
        SkillManagementService service = new SkillManagementService(
                new RegistrySkillDefinitionStore(registry),
                lifecycleStateStore);

        assertThatThrownBy(() -> service.createSkill(skill("planner", "REASONING")).await().indefinitely())
                .isInstanceOf(SkillManagementWriteException.class)
                .hasMessageContaining("create skill consistently");
        assertThat(registry.getSkill("planner")).isEmpty();
    }

    @Test
    void emitsFailureEventWhenCreateRollbackIsRequired() {
        TestSkillRegistry registry = new TestSkillRegistry();
        ToggleLifecycleStateStore lifecycleStateStore = new ToggleLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        lifecycleStateStore.failSaves = true;
        SkillManagementService service = TestSkillManagementServices.builder(registry)
                .lifecycleStateStore(lifecycleStateStore)
                .eventSink(eventSink)
                .build();

        assertThatThrownBy(() -> service.createSkill(skill("planner", "REASONING")).await().indefinitely())
                .isInstanceOf(SkillManagementWriteException.class)
                .hasMessageContaining("create skill consistently");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.CREATE_SKILL);
        assertThat(event.skillId()).isEqualTo("planner");
        assertThat(event.success()).isFalse();
        assertThat(event.attributes()).containsEntry("errorType", IllegalStateException.class.getSimpleName());
    }

    @Test
    void normalizesEventDefaultsAndAttributes() {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        attributes.put("b", "2");
        attributes.put(null, "ignored");
        attributes.put("a", null);

        SkillManagementEvent event = new SkillManagementEvent(
                null,
                SkillManagementEventOperation.CREATE_SKILL,
                null,
                true,
                attributes);

        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.skillId()).isEmpty();
        assertThat(event.attributes()).containsOnly(Map.entry("b", "2"));
        assertThatThrownBy(() -> event.attributes().put("c", "3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void restoresPreviousDefinitionWhenLifecycleRevisionFails() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillDefinition original = skill("planner", "REASONING");
        registry.registerSkill(original);
        ToggleLifecycleStateStore lifecycleStateStore = new ToggleLifecycleStateStore();
        lifecycleStateStore.put(new SkillLifecycleState(
                "planner",
                SkillLifecycleStatus.DISABLED,
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                3));
        SkillManagementService service = new SkillManagementService(
                new RegistrySkillDefinitionStore(registry),
                lifecycleStateStore);
        SkillDefinition updated = TestSkillDefinitions.builder("planner")
                .name("Updated")
                .description("Updated description")
                .category("REASONING")
                .systemPrompt("Updated prompt.")
                .build();

        lifecycleStateStore.failSaves = true;

        assertThatThrownBy(() -> service.updateSkill("planner", updated).await().indefinitely())
                .isInstanceOf(SkillManagementWriteException.class)
                .hasMessageContaining("update skill consistently");
        assertThat(registry.getSkill("planner")).contains(original);
        assertThat(lifecycleStateStore.get("planner").orElseThrow().revision()).isEqualTo(3);
        assertThat(lifecycleStateStore.get("planner").orElseThrow().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
    }

    @Test
    void restoresDeletedDefinitionWhenLifecycleCleanupFails() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillDefinition original = skill("planner", "REASONING");
        registry.registerSkill(original);
        ToggleLifecycleStateStore lifecycleStateStore = new ToggleLifecycleStateStore();
        lifecycleStateStore.put(SkillLifecycleState.created("planner"));
        SkillManagementService service = new SkillManagementService(
                new RegistrySkillDefinitionStore(registry),
                lifecycleStateStore);

        lifecycleStateStore.failRemoves = true;

        assertThatThrownBy(() -> service.deleteSkill("planner").await().indefinitely())
                .isInstanceOf(SkillManagementWriteException.class)
                .hasMessageContaining("delete skill consistently");
        assertThat(registry.getSkill("planner")).contains(original);
        assertThat(lifecycleStateStore.get("planner")).isPresent();
    }

    @Test
    void validatesDefinitionsBeforeRegistering() {
        SkillManagementService service = new SkillManagementService(new TestSkillRegistry());

        SkillDefinition invalid = new SkillDefinition(
                "bad",
                "",
                "Missing name and prompt",
                "GENERAL",
                "",
                Map.of(),
                null,
                3.0,
                0,
                null,
                null,
                List.of(),
                null,
                Map.of());

        assertThat(service.validateSkill(invalid).valid()).isFalse();
        assertThatThrownBy(() -> service.createSkill(invalid).await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skill name is required")
                .hasMessageContaining("System prompt is required");
    }

    private SkillDefinition skill(String id, String category) {
        return TestSkillDefinitions.builder(id)
                .name("Planner")
                .description("Plans tasks")
                .category(category)
                .systemPrompt("Plan carefully.")
                .build();
    }

    private SkillManagementEvent event(
            SkillManagementEventOperation operation,
            String skillId,
            boolean success) {
        return event(operation, skillId, success, Map.of());
    }

    private SkillManagementEvent event(
            SkillManagementEventOperation operation,
            String skillId,
            boolean success,
            Map<String, String> attributes) {
        return new SkillManagementEvent(
                Instant.parse("2026-01-01T00:00:00Z"),
                operation,
                skillId,
                success,
                attributes);
    }

    private static final class FailingReadableEventSink
            implements SkillManagementEventSink, SkillManagementEventReader {

        @Override
        public void record(SkillManagementEvent event) {
        }

        @Override
        public SkillManagementEventPage query(SkillManagementEventQuery query) {
            throw new IllegalStateException("event reader failed");
        }
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

    private static final class FailingSkillArtifactStore implements SkillArtifactStore {

        @Override
        public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
            throw new IllegalStateException("artifact get failed");
        }

        @Override
        public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
            throw new IllegalStateException("artifact list failed");
        }

        @Override
        public void putArtifact(SkillArtifact artifact) {
            throw new IllegalStateException("artifact put failed");
        }

        @Override
        public boolean deleteArtifact(SkillArtifactReference reference) {
            throw new IllegalStateException("artifact delete failed");
        }
    }
}

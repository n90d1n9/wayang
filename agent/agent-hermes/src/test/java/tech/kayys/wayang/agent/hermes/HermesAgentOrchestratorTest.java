package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillDefinitionStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillLifecycleStateStore;
import tech.kayys.wayang.agent.skills.management.SkillArtifact;
import tech.kayys.wayang.agent.skills.management.SkillArtifactQuery;
import tech.kayys.wayang.agent.skills.management.SkillArtifactReference;
import tech.kayys.wayang.agent.skills.management.SkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.AgentEvent;
import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentState;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesAgentOrchestratorTest {

    @Test
    void decoratesDelegateRequestAndLearnsAfterExecution(@TempDir Path tempDir) {
        CapturingDelegate delegate = new CapturingDelegate();
        SkillManagementService service = new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new InMemorySkillArtifactStore(),
                SkillManagementEventSink.noop());
        List<String> runtimeCalls = new ArrayList<>();
        List<HermesRuntimeEvent> runtimeEvents = new ArrayList<>();
        HermesRuntimePorts runtimePorts = new HermesRuntimePorts(
                directive -> captured(runtimeCalls, "execution", directive.operation(), directive.backend(), directive.toMetadata()),
                directive -> captured(runtimeCalls, "gateway", directive.operation(), directive.destinationId(), directive.toMetadata()),
                null,
                null,
                directive -> captured(
                        runtimeCalls,
                        "provider-routing",
                        directive.operation(),
                        directive.selectedProvider(),
                        directive.toMetadata()),
                null,
                null,
                null);
        HermesAgentOrchestrator orchestrator = new HermesAgentOrchestrator(
                delegate,
                HermesAgentModeConfig.defaults(),
                        request -> Uni.createFrom().item(new HermesMemorySnapshot(
                                "Remember the release checklist.",
                                "",
                                List.of(),
                                Map.of())),
                new HermesLearningLoop(service),
                runtimePorts,
                runtimeEvents::add);

        AgentResponse response = orchestrator.execute(AgentRequest.builder()
                        .requestId("req-4")
                        .prompt("Package a release candidate")
                        .build())
                .await()
                .indefinitely();

        assertThat(response.strategy()).isEqualTo(HermesAgentMode.MODE_ID);
        assertThat(orchestrator.supportedFeatures()).contains("gateway-continuity", "parallel-subagents");
        Map<String, Object> recommendedParameters = orchestrator.getRecommendedParameters();
        assertThat(recommendedParameters)
                .containsKey(HermesMetadataKeys.METADATA_CAPABILITIES)
                .containsKey(HermesMetadataKeys.PARAM_RUNTIME_DIAGNOSTICS)
                .containsKey(HermesMetadataKeys.PARAM_METADATA_CONTRACT)
                .containsKey(HermesMetadataKeys.PARAM_EXECUTION_DIRECTIVE)
                .containsKey(HermesMetadataKeys.PARAM_GATEWAY_DIRECTIVE)
                .containsKey(HermesMetadataKeys.PARAM_AUTOMATION_INTENT)
                .containsKey(HermesMetadataKeys.PARAM_AUTOMATION_DIRECTIVE)
                .containsKey(HermesMetadataKeys.PARAM_DELEGATION_PLAN)
                .containsKey(HermesMetadataKeys.PARAM_DELEGATION_DIRECTIVE)
                .containsKey(HermesMetadataKeys.PARAM_PROVIDER_ROUTING_PLAN)
                .containsKey(HermesMetadataKeys.PARAM_PROVIDER_ROUTING_DIRECTIVE)
                .containsKey(HermesMetadataKeys.PARAM_MEMORY_REFLECTION_PLAN)
                .containsKey(HermesMetadataKeys.PARAM_MEMORY_REFLECTION_DIRECTIVE)
                .containsKey(HermesMetadataKeys.PARAM_TRAJECTORY_EXPORT_PLAN)
                .containsKey(HermesMetadataKeys.PARAM_TRAJECTORY_EXPORT_DIRECTIVE)
                .containsKey(HermesMetadataKeys.PARAM_SKILL_LINEAGE_PLAN)
                .containsKey(HermesMetadataKeys.PARAM_SKILL_LINEAGE_DIRECTIVE);
        assertThat(metadataMap(recommendedParameters, HermesMetadataKeys.PARAM_RUNTIME_DIAGNOSTICS))
                .containsKey(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION);
        @SuppressWarnings("unchecked")
        Map<String, Object> metadataContract =
                (Map<String, Object>) recommendedParameters.get(HermesMetadataKeys.PARAM_METADATA_CONTRACT);
        assertThat(metadataContract)
                .containsEntry("id", HermesMetadataContract.CURRENT_ID)
                .containsEntry("schemaVersion", HermesMetadataContract.CURRENT_SCHEMA_VERSION);
        assertThat(orchestrator.buildInferenceRequest(
                        AgentRequest.builder().requestId("req-inference").prompt("Inspect diagnostics").build(),
                        null)
                .build()
                .metadata())
                .containsKey(HermesMetadataKeys.METADATA_RUNTIME_DIAGNOSTICS);
        HermesPortDispatchResult diagnosticsResult =
                orchestrator.inspectRuntimeDiagnostics(HermesRuntimeDiagnosticsDirective.summary());
        assertThat(diagnosticsResult.status()).isEqualTo("inspected");
        assertThat(diagnosticsResult.metadata())
                .containsEntry("ready", true)
                .containsEntry("view", "summary")
                .containsKey("diagnostics");
        assertThat(delegate.lastRequest).isNotNull();
        assertThat(delegate.lastRequest.strategy()).isEqualTo(tech.kayys.wayang.agent.spi.OrchestrationStrategy.HERMES_AGENT);
        assertThat(delegate.lastRequest.systemPrompt()).contains("Remember the release checklist.");
        assertThat(delegate.lastRequest.context()).containsEntry(HermesAgentMode.CONTEXT_MODE_KEY, HermesAgentMode.MODE_ID);
        assertThat(runtimeCalls).containsExactly("execution", "gateway", "provider-routing");
        assertThat(runtimeEvents).extracting(HermesRuntimeEvent::type)
                .containsExactly(
                        HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                        HermesRuntimeEvent.TYPE_DIRECTIVES_DISPATCHED,
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED);
        assertThat(runtimeEvents).extracting(HermesRuntimeEvent::requestId).containsOnly("req-4");
        assertThat(runtimeEvents.get(1).outcome()).isEqualTo("healthy");
        assertThat(runtimeEvents.get(1).metadata())
                .containsKey("dispatchReport")
                .containsEntry("attentionCount", 0);
        assertThat(runtimeEvents.get(2).metadata())
                .containsEntry("successful", true)
                .containsEntry("totalSteps", 3);
        assertThat(runtimeEvents.get(3).outcome()).isEqualTo("created");
        assertThat(runtimeEvents.get(3).metadata())
                .containsEntry("decision", "created")
                .containsEntry("persisted", true)
                .containsEntry("responseSuccessful", true)
                .containsKey("skillId");
        assertThat(delegate.lastRequest.context())
                .containsKey(HermesMetadataKeys.CONTEXT_RUNTIME_DIAGNOSTICS)
                .containsKey(HermesMetadataKeys.CONTEXT_DIRECTIVE_DISPATCH_REPORT);
        assertThat(delegate.lastRequest.parameters())
                .containsKey(HermesMetadataKeys.PARAM_RUNTIME_DIAGNOSTICS)
                .containsKey(HermesMetadataKeys.PARAM_DIRECTIVE_DISPATCH_REPORT);
        assertThat(delegate.lastRequest.context().get(HermesMetadataKeys.CONTEXT_RUNTIME_DIAGNOSTICS))
                .isEqualTo(delegate.lastRequest.parameters().get(HermesMetadataKeys.PARAM_RUNTIME_DIAGNOSTICS));
        @SuppressWarnings("unchecked")
        Map<String, Object> runtimeDiagnostics =
                (Map<String, Object>) delegate.lastRequest.context()
                        .get(HermesMetadataKeys.CONTEXT_RUNTIME_DIAGNOSTICS);
        assertThat(runtimeDiagnostics)
                .containsEntry("ready", true)
                .containsEntry("runtimePortsReady", true)
                .containsEntry("skillPersistenceReady", true)
                .containsEntry("configuredPortCount", 3L)
                .containsKey(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION);
        @SuppressWarnings("unchecked")
        Map<String, Object> dispatchReport =
                (Map<String, Object>) delegate.lastRequest.context()
                        .get(HermesMetadataKeys.CONTEXT_DIRECTIVE_DISPATCH_REPORT);
        assertThat(dispatchReport)
                .containsEntry("successful", true)
                .containsEntry("dispatchedCount", 3L)
                .containsEntry("skippedCount", 5L);
        assertThat(service.listSkills().await().indefinitely()).extracting(skill -> skill.id())
                .anyMatch(id -> id.startsWith("hermes-package-a-release-candidate"));
    }

    @Test
    void emitsLearningFailureEventWithoutFailingResponse(@TempDir Path tempDir) {
        CapturingDelegate delegate = new CapturingDelegate();
        SkillManagementService service = new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new FailingSkillArtifactStore(),
                SkillManagementEventSink.noop());
        List<HermesRuntimeEvent> runtimeEvents = new ArrayList<>();
        HermesAgentOrchestrator orchestrator = new HermesAgentOrchestrator(
                delegate,
                HermesAgentModeConfig.defaults(),
                HermesMemorySnapshotProvider.none(),
                new HermesLearningLoop(service),
                HermesRuntimePorts.noop(),
                runtimeEvents::add);

        AgentResponse response = orchestrator.execute(AgentRequest.builder()
                        .requestId("req-learning-failed")
                        .prompt("Package a release candidate")
                        .build())
                .await()
                .indefinitely();

        assertThat(response.successful()).isTrue();
        assertThat(runtimeEvents).extracting(HermesRuntimeEvent::type)
                .contains(
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        HermesRuntimeEvent.TYPE_SKILL_LEARNING_FAILED);
        HermesRuntimeEvent learningFailed = runtimeEvents.stream()
                .filter(event -> HermesRuntimeEvent.TYPE_SKILL_LEARNING_FAILED.equals(event.type()))
                .findFirst()
                .orElseThrow();
        assertThat(learningFailed.outcome()).isEqualTo("failed");
        assertThat(learningFailed.metadata())
                .containsEntry("responseSuccessful", true)
                .containsKey("errorType");
        assertThat(String.valueOf(learningFailed.metadata().get("error")))
                .contains("Failed to put skill artifact consistently");
    }

    @Test
    void observesLearningAuditRetentionAfterLearningWithoutDuplicateAlerts(@TempDir Path tempDir) {
        CapturingDelegate delegate = new CapturingDelegate();
        SkillManagementService service = new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new InMemorySkillArtifactStore(),
                SkillManagementEventSink.noop());
        HermesAgentModeConfig config = HermesAgentModeConfig.defaults();
        HermesLearningPromotionReceiptLedger receiptLedger =
                HermesLearningPromotionReceiptLedger.fileSystem(
                        tempDir.resolve("learning/promotion-receipts.jsonl"),
                        1);
        HermesLearnedSkillRepository learnedSkills = new HermesLearnedSkillRepository(
                service,
                new HermesSkillMarkdownRenderer(),
                config.skillPersistenceStrategy().routePlan().targetPlan());
        List<HermesRuntimeEvent> runtimeEvents = new ArrayList<>();
        HermesAgentOrchestrator orchestrator = new HermesAgentOrchestrator(
                delegate,
                config,
                HermesMemorySnapshotProvider.none(),
                new HermesLearningLoop(
                        learnedSkills,
                        new HermesSkillDistiller(),
                        config,
                        new HermesLearningSignalFactory(),
                        new HermesSkillReusePolicy(),
                        receiptLedger),
                HermesRuntimePorts.noop(),
                runtimeEvents::add,
                null,
                new HermesLearningAuditRetentionObserver(
                        new HermesLearningAuditService(receiptLedger),
                        new HermesLearningAuditRetentionEventMonitor(runtimeEvents::add)));

        orchestrator.execute(AgentRequest.builder()
                        .requestId("req-retention-a")
                        .prompt("Package a release candidate")
                        .build())
                .await()
                .indefinitely();
        orchestrator.execute(AgentRequest.builder()
                        .requestId("req-retention-b")
                        .prompt("Document a launch checklist")
                        .build())
                .await()
                .indefinitely();

        List<HermesRuntimeEvent> retentionEvents = runtimeEvents.stream()
                .filter(event -> HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION.equals(event.type()))
                .toList();
        assertThat(retentionEvents).hasSize(1);
        assertThat(retentionEvents.get(0).outcome()).isEqualTo("at-capacity");
        assertThat(retentionEvents.get(0).metadata())
                .containsEntry("retentionEventReason", "first-observation")
                .containsEntry("retentionState", "at-capacity")
                .containsEntry("currentRetentionState", "at-capacity")
                .containsEntry("ledgerType", "file-system")
                .containsEntry("maxEntries", 1);
        assertThat(orchestrator.learningAuditRetentionObservation())
                .extracting(
                        HermesLearningAuditRetentionObservation::outcome,
                        HermesLearningAuditRetentionObservation::reason,
                        HermesLearningAuditRetentionObservation::emitted)
                .containsExactly("suppressed", "duplicate-state", false);
        HermesPortDispatchResult diagnostics = orchestrator.inspectRuntimeDiagnostics(
                HermesRuntimeDiagnosticsDirective.learningAudit());
        assertThat(diagnostics.metadata())
                .containsKey(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION);
        assertThat(metadataMap(diagnostics.metadata(), HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION))
                .containsEntry("outcome", "suppressed")
                .containsEntry("reason", "duplicate-state");
        assertThat(metadataMap(diagnostics.metadata(), "diagnostics"))
                .containsKey(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION);
        assertThat(runtimeEvents).extracting(HermesRuntimeEvent::type)
                .contains(HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }

    private static HermesPortDispatchResult captured(
            List<String> calls,
            String port,
            String operation,
            String target,
            Map<String, Object> metadata) {
        calls.add(port);
        return new HermesPortDispatchResult(
                port,
                operation,
                target,
                true,
                true,
                true,
                "captured",
                "captured by test",
                metadata);
    }

    private static final class CapturingDelegate implements AgentOrchestrator {
        private AgentRequest lastRequest;

        @Override
        public String strategyId() {
            return "delegate";
        }

        @Override
        public Uni<AgentResponse> execute(AgentRequest request) {
            this.lastRequest = request;
            return Uni.createFrom().item(AgentResponse.builder()
                    .runId("run-4")
                    .requestId(request.requestId())
                    .answer("Release candidate packaged and smoke tested")
                    .steps(List.of(
                            step(1, "filesystem"),
                            step(2, "terminal"),
                            step(3, "rag")))
                    .totalSteps(3)
                    .successful(true)
                    .strategy("delegate")
                    .build());
        }

        @Override
        public Multi<AgentEvent> stream(AgentRequest request) {
            this.lastRequest = request;
            return Multi.createFrom().item(AgentEvent.started("run-stream", request.prompt()));
        }

        @Override
        public Uni<AgentState> step(AgentState state) {
            return Uni.createFrom().item(state);
        }

        @Override
        public boolean isTerminal(AgentState state) {
            return state.isTerminal();
        }

        private AgentState.ReasoningStep step(int number, String skillId) {
            return new AgentState.ReasoningStep(
                    number,
                    "Step " + number,
                    new AgentState.AgentAction(skillId, "complete part " + number, Map.of(), Instant.now()),
                    "ok",
                    5,
                    true);
        }
    }

    private static final class FailingSkillArtifactStore implements SkillArtifactStore {

        @Override
        public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
            return Optional.empty();
        }

        @Override
        public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
            return List.of();
        }

        @Override
        public void putArtifact(SkillArtifact artifact) {
            throw new IllegalStateException("artifact store unavailable");
        }

        @Override
        public boolean deleteArtifact(SkillArtifactReference reference) {
            return false;
        }
    }
}

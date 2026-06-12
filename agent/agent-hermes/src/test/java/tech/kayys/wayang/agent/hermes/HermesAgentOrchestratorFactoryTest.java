package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillDefinitionStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillLifecycleStateStore;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceResponse;
import tech.kayys.wayang.agent.spi.InferenceTypes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesAgentOrchestratorFactoryTest {

    @Test
    void assemblesExecutableHermesOrchestrator(@TempDir Path tempDir) {
        RecordingInferenceBackend backend = new RecordingInferenceBackend("factory answer");
        List<HermesRuntimeEvent> runtimeEvents = new ArrayList<>();

        HermesAgentOrchestrator orchestrator = HermesAgentOrchestratorFactory.create(
                HermesAgentRuntimeAssemblyRequest.builder()
                        .inferenceBackend(backend)
                        .skillManagementService(service(tempDir))
                        .memorySnapshotProvider(HermesMemorySnapshotProvider.none())
                        .config(HermesAgentModeConfig.defaults())
                        .resources(HermesPersistenceResources.empty())
                        .portContributions(HermesRuntimePortContributions.empty())
                        .runtimeEventSink(Optional.of(runtimeEvents::add))
                        .build());

        AgentResponse response = orchestrator.execute(AgentRequest.builder()
                        .requestId("req-factory")
                        .prompt("Inspect Hermes assembly")
                        .build())
                .await()
                .indefinitely();

        assertThat(response.answer()).isEqualTo("factory answer");
        assertThat(response.strategy()).isEqualTo(HermesAgentMode.MODE_ID);
        assertThat(backend.lastRequest)
                .isNotNull()
                .extracting(InferenceRequest::requestId)
                .isEqualTo("req-factory");
        assertThat(orchestrator.getRecommendedParameters())
                .containsKey(HermesMetadataKeys.PARAM_RUNTIME_DIAGNOSTICS)
                .containsKey(HermesMetadataKeys.PARAM_METADATA_CONTRACT);
        assertThat(orchestrator.runtimeDiagnostics().assemblyReport().inferenceBackend())
                .isEqualTo("recording");
        assertThat(orchestrator.runtimeDiagnostics().assemblyReport().runtimeEventSinkContributed())
                .isTrue();
        assertThat(runtimeEvents).extracting(HermesRuntimeEvent::type)
                .contains(
                        HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                        HermesRuntimeEvent.TYPE_DIRECTIVES_DISPATCHED,
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED);
    }

    @Test
    void assemblyRequestNormalizesOptionalRuntimeParts(@TempDir Path tempDir) {
        HermesAgentRuntimeAssemblyRequest request = HermesAgentRuntimeAssemblyRequest.builder()
                .inferenceBackend(new RecordingInferenceBackend("defaults"))
                .skillManagementService(service(tempDir))
                .runtimeEventSink(null)
                .build();

        assertThat(request.memorySnapshotProvider()).isNotNull();
        assertThat(request.config()).isNotNull();
        assertThat(request.resources().objectStorageService()).isEmpty();
        assertThat(request.resources().dataSource()).isEmpty();
        assertThat(request.portContributions().skillLineageRepairAdapters()).isEmpty();
        assertThat(request.runtimeEventSink()).isEmpty();
    }

    @Test
    void assemblyRequestRequiresCoreDependencies(@TempDir Path tempDir) {
        assertThatThrownBy(() -> HermesAgentRuntimeAssemblyRequest.builder()
                .skillManagementService(service(tempDir))
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("inferenceBackend");
        assertThatThrownBy(() -> HermesAgentRuntimeAssemblyRequest.builder()
                .inferenceBackend(new RecordingInferenceBackend("missing-service"))
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("skillManagementService");
    }

    private static SkillManagementService service(Path tempDir) {
        return new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new InMemorySkillArtifactStore(),
                SkillManagementEventSink.noop());
    }

    private static final class RecordingInferenceBackend implements InferenceBackend {
        private final String content;
        private InferenceRequest lastRequest;

        private RecordingInferenceBackend(String content) {
            this.content = content;
        }

        @Override
        public String name() {
            return "recording";
        }

        @Override
        public String version() {
            return "test";
        }

        @Override
        public Uni<InferenceResponse> infer(InferenceRequest request) {
            this.lastRequest = request;
            return Uni.createFrom().item(InferenceResponse.builder()
                    .requestId(request.requestId())
                    .content(content)
                    .durationMs(7)
                    .build());
        }

        @Override
        public Multi<InferenceTypes.StreamingChunk> stream(InferenceRequest request) {
            return Multi.createFrom().empty();
        }

        @Override
        public List<InferenceTypes.ProviderInfo> listProviders() {
            return List.of();
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public Map<String, Object> defaultParameters() {
            return Map.of();
        }
    }
}

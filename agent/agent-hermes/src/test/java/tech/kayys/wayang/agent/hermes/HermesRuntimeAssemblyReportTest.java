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
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceResponse;
import tech.kayys.wayang.agent.spi.InferenceTypes;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimeAssemblyReportTest {

    @Test
    void describesSelectedBackendsAndRuntimeContributions(@TempDir Path tempDir) {
        SkillManagementService skillManagementService = service(tempDir);
        HermesLearnedSkillRepository learnedSkills = new HermesLearnedSkillRepository(
                skillManagementService,
                new HermesSkillMarkdownRenderer());
        HermesRuntimePortContributions portContributions = new HermesRuntimePortContributions(
                Optional.empty(),
                Optional.empty(),
                Optional.of(HermesExecutionPort.noop()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of());
        HermesRuntimeAssemblyReport report = HermesRuntimeAssemblyReport.from(
                HermesAgentRuntimeAssemblyRequest.builder()
                        .inferenceBackend(new RecordingInferenceBackend())
                        .skillManagementService(skillManagementService)
                        .portContributions(portContributions)
                        .runtimeEventSink(Optional.of(HermesRuntimeEventSink.noop()))
                        .build(),
                learnedSkills,
                HermesRuntimePorts.builder()
                        .executionPort(new ConfiguredExecutionPort())
                        .build());

        assertThat(report.inferenceBackend()).isEqualTo("recording");
        assertThat(report.runtimeEventSinkContributed()).isTrue();
        assertThat(report.contributedRuntimePorts()).containsExactly("execution");
        assertThat(report.configuredRuntimePorts()).containsExactly("execution");
        assertThat(report.learnedSkillTargetSummary()).contains("definitions=");
        assertThat(report.toMetadata())
                .containsEntry("inferenceBackend", "recording")
                .containsEntry("contributedRuntimePortCount", 1)
                .containsEntry("configuredRuntimePortCount", 1)
                .containsKey("skillManagementServiceClass");
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
            return Uni.createFrom().item(InferenceResponse.builder()
                    .requestId(request.requestId())
                    .content("ok")
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

    private static final class ConfiguredExecutionPort implements HermesExecutionPort {
        @Override
        public HermesPortDispatchResult dispatch(HermesExecutionDirective directive) {
            return new HermesPortDispatchResult(
                    "execution",
                    directive.operation(),
                    directive.backend(),
                    true,
                    true,
                    true,
                    "ok",
                    "execution configured",
                    directive.toMetadata());
        }

        @Override
        public HermesRuntimePortDescriptor descriptor() {
            return HermesRuntimePortDescriptor.configured("execution", this);
        }
    }
}

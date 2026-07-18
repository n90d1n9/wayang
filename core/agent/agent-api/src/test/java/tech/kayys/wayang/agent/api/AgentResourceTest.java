package tech.kayys.wayang.agent.api;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.core.AgentClient;
import tech.kayys.wayang.agent.spi.AgentEvent;
import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.AgentState;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceResponse;
import tech.kayys.wayang.agent.spi.InferenceTypes;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentResourceTest {

    @Test
    void runMapsRequestToActiveAgentClient() {
        RecordingInferenceBackend backend = new RecordingInferenceBackend("done");
        AgentResource resource = resource(backend, registry());

        Response response = resource.run(new AgentResource.AgentRunRequest(
                "hello",
                "system",
                "react",
                List.of("echo"),
                4,
                "PT5S",
                "tenant-a",
                "user-a",
                "session-a",
                "test-model",
                Map.of("traceId", "trace-1")))
                .await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertThat(body)
                .containsEntry("answer", "done")
                .containsEntry("successful", true)
                .containsEntry("strategy", "react");
        assertThat(backend.lastRequest.model()).isEqualTo("test-model");
        assertThat(backend.lastRequest.requestId()).isNotBlank();
        assertThat(backend.lastRequest.messages()).extracting(InferenceTypes.ChatMessage::content)
                .containsExactly("hello");
    }

    @Test
    void runRejectsMissingPrompt() {
        AgentResource resource = resource(new RecordingInferenceBackend("unused"), registry());

        Response response = resource.run(new AgentResource.AgentRunRequest(
                "",
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null))
                .await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo(Map.of("error", "Prompt is required"));
    }

    @Test
    void runAcceptsHermesStrategyWhenClientHasHermesOrchestrator() {
        RecordingAgentOrchestrator hermes = new RecordingAgentOrchestrator("hermes-agent");
        AgentClient client = AgentClient.builder()
                .inferenceBackend(new RecordingInferenceBackend("unused"))
                .orchestrator(hermes)
                .build();
        AgentResource resource = resource(client, registry());

        Response response = resource.run(new AgentResource.AgentRunRequest(
                "persist this workflow",
                null,
                "hermes-agent",
                List.of(),
                0,
                null,
                "tenant-a",
                "user-a",
                "session-a",
                null,
                null))
                .await().indefinitely();

        assertThat(response.getStatus()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertThat(body)
                .containsEntry("answer", "handled by hermes-agent")
                .containsEntry("successful", true)
                .containsEntry("strategy", "hermes-agent");
        assertThat(hermes.lastRequest.strategy()).isEqualTo(OrchestrationStrategy.HERMES_AGENT);
        assertThat(hermes.lastRequest.tenantId()).isEqualTo("tenant-a");
        assertThat(hermes.lastRequest.sessionId()).isEqualTo("session-a");
    }

    @Test
    void listSkillsAndHealthUseRuntimeAndDefinitionRegistries() {
        AgentApiTestSkillRegistry registry = registry();
        registry.register(new EchoSkill());
        registry.registerSkill(SkillDefinition.builder()
                .id("planner")
                .name("Planner")
                .description("Plans tasks")
                .category("REASONING")
                .systemPrompt("Plan carefully.")
                .metadata(Map.of("version", "2.0.0", "priority", 12))
                .build());
        AgentResource resource = resource(new RecordingInferenceBackend("unused"), registry);

        assertThat(resource.listSkills(null)).extracting(AgentResource.SkillSummary::id)
                .containsExactly("echo", "planner");
        assertThat(resource.listSkills(new AgentSkillsRequest("REASONING"))).extracting(AgentResource.SkillSummary::id)
                .containsExactly("planner");
        assertThat(resource.getSkill("planner").getStatus()).isEqualTo(200);
        assertThat(resource.getSkill("missing").getStatus()).isEqualTo(404);
        assertThat(resource.health())
                .isEqualTo(new AgentResource.AgentHealthResponse("UP", 2, 1, 1));
    }

    private AgentResource resource(InferenceBackend backend, SkillRegistry registry) {
        return resource(AgentClient.builder().inferenceBackend(backend).build(), registry);
    }

    private AgentResource resource(AgentClient client, SkillRegistry registry) {
        AgentResource resource = new AgentResource();
        resource.agentClient = client;
        resource.skillRegistry = registry;
        return resource;
    }

    private AgentApiTestSkillRegistry registry() {
        return new AgentApiTestSkillRegistry();
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
            lastRequest = request;
            return Uni.createFrom().item(InferenceResponse.builder()
                    .responseId("response-1")
                    .requestId(request.requestId())
                    .model(request.model())
                    .content(content)
                    .usage(InferenceTypes.TokenUsage.of(1, 1))
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
    }

    private static final class RecordingAgentOrchestrator implements AgentOrchestrator {
        private final String strategyId;
        private AgentRequest lastRequest;

        private RecordingAgentOrchestrator(String strategyId) {
            this.strategyId = strategyId;
        }

        @Override
        public String strategyId() {
            return strategyId;
        }

        @Override
        public Uni<AgentResponse> execute(AgentRequest request) {
            lastRequest = request;
            return Uni.createFrom().item(AgentResponse.builder()
                    .requestId(request.requestId())
                    .answer("handled by " + strategyId)
                    .strategy(strategyId)
                    .successful(true)
                    .build());
        }

        @Override
        public Multi<AgentEvent> stream(AgentRequest request) {
            return Multi.createFrom().empty();
        }

        @Override
        public Uni<AgentState> step(AgentState state) {
            return Uni.createFrom().item(state);
        }

        @Override
        public boolean isTerminal(AgentState state) {
            return state.isTerminal();
        }
    }

    private static final class EchoSkill implements AgentSkill {
        @Override
        public String id() {
            return "echo";
        }

        @Override
        public String name() {
            return "Echo";
        }

        @Override
        public String description() {
            return "Echoes text";
        }

        @Override
        public String category() {
            return SkillCategory.EXECUTION.name();
        }

        @Override
        public boolean isHealthy() {
            return false;
        }

        @Override
        public Uni<Map<String, Object>> execute(Map<String, Object> context) {
            return Uni.createFrom().item(Map.of("success", true));
        }
    }

}

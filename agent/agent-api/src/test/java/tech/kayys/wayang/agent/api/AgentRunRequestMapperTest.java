package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRunRequestMapperTest {

    private final AgentRunRequestMapper mapper = new AgentRunRequestMapper();

    @Test
    void mapsRequestDefaultsAndFiltersBlankSkills() {
        AgentRequest request = mapper.toAgentRequest(new AgentResource.AgentRunRequest(
                "hello",
                null,
                null,
                List.of("echo", "", "  ", "planner"),
                0,
                null,
                "",
                "user-a",
                "session-a",
                "model-a",
                Map.of("traceId", "trace-1")), false);

        assertThat(request.prompt()).isEqualTo("hello");
        assertThat(request.strategy()).isEqualTo(OrchestrationStrategy.REACT);
        assertThat(request.allowedSkills()).containsExactly("echo", "planner");
        assertThat(request.getMaxSteps()).isEqualTo(15);
        assertThat(request.tenantId()).isEqualTo("default");
        assertThat(request.userId()).isEqualTo("user-a");
        assertThat(request.sessionId()).isEqualTo("session-a");
        assertThat(request.modelId()).isEqualTo("model-a");
        assertThat(request.context()).containsEntry("traceId", "trace-1");
        assertThat(request.stream()).isFalse();
    }

    @Test
    void mapsExplicitStrategyTimeoutAndStreamingFlag() {
        AgentRequest request = mapper.toAgentRequest(new AgentResource.AgentRunRequest(
                "work",
                "system",
                "hermes-agent",
                List.of(),
                7,
                "30",
                "tenant-a",
                null,
                null,
                null,
                null), true);

        assertThat(request.systemPrompt()).isEqualTo("system");
        assertThat(request.strategy()).isEqualTo(OrchestrationStrategy.HERMES_AGENT);
        assertThat(request.getMaxSteps()).isEqualTo(7);
        assertThat(request.getTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(request.tenantId()).isEqualTo("tenant-a");
        assertThat(request.stream()).isTrue();
    }

    @Test
    void mapsUnknownStrategyToCustom() {
        AgentRequest request = mapper.toAgentRequest(new AgentResource.AgentRunRequest(
                "work",
                null,
                "experimental",
                null,
                0,
                "PT2M",
                null,
                null,
                null,
                null,
                null), false);

        assertThat(request.strategy()).isEqualTo(OrchestrationStrategy.CUSTOM);
        assertThat(request.getTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void rejectsNullOrBlankPrompt() {
        assertThatThrownBy(() -> mapper.toAgentRequest(null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt is required");
        assertThatThrownBy(() -> mapper.toAgentRequest(new AgentResource.AgentRunRequest(
                " ",
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt is required");
    }
}

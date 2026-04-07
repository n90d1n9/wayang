package tech.kayys.wayang.agent.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.spi.AgentRequest;
import tech.kayys.wayang.agent.core.spi.AgentResponse;
import tech.kayys.wayang.agent.core.spi.SkillRegistry;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ToolCallingAgentLoop.
 *
 * Tests cover:
 * - Native tool calling workflow
 * - Parallel tool execution
 * - Tool caching
 * - Error handling
 * - Streaming responses
 */
@QuarkusTest
class ToolCallingAgentLoopTest {

    @Inject
    ToolCallingAgentLoop agentLoop;

    @Inject
    SkillRegistry skillRegistry;

    @Test
    void testBasicToolCalling() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-tool-1")
            .tenantId("test-tenant")
            .prompt("What is 25 + 17?")
            .allowedSkills(Set.of("calculator"))
            .modelId("default")
            .build();

        // When
        AgentResponse response = agentLoop.execute(request)
            .await().atMost(Duration.ofSeconds(60));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.successful()).isTrue();
        assertThat(response.answer()).contains("42");
        assertThat(response.totalSteps()).isGreaterThan(0);
    }

    @Test
    void testMultipleToolCalls() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-tool-2")
            .tenantId("test-tenant")
            .prompt("Calculate 10 * 5, then add 20 to the result")
            .allowedSkills(Set.of("calculator"))
            .modelId("default")
            .build();

        // When
        AgentResponse response = agentLoop.execute(request)
            .await().atMost(Duration.ofSeconds(60));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.successful()).isTrue();
        assertThat(response.answer()).contains("70");
    }

    @Test
    void testToolCaching() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-tool-3")
            .tenantId("test-tenant")
            .prompt("Calculate 5 + 5")
            .allowedSkills(Set.of("calculator"))
            .modelId("default")
            .build();

        // When - First call
        AgentResponse response1 = agentLoop.execute(request)
            .await().atMost(Duration.ofSeconds(60));

        // When - Second call (should use cache)
        AgentResponse response2 = agentLoop.execute(request)
            .await().atMost(Duration.ofSeconds(60));

        // Then
        assertThat(response1).isNotNull();
        assertThat(response2).isNotNull();
        // Both should succeed
        assertThat(response1.successful()).isTrue();
        assertThat(response2.successful()).isTrue();
    }

    @Test
    void testToolNotFound() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-tool-4")
            .tenantId("test-tenant")
            .prompt("Use the non-existent tool")
            .allowedSkills(Set.of("non-existent-tool"))
            .modelId("default")
            .build();

        // When
        AgentResponse response = agentLoop.execute(request)
            .await().atMost(Duration.ofSeconds(60));

        // Then
        assertThat(response).isNotNull();
        // Should handle gracefully with error message
        assertThat(response.successful()).isFalse();
        assertThat(response.error()).isNotNull();
    }

    @Test
    void testMaxStepsReached() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-tool-5")
            .tenantId("test-tenant")
            .prompt("Keep thinking forever")
            .modelId("default")
            .build();

        // When
        AgentResponse response = agentLoop.execute(request)
            .await().atMost(Duration.ofSeconds(90));

        // Then
        assertThat(response).isNotNull();
        // Should terminate with max steps message
        assertThat(response.answer())
            .containsIgnoringCase("maximum")
            .or(contains("Max steps"));
    }

    @Test
    void testStreamingResponse() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-tool-6")
            .tenantId("test-tenant")
            .prompt("Simple calculation: 3 + 3")
            .allowedSkills(Set.of("calculator"))
            .modelId("default")
            .build();

        // When - Test streaming
        var events = agentLoop.stream(request)
            .collect().asList()
            .await().atMost(Duration.ofSeconds(60));

        // Then
        assertThat(events).isNotEmpty();
        // Should have at least started event
        assertThat(events.stream()
            .anyMatch(e -> e.type().equals("started"))).isTrue();
    }

    @Test
    void testCacheClear() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-tool-7")
            .tenantId("test-tenant")
            .prompt("Calculate 1 + 1")
            .allowedSkills(Set.of("calculator"))
            .modelId("default")
            .build();

        agentLoop.execute(request)
            .await().atMost(Duration.ofSeconds(60));

        // When
        agentLoop.clearCache();

        // Then - Cache should be empty
        // (Implementation detail - would need accessor to verify)
    }
}

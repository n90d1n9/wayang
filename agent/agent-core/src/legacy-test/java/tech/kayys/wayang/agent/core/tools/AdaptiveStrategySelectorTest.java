package tech.kayys.wayang.agent.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.core.spi.AgentRequest;
import tech.kayys.wayang.agent.core.spi.OrchestrationStrategy;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AdaptiveStrategySelector.
 *
 * Tests cover:
 * - Task type analysis
 * - Strategy selection logic
 * - Statistics tracking
 * - Cache behavior
 */
@QuarkusTest
class AdaptiveStrategySelectorTest {

    @Inject
    AdaptiveStrategySelector selector;

    @Test
    void testSimpleTaskSelection() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-1")
            .tenantId("test")
            .prompt("What is the capital of France?")
            .build();

        // When
        AgentOrchestrator orchestrator = selector.selectOrchestrator(request);

        // Then
        assertThat(orchestrator).isNotNull();
        assertThat(orchestrator.strategyId()).isEqualTo("react");
    }

    @Test
    void testToolRequiredTaskSelection() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-2")
            .tenantId("test")
            .prompt("Calculate the sum of 1 to 100")
            .allowedSkills(java.util.Set.of("calculator"))
            .build();

        // When
        AgentOrchestrator orchestrator = selector.selectOrchestrator(request);

        // Then
        assertThat(orchestrator).isNotNull();
        // Should select tool-calling or react strategy
        assertThat(orchestrator.strategyId())
            .isIn("tool-calling", "react");
    }

    @Test
    void testComplexTaskSelection() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-3")
            .tenantId("test")
            .prompt("First, analyze the data. Then, create a plan. Finally, execute the plan step by step.")
            .build();

        // When
        String taskType = selector.analyzeTaskType(request);
        AgentOrchestrator orchestrator = selector.selectOrchestrator(request);

        // Then
        assertThat(taskType).isEqualTo("complex");
        assertThat(orchestrator.strategyId()).isEqualTo("plan-and-execute");
    }

    @Test
    void testQualityCriticalTaskSelection() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-4")
            .tenantId("test")
            .prompt("This is very important and critical. Must be accurate and verified.")
            .parameters(Map.of("quality_critical", true))
            .build();

        // When
        String taskType = selector.analyzeTaskType(request);
        AgentOrchestrator orchestrator = selector.selectOrchestrator(request);

        // Then
        assertThat(taskType).isEqualTo("quality_critical");
        assertThat(orchestrator.strategyId()).isEqualTo("reflexion");
    }

    @Test
    void testMultiDomainTaskSelection() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-5")
            .tenantId("test")
            .prompt("Compare and analyze multiple sources across different domains")
            .allowedSkills(java.util.Set.of("search", "analyze", "summarize"))
            .build();

        // When
        String taskType = selector.analyzeTaskType(request);
        AgentOrchestrator orchestrator = selector.selectOrchestrator(request);

        // Then
        assertThat(taskType).isEqualTo("multi_domain");
        assertThat(orchestrator.strategyId()).isEqualTo("multi-agent");
    }

    @Test
    void testStatisticsRecording() {
        // Given
        String strategyId = "react";

        // When
        selector.recordResult(strategyId, true, 1000, 0.9);
        selector.recordResult(strategyId, true, 1200, 0.85);
        selector.recordResult(strategyId, false, 800, 0.0);

        AdaptiveStrategySelector.StrategyStats stats = 
            selector.getStrategyStats(strategyId);

        // Then
        assertThat(stats.getTotalExecutions()).isEqualTo(3);
        assertThat(stats.getSuccessfulExecutions()).isEqualTo(2);
        assertThat(stats.getSuccessRate()).isGreaterThan(0.6);
        assertThat(stats.getAvgDurationMs()).isGreaterThan(0);
    }

    @Test
    void testTaskTypeCaching() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-6")
            .tenantId("test")
            .prompt("Simple question")
            .build();

        // When - First call
        String taskType1 = selector.analyzeTaskType(request);
        
        // When - Second call (should use cache)
        String taskType2 = selector.analyzeTaskType(request);

        // Then
        assertThat(taskType1).isEqualTo(taskType2);
    }

    @Test
    void testClearCache() {
        // Given
        AgentRequest request = AgentRequest.builder()
            .requestId("test-7")
            .tenantId("test")
            .prompt("Test")
            .build();

        selector.analyzeTaskType(request);

        // When
        selector.clearTaskCache();

        // Then - Cache should be empty, will re-analyze
        String taskType = selector.analyzeTaskType(request);
        assertThat(taskType).isNotNull();
    }
}

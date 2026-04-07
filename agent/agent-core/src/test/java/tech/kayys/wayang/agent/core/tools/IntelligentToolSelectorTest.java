package tech.kayys.wayang.agent.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnCi;
import tech.kayys.wayang.agent.core.spi.SkillRegistry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Unit tests for IntelligentToolSelector.
 */
@QuarkusTest
class IntelligentToolSelectorTest {

    @Inject
    IntelligentToolSelector toolSelector;

    @Inject
    SkillRegistry skillRegistry;

    private IntelligentToolSelector.ProjectContext reactContext;
    private IntelligentToolSelector.ProjectContext springContext;
    private IntelligentToolSelector.ProjectContext vueContext;

    @BeforeEach
    void setUp() {
        // React project context
        reactContext = IntelligentToolSelector.ProjectContext.builder()
            .framework("react")
            .framework("typescript")
            .feature("tailwind", true)
            .feature("redux", true)
            .feature("jest", true)
            .buildTool("npm")
            .testFramework("jest")
            .build();

        // Spring project context
        springContext = IntelligentToolSelector.ProjectContext.builder()
            .framework("spring")
            .framework("java")
            .feature("maven", true)
            .feature("junit", true)
            .feature("postgresql", true)
            .buildTool("maven")
            .testFramework("junit")
            .build();

        // Vue project context
        vueContext = IntelligentToolSelector.ProjectContext.builder()
            .framework("vue")
            .framework("typescript")
            .feature("tailwind", true)
            .feature("pinia", true)
            .feature("vitest", true)
            .buildTool("npm")
            .testFramework("vitest")
            .build();
    }

    @Test
    @DisplayName("Should select tools based on framework detection")
    void shouldSelectToolsByFramework() {
        // When
        IntelligentToolSelector.ToolChain chain = toolSelector.selectToolsByFramework(
            reactContext, "Create a new React component with Redux state"
        );

        // Then
        assertThat(chain).isNotNull();
        assertThat(chain.size()).isGreaterThan(0);
        assertThat(chain.getTools()).isNotEmpty();
    }

    @Test
    @DisplayName("Should select React-specific tools")
    void shouldSelectReactTools() {
        // When
        IntelligentToolSelector.ToolChain chain = toolSelector.selectToolsByFramework(
            reactContext, "Create component"
        );

        // Then
        assertThat(chain.getTools()).extracting("toolId")
            .anyMatch(id -> id.contains("react") || id.contains("component"));
    }

    @Test
    @DisplayName("Should select Spring-specific tools")
    void shouldSelectSpringTools() {
        // When
        IntelligentToolSelector.ToolChain chain = toolSelector.selectToolsByFramework(
            springContext, "Create REST API endpoint"
        );

        // Then
        assertThat(chain.getTools()).extracting("toolId")
            .anyMatch(id -> id.contains("spring") || id.contains("java") || id.contains("controller"));
    }

    @Test
    @DisplayName("Should select Vue-specific tools")
    void shouldSelectVueTools() {
        // When
        IntelligentToolSelector.ToolChain chain = toolSelector.selectToolsByFramework(
            vueContext, "Create Vue component"
        );

        // Then
        assertThat(chain.getTools()).extracting("toolId")
            .anyMatch(id -> id.contains("vue") || id.contains("component"));
    }

    @Test
    @DisplayName("Should handle Tailwind CSS detection")
    void shouldDetectTailwind() {
        // Then
        assertThat(reactContext.hasTailwind()).isTrue();
        assertThat(vueContext.hasTailwind()).isTrue();
        assertThat(springContext.hasTailwind()).isFalse();
    }

    @Test
    @DisplayName("Should handle state management detection")
    void shouldDetectStateManagement() {
        // Then
        assertThat(reactContext.hasRedux()).isTrue();
        assertThat(reactContext.hasZustand()).isFalse();
    }

    @Test
    @DisplayName("Should handle build tool detection")
    void shouldDetectBuildTool() {
        // Then
        assertThat(reactContext.hasMaven()).isFalse();
        assertThat(reactContext.hasGradle()).isFalse();
        assertThat(springContext.hasMaven()).isTrue();
    }

    @Test
    @DisplayName("Should get tool recommendations")
    @DisabledOnCi // Requires AI model availability
    void shouldGetRecommendations() {
        // When
        List<IntelligentToolSelector.ToolRecommendation> recommendations = toolSelector.recommendTools(
            reactContext, "Create a form with validation"
        ).await().atMost(Duration.ofSeconds(15));

        // Then
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).allMatch(r -> r.confidence() >= 0.0 && r.confidence() <= 1.0);
        assertThat(recommendations).allMatch(r -> r.toolId() != null && !r.toolId().isBlank());
    }

    @Test
    @DisplayName("Should execute tool chain")
    void shouldExecuteToolChain() {
        // Given
        IntelligentToolSelector.ToolChain chain = new IntelligentToolSelector.ToolChain();
        chain.addTool(new IntelligentToolSelector.SelectedTool(
            "test_tool",
            0.9,
            "Test selection",
            1,
            Map.of("key", "value"),
            true
        ));

        // When
        IntelligentToolSelector.ToolChainResult result = toolSelector.executeToolChain(
            chain, reactContext
        ).await().atMost(Duration.ofSeconds(10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasFailure()).isFalse();
    }

    @Test
    @DisplayName("Should track tool performance metrics")
    void shouldTrackToolMetrics() {
        // Given
        IntelligentToolSelector.ToolChain chain = new IntelligentToolSelector.ToolChain();
        chain.addTool(new IntelligentToolSelector.SelectedTool(
            "metrics_tool",
            0.8,
            "Test",
            1,
            Map.of(),
            true
        ));

        // When - Execute multiple times
        for (int i = 0; i < 3; i++) {
            toolSelector.executeToolChain(chain, reactContext)
                .await().atMost(Duration.ofSeconds(5));
        }

        // Then
        IntelligentToolSelector.ToolPerformanceMetrics metrics = toolSelector.getToolMetrics("metrics_tool");
        assertThat(metrics.executionCount()).isGreaterThan(0);
        assertThat(metrics.avgDurationMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should get all tool metrics")
    void shouldGetAllToolMetrics() {
        // When
        Map<String, IntelligentToolSelector.ToolPerformanceMetrics> allMetrics = toolSelector.getAllToolMetrics();

        // Then
        assertThat(allMetrics).isNotNull();
        // May be empty if no tools executed yet
    }

    @Test
    @DisplayName("Should clear selection cache")
    void shouldClearCache() {
        // When
        toolSelector.clearCache();

        // Then - should not throw exception
        // Cache is cleared successfully
    }

    @Test
    @DisplayName("Should handle empty tool chain")
    void shouldHandleEmptyChain() {
        // Given
        IntelligentToolSelector.ToolChain emptyChain = new IntelligentToolSelector.ToolChain();

        // When
        IntelligentToolSelector.ToolChainResult result = toolSelector.executeToolChain(
            emptyChain, reactContext
        ).await().atMost(Duration.ofSeconds(5));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasFailure()).isFalse();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should order tools by execution order")
    void shouldOrderTools() {
        // Given
        IntelligentToolSelector.ToolChain chain = new IntelligentToolSelector.ToolChain();
        chain.addTool(new IntelligentToolSelector.SelectedTool(
            "tool_3", 0.8, "Third", 3, Map.of(), true
        ));
        chain.addTool(new IntelligentToolSelector.SelectedTool(
            "tool_1", 0.9, "First", 1, Map.of(), true
        ));
        chain.addTool(new IntelligentToolSelector.SelectedTool(
            "tool_2", 0.7, "Second", 2, Map.of(), true
        ));

        // When
        List<IntelligentToolSelector.SelectedTool> tools = chain.getTools();

        // Then
        assertThat(tools).hasSize(3);
        assertThat(tools.get(0).order()).isEqualTo(1);
        assertThat(tools.get(1).order()).isEqualTo(2);
        assertThat(tools.get(2).order()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should get recommendations from chain")
    void shouldGetRecommendationsFromChain() {
        // Given
        IntelligentToolSelector.ToolChain chain = new IntelligentToolSelector.ToolChain();
        chain.addTool(new IntelligentToolSelector.SelectedTool(
            "test_tool", 0.85, "Test reason", 1, Map.of(), true
        ));

        // When
        List<IntelligentToolSelector.ToolRecommendation> recommendations = chain.getRecommendations();

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).toolId()).isEqualTo("test_tool");
        assertThat(recommendations.get(0).confidence()).isEqualTo(0.85);
        assertThat(recommendations.get(0).reason()).isEqualTo("Test reason");
    }

    @Test
    @DisplayName("Should calculate success rate in metrics")
    void shouldCalculateSuccessRate() {
        // Given
        IntelligentToolSelector.ToolPerformanceMetrics metrics = IntelligentToolSelector.ToolPerformanceMetrics
            .empty("test")
            .recordExecution(100, true)
            .recordExecution(100, true)
            .recordExecution(100, false)
            .recordExecution(100, true);

        // Then
        assertThat(metrics.successCount()).isEqualTo(3);
        assertThat(metrics.failureCount()).isEqualTo(1);
        assertThat(metrics.getSuccessRate()).isCloseTo(0.75, within(0.01));
    }

    @Test
    @DisplayName("Should handle tool with continue on error")
    void shouldHandleContinueOnError() {
        // Given
        IntelligentToolSelector.ToolChain chain = new IntelligentToolSelector.ToolChain();
        chain.addTool(new IntelligentToolSelector.SelectedTool(
            "tool_1", 0.9, "First", 1, Map.of(), true
        ));
        chain.addTool(new IntelligentToolSelector.SelectedTool(
            "tool_2", 0.8, "Second", 2, Map.of(), false // stop on error
        ));

        // When
        IntelligentToolSelector.ToolChainResult result = toolSelector.executeToolChain(
            chain, reactContext
        ).await().atMost(Duration.ofSeconds(10));

        // Then
        assertThat(result).isNotNull();
        // Both tools should attempt execution (since first succeeds)
    }

    @Test
    @DisplayName("Should create project context with builder")
    void shouldCreateContextWithBuilder() {
        // When
        IntelligentToolSelector.ProjectContext context = IntelligentToolSelector.ProjectContext.builder()
            .framework("react")
            .framework("nextjs")
            .language("typescript")
            .language("javascript")
            .feature("tailwind", true)
            .feature("eslint", true)
            .buildTool("npm")
            .testFramework("jest")
            .modelId("test-model")
            .build();

        // Then
        assertThat(context.getFrameworks()).contains("react", "nextjs");
        assertThat(context.getLanguages()).contains("typescript", "javascript");
        assertThat(context.hasTailwind()).isTrue();
        assertThat(context.hasBootstrap()).isFalse();
    }

    @Test
    @DisplayName("Should handle tool chain result statistics")
    void shouldHandleChainResultStats() {
        // Given
        List<IntelligentToolSelector.ToolExecutionResult> results = List.of(
            IntelligentToolSelector.ToolExecutionResult.success("tool1", 100, "Success 1"),
            IntelligentToolSelector.ToolExecutionResult.success("tool2", 150, "Success 2"),
            IntelligentToolSelector.ToolExecutionResult.failure("tool3", 50, "Error"),
            IntelligentToolSelector.ToolExecutionResult.success("tool4", 200, "Success 3")
        );
        IntelligentToolSelector.ToolChainResult result = new IntelligentToolSelector.ToolChainResult(results, true);

        // Then
        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.hasFailure()).isTrue();
    }

    @Test
    @DisplayName("Should handle cached tool selection")
    void shouldHandleCachedSelection() {
        // Given
        String task = "Create a test component";

        // When - First call (cache miss)
        IntelligentToolSelector.ToolChain chain1 = toolSelector.selectToolsByFramework(reactContext, task);

        // When - Second call (should use same logic, may be cached internally)
        IntelligentToolSelector.ToolChain chain2 = toolSelector.selectToolsByFramework(reactContext, task);

        // Then
        assertThat(chain1).isNotNull();
        assertThat(chain2).isNotNull();
        // Both should return valid chains
    }

    @Test
    @DisplayName("Should handle different CSS frameworks")
    void shouldHandleCssFrameworks() {
        // Given
        IntelligentToolSelector.ProjectContext bootstrapContext = IntelligentToolSelector.ProjectContext.builder()
            .framework("react")
            .feature("bootstrap", true)
            .build();

        IntelligentToolSelector.ProjectContext materialContext = IntelligentToolSelector.ProjectContext.builder()
            .framework("angular")
            .feature("material", true)
            .build();

        // Then
        assertThat(bootstrapContext.hasBootstrap()).isTrue();
        assertThat(bootstrapContext.hasTailwind()).isFalse();
        assertThat(materialContext.hasBootstrap()).isFalse();
    }

    // Helper method for assertion
    private org.assertj.core.api.DoubleAssert within(double value) {
        return org.assertj.core.api.Assertions.within(value);
    }
}

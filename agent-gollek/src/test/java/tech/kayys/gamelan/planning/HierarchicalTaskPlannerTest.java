package tech.kayys.gamelan.planning;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HierarchicalTaskPlannerTest {

    @Mock GollekSdk     sdk;
    @Mock GamelanConfig config;

    @InjectMocks HierarchicalTaskPlanner planner;

    @BeforeEach
    void setUp() throws SdkException {
        when(config.defaultModel()).thenReturn("test-model");
        when(config.maxTokens()).thenReturn(2048);
        when(config.temperature()).thenReturn(0.2);

        // Default: return a valid 2-subtask decomposition
        InferenceResponse resp = mock(InferenceResponse.class);
        when(resp.getContent()).thenReturn("""
                [
                  {"task": "Read and analyze the codebase", "type": "ATOMIC",
                   "tools": ["read_file"], "estimatedTokens": 500, "risk": "LOW"},
                  {"task": "Apply the fix", "type": "ATOMIC",
                   "tools": ["apply_patch"], "estimatedTokens": 300, "risk": "MEDIUM"}
                ]
                """);
        when(sdk.createCompletion(any(InferenceRequest.class))).thenReturn(resp);
    }

    @Test
    void planReturnsValidPlanForSimpleTask() {
        HierarchicalTaskPlanner.Plan plan = planner.plan(
                "Fix the bug in UserService",
                HierarchicalTaskPlanner.PlanningContext.defaults());

        assertThat(plan).isNotNull();
        assertThat(plan.goal()).isEqualTo("Fix the bug in UserService");
        assertThat(plan.id()).isNotBlank();
        assertThat(plan.createdAt()).isNotNull();
    }

    @Test
    void planContainsDecomposedTasks() throws SdkException {
        HierarchicalTaskPlanner.Plan plan = planner.plan(
                "Refactor UserService to use records",
                HierarchicalTaskPlanner.PlanningContext.defaults());

        assertThat(plan.tasks()).isNotEmpty();
    }

    @Test
    void estimatedTokensIsPositive() {
        HierarchicalTaskPlanner.Plan plan = planner.plan(
                "Fix null pointer", HierarchicalTaskPlanner.PlanningContext.defaults());
        assertThat(plan.estimatedTokens()).isGreaterThan(0);
    }

    @Test
    void planHasExecutionMode() {
        HierarchicalTaskPlanner.Plan plan = planner.plan(
                "simple task", HierarchicalTaskPlanner.PlanningContext.defaults());
        assertThat(plan.mode()).isNotNull();
    }

    @Test
    void planningContextDefaultsAreReasonable() {
        HierarchicalTaskPlanner.PlanningContext ctx =
                HierarchicalTaskPlanner.PlanningContext.defaults();
        assertThat(ctx.tokenBudget()).isGreaterThan(0);
        assertThat(ctx.projectContext()).isNotNull();
        assertThat(ctx.availableTools()).isNotNull();
    }

    @Test
    void recordOutcomeDoesNotThrow() {
        HierarchicalTaskPlanner.Plan plan = planner.plan(
                "task", HierarchicalTaskPlanner.PlanningContext.defaults());
        assertThatCode(() -> planner.recordOutcome(plan, true, 5000L))
                .doesNotThrowAnyException();
    }

    @Test
    void gracefullyHandlesLlmFailure() throws SdkException {
        when(sdk.createCompletion(any())).thenThrow(new SdkException("Network error"));

        // Should not throw — should fall back to leaf node
        HierarchicalTaskPlanner.Plan plan = planner.plan(
                "fix the bug", HierarchicalTaskPlanner.PlanningContext.defaults());
        assertThat(plan).isNotNull();
    }

    @Test
    void gracefullyHandlesMalformedLlmResponse() throws SdkException {
        InferenceResponse bad = mock(InferenceResponse.class);
        when(bad.getContent()).thenReturn("not valid JSON at all!!!");
        when(sdk.createCompletion(any())).thenReturn(bad);

        HierarchicalTaskPlanner.Plan plan = planner.plan(
                "analyze this", HierarchicalTaskPlanner.PlanningContext.defaults());
        assertThat(plan).isNotNull();
    }

    @Test
    void taskNodeRecordHasCorrectDefaults() {
        HierarchicalTaskPlanner.TaskNode leaf =
                HierarchicalTaskPlanner.TaskNode.leaf("do something", 0);
        assertThat(leaf.task()).isEqualTo("do something");
        assertThat(leaf.type()).isEqualTo(HierarchicalTaskPlanner.TaskNode.TaskType.ATOMIC);
        assertThat(leaf.depth()).isEqualTo(0);
        assertThat(leaf.risk()).isEqualTo(HierarchicalTaskPlanner.TaskNode.RiskLevel.LOW);
        assertThat(leaf.id()).isNotBlank();
    }

    @Test
    void sequentialPlanHasCorrectMode() {
        HierarchicalTaskPlanner.Plan plan = HierarchicalTaskPlanner.Plan.sequential(
                "goal", java.util.List.of(
                        HierarchicalTaskPlanner.TaskNode.leaf("t1", 0)));
        assertThat(plan.mode()).isEqualTo(HierarchicalTaskPlanner.ExecutionMode.SEQUENTIAL);
    }

    @Test
    void parallelPlanHasCorrectMode() {
        HierarchicalTaskPlanner.Plan plan = HierarchicalTaskPlanner.Plan.parallelized(
                "goal", java.util.List.of(
                        HierarchicalTaskPlanner.TaskNode.leaf("t1", 0)));
        assertThat(plan.mode()).isEqualTo(HierarchicalTaskPlanner.ExecutionMode.PARALLEL);
    }

    @Test
    void minimalPlanKeepsOnlyAtomicTasks() {
        var tasks = java.util.List.of(
                HierarchicalTaskPlanner.TaskNode.leaf("atomic-task", 0),
                new HierarchicalTaskPlanner.TaskNode(
                        java.util.UUID.randomUUID().toString(), "composite-task",
                        HierarchicalTaskPlanner.TaskNode.TaskType.COMPOSITE,
                        0, java.util.List.of(), java.util.List.of(), 1000,
                        HierarchicalTaskPlanner.TaskNode.RiskLevel.LOW, java.util.List.of()));

        HierarchicalTaskPlanner.Plan plan = HierarchicalTaskPlanner.Plan.minimal("goal", tasks);
        assertThat(plan.tasks()).allMatch(
                t -> t.type() == HierarchicalTaskPlanner.TaskNode.TaskType.ATOMIC);
    }
}

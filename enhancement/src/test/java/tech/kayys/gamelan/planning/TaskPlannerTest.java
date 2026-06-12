package tech.kayys.gamelan.planning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.DirectCallOrchestrator;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.config.GamelanConfig;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskPlannerTest {

    @Mock DirectCallOrchestrator llm;
    @Mock GamelanConfig          config;
    @InjectMocks TaskPlanner     planner;

    private OrchestratorResult resp(String text) {
        return OrchestratorResult.ok(text, "direct", 1, List.of(), Duration.ZERO);
    }

    // ── shouldPlan heuristics ──────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "what is Java?,                     false",
        "fix the NPE,                       false",
        "refactor all services and migrate, true",
        "comprehensive audit of the entire codebase, true",
        "update every endpoint and add tests and documentation, true"
    })
    void shouldPlanHeuristic(String task, boolean expected) {
        assertThat(planner.shouldPlan(task)).isEqualTo(expected);
    }

    @Test
    void shouldNotPlanForNullOrEmpty() {
        assertThat(planner.shouldPlan(null)).isFalse();
        assertThat(planner.shouldPlan("")).isFalse();
    }

    // ── Plan parsing ───────────────────────────────────────────────────────

    @Test
    void parsesWellFormedLlmResponse() {
        String llmResponse = """
                GOAL: Refactor UserService for better testability
                STRATEGY: Extract interface, add constructor injection
                ESTIMATED_COST: MEDIUM
                STEPS:
                1. [TOOL: read_file] [HUMAN: no] Read UserService.java
                2. [TOOL: apply_patch] [HUMAN: yes] Extract IUserService interface
                3. [TOOL: run_command] [HUMAN: no] Run mvn test to verify
                RISKS:
                - May break existing mocks
                """;

        when(llm.execute(any(AgentRequest.class))).thenReturn(resp(llmResponse));
        when(config.defaultModel()).thenReturn("test-model");

        TaskPlanner.Plan plan = planner.plan("refactor user service", "test-model");

        assertThat(plan.goal()).contains("UserService");
        assertThat(plan.estimatedCost()).isEqualTo(TaskPlanner.Plan.Cost.MEDIUM);
        assertThat(plan.steps()).hasSize(3);
        assertThat(plan.steps().get(0).tool()).isEqualTo("read_file");
        assertThat(plan.steps().get(1).requiresHumanApproval()).isTrue();
        assertThat(plan.steps().get(2).requiresHumanApproval()).isFalse();
        assertThat(plan.risks()).hasSize(1);
        assertThat(plan.risks().get(0)).contains("mocks");
    }

    @Test
    void fallsBackToTrivialPlanOnLlmError() {
        when(llm.execute(any())).thenThrow(new RuntimeException("model unavailable"));
        when(config.defaultModel()).thenReturn("test-model");

        TaskPlanner.Plan plan = planner.plan("some task", "test-model");

        assertThat(plan).isNotNull();
        assertThat(plan.steps()).hasSize(1);
        assertThat(plan.estimatedCost()).isEqualTo(TaskPlanner.Plan.Cost.LOW);
    }

    @Test
    void parsesLowAndHighCost() {
        when(llm.execute(any())).thenReturn(resp("GOAL: t\nSTRATEGY: s\nESTIMATED_COST: HIGH\nSTEPS:\n1. do it\nRISKS:"));
        when(config.defaultModel()).thenReturn("m");
        assertThat(planner.plan("task", "m").estimatedCost())
                .isEqualTo(TaskPlanner.Plan.Cost.HIGH);

        when(llm.execute(any())).thenReturn(resp("GOAL: t\nSTRATEGY: s\nESTIMATED_COST: LOW\nSTEPS:\n1. do it\nRISKS:"));
        assertThat(planner.plan("task", "m").estimatedCost())
                .isEqualTo(TaskPlanner.Plan.Cost.LOW);
    }

    // ── Plan types ─────────────────────────────────────────────────────────

    @Test
    void trivialPlanHasOneStep() {
        TaskPlanner.Plan plan = TaskPlanner.Plan.trivial("simple task");
        assertThat(plan.steps()).hasSize(1);
        assertThat(plan.hasHumanGates()).isFalse();
        assertThat(plan.estimatedCost()).isEqualTo(TaskPlanner.Plan.Cost.LOW);
    }

    @Test
    void hasHumanGatesDetectsApprovalSteps() {
        when(llm.execute(any())).thenReturn(resp(
                "GOAL: g\nSTRATEGY: s\nESTIMATED_COST: LOW\nSTEPS:\n"
                + "1. [HUMAN: yes] dangerous operation\nRISKS:"));
        when(config.defaultModel()).thenReturn("m");

        TaskPlanner.Plan plan = planner.plan("task", "m");
        assertThat(plan.hasHumanGates()).isTrue();
    }

    @Test
    void planSummaryContainsAllSections() {
        TaskPlanner.Plan plan = TaskPlanner.Plan.trivial("test task");
        String summary = plan.summary();
        assertThat(summary).contains("GOAL:");
        assertThat(summary).contains("STRATEGY:");
        assertThat(summary).contains("COST:");
    }
}

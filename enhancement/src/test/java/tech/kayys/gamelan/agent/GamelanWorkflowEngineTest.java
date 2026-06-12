package tech.kayys.gamelan.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GamelanWorkflowEngine}.
 * Uses Mockito to stub AgentLoop without needing a real LLM.
 */
@ExtendWith(MockitoExtension.class)
class GamelanWorkflowEngineTest {

    @Mock AgentLoop    agentLoop;
    @Mock GamelanConfig config;
    @InjectMocks GamelanWorkflowEngine engine;

    @BeforeEach
    void setUp() {
        when(config.requestTimeoutSeconds()).thenReturn(30);
        when(config.tokenBudget()).thenReturn(6000);
    }

    private AgentResponse successResp(String text) {
        return AgentResponse.builder().text(text).build();
    }

    private AgentResponse errorResp(String text) {
        return AgentResponse.builder().text("[LLM_ERROR] " + text).error(true).build();
    }

    @Test
    void sequentialWorkflowRunsStepsInOrder() {
        when(agentLoop.process(contains("step one"), any(), any(), anyBoolean()))
                .thenReturn(successResp("output of step one"));
        when(agentLoop.process(contains("step two"), any(), any(), anyBoolean()))
                .thenReturn(successResp("output of step two"));

        var workflow = GamelanWorkflowEngine.GamelanWorkflow.builder()
                .name("test")
                .sequential(
                    GamelanWorkflowEngine.WorkflowStep.of("step-one", "do step one"),
                    GamelanWorkflowEngine.WorkflowStep.of("step-two", "do step two")
                ).build();

        var result = engine.execute(workflow, new ConversationSession(null), "model", null);

        assertThat(result.success()).isTrue();
        assertThat(result.stepResults()).hasSize(2);
        assertThat(result.stepResults().get(0).stepName()).isEqualTo("step-one");
        assertThat(result.stepResults().get(1).stepName()).isEqualTo("step-two");
        assertThat(result.stepOutput("step-one")).isEqualTo("output of step one");
    }

    @Test
    void sequentialStopOnRequiredFailure() {
        when(agentLoop.process(anyString(), any(), any(), anyBoolean()))
                .thenReturn(errorResp("something broke"));

        var workflow = GamelanWorkflowEngine.GamelanWorkflow.builder()
                .name("stop-test")
                .sequential(
                    GamelanWorkflowEngine.WorkflowStep.required("required-step", "must succeed"),
                    GamelanWorkflowEngine.WorkflowStep.of("optional-step", "this should be skipped")
                ).build();

        var result = engine.execute(workflow, new ConversationSession(null), "model", null);

        assertThat(result.success()).isFalse();
        // Only one step should have run (second was skipped after required failure)
        assertThat(result.stepResults()).hasSize(1);
        // Optional step never ran
        verify(agentLoop, times(1)).process(anyString(), any(), any(), anyBoolean());
    }

    @Test
    void parallelWorkflowRunsAllSteps() {
        when(agentLoop.process(anyString(), any(), any(), anyBoolean()))
                .thenAnswer(inv -> successResp("result for: " + inv.getArgument(0, String.class).substring(0, 20)));

        var workflow = GamelanWorkflowEngine.GamelanWorkflow.builder()
                .name("parallel-test")
                .parallel(
                    GamelanWorkflowEngine.WorkflowStep.of("alpha", "alpha task"),
                    GamelanWorkflowEngine.WorkflowStep.of("beta",  "beta task"),
                    GamelanWorkflowEngine.WorkflowStep.of("gamma", "gamma task")
                ).build();

        var result = engine.execute(workflow, new ConversationSession(null), "model", null);

        assertThat(result.stepResults()).hasSize(3);
        assertThat(result.stepResults()).extracting(GamelanWorkflowEngine.StepResult::stepName)
                .containsExactlyInAnyOrder("alpha", "beta", "gamma");
    }

    @Test
    void mapReduceFiltersFailedStepsFromSynthesis() {
        // Two steps succeed, one fails
        when(agentLoop.process(contains("good-a"), any(), any(), anyBoolean()))
                .thenReturn(successResp("output A"));
        when(agentLoop.process(contains("bad-b"), any(), any(), anyBoolean()))
                .thenReturn(errorResp("failed B"));
        when(agentLoop.process(contains("good-c"), any(), any(), anyBoolean()))
                .thenReturn(successResp("output C"));
        // Synthesis prompt should only contain A and C
        when(agentLoop.process(contains("output A"), any(), any(), anyBoolean()))
                .thenReturn(successResp("synthesized result"));

        var workflow = GamelanWorkflowEngine.GamelanWorkflow.builder()
                .name("mr-test")
                .mapReduce(
                    GamelanWorkflowEngine.WorkflowStep.of("good-a", "do good-a"),
                    GamelanWorkflowEngine.WorkflowStep.of("bad-b",  "do bad-b"),
                    GamelanWorkflowEngine.WorkflowStep.of("good-c", "do good-c")
                ).build();

        var result = engine.execute(workflow, new ConversationSession(null), "model", null);

        // Should have 4 results: 3 map + 1 synthesis
        assertThat(result.stepResults()).hasSize(4);
        assertThat(result.stepOutput("synthesis")).isEqualTo("synthesized result");

        // Verify synthesis prompt did NOT contain "failed B" output
        verify(agentLoop).process(argThat(s -> s.contains("output A") && !s.contains("failed B")),
                any(), any(), anyBoolean());
    }

    @Test
    void workflowCallbackIsInvokedForEachStep() {
        when(agentLoop.process(anyString(), any(), any(), anyBoolean()))
                .thenReturn(successResp("ok"));

        var workflow = GamelanWorkflowEngine.GamelanWorkflow.builder()
                .name("callback-test")
                .sequential(
                    GamelanWorkflowEngine.WorkflowStep.of("a", "task a"),
                    GamelanWorkflowEngine.WorkflowStep.of("b", "task b")
                ).build();

        List<String> callbackNames = new ArrayList<>();
        engine.execute(workflow, new ConversationSession(null), "model",
                step -> callbackNames.add(step.stepName()));

        assertThat(callbackNames).containsExactly("a", "b");
    }

    @Test
    void workflowResultHasElapsedTime() {
        when(agentLoop.process(anyString(), any(), any(), anyBoolean()))
                .thenReturn(successResp("done"));

        var workflow = GamelanWorkflowEngine.GamelanWorkflow.builder()
                .name("timing-test")
                .sequential(GamelanWorkflowEngine.WorkflowStep.of("s", "task"))
                .build();

        var result = engine.execute(workflow, new ConversationSession(null), "model", null);
        assertThat(result.elapsed()).isNotNull();
        assertThat(result.elapsed().toMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void builderRejectsEmptyWorkflow() {
        assertThatThrownBy(() ->
            GamelanWorkflowEngine.GamelanWorkflow.builder().name("empty").build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("no phases");
    }

    @Test
    void stepResultSafeOutputHandlesNull() {
        var step = new GamelanWorkflowEngine.StepResult(
                "test", null, true, List.of(), java.time.Duration.ZERO);
        assertThat(step.safeOutput()).isEqualTo("");
    }
}

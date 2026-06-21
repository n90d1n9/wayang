package tech.kayys.gamelan.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.orchestration.ToolMetrics;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.AgentMemory;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gamelan.skill.SkillRegistry;
import tech.kayys.gamelan.skill.SkillSelector;
import tech.kayys.gamelan.tool.ToolExecutor;
import tech.kayys.gamelan.tool.ToolResult;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.agent.orchestration.SingleAgentOrchestrator;
import tech.kayys.gamelan.session.ConversationSession;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the ReAct loop guard, error propagation, cancellation,
 * and observability hooks in {@link SingleAgentOrchestrator}.
 *
 * <p>Migrated from the legacy {@code AgentLoop}-based tests.
 * {@link AgentLoop} is now a deprecated adapter; all new tests
 * target {@link SingleAgentOrchestrator} directly.
 */
@ExtendWith(MockitoExtension.class)
class AgentLoopLoopGuardTest {

    @Mock GollekSdk      sdk;
    @Mock SkillRegistry  skillRegistry;
    @Mock SkillSelector  skillSelector;
    @Mock ToolExecutor   toolExecutor;
    @Mock PromptBuilder  promptBuilder;
    @Mock ToolCallParser toolCallParser;
    @Mock GamelanConfig  config;
    @Mock AgentMemory    memory;
    @Mock TokenTracker   tokenTracker;
    @Mock ToolMetrics    toolMetrics;   // required by SingleAgentOrchestrator

    @InjectMocks SingleAgentOrchestrator orchestrator;

    @BeforeEach
    void setUp() throws SdkException {
        when(skillSelector.select(any(), any())).thenReturn(List.of());
        when(skillRegistry.listAll()).thenReturn(List.of());
        when(promptBuilder.buildSystemPrompt(any())).thenReturn("system");
        when(config.temperature()).thenReturn(0.7);
        when(config.maxTokens()).thenReturn(2048);
        when(config.requestTimeoutSeconds()).thenReturn(30);
        when(config.defaultModel()).thenReturn("test-model");
        when(memory.extractAndStore(any())).thenReturn(0);
        when(toolCallParser.parse(any())).thenReturn(List.of());

        InferenceResponse mockResp = mock(InferenceResponse.class);
        when(mockResp.getContent()).thenReturn("Final answer");
        when(sdk.createCompletion(any(InferenceRequest.class))).thenReturn(mockResp);
    }

    private AgentRequest req(String task) {
        return AgentRequest.builder(task).session(new ConversationSession(null)).build();
    }

    @Test
    void loopGuardBreaksOnRepeatedToolCalls() throws SdkException {
        ToolCall repeatedCall = new ToolCall("read_file",
                Map.of("path", "same.txt"), "<tc/>");

        InferenceResponse withTool = mock(InferenceResponse.class);
        when(withTool.getContent()).thenReturn("<tool_call/>");
        when(sdk.createCompletion(any())).thenReturn(withTool);
        when(toolCallParser.parse(any())).thenReturn(List.of(repeatedCall));
        when(toolExecutor.execute(any())).thenReturn(ToolResult.success("read_file", "c"));

        OrchestratorResult result = orchestrator.execute(req("do something"));

        // Loop guard threshold is 3 — should stop before 10 iterations
        verify(sdk, atMost(5)).createCompletion(any());
        assertThat(result.answer()).containsIgnoringCase("Stopped");
    }

    @Test
    void llmErrorPropagatesToResult() throws SdkException {
        when(sdk.createCompletion(any())).thenThrow(new SdkException("Connection refused"));

        OrchestratorResult result = orchestrator.execute(req("hello"));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Connection refused");
    }

    @Test
    void cancellationStopsLoop() throws SdkException {
        InferenceResponse resp = mock(InferenceResponse.class);
        when(resp.getContent()).thenReturn("partial");
        when(sdk.createCompletion(any())).thenAnswer(inv -> {
            orchestrator.cancelCurrentThread(); // cancel mid-first-iteration
            return resp;
        });
        when(toolCallParser.parse("partial"))
                .thenReturn(List.of(new ToolCall("tool", Map.of(), "<tc/>")));

        OrchestratorResult result = orchestrator.execute(req("task"));

        // Should have stopped early due to cancellation
        verify(sdk, atMost(3)).createCompletion(any());
    }

    @Test
    void tokenTrackerIsCalledAfterTurn() throws SdkException {
        orchestrator.execute(req("hello"));
        verify(tokenTracker, times(1)).record(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void toolMetricsRecordedPerToolCall() throws SdkException {
        ToolCall call = new ToolCall("read_file", Map.of("path", "x"), "<tc/>");
        InferenceResponse withTool = mock(InferenceResponse.class);
        when(withTool.getContent()).thenReturn("<tool_call/>");
        InferenceResponse finalResp = mock(InferenceResponse.class);
        when(finalResp.getContent()).thenReturn("Done.");

        when(sdk.createCompletion(any())).thenReturn(withTool).thenReturn(finalResp);
        when(toolCallParser.parse(anyString()))
                .thenReturn(List.of(call))
                .thenReturn(List.of());
        when(toolExecutor.execute(any())).thenReturn(ToolResult.success("read_file", "c"));

        orchestrator.execute(req("read x"));

        verify(toolMetrics, times(1)).record(eq("read_file"), anyLong(), anyBoolean());
    }

    @Test
    void skillsAreSelectedFromUserMessage() throws SdkException {
        Skill mockSkill = mock(Skill.class);
        when(mockSkill.name()).thenReturn("test-skill");
        when(skillSelector.select(eq("my task"), any())).thenReturn(List.of(mockSkill));

        OrchestratorResult result = orchestrator.execute(req("my task"));

        assertThat(result.strategy()).isEqualTo("react");
    }

    @Test
    void eventListenerReceivesCallbacks() throws SdkException {
        List<String> received = new java.util.ArrayList<>();
        tech.kayys.gamelan.agent.orchestration.AgentEventListener listener =
                new tech.kayys.gamelan.agent.orchestration.AgentEventListener() {
            @Override public void onRunStart(String t, String m) { received.add("start"); }
            @Override public void onComplete(String a, int i)    { received.add("complete"); }
        };

        orchestrator.execute(req("task"), listener);

        assertThat(received).contains("start", "complete");
    }
}

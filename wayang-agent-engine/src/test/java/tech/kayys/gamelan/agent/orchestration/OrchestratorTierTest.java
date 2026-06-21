package tech.kayys.gamelan.agent.orchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.AgentMemory;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.skill.SkillRegistry;
import tech.kayys.gamelan.skill.SkillSelector;
import tech.kayys.gamelan.tool.ToolExecutor;
import tech.kayys.gamelan.tool.ToolResult;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for all three orchestration tiers and the OrchestratorSelector.
 *
 * NOTE: {@code @Mock ToolMetrics} is required because {@link SingleAgentOrchestrator}
 * declares {@code @Inject ToolMetrics toolMetrics}. Without this mock Mockito's
 * {@code @InjectMocks} cannot satisfy the injection and the test class will fail
 * with a NullPointerException on the first {@code toolMetrics.record()} call.
 */
@ExtendWith(MockitoExtension.class)
class OrchestratorTierTest {

    // ── Mocks for SingleAgentOrchestrator ──────────────────────────────────
    @Mock GollekSdk       sdk;
    @Mock PromptBuilder   promptBuilder;
    @Mock ToolCallParser  toolCallParser;
    @Mock ToolExecutor    toolExecutor;
    @Mock SkillSelector   skillSelector;
    @Mock SkillRegistry   skillRegistry;
    @Mock AgentMemory     memory;
    @Mock TokenTracker    tokenTracker;
    @Mock ToolMetrics     toolMetrics;     // REQUIRED — was missing, caused NPE
    @Mock GamelanConfig   config;

    @InjectMocks SingleAgentOrchestrator singleAgent;

    @BeforeEach
    void setUp() throws SdkException {
        when(skillSelector.select(any(), any())).thenReturn(List.of());
        when(skillRegistry.listAll()).thenReturn(List.of());
        when(promptBuilder.buildSystemPrompt(any())).thenReturn("system-prompt");
        when(config.temperature()).thenReturn(0.7);
        when(config.maxTokens()).thenReturn(2048);
        when(config.requestTimeoutSeconds()).thenReturn(30);
        when(config.defaultModel()).thenReturn("test-model");
        when(memory.extractAndStore(any())).thenReturn(0);
        when(toolCallParser.parse(any())).thenReturn(List.of()); // no tool calls by default

        InferenceResponse mockResp = mock(InferenceResponse.class);
        when(mockResp.getContent()).thenReturn("Final answer from agent");
        when(sdk.createCompletion(any(InferenceRequest.class))).thenReturn(mockResp);
    }

    // ── Tier 1: Direct call ────────────────────────────────────────────────

    @Test
    void directStrategyId() {
        assertThat(new DirectCallOrchestrator().strategyId()).isEqualTo("direct");
    }

    @Test
    void directDoesNotSupportTools() {
        assertThat(new DirectCallOrchestrator().supportsTools()).isFalse();
    }

    // ── Tier 2: Single agent ───────────────────────────────────────────────

    @Test
    void singleAgentReturnsAnswerWhenNoToolCalls() throws SdkException {
        AgentRequest request = AgentRequest.builder("explain this code")
                .session(new ConversationSession(null)).build();

        OrchestratorResult result = singleAgent.execute(request);

        assertThat(result.success()).isTrue();
        assertThat(result.answer()).contains("Final answer from agent");
        assertThat(result.strategy()).isEqualTo("react");
    }

    @Test
    void singleAgentCallsToolAndContinues() throws SdkException {
        ToolCall toolCall = new ToolCall("read_file", Map.of("path", "Main.java"), "<tc/>");

        // First LLM call returns a tool call; second returns the final answer
        InferenceResponse toolResp = mock(InferenceResponse.class);
        when(toolResp.getContent()).thenReturn("<tool_call><n>read_file</n><path>Main.java</path></tool_call>");
        InferenceResponse finalResp = mock(InferenceResponse.class);
        when(finalResp.getContent()).thenReturn("Here is my analysis.");

        when(sdk.createCompletion(any()))
                .thenReturn(toolResp)
                .thenReturn(finalResp);
        when(toolCallParser.parse(anyString()))
                .thenReturn(List.of(toolCall))
                .thenReturn(List.of());
        when(toolExecutor.execute(any()))
                .thenReturn(ToolResult.success("read_file", "file content"));

        AgentRequest req = AgentRequest.builder("read Main.java")
                .session(new ConversationSession(null)).build();
        OrchestratorResult result = singleAgent.execute(req);

        assertThat(result.toolResults()).hasSize(1);
        verify(toolExecutor, times(1)).execute(any());
        verify(toolMetrics, times(1)).record(anyString(), anyLong(), anyBoolean());
    }

    @Test
    void singleAgentRespectsToolAllowlist() throws SdkException {
        ToolCall blocked = new ToolCall("run_command", Map.of("command", "rm -rf"), "<tc/>");
        when(toolCallParser.parse(anyString()))
                .thenReturn(List.of(blocked))
                .thenReturn(List.of());

        // Only allow read_file — run_command should be blocked
        AgentRequest req = AgentRequest.builder("task")
                .allowedTools(List.of("read_file"))
                .session(new ConversationSession(null))
                .build();
        singleAgent.execute(req);

        // run_command must NOT have been executed
        verify(toolExecutor, never()).execute(any());
    }

    @Test
    void singleAgentLoopGuardStopsOnRepetition() throws SdkException {
        ToolCall repeating = new ToolCall("read_file", Map.of("path", "x.txt"), "<tc/>");
        when(toolCallParser.parse(any())).thenReturn(List.of(repeating));
        when(toolExecutor.execute(any())).thenReturn(ToolResult.success("read_file", "c"));

        AgentRequest req = AgentRequest.builder("task")
                .maxSteps(10).session(new ConversationSession(null)).build();
        OrchestratorResult result = singleAgent.execute(req);

        // Should stop before 10 iterations — loop guard kicks in at 3 repeats
        verify(sdk, atMost(5)).createCompletion(any());
        assertThat(result.answer()).contains("Stopped");
    }

    @Test
    void singleAgentLlmErrorPropagatesToResult() throws SdkException {
        when(sdk.createCompletion(any())).thenThrow(new SdkException("Connection refused"));

        AgentRequest req = AgentRequest.builder("task")
                .session(new ConversationSession(null)).build();
        OrchestratorResult result = singleAgent.execute(req);

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Connection refused");
    }

    @Test
    void singleAgentRecordsTokenUsage() throws SdkException {
        AgentRequest req = AgentRequest.builder("task")
                .session(new ConversationSession(null)).build();
        singleAgent.execute(req);
        verify(tokenTracker, times(1)).record(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void singleAgentCallsEventListener() throws SdkException {
        List<String> events = new ArrayList<>();
        AgentEventListener listener = new AgentEventListener() {
            @Override public void onRunStart(String t, String m) { events.add("run_start"); }
            @Override public void onIterationStart(int i, int max) { events.add("iter_start"); }
            @Override public void onIterationEnd(int i, String r) { events.add("iter_end"); }
            @Override public void onComplete(String a, int i) { events.add("complete"); }
        };

        AgentRequest req = AgentRequest.builder("task")
                .session(new ConversationSession(null)).build();
        singleAgent.execute(req, listener);

        assertThat(events).containsExactly("run_start", "iter_start", "iter_end", "complete");
    }

    // ── Tier 2 variant: Reflexion ──────────────────────────────────────────

    @Test
    void reflexionStrategyId() {
        assertThat(new ReflexionOrchestrator().strategyId()).isEqualTo("reflexion");
    }

    @Test
    void reflexionDoesNotSupportTools() {
        assertThat(new ReflexionOrchestrator().supportsTools()).isFalse();
    }

    // ── Tier 3: Multi-agent ────────────────────────────────────────────────

    @Test
    void multiAgentStrategyId() {
        assertThat(new MultiAgentOrchestrator().strategyId()).isEqualTo("multi-agent");
    }

    // ── Selector ───────────────────────────────────────────────────────────

    @Test
    void selectorAutoChoosesDirectForShortQuestion() {
        OrchestratorSelector sel = buildSelector();
        AgentOrchestrator chosen = sel.select(null, "what is Java?");
        assertThat(chosen.strategyId()).isEqualTo("direct");
    }

    @Test
    void selectorAutoChoosesReactForFileTask() {
        OrchestratorSelector sel = buildSelector();
        assertThat(sel.select(null, "read the file and fix the bug").strategyId())
                .isEqualTo("react");
    }

    @Test
    void selectorAutoChoosesMultiForCrossDomain() {
        OrchestratorSelector sel = buildSelector();
        assertThat(sel.select(null,
                "full review of src/ and security and performance").strategyId())
                .isEqualTo("multi-agent");
    }

    @Test
    void selectorHonoursExplicitStrategy() {
        OrchestratorSelector sel = buildSelector();
        assertThat(sel.select("direct",    "anything").strategyId()).isEqualTo("direct");
        assertThat(sel.select("react",     "anything").strategyId()).isEqualTo("react");
        assertThat(sel.select("reflexion", "anything").strategyId()).isEqualTo("reflexion");
        assertThat(sel.select("multi",     "anything").strategyId()).isEqualTo("multi-agent");
        assertThat(sel.select("1",         "anything").strategyId()).isEqualTo("direct");
        assertThat(sel.select("3",         "anything").strategyId()).isEqualTo("multi-agent");
    }

    @Test
    void selectorFallsBackToReactForUnknownStrategy() {
        OrchestratorSelector sel = buildSelector();
        assertThat(sel.select("unknown-strategy", "task").strategyId()).isEqualTo("react");
    }

    @Test
    void selectorAllReturnsAllFour() {
        OrchestratorSelector sel = buildSelector();
        assertThat(sel.all()).hasSize(4);
    }

    // ── AG-UI events ───────────────────────────────────────────────────────

    @Test
    void aguiEventSseFrameFormat() {
        String frame = tech.kayys.gamelan.agent.agui.AguiEvent
                .runStarted("run-123").toSseFrame();
        assertThat(frame).startsWith("data: {");
        assertThat(frame).contains("RUN_STARTED");
        assertThat(frame).contains("run-123");
        assertThat(frame).endsWith("\n\n");
    }

    @Test
    void aguiTextDeltaCarriesDelta() {
        var e = tech.kayys.gamelan.agent.agui.AguiEvent.textDelta("r", "m", "hello");
        assertThat(e.delta()).isEqualTo("hello");
        assertThat(e.type()).isEqualTo("TEXT_MESSAGE_CONTENT");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private OrchestratorSelector buildSelector() {
        OrchestratorSelector sel = new OrchestratorSelector();
        set(sel, "direct",     new DirectCallOrchestrator());
        set(sel, "react",      singleAgent);
        set(sel, "reflexion",  new ReflexionOrchestrator());
        set(sel, "multiAgent", new MultiAgentOrchestrator());
        return sel;
    }

    private void set(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set " + field, e);
        }
    }
}

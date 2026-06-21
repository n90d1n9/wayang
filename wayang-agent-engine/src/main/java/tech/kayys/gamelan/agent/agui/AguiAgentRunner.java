package tech.kayys.gamelan.agent.agui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * AG-UI protocol-compliant agent runner.
 *
 * <p>Emits typed {@link AguiEvent}s during agent execution, conforming to
 * <a href="https://docs.ag-ui.com/concepts/events">AG-UI event spec</a>.
 * Events are delivered to a {@link Consumer&lt;AguiEvent&gt;} which can:
 * <ul>
 *   <li>Write to an HTTP SSE response stream</li>
 *   <li>Push over a WebSocket connection</li>
 *   <li>Render to the terminal (see {@link AguiTerminalRenderer})</li>
 *   <li>Drive a frontend UI framework</li>
 * </ul>
 *
 * <h2>Concurrency model</h2>
 * This bean is {@code @ApplicationScoped} (singleton). All shared state has
 * been eliminated — each call to {@link #run} is fully self-contained. No
 * {@code AtomicBoolean}, no instance-level mutable state.
 *
 * <h2>Strategy routing</h2>
 * Routes through {@link OrchestratorSelector} so the caller's explicit
 * strategy choice (direct / react / reflexion / multi) is respected.
 * For {@link SingleAgentOrchestrator}, events are emitted granularly via
 * an {@link AgentEventListener} bridge — one {@code TEXT_MESSAGE_CONTENT}
 * event per streaming token, one {@code TOOL_CALL_START/END} per tool.
 * For other strategies (direct, reflexion, multi) a coarser event sequence
 * is emitted since those tiers do not support streaming.
 *
 * <h2>Integration example — Quarkus SSE endpoint</h2>
 * <pre>{@code
 * @GET @Path("/agent/run")
 * @Produces(MediaType.SERVER_SENT_EVENTS)
 * public Multi<String> stream(@QueryParam("task") String task) {
 *     AgentRequest req = AgentRequest.builder(task).stream(true).build();
 *     return Multi.createFrom().emitter(em ->
 *             Thread.ofVirtual().start(() ->
 *                     runner.run(req, null, e -> em.emit(e.toSseFrame()))));
 * }
 * }</pre>
 *
 * <h2>AG-UI event sequence (ReAct / Tier 2)</h2>
 * <pre>
 * RUN_STARTED
 *   STEP_STARTED(iter=0)
 *     TEXT_MESSAGE_START
 *     TEXT_MESSAGE_CONTENT × N    ← one per streaming token
 *     TEXT_MESSAGE_END
 *     [TOOL_CALL_START / TOOL_CALL_END] × M
 *   STEP_FINISHED(iter=0)
 *   ... repeat per iteration ...
 * STATE_SNAPSHOT
 * RUN_FINISHED
 * </pre>
 */
@ApplicationScoped
public class AguiAgentRunner {

    private static final Logger log = LoggerFactory.getLogger(AguiAgentRunner.class);

    @Inject OrchestratorSelector    selector;
    @Inject SingleAgentOrchestrator singleAgent;
    @Inject GamelanConfig           config;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Execute the agent and emit AG-UI events to the consumer.
     *
     * <p>Blocks until complete. Call from a virtual thread to avoid blocking
     * the Quarkus event loop.
     *
     * @param request  the agent task and configuration
     * @param strategy strategy name override, or {@code null} for auto-select
     * @param emitter  receives AG-UI events as they occur; must be thread-safe
     * @return the final answer text
     */
    public String run(AgentRequest request, String strategy, Consumer<AguiEvent> emitter) {
        String runId = UUID.randomUUID().toString();
        emitter.accept(AguiEvent.runStarted(runId));

        try {
            AgentOrchestrator orch = selector.select(strategy, request.task());
            String answer;

            if (orch instanceof SingleAgentOrchestrator single) {
                // Fine-grained events: one per token and per tool via AgentEventListener
                AgentEventListener bridge = buildBridge(runId, emitter);
                OrchestratorResult result = single.execute(request, bridge);
                answer = result.answer();
            } else {
                // Coarse events for non-streaming tiers (direct, reflexion, multi)
                String msgId = UUID.randomUUID().toString();
                emitter.accept(AguiEvent.stepStarted(runId, 0));
                emitter.accept(AguiEvent.textStart(runId, msgId));

                OrchestratorResult result = orch.execute(request);
                answer = result.answer();

                // Emit the complete answer as a single content event
                if (!answer.isBlank()) {
                    emitter.accept(AguiEvent.textDelta(runId, msgId, answer));
                }
                emitter.accept(AguiEvent.textEnd(runId, msgId));
                emitter.accept(AguiEvent.stepFinished(runId, 0));
            }

            emitter.accept(AguiEvent.stateSnapshot(runId,
                    Map.of("task",      request.task(),
                           "answer",    answer,
                           "strategy",  strategy != null ? strategy : "auto",
                           "timestamp", System.currentTimeMillis())));
            emitter.accept(AguiEvent.runFinished(runId, true, null));
            return answer;

        } catch (Exception e) {
            log.error("[agui] run {} failed: {}", runId, e.getMessage(), e);
            emitter.accept(AguiEvent.error(runId, e.getMessage()));
            emitter.accept(AguiEvent.runFinished(runId, false, e.getMessage()));
            return "";
        }
    }

    // ── Event bridge ───────────────────────────────────────────────────────

    /**
     * Translates {@link AgentEventListener} callbacks into AG-UI events.
     * Each instance is created per {@link #run} call — no shared state.
     */
    private AgentEventListener buildBridge(String runId, Consumer<AguiEvent> emitter) {
        return new AgentEventListener() {
            private String currentMsgId = null;

            @Override
            public void onIterationStart(int iter, int max) {
                currentMsgId = UUID.randomUUID().toString();
                emitter.accept(AguiEvent.stepStarted(runId, iter));
                emitter.accept(AguiEvent.textStart(runId, currentMsgId));
            }

            @Override
            public void onTextChunk(String chunk) {
                if (currentMsgId != null && chunk != null && !chunk.isEmpty()) {
                    emitter.accept(AguiEvent.textDelta(runId, currentMsgId, chunk));
                }
            }

            @Override
            public void onIterationEnd(int iter, String stopReason) {
                if (currentMsgId != null) {
                    emitter.accept(AguiEvent.textEnd(runId, currentMsgId));
                    currentMsgId = null;
                }
                emitter.accept(AguiEvent.stepFinished(runId, iter));
            }

            @Override
            public void onToolStart(String toolName, String inputSummary) {
                // Use a placeholder callId; the actual mapping is tracked per-call
                emitter.accept(AguiEvent.toolCallStart(runId,
                        UUID.randomUUID().toString(), toolName));
            }

            @Override
            public void onToolEnd(String toolName, String resultSummary,
                                   boolean error, long durationMs) {
                emitter.accept(AguiEvent.toolCallEnd(runId,
                        UUID.randomUUID().toString(), toolName,
                        Map.of("error", error,
                               "ms",    durationMs,
                               "preview", truncate(resultSummary, 200))));
            }

            @Override
            public void onError(String errorMessage, int iteration) {
                emitter.accept(AguiEvent.error(runId, errorMessage));
            }
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}

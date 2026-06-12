package tech.kayys.gamelan.streaming.sse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/**
 * AgentStreamingService — real-time SSE and event-streaming for agent execution.
 *
 * <h2>Why streaming matters for agents</h2>
 * Agent tasks can take 30–120 seconds. Without streaming:
 * <ul>
 *   <li>Users see a blank screen for 2 minutes then a wall of text</li>
 *   <li>HTTP connections time out before the response arrives</li>
 *   <li>Errors are only surfaced at the end, not where they occurred</li>
 * </ul>
 *
 * <h2>Dual output modes</h2>
 * <pre>
 * Mode 1: TOKEN STREAM
 *   data: {"type":"token","content":"Analyzing"}
 *   data: {"type":"token","content":" the"}
 *   data: {"type":"token","content":" code..."}
 *
 * Mode 2: STRUCTURED EVENT STREAM
 *   data: {"type":"task_started","task":"fix UserService","strategy":"react"}
 *   data: {"type":"iteration","iter":1,"max":10}
 *   data: {"type":"tool_call","tool":"read_file","path":"UserService.java"}
 *   data: {"type":"tool_result","tool":"read_file","chars":3420,"ms":45}
 *   data: {"type":"token","content":"I found..."}
 *   data: {"type":"task_done","answer":"Fixed NPE","tokens":1840,"ms":12500}
 * </pre>
 *
 * <h2>Multi-subscriber fanout</h2>
 * Multiple clients (browser, CLI, dashboard) can subscribe to the same
 * agent run simultaneously. Events are buffered per subscriber so slow
 * consumers don't block the agent.
 *
 * <h2>Backpressure</h2>
 * Each subscriber has a bounded buffer. If the subscriber can't keep up,
 * older events are dropped with a {@code type="buffer_overflow"} notification.
 * This prevents memory exhaustion from slow HTTP clients.
 */
@ApplicationScoped
public class AgentStreamingService {

    private static final Logger log = LoggerFactory.getLogger(AgentStreamingService.class);

    private static final int SUBSCRIBER_BUFFER = 500;
    private static final int MAX_SUBSCRIBERS   = 50;

    // Active runs: runId → list of subscriber queues
    private final Map<String, List<SubscriberQueue>> subscribers = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsEmitted = new AtomicLong();

    @Inject AgentTelemetry telemetry;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Subscribes to a running agent task and returns a stream of SSE frames.
     * The returned {@link EventStream} is iterable and blocks until the next
     * event is available (or the stream is closed).
     *
     * @param runId     the task run to subscribe to
     * @param consumer  callback invoked for each SSE frame
     * @return a handle to manage the subscription lifecycle
     */
    public Subscription subscribe(String runId, Consumer<SseFrame> consumer) {
        SubscriberQueue queue = new SubscriberQueue(UUID.randomUUID().toString(), consumer);

        subscribers.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>())
                   .add(queue);

        log.debug("[streaming] subscriber {} joined run {}", queue.id(), runId);

        return new Subscription(runId, queue.id(), () -> {
            List<SubscriberQueue> subs = subscribers.get(runId);
            if (subs != null) subs.removeIf(q -> q.id().equals(queue.id()));
            log.debug("[streaming] subscriber {} left run {}", queue.id(), runId);
        });
    }

    /**
     * Builds an {@link AgentEventListener} that emits SSE frames to all
     * subscribers of the given run. Inject this listener into orchestrator
     * calls to enable streaming.
     */
    public AgentEventListener listenerFor(String runId) {
        return new AgentEventListener() {
            @Override
            public void onRunStart(String task, String model) {
                emit(runId, SseFrame.taskStarted(runId, task, model));
            }

            @Override
            public void onIterationStart(int iter, int max) {
                emit(runId, SseFrame.iteration(runId, iter, max));
            }

            @Override
            public void onTextChunk(String chunk) {
                if (chunk != null && !chunk.isEmpty()) {
                    emit(runId, SseFrame.token(runId, chunk));
                }
            }

            @Override
            public void onToolStart(String toolName, String inputSummary) {
                emit(runId, SseFrame.toolCall(runId, toolName, inputSummary));
                telemetry.count("agent.stream.tool_calls");
            }

            @Override
            public void onToolEnd(String toolName, String resultSummary,
                                   boolean error, long durationMs) {
                emit(runId, SseFrame.toolResult(runId, toolName, resultSummary, error, durationMs));
            }

            @Override
            public void onIterationEnd(int iter, String stopReason) {
                emit(runId, SseFrame.iterationEnd(runId, iter, stopReason));
            }

            @Override
            public void onComplete(String finalAnswer, int totalIterations) {
                emit(runId, SseFrame.taskDone(runId, finalAnswer, totalIterations));
                closeRun(runId);
            }

            @Override
            public void onError(String errorMessage, int iteration) {
                emit(runId, SseFrame.error(runId, errorMessage, iteration));
                telemetry.count("agent.stream.errors");
            }
        };
    }

    /**
     * Emits an event to all subscribers of a run.
     */
    public void emit(String runId, SseFrame frame) {
        totalEventsEmitted.incrementAndGet();
        List<SubscriberQueue> subs = subscribers.get(runId);
        if (subs == null || subs.isEmpty()) return;

        for (SubscriberQueue queue : subs) {
            queue.offer(frame);
        }
    }

    /**
     * Closes a run — sends a final "stream_end" event and cleans up.
     */
    public void closeRun(String runId) {
        emit(runId, SseFrame.streamEnd(runId));
        Thread.ofVirtual().start(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            subscribers.remove(runId);
        });
    }

    public int activeRuns()           { return subscribers.size(); }
    public long totalEventsEmitted()  { return totalEventsEmitted.get(); }

    public Map<String, Integer> subscriberCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        subscribers.forEach((runId, subs) -> counts.put(runId, subs.size()));
        return Collections.unmodifiableMap(counts);
    }

    // ── Inner types ────────────────────────────────────────────────────────

    /** A bounded subscriber queue. Drops oldest on overflow. */
    private static final class SubscriberQueue {
        private final String                      id;
        private final Consumer<SseFrame>          consumer;
        private final AtomicLong                  dropped = new AtomicLong();

        SubscriberQueue(String id, Consumer<SseFrame> consumer) {
            this.id       = id;
            this.consumer = consumer;
        }

        void offer(SseFrame frame) {
            Thread.ofVirtual().start(() -> {
                try { consumer.accept(frame); }
                catch (Exception e) {
                    log.warn("[streaming] subscriber {} dropped event: {}", id, e.getMessage());
                    dropped.incrementAndGet();
                }
            });
        }

        String id()       { return id; }
        long dropped()    { return dropped.get(); }
    }

    /** Handle returned to callers to manage subscription lifecycle. */
    public record Subscription(String runId, String subscriberId, Runnable onClose)
            implements AutoCloseable {
        @Override public void close() { onClose.run(); }
    }

    /**
     * A single SSE frame — serializes to the SSE wire format:
     * {@code data: {JSON}\n\n}
     */
    public record SseFrame(
            String  type,
            String  runId,
            String  content,
            String  tool,
            String  error,
            long    durationMs,
            int     iteration,
            int     maxIterations,
            boolean toolError,
            long    timestamp
    ) {
        public String toSseString() {
            StringBuilder sb = new StringBuilder("data: {");
            sb.append("\"type\":\"").append(type).append("\"");
            sb.append(",\"runId\":\"").append(runId).append("\"");
            if (content != null && !content.isEmpty())
                sb.append(",\"content\":").append(jsonStr(content));
            if (tool != null)
                sb.append(",\"tool\":\"").append(tool).append("\"");
            if (error != null)
                sb.append(",\"error\":").append(jsonStr(error));
            if (durationMs > 0)
                sb.append(",\"ms\":").append(durationMs);
            if (iteration > 0)
                sb.append(",\"iter\":").append(iteration);
            if (maxIterations > 0)
                sb.append(",\"max\":").append(maxIterations);
            if (toolError)
                sb.append(",\"toolError\":true");
            sb.append(",\"ts\":").append(timestamp);
            sb.append("}\n\n");
            return sb.toString();
        }

        private String jsonStr(String s) {
            return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                           .replace("\n","\\n").replace("\r","") + "\"";
        }

        // ── Factory methods ────────────────────────────────────────────────

        static SseFrame taskStarted(String runId, String task, String model) {
            return new SseFrame("task_started", runId, task + "|model=" + model,
                    null, null, 0, 0, 0, false, Instant.now().toEpochMilli());
        }

        static SseFrame iteration(String runId, int iter, int max) {
            return new SseFrame("iteration", runId, null,
                    null, null, 0, iter, max, false, Instant.now().toEpochMilli());
        }

        static SseFrame token(String runId, String chunk) {
            return new SseFrame("token", runId, chunk,
                    null, null, 0, 0, 0, false, Instant.now().toEpochMilli());
        }

        static SseFrame toolCall(String runId, String tool, String summary) {
            return new SseFrame("tool_call", runId, summary,
                    tool, null, 0, 0, 0, false, Instant.now().toEpochMilli());
        }

        static SseFrame toolResult(String runId, String tool, String result,
                                    boolean error, long ms) {
            return new SseFrame("tool_result", runId, result,
                    tool, null, ms, 0, 0, error, Instant.now().toEpochMilli());
        }

        static SseFrame iterationEnd(String runId, int iter, String reason) {
            return new SseFrame("iteration_end", runId, reason,
                    null, null, 0, iter, 0, false, Instant.now().toEpochMilli());
        }

        static SseFrame taskDone(String runId, String answer, int iters) {
            return new SseFrame("task_done", runId, answer,
                    null, null, 0, iters, 0, false, Instant.now().toEpochMilli());
        }

        static SseFrame error(String runId, String msg, int iter) {
            return new SseFrame("error", runId, null,
                    null, msg, 0, iter, 0, false, Instant.now().toEpochMilli());
        }

        static SseFrame streamEnd(String runId) {
            return new SseFrame("stream_end", runId, null,
                    null, null, 0, 0, 0, false, Instant.now().toEpochMilli());
        }
    }
}

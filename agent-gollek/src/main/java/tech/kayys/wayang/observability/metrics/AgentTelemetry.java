package tech.kayys.gamelan.observability.metrics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * AgentTelemetry — unified observability for every layer of the agentic stack.
 *
 * <h2>Why agent-specific telemetry</h2>
 * Standard APM tools (Prometheus, Datadog) are designed for request/response
 * services. Agents have fundamentally different characteristics:
 * <ul>
 *   <li>Variable-length "requests" (1 iteration → 30 iterations on same task)</li>
 *   <li>Tool call chains that branch and merge non-linearly</li>
 *   <li>LLM token cost as a first-class metric (not CPU/memory)</li>
 *   <li>Quality metrics that are not binary (correctness, completeness, coherence)</li>
 *   <li>Memory layer performance (episodic hit rate, semantic recall precision)</li>
 * </ul>
 *
 * <h2>Metric taxonomy</h2>
 * <pre>
 * agent.task.*          — per-task outcomes (duration, tokens, steps, quality)
 * agent.tool.*          — per-tool performance (latency, error rate, rate)
 * agent.memory.*        — memory layer health (hit rates, retention, sizes)
 * agent.planning.*      — planning quality (GoT scores, plan accuracy)
 * agent.evolution.*     — AVO progress (quality delta, generation count)
 * agent.cost.*          — economic metrics (token spend, budget utilization)
 * agent.hyperagent.*    — multi-agent metrics (convergence rounds, conflict rate)
 * </pre>
 *
 * <h2>Span tracing</h2>
 * Every agentic operation can be wrapped in a {@link Span} — a timed, named,
 * attributed unit of work. Spans nest to form traces:
 * <pre>
 * Span("task:add-tests")
 *   Span("planning:got") → 8 nodes, 5 simulations, score=0.84
 *   Span("execution:dag") → 4 nodes, 3 parallel
 *     Span("tool:read_file") → 45ms, 1.2KB
 *     Span("tool:write_file") → 12ms
 *   Span("memory:consolidate") → +3 AKUs, 1 procedure
 * </pre>
 *
 * <h2>Export</h2>
 * Metrics are exported to:
 * <ul>
 *   <li>JSONL file at {@code ~/.gamelan/telemetry/metrics.jsonl} (always)</li>
 *   <li>OpenTelemetry OTLP endpoint (when configured)</li>
 *   <li>Prometheus text format via {@link #prometheusText()} (on demand)</li>
 * </ul>
 */
@ApplicationScoped
public class AgentTelemetry {

    private static final Logger log = LoggerFactory.getLogger(AgentTelemetry.class);

    // Core metric stores — all thread-safe
    private final Map<String, AtomicLong>           counters   = new ConcurrentHashMap<>();
    private final Map<String, LatencyHistogram>      histograms = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<Double>> gauges   = new ConcurrentHashMap<>();
    private final Deque<SpanRecord>                  spans      = new ArrayDeque<>();
    private final Deque<SpanRecord>                  recentSpans= new ArrayDeque<>();

    private final Object spansLock = new Object();

    // Session start
    private final Instant sessionStart = Instant.now();

    @Inject GamelanConfig config;

    // ── Counter API ────────────────────────────────────────────────────────

    /** Increments a counter by 1. Creates it if it doesn't exist. */
    public void count(String name) { count(name, 1); }

    /** Increments a counter by the given delta. */
    public void count(String name, long delta) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(delta);
    }

    /** Returns the current value of a counter. */
    public long getCount(String name) {
        AtomicLong c = counters.get(name);
        return c == null ? 0L : c.get();
    }

    // ── Histogram API (latency distribution) ───────────────────────────────

    /**
     * Records a latency observation (in milliseconds).
     * Maintains min/max/sum/count and percentile buckets.
     */
    public void recordLatency(String name, long ms) {
        histograms.computeIfAbsent(name, k -> new LatencyHistogram()).record(ms);
    }

    public LatencyHistogram histogram(String name) {
        return histograms.getOrDefault(name, new LatencyHistogram());
    }

    // ── Gauge API ──────────────────────────────────────────────────────────

    /** Sets a gauge to an absolute value. */
    public void gauge(String name, double value) {
        gauges.computeIfAbsent(name, k -> new AtomicReference<>(0.0)).set(value);
    }

    public double getGauge(String name) {
        AtomicReference<Double> g = gauges.get(name);
        return g == null ? 0.0 : g.get();
    }

    // ── Span / Trace API ───────────────────────────────────────────────────

    /**
     * Creates and starts a new span. Use with try-with-resources:
     * <pre>
     * try (var span = telemetry.span("planning:got")) {
     *     span.attr("simulations", 30).attr("task", task);
     *     // ... work ...
     *     span.success();
     * }
     * </pre>
     */
    public Span span(String name) {
        return new Span(name, this);
    }

    /**
     * Times a supplier and records the latency under the given metric name.
     */
    public <T> T timed(String metricName, Supplier<T> work) {
        long start = System.currentTimeMillis();
        try {
            T result = work.get();
            recordLatency(metricName, System.currentTimeMillis() - start);
            count(metricName + ".success");
            return result;
        } catch (Exception e) {
            recordLatency(metricName, System.currentTimeMillis() - start);
            count(metricName + ".error");
            throw e;
        }
    }

    void recordSpan(SpanRecord record) {
        synchronized (spansLock) {
            spans.addLast(record);
            recentSpans.addLast(record);
            while (recentSpans.size() > 500) recentSpans.pollFirst(); // keep last 500
        }
        recordLatency(record.name(), record.durationMs());
        if (!record.success()) count(record.name() + ".error");
        count(record.name() + ".calls");

        // Async persist to JSONL
        Thread.ofVirtual().start(() -> appendToJournal(record));
    }

    // ── Semantic Agent Metrics ─────────────────────────────────────────────

    /** Records a completed agent task. */
    public void recordTask(String strategy, boolean success, int steps,
                            int tokens, long durationMs, double qualityScore) {
        count("agent.task.total");
        count(success ? "agent.task.success" : "agent.task.failure");
        recordLatency("agent.task.duration", durationMs);
        count("agent.task.steps.total", steps);
        count("agent.task.tokens.total", tokens);
        gauge("agent.task.quality.last", qualityScore);
        recordLatency("agent.task.tokens", tokens);  // token "latency" = distribution
        count("agent.task.strategy." + strategy);
    }

    /** Records a tool execution. */
    public void recordTool(String toolName, long durationMs, boolean error, int outputBytes) {
        count("agent.tool." + toolName + ".calls");
        if (error) count("agent.tool." + toolName + ".errors");
        recordLatency("agent.tool." + toolName + ".latency", durationMs);
        count("agent.tool.output.bytes.total", outputBytes);
    }

    /** Records memory layer access. */
    public void recordMemoryAccess(String layer, boolean hit, int itemsReturned) {
        count("agent.memory." + layer + ".accesses");
        count(hit ? "agent.memory." + layer + ".hits" : "agent.memory." + layer + ".misses");
        count("agent.memory." + layer + ".items.returned", itemsReturned);
    }

    /** Records GoT planning metrics. */
    public void recordPlanningGoT(int nodes, int edges, double bestScore, long durationMs) {
        count("agent.planning.got.total");
        recordLatency("agent.planning.got.duration", durationMs);
        gauge("agent.planning.got.nodes.last", nodes);
        gauge("agent.planning.got.score.last", bestScore);
    }

    /** Records AVO evolution event. */
    public void recordEvolution(String skillName, boolean improved,
                                 double qualityDelta, int generations) {
        count("agent.evolution.total");
        count(improved ? "agent.evolution.improved" : "agent.evolution.unchanged");
        gauge("agent.evolution.quality.delta.last", qualityDelta);
        count("agent.evolution.generations.total", generations);
    }

    /** Records hyperagent session. */
    public void recordHyperagent(int domains, int conflicts, boolean converged, long durationMs) {
        count("agent.hyperagent.total");
        count("agent.hyperagent.domains.total", domains);
        count("agent.hyperagent.conflicts.total", conflicts);
        count(converged ? "agent.hyperagent.converged" : "agent.hyperagent.timeout");
        recordLatency("agent.hyperagent.duration", durationMs);
    }

    // ── Reporting ──────────────────────────────────────────────────────────

    /**
     * Exports current metrics in Prometheus text format.
     * Compatible with any Prometheus scraper.
     */
    public String prometheusText() {
        StringBuilder sb = new StringBuilder();
        Instant now = Instant.now();
        long epochMs = now.toEpochMilli();

        // Counters
        counters.forEach((name, value) -> {
            String metric = prometheusName(name);
            sb.append("# TYPE ").append(metric).append(" counter\n");
            sb.append(metric).append(" ").append(value.get()).append(" ").append(epochMs).append("\n");
        });

        // Gauges
        gauges.forEach((name, ref) -> {
            String metric = prometheusName(name);
            sb.append("# TYPE ").append(metric).append(" gauge\n");
            sb.append(metric).append(" ").append(ref.get()).append(" ").append(epochMs).append("\n");
        });

        // Histograms (p50, p95, p99)
        histograms.forEach((name, hist) -> {
            String metric = prometheusName(name);
            sb.append("# TYPE ").append(metric).append(" summary\n");
            sb.append(metric).append("{quantile=\"0.5\"} ").append(hist.p50()).append("\n");
            sb.append(metric).append("{quantile=\"0.95\"} ").append(hist.p95()).append("\n");
            sb.append(metric).append("{quantile=\"0.99\"} ").append(hist.p99()).append("\n");
            sb.append(metric).append("_count ").append(hist.count()).append("\n");
            sb.append(metric).append("_sum ").append(hist.sum()).append("\n");
        });

        return sb.toString();
    }

    /**
     * Returns a human-readable dashboard text suitable for terminal display.
     */
    public String dashboard() {
        Duration sessionDuration = Duration.between(sessionStart, Instant.now());
        StringBuilder sb = new StringBuilder();

        sb.append("╔══ Agent Telemetry Dashboard ══════════════════════════════════╗\n");
        sb.append(String.format("║  Session: %s (%ds elapsed)%n",
                sessionStart.toString().substring(0,19),
                sessionDuration.getSeconds()));

        // Task metrics
        long tasks = getCount("agent.task.total");
        long success = getCount("agent.task.success");
        if (tasks > 0) {
            sb.append(String.format("║  Tasks: %d total | %.0f%% success | %.1fk tokens%n",
                    tasks, tasks > 0 ? 100.0*success/tasks : 0,
                    getCount("agent.task.tokens.total")/1000.0));
        }

        // Tool metrics — top 5 by call count
        sb.append("║  Top tools:\n");
        counters.entrySet().stream()
                .filter(e -> e.getKey().startsWith("agent.tool.") && e.getKey().endsWith(".calls"))
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue(
                        Comparator.comparingLong(AtomicLong::get)).reversed())
                .limit(5)
                .forEach(e -> {
                    String tool = e.getKey().replace("agent.tool.","").replace(".calls","");
                    LatencyHistogram h = histogram("agent.tool." + tool + ".latency");
                    sb.append(String.format("║    %-20s %5d calls  p95=%dms%n",
                            tool, e.getValue().get(), h.p95()));
                });

        // Memory metrics
        long memAccesses = counters.entrySet().stream()
                .filter(e -> e.getKey().contains("agent.memory") && e.getKey().endsWith(".accesses"))
                .mapToLong(e -> e.getValue().get()).sum();
        if (memAccesses > 0) {
            sb.append(String.format("║  Memory: %d accesses%n", memAccesses));
        }

        // Recent spans
        sb.append("║  Recent spans (last 5):\n");
        synchronized (spansLock) {
            new ArrayDeque<>(recentSpans).descendingIterator().forEachRemaining(span -> {
                if (sb.toString().lines().count() < 20) {
                    sb.append(String.format("║    %s %-30s %dms%n",
                            span.success() ? "✓" : "✗",
                            span.name().length() > 30 ? span.name().substring(0,30) : span.name(),
                            span.durationMs()));
                }
            });
        }

        sb.append("╚═══════════════════════════════════════════════════════════════╝");
        return sb.toString();
    }

    /**
     * Resets all metrics — useful between test runs.
     */
    public void reset() {
        counters.clear();
        histograms.clear();
        gauges.clear();
        synchronized (spansLock) { spans.clear(); recentSpans.clear(); }
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String prometheusName(String name) {
        return "gamelan_" + name.replace(".", "_").replace("-", "_");
    }

    private void appendToJournal(SpanRecord record) {
        Path dir = Path.of(System.getProperty("user.home"), ".gamelan", "telemetry");
        try {
            Files.createDirectories(dir);
            String line = String.format(
                    "{\"name\":\"%s\",\"durationMs\":%d,\"success\":%b,\"ts\":\"%s\",\"attrs\":%s}%n",
                    record.name(), record.durationMs(), record.success(),
                    record.startedAt(), record.attributes());
            Files.writeString(dir.resolve("spans.jsonl"), line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    // ── Inner types ────────────────────────────────────────────────────────

    /**
     * A timed, attributed span. Use with try-with-resources.
     */
    public static final class Span implements AutoCloseable {
        private final String              name;
        private final AgentTelemetry      telemetry;
        private final Instant             startedAt;
        private final Map<String, Object> attributes = new LinkedHashMap<>();
        private boolean                   success    = true;
        private String                    error;

        Span(String name, AgentTelemetry telemetry) {
            this.name      = name;
            this.telemetry = telemetry;
            this.startedAt = Instant.now();
        }

        public Span attr(String key, Object value) {
            attributes.put(key, value);
            return this;
        }

        public Span success() { this.success = true; return this; }

        public Span error(String msg) {
            this.success = false;
            this.error   = msg;
            return this;
        }

        @Override
        public void close() {
            long ms = Duration.between(startedAt, Instant.now()).toMillis();
            telemetry.recordSpan(new SpanRecord(name, ms, success, error, startedAt, attributes));
        }
    }

    public record SpanRecord(
            String               name,
            long                 durationMs,
            boolean              success,
            String               error,
            Instant              startedAt,
            Map<String, Object>  attributes
    ) {}

    /**
     * Lock-free latency histogram with exponential buckets.
     * Tracks: count, sum, min, max, and percentile estimates.
     */
    public static final class LatencyHistogram {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong sum   = new AtomicLong();
        private final AtomicLong min   = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong max   = new AtomicLong(0);

        // Exponential buckets: 1ms, 2ms, 5ms, 10ms, 25ms, 50ms, 100ms, 250ms, 500ms, 1s, 5s, ∞
        private static final long[] BOUNDS = {1,2,5,10,25,50,100,250,500,1000,5000};
        private final AtomicLong[] buckets = new AtomicLong[BOUNDS.length + 1];

        public LatencyHistogram() {
            for (int i = 0; i <= BOUNDS.length; i++) buckets[i] = new AtomicLong();
        }

        public void record(long ms) {
            count.incrementAndGet();
            sum.addAndGet(ms);
            min.updateAndGet(cur -> Math.min(cur, ms));
            max.updateAndGet(cur -> Math.max(cur, ms));

            for (int i = 0; i < BOUNDS.length; i++) {
                if (ms <= BOUNDS[i]) { buckets[i].incrementAndGet(); return; }
            }
            buckets[BOUNDS.length].incrementAndGet(); // overflow
        }

        public long count() { return count.get(); }
        public long sum()   { return sum.get(); }
        public long min()   { return count.get() == 0 ? 0 : min.get(); }
        public long max()   { return max.get(); }
        public double avg() { long c = count.get(); return c == 0 ? 0 : (double)sum.get()/c; }

        public long p50()  { return percentile(0.50); }
        public long p95()  { return percentile(0.95); }
        public long p99()  { return percentile(0.99); }

        private long percentile(double p) {
            long total = count.get();
            if (total == 0) return 0;
            long target = (long) Math.ceil(p * total);
            long cum = 0;
            for (int i = 0; i < BOUNDS.length; i++) {
                cum += buckets[i].get();
                if (cum >= target) return BOUNDS[i];
            }
            return BOUNDS[BOUNDS.length - 1];
        }

        @Override public String toString() {
            return String.format("Histogram[count=%d, avg=%.1fms, p50=%d, p95=%d, p99=%d]",
                    count(), avg(), p50(), p95(), p99());
        }
    }
}

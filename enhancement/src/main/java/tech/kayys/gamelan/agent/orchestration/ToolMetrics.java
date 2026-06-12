package tech.kayys.gamelan.agent.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Per-tool runtime metrics collected across an agent session.
 *
 * <p>Adapted from the Wayang {@code ToolMetrics} in the uploaded orchestration
 * sources, adjusted to fit the Gamelan CDI context.
 *
 * <h2>Tracked per tool</h2>
 * <ul>
 *   <li>Call count, error count, error rate %</li>
 *   <li>Total / min / max / avg latency (ms)</li>
 *   <li>Slow-call detection (> 5 s default threshold)</li>
 *   <li>Ring buffer of last 20 call timestamps + durations</li>
 * </ul>
 *
 * <h2>Session metrics</h2>
 * <ul>
 *   <li>Wall-clock session time</li>
 *   <li>Most-used and slowest tools</li>
 *   <li>JSON export for persistence / dashboards</li>
 * </ul>
 */
@ApplicationScoped
public class ToolMetrics {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final int  HISTORY_RING  = 20;
    private static final long SLOW_MS       = 5_000L;

    // ── Per-tool stat tracker ──────────────────────────────────────────────

    public static class ToolStat {
        public final  String     name;
        public final  AtomicLong calls   = new AtomicLong();
        public final  AtomicLong errors  = new AtomicLong();
        public final  AtomicLong totalMs = new AtomicLong();
        public final  AtomicLong minMs   = new AtomicLong(Long.MAX_VALUE);
        public final  AtomicLong maxMs   = new AtomicLong(0);

        // Ring buffer: [epochMs, durationMs, errorFlag]
        private final Deque<long[]> history = new ArrayDeque<>();

        ToolStat(String name) { this.name = name; }

        public synchronized void record(long durationMs, boolean error) {
            calls.incrementAndGet();
            totalMs.addAndGet(durationMs);
            if (error) errors.incrementAndGet();
            minMs.updateAndGet(cur -> Math.min(cur, durationMs));
            maxMs.updateAndGet(cur -> Math.max(cur, durationMs));
            history.addLast(new long[]{ System.currentTimeMillis(), durationMs, error ? 1 : 0 });
            while (history.size() > HISTORY_RING) history.pollFirst();
        }

        public double avgMs() {
            long c = calls.get();
            return c == 0 ? 0.0 : (double) totalMs.get() / c;
        }

        public double errorRate() {
            long c = calls.get();
            return c == 0 ? 0.0 : errors.get() * 100.0 / c;
        }

        public synchronized List<long[]> recentHistory() {
            return List.copyOf(history);
        }
    }

    // ── Session state ──────────────────────────────────────────────────────

    private final ConcurrentMap<String, ToolStat> stats   = new ConcurrentHashMap<>();
    private final long                            startMs = System.currentTimeMillis();
    private volatile long                         endMs   = -1;

    // ── Record ─────────────────────────────────────────────────────────────

    /**
     * Records a completed tool call.
     *
     * @param toolName the tool that was called
     * @param startMs  wall-clock time when the call started (from {@code System.currentTimeMillis()})
     * @param error    true if the tool returned a failure result
     */
    public void record(String toolName, long startMs, boolean error) {
        long elapsed = System.currentTimeMillis() - startMs;
        stats.computeIfAbsent(toolName, ToolStat::new).record(elapsed, error);
        if (elapsed >= SLOW_MS) {
            System.err.printf("  ⏱ SLOW TOOL: %s took %.1fs%n", toolName, elapsed / 1000.0);
        }
    }

    public void finish() { endMs = System.currentTimeMillis(); }

    public void reset() { stats.clear(); endMs = -1; }

    // ── Query ──────────────────────────────────────────────────────────────

    public ToolStat      stat(String name)  { return stats.get(name); }
    public Set<String>   toolNames()        { return Collections.unmodifiableSet(stats.keySet()); }
    public long          sessionMs()        { return (endMs > 0 ? endMs : System.currentTimeMillis()) - startMs; }
    public long          totalCalls()       { return stats.values().stream().mapToLong(s -> s.calls.get()).sum(); }
    public long          totalErrors()      { return stats.values().stream().mapToLong(s -> s.errors.get()).sum(); }

    public Optional<String> mostUsed() {
        return stats.entrySet().stream()
                .max(Comparator.comparingLong(e -> e.getValue().calls.get()))
                .map(Map.Entry::getKey);
    }

    public Optional<String> slowest() {
        return stats.entrySet().stream()
                .filter(e -> e.getValue().calls.get() > 0)
                .max(Comparator.comparingDouble(e -> e.getValue().avgMs()))
                .map(Map.Entry::getKey);
    }

    public List<String> slowTools() {
        return stats.entrySet().stream()
                .filter(e -> e.getValue().maxMs.get() >= SLOW_MS)
                .map(Map.Entry::getKey).sorted().toList();
    }

    // ── Text summary ───────────────────────────────────────────────────────

    public String summary() {
        if (stats.isEmpty()) return "(no tool calls recorded)";

        List<Map.Entry<String, ToolStat>> sorted = stats.entrySet().stream()
                .sorted(Comparator.comparingLong(
                        (Map.Entry<String, ToolStat> e) -> -e.getValue().calls.get()))
                .toList();

        int w0 = sorted.stream().mapToInt(e -> e.getKey().length()).max().orElse(10) + 2;
        String fmt = "  %-" + w0 + "s %6s %6s %8s %8s %8s%n";

        StringBuilder sb = new StringBuilder("Tool Metrics:\n\n");
        sb.append(String.format(fmt, "Tool", "calls", "errors", "avg ms", "max ms", "total s"));
        sb.append("  ").append("─".repeat(w0 + 42)).append("\n");

        for (var e : sorted) {
            ToolStat s = e.getValue();
            String errCell = s.errors.get() > 0 ? "⚠ " + s.errors.get() : "0";
            sb.append(String.format(fmt, e.getKey(), s.calls.get(), errCell,
                    String.format("%.0f", s.avgMs()), s.maxMs.get(),
                    String.format("%.1f", s.totalMs.get() / 1000.0)));
        }

        sb.append("  ").append("─".repeat(w0 + 42)).append("\n");
        sb.append(String.format("  %-" + w0 + "s %6d %6d%n", "TOTAL", totalCalls(), totalErrors()));
        sb.append(String.format("%n  Session: %.1fs%n", sessionMs() / 1000.0));
        mostUsed().ifPresent(t -> sb.append("  Most used: ").append(t).append("\n"));
        slowest().ifPresent(t -> sb.append(String.format("  Slowest:   %s (avg %.0fms)%n", t, stats.get(t).avgMs())));
        if (!slowTools().isEmpty())
            sb.append("  Slow (>5s): ").append(String.join(", ", slowTools())).append("\n");

        return sb.toString();
    }

    // ── JSON export ────────────────────────────────────────────────────────

    public String toJson() {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("session_duration_ms", sessionMs());
            root.put("total_calls",  totalCalls());
            root.put("total_errors", totalErrors());

            ObjectNode toolsNode = MAPPER.createObjectNode();
            for (var e : stats.entrySet()) {
                ToolStat s = e.getValue();
                ObjectNode tn = MAPPER.createObjectNode();
                tn.put("calls",      s.calls.get());
                tn.put("errors",     s.errors.get());
                tn.put("avg_ms",     String.format("%.1f", s.avgMs()));
                tn.put("min_ms",     s.minMs.get() == Long.MAX_VALUE ? 0 : s.minMs.get());
                tn.put("max_ms",     s.maxMs.get());
                tn.put("total_ms",   s.totalMs.get());
                ArrayNode hist = MAPPER.createArrayNode();
                s.recentHistory().forEach(h -> {
                    ObjectNode entry = MAPPER.createObjectNode();
                    entry.put("ts_ms", h[0]);
                    entry.put("dur_ms", h[1]);
                    entry.put("error", h[2] == 1);
                    hist.add(entry);
                });
                tn.set("recent", hist);
                toolsNode.set(e.getKey(), tn);
            }
            root.set("tools", toolsNode);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }

    public void saveToFile(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, toJson(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}

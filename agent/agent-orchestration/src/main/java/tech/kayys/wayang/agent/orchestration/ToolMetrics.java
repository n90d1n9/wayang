package tech.kayys.wayang.agent.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Per-tool runtime statistics collected across an agent session.
 *
 * Tracks per tool:
 *   - Call count, error count
 *   - Total / min / max / avg latency
 *   - Last N call timestamps (ring buffer, N=20)
 *   - Slow-call detection (configurable threshold)
 *
 * Session-level:
 *   - Total wall-clock time
 *   - Most-used and slowest tools
 *   - JSON export for persistence / reporting
 */
public class ToolMetrics {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final int    HISTORY_SIZE    = 20;
    private static final long   SLOW_THRESHOLD_MS = 5_000;  // 5 s

    // ── Per-tool stats ────────────────────────────────────────

    public static class ToolStat {
        public final String        name;
        public final AtomicLong    calls     = new AtomicLong();
        public final AtomicLong    errors    = new AtomicLong();
        public final AtomicLong    totalMs   = new AtomicLong();
        public final AtomicLong    minMs     = new AtomicLong(Long.MAX_VALUE);
        public final AtomicLong    maxMs     = new AtomicLong(0);

        // Ring buffer of recent call timestamps + durations
        private final Deque<long[]> history = new ArrayDeque<>(); // [epochMs, durationMs, errorFlag]

        ToolStat(String name) { this.name = name; }

        public synchronized void record(long durationMs, boolean error) {
            calls.incrementAndGet();
            totalMs.addAndGet(durationMs);
            if (error) errors.incrementAndGet();
            minMs.updateAndGet(cur -> Math.min(cur, durationMs));
            maxMs.updateAndGet(cur -> Math.max(cur, durationMs));

            // Ring buffer: keep last HISTORY_SIZE entries
            history.addLast(new long[]{System.currentTimeMillis(), durationMs, error ? 1 : 0});
            while (history.size() > HISTORY_SIZE) history.pollFirst();
        }

        public double avgMs() {
            long c = calls.get();
            return c == 0 ? 0.0 : (double) totalMs.get() / c;
        }

        public double errorRate() {
            long c = calls.get();
            return c == 0 ? 0.0 : (double) errors.get() / c * 100.0;
        }

        public synchronized List<long[]> recentHistory() {
            return List.copyOf(history);
        }
    }

    // ── Session state ─────────────────────────────────────────

    private final ConcurrentMap<String, ToolStat> stats    = new ConcurrentHashMap<>();
    private final long                             startMs  = System.currentTimeMillis();
    private volatile long                          endMs    = -1;

    // ── Record ────────────────────────────────────────────────

    public long startTimer() { return System.currentTimeMillis(); }

    public void record(String toolName, long startMs, boolean error) {
        long elapsed = System.currentTimeMillis() - startMs;
        stats.computeIfAbsent(toolName, ToolStat::new).record(elapsed, error);

        if (elapsed >= SLOW_THRESHOLD_MS) {
            System.out.printf("  ⏱ SLOW TOOL: %s took %.1fs%n", toolName, elapsed / 1000.0);
        }
    }

    public void finish() { endMs = System.currentTimeMillis(); }

    // ── Query ─────────────────────────────────────────────────

    public ToolStat         get(String name)   { return stats.get(name); }
    public Set<String>      toolNames()        { return Collections.unmodifiableSet(stats.keySet()); }
    public void             reset()            { stats.clear(); endMs = -1; }
    public long             sessionMs()        { return (endMs > 0 ? endMs : System.currentTimeMillis()) - startMs; }

    public long totalCalls()  { return stats.values().stream().mapToLong(s -> s.calls.get()).sum(); }
    public long totalErrors() { return stats.values().stream().mapToLong(s -> s.errors.get()).sum(); }

    /** Tool with most calls. */
    public Optional<String> mostUsed() {
        return stats.entrySet().stream()
                .max(Comparator.comparingLong(e -> e.getValue().calls.get()))
                .map(Map.Entry::getKey);
    }

    /** Tool with highest average latency. */
    public Optional<String> slowest() {
        return stats.entrySet().stream()
                .filter(e -> e.getValue().calls.get() > 0)
                .max(Comparator.comparingDouble(e -> e.getValue().avgMs()))
                .map(Map.Entry::getKey);
    }

    /** All tools that exceeded the slow threshold at least once. */
    public List<String> slowTools() {
        return stats.entrySet().stream()
                .filter(e -> e.getValue().maxMs.get() >= SLOW_THRESHOLD_MS)
                .map(Map.Entry::getKey)
                .sorted().toList();
    }

    // ── Text summary ──────────────────────────────────────────

    public String summary() {
        if (stats.isEmpty()) return "(no tool calls recorded)";

        List<Map.Entry<String, ToolStat>> sorted = stats.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, ToolStat> e) -> -e.getValue().calls.get()))
                .toList();

        int w0 = sorted.stream().mapToInt(e -> e.getKey().length()).max().orElse(10) + 2;
        String fmt = "  %-" + w0 + "s %6s %6s %8s %8s %8s%n";

        StringBuilder sb = new StringBuilder("Tool Metrics:\n\n");
        sb.append(String.format(fmt, "Tool", "calls", "errors", "avg ms", "max ms", "total s"));
        sb.append("  " + "─".repeat(w0 + 42) + "\n");

        for (var e : sorted) {
            ToolStat s = e.getValue();
            String errCell = s.errors.get() > 0 ? "⚠ " + s.errors.get() : "0";
            sb.append(String.format(fmt,
                    e.getKey(),
                    s.calls.get(),
                    errCell,
                    String.format("%.0f", s.avgMs()),
                    s.maxMs.get(),
                    String.format("%.1f", s.totalMs.get() / 1000.0)));
        }

        sb.append("  " + "─".repeat(w0 + 42) + "\n");
        sb.append(String.format("  %-" + w0 + "s %6d %6d%n", "TOTAL", totalCalls(), totalErrors()));
        sb.append(String.format("%n  Session wall time: %.1fs%n", sessionMs() / 1000.0));

        mostUsed().ifPresent(t -> sb.append("  Most used: ").append(t).append("\n"));
        slowest().ifPresent(t -> sb.append(String.format("  Slowest:   %s (avg %.0fms)%n",
                t, stats.get(t).avgMs())));
        if (!slowTools().isEmpty())
            sb.append("  Slow tools (>5s): ").append(String.join(", ", slowTools())).append("\n");

        return sb.toString();
    }

    // ── JSON export ───────────────────────────────────────────

    public String toJson() throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("session_start_ms", startMs);
        root.put("session_end_ms",   endMs > 0 ? endMs : System.currentTimeMillis());
        root.put("session_duration_ms", sessionMs());
        root.put("total_calls",  totalCalls());
        root.put("total_errors", totalErrors());
        root.put("generated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        ObjectNode toolsNode = MAPPER.createObjectNode();
        for (var e : stats.entrySet()) {
            ToolStat s = e.getValue();
            ObjectNode tn = MAPPER.createObjectNode();
            tn.put("calls",      s.calls.get());
            tn.put("errors",     s.errors.get());
            tn.put("error_rate", String.format("%.1f%%", s.errorRate()));
            tn.put("avg_ms",     String.format("%.1f", s.avgMs()));
            tn.put("min_ms",     s.minMs.get() == Long.MAX_VALUE ? 0 : s.minMs.get());
            tn.put("max_ms",     s.maxMs.get());
            tn.put("total_ms",   s.totalMs.get());

            ArrayNode hist = MAPPER.createArrayNode();
            s.recentHistory().forEach(h -> {
                ObjectNode entry = MAPPER.createObjectNode();
                entry.put("timestamp_ms", h[0]);
                entry.put("duration_ms",  h[1]);
                entry.put("error",        h[2] == 1);
                hist.add(entry);
            });
            tn.set("recent_calls", hist);
            toolsNode.set(e.getKey(), tn);
        }
        root.set("tools", toolsNode);
        return MAPPER.writeValueAsString(root);
    }

    /** Save metrics JSON to a file. */
    public void saveToFile(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, toJson(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}

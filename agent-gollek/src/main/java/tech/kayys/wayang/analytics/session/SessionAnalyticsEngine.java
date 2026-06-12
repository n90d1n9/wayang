package tech.kayys.gamelan.analytics.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * SessionAnalyticsEngine — cross-session pattern detection and usage insights.
 *
 * <h2>What analytics tells you that metrics don't</h2>
 * Metrics answer: "how many calls?" Analytics answers: "why?"
 * <ul>
 *   <li><b>Task complexity distribution</b>: are most tasks simple (1-2 iterations)
 *       or complex (8+ iterations)? Guides capacity planning.</li>
 *   <li><b>Bottleneck identification</b>: which tools consistently take the longest?
 *       Which steps repeat most often? Where does the agent get stuck in loops?</li>
 *   <li><b>User journey mapping</b>: what sequence of tasks do users actually perform?
 *       Do they debug → fix → test in order, or jump around?</li>
 *   <li><b>Failure clustering</b>: are failures isolated or do they cluster around
 *       specific time periods, file types, or task categories?</li>
 *   <li><b>Personalization signals</b>: which tool sequences lead to user satisfaction?
 *       Which task categories have the highest retry rate?</li>
 * </ul>
 *
 * <h2>Computed analytics</h2>
 * <pre>
 * Task complexity:  SIMPLE (1-2 iter) | MODERATE (3-5) | COMPLEX (6-10) | VERY_COMPLEX (10+)
 * Tool heat map:    per-tool (calls, avg latency, error rate, typical output size)
 * Journey flows:    common task sequences (A→B→C) with frequency counts
 * Peak usage:       hour-of-day and day-of-week patterns
 * Bottlenecks:      steps that consistently take >5s or fail >20% of the time
 * Success factors:  features correlated with successful task completion
 * </pre>
 */
@ApplicationScoped
public class SessionAnalyticsEngine {

    private static final Logger log = LoggerFactory.getLogger(SessionAnalyticsEngine.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Inject EpisodicMemory episodic;
    @Inject AgentTelemetry telemetry;

    // Rolling window of session events for real-time analytics
    private final Deque<SessionEvent> eventWindow    = new ArrayDeque<>();
    private final Map<String, ToolStats> toolHeatMap = new ConcurrentHashMap<>();
    private final Map<String, Long>  sequenceCounts  = new ConcurrentHashMap<>();
    private final List<JourneyRecord> journeys        = new CopyOnWriteArrayList<>();

    @PostConstruct
    void init() {
        // Load any persisted analytics state
        loadState();
        log.info("[analytics] initialized: {} tools tracked, {} journeys",
                toolHeatMap.size(), journeys.size());
    }

    // ── Event recording ────────────────────────────────────────────────────

    /**
     * Records a completed agent task for analytics.
     * Call this after every task completes.
     */
    public void recordTask(String taskId, String task, String category,
                            boolean success, int iterations, long durationMs,
                            List<String> toolsUsed, double qualityScore) {
        Instant now = Instant.now();
        TaskComplexity complexity = classifyComplexity(iterations, durationMs);

        SessionEvent event = new SessionEvent(taskId, task, category, success,
                iterations, durationMs, toolsUsed, qualityScore, complexity, now);

        synchronized (eventWindow) {
            eventWindow.addLast(event);
            if (eventWindow.size() > 1000) eventWindow.pollFirst();
        }

        // Update tool heat map
        toolsUsed.forEach(tool -> toolHeatMap.computeIfAbsent(tool, k -> new ToolStats(tool))
                .record(durationMs / Math.max(1, toolsUsed.size()), !success));

        // Update journey sequences
        updateJourneySequences(toolsUsed);

        telemetry.count("analytics.tasks.recorded");
        telemetry.gauge("analytics.complexity." + complexity.name().toLowerCase(), 1.0);

        // Persist async
        Thread.ofVirtual().start(() -> persistEvent(event));
    }

    /**
     * Records a real-time tool call event.
     */
    public void recordToolCall(String toolName, long durationMs, boolean error,
                                int outputBytes, Instant timestamp) {
        toolHeatMap.computeIfAbsent(toolName, k -> new ToolStats(toolName))
                .record(durationMs, error);
        telemetry.recordTool(toolName, durationMs, error, outputBytes);
    }

    // ── Analysis ──────────────────────────────────────────────────────────

    /**
     * Returns a full analytics report for a time window.
     *
     * @param since  start of the analysis window (null = all time)
     */
    public AnalyticsReport generateReport(Instant since) {
        List<SessionEvent> events = getEvents(since);
        if (events.isEmpty()) return AnalyticsReport.empty();

        // Complexity distribution
        Map<TaskComplexity, Long> complexityDist = events.stream()
                .collect(Collectors.groupingBy(SessionEvent::complexity, Collectors.counting()));

        // Success rate overall and by category
        double overallSuccessRate = events.stream()
                .mapToInt(e -> e.success() ? 1 : 0).average().orElse(0);
        Map<String, Double> successByCategory = events.stream()
                .collect(Collectors.groupingBy(SessionEvent::category,
                        Collectors.averagingInt(e -> e.success() ? 1 : 0)));

        // Tool heat map summary
        List<ToolHeatEntry> toolHeat = toolHeatMap.values().stream()
                .sorted(Comparator.comparingLong(ToolStats::totalCalls).reversed())
                .map(s -> new ToolHeatEntry(s.toolName(), s.totalCalls(),
                        s.avgDurationMs(), s.errorRate(), s.totalOutputBytes()))
                .toList();

        // Bottlenecks: tools with avg latency > 5s or error rate > 20%
        List<Bottleneck> bottlenecks = toolHeat.stream()
                .filter(t -> t.avgLatencyMs() > 5000 || t.errorRate() > 0.2)
                .map(t -> new Bottleneck(t.toolName(),
                        t.avgLatencyMs() > 5000 ? BottleneckType.HIGH_LATENCY : BottleneckType.HIGH_ERROR_RATE,
                        String.format("avg=%.0fms error=%.0f%%", t.avgLatencyMs(), t.errorRate() * 100)))
                .toList();

        // Common tool sequences
        List<SequencePattern> topSequences = sequenceCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new SequencePattern(e.getKey(), e.getValue()))
                .toList();

        // Peak usage by hour
        Map<Integer, Long> byHour = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.timestamp().atZone(ZoneId.systemDefault()).getHour(),
                        Collectors.counting()));

        // Task category breakdown
        Map<String, Long> byCategory = events.stream()
                .collect(Collectors.groupingBy(SessionEvent::category, Collectors.counting()));

        // Average quality by category
        Map<String, Double> qualityByCategory = events.stream()
                .filter(e -> e.qualityScore() > 0)
                .collect(Collectors.groupingBy(SessionEvent::category,
                        Collectors.averagingDouble(SessionEvent::qualityScore)));

        return new AnalyticsReport(
                since, Instant.now(), events.size(),
                overallSuccessRate, complexityDist, successByCategory,
                toolHeat, bottlenecks, topSequences,
                byHour, byCategory, qualityByCategory);
    }

    /**
     * Identifies tasks that the agent struggles with (high retry rate or low quality).
     */
    public List<StrugglePattern> identifyStrugglePatterns() {
        List<SessionEvent> events = getEvents(null);

        // Group by category and find high-failure categories
        return events.stream()
                .collect(Collectors.groupingBy(SessionEvent::category))
                .entrySet().stream()
                .map(e -> {
                    List<SessionEvent> catEvents = e.getValue();
                    double failRate = catEvents.stream()
                            .mapToInt(ev -> ev.success() ? 0 : 1).average().orElse(0);
                    double avgIter  = catEvents.stream()
                            .mapToInt(SessionEvent::iterations).average().orElse(0);
                    double avgQuality = catEvents.stream()
                            .mapToDouble(SessionEvent::qualityScore).average().orElse(0);
                    return new StrugglePattern(e.getKey(), catEvents.size(),
                            failRate, avgIter, avgQuality);
                })
                .filter(s -> s.failureRate() > 0.3 || s.avgIterations() > 7)
                .sorted(Comparator.comparingDouble(StrugglePattern::failureRate).reversed())
                .toList();
    }

    /**
     * Returns improvement recommendations based on analytics.
     */
    public List<Recommendation> generateRecommendations() {
        List<Recommendation> recs = new ArrayList<>();
        AnalyticsReport report = generateReport(Instant.now().minus(Duration.ofDays(7)));

        // High-latency tools
        report.bottlenecks().stream()
                .filter(b -> b.type() == BottleneckType.HIGH_LATENCY)
                .forEach(b -> recs.add(new Recommendation(
                        RecommendationType.PERFORMANCE,
                        "Tool '" + b.name() + "' is slow (avg " + b.detail() + ")",
                        "Consider caching its results or batching calls with ToolPipeline")));

        // Low success rates
        report.successByCategory().entrySet().stream()
                .filter(e -> e.getValue() < 0.6)
                .forEach(e -> recs.add(new Recommendation(
                        RecommendationType.SKILL_IMPROVEMENT,
                        "Category '" + e.getKey() + "' has low success rate (" +
                                String.format("%.0f%%", e.getValue() * 100) + ")",
                        "Consider running SkillEvolutionEngine on the relevant skill")));

        // High complexity tasks
        long veryComplex = report.complexityDistribution()
                .getOrDefault(TaskComplexity.VERY_COMPLEX, 0L);
        if (veryComplex > 5) recs.add(new Recommendation(
                RecommendationType.ARCHITECTURE,
                veryComplex + " tasks required 10+ iterations in the last 7 days",
                "Use HyperagentOrchestrator or DagExecutionEngine to decompose complex tasks"));

        return recs;
    }

    /**
     * Returns the tool heat map (most-used tools and their performance).
     */
    public List<ToolHeatEntry> toolHeatMap() {
        return toolHeatMap.values().stream()
                .sorted(Comparator.comparingLong(ToolStats::totalCalls).reversed())
                .map(s -> new ToolHeatEntry(s.toolName(), s.totalCalls(),
                        s.avgDurationMs(), s.errorRate(), s.totalOutputBytes()))
                .toList();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private TaskComplexity classifyComplexity(int iterations, long durationMs) {
        if (iterations <= 2 && durationMs < 10_000) return TaskComplexity.SIMPLE;
        if (iterations <= 5 && durationMs < 30_000) return TaskComplexity.MODERATE;
        if (iterations <= 10 && durationMs < 120_000) return TaskComplexity.COMPLEX;
        return TaskComplexity.VERY_COMPLEX;
    }

    private void updateJourneySequences(List<String> tools) {
        if (tools.size() < 2) return;
        // Record all bigrams (consecutive pairs)
        for (int i = 0; i < tools.size() - 1; i++) {
            String bigram = tools.get(i) + "→" + tools.get(i + 1);
            sequenceCounts.merge(bigram, 1L, Long::sum);
        }
        // Record the full sequence if it's short enough
        if (tools.size() <= 5) {
            sequenceCounts.merge(String.join("→", tools), 1L, Long::sum);
        }
    }

    private List<SessionEvent> getEvents(Instant since) {
        synchronized (eventWindow) {
            return eventWindow.stream()
                    .filter(e -> since == null || e.timestamp().isAfter(since))
                    .toList();
        }
    }

    private void persistEvent(SessionEvent event) {
        Path dir = Path.of(System.getProperty("user.home"), ".gamelan", "analytics");
        try {
            Files.createDirectories(dir);
            String line = MAPPER.writeValueAsString(event) + "\n";
            Files.writeString(dir.resolve("sessions.jsonl"), line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    @SuppressWarnings("unchecked")
    private void loadState() {
        Path dir = Path.of(System.getProperty("user.home"), ".gamelan", "analytics");
        Path file = dir.resolve("sessions.jsonl");
        if (!Files.exists(file)) return;
        try {
            Files.lines(file).limit(500).forEach(line -> {
                try {
                    SessionEvent event = MAPPER.readValue(line, SessionEvent.class);
                    eventWindow.addLast(event);
                    event.toolsUsed().forEach(t ->
                            toolHeatMap.computeIfAbsent(t, k -> new ToolStats(t))
                                    .record(event.durationMs() / Math.max(1, event.toolsUsed().size()),
                                            !event.success()));
                    updateJourneySequences(event.toolsUsed());
                } catch (Exception ignored) {}
            });
        } catch (IOException e) {
            log.warn("[analytics] could not load state: {}", e.getMessage());
        }
    }

    // ── Inner types ────────────────────────────────────────────────────────

    private static final class ToolStats {
        private final String toolName;
        private long   totalCalls;
        private long   errorCalls;
        private long   totalDurationMs;
        private long   totalOutputBytes;

        ToolStats(String name) { this.toolName = name; }

        synchronized void record(long durationMs, boolean error) {
            totalCalls++;
            totalDurationMs += durationMs;
            if (error) errorCalls++;
        }

        String toolName()        { return toolName; }
        long   totalCalls()      { return totalCalls; }
        long   totalOutputBytes(){ return totalOutputBytes; }
        double avgDurationMs()   { return totalCalls == 0 ? 0 : (double) totalDurationMs / totalCalls; }
        double errorRate()       { return totalCalls == 0 ? 0 : (double) errorCalls / totalCalls; }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum TaskComplexity    { SIMPLE, MODERATE, COMPLEX, VERY_COMPLEX }
    public enum BottleneckType    { HIGH_LATENCY, HIGH_ERROR_RATE, LOOP_DETECTED }
    public enum RecommendationType{ PERFORMANCE, SKILL_IMPROVEMENT, ARCHITECTURE, CONFIGURATION }

    public record SessionEvent(
            String         taskId,
            String         task,
            String         category,
            boolean        success,
            int            iterations,
            long           durationMs,
            List<String>   toolsUsed,
            double         qualityScore,
            TaskComplexity complexity,
            Instant        timestamp
    ) {}

    public record ToolHeatEntry(
            String toolName, long totalCalls, double avgLatencyMs, double errorRate, long totalOutputBytes
    ) {}

    public record Bottleneck(String name, BottleneckType type, String detail) {}

    public record SequencePattern(String sequence, long occurrences) {}

    public record StrugglePattern(
            String category, int taskCount, double failureRate,
            double avgIterations, double avgQualityScore
    ) {
        public String summary() {
            return String.format("[%s]: fail=%.0f%% avgIter=%.1f quality=%.2f (%d tasks)",
                    category, failureRate * 100, avgIterations, avgQualityScore, taskCount);
        }
    }

    public record Recommendation(RecommendationType type, String observation, String action) {}

    public record JourneyRecord(List<String> toolSequence, Instant completedAt) {}

    public record AnalyticsReport(
            Instant                        since,
            Instant                        generatedAt,
            int                            totalTasks,
            double                         overallSuccessRate,
            Map<TaskComplexity, Long>      complexityDistribution,
            Map<String, Double>            successByCategory,
            List<ToolHeatEntry>            toolHeatMap,
            List<Bottleneck>               bottlenecks,
            List<SequencePattern>          topSequences,
            Map<Integer, Long>             tasksByHour,
            Map<String, Long>              tasksByCategory,
            Map<String, Double>            qualityByCategory
    ) {
        static AnalyticsReport empty() {
            return new AnalyticsReport(null, Instant.now(), 0, 0.0,
                    Map.of(), Map.of(), List.of(), List.of(), List.of(),
                    Map.of(), Map.of(), Map.of());
        }

        public String summary() {
            return String.format(
                    "Analytics[%d tasks]: %.0f%% success | %d bottlenecks | %d categories",
                    totalTasks, overallSuccessRate * 100, bottlenecks.size(), tasksByCategory.size());
        }
    }
}

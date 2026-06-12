package tech.kayys.gamelan.observability;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.orchestration.AgentEventListener;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.planning.HierarchicalTaskPlanner;
import tech.kayys.gamelan.planning.adaptive.AdaptivePlanner;
import tech.kayys.gamelan.planning.versioning.PlanVersionStore;
import tech.kayys.gamelan.resilience.circuit.AgentResilienceKit;
import tech.kayys.gamelan.streaming.sse.AgentStreamingService;
import tech.kayys.gollek.sdk.core.GollekSdk;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Enhancement suite: telemetry, resilience, streaming, adaptive planning, cache.
 */
@ExtendWith(MockitoExtension.class)
class EnhancementSuiteTest {

    // ── AgentTelemetry ────────────────────────────────────────────────────

    private AgentTelemetry telemetry;

    @BeforeEach
    void setUpTelemetry() {
        telemetry = new AgentTelemetry();
    }

    @Test
    void counterStartsAtZero() {
        assertThat(telemetry.getCount("agent.task.total")).isEqualTo(0);
    }

    @Test
    void countIncrements() {
        telemetry.count("my.metric");
        telemetry.count("my.metric");
        assertThat(telemetry.getCount("my.metric")).isEqualTo(2);
    }

    @Test
    void countByDelta() {
        telemetry.count("tokens", 1500);
        assertThat(telemetry.getCount("tokens")).isEqualTo(1500);
    }

    @Test
    void gaugeSetAndGet() {
        telemetry.gauge("quality.last", 0.87);
        assertThat(telemetry.getGauge("quality.last")).isEqualTo(0.87);
    }

    @Test
    void gaugeUpdatesOnSet() {
        telemetry.gauge("score", 0.5);
        telemetry.gauge("score", 0.9);
        assertThat(telemetry.getGauge("score")).isEqualTo(0.9);
    }

    @Test
    void histogramRecordsDistribution() {
        telemetry.recordLatency("tool.latency", 10L);
        telemetry.recordLatency("tool.latency", 100L);
        telemetry.recordLatency("tool.latency", 500L);

        AgentTelemetry.LatencyHistogram h = telemetry.histogram("tool.latency");
        assertThat(h.count()).isEqualTo(3);
        assertThat(h.min()).isLessThanOrEqualTo(10L);
        assertThat(h.max()).isGreaterThanOrEqualTo(500L);
        assertThat(h.p95()).isGreaterThan(0L);
    }

    @Test
    void spanRecordsOnClose() throws Exception {
        try (AgentTelemetry.Span span = telemetry.span("planning:got")) {
            span.attr("nodes", 15).attr("score", 0.82);
            Thread.sleep(5);
        }
        // After close, latency should be recorded
        assertThat(telemetry.histogram("planning:got").count()).isEqualTo(1);
        assertThat(telemetry.getCount("planning:got.calls")).isEqualTo(1);
    }

    @Test
    void spanRecordsErrorOnFailure() {
        try (AgentTelemetry.Span span = telemetry.span("tool:shell")) {
            span.error("timeout after 60s");
        }
        assertThat(telemetry.getCount("tool:shell.error")).isEqualTo(1);
    }

    @Test
    void timedWrapperRecordsLatency() {
        String result = telemetry.timed("embed.compute", () -> "result");
        assertThat(result).isEqualTo("result");
        assertThat(telemetry.histogram("embed.compute").count()).isEqualTo(1);
        assertThat(telemetry.getCount("embed.compute.success")).isEqualTo(1);
    }

    @Test
    void timedWrapperRecordsError() {
        assertThatThrownBy(() ->
                telemetry.timed("failing.op", () -> { throw new RuntimeException("boom"); }))
                .isInstanceOf(RuntimeException.class);
        assertThat(telemetry.getCount("failing.op.error")).isEqualTo(1);
    }

    @Test
    void recordTaskUpdatesAllCounters() {
        telemetry.recordTask("react", true, 5, 1200, 8500L, 0.91);
        assertThat(telemetry.getCount("agent.task.total")).isEqualTo(1);
        assertThat(telemetry.getCount("agent.task.success")).isEqualTo(1);
        assertThat(telemetry.getCount("agent.task.tokens.total")).isEqualTo(1200);
        assertThat(telemetry.getGauge("agent.task.quality.last")).isEqualTo(0.91);
    }

    @Test
    void recordToolUpdatesPerToolMetrics() {
        telemetry.recordTool("read_file", 45L, false, 3420);
        assertThat(telemetry.getCount("agent.tool.read_file.calls")).isEqualTo(1);
        assertThat(telemetry.getCount("agent.tool.read_file.errors")).isEqualTo(0);
    }

    @Test
    void prometheusTextContainsCounters() {
        telemetry.count("agent.task.total", 5);
        String prom = telemetry.prometheusText();
        assertThat(prom).contains("gamelan_agent_task_total");
        assertThat(prom).contains("5");
    }

    @Test
    void dashboardIsNonBlank() {
        telemetry.recordTask("react", true, 3, 800, 5000L, 0.8);
        assertThat(telemetry.dashboard()).isNotBlank().contains("Dashboard");
    }

    @Test
    void resetClearsAllMetrics() {
        telemetry.count("some.metric", 100);
        telemetry.reset();
        assertThat(telemetry.getCount("some.metric")).isEqualTo(0);
    }

    @Test
    void histogramPercentileBuckets() {
        AgentTelemetry.LatencyHistogram h = new AgentTelemetry.LatencyHistogram();
        // record 100 values from 1ms to 1000ms
        for (int i = 1; i <= 100; i++) h.record(i * 10L);
        assertThat(h.p50()).isGreaterThan(0L);
        assertThat(h.p95()).isGreaterThanOrEqualTo(h.p50());
        assertThat(h.p99()).isGreaterThanOrEqualTo(h.p95());
        assertThat(h.avg()).isBetween(100.0, 600.0);
    }

    // ── AgentResilienceKit ────────────────────────────────────────────────

    private AgentResilienceKit resilience;

    @BeforeEach
    void setUpResilience() {
        resilience = new AgentResilienceKit();
    }

    @Test
    void circuitBreakerClosedByDefault() {
        assertThat(resilience.circuitStates()).isEmpty();
        // First use creates it in CLOSED state
        resilience.withCircuitBreaker("test-tool", () -> "ok");
        assertThat(resilience.circuitStates().get("test-tool"))
                .isEqualTo(AgentResilienceKit.CircuitBreaker.State.CLOSED);
    }

    @Test
    void circuitBreakerReturnsResult() {
        String result = resilience.withCircuitBreaker("db-tool", () -> "result");
        assertThat(result).isEqualTo("result");
    }

    @Test
    void circuitBreakerOpensAfterFailures() {
        // Trigger failures to open the circuit
        AgentResilienceKit.CircuitBreaker cb = new AgentResilienceKit.CircuitBreaker(
                "test",
                new AgentResilienceKit.CircuitBreakerConfig(0.5, 30, 3));

        for (int i = 0; i < 10; i++) {
            try { cb.execute(() -> { throw new RuntimeException("fail"); }); }
            catch (Exception ignored) {}
        }
        assertThat(cb.state()).isEqualTo(AgentResilienceKit.CircuitBreaker.State.OPEN);
    }

    @Test
    void forceOpenAndResetWork() {
        resilience.withCircuitBreaker("resource", () -> "ok");
        resilience.forceOpen("resource");
        assertThat(resilience.circuitStates().get("resource"))
                .isEqualTo(AgentResilienceKit.CircuitBreaker.State.OPEN);

        resilience.reset("resource");
        assertThat(resilience.circuitStates().get("resource"))
                .isEqualTo(AgentResilienceKit.CircuitBreaker.State.CLOSED);
    }

    @Test
    void circuitOpenThrowsImmediately() {
        resilience.forceOpen("broken-tool");
        assertThatThrownBy(() -> resilience.withCircuitBreaker("broken-tool", () -> "ok"))
                .isInstanceOf(AgentResilienceKit.CircuitOpenException.class);
    }

    @Test
    void retrySucceedsOnSecondAttempt() {
        AtomicInteger attempts = new AtomicInteger();
        String result = resilience.withRetry("flaky-op", 3, 10,
                e -> true,
                () -> {
                    if (attempts.incrementAndGet() < 2) throw new RuntimeException("transient");
                    return "success";
                });
        assertThat(result).isEqualTo("success");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void retryGivesUpAfterMaxAttempts() {
        assertThatThrownBy(() ->
                resilience.withRetry("always-fails", 3, 1,
                        e -> true, () -> { throw new RuntimeException("always"); }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed after");
    }

    @Test
    void bulkheadLimitsParallelism() throws Exception {
        Semaphore inFlight = new Semaphore(0);
        CountDownLatch latch = new CountDownLatch(1);

        // Fill the bulkhead (max=2)
        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        exec.submit(() -> resilience.withBulkhead("test-cat", 2, 5000,
                () -> { inFlight.release(); try { latch.await(); } catch (Exception e) {} return null; }));
        exec.submit(() -> resilience.withBulkhead("test-cat", 2, 5000,
                () -> { inFlight.release(); try { latch.await(); } catch (Exception e) {} return null; }));

        // Wait for both to be in the bulkhead
        inFlight.tryAcquire(2, 2, TimeUnit.SECONDS);

        // Third call should be rejected
        assertThatThrownBy(() -> resilience.withBulkhead("test-cat", 2, 100, () -> "ok"))
                .isInstanceOf(AgentResilienceKit.BulkheadFullException.class);

        latch.countDown();
        exec.shutdown();
    }

    @Test
    void bulkheadStatsTrackRejections() {
        // Fill bulkhead then overflow
        try {
            for (int i = 0; i < 5; i++) {
                resilience.withBulkhead("tiny", 1, 1, () -> { Thread.sleep(100); return null; });
            }
        } catch (Exception ignored) {}

        var stats = resilience.bulkheadStats().get("tiny");
        assertThat(stats).isNotNull();
        assertThat(stats.summary()).contains("tiny");
    }

    // ── AgentStreamingService ─────────────────────────────────────────────

    private AgentStreamingService streaming;

    @BeforeEach
    void setUpStreaming() {
        streaming = new AgentStreamingService();
    }

    @Test
    void subscribeAndReceiveEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<AgentStreamingService.SseFrame> received = new CopyOnWriteArrayList<>();

        try (var sub = streaming.subscribe("run-1", frame -> {
            received.add(frame);
            latch.countDown();
        })) {
            streaming.emit("run-1", AgentStreamingService.SseFrame.token("run-1", "hello"));
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(received).hasSize(1);
        assertThat(received.get(0).type()).isEqualTo("token");
        assertThat(received.get(0).content()).isEqualTo("hello");
    }

    @Test
    void ssFrameToSseStringFormat() {
        AgentStreamingService.SseFrame frame =
                AgentStreamingService.SseFrame.token("run-99", "chunk");
        String sse = frame.toSseString();
        assertThat(sse).startsWith("data: {");
        assertThat(sse).contains("\"type\":\"token\"");
        assertThat(sse).contains("\"content\":\"chunk\"");
        assertThat(sse).endsWith("\n\n");
    }

    @Test
    void listenerForEmitsToSubscribers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<String> types = new CopyOnWriteArrayList<>();

        streaming.subscribe("run-2", frame -> { types.add(frame.type()); latch.countDown(); });
        AgentEventListener listener = streaming.listenerFor("run-2");

        listener.onRunStart("test task", "llama3");
        listener.onTextChunk("hello");
        listener.onComplete("done", 1);

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(types).contains("task_started", "token");
    }

    @Test
    void multipleSubscribersAllReceiveEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<String> received = new CopyOnWriteArrayList<>();

        streaming.subscribe("run-3", f -> { received.add("A"); latch.countDown(); });
        streaming.subscribe("run-3", f -> { received.add("B"); latch.countDown(); });
        streaming.emit("run-3", AgentStreamingService.SseFrame.token("run-3", "broadcast"));

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(received).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void subscriberCountTracking() {
        try (var s1 = streaming.subscribe("run-4", f -> {})) {
            assertThat(streaming.activeRuns()).isEqualTo(1);
        }
    }

    @Test
    void sseFrameFactoriesHaveCorrectTypes() {
        assertThat(AgentStreamingService.SseFrame.taskStarted("r","task","m").type()).isEqualTo("task_started");
        assertThat(AgentStreamingService.SseFrame.iteration("r",1,5).type()).isEqualTo("iteration");
        assertThat(AgentStreamingService.SseFrame.toolCall("r","read_file","x").type()).isEqualTo("tool_call");
        assertThat(AgentStreamingService.SseFrame.taskDone("r","done",3).type()).isEqualTo("task_done");
        assertThat(AgentStreamingService.SseFrame.error("r","err",2).type()).isEqualTo("error");
        assertThat(AgentStreamingService.SseFrame.streamEnd("r").type()).isEqualTo("stream_end");
    }

    // ── AdaptivePlanner ───────────────────────────────────────────────────

    @Mock EpisodicMemory     episodic2;
    @Mock PlanVersionStore   versionStore2;
    @Mock HierarchicalTaskPlanner htn2;
    @Mock GamelanConfig      config2;

    @InjectMocks AdaptivePlanner adaptivePlanner;

    @BeforeEach
    void setUpAdaptivePlanner() {
        when(episodic2.findRelevant(any(), anyInt())).thenReturn(List.of());
        when(episodic2.all()).thenReturn(List.of());
        when(versionStore2.findBest(any())).thenReturn(Optional.empty());
        when(htn2.plan(any(), any(), any())).thenReturn(simplePlan("base", 3));
    }

    @Test
    void planReturnsAdaptedPlanWithExplanation() {
        AdaptivePlanner.AdaptedPlan result = adaptivePlanner.plan("add unit tests", null);
        assertThat(result).isNotNull();
        assertThat(result.plan()).isNotNull();
        assertThat(result.explanation()).isNotBlank();
        assertThat(result.category()).isNotBlank();
    }

    @Test
    void planClassifiesTaskCategory() {
        when(htn2.plan(any(), any(), any())).thenReturn(simplePlan("test-plan", 3));
        AdaptivePlanner.AdaptedPlan result = adaptivePlanner.plan("write unit tests for OrderService", null);
        assertThat(result.category()).isEqualTo("testing");
    }

    @Test
    void planUsesWarmStartWhenHistoryAvailable() {
        when(versionStore2.findBest(any())).thenReturn(
                Optional.of(new PlanVersionStore.PlanVersion(
                        "ver-1", "fp", "task", simplePlan("historical", 4),
                        PlanVersionStore.PlanMetrics.success(2000L, 3, 800),
                        Set.of("production"), Instant.now())));

        AdaptivePlanner.AdaptedPlan result =
                adaptivePlanner.plan("add unit tests to UserService", null);
        assertThat(result.isWarmStart()).isTrue();
    }

    @Test
    void recordOutcomeUpdatesSuccessRate() {
        adaptivePlanner.recordOutcome("testing", true, 5000L, 4000L, 1200, 1000);
        adaptivePlanner.recordOutcome("testing", true, 5000L, 4000L, 1200, 1000);
        adaptivePlanner.recordOutcome("testing", false, 8000L, 4000L, 2000, 1000);

        AdaptivePlanner.CalibrationReport report = adaptivePlanner.calibrationReport();
        assertThat(report.categoryStats()).containsKey("testing");
        assertThat(report.categoryStats().get("testing").successRate()).isBetween(0.5, 1.0);
    }

    @Test
    void tokenCalibrationAdjustsEstimates() {
        // Record 3 outcomes where actual is always 1.5x estimated → calibration factor ~1.5
        for (int i = 0; i < 5; i++) {
            adaptivePlanner.recordOutcome("security", true, 5000L, 5000L, 1500, 1000);
        }
        AdaptivePlanner.CalibrationReport report = adaptivePlanner.calibrationReport();
        AdaptivePlanner.CategoryStats stats = report.categoryStats().get("security");
        assertThat(stats).isNotNull();
        assertThat(stats.tokenCalibrationFactor()).isGreaterThan(1.0);
    }

    @Test
    void calibrationReportSummaryIsNonBlank() {
        adaptivePlanner.recordOutcome("bug-fix", true, 3000L, 3000L, 800, 800);
        assertThat(adaptivePlanner.calibrationReport().summary()).isNotBlank();
    }

    @Test
    void resetLearningClearsAllData() {
        adaptivePlanner.recordOutcome("testing", true, 5000L, 4000L, 1200, 1000);
        adaptivePlanner.resetLearning();
        assertThat(adaptivePlanner.calibrationReport().categoryStats()).isEmpty();
    }

    @Test
    void confidenceGrowsWithDataPoints() {
        // Low confidence with no data
        AdaptivePlanner.AdaptedPlan result1 = adaptivePlanner.plan("fix bug", null);
        assertThat(result1.confidence()).isLessThan(0.5);

        // After recording outcomes, confidence should grow
        for (int i = 0; i < 10; i++) {
            adaptivePlanner.recordOutcome("bug-fix", true, 3000L, 3000L, 800, 800);
        }
        AdaptivePlanner.AdaptedPlan result2 = adaptivePlanner.plan("fix another bug", null);
        // After more data the confidence of bug-fix category is higher
        assertThat(result2.confidence()).isGreaterThanOrEqualTo(result1.confidence());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private HierarchicalTaskPlanner.Plan simplePlan(String name, int steps) {
        List<HierarchicalTaskPlanner.TaskNode> tasks = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            tasks.add(new HierarchicalTaskPlanner.TaskNode(
                    "t" + i, "step " + i,
                    HierarchicalTaskPlanner.TaskNode.TaskType.ATOMIC,
                    HierarchicalTaskPlanner.TaskNode.RiskLevel.LOW,
                    List.of(), List.of(), 300, "done", Map.of()));
        }
        return new HierarchicalTaskPlanner.Plan(
                UUID.randomUUID().toString(), name, "goal", tasks,
                HierarchicalTaskPlanner.ExecutionMode.SEQUENTIAL,
                steps * 300, 5000L, 0.8, 4800L, 1, Instant.now());
    }
}

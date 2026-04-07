package tech.kayys.wayang.rag.retrieval;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class RagRetrievalEvalGuardrailService {

    private final RagRuntimeConfig config;
    private final RagRetrievalEvalHistoryService historyService;
    private final RagRetrievalEvalMetrics metrics;
    private final Clock clock;

    @Inject
    public RagRetrievalEvalGuardrailService(
            RagRuntimeConfig config,
            RagRetrievalEvalHistoryService historyService,
            RagRetrievalEvalMetrics metrics) {
        this(config, historyService, metrics, Clock.systemUTC());
    }

    RagRetrievalEvalGuardrailService(
            RagRuntimeConfig config,
            RagRetrievalEvalHistoryService historyService,
            RagRetrievalEvalMetrics metrics,
            Clock clock) {
        this.config = Objects.requireNonNull(config, "config");
        this.historyService = Objects.requireNonNull(historyService, "historyService");
        this.metrics = metrics;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public RagRetrievalEvalGuardrailStatus evaluate(String tenantId, String datasetName, Integer windowOverride) {
        Instant now = clock.instant();
        int window = windowOverride != null && windowOverride > 0
                ? windowOverride
                : Math.max(2, config.getRetrievalEvalGuardrailWindowSize());

        RagRetrievalEvalTrendResponse trend = historyService.trend(tenantId, datasetName, window);

        if (!config.isRetrievalEvalGuardrailEnabled()) {
            RagRetrievalEvalGuardrailStatus status = new RagRetrievalEvalGuardrailStatus(
                    false,
                    true,
                    emptyToNull(tenantId),
                    emptyToNull(datasetName),
                    window,
                    trend.runCount(),
                    "guardrail_disabled",
                    List.of(),
                    trend,
                    now);
            record(status);
            return status;
        }

        if (trend.latest() == null || trend.previous() == null) {
            RagRetrievalEvalGuardrailStatus status = new RagRetrievalEvalGuardrailStatus(
                    true,
                    true,
                    emptyToNull(tenantId),
                    emptyToNull(datasetName),
                    window,
                    trend.runCount(),
                    "insufficient_history",
                    List.of(),
                    trend,
                    now);
            record(status);
            return status;
        }

        List<RagRetrievalEvalGuardrailBreach> breaches = new ArrayList<>();

        maybeAddLowerBoundBreach(
                breaches,
                "recall_at_k_delta",
                trend.recallAtKDelta(),
                -Math.abs(config.getRetrievalEvalGuardrailRecallDropMax()));
        maybeAddLowerBoundBreach(
                breaches,
                "mrr_delta",
                trend.mrrDelta(),
                -Math.abs(config.getRetrievalEvalGuardrailMrrDropMax()));
        maybeAddUpperBoundBreach(
                breaches,
                "latency_p95_ms_delta",
                trend.latencyP95MsDelta(),
                Math.abs(config.getRetrievalEvalGuardrailLatencyP95IncreaseMaxMs()));
        maybeAddUpperBoundBreach(
                breaches,
                "latency_avg_ms_delta",
                trend.latencyAvgMsDelta(),
                Math.abs(config.getRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs()));

        RagRetrievalEvalGuardrailStatus status = new RagRetrievalEvalGuardrailStatus(
                true,
                breaches.isEmpty(),
                emptyToNull(tenantId),
                emptyToNull(datasetName),
                window,
                trend.runCount(),
                breaches.isEmpty() ? "ok" : "regression_detected",
                List.copyOf(breaches),
                trend,
                now);
        record(status);
        return status;
    }

    private void maybeAddLowerBoundBreach(
            List<RagRetrievalEvalGuardrailBreach> breaches,
            String metric,
            Double observed,
            double threshold) {
        if (observed == null) {
            return;
        }
        if (observed < threshold) {
            breaches.add(new RagRetrievalEvalGuardrailBreach(
                    metric,
                    observed,
                    threshold,
                    "Observed delta=" + observed + " is below minimum threshold=" + threshold));
        }
    }

    private void maybeAddUpperBoundBreach(
            List<RagRetrievalEvalGuardrailBreach> breaches,
            String metric,
            Double observed,
            double threshold) {
        if (observed == null) {
            return;
        }
        if (observed > threshold) {
            breaches.add(new RagRetrievalEvalGuardrailBreach(
                    metric,
                    observed,
                    threshold,
                    "Observed delta=" + observed + " exceeds maximum threshold=" + threshold));
        }
    }

    private void record(RagRetrievalEvalGuardrailStatus status) {
        if (metrics != null) {
            metrics.recordGuardrail(status);
        }
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

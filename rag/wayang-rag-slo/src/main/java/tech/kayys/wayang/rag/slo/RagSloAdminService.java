package tech.kayys.wayang.rag.slo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;
import tech.kayys.wayang.rag.runtime.RagObservabilityMetrics;
import tech.kayys.wayang.rag.runtime.RagObservabilityMetrics.RagSloSnapshot;
import tech.kayys.wayang.rag.embedding.EmbeddingSchemaHistoryCompactorJob;
import tech.kayys.wayang.rag.embedding.EmbeddingSchemaHistoryCompactorStatus;
import tech.kayys.wayang.rag.retrieval.RagRetrievalEvalGuardrailService;
import tech.kayys.wayang.rag.retrieval.RagRetrievalEvalGuardrailStatus;
import tech.kayys.wayang.rag.retrieval.RagRetrievalEvalGuardrailBreach;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RagSloAdminService {

    private final RagRuntimeConfig config;
    private final RagObservabilityMetrics metrics;
    private final EmbeddingSchemaHistoryCompactorJob compactorJob;
    private final RagRetrievalEvalGuardrailService evalGuardrailService;

    @Inject
    public RagSloAdminService(
            RagRuntimeConfig config,
            RagObservabilityMetrics metrics,
            EmbeddingSchemaHistoryCompactorJob compactorJob,
            RagRetrievalEvalGuardrailService evalGuardrailService) {
        this.config = config;
        this.metrics = metrics;
        this.compactorJob = compactorJob;
        this.evalGuardrailService = evalGuardrailService;
    }

    public RagSloAdminService(RagRuntimeConfig config, RagObservabilityMetrics metrics) {
        this(config, metrics, null, null);
    }

    public RagSloAdminService(RagRuntimeConfig config, RagObservabilityMetrics metrics,
            EmbeddingSchemaHistoryCompactorJob compactorJob) {
        this(config, metrics, compactorJob, null);
    }

    public RagSloStatus status() {
        RagSloThresholds thresholds = new RagSloThresholds(
                config.getSloEmbeddingLatencyP95Ms(),
                config.getSloSearchLatencyP95Ms(),
                config.getSloIngestLatencyP95Ms(),
                config.getSloEmbeddingFailureRate(),
                config.getSloSearchFailureRate(),
                config.getSloIndexLagMs(),
                config.getSloCompactionFailureRate(),
                config.getSloCompactionCycleStalenessMs(),
                config.getSloSeverityWarningMultiplier(),
                config.getSloSeverityCriticalMultiplier(),
                config.getSloSeverityWarningByMetric(),
                config.getSloSeverityCriticalByMetric(),
                config.isSloAlertEnabled(),
                config.getSloAlertMinSeverity(),
                config.getSloAlertCooldownMs());
        RagSloSnapshot snapshot = metrics.snapshot();
        List<RagSloBreach> breaches = evaluateBreaches(
                thresholds,
                snapshot,
                compactorJob == null ? null : compactorJob.status(),
                evalGuardrailService == null ? null : evalGuardrailService.evaluate(null, null, null));
        return new RagSloStatus(breaches.isEmpty(), thresholds, snapshot, List.copyOf(breaches), Instant.now());
    }

    private List<RagSloBreach> evaluateBreaches(
            RagSloThresholds thresholds,
            RagSloSnapshot snapshot,
            EmbeddingSchemaHistoryCompactorStatus compactorStatus,
            RagRetrievalEvalGuardrailStatus evalGuardrailStatus) {
        List<RagSloBreach> breaches = new ArrayList<>();
        checkThreshold(
                breaches,
                "embedding_latency_p95_ms",
                snapshot.embeddingLatencyP95Ms(),
                thresholds.embeddingLatencyP95Ms());
        checkThreshold(
                breaches,
                "search_latency_p95_ms",
                snapshot.searchLatencyP95Ms(),
                thresholds.searchLatencyP95Ms());
        checkThreshold(
                breaches,
                "ingest_latency_p95_ms",
                snapshot.ingestLatencyP95Ms(),
                thresholds.ingestLatencyP95Ms());
        checkThreshold(
                breaches,
                "embedding_failure_rate",
                snapshot.embeddingFailureRate(),
                thresholds.embeddingFailureRate());
        checkThreshold(
                breaches,
                "search_failure_rate",
                snapshot.searchFailureRate(),
                thresholds.searchFailureRate());
        checkThreshold(
                breaches,
                "index_lag_ms",
                snapshot.indexLagMs(),
                thresholds.indexLagMs());
        evaluateCompactorBreaches(breaches, thresholds, compactorStatus);
        evaluateEvalGuardrailBreaches(breaches, evalGuardrailStatus);
        return breaches;
    }

    private void evaluateCompactorBreaches(
            List<RagSloBreach> breaches,
            RagSloThresholds thresholds,
            EmbeddingSchemaHistoryCompactorStatus compactorStatus) {
        if (compactorStatus == null || !compactorStatus.enabled()) {
            return;
        }
        double failureRate = compactorFailureRate(compactorStatus);
        checkThreshold(
                breaches,
                "compaction_failure_rate",
                failureRate,
                thresholds.compactionFailureRate());

        double stalenessMs = cycleStalenessMs(compactorStatus);
        checkThreshold(
                breaches,
                "compaction_cycle_staleness_ms",
                stalenessMs,
                thresholds.compactionCycleStalenessMs());
    }

    private void evaluateEvalGuardrailBreaches(
            List<RagSloBreach> breaches,
            RagRetrievalEvalGuardrailStatus guardrailStatus) {
        if (guardrailStatus == null || !guardrailStatus.enabled() || guardrailStatus.healthy()) {
            return;
        }
        checkThreshold(breaches, "eval_guardrail_breach_count", guardrailStatus.breaches().size(), 0.0);
        for (RagRetrievalEvalGuardrailBreach guardrailBreach : guardrailStatus.breaches()) {
            String metric = "eval_guardrail_" + guardrailBreach.metric();
            breaches.add(new RagSloBreach(
                    metric,
                    guardrailBreach.observedDelta(),
                    guardrailBreach.threshold(),
                    "critical",
                    "Retrieval eval regression detected: " + guardrailBreach.message()));
        }
    }

    private static double compactorFailureRate(EmbeddingSchemaHistoryCompactorStatus status) {
        double failures = status.totalFailures();
        double processed = status.totalTenantsProcessed();
        double total = failures + processed;
        if (total <= 0.0) {
            return 0.0;
        }
        return failures / total;
    }

    private static double cycleStalenessMs(EmbeddingSchemaHistoryCompactorStatus status) {
        if (status.lastCycleFinishedAt() == null) {
            return Double.MAX_VALUE;
        }
        return Math.max(0L, Instant.now().toEpochMilli() - status.lastCycleFinishedAt().toEpochMilli());
    }

    private void checkThreshold(List<RagSloBreach> breaches, String metric, double observed, double threshold) {
        double warningMultiplier = resolveWarningMultiplier(metric);
        double criticalMultiplier = resolveCriticalMultiplier(metric, warningMultiplier);
        if (isBreach(observed, threshold, warningMultiplier)) {
            String severity = classifySeverity(
                    observed,
                    threshold,
                    warningMultiplier,
                    criticalMultiplier);
            breaches.add(new RagSloBreach(
                    metric,
                    observed,
                    threshold,
                    severity,
                    "Observed " + metric + "=" + observed + " exceeds threshold=" + threshold
                            + " (severity=" + severity + ")"));
        }
    }

    private double resolveWarningMultiplier(String metric) {
        return config.getSloSeverityWarningByMetric().getOrDefault(
                metric,
                config.getSloSeverityWarningMultiplier());
    }

    private double resolveCriticalMultiplier(String metric, double warningMultiplier) {
        return config.getSloSeverityCriticalByMetric().getOrDefault(
                metric,
                Math.max(warningMultiplier, config.getSloSeverityCriticalMultiplier()));
    }

    private static boolean isBreach(double observed, double threshold, double warningMultiplier) {
        if (threshold <= 0.0) {
            return observed > threshold;
        }
        double normalizedWarning = Math.max(1.0, warningMultiplier);
        return observed > threshold * normalizedWarning;
    }

    private static String classifySeverity(
            double observed,
            double threshold,
            double warningMultiplier,
            double criticalMultiplier) {
        if (threshold <= 0.0) {
            return "critical";
        }
        double normalizedWarning = Math.max(1.0, warningMultiplier);
        double normalizedCritical = Math.max(normalizedWarning, criticalMultiplier);
        double ratio = observed / threshold;
        return ratio >= normalizedCritical ? "critical" : "warning";
    }
}

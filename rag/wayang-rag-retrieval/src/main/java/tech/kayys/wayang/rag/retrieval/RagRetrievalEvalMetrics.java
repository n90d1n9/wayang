package tech.kayys.wayang.rag.retrieval;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class RagRetrievalEvalMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicLong lastRecallAtKScaled = new AtomicLong(0L);
    private final AtomicLong lastMrrScaled = new AtomicLong(0L);
    private final AtomicLong lastLatencyP95MsScaled = new AtomicLong(0L);
    private final AtomicLong lastLatencyAvgMsScaled = new AtomicLong(0L);
    private final AtomicLong lastGuardrailHealthy = new AtomicLong(1L);
    private final AtomicLong lastGuardrailBreachCount = new AtomicLong(0L);

    public RagRetrievalEvalMetrics() {
        this((MeterRegistry) null);
    }

    @Inject
    public RagRetrievalEvalMetrics(Instance<MeterRegistry> meterRegistryInstance) {
        this.meterRegistry = meterRegistryInstance != null && meterRegistryInstance.isResolvable()
                ? meterRegistryInstance.get()
                : null;
    }

    public RagRetrievalEvalMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initGauges() {
        if (meterRegistry == null) {
            return;
        }
        Gauge.builder("wayang.rag.eval.retrieval.last.recall_at_k", lastRecallAtKScaled,
                        value -> value.get() / 1000.0d)
                .register(meterRegistry);
        Gauge.builder("wayang.rag.eval.retrieval.last.mrr", lastMrrScaled,
                        value -> value.get() / 1000.0d)
                .register(meterRegistry);
        Gauge.builder("wayang.rag.eval.retrieval.last.latency_p95_ms", lastLatencyP95MsScaled,
                        value -> value.get() / 1000.0d)
                .register(meterRegistry);
        Gauge.builder("wayang.rag.eval.retrieval.last.latency_avg_ms", lastLatencyAvgMsScaled,
                        value -> value.get() / 1000.0d)
                .register(meterRegistry);
        Gauge.builder("wayang.rag.eval.retrieval.guardrail.last.healthy", lastGuardrailHealthy, AtomicLong::get)
                .register(meterRegistry);
        Gauge.builder("wayang.rag.eval.retrieval.guardrail.last.breach_count", lastGuardrailBreachCount, AtomicLong::get)
                .register(meterRegistry);
    }

    public void recordRun(RagRetrievalEvalResponse response) {
        if (response == null) {
            return;
        }
        lastRecallAtKScaled.set(scale(response.recallAtK()));
        lastMrrScaled.set(scale(response.mrr()));
        lastLatencyP95MsScaled.set(scale(response.latencyP95Ms()));
        lastLatencyAvgMsScaled.set(scale(response.latencyAvgMs()));
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("wayang.rag.eval.retrieval.run.count")
                .tag("tenant", safe(response.tenantId()))
                .tag("dataset", safe(response.datasetName()))
                .register(meterRegistry)
                .increment();
        Counter.builder("wayang.rag.eval.retrieval.query.count")
                .tag("tenant", safe(response.tenantId()))
                .tag("dataset", safe(response.datasetName()))
                .register(meterRegistry)
                .increment(Math.max(0, response.queryCount()));
        Counter.builder("wayang.rag.eval.retrieval.hit.count")
                .tag("tenant", safe(response.tenantId()))
                .tag("dataset", safe(response.datasetName()))
                .register(meterRegistry)
                .increment(Math.max(0, response.hitCount()));
        DistributionSummary.builder("wayang.rag.eval.retrieval.recall_at_k")
                .tag("tenant", safe(response.tenantId()))
                .tag("dataset", safe(response.datasetName()))
                .register(meterRegistry)
                .record(Math.max(0.0, response.recallAtK()));
        DistributionSummary.builder("wayang.rag.eval.retrieval.mrr")
                .tag("tenant", safe(response.tenantId()))
                .tag("dataset", safe(response.datasetName()))
                .register(meterRegistry)
                .record(Math.max(0.0, response.mrr()));
        DistributionSummary.builder("wayang.rag.eval.retrieval.latency_p95_ms")
                .tag("tenant", safe(response.tenantId()))
                .tag("dataset", safe(response.datasetName()))
                .register(meterRegistry)
                .record(Math.max(0.0, response.latencyP95Ms()));
        DistributionSummary.builder("wayang.rag.eval.retrieval.latency_avg_ms")
                .tag("tenant", safe(response.tenantId()))
                .tag("dataset", safe(response.datasetName()))
                .register(meterRegistry)
                .record(Math.max(0.0, response.latencyAvgMs()));
    }

    public void recordGuardrail(RagRetrievalEvalGuardrailStatus status) {
        if (status == null) {
            return;
        }
        lastGuardrailHealthy.set(status.healthy() ? 1L : 0L);
        lastGuardrailBreachCount.set(status.breaches() == null ? 0L : status.breaches().size());
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("wayang.rag.eval.retrieval.guardrail.check.count")
                .tag("tenant", safe(status.tenantId()))
                .tag("dataset", safe(status.datasetName()))
                .register(meterRegistry)
                .increment();
        if (!status.healthy()) {
            Counter.builder("wayang.rag.eval.retrieval.guardrail.breach.count")
                    .tag("tenant", safe(status.tenantId()))
                    .tag("dataset", safe(status.datasetName()))
                    .register(meterRegistry)
                    .increment();
        }
    }

    private static long scale(double value) {
        return Math.round(value * 1000.0d);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}

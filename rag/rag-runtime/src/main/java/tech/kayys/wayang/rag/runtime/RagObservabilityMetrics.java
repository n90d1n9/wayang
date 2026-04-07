package tech.kayys.wayang.rag.runtime;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class RagObservabilityMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicLong indexLagMs = new AtomicLong(0L);

    public RagObservabilityMetrics() {
        this((MeterRegistry) null);
    }

    @Inject
    public RagObservabilityMetrics(Instance<MeterRegistry> meterRegistryInstance) {
        this.meterRegistry = meterRegistryInstance != null && meterRegistryInstance.isResolvable()
                ? meterRegistryInstance.get()
                : null;
    }

    public RagObservabilityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initGauge() {
        if (meterRegistry == null) {
            return;
        }
        Gauge.builder("wayang.rag.index.lag.ms", indexLagMs, AtomicLong::get)
                .description("Observed index lag in milliseconds")
                .register(meterRegistry);
    }

    public void recordEmbeddingSuccess(String model, int batchSize, long latencyMs) {
        if (meterRegistry == null) {
            return;
        }
        Timer.builder("wayang.rag.embedding.latency")
                .description("Embedding latency in milliseconds")
                .tag("model", safe(model))
                .publishPercentiles(0.95)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
        DistributionSummary.builder("wayang.rag.embedding.batch.size")
                .description("Embedding batch size")
                .tag("model", safe(model))
                .register(meterRegistry)
                .record(batchSize);
    }

    public void recordEmbeddingFailure(String model) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("wayang.rag.embedding.failure.count")
                .description("Embedding failures")
                .tag("model", safe(model))
                .register(meterRegistry)
                .increment();
    }

    public void recordSearchSuccess(String tenantId, long latencyMs, int resultSize) {
        if (meterRegistry == null) {
            return;
        }
        Timer.builder("wayang.rag.search.latency")
                .description("Search latency in milliseconds")
                .tag("tenant", safe(tenantId))
                .publishPercentiles(0.95)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
        DistributionSummary.builder("wayang.rag.search.result.size")
                .description("Search result size")
                .tag("tenant", safe(tenantId))
                .register(meterRegistry)
                .record(resultSize);
    }

    public void recordSearchFailure(String tenantId) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("wayang.rag.search.failure.count")
                .description("Search failures")
                .tag("tenant", safe(tenantId))
                .register(meterRegistry)
                .increment();
    }

    public void recordIngestion(String tenantId, int documentsIngested, int segmentsCreated, long durationMs) {
        if (meterRegistry == null) {
            return;
        }
        Timer.builder("wayang.rag.ingest.latency")
                .description("Ingestion latency in milliseconds")
                .tag("tenant", safe(tenantId))
                .publishPercentiles(0.95)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        DistributionSummary.builder("wayang.rag.ingest.batch.documents")
                .description("Documents ingested in a batch")
                .tag("tenant", safe(tenantId))
                .register(meterRegistry)
                .record(documentsIngested);
        DistributionSummary.builder("wayang.rag.ingest.batch.segments")
                .description("Segments created in a batch")
                .tag("tenant", safe(tenantId))
                .register(meterRegistry)
                .record(segmentsCreated);
        indexLagMs.set(Math.max(0L, durationMs));
    }

    public long currentIndexLagMs() {
        return indexLagMs.get();
    }

    public RagSloSnapshot snapshot() {
        if (meterRegistry == null) {
            return new RagSloSnapshot(0.0, 0.0, 0.0, 0.0, 0.0, indexLagMs.get(), 0, 0, 0, 0);
        }

        long embeddingSuccess = sumTimerCount("wayang.rag.embedding.latency");
        long embeddingFailure = sumCounterCount("wayang.rag.embedding.failure.count");
        long searchSuccess = sumTimerCount("wayang.rag.search.latency");
        long searchFailure = sumCounterCount("wayang.rag.search.failure.count");

        return new RagSloSnapshot(
                percentile95Ms("wayang.rag.embedding.latency"),
                percentile95Ms("wayang.rag.search.latency"),
                percentile95Ms("wayang.rag.ingest.latency"),
                failureRate(embeddingFailure, embeddingSuccess),
                failureRate(searchFailure, searchSuccess),
                indexLagMs.get(),
                embeddingSuccess,
                embeddingFailure,
                searchSuccess,
                searchFailure);
    }

    private double percentile95Ms(String meterName) {
        return meterRegistry.find(meterName).timers().stream()
                .mapToDouble(this::timerP95Ms)
                .max()
                .orElse(0.0);
    }

    private double timerP95Ms(Timer timer) {
        ValueAtPercentile[] percentiles = timer.takeSnapshot().percentileValues();
        if (percentiles != null) {
            for (ValueAtPercentile percentile : percentiles) {
                if (Math.abs(percentile.percentile() - 0.95) < 0.0001) {
                    return percentile.value(TimeUnit.MILLISECONDS);
                }
            }
        }
        return timer.max(TimeUnit.MILLISECONDS);
    }

    private long sumTimerCount(String meterName) {
        return meterRegistry.find(meterName).timers().stream()
                .mapToLong(Timer::count)
                .sum();
    }

    private long sumCounterCount(String meterName) {
        return (long) meterRegistry.find(meterName).counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    private double failureRate(long failureCount, long successCount) {
        long total = failureCount + successCount;
        if (total <= 0) {
            return 0.0;
        }
        return (double) failureCount / total;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    /**
     * Immutable snapshot of RAG SLO metrics at a point in time.
     */
    public record RagSloSnapshot(
            double embeddingLatencyP95Ms,
            double searchLatencyP95Ms,
            double ingestLatencyP95Ms,
            double embeddingFailureRate,
            double searchFailureRate,
            long indexLagMs,
            long embeddingSuccessCount,
            long embeddingFailureCount,
            long searchSuccessCount,
            long searchFailureCount) {
    }
}

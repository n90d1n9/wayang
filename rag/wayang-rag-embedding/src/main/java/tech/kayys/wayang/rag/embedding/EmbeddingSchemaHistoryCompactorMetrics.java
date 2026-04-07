package tech.kayys.wayang.rag.embedding;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class EmbeddingSchemaHistoryCompactorMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicLong enabledGauge = new AtomicLong(0L);
    private final AtomicLong dryRunGauge = new AtomicLong(0L);
    private final AtomicLong maxEventsGauge = new AtomicLong(0L);
    private final AtomicLong maxAgeDaysGauge = new AtomicLong(0L);
    private final AtomicLong lastCycleStartedEpochSeconds = new AtomicLong(0L);
    private final AtomicLong lastCycleFinishedEpochSeconds = new AtomicLong(0L);
    private final AtomicLong lastCycleTenantsProcessed = new AtomicLong(0L);
    private final AtomicLong lastCycleRemovedCount = new AtomicLong(0L);

    public EmbeddingSchemaHistoryCompactorMetrics() {
        this((MeterRegistry) null);
    }

    @Inject
    public EmbeddingSchemaHistoryCompactorMetrics(Instance<MeterRegistry> meterRegistryInstance) {
        this.meterRegistry = meterRegistryInstance != null && meterRegistryInstance.isResolvable()
                ? meterRegistryInstance.get()
                : null;
    }

    public EmbeddingSchemaHistoryCompactorMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initGauges() {
        if (meterRegistry == null) {
            return;
        }
        Gauge.builder("wayang.rag.embedding.schema.compaction.enabled", enabledGauge, AtomicLong::get).register(meterRegistry);
        Gauge.builder("wayang.rag.embedding.schema.compaction.dry_run", dryRunGauge, AtomicLong::get).register(meterRegistry);
        Gauge.builder("wayang.rag.embedding.schema.compaction.max_events", maxEventsGauge, AtomicLong::get).register(meterRegistry);
        Gauge.builder("wayang.rag.embedding.schema.compaction.max_age_days", maxAgeDaysGauge, AtomicLong::get)
                .register(meterRegistry);
        Gauge.builder("wayang.rag.embedding.schema.compaction.last_cycle.started.epoch_seconds",
                lastCycleStartedEpochSeconds, AtomicLong::get).register(meterRegistry);
        Gauge.builder("wayang.rag.embedding.schema.compaction.last_cycle.finished.epoch_seconds",
                lastCycleFinishedEpochSeconds, AtomicLong::get).register(meterRegistry);
        Gauge.builder("wayang.rag.embedding.schema.compaction.last_cycle.tenants_processed",
                lastCycleTenantsProcessed, AtomicLong::get).register(meterRegistry);
        Gauge.builder("wayang.rag.embedding.schema.compaction.last_cycle.removed_count",
                lastCycleRemovedCount, AtomicLong::get).register(meterRegistry);
    }

    public void recordPolicy(EmbeddingSchemaHistoryCompactorJob.CompactionPolicy policy) {
        enabledGauge.set(policy.enabled() ? 1L : 0L);
        dryRunGauge.set(policy.dryRun() ? 1L : 0L);
        maxEventsGauge.set(Math.max(0, policy.maxEvents()));
        maxAgeDaysGauge.set(Math.max(0, policy.maxAgeDays()));
    }

    public void recordCycleStarted(Instant startedAt) {
        lastCycleStartedEpochSeconds.set(toEpochSeconds(startedAt));
    }

    public void recordCycleFinished(Instant finishedAt, int tenantsProcessed, long removedCount) {
        lastCycleFinishedEpochSeconds.set(toEpochSeconds(finishedAt));
        lastCycleTenantsProcessed.set(Math.max(0, tenantsProcessed));
        lastCycleRemovedCount.set(Math.max(0L, removedCount));
        increment("wayang.rag.embedding.schema.compaction.cycle.count");
        incrementBy("wayang.rag.embedding.schema.compaction.tenants_processed.count", tenantsProcessed);
        incrementBy("wayang.rag.embedding.schema.compaction.removed.count", removedCount);
    }

    public void recordFailure() {
        increment("wayang.rag.embedding.schema.compaction.failure.count");
    }

    private void increment(String meterName) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder(meterName).register(meterRegistry).increment();
    }

    private void incrementBy(String meterName, long amount) {
        if (meterRegistry == null || amount <= 0) {
            return;
        }
        Counter.builder(meterName).register(meterRegistry).increment(amount);
    }

    private static long toEpochSeconds(Instant value) {
        return value == null ? 0L : value.getEpochSecond();
    }
}

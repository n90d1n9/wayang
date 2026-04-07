package tech.kayys.wayang.rag.embedding;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@ApplicationScoped
public class EmbeddingSchemaHistoryCompactorJob {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddingSchemaHistoryCompactorJob.class);

    @Inject
    EmbeddingSchemaAdminService schemaAdminService;

    @Inject
    EmbeddingSchemaHistoryCompactorMetrics metrics;

    private volatile Instant lastCycleStartedAt;
    private volatile Instant lastCycleFinishedAt;
    private volatile String lastError;
    private volatile int lastCycleTenantsProcessed;
    private volatile long lastCycleRemovedCount;
    private final AtomicLong totalCycles = new AtomicLong();
    private final AtomicLong totalTenantsProcessed = new AtomicLong();
    private final AtomicLong totalFailures = new AtomicLong();

    @Scheduled(every = "{rag.runtime.embedding.schema.history.compaction.interval:24h}")
    void scheduledRun() {
        runCompactionCycle(loadPolicy(ConfigProvider.getConfig()));
    }

    void runCompactionCycle(CompactionPolicy policy) {
        if (!policy.enabled()) {
            if (metrics != null) {
                metrics.recordPolicy(policy);
            }
            return;
        }
        lastCycleStartedAt = Instant.now();
        if (metrics != null) {
            metrics.recordPolicy(policy);
            metrics.recordCycleStarted(lastCycleStartedAt);
        }
        lastError = null;
        int cycleTenantsProcessed = 0;
        long cycleRemovedCount = 0L;
        Set<String> tenants = policy.tenants().isEmpty()
                ? schemaAdminService.tenantIdsWithHistory()
                : policy.tenants();
        for (String tenant : tenants) {
            try {
                EmbeddingSchemaHistoryCompactionStatus status = schemaAdminService.compactHistory(
                        tenant,
                        new EmbeddingSchemaHistoryCompactionRequest(
                                policy.maxEvents(),
                                policy.maxAgeDays(),
                                policy.dryRun()));
                LOG.info(
                        "Schema history compaction tenant={} dryRun={} before={} after={} removed={}",
                        tenant,
                        status.dryRun(),
                        status.beforeCount(),
                        status.afterCount(),
                        status.removedCount());
                cycleTenantsProcessed++;
                cycleRemovedCount += Math.max(0, status.removedCount());
            } catch (RuntimeException ex) {
                totalFailures.incrementAndGet();
                if (metrics != null) {
                    metrics.recordFailure();
                }
                lastError = "tenant=" + tenant + ": " + ex.getMessage();
                LOG.error("Failed schema history compaction for tenant={}", tenant, ex);
            }
        }
        lastCycleTenantsProcessed = cycleTenantsProcessed;
        lastCycleRemovedCount = cycleRemovedCount;
        totalCycles.incrementAndGet();
        totalTenantsProcessed.addAndGet(cycleTenantsProcessed);
        lastCycleFinishedAt = Instant.now();
        if (metrics != null) {
            metrics.recordCycleFinished(lastCycleFinishedAt, cycleTenantsProcessed, cycleRemovedCount);
        }
    }

    CompactionPolicy loadPolicy(Config config) {
        boolean enabled = config.getOptionalValue(
                "rag.runtime.embedding.schema.history.compaction.enabled",
                Boolean.class).orElse(false);
        int maxEvents = config.getOptionalValue(
                "rag.runtime.embedding.schema.history.compaction.max-events",
                Integer.class).orElse(200);
        int maxAgeDays = config.getOptionalValue(
                "rag.runtime.embedding.schema.history.compaction.max-age-days",
                Integer.class).orElse(30);
        boolean dryRun = config.getOptionalValue(
                "rag.runtime.embedding.schema.history.compaction.dry-run",
                Boolean.class).orElse(false);
        Set<String> tenants = parseTenantSet(config.getOptionalValue(
                "rag.runtime.embedding.schema.history.compaction.tenants",
                String.class).orElse(""));
        return new CompactionPolicy(enabled, maxEvents, maxAgeDays, dryRun, tenants);
    }

    Set<String> parseTenantSet(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public EmbeddingSchemaHistoryCompactorStatus status() {
        CompactionPolicy policy = loadPolicy(ConfigProvider.getConfig());
        return new EmbeddingSchemaHistoryCompactorStatus(
                policy.enabled(),
                policy.maxEvents(),
                policy.maxAgeDays(),
                policy.dryRun(),
                policy.tenants(),
                lastCycleStartedAt,
                lastCycleFinishedAt,
                totalCycles.get(),
                totalTenantsProcessed.get(),
                totalFailures.get(),
                lastCycleTenantsProcessed,
                lastCycleRemovedCount,
                lastError);
    }

    record CompactionPolicy(
            boolean enabled,
            int maxEvents,
            int maxAgeDays,
            boolean dryRun,
            Set<String> tenants) {
    }
}

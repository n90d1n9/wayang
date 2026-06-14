package tech.kayys.wayang.agent.analytics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Analytics service for tracking skill usage patterns and performance metrics.
 * 
 * Features:
 * - Track skill execution counts and success rates
 * - Monitor performance metrics (latency, throughput)
 * - Identify popular skills and usage trends
 * - Detect anomalies in skill behavior
 * - Provide insights for optimization
 */
@ApplicationScoped
public class SkillAnalyticsService {
    
    private static final Logger LOG = Logger.getLogger(SkillAnalyticsService.class);
    
    @Inject
    MeterRegistry meterRegistry;
    
    // In-memory storage for recent events (for quick analytics)
    private final Map<String, SkillMetrics> skillMetricsCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> tenantUsageCounters = new ConcurrentHashMap<>();
    
    // Metrics
    private Counter totalExecutions;
    private Counter successfulExecutions;
    private Counter failedExecutions;
    private Timer executionTimer;
    private DistributionSummary latencyHistogram;
    
    void init() {
        // Initialize global metrics
        totalExecutions = Counter.builder("wayang.skill.executions.total")
            .description("Total number of skill executions")
            .register(meterRegistry);
        
        successfulExecutions = Counter.builder("wayang.skill.executions.successful")
            .description("Number of successful skill executions")
            .register(meterRegistry);
        
        failedExecutions = Counter.builder("wayang.skill.executions.failed")
            .description("Number of failed skill executions")
            .register(meterRegistry);
        
        executionTimer = Timer.builder("wayang.skill.execution.duration")
            .description("Skill execution duration")
            .register(meterRegistry);
        
        latencyHistogram = DistributionSummary.builder("wayang.skill.execution.latency")
            .description("Skill execution latency distribution")
            .baseUnit("milliseconds")
            .register(meterRegistry);
    }
    
    /**
     * Record a skill usage event.
     */
    public Uni<Void> recordEvent(SkillUsageEvent event) {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                try {
                    switch (event.eventType()) {
                        case EXECUTION_STARTED:
                            handleExecutionStarted(event);
                            break;
                        case EXECUTION_COMPLETED:
                            handleExecutionCompleted(event);
                            break;
                        case EXECUTION_FAILED:
                            handleExecutionFailed(event);
                            break;
                        case CACHE_HIT:
                            recordCacheHit(event);
                            break;
                        case CACHE_MISS:
                            recordCacheMiss(event);
                            break;
                        default:
                            LOG.debugf("Unhandled event type: %s for skill %s", 
                                event.eventType(), event.skillId());
                    }
                } catch (Exception e) {
                    LOG.errorf(e, "Failed to record analytics event: %s", event.eventId());
                }
            });
    }
    
    private void handleExecutionStarted(SkillUsageEvent event) {
        totalExecutions.increment();
        
        SkillMetrics metrics = getOrCreateMetrics(event.skillId());
        metrics.executionStartTimes.put(event.eventId(), Instant.now());
        metrics.runningCount.incrementAndGet();
        
        LOG.debugf("Skill execution started: %s (tenant: %s, user: %s)", 
            event.skillId(), event.tenantId(), event.userId());
    }
    
    private void handleExecutionCompleted(SkillUsageEvent event) {
        successfulExecutions.increment();
        
        long duration = event.durationMs();
        executionTimer.record(Duration.ofMillis(duration));
        latencyHistogram.record(duration);
        
        SkillMetrics metrics = getOrCreateMetrics(event.skillId());
        metrics.recordSuccess(duration);
        metrics.runningCount.decrementAndGet();
        
        // Update tenant counter
        tenantUsageCounters.computeIfAbsent(event.tenantId(), k -> new AtomicLong())
            .incrementAndGet();
        
        LOG.debugf("Skill execution completed: %s in %dms", event.skillId(), duration);
    }
    
    private void handleExecutionFailed(SkillUsageEvent event) {
        failedExecutions.increment();
        
        SkillMetrics metrics = getOrCreateMetrics(event.skillId());
        metrics.recordFailure();
        metrics.runningCount.decrementAndGet();
        
        LOG.warnf("Skill execution failed: %s - %s", event.skillId(), event.errorMessage());
    }
    
    private void recordCacheHit(SkillUsageEvent event) {
        SkillMetrics metrics = getOrCreateMetrics(event.skillId());
        metrics.cacheHits.incrementAndGet();
    }
    
    private void recordCacheMiss(SkillUsageEvent event) {
        SkillMetrics metrics = getOrCreateMetrics(event.skillId());
        metrics.cacheMisses.incrementAndGet();
    }
    
    private SkillMetrics getOrCreateMetrics(String skillId) {
        return skillMetricsCache.computeIfAbsent(skillId, k -> new SkillMetrics());
    }
    
    /**
     * Get analytics for a specific skill.
     */
    public Uni<SkillAnalytics> getSkillAnalytics(String skillId) {
        return Uni.createFrom().item(() -> {
            SkillMetrics metrics = skillMetricsCache.get(skillId);
            if (metrics == null) {
                return SkillAnalytics.empty(skillId);
            }
            return metrics.toAnalytics(skillId);
        });
    }
    
    /**
     * Get top N most used skills.
     */
    public Uni<TopSkillsReport> getTopSkills(int limit) {
        return Uni.createFrom().item(() -> {
            return skillMetricsCache.entrySet().stream()
                .filter(e -> e.getValue().totalExecutions.get() > 0)
                .sorted((a, b) -> Long.compare(
                    b.getValue().totalExecutions.get(),
                    a.getValue().totalExecutions.get()))
                .limit(limit)
                .map(e -> new TopSkillEntry(
                    e.getKey(),
                    e.getValue().totalExecutions.get(),
                    e.getValue().successfulExecutions.get(),
                    e.getValue().calculateSuccessRate(),
                    e.getValue().averageLatencyMs()
                ))
                .collect(Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> new TopSkillsReport(list, Instant.now())
                ));
        });
    }
    
    /**
     * Get tenant usage statistics.
     */
    public Uni<TenantUsageStats> getTenantUsage(String tenantId) {
        return Uni.createFrom().item(() -> {
            AtomicLong counter = tenantUsageCounters.get(tenantId);
            long count = counter != null ? counter.get() : 0;
            
            return new TenantUsageStats(
                tenantId,
                count,
                skillMetricsCache.size(),
                Instant.now()
            );
        });
    }
    
    /**
     * Clear analytics cache for a skill.
     */
    public void clearCache(String skillId) {
        skillMetricsCache.remove(skillId);
        LOG.infof("Cleared analytics cache for skill: %s", skillId);
    }
    
    /**
     * Clear all analytics cache.
     */
    public void clearAllCache() {
        skillMetricsCache.clear();
        tenantUsageCounters.clear();
        LOG.info("Cleared all analytics cache");
    }
    
    // Inner classes for metrics storage
    
    private static class SkillMetrics {
        final AtomicLong totalExecutions = new AtomicLong();
        final AtomicLong successfulExecutions = new AtomicLong();
        final AtomicLong failedExecutions = new AtomicLong();
        final AtomicLong runningCount = new AtomicLong();
        final AtomicLong cacheHits = new AtomicLong();
        final AtomicLong cacheMisses = new AtomicLong();
        final AtomicLong totalLatencyMs = new AtomicLong();
        final AtomicLong latencyCount = new AtomicLong();
        final Map<String, Instant> executionStartTimes = new ConcurrentHashMap<>();
        
        synchronized void recordSuccess(long durationMs) {
            totalExecutions.incrementAndGet();
            successfulExecutions.incrementAndGet();
            totalLatencyMs.addAndGet(durationMs);
            latencyCount.incrementAndGet();
        }
        
        synchronized void recordFailure() {
            totalExecutions.incrementAndGet();
            failedExecutions.incrementAndGet();
        }
        
        double calculateSuccessRate() {
            long total = totalExecutions.get();
            if (total == 0) return 0.0;
            return (double) successfulExecutions.get() / total * 100.0;
        }
        
        double averageLatencyMs() {
            long count = latencyCount.get();
            if (count == 0) return 0.0;
            return (double) totalLatencyMs.get() / count;
        }
        
        SkillAnalytics toAnalytics(String skillId) {
            return new SkillAnalytics(
                skillId,
                totalExecutions.get(),
                successfulExecutions.get(),
                failedExecutions.get(),
                calculateSuccessRate(),
                averageLatencyMs(),
                cacheHits.get(),
                cacheMisses.get(),
                runningCount.get(),
                Instant.now()
            );
        }
    }
}

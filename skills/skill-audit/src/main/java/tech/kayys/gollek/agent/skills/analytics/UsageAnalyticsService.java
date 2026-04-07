package tech.kayys.gollek.agent.skills.analytics;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Usage analytics service for tracking skill usage patterns and performance.
 *
 * <p>Features:
 * <ul>
 *   <li>Execution tracking</li>
 *   <li>Performance metrics</li>
 *   <li>User analytics</li>
 *   <li>Popular skills tracking</li>
 *   <li>Error tracking</li>
 *   <li>Token usage tracking</li>
 * </ul>
 */
@ApplicationScoped
public class UsageAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(UsageAnalyticsService.class);

    private final Map<String, SkillMetrics> skillMetrics;
    private final Map<String, Set<String>> skillUsers;
    private final Map<String, AtomicLong> dailyExecutions;

    public UsageAnalyticsService() {
        this.skillMetrics = new ConcurrentHashMap<>();
        this.skillUsers = new ConcurrentHashMap<>();
        this.dailyExecutions = new ConcurrentHashMap<>();
    }

    /**
     * Record skill execution.
     */
    public void recordExecution(String skillId, String userId, long durationMs, boolean success) {
        recordExecution(skillId, userId, durationMs, success, 0);
    }

    /**
     * Record skill execution with token usage.
     */
    public void recordExecution(String skillId, String userId, long durationMs, 
                               boolean success, int tokensUsed) {
        // Update skill metrics
        skillMetrics.compute(skillId, (id, metrics) -> {
            if (metrics == null) {
                metrics = new SkillMetrics();
            }
            return metrics.recordExecution(durationMs, success, tokensUsed);
        });

        // Track unique users
        skillUsers.computeIfAbsent(skillId, k -> ConcurrentHashMap.newKeySet())
                  .add(userId);

        // Track daily executions
        String dayKey = Instant.now().getEpochSecond() / 86400;
        dailyExecutions.computeIfAbsent(dayKey, k -> new AtomicLong(0))
                       .incrementAndGet();

        log.debug("Recorded execution for skill: {} (duration: {}ms, success: {})", 
                  skillId, durationMs, success);
    }

    /**
     * Get usage metrics for a skill.
     */
    public SkillUsageMetrics getSkillMetrics(String skillId) {
        SkillMetrics metrics = skillMetrics.get(skillId);
        if (metrics == null) {
            return SkillUsageMetrics.empty(skillId);
        }

        int uniqueUsers = skillUsers.getOrDefault(skillId, Set.of()).size();

        return new SkillUsageMetrics(
            skillId,
            metrics.totalExecutions(),
            metrics.successfulExecutions(),
            metrics.failedExecutions(),
            metrics.avgExecutionTimeMs(),
            metrics.lastExecutionTime(),
            uniqueUsers,
            metrics.totalTokens()
        );
    }

    /**
     * Get most popular skills.
     */
    public List<SkillUsageMetrics> getPopularSkills(int limit) {
        return skillMetrics.entrySet().stream()
                .map(entry -> getSkillMetrics(entry.getKey()))
                .sorted(Comparator.comparingLong(SkillUsageMetrics::totalExecutions).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Get slowest skills.
     */
    public List<SkillUsageMetrics> getSlowestSkills(int limit) {
        return skillMetrics.entrySet().stream()
                .map(entry -> getSkillMetrics(entry.getKey()))
                .filter(m -> m.totalExecutions() > 0)
                .sorted(Comparator.comparingDouble(SkillUsageMetrics::avgExecutionTimeMs).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Get skills with highest failure rate.
     */
    public List<SkillUsageMetrics> getHighestFailureRateSkills(int limit) {
        return skillMetrics.entrySet().stream()
                .map(entry -> getSkillMetrics(entry.getKey()))
                .filter(m -> m.totalExecutions() > 10) // Minimum executions
                .sorted(Comparator.comparingDouble(SkillUsageMetrics::getFailureRate).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Get total executions today.
     */
    public long getTodayExecutions() {
        String dayKey = Instant.now().getEpochSecond() / 86400;
        return dailyExecutions.getOrDefault(dayKey, new AtomicLong(0)).get();
    }

    /**
     * Get total skills tracked.
     */
    public int getTotalSkillsTracked() {
        return skillMetrics.size();
    }

    /**
     * Get overall statistics.
     */
    public AnalyticsStats getOverallStats() {
        long totalExecutions = skillMetrics.values().stream()
                .mapToLong(SkillMetrics::totalExecutions)
                .sum();

        long totalSuccess = skillMetrics.values().stream()
                .mapToLong(SkillMetrics::successfulExecutions)
                .sum();

        long totalFailures = skillMetrics.values().stream()
                .mapToLong(SkillMetrics::failedExecutions)
                .sum();

        long totalTokens = skillMetrics.values().stream()
                .mapToLong(SkillMetrics::totalTokens)
                .sum();

        int totalUsers = skillUsers.values().stream()
                .mapToInt(Set::size)
                .sum();

        return new AnalyticsStats(
            totalExecutions,
            totalSuccess,
            totalFailures,
            totalTokens,
            totalUsers,
            skillMetrics.size()
        );
    }

    /**
     * Clear metrics for a skill.
     */
    public void clearMetrics(String skillId) {
        skillMetrics.remove(skillId);
        skillUsers.remove(skillId);
        log.info("Cleared metrics for skill: {}", skillId);
    }

    /**
     * Clear all metrics.
     */
    public void clearAllMetrics() {
        skillMetrics.clear();
        skillUsers.clear();
        dailyExecutions.clear();
        log.info("Cleared all metrics");
    }

    /**
     * Internal skill metrics tracker.
     */
    private static class SkillMetrics {
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong failedExecutions = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong totalTokens = new AtomicLong(0);
        private volatile Instant lastExecutionTime = Instant.now();

        public SkillMetrics recordExecution(long durationMs, boolean success, int tokensUsed) {
            totalExecutions.incrementAndGet();
            totalDuration.addAndGet(durationMs);
            totalTokens.addAndGet(tokensUsed);
            lastExecutionTime = Instant.now();

            if (success) {
                successfulExecutions.incrementAndGet();
            } else {
                failedExecutions.incrementAndGet();
            }

            return this;
        }

        public long totalExecutions() {
            return totalExecutions.get();
        }

        public long successfulExecutions() {
            return successfulExecutions.get();
        }

        public long failedExecutions() {
            return failedExecutions.get();
        }

        public double avgExecutionTimeMs() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalDuration.get() / total : 0.0;
        }

        public Instant lastExecutionTime() {
            return lastExecutionTime;
        }

        public long totalTokens() {
            return totalTokens.get();
        }
    }

    /**
     * Overall analytics statistics.
     */
    public record AnalyticsStats(
        long totalExecutions,
        long totalSuccess,
        long totalFailures,
        long totalTokens,
        int totalUsers,
        int totalSkills
    ) {
        public double getOverallSuccessRate() {
            long total = totalExecutions;
            return total > 0 ? (double) totalSuccess / total : 0.0;
        }

        public double getOverallFailureRate() {
            long total = totalExecutions;
            return total > 0 ? (double) totalFailures / total : 0.0;
        }
    }
}

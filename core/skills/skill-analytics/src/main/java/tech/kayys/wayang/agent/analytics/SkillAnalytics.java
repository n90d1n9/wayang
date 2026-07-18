package tech.kayys.wayang.agent.analytics;

import java.time.Instant;

/**
 * Analytics data for a skill.
 */
public record SkillAnalytics(
    String skillId,
    long totalExecutions,
    long successfulExecutions,
    long failedExecutions,
    double successRate,
    double averageLatencyMs,
    long cacheHits,
    long cacheMisses,
    long runningCount,
    Instant lastUpdated
) {
    public static SkillAnalytics empty(String skillId) {
        return new SkillAnalytics(skillId, 0, 0, 0, 0.0, 0.0, 0, 0, 0, Instant.now());
    }
    
    public double cacheHitRate() {
        long total = cacheHits + cacheMisses;
        if (total == 0) return 0.0;
        return (double) cacheHits / total * 100.0;
    }
}

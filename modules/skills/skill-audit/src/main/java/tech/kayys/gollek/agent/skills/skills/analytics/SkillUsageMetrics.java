package tech.kayys.wayang.agent.skills.analytics;

import java.time.Instant;

/**
 * Aggregate runtime metrics for a skill.
 */
public record SkillUsageMetrics(
        String skillId,
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions,
        double averageDurationMs,
        long totalDurationMs,
        Instant firstExecutedAt,
        Instant lastExecutedAt) {

    public double failureRate() {
        return totalExecutions == 0 ? 0.0 : (double) failedExecutions / totalExecutions;
    }

    public double successRate() {
        return totalExecutions == 0 ? 0.0 : (double) successfulExecutions / totalExecutions;
    }

    public static SkillUsageMetrics empty(String skillId) {
        return new SkillUsageMetrics(skillId, 0, 0, 0, 0.0, 0, null, null);
    }
}

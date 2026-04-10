package tech.kayys.gollek.agent.skills.analytics;

import java.time.Instant;

/**
 * Usage metrics for a skill.
 *
 * @param skillId skill identifier
 * @param totalExecutions total number of executions
 * @param successfulExecutions number of successful executions
 * @param failedExecutions number of failed executions
 * @param avgExecutionTimeMs average execution time in milliseconds
 * @param lastExecutionTime timestamp of last execution
 * @param uniqueUsers number of unique users
 * @param totalTokens total tokens consumed
 */
public record SkillUsageMetrics(
    String skillId,
    long totalExecutions,
    long successfulExecutions,
    long failedExecutions,
    double avgExecutionTimeMs,
    Instant lastExecutionTime,
    int uniqueUsers,
    long totalTokens
) {
    public static SkillUsageMetrics empty(String skillId) {
        return new SkillUsageMetrics(
            skillId, 0, 0, 0, 0.0, Instant.now(), 0, 0
        );
    }

    public double getSuccessRate() {
        long total = totalExecutions;
        return total > 0 ? (double) successfulExecutions / total : 0.0;
    }

    public double getFailureRate() {
        long total = totalExecutions;
        return total > 0 ? (double) failedExecutions / total : 0.0;
    }
}

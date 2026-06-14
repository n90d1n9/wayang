package tech.kayys.wayang.agent.analytics;

/**
 * Entry in the top skills report.
 */
public record TopSkillEntry(
    String skillId,
    long totalExecutions,
    long successfulExecutions,
    double successRate,
    double averageLatencyMs
) {}

package tech.kayys.wayang.agent.analytics;

import java.time.Instant;
import java.util.List;

/**
 * Report of top skills by usage.
 */
public record TopSkillsReport(
    List<TopSkillEntry> topSkills,
    Instant generatedAt
) {}

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

/**
 * Tenant usage statistics.
 */
public record TenantUsageStats(
    String tenantId,
    long totalExecutions,
    long uniqueSkillsUsed,
    Instant lastUpdated
) {}

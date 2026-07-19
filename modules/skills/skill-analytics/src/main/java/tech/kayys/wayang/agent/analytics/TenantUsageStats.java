package tech.kayys.wayang.agent.analytics;

import java.time.Instant;

/**
 * Tenant usage statistics.
 */
public record TenantUsageStats(
    String tenantId,
    long totalExecutions,
    long uniqueSkillsUsed,
    Instant lastUpdated
) {}

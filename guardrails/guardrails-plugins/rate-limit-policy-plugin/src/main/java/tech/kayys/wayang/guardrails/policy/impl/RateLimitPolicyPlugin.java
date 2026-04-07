package tech.kayys.wayang.guardrails.policy.impl;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.guardrails.plugin.api.NodeContext;
import tech.kayys.wayang.guardrails.plugin.api.CheckPhase;
import tech.kayys.wayang.guardrails.plugin.api.GuardrailPolicyPlugin;
import tech.kayys.wayang.guardrails.plugin.api.PolicyCheckResult;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limit Policy Plugin implementation.
 * Implements rate limiting based on tenant and user IDs.
 */
@ApplicationScoped
public class RateLimitPolicyPlugin implements GuardrailPolicyPlugin {

    // In-memory storage for rate limiting - in production, this would use Redis or
    // similar
    private final Map<String, RequestCount> requestCounts = new ConcurrentHashMap<>();

    // Default rate limits (requests per minute)
    private static final int DEFAULT_TENANT_LIMIT = 100;
    private static final int DEFAULT_USER_LIMIT = 10;

    @Override
    public String id() {
        return "rate-limit-policy-plugin";
    }

    @Override
    public String name() {
        return "Rate Limit Policy Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Implements rate limiting for tenants and users";
    }

    @Override
    public Uni<PolicyCheckResult> evaluate(NodeContext context) {
        String tenantId = context.tenantId();
        String userId = context.metadata().userId();

        // Check tenant-level rate limit
        boolean tenantAllowed = checkTenantRateLimit(tenantId);
        if (!tenantAllowed) {
            return Uni.createFrom().item(new PolicyCheckResult(
                    id(),
                    name(),
                    false,
                    "Tenant rate limit exceeded"));
        }

        // Check user-level rate limit
        boolean userAllowed = checkUserRateLimit(tenantId, userId);
        if (!userAllowed) {
            return Uni.createFrom().item(new PolicyCheckResult(
                    id(),
                    name(),
                    false,
                    "User rate limit exceeded"));
        }

        // Update counters
        incrementRequestCount(tenantId, userId);

        return Uni.createFrom().item(new PolicyCheckResult(
                id(),
                name(),
                true,
                null));
    }

    private boolean checkTenantRateLimit(String tenantId) {
        String key = "tenant:" + tenantId;
        RequestCount count = requestCounts.computeIfAbsent(key, k -> new RequestCount());

        // Reset counter if it's a new minute
        long currentMinute = Instant.now().getEpochSecond() / 60;
        if (count.minute != currentMinute) {
            count.reset(currentMinute);
        }

        int limit = getTenantLimit(tenantId);
        return count.count < limit;
    }

    private boolean checkUserRateLimit(String tenantId, String userId) {
        String key = "user:" + tenantId + ":" + userId;
        RequestCount count = requestCounts.computeIfAbsent(key, k -> new RequestCount());

        // Reset counter if it's a new minute
        long currentMinute = Instant.now().getEpochSecond() / 60;
        if (count.minute != currentMinute) {
            count.reset(currentMinute);
        }

        int limit = getUserLimit(userId);
        return count.count < limit;
    }

    private void incrementRequestCount(String tenantId, String userId) {
        // Increment tenant counter
        String tenantKey = "tenant:" + tenantId;
        RequestCount tenantCount = requestCounts.computeIfAbsent(tenantKey, k -> new RequestCount());
        tenantCount.increment();

        // Increment user counter
        String userKey = "user:" + tenantId + ":" + userId;
        RequestCount userCount = requestCounts.computeIfAbsent(userKey, k -> new RequestCount());
        userCount.increment();
    }

    private int getTenantLimit(String tenantId) {
        // In a real implementation, this would come from a configuration or database
        // For now, we'll use a default value
        return DEFAULT_TENANT_LIMIT;
    }

    private int getUserLimit(String userId) {
        // In a real implementation, this would come from a configuration or database
        // For now, we'll use a default value
        return DEFAULT_USER_LIMIT;
    }

    @Override
    public String getCategory() {
        return "rate_limit";
    }

    @Override
    public CheckPhase[] applicablePhases() {
        return new CheckPhase[] { CheckPhase.PRE_EXECUTION };
    }

    // Inner class to track request counts
    private static class RequestCount {
        volatile int count = 0;
        volatile long minute = Instant.now().getEpochSecond() / 60;

        synchronized void increment() {
            count++;
        }

        synchronized void reset(long newMinute) {
            count = 0;
            minute = newMinute;
        }
    }
}
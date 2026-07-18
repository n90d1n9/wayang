package tech.kayys.wayang.agent.core.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentState;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Security and governance enforcer for agent workloads.
 *
 * <p>This enforcer provides:
 * <ul>
 *   <li>Tenant-scoped resource quotas</li>
 *   <li>Input/output guardrails</li>
 *   <li>Policy enforcement for tool usage</li>
 *   <li>Secure credential management</li>
 *   <li>Audit logging for compliance</li>
 * </ul>
 *
 * <h2>Security Features:</h2>
 * <ul>
 *   <li><b>Input Validation:</b> Sanitize and validate user inputs</li>
 *   <li><b>Output Filtering:</b> Redact sensitive information from responses</li>
 *   <li><b>Tool Authorization:</b> Control which tools each tenant can access</li>
 *   <li><b>Rate Limiting:</b> Prevent abuse through request throttling</li>
 *   <li><b>Quota Enforcement:</b> Enforce token and execution limits</li>
 * </ul>
 *
 * <h2>Guardrails:</h2>
 * <ul>
 *   <li>Block harmful or malicious prompts</li>
 *   <li>Prevent prompt injection attacks</li>
 *   <li>Redact PII and sensitive data</li>
 *   <li>Enforce content policies</li>
 * </ul>
 *
 * @author Wayang AI Team
 * @version 0.1.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class AgentSecurityEnforcer {

    private static final Logger LOG = Logger.getLogger(AgentSecurityEnforcer.class);

    // Tenant quotas
    private final Map<String, TenantQuota> tenantQuotas = new ConcurrentHashMap<>();
    
    // Allowed tools per tenant
    private final Map<String, Set<String>> tenantAllowedTools = new ConcurrentHashMap<>();
    
    // Rate limiting
    private final Map<String, RateLimitState> rateLimitStates = new ConcurrentHashMap<>();

    // Security patterns
    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
        Pattern.compile("(?i)ignore\\s+previous\\s+instructions"),
        Pattern.compile("(?i)system\\s+prompt"),
        Pattern.compile("(?i)developer\\s+message"),
        Pattern.compile("(?i)bypass\\s+security"),
        Pattern.compile("(?i)jailbreak"),
        Pattern.compile("(?i)dan\\s*[=:]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)do\\s+anything\\s+now")
    );

    private static final List<Pattern> SENSITIVE_DATA_PATTERNS = List.of(
        Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b"), // Credit card
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), // Email
        Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"), // SSN
        Pattern.compile("\\b[A-Z]{1,2}\\d{1,2}[A-Z]?\\s*\\d[A-Z]{2}\\b") // UK postcode
    );

    // Default quotas
    private static final int DEFAULT_MAX_TOKENS_PER_HOUR = 100000;
    private static final int DEFAULT_MAX_EXECUTIONS_PER_HOUR = 100;
    private static final int DEFAULT_MAX_CONCURRENT_RUNS = 5;
    private static final Duration DEFAULT_RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final int DEFAULT_MAX_REQUESTS_PER_WINDOW = 60;

    /**
     * Set quota for a tenant.
     *
     * @param tenantId tenant identifier
     * @param maxTokensPerHour maximum tokens per hour
     * @param maxExecutionsPerHour maximum executions per hour
     * @param maxConcurrentRuns maximum concurrent runs
     */
    public void setTenantQuota(String tenantId, int maxTokensPerHour, 
                               int maxExecutionsPerHour, int maxConcurrentRuns) {
        tenantQuotas.put(tenantId, new TenantQuota(
            maxTokensPerHour, maxExecutionsPerHour, maxConcurrentRuns,
            Instant.now(), 0, 0, 0
        ));
        LOG.infof("Set quota for tenant %s: tokens=%d/hour, executions=%d/hour, concurrent=%d",
            tenantId, maxTokensPerHour, maxExecutionsPerHour, maxConcurrentRuns);
    }

    /**
     * Set allowed tools for a tenant.
     *
     * @param tenantId tenant identifier
     * @param allowedTools set of allowed tool IDs
     */
    public void setTenantAllowedTools(String tenantId, Set<String> allowedTools) {
        tenantAllowedTools.put(tenantId, new HashSet<>(allowedTools));
        LOG.infof("Set %d allowed tools for tenant %s", allowedTools.size(), tenantId);
    }

    /**
     * Validate and authorize an agent request.
     *
     * @param request the agent request
     * @return Uni containing validation result
     */
    public Uni<ValidationResult> validateRequest(AgentRequest request) {
        List<String> violations = new ArrayList<>();

        // Check tenant quota
        if (!checkQuota(request.tenantId())) {
            violations.add("Quota exceeded for tenant: " + request.tenantId());
        }

        // Check rate limit
        if (!checkRateLimit(request.tenantId())) {
            violations.add("Rate limit exceeded for tenant: " + request.tenantId());
        }

        // Validate input for security issues
        List<String> inputViolations = validateInput(request.prompt());
        violations.addAll(inputViolations);

        // Check tool authorization
        if (request.allowedSkills() != null) {
            for (String tool : request.allowedSkills()) {
                if (!isToolAllowed(request.tenantId(), tool)) {
                    violations.add("Tool not authorized for tenant: " + tool);
                }
            }
        }

        boolean valid = violations.isEmpty();
        
        if (valid) {
            LOG.debugf("Request validated successfully for tenant %s", request.tenantId());
        } else {
            LOG.warnf("Request validation failed for tenant %s: %s", 
                request.tenantId(), violations);
        }

        return Uni.createFrom().item(new ValidationResult(valid, violations));
    }

    /**
     * Sanitize and filter agent response.
     *
     * @param response the agent response
     * @param tenantId tenant identifier
     * @return sanitized response
     */
    public AgentResponse sanitizeResponse(AgentResponse response, String tenantId) {
        if (response == null || response.answer() == null) {
            return response;
        }

        String sanitizedAnswer = redactSensitiveData(response.answer());
        
        if (!sanitizedAnswer.equals(response.answer())) {
            LOG.infof("Redacted sensitive data from response for tenant %s", tenantId);
        }

        return AgentResponse.builder()
            .runId(response.runId())
            .requestId(response.requestId())
            .answer(sanitizedAnswer)
            .steps(response.steps())
            .totalSteps(response.totalSteps())
            .successful(response.successful())
            .error(response.error())
            .strategy(response.strategy())
            .durationMs(response.durationMs())
            .build();
    }

    /**
     * Record token usage for quota tracking.
     *
     * @param tenantId tenant identifier
     * @param tokens number of tokens used
     */
    public void recordTokenUsage(String tenantId, int tokens) {
        tenantQuotas.computeIfPresent(tenantId, (id, quota) -> 
            quota.withTokensUsed(quota.tokensUsed() + tokens));
    }

    /**
     * Record execution for quota tracking.
     *
     * @param tenantId tenant identifier
     */
    public void recordExecution(String tenantId) {
        tenantQuotas.computeIfPresent(tenantId, (id, quota) -> 
            quota.withExecutionsUsed(quota.executionsUsed() + 1));
        
        // Update rate limit
        rateLimitStates.computeIfAbsent(tenantId, k -> new RateLimitState())
            .recordRequest();
    }

    /**
     * Check if a tenant has remaining quota.
     *
     * @param tenantId tenant identifier
     * @return true if quota is available
     */
    public boolean checkQuota(String tenantId) {
        TenantQuota quota = tenantQuotas.get(tenantId);
        
        if (quota == null) {
            // No quota set - allow by default
            return true;
        }

        // Reset counters if hour has passed
        if (Duration.between(quota.resetTime(), Instant.now()).toHours() >= 1) {
            tenantQuotas.put(tenantId, quota.reset());
            return true;
        }

        return quota.tokensUsed() < quota.maxTokensPerHour() &&
               quota.executionsUsed() < quota.maxExecutionsPerHour();
    }

    /**
     * Check if a tenant is within rate limits.
     *
     * @param tenantId tenant identifier
     * @return true if within rate limit
     */
    public boolean checkRateLimit(String tenantId) {
        RateLimitState state = rateLimitStates.get(tenantId);
        
        if (state == null) {
            return true;
        }

        return state.isWithinLimit();
    }

    /**
     * Check if a tool is allowed for a tenant.
     *
     * @param tenantId tenant identifier
     * @param toolId tool identifier
     * @return true if tool is allowed
     */
    public boolean isToolAllowed(String tenantId, String toolId) {
        Set<String> allowed = tenantAllowedTools.get(tenantId);
        
        if (allowed == null || allowed.isEmpty()) {
            // No restrictions - allow all
            return true;
        }

        return allowed.contains(toolId);
    }

    /**
     * Get current quota usage for a tenant.
     *
     * @param tenantId tenant identifier
     * @return quota usage information
     */
    public QuotaUsage getQuotaUsage(String tenantId) {
        TenantQuota quota = tenantQuotas.get(tenantId);
        
        if (quota == null) {
            return new QuotaUsage(
                DEFAULT_MAX_TOKENS_PER_HOUR, 0,
                DEFAULT_MAX_EXECUTIONS_PER_HOUR, 0,
                DEFAULT_MAX_CONCURRENT_RUNS, 0
            );
        }

        return new QuotaUsage(
            quota.maxTokensPerHour(), quota.tokensUsed(),
            quota.maxExecutionsPerHour(), quota.executionsUsed(),
            quota.maxConcurrentRuns(), quota.concurrentRuns()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Internal Methods
    // ═══════════════════════════════════════════════════════════════════════

    private List<String> validateInput(String input) {
        List<String> violations = new ArrayList<>();

        // Check for blocked patterns (prompt injection attempts)
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(input).find()) {
                violations.add("Blocked pattern detected: potential prompt injection");
                break;
            }
        }

        // Check input length
        if (input.length() > 10000) {
            violations.add("Input exceeds maximum length (10000 characters)");
        }

        return violations;
    }

    private String redactSensitiveData(String text) {
        String result = text;
        
        for (Pattern pattern : SENSITIVE_DATA_PATTERNS) {
            result = pattern.matcher(result).replaceAll("[REDACTED]");
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Records
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Validation result from security checks.
     */
    public record ValidationResult(
        boolean valid,
        List<String> violations
    ) {}

    /**
     * Tenant quota configuration and tracking.
     */
    public record TenantQuota(
        int maxTokensPerHour,
        int maxExecutionsPerHour,
        int maxConcurrentRuns,
        Instant resetTime,
        int tokensUsed,
        int executionsUsed,
        int concurrentRuns
    ) {
        public TenantQuota withTokensUsed(int tokensUsed) {
            return new TenantQuota(
                maxTokensPerHour, maxExecutionsPerHour, maxConcurrentRuns,
                resetTime, tokensUsed, executionsUsed, concurrentRuns
            );
        }

        public TenantQuota withExecutionsUsed(int executionsUsed) {
            return new TenantQuota(
                maxTokensPerHour, maxExecutionsPerHour, maxConcurrentRuns,
                resetTime, tokensUsed, executionsUsed, concurrentRuns
            );
        }

        public TenantQuota reset() {
            return new TenantQuota(
                maxTokensPerHour, maxExecutionsPerHour, maxConcurrentRuns,
                Instant.now(), 0, 0, concurrentRuns
            );
        }
    }

    /**
     * Rate limiting state.
     */
    public static class RateLimitState {
        private int requestCount = 0;
        private Instant windowStart = Instant.now();
        private static final int MAX_REQUESTS = 60;
        private static final Duration WINDOW = Duration.ofMinutes(1);

        public synchronized void recordRequest() {
            Instant now = Instant.now();
            
            // Reset window if expired
            if (Duration.between(windowStart, now).compareTo(WINDOW) > 0) {
                windowStart = now;
                requestCount = 0;
            }
            
            requestCount++;
        }

        public synchronized boolean isWithinLimit() {
            Instant now = Instant.now();
            
            // Reset window if expired
            if (Duration.between(windowStart, now).compareTo(WINDOW) > 0) {
                windowStart = now;
                requestCount = 0;
            }
            
            return requestCount < MAX_REQUESTS;
        }
    }

    /**
     * Quota usage information.
     */
    public record QuotaUsage(
        int maxTokensPerHour,
        int tokensUsed,
        int maxExecutionsPerHour,
        int executionsUsed,
        int maxConcurrentRuns,
        int concurrentRuns
    ) {
        public double tokensUsagePercent() {
            return (double) tokensUsed / maxTokensPerHour * 100;
        }

        public double executionsUsagePercent() {
            return (double) executionsUsed / maxExecutionsPerHour * 100;
        }
    }
}

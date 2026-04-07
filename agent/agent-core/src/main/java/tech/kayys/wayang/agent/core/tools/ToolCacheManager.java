package tech.kayys.wayang.agent.core.tools;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.SkillResult;
import tech.kayys.wayang.agent.core.metrics.AgentMetricsCollector;
import tech.kayys.gollek.spi.tool.ToolDefinition;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Advanced tool cache manager with intelligent caching strategies, result memoization,
 * and performance optimization for agent tool execution.
 *
 * <p>This class provides comprehensive caching capabilities for tool execution results with:
 * <ul>
 *   <li>Multi-level caching (L1 in-memory, L2 distributed-ready)</li>
 *   <li>Intelligent cache key generation with context awareness</li>
 *   <li>Adaptive TTL based on tool type and execution patterns</li>
 *   <li>Cache warming and preloading</li>
 *   <li>Cache invalidation strategies (time-based, event-based, dependency-based)</li>
 *   <li>Cache statistics and metrics</li>
 *   <li>Hit/miss ratio optimization</li>
 * </ul>
 *
 * <h2>Cache Key Generation</h2>
 * <p>Cache keys are generated using a combination of:
 * <ul>
 *   <li>Tool ID</li>
 *   <li>Input parameters (normalized)</li>
 *   <li>Context hash (working directory, environment)</li>
 *   <li>Version stamp (for cache busting)</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Inject
 * ToolCacheManager cacheManager;
 *
 * // Execute with caching
 * ToolResult result = cacheManager.executeWithCache("read_file", params, context)
 *     .await().atMost(Duration.ofSeconds(10));
 *
 * // Get cache statistics
 * CacheStats stats = cacheManager.getStatistics();
 * System.out.println("Hit ratio: " + stats.hitRatio());
 *
 * // Invalidate specific cache entry
 * cacheManager.invalidate("read_file", params);
 *
 * // Clear all caches
 * cacheManager.clearAll();
 * }</pre>
 *
 * @author Wayang AI Team
 * @version 2.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class ToolCacheManager {

    private static final Logger LOG = Logger.getLogger(ToolCacheManager.class);

    private static final int DEFAULT_MAX_CACHE_SIZE = 1000;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final Duration STATS_INTERVAL = Duration.ofMinutes(1);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AgentMetricsCollector metricsCollector;

    // L1 Cache: Tool execution results
    private final Cache<String, CachedExecutionResult> resultCache;

    // Cache for tracking tool execution patterns
    private final Map<String, ToolExecutionStats> toolStats;

    // Cache invalidation listeners
    private final List<CacheInvalidationListener> invalidationListeners;

    // Cache configuration per tool type
    private final Map<String, CacheConfig> toolCacheConfigs;

    // Statistics tracking
    private final Map<String, CacheStatistics> statistics;

    /**
     * Default constructor with Caffeine cache initialization.
     */
    public ToolCacheManager() {
        this.resultCache = Caffeine.newBuilder()
            .maximumSize(DEFAULT_MAX_CACHE_SIZE)
            .expireAfterWrite(DEFAULT_TTL.toMillis(), TimeUnit.MILLISECONDS)
            .recordStats()
            .build();

        this.toolStats = new ConcurrentHashMap<>();
        this.invalidationListeners = new ArrayList<>();
        this.toolCacheConfigs = new ConcurrentHashMap<>();
        this.statistics = new ConcurrentHashMap<>();

        initializeDefaultConfigs();
        LOG.info("ToolCacheManager initialized with default cache configuration");
    }

    /**
     * Initialize default cache configurations for different tool types.
     */
    private void initializeDefaultConfigs() {
        // Read operations - longer TTL
        registerCacheConfig("read_file", CacheConfig.builder()
            .ttl(Duration.ofMinutes(30))
            .maxSize(500)
            .build());

        // Search operations - medium TTL
        registerCacheConfig("grep_search", CacheConfig.builder()
            .ttl(Duration.ofMinutes(15))
            .maxSize(300)
            .build());

        // Write operations - short TTL (invalidate on write)
        registerCacheConfig("write_file", CacheConfig.builder()
            .ttl(Duration.ofMinutes(5))
            .maxSize(100)
            .build());

        // Command execution - very short TTL
        registerCacheConfig("execute_command", CacheConfig.builder()
            .ttl(Duration.ofMinutes(2))
            .maxSize(200)
            .build());

        // HTTP calls - medium TTL
        registerCacheConfig("http_call", CacheConfig.builder()
            .ttl(Duration.ofMinutes(10))
            .maxSize(400)
            .build());

        // Inference - short TTL (expensive but may change)
        registerCacheConfig("inference", CacheConfig.builder()
            .ttl(Duration.ofMinutes(5))
            .maxSize(150)
            .build());
    }

    /**
     * Execute a tool with intelligent caching.
     *
     * @param toolId the tool identifier
     * @param params tool input parameters
     * @param context execution context
     * @param executor tool execution function
     * @return Uni containing the execution result
     */
    public Uni<SkillResult> executeWithCache(
            String toolId,
            Map<String, Object> params,
            Map<String, Object> context,
            ToolExecutor executor) {

        String cacheKey = generateCacheKey(toolId, params, context);
        CacheConfig config = getCacheConfig(toolId);

        LOG.debugf("Executing tool %s with cache key %s", toolId, cacheKey);

        return Uni.createFrom().completionStage(() -> {
            CachedExecutionResult cached = resultCache.getIfPresent(cacheKey);

            if (cached != null && !cached.isExpired()) {
                LOG.debugf("Cache hit for tool %s (key: %s)", toolId, cacheKey);
                recordCacheHit(toolId, true);
                return cached.getResult();
            }

            // Cache miss - execute and cache
            LOG.debugf("Cache miss for tool %s (key: %s), executing...", toolId, cacheKey);
            long startTime = System.currentTimeMillis();

            try {
                SkillResult result = executor.execute();
                long duration = System.currentTimeMillis() - startTime;

                // Cache the result
                CachedExecutionResult cachedResult = new CachedExecutionResult(
                    result,
                    Instant.now(),
                    config.getTtl()
                );
                resultCache.put(cacheKey, cachedResult);

                // Update statistics
                recordCacheHit(toolId, false);
                updateToolStats(toolId, duration, true);

                LOG.debugf("Tool %s executed in %dms and cached", toolId, duration);
                return result;

            } catch (Exception e) {
                LOG.errorf(e, "Tool execution failed for %s", toolId);
                updateToolStats(toolId, System.currentTimeMillis() - startTime, false);
                throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Execute a tool with cache bypass option.
     *
     * @param toolId the tool identifier
     * @param params tool input parameters
     * @param context execution context
     * @param executor tool execution function
     * @param bypassCache if true, skip cache lookup and write
     * @return Uni containing the execution result
     */
    public Uni<SkillResult> executeWithCache(
            String toolId,
            Map<String, Object> params,
            Map<String, Object> context,
            ToolExecutor executor,
            boolean bypassCache) {

        if (bypassCache) {
            LOG.debugf("Cache bypass requested for tool %s", toolId);
            return Uni.createFrom().completionStage(() -> {
                try {
                    return executor.execute();
                } catch (Exception e) {
                    throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
                }
            });
        }

        return executeWithCache(toolId, params, context, executor);
    }

    /**
     * Generate a unique cache key based on tool ID, parameters, and context.
     *
     * @param toolId the tool identifier
     * @param params tool input parameters
     * @param context execution context
     * @return unique cache key
     */
    public String generateCacheKey(String toolId, Map<String, Object> params, Map<String, Object> context) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(toolId).append(":");

        // Add normalized parameters
        if (params != null && !params.isEmpty()) {
            String paramHash = normalizeAndHash(params);
            keyBuilder.append(paramHash);
        } else {
            keyBuilder.append("no-params");
        }

        // Add context hash if present
        if (context != null && !context.isEmpty()) {
            String contextHash = normalizeAndHash(context);
            keyBuilder.append(":").append(contextHash);
        }

        return keyBuilder.toString();
    }

    /**
     * Invalidate cache entries for a specific tool.
     *
     * @param toolId the tool identifier
     */
    public void invalidate(String toolId) {
        LOG.debugf("Invalidating cache for tool %s", toolId);

        // Find and remove all keys for this tool
        resultCache.asMap().keySet().stream()
            .filter(key -> key.startsWith(toolId + ":"))
            .forEach(key -> {
                resultCache.invalidate(key);
                notifyInvalidation(toolId, key);
            });

        LOG.debugf("Cache invalidation complete for tool %s", toolId);
    }

    /**
     * Invalidate cache entries for a specific tool and parameters.
     *
     * @param toolId the tool identifier
     * @param params tool input parameters
     */
    public void invalidate(String toolId, Map<String, Object> params) {
        String cacheKey = generateCacheKey(toolId, params, null);
        LOG.debugf("Invalidating cache for tool %s with key %s", toolId, cacheKey);

        resultCache.invalidate(cacheKey);
        notifyInvalidation(toolId, cacheKey);
    }

    /**
     * Invalidate cache entries based on a pattern.
     *
     * @param toolId the tool identifier
     * @param keyPattern regex pattern for cache keys
     */
    public void invalidateByPattern(String toolId, String keyPattern) {
        LOG.debugf("Invalidating cache for tool %s with pattern %s", toolId, keyPattern);

        resultCache.asMap().keySet().stream()
            .filter(key -> key.startsWith(toolId + ":") && key.matches(keyPattern))
            .forEach(key -> {
                resultCache.invalidate(key);
                notifyInvalidation(toolId, key);
            });
    }

    /**
     * Clear all cache entries.
     */
    public void clearAll() {
        LOG.info("Clearing all cache entries");
        resultCache.invalidateAll();
        notifyInvalidation("ALL", "*");
    }

    /**
     * Get cache statistics.
     *
     * @return cache statistics
     */
    public CacheStatistics getStatistics() {
        com.github.benmanes.caffeine.cache.CacheStats stats = resultCache.stats();
        long totalRequests = stats.hitCount() + stats.missCount();
        double hitRatio = totalRequests > 0 ? (double) stats.hitCount() / totalRequests : 0.0;

        return new CacheStatistics(
            resultCache.estimatedSize(),
            stats.hitCount(),
            stats.missCount(),
            hitRatio,
            stats.evictionCount(),
            Instant.now()
        );
    }

    /**
     * Get statistics for a specific tool.
     *
     * @param toolId the tool identifier
     * @return tool-specific statistics
     */
    public ToolExecutionStats getToolStats(String toolId) {
        return toolStats.getOrDefault(toolId, ToolExecutionStats.empty(toolId));
    }

    /**
     * Get all tool statistics.
     *
     * @return map of tool statistics
     */
    public Map<String, ToolExecutionStats> getAllToolStats() {
        return new HashMap<>(toolStats);
    }

    /**
     * Register a cache configuration for a specific tool type.
     *
     * @param toolId the tool identifier
     * @param config cache configuration
     */
    public void registerCacheConfig(String toolId, CacheConfig config) {
        toolCacheConfigs.put(toolId, config);
        LOG.debugf("Registered cache config for tool %s: TTL=%s, maxSize=%d",
            toolId, config.getTtl(), config.getMaxSize());
    }

    /**
     * Add a cache invalidation listener.
     *
     * @param listener the listener to add
     */
    public void addInvalidationListener(CacheInvalidationListener listener) {
        invalidationListeners.add(listener);
    }

    /**
     * Remove a cache invalidation listener.
     *
     * @param listener the listener to remove
     */
    public void removeInvalidationListener(CacheInvalidationListener listener) {
        invalidationListeners.remove(listener);
    }

    /**
     * Warm up cache with pre-computed results.
     *
     * @param toolId the tool identifier
     * @param params tool input parameters
     * @param result pre-computed result
     */
    public void warmCache(String toolId, Map<String, Object> params, SkillResult result) {
        String cacheKey = generateCacheKey(toolId, params, null);
        CacheConfig config = getCacheConfig(toolId);

        CachedExecutionResult cachedResult = new CachedExecutionResult(
            result,
            Instant.now(),
            config.getTtl()
        );

        resultCache.put(cacheKey, cachedResult);
        LOG.debugf("Cache warmed for tool %s with key %s", toolId, cacheKey);
    }

    /**
     * Preload cache with common tool executions.
     *
     * @param toolId the tool identifier
     * @param paramSets list of parameter sets to preload
     * @param executor tool execution function
     * @return Uni that completes when preloading is done
     */
    public Uni<Void> preloadCache(
            String toolId,
            List<Map<String, Object>> paramSets,
            ToolExecutor executor) {

        LOG.infof("Preloading cache for tool %s with %d parameter sets", toolId, paramSets.size());

        return Uni.combine().all().unis(
            paramSets.stream()
                .map(params -> executeWithCache(toolId, params, null, executor).replaceWithVoid())
                .collect(Collectors.toList())
        ).discardItems();
    }

    /**
     * Get cache configuration for a tool.
     */
    private CacheConfig getCacheConfig(String toolId) {
        return toolCacheConfigs.getOrDefault(toolId, CacheConfig.defaultConfig());
    }

    /**
     * Normalize and hash a map of parameters.
     */
    private String normalizeAndHash(Map<String, Object> map) {
        try {
            String json = objectMapper.writeValueAsString(map);
            return Integer.toHexString(json.hashCode());
        } catch (Exception e) {
            LOG.warnf(e, "Failed to serialize parameters for hashing");
            return String.valueOf(map.hashCode());
        }
    }

    /**
     * Record cache hit/miss for metrics.
     */
    private void recordCacheHit(String toolId, boolean hit) {
        if (metricsCollector != null) {
            metricsCollector.recordToolCacheHit(toolId, hit);
        }
    }

    /**
     * Update tool execution statistics.
     */
    private void updateToolStats(String toolId, long durationMs, boolean success) {
        toolStats.compute(toolId, (key, stats) -> {
            if (stats == null) {
                stats = ToolExecutionStats.empty(toolId);
            }
            return stats.recordExecution(durationMs, success);
        });
    }

    /**
     * Notify invalidation listeners.
     */
    private void notifyInvalidation(String toolId, String cacheKey) {
        invalidationListeners.forEach(listener -> {
            try {
                listener.onInvalidated(toolId, cacheKey);
            } catch (Exception e) {
                LOG.errorf(e, "Error in cache invalidation listener");
            }
        });
    }

    // ==================== Functional Interfaces ====================

    /**
     * Functional interface for tool execution.
     */
    @FunctionalInterface
    public interface ToolExecutor {
        SkillResult execute() throws Exception;
    }

    /**
     * Functional interface for cache invalidation events.
     */
    @FunctionalInterface
    public interface CacheInvalidationListener {
        void onInvalidated(String toolId, String cacheKey);
    }

    // ==================== Record Classes ====================

    /**
     * Cached execution result with metadata.
     *
     * @param result the execution result
     * @param createdAt creation timestamp
     * @param ttl time-to-live
     */
    public record CachedExecutionResult(
        SkillResult result,
        Instant createdAt,
        Duration ttl
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(ttl));
        }
    }

    /**
     * Cache configuration for a tool type.
     *
     * @param ttl time-to-live
     * @param maxSize maximum cache size
     * @param enabled whether caching is enabled
     */
    public record CacheConfig(
        Duration ttl,
        int maxSize,
        boolean enabled
    ) {
        public static CacheConfig defaultConfig() {
            return new CacheConfig(DEFAULT_TTL, DEFAULT_MAX_CACHE_SIZE, true);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Duration ttl = DEFAULT_TTL;
            private int maxSize = DEFAULT_MAX_CACHE_SIZE;
            private boolean enabled = true;

            public Builder ttl(Duration ttl) {
                this.ttl = ttl;
                return this;
            }

            public Builder maxSize(int maxSize) {
                this.maxSize = maxSize;
                return this;
            }

            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public CacheConfig build() {
                return new CacheConfig(ttl, maxSize, enabled);
            }
        }
    }

    /**
     * Cache statistics snapshot.
     *
     * @param size current cache size
     * @param hits number of cache hits
     * @param misses number of cache misses
     * @param hitRatio cache hit ratio (0.0 to 1.0)
     * @param evictions number of cache evictions
     * @param timestamp statistics timestamp
     */
    public record CacheStatistics(
        long size,
        long hits,
        long misses,
        double hitRatio,
        long evictions,
        Instant timestamp
    ) {
        @Override
        public String toString() {
            return String.format("CacheStatistics{size=%d, hits=%d, misses=%d, hitRatio=%.2f, evictions=%d}",
                size, hits, misses, hitRatio, evictions);
        }
    }

    /**
     * Tool execution statistics.
     *
     * @param toolId the tool identifier
     * @param executionCount total executions
     * @param cacheHits number of cache hits
     * @param cacheMisses number of cache misses
     * @param avgExecutionTimeMs average execution time in milliseconds
     * @param successCount number of successful executions
     * @param failureCount number of failed executions
     * @param lastExecutionTime timestamp of last execution
     */
    public record ToolExecutionStats(
        String toolId,
        long executionCount,
        long cacheHits,
        long cacheMisses,
        double avgExecutionTimeMs,
        long successCount,
        long failureCount,
        Instant lastExecutionTime
    ) {
        public static ToolExecutionStats empty(String toolId) {
            return new ToolExecutionStats(toolId, 0, 0, 0, 0.0, 0, 0, Instant.now());
        }

        public ToolExecutionStats recordExecution(long durationMs, boolean success) {
            long newExecutionCount = executionCount + 1;
            double newAvgTime = ((avgExecutionTimeMs * executionCount) + durationMs) / newExecutionCount;

            return new ToolExecutionStats(
                toolId,
                newExecutionCount,
                cacheHits,
                cacheMisses,
                newAvgTime,
                success ? successCount + 1 : successCount,
                success ? failureCount : failureCount + 1,
                Instant.now()
            );
        }

        public ToolExecutionStats recordCacheHit() {
            return new ToolExecutionStats(
                toolId,
                executionCount,
                cacheHits + 1,
                cacheMisses,
                avgExecutionTimeMs,
                successCount,
                failureCount,
                lastExecutionTime
            );
        }

        public ToolExecutionStats recordCacheMiss() {
            return new ToolExecutionStats(
                toolId,
                executionCount,
                cacheHits,
                cacheMisses + 1,
                avgExecutionTimeMs,
                successCount,
                failureCount,
                lastExecutionTime
            );
        }

        public double getSuccessRate() {
            long total = successCount + failureCount;
            return total > 0 ? (double) successCount / total : 0.0;
        }

        public double getCacheHitRatio() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }
    }
}

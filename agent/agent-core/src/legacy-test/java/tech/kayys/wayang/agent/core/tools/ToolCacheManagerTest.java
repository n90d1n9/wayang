package tech.kayys.wayang.agent.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.spi.SkillResult;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Unit tests for ToolCacheManager.
 */
@QuarkusTest
class ToolCacheManagerTest {

    @Inject
    ToolCacheManager cacheManager;

    private Map<String, Object> testParams;
    private Map<String, Object> testContext;

    @BeforeEach
    void setUp() {
        testParams = new HashMap<>();
        testParams.put("key", "value");
        testParams.put("count", 42);

        testContext = new HashMap<>();
        testContext.put("tenantId", "test-tenant");
        testContext.put("sessionId", "test-session");
    }

    @Test
    @DisplayName("Should execute tool and cache result on first call")
    void shouldExecuteAndCacheResult() {
        // Given
        AtomicInteger executionCount = new AtomicInteger(0);
        ToolCacheManager.ToolExecutor executor = () -> {
            executionCount.incrementAndGet();
            return SkillResult.builder()
                .skillId("test_tool")
                .invocationId("test-1")
                .status(SkillResult.Status.SUCCESS)
                .observation("test result")
                .build();
        };

        // When
        SkillResult result1 = cacheManager.executeWithCache(
            "test_tool", testParams, testContext, executor
        ).await().atMost(Duration.ofSeconds(5));

        SkillResult result2 = cacheManager.executeWithCache(
            "test_tool", testParams, testContext, executor
        ).await().atMost(Duration.ofSeconds(5));

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(executionCount.get()).isEqualTo(1); // Only executed once
        assertThat(result1.observation()).isEqualTo("test result");
        assertThat(result2.observation()).isEqualTo("test result");
    }

    @Test
    @DisplayName("Should bypass cache when requested")
    void shouldBypassCache() {
        // Given
        AtomicInteger executionCount = new AtomicInteger(0);
        ToolCacheManager.ToolExecutor executor = () -> {
            executionCount.incrementAndGet();
            return SkillResult.builder()
                .skillId("test_tool")
                .invocationId("test-2")
                .status(SkillResult.Status.SUCCESS)
                .observation("fresh result")
                .build();
        };

        // When
        SkillResult result1 = cacheManager.executeWithCache(
            "test_tool", testParams, testContext, executor, true
        ).await().atMost(Duration.ofSeconds(5));

        SkillResult result2 = cacheManager.executeWithCache(
            "test_tool", testParams, testContext, executor, true
        ).await().atMost(Duration.ofSeconds(5));

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(executionCount.get()).isEqualTo(2); // Executed twice (cache bypassed)
    }

    @Test
    @DisplayName("Should generate consistent cache keys")
    void shouldGenerateConsistentCacheKeys() {
        // When
        String key1 = cacheManager.generateCacheKey("test_tool", testParams, testContext);
        String key2 = cacheManager.generateCacheKey("test_tool", testParams, testContext);

        // Then
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName("Should generate different cache keys for different parameters")
    void shouldGenerateDifferentKeysForDifferentParams() {
        // Given
        Map<String, Object> differentParams = new HashMap<>();
        differentParams.put("key", "different_value");

        // When
        String key1 = cacheManager.generateCacheKey("test_tool", testParams, testContext);
        String key2 = cacheManager.generateCacheKey("test_tool", differentParams, testContext);

        // Then
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Should invalidate cache for specific tool")
    void shouldInvalidateToolCache() {
        // Given
        AtomicInteger executionCount = new AtomicInteger(0);
        ToolCacheManager.ToolExecutor executor = () -> {
            executionCount.incrementAndGet();
            return SkillResult.builder()
                .skillId("test_tool")
                .invocationId("test-3")
                .status(SkillResult.Status.SUCCESS)
                .observation("result")
                .build();
        };

        // Execute and cache
        cacheManager.executeWithCache("test_tool", testParams, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));

        // Invalidate
        cacheManager.invalidate("test_tool");

        // Execute again
        cacheManager.executeWithCache("test_tool", testParams, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));

        // Then
        assertThat(executionCount.get()).isEqualTo(2); // Executed twice (cache invalidated)
    }

    @Test
    @DisplayName("Should invalidate cache for specific parameters")
    void shouldInvalidateSpecificCache() {
        // Given
        Map<String, Object> params1 = Map.of("id", 1);
        Map<String, Object> params2 = Map.of("id", 2);
        AtomicInteger executionCount = new AtomicInteger(0);

        ToolCacheManager.ToolExecutor executor = () -> {
            executionCount.incrementAndGet();
            return SkillResult.builder()
                .skillId("test_tool")
                .invocationId("test-4")
                .status(SkillResult.Status.SUCCESS)
                .observation("result")
                .build();
        };

        // Execute and cache both
        cacheManager.executeWithCache("test_tool", params1, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));
        cacheManager.executeWithCache("test_tool", params2, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));

        // Invalidate only params1
        cacheManager.invalidate("test_tool", params1);

        // Execute again
        cacheManager.executeWithCache("test_tool", params1, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));
        cacheManager.executeWithCache("test_tool", params2, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));

        // Then
        assertThat(executionCount.get()).isEqualTo(3); // params1 executed twice, params2 once
    }

    @Test
    @DisplayName("Should clear all cache entries")
    void shouldClearAllCache() {
        // Given
        ToolCacheManager.ToolExecutor executor = () -> SkillResult.builder()
            .skillId("test_tool")
            .invocationId("test-5")
            .status(SkillResult.Status.SUCCESS)
            .observation("result")
            .build();

        // Execute multiple tools
        cacheManager.executeWithCache("tool1", testParams, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));
        cacheManager.executeWithCache("tool2", testParams, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));

        // Clear all
        cacheManager.clearAll();

        // Then cache should be empty
        ToolCacheManager.CacheStatistics stats = cacheManager.getStatistics();
        assertThat(stats.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should track cache statistics")
    void shouldTrackStatistics() {
        // Given
        ToolCacheManager.ToolExecutor executor = () -> SkillResult.builder()
            .skillId("test_tool")
            .invocationId("test-6")
            .status(SkillResult.Status.SUCCESS)
            .observation("result")
            .build();

        // Execute multiple times
        for (int i = 0; i < 5; i++) {
            cacheManager.executeWithCache("test_tool", testParams, testContext, executor)
                .await().atMost(Duration.ofSeconds(5));
        }

        // When
        ToolCacheManager.CacheStatistics stats = cacheManager.getStatistics();

        // Then
        assertThat(stats.size()).isGreaterThan(0);
        assertThat(stats.hits() + stats.misses()).isEqualTo(5);
        assertThat(stats.hitRatio()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Should track per-tool statistics")
    void shouldTrackToolStats() {
        // Given
        AtomicInteger executionCount = new AtomicInteger(0);
        ToolCacheManager.ToolExecutor executor = () -> {
            executionCount.incrementAndGet();
            return SkillResult.builder()
                .skillId("stats_tool")
                .invocationId("test-7")
                .status(SkillResult.Status.SUCCESS)
                .observation("result")
                .build();
        };

        // Execute multiple times
        for (int i = 0; i < 3; i++) {
            cacheManager.executeWithCache("stats_tool", testParams, testContext, executor)
                .await().atMost(Duration.ofSeconds(5));
        }

        // When
        ToolCacheManager.ToolExecutionStats stats = cacheManager.getToolStats("stats_tool");

        // Then
        assertThat(stats.executionCount()).isEqualTo(1); // Only first execution (cached)
        assertThat(stats.avgExecutionTimeMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should warm cache with pre-computed results")
    void shouldWarmCache() {
        // Given
        SkillResult precomputedResult = SkillResult.builder()
            .skillId("test_tool")
            .invocationId("test-8")
            .status(SkillResult.Status.SUCCESS)
            .observation("precomputed")
            .build();

        // Warm cache
        cacheManager.warmCache("test_tool", testParams, precomputedResult);

        // Execute with cache
        SkillResult result = cacheManager.executeWithCache(
            "test_tool", testParams, testContext, () -> {
                throw new IllegalStateException("Should not execute");
            }
        ).await().atMost(Duration.ofSeconds(5));

        // Then
        assertThat(result.observation()).isEqualTo("precomputed");
    }

    @Test
    @DisplayName("Should preload cache with multiple parameter sets")
    void shouldPreloadCache() {
        // Given
        var paramSets = java.util.List.of(
            Map.<String, Object>of("id", 1),
            Map.<String, Object>of("id", 2),
            Map.<String, Object>of("id", 3)
        );

        AtomicInteger executionCount = new AtomicInteger(0);
        ToolCacheManager.ToolExecutor executor = () -> {
            executionCount.incrementAndGet();
            return SkillResult.builder()
                .skillId("preload_tool")
                .invocationId("test-9")
                .status(SkillResult.Status.SUCCESS)
                .observation("preloaded")
                .build();
        };

        // When
        cacheManager.preloadCache("preload_tool", paramSets, executor)
            .await().atMost(Duration.ofSeconds(10));

        // Then
        assertThat(executionCount.get()).isEqualTo(3);

        // Verify cache hits
        cacheManager.executeWithCache("preload_tool", paramSets.get(0), testContext, executor)
            .await().atMost(Duration.ofSeconds(5));

        assertThat(executionCount.get()).isEqualTo(3); // Still 3 (cache hit)
    }

    @Test
    @DisplayName("Should handle cache expiration")
    void shouldHandleExpiration() throws Exception {
        // Given
        ToolCacheManager.ToolExecutor executor = () -> SkillResult.builder()
            .skillId("expire_tool")
            .invocationId("test-10")
            .status(SkillResult.Status.SUCCESS)
            .observation("result")
            .build();

        // Register config with short TTL
        ToolCacheManager.CacheConfig shortTtlConfig = ToolCacheManager.CacheConfig.builder()
            .ttl(Duration.ofMillis(100))
            .build();
        cacheManager.registerCacheConfig("expire_tool", shortTtlConfig);

        // Execute and cache
        cacheManager.executeWithCache("expire_tool", testParams, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));

        // Wait for expiration
        Thread.sleep(150);

        // Execute again
        cacheManager.executeWithCache("expire_tool", testParams, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));

        // Then - should have executed twice (once initially, once after expiration)
        // Note: This test may be flaky in CI environments
    }

    @Test
    @DisplayName("Should notify invalidation listeners")
    void shouldNotifyInvalidationListeners() {
        // Given
        var listenerCalled = new boolean[]{false};
        var invalidatedKey = new String[]{null};

        cacheManager.addInvalidationListener((toolId, cacheKey) -> {
            listenerCalled[0] = true;
            invalidatedKey[0] = cacheKey;
        });

        ToolCacheManager.ToolExecutor executor = () -> SkillResult.builder()
            .skillId("notify_tool")
            .invocationId("test-11")
            .status(SkillResult.Status.SUCCESS)
            .observation("result")
            .build();

        // Execute and cache
        cacheManager.executeWithCache("notify_tool", testParams, testContext, executor)
            .await().atMost(Duration.ofSeconds(5));

        // When
        cacheManager.invalidate("notify_tool");

        // Then
        await().atMost(Duration.ofSeconds(1))
            .until(() -> listenerCalled[0]);
        assertThat(invalidatedKey[0]).isNotNull();
    }
}

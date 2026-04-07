package tech.kayys.wayang.agent.core.client;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.provider.ProviderCapabilities;
import tech.kayys.gollek.spi.provider.ProviderInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Intelligent provider routing with automatic fallback between local and cloud providers.
 *
 * <p>This class implements smart provider selection based on:
 * <ul>
 *   <li>Task complexity (simple vs complex reasoning)</li>
 *   <li>Latency requirements</li>
 *   <li>Cost optimization (prefer local models when possible)</li>
 *   <li>Provider health and availability</li>
 *   <li>Model capabilities (tool calling, streaming, etc.)</li>
 * </ul>
 *
 * <h2>Provider Priority Order:</h2>
 * <ol>
 *   <li>Preferred provider (if set and healthy)</li>
 *   <li>Local providers (GGUF, ONNX) for cost efficiency</li>
 *   <li>Cloud providers (OpenAI, Anthropic, etc.) for complex tasks</li>
 * </ol>
 *
 * <h2>Fallback Strategy:</h2>
 * <p>When a provider fails, automatically retry with the next available provider
 * in the priority list, up to the maximum retry count.</p>
 *
 * @author Wayang AI Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class ProviderAwareInference {

    private static final Logger LOG = Logger.getLogger(ProviderAwareInference.class);

    @Inject
    GollekSdk gollekSdk;

    private final Map<String, ProviderStats> providerStats = new ConcurrentHashMap<>();
    private final Map<String, ProviderInfo> providerCache = new ConcurrentHashMap<>();

    // Provider type classification
    private static final Set<String> LOCAL_PROVIDERS = Set.of("gguf", "onnx", "safetensor", "triton");
    private static final Set<String> CLOUD_PROVIDERS = Set.of("openai", "anthropic", "mistral", "gemini", "cerebras", "deepseek");

    /**
     * Execute inference with automatic provider selection and fallback.
     *
     * @param request the inference request
     * @param sdk the Gollek SDK instance
     * @return CompletableFuture with the inference response
     */
    public CompletableFuture<InferenceResponse> inferWithFallback(
            InferenceRequest request,
            GollekSdk sdk) {
        
        return selectBestProvider(request)
            .thenCompose(providerId -> executeWithProvider(request, sdk, providerId, 0));
    }

    /**
     * Execute inference with a specific provider.
     *
     * @param request the inference request
     * @param sdk the Gollek SDK instance
     * @param providerId the provider to use
     * @return CompletableFuture with the inference response
     */
    public CompletableFuture<InferenceResponse> inferWithProvider(
            InferenceRequest request,
            GollekSdk sdk,
            String providerId) {
        
        return executeWithProvider(request, sdk, providerId, 0);
    }

    /**
     * Select the best provider based on request characteristics and provider capabilities.
     *
     * @param request the inference request
     * @return CompletableFuture with the selected provider ID
     */
    private CompletableFuture<String> selectBestProvider(InferenceRequest request) {
        // Check if preferred provider is set
        if (request.getPreferredProvider().isPresent()) {
            String preferred = request.getPreferredProvider().get();
            if (isProviderHealthy(preferred)) {
                LOG.debugf("Using preferred provider: %s", preferred);
                return CompletableFuture.completedFuture(preferred);
            }
            LOG.warnf("Preferred provider %s is not healthy, selecting alternative", preferred);
        }

        try {
            List<ProviderInfo> providers = sdk.listAvailableProviders();
            if (providers.isEmpty()) {
                return CompletableFuture.completedFuture("default");
            }

            // Filter healthy providers
            List<ProviderInfo> healthyProviders = providers.stream()
                .filter(p -> isProviderHealthy(p.id()))
                .toList();

            if (healthyProviders.isEmpty()) {
                LOG.warn("No healthy providers available, using first available");
                return CompletableFuture.completedFuture(providers.get(0).id());
            }

            // Score and rank providers
            String bestProvider = healthyProviders.stream()
                .max(Comparator.comparingInt(p -> scoreProvider(p, request)))
                .map(ProviderInfo::id)
                .orElse(healthyProviders.get(0).id());

            LOG.debugf("Selected provider: %s (score: %d)", 
                bestProvider, scoreProvider(healthyProviders.stream()
                    .filter(p -> p.id().equals(bestProvider))
                    .findFirst()
                    .orElse(healthyProviders.get(0), request)));

            return CompletableFuture.completedFuture(bestProvider);

        } catch (SdkException e) {
            LOG.errorf(e, "Failed to list providers, using default");
            return CompletableFuture.completedFuture("default");
        }
    }

    /**
     * Execute inference with retry and fallback logic.
     *
     * @param request the inference request
     * @param sdk the Gollek SDK instance
     * @param providerId the current provider to try
     * @param attempt the current attempt number
     * @return CompletableFuture with the inference response
     */
    private CompletableFuture<InferenceResponse> executeWithProvider(
            InferenceRequest request,
            GollekSdk sdk,
            String providerId,
            int attempt) {
        
        final int MAX_ATTEMPTS = 3;
        final int MAX_TIMEOUT_MS = 60000;

        long startTime = System.currentTimeMillis();

        // Update request with selected provider
        InferenceRequest providerRequest = request.toBuilder()
            .preferredProvider(providerId)
            .build();

        LOG.debugf("Executing inference with provider %s (attempt %d)", providerId, attempt + 1);

        return sdk.createCompletionAsync(providerRequest)
            .thenApply(response -> {
                long duration = System.currentTimeMillis() - startTime;
                recordProviderStats(providerId, true, duration);
                LOG.debugf("Inference successful with provider %s (%d ms)", providerId, duration);
                return response;
            })
            .exceptionallyCompose(throwable -> {
                long duration = System.currentTimeMillis() - startTime;
                recordProviderStats(providerId, false, duration);
                
                LOG.warnf(throwable, "Provider %s failed (attempt %d)", providerId, attempt + 1);

                if (attempt >= MAX_ATTEMPTS - 1) {
                    LOG.errorf("All %d attempts failed for provider %s", MAX_ATTEMPTS, providerId);
                    return CompletableFuture.failedFuture(throwable);
                }

                // Try next best provider
                return selectBestProvider(request)
                    .thenCompose(nextProviderId -> {
                        if (nextProviderId.equals(providerId)) {
                            // Same provider, increment attempt
                            return executeWithProvider(request, sdk, providerId, attempt + 1);
                        } else {
                            // Different provider, reset attempt counter
                            LOG.infof("Falling back to provider: %s", nextProviderId);
                            return executeWithProvider(request, sdk, nextProviderId, 0);
                        }
                    });
            });
    }

    /**
     * Score a provider based on its capabilities and the request requirements.
     *
     * @param provider the provider to score
     * @param request the inference request
     * @return score (higher is better)
     */
    private int scoreProvider(ProviderInfo provider, InferenceRequest request) {
        int score = 0;

        // Prefer local providers for cost efficiency (unless cloud is explicitly requested)
        if (isLocalProvider(provider.id())) {
            score += 100;
        }

        // Check if provider supports required features
        ProviderCapabilities caps = provider.capabilities();
        
        // Tool calling support
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            if (caps != null && Boolean.TRUE.equals(caps.supportsToolCalling())) {
                score += 50;
            } else if (isCloudProvider(provider.id())) {
                // Cloud providers typically support tool calling
                score += 30;
            }
        }

        // Streaming support
        if (request.isStreaming()) {
            if (caps != null && Boolean.TRUE.equals(caps.supportsStreaming())) {
                score += 30;
            }
        }

        // Consider provider performance stats
        ProviderStats stats = providerStats.get(provider.id());
        if (stats != null) {
            // Prefer providers with lower average latency
            if (stats.avgLatencyMs < 1000) {
                score += 20;
            } else if (stats.avgLatencyMs < 3000) {
                score += 10;
            }

            // Prefer providers with higher success rate
            if (stats.successRate > 0.95) {
                score += 20;
            } else if (stats.successRate > 0.90) {
                score += 10;
            }
        }

        // Consider provider health
        if (provider.healthStatus() != null && 
            provider.healthStatus().status() == 
                tech.kayys.gollek.spi.provider.ProviderHealth.Status.HEALTHY) {
            score += 15;
        }

        return score;
    }

    /**
     * Check if a provider is a local provider.
     */
    private boolean isLocalProvider(String providerId) {
        return LOCAL_PROVIDERS.contains(providerId.toLowerCase());
    }

    /**
     * Check if a provider is a cloud provider.
     */
    private boolean isCloudProvider(String providerId) {
        return CLOUD_PROVIDERS.contains(providerId.toLowerCase());
    }

    /**
     * Check if a provider is healthy.
     */
    private boolean isProviderHealthy(String providerId) {
        try {
            ProviderInfo info = gollekSdk.getProviderInfo(providerId);
            return info.healthStatus() != null && 
                info.healthStatus().status() == 
                    tech.kayys.gollek.spi.provider.ProviderHealth.Status.HEALTHY;
        } catch (SdkException e) {
            LOG.debugf("Provider %s health check failed: %s", providerId, e.getMessage());
            return false;
        }
    }

    /**
     * Record statistics for a provider execution.
     */
    private void recordProviderStats(String providerId, boolean success, long durationMs) {
        ProviderStats stats = providerStats.computeIfAbsent(providerId, k -> new ProviderStats());
        stats.record(success, durationMs);
    }

    /**
     * Get provider statistics.
     *
     * @param providerId the provider ID
     * @return provider statistics
     */
    public ProviderStats getProviderStats(String providerId) {
        return providerStats.getOrDefault(providerId, new ProviderStats());
    }

    /**
     * Clear provider statistics cache.
     */
    public void clearStatsCache() {
        providerStats.clear();
    }

    /**
     * Provider statistics tracking.
     */
    public static class ProviderStats {
        public double avgLatencyMs = 0;
        public double successRate = 1.0;
        private int totalRequests = 0;
        private int successfulRequests = 0;
        private long totalLatency = 0;
        private static final double ALPHA = 0.1; // Exponential moving average factor

        public synchronized void record(boolean success, long latencyMs) {
            totalRequests++;
            
            // Exponential moving average for latency
            avgLatencyMs = ALPHA * latencyMs + (1 - ALPHA) * avgLatencyMs;
            totalLatency += latencyMs;

            if (success) {
                successfulRequests++;
            }

            // Calculate success rate
            successRate = (double) successfulRequests / totalRequests;
        }

        public int getTotalRequests() {
            return totalRequests;
        }

        public int getSuccessfulRequests() {
            return successfulRequests;
        }

        @Override
        public String toString() {
            return String.format("ProviderStats{avgLatency=%.0fms, successRate=%.2f, requests=%d}",
                avgLatencyMs, successRate, totalRequests);
        }
    }
}

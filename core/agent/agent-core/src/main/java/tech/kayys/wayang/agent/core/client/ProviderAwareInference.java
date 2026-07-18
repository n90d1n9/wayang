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

import tech.kayys.wayang.gollek.sdk.WayangYaffFrame;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.nio.ByteBuffer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
 * @version 0.1.0
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
            List<ProviderInfo> providers = gollekSdk.listAvailableProviders();
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
                    .orElse(healthyProviders.get(0)), request));

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

        // If provider looks local, try control-plane (HTTP) fast-path when configured
        if (isLocalProvider(providerId)) {
            String ctrl = System.getProperty("WAYANG_YAFF_CONTROL_URL");
            if ((ctrl == null || ctrl.isBlank())) ctrl = System.getenv("WAYANG_YAFF_CONTROL_URL");
            if (ctrl != null && !ctrl.isBlank()) {
                try {
                    LOG.debugf("Attempting YAFF control-plane fast-path for provider %s via %s", providerId, ctrl);
                    // Try to marshal request into bytes (prefer YAFF adapter if present)
                    byte[] payload = null;
                    try {
                        Class<?> adapterCls = Class.forName("tech.kayys.wayang.yaffffm.YaffFFMAdapter");
                        Object adapter = adapterCls.getDeclaredConstructor().newInstance();
                        var m = adapterCls.getMethod("marshalBytes", Object.class);
                        Object bufObj = m.invoke(adapter, providerRequest);
                        if (bufObj instanceof byte[]) payload = (byte[]) bufObj;
                    } catch (ClassNotFoundException cnf) {
                        // no adapter; fall back to prompt bytes
                    } catch (NoSuchMethodException nsme) {
                        // older adapter API may expose marshal(Object) -> ByteBuffer
                        try {
                            Class<?> adapterCls = Class.forName("tech.kayys.wayang.yaffffm.YaffFFMAdapter");
                            Object adapter = adapterCls.getDeclaredConstructor().newInstance();
                            var m = adapterCls.getMethod("marshal", Object.class);
                            Object bufObj = m.invoke(adapter, providerRequest);
                            if (bufObj instanceof ByteBuffer) {
                                ByteBuffer bb = (ByteBuffer) bufObj;
                                byte[] b = new byte[bb.remaining()]; bb.get(b); payload = b;
                            }
                        } catch (Throwable ignored) {}
                    }

                    if (payload == null) {
                        String prompt = providerRequest.getPrompt() != null ? providerRequest.getPrompt() : "";
                        payload = prompt.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    }

                    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
                    HttpRequest httpReq = HttpRequest.newBuilder()
                            .uri(URI.create(ctrl).resolve("/allocate"))
                            .timeout(Duration.ofSeconds(10))
                            .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                            .build();
                    HttpResponse<byte[]> resp = client.send(httpReq, HttpResponse.BodyHandlers.ofByteArray());
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        byte[] body = resp.body();
                        // Try to parse JSON AllocateResponse from control-plane
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            // Use SDK Frame helper to allocate and get structured frame metadata
                            try {
                            WayangYaffFrame frame = WayangYaffFrame.allocate(ctrl, body);
                            InferenceResponse ir = InferenceResponse.builder()
                                    .requestId(providerRequest.getRequestId())
                                    .content("[yaff-allocated]")
                                    .model(providerRequest.getModel())
                                    .durationMs(System.currentTimeMillis() - startTime)
                                    .timestamp(java.time.Instant.now())
                                    .metadata(java.util.Map.of(
                                            "yaff.id", frame.id(),
                                            "yaff.path", frame.path(),
                                            "yaff.offset", frame.offset(),
                                            "yaff.length", frame.length()
                                    ))
                                    .build();
                            recordProviderStats(providerId, true, System.currentTimeMillis() - startTime);
                            LOG.debugf("YAFF control-plane fast-path returned for provider %s (id=%s)", providerId, frame.id());
                            return CompletableFuture.completedFuture(ir);
                            } catch (Throwable t) {
                            // fall through to previous parsing fallback
                            java.util.Map<String, Object> obj = mapper.readValue(body, java.util.Map.class);
                            Object id = obj.get("id");
                            Object path = obj.get("path");
                            Object offset = obj.get("offset");
                            Object length = obj.get("length");
                            InferenceResponse ir = InferenceResponse.builder()
                                    .requestId(providerRequest.getRequestId())
                                    .content("[yaff-allocated]")
                                    .model(providerRequest.getModel())
                                    .durationMs(System.currentTimeMillis() - startTime)
                                    .timestamp(java.time.Instant.now())
                                    .metadata(java.util.Map.of(
                                            "yaff.id", id == null ? "" : id.toString(),
                                            "yaff.path", path == null ? "" : path.toString(),
                                            "yaff.offset", offset == null ? 0L : ((Number) offset).longValue(),
                                            "yaff.length", length == null ? 0L : ((Number) length).longValue()
                                    ))
                                    .build();
                            recordProviderStats(providerId, true, System.currentTimeMillis() - startTime);
                            LOG.debugf("YAFF control-plane returned non-proto for provider %s: %s", providerId, t.getMessage());
                            return CompletableFuture.completedFuture(ir);
                            }
                        } catch (Throwable parseErr) {
                            // Fallback to raw content
                            String content = new String(body, java.nio.charset.StandardCharsets.UTF_8);
                            InferenceResponse ir = InferenceResponse.builder()
                                    .requestId(providerRequest.getRequestId())
                                    .content(content == null ? "" : content)
                                    .model(providerRequest.getModel())
                                    .durationMs(System.currentTimeMillis() - startTime)
                                    .timestamp(java.time.Instant.now())
                                    .build();
                            recordProviderStats(providerId, true, System.currentTimeMillis() - startTime);
                            LOG.debugf("YAFF control-plane returned non-JSON for provider %s: %s", providerId, parseErr.getMessage());
                            return CompletableFuture.completedFuture(ir);
                        }
                    } else {
                        LOG.warnf("YAFF control-plane returned status %d", resp.statusCode());
                    }
                } catch (Throwable t) {
                    LOG.warnf(t, "YAFF control-plane fast-path failed for provider %s: %s", providerId, t.getMessage());
                }
            }

            // If control-plane path not used or failed, try reflection-based SHM adapter
            try {
                LOG.debugf("Attempting YAFF SHM fast-path for provider %s", providerId);
                // Try to load adapter and transport classes
                Class<?> transportCls = Class.forName("tech.kayys.wayang.yaffffm.ShmYaffTransportProvider");
                Object transport = transportCls.getDeclaredConstructor().newInstance();

                // Try to find a marshal helper (YaffFFMAdapter) to convert request -> ByteBuffer
                ByteBuffer requestBuf = null;
                try {
                    Class<?> adapterCls = Class.forName("tech.kayys.wayang.yaffffm.YaffFFMAdapter");
                    Object adapter = adapterCls.getDeclaredConstructor().newInstance();
                    var m = adapterCls.getMethod("marshal", Object.class);
                    Object bufObj = m.invoke(adapter, providerRequest);
                    if (bufObj instanceof ByteBuffer) requestBuf = (ByteBuffer) bufObj;
                } catch (ClassNotFoundException cnf) {
                    // Adapter not present; fall back to JSON prompt bytes
                }

                if (requestBuf == null) {
                    String prompt = providerRequest.getPrompt() != null ? providerRequest.getPrompt() : "";
                    requestBuf = ByteBuffer.wrap(prompt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                // Invoke transport.sendRequest(ByteBuffer)
                var sendMethod = transportCls.getMethod("sendRequest", ByteBuffer.class);
                Object respObj = sendMethod.invoke(transport, requestBuf);
                if (respObj instanceof ByteBuffer) {
                    ByteBuffer respBuf = (ByteBuffer) respObj;
                    // Convert response bytes to String
                    byte[] bytes = new byte[respBuf.remaining()];
                    respBuf.get(bytes);
                    String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

                    // Build a simple InferenceResponse
                    InferenceResponse ir = InferenceResponse.builder()
                            .requestId(providerRequest.getRequestId())
                            .content(content == null ? "" : content)
                            .model(providerRequest.getModel())
                            .durationMs(System.currentTimeMillis() - startTime)
                            .timestamp(java.time.Instant.now())
                            .build();

                    recordProviderStats(providerId, true, System.currentTimeMillis() - startTime);
                    LOG.debugf("YAFF SHM fast-path returned for provider %s", providerId);
                    return CompletableFuture.completedFuture(ir);
                } else {
                    LOG.debugf("YAFF SHM transport returned unsupported type: %s", respObj == null ? "null" : respObj.getClass());
                }
            } catch (ClassNotFoundException e) {
                LOG.debugf("YAFF SHM adapter not available on classpath: %s", e.getMessage());
            } catch (Throwable t) {
                LOG.warnf(t, "YAFF SHM fast-path failed for provider %s: %s", providerId, t.getMessage());
            }
        }

        // Fallback to SDK path
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
            if (caps != null && caps.isToolCalling()) {
                score += 50;
            } else if (isCloudProvider(provider.id())) {
                // Cloud providers typically support tool calling
                score += 30;
            }
        }

        // Streaming support
        if (request.isStreaming()) {
            if (caps != null && caps.isStreaming()) {
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
            provider.healthStatus() == 
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
                info.healthStatus() == 
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

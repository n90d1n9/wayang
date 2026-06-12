package tech.kayys.wayang.agent.core.client;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.inference.StreamingInferenceChunk;
import tech.kayys.gollek.spi.tool.ToolDefinition;
import tech.kayys.gollek.sdk.mcp.McpRegistryManager;
import tech.kayys.gollek.spi.model.ModelInfo;
import tech.kayys.gollek.spi.provider.ProviderInfo;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified client for Gollek inference integration with agent-specific enhancements.
 *
 * <p>This class wraps {@link GollekSdk} to provide seamless agent operations with:
 * <ul>
 *   <li>Provider-aware inference with automatic fallback</li>
 *   <li>Native tool calling support</li>
 *   <li>Streaming support for real-time token generation</li>
 *   <li>Intelligent model routing based on task complexity</li>
 *   <li>Built-in retry and circuit breaker patterns</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Inject
 * GollekAgentClient agentClient;
 *
 * // Simple inference
 * InferenceResponse response = agentClient.infer(request)
 *     .await().atMost(Duration.ofSeconds(30));
 *
 * // Streaming inference
 * Multi<StreamingInferenceChunk> stream = agentClient.stream(request);
 *
 * // Tool calling
 * InferenceResponse response = agentClient.inferWithTools(request, tools)
 *     .await().atMost(Duration.ofSeconds(30));
 * }</pre>
 *
 * @author Wayang AI Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class GollekAgentClient {

    private static final Logger LOG = Logger.getLogger(GollekAgentClient.class);

    @Inject
    GollekSdk gollekSdk;

    @Inject
    ProviderAwareInference providerAwareInference;

    private final Map<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * Execute inference request with automatic provider routing and fallback.
     *
     * @param request the inference request
     * @return Uni containing the inference response
     */
    public Uni<InferenceResponse> infer(InferenceRequest request) {
        LOG.debugf("Executing inference for request %s", request.getRequestId());
        
        return Uni.createFrom().completionStage(
                providerAwareInference.inferWithFallback(request, gollekSdk)
            )
            .onFailure().recoverWithUni(err -> {
                LOG.errorf(err, "Inference failed for request %s", request.getRequestId());
                return Uni.createFrom().failure(err);
            });
    }

    /**
     * Execute inference with native tool calling support.
     *
     * @param request the inference request
     * @param tools list of tool definitions for the model to use
     * @return Uni containing the inference response with potential tool calls
     */
    public Uni<InferenceResponse> inferWithTools(InferenceRequest request, List<ToolDefinition> tools) {
        LOG.debugf("Executing inference with %d tools for request %s", tools.size(), request.getRequestId());
        
        InferenceRequest requestWithTools = request.toBuilder()
            .tools(tools)
            .toolChoice("auto")
            .build();
        
        return infer(requestWithTools)
            .onItem().transform(response -> {
                if (response.hasToolCalls()) {
                    LOG.debugf("Response contains %d tool calls", response.getToolCalls().size());
                }
                return response;
            });
    }

    /**
     * Execute streaming inference with real-time token generation.
     *
     * @param request the inference request with streaming enabled
     * @return Multi emitting streaming chunks as they arrive
     */
    public Multi<StreamingInferenceChunk> stream(InferenceRequest request) {
        LOG.debugf("Starting streaming inference for request %s", request.getRequestId());
        
        InferenceRequest streamingRequest = request.toBuilder()
            .streaming(true)
            .build();
        
        return Multi.createFrom().emitter(emitter -> {
            try {
                Multi<StreamingInferenceChunk> stream = gollekSdk.streamCompletion(streamingRequest);
                stream.subscribe().with(
                    chunk -> {
                        if (!emitter.isCancelled()) {
                            emitter.emit(chunk);
                        }
                    },
                    failure -> {
                        if (!emitter.isCancelled()) {
                            emitter.fail(failure);
                        }
                    },
                    () -> {
                        if (!emitter.isCancelled()) {
                            emitter.complete();
                        }
                    }
                );
            } catch (Exception e) {
                emitter.fail(e);
            }
        });
    }

    /**
     * Execute streaming inference with tool calling support.
     *
     * @param request the inference request
     * @param tools list of tool definitions
     * @return Multi emitting streaming chunks
     */
    public Multi<StreamingInferenceChunk> streamWithTools(InferenceRequest request, List<ToolDefinition> tools) {
        InferenceRequest requestWithTools = request.toBuilder()
            .tools(tools)
            .toolChoice("auto")
            .streaming(true)
            .build();
        
        return stream(requestWithTools);
    }

    /**
     * Execute inference with retry and circuit breaker protection.
     *
     * @param request the inference request
     * @param maxRetries maximum number of retry attempts
     * @param timeout timeout for the entire operation
     * @return Uni containing the inference response
     */
    public Uni<InferenceResponse> inferWithRetry(InferenceRequest request, int maxRetries, Duration timeout) {
        String providerId = request.getPreferredProvider().orElse("default");
        
        if (isCircuitOpen(providerId)) {
            return Uni.createFrom().failure(
                new CircuitBreakerOpenException("Circuit breaker is open for provider: " + providerId)
            );
        }
        
        return infer(request)
            .onFailure().retry().atMost(maxRetries)
            .onItemOrFailure().transformToUni((response, failure) -> {
                if (failure != null) {
                    recordFailure(providerId);
                    return Uni.createFrom().failure(failure);
                } else {
                    recordSuccess(providerId);
                    return Uni.createFrom().item(response);
                }
            })
            .ifNoItem().after(timeout).fail();
    }

    /**
     * Get available providers from Gollek SDK.
     *
     * @return list of available provider IDs
     */
    public Uni<List<String>> getAvailableProviders() {
        return Uni.createFrom().item(() -> {
                try {
                    return gollekSdk.listAvailableProviders();
                } catch (SdkException e) {
                    throw new RuntimeException(e);
                }
            })
            .onItem().transform(providers -> 
                providers.stream()
                    .map(p -> p.id())
                    .toList()
            );
    }

    public ProviderInfo getProviderInfo(String providerId) throws SdkException {
        return gollekSdk.getProviderInfo(providerId);
    }

    public List<ModelInfo> listModels() throws SdkException {
        return gollekSdk.listModels();
    }

    public McpRegistryManager mcpRegistry() {
        return gollekSdk.mcpRegistry();
    }

    /**
     * Set preferred provider for subsequent inference requests.
     *
     * @param providerId the provider ID to set as preferred
     * @return Uni that completes when provider is set
     */
    public Uni<Void> setPreferredProvider(String providerId) {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                try {
                    gollekSdk.setPreferredProvider(providerId);
                    LOG.infof("Set preferred provider to: %s", providerId);
                } catch (SdkException e) {
                    LOG.errorf(e, "Failed to set preferred provider: %s", providerId);
                }
            });
    }

    /**
     * Get provider health status.
     *
     * @param providerId the provider ID to check
     * @return true if provider is healthy, false otherwise
     */
    public boolean isProviderHealthy(String providerId) {
        try {
            var providerInfo = gollekSdk.getProviderInfo(providerId);
            return providerInfo.healthStatus() == 
                tech.kayys.gollek.spi.provider.ProviderHealth.Status.HEALTHY;
        } catch (SdkException e) {
            LOG.warnf(e, "Failed to get health status for provider: %s", providerId);
            return false;
        }
    }

    // ── Circuit Breaker Logic ──────────────────────────────────────────────

    private boolean isCircuitOpen(String providerId) {
        CircuitBreakerState state = circuitBreakers.get(providerId);
        if (state == null) {
            return false;
        }
        return state.isOpen();
    }

    private void recordSuccess(String providerId) {
        circuitBreakers.computeIfAbsent(providerId, k -> new CircuitBreakerState())
            .recordSuccess();
    }

    private void recordFailure(String providerId) {
        circuitBreakers.computeIfAbsent(providerId, k -> new CircuitBreakerState())
            .recordFailure();
    }

    /**
     * Circuit breaker state for tracking provider failures.
     */
    private static class CircuitBreakerState {
        private static final int FAILURE_THRESHOLD = 5;
        private static final Duration RESET_TIMEOUT = Duration.ofMinutes(1);

        private int failureCount = 0;
        private long lastFailureTime = 0;
        private boolean open = false;

        public synchronized void recordSuccess() {
            failureCount = 0;
            open = false;
        }

        public synchronized void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            if (failureCount >= FAILURE_THRESHOLD) {
                open = true;
            }
        }

        public synchronized boolean isOpen() {
            if (!open) {
                return false;
            }
            // Check if reset timeout has passed
            if (System.currentTimeMillis() - lastFailureTime > RESET_TIMEOUT.toMillis()) {
                open = false;
                failureCount = 0;
                return false;
            }
            return true;
        }
    }

    /**
     * Exception thrown when circuit breaker is open.
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}

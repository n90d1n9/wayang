package tech.kayys.wayang.agent.spi;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;

/**
 * Backend-agnostic inference interface.
 *
 * <p>
 * This SPI decouples agent orchestration from specific inference implementations.
 * Backend adapters (e.g., GollekBackendAdapter, OllamaBackendAdapter) implement
 * this interface to provide inference capabilities.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Programmatic usage (no Quarkus)
 * InferenceBackend backend = BackendRegistry.getDefault();
 * Uni<tech.kayys.wayang.agent.spi.InferenceResponse> response = backend.infer(request);
 *
 * // Or via CDI in Quarkus
 * InferenceBackend inferenceBackend;
 * }</pre>
 *
 * <h3>Implementation Requirements:</h3>
 * <ul>
 *   <li>Must be thread-safe for concurrent inference requests</li>
 *   <li>Must handle retry/fallback logic if configured</li>
 *   <li>Must emit telemetry (tracing, metrics) if enabled</li>
 *   <li>Must respect request timeouts and cancellation</li>
 * </ul>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public interface InferenceBackend {

    /**
     * Unique backend identifier (e.g., "gollek", "ollama", "vllm", "openai").
     *
     * @return backend name
     */
    String name();

    /**
     * Human-readable backend version.
     *
     * @return version string
     */
    String version();

    /**
     * Execute a single inference request.
     *
     * @param request inference request with prompt, parameters, tools
     * @return Uni containing inference response
     */
    Uni<tech.kayys.wayang.agent.spi.InferenceResponse> infer(InferenceRequest request);

    /**
     * Execute inference with streaming response chunks.
     *
     * @param request inference request with prompt, parameters, tools
     * @return Multi emitting streaming chunks until completion
     */
    Multi<InferenceTypes.StreamingChunk> stream(InferenceRequest request);

    /**
     * List available model providers in this backend.
     *
     * @return list of provider information
     */
    List<InferenceTypes.ProviderInfo> listProviders();

    /**
     * Check if the backend is healthy and ready to serve requests.
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Get backend capabilities as a record.
     *
     * @return capabilities record
     */
    default tech.kayys.wayang.agent.spi.BackendCapabilities capabilitiesInfo() {
        return tech.kayys.wayang.agent.spi.BackendCapabilities.none();
    }

    /**
     * Initialize the backend with configuration.
     * Called once at startup.
     *
     * @param config backend-specific configuration
     */
    default void initialize(Map<String, Object> config) {
        // No-op by default
    }

    /**
     * Shutdown the backend gracefully.
     * Called during application shutdown.
     */
    default void shutdown() {
        // No-op by default
    }

    /**
     * Backend capability flags.
     *
     * @param capabilities bitset of supported capabilities
     * @return true if all requested capabilities are supported
     */
    default boolean supports(long capabilities) {
        return (capabilities() & capabilities) == capabilities;
    }

    // ── Capability Flags ─────────────────────────────────────────────────

    /** Supports streaming responses */
    long CAP_STREAMING = 1L << 0;

    /** Supports native tool calling (function calling) */
    long CAP_TOOL_CALLING = 1L << 1;

    /** Supports multimodal inputs (images, audio) */
    long CAP_MULTIMODAL = 1L << 2;

    /** Supports structured outputs (JSON schema validation) */
    long CAP_STRUCTURED_OUTPUT = 1L << 3;

    /** Supports parallel tool calls */
    long CAP_PARALLEL_TOOLS = 1L << 4;

    /** Supports vision/image understanding */
    long CAP_VISION = 1L << 5;

    /** Supports audio/speech processing */
    long CAP_AUDIO = 1L << 6;

    /** Supports embedding generation */
    long CAP_EMBEDDING = 1L << 7;

    // ── Default Implementations ──────────────────────────────────────────

    /**
     * Get capability bitset for this backend.
     * Override to declare supported capabilities.
     *
     * @return capability flags
     */
    default long capabilities() {
        return 0;
    }

    /**
     * Get default inference parameters for this backend.
     *
     * @return map of parameter name to default value
     */
    default Map<String, Object> defaultParameters() {
        return Map.of(
            "temperature", 0.7,
            "max_tokens", 2048
        );
    }
}

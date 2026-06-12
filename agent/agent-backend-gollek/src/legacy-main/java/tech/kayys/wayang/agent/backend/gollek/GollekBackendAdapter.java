package tech.kayys.wayang.agent.backend.gollek;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.wayang.agent.spi.inference.StreamingInferenceChunk;
import tech.kayys.wayang.agent.spi.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Backend adapter that wraps Gollek SDK to implement the backend-agnostic
 * {@link InferenceBackend} SPI.
 *
 * <p>
 * This adapter translates between wayang-gollek's backend-agnostic SPI types
 * and Gollek SDK's native types, enabling any code that depends on
 * {@code InferenceBackend} to work with Gollek without direct coupling.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Programmatic (no Quarkus)
 * GollekSdk sdk = GollekSdk.builder().build();
 * InferenceBackend backend = new GollekBackendAdapter(sdk);
 * backend.initialize(config);
 *
 * Uni<InferenceResponse> response = backend.infer(request);
 * }</pre>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Full inference support (sync + streaming)</li>
 *   <li>Provider discovery and health checking</li>
 *   <li>Capability detection from Gollek provider registry</li>
 *   <li>Graceful lifecycle management</li>
 * </ul>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public class GollekBackendAdapter implements InferenceBackend {

    private static final Logger LOG = Logger.getLogger(GollekBackendAdapter.class);

    private final GollekSdk gollekSdk;
    private BackendCapabilities capabilities;
    private volatile boolean initialized = false;

    /**
     * Create adapter with pre-configured Gollek SDK instance.
     *
     * @param gollekSdk configured Gollek SDK instance
     */
    public GollekBackendAdapter(GollekSdk gollekSdk) {
        this.gollekSdk = gollekSdk != null ? gollekSdk : GollekSdk.builder().build();
    }

    @Override
    public String name() {
        return "gollek";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public Uni<InferenceResponse> infer(InferenceRequest request) {
        ensureInitialized();

        LOG.debugf("Executing inference via Gollek for request %s", request.requestId());

        tech.kayys.wayang.agent.spi.inference.InferenceRequest gollekRequest = mapToGollekRequest(request);

        return Uni.createFrom().completionStage(
                gollekSdk.createCompletionAsync(gollekRequest)
            )
            .map(this::mapFromGollekResponse)
            .onFailure().transform(err -> {
                LOG.errorf(err, "Gollek inference failed for request %s", request.requestId());
                return new RuntimeException("Gollek inference failed: " + err.getMessage(), err);
            });
    }

    @Override
    public Multi<StreamingChunk> stream(InferenceRequest request) {
        ensureInitialized();

        LOG.debugf("Streaming inference via Gollek for request %s", request.requestId());

        tech.kayys.wayang.agent.spi.inference.InferenceRequest gollekRequest = mapToGollekRequest(request);
        gollekRequest = gollekRequest.toBuilder().stream(true).build();

        Multi<StreamingInferenceChunk> gollekStream = gollekSdk.streamCompletion(gollekRequest);

        return gollekStream
            .map(this::mapFromGollekChunk)
            .onFailure().transform(err -> {
                LOG.errorf(err, "Gollek streaming failed for request %s", request.requestId());
                return new RuntimeException("Gollek streaming failed: " + err.getMessage(), err);
            });
    }

    @Override
    public List<ProviderInfo> listProviders() {
        ensureInitialized();

        try {
            List<tech.kayys.wayang.agent.spi.provider.ProviderInfo> gollekProviders = gollekSdk.listAvailableProviders();

            return gollekProviders.stream()
                .map(this::mapFromGollekProvider)
                .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to list Gollek providers");
            return List.of();
        }
    }

    @Override
    public boolean isHealthy() {
        if (!initialized) {
            return false;
        }

        try {
            // Check if SDK is initialized and has at least one healthy provider
            List<tech.kayys.wayang.agent.spi.provider.ProviderInfo> providers = gollekSdk.listAvailableProviders();
            return providers.stream().anyMatch(p -> p.isHealthy());
        } catch (Exception e) {
            LOG.debugf("Gollek health check failed: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BackendCapabilities capabilities() {
        if (capabilities == null) {
            detectCapabilities();
        }
        return capabilities;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        if (initialized) {
            LOG.debug("GollekBackendAdapter already initialized");
            return;
        }

        LOG.info("Initializing GollekBackendAdapter");

        try {
            // SDK should already be initialized, but verify
            if (gollekSdk == null) {
                throw new IllegalStateException("GollekSdk instance is null");
            }

            detectCapabilities();
            initialized = true;

            LOG.info("GollekBackendAdapter initialized successfully");
        } catch (Exception e) {
            LOG.errorf(e, "Failed to initialize GollekBackendAdapter");
            throw new RuntimeException("Failed to initialize GollekBackendAdapter", e);
        }
    }

    @Override
    public void shutdown() {
        if (!initialized) {
            return;
        }

        LOG.info("Shutting down GollekBackendAdapter");

        try {
            // GollekSdk may have its own shutdown logic
            // gollekSdk.shutdown();  // Uncomment if SDK supports shutdown

            initialized = false;
            LOG.info("GollekBackendAdapter shut down successfully");
        } catch (Exception e) {
            LOG.errorf(e, "Error shutting down GollekBackendAdapter");
        }
    }

    @Override
    public long capabilities() {
        BackendCapabilities caps = capabilities();
        long flags = 0;
        if (caps.streaming()) flags |= CAP_STREAMING;
        if (caps.toolCalling()) flags |= CAP_TOOL_CALLING;
        if (caps.multimodal()) flags |= CAP_MULTIMODAL;
        if (caps.structuredOutput()) flags |= CAP_STRUCTURED_OUTPUT;
        if (caps.parallelTools()) flags |= CAP_PARALLEL_TOOLS;
        return flags;
    }

    // ── Type Mapping ─────────────────────────────────────────────────────

    /**
     * Map backend-agnostic InferenceRequest to Gollek InferenceRequest.
     */
    private tech.kayys.wayang.agent.spi.inference.InferenceRequest mapToGollekRequest(InferenceRequest request) {
        tech.kayys.wayang.agent.spi.inference.InferenceRequest.Builder builder =
            tech.kayys.wayang.agent.spi.inference.InferenceRequest.builder()
                .requestId(request.requestId())
                .model(request.model())
                .temperature(request.temperature())
                .maxTokens(request.maxTokens())
                .topP(request.topP())
                .timeout(request.timeout())
                .stream(request.stream());

        // Map chat messages
        if (!request.messages().isEmpty()) {
            List<tech.kayys.wayang.agent.spi.inference.ChatMessage> gollekMessages = request.messages().stream()
                .map(this::mapToGollekMessage)
                .collect(Collectors.toList());
            builder.messages(gollekMessages);
        }

        // Map tools
        if (!request.tools().isEmpty()) {
            List<tech.kayys.wayang.agent.spi.tool.ToolDefinition> gollekTools = request.tools().stream()
                .map(this::mapToGollekTool)
                .collect(Collectors.toList());
            builder.tools(gollekTools);
        }

        // Tool choice
        if (request.toolChoice() != null) {
            builder.toolChoice(request.toolChoice());
        }

        // Stop sequences
        if (!request.stopSequences().isEmpty()) {
            builder.stop(request.stopSequences());
        }

        return builder.build();
    }

    /**
     * Map Gollek InferenceResponse to backend-agnostic InferenceResponse.
     */
    private InferenceResponse mapFromGollekResponse(tech.kayys.wayang.agent.spi.inference.InferenceResponse response) {
        AssistantMessage message = new AssistantMessage(
            response.getContent(),
            response.getToolCalls() != null ?
                response.getToolCalls().stream()
                    .map(tc -> new ToolCall(tc.id(), tc.type(), tc.name(), tc.arguments()))
                    .collect(Collectors.toList()) :
                List.of()
        );

        TokenUsage usage = response.getUsage() != null ?
            TokenUsage.of(
                response.getUsage().promptTokens(),
                response.getUsage().completionTokens()
            ) : null;

        return InferenceResponse.builder()
            .responseId(response.getResponseId())
            .requestId(response.getRequestId())
            .model(response.getModel())
            .message(message)
            .finishReason(response.getFinishReason())
            .usage(usage)
            .durationMs(response.getDurationMs())
            .build();
    }

    /**
     * Map Gollek StreamingChunk to backend-agnostic StreamingChunk.
     */
    private StreamingChunk mapFromGollekChunk(StreamingInferenceChunk chunk) {
        List<ToolCall> toolCalls = chunk.getToolCalls() != null ?
            chunk.getToolCalls().stream()
                .map(tc -> new ToolCall(tc.id(), tc.type(), tc.name(), tc.arguments()))
                .collect(Collectors.toList()) :
            List.of();

        TokenUsage usage = chunk.getUsage() != null ?
            TokenUsage.of(
                chunk.getUsage().promptTokens(),
                chunk.getUsage().completionTokens()
            ) : null;

        return new StreamingChunk(
            chunk.getResponseId(),
            chunk.getDelta(),
            toolCalls,
            chunk.getFinishReason(),
            usage
        );
    }

    /**
     * Map Gollek ProviderInfo to backend-agnostic ProviderInfo.
     */
    private ProviderInfo mapFromGollekProvider(tech.kayys.wayang.agent.spi.provider.ProviderInfo gollekProvider) {
        return new ProviderInfo(
            gollekProvider.getId(),
            gollekProvider.getName(),
            gollekProvider.getModel(),
            gollekProvider.isHealthy(),
            Map.of(
                "type", gollekProvider.getType(),
                "latencyMs", gollekProvider.getAvgLatencyMs(),
                "successRate", gollekProvider.getSuccessRate()
            )
        );
    }

    /**
     * Map backend-agnostic ChatMessage to Gollek ChatMessage.
     */
    private tech.kayys.wayang.agent.spi.inference.ChatMessage mapToGollekMessage(ChatMessage message) {
        return switch (message) {
            case SystemMessage sm -> tech.kayys.wayang.agent.spi.inference.ChatMessage.system(sm.content());
            case UserMessage um -> {
                if (um.parts().isEmpty()) {
                    yield tech.kayys.wayang.agent.spi.inference.ChatMessage.user(um.content());
                } else {
                    // Multimodal message
                    List<tech.kayys.wayang.agent.spi.inference.ContentPart> parts = um.parts().stream()
                        .map(this::mapToGollekContentPart)
                        .collect(Collectors.toList());
                    yield tech.kayys.wayang.agent.spi.inference.ChatMessage.user(parts);
                }
            }
            case AssistantMessage am -> {
                if (am.toolCalls().isEmpty()) {
                    yield tech.kayys.wayang.agent.spi.inference.ChatMessage.assistant(am.content());
                } else {
                    List<tech.kayys.wayang.agent.spi.inference.ToolCall> toolCalls = am.toolCalls().stream()
                        .map(tc -> new tech.kayys.wayang.agent.spi.inference.ToolCall(
                            tc.id(), tc.type(), tc.name(), tc.arguments()))
                        .collect(Collectors.toList());
                    yield tech.kayys.wayang.agent.spi.inference.ChatMessage.assistantWithTools(am.content(), toolCalls);
                }
            }
            case ToolResultMessage trm -> tech.kayys.wayang.agent.spi.inference.ChatMessage.toolResult(
                trm.toolCallId(), trm.toolName(), trm.content());
        };
    }

    /**
     * Map backend-agnostic ContentPart to Gollek ContentPart.
     */
    private tech.kayys.wayang.agent.spi.inference.ContentPart mapToGollekContentPart(ContentPart part) {
        return switch (part) {
            case TextPart tp -> tech.kayys.wayang.agent.spi.inference.ContentPart.text(tp.text());
            case ImagePart ip -> tech.kayys.wayang.agent.spi.inference.ContentPart.image(ip.imageUrl(), ip.mimeType());
        };
    }

    /**
     * Map backend-agnostic ToolDefinition to Gollek ToolDefinition.
     */
    private tech.kayys.wayang.agent.spi.tool.ToolDefinition mapToGollekTool(ToolDefinition tool) {
        return new tech.kayys.wayang.agent.spi.tool.ToolDefinition(
            tool.name(),
            tool.description(),
            tool.parameters()
        );
    }

    // ── Capability Detection ─────────────────────────────────────────────

    /**
     * Detect capabilities from Gollek provider registry.
     */
    private void detectCapabilities() {
        try {
            List<tech.kayys.wayang.agent.spi.provider.ProviderInfo> providers = gollekSdk.listAvailableProviders();

            boolean streaming = false;
            boolean toolCalling = false;
            boolean multimodal = false;
            List<String> models = new java.util.ArrayList<>();

            for (tech.kayys.wayang.agent.spi.provider.ProviderInfo provider : providers) {
                models.add(provider.getModel());

                // Check capabilities from provider metadata
                Map<String, Object> metadata = provider.getMetadata();
                if (metadata != null) {
                    if (Boolean.TRUE.equals(metadata.get("supports_streaming"))) {
                        streaming = true;
                    }
                    if (Boolean.TRUE.equals(metadata.get("supports_tool_calling"))) {
                        toolCalling = true;
                    }
                    if (Boolean.TRUE.equals(metadata.get("supports_multimodal"))) {
                        multimodal = true;
                    }
                }
            }

            this.capabilities = new BackendCapabilities(
                streaming,
                toolCalling,
                multimodal,
                true,  // Gollek supports structured outputs
                toolCalling,  // Parallel tools if tool calling supported
                multimodal,  // Vision if multimodal supported
                false,  // Audio not yet
                false,  // Embedding separate
                1000,  // Reasonable default
                models
            );

            LOG.debugf("Detected Gollek capabilities: streaming=%s, toolCalling=%s, multimodal=%s",
                streaming, toolCalling, multimodal);
        } catch (Exception e) {
            LOG.warnf("Failed to detect Gollek capabilities, using defaults: %s", e.getMessage());
            this.capabilities = BackendCapabilities.none();
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private void ensureInitialized() {
        if (!initialized) {
            initialize(Map.of());
        }
    }
}

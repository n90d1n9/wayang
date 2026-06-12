package tech.kayys.wayang.agent.backend.gollek;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.StreamingInferenceChunk;
import tech.kayys.wayang.agent.spi.BackendCapabilities;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceResponse;
import tech.kayys.wayang.agent.spi.InferenceTypes.AssistantMessage;
import tech.kayys.wayang.agent.spi.InferenceTypes.ChatMessage;
import tech.kayys.wayang.agent.spi.InferenceTypes.ContentPart;
import tech.kayys.wayang.agent.spi.InferenceTypes.ImagePart;
import tech.kayys.wayang.agent.spi.InferenceTypes.ProviderInfo;
import tech.kayys.wayang.agent.spi.InferenceTypes.StreamingChunk;
import tech.kayys.wayang.agent.spi.InferenceTypes.SystemMessage;
import tech.kayys.wayang.agent.spi.InferenceTypes.TextPart;
import tech.kayys.wayang.agent.spi.InferenceTypes.TokenUsage;
import tech.kayys.wayang.agent.spi.InferenceTypes.ToolCall;
import tech.kayys.wayang.agent.spi.InferenceTypes.ToolDefinition;
import tech.kayys.wayang.agent.spi.InferenceTypes.ToolResultMessage;
import tech.kayys.wayang.agent.spi.InferenceTypes.UserMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapts the current Gollek SDK inference API to the backend-agnostic Wayang
 * {@link InferenceBackend} contract.
 */
public class GollekBackendAdapter implements InferenceBackend {

    private static final Logger LOG = Logger.getLogger(GollekBackendAdapter.class);

    private final GollekSdk gollekSdk;
    private volatile BackendCapabilities capabilities = BackendCapabilities.none();
    private volatile boolean initialized;

    public GollekBackendAdapter(GollekSdk gollekSdk) {
        this.gollekSdk = Objects.requireNonNull(gollekSdk, "gollekSdk");
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

        tech.kayys.gollek.spi.inference.InferenceRequest gollekRequest = toGollekRequest(request);
        long startedAt = System.currentTimeMillis();

        return Uni.createFrom()
                .completionStage(gollekSdk.createCompletionAsync(gollekRequest))
                .map(response -> fromGollekResponse(request, response, startedAt))
                .onFailure().transform(error -> {
                    LOG.errorf(error, "Gollek inference failed for request %s", request.requestId());
                    return new RuntimeException("Gollek inference failed: " + error.getMessage(), error);
                });
    }

    @Override
    public Multi<StreamingChunk> stream(InferenceRequest request) {
        ensureInitialized();

        tech.kayys.gollek.spi.inference.InferenceRequest gollekRequest = toGollekRequest(
                new InferenceRequest(
                        request.requestId(),
                        request.model(),
                        request.messages(),
                        request.tools(),
                        request.toolChoice(),
                        request.temperature(),
                        request.maxTokens(),
                        request.topP(),
                        request.stopSequences(),
                        true,
                        request.timeout(),
                        request.metadata()));

        return gollekSdk.streamCompletion(gollekRequest)
                .map(this::fromGollekChunk)
                .onFailure().transform(error -> {
                    LOG.errorf(error, "Gollek streaming failed for request %s", request.requestId());
                    return new RuntimeException("Gollek streaming failed: " + error.getMessage(), error);
                });
    }

    @Override
    public List<ProviderInfo> listProviders() {
        ensureInitialized();
        try {
            return gollekSdk.listAvailableProviders().stream()
                    .map(this::fromGollekProvider)
                    .toList();
        } catch (Exception error) {
            LOG.warnf("Failed to list Gollek providers: %s", error.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean isHealthy() {
        if (!initialized) {
            return false;
        }
        try {
            return gollekSdk.listAvailableProviders().stream()
                    .anyMatch(provider -> provider.healthStatus()
                            == tech.kayys.gollek.spi.provider.ProviderHealth.Status.HEALTHY);
        } catch (Exception error) {
            LOG.debugf("Gollek health check failed: %s", error.getMessage());
            return false;
        }
    }

    @Override
    public BackendCapabilities capabilitiesInfo() {
        ensureInitialized();
        return capabilities;
    }

    @Override
    public long capabilities() {
        BackendCapabilities caps = capabilitiesInfo();
        long flags = 0;
        if (caps.streaming()) flags |= CAP_STREAMING;
        if (caps.toolCalling()) flags |= CAP_TOOL_CALLING;
        if (caps.multimodal()) flags |= CAP_MULTIMODAL;
        if (caps.structuredOutput()) flags |= CAP_STRUCTURED_OUTPUT;
        if (caps.parallelTools()) flags |= CAP_PARALLEL_TOOLS;
        if (caps.vision()) flags |= CAP_VISION;
        if (caps.audio()) flags |= CAP_AUDIO;
        if (caps.embedding()) flags |= CAP_EMBEDDING;
        return flags;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        if (initialized) {
            return;
        }
        capabilities = detectCapabilities();
        initialized = true;
    }

    @Override
    public void shutdown() {
        initialized = false;
    }

    private tech.kayys.gollek.spi.inference.InferenceRequest toGollekRequest(InferenceRequest request) {
        tech.kayys.gollek.spi.inference.InferenceRequest.Builder builder =
                tech.kayys.gollek.spi.inference.InferenceRequest.builder()
                        .requestId(request.requestId())
                        .model(request.model())
                        .messages(request.messages().stream().map(this::toGollekMessage).toList())
                        .tools(request.tools().stream().map(this::toGollekTool).toList())
                        .toolChoice(request.toolChoice())
                        .temperature(request.temperature())
                        .maxTokens(request.maxTokens())
                        .topP(request.topP())
                        .streaming(request.stream())
                        .timeout(request.timeout())
                        .metadata(request.metadata());

        metadataValue(request, "apiKey", String.class).ifPresent(builder::apiKey);
        metadataValue(request, "preferredProvider", String.class).ifPresent(builder::preferredProvider);
        metadataValue(request, "plugin", String.class).ifPresent(builder::plugin);
        metadataValue(request, "userId", String.class).ifPresent(builder::userId);
        metadataValue(request, "sessionId", String.class).ifPresent(builder::sessionId);
        metadataValue(request, "traceId", String.class).ifPresent(builder::traceId);

        return builder.build();
    }

    private Message toGollekMessage(ChatMessage message) {
        return switch (message) {
            case SystemMessage system -> Message.system(system.content());
            case UserMessage user -> Message.user(userContent(user));
            case AssistantMessage assistant -> Message.assistant(assistant.content());
            case ToolResultMessage tool -> Message.tool(tool.toolCallId(), tool.content());
        };
    }

    private String userContent(UserMessage message) {
        if (message.parts().isEmpty()) {
            return message.content();
        }
        return message.parts().stream()
                .map(this::contentPartText)
                .filter(part -> !part.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse(message.content());
    }

    private String contentPartText(ContentPart part) {
        return switch (part) {
            case TextPart text -> text.text();
            case ImagePart image -> "[image:" + image.mimeType() + ":" + image.imageUrl() + "]";
        };
    }

    private tech.kayys.gollek.spi.tool.ToolDefinition toGollekTool(ToolDefinition tool) {
        return tech.kayys.gollek.spi.tool.ToolDefinition.builder()
                .name(tool.name())
                .description(tool.description())
                .parameters(tool.parameters())
                .build();
    }

    private InferenceResponse fromGollekResponse(
            InferenceRequest request,
            tech.kayys.gollek.spi.inference.InferenceResponse response,
            long startedAt) {

        AssistantMessage message = new AssistantMessage(
                response.getContent(),
                toWayangToolCalls(response.getToolCalls()));

        return InferenceResponse.builder()
                .responseId(responseId(response, request))
                .requestId(response.getRequestId())
                .model(response.getModel())
                .message(message)
                .finishReason(finishReason(response.getFinishReason()))
                .usage(TokenUsage.of(response.getInputTokens(), response.getOutputTokens()))
                .durationMs(durationMs(response, startedAt))
                .build();
    }

    private List<ToolCall> toWayangToolCalls(
            List<tech.kayys.gollek.spi.inference.InferenceResponse.ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        AtomicInteger index = new AtomicInteger();
        return toolCalls.stream()
                .map(call -> new ToolCall(
                        "gollek-tool-call-" + index.incrementAndGet(),
                        "function",
                        call.name(),
                        String.valueOf(call.arguments())))
                .toList();
    }

    private StreamingChunk fromGollekChunk(StreamingInferenceChunk chunk) {
        TokenUsage usage = chunk.usage() == null
                ? null
                : TokenUsage.of(safeInt(chunk.usage().inputTokens()), safeInt(chunk.usage().outputTokens()));

        return new StreamingChunk(
                chunk.requestId(),
                chunk.delta(),
                List.of(),
                chunk.finished() ? finishReason(chunk.finishReason()) : null,
                usage);
    }

    private ProviderInfo fromGollekProvider(tech.kayys.gollek.spi.provider.ProviderInfo provider) {
        String model = provider.supportedModels().stream().findFirst().orElse("");
        boolean healthy = provider.healthStatus()
                == tech.kayys.gollek.spi.provider.ProviderHealth.Status.HEALTHY;

        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "vendor", provider.vendor());
        putIfPresent(metadata, "version", provider.version());
        putIfPresent(metadata, "description", provider.description());
        putIfPresent(metadata, "healthStatus", provider.healthStatus() == null ? null : provider.healthStatus().name());
        putIfPresent(metadata, "supportedModels", provider.supportedModels());
        putIfPresent(metadata, "metadata", provider.metadata());

        return new ProviderInfo(
                provider.id(),
                provider.name(),
                model,
                healthy,
                metadata);
    }

    private BackendCapabilities detectCapabilities() {
        try {
            List<tech.kayys.gollek.spi.provider.ProviderInfo> providers = gollekSdk.listAvailableProviders();
            boolean streaming = false;
            boolean toolCalling = false;
            boolean multimodal = false;
            boolean structuredOutput = false;
            boolean vision = false;
            boolean embedding = false;
            Set<String> supportedModels = new LinkedHashSet<>();

            for (tech.kayys.gollek.spi.provider.ProviderInfo provider : providers) {
                supportedModels.addAll(provider.supportedModels());
                tech.kayys.gollek.spi.provider.ProviderCapabilities caps = provider.capabilities();
                if (caps == null) {
                    continue;
                }
                streaming |= caps.isStreaming();
                toolCalling |= caps.isToolCalling() || caps.isFunctionCalling();
                multimodal |= caps.isMultimodal();
                structuredOutput |= caps.isStructuredOutputs();
                vision |= caps.isMultimodal();
                embedding |= caps.isEmbeddings();
                supportedModels.addAll(caps.getSupportedModels());
            }

            return new BackendCapabilities(
                    streaming,
                    toolCalling,
                    multimodal,
                    structuredOutput,
                    toolCalling,
                    vision,
                    false,
                    embedding,
                    Math.max(1, providers.size()),
                    new ArrayList<>(supportedModels));
        } catch (Exception error) {
            LOG.warnf("Failed to detect Gollek capabilities, using defaults: %s", error.getMessage());
            return BackendCapabilities.none();
        }
    }

    private String responseId(
            tech.kayys.gollek.spi.inference.InferenceResponse response,
            InferenceRequest request) {
        Object responseId = response.getMetadata() == null ? null : response.getMetadata().get("responseId");
        if (responseId != null) {
            return responseId.toString();
        }
        return response.getRequestId() != null ? response.getRequestId() : request.requestId();
    }

    private String finishReason(Object finishReason) {
        return finishReason == null ? "stop" : finishReason.toString().toLowerCase();
    }

    private long durationMs(tech.kayys.gollek.spi.inference.InferenceResponse response, long startedAt) {
        return response.getDurationMs() > 0
                ? response.getDurationMs()
                : System.currentTimeMillis() - startedAt;
    }

    private int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private <T> Optional<T> metadataValue(InferenceRequest request, String key, Class<T> type) {
        Object value = request.metadata().get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            initialize(Map.of());
        }
    }
}

package tech.kayys.wayang.agent.core.inference;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.wayang.agent.core.client.GollekAgentClient;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.inference.StreamingInferenceChunk;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.tool.ToolDefinition;
import tech.kayys.gollek.sdk.mcp.McpServerSummary;
import tech.kayys.gollek.sdk.mcp.McpToolModel;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Shared inference service for all Wayang agents.
 * Provides a simplified interface to the Gollek SDK for inference requests.
 *
 * <p>
 * This service is injected into agent executors via CDI and handles:
 * <ul>
 * <li>Provider selection and fallback</li>
 * <li>Request/response mapping</li>
 * <li>Error handling and retry logic</li>
 * <li>Performance tracking</li>
 * <li>Streaming inference (via {@code GollekSdk.streamCompletion})</li>
 * <li>MCP tool discovery and injection</li>
 * </ul>
 * 
 * <p><strong>Enhanced with GollekAgentClient for:</strong>
 * <ul>
 * <li>Circuit breaker pattern</li>
 * <li>Automatic retry with exponential backoff</li>
 * <li>Provider-aware intelligent routing</li>
 * <li>Native tool calling support</li>
 * </ul>
 */
@ApplicationScoped
public class GollekInferenceService {
    private static final Logger log = LoggerFactory.getLogger(GollekInferenceService.class);

    @Inject
    GollekAgentClient agentClient;  // Enhanced: Use GollekAgentClient instead of GollekSdk

    @Inject
    tech.kayys.wayang.agent.spi.memory.AgentMemoryManager memoryService;

    @Inject
    jakarta.enterprise.inject.Instance<tech.kayys.wayang.memory.spi.DirectAgentContext> directAgentContexts;

    @Inject
    tech.kayys.wayang.agent.core.tools.ToolRegistry toolRegistry;

    // ==================== Synchronous Inference ====================

    /**
     * Perform synchronous inference (single-shot, no tool loop).
     *
     * @param request Agent inference request
     * @return Agent inference response
     */
    public AgentInferenceResponse infer(AgentInferenceRequest request) {
        Instant start = Instant.now();

        try {
            String systemPromptWithMemory = injectMemoryContext(request);
            List<ToolDefinition> allTools = resolveTools(request);
            InferenceRequest gollekRequest = buildGollekRequest(request, systemPromptWithMemory, allTools);
            
            // Enhanced: Use GollekAgentClient with circuit breaker and retry
            InferenceResponse gollekResponse = agentClient.infer(gollekRequest)
                .await().atMost(Duration.ofSeconds(30));
                
            Duration latency = Duration.between(start, Instant.now());
            AgentInferenceResponse response = mapToAgentResponse(gollekResponse, latency);
            storeInteractionMemory(request, gollekResponse);
            return response;

        } catch (Exception e) {
            log.error("Inference failed: {}", e.getMessage(), e);
            return AgentInferenceResponse.builder()
                    .error(e.getMessage())
                    .latency(Duration.between(start, Instant.now()))
                    .build();
        }
    }

    // ==================== ReAct Tool Execution Loop ====================

    /**
     * Perform inference with a ReAct-style tool execution loop.
     *
     * <p>
     * Flow:
     * <ol>
     * <li>Send messages + tools to LLM</li>
     * <li>If LLM returns tool_calls → execute each tool via
     * {@code ToolRegistry}</li>
     * <li>Append assistant message (with tool_calls) + tool result messages</li>
     * <li>Re-call LLM with updated messages</li>
     * <li>Repeat until LLM returns a STOP finish_reason or max iterations
     * reached</li>
     * </ol>
     *
     * @param request Agent inference request (with tools and maxToolIterations)
     * @return Agent inference response with tool execution audit trail
     */
    public AgentInferenceResponse inferWithToolLoop(AgentInferenceRequest request) {
        Instant start = Instant.now();
        int maxIterations = request.getMaxToolIterations();
        List<ToolDefinition> tools = resolveTools(request);

        // If no tools, fall back to simple inference
        if (tools == null || tools.isEmpty() || maxIterations <= 0) {
            return infer(request);
        }

        try {
            String systemPromptWithMemory = injectMemoryContext(request);

            // Build the initial message list
            List<Message> messages = buildMessageList(request, systemPromptWithMemory);

            List<AgentInferenceResponse.ToolExecutionResult> allToolResults = new ArrayList<>();
            InferenceResponse lastResponse = null;
            int iteration = 0;
            int totalInputTokens = 0;
            int totalOutputTokens = 0;

            for (iteration = 0; iteration < maxIterations; iteration++) {
                // Build and execute inference request
                InferenceRequest gollekRequest = buildGollekRequestFromMessages(
                        request, messages, tools);
                lastResponse = agentClient.infer(gollekRequest)
                        .await().atMost(Duration.ofSeconds(30));

                totalInputTokens += lastResponse.getInputTokens();
                totalOutputTokens += lastResponse.getOutputTokens();

                // Check if LLM wants to call tools
                if (!lastResponse.hasToolCalls()) {
                    log.debug("ReAct loop completed after {} iterations (no more tool calls)", iteration);
                    break;
                }

                log.debug("ReAct iteration {}: LLM requested {} tool calls",
                        iteration, lastResponse.getToolCalls().size());

                // Add assistant message with tool calls to conversation
                // Build tool calls list for the Message
                List<tech.kayys.gollek.spi.tool.ToolCall> spiToolCalls = lastResponse.getToolCalls().stream()
                        .map(tc -> tech.kayys.gollek.spi.tool.ToolCall.builder()
                                .name(tc.name())
                                .arguments(tc.arguments())
                                .build())
                        .toList();

                messages.add(new Message(
                        Message.Role.ASSISTANT,
                        lastResponse.getContent(),
                        null,
                        null,
                        spiToolCalls,
                        null));

                // Execute each tool call and add results
                for (var toolCall : lastResponse.getToolCalls()) {
                    Instant toolStart = Instant.now();
                    String toolCallId = "call_" + System.nanoTime();

                    try {
                        // Execute tool via ToolRegistry
                        Map<String, Object> toolResult = toolRegistry
                                .executeTool(toolCall.name(), toolCall.arguments(), Map.of())
                                .await().indefinitely();

                        String resultJson = toolResult != null ? toolResult.toString() : "{}";

                        // Add tool result message
                        messages.add(Message.tool(toolCallId, resultJson));

                        allToolResults.add(new AgentInferenceResponse.ToolExecutionResult(
                                toolCall.name(),
                                toolCall.arguments(),
                                toolResult,
                                true,
                                null,
                                Duration.between(toolStart, Instant.now()).toMillis()));

                        log.debug("Tool '{}' executed successfully", toolCall.name());

                    } catch (Exception e) {
                        log.warn("Tool '{}' execution failed: {}", toolCall.name(), e.getMessage());

                        String errorMsg = "Tool execution failed: " + e.getMessage();
                        messages.add(Message.tool(toolCallId, errorMsg));

                        allToolResults.add(new AgentInferenceResponse.ToolExecutionResult(
                                toolCall.name(),
                                toolCall.arguments(),
                                null,
                                false,
                                e.getMessage(),
                                Duration.between(toolStart, Instant.now()).toMillis()));
                    }
                }
            }

            if (lastResponse == null) {
                return AgentInferenceResponse.builder()
                        .error("No inference response generated")
                        .latency(Duration.between(start, Instant.now()))
                        .build();
            }

            // Build final response
            Duration latency = Duration.between(start, Instant.now());
            AgentInferenceResponse response = mapToAgentResponse(lastResponse, latency);
            response.setToolResults(allToolResults);
            response.setIterations(iteration);
            response.setTotalTokens(totalInputTokens + totalOutputTokens);
            response.setPromptTokens(totalInputTokens);
            response.setCompletionTokens(totalOutputTokens);

            // Store final interaction in memory
            storeInteractionMemory(request, lastResponse);

            return response;

        } catch (Exception e) {
            log.error("ReAct loop failed: {}", e.getMessage(), e);
            return AgentInferenceResponse.builder()
                    .error(e.getMessage())
                    .latency(Duration.between(start, Instant.now()))
                    .build();
        }
    }

    // ==================== Async Inference ====================

    /**
     * Perform asynchronous inference using Mutiny Uni.
     *
     * @param request Agent inference request
     * @return Uni containing agent inference response
     */
    public Uni<AgentInferenceResponse> inferAsync(AgentInferenceRequest request) {
        return Uni.createFrom().item(() -> infer(request));
    }

    // ==================== Streaming Inference ====================

    /**
     * Perform streaming inference. Returns a reactive {@link Multi} of
     * {@link StreamingInferenceChunk} objects that can be consumed as they arrive.
     *
     * <p>
     * This is useful for real-time UI updates, progressive rendering,
     * and scenarios where low latency to first token matters.
     *
     * @param request Agent inference request
     * @return Multi stream of chunks
     */
    public Multi<StreamingInferenceChunk> inferStream(AgentInferenceRequest request) {
        // 1. Inject memory context
        String systemPromptWithMemory = injectMemoryContext(request);

        // 2. Merge MCP + skill tools
        List<ToolDefinition> allTools = resolveTools(request);

        // 3. Build request with streaming enabled
        AgentInferenceRequest streamingRequest = AgentInferenceRequest.builder()
                .systemPrompt(request.getSystemPrompt())
                .userPrompt(request.getUserPrompt())
                .preferredProvider(request.getPreferredProvider())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .model(request.getModel())
                .additionalParams(request.getAdditionalParams())
                .agentId(request.getAgentId())
                .useMemory(request.getUseMemory())
                .stream(true)
                .tools(allTools)
                .build();

        InferenceRequest gollekRequest = buildGollekRequest(streamingRequest, systemPromptWithMemory, allTools);

        try {
            // Enhanced: Use GollekAgentClient streaming
            return agentClient.stream(gollekRequest);
        } catch (Exception e) {
            log.error("Streaming inference failed: {}", e.getMessage(), e);
            return Multi.createFrom().failure(e);
        }
    }

    // ==================== Fallback Inference ====================

    /**
     * Perform inference with automatic fallback to alternate provider.
     *
     * @param request          Agent inference request
     * @param fallbackProvider Fallback provider ID if primary fails
     * @return Agent inference response
     */
    public AgentInferenceResponse inferWithFallback(
            AgentInferenceRequest request,
            String fallbackProvider) {

        // Try primary provider
        AgentInferenceResponse response = infer(request);

        // If failed and fallback is specified, try fallback
        if (response.isError() && fallbackProvider != null) {
            log.warn("Primary inference failed, trying fallback provider: {}", fallbackProvider);

            AgentInferenceRequest fallbackRequest = AgentInferenceRequest.builder()
                    .systemPrompt(request.getSystemPrompt())
                    .userPrompt(request.getUserPrompt())
                    .preferredProvider(fallbackProvider)
                    .temperature(request.getTemperature())
                    .maxTokens(request.getMaxTokens())
                    .model(request.getModel())
                    .additionalParams(request.getAdditionalParams())
                    .agentId(request.getAgentId())
                    .useMemory(request.getUseMemory())
                    .stream(request.getStream() != null ? request.getStream() : false)
                    .tools(request.getTools()) // ← FIX: carry tools into fallback
                    .build();

            response = infer(fallbackRequest);
        }

        return response;
    }

    // ==================== MCP Tools Discovery ====================

    /**
     * Discover enabled MCP servers from the Gollek SDK.
     * The actual MCP tool execution is handled by the inference engine at request
     * time
     * when MCP servers are registered. This method returns the list of enabled
     * server names
     * so they can be included in request metadata for the engine to activate.
     *
     * @return list of enabled MCP server names
     */
    public List<String> discoverEnabledMcpServers() {
        try {
            // Enhanced: Use GollekAgentClient's MCP registry
            var mcpRegistry = agentClient.mcpRegistry();
            if (mcpRegistry == null) {
                return List.of();
            }

            var servers = mcpRegistry.list();
            List<String> enabledServers = servers.stream()
                    .filter(tech.kayys.gollek.sdk.mcp.McpServerSummary::enabled)
                    .map(tech.kayys.gollek.sdk.mcp.McpServerSummary::name)
                    .toList();

            log.debug("Discovered {} enabled MCP servers out of {} total",
                    enabledServers.size(), servers.size());
            return enabledServers;

        } catch (UnsupportedOperationException e) {
            log.trace("MCP not supported by current GollekSdk implementation");
            return List.of();
        } catch (Exception e) {
            log.warn("MCP server discovery failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ==================== Provider & Model Operations ====================

    /**
     * List all available providers.
     *
     * @return List of provider IDs
     */
    public List<String> listAvailableProviders() {
        try {
            // Enhanced: Use GollekAgentClient's provider registry
            return agentClient.getAvailableProviders()
                    .await().atMost(Duration.ofSeconds(10));
        } catch (Exception e) {
            log.error("Failed to list providers: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get information about a specific provider.
     *
     * @param providerId Provider ID
     * @return Provider information, or null if not found
     */
    public tech.kayys.gollek.spi.provider.ProviderInfo getProviderInfo(String providerId) {
        try {
            return agentClient.getProviderInfo(providerId);
        } catch (Exception e) {
            log.error("Failed to get provider info for {}: {}", providerId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * List all available models.
     */
    public List<tech.kayys.gollek.spi.model.ModelInfo> listModels() {
        try {
            return agentClient.listModels();
        } catch (Exception e) {
            log.error("Failed to list models: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ==================== Internal Helpers ====================

    /**
     * Resolve tools for the inference request.
     * Merges explicit tools from the request with discovered tools from enabled MCP
     * servers.
     */
    private List<ToolDefinition> resolveTools(AgentInferenceRequest request) {
        List<ToolDefinition> tools = new ArrayList<>();

        // 1. Add tools explicitly requested (if any)
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            tools.addAll(request.getTools());
        }

        // 2. Discover and bridge MCP tools from enabled servers
        try {
            // Enhanced: Use GollekAgentClient's MCP registry
            var mcpRegistry = agentClient.mcpRegistry();
            if (mcpRegistry != null) {
                List<McpServerSummary> servers = mcpRegistry.list();
                for (McpServerSummary server : servers) {
                    if (server.enabled()) {
                        log.debug("Bridging tools from MCP server: {}", server.name());
                        try {
                            List<McpToolModel> mcpTools = mcpRegistry.listTools(server.name());
                            for (McpToolModel mcpTool : mcpTools) {
                                tools.add(ToolDefinition.fromMCPSchema(
                                        mcpTool.name(),
                                        mcpTool.description(),
                                        mcpTool.inputSchema()));
                            }
                        } catch (Exception e) {
                            log.warn("Failed to list tools for MCP server {}: {}", server.name(), e.getMessage());
                        }
                    }
                }
            }
        } catch (UnsupportedOperationException e) {
            log.trace("MCP bridging not supported by current GollekSdk implementation");
        } catch (Exception e) {
            log.warn("MCP tool bridging failed: {}", e.getMessage());
        }

        return tools.isEmpty() ? null : tools;
    }

    /**
     * Inject memory context into the system prompt if enabled.
     */
    private String injectMemoryContext(AgentInferenceRequest request) {
        String systemPrompt = request.getSystemPrompt();

        if (Boolean.TRUE.equals(request.getUseMemory()) && request.getAgentId() != null) {
            try {
                String memoryContext = memoryService
                        .retrieveContext(request.getAgentId(), request.getUserPrompt(), 5)
                        .await().indefinitely();
                if (memoryContext != null && !memoryContext.isBlank()) {
                    systemPrompt = (systemPrompt != null ? systemPrompt : "")
                            + "\n\nRelevant Context:\n" + memoryContext;
                    log.debug("Injected memory context for agent {}", request.getAgentId());
                }
            } catch (Exception e) {
                log.warn("Memory context retrieval failed for agent {}: {}",
                        request.getAgentId(), e.getMessage());
            }
        }

        return systemPrompt;
    }

    /**
     * Store the user/assistant interaction in memory if enabled.
     */
    private void storeInteractionMemory(AgentInferenceRequest request, InferenceResponse gollekResponse) {
        if (Boolean.TRUE.equals(request.getUseMemory()) && request.getAgentId() != null) {
            memoryService.storeMemory(request.getAgentId(),
                    "User: " + request.getUserPrompt() + "\nAssistant: " + gollekResponse.getContent(),
                    null).subscribe().with(
                            id -> log.debug("Stored interaction memory: {}", id),
                            err -> log.warn("Failed to store interaction memory: {}", err.getMessage()));
        }
    }

    /**
     * Build the initial message list from the agent request.
     * Includes: system prompt → conversation history → current user message.
     */
    private List<Message> buildMessageList(AgentInferenceRequest request, String systemPrompt) {
        List<Message> messages = new ArrayList<>();

        // System prompt
        String finalSystemPrompt = systemPrompt != null ? systemPrompt : request.getSystemPrompt();
        if (finalSystemPrompt != null && !finalSystemPrompt.isBlank()) {
            messages.add(Message.system(finalSystemPrompt));
        }

        // Conversation history (multi-turn)
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            messages.addAll(request.getConversationHistory());
        }

        // Current user message
        if (request.getUserPrompt() != null && !request.getUserPrompt().isBlank()) {
            messages.add(Message.user(request.getUserPrompt()));
        }

        return messages;
    }

    /**
     * Build Gollek inference request from agent request — thread-safe.
     */
    private InferenceRequest buildGollekRequest(
            AgentInferenceRequest request,
            String overrideSystemPrompt,
            List<ToolDefinition> tools) {

        List<Message> messages = buildMessageList(request, overrideSystemPrompt);
        return buildGollekRequestFromMessages(request, messages, tools);
    }

    /**
     * Build Gollek inference request from an explicit message list.
     * Used by the ReAct loop where messages accumulate across iterations.
     */
    private InferenceRequest buildGollekRequestFromMessages(
            AgentInferenceRequest request,
            List<Message> messages,
            List<ToolDefinition> tools) {

        int maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : 2048;

        InferenceRequest.Builder builder = InferenceRequest.builder()
                .messages(messages)
                .model(request.getModel())
                .temperature(request.getTemperature())
                .maxTokens(maxTokens)
                .streaming(request.getStream() != null ? request.getStream() : false);

        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools);
        }

        if (Boolean.TRUE.equals(request.getUseMemory()) && request.getAgentId() != null 
                && directAgentContexts != null && directAgentContexts.isResolvable()) {
            try {
                // Fetch native memory segment zero-copy
                java.lang.foreign.MemorySegment nativeContext = directAgentContexts.get()
                        .getNativeContext(request.getAgentId(), java.lang.foreign.Arena.global())
                        .await().indefinitely()
                        .orElse(null);
                if (nativeContext != null) {
                    builder.nativeContextSegment(nativeContext);
                    log.debug("Injected zero-copy native memory segment for agent {}", request.getAgentId());
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve native memory context for agent {}: {}", request.getAgentId(), e.getMessage());
            }
        }

        String preferredProvider = resolvePreferredProvider(request);
        if (preferredProvider != null && !preferredProvider.isBlank()) {
            builder.preferredProvider(preferredProvider);
        }

        resolveApiKey(request, preferredProvider).ifPresent(builder::apiKey);
        Map<String, Object> metadata = buildRequestMetadata(request, preferredProvider);
        if (!metadata.isEmpty()) {
            builder.metadata(metadata);
        }

        return builder.build();
    }

    private String resolvePreferredProvider(AgentInferenceRequest request) {
        if (request.getPreferredProvider() != null && !request.getPreferredProvider().isBlank()) {
            return request.getPreferredProvider();
        }

        Map<String, Object> context = requestContextMap(request);
        if (context.isEmpty()) {
            return null;
        }

        String providerMode = stringValue(context.get("providerMode"));
        String directPreferred = firstNonBlank(
                stringValue(context.get("preferredProvider")),
                stringValue(context.get("provider")));
        if (directPreferred != null) {
            return directPreferred;
        }

        Map<String, Object> cloudProvider = mapValue(context.get("cloudProvider"));
        Map<String, Object> localProvider = mapValue(context.get("localProvider"));

        if ("cloud".equalsIgnoreCase(providerMode)) {
            return firstNonBlank(
                    stringValue(cloudProvider.get("providerId")),
                    stringValue(context.get("fallbackProvider")),
                    stringValue(localProvider.get("providerId")));
        }
        if ("local".equalsIgnoreCase(providerMode)) {
            return firstNonBlank(
                    stringValue(localProvider.get("providerId")),
                    stringValue(context.get("fallbackProvider")),
                    stringValue(cloudProvider.get("providerId")));
        }

        return firstNonBlank(
                stringValue(cloudProvider.get("providerId")),
                stringValue(localProvider.get("providerId")),
                stringValue(context.get("fallbackProvider")));
    }

    private Optional<String> resolveApiKey(AgentInferenceRequest request, String providerId) {
        Map<String, String> credentials = resolvedCredentialMap(request);
        if (credentials.isEmpty()) {
            return Optional.empty();
        }

        String providerHint = providerHint(providerId);
        if (!providerHint.isBlank()) {
            for (Map.Entry<String, String> entry : credentials.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                if (key.contains(providerHint) && looksLikeSecretKeyName(key)) {
                    return Optional.of(entry.getValue());
                }
            }
        }

        for (Map.Entry<String, String> entry : credentials.entrySet()) {
            if (looksLikeSecretKeyName(entry.getKey().toLowerCase(Locale.ROOT))) {
                return Optional.of(entry.getValue());
            }
        }

        if (credentials.size() == 1) {
            return Optional.of(credentials.values().iterator().next());
        }

        return Optional.empty();
    }

    private Map<String, Object> buildRequestMetadata(AgentInferenceRequest request, String preferredProvider) {
        Map<String, Object> context = requestContextMap(request);
        if (context.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        String providerMode = stringValue(context.get("providerMode"));
        if (providerMode != null) {
            metadata.put("providerMode", providerMode);
        }
        if (preferredProvider != null && !preferredProvider.isBlank()) {
            metadata.put("preferredProvider", preferredProvider);
        }
        Map<String, String> credentials = resolvedCredentialMap(request);
        if (!credentials.isEmpty()) {
            metadata.put("resolvedCredentialNames", credentials.keySet().stream().sorted().toList());
        }
        return metadata;
    }

    private Map<String, Object> requestContextMap(AgentInferenceRequest request) {
        Map<String, Object> additional = request.getAdditionalParams();
        if (additional == null || additional.isEmpty()) {
            return Map.of();
        }
        Object context = additional.get("context");
        if (context instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Collections.emptyMap();
    }

    private Map<String, String> resolvedCredentialMap(AgentInferenceRequest request) {
        Map<String, String> resolved = new LinkedHashMap<>();
        Map<String, Object> additional = request.getAdditionalParams();
        if (additional != null) {
            mergeCredentialMap(resolved, additional.get("_resolvedCredentials"));
        }
        Map<String, Object> context = requestContextMap(request);
        if (!context.isEmpty()) {
            mergeCredentialMap(resolved, context.get("_resolvedCredentials"));
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private static void mergeCredentialMap(Map<String, String> target, Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return;
        }
        map.forEach((k, v) -> {
            if (k == null || v == null) {
                return;
            }
            String key = String.valueOf(k).trim();
            String value = String.valueOf(v).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                target.putIfAbsent(key, value);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }

    private static String stringValue(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean looksLikeSecretKeyName(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("apikey")
                || normalized.contains("api-key")
                || normalized.contains("api_key")
                || normalized.contains("token")
                || normalized.endsWith("key");
    }

    private static String providerHint(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return "";
        }
        String normalized = providerId.toLowerCase(Locale.ROOT);
        if (normalized.contains("openai"))
            return "openai";
        if (normalized.contains("anthropic"))
            return "anthropic";
        if (normalized.contains("gemini") || normalized.contains("google"))
            return "gemini";
        if (normalized.contains("mistral"))
            return "mistral";
        if (normalized.contains("cerebras"))
            return "cerebras";
        if (normalized.contains("azure"))
            return "azure";
        if (normalized.contains("ollama"))
            return "ollama";
        return normalized;
    }

    /**
     * Map Gollek inference response to agent response.
     */
    private AgentInferenceResponse mapToAgentResponse(
            InferenceResponse gollekResponse,
            Duration latency) {

        // Extract provider from response metadata if available
        String providerUsed = null;
        if (gollekResponse.getMetadata() != null) {
            Object prov = gollekResponse.getMetadata().get("provider");
            if (prov != null) {
                providerUsed = String.valueOf(prov);
            }
        }

        return AgentInferenceResponse.builder()
                .content(gollekResponse.getContent())
                .providerUsed(providerUsed)
                .modelUsed(gollekResponse.getModel())
                .promptTokens(gollekResponse.getInputTokens())
                .completionTokens(gollekResponse.getOutputTokens())
                .totalTokens(gollekResponse.getTokensUsed())
                .latency(latency)
                .cached(false)
                .toolCalls(gollekResponse.getToolCalls())
                .finishReason(gollekResponse.getFinishReason() != null ? gollekResponse.getFinishReason().name() : null)
                .build();
    }
}

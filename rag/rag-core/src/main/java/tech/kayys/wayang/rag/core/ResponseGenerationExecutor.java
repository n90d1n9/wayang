package tech.kayys.wayang.rag.core;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.error.ErrorInfo;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.gamelan.sdk.executor.core.AbstractWorkflowExecutor;
import tech.kayys.gamelan.engine.protocol.CommunicationType;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.wayang.security.secrets.core.SecretManager;
import tech.kayys.wayang.security.secrets.dto.RetrieveSecretRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG RESPONSE GENERATION EXECUTOR - INTERNAL IMPLEMENTATION
 */
@Executor(executorType = "rag-response-generation", communicationType = CommunicationType.GRPC, maxConcurrentTasks = 15, supportedNodeTypes = {
                "TASK", "RAG_GENERATION" }, version = "1.0.1")
@ApplicationScoped
public class ResponseGenerationExecutor extends AbstractWorkflowExecutor {

        private static final Logger LOG = LoggerFactory.getLogger(ResponseGenerationExecutor.class);

        public record GenerationResult(String response, List<Citation> citations, int tokensUsed) {
        }

        public record GenerationContext(
                        String query,
                        List<String> contexts,
                        List<Map<String, Object>> contextMetadata,
                        List<ConversationTurn> conversationHistory,
                        GenerationConfig config,
                        boolean includeCitations,
                        boolean useCache,
                        String templateId,
                        String apiKey) {
        }

        @Inject
        GollekSdk gollekSdk;
        @Inject
        PromptTemplateService promptTemplateService;
        @Inject
        CitationService citationService;
        @Inject
        ResponseGuardrailEngine guardrailEngine;
        @Inject
        ResponseCacheService cacheService;
        @Inject
        GenerationMetricsCollector metricsCollector;
        @Inject
        SecretManager secretManager;

        @ConfigProperty(name = "gamelan.rag.generation.provider", defaultValue = "gollek")
        String defaultProvider;

        @ConfigProperty(name = "gamelan.rag.generation.model", defaultValue = "Qwen/Qwen2.5-0.5B-Instruct")
        String defaultModel;

        @ConfigProperty(name = "gamelan.rag.generation.temperature", defaultValue = "0.7")
        double defaultTemperature;

        @ConfigProperty(name = "gamelan.rag.generation.max-tokens", defaultValue = "1000")
        int defaultMaxTokens;

        @ConfigProperty(name = "gamelan.rag.generation.include-citations", defaultValue = "true")
        boolean defaultIncludeCitations;

        @ConfigProperty(name = "gamelan.rag.generation.use-cache", defaultValue = "true")
        boolean defaultUseCache;

        @ConfigProperty(name = "gamelan.rag.generation.timeout", defaultValue = "60")
        int timeoutSeconds;

        @Override
        public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
                LOG.info("Starting response generation for run: {}, node: {}",
                                task.runId().value(), task.nodeId().value());

                Instant startTime = Instant.now();
                Map<String, Object> context = task.context();

                GenerationContext genCtx = extractConfiguration(context);

                return validateConfiguration(genCtx)
                                .flatMap(valid -> {
                                        if (!valid) {
                                                return Uni.createFrom().item(SimpleNodeExecutionResult.failure(
                                                                task.runId(), task.nodeId(), task.attempt(),
                                                                new ErrorInfo(
                                                                                "CONFIG_INVALID",
                                                                                "Invalid generation configuration",
                                                                                "",
                                                                                Map.of("retryable", false)),
                                                                task.token()));
                                        }

                                        // Check cache
                                        if (genCtx.useCache()) {
                                                String cacheKey = generateCacheKey(genCtx);
                                                String cachedResponse = cacheService.get(cacheKey);

                                                if (cachedResponse != null) {
                                                        LOG.info("Cache hit for query: {}", genCtx.query());
                                                        return Uni.createFrom().item(SimpleNodeExecutionResult.success(
                                                                        task.runId(), task.nodeId(), task.attempt(),
                                                                        Map.of("response", cachedResponse, "cached",
                                                                                        true,
                                                                                        "query", genCtx.query()),
                                                                        task.token(),
                                                                        Duration.ZERO));
                                                }
                                        }

                                        return resolveApiKey(genCtx, task.runId().value())
                                                        .flatMap(apiKey -> generateResponse(genCtx, apiKey,
                                                                        task.runId().value()))
                                                        .map(result -> {
                                                                long durationMs = Duration
                                                                                .between(startTime, Instant.now())
                                                                                .toMillis();

                                                                metricsCollector.recordGeneration(
                                                                                task.runId().value(),
                                                                                result.tokensUsed(), durationMs);

                                                                if (genCtx.useCache()) {
                                                                        cacheService.put(generateCacheKey(genCtx),
                                                                                        result.response());
                                                                }

                                                                return SimpleNodeExecutionResult.success(
                                                                                task.runId(), task.nodeId(),
                                                                                task.attempt(),
                                                                                Map.of(
                                                                                                "response",
                                                                                                result.response(),
                                                                                                "citations",
                                                                                                result.citations(),
                                                                                                "tokensUsed",
                                                                                                result.tokensUsed(),
                                                                                                "durationMs",
                                                                                                durationMs,
                                                                                                "model",
                                                                                                genCtx.config().model(),
                                                                                                "cached", false,
                                                                                                "query",
                                                                                                genCtx.query()),
                                                                                task.token(),
                                                                                Duration.ofMillis(durationMs));
                                                        })
                                                        .onFailure().recoverWithItem(error -> {
                                                                LOG.error("Response generation failed", error);
                                                                return SimpleNodeExecutionResult.failure(
                                                                                task.runId(), task.nodeId(),
                                                                                task.attempt(),
                                                                                new ErrorInfo(
                                                                                                "INFERENCE_REQUEST_FAILED",
                                                                                                error.getMessage(),
                                                                                                "",
                                                                                                Map.of("retryable",
                                                                                                                true)),
                                                                                task.token());
                                                        });
                                });
        }

        private Uni<String> resolveApiKey(GenerationContext genCtx, String tenantId) {
                if (genCtx.apiKey() != null && !genCtx.apiKey().isBlank()) {
                        return Uni.createFrom().item(genCtx.apiKey());
                }

                String secretPath = String.format("services/%s/api-key", genCtx.config().provider());
                LOG.debug("Resolving API key from Vault for tenant: {}, path: {}", tenantId, secretPath);

                return secretManager.retrieve(RetrieveSecretRequest.latest(tenantId, secretPath))
                                .map(secret -> secret.data().get("api_key"))
                                .onFailure().recoverWithItem(() -> {
                                        LOG.warn("Failed to retrieve API key from Vault for path: {}. Falling back to env.",
                                                        secretPath);
                                        return System.getenv("OPENAI_API_KEY");
                                });
        }

        private Uni<GenerationResult> generateResponse(GenerationContext genCtx, String apiKey, String workflowRunId) {
                LOG.debug("Generating response for query: '{}' using model: {}",
                                genCtx.query(), genCtx.config().model());

                return Uni.createFrom().item(() -> {
                        try {
                                List<Message> messages = buildMessages(genCtx);

                                InferenceRequest request = InferenceRequest.builder()
                                                .model(genCtx.config().model())
                                                .preferredProvider(genCtx.config().provider())
                                                .messages(messages)
                                                .temperature(genCtx.config().temperature())
                                                .maxTokens(genCtx.config().maxTokens())
                                                .apiKey(apiKey)
                                                .build();

                                InferenceResponse response = gollekSdk.createCompletion(request);
                                String responseText = response.getContent();

                                responseText = guardrailEngine.validateAndSanitize(responseText, genCtx.config());

                                List<Citation> citations = Collections.emptyList();
                                if (genCtx.includeCitations() && !genCtx.contexts().isEmpty()) {
                                        citations = citationService.generateCitations(
                                                        responseText, genCtx.contexts(), genCtx.contextMetadata());
                                }

                                int tokensUsed = response.getTokensUsed();

                                return new GenerationResult(responseText, citations, tokensUsed);
                        } catch (tech.kayys.gollek.sdk.exception.SdkException e) {
                                throw new RuntimeException("Inference failed", e);
                        }
                });
        }

        private List<Message> buildMessages(GenerationContext genCtx) {
                List<Message> messages = new ArrayList<>();

                String systemPrompt = promptTemplateService.getSystemPrompt(genCtx.config());
                messages.add(Message.system(systemPrompt));

                String userPrompt = promptTemplateService.buildUserPrompt(
                                genCtx.query(), genCtx.contexts(), genCtx.conversationHistory());
                messages.add(Message.user(userPrompt));

                return messages;
        }

        @SuppressWarnings("unchecked")
        private GenerationContext extractConfiguration(Map<String, Object> context) {
                String query = (String) context.get("query");
                List<String> contexts = (List<String>) context.getOrDefault("contexts", List.of());
                List<Map<String, Object>> contextMetadata = (List<Map<String, Object>>) context.getOrDefault("metadata",
                                List.of());
                List<ConversationTurn> history = extractConversationHistory(context);

                String provider = (String) context.getOrDefault("provider", defaultProvider);
                String model = (String) context.getOrDefault("model", defaultModel);
                String apiKey = (String) context.getOrDefault("apiKey", System.getenv("OPENAI_API_KEY"));

                double temperature = context.containsKey("temperature")
                                ? ((Number) context.get("temperature")).doubleValue()
                                : defaultTemperature;
                int maxTokens = context.containsKey("maxTokens") ? ((Number) context.get("maxTokens")).intValue()
                                : defaultMaxTokens;
                boolean includeCitations = context.containsKey("includeCitations")
                                ? (Boolean) context.get("includeCitations")
                                : defaultIncludeCitations;
                boolean useCache = context.containsKey("useCache") ? (Boolean) context.get("useCache")
                                : defaultUseCache;
                String templateId = (String) context.getOrDefault("templateId", "default");

                GenerationConfig config = new GenerationConfig(provider, model, (float) temperature, maxTokens,
                                1.0f, 0.0f, 0.0f, List.of(), "You are a helpful assistant.",
                                Map.of(), includeCitations, false, CitationStyle.INLINE_NUMBERED,
                                false, false, Map.of());

                return new GenerationContext(query, contexts, contextMetadata, history,
                                config, includeCitations, useCache, templateId, apiKey);
        }

        @SuppressWarnings("unchecked")
        private List<ConversationTurn> extractConversationHistory(Map<String, Object> context) {
                if (!context.containsKey("conversationHistory"))
                        return List.of();

                List<Map<String, Object>> historyList = (List<Map<String, Object>>) context.get("conversationHistory");

                return historyList.stream()
                                .map(turn -> new ConversationTurn(
                                                (String) turn.get("role"), (String) turn.get("content"), Instant.now()))
                                .collect(Collectors.toList());
        }

        private Uni<Boolean> validateConfiguration(GenerationContext genCtx) {
                return Uni.createFrom().item(() -> {
                        if (genCtx.query() == null || genCtx.query().isBlank()) {
                                LOG.error("No query provided");
                                return false;
                        }
                        if (genCtx.config().provider() == null || genCtx.config().provider().isBlank()) {
                                LOG.error("No provider specified");
                                return false;
                        }
                        if (genCtx.config().model() == null || genCtx.config().model().isBlank()) {
                                LOG.error("No model specified");
                                return false;
                        }
                        if (genCtx.config().temperature() < 0.0 || genCtx.config().temperature() > 2.0) {
                                LOG.error("Invalid temperature: {}", genCtx.config().temperature());
                                return false;
                        }
                        if (genCtx.config().maxTokens() <= 0 || genCtx.config().maxTokens() > 32000) {
                                LOG.error("Invalid maxTokens: {}", genCtx.config().maxTokens());
                                return false;
                        }
                        return true;
                });
        }

        private String generateCacheKey(GenerationContext genCtx) {
                String contextsHash = String.valueOf(genCtx.contexts().hashCode());
                String modelKey = genCtx.config().provider() + ":" + genCtx.config().model();
                return String.format("rag-gen:%s:%s:%s",
                                modelKey, genCtx.query().hashCode(), contextsHash);
        }

        @Override
        public boolean canHandle(NodeExecutionTask task) {
                Map<String, Object> context = task.context();
                return context.containsKey("query") &&
                                (context.containsKey("contexts") || context.containsKey("metadata"));
        }
}

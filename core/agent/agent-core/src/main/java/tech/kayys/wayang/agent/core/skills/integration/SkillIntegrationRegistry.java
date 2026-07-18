package tech.kayys.wayang.agent.core.skills.integration;

import tech.kayys.wayang.agent.core.skills.adapters.*;
import tech.kayys.wayang.agent.core.skills.loader.SkillManifestCatalogChange;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Unified integration registry for skill adapters across all wayang-gollek modules.
 * 
 * Provides centralized management of skill integrations with:
 * - Tools Module (SkillAsToolAdapter)
 * - Prompt Module (PromptContextProvider)
 * - Memory Module (SkillMemoryProvider)
 * - Guardrails Module (SkillSafetyValidator)
 * - HITL Module (HITLSkillExecutor)
 * - RAG Module (RAGAwareSkillContext)
 * - MCP Module (MCPSkillProvider)
 * - Vector/Embedding Module (VectorSkillIndexer)
 */
public class SkillIntegrationRegistry {

    private static final Logger LOG = Logger.getLogger(SkillIntegrationRegistry.class);

    private static final String TOOLS = "tools";
    private static final String PROMPT = "prompt";
    private static final String MEMORY = "memory";
    private static final String GUARDRAILS = "guardrails";
    private static final String HITL = "hitl";
    private static final String RAG = "rag";
    private static final String MCP = "mcp";
    private static final String VECTOR = "vector";

    private static final List<String> INTEGRATION_KEYS = List.of(
            TOOLS,
            PROMPT,
            MEMORY,
            GUARDRAILS,
            HITL,
            RAG,
            MCP,
            VECTOR);

    private final SkillRegistry skillRegistry;
    private final Map<String, IntegrationHandle<?>> integrations;

    public SkillIntegrationRegistry(SkillRegistry skillRegistry) {
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
        this.integrations = new ConcurrentHashMap<>();
    }

    /**
     * Initialize all integrations.
     */
    public Uni<SkillIntegrationRegistry> initialize() {
        return Uni.createFrom().item(this)
            .chain(self -> initializeToolIntegration().map(v -> self))
            .chain(self -> initializePromptIntegration().map(v -> self))
            .chain(self -> initializeMemoryIntegration().map(v -> self))
            .chain(self -> initializeSafetyIntegration().map(v -> self))
            .chain(self -> initializeHITLIntegration().map(v -> self))
            .chain(self -> initializeRAGIntegration().map(v -> self))
            .chain(self -> initializeMCPIntegration().map(v -> self))
            .chain(self -> initializeVectorIntegration().map(v -> self));
    }

    /**
     * Tools Module Integration: Skills as tools.
     */
    private Uni<Void> initializeToolIntegration() {
        return Uni.createFrom().item(() -> List.copyOf(SkillAsToolAdapter.adaptSkills(skillRegistry)))
                .invoke(toolAdapters -> {
                    register(TOOLS, toolAdapters);
                    LOG.debugf("Tools integration initialized with %d skill adapters", toolAdapters.size());
                })
                .replaceWithVoid();
    }

    /**
     * Prompt Module Integration: Prompt context providers.
     */
    private Uni<Void> initializePromptIntegration() {
        return initializeContextFactory(PROMPT, PromptContextProvider.class, PromptContextProvider::new);
    }

    /**
     * Memory Module Integration: Memory providers.
     */
    private Uni<Void> initializeMemoryIntegration() {
        return initializeContextFactory(MEMORY, SkillMemoryProvider.class, SkillMemoryProvider::new);
    }

    /**
     * Guardrails Module Integration: Safety validators.
     */
    private Uni<Void> initializeSafetyIntegration() {
        return initializeContextFactory(GUARDRAILS, SkillSafetyValidator.class, SkillSafetyValidator::new);
    }

    /**
     * HITL Module Integration: Human feedback handlers.
     */
    private Uni<Void> initializeHITLIntegration() {
        return initializeContextFactory(HITL, HITLSkillExecutor.class, HITLSkillExecutor::new);
    }

    /**
     * RAG Module Integration: RAG-aware skill context.
     */
    private Uni<Void> initializeRAGIntegration() {
        return initializeContextFactory(RAG, RAGAwareSkillContext.class, RAGAwareSkillContext::new);
    }

    /**
     * MCP Module Integration: Skills via MCP protocol.
     */
    private Uni<Void> initializeMCPIntegration() {
        MCPSkillProvider mcpProvider = new MCPSkillProvider(skillRegistry);
        return mcpProvider.initialize()
                .invoke(initialized -> {
                    register(MCP, initialized);
                    LOG.debug("MCP skill provider integration initialized");
                })
                .replaceWithVoid();
    }

    /**
     * Vector/Embedding Module Integration: Semantic indexing.
     */
    private Uni<Void> initializeVectorIntegration() {
        VectorSkillIndexer indexer = new VectorSkillIndexer(skillRegistry);
        return indexer.indexAllSkills()
                .invoke(() -> {
                    register(VECTOR, indexer);
                    LOG.debug("Vector skill indexer integration initialized");
                });
    }

    /**
     * Get tool adapters for tools module integration.
     */
    @SuppressWarnings("unchecked")
    public List<SkillAsToolAdapter> getToolAdapters() {
        Object tools = integrations.getOrDefault(TOOLS, IntegrationHandle.empty()).adapter();
        if (!(tools instanceof List<?> list)) {
            return List.of();
        }
        return (List<SkillAsToolAdapter>) list;
    }

    /**
     * Get the initialized MCP provider.
     */
    public Optional<MCPSkillProvider> getMcpProvider() {
        return findAdapter(MCP, MCPSkillProvider.class);
    }

    /**
     * Get the initialized vector skill indexer.
     */
    public Optional<VectorSkillIndexer> getVectorIndexer() {
        return findAdapter(VECTOR, VectorSkillIndexer.class);
    }

    /**
     * Refresh cache-backed integrations from a filesystem manifest catalog diff.
     */
    public Uni<SkillIntegrationRefreshResult> refreshSkillIntegrations(SkillManifestCatalogChange change) {
        return refreshSkillIntegrations(SkillIntegrationRefreshRequest.from(change));
    }

    /**
     * Refresh cache-backed integrations from a skill lifecycle delta.
     */
    public Uni<SkillIntegrationRefreshResult> refreshSkillIntegrations(SkillIntegrationRefreshRequest request) {
        SkillIntegrationRefreshRequest safeRequest =
                request == null ? SkillIntegrationRefreshRequest.empty() : request;
        if (!safeRequest.hasChanges()) {
            return Uni.createFrom().item(new SkillIntegrationRefreshResult(safeRequest, List.of()));
        }

        SkillIntegrationRefreshImpact toolImpact = refreshToolIntegration(safeRequest);
        return refreshVectorIntegration(safeRequest)
                .map(vectorImpact -> new SkillIntegrationRefreshResult(
                        safeRequest,
                        List.of(toolImpact, vectorImpact)));
    }

    /**
     * Create a prompt-context adapter for a skill execution context.
     */
    public PromptContextProvider createPromptContextProvider(SkillContext context) {
        return createContextAdapter(PROMPT, PromptContextProvider.class, context);
    }

    /**
     * Create a memory adapter for a skill execution context.
     */
    public SkillMemoryProvider createMemoryProvider(SkillContext context) {
        return createContextAdapter(MEMORY, SkillMemoryProvider.class, context);
    }

    /**
     * Create a safety validator for a skill execution context.
     */
    public SkillSafetyValidator createSafetyValidator(SkillContext context) {
        return createContextAdapter(GUARDRAILS, SkillSafetyValidator.class, context);
    }

    /**
     * Create a HITL executor for a skill execution context.
     */
    public HITLSkillExecutor createHITLExecutor(SkillContext context) {
        return createContextAdapter(HITL, HITLSkillExecutor.class, context);
    }

    /**
     * Create a RAG-aware context adapter for a skill execution context.
     */
    public RAGAwareSkillContext createRAGAwareSkillContext(SkillContext context) {
        return createContextAdapter(RAG, RAGAwareSkillContext.class, context);
    }

    /**
     * Get status of all integrations.
     */
    public Map<String, String> getIntegrationStatus() {
        Map<String, String> status = new LinkedHashMap<>();
        INTEGRATION_KEYS.forEach(key -> status.put(
                key,
                integrations.containsKey(key) ? "initialized" : "pending"));
        return Collections.unmodifiableMap(status);
    }

    private <T> Uni<Void> initializeContextFactory(
            String key,
            Class<T> adapterType,
            Function<SkillContext, T> factory) {
        return Uni.createFrom().voidItem()
                .invoke(() -> {
                    register(key, new ContextAdapterFactory<>(adapterType, factory));
                    LOG.debugf("%s context adapter factory initialized", key);
                });
    }

    private <T> void register(String key, T adapter) {
        integrations.put(key, new IntegrationHandle<>(
                requireKey(key),
                Objects.requireNonNull(adapter, "adapter"),
                Instant.now()));
    }

    private SkillIntegrationRefreshImpact refreshToolIntegration(SkillIntegrationRefreshRequest request) {
        if (!integrations.containsKey(TOOLS)) {
            return SkillIntegrationRefreshImpact.skipped(TOOLS, request.changedNames());
        }

        Map<String, SkillAsToolAdapter> adapters = new LinkedHashMap<>();
        getToolAdapters().forEach(adapter -> {
            if (adapter != null && hasText(adapter.id())) {
                adapters.put(adapter.id().trim(), adapter);
            }
        });

        List<String> removed = new ArrayList<>();
        request.removed().forEach(skillId -> {
            if (adapters.remove(skillId) != null) {
                removed.add(skillId);
            }
        });

        List<String> refreshed = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        request.upsertedNames().forEach(skillId -> findSkillDefinition(skillId)
                .ifPresentOrElse(
                        skill -> {
                            adapters.put(skill.id().trim(), SkillAsToolAdapter.adaptSkill(skill, skillRegistry));
                            refreshed.add(skill.id().trim());
                        },
                        () -> skipped.add(skillId)));

        register(TOOLS, List.copyOf(adapters.values()));
        return SkillIntegrationRefreshImpact.of(TOOLS, refreshed, removed, skipped);
    }

    private Uni<SkillIntegrationRefreshImpact> refreshVectorIntegration(SkillIntegrationRefreshRequest request) {
        Optional<VectorSkillIndexer> indexer = getVectorIndexer();
        if (indexer.isEmpty()) {
            return Uni.createFrom().item(SkillIntegrationRefreshImpact.skipped(VECTOR, request.changedNames()));
        }

        List<SkillDefinition> changedSkills = new ArrayList<>();
        List<String> refreshed = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        request.upsertedNames().forEach(skillId -> findSkillDefinition(skillId)
                .ifPresentOrElse(
                        skill -> {
                            changedSkills.add(skill);
                            refreshed.add(skill.id().trim());
                        },
                        () -> skipped.add(skillId)));

        return indexer.get().refreshSkillIndex(changedSkills, request.removed())
                .replaceWith(SkillIntegrationRefreshImpact.of(
                        VECTOR,
                        refreshed,
                        request.removed(),
                        skipped));
    }

    private Optional<SkillDefinition> findSkillDefinition(String skillId) {
        if (!hasText(skillId)) {
            return Optional.empty();
        }
        try {
            return skillRegistry.getSkill(skillId.trim());
        } catch (RuntimeException error) {
            LOG.debugf(error, "Failed to look up skill definition during integration refresh: %s", skillId);
            return Optional.empty();
        }
    }

    private <T> Optional<T> findAdapter(String key, Class<T> type) {
        IntegrationHandle<?> handle = integrations.get(key);
        if (handle == null || !type.isInstance(handle.adapter())) {
            return Optional.empty();
        }
        return Optional.of(type.cast(handle.adapter()));
    }

    SkillRegistry skillRegistry() {
        return skillRegistry;
    }

    private <T> T createContextAdapter(String key, Class<T> type, SkillContext context) {
        Objects.requireNonNull(context, "context");
        IntegrationHandle<?> handle = integrations.get(key);
        if (handle == null) {
            throw new IllegalStateException("Integration is not initialized: " + key);
        }
        if (!(handle.adapter() instanceof ContextAdapterFactory<?> factory)) {
            throw new IllegalStateException("Integration is not a context adapter factory: " + key);
        }
        Object adapter = factory.create(context);
        if (!type.isInstance(adapter)) {
            throw new IllegalStateException("Integration " + key + " produced "
                    + adapter.getClass().getSimpleName() + " instead of " + type.getSimpleName());
        }
        return type.cast(adapter);
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Integration key must not be blank");
        }
        return key.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record IntegrationHandle<T>(String key, T adapter, Instant initializedAt) {
        private IntegrationHandle {
            key = requireKey(key);
            adapter = Objects.requireNonNull(adapter, "adapter");
            initializedAt = initializedAt == null ? Instant.now() : initializedAt;
        }

        private static IntegrationHandle<Object> empty() {
            return new IntegrationHandle<>("empty", List.of(), Instant.EPOCH);
        }
    }

    private static final class ContextAdapterFactory<T> {
        private final Class<T> adapterType;
        private final Function<SkillContext, T> factory;

        private ContextAdapterFactory(Class<T> adapterType, Function<SkillContext, T> factory) {
            this.adapterType = Objects.requireNonNull(adapterType, "adapterType");
            this.factory = Objects.requireNonNull(factory, "factory");
        }

        private T create(SkillContext context) {
            T adapter = factory.apply(Objects.requireNonNull(context, "context"));
            if (adapter == null) {
                throw new IllegalStateException(adapterType.getSimpleName() + " factory returned null");
            }
            return adapter;
        }
    }
}

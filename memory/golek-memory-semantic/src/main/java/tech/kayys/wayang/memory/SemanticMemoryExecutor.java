package tech.kayys.wayang.memory;

import tech.kayys.wayang.memory.executor.AbstractMemoryExecutor;
import tech.kayys.wayang.memory.executor.MemoryOperationType;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.protocol.CommunicationType;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.model.MemoryType;
import tech.kayys.wayang.memory.service.VectorMemoryStore;
import tech.kayys.wayang.memory.spi.EmbeddingService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Semantic memory executor for storing and retrieving factual knowledge and
 * concepts.
 * Optimized for general knowledge, definitions, relationships, and conceptual
 * understanding.
 * Supports knowledge graph-like queries and concept linking.
 */
@ApplicationScoped
@Executor(executorType = "semantic-memory-executor", communicationType = CommunicationType.GRPC, maxConcurrentTasks = 30, supportedNodeTypes = {
        "semantic-memory", "semantic-memory-task", "knowledge-memory", "concept-memory" }, version = "1.0.0")
public class SemanticMemoryExecutor extends AbstractMemoryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(SemanticMemoryExecutor.class);

    /**
     * Default category for semantic memories
     */
    @ConfigProperty(name = "wayang.memory.semantic.default.category", defaultValue = "general")
    String defaultCategory;

    /**
     * Enable concept linking
     */
    @ConfigProperty(name = "wayang.memory.semantic.concept.linking", defaultValue = "true")
    boolean enableConceptLinking;

    /**
     * Default search limit
     */
    @ConfigProperty(name = "wayang.memory.semantic.search.limit", defaultValue = "15")
    int defaultSearchLimit;

    @Inject
    VectorMemoryStore vectorMemoryStore;

    @Inject
    EmbeddingService embeddingService;

    @Override
    protected String getMemoryType() {
        return "semantic";
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = task.context() == null ? Map.of() : task.context();
        String agentId = resolveAgentId(context);
        MemoryOperationType operation = resolveOperation(context);

        LOG.info("Executing semantic memory task: runId={}, nodeId={}, agentId={}, operation={}",
                task.runId(), task.nodeId(), agentId, operation);

        return switch (operation) {
            case STORE -> handleSemanticStore(task, context, agentId, startedAt);
            case RETRIEVE, SEARCH -> handleSemanticSearch(task, context, agentId, startedAt);
            case CONTEXT -> handleSemanticContext(task, context, agentId, startedAt);
            case CLEAR -> handleSemanticClear(task, context, agentId, startedAt);
            case STATS -> handleSemanticStats(task, context, agentId, startedAt);
            default -> super.execute(task);
        };
    }

    /**
     * Handle STORE operation for semantic knowledge
     */
    private Uni<NodeExecutionResult> handleSemanticStore(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String content = resolveContent(context);
        if (content == null || content.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: content", startedAt));
        }

        String category = resolveCategory(context);
        List<String> concepts = resolveConcepts(context);
        Map<String, Object> metadata = resolveMetadata(context);

        // Add semantic-specific metadata
        metadata.put("memoryType", "semantic");
        metadata.put("agentId", agentId);
        metadata.put("category", category);
        metadata.put("concepts", concepts);
        metadata.put("knowledgeType", "factual");

        // Create semantic memory with embedding
        return embeddingService.embed(content)
                .flatMap(embedding -> {
                    Memory memory = Memory.builder()
                            .content(content)
                            .embedding(toFloatArray(embedding))
                            .type(MemoryType.SEMANTIC)
                            .metadata(metadata)
                            .namespace(buildSemanticNamespace(agentId, category))
                            .importance(0.8) // Semantic knowledge typically has high importance
                            .build();

                    return vectorMemoryStore.store(memory);
                })
                .onItem().transform(memoryId -> {
                    LOG.info("Stored semantic memory: agentId={}, category={}, memoryId={}, concepts={}",
                            agentId, category, memoryId, concepts.size());

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "store",
                            "memoryType", "semantic",
                            "agentId", agentId,
                            "memoryId", memoryId,
                            "category", category,
                            "concepts", concepts,
                            "contentLength", content.length()), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle RETRIEVE/SEARCH operation with concept-based filtering
     */
    private Uni<NodeExecutionResult> handleSemanticSearch(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String query = resolveQuery(context);
        if (query == null || query.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: query", startedAt));
        }

        int limit = resolveLimit(context, defaultSearchLimit);
        String category = resolveCategory(context);
        Map<String, Object> filters = resolveFilters(context, agentId, category);

        return embeddingService.embed(query)
                .flatMap(embedding -> {
                    return vectorMemoryStore.search(
                            toFloatArray(embedding),
                            limit,
                            0.5, // Lower threshold for semantic search
                            filters);
                })
                .onItem().transform(scoredMemories -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (var scoredMemory : scoredMemories) {
                        Map<String, Object> result = new HashMap<>();
                        Memory memory = scoredMemory.getMemory();
                        result.put("id", memory.getId());
                        result.put("content", memory.getContent());
                        result.put("score", scoredMemory.getScore());
                        result.put("category", memory.getMetadata().get("category"));
                        result.put("concepts", memory.getMetadata().get("concepts"));
                        result.put("knowledgeType", memory.getMetadata().get("knowledgeType"));
                        result.put("timestamp", memory.getTimestamp().toString());
                        results.add(result);
                    }

                    LOG.info("Searched semantic memory: agentId={}, query={}, found={}, category={}",
                            agentId, query, results.size(), category);

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "search",
                            "memoryType", "semantic",
                            "agentId", agentId,
                            "query", query,
                            "category", category,
                            "count", results.size(),
                            "limit", limit,
                            "results", results), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle CONTEXT operation - get knowledge by category
     */
    private Uni<NodeExecutionResult> handleSemanticContext(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String category = resolveCategory(context);
        int limit = resolveLimit(context, defaultSearchLimit);
        Map<String, Object> filters = resolveFilters(context, agentId, category);

        // Use category-based query
        String contextQuery = category != null && !category.equals(defaultCategory)
                ? category + " knowledge"
                : "general knowledge";

        return embeddingService.embed(contextQuery)
                .flatMap(embedding -> {
                    return vectorMemoryStore.search(
                            toFloatArray(embedding),
                            limit,
                            0.3,
                            filters);
                })
                .onItem().transform(scoredMemories -> {
                    List<Map<String, Object>> entries = new ArrayList<>();
                    for (var scoredMemory : scoredMemories) {
                        Memory memory = scoredMemory.getMemory();
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("id", memory.getId());
                        entry.put("content", memory.getContent());
                        entry.put("category", memory.getMetadata().get("category"));
                        entry.put("concepts", memory.getMetadata().get("concepts"));
                        entry.put("timestamp", memory.getTimestamp().toString());
                        entries.add(entry);
                    }

                    // Group by category
                    Map<String, List<Map<String, Object>>> groupedByCategory = entries.stream()
                            .collect(Collectors.groupingBy(e -> (String) ((Map<String, Object>) e.get("metadata"))
                                    .getOrDefault("category", defaultCategory)));

                    LOG.info("Retrieved semantic context: agentId={}, category={}, count={}",
                            agentId, category, entries.size());

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "context",
                            "memoryType", "semantic",
                            "agentId", agentId,
                            "category", category,
                            "count", entries.size(),
                            "entries", entries,
                            "groupedByCategory", groupedByCategory), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle CLEAR operation - clear semantic memories by category
     */
    private Uni<NodeExecutionResult> handleSemanticClear(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String category = resolveCategory(context);
        String namespace = buildSemanticNamespace(agentId, category);

        return vectorMemoryStore.deleteNamespace(namespace)
                .onItem().transform(count -> {
                    LOG.info("Cleared semantic memory: agentId={}, category={}, count={}",
                            agentId, category, count);

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "clear",
                            "memoryType", "semantic",
                            "agentId", agentId,
                            "category", category,
                            "clearedCount", count), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle STATS operation - get semantic memory statistics
     */
    private Uni<NodeExecutionResult> handleSemanticStats(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String category = resolveCategory(context);
        String namespace = buildSemanticNamespace(agentId, category);

        return vectorMemoryStore.getStatistics(namespace)
                .onItem().transform(stats -> {
                    Map<String, Object> statsMap = new HashMap<>();
                    statsMap.put("memoryType", "semantic");
                    statsMap.put("agentId", agentId);
                    statsMap.put("category", category);
                    statsMap.put("namespace", stats.getNamespace());
                    statsMap.put("totalCount", stats.getTotalMemories());
                    statsMap.put("semanticCount", stats.getSemanticCount());

                    LOG.info("Retrieved semantic memory stats: agentId={}, category={}, totalCount={}",
                            agentId, category, stats.getTotalMemories());

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "stats",
                            "memoryType", "semantic",
                            "agentId", agentId,
                            "category", category,
                            "stats", statsMap), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Resolve category from context
     */
    private String resolveCategory(Map<String, Object> context) {
        String category = (String) context.get("category");
        if (category == null || category.isBlank()) {
            category = (String) context.get("knowledgeCategory");
        }
        return category != null && !category.isBlank() ? category : defaultCategory;
    }

    /**
     * Resolve concepts from context
     */
    @SuppressWarnings("unchecked")
    private List<String> resolveConcepts(Map<String, Object> context) {
        Object concepts = context.get("concepts");
        if (concepts instanceof List list) {
            return (List<String>) list;
        }
        if (concepts instanceof String str) {
            return Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return Collections.emptyList();
    }

    /**
     * Resolve filters from context
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveFilters(Map<String, Object> context, String agentId, String category) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("agentId", agentId);
        filters.put("memoryType", "semantic");

        if (category != null && !category.equals(defaultCategory)) {
            filters.put("category", category);
        }

        Object customFilters = context.get("filters");
        if (customFilters instanceof Map map) {
            filters.putAll(map);
        }

        return filters;
    }

    /**
     * Build namespace for semantic memory
     */
    private String buildSemanticNamespace(String agentId, String category) {
        return agentId + ":semantic:" + category;
    }

    /**
     * Convert List<Float> to float[]
     */
    private float[] toFloatArray(List<Float> list) {
        if (list == null)
            return new float[0];
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}

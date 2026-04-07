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
import tech.kayys.wayang.memory.spi.AgentMemory;
import tech.kayys.wayang.memory.spi.EmbeddingService;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Instant;
import java.util.*;

/**
 * Long-term memory executor implementing vector-based persistent memory
 * storage.
 * Provides semantic search capabilities and permanent memory retention.
 * Supports importance-based memory consolidation and decay.
 */
@ApplicationScoped
@Executor(executorType = "longterm-memory-executor", communicationType = CommunicationType.GRPC, maxConcurrentTasks = 30, supportedNodeTypes = {
        "longterm-memory", "longterm-memory-task", "vector-memory", "persistent-memory" }, version = "1.0.0")
public class LongTermMemoryExecutor extends AbstractMemoryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(LongTermMemoryExecutor.class);

    /**
     * Default importance threshold for memory retention
     */
    @ConfigProperty(name = "wayang.memory.longterm.importance.threshold", defaultValue = "0.5")
    double defaultImportanceThreshold;

    /**
     * Decay rate for memory importance (per hour)
     */
    @ConfigProperty(name = "wayang.memory.longterm.decay.rate", defaultValue = "0.01")
    double decayRate;

    /**
     * Default search limit
     */
    @ConfigProperty(name = "wayang.memory.longterm.search.limit", defaultValue = "10")
    int defaultSearchLimit;

    /**
     * Minimum similarity threshold for vector search
     */
    @ConfigProperty(name = "wayang.memory.longterm.min.similarity", defaultValue = "0.7")
    float defaultMinSimilarity;

    @Inject
    AgentMemory agentMemory;

    @Inject
    VectorMemoryStore vectorMemoryStore;

    @Inject
    EmbeddingService embeddingService;

    @Override
    protected String getMemoryType() {
        return "longterm";
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = task.context() == null ? Map.of() : task.context();
        String agentId = resolveAgentId(context);
        MemoryOperationType operation = resolveOperation(context);

        LOG.info("Executing long-term memory task: runId={}, nodeId={}, agentId={}, operation={}",
                task.runId(), task.nodeId(), agentId, operation);

        return switch (operation) {
            case STORE -> handleLongTermStore(task, context, agentId, startedAt);
            case RETRIEVE, SEARCH -> handleLongTermSearch(task, context, agentId, startedAt);
            case CONTEXT -> handleLongTermContext(task, context, agentId, startedAt);
            case DELETE -> handleLongTermDelete(task, context, agentId, startedAt);
            case CLEAR -> handleLongTermClear(task, context, agentId, startedAt);
            case STATS -> handleLongTermStats(task, context, agentId, startedAt);
            case CONSOLIDATE -> handleLongTermConsolidate(task, context, agentId, startedAt);
            default -> super.execute(task);
        };
    }

    /**
     * Handle STORE operation with vector embedding and importance scoring
     */
    private Uni<NodeExecutionResult> handleLongTermStore(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String content = resolveContent(context);
        if (content == null || content.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: content", startedAt));
        }

        double importance = resolveImportance(context, defaultImportanceThreshold);
        Map<String, Object> metadata = resolveMetadata(context);

        // Add long-term specific metadata
        metadata.put("memoryType", "longterm");
        metadata.put("agentId", agentId);
        metadata.put("importance", importance);

        // Create memory with embedding
        return embeddingService.embed(content)
                .flatMap(embedding -> {
                    Memory memory = Memory.builder()
                            .content(content)
                            .embedding(toFloatArray(embedding))
                            .type(MemoryType.EPISODIC)
                            .metadata(metadata)
                            .importance(importance)
                            .namespace(agentId)
                            .build();

                    return vectorMemoryStore.store(memory);
                })
                .onItem().transform(memoryId -> {
                    LOG.info("Stored long-term memory: agentId={}, memoryId={}, importance={}",
                            agentId, memoryId, importance);

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "store",
                            "memoryType", "longterm",
                            "agentId", agentId,
                            "memoryId", memoryId,
                            "contentLength", content.length(),
                            "importance", importance), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle RETRIEVE/SEARCH operation with vector similarity search
     */
    private Uni<NodeExecutionResult> handleLongTermSearch(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String query = resolveQuery(context);
        if (query == null || query.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: query", startedAt));
        }

        int limit = resolveLimit(context, defaultSearchLimit);
        float minSimilarity = (float) resolveMinSimilarity(context, defaultMinSimilarity);
        Map<String, Object> filters = resolveFilters(context, agentId);

        return embeddingService.embed(query)
                .flatMap(embedding -> {
                    return vectorMemoryStore.search(
                            toFloatArray(embedding),
                            limit,
                            minSimilarity,
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
                        result.put("importance", memory.getImportance());
                        result.put("timestamp", memory.getTimestamp().toString());
                        result.put("metadata", memory.getMetadata());
                        results.add(result);
                    }

                    LOG.info("Searched long-term memory: agentId={}, query={}, found={}, minSimilarity={}",
                            agentId, query, results.size(), minSimilarity);

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "search",
                            "memoryType", "longterm",
                            "agentId", agentId,
                            "query", query,
                            "count", results.size(),
                            "limit", limit,
                            "minSimilarity", minSimilarity,
                            "results", results), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle CONTEXT operation - get recent long-term memories
     */
    private Uni<NodeExecutionResult> handleLongTermContext(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        int limit = resolveLimit(context, defaultSearchLimit);
        Map<String, Object> filters = resolveFilters(context, agentId);

        // Use a zero vector for recency-based retrieval
        return embeddingService.embed("recent context")
                .flatMap(embedding -> {
                    return vectorMemoryStore.search(
                            toFloatArray(embedding),
                            limit,
                            0.0,
                            filters);
                })
                .onItem().transform(scoredMemories -> {
                    List<Map<String, Object>> entries = new ArrayList<>();
                    for (var scoredMemory : scoredMemories) {
                        Memory memory = scoredMemory.getMemory();
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("id", memory.getId());
                        entry.put("content", memory.getContent());
                        entry.put("timestamp", memory.getTimestamp().toString());
                        entry.put("importance", memory.getImportance());
                        entry.put("metadata", memory.getMetadata());
                        entries.add(entry);
                    }

                    LOG.info("Retrieved long-term context: agentId={}, count={}", agentId, entries.size());

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "context",
                            "memoryType", "longterm",
                            "agentId", agentId,
                            "count", entries.size(),
                            "entries", entries), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle DELETE operation - delete specific memory by ID
     */
    private Uni<NodeExecutionResult> handleLongTermDelete(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String memoryId = resolveMemoryId(context);
        if (memoryId == null || memoryId.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: memoryId", startedAt));
        }

        return vectorMemoryStore.delete(memoryId)
                .onItem().transform(deleted -> {
                    LOG.info("Deleted long-term memory: agentId={}, memoryId={}, deleted={}",
                            agentId, memoryId, deleted);

                    return createSuccessResult(task, Map.of(
                            "success", deleted,
                            "operation", "delete",
                            "memoryType", "longterm",
                            "agentId", agentId,
                            "memoryId", memoryId,
                            "deleted", deleted), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle CLEAR operation - clear all long-term memories for agent
     */
    private Uni<NodeExecutionResult> handleLongTermClear(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        return vectorMemoryStore.deleteNamespace(agentId)
                .onItem().transform(count -> {
                    LOG.info("Cleared long-term memory namespace: agentId={}, count={}", agentId, count);

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "clear",
                            "memoryType", "longterm",
                            "agentId", agentId,
                            "clearedCount", count), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle STATS operation - get memory statistics
     */
    private Uni<NodeExecutionResult> handleLongTermStats(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        return vectorMemoryStore.getStatistics(agentId)
                .onItem().transform(stats -> {
                    Map<String, Object> statsMap = new HashMap<>();
                    statsMap.put("memoryType", "longterm");
                    statsMap.put("agentId", agentId);
                    statsMap.put("namespace", stats.getNamespace());
                    statsMap.put("totalCount", stats.getTotalMemories());
                    statsMap.put("episodicCount", stats.getEpisodicCount());
                    statsMap.put("semanticCount", stats.getSemanticCount());
                    statsMap.put("avgImportance", stats.getAvgImportance());
                    statsMap.put("oldestMemory", stats.getOldestMemory());
                    statsMap.put("newestMemory", stats.getNewestMemory());

                    LOG.info("Retrieved long-term memory stats: agentId={}, totalCount={}",
                            agentId, stats.getTotalMemories());

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "stats",
                            "memoryType", "longterm",
                            "agentId", agentId,
                            "stats", statsMap), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle CONSOLIDATE operation - consolidate memories based on importance decay
     */
    private Uni<NodeExecutionResult> handleLongTermConsolidate(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        double threshold = resolveMinSimilarity(context, defaultImportanceThreshold);
        Map<String, Object> filters = resolveFilters(context, agentId);

        // Get all memories for this agent
        return embeddingService.embed("consolidation")
                .flatMap(embedding -> {
                    return vectorMemoryStore.search(
                            toFloatArray(embedding),
                            Integer.MAX_VALUE,
                            0.0,
                            filters);
                })
                .flatMap(scoredMemories -> {
                    List<String> toDelete = new ArrayList<>();
                    List<Memory> toUpdate = new ArrayList<>();

                    for (var scoredMemory : scoredMemories) {
                        Memory memory = scoredMemory.getMemory();
                        double decayedImportance = memory.getDecayedImportance(decayRate);

                        if (decayedImportance < threshold) {
                            toDelete.add(memory.getId());
                        } else if (decayedImportance != memory.getImportance()) {
                            // Update importance
                            Memory updated = Memory.builder()
                                    .id(memory.getId())
                                    .namespace(memory.getNamespace())
                                    .content(memory.getContent())
                                    .embedding(memory.getEmbedding())
                                    .type(memory.getType())
                                    .metadata(memory.getMetadata())
                                    .timestamp(memory.getTimestamp())
                                    .importance(decayedImportance)
                                    .build();
                            toUpdate.add(updated);
                        }
                    }

                    // Process deletions and updates sequentially
                    return processDeletesAndUpdates(toDelete, toUpdate);
                })
                .onItem().transform(result -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> resultMap = (Map<String, Integer>) result;
                    LOG.info("Consolidated long-term memory: agentId={}, deleted={}, updated={}",
                            agentId, resultMap.get("deleted"), resultMap.get("updated"));

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "consolidate",
                            "memoryType", "longterm",
                            "agentId", agentId,
                            "deletedCount", resultMap.get("deleted"),
                            "updatedCount", resultMap.get("updated"),
                            "threshold", threshold), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Process deletions and updates sequentially
     */
    private Uni<Map<String, Integer>> processDeletesAndUpdates(List<String> toDelete, List<Memory> toUpdate) {
        if (toDelete.isEmpty() && toUpdate.isEmpty()) {
            return Uni.createFrom().item(Map.of("deleted", 0, "updated", 0));
        }

        // Process deletions
        Uni<Integer> deleteCount;
        if (toDelete.isEmpty()) {
            deleteCount = Uni.createFrom().item(0);
        } else {
            List<Uni<Integer>> deleteUnis = new ArrayList<>();
            for (String id : toDelete) {
                deleteUnis.add(vectorMemoryStore.delete(id).map(d -> 1));
            }
            deleteCount = Uni.combine().all().unis(deleteUnis).combinedWith(
                    results -> {
                        int sum = 0;
                        for (Object r : results) {
                            sum += (Integer) r;
                        }
                        return sum;
                    });
        }

        // Process updates
        Uni<Integer> updateCount;
        if (toUpdate.isEmpty()) {
            updateCount = Uni.createFrom().item(0);
        } else {
            List<Uni<Integer>> updateUnis = new ArrayList<>();
            for (Memory memory : toUpdate) {
                updateUnis.add(vectorMemoryStore.store(memory).map(id -> 1));
            }
            updateCount = Uni.combine().all().unis(updateUnis).combinedWith(
                    results -> {
                        int sum = 0;
                        for (Object r : results) {
                            sum += (Integer) r;
                        }
                        return sum;
                    });
        }

        return deleteCount
                .flatMap(deleted -> updateCount.map(updated -> Map.of("deleted", deleted, "updated", updated)));
    }

    /**
     * Resolve importance from context
     */
    private double resolveImportance(Map<String, Object> context, double defaultValue) {
        Object importance = context.get("importance");
        if (importance instanceof Number number) {
            return Math.max(0.0, Math.min(1.0, number.doubleValue()));
        }
        if (importance instanceof String str) {
            try {
                return Math.max(0.0, Math.min(1.0, Double.parseDouble(str)));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Resolve filters from context
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveFilters(Map<String, Object> context, String agentId) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("agentId", agentId);

        Object customFilters = context.get("filters");
        if (customFilters instanceof Map map) {
            filters.putAll(map);
        }

        return filters;
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

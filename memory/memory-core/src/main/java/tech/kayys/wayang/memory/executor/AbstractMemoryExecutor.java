package tech.kayys.wayang.memory.executor;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.node.NodeExecutionStatus;
import tech.kayys.gamelan.plugin.executor.ExecutorPlugin;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.wayang.memory.spi.AgentMemory;
import tech.kayys.wayang.memory.spi.MemoryEntry;
import tech.kayys.gamelan.engine.plugin.PluginContext;
import tech.kayys.gamelan.engine.plugin.PluginException;
import tech.kayys.gamelan.engine.plugin.PluginMetadata;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all memory executors.
 * Provides common functionality for memory operations including store, retrieve, search, update, and delete.
 */
public abstract class AbstractMemoryExecutor implements ExecutorPlugin {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    protected AgentMemory agentMemory;

    @Override
    public void initialize(PluginContext context) throws PluginException {
        // Default no-op initialization
    }

    @Override
    public void start() throws PluginException {
        // Default no-op start
    }

    @Override
    public void stop() throws PluginException {
        // Default no-op stop
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata(
            getExecutorType(),
            "Memory Executor for " + getMemoryType(),
            "1.0.0",
            "Wayang",
            "Executor for " + getMemoryType() + " memory operations",
            List.of(),
            Map.of()
        );
    }

    @Override
    public boolean canHandle(NodeExecutionTask task) {
        Object taskType = task.context().get("__node_type__");
        if (taskType == null) return false;
        
        return Arrays.asList(getSupportedNodeTypes()).contains(taskType.toString());
    }

    /**
     * Execute a memory operation task
     */
    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = task.context() == null ? Map.of() : task.context();

        logger.info("Executing memory task: runId={}, nodeId={}, executorType={}, operation={}",
                task.runId(), task.nodeId(), getExecutorType(), resolveOperation(context));

        return beforeExecute(task)
                .chain(() -> doExecute(task, context))
                .invoke(result -> logger.info("Memory task completed successfully: runId={}, nodeId={}",
                        task.runId(), task.nodeId()))
                .call(result -> afterExecute(task, result))
                .onFailure().invoke(error -> {
                    logger.error("Memory task execution failed: runId={}, nodeId={}, error={}",
                            task.runId(), task.nodeId(), error.getMessage(), error);
                    onError(task, error).subscribe().with(
                            v -> {
                            },
                            e -> logger.error("Error in onError handler", e));
                });
    }

    /**
     * Hook called before task execution.
     * Default implementation returns a completed Uni<Void>.
     */
    protected Uni<Void> beforeExecute(NodeExecutionTask task) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Hook called after successful task execution.
     * Default implementation returns a completed Uni<Void>.
     */
    protected Uni<Void> afterExecute(NodeExecutionTask task, NodeExecutionResult result) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Hook called when task execution fails.
     * Default implementation returns a completed Uni<Void>.
     */
    protected Uni<Void> onError(NodeExecutionTask task, Throwable error) {
        return Uni.createFrom().voidItem();
    }


    /**
     * Execute memory operation based on operation type
     */
    protected Uni<NodeExecutionResult> doExecute(NodeExecutionTask task, Map<String, Object> context) {
        MemoryOperationType operation = resolveOperation(context);
        String agentId = resolveAgentId(context);
        
        return switch (operation) {
            case STORE -> handleStore(task, context, agentId);
            case RETRIEVE -> handleRetrieve(task, context, agentId);
            case SEARCH -> handleSearch(task, context, agentId);
            case UPDATE -> handleUpdate(task, context, agentId);
            case DELETE -> handleDelete(task, context, agentId);
            case CLEAR -> handleClear(task, context, agentId);
            case CONTEXT -> handleContext(task, context, agentId);
            case CONSOLIDATE -> handleConsolidate(task, context, agentId);
            case STATS -> handleStats(task, context, agentId);
        };
    }

    /**
     * Handle STORE operation - store a memory entry
     */
    protected Uni<NodeExecutionResult> handleStore(NodeExecutionTask task, Map<String, Object> context, String agentId) {
        String content = resolveContent(context);
        if (content == null || content.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: content", Instant.now()));
        }

        MemoryEntry entry = createMemoryEntry(content, context);
        
        return agentMemory.store(agentId, entry)
                .onItem().transform(v -> createSuccessResult(task, Map.of(
                        "success", true,
                        "operation", "store",
                        "agentId", agentId,
                        "contentLength", content.length()
                ), Instant.now()))
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, Instant.now()));
    }

    /**
     * Handle RETRIEVE operation - retrieve relevant memories
     */
    protected Uni<NodeExecutionResult> handleRetrieve(NodeExecutionTask task, Map<String, Object> context, String agentId) {
        String query = resolveQuery(context);
        int limit = resolveLimit(context, 10);

        Uni<List<MemoryEntry>> retrieval;
        if (query != null && !query.isBlank()) {
            retrieval = agentMemory.retrieve(agentId, query, limit);
        } else {
            retrieval = agentMemory.getContext(agentId);
        }

        return retrieval
                .onItem().transform(entries -> createSuccessResult(task, Map.of(
                        "success", true,
                        "operation", "retrieve",
                        "agentId", agentId,
                        "count", entries.size(),
                        "entries", serializeEntries(entries)
                ), Instant.now()))
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, Instant.now()));
    }

    /**
     * Handle SEARCH operation - search with query and filters
     */
    protected Uni<NodeExecutionResult> handleSearch(NodeExecutionTask task, Map<String, Object> context, String agentId) {
        String query = resolveQuery(context);
        int limit = resolveLimit(context, 10);
        double minSimilarity = resolveMinSimilarity(context, 0.0);

        if (query == null || query.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: query", Instant.now()));
        }

        return agentMemory.retrieve(agentId, query, limit)
                .onItem().transform(entries -> {
                    List<Map<String, Object>> filteredEntries = entries.stream()
                            .map(this::serializeEntry)
                            .filter(entry -> passesFilters(entry, context))
                            .limit(limit)
                            .toList();

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "search",
                            "agentId", agentId,
                            "query", query,
                            "count", filteredEntries.size(),
                            "entries", filteredEntries,
                            "minSimilarity", minSimilarity
                    ), Instant.now());
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, Instant.now()));
    }

    /**
     * Handle UPDATE operation - update existing memory entry
     */
    protected Uni<NodeExecutionResult> handleUpdate(NodeExecutionTask task, Map<String, Object> context, String agentId) {
        String memoryId = resolveMemoryId(context);
        String content = resolveContent(context);

        if (memoryId == null || memoryId.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: memoryId", Instant.now()));
        }

        if (content == null || content.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: content", Instant.now()));
        }

        MemoryEntry entry = new MemoryEntry(
                memoryId,
                content,
                Instant.now(),
                resolveMetadata(context)
        );

        return agentMemory.store(agentId, entry)
                .onItem().transform(v -> createSuccessResult(task, Map.of(
                        "success", true,
                        "operation", "update",
                        "agentId", agentId,
                        "memoryId", memoryId
                ), Instant.now()))
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, Instant.now()));
    }

    /**
     * Handle DELETE operation - delete memory entry
     */
    protected Uni<NodeExecutionResult> handleDelete(NodeExecutionTask task, Map<String, Object> context, String agentId) {
        String memoryId = resolveMemoryId(context);

        if (memoryId == null || memoryId.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: memoryId", Instant.now()));
        }

        // Note: Current AgentMemory SPI doesn't support delete by ID
        // This would require an extension to the SPI
        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", false,
                "operation", "delete",
                "agentId", agentId,
                "memoryId", memoryId,
                "message", "Delete operation not yet supported in current SPI"
        ), Instant.now()));
    }

    /**
     * Handle CLEAR operation - clear all memory for agent
     */
    protected Uni<NodeExecutionResult> handleClear(NodeExecutionTask task, Map<String, Object> context, String agentId) {
        return agentMemory.clear(agentId)
                .onItem().transform(v -> createSuccessResult(task, Map.of(
                        "success", true,
                        "operation", "clear",
                        "agentId", agentId
                ), Instant.now()))
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, Instant.now()));
    }

    /**
     * Handle CONTEXT operation - get current context
     */
    protected Uni<NodeExecutionResult> handleContext(NodeExecutionTask task, Map<String, Object> context, String agentId) {
        return agentMemory.getContext(agentId)
                .onItem().transform(entries -> createSuccessResult(task, Map.of(
                        "success", true,
                        "operation", "context",
                        "agentId", agentId,
                        "count", entries.size(),
                        "entries", serializeEntries(entries)
                ), Instant.now()))
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, Instant.now()));
    }

    /**
     * Handle CONSOLIDATE operation - consolidate/summarize memories
     */
    protected Uni<NodeExecutionResult> handleConsolidate(NodeExecutionTask task, Map<String, Object> context, String agentId) {
        // Placeholder for consolidation logic
        // This would typically involve summarizing multiple entries into one
        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "consolidate",
                "agentId", agentId,
                "message", "Consolidation not yet implemented"
        ), Instant.now()));
    }

    /**
     * Handle STATS operation - get memory statistics
     */
    protected Uni<NodeExecutionResult> handleStats(NodeExecutionTask task, Map<String, Object> context, String agentId) {
        // Placeholder for statistics
        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "stats",
                "agentId", agentId,
                "message", "Statistics not yet implemented"
        ), Instant.now()));
    }

    /**
     * Get the memory type this executor handles
     */
    protected abstract String getMemoryType();

    /**
     * Resolve operation type from context
     */
    protected MemoryOperationType resolveOperation(Map<String, Object> context) {
        String operation = (String) context.get("operation");
        if (operation == null) {
            operation = (String) context.get("memoryOperation");
        }
        return MemoryOperationType.fromValue(operation);
    }

    /**
     * Resolve agent ID from context
     */
    protected String resolveAgentId(Map<String, Object> context) {
        String agentId = (String) context.get("agentId");
        if (agentId == null || agentId.isBlank()) {
            agentId = (String) context.get("sessionId");
        }
        if (agentId == null || agentId.isBlank()) {
            agentId = "default-agent";
        }
        return agentId;
    }

    /**
     * Resolve content from context
     */
    protected String resolveContent(Map<String, Object> context) {
        String content = (String) context.get("content");
        if (content == null || content.isBlank()) {
            content = (String) context.get("text");
        }
        if (content == null || content.isBlank()) {
            content = (String) context.get("message");
        }
        return content;
    }

    /**
     * Resolve query from context
     */
    protected String resolveQuery(Map<String, Object> context) {
        String query = (String) context.get("query");
        if (query == null || query.isBlank()) {
            query = (String) context.get("search");
        }
        if (query == null || query.isBlank()) {
            query = (String) context.get("question");
        }
        return query;
    }

    /**
     * Resolve memory ID from context
     */
    protected String resolveMemoryId(Map<String, Object> context) {
        String memoryId = (String) context.get("memoryId");
        if (memoryId == null) {
            memoryId = (String) context.get("id");
        }
        return memoryId;
    }

    /**
     * Resolve limit from context
     */
    protected int resolveLimit(Map<String, Object> context, int defaultValue) {
        Object limit = context.get("limit");
        if (limit instanceof Number number) {
            return number.intValue();
        }
        if (limit instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Resolve minimum similarity threshold from context
     */
    protected double resolveMinSimilarity(Map<String, Object> context, double defaultValue) {
        Object minSim = context.get("minSimilarity");
        if (minSim instanceof Number number) {
            return number.doubleValue();
        }
        if (minSim instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Resolve metadata from context
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> resolveMetadata(Map<String, Object> context) {
        Object metadata = context.get("metadata");
        if (metadata instanceof Map) {
            return new HashMap<>((Map<String, Object>) metadata);
        }
        return new HashMap<>();
    }

    /**
     * Create a memory entry from content and context
     */
    protected MemoryEntry createMemoryEntry(String content, Map<String, Object> context) {
        String id = resolveMemoryId(context);
        Map<String, Object> metadata = resolveMetadata(context);
        
        // Add memory type to metadata
        metadata.put("memoryType", getMemoryType());
        
        return new MemoryEntry(
                id,
                content,
                Instant.now(),
                metadata
        );
    }

    /**
     * Serialize a memory entry to map
     */
    protected Map<String, Object> serializeEntry(MemoryEntry entry) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", entry.id());
        result.put("content", entry.content());
        result.put("timestamp", entry.timestamp() != null ? entry.timestamp().toString() : null);
        result.put("metadata", entry.metadata());
        return result;
    }

    /**
     * Serialize a list of memory entries
     */
    protected List<Map<String, Object>> serializeEntries(List<MemoryEntry> entries) {
        return entries.stream()
                .map(this::serializeEntry)
                .toList();
    }

    /**
     * Check if entry passes filters
     */
    @SuppressWarnings("unchecked")
    protected boolean passesFilters(Map<String, Object> entry, Map<String, Object> context) {
        Object filters = context.get("filters");
        if (!(filters instanceof Map filterMap)) {
            return true; // No filters, pass all
        }

        for (Object keyObj : filterMap.keySet()) {
            String key = (String) keyObj;
            Object expectedValue = filterMap.get(key);
            
            // Check in metadata
            if (entry.containsKey("metadata") && entry.get("metadata") instanceof Map metadata) {
                if (!metadata.containsKey(key) || !metadata.get(key).equals(expectedValue)) {
                    return false;
                }
            } else if (!entry.containsKey(key) || !entry.get(key).equals(expectedValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create success result
     */
    protected NodeExecutionResult createSuccessResult(NodeExecutionTask task, Map<String, Object> output, Instant startedAt) {
        return tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult.success(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                output,
                task.token(),
                Duration.between(startedAt, Instant.now())
        );
    }

    /**
     * Create failure result
     */
    protected NodeExecutionResult createFailureResult(NodeExecutionTask task, String message, Instant startedAt) {
        logger.error("Memory operation failed: {}", message);
        return tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult.failure(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                new tech.kayys.gamelan.engine.error.ErrorInfo("MEMORY_ERROR", message, null, Map.of(
                        "operation", resolveOperation(task.context()).getValue()
                )),
                task.token()
        );
    }

    /**
     * Create failure result from exception
     */
    protected NodeExecutionResult createFailureResult(NodeExecutionTask task, Throwable error, Instant startedAt) {
        logger.error("Memory operation failed with exception", error);
        
        java.io.StringWriter sw = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(sw));
        String stackTrace = sw.toString();

        return tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult.failure(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                new tech.kayys.gamelan.engine.error.ErrorInfo(
                        error.getClass().getSimpleName(), 
                        error.getMessage() != null ? error.getMessage() : "Unknown error", 
                        stackTrace,
                        Map.of("operation", resolveOperation(task.context()).getValue())
                ),
                task.token()
        );
    }

    /**
     * Get executor type (memory type + "-memory-executor")
     */
    public String getExecutorType() {
        return getMemoryType() + "-memory-executor";
    }

    /**
     * Get supported node types
     */
    public String[] getSupportedNodeTypes() {
        return new String[] { 
            getMemoryType() + "-memory", 
            getMemoryType() + "-memory-task",
            getMemoryType() + "-memory-node",
            "memory-operation"
        };
    }
}

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
import tech.kayys.wayang.memory.spi.AgentMemory;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-term memory executor implementing buffer/window-based memory.
 * Maintains recent conversation history with configurable window size.
 * Uses FIFO (First-In-First-Out) strategy for memory management.
 */
@ApplicationScoped
@Executor(executorType = "short-memory-executor", communicationType = CommunicationType.GRPC, maxConcurrentTasks = 50, supportedNodeTypes = {
        "short-memory", "short-memory-task", "buffer-memory", "conversation-history" }, version = "1.0.0")
public class ShortTermMemoryExecutor extends AbstractMemoryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(ShortTermMemoryExecutor.class);

    /**
     * Default window size for short-term memory (number of entries to keep)
     */
    @ConfigProperty(name = "wayang.memory.short.window.size", defaultValue = "20")
    int defaultWindowSize;

    /**
     * In-memory buffer storage for short-term memories.
     * Key: agentId, Value: deque of memory entries
     */
    private final Map<String, Deque<MemoryEntry>> shortTermBuffers = new ConcurrentHashMap<>();

    @Inject
    AgentMemory agentMemory;

    @Override
    protected String getMemoryType() {
        return "short";
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = task.context() == null ? Map.of() : task.context();
        String agentId = resolveAgentId(context);
        MemoryOperationType operation = resolveOperation(context);

        LOG.info("Executing short-term memory task: runId={}, nodeId={}, agentId={}, operation={}",
                task.runId(), task.nodeId(), agentId, operation);

        return switch (operation) {
            case STORE -> handleShortTermStore(task, context, agentId, startedAt);
            case RETRIEVE, CONTEXT -> handleShortTermContext(task, context, agentId, startedAt);
            case CLEAR -> handleShortTermClear(task, context, agentId, startedAt);
            case STATS -> handleShortTermStats(task, context, agentId, startedAt);
            default -> handleShortTermDefault(task, context, agentId, startedAt);
        };
    }

    /**
     * Handle STORE operation for short-term memory with window management
     */
    private Uni<NodeExecutionResult> handleShortTermStore(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String content = resolveContent(context);
        if (content == null || content.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: content", startedAt));
        }

        int windowSize = resolveLimit(context, defaultWindowSize);
        MemoryEntry entry = createMemoryEntry(content, context);

        // Get or create buffer for this agent
        Deque<MemoryEntry> buffer = shortTermBuffers.computeIfAbsent(agentId, k -> new ArrayDeque<>(windowSize));

        // Add new entry
        buffer.addLast(entry);

        // Enforce window size (FIFO)
        while (buffer.size() > windowSize) {
            MemoryEntry removed = buffer.removeFirst();
            LOG.debug("Removed old memory entry from short-term buffer: agentId={}, entryId={}",
                    agentId, removed.id());
        }

        LOG.info("Stored short-term memory: agentId={}, windowSize={}, currentSize={}",
                agentId, windowSize, buffer.size());

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "store",
                "memoryType", "short",
                "agentId", agentId,
                "contentLength", content.length(),
                "windowSize", windowSize,
                "currentSize", buffer.size()), startedAt));
    }

    /**
     * Handle CONTEXT/RETRIEVE operation - get recent conversation history
     */
    private Uni<NodeExecutionResult> handleShortTermContext(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        int limit = resolveLimit(context, defaultWindowSize);
        Deque<MemoryEntry> buffer = shortTermBuffers.get(agentId);

        List<MemoryEntry> entries;
        if (buffer == null || buffer.isEmpty()) {
            entries = Collections.emptyList();
        } else {
            // Get most recent entries up to limit
            entries = buffer.stream()
                    .skip(Math.max(0, buffer.size() - limit))
                    .toList();
        }

        LOG.info("Retrieved short-term context: agentId={}, count={}, limit={}",
                agentId, entries.size(), limit);

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "context",
                "memoryType", "short",
                "agentId", agentId,
                "count", entries.size(),
                "limit", limit,
                "entries", serializeEntries(entries)), startedAt));
    }

    /**
     * Handle CLEAR operation - clear short-term buffer
     */
    private Uni<NodeExecutionResult> handleShortTermClear(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        Deque<MemoryEntry> removed = shortTermBuffers.remove(agentId);
        int clearedCount = removed != null ? removed.size() : 0;

        LOG.info("Cleared short-term memory: agentId={}, clearedCount={}", agentId, clearedCount);

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "clear",
                "memoryType", "short",
                "agentId", agentId,
                "clearedCount", clearedCount), startedAt));
    }

    /**
     * Handle STATS operation - get buffer statistics
     */
    private Uni<NodeExecutionResult> handleShortTermStats(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        Deque<MemoryEntry> buffer = shortTermBuffers.get(agentId);
        int windowSize = resolveLimit(context, defaultWindowSize);

        Map<String, Object> stats = new HashMap<>();
        stats.put("memoryType", "short");
        stats.put("agentId", agentId);
        stats.put("currentSize", buffer != null ? buffer.size() : 0);
        stats.put("windowSize", windowSize);
        stats.put("isEmpty", buffer == null || buffer.isEmpty());
        stats.put("isFull", buffer != null && buffer.size() >= windowSize);

        if (buffer != null && !buffer.isEmpty()) {
            stats.put("oldestEntry", buffer.getFirst().timestamp().toString());
            stats.put("newestEntry", buffer.getLast().timestamp().toString());
        }

        LOG.info("Retrieved short-term memory stats: agentId={}, size={}", agentId, stats.get("currentSize"));

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "stats",
                "memoryType", "short",
                "agentId", agentId,
                "stats", stats), startedAt));
    }

    /**
     * Handle unsupported operations
     */
    private Uni<NodeExecutionResult> handleShortTermDefault(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        MemoryOperationType operation = resolveOperation(context);

        // For SEARCH, use query-based retrieval from agentMemory
        if (operation == MemoryOperationType.SEARCH) {
            return handleShortTermSearch(task, context, agentId, startedAt);
        }

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", false,
                "operation", operation.getValue(),
                "memoryType", "short",
                "agentId", agentId,
                "message", "Operation not supported for short-term memory: " + operation.getValue()), startedAt));
    }

    /**
     * Handle SEARCH operation - simple text search in buffer
     */
    private Uni<NodeExecutionResult> handleShortTermSearch(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String query = resolveQuery(context);
        if (query == null || query.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: query", startedAt));
        }

        int limit = resolveLimit(context, defaultWindowSize);
        Deque<MemoryEntry> buffer = shortTermBuffers.get(agentId);

        List<MemoryEntry> results;
        if (buffer == null || buffer.isEmpty()) {
            results = Collections.emptyList();
        } else {
            // Simple text search (case-insensitive)
            String queryLower = query.toLowerCase();
            results = buffer.stream()
                    .filter(entry -> entry.content() != null &&
                            entry.content().toLowerCase().contains(queryLower))
                    .skip(Math.max(0, buffer.size() - limit))
                    .toList();
        }

        LOG.info("Searched short-term memory: agentId={}, query={}, found={}",
                agentId, query, results.size());

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "search",
                "memoryType", "short",
                "agentId", agentId,
                "query", query,
                "count", results.size(),
                "entries", serializeEntries(results)), startedAt));
    }

    /**
     * Get buffer for testing/monitoring purposes
     */
    public Optional<Deque<MemoryEntry>> getBuffer(String agentId) {
        return Optional.ofNullable(shortTermBuffers.get(agentId));
    }

    /**
     * Get all agent IDs with active short-term buffers
     */
    public Set<String> getActiveAgentIds() {
        return Collections.unmodifiableSet(shortTermBuffers.keySet());
    }

    /**
     * Cleanup old buffers (can be called periodically)
     */
    public Uni<Integer> cleanupIdleBuffers(Duration maxIdleTime) {
        Instant now = Instant.now();
        List<String> toRemove = new ArrayList<>();

        shortTermBuffers.forEach((agentId, buffer) -> {
            if (buffer != null && !buffer.isEmpty()) {
                MemoryEntry oldest = buffer.getFirst();
                Duration idleTime = Duration.between(oldest.timestamp(), now);
                if (idleTime.compareTo(maxIdleTime) > 0) {
                    toRemove.add(agentId);
                }
            }
        });

        toRemove.forEach(shortTermBuffers::remove);
        LOG.info("Cleaned up {} idle short-term buffers", toRemove.size());

        return Uni.createFrom().item(toRemove.size());
    }
}

package tech.kayys.wayang.memory;

import tech.kayys.wayang.memory.executor.AbstractMemoryExecutor;
import tech.kayys.wayang.memory.executor.MemoryOperationType;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.protocol.CommunicationType;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Working memory executor for active context and task-related memory.
 * Optimized for short-lived, high-access frequency data during active task
 * execution.
 * Supports attention-based prioritization and automatic expiration.
 */
@ApplicationScoped
@Executor(executorType = "working-memory-executor", communicationType = CommunicationType.GRPC, maxConcurrentTasks = 100, supportedNodeTypes = {
        "working-memory", "working-memory-task", "active-context", "task-context" }, version = "1.0.0")
public class WorkingMemoryExecutor extends AbstractMemoryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(WorkingMemoryExecutor.class);

    /**
     * Default capacity for working memory (number of slots)
     */
    @ConfigProperty(name = "wayang.memory.working.capacity", defaultValue = "7")
    int defaultCapacity;

    /**
     * Default TTL for working memory entries (in minutes)
     */
    @ConfigProperty(name = "wayang.memory.working.ttl.minutes", defaultValue = "30")
    int defaultTtlMinutes;

    /**
     * Enable attention-based prioritization
     */
    @ConfigProperty(name = "wayang.memory.working.attention.enabled", defaultValue = "true")
    boolean enableAttention;

    /**
     * In-memory working memory storage.
     * Key: agentId, Value: WorkingMemoryContext
     */
    private final Map<String, WorkingMemoryContext> workingMemories = new ConcurrentHashMap<>();

    @Override
    protected String getMemoryType() {
        return "working";
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = task.context() == null ? Map.of() : task.context();
        String agentId = resolveAgentId(context);
        MemoryOperationType operation = resolveOperation(context);

        LOG.info("Executing working memory task: runId={}, nodeId={}, agentId={}, operation={}",
                task.runId(), task.nodeId(), agentId, operation);

        return switch (operation) {
            case STORE -> handleWorkingStore(task, context, agentId, startedAt);
            case RETRIEVE, CONTEXT -> handleWorkingContext(task, context, agentId, startedAt);
            case SEARCH -> handleWorkingSearch(task, context, agentId, startedAt);
            case UPDATE -> handleWorkingUpdate(task, context, agentId, startedAt);
            case DELETE -> handleWorkingDelete(task, context, agentId, startedAt);
            case CLEAR -> handleWorkingClear(task, context, agentId, startedAt);
            case STATS -> handleWorkingStats(task, context, agentId, startedAt);
            default -> super.execute(task);
        };
    }

    /**
     * Handle STORE operation with capacity management and attention scoring
     */
    private Uni<NodeExecutionResult> handleWorkingStore(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String content = resolveContent(context);
        if (content == null || content.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: content", startedAt));
        }

        int capacity = resolveLimit(context, defaultCapacity);
        int ttlMinutes = resolveTtlMinutes(context, defaultTtlMinutes);
        double attention = resolveAttention(context, 1.0);
        String slot = resolveSlot(context);

        WorkingMemoryContext wmContext = getOrCreateWorkingMemory(agentId, capacity);
        MemoryEntry entry = createMemoryEntry(content, context);

        // Add working memory specific metadata
        Map<String, Object> metadata = new HashMap<>(entry.metadata());
        metadata.put("memoryType", "working");
        metadata.put("attention", attention);
        metadata.put("slot", slot != null ? slot : "default");
        metadata.put("ttlMinutes", ttlMinutes);
        metadata.put("accessCount", 0);
        metadata.put("lastAccess", Instant.now().toString());

        MemoryEntry workingEntry = new MemoryEntry(
                entry.id(),
                entry.content(),
                entry.timestamp(),
                metadata);

        // Store in appropriate slot or default slot
        String slotKey = slot != null ? slot : "default";
        wmContext.slots.computeIfAbsent(slotKey, k -> new ArrayList<>(capacity));
        List<MemoryEntry> slotEntries = wmContext.slots.get(slotKey);

        // Check if entry with same ID exists (update case)
        boolean updated = false;
        for (int i = 0; i < slotEntries.size(); i++) {
            if (slotEntries.get(i).id().equals(workingEntry.id())) {
                slotEntries.set(i, workingEntry);
                updated = true;
                LOG.debug("Updated working memory entry: agentId={}, slot={}, entryId={}",
                        agentId, slotKey, workingEntry.id());
                break;
            }
        }

        if (!updated) {
            // Add new entry
            slotEntries.add(workingEntry);

            // Enforce capacity (remove lowest attention entries)
            while (slotEntries.size() > capacity) {
                MemoryEntry removed = removeLowestAttention(slotEntries);
                LOG.debug("Removed low-attention entry from working memory: agentId={}, entryId={}",
                        agentId, removed.id());
            }
        }

        wmContext.lastAccess = Instant.now();

        LOG.info("Stored working memory: agentId={}, slot={}, capacity={}, currentSize={}, attention={}",
                agentId, slotKey, capacity, slotEntries.size(), attention);

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "store",
                "memoryType", "working",
                "agentId", agentId,
                "slot", slotKey,
                "capacity", capacity,
                "currentSize", slotEntries.size(),
                "attention", attention,
                "updated", updated,
                "contentLength", content.length()), startedAt));
    }

    /**
     * Handle CONTEXT operation - get active working memory
     */
    private Uni<NodeExecutionResult> handleWorkingContext(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String slot = resolveSlot(context);
        WorkingMemoryContext wmContext = workingMemories.get(agentId);

        List<MemoryEntry> entries;
        if (wmContext == null) {
            entries = Collections.emptyList();
        } else if (slot != null && wmContext.slots.containsKey(slot)) {
            entries = wmContext.slots.get(slot);
        } else if (slot == null) {
            // Return all slots combined
            entries = wmContext.slots.values().stream()
                    .flatMap(List::stream)
                    .sorted((a, b) -> {
                        double attentionA = getAttention(a);
                        double attentionB = getAttention(b);
                        return Double.compare(attentionB, attentionA); // Highest attention first
                    })
                    .toList();
        } else {
            entries = Collections.emptyList();
        }

        // Update access metadata
        entries.forEach(entry -> incrementAccess(entry));
        if (wmContext != null) {
            wmContext.lastAccess = Instant.now();
        }

        LOG.info("Retrieved working memory context: agentId={}, slot={}, count={}",
                agentId, slot, entries.size());

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "context",
                "memoryType", "working",
                "agentId", agentId,
                "slot", slot != null ? slot : "all",
                "count", entries.size(),
                "entries", serializeEntries(entries)), startedAt));
    }

    /**
     * Handle SEARCH operation - search within working memory
     */
    private Uni<NodeExecutionResult> handleWorkingSearch(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String query = resolveQuery(context);
        if (query == null || query.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: query", startedAt));
        }

        String slot = resolveSlot(context);
        WorkingMemoryContext wmContext = workingMemories.get(agentId);

        List<MemoryEntry> entries;
        if (wmContext == null) {
            entries = Collections.emptyList();
        } else if (slot != null && wmContext.slots.containsKey(slot)) {
            entries = wmContext.slots.get(slot);
        } else {
            entries = wmContext.slots.values().stream()
                    .flatMap(List::stream)
                    .toList();
        }

        // Simple text search
        String queryLower = query.toLowerCase();
        List<MemoryEntry> results = entries.stream()
                .filter(entry -> entry.content() != null &&
                        entry.content().toLowerCase().contains(queryLower))
                .sorted((a, b) -> {
                    double attentionA = getAttention(a);
                    double attentionB = getAttention(b);
                    return Double.compare(attentionB, attentionA);
                })
                .toList();

        // Update access metadata
        results.forEach(entry -> incrementAccess(entry));

        LOG.info("Searched working memory: agentId={}, query={}, found={}",
                agentId, query, results.size());

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "search",
                "memoryType", "working",
                "agentId", agentId,
                "query", query,
                "slot", slot != null ? slot : "all",
                "count", results.size(),
                "entries", serializeEntries(results)), startedAt));
    }

    /**
     * Handle UPDATE operation - update existing entry
     */
    private Uni<NodeExecutionResult> handleWorkingUpdate(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String memoryId = resolveMemoryId(context);
        String content = resolveContent(context);

        if (memoryId == null || memoryId.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: memoryId", startedAt));
        }

        if (content == null || content.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: content", startedAt));
        }

        WorkingMemoryContext wmContext = workingMemories.get(agentId);
        boolean found = false;

        if (wmContext != null) {
            for (List<MemoryEntry> slotEntries : wmContext.slots.values()) {
                for (int i = 0; i < slotEntries.size(); i++) {
                    MemoryEntry entry = slotEntries.get(i);
                    if (entry.id().equals(memoryId)) {
                        Map<String, Object> metadata = new HashMap<>(entry.metadata());
                        metadata.put("lastModified", Instant.now().toString());

                        MemoryEntry updatedEntry = new MemoryEntry(
                                memoryId,
                                content,
                                Instant.now(),
                                metadata);
                        slotEntries.set(i, updatedEntry);
                        found = true;
                        break;
                    }
                }
                if (found)
                    break;
            }
        }

        if (!found) {
            return Uni.createFrom().item(createSuccessResult(task, Map.of(
                    "success", false,
                    "operation", "update",
                    "memoryType", "working",
                    "agentId", agentId,
                    "memoryId", memoryId,
                    "message", "Entry not found"), startedAt));
        }

        LOG.info("Updated working memory entry: agentId={}, memoryId={}", agentId, memoryId);

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "update",
                "memoryType", "working",
                "agentId", agentId,
                "memoryId", memoryId), startedAt));
    }

    /**
     * Handle DELETE operation - remove entry from working memory
     */
    private Uni<NodeExecutionResult> handleWorkingDelete(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String memoryId = resolveMemoryId(context);
        if (memoryId == null || memoryId.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: memoryId", startedAt));
        }

        WorkingMemoryContext wmContext = workingMemories.get(agentId);
        boolean deleted = false;

        if (wmContext != null) {
            for (List<MemoryEntry> slotEntries : wmContext.slots.values()) {
                for (int i = 0; i < slotEntries.size(); i++) {
                    if (slotEntries.get(i).id().equals(memoryId)) {
                        slotEntries.remove(i);
                        deleted = true;
                        break;
                    }
                }
                if (deleted)
                    break;
            }
        }

        LOG.info("Deleted working memory entry: agentId={}, memoryId={}, deleted={}",
                agentId, memoryId, deleted);

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", deleted,
                "operation", "delete",
                "memoryType", "working",
                "agentId", agentId,
                "memoryId", memoryId,
                "deleted", deleted), startedAt));
    }

    /**
     * Handle CLEAR operation - clear working memory
     */
    private Uni<NodeExecutionResult> handleWorkingClear(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String slot = resolveSlot(context);
        WorkingMemoryContext removed = workingMemories.remove(agentId);

        int clearedCount = 0;
        if (removed != null) {
            if (slot != null) {
                clearedCount = removed.slots.getOrDefault(slot, Collections.emptyList()).size();
                removed.slots.remove(slot);
            } else {
                clearedCount = removed.slots.values().stream()
                        .mapToInt(List::size)
                        .sum();
            }
        }

        LOG.info("Cleared working memory: agentId={}, slot={}, clearedCount={}",
                agentId, slot, clearedCount);

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "clear",
                "memoryType", "working",
                "agentId", agentId,
                "slot", slot != null ? slot : "all",
                "clearedCount", clearedCount), startedAt));
    }

    /**
     * Handle STATS operation - get working memory statistics
     */
    private Uni<NodeExecutionResult> handleWorkingStats(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        WorkingMemoryContext wmContext = workingMemories.get(agentId);
        int capacity = resolveLimit(context, defaultCapacity);

        Map<String, Object> stats = new HashMap<>();
        stats.put("memoryType", "working");
        stats.put("agentId", agentId);
        stats.put("capacity", capacity);

        if (wmContext == null) {
            stats.put("totalEntries", 0);
            stats.put("slotCount", 0);
            stats.put("isEmpty", true);
            stats.put("utilization", 0.0);
        } else {
            int totalEntries = wmContext.slots.values().stream()
                    .mapToInt(List::size)
                    .sum();
            stats.put("totalEntries", totalEntries);
            stats.put("slotCount", wmContext.slots.size());
            stats.put("isEmpty", totalEntries == 0);
            stats.put("utilization", (double) totalEntries / (capacity * wmContext.slots.size()));
            stats.put("created", wmContext.created.toString());
            stats.put("lastAccess", wmContext.lastAccess.toString());

            // Per-slot stats
            Map<String, Object> slotStats = new HashMap<>();
            wmContext.slots.forEach((slotName, entries) -> {
                Map<String, Object> slotStat = new HashMap<>();
                slotStat.put("count", entries.size());
                slotStat.put("capacity", capacity);
                slotStat.put("avgAttention", entries.stream()
                        .mapToDouble(this::getAttention)
                        .average()
                        .orElse(0.0));
                slotStats.put(slotName, slotStat);
            });
            stats.put("slots", slotStats);
        }

        LOG.info("Retrieved working memory stats: agentId={}, totalEntries={}",
                agentId, stats.get("totalEntries"));

        return Uni.createFrom().item(createSuccessResult(task, Map.of(
                "success", true,
                "operation", "stats",
                "memoryType", "working",
                "agentId", agentId,
                "stats", stats), startedAt));
    }

    /**
     * Get or create working memory context for agent
     */
    private WorkingMemoryContext getOrCreateWorkingMemory(String agentId, int capacity) {
        return workingMemories.computeIfAbsent(agentId, k -> new WorkingMemoryContext(capacity, Instant.now()));
    }

    /**
     * Remove entry with lowest attention score
     */
    private MemoryEntry removeLowestAttention(List<MemoryEntry> entries) {
        if (entries.isEmpty())
            return null;

        MemoryEntry lowest = entries.get(0);
        double lowestAttention = getAttention(lowest);

        for (MemoryEntry entry : entries) {
            double attention = getAttention(entry);
            if (attention < lowestAttention) {
                lowest = entry;
                lowestAttention = attention;
            }
        }

        entries.remove(lowest);
        return lowest;
    }

    /**
     * Get attention score from entry metadata
     */
    private double getAttention(MemoryEntry entry) {
        Object attention = entry.metadata().get("attention");
        if (attention instanceof Number number) {
            return number.doubleValue();
        }
        return 1.0;
    }

    /**
     * Increment access count for entry
     */
    private void incrementAccess(MemoryEntry entry) {
        Map<String, Object> metadata = new HashMap<>(entry.metadata());
        Object accessCount = metadata.get("accessCount");
        int count = accessCount instanceof Number number ? number.intValue() : 0;
        metadata.put("accessCount", count + 1);
        metadata.put("lastAccess", Instant.now().toString());
    }

    /**
     * Resolve attention score from context
     */
    private double resolveAttention(Map<String, Object> context, double defaultValue) {
        Object attention = context.get("attention");
        if (attention instanceof Number number) {
            return Math.max(0.0, Math.min(1.0, number.doubleValue()));
        }
        if (attention instanceof String str) {
            try {
                return Math.max(0.0, Math.min(1.0, Double.parseDouble(str)));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Resolve slot name from context
     */
    private String resolveSlot(Map<String, Object> context) {
        String slot = (String) context.get("slot");
        if (slot == null || slot.isBlank()) {
            slot = (String) context.get("slotName");
        }
        return slot;
    }

    /**
     * Resolve TTL in minutes from context
     */
    private int resolveTtlMinutes(Map<String, Object> context, int defaultValue) {
        Object ttl = context.get("ttlMinutes");
        if (ttl instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        if (ttl instanceof String str) {
            try {
                return Math.max(1, Integer.parseInt(str));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Cleanup expired working memory entries
     */
    public Uni<Integer> cleanupExpiredEntries() {
        Instant now = Instant.now();
        int totalRemoved = 0;

        for (WorkingMemoryContext context : workingMemories.values()) {
            for (List<MemoryEntry> entries : context.slots.values()) {
                Iterator<MemoryEntry> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    MemoryEntry entry = iterator.next();
                    Object ttlMinutes = entry.metadata().get("ttlMinutes");
                    if (ttlMinutes instanceof Number number) {
                        int ttl = number.intValue();
                        Instant expiry = Instant.ofEpochSecond(entry.timestamp().getEpochSecond() + (ttl * 60));
                        if (expiry.isBefore(now)) {
                            iterator.remove();
                            totalRemoved++;
                        }
                    }
                }
            }
        }

        if (totalRemoved > 0) {
            LOG.info("Cleaned up {} expired working memory entries", totalRemoved);
        }

        return Uni.createFrom().item(totalRemoved);
    }

    /**
     * Working memory context holder
     */
    private static class WorkingMemoryContext {
        final Instant created;
        Instant lastAccess;
        final Map<String, List<MemoryEntry>> slots;

        WorkingMemoryContext(int capacity, Instant created) {
            this.created = created;
            this.lastAccess = created;
            this.slots = new ConcurrentHashMap<>();
        }
    }
}

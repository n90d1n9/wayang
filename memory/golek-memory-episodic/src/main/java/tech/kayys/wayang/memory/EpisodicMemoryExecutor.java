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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Episodic memory executor for storing and retrieving event-based memories.
 * Optimized for personal experiences, specific events, and temporal sequences.
 * Supports time-based queries and event reconstruction.
 */
@ApplicationScoped
@Executor(executorType = "episodic-memory-executor", communicationType = CommunicationType.GRPC, maxConcurrentTasks = 30, supportedNodeTypes = {
        "episodic-memory", "episodic-memory-task", "event-memory", "experience-memory" }, version = "1.0.0")
public class EpisodicMemoryExecutor extends AbstractMemoryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(EpisodicMemoryExecutor.class);

    /**
     * Default event type for episodic memories
     */
    @ConfigProperty(name = "wayang.memory.episodic.default.event.type", defaultValue = "general")
    String defaultEventType;

    /**
     * Enable temporal ordering
     */
    @ConfigProperty(name = "wayang.memory.episodic.temporal.ordering", defaultValue = "true")
    boolean enableTemporalOrdering;

    /**
     * Default search limit
     */
    @ConfigProperty(name = "wayang.memory.episodic.search.limit", defaultValue = "20")
    int defaultSearchLimit;

    /**
     * Time window for related events (in hours)
     */
    @ConfigProperty(name = "wayang.memory.episodic.related.time.window.hours", defaultValue = "24")
    int relatedTimeWindowHours;

    @Inject
    VectorMemoryStore vectorMemoryStore;

    @Inject
    EmbeddingService embeddingService;

    @Override
    protected String getMemoryType() {
        return "episodic";
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = task.context() == null ? Map.of() : task.context();
        String agentId = resolveAgentId(context);
        MemoryOperationType operation = resolveOperation(context);

        LOG.info("Executing episodic memory task: runId={}, nodeId={}, agentId={}, operation={}",
                task.runId(), task.nodeId(), agentId, operation);

        return switch (operation) {
            case STORE -> handleEpisodicStore(task, context, agentId, startedAt);
            case RETRIEVE, SEARCH -> handleEpisodicSearch(task, context, agentId, startedAt);
            case CONTEXT -> handleEpisodicContext(task, context, agentId, startedAt);
            case CLEAR -> handleEpisodicClear(task, context, agentId, startedAt);
            case STATS -> handleEpisodicStats(task, context, agentId, startedAt);
            default -> super.execute(task);
        };
    }

    /**
     * Handle STORE operation for episodic event memory
     */
    private Uni<NodeExecutionResult> handleEpisodicStore(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String content = resolveContent(context);
        if (content == null || content.isBlank()) {
            return Uni.createFrom().item(createFailureResult(task, "Missing required field: content", startedAt));
        }

        String eventType = resolveEventType(context);
        Instant eventTime = resolveEventTime(context, startedAt);
        List<String> participants = resolveParticipants(context);
        String location = resolveLocation(context);
        Map<String, Object> metadata = resolveMetadata(context);

        // Add episodic-specific metadata
        metadata.put("memoryType", "episodic");
        metadata.put("agentId", agentId);
        metadata.put("eventType", eventType);
        metadata.put("eventTime", eventTime.toString());
        metadata.put("participants", participants);
        metadata.put("location", location != null ? location : "unknown");
        metadata.put("emotionalValence", resolveEmotionalValence(context, 0.0));

        // Create episodic memory with embedding
        return embeddingService.embed(content)
                .flatMap(embedding -> {
                    Memory memory = Memory.builder()
                            .content(content)
                            .embedding(toFloatArray(embedding))
                            .type(MemoryType.EPISODIC)
                            .metadata(metadata)
                            .namespace(buildEpisodicNamespace(agentId, eventType))
                            .timestamp(eventTime)
                            .importance(calculateEpisodicImportance(eventType, participants))
                            .build();

                    return vectorMemoryStore.store(memory);
                })
                .onItem().transform(memoryId -> {
                    LOG.info("Stored episodic memory: agentId={}, eventType={}, memoryId={}, eventTime={}",
                            agentId, eventType, memoryId, eventTime);

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "store",
                            "memoryType", "episodic",
                            "agentId", agentId,
                            "memoryId", memoryId,
                            "eventType", eventType,
                            "eventTime", eventTime.toString(),
                            "participants", participants,
                            "contentLength", content.length()), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle RETRIEVE/SEARCH operation with temporal and event-based filtering
     */
    private Uni<NodeExecutionResult> handleEpisodicSearch(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String query = resolveQuery(context);
        int limit = resolveLimit(context, defaultSearchLimit);
        String eventType = resolveEventType(context);
        Instant startTime = resolveStartTime(context);
        Instant endTime = resolveEndTime(context);
        Map<String, Object> filters = resolveFilters(context, agentId, eventType);

        Uni<List<Map<String, Object>>> searchResult;

        if (query != null && !query.isBlank()) {
            // Semantic search with query
            searchResult = embeddingService.embed(query)
                    .flatMap(embedding -> {
                        return vectorMemoryStore.search(
                                toFloatArray(embedding),
                                limit * 2, // Get more results for filtering
                                0.5,
                                filters);
                    })
                    .map(scoredMemories -> {
                        List<Map<String, Object>> results = new ArrayList<>();
                        for (var scoredMemory : scoredMemories) {
                            Memory memory = scoredMemory.getMemory();
                            if (passesTemporalFilter(memory, startTime, endTime)) {
                                Map<String, Object> result = serializeEpisodicMemory(memory, scoredMemory.getScore());
                                results.add(result);
                            }
                        }
                        return results.stream().limit(limit).toList();
                    });
        } else {
            // Time-based retrieval
            searchResult = embeddingService.embed("recent events")
                    .flatMap(embedding -> {
                        return vectorMemoryStore.search(
                                toFloatArray(embedding),
                                limit * 2,
                                0.3,
                                filters);
                    })
                    .map(scoredMemories -> {
                        List<Map<String, Object>> results = new ArrayList<>();
                        for (var scoredMemory : scoredMemories) {
                            Memory memory = scoredMemory.getMemory();
                            if (passesTemporalFilter(memory, startTime, endTime)) {
                                Map<String, Object> result = serializeEpisodicMemory(memory, scoredMemory.getScore());
                                results.add(result);
                            }
                        }
                        // Sort by event time
                        results.sort((a, b) -> {
                            String timeA = (String) ((Map<String, Object>) a.get("metadata")).get("eventTime");
                            String timeB = (String) ((Map<String, Object>) b.get("metadata")).get("eventTime");
                            return timeB.compareTo(timeA); // Most recent first
                        });
                        return results.stream().limit(limit).toList();
                    });
        }

        return searchResult
                .onItem().transform(results -> {
                    LOG.info("Searched episodic memory: agentId={}, query={}, eventType={}, found={}",
                            agentId, query != null ? query : "N/A", eventType, results.size());

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "search",
                            "memoryType", "episodic",
                            "agentId", agentId,
                            "query", query,
                            "eventType", eventType,
                            "count", results.size(),
                            "limit", limit,
                            "results", results), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle CONTEXT operation - get recent episodic context
     */
    private Uni<NodeExecutionResult> handleEpisodicContext(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String eventType = resolveEventType(context);
        int limit = resolveLimit(context, defaultSearchLimit);
        Map<String, Object> filters = resolveFilters(context, agentId, eventType);

        return embeddingService.embed("recent experiences")
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
                        Map<String, Object> entry = serializeEpisodicMemory(memory, scoredMemory.getScore());
                        entries.add(entry);
                    }

                    // Sort by event time (most recent first)
                    entries.sort((a, b) -> {
                        String timeA = (String) ((Map<String, Object>) a.get("metadata")).get("eventTime");
                        String timeB = (String) ((Map<String, Object>) b.get("metadata")).get("eventTime");
                        return timeB.compareTo(timeA);
                    });

                    LOG.info("Retrieved episodic context: agentId={}, eventType={}, count={}",
                            agentId, eventType, entries.size());

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "context",
                            "memoryType", "episodic",
                            "agentId", agentId,
                            "eventType", eventType,
                            "count", entries.size(),
                            "entries", entries), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle CLEAR operation - clear episodic memories by event type
     */
    private Uni<NodeExecutionResult> handleEpisodicClear(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String eventType = resolveEventType(context);
        String namespace = buildEpisodicNamespace(agentId, eventType);

        return vectorMemoryStore.deleteNamespace(namespace)
                .onItem().transform(count -> {
                    LOG.info("Cleared episodic memory: agentId={}, eventType={}, count={}",
                            agentId, eventType, count);

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "clear",
                            "memoryType", "episodic",
                            "agentId", agentId,
                            "eventType", eventType,
                            "clearedCount", count), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Handle STATS operation - get episodic memory statistics
     */
    private Uni<NodeExecutionResult> handleEpisodicStats(
            NodeExecutionTask task,
            Map<String, Object> context,
            String agentId,
            Instant startedAt) {

        String eventType = resolveEventType(context);
        String namespace = buildEpisodicNamespace(agentId, eventType);

        return vectorMemoryStore.getStatistics(namespace)
                .onItem().transform(stats -> {
                    Map<String, Object> statsMap = new HashMap<>();
                    statsMap.put("memoryType", "episodic");
                    statsMap.put("agentId", agentId);
                    statsMap.put("eventType", eventType);
                    statsMap.put("namespace", stats.getNamespace());
                    statsMap.put("totalCount", stats.getTotalMemories());
                    statsMap.put("episodicCount", stats.getEpisodicCount());
                    statsMap.put("oldestEvent", stats.getOldestMemory());
                    statsMap.put("newestEvent", stats.getNewestMemory());

                    LOG.info("Retrieved episodic memory stats: agentId={}, eventType={}, totalCount={}",
                            agentId, eventType, stats.getTotalMemories());

                    return createSuccessResult(task, Map.of(
                            "success", true,
                            "operation", "stats",
                            "memoryType", "episodic",
                            "agentId", agentId,
                            "eventType", eventType,
                            "stats", statsMap), startedAt);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error, startedAt));
    }

    /**
     * Resolve event type from context
     */
    private String resolveEventType(Map<String, Object> context) {
        String eventType = (String) context.get("eventType");
        if (eventType == null || eventType.isBlank()) {
            eventType = (String) context.get("event_type");
        }
        if (eventType == null || eventType.isBlank()) {
            eventType = (String) context.get("category");
        }
        return eventType != null && !eventType.isBlank() ? eventType : defaultEventType;
    }

    /**
     * Resolve event time from context
     */
    private Instant resolveEventTime(Map<String, Object> context, Instant defaultTime) {
        Object eventTime = context.get("eventTime");
        if (eventTime instanceof Instant instant) {
            return instant;
        }
        if (eventTime instanceof String str) {
            try {
                return Instant.parse(str);
            } catch (Exception e) {
                LOG.warn("Failed to parse event time: {}", str);
            }
        }
        if (eventTime instanceof Number num) {
            return Instant.ofEpochMilli(num.longValue());
        }
        return defaultTime;
    }

    /**
     * Resolve start time filter from context
     */
    private Instant resolveStartTime(Map<String, Object> context) {
        Object startTime = context.get("startTime");
        if (startTime instanceof Instant instant) {
            return instant;
        }
        if (startTime instanceof String str) {
            try {
                return Instant.parse(str);
            } catch (Exception e) {
                LOG.warn("Failed to parse start time: {}", str);
            }
        }
        return null;
    }

    /**
     * Resolve end time filter from context
     */
    private Instant resolveEndTime(Map<String, Object> context) {
        Object endTime = context.get("endTime");
        if (endTime instanceof Instant instant) {
            return instant;
        }
        if (endTime instanceof String str) {
            try {
                return Instant.parse(str);
            } catch (Exception e) {
                LOG.warn("Failed to parse end time: {}", str);
            }
        }
        return null;
    }

    /**
     * Resolve participants from context
     */
    @SuppressWarnings("unchecked")
    private List<String> resolveParticipants(Map<String, Object> context) {
        Object participants = context.get("participants");
        if (participants instanceof List list) {
            return (List<String>) list;
        }
        if (participants instanceof String str) {
            return Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return Collections.emptyList();
    }

    /**
     * Resolve location from context
     */
    private String resolveLocation(Map<String, Object> context) {
        String location = (String) context.get("location");
        if (location == null || location.isBlank()) {
            location = (String) context.get("place");
        }
        return location;
    }

    /**
     * Resolve emotional valence from context
     */
    private double resolveEmotionalValence(Map<String, Object> context, double defaultValue) {
        Object valence = context.get("emotionalValence");
        if (valence instanceof Number number) {
            return Math.max(-1.0, Math.min(1.0, number.doubleValue()));
        }
        return defaultValue;
    }

    /**
     * Calculate importance based on event characteristics
     */
    private double calculateEpisodicImportance(String eventType, List<String> participants) {
        double importance = 0.5;

        // Boost importance for certain event types
        if (eventType != null) {
            if (eventType.equalsIgnoreCase("milestone") ||
                    eventType.equalsIgnoreCase("achievement") ||
                    eventType.equalsIgnoreCase("critical")) {
                importance += 0.3;
            } else if (eventType.equalsIgnoreCase("meeting") ||
                    eventType.equalsIgnoreCase("conversation")) {
                importance += 0.1;
            }
        }

        // Boost importance for social events
        if (participants != null && !participants.isEmpty()) {
            importance += Math.min(0.2, participants.size() * 0.05);
        }

        return Math.min(1.0, importance);
    }

    /**
     * Check if memory passes temporal filter
     */
    private boolean passesTemporalFilter(Memory memory, Instant startTime, Instant endTime) {
        Instant eventTime = memory.getTimestamp();

        if (startTime != null && eventTime.isBefore(startTime)) {
            return false;
        }
        if (endTime != null && eventTime.isAfter(endTime)) {
            return false;
        }

        return true;
    }

    /**
     * Serialize episodic memory to map
     */
    private Map<String, Object> serializeEpisodicMemory(Memory memory, double score) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", memory.getId());
        result.put("content", memory.getContent());
        result.put("score", score);
        result.put("eventTime", memory.getTimestamp().toString());
        result.put("importance", memory.getImportance());
        result.put("metadata", memory.getMetadata());
        return result;
    }

    /**
     * Resolve filters from context
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveFilters(Map<String, Object> context, String agentId, String eventType) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("agentId", agentId);
        filters.put("memoryType", "episodic");

        if (eventType != null && !eventType.equals(defaultEventType)) {
            filters.put("eventType", eventType);
        }

        Object customFilters = context.get("filters");
        if (customFilters instanceof Map map) {
            filters.putAll(map);
        }

        return filters;
    }

    /**
     * Build namespace for episodic memory
     */
    private String buildEpisodicNamespace(String agentId, String eventType) {
        return agentId + ":episodic:" + eventType;
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

package tech.kayys.wayang.agent.adapter;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.memory.AgentMemoryManager;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Small in-memory {@link AgentMemoryManager} adapter for tests and standalone
 * tools that need memory behavior without the vector-memory runtime.
 *
 * <p>
 * This class is intentionally not a CDI bean. The canonical runtime memory
 * implementation remains in {@code agent-core}.
 */
public class AgentMemoryManagerAdapter implements AgentMemoryManager {

    private final ConcurrentMap<String, CopyOnWriteArrayList<MemoryRecord>> memories = new ConcurrentHashMap<>();

    @Override
    public Uni<String> storeMemory(String agentId, String content, Map<String, Object> metadata) {
        if (agentId == null || agentId.isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("Agent id must not be blank"));
        }
        String memoryId = UUID.randomUUID().toString();
        MemoryRecord record = new MemoryRecord(
                memoryId,
                content == null ? "" : content,
                sanitizeMetadata(metadata),
                Instant.now());
        memories.computeIfAbsent(agentId, ignored -> new CopyOnWriteArrayList<>()).add(record);
        return Uni.createFrom().item(memoryId);
    }

    @Override
    public Uni<String> retrieveContext(String agentId, String query, int limit) {
        if (limit <= 0) {
            return Uni.createFrom().item("");
        }
        return Uni.createFrom().item(() -> memories.getOrDefault(agentId, new CopyOnWriteArrayList<>()).stream()
                .filter(record -> matches(record.content(), query))
                .sorted(Comparator.comparing(MemoryRecord::createdAt).reversed())
                .limit(limit)
                .map(record -> "- " + record.content())
                .reduce((left, right) -> left + "\n" + right)
                .orElse(""));
    }

    @Override
    public Uni<String> storeObservation(String agentId, String toolName, String observation) {
        String safeToolName = toolName == null || toolName.isBlank() ? "unknown" : toolName;
        return storeMemory(
                agentId,
                "Tool [" + safeToolName + "] output: " + (observation == null ? "" : observation),
                Map.of("type", "tool_output", "tool", safeToolName));
    }

    public List<MemoryRecord> memoriesFor(String agentId) {
        return List.copyOf(memories.getOrDefault(agentId, new CopyOnWriteArrayList<>()));
    }

    public void clear() {
        memories.clear();
    }

    private boolean matches(String content, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedContent = content.toLowerCase(java.util.Locale.ROOT);
        return Arrays.stream(query.toLowerCase(java.util.Locale.ROOT).split("\\W+"))
                .filter(token -> !token.isBlank())
                .anyMatch(normalizedContent::contains);
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                sanitized.put(key, value);
            }
        });
        return Map.copyOf(sanitized);
    }

    public record MemoryRecord(String id, String content, Map<String, Object> metadata, Instant createdAt) {
    }
}

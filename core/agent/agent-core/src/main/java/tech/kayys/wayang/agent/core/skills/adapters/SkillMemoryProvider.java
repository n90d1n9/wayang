package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillContextKeys;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import io.smallrye.mutiny.Uni;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory-aware skill context provider.
 * 
 * Stores and retrieves skill execution context from memory systems (episodic/semantic),
 * enabling skills to access prior execution history and learned patterns.
 */
public class SkillMemoryProvider {

    private final SkillContext context;
    private final Map<String, Object> memoryStore;

    public SkillMemoryProvider(SkillContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.memoryStore = new ConcurrentHashMap<>();
    }

    /**
     * Store skill execution context in memory.
     */
    public Uni<Void> storeContext(String key, Object value) {
        if (hasText(key) && value != null) {
            memoryStore.put(key.trim(), value);
        }
        return Uni.createFrom().voidItem();
    }

    /**
     * Retrieve skill context from memory.
     */
    public <T> Optional<T> getContext(String key, Class<T> type) {
        if (!hasText(key) || type == null) {
            return Optional.empty();
        }
        Object value = memoryStore.get(key.trim());
        return type.isInstance(value)
            ? Optional.of(type.cast(value))
            : Optional.empty();
    }

    /**
     * Store skill execution result for future reference.
     */
    public Uni<Void> storeResult(SkillResult result) {
        if (result == null) {
            return Uni.createFrom().voidItem();
        }
        memoryStore.put(memoryKey(SkillContextKeys.MEMORY_LAST_RESULT), result);
        if (result.status() != null) {
            memoryStore.put(memoryKey(SkillContextKeys.MEMORY_LAST_STATUS), result.status());
        }
        memoryStore.put(memoryKey(SkillContextKeys.MEMORY_LAST_SUCCESS), result.success());
        return Uni.createFrom().voidItem();
    }

    /**
     * Retrieve last skill execution result.
     */
    public Optional<SkillResult> getLastResult() {
        return getContext(memoryKey(SkillContextKeys.MEMORY_LAST_RESULT), SkillResult.class);
    }

    /**
     * Store skill execution metrics.
     */
    public Uni<Void> storeMetrics(Map<String, Object> metrics) {
        Map<String, Object> copied = copyMap(metrics);
        if (!copied.isEmpty()) {
            memoryStore.put(memoryKey(SkillContextKeys.MEMORY_METRICS), copied);
        }
        return Uni.createFrom().voidItem();
    }

    /**
     * Get all stored context for this skill.
     */
    public Map<String, Object> getAllContext() {
        return Map.copyOf(memoryStore);
    }

    /**
     * Clear memory for this skill.
     */
    public Uni<Void> clearMemory() {
        memoryStore.clear();
        return Uni.createFrom().voidItem();
    }

    private String memoryKey(String prefix) {
        return SkillContextKeys.scopedMemoryKey(prefix, context.skillId());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Map<String, Object> copyMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (hasText(key) && value != null) {
                copied.put(key.trim(), value);
            }
        });
        return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
    }
}

package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import io.smallrye.mutiny.Uni;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        this.context = context;
        this.memoryStore = new HashMap<>();
    }

    /**
     * Store skill execution context in memory.
     */
    public Uni<Void> storeContext(String key, Object value) {
        memoryStore.put(key, value);
        return Uni.createFrom().voidItem();
    }

    /**
     * Retrieve skill context from memory.
     */
    public <T> Optional<T> getContext(String key, Class<T> type) {
        Object value = memoryStore.get(key);
        return value != null && type.isAssignableFrom(value.getClass())
            ? Optional.of(type.cast(value))
            : Optional.empty();
    }

    /**
     * Store skill execution result for future reference.
     */
    public Uni<Void> storeResult(SkillResult result) {
        memoryStore.put("last_result_" + context.skillId(), result);
        memoryStore.put("last_status_" + context.skillId(), result.status());
        memoryStore.put("last_success_" + context.skillId(), result.success());
        return Uni.createFrom().voidItem();
    }

    /**
     * Retrieve last skill execution result.
     */
    public Optional<SkillResult> getLastResult() {
        return getContext("last_result_" + context.skillId(), SkillResult.class);
    }

    /**
     * Store skill execution metrics.
     */
    public Uni<Void> storeMetrics(Map<String, Object> metrics) {
        memoryStore.put("metrics_" + context.skillId(), metrics);
        return Uni.createFrom().voidItem();
    }

    /**
     * Get all stored context for this skill.
     */
    public Map<String, Object> getAllContext() {
        return new HashMap<>(memoryStore);
    }

    /**
     * Clear memory for this skill.
     */
    public Uni<Void> clearMemory() {
        memoryStore.clear();
        return Uni.createFrom().voidItem();
    }
}

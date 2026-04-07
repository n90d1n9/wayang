package tech.kayys.wayang.agent.spi;

import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Map;

/**
 * Simple skill interface for agent skill execution.
 * Self-contained without external dependencies.
 */
public interface AgentSkill {
    String id();
    String name();
    default List<String> aliases() { return List.of(); }
    String description();
    default String version() { return "1.0.0"; }
    default String category() { return "GENERAL"; }
    default int priority() { return 100; }
    default void initialize(Map<String, Object> config) {}
    default void shutdown() {}
    Uni<Map<String, Object>> execute(Map<String, Object> context);
    default boolean canHandle(Map<String, Object> inputs) { return true; }
    default boolean isHealthy() { return true; }
}

package tech.kayys.wayang.agent.spi;

/**
 * Memory configuration for an agent request.
 */
public record AgentMemoryConfig(
        boolean conversationEnabled,
        int maxHistoryTurns,
        boolean vectorMemoryEnabled,
        String memoryNamespace) {
    
    public static AgentMemoryConfig defaults() {
        return new AgentMemoryConfig(true, 10, false, null);
    }
}

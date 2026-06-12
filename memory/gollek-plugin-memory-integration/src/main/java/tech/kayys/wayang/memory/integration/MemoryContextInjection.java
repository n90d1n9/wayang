package tech.kayys.wayang.memory.integration;

import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.util.List;
import java.util.Map;

/**
 * Result of resolving memory context for an agent or inference request.
 */
public record MemoryContextInjection(
        String agentId,
        String query,
        List<MemoryEntry> entries,
        String contextMessage) {

    public static final String RETRIEVED_MEMORIES_KEY = "retrievedMemories";
    public static final String MEMORY_CONTEXT_MESSAGE_KEY = "memoryContextMessage";
    public static final String MEMORY_AGENT_ID_KEY = "memoryAgentId";

    public MemoryContextInjection {
        entries = entries != null ? List.copyOf(entries) : List.of();
        contextMessage = contextMessage != null ? contextMessage : "";
    }

    public boolean hasMemories() {
        return !entries.isEmpty();
    }

    public List<String> memoryContents() {
        return entries.stream()
                .map(MemoryEntry::content)
                .filter(content -> content != null && !content.isBlank())
                .toList();
    }

    public Map<String, Object> contextVariables() {
        return Map.of(
                RETRIEVED_MEMORIES_KEY, memoryContents(),
                MEMORY_CONTEXT_MESSAGE_KEY, contextMessage,
                MEMORY_AGENT_ID_KEY, agentId);
    }
}

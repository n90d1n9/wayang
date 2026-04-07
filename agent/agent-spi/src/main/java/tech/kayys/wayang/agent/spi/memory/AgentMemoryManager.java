package tech.kayys.wayang.agent.spi.memory;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Interface for managing agent memory and context retrieval.
 */
public interface AgentMemoryManager {

    /**
     * Store a memory for a specific agent/session.
     *
     * @param agentId Unique identifier for the agent
     * @param content The text content to store
     * @param metadata Optional metadata to attach
     * @return Uni with the ID of the stored memory
     */
    Uni<String> storeMemory(String agentId, String content, Map<String, Object> metadata);

    /**
     * Retrieve relevant memories for context injection based on a query.
     *
     * @param agentId Unique identifier for the agent
     * @param query Search query/context
     * @param limit Maximum number of memories to return
     * @return Uni with a formatted string of retrieved context
     */
    Uni<String> retrieveContext(String agentId, String query, int limit);

    /**
     * Store an observation or result from a tool execution as a memory.
     *
     * @param agentId Unique identifier for the agent
     * @param toolName Name of the tool executed
     * @param observation The result/output of the tool
     * @return Uni with the ID of the stored memory
     */
    Uni<String> storeObservation(String agentId, String toolName, String observation);
}

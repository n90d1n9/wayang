package tech.kayys.wayang.agent.core.memory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.memory.impl.VectorAgentMemory;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.service.VectorMemoryStore;
import tech.kayys.wayang.memory.spi.MemoryEntry;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Memory Integration Service for Agents
 *
 * Bridges the wayang-gollek agent module with the memory module to provide:
 * - Context retrieval for agent reasoning
 * - Memory storage for interactions
 * - Memory-aware skill selection
 * - Session-based memory management
 *
 * Usage:
 * {@code
 * @Inject
 * AgentMemoryService memoryService;
 *
 * // Retrieve context before agent reasoning
 * Uni<String> context = memoryService.getContextPrompt("agent-123");
 *
 * // Store interaction after execution
 * memoryService.storeInteraction("agent-123", request, response);
 * }
 */
@ApplicationScoped
public class AgentMemoryService {

    private static final Logger LOG = LoggerFactory.getLogger(AgentMemoryService.class);

    @Inject
    VectorAgentMemory vectorAgentMemory;

    @Inject
    VectorMemoryStore vectorMemoryStore;

    /**
     * Retrieve context for agent reasoning as a formatted prompt segment
     *
     * @param agentId The agent ID
     * @return Reactive context prompt fragment
     */
    public Uni<String> getContextPrompt(String agentId) {
        return getContextPrompt(agentId, 10);
    }

    /**
     * Retrieve context for agent reasoning with configurable limit
     *
     * @param agentId The agent ID
     * @param limit   Maximum number of context entries to retrieve
     * @return Reactive context prompt fragment
     */
    public Uni<String> getContextPrompt(String agentId, int limit) {
        return vectorAgentMemory.getContext(agentId)
                .map(contextMemories -> {
                    if (contextMemories.isEmpty()) {
                        return "";
                    }

                    StringBuilder context = new StringBuilder();
                    context.append("\n## Conversation History:\n");

                    contextMemories.stream()
                            .limit(limit)
                            .forEach(memory -> {
                                context.append("- ").append(memory.content()).append("\n");
                            });

                    return context.toString();
                })
                .onFailure().invoke(ex -> {
                    LOG.warn("Failed to retrieve context for agent {}: {}", agentId, ex.getMessage());
                })
                .onFailure().recoverWithItem("");
    }

    /**
     * Store an agent interaction (user prompt and agent response)
     *
     * @param agentId The agent ID
     * @param request The original agent request
     * @param response The agent response
     * @return Reactive completion
     */
    public Uni<Void> storeInteraction(String agentId, AgentRequest request, AgentResponse response) {
        return storeInteraction(agentId, request.sessionId(), request.userId(),
                request.prompt(), response.content());
    }

    /**
     * Store an agent interaction with all details
     *
     * @param agentId   The agent ID
     * @param sessionId The session ID
     * @param userId    The user ID
     * @param prompt    The user prompt
     * @param response  The agent response
     * @return Reactive completion
     */
    public Uni<Void> storeInteraction(
            String agentId,
            String sessionId,
            String userId,
            String prompt,
            String response) {

        // Store user prompt
        MemoryEntry userEntry = new MemoryEntry(
                UUID.randomUUID().toString(),
                prompt,
                Instant.now(),
                Map.of(
                        "agentId", agentId,
                        "sessionId", sessionId != null ? sessionId : "unknown",
                        "userId", userId != null ? userId : "unknown",
                        "type", "user-message",
                        "source", "agent-interaction"
                )
        );

        // Store agent response
        MemoryEntry agentEntry = new MemoryEntry(
                UUID.randomUUID().toString(),
                response,
                Instant.now(),
                Map.of(
                        "agentId", agentId,
                        "sessionId", sessionId != null ? sessionId : "unknown",
                        "userId", userId != null ? userId : "unknown",
                        "type", "agent-message",
                        "source", "agent-interaction"
                )
        );

        return vectorAgentMemory.store(agentId, userEntry)
                .flatMap(__ -> vectorAgentMemory.store(agentId, agentEntry))
                .onFailure().invoke(ex -> {
                    LOG.warn("Failed to store interaction for agent {}: {}", agentId, ex.getMessage());
                })
                .onFailure().recoverWithVoid();
    }

    /**
     * Retrieve session-specific context
     *
     * @param agentId   The agent ID
     * @param sessionId The session ID
     * @return Reactive list of session memories
     */
    public Uni<List<MemoryEntry>> getSessionMemories(String agentId, String sessionId) {
        return getSessionMemories(agentId, sessionId, 10);
    }

    /**
     * Retrieve session-specific context with limit
     *
     * @param agentId   The agent ID
     * @param sessionId The session ID
     * @param limit     Maximum number of memories to retrieve
     * @return Reactive list of session memories
     */
    public Uni<List<MemoryEntry>> getSessionMemories(
            String agentId,
            String sessionId,
            int limit) {

        Map<String, Object> filters = Map.of(
                "agentId", agentId,
                "sessionId", sessionId
        );

        return vectorMemoryStore.searchByFilter(filters)
                .map(memories -> memories.stream()
                        .sorted((a, b) -> b.getMetadata()
                                .get("timestamp")
                                .toString()
                                .compareTo(a.getMetadata()
                                        .get("timestamp")
                                        .toString()))
                        .limit(limit)
                        .map(this::toMemoryEntry)
                        .collect(Collectors.toList()))
                .onFailure().invoke(ex -> {
                    LOG.warn("Failed to retrieve session memories: {}", ex.getMessage());
                })
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    /**
     * Clear agent memory (when session ends or agent is reset)
     *
     * @param agentId The agent ID
     * @return Reactive completion
     */
    public Uni<Void> clearMemory(String agentId) {
        return vectorMemoryStore.deleteNamespace(agentId)
                .onItem().invoke(deletedCount -> {
                    LOG.info("Cleared {} memory entries for agent {}", deletedCount, agentId);
                })
                .replaceWithVoid()
                .onFailure().invoke(ex -> {
                    LOG.warn("Failed to clear memory for agent {}: {}", agentId, ex.getMessage());
                })
                .onFailure().recoverWithVoid();
    }

    /**
     * Get agent memory statistics
     *
     * @param agentId The agent ID
     * @return Reactive statistics
     */
    public Uni<AgentMemoryStats> getMemoryStats(String agentId) {
        return vectorMemoryStore.getStatistics(agentId)
                .map(stats -> new AgentMemoryStats(
                        agentId,
                        stats.getTotalMemories(),
                        stats.getAverageSize(),
                        stats.getLastUpdated()
                ))
                .onFailure().recoverWithItem(
                        new AgentMemoryStats(agentId, 0, 0, Instant.now())
                );
    }

    /**
     * Store a skill usage observation for later skill selection
     *
     * @param agentId   The agent ID
     * @param skillId   The skill ID
     * @param success   Whether the skill executed successfully
     * @param result    The skill result/observation
     * @return Reactive completion
     */
    public Uni<Void> recordSkillUsage(String agentId, String skillId, boolean success, String result) {
        MemoryEntry skillEntry = new MemoryEntry(
                UUID.randomUUID().toString(),
                "Skill: " + skillId + " - " + (success ? "Success" : "Failed") + ": " + result,
                Instant.now(),
                Map.of(
                        "agentId", agentId,
                        "type", "skill-usage",
                        "skillId", skillId,
                        "success", String.valueOf(success),
                        "source", "agent-skill-execution"
                )
        );

        return vectorAgentMemory.store(agentId, skillEntry)
                .onFailure().recoverWithVoid();
    }

    /**
     * Get formatted context prompt with optional system message enhancement
     *
     * @param agentId       The agent ID
     * @param baseSystemMsg The base system message
     * @return Enhanced system message with context
     */
    public Uni<String> enhanceSystemPrompt(String agentId, String baseSystemMsg) {
        return getContextPrompt(agentId)
                .map(context -> baseSystemMsg + context);
    }

    /**
     * Convert Memory to MemoryEntry
     */
    private MemoryEntry toMemoryEntry(Memory memory) {
        return new MemoryEntry(
                memory.getId(),
                memory.getContent(),
                Instant.now(),
                memory.getMetadata()
        );
    }

    /**
     * Memory statistics record
     */
    public record AgentMemoryStats(
            String agentId,
            long totalMemories,
            double averageSize,
            Instant lastUpdated) {
    }
}

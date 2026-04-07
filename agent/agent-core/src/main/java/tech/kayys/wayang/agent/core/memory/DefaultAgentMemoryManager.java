package tech.kayys.wayang.agent.core.memory;

import io.smallrye.mutiny.Uni;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.embedding.EmbeddingException;
import tech.kayys.wayang.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingResponse;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.service.VectorMemoryStore;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of AgentMemoryManager.
 * Wraps the generic VectorMemoryStore to provide agent-specific memory
 * operations.
 */
@jakarta.enterprise.context.ApplicationScoped
public class DefaultAgentMemoryManager implements AgentMemoryManager {
    private static final Logger log = LoggerFactory.getLogger(DefaultAgentMemoryManager.class);

    private static final String DEFAULT_MEMORY_EMBEDDING_MODEL = "tfidf-512";

    @Inject
    VectorMemoryStore vectorStore;

    @Inject
    EmbeddingService embeddingService;

    /**
     * Store a memory for a specific agent/session.
     */
    public Uni<String> storeMemory(String agentId, String content, Map<String, Object> metadata) {
        float[] embedding = generateEmbedding(content);
        Map<String, Object> mergedMetadata = mergeMetadata(agentId, metadata);

        Memory memory = Memory.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .embedding(embedding)
                .metadata(mergedMetadata)
                .build();

        return vectorStore.store(memory);
    }

    /**
     * Retrieve relevant memories for context injection.
     */
    public Uni<String> retrieveContext(String agentId, String query, int limit) {
        float[] queryEmbedding = generateEmbedding(query);

        return vectorStore.search(queryEmbedding, limit, 0.7, Map.of("agentId", agentId))
                .map(scoredMemories -> scoredMemories.stream()
                        .map(sm -> "- " + sm.getMemory().getContent())
                        .collect(Collectors.joining("\n")));
    }

    /**
     * Store observation/result from a tool execution.
     */
    public Uni<String> storeObservation(String agentId, String toolName, String observation) {
        return storeMemory(agentId,
                "Tool [" + toolName + "] output: " + observation,
                Map.of("type", "tool_output", "tool", toolName));
    }

    private float[] generateEmbedding(String text) {
        try {
            EmbeddingRequest request = new EmbeddingRequest(
                    java.util.List.of(text == null ? "" : text),
                    DEFAULT_MEMORY_EMBEDDING_MODEL,
                    null,
                    true);
            return embeddingService.embed(request).map(EmbeddingResponse::first).await().indefinitely();
        } catch (EmbeddingException ex) {
            log.warn("Embedding generation failed, using zero vector fallback: {}", ex.getMessage());
            return new float[512];
        }
    }

    private Map<String, Object> mergeMetadata(String agentId, Map<String, Object> metadata) {
        Map<String, Object> merged = new HashMap<>();
        merged.put("agentId", agentId);
        merged.put("timestamp", Instant.now().toString());
        merged.put("type", "conversation_history");
        if (metadata != null && !metadata.isEmpty()) {
            merged.putAll(metadata);
        }
        return merged;
    }
}

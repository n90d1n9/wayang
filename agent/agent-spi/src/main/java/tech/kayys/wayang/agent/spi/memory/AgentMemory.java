package tech.kayys.wayang.agent.spi.memory;

import io.smallrye.mutiny.Uni;
import tech.kayys.gollek.spi.Message;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core SPI for agent memory systems.
 *
 * <p>
 * Provides four layers of memory:
 * <ul>
 * <li><b>Working Memory</b> - Short-term scratchpad for current reasoning
 * turn</li>
 * <li><b>Conversation Memory</b> - Session history and user preferences</li>
 * <li><b>Vector Memory</b> - Semantic memory with embedding-based
 * retrieval</li>
 * <li><b>Episodic Memory</b> - Long-term storage of past agent sessions</li>
 * </ul>
 *
 * <h3>Memory Lifecycle:</h3>
 * 
 * <pre>
 *   Working Memory:    Created per run → Cleared after run
 *   Conversation:      Created per session → Expires after TTL
 *   Vector:            Persistent → Manual deletion
 *   Episodic:          Persistent → Auto-purge after retention period
 * </pre>
 *
 * <h3>Multi-Tenancy:</h3>
 * All memory operations are tenant-scoped. Tenant ID must be provided
 * either explicitly or via {@link TenantContext}.
 *
 * @author Gollek Team
 * @version 2.0.0
 */
public interface AgentMemory {

    // ═══════════════════════════════════════════════════════════════
    // Working Memory (Short-term, per-run)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Store a value in working memory.
     *
     * @param runId unique run identifier
     * @param key   memory key
     * @param value value to store (must be serializable)
     * @return completion future
     */
    Uni<Void> setWorking(String runId, String key, Object value);

    /**
     * Retrieve a value from working memory.
     *
     * @param runId unique run identifier
     * @param key   memory key
     * @param type  expected type
     * @param <T>   value type
     * @return optional value
     */
    <T> Uni<Optional<T>> getWorking(String runId, String key, Class<T> type);

    /**
     * Get all working memory entries for a run.
     *
     * @param runId unique run identifier
     * @return map of key-value pairs
     */
    Uni<Map<String, Object>> getAllWorking(String runId);

    /**
     * Clear working memory for a run.
     *
     * @param runId unique run identifier
     * @return completion future
     */
    Uni<Void> clearWorking(String runId);

    // ═══════════════════════════════════════════════════════════════
    // Conversation Memory (Session history)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add a message to conversation history.
     *
     * @param conversationId unique conversation identifier
     * @param tenantId       tenant identifier
     * @param message        message to add
     * @return completion future
     */
    Uni<Void> addMessage(String conversationId, String tenantId, Message message);

    /**
     * Get conversation history.
     *
     * @param conversationId unique conversation identifier
     * @param tenantId       tenant identifier
     * @param limit          maximum number of messages to retrieve (0 = all)
     * @return list of messages (newest last)
     */
    Uni<List<Message>> getConversation(String conversationId, String tenantId, int limit);

    /**
     * Get conversation metadata.
     *
     * @param conversationId unique conversation identifier
     * @param tenantId       tenant identifier
     * @return conversation metadata
     */
    Uni<ConversationMetadata> getConversationMetadata(String conversationId, String tenantId);

    /**
     * Clear conversation history.
     *
     * @param conversationId unique conversation identifier
     * @param tenantId       tenant identifier
     * @return completion future
     */
    Uni<Void> clearConversation(String conversationId, String tenantId);

    /**
     * List all conversations for a tenant.
     *
     * @param tenantId tenant identifier
     * @param after    only conversations created after this timestamp
     * @return list of conversation metadata
     */
    Uni<List<ConversationMetadata>> listConversations(String tenantId, Instant after);

    // ═══════════════════════════════════════════════════════════════
    // Vector Memory (Semantic memory with embeddings)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Store an embedding in vector memory.
     *
     * @param collection collection name
     * @param tenantId   tenant identifier
     * @param embedding  embedding data
     * @return stored embedding ID
     */
    Uni<String> storeEmbedding(String collection, String tenantId, EmbeddingData embedding);

    /**
     * Search for similar embeddings.
     *
     * @param collection collection name
     * @param tenantId   tenant identifier
     * @param query      query text (will be embedded if embedding service
     *                   available)
     * @param topK       number of results to return
     * @return search results
     */
    Uni<VectorSearchResult> searchSimilar(
            String collection,
            String tenantId,
            String query,
            int topK);

    /**
     * Search for similar embeddings using pre-computed vector.
     *
     * @param collection collection name
     * @param tenantId   tenant identifier
     * @param vector     query vector
     * @param topK       number of results to return
     * @return search results
     */
    Uni<VectorSearchResult> searchSimilarByVector(
            String collection,
            String tenantId,
            float[] vector,
            int topK);

    /**
     * Delete embeddings from a collection.
     *
     * @param collection   collection name
     * @param tenantId     tenant identifier
     * @param embeddingIds IDs of embeddings to delete (empty = delete all)
     * @return completion future
     */
    Uni<Void> deleteEmbeddings(
            String collection,
            String tenantId,
            List<String> embeddingIds);

    /**
     * List collections for a tenant.
     *
     * @param tenantId tenant identifier
     * @return list of collection names with metadata
     */
    Uni<List<CollectionMetadata>> listCollections(String tenantId);

    // ═══════════════════════════════════════════════════════════════
    // Episodic Memory (Long-term session storage)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Store an episode (complete agent session).
     *
     * @param episode episode to store
     * @return stored episode with generated ID
     */
    Uni<Episode> storeEpisode(Episode episode);

    /**
     * Retrieve episodes by tenant.
     *
     * @param tenantId tenant identifier
     * @param after    only episodes after this timestamp
     * @param limit    maximum number of episodes (0 = all)
     * @return list of episodes
     */
    Uni<List<Episode>> retrieveEpisodes(String tenantId, Instant after, int limit);

    /**
     * Retrieve a specific episode.
     *
     * @param episodeId episode identifier
     * @param tenantId  tenant identifier
     * @return optional episode
     */
    Uni<Optional<Episode>> getEpisode(String episodeId, String tenantId);

    /**
     * Delete an episode.
     *
     * @param episodeId episode identifier
     * @param tenantId  tenant identifier
     * @return completion future
     */
    Uni<Void> forgetEpisode(String episodeId, String tenantId);

    /**
     * Search episodes by content/summary.
     *
     * @param tenantId tenant identifier
     * @param query    search query
     * @param topK     number of results
     * @return search results
     */
    Uni<EpisodeSearchResult> searchEpisodes(String tenantId, String query, int topK);

    // ═══════════════════════════════════════════════════════════════
    // Fact Memory (Persistent Key-Value)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Store a persistent fact.
     *
     * @param tenantId tenant identifier
     * @param key      fact key
     * @param content  fact content
     * @param vector   optional embedding vector
     * @param metadata optional metadata
     * @return completion future
     */
    Uni<Void> storeFact(String tenantId, String key, String content, float[] vector, Map<String, Object> metadata);

    /**
     * Search for facts by vector similarity.
     *
     * @param tenantId tenant identifier
     * @param vector   query vector
     * @param limit    maximum results
     * @return list of matching facts
     */
    Uni<List<Fact>> searchFacts(String tenantId, float[] vector, int limit);

    /**
     * Delete a fact by key.
     *
     * @param tenantId tenant identifier
     * @param key      fact key
     * @return completion future
     */
    Uni<Void> deleteFact(String tenantId, String key);

    // ═══════════════════════════════════════════════════════════════
    // Memory Management
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get memory statistics.
     *
     * @param tenantId tenant identifier
     * @return memory statistics
     */
    Uni<MemoryStats> getStats(String tenantId);

    /**
     * Clear all memory for a tenant (use with caution).
     *
     * @param tenantId tenant identifier
     * @return completion future
     */
    Uni<Void> clearAll(String tenantId);

    /**
     * Check if memory system is healthy.
     *
     * @return health status
     */
    Uni<MemoryHealth> health();

    // ═══════════════════════════════════════════════════════════════
    // Value Types
    // ═══════════════════════════════════════════════════════════════

    /**
     * Embedding data for vector storage.
     */
    record EmbeddingData(
            String id,
            String content,
            float[] vector,
            Map<String, Object> metadata,
            Instant createdAt) {
    }

    /**
     * Vector search result.
     */
    record VectorSearchResult(
            List<VectorMatch> matches,
            long totalFound,
            long searchTimeMs) {
    }

    /**
     * Vector match with similarity score.
     */
    record VectorMatch(
            String id,
            String content,
            float similarity,
            Map<String, Object> metadata) {
    }

    /**
     * Collection metadata.
     */
    record CollectionMetadata(
            String name,
            long vectorCount,
            int dimensions,
            String distanceMetric,
            Instant createdAt,
            Instant updatedAt) {
    }

    /**
     * Conversation metadata.
     */
    record ConversationMetadata(
            String id,
            String tenantId,
            String userId,
            int messageCount,
            Instant createdAt,
            Instant lastActivityAt,
            Map<String, String> metadata) {
    }

    /**
     * Episode (complete agent session).
     */
    record Episode(
            String id,
            String tenantId,
            String userId,
            String type,
            String summary,
            String content,
            List<String> tags,
            Map<String, Object> metadata,
            Instant createdAt) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String id;
            private String tenantId;
            private String userId;
            private String type = "agent-session";
            private String summary;
            private String content;
            private List<String> tags = List.of();
            private Map<String, Object> metadata = Map.of();

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder tenantId(String tenantId) {
                this.tenantId = tenantId;
                return this;
            }

            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public Builder type(String type) {
                this.type = type;
                return this;
            }

            public Builder summary(String summary) {
                this.summary = summary;
                return this;
            }

            public Builder content(String content) {
                this.content = content;
                return this;
            }

            public Builder tags(List<String> tags) {
                this.tags = tags;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public Episode build() {
                return new Episode(
                        id != null ? id : java.util.UUID.randomUUID().toString(),
                        tenantId, userId, type, summary, content, tags, metadata,
                        Instant.now());
            }
        }
    }

    /**
     * Episode search result.
     */
    record EpisodeSearchResult(
            List<Episode> episodes,
            long totalFound,
            long searchTimeMs) {
    }

    /**
     * Memory statistics.
     */
    record MemoryStats(
            long workingMemoryEntries,
            long conversationCount,
            long conversationMessageCount,
            long vectorCount,
            long episodeCount,
            long storageBytes,
            Instant lastCleanup) {
    }

    /**
     * Fact record.
     */
    record Fact(String key, String text, Map<String, Object> metadata) {
    }

    /**
     * Memory health status.
     */
    record MemoryHealth(
            boolean isHealthy,
            String message,
            Map<String, Object> details,
            Instant checkedAt) {
        public static MemoryHealth healthy() {
            return new MemoryHealth(true, "OK", Map.of(), Instant.now());
        }

        public static MemoryHealth healthy(Map<String, Object> details) {
            return new MemoryHealth(true, "OK", details, Instant.now());
        }

        public static MemoryHealth unhealthy(String message) {
            return new MemoryHealth(false, message, Map.of(), Instant.now());
        }

        public static MemoryHealth unhealthy(String message, Map<String, Object> details) {
            return new MemoryHealth(false, message, details, Instant.now());
        }
    }
}

package tech.kayys.wayang.memory.service;

import tech.kayys.wayang.memory.model.*;
import tech.kayys.wayang.memory.entity.*;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the Memory Service
 */
@ApplicationScoped
public class MemoryServiceImpl implements MemoryService {
    
    private static final Logger LOG = LoggerFactory.getLogger(MemoryServiceImpl.class);
    
    @Inject
    RedisAPI redisAPI;
    
    @Inject
    MemoryEventPublisher eventPublisher;

    @Inject
    tech.kayys.wayang.embedding.EmbeddingService embeddingService;
    
    @ConfigProperty(name = "memory.cache.ttl", defaultValue = "PT1H")
    Duration cacheTTL;
    
    @ConfigProperty(name = "memory.max.conversations", defaultValue = "50")
    int maxConversations;
    
    @ConfigProperty(name = "memory.similarity.threshold", defaultValue = "0.7")
    double similarityThreshold;

    @Override
    @WithTransaction
    public Uni<MemoryContext> getContext(String sessionId, String userId) {
        LOG.info("Retrieving memory context for session: {}, user: {}", sessionId, userId);
        
        String cacheKey = "memory:context:" + sessionId;
        
        return redisAPI.get(cacheKey)
            .onItem().transformToUni(cached -> {
                if (cached != null) {
                    return deserializeContext(cached.toString());
                } else {
                    return loadContextFromDatabase(sessionId, userId)
                        .onItem().transformToUni(context -> 
                            cacheContext(cacheKey, context)
                                .replaceWith(context)
                        );
                }
            })
            .onFailure().recoverWithUni(throwable -> {
                LOG.warn("Failed to retrieve from cache, loading from database", throwable);
                return loadContextFromDatabase(sessionId, userId);
            });
    }

    @Override
    @WithTransaction
    public Uni<Void> storeContext(MemoryContext context) {
        LOG.info("Storing memory context for session: {}", context.getSessionId());
        
        return persistContextToDatabase(context)
            .onItem().transformToUni(unused -> {
                String cacheKey = "memory:context:" + context.getSessionId();
                return cacheContext(cacheKey, context);
            })
            .onItem().transformToUni(unused -> 
                eventPublisher.publishMemoryUpdated(context)
            )
            .replaceWithVoid();
    }

    @Override
    @WithTransaction
    public Uni<Void> storeExecutionResult(String sessionId, AgentResponse result) {
        LOG.info("Storing execution result for session: {}, result: {}", sessionId, result.getId());
        
        return createConversationMemory(result)
            .onItem().transformToUni(memory -> 
                persistExecutionResult(result)
                    .onItem().transformToUni(unused -> 
                        addMemoryToSession(sessionId, memory)
                    )
            )
            .onItem().transformToUni(unused -> {
                String cacheKey = "memory:context:" + sessionId;
                return invalidateCache(cacheKey);
            })
            .onItem().transformToUni(unused -> 
                eventPublisher.publishExecutionStored(sessionId, result)
            )
            .replaceWithVoid();
    }

    @Override
    public Uni<List<AgentResponse>> getRecentResults(String sessionId, int limit) {
        LOG.info("Retrieving recent results for session: {}, limit: {}", sessionId, limit);
        
        return ExecutionResultEntity.<ExecutionResultEntity>find(
                "sessionId = ?1 ORDER BY timestamp DESC", sessionId)
            .page(0, limit)
            .list()
            .onItem().transform(entities -> 
                entities.stream()
                    .map(this::convertToAgentResponse)
                    .collect(Collectors.toList())
            );
    }

    @Override
    public Uni<MemoryContext> summarizeAndCompact(String sessionId) {
        LOG.info("Summarizing and compacting memory for session: {}", sessionId);
        // This would involve calling an LLM to summarize the conversation history
        // and replacing multiple detailed entries with a single summary entry
        return getContext(sessionId, "system") // Placeholder: just return context for now
            .onItem().delayIt().by(Duration.ofMillis(100)); // Simulate work
    }

    // Additional semantic search capabilities
    @Override
    public Uni<List<ConversationMemory>> findSimilarMemories(String sessionId, String query, int limit) {
        LOG.info("Finding similar memories for session: {}, query: {}", sessionId, query);

        return embeddingService.embedOne(query)
            .onItem().transformToUni(queryVector -> {
                // This would normally call a vector store, but for now we'll simulate it
                // by fetching all memories for the session and calculating similarity.
                // In a real scenario, tech.kayys.wayang.memory.service.VectorMemoryStore would be used.
                return ConversationMemoryEntity.<ConversationMemoryEntity>find("sessionId = ?1", sessionId)
                    .list()
                    .onItem().transform(entities ->
                        entities.stream()
                            .map(entity -> {
                                double similarity = calculateCosineSimilarity(queryVector, entity.getEmbedding());
                                return convertToConversationMemory(entity, similarity);
                            })
                            .filter(m -> m.getRelevanceScore() >= similarityThreshold)
                            .sorted(Comparator.comparing(ConversationMemory::getRelevanceScore).reversed())
                            .limit(limit)
                            .collect(Collectors.toList())
                    );
            });
    }

    private double calculateCosineSimilarity(float[] vectorA, List<Float> vectorB) {
        if (vectorB == null || vectorA.length != vectorB.size()) return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB.get(i);
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // Private helper methods
    private Uni<MemoryContext> loadContextFromDatabase(String sessionId, String userId) {
        return MemorySessionEntity.<MemorySessionEntity>findById(sessionId)
            .onItem().transformToUni(entity -> {
                if (entity == null) {
                    return createNewSession(sessionId, userId);
                }
                
                return ConversationMemoryEntity.<ConversationMemoryEntity>find(
                        "sessionId = ?1 ORDER BY timestamp ASC", sessionId)
                    .list()
                    .onItem().transform(memories -> 
                        new MemoryContext(
                            entity.getSessionId(),
                            entity.getUserId(),
                            memories.stream()
                                .map(m -> convertToConversationMemory(m, null))
                                .collect(Collectors.toList()),
                            entity.getMetadata() != null ? new HashMap<>(entity.getMetadata()) : new HashMap<>(),
                            entity.getCreatedAt(),
                            entity.getUpdatedAt()
                        )
                    );
            });
    }

    private Uni<MemoryContext> createNewSession(String sessionId, String userId) {
        MemorySessionEntity entity = new MemorySessionEntity();
        entity.setSessionId(sessionId);
        entity.setUserId(userId);
        entity.setMetadata(new HashMap<>());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        
        return entity.persist()
            .onItem().transform(unused -> 
                new MemoryContext(
                    sessionId,
                    userId,
                    new ArrayList<>(),
                    new HashMap<>(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
                )
            );
    }

    private Uni<Void> persistContextToDatabase(MemoryContext context) {
        return MemorySessionEntity.<MemorySessionEntity>findById(context.getSessionId())
            .onItem().transformToUni(entity -> {
                boolean isNew = false;
                if (entity == null) {
                    entity = new MemorySessionEntity();
                    entity.setSessionId(context.getSessionId());
                    entity.setUserId(context.getUserId());
                    entity.setCreatedAt(context.getCreatedAt());
                    isNew = true;
                }
                
                entity.setMetadata(context.getMetadata().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()))));
                entity.setUpdatedAt(Instant.now());
                
                if (isNew) {
                    return entity.persist();
                }
                return Uni.createFrom().item(entity);
            })
            .replaceWithVoid();
    }

    private Uni<ConversationMemory> createConversationMemory(AgentResponse result) {
        return embeddingService.embedOne(result.getContent())
            .map(vector -> {
                List<Float> embeddingList = new ArrayList<>(vector.length);
                for (float v : vector) {
                    embeddingList.add(v);
                }

                return new ConversationMemory(
                    result.getId(),
                    "assistant",
                    result.getContent(),
                    result.getMetadata(),
                    embeddingList,
                    result.getTimestamp(),
                    1.0
                );
            });
    }

    private Uni<Void> addMemoryToSession(String sessionId, ConversationMemory memory) {
        ConversationMemoryEntity entity = new ConversationMemoryEntity();
        entity.setId(memory.getId());
        entity.setSessionId(sessionId);
        entity.setRole(memory.getRole());
        entity.setContent(memory.getContent());
        entity.setMetadata(memory.getMetadata().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
        entity.setEmbedding(memory.getEmbedding());
        entity.setTimestamp(memory.getTimestamp());
        entity.setRelevanceScore(memory.getRelevanceScore());
        
        return entity.persist().replaceWithVoid();
    }

    private Uni<Void> persistExecutionResult(AgentResponse result) {
        ExecutionResultEntity entity = new ExecutionResultEntity();
        entity.setId(result.getId());
        entity.setSessionId(result.getSessionId());
        entity.setContent(result.getContent());
        entity.setType(result.getType());
        entity.setStatus(result.getStatus());
        entity.setMetadata(result.getMetadata().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
        entity.setToolCalls(result.getToolCalls());
        entity.setTimestamp(result.getTimestamp());
        
        return entity.persist().replaceWithVoid();
    }

    private ConversationMemory convertToConversationMemory(ConversationMemoryEntity entity, Double relevanceScore) {
        return new ConversationMemory(
            entity.getId(),
            entity.getRole(),
            entity.getContent(),
            entity.getMetadata() != null ? new HashMap<>(entity.getMetadata()) : new HashMap<>(),
            entity.getEmbedding(),
            entity.getTimestamp(),
            relevanceScore != null ? relevanceScore : entity.getRelevanceScore()
        );
    }

    private AgentResponse convertToAgentResponse(ExecutionResultEntity entity) {
        return new AgentResponse(
            entity.getId(),
            entity.getSessionId(),
            entity.getContent(),
            entity.getType(),
            entity.getMetadata() != null ? new HashMap<>(entity.getMetadata()) : new HashMap<>(),
            entity.getTimestamp(),
            entity.getStatus(),
            entity.getToolCalls()
        );
    }

    private Uni<Void> cacheContext(String cacheKey, MemoryContext context) {
        return serializeContext(context)
            .onItem().transformToUni(serialized -> 
                redisAPI.setex(cacheKey, String.valueOf(cacheTTL.toSeconds()), serialized)
            )
            .replaceWithVoid();
    }

    private Uni<Void> invalidateCache(String cacheKey) {
        return redisAPI.del(List.of(cacheKey)).replaceWithVoid();
    }

    private Uni<MemoryContext> deserializeContext(String json) {
        // Simplified placeholder
        return Uni.createFrom().nullItem();
    }

    private Uni<String> serializeContext(MemoryContext context) {
        // Simplified placeholder
        return Uni.createFrom().item("{}");
    }
}
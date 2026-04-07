# RAG-Memory-Vector Integration Guide

## Overview

This guide describes the integration between RAG (Retrieval-Augmented Generation), Memory, and Vector executors, creating a unified system for intelligent information retrieval and generation.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        RAG System                                │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Retrieval   │  │  Generation  │  │  Reranking   │          │
│  │  Executor    │  │  Executor    │  │  Executor    │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                   │
│         └─────────────────┼─────────────────┘                   │
│                           │                                      │
│                  ┌────────▼────────┐                             │
│                  │ VectorRetriever │                             │
│                  └────────┬────────┘                             │
└───────────────────────────┼─────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼───────┐  ┌───────▼────────┐
│  Vector Store  │  │   Memory     │  │  Graph Store   │
│  (Dense)       │  │   (Semantic) │  │  (Relational)  │
└────────────────┘  └──────────────┘  └────────────────┘
```

## Integration Points

### 1. RAG + Vector Store

RAG uses vector stores for dense retrieval:

```java
@Inject
RetrievalExecutor retrievalExecutor;

@Inject
VectorStore vectorStore;

// Ingest documents into RAG with vector indexing
RagQuery query = RagQuery.builder()
    .query("What is machine learning?")
    .topK(5)
    .strategy(SearchStrategy.DENSE)  // Use vector search
    .build();

RagResult result = retrievalExecutor.retrieve(query);
```

### 2. RAG + Memory

RAG can retrieve from memory systems for context:

```java
@Inject
SemanticMemoryExecutor semanticMemory;

@Inject
ResponseGenerationExecutor generationExecutor;

// Get relevant memories as RAG context
List<Memory> memories = semanticMemory.search(query, 10, 0.7);

// Convert to RAG chunks
List<RagChunk> chunks = memories.stream()
    .map(m -> RagChunk.builder()
        .content(m.getContent())
        .metadata(m.getMetadata())
        .build())
    .collect(Collectors.toList());

// Generate response with memory-augmented context
RagResponse response = generationExecutor.generate(query, chunks);
```

### 3. RAG + Vector + Memory (Unified)

Complete integration for intelligent retrieval:

```java
@Inject
VectorMemoryAdapter vectorMemory;

@Inject
RetrievalExecutor ragRetriever;

public Uni<RagResponse> answerWithRAG(String query) {
    // 1. Get relevant memories from vector store
    List<Memory> memories = vectorMemory.searchSimilarMemories(query, 10);
    
    // 2. Convert to RAG chunks
    List<RagChunk> memoryChunks = memories.stream()
        .map(this::memoryToRagChunk)
        .collect(Collectors.toList());
    
    // 3. Retrieve additional documents from RAG
    RagQuery ragQuery = RagQuery.builder()
        .query(query)
        .topK(5)
        .build();
    
    RagResult ragResult = ragRetriever.retrieve(ragQuery);
    
    // 4. Combine memory chunks + RAG documents
    List<RagChunk> allChunks = Stream.concat(
            memoryChunks.stream(),
            ragResult.getChunks().stream())
        .collect(Collectors.toList());
    
    // 5. Generate response
    return generationExecutor.generate(query, allChunks);
}
```

## Configuration

```properties
# RAG Configuration
wayang.rag.enabled=true
wayang.rag.retrieval.top-k=5
wayang.rag.retrieval.threshold=0.7
wayang.rag.generation.model=llama-3.2-3b

# Vector Store Configuration
wayang.vector.store.type=pgvector
wayang.vector.store.pgvector.url=jdbc:postgresql://localhost:5432/wayang

# Memory Configuration
wayang.memory.semantic.enabled=true
wayang.memory.semantic.vector-index=semantic_memories

# Integration Settings
wayang.rag.memory.integration.enabled=true
wayang.rag.memory.weight=0.5  # Balance between memory and documents
```

## Usage Patterns

### Pattern 1: Memory-Augmented RAG

Use memory as additional context for RAG:

```java
public class MemoryAugmentedRAG {
    
    @Inject
    RetrievalExecutor ragRetriever;
    
    @Inject
    VectorMemoryAdapter vectorMemory;
    
    @Inject
    ResponseGenerationExecutor generator;
    
    public RagResponse answer(String query) {
        // Retrieve from RAG documents
        RagResult ragResult = ragRetriever.retrieve(
            RagQuery.builder().query(query).topK(5).build()
        );
        
        // Retrieve from memory
        List<Memory> memories = vectorMemory.searchSimilarMemories(query, 5);
        
        // Combine contexts
        List<RagChunk> combined = new ArrayList<>();
        combined.addAll(ragResult.getChunks());
        memories.forEach(m -> combined.add(memoryToChunk(m)));
        
        // Generate with full context
        return generator.generate(query, combined);
    }
}
```

### Pattern 2: Hierarchical RAG

Use memory for high-level context, RAG for details:

```java
public class HierarchicalRAG {
    
    public RagResponse answer(String query) {
        // 1. Get high-level context from working memory
        List<Memory> context = workingMemory.getRelevantContext(query, 3);
        
        // 2. Get detailed information from RAG
        RagResult details = ragRetriever.retrieve(
            RagQuery.builder()
                .query(query)
                .topK(10)
                .build()
        );
        
        // 3. Generate with hierarchical context
        return generator.generate(query, combine(context, details));
    }
}
```

### Pattern 3: Episodic RAG

Use episodic memory for conversation-aware RAG:

```java
public class EpisodicRAG {
    
    @Inject
    EpisodicMemoryExecutor episodicMemory;
    
    public RagResponse answerInContext(String sessionId, String query) {
        // Get conversation history
        ConversationMemory history = episodicMemory.getConversation(sessionId);
        
        // Use history to enhance RAG query
        String enhancedQuery = enhanceQueryWithHistory(query, history);
        
        // Retrieve with enhanced query
        RagResult result = ragRetriever.retrieve(
            RagQuery.builder().query(enhancedQuery).topK(5).build()
        );
        
        return generator.generate(query, result.getChunks());
    }
}
```

## API Reference

### RAG Core Components

| Component | Purpose | Integration |
|-----------|---------|-------------|
| `RetrievalExecutor` | Retrieve relevant documents | Uses VectorStore |
| `ResponseGenerationExecutor` | Generate responses | Uses retrieved chunks |
| `VectorRetriever` | Dense vector retrieval | Direct VectorStore access |
| `Reranker` | Re-rank results | Post-retrieval |

### Memory Integration

| Memory Type | RAG Use Case |
|-------------|--------------|
| Semantic | Knowledge base, facts |
| Episodic | Conversation history |
| Working | Current context |
| Long-term | Persistent knowledge |

### Vector Integration

| Vector Store | Best For |
|--------------|----------|
| In-Memory | Development, testing |
| FAISS | High-performance production |
| PGVector | PostgreSQL deployments |
| Milvus | Large-scale deployments |
| Qdrant | Feature-rich production |

## Performance Optimization

### Caching Strategy

```java
@Inject
ResponseCacheService cacheService;

public RagResponse getCachedOrRetrieve(String query) {
    // Check cache first
    Optional<RagResponse> cached = cacheService.get(query);
    if (cached.isPresent()) {
        return cached.get();
    }
    
    // Retrieve and generate
    RagResponse response = retrieveAndGenerate(query);
    
    // Cache for future
    cacheService.put(query, response);
    
    return response;
}
```

### Batch Retrieval

```java
// Batch multiple queries
List<String> queries = List.of("query1", "query2", "query3");

List<RagResult> results = queries.stream()
    .map(q -> ragRetriever.retrieve(
        RagQuery.builder().query(q).topK(5).build()))
    .collect(Collectors.toList());
```

### Parallel Processing

```java
// Retrieve from memory and documents in parallel
Uni<List<Memory>> memoryUni = Uni.createFrom()
    .item(() -> vectorMemory.searchSimilarMemories(query, 5));

Uni<RagResult> ragUni = Uni.createFrom()
    .item(() -> ragRetriever.retrieve(
        RagQuery.builder().query(query).topK(5).build()));

Uni.combine().all().unis(memoryUni, ragUni)
    .combinedWith((memories, ragResult) -> {
        // Combine and generate
        return generate(query, memories, ragResult);
    });
```

## Best Practices

1. **Balance Memory and Documents**: Don't rely solely on one source
2. **Use Appropriate TopK**: Adjust based on query complexity
3. **Implement Caching**: Cache frequent queries
4. **Monitor Performance**: Track retrieval and generation metrics
5. **Handle Failures Gracefully**: Fallback to memory if RAG fails

## Troubleshooting

### Irrelevant Results

**Problem**: RAG retrieves irrelevant documents

**Solutions**:
- Increase similarity threshold
- Use query expansion
- Add reranking step
- Combine with memory retrieval

### Slow Retrieval

**Problem**: RAG retrieval is slow

**Solutions**:
- Use appropriate vector store (FAISS for large datasets)
- Implement caching
- Reduce topK
- Add metadata filters

### Context Overload

**Problem**: Too much context for generator

**Solutions**:
- Reduce topK
- Use summarization
- Implement context compression
- Prioritize most relevant chunks

## Resources

- [RAG Documentation](../rag/README.md)
- [Vector Executor Documentation](../vector/README.md)
- [Memory Documentation](../memory/README.md)
- [Vector-Memory Integration](../memory/VECTOR_MEMORY_INTEGRATION.md)

# Vector-Memory Integration Guide

## Overview

The Vector-Memory integration provides seamless connection between vector storage and memory systems for AI agents, enabling:
- Semantic memory storage using vector embeddings
- Similarity-based memory retrieval
- Long-term memory with vector indexing
- RAG (Retrieval-Augmented Generation) support
- Episodic memory with semantic search

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Memory Executor                        │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Working     │  │  Episodic    │  │  Semantic    │  │
│  │  Memory      │  │  Memory      │  │  Memory      │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │           │
│         └─────────────────┼─────────────────┘           │
│                           │                              │
│                  ┌────────▼────────┐                     │
│                  │ Vector Adapter  │                     │
│                  └────────┬────────┘                     │
└───────────────────────────┼────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                   Vector Executor                       │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  In-Memory   │  │  FAISS       │  │  PGVector    │  │
│  │  Store       │  │  Store       │  │  Store       │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Integration Points

### 1. Semantic Memory + Vector Store

Semantic memories are stored as vectors for similarity-based retrieval:

```java
@Inject
SemanticMemoryExecutor semanticMemory;

@Inject
VectorStore vectorStore;

// Store semantic memory with embedding
Memory memory = Memory.builder()
    .type(MemoryType.SEMANTIC)
    .content("Machine learning is a subset of AI")
    .metadata(Map.of("topic", "AI", "confidence", 0.95))
    .build();

// Generate embedding and store
semanticMemory.store(memory);  // Automatically stores in vector store

// Search by similarity
List<Memory> results = semanticMemory.search(
    "What is ML?",
    5,  // top K
    0.7 // similarity threshold
);
```

### 2. Episodic Memory + Vector Store

Episodic memories (conversations, experiences) indexed by embeddings:

```java
@Inject
EpisodicMemoryExecutor episodicMemory;

// Store conversation
ConversationMemory conversation = ConversationMemory.builder()
    .sessionId("session-123")
    .messages(messages)
    .timestamp(Instant.now())
    .build();

episodicMemory.store(conversation);  // Indexed in vector store

// Find similar conversations
List<ConversationMemory> similar = episodicMemory.findSimilar(
    "User asking about pricing",
    0.8 // similarity threshold
);
```

### 3. Working Memory + Vector Context

Working memory uses vector store for context retrieval:

```java
@Inject
WorkingMemoryExecutor workingMemory;

// Get relevant context for current task
List<Memory> context = workingMemory.getRelevantContext(
    currentTask,
    10  // max items
);

// Context retrieved from vector store based on similarity
```

## Configuration

```properties
# Vector Store Configuration
wayang.vector.store.type=pgvector
wayang.vector.store.pgvector.url=jdbc:postgresql://localhost:5432/wayang
wayang.vector.store.pgvector.username=wayang
wayang.vector.store.pgvector.password=secret

# Memory Configuration
wayang.memory.semantic.enabled=true
wayang.memory.semantic.vector-index=semantic_memories
wayang.memory.episodic.enabled=true
wayang.memory.episodic.vector-index=episodic_memories
wayang.memory.working.enabled=true
wayang.memory.working.context-size=10

# Integration Settings
wayang.memory.vector.integration.enabled=true
wayang.memory.vector.similarity-threshold=0.7
wayang.memory.vector.max-results=20
wayang.memory.vector.embedding-model=sentence-transformers
```

## Usage Examples

### RAG with Vector-Memory Integration

```java
@Inject
VectorStore vectorStore;

@Inject
SemanticMemoryExecutor semanticMemory;

public Uni<String> answerQuestion(String question) {
    // 1. Retrieve relevant context from vector store
    VectorQuery query = VectorQuery.builder()
        .query(question)
        .topK(5)
        .threshold(0.7)
        .build();
    
    Uni<List<VectorEntry>> context = vectorStore.search(query);
    
    // 2. Also get semantic memories
    Uni<List<Memory>> memories = semanticMemory.search(question, 5, 0.7);
    
    // 3. Combine and generate answer
    return Uni.combine().all().unis(context, memories)
        .combinedWith((ctx, mem) -> {
            String augmentedPrompt = buildPrompt(question, ctx, mem);
            return llm.generate(augmentedPrompt);
        });
}
```

### Long-Term Memory Consolidation

```java
@Inject
LongTermMemoryExecutor longTermMemory;

@Inject
VectorStore vectorStore;

// Consolidate working memory to long-term with vector indexing
public void consolidateMemories() {
    List<Memory> workingMemories = workingMemory.getAll();
    
    for (Memory memory : workingMemories) {
        // Store in long-term memory
        longTermMemory.store(memory);
        
        // Also index in vector store for similarity search
        VectorEntry entry = VectorEntry.builder()
            .id(memory.getId())
            .content(memory.getContent())
            .metadata(memory.getMetadata())
            .build();
        
        vectorStore.store(List.of(entry));
    }
}
```

## API Reference

### VectorStore Interface

| Method | Description |
|--------|-------------|
| `store(List<VectorEntry>)` | Store entries with embeddings |
| `search(VectorQuery)` | Similarity search |
| `search(VectorQuery, Map)` | Search with filters |
| `delete(List<String>)` | Delete by IDs |
| `deleteByFilters(Map)` | Delete by filters |

### Memory Executors

| Executor | Vector Integration |
|----------|-------------------|
| `SemanticMemoryExecutor` | Stores/searches semantic memories as vectors |
| `EpisodicMemoryExecutor` | Indexes conversations by embeddings |
| `LongTermMemoryExecutor` | Persistent vector-indexed storage |
| `WorkingMemoryExecutor` | Context retrieval from vectors |

## Performance Considerations

| Operation | In-Memory | FAISS | PGVector |
|-----------|-----------|-------|----------|
| Store | O(1) | O(log n) | O(log n) |
| Search | O(n) | O(log n) | O(log n) |
| Delete | O(n) | O(log n) | O(log n) |
| Scale | <10K | >1M | >100K |

### Recommendations

1. **Use In-Memory** for development and testing (<10K entries)
2. **Use FAISS** for high-performance production (>1M entries)
3. **Use PGVector** for integrated PostgreSQL deployments

## Best Practices

1. **Index Frequently Accessed Memories**: Store important memories in vector store for fast retrieval
2. **Use Metadata Filters**: Combine similarity search with metadata filters for precision
3. **Batch Operations**: Store multiple entries together for better performance
4. **Set Appropriate Thresholds**: Tune similarity thresholds based on use case
5. **Monitor Vector Store Size**: Implement cleanup strategies for old memories

## Troubleshooting

### Low Similarity Scores

**Problem**: Retrieved memories have low similarity scores

**Solutions**:
- Lower the similarity threshold (default: 0.7)
- Use better embedding models
- Increase topK parameter

### Slow Search Performance

**Problem**: Vector search is slow

**Solutions**:
- Use FAISS for large datasets
- Add metadata filters to reduce search space
- Implement caching for frequent queries

### Memory Duplication

**Problem**: Same memory stored multiple times

**Solutions**:
- Implement deduplication before storing
- Use unique IDs based on content hash
- Set up memory consolidation jobs

## Resources

- [Vector Executor Documentation](../vector/README.md)
- [Memory Executor Documentation](../memory/README.md)
- [RAG Implementation Guide](../rag/README.md)

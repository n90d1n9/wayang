# Wayang Memory Module - Quick Start Guide

## What Was Improved?

| Issue | Before | After |
|-------|--------|-------|
| **Context Retrieval** | ❌ Empty | ✅ 10 recent memories |
| **Embeddings** | 1 option (TFIDF) | 2 options (TFIDF + OpenAI) |
| **Caching** | ❌ None | ✅ Automatic cache |
| **Metadata Search** | ❌ Vector only | ✅ Vector + Filter |

## Enable It

### Option 1: Keep Existing (Local Embeddings)
No changes needed - still works!

```properties
# application.properties
gamelan.embedding.provider=local
```

### Option 2: Use OpenAI (Recommended for Production)
```properties
# application.properties
gamelan.embedding.provider=openai
gamelan.embedding.openai.api-key=sk-your-api-key
gamelan.embedding.openai.cache-enabled=true
```

## Use It

### Get Agent Context
```java
@Inject
VectorAgentMemory agentMemory;

// Get 10 most recent memories for agent
agentMemory.getContext("agent-123")
    .subscribe().with(contextMemories -> {
        System.out.println("Found " + contextMemories.size() + " context items");
        contextMemories.forEach(memory -> 
            System.out.println(memory.content())
        );
    });
```

### Search by Metadata
```java
Map<String, Object> filters = Map.of(
    "agentId", "agent-123",
    "type", "user-message"
);

vectorMemoryStore.searchByFilter(filters)
    .subscribe().with(memories -> {
        // Process memories matching filters
    });
```

## Configuration

### Common Settings
```properties
# Context window size
wayang.memory.agent.context.limit=10

# Memory buffer size
wayang.memory.short.window.size=50

# Embedding cache (if using OpenAI)
gamelan.embedding.openai.cache.max-size=100000
gamelan.embedding.openai.cache.ttl-hours=24
```

## Documents

| Document | Purpose |
|----------|---------|
| **MEMORY_IMPROVEMENTS.md** | Detailed implementation & architecture |
| **CONFIGURATION_GUIDE.md** | All configuration options |
| **IMPROVEMENTS_SUMMARY.txt** | High-level overview |
| **QUICK_START.md** | This file - quick reference |

## Troubleshooting

**Empty context returned?**
```properties
# Check these settings
wayang.memory.agent.context.limit=10
wayang.memory.agent.context.sort-by=timestamp
```

**Slow embeddings?**
```properties
# Enable caching
gamelan.embedding.openai.cache-enabled=true
```

**Red, slow performance?**
```properties
# Reduce cache size
gamelan.embedding.openai.cache.max-size=50000
wayang.memory.cache.max-size-mb=256
```

## What's Next?

1. ✅ Context retrieval (DONE)
2. ✅ OpenAI embeddings (DONE)
3. ⏳ Persistence for all memory types (next)
4. ⏳ Memory consolidation (next)
5. ⏳ Thread safety improvements (next)

See MEMORY_IMPROVEMENTS.md for full roadmap.

## Support

- Configuration issues → see **CONFIGURATION_GUIDE.md**
- Architecture questions → see **MEMORY_IMPROVEMENTS.md**
- Quick answers → see **QUICK_START.md** (this file)

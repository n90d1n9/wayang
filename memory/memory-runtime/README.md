# Memory Executors Module

Comprehensive memory executor implementations for the Wayang AI Agent Platform, providing multiple memory types with specialized handling for different cognitive memory functions.

## Overview

The memory executors module implements a multi-tier memory system inspired by human cognitive architecture, providing:

- **Short-Term Memory**: Buffer-based recent conversation history
- **Long-Term Memory**: Vector-based persistent semantic storage
- **Semantic Memory**: Factual knowledge and concepts
- **Episodic Memory**: Event-based experiences with temporal context
- **Working Memory**: Active task context with attention management

## Architecture

```
wayang-memory-runtime/
├── src/main/java/tech/kayys/wayang/memory/executor/
│   ├── MemoryOperationType.java          # Operation type enum
│   ├── AbstractMemoryExecutor.java       # Base executor class
│   ├── ShortTermMemoryExecutor.java      # Buffer memory implementation
│   ├── LongTermMemoryExecutor.java       # Vector storage implementation
│   ├── SemanticMemoryExecutor.java       # Knowledge memory implementation
│   ├── EpisodicMemoryExecutor.java       # Event memory implementation
│   └── WorkingMemoryExecutor.java        # Active context implementation
└── src/test/java/tech/kayys/wayang/memory/executor/
    ├── ShortTermMemoryExecutorTest.java
    ├── LongTermMemoryExecutorTest.java
    └── WorkingMemoryExecutorTest.java
```

## Memory Types

### 1. Short-Term Memory (`short-memory-executor`)

Buffer-based memory for recent conversation history with FIFO management.

**Features:**
- Configurable window size (default: 20 entries)
- Automatic oldest-entry eviction
- Fast O(1) access to recent context
- Text-based search capability

**Configuration:**
```properties
wayang.memory.short.window.size=20
```

**Usage Example:**
```java
Map<String, Object> context = Map.of(
    "agentId", "agent-123",
    "operation", "store",
    "content", "User asked about weather",
    "memoryType", "short"
);
```

**Supported Operations:**
- `store` - Add entry to buffer
- `context` - Retrieve recent entries
- `search` - Text search within buffer
- `clear` - Clear buffer
- `stats` - Get buffer statistics

### 2. Long-Term Memory (`longterm-memory-executor`)

Vector-based persistent memory with semantic search capabilities.

**Features:**
- Semantic similarity search
- Importance-based memory management
- Automatic importance decay
- Namespace-based isolation
- Consolidation support

**Configuration:**
```properties
wayang.memory.longterm.importance.threshold=0.5
wayang.memory.longterm.decay.rate=0.01
wayang.memory.longterm.search.limit=10
wayang.memory.longterm.min.similarity=0.7
```

**Usage Example:**
```java
Map<String, Object> context = Map.of(
    "agentId", "agent-123",
    "operation", "search",
    "query", "user preferences",
    "memoryType", "longterm",
    "limit", 5,
    "minSimilarity", 0.75
);
```

**Supported Operations:**
- `store` - Store with vector embedding
- `search` - Semantic similarity search
- `context` - Retrieve recent memories
- `delete` - Delete specific memory
- `clear` - Clear namespace
- `consolidate` - Apply importance decay
- `stats` - Get storage statistics

### 3. Semantic Memory (`semantic-memory-executor`)

Knowledge-based memory for facts, concepts, and relationships.

**Features:**
- Category-based organization
- Concept linking
- Knowledge type classification
- High importance retention

**Configuration:**
```properties
wayang.memory.semantic.default.category=general
wayang.memory.semantic.concept.linking=true
wayang.memory.semantic.search.limit=15
```

**Usage Example:**
```java
Map<String, Object> context = Map.of(
    "agentId", "agent-123",
    "operation", "store",
    "content", "Java is an object-oriented programming language",
    "memoryType", "semantic",
    "category", "programming",
    "concepts", ["Java", "OOP", "programming language"]
);
```

**Supported Operations:**
- `store` - Store knowledge entry
- `search` - Search by query and category
- `context` - Get knowledge by category
- `clear` - Clear category
- `stats` - Get category statistics

### 4. Episodic Memory (`episodic-memory-executor`)

Event-based memory for personal experiences with temporal context.

**Features:**
- Time-stamped event storage
- Temporal filtering
- Participant tracking
- Location context
- Emotional valence

**Configuration:**
```properties
wayang.memory.episodic.default.event.type=general
wayang.memory.episodic.temporal.ordering=true
wayang.memory.episodic.search.limit=20
wayang.memory.episodic.related.time.window.hours=24
```

**Usage Example:**
```java
Map<String, Object> context = Map.of(
    "agentId", "agent-123",
    "operation", "store",
    "content", "Meeting with team about project launch",
    "memoryType", "episodic",
    "eventType", "meeting",
    "eventTime", "2024-01-15T10:00:00Z",
    "participants", ["Alice", "Bob", "Charlie"],
    "location", "Conference Room A"
);
```

**Supported Operations:**
- `store` - Store event memory
- `search` - Search with temporal filters
- `context` - Get recent events
- `clear` - Clear by event type
- `stats` - Get event statistics

### 5. Working Memory (`working-memory-executor`)

Active task context with attention-based prioritization.

**Features:**
- Slot-based organization
- Attention scoring
- Capacity management
- TTL-based expiration
- Access tracking

**Configuration:**
```properties
wayang.memory.working.capacity=7
wayang.memory.working.ttl.minutes=30
wayang.memory.working.attention.enabled=true
```

**Usage Example:**
```java
Map<String, Object> context = Map.of(
    "agentId", "agent-123",
    "operation", "store",
    "content", "Current task: implement user authentication",
    "memoryType", "working",
    "slot", "active-task",
    "attention", 0.9,
    "ttlMinutes", 60
);
```

**Supported Operations:**
- `store` - Store in slot
- `context` - Get active context
- `search` - Search within working memory
- `update` - Update existing entry
- `delete` - Remove entry
- `clear` - Clear slot or all
- `stats` - Get utilization stats

## Memory Operations

All executors support the following operation types via the `operation` context field:

| Operation | Description | Read/Write |
|-----------|-------------|------------|
| `store` | Store/create memory entry | Write |
| `retrieve` | Retrieve relevant memories | Read |
| `search` | Search with query/filters | Read |
| `update` | Update existing entry | Write |
| `delete` | Delete specific entry | Write |
| `clear` | Clear memory/namespace | Write |
| `context` | Get current context | Read |
| `consolidate` | Consolidate memories | Write |
| `stats` | Get statistics | Read |

## Usage in Workflow Nodes

### Example: RAG with Memory Context

```yaml
node:
  type: rag-query
  config:
    executorType: rag-executor
    query: "${input.question}"
    
  # First, retrieve relevant memory context
  preExecutors:
    - type: longterm-memory
      config:
        operation: search
        query: "${input.question}"
        limit: 5
        outputMapping: context.memories
        
    - type: short-memory
      config:
        operation: context
        limit: 10
        outputMapping: context.recent
```

### Example: Multi-Memory Agent

```yaml
node:
  type: agent-task
  config:
    agentType: conversational-agent
    
  # Use multiple memory types
  memoryConfig:
    - type: working-memory
      operation: context
      slot: conversation-state
      
    - type: episodic-memory
      operation: search
      eventType: conversation
      limit: 5
      
    - type: semantic-memory
      operation: context
      category: user-preferences
```

## Integration with AgentMemory SPI

The executors integrate with the `AgentMemory` SPI for unified memory access:

```java
@Inject
AgentMemory agentMemory;

// Store memory
agentMemory.store(agentId, new MemoryEntry(
    null,  // ID (auto-generated)
    "Content to remember",
    Instant.now(),
    Map.of("type", "conversation")
));

// Retrieve relevant memories
Uni<List<MemoryEntry>> memories = 
    agentMemory.retrieve(agentId, "search query", 10);

// Get context
Uni<List<MemoryEntry>> context = 
    agentMemory.getContext(agentId);
```

## Testing

Run tests with:

```bash
mvn test -pl wayang/executors/memory/memory-runtime
```

Individual test classes:
- `ShortTermMemoryExecutorTest` - Buffer memory tests
- `LongTermMemoryExecutorTest` - Vector storage tests
- `WorkingMemoryExecutorTest` - Active context tests

## Performance Considerations

| Memory Type | Access Pattern | Best For |
|-------------|---------------|----------|
| Short-Term | Sequential FIFO | Recent conversation history |
| Long-Term | Vector similarity | Semantic search, facts |
| Semantic | Category + vector | Knowledge bases, concepts |
| Episodic | Temporal + vector | Events, experiences |
| Working | Random access (slots) | Active task context |

## Monitoring

Memory executors expose metrics via Micrometer:

- `memory.operations.total` - Total operations by type
- `memory.operations.duration` - Operation latency
- `memory.entries.count` - Entry count by type
- `memory.capacity.utilization` - Capacity usage (working memory)

## Error Handling

All operations return structured error responses:

```json
{
  "success": false,
  "error": "Missing required field: content",
  "operation": "store",
  "memoryType": "short"
}
```

## Future Enhancements

- [ ] Memory consolidation strategies
- [ ] Cross-memory-type queries
- [ ] Memory compression/summarization
- [ ] Distributed memory storage
- [ ] Memory access pattern analytics
- [ ] Automatic memory type selection

## Related Modules

- `wayang-memory-core` - Core memory interfaces and models
- `wayang-vector-core` - Vector storage infrastructure
- `workflow-gamelan` - Workflow orchestration engine

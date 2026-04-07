# Agent + Memory Module Integration Guide

## Overview

This guide provides detailed instructions for integrating the **wayang-gollek memory module** with the **wayang-gollek agent module** to enable stateful, context-aware agent execution.

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                   Agent Execution Layer                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         AgentOrchestrator                            │   │
│  │  (Orchestrates agent execution, tool calling)        │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
                           ↕ (before & after)
┌────────────────────────────────────────────────────────────┐
│              AgentMemoryService (Bridge)                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ • getContextPrompt() - Retrieve conversation history │   │
│  │ • storeInteraction() - Save interaction to memory    │   │
│  │ • getMemoryStats() - Memory usage statistics         │   │
│  │ • recordSkillUsage() - Track skill execution         │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
                           ↕ (storage & retrieval)
┌────────────────────────────────────────────────────────────┐
│                 Memory Module Layer                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         VectorAgentMemory + VectorMemoryStore        │   │
│  │  (Semantic search, embeddings, persistence)          │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
```

## Integration Points

### 1. Agent Request Enhancement (Pre-Execution)

Before executing an agent, retrieve conversation history to provide context:

```java
@Inject
AgentMemoryService memoryService;

@Inject
AgentOrchestrator orchestrator;

Uni<AgentResponse> executeWithContext(String agentId, String userPrompt) {
    // Step 1: Retrieve conversation history
    return memoryService.getContextPrompt(agentId, 10)
        // Step 2: Enhance system prompt with context
        .map(context -> userPrompt + context)
        // Step 3: Create agent request
        .map(enhancedPrompt -> AgentRequest.builder()
            .agentId(agentId)
            .prompt(enhancedPrompt)
            .strategy(OrchestrationStrategy.REACT)
            .build())
        // Step 4: Execute agent
        .flatMap(request -> orchestrator.execute(request));
}
```

### 2. Interaction Storage (Post-Execution)

After agent execution, store the interaction for future context:

```java
Uni<Void> storeAgentInteraction(String agentId, AgentRequest req, AgentResponse resp) {
    return memoryService.storeInteraction(
        agentId,
        req.sessionId(),
        req.userId(),
        req.prompt(),
        resp.content()
    );
}
```

### 3. Session-Based Context

Retrieve context specific to a conversation session:

```java
Uni<String> getSessionContext(String agentId, String sessionId, int limit) {
    return memoryService.getSessionMemories(agentId, sessionId, limit)
        .map(memories -> memories.stream()
            .map(MemoryEntry::getContent)
            .collect(Collectors.joining("\n")));
}
```

### 4. Skill Execution Tracking

Record which skills succeed/fail for adaptive skill selection:

```java
Uni<Void> executeSkillWithTracking(String agentId, String skillId) {
    return executeSkill(skillId)
        .flatMap(result -> memoryService.recordSkillUsage(
            agentId,
            skillId,
            true,
            result))
        .onFailure()
        .flatMap(ex -> memoryService.recordSkillUsage(
            agentId,
            skillId,
            false,
            ex.getMessage()));
}
```

## Implementation Patterns

### Pattern 1: Stateful Agent Loop

```java
public class StatefulAgentLoop {
    
    @Inject AgentMemoryService memory;
    @Inject AgentOrchestrator orchestrator;
    
    public Uni<List<AgentResponse>> executeMultiTurnConversation(
            String agentId,
            String sessionId,
            List<String> prompts) {
        
        return Uni.combine()
            .all()
            .unis(prompts.stream()
                .map(prompt -> executeTurn(agentId, sessionId, prompt))
                .collect(Collectors.toList()))
            .asList();
    }
    
    private Uni<AgentResponse> executeTurn(
            String agentId,
            String sessionId,
            String prompt) {
        
        return memory.getContextPrompt(agentId, 5)
            .flatMap(context -> {
                AgentRequest request = AgentRequest.builder()
                    .prompt(prompt + context)
                    .agentId(agentId)
                    .sessionId(sessionId)
                    .build();
                
                return orchestrator.execute(request);
            })
            .flatMap(response -> 
                memory.storeInteraction(
                    agentId, sessionId, null, prompt, 
                    response.content())
                .map(__ -> response));
    }
}
```

### Pattern 2: Memory-Aware Skill Selection

```java
public class MemoryAwareSkillSelector {
    
    @Inject AgentMemoryService memory;
    @Inject SkillRegistry skillRegistry;
    
    public Uni<Skill> selectBestSkill(String agentId, String task) {
        return memory.getMemoryStats(agentId)
            .flatMap(stats -> {
                // Get list of previously successful skills
                return skillRegistry.getSkillsBySuccessRate(
                    agentId, 
                    stats.totalMemories());
            })
            .map(skills -> skills.get(0));  // Best skill
    }
}
```

### Pattern 3: Fallback with Memory Context

```java
public class MemoryAwareErrorRecovery {
    
    @Inject AgentMemoryService memory;
    @Inject AgentOrchestrator orchestrator;
    
    public Uni<AgentResponse> executeWithFallback(
            String agentId,
            AgentRequest request) {
        
        return orchestrator.execute(request)
            .onFailure()
            .flatMap(ex -> {
                // Try again with memory context suggesting different approach
                return memory.getContextPrompt(agentId, 15)
                    .map(context -> request.withPrompt(
                        request.prompt() + 
                        "\n\nPrevious attempts:\n" + context))
                    .flatMap(orchestrator::execute);
            });
    }
}
```

## Configuration

### Dependency Injection Setup

```java
// In application configuration
@ApplicationScoped
public class MemoryConfig {
    
    @Produces
    @ApplicationScoped
    AgentMemoryService agentMemoryService(
            VectorAgentMemory vectorMemory,
            VectorMemoryStore memoryStore) {
        return new AgentMemoryService(vectorMemory, memoryStore);
    }
}
```

### Configuration Properties

Add to `application.properties`:

```properties
# Memory configuration
gamelan.embedding.provider=openai
gamelan.embedding.openai.api-key=${OPENAI_API_KEY}
gamelan.embedding.cache.enabled=true
gamelan.embedding.cache.ttl.minutes=60
gamelan.embedding.cache.max-size=100000

# Agent memory configuration
wayang.memory.agent.context.limit=10
wayang.memory.agent.session.ttl=7d
wayang.memory.agent.consolidation.enabled=true

# Vector store configuration
wayang.vectorstore.implementation=inmemory
wayang.vectorstore.embedding.dimension=1536
```

## REST API Examples

### Example 1: Chat with Memory Context

```bash
curl -X POST http://localhost:8080/agents/my-agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "sessionId": "session-456",
    "message": "What did we discuss earlier?"
  }'
```

Response:
```json
{
  "requestId": "req-789",
  "content": "Earlier we discussed...",
  "success": true,
  "status": "Successfully executed with memory context"
}
```

### Example 2: Get Memory Statistics

```bash
curl http://localhost:8080/agents/my-agent/memory/stats
```

Response:
```json
{
  "agentId": "my-agent",
  "totalMemories": 42,
  "averageSize": 156.7,
  "lastUpdated": "2024-03-31T10:30:00Z",
  "success": true
}
```

### Example 3: Clear Agent Memory

```bash
curl -X DELETE http://localhost:8080/agents/my-agent/memory
```

## Testing Integration

### Unit Test Example

```java
@QuarkusTest
public class AgentMemoryIntegrationTest {
    
    @Inject
    AgentMemoryService memoryService;
    
    @Inject
    AgentOrchestrator orchestrator;
    
    @Test
    public void testContextRetrievalBeforeExecution() {
        String agentId = "test-agent";
        
        // Store some interactions
        MemoryEntry entry1 = new MemoryEntry(
            UUID.randomUUID().toString(),
            "User asked about weather",
            Instant.now(),
            Map.of("agentId", agentId, "type", "user-message"));
        
        MemoryEntry entry2 = new MemoryEntry(
            UUID.randomUUID().toString(),
            "Provided weather information",
            Instant.now(),
            Map.of("agentId", agentId, "type", "agent-message"));
        
        // Retrieve context
        String context = memoryService.getContextPrompt(agentId, 10)
            .await().indefinitely();
        
        assertTrue(context.contains("Conversation History"));
    }
    
    @Test
    public void testInteractionStorage() {
        String agentId = "test-agent";
        
        AgentResponse response = new AgentResponse(
            "req-1", "Hello!", true, "SUCCESS", 100);
        
        memoryService.storeInteraction(
            agentId,
            "session-1",
            "user-1",
            "Hi there",
            "Hello!")
            .await().indefinitely();
        
        List<MemoryEntry> memories = memoryService.getSessionMemories(
            agentId, "session-1")
            .await().indefinitely();
        
        assertEquals(2, memories.size());  // User + agent message
    }
}
```

### Integration Test Example

```java
@QuarkusTest
public class EndToEndAgentMemoryTest {
    
    @Inject
    AgentMemoryService memory;
    
    @Inject
    AgentOrchestrator orchestrator;
    
    @Test
    public void testFullConversationFlow() {
        String agentId = "e2e-agent";
        String sessionId = "e2e-session";
        
        // Turn 1
        executeAndStore(agentId, sessionId, "What is 2+2?");
        
        // Turn 2 - should have context from Turn 1
        String context = memory.getContextPrompt(agentId, 10)
            .await().indefinitely();
        
        assertTrue(context.contains("2+2"));
    }
}
```

## Common Patterns

### Multi-Turn Conversation

```java
public Uni<List<String>> multiTurnConversation(
        String agentId,
        String sessionId,
        List<String> userMessages) {
    
    return Uni.combine()
        .all()
        .unis(userMessages.stream()
            .map(msg -> memory.getContextPrompt(agentId, 5)
                .flatMap(ctx -> executeAgent(agentId, msg + ctx))
                .flatMap(resp -> 
                    memory.storeInteraction(agentId, sessionId, null, 
                        msg, resp.content())
                    .map(__ -> resp.content())))
            .collect(Collectors.toList()))
        .asList();
}
```

### Skill Selection Based on History

```java
public Uni<String> selectApproach(String agentId, String task) {
    return memory.getSessionMemories(agentId, null, 20)
        .map(memories -> {
            // Analyze past approaches for similar tasks
            Map<String, Integer> approachCounts = new HashMap<>();
            for (MemoryEntry m : memories) {
                String approach = extractApproach(m);
                approachCounts.merge(approach, 1, Integer::sum);
            }
            
            // Return most common approach
            return approachCounts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("default");
        });
}
```

## Performance Considerations

### Embedding Cache

The memory module includes embedding caching with configurable settings:

```properties
# Configure cache for better performance
gamelan.embedding.cache.enabled=true
gamelan.embedding.cache.ttl.minutes=120
gamelan.embedding.cache.max-size=100000
gamelan.embedding.cache.batch-size=32
```

Expected improvements:
- **Cache hit rate**: 85-90% for typical conversational workloads
- **Latency reduction**: 50-200ms per embedding (with cache) vs 500-2000ms (without)

### Memory Query Optimization

```java
// Use filter-based search for exact matches (faster)
memory.getSessionMemories(agentId, sessionId, 10);

// Use vector search for semantic similarity (slower but more relevant)
vectorMemoryStore.search("semantic query", 10);

// Use hybrid approach for best results
// Filter first, then semantic within filtered results
```

## Troubleshooting

### Issue: Empty Context Returned

```java
// Check if memories are being stored
AgentMemoryStats stats = memory.getMemoryStats(agentId)
    .await().indefinitely();

if (stats.totalMemories() == 0) {
    // No memories stored yet - first interaction should populate
    LOG.warn("No memories found for agent {}", agentId);
}
```

### Issue: Memory Bloat

```java
// Clean up old memories periodically
scheduler.scheduleAtFixedRate(
    () -> memory.clearMemory(agentId),
    Duration.ofDays(7));  // Clear after 7 days
```

### Issue: Slow Context Retrieval

```java
// Reduce context limit for faster retrieval
memory.getContextPrompt(agentId, 5);  // Get only 5 entries instead of 10

// Or enable caching
gamelan.embedding.cache.enabled=true
```

## Next Steps

1. **Implement custom context filtering**
   - Filter by memory type (episodic vs semantic)
   - Filter by importance/relevance score
   - Filter by timestamp range

2. **Add memory consolidation**
   - Merge similar memories
   - Generate summaries of old conversations
   - Implement forgetting (TTL-based cleanup)

3. **Advanced skill selection**
   - Use memory to predict best skills
   - Track skill success rates per task type
   - Auto-adjust skill parameters based on history

4. **Distributed agent scenarios**
   - Share memory across agent instances
   - Implement distributed consensus for memory updates
   - Use Redis for shared embedding cache

## References

- [Memory Module Documentation](./MEMORY_IMPROVEMENTS.md)
- [Agent Module SPI](./agent-spi/README.md)
- [Configuration Guide](./CONFIGURATION_GUIDE.md)
- [OpenAI Embedding Service](./memory-core/src/main/java/tech/kayys/wayang/memory/service/OpenAIEmbeddingService.java)

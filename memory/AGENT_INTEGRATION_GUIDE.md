# Memory-Agent Integration Guide

## Overview

This guide explains how to integrate the improved wayang-gollek memory module with the wayang-gollek agent module for enhanced context-aware reasoning and decision-making.

## Integration Architecture

```
Agent Request
    ↓
[Pre-Processing Phase]
    ├─ Extract agentId, sessionId from request
    ├─ Call MemoryIntegrationPlugin
    │  ├─ Retrieve agent context via VectorAgentMemory.getContext()
    │  ├─ Inject context as system message
    │  └─ Optionally store user prompt in memory
    └─ Enhance system prompt with context
    ↓
[LLM Inference]
    ├─ Use context-enhanced system prompt
    ├─ Better reasoning with conversation history
    └─ More coherent responses
    ↓
[Post-Processing Phase]
    ├─ Extract LLM response
    ├─ Store response/observations in memory
    ├─ Update agent state
    └─ Return enriched response
    ↓
Agent Response + Memory Updated
```

## Integration Points

### 1. AgentRequest Enhancement

Add memory configuration to agent requests:

```java
AgentRequest request = AgentRequest.builder()
    .prompt("What was our previous discussion?")
    .agentId("agent-123")
    .sessionId("session-456")
    .userId("user-789")
    // NEW: Memory configuration
    .memoryConfig(AgentMemoryConfig.builder()
        .enabled(true)
        .includeContext(true)
        .contextLimit(10)  // 10 most recent memories
        .persistMemory(true)
        .build())
    .build();
```

### 2. Memory-Aware Orchestration

Agent orchestrator retrieves context before reasoning:

```java
@Override
public Uni<AgentState> step(AgentState state) {
    // Get memory context first
    return getMemoryContext(state.agentId(), state.sessionId())
        .flatMap(contextMemories -> {
            // Enhance system prompt with context
            String enhancedPrompt = enhancePrompt(
                state.systemPrompt(),
                contextMemories
            );
            
            // Continue with enhanced context
            return llmService.inference(
                state.userPrompt(),
                enhancedPrompt
            ).map(response -> 
                state.withObservation(response)
            );
        });
}

private Uni<List<MemoryEntry>> getMemoryContext(String agentId, String sessionId) {
    return agentMemory.getContext(agentId)
        .onFailure().recoverWithItem(Collections.emptyList());
}
```

### 3. Memory Persistence

Store agent interactions in memory:

```java
@Override
public Uni<AgentResponse> execute(AgentRequest request) {
    return orchestrate(request)
        // Store prompt in memory
        .flatMap(response -> {
            MemoryEntry promptEntry = new MemoryEntry(
                UUID.randomUUID().toString(),
                "user:" + request.prompt(),
                Instant.now(),
                Map.of(
                    "agentId", request.agentId(),
                    "sessionId", request.sessionId(),
                    "userId", request.userId(),
                    "type", "user-prompt"
                )
            );
            
            return agentMemory.store(
                request.agentId(),
                promptEntry
            );
        })
        // Store response in memory
        .flatMap(__ -> {
            MemoryEntry responseEntry = new MemoryEntry(
                UUID.randomUUID().toString(),
                "assistant:" + response.content(),
                Instant.now(),
                Map.of(
                    "agentId", request.agentId(),
                    "sessionId", request.sessionId(),
                    "userId", request.userId(),
                    "type", "agent-response"
                )
            );
            
            return agentMemory.store(request.agentId(), responseEntry)
                .map(__ -> response);
        });
}
```

## Implementation Steps

### Step 1: Dependency Injection

Inject the memory service into your agent orchestrator:

```java
@ApplicationScoped
public class ReActOrchestrator implements AgentOrchestrator {
    
    @Inject
    VectorAgentMemory agentMemory;
    
    @Inject
    VectorMemoryStore vectorMemoryStore;
    
    @Inject
    EmbeddingService embeddingService;
    
    // ... rest of orchestrator
}
```

### Step 2: Context Retrieval in Pre-Processing

```java
private String buildSystemPromptWithContext(
        String basePrompt,
        String agentId,
        String sessionId) {
    return agentMemory.getContext(agentId)
        .map(contextMemories -> {
            StringBuilder context = new StringBuilder(basePrompt);
            
            if (!contextMemories.isEmpty()) {
                context.append("\n\n## Conversation History:\n");
                contextMemories.forEach(memory -> {
                    context.append("- ").append(memory.content()).append("\n");
                });
            }
            
            return context.toString();
        })
        .await().indefinitely();  // or use reactive chains
}
```

### Step 3: Memory Storage in Post-Processing

```java
private Uni<Void> storeInteractionInMemory(
        String agentId,
        String prompt,
        String response,
        String sessionId,
        String userId) {
    
    // Store user prompt
    MemoryEntry userEntry = new MemoryEntry(
        UUID.randomUUID().toString(),
        prompt,
        Instant.now(),
        Map.of(
            "agentId", agentId,
            "sessionId", sessionId,
            "userId", userId,
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
            "sessionId", sessionId,
            "userId", userId,
            "type", "agent-message",
            "source", "agent-interaction"
        )
    );
    
    return agentMemory.store(agentId, userEntry)
        .flatMap(__ -> agentMemory.store(agentId, agentEntry));
}
```

## Configuration

Add to `application.properties`:

```properties
# Agent Memory Integration
wayang.agent.memory.enabled=true
wayang.agent.memory.persist=true
wayang.agent.memory.context-limit=10
wayang.agent.memory.include-metadata=true

# Use which embedding provider (from memory module)
gamelan.embedding.provider=openai
gamelan.embedding.openai.api-key=${OPENAI_API_KEY}
gamelan.embedding.openai.cache-enabled=true

# Agent request timeout (for memory operations)
wayang.agent.memory.operation-timeout-secs=10

# Max memory retrieval time
wayang.agent.memory.retrieve-timeout-secs=5
```

## Using Agent with Memory

### Example 1: Simple Agent with Context

```java
@POST
@Path("/agents/{agentId}/chat")
@Produces(MediaType.APPLICATION_JSON)
public Uni<AgentResponse> chat(
        @PathParam("agentId") String agentId,
        ChatRequest chatRequest) {
    
    AgentRequest request = AgentRequest.builder()
        .agentId(agentId)
        .userId(chatRequest.userId())
        .sessionId(chatRequest.sessionId())
        .prompt(chatRequest.message())
        .memoryConfig(AgentMemoryConfig.builder()
            .enabled(true)
            .includeContext(true)
            .build())
        .build();
    
    return agentOrchestrator.execute(request);
}
```

### Example 2: Agent with Context Injection

```java
// Before: Empty context
prompt = "What were we talking about?"
response = "I don't know, this is our first message"

// After: With memory integration
prompt = "What were we talking about?"
systemPrompt injected with context:
  "## Conversation History:
   - User: Tell me about machine learning
   - Agent: Machine learning is..."
response = "We were discussing machine learning. The key concepts are..."
```

## Advanced Integration

### Memory-Aware Skill Selection

Use memory to select relevant skills:

```java
private Uni<List<String>> selectRelevantSkills(
        String agentId,
        String userPrompt) {
    
    return vectorMemoryStore.search(
        embeddingService.embedOne(userPrompt).await().indefinitely().toArray(new Float[0]),
        limit=5,
        filters=Map.of("agentId", agentId, "type", "skill-usage")
    ).map(results -> results.stream()
        .map(memory -> extractSkillNameFromMemory(memory))
        .distinct()
        .collect(Collectors.toList()));
}
```

### Memory-Based Agent Profiling

Understand agent behavior from memory:

```java
public Uni<AgentProfile> profileAgent(String agentId) {
    return vectorMemoryStore.searchByFilter(Map.of("agentId", agentId))
        .map(memories -> {
            int totalInteractions = memories.size();
            double averageResponseLength = memories.stream()
                .mapToInt(m -> m.getContent().length())
                .average().orElse(0);
            
            List<String> frequentSkills = extractFrequentSkills(memories);
            
            return new AgentProfile(
                agentId,
                totalInteractions,
                averageResponseLength,
                frequentSkills,
                Instant.now()
            );
        });
}
```

### Session-Based Context Windows

Different context per session:

```java
private Uni<List<MemoryEntry>> getSessionContext(
        String agentId,
        String sessionId,
        int limit) {
    
    return vectorMemoryStore.searchByFilter(Map.of(
        "agentId", agentId,
        "sessionId", sessionId
    )).map(memories -> memories.stream()
        .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
        .limit(limit)
        .collect(Collectors.toList()));
}
```

## Testing Integration

### Unit Test Example

```java
@QuarkusTest
class AgentMemoryIntegrationTest {
    
    @Inject
    AgentOrchestrator orchestrator;
    
    @Inject
    VectorAgentMemory agentMemory;
    
    @Test
    public void testAgentRetrievesContext() {
        // Store initial memory
        agentMemory.store("agent-1", new MemoryEntry(
            "1",
            "Previous discussion about AI",
            Instant.now(),
            Map.of("agentId", "agent-1")
        )).await().indefinitely();
        
        // Execute agent request
        AgentRequest request = AgentRequest.builder()
            .agentId("agent-1")
            .prompt("What were we discussing?")
            .memoryConfig(AgentMemoryConfig.builder()
                .enabled(true)
                .includeContext(true)
                .build())
            .build();
        
        AgentResponse response = orchestrator.execute(request)
            .await().indefinitely();
        
        // Verify context was included in reasoning
        assertTrue(response.content().contains("AI"));
    }
}
```

## Performance Considerations

1. **Context Retrieval Overhead**
   - 10-50ms for metadata filter search (no vector computation)
   - 50-200ms for semantic search with embeddings
   - Use metadata filters for speed, semantic for relevance

2. **Cache Configuration**
   - Enable embedding cache for 90%+ hit rate
   - Configure Redis backend for distributed caching

3. **Memory Limits**
   - Limit context to 10 recent memories (tunable)
   - Use importance scoring to select best memories
   - Implement memory consolidation to manage growth

## Troubleshooting

### Issue: Agent ignoring context

**Cause:** Memory not properly injected into system prompt

**Solution:** Verify context is in system prompt:
```properties
wayang.agent.memory.include-context=true
wayang.agent.memory.context-limit=10
```

### Issue: Slow agent responses

**Cause:** Memory operations taking too long

**Solution:** Use metadata filters instead of semantic search:
```java
// Slow (vector-based)
vectorMemoryStore.search(embedding, limit=10)

// Fast (metadata-based)
vectorMemoryStore.searchByFilter(Map.of("agentId", agentId))
```

### Issue: Memory not persisting

**Cause:** Memory storage not enabled

**Solution:** Enable persistence:
```properties
wayang.agent.memory.persist=true
wayang.memory.redis.host=localhost
wayang.memory.redis.port=6379
```

## Next Steps

1. ✅ Choose integration pattern (simple vs advanced)
2. ✅ Configure memory service in agent orchestrator
3. ✅ Add memory configuration to AgentRequest builder
4. ✅ Implement context injection in system prompt
5. ✅ Store agent interactions post-execution
6. ✅ Test with your agents
7. ✅ Monitor memory performance

## See Also

- `MEMORY_IMPROVEMENTS.md` - Memory module details
- `CONFIGURATION_GUIDE.md` - All configuration options
- Agent SPI documentation
- AgentOrchestrator interface docs

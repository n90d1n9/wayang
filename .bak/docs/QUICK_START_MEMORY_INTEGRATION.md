# Agent + Memory Integration: Quick Start Guide

## 5-Minute Setup

### 1. Add Dependencies

Your `agent-core/pom.xml` should include:

```xml
<dependency>
    <groupId>tech.kayys.wayang</groupId>
    <artifactId>wayang-memory-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2. Inject the Bridge Service

```java
@ApplicationScoped
public class MyAgentService {
    @Inject
    AgentMemoryService memoryService;
    
    @Inject
    AgentOrchestrator orchestrator;
}
```

### 3. Basic Usage Pattern

```java
// Retrieve context → Execute agent → Store interaction
Uni<String> response = memoryService
    .getContextPrompt(agentId, 10)              // Get history
    .map(context -> userPrompt + context)        // Enhance prompt
    .flatMap(enhanced -> executeAgent(enhanced)) // Execute
    .flatMap(result -> memoryService             // Store
        .storeInteraction(agentId, sessionId, userId, 
                         userPrompt, result)
        .map(__ -> result));
```

## Common Use Cases

### Use Case 1: Multi-Turn Chatbot

```java
// Store each message, retrieve context for next turn
for (String userMessage : conversation) {
    String context = memoryService
        .getContextPrompt(agentId)
        .await().indefinitely();
    
    String response = executeAgent(userMessage + context)
        .await().indefinitely();
    
    memoryService.storeInteraction(agentId, sessionId, userId,
                                   userMessage, response)
        .await().indefinitely();
}
```

### Use Case 2: Skill Learning Agent

```java
// Track which skills work best and use that knowledge
String bestSkill = memoryService
    .getSessionMemories(agentId, sessionId, 20)
    .map(memories -> analyzeBestSkill(memories))
    .await().indefinitely();

String response = executeWithSkill(bestSkill, prompt)
    .await().indefinitely();

memoryService.recordSkillUsage(agentId, bestSkill, true, response)
    .await().indefinitely();
```

### Use Case 3: Personalized Agent

```java
// Get user-specific history to personalize responses
String userHistory = memoryService
    .getSessionMemories(agentId, sessionId, 30)
    .map(this::summarizeUserPreferences)
    .await().indefinitely();

String personalizedResponse = executeAgent(
    prompt + "\nUser preferences: " + userHistory)
    .await().indefinitely();
```

## Configuration

### application.properties

```properties
# Memory backend
gamelan.embedding.provider=openai
gamelan.embedding.openai.api-key=${OPENAI_API_KEY}

# Caching (improves performance by 50-90%)
gamelan.embedding.cache.enabled=true
gamelan.embedding.cache.ttl.minutes=60

# Context retrieval limits
wayang.memory.agent.context.limit=10

# Storage (optional - for future persistence layer)
wayang.memory.persistence.enabled=false
wayang.memory.consolidation.enabled=false
```

### Environment Variables

```bash
export OPENAI_API_KEY=sk-...
```

## Testing

### Test Setup

```java
@QuarkusTest
public class AgentMemoryIntegrationTest {
    @Inject
    AgentMemoryService memoryService;
    
    @Test
    public void testContextRetrieval() {
        String context = memoryService
            .getContextPrompt("test-agent")
            .await().indefinitely();
        
        assertNotNull(context);
    }
}
```

### Mock for Unit Tests

```java
// Mock the memory service
AgentMemoryService mockMemory = Mockito.mock(AgentMemoryService.class);
Mockito.when(mockMemory.getContextPrompt("agent-1"))
    .thenReturn(Uni.createFrom().item("Previous context"));

// Use in test
String response = executeWithMemory(mockMemory, "prompt");
verify(mockMemory).storeInteraction(any(), any(), any(), any(), any());
```

## API Reference

### AgentMemoryService Methods

#### Context Retrieval
```java
// Get formatted context for agent reasoning
Uni<String> getContextPrompt(String agentId)
Uni<String> getContextPrompt(String agentId, int limit)

// Get raw memory entries for session
Uni<List<MemoryEntry>> getSessionMemories(String agentId, String sessionId)
Uni<List<MemoryEntry>> getSessionMemories(String agentId, String sessionId, int limit)
```

#### Storage
```java
// Store user-agent interaction
Uni<Void> storeInteraction(String agentId, AgentRequest request, AgentResponse response)
Uni<Void> storeInteraction(String agentId, String sessionId, String userId, 
                          String prompt, String response)

// Record skill execution
Uni<Void> recordSkillUsage(String agentId, String skillId, boolean success, String result)
```

#### Management
```java
// Get statistics
Uni<AgentMemoryStats> getMemoryStats(String agentId)

// Clear all memories for agent
Uni<Void> clearMemory(String agentId)

// Enhance system prompt with context
Uni<String> enhanceSystemPrompt(String agentId, String baseSystemMsg)
```

## Examples

### Example 1: Simple Echo Agent with Memory

```java
@Path("/chat")
@POST
public Uni<String> chat(
    @QueryParam("agentId") String agentId,
    String message) {
    
    return memoryService.getContextPrompt(agentId)
        .flatMap(context -> Uni.createFrom()
            .item(message + " [Previously: " + context + "]"))
        .flatMap(enhanced -> memoryService
            .storeInteraction(agentId, "session-1", "user-1", 
                            message, enhanced)
            .map(__ -> enhanced));
}
```

### Example 2: ReAct Agent with Memory

```java
public Uni<String> executeReActWithMemory(
    String agentId,
    String userQuery) {
    
    return memoryService.getContextPrompt(agentId, 5)
        .map(context -> buildReActPrompt(userQuery, context))
        .flatMap(prompt -> orchestrator.execute(
            AgentRequest.builder()
                .agentId(agentId)
                .prompt(prompt)
                .strategy(OrchestrationStrategy.REACT)
                .build()))
        .flatMap(response -> memoryService
            .storeInteraction(agentId, "session-1", "user-1",
                            userQuery, response.content())
            .map(__ -> response.content()));
}
```

### Example 3: Batch Processing with Memory

```java
public Uni<List<String>> processBatch(
    String agentId,
    List<String> items) {
    
    return Uni.combine()
        .all()
        .unis(items.stream()
            .map(item -> processWithMemory(agentId, item))
            .collect(Collectors.toList()))
        .asList();
}

private Uni<String> processWithMemory(String agentId, String item) {
    return memoryService.getContextPrompt(agentId)
        .flatMap(context -> executeAgent(item, context))
        .flatMap(result -> memoryService
            .storeInteraction(agentId, "batch", null, item, result)
            .map(__ -> result));
}
```

## Troubleshooting

### Problem: "Empty context returned"

**Cause**: No memories stored yet for the agent.

**Solution**: First interaction will be stored, subsequent calls will have context.

```java
// Check if memories exist
AgentMemoryStats stats = memoryService.getMemoryStats(agentId)
    .await().indefinitely();

if (stats.totalMemories() == 0) {
    LOG.info("First interaction - memory will be populated after execution");
}
```

### Problem: "Slow context retrieval"

**Cause**: Retrieving too many memories or cache not enabled.

**Solution**:

```properties
# Enable caching
gamelan.embedding.cache.enabled=true
gamelan.embedding.cache.ttl.minutes=60

# Reduce context size
wayang.memory.agent.context.limit=5
```

### Problem: "OutOfMemory with InMemory vector store"

**Cause**: Too many memories accumulating.

**Solution**:

```java
// Clear old memories periodically
scheduler.scheduleAtFixedRate(
    () -> memoryService.clearMemory(agentId),
    Duration.ofDays(1)  // Clear daily
);
```

## Next Steps

1. **Implement in your agent**: Use `StatefulAgentExecutor` as a reference
2. **Add persistence**: Enable TIER 2 features for database storage
3. **Monitor performance**: Use `getMemoryStats()` to track memory usage
4. **Optimize context**: Experiment with context limits and caching
5. **Scale**: Plan for distributed memory (TIER 3 roadmap)

## Files Reference

| File | Purpose |
|------|---------|
| `AgentMemoryService.java` | Main integration bridge |
| `MemoryEnabledAgentEndpoint.java` | REST API example |
| `StatefulAgentExecutor.java` | Multi-turn conversation example |
| `AGENT_MEMORY_INTEGRATION_GUIDE.md` | Full integration guide |
| `MEMORY_IMPROVEMENTS.md` | Memory module architecture |

## Support

For issues or questions:
1. Check `AGENT_MEMORY_INTEGRATION_GUIDE.md` for detailed patterns
2. Review examples in `agent-core/src/main/java/tech/kayys/gollek/agent/integration/examples/`
3. Check memory module logs: `LOG.debug()` statements in `AgentMemoryService`
4. Enable verbose logging: `quarkus.log.level=DEBUG` in `application.properties`

---

**Version**: 1.0  
**Last Updated**: 2024-03-31  
**Compatibility**: Wayang Platform 0.1.0+, Quarkus 3.32.2+

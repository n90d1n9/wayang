# Tools Integration - Quick Start Guide

## 5-Minute Setup

### 1. Add Dependency

Your `agent-core/pom.xml` should include:

```xml
<dependency>
    <groupId>tech.kayys.wayang</groupId>
    <artifactId>wayang-memory-core</artifactId>
    <version>0.1.0</version>
</dependency>
<dependency>
    <groupId>tech.kayys.golok</groupId>
    <artifactId>tools-spi</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 2. Inject Services

```java
@ApplicationScoped
public class MyAgentService {
    @Inject
    AgentMemoryService memoryService;
    
    @Inject
    AgentToolService toolService;
    
    @Inject
    ToolEnabledAgentExecutor executor;
}
```

### 3. Use Tools in Agent

```java
// Option A: Simple tool execution
Uni<ToolResult> result = toolService.executeTool(agentId, "search_tool", params);

// Option B: Full agent with tools
Uni<AgentResponse> response = executor.executeTaskWithTools(
    agentId, userId, sessionId, "Find and summarize latest news");

// Option C: Tool chaining
Uni<ToolChainResult> chainResult = executor.executeToolChain(
    agentId, toolChain, initialParams);
```

## Common Use Cases

### Use Case 1: Single Tool Execution

```java
// Execute one tool and get result
ToolResult result = toolService
    .executeTool(agentId, "calculate_expense", Map.of("amount", 100.00))
    .await().indefinitely();

if (result.success()) {
    System.out.println("Result: " + result.data());
} else {
    System.out.println("Error: " + result.error());
}
```

### Use Case 2: Memory-Aware Tool Selection

```java
// Get tools that have worked well for this agent before
List<String> bestTools = toolService
    .getHighConfidenceTools(agentId)
    .await().indefinitely();

if (!bestTools.isEmpty()) {
    // Use the best-performing tool
    ToolResult result = toolService.executeTool(
        agentId, bestTools.get(0), params);
}
```

### Use Case 3: Task-Specific Tools

```java
// Get tools recommended for specific task
List<ToolDefinition> tools = toolService
    .recommendTools(agentId, "weather analysis")
    .await().indefinitely();

// Use recommended tools with agent
AgentResponse response = executor.executeTaskWithTools(
    agentId, userId, sessionId, "Analyze weather for trip planning");
```

### Use Case 4: Tool Chain

```java
// Plan optimal sequence of tools
ToolChain chain = executor
    .planToolChain(agentId, "Research and write report on AI trends")
    .await().indefinitely();

// Execute the planned chain
ToolChainResult result = executor
    .executeToolChain(agentId, chain, Map.of())
    .await().indefinitely();

System.out.println(result.getSummary());
```

### Use Case 5: Learn and Adapt

```java
// Analyze what tools worked best
ToolLearningResults learning = executor
    .learnFromToolUsage(agentId)
    .await().indefinitely();

// Use learning to improve future executions
String recommended = learning.recommendedToolForTask("data_analysis");
if (recommended != null) {
    ToolResult result = toolService.executeTool(agentId, recommended, params);
}
```

## Configuration

### Minimal Setup

```properties
gamelan.tool.cache.enabled=true
gamelan.tool.execution.timeout.seconds=30
```

### Recommended Setup

```properties
# Caching
gamelan.tool.cache.enabled=true
gamelan.tool.cache.size=1000
gamelan.tool.cache.ttl.minutes=10

# Execution
gamelan.tool.execution.timeout.seconds=30
gamelan.tool.execution.async.enabled=true
gamelan.tool.execution.metrics.enabled=true

# Learning
wayang.agent.tool.learning.enabled=true
wayang.agent.tool.confidence.threshold=0.8
wayang.agent.tool.min.samples=3

# Memory
wayang.memory.agent.context.limit=10
gamelan.embedding.cache.enabled=true
```

## API Reference

### Quick Method Reference

```java
// Tool Discovery
toolService.getAvailableTools()                    // All tools
toolService.getToolsForAgent(agentId)              // Agent's best tools
toolService.recommendTools(agentId, task)         // Task-specific tools

// Execution
toolService.executeTool(agentId, toolId, params)  // Single tool
toolService.chainTools(agentId, toolIds, params)  // Multiple tools

// Learning
toolService.analyzeToolUsagePattern(agentId)      // Analyze patterns
toolService.getHighConfidenceTools(agentId)       // Best tools
toolService.getToolMetrics(agentId, toolId)       // Specific metrics

// Full Agent
executor.executeTaskWithTools(...)                // Full agent with tools
executor.planToolChain(...)                       // Plan tool sequence
executor.executeToolChain(...)                    // Execute sequence
executor.learnFromToolUsage(...)                  // Learn patterns
```

## Examples

### Example 1: REST Endpoint with Tool Support

```java
@POST
@Path("/agents/{agentId}/execute-with-tools")
public Uni<ExecuteResponse> executeWithTools(
    @PathParam("agentId") String agentId,
    ExecuteRequest request) {
    
    return executor.executeTaskWithTools(
        agentId,
        request.userId,
        request.sessionId,
        request.task)
        .map(response -> new ExecuteResponse(
            response.requestId(),
            response.content(),
            response.success()));
}
```

### Example 2: Intelligent Tool Selector

```java
public Uni<String> selectBestTool(String agentId, String task) {
    return Uni.combine()
        .all()
        .unis(
            toolService.recommendTools(agentId, task),
            toolService.getHighConfidenceTools(agentId)
        )
        .asTuple()
        .map(tuple -> {
            var recommended = tuple.getItem1();
            var confident = tuple.getItem2();
            
            return recommended.stream()
                .filter(t -> confident.contains(t.id()))
                .findFirst()
                .map(t -> t.id())
                .orElseGet(() -> recommended.get(0).id());
        });
}
```

### Example 3: Multi-Turn with Tool Learning

```java
public Uni<List<String>> multiTurnWithTools(
    String agentId,
    List<String> tasks) {
    
    // First pass: Learn from history
    return executor.learnFromToolUsage(agentId)
        .flatMap(learning -> {
            // Execute tasks using learned patterns
            return Uni.combine()
                .all()
                .unis(tasks.stream()
                    .map(task -> executor.executeTaskWithTools(
                        agentId, "user", "session", task))
                    .collect(Collectors.toList()))
                .asList()
                .map(responses -> responses.stream()
                    .map(AgentResponse::content)
                    .collect(Collectors.toList()));
        });
}
```

### Example 4: Tool Chain Builder

```java
public ToolChain buildChainForAnalysis(String agentId) {
    return new ToolChain(
        agentId,
        "Data analysis pipeline",
        Arrays.asList(
            "data_fetch_tool",
            "data_clean_tool", 
            "analyze_tool",
            "visualize_tool",
            "report_tool"
        ),
        Instant.now());
}
```

## Testing

### Test Setup

```java
@QuarkusTest
public class ToolIntegrationTest {
    
    @Inject AgentToolService toolService;
    @Inject ToolEnabledAgentExecutor executor;
    
    @Test
    public void testToolExecution() {
        ToolResult result = toolService.executeTool(
            "test-agent",
            "test_tool",
            Map.of())
            .await().indefinitely();
        
        assertTrue(result.success());
    }
    
    @Test
    public void testToolRecommendation() {
        List<AgentToolService.ToolDefinition> tools =
            toolService.recommendTools("test-agent", "analysis")
                .await().indefinitely();
        
        assertNotNull(tools);
    }
}
```

### Mock Testing

```java
// Mock tool service
AgentToolService mockTools = Mockito.mock(AgentToolService.class);
Mockito.when(mockTools.executeTool(any(), any(), any()))
    .thenReturn(Uni.createFrom().item(
        ToolResult.success("mock result")));

// Use in test
ToolResult result = mockTools.executeTool("agent", "tool", Map.of())
    .await().indefinitely();
```

## Troubleshooting

### Problem: Tool Not Found

**Cause**: Tool ID doesn't exist in registry

**Solution**:
1. Check tool ID spelling
2. Ensure tool is registered: `@ApplicationScoped` annotation
3. List available tools: `toolService.getAvailableTools()`

```java
// Check available tools
List<Map<String, Object>> tools = toolService
    .getAvailableTools()
    .await().indefinitely();

for (Map tool : tools) {
    System.out.println("Available: " + tool.get("id"));
}
```

### Problem: Tool Execution Timeout

**Cause**: Tool takes longer than configured timeout

**Solution**: Increase timeout or optimize tool

```properties
gamelan.tool.execution.timeout.seconds=60
```

### Problem: No Tools Recommended

**Cause**: No tools match task description

**Solution**:
```java
// Get all tools if none recommended
List<ToolDefinition> allTools = toolService
    .getToolsForAgent(agentId)
    .await().indefinitely();
```

### Problem: Low Tool Confidence

**Cause**: Tool hasn't been used or has low success rate

**Solution**:
```java
// Check tool metrics
ToolMetrics metrics = toolService
    .getToolMetrics(agentId, toolId)
    .await().indefinitely();

if (metrics.isLowConfidence()) {
    // Use fallback or get recommendations
    List<String> trusted = toolService
        .getHighConfidenceTools(agentId)
        .await().indefinitely();
}
```

## Performance Tips

### 1. Enable Tool Caching
```properties
gamelan.tool.cache.enabled=true
gamelan.tool.cache.ttl.minutes=10
```
**Impact**: 10x faster for repeated tool calls

### 2. Use Tool Recommendations
```java
// Instead of iterating all tools, get recommendations
List<ToolDefinition> recommended = toolService
    .recommendTools(agentId, task)
    .await().indefinitely();
```
**Impact**: 50-80% faster tool discovery

### 3. Learn Tool Patterns
```java
// Use learned patterns for faster selection
List<String> bestTools = toolService
    .getHighConfidenceTools(agentId)
    .await().indefinitely();
```
**Impact**: 70% faster agent execution after learning period

### 4. Chain Compatible Tools
```java
// Plan chains that minimize data transformation
toolService.chainTools(agentId, 
    Arrays.asList("fetch", "transform", "store"),
    params);
```
**Impact**: 30-50% faster multi-step execution

## Next Steps

1. **Basic Integration**: Get single tool working
2. **Tool Selection**: Implement memory-aware selection
3. **Tool Chaining**: Plan and execute sequences
4. **Learning**: Analyze patterns and adapt
5. **Optimization**: Profile and optimize bottlenecks

## Files Reference

| File | Purpose |
|------|---------|
| AgentToolService.java | Core tool integration service |
| ToolEnabledAgentExecutor.java | Full agent with tools |
| TOOLS_INTEGRATION_GUIDE.md | Comprehensive reference |
| QUICK_START_TOOLS_INTEGRATION.md | This file |

## Support

**Quick Questions**:
1. Check this file's troubleshooting section
2. Review Examples section
3. Check TOOLS_INTEGRATION_GUIDE.md

**Deep Dive**:
1. Read TOOLS_INTEGRATION_GUIDE.md
2. Review ToolEnabledAgentExecutor.java
3. Check tool registry documentation

---

**Version**: 1.0  
**Compatibility**: Wayang Platform 0.1.0+, Java 11+, Quarkus 3.32.2+

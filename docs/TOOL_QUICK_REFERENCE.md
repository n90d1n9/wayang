# ToolCacheManager & IntelligentToolSelector - Quick Reference

## ToolCacheManager - Quick Start

### Inject
```java
@Inject
ToolCacheManager cacheManager;
```

### Basic Usage
```java
// Execute with caching
SkillResult result = cacheManager.executeWithCache(
    "read_file",           // tool ID
    params,                // Map<String, Object>
    context,               // Map<String, Object>
    () -> executeTool()    // ToolExecutor
).await().atMost(Duration.ofSeconds(10));
```

### Cache Control
```java
// Invalidate
cacheManager.invalidate("read_file");
cacheManager.invalidate("read_file", params);
cacheManager.clearAll();

// Warm
cacheManager.warmCache("read_file", params, result);

// Preload
cacheManager.preloadCache("read_file", paramSets, executor);
```

### Statistics
```java
CacheStatistics stats = cacheManager.getStatistics();
System.out.println("Hit ratio: " + stats.hitRatio());

ToolExecutionStats toolStats = cacheManager.getToolStats("read_file");
System.out.println("Avg time: " + toolStats.avgExecutionTimeMs() + "ms");
```

### Configuration
```yaml
gollek:
  agent:
    tools:
      cache:
        max-size: 1000
        default-ttl: 10m
        tool-configs:
          read_file:
            ttl: 30m
            max-size: 500
```

---

## IntelligentToolSelector - Quick Start

### Inject
```java
@Inject
IntelligentToolSelector toolSelector;
```

### Build Context
```java
ProjectContext context = ProjectContext.builder()
    .framework("react")
    .framework("typescript")
    .feature("tailwind", true)
    .feature("redux", true)
    .buildTool("npm")
    .build();
```

### Select Tools
```java
// AI-powered selection
ToolChain chain = toolSelector.selectTools(context, "Create REST API")
    .await().atMost(Duration.ofSeconds(10));

// Framework-based (faster)
ToolChain chain = toolSelector.selectToolsByFramework(context, "Create component");
```

### Get Recommendations
```java
List<ToolRecommendation> recs = toolSelector.recommendTools(context, task)
    .await().atMost(Duration.ofSeconds(10));

recs.forEach(r -> 
    System.out.printf("%s: %.2f - %s%n", r.toolId(), r.confidence(), r.reason())
);
```

### Execute Chain
```java
ToolChainResult result = toolSelector.executeToolChain(chain, context)
    .await().atMost(Duration.ofSeconds(30));

System.out.println("Success: " + result.getSuccessCount());
System.out.println("Failures: " + result.getFailureCount());
```

### Monitor Performance
```java
ToolPerformanceMetrics metrics = toolSelector.getToolMetrics("react_component");
System.out.println("Success rate: " + metrics.getSuccessRate());
System.out.println("Avg duration: " + metrics.avgDurationMs() + "ms");
```

### Clear Cache
```java
toolSelector.clearCache();
```

---

## Common Patterns

### Pattern 1: Cache-First Execution
```java
public Uni<SkillResult> execute(String toolId, Map<String, Object> params) {
    return cacheManager.executeWithCache(
        toolId, params, context,
        () -> skillRegistry.find(toolId)
            .map(skill -> skill.execute(context))
            .await().indefinitely()
    );
}
```

### Pattern 2: Select + Execute + Cache
```java
public Uni<ToolChainResult> workflow(String task, ProjectContext context) {
    return toolSelector.selectTools(context, task)
        .flatMap(chain -> {
            List<Uni<?>> executions = chain.getTools().stream()
                .map(tool -> 
                    cacheManager.executeWithCache(
                        tool.toolId(), tool.parameters(), context,
                        () -> executeTool(tool)
                    )
                )
                .collect(Collectors.toList());
            
            return Uni.combine().all().unis(executions)
                .combinedWith(results -> new ToolChainResult(results, false));
        });
}
```

### Pattern 3: Fallback Strategy
```java
public Uni<ToolChain> selectWithFallback(String task, ProjectContext context) {
    return toolSelector.selectTools(context, task)
        .onFailure().recoverWithUni(
            () -> Uni.createFrom().item(
                toolSelector.selectToolsByFramework(context, task)
            )
        );
}
```

### Pattern 4: Monitor & Alert
```java
@Scheduled(every = "1m")
void monitorPerformance() {
    toolSelector.getAllToolMetrics().forEach((id, metrics) -> {
        if (metrics.getSuccessRate() < 0.8) {
            LOG.warnf("Low success rate: %s - %.2f", id, metrics.getSuccessRate());
        }
    });
    
    CacheStatistics stats = cacheManager.getStatistics();
    if (stats.hitRatio() < 0.3) {
        LOG.warnf("Low cache hit ratio: %.2f", stats.hitRatio());
    }
}
```

---

## Default TTL Values

| Tool Type | TTL |
|-----------|-----|
| read_file | 30m |
| grep_search | 15m |
| write_file | 5m |
| execute_command | 2m |
| http_call | 10m |
| inference | 5m |

---

## Framework Mappings

### Frontend
- **React**: `react_component`, `react_state`, `react_hook`
- **Vue**: `vue_component`, `vue_composable`, `pinia_store`
- **Angular**: `angular_component`, `angular_service`
- **Svelte**: `svelte_component`, `svelte_store`

### Backend
- **Spring**: `spring_controller`, `spring_service`, `maven_dependency`
- **Quarkus**: `quarkus_resource`, `quarkus_service`
- **FastAPI**: `python_function`, `pydantic_model`
- **Express**: `express_route`, `express_middleware`

### CSS
- **Tailwind**: `tailwind_class`, `tailwind_component`
- **Bootstrap**: `bootstrap_component`, `bootstrap_class`
- **Material**: `material_component`, `material_theme`

### State
- **Redux**: `redux_slice`, `redux_selector`
- **Zustand**: `zustand_store`, `zustand_hook`
- **MobX**: `mobx_store`, `mobx_action`

---

## Error Handling

### Timeout
```java
try {
    ToolChain chain = toolSelector.selectTools(context, task)
        .await().atMost(Duration.ofSeconds(10));
} catch (TimeoutException e) {
    // Fallback to framework-based
    chain = toolSelector.selectToolsByFramework(context, task);
}
```

### Empty Chain
```java
ToolChain chain = toolSelector.selectTools(context, task)
    .await().atMost(Duration.ofSeconds(10));

if (chain.isEmpty()) {
    // Handle empty selection
    chain = createManualChain(task);
}
```

### Cache Miss
```java
SkillResult result = cacheManager.executeWithCache(
    toolId, params, context, executor,
    true // bypass cache for fresh data
);
```

---

## Performance Tips

### 1. Warm Common Operations
```java
@PostConstruct
void warmCache() {
    List<String> commonFiles = List.of("pom.xml", "package.json");
    commonFiles.forEach(file -> 
        cacheManager.warmCache("read_file", 
            Map.of("path", file), 
            precomputedResult)
    );
}
```

### 2. Use Framework Selection for Simple Tasks
```java
if (taskComplexity < THRESHOLD) {
    chain = toolSelector.selectToolsByFramework(context, task);
} else {
    chain = toolSelector.selectTools(context, task);
}
```

### 3. Monitor Hit Rates
```java
CacheStatistics stats = cacheManager.getStatistics();
if (stats.hitRatio() < 0.5) {
    // Increase TTL or warm cache
}
```

### 4. Batch Preloading
```java
List<Map<String, Object>> commonParams = getCommonParams();
cacheManager.preloadCache("read_file", commonParams, executor)
    .await().indefinitely();
```

---

## Troubleshooting

### Low Cache Hit Rate
```yaml
# Increase TTL
gollek:
  agent:
    tools:
      cache:
        default-ttl: 30m  # was 10m
```

### High Memory Usage
```yaml
# Reduce cache size
gollek:
  agent:
    tools:
      cache:
        max-size: 500  # was 1000
```

### Poor Tool Selection
```java
// Provide richer context
ProjectContext context = ProjectContext.builder()
    .framework("react")
    .framework("typescript")
    .feature("tailwind", true)
    .feature("redux", true)
    .feature("jest", true)
    .buildTool("npm")
    .testFramework("jest")
    .build();
```

### Slow Selection
```java
// Enable caching
// Use framework-based for simple tasks
if (isSimpleTask(task)) {
    chain = toolSelector.selectToolsByFramework(context, task);
}
```

---

## Complete Example

```java
@ApplicationScoped
public class CodeGenerationService {

    @Inject
    IntelligentToolSelector toolSelector;

    @Inject
    ToolCacheManager cacheManager;

    @Inject
    SkillRegistry skillRegistry;

    public String generateComponent(String description, String framework) {
        // Build context
        ProjectContext context = ProjectContext.builder()
            .framework(framework)
            .framework("typescript")
            .feature("tailwind", true)
            .build();

        // Select tools
        ToolChain chain = toolSelector.selectTools(context, description)
            .await().atMost(Duration.ofSeconds(10));

        // Execute with caching
        List<ToolExecutionResult> results = chain.getTools().stream()
            .map(tool -> 
                cacheManager.executeWithCache(
                    tool.toolId(),
                    tool.parameters(),
                    Map.of("framework", framework),
                    () -> executeTool(tool, context)
                ).await().atMost(Duration.ofSeconds(10))
            )
            .map(r -> new ToolExecutionResult(
                r.skillId(), true, r.durationMs(), r.observation(), null
            ))
            .collect(Collectors.toList());

        // Aggregate results
        return results.stream()
            .map(ToolExecutionResult::result)
            .collect(Collectors.joining("\n\n"));
    }

    private SkillResult executeTool(SelectedTool tool, ProjectContext context) {
        return skillRegistry.find(tool.toolId())
            .map(skill -> skill.execute(new SkillContext(tool.parameters())))
            .orElse(SkillResult.failure(tool.toolId(), 
                new IllegalStateException("Tool not found: " + tool.toolId())));
    }
}
```

---

## More Info

- **Full Guide**: `TOOL_CACHE_SELECTOR_GUIDE.md`
- **Implementation Summary**: `TOOL_IMPROVEMENTS_SUMMARY.md`
- **Agent Improvements**: `AGENT_IMPROVEMENTS.md`
- **Quick Start**: `QUICKSTART.md`

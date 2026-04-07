# Phase 2b Integration: Gamelan Workflows + Graph Module

## Overview

Integrating the Graph module with Gamelan Workflows creates a **comprehensive workflow knowledge system** that captures, visualizes, and optimizes workflow execution through relationship-based reasoning.

## Integration Architecture

```
Agent
  ↓
AgentGamelanService (Phase 2a)
  ├─ Invokes Workflow
  └─ Stores execution in WorkflowGraph (NEW)
      ├─ Workflow Structure (DAG)
      │   ├─ WorkflowStep nodes
      │   ├─ Dependencies (FOLLOWS, DEPENDS_ON)
      │   └─ Data flow (FEEDS_INTO)
      ├─ Execution History
      │   ├─ Execution run nodes
      │   ├─ Step execution nodes
      │   └─ Temporal relationships (EXECUTED_AT)
      ├─ Results Storage
      │   ├─ Output nodes
      │   └─ State transitions (PRODUCES, TRANSFORMS)
      └─ Optimization Paths
          ├─ Parallel execution candidates
          ├─ Circular dependency detection
          └─ Critical path analysis

Graph Operations Enable:
✅ Workflow DAG visualization
✅ Multi-hop reasoning (find optimal paths)
✅ Dependency detection (circular, cascading)
✅ Execution pattern learning
✅ Agent decision enhancement (use graph for context)
```

## Key Components to Implement

### 1. WorkflowGraphService (NEW - 300+ lines)

**Purpose**: Bridge between AgentGamelanService and GraphStore

```java
@ApplicationScoped
public class WorkflowGraphService {
    
    @Inject GraphStore graphStore;
    @Inject AgentGamelanService gamelanService;
    
    // Workflow Structure Operations
    public String storeWorkflowDefinition(WorkflowDefinition workflow)
    public void updateWorkflowStructure(String workflowId, WorkflowDefinition workflow)
    public Optional<WorkflowGraph> getWorkflowStructure(String workflowId)
    
    // Execution Tracking
    public void recordExecutionRun(String agentId, RunResponse run)
    public void recordStepExecution(String runId, StepExecution stepExec)
    public void recordStepOutput(String stepExecutionId, Object output)
    
    // Analysis & Optimization
    public List<List<WorkflowStep>> findOptimalExecutionPaths(String workflowId)
    public List<List<WorkflowStep>> detectCircularDependencies(String workflowId)
    public List<WorkflowStep> findCriticalPath(String workflowId)
    public boolean canExecuteInParallel(List<String> stepIds)
    
    // Learning
    public WorkflowMetrics calculateWorkflowMetrics(String workflowId)
    public List<ExecutionPattern> findCommonPatterns(String workflowId)
    public void suggestOptimizations(String workflowId)
}
```

### 2. Node Types in Graph

**WorkflowDefinition Nodes:**
```
Node(label: "Workflow", properties: {
    workflowId: "data-processing",
    name: "Data Processing Pipeline",
    version: "1.0",
    description: "Process and transform raw data",
    owner: "data-team",
    createdAt: timestamp,
    updatetAt: timestamp
})
```

**WorkflowStep Nodes:**
```
Node(label: "WorkflowStep", properties: {
    stepId: "fetch-data",
    workflowId: "data-processing",
    name: "Fetch Data from Source",
    type: "DataSourceStep",
    timeout: 300000,
    retryPolicy: "exponential-backoff",
    order: 1
})
```

**ExecutionRun Nodes:**
```
Node(label: "ExecutionRun", properties: {
    runId: "run-2026-04-02-001",
    workflowId: "data-processing",
    agentId: "agent-001",
    status: "RUNNING",
    startTime: timestamp,
    endTime: null,
    durationMs: null
})
```

**StepExecution Nodes:**
```
Node(label: "StepExecution", properties: {
    stepExecutionId: "step-exec-001",
    runId: "run-2026-04-02-001",
    stepId: "fetch-data",
    status: "COMPLETED",
    startTime: timestamp,
    endTime: timestamp,
    durationMs: 1500,
    outputSize: "2.5MB"
})
```

**OutputData Nodes:**
```
Node(label: "OutputData", properties: {
    dataId: "output-fetch-data-001",
    stepExecutionId: "step-exec-001",
    dataType: "JSON",
    size: 2500000,
    schema: "...",
    hash: "abc123..."
})
```

### 3. Relationship Types

**Structure Relationships:**
- `WORKFLOW_STEP`: Workflow → WorkflowStep (composition)
- `FOLLOWS`: StepA → StepB (execution order)
- `DEPENDS_ON`: StepA → StepB (data dependency)
- `FEEDS_INTO`: StepA → StepB (data flow)
- `HAS_ALTERNATIVE`: StepA → StepB (conditional paths)

**Execution Relationships:**
- `EXECUTED_RUN`: Workflow → ExecutionRun
- `EXECUTES_STEP`: ExecutionRun → StepExecution
- `PRODUCES`: StepExecution → OutputData
- `USES_INPUT`: StepExecution → OutputData (from previous step)
- `TRANSFORMS_TO`: OutputData → OutputData (transformation chain)
- `FAILED_AT`: ExecutionRun → StepExecution (failure tracking)

**Optimization Relationships:**
- `PARALLEL_CANDIDATE`: Step A ⇄ Step B (can execute in parallel)
- `CIRCULAR_DEPENDENCY`: Step A ⇄ Step B (cycle detection)
- `CRITICAL_PATH`: Step A → Step B (performance critical)

### 4. Query Examples

**Find workflow execution history:**
```cypher
MATCH (w:Workflow {workflowId: "data-processing"})
      -[:EXECUTED_RUN]->(run:ExecutionRun)
      -[:EXECUTES_STEP]->(stepExec:StepExecution)
RETURN run, stepExec
ORDER BY run.startTime DESC
LIMIT 10
```

**Detect circular dependencies:**
```cypher
MATCH p = (s:WorkflowStep)-[:DEPENDS_ON*]->(:WorkflowStep{stepId: $stepId})
WHERE s.stepId = $stepId
RETURN p
```

**Find critical path (longest execution time):**
```cypher
MATCH path = (run:ExecutionRun)-[:EXECUTES_STEP*]->(stepExec:StepExecution)
WHERE run.runId = $runId
RETURN path
ORDER BY reduce(time = 0, s IN nodes(path) | time + s.durationMs) DESC
LIMIT 1
```

**Analyze execution patterns:**
```cypher
MATCH (run:ExecutionRun)-[:EXECUTES_STEP]->(stepExec:StepExecution)
WHERE run.workflowId = $workflowId
RETURN stepExec.stepId, 
       COUNT(*) as executionCount,
       AVG(stepExec.durationMs) as avgDuration,
       MIN(stepExec.durationMs) as minDuration,
       MAX(stepExec.durationMs) as maxDuration
ORDER BY executionCount DESC
```

## Integration with Phase 2a (AgentGamelanService)

**Enhanced Workflow Creation:**
```java
// Phase 2a: AgentGamelanService
Uni<RunResponse> response = gamelanService.createAndStartWorkflow(
    "agent-001",
    "data-processing",
    Map.of("source", "database")
);

// NEW (Phase 2b): WorkflowGraphService automatically:
// 1. Records workflow structure in graph (if not already stored)
// 2. Creates ExecutionRun node
// 3. Monitors step execution and records in graph
// 4. Stores outputs in graph
// 5. Creates optimization suggestions
```

**AutoWiring for Automatic Recording:**
```java
@ApplicationScoped
class GamelanExecutionListener {
    @Inject WorkflowGraphService graphService;
    
    // Listen to RunResponse events and record in graph
    @Observes RunResponseEvent
    void onRunResponse(RunResponseEvent event) {
        graphService.recordExecutionRun(event.getAgentId(), event.getRunResponse());
    }
    
    @Observes StepCompletionEvent
    void onStepCompletion(StepCompletionEvent event) {
        graphService.recordStepExecution(event.getRunId(), event.getStepExecution());
    }
}
```

## Data Models (NEW)

### WorkflowGraph POJO
```java
public class WorkflowGraph {
    private String workflowId;
    private List<WorkflowStepNode> steps;
    private List<DependencyEdge> dependencies;
    private List<DataFlowEdge> dataFlows;
    private GraphStatistics statistics;
}
```

### WorkflowStepNode
```java
public class WorkflowStepNode {
    private String stepId;
    private String name;
    private String stepType;
    private int order;
    private long timeoutMs;
    private Map<String, Object> properties;
}
```

### ExecutionPath
```java
public class ExecutionPath {
    private List<WorkflowStep> steps;
    private long estimatedDurationMs;
    private List<String> parallelGroups;
    private boolean isCriticalPath;
}
```

## Benefits of This Integration

1. **Workflow Visualization**: Generate DAG diagrams from graph nodes
2. **Dependency Analysis**: Detect circular dependencies, cascading failures
3. **Performance Optimization**: 
   - Find critical path for bottleneck analysis
   - Identify parallelizable steps
   - Suggest execution order changes
4. **Learning & Adaptation**:
   - Track execution patterns over time
   - Identify common failure points
   - Recommend workflow improvements
5. **Debugging & Auditing**:
   - Complete execution history with relationships
   - Trace data transformations
   - Understand step interactions
6. **Multi-Hop Reasoning**:
   - Find workflows similar to current one
   - Recommend execution strategies based on patterns
   - Connect agents to successful workflows

## Implementation Phases

### Phase 2b (This): Foundation
- [ ] WorkflowGraphService (core operations)
- [ ] Graph node/relationship definitions
- [ ] Integration with AgentGamelanService
- [ ] Basic recording of workflow structure & executions

### Phase 2c: Optimization
- [ ] Dependency analysis (circular, cascading)
- [ ] Critical path detection
- [ ] Parallel execution planning
- [ ] Performance metrics calculation

### Phase 2d: Learning
- [ ] Pattern detection (common execution paths)
- [ ] Anomaly detection (unusual patterns)
- [ ] Recommendations engine
- [ ] Optimization suggestions

### Phase 2e: Integration with Tools
- [ ] Gamelan tools for graph queries
- [ ] Agent tools for workflow analysis
- [ ] Memory integration for workflow-based learning

## Configuration

```properties
# Graph Storage Configuration
wayang.graph.store.type=neo4j  # or inmemory for testing

# Neo4j Configuration
wayang.graph.store.neo4j.uri=bolt://localhost:7687
wayang.graph.store.neo4j.username=neo4j
wayang.graph.store.neo4j.password=password

# Workflow Graph Configuration
wayang.workflow.graph.auto-record=true
wayang.workflow.graph.capture-outputs=true
wayang.workflow.graph.optimization-level=advanced
```

## Success Criteria

✅ Workflow definitions stored in graph  
✅ Execution runs and steps recorded automatically  
✅ Dependency detection working  
✅ Critical path analysis functional  
✅ Cypher queries for workflow analysis  
✅ No breaking changes to Phase 2a  
✅ Full integration test coverage  

## Next Steps

1. Create WorkflowGraphService (300+ lines)
2. Define graph node/relationship types
3. Implement execution recording
4. Add to AgentGamelanService
5. Create Cypher query library
6. Integration tests with graph module
7. Documentation and examples

---

**Total Lines**: ~500+ lines (service + models + tests)  
**Time Estimate**: 3-4 hours  
**Dependencies**: GraphStore (existing), AgentGamelanService (existing)  
**Breaking Changes**: 0

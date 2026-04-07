# Gollek Agent Module - Agentic AI Improvements

## Overview

This document describes the comprehensive improvements made to the `gollek-extension/agent` module for enhanced agentic AI capabilities based on the Gollek inference engine.

## Architecture Summary

The improved agent module provides:

1. **Unified Gollek Integration** - Seamless integration with Gollek inference engine
2. **Provider-Aware Inference** - Intelligent routing between local (GGUF, ONNX) and cloud (OpenAI, Anthropic) providers
3. **Advanced Orchestration** - Multiple reasoning strategies with dynamic selection
4. **Native Tool Calling** - Direct integration with Gollek's ToolDefinition SPI
5. **Multi-Agent Collaboration** - Coordinated multi-agent workflows
6. **Enterprise Features** - Security, observability, checkpointing, and governance

## New Components

### 1. Core Integration Classes

#### GollekAgentClient
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/client/GollekAgentClient.java`

Unified client for Gollek inference integration with:
- Provider-aware inference with automatic fallback
- Native tool calling support
- Streaming support for real-time token generation
- Built-in circuit breaker pattern
- Retry mechanisms

**Usage Example:**
```java
@Inject
GollekAgentClient agentClient;

// Simple inference
InferenceResponse response = agentClient.infer(request)
    .await().atMost(Duration.ofSeconds(30));

// Tool calling
InferenceResponse response = agentClient.inferWithTools(request, tools)
    .await().atMost(Duration.ofSeconds(30));

// Streaming
Multi<StreamingInferenceChunk> stream = agentClient.stream(request);
```

#### ProviderAwareInference
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/client/ProviderAwareInference.java`

Intelligent provider routing with:
- Task complexity analysis
- Latency and cost optimization
- Provider health monitoring
- Automatic fallback on failures
- Performance tracking with exponential moving averages

**Provider Selection Logic:**
1. Preferred provider (if set and healthy)
2. Local providers (GGUF, ONNX) for cost efficiency
3. Cloud providers (OpenAI, Anthropic) for complex tasks
4. Fallback chain based on health and performance

#### AgentProviderRegistry
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/registry/AgentProviderRegistry.java`

Provider management with:
- Provider discovery and registration
- Capability-based provider selection
- Tenant-specific provider configurations
- Health monitoring
- Dynamic enablement/disablement

### 2. Enhanced Orchestration

#### ToolCallingAgentLoop
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/orchestrator/ToolCallingAgentLoop.java`

Enhanced agent loop with native Gollek tool calling:
- Native tool calling from inference engine
- Parallel tool execution with dependency resolution
- Tool result caching and memoization
- Tool composition and chaining
- Detailed metrics and observability

**Features:**
- Supports up to 5 parallel tool calls per iteration
- 10-minute tool result cache with automatic cleanup
- Automatic dependency analysis for parallel execution
- Comprehensive error handling and recovery

#### MultiAgentCoordinator
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/coordinator/MultiAgentCoordinator.java`

Multi-agent collaboration with:
- Task decomposition and distribution
- Agent role management (Planner, Executor, Reviewer, Synthesizer)
- Inter-agent communication
- Result aggregation and synthesis
- Consensus building

**Coordination Patterns:**
- **Sequential:** Agents work in sequence
- **Parallel:** Multiple agents on independent subtasks
- **Hierarchical:** Manager delegates to workers
- **Consensus:** Multiple agents vote on solutions

#### AdaptiveStrategySelector
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/selector/AdaptiveStrategySelector.java`

Dynamic strategy selection based on:
- Task complexity analysis
- Required capabilities
- Performance requirements
- Historical success rates

**Strategy Selection Heuristics:**
| Task Type | Selected Strategy |
|-----------|------------------|
| Simple Q&A | Direct inference |
| Tool-required | ReAct / Tool-Calling |
| Complex reasoning | Plan-and-Execute |
| Quality-critical | Reflexion |
| Multi-domain | Multi-Agent |

### 3. Service Layer

#### AgenticInferenceService
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/service/AgenticInferenceService.java`

Specialized inference service for agents:
- Reasoning-optimized default parameters
- Built-in system prompt templates
- Tool calling with automatic schema generation
- Context management and compression
- Multi-turn conversation support

**System Prompt Templates:**
- ReAct format
- Plan-and-Execute
- Reflexion

### 4. Observability

#### AgentMetricsCollector
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/metrics/AgentMetricsCollector.java`

Metrics collection with Micrometer integration:
- Execution counts and success rates
- Per-step latency and token usage
- Tool execution metrics
- Provider performance
- Tenant-specific metrics

**Exported Metrics:**
- `agent.executions.total` - Counter
- `agent.executions.duration` - Timer
- `agent.steps.total` - Counter
- `agent.steps.latency` - Timer
- `agent.tokens.used` - Counter
- `agent.tools.executed` - Counter
- `agent.provider.latency` - Timer

### 5. State Management

#### AgentCheckpointManager
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/checkpoint/AgentCheckpointManager.java`

State persistence and recovery:
- Save execution state at any point
- Resume from checkpoints after failures
- Multiple checkpoint versions
- Filesystem and database storage
- Automatic cleanup with TTL

**Use Cases:**
- Long-running workflows
- Human-in-the-loop scenarios
- Failure recovery
- Debugging and auditing

### 6. Security

#### AgentSecurityEnforcer
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/security/AgentSecurityEnforcer.java`

Security and governance:
- Tenant-scoped resource quotas
- Input/output guardrails
- Policy enforcement for tool usage
- Secure credential management
- Audit logging

**Security Features:**
- Input validation (prompt injection detection)
- Output filtering (PII redaction)
- Tool authorization
- Rate limiting
- Quota enforcement

### 7. Updated SPI

#### AgentOrchestrator (v2.0)
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/spi/AgentOrchestrator.java`

Enhanced orchestrator interface with:
- Tool calling support (`supportsToolCalling()`, `onToolCall()`)
- Streaming support (`supportsStreaming()`)
- Checkpoint/resume (`supportsCheckpoint()`, `saveState()`, `restoreState()`)
- Multi-agent coordination (`supportsMultiAgent()`)
- Provider preferences (`getPreferredProvider()`)
- Lifecycle hooks (`onStart()`, `onComplete()`, `onError()`)

### 8. Configuration

#### AgentConfig (v2.0)
**Location:** `/gollek-extension/agent/agent-core/src/main/java/tech/kayys/gollek/agent/core/AgentConfig.java`

Comprehensive configuration with new sections:

```yaml
gollek:
  agent:
    # Provider Configuration
    provider:
      preferred: openai
      fallback-enabled: true
      local-preferred: false
      enabled-providers: [openai, gguf, onnx]
      configs:
        openai:
          api-key: ${OPENAI_API_KEY}
          timeout-seconds: 30
          max-retries: 3
    
    # Inference Configuration
    inference:
      default-temperature: 0.7
      default-max-tokens: 2048
      timeout: 60s
      streaming-enabled: true
      tool-calling-enabled: true
      context-window: 8192
      context-compression-enabled: true
    
    # Security Configuration
    security:
      enabled: true
      input-validation-enabled: true
      output-filtering-enabled: true
      prompt-injection-detection-enabled: true
      pii-redaction-enabled: true
      max-input-length: 10000
    
    # Checkpoint Configuration
    checkpoint:
      enabled: true
      storage-backend: filesystem
      ttl-hours: 24
      max-per-run: 10
      filesystem-dir: /tmp/wayang/agent-checkpoints
    
    # Orchestrator-specific Configuration
    orchestrators:
      tool-calling:
        temperature: 0.7
        max-tokens: 2048
        parallel-execution-enabled: true
        caching-enabled: true
        cache-ttl-minutes: 10
      
      reflexion:
        max-reflections: 3
        evaluator-temperature: 0.2
        reviser-temperature: 0.6
      
      multi-agent:
        enabled: true
        max-agents: 5
        communication-enabled: true
```

## Integration with Gollek

The agent module integrates with Gollek through:

### Inference Engine
- `gollek-sdk-core` - GollekSdk for inference operations
- `gollek-spi-inference` - InferenceRequest/Response SPI
- `gollek-engine` - InferenceService for execution

### Provider Management
- `gollek-provider-core` - Provider configuration and routing
- `gollek-spi-provider` - ProviderInfo and capabilities

### Tool Integration
- `gollek-tool-core` - Tool execution and result handling
- `gollek-spi` - ToolDefinition for native tool calling

## Migration Guide

### From v1.x to v2.0

1. **Update Dependencies:**
   Ensure you have the latest Gollek SDK and SPI dependencies.

2. **Update Configuration:**
   Migrate your configuration to the new structure with provider, inference, security, and checkpoint sections.

3. **Update Custom Orchestrators:**
   If you have custom orchestrators, implement the new default methods as needed:
   ```java
   @Override
   public boolean supportsToolCalling() {
       return true;
   }
   
   @Override
   public Uni<String> onToolCall(AgentState state, String toolName, Map<String, Object> arguments) {
       // Implement tool execution
   }
   ```

4. **Enable New Features:**
   Opt-in to new features through configuration:
   ```yaml
   gollek:
     agent:
       checkpoint:
         enabled: true
       security:
         enabled: true
       metrics:
         detailed-steps-enabled: true
   ```

## Testing

### Unit Tests
Test classes should be created for:
- `GollekAgentClientTest` - Client functionality
- `ProviderAwareInferenceTest` - Provider routing
- `ToolCallingAgentLoopTest` - Tool calling
- `MultiAgentCoordinatorTest` - Multi-agent workflows
- `AdaptiveStrategySelectorTest` - Strategy selection
- `AgentSecurityEnforcerTest` - Security features

### Integration Tests
- End-to-end agent workflows
- Provider fallback scenarios
- Multi-agent collaboration
- Checkpoint/resume functionality

### Example Test:
```java
@QuarkusTest
class ToolCallingAgentLoopTest {
    
    @Inject
    ToolCallingAgentLoop agentLoop;
    
    @Inject
    SkillRegistry skillRegistry;
    
    @Test
    void testToolCalling() {
        AgentRequest request = AgentRequest.builder()
            .requestId("test-1")
            .tenantId("test-tenant")
            .prompt("Calculate 2 + 2")
            .allowedSkills(Set.of("calculator"))
            .build();
        
        AgentResponse response = agentLoop.execute(request)
            .await().atMost(Duration.ofSeconds(30));
        
        assertThat(response.successful()).isTrue();
        assertThat(response.answer()).contains("4");
    }
}
```

## Performance Considerations

### Provider Selection
- Local providers (GGUF, ONNX) are preferred for cost efficiency
- Cloud providers used for complex reasoning tasks
- Automatic fallback ensures high availability

### Caching
- Tool results cached for 10 minutes
- Task type analysis cached for 60 minutes
- Provider health cached with automatic refresh

### Parallel Execution
- Up to 5 tools executed in parallel
- Dependency analysis prevents race conditions
- Configurable thread pool size

### Memory Management
- Conversation history limited to 50 messages by default
- Context compression for long conversations
- Automatic checkpoint cleanup with TTL

## Security Best Practices

1. **Enable Input Validation:**
   Detects and blocks prompt injection attempts

2. **Enable Output Filtering:**
   Redacts PII and sensitive data from responses

3. **Configure Quotas:**
   Prevent resource exhaustion with tenant quotas

4. **Tool Authorization:**
   Restrict tool access per tenant

5. **Audit Logging:**
   Enable comprehensive logging for compliance

## Future Enhancements

Planned improvements:
- AI-powered decision agent for workflow optimization
- Predictive scaling agent for proactive resource management
- Anomaly detection for unusual patterns
- Enhanced visualization for agent interactions
- Zero-trust security architecture

## Support

For issues and questions:
- GitHub Issues: [wayang-platform/issues](https://github.com/wayang-platform/issues)
- Documentation: [wayang-platform/docs](https://github.com/wayang-platform/docs)
- Discussion: [wayang-platform/discussions](https://github.com/wayang-platform/discussions)

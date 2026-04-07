# Gollek Agent Module - Quick Start Guide

## Overview

The improved `gollek-extension/agent` module provides enhanced agentic AI capabilities with seamless integration to the Gollek inference engine, supporting both local (GGUF, ONNX) and cloud (OpenAI, Anthropic, Mistral) providers.

## Prerequisites

- Java 21
- Maven 3.8+
- Gollek SDK configured with at least one provider
- Quarkus 3.x project

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>tech.kayys.gollek.agent</groupId>
    <artifactId>agent-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Configuration

### Minimal Configuration

```yaml
gollek:
  agent:
    default-strategy: react
    default-max-steps: 10
    default-model: default
    provider:
      preferred: openai
      fallback-enabled: true
```

### Full Configuration

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
        gguf:
          timeout-seconds: 60
          max-retries: 2

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

    # Orchestrator Configuration
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

## Usage Examples

### 1. Simple Agent Execution

```java
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import tech.kayys.gollek.agent.core.AgentBuilder;
import tech.kayys.gollek.agent.spi.AgentResponse;

@Path("/agent")
public class AgentResource {

    @Inject
    AgentBuilder agentBuilder;

    @POST
    @Path("/run")
    public String runAgent(String task) {
        AgentResponse response = agentBuilder
            .newAgent()
            .withPrompt(task)
            .usingSkills("inference", "rag")
            .withMaxSteps(5)
            .forTenant("my-tenant")
            .executeBlocking();

        return response.answer();
    }
}
```

### 2. Using GollekAgentClient Directly

```java
import jakarta.inject.Inject;
import tech.kayys.gollek.agent.client.GollekAgentClient;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.Message;

public class InferenceService {

    @Inject
    GollekAgentClient agentClient;

    public String infer(String prompt) {
        InferenceRequest request = InferenceRequest.builder()
            .model("Qwen/Qwen2.5-7B-Instruct")
            .message(Message.user(prompt))
            .parameter("temperature", 0.7)
            .parameter("max_tokens", 512)
            .build();

        InferenceResponse response = agentClient.infer(request)
            .await().atMost(Duration.ofSeconds(30));

        return response.getContent();
    }
}
```

### 3. Tool Calling

```java
import tech.kayys.gollek.agent.client.GollekAgentClient;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.tool.ToolDefinition;

public class ToolCallingExample {

    @Inject
    GollekAgentClient agentClient;

    public String executeWithTools(String task, List<ToolDefinition> tools) {
        InferenceRequest request = InferenceRequest.builder()
            .model("openai:gpt-4")
            .message(Message.user(task))
            .tools(tools)
            .toolChoice("auto")
            .parameter("temperature", 0.7)
            .build();

        InferenceResponse response = agentClient.inferWithTools(request, tools)
            .await().atMost(Duration.ofSeconds(60));

        // Check if model wants to call tools
        if (response.hasToolCalls()) {
            // Execute tools and continue conversation
            // ...
        }

        return response.getContent();
    }
}
```

### 4. Streaming Support

```java
import io.smallrye.mutiny.Multi;
import tech.kayys.gollek.agent.client.GollekAgentClient;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.StreamingInferenceChunk;

public class StreamingExample {

    @Inject
    GollekAgentClient agentClient;

    public Multi<StreamingInferenceChunk> streamResponse(String prompt) {
        InferenceRequest request = InferenceRequest.builder()
            .model("gguf:llama-2-7b-chat.gguf")
            .message(Message.user(prompt))
            .streaming(true)
            .build();

        return agentClient.stream(request);
    }
}
```

### 5. Multi-Agent Collaboration

```java
import tech.kayys.gollek.agent.coordinator.MultiAgentCoordinator;
import tech.kayys.gollek.agent.spi.AgentResponse;

public class MultiAgentExample {

    @Inject
    MultiAgentCoordinator coordinator;

    public String solveComplexTask(String task) {
        AgentResponse response = coordinator.execute(task)
            .withAgents("planner", "executor", "reviewer")
            .withMaxIterations(3)
            .executeBlocking();

        return response.answer();
    }
}
```

### 6. Adaptive Strategy Selection

```java
import tech.kayys.gollek.agent.selector.AdaptiveStrategySelector;
import tech.kayys.gollek.agent.spi.AgentOrchestrator;
import tech.kayys.gollek.agent.spi.OrchestrationStrategy;

public class AdaptiveExample {

    @Inject
    AdaptiveStrategySelector strategySelector;

    public void executeWithAdaptiveStrategy(String task) {
        // Automatically selects best strategy based on task analysis
        AgentOrchestrator orchestrator = strategySelector.selectStrategy(task);

        // Use the selected orchestrator
        // ...
    }
}
```

### 7. Checkpoint and Resume

```java
import tech.kayys.gollek.agent.checkpoint.AgentCheckpointManager;
import tech.kayys.gollek.agent.spi.AgentState;

public class CheckpointExample {

    @Inject
    AgentCheckpointManager checkpointManager;

    public void saveState(String runId, AgentState state) {
        checkpointManager.saveCheckpoint(runId, state);
    }

    public AgentState restoreState(String runId) {
        return checkpointManager.loadCheckpoint(runId)
            .orElseThrow(() -> new IllegalStateException("Checkpoint not found"));
    }
}
```

### 8. Security Enforcement

```java
import tech.kayys.gollek.agent.security.AgentSecurityEnforcer;
import tech.kayys.gollek.agent.spi.AgentRequest;

public class SecurityExample {

    @Inject
    AgentSecurityEnforcer securityEnforcer;

    public void validateRequest(AgentRequest request) {
        // Check quotas
        securityEnforcer.checkQuota(request.tenantId());

        // Validate input
        securityEnforcer.validateInput(request.prompt())
            .ifInvalid(throw new IllegalArgumentException("Invalid input"));

        // Filter output
        String filteredOutput = securityEnforcer.filterOutput(rawOutput);
    }
}
```

## Available Skills

The agent module comes with built-in skills:

1. **inference** - LLM inference through Gollek
2. **rag** - Retrieval-augmented generation
3. **code-execution** - Execute Python/Node.js code
4. **http-call** - Make HTTP requests
5. **embedding** - Generate embeddings
6. **summarization** - Summarize text
7. **memory-store** - Store/retrieve from memory

## Available Orchestrators

1. **react** - ReAct (Reason + Act) strategy
2. **plan-and-execute** - Plan first, then execute
3. **reflexion** - Self-reflection for quality improvement
4. **tool-calling** - Native tool calling support
5. **multi-agent** - Multi-agent collaboration

## Provider Support

### Local Providers
- **GGUF** - llama.cpp format models
- **ONNX** - ONNX Runtime models
- **SafeTensors** - PyTorch models

### Cloud Providers
- **OpenAI** - GPT-4, GPT-3.5
- **Anthropic** - Claude
- **Mistral** - Mistral models
- **Google** - Gemini
- **Ollama** - Self-hosted models

## Monitoring and Observability

### Metrics

The agent module exports the following metrics (Micrometer format):

- `agent.executions.total` - Total agent executions
- `agent.executions.duration` - Execution duration
- `agent.steps.total` - Total reasoning steps
- `agent.steps.latency` - Step latency
- `agent.tokens.used` - Token consumption
- `agent.tools.executed` - Tool executions
- `agent.provider.latency` - Provider-specific latency

### Distributed Tracing

OpenTelemetry integration is available for distributed tracing. Enable it with:

```yaml
quarkus:
  opentelemetry:
    enabled: true
    tracer:
      service-name: wayang-agent
```

## Best Practices

### 1. Provider Selection

```yaml
# For cost-sensitive workloads
gollek:
  agent:
    provider:
      local-preferred: true
      fallback-enabled: true

# For quality-critical workloads
gollek:
  agent:
    provider:
      preferred: openai
      fallback-enabled: true
```

### 2. Security

```yaml
# Enable all security features
gollek:
  agent:
    security:
      enabled: true
      input-validation-enabled: true
      output-filtering-enabled: true
      prompt-injection-detection-enabled: true
      pii-redaction-enabled: true
```

### 3. Performance

```yaml
# Optimize for low latency
gollek:
  agent:
    inference:
      timeout: 30s
      streaming-enabled: true
    orchestrators:
      tool-calling:
        parallel-execution-enabled: true
        caching-enabled: true
```

### 4. Long-Running Workflows

```yaml
# Enable checkpointing for recovery
gollek:
  agent:
    checkpoint:
      enabled: true
      storage-backend: filesystem
      ttl-hours: 24
```

## Troubleshooting

### Common Issues

#### 1. Provider Not Found

**Error:** `No provider available for model X`

**Solution:**
- Ensure the provider is configured in `application.yaml`
- Check that the provider is enabled
- Verify API keys are set

#### 2. Tool Calling Not Working

**Error:** `Model does not support tool calling`

**Solution:**
- Use a model that supports tool calling (GPT-4, Claude)
- Enable tool calling in configuration:
  ```yaml
  gollek:
    agent:
      inference:
        tool-calling-enabled: true
  ```

#### 3. Checkpoint Save Failed

**Error:** `Failed to save checkpoint`

**Solution:**
- Ensure checkpoint directory exists and is writable
- Check disk space
- Verify filesystem permissions

## Migration from v1.x

### Breaking Changes

1. **Configuration Structure:** Updated to nested structure
2. **SPI Methods:** New default methods added to `AgentOrchestrator`
3. **Provider Selection:** Now uses `ProviderAwareInference`

### Migration Steps

1. Update configuration to new structure
2. Implement new `AgentOrchestrator` methods if needed
3. Update imports to use new package structure
4. Test with fallback disabled first

## Additional Resources

- [Full Documentation](AGENT_IMPROVEMENTS.md)
- [Implementation Summary](IMPLEMENTATION_SUMMARY.md)
- [API Reference](https://wayang-platform.github.io/docs/agent-api)
- [Examples Repository](https://github.com/wayang-platform/agent-examples)

## Support

- GitHub Issues: [wayang-platform/issues](https://github.com/wayang-platform/issues)
- Discussion: [wayang-platform/discussions](https://github.com/wayang-platform/discussions)
- Email: support@wayang.tech

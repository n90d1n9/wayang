# Gollek Agentic Core System — AGENTIC-QWEN.md

**Version:** 2.0.0  
**Platform:** Gollek Inference Engine (Quarkus 3.x / Multi-Tenant)  
**Author:** Gollek Team  
**Status:** ✅ Production Ready

---

## Overview

The **Gollek Agentic Core System** is a **skills-based, memory-enabled, tool-driven agent framework** built on top of the Gollek inference engine. It provides a production-ready foundation for building intelligent agents that can:

- 🧠 **Execute multi-step reasoning** using ReAct, Plan-and-Execute, Chain-of-Thought, and Reflexion patterns
- 🛠️ **Call registered skills and tools** during inference with dynamic discovery
- 💾 **Maintain multi-layer memory** (working, conversation, vector, episodic) across sessions
- 🔄 **Route across multiple LLM providers** and model formats seamlessly
- 🏢 **Operate in multi-tenant enterprise** environments with isolation and quotas
- 🔌 **Extend via plugins** with hot-reload capability

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Gollek Agentic Core System                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────┐     ┌──────────────────┐    ┌──────────────┐ │
│  │   AgentRequest   │────▶│ AgentOrchestrator│───▶│   Memory     │ │
│  │   (User Input)   │     │  (ReAct/P&E/CoT) │    │   Manager    │ │
│  └──────────────────┘     └────────┬─────────┘    └──────────────┘ │
│                                    │                                 │
│                           ┌────────▼────────┐                       │
│                           │  SkillRegistry  │                       │
│                           │  + ToolAdapter  │                       │
│                           └────────┬────────┘                       │
│                                    │ SkillCall                      │
│           ┌────────────────────────┼────────────────────────┐       │
│           │                        │                        │       │
│    ┌──────▼──────┐         ┌──────▼──────┐          ┌──────▼──────┐│
│    │ Built-in    │         │   Plugin    │          │   External  ││
│    │ Skills      │         │   Skills    │          │   Tools     ││
│    │             │         │             │          │             ││
│    │ • Inference │         │ • Custom    │          │ • MCP       ││
│    │ • RAG       │         │ • Domain    │          │ • REST API  ││
│    │ • Code      │         │ • Enterprise│          │ • gRPC      ││
│    │ • HTTP      │         │ • Plugin    │          │ • CLI       ││
│    │ • Memory    │         │             │          │             ││
│    └─────────────┘         └─────────────┘          └─────────────┘│
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Module Structure

```
inference-gollek/plugins/agent/agent-core/
├── src/main/java/tech/kayys/gollek/agent/
│   ├── core/
│   │   ├── AgentConfig.java              # Configuration mapping
│   │   ├── AgentBuilder.java             # Fluent agent builder
│   │   ├── AgentOrchestrator.java        # Core orchestrator SPI
│   │   ├── AgentState.java               # State management
│   │   ├── AgentHealthCheck.java         # Health monitoring
│   │   ├── ReActOrchestrator.java        # ReAct implementation
│   │   ├── PlanAndExecuteOrchestrator.java # P&E implementation
│   │   ├── ChainOfThoughtOrchestrator.java # CoT implementation
│   │   └── ReflexionOrchestrator.java    # Reflexion implementation
│   ├── memory/
│   │   ├── AgentMemory.java              # Memory SPI
│   │   ├── WorkingMemory.java            # Short-term working memory
│   │   ├── ConversationMemory.java       # Conversation history
│   │   ├── VectorMemory.java             # Semantic/vector memory
│   │   ├── EpisodicMemory.java           # Long-term episodic memory
│   │   └── MemoryManager.java            # Memory orchestration
│   ├── skills/
│   │   ├── AgentSkill.java               # Skill SPI
│   │   ├── SkillDescriptor.java          # Skill annotation
│   │   ├── SkillContext.java             # Skill execution context
│   │   ├── SkillResult.java              # Skill execution result
│   │   ├── SkillRegistry.java            # Skill registry SPI
│   │   ├── DefaultSkillRegistry.java     # Default implementation
│   │   ├── SkillLoader.java              # Dynamic skill loading
│   │   └── builtin/                      # Built-in skills
│   │       ├── InferenceSkill.java
│   │       ├── RAGSkill.java
│   │       ├── CodeExecutionSkill.java
│   │       ├── HttpCallSkill.java
│   │       ├── MemoryStoreSkill.java
│   │       ├── EmbeddingSkill.java
│   │       └── SummarizationSkill.java
│   ├── tools/
│   │   ├── Tool.java                     # Tool SPI
│   │   ├── ToolDescriptor.java           # Tool annotation
│   │   ├── ToolContext.java              # Tool execution context
│   │   ├── ToolResult.java               # Tool execution result
│   │   ├── ToolRegistry.java             # Tool registry SPI
│   │   ├── ToolAdapter.java              # Skill↔Tool adapter
│   │   └── external/                     # External tool integrations
│   │       ├── MCPToolProvider.java      # Model Context Protocol
│   │       ├── RESTToolProvider.java     # REST API tools
│   │       └── CLIToolProvider.java      # CLI command tools
│   ├── dto/
│   │   ├── AgentRequest.java
│   │   ├── AgentResponse.java
│   │   ├── AgentEvent.java
│   │   └── AgentConfigDTO.java
│   └── api/
│       ├── AgentResource.java            # REST API endpoints
│       └── AgentWebSocket.java           # Streaming WebSocket
└── src/test/java/
```

---

## Core Concepts

### 1. Skills

**Skills** are self-contained, versioned, discoverable units of agent capability. Each skill:

- Implements the `AgentSkill` interface
- Declares metadata via `@SkillDescriptor`
- Provides input/output schemas
- Can be discovered dynamically via ServiceLoader or plugin loading

**Skill Contract:**
```java
@SkillDescriptor(
    id          = "inference",
    name        = "LLM Inference",
    description = "Execute natural language inference via configured LLM providers",
    version     = "1.0.0",
    category    = SkillCategory.REASONING,
    triggers    = {"infer", "generate", "llm", "ask model"},
    inputs      = {
        @SkillInput(name="prompt", type="string", required=true),
        @SkillInput(name="model", type="string", required=false)
    },
    outputs     = {
        @SkillOutput(name="response", type="string"),
        @SkillOutput(name="tokensUsed", type="integer")
    }
)
public class InferenceSkill implements AgentSkill {
    
    @Override
    public Uni<SkillResult> execute(SkillContext context) {
        String prompt = context.getInput("prompt", String.class);
        String model = context.getInput("model", String.class).orElse("default");
        
        // Execute inference via Gollek
        return inferenceService.infer(buildRequest(prompt, model))
            .map(response -> SkillResult.success(Map.of(
                "response", response.getContent(),
                "tokensUsed", response.getTokensUsed()
            )));
    }
}
```

### 2. Tools

**Tools** are external capabilities that agents can invoke. Tools can be:

- **Internal Skills** — Wrapped from the skill registry
- **MCP Tools** — Model Context Protocol servers
- **REST APIs** — HTTP endpoints with OpenAPI specs
- **CLI Commands** — Shell commands with structured I/O
- **gRPC Services** — Protocol buffer-based services

**Tool Adapter Pattern:**
```java
public class ToolAdapter {
    
    private final SkillRegistry skillRegistry;
    private final List<ToolProvider> toolProviders;
    
    /**
     * Adapt a skill to a tool descriptor for LLM consumption
     */
    public ToolDescriptor adaptSkill(AgentSkill skill) {
        return ToolDescriptor.builder()
            .id("skill:" + skill.id())
            .name(skill.descriptor().name())
            .description(skill.descriptor().description())
            .schema(buildJsonSchema(skill.descriptor()))
            .source(ToolSource.INTERNAL_SKILL)
            .build();
    }
    
    /**
     * Execute a tool (routes to appropriate provider)
     */
    public Uni<ToolResult> execute(ToolContext context) {
        return switch (context.getToolSource()) {
            case INTERNAL_SKILL -> executeSkill(context);
            case MCP_SERVER -> executeMCP(context);
            case REST_API -> executeREST(context);
            case CLI_COMMAND -> executeCLI(context);
        };
    }
}
```

### 3. Memory

The **Memory System** provides four layers of memory for agents:

```
┌─────────────────────────────────────────────────────────┐
│              Agent Memory Architecture                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────┐      ┌─────────────────────────┐  │
│  │ Working Memory  │      │  Conversation Memory    │  │
│  │ • Current turn  │      │  • Chat history         │  │
│  │ • Reasoning     │      │  • Session context      │  │
│  │ • Scratchpad    │      │  • User preferences     │  │
│  │ TTL: ~minutes   │      │  TTL: ~hours            │  │
│  └─────────────────┘      └─────────────────────────┘  │
│                                                         │
│  ┌─────────────────┐      ┌─────────────────────────┐  │
│  │ Vector Memory   │      │  Episodic Memory        │  │
│  │ • Embeddings    │      │  • Past sessions        │  │
│  │ • Semantic search│     │  • Long-term knowledge  │  │
│  │ • RAG retrieval │      │  • User history         │  │
│  │ Backend: Qdrant │      │  Backend: PostgreSQL    │  │
│  └─────────────────┘      └─────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Memory SPI:**
```java
public interface AgentMemory {
    
    // Working Memory (short-term)
    Uni<Void> setWorking(String key, Object value);
    <T> Uni<T> getWorking(String key, Class<T> type);
    Uni<Map<String, Object>> getAllWorking();
    Uni<Void> clearWorking();
    
    // Conversation Memory (session history)
    Uni<Void> addMessage(String conversationId, Message message);
    Uni<List<Message>> getConversation(String conversationId, int limit);
    Uni<Void> clearConversation(String conversationId);
    
    // Vector Memory (semantic)
    Uni<VectorSearchResult> searchSimilar(String query, int topK);
    Uni<Void> storeEmbedding(String collection, Embedding embedding);
    Uni<Void> deleteEmbeddings(String collection, String tenantId);
    
    // Episodic Memory (long-term)
    Uni<Episode> storeEpisode(Episode episode);
    Uni<List<Episode>> retrieveEpisodes(String tenantId, Instant after);
    Uni<Void> forgetEpisode(String episodeId);
}
```

### 4. Orchestration Strategies

The agent supports multiple reasoning patterns:

#### ReAct (Reason + Act)
```
Thought → Action(ToolCall) → Observation → Thought → ... → FinalAnswer
```
**Best for:** Open-ended tasks, tool-heavy reasoning, exploration

#### Plan-and-Execute
```
Plan[step1, step2, step3] → Execute(step1) → Execute(step2) → Synthesize
```
**Best for:** Structured tasks, known sub-goals, multi-step workflows

#### Chain-of-Thought
```
CoT[reasoning steps] → FinalAnswer
```
**Best for:** Mathematical/logical reasoning, no external tools

#### Reflexion
```
Attempt → Critique → Reflect → Improved Attempt → ...
```
**Best for:** Self-improvement, code generation, iterative refinement

---

## Built-in Skills Catalog

| ID | Category | Description | Triggers |
|----|----------|-------------|----------|
| `inference` | REASONING | Direct LLM inference via Gollek providers | infer, generate, llm |
| `rag` | RETRIEVAL | Retrieval-Augmented Generation with vector store | search, retrieve, find |
| `code-execution` | EXECUTION | Sandboxed code execution (Python/Java/JS) | run code, execute, compile |
| `web-search` | RETRIEVAL | Web search and content extraction | search web, google, find |
| `http-call` | INTEGRATION | Make authenticated HTTP API calls | api call, http request |
| `summarization` | REASONING | Multi-document summarization | summarize, condense |
| `embedding` | VECTOR | Generate vector embeddings | embed, vectorize |
| `memory-store` | MEMORY | Store/retrieve from long-term memory | remember, save, recall |
| `sql-query` | DATA | Execute SQL queries against datasources | query database, sql |
| `document-qa` | RETRIEVAL | QA over uploaded documents | document question, pdf |

---

## Configuration

### application.yaml

```yaml
gollek:
  agent:
    enabled: true
    default-strategy: react
    default-max-steps: 15
    default-timeout-seconds: 120
    default-model: llama-3-8b-instruct
    
    # Multi-tenancy
    multitenancy:
      enabled: true
      header-name: X-Tenant-Id
    
    # Memory configuration
    memory:
      backend: hybrid  # in-memory | redis | pg-vector | hybrid
      redis:
        url: redis://localhost:6379
        key-prefix: gollek:agent:
        ttl-seconds: 3600
      conversation:
        max-history-per-session: 50
        session-ttl-seconds: 7200
      vector:
        enabled: true
        provider: qdrant
        endpoint: http://localhost:6333
        collection-prefix: gollek_
      episodic:
        enabled: true
        backend: postgresql
        retention-days: 90
    
    # Orchestrator-specific config
    orchestrators:
      react:
        temperature: 0.1
        max-tokens-per-step: 1024
        stop-tokens: ["Observation:", "\nObservation:"]
      plan-and-execute:
        planner-temperature: 0.3
        planner-max-tokens: 512
        executor-temperature: 0.5
        executor-max-tokens: 512
        synthesizer-temperature: 0.4
    
    # Skill configuration
    skills:
      auto-discover: true
      scan-packages:
        - tech.kayys.gollek.agent.skills
        - com.mycompany.skills
      inference:
        enabled: true
        default-model: llama-3-8b-instruct
        default-max-tokens: 1024
        default-temperature: 0.7
      rag:
        enabled: true
        default-collection: agent-knowledge
        default-top-k: 5
        embedding-model: qwen-embed
      code-execution:
        enabled: true
        python-executable: python3
        node-executable: node
        default-timeout-seconds: 30
      http-call:
        enabled: true
        default-timeout-seconds: 15
        allowed-hosts: api.openai.com,api.anthropic.com
    
    # Tools configuration
    tools:
      mcp:
        enabled: true
        servers:
          - name: filesystem
            command: npx
            args: ["-y", "@modelcontextprotocol/server-filesystem", "/data"]
          - name: postgres
            command: npx
            args: ["-y", "@modelcontextprotocol/server-postgres", "postgresql://localhost"]
      rest:
        enabled: true
        specs:
          - url: https://api.openai.com/openapi.yaml
            auth: bearer
      cli:
        enabled: false
        allowed-commands: ["ls", "cat", "grep", "wc"]
    
    # Metrics & observability
    metrics:
      enabled: true
      include-tenant-tag: true
    tracing:
      enabled: true
      sampler: probabilistic
      probability: 0.1
    
    # Tenants
    tenants:
      default:
        max-concurrent-agents: 10
        allowed-skills: "*"
        max-steps: 15
        memory-limit-mb: 512
      premium:
        max-concurrent-agents: 50
        allowed-skills: "*"
        max-steps: 30
        memory-limit-mb: 2048
```

---

## Usage

### Programmatic API

```java
@Inject
AgentOrchestrator orchestrator;

@Inject
SkillRegistry skillRegistry;

// Build agent request
AgentRequest request = AgentRequest.builder()
    .prompt("Analyze the Q3 sales data and find top 5 customers by revenue growth")
    .strategy(OrchestrationStrategy.REACT)
    .skills("sql-query", "data-analysis", "summarization")
    .context("database", "sales_db")
    .context("quarter", "Q3-2024")
    .maxSteps(15)
    .timeout(Duration.ofSeconds(120))
    .tenantId("enterprise-tenant")
    .conversationId("conv-123")
    .stream(false)
    .build();

// Execute
Uni<AgentResponse> response = orchestrator.execute(request);

// Subscribe
response.subscribe().with(
    result -> {
        System.out.println("Answer: " + result.getAnswer());
        System.out.println("Steps: " + result.getSteps());
        System.out.println("Duration: " + result.getTotalDuration());
    },
    error -> System.err.println("Error: " + error.getMessage())
);
```

### Streaming

```java
@Inject
AgentOrchestrator orchestrator;

AgentRequest request = AgentRequest.builder()
    .prompt("Research and summarize recent AI breakthroughs")
    .strategy(OrchestrationStrategy.REACT)
    .stream(true)
    .build();

// Stream events
Multi<AgentEvent> events = orchestrator.stream(request);

events.subscribe().with(
    event -> {
        switch (event.getType()) {
            case STARTED -> System.out.println("Agent started: " + event.getPrompt());
            case THOUGHT -> System.out.println("Thinking: " + event.getContent());
            case ACTION -> System.out.println("Action: " + event.getAction());
            case OBSERVATION -> System.out.println("Observation: " + event.getContent());
            case FINAL_ANSWER -> System.out.println("Answer: " + event.getContent());
            case ERROR -> System.err.println("Error: " + event.getError());
        }
    }
);
```

### REST API

```http
POST /api/v1/agents/run
Content-Type: application/json
X-Tenant-ID: enterprise-tenant

{
  "prompt": "Search for recent AI papers about LLM efficiency and summarize key findings",
  "strategy": "react",
  "skills": ["web-search", "summarization"],
  "maxSteps": 15,
  "timeout": "PT120S",
  "stream": true,
  "context": {
    "searchDepth": "comprehensive",
    "minDate": "2024-01-01"
  }
}
```

**Response:**
```json
{
  "runId": "run-abc123",
  "requestId": "req-xyz789",
  "answer": "Recent AI papers on LLM efficiency focus on three main areas...",
  "steps": [
    {
      "step": 1,
      "thought": "I need to search for recent papers on LLM efficiency",
      "action": {
        "skillId": "web-search",
        "inputs": {"query": "LLM efficiency papers 2024"}
      },
      "observation": "Found 15 relevant papers...",
      "durationMs": 1523
    }
  ],
  "totalSteps": 5,
  "totalDurationMs": 8456,
  "successful": true
}
```

### WebSocket Streaming

```javascript
const ws = new WebSocket('ws://localhost:8080/api/v1/agents/stream');

ws.onopen = () => {
  ws.send(JSON.stringify({
    prompt: "Analyze this dataset and find patterns",
    strategy: "react",
    skills: ["data-analysis", "summarization"],
    stream: true
  }));
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log(`${data.type}: ${data.content}`);
};
```

---

## Skill Development

### Creating a Custom Skill

#### Step 1: Implement AgentSkill

```java
@ApplicationScoped
@SkillDescriptor(
    id          = "sentiment-analysis",
    name        = "Sentiment Analysis",
    description = "Analyze sentiment of text using fine-tuned classification model",
    version     = "1.0.0",
    category    = SkillCategory.ANALYTICS,
    triggers    = {"sentiment", "emotion", "tone analysis"},
    inputs      = {
        @SkillInput(name="text", type="string", required=true, description="Text to analyze"),
        @SkillInput(name="model", type="string", required=false, description="Model to use")
    },
    outputs     = {
        @SkillOutput(name="sentiment", type="string", description="positive/negative/neutral"),
        @SkillOutput(name="confidence", type="number", description="Confidence score 0-1"),
        @SkillOutput(name="emotions", type="object", description="Emotion breakdown")
    }
)
public class SentimentAnalysisSkill implements AgentSkill {
    
    @Inject
    InferenceService inferenceService;
    
    @Override
    public Uni<SkillResult> execute(SkillContext context) {
        String text = context.getInput("text", String.class)
            .orElseThrow(() -> new SkillException("Missing required input: text"));
        
        String model = context.getInput("model", String.class).orElse("sentiment-bert");
        
        String prompt = """
            Analyze the sentiment of the following text.
            Respond in JSON format:
            {
              "sentiment": "positive|negative|neutral",
              "confidence": 0.0-1.0,
              "emotions": {"joy": 0.0, "anger": 0.0, ...}
            }
            
            Text: %s
            """.formatted(text);
        
        return inferenceService.inferAsync(buildRequest(model, prompt))
            .map(response -> {
                JsonNode result = parseJson(response.getContent());
                return SkillResult.success(Map.of(
                    "sentiment", result.get("sentiment").asText(),
                    "confidence", result.get("confidence").asDouble(),
                    "emotions", result.get("emotions")
                ));
            });
    }
    
    @Override
    public Set<String> aliases() {
        return Set.of("analyze-sentiment", "emotion-detection");
    }
}
```

#### Step 2: Register Skill

**Option A: ServiceLoader (automatic)**

Create `src/main/resources/META-INF/services/tech.kayys.gollek.agent.spi.AgentSkill`:
```
com.mycompany.skills.SentimentAnalysisSkill
```

**Option B: Plugin Loading**

Package as JAR with manifest:
```manifest
Plugin-Id: sentiment-analysis-skill
Plugin-Type: agent-skill
Plugin-Version: 1.0.0
Plugin-Provider: com.mycompany.skills.SentimentAnalysisSkill
Plugin-Capabilities: sentiment-analysis, text-classification
```

Deploy to `~/.gollek/plugins/agent-skills/`

#### Step 3: Test Skill

```java
@QuarkusTest
class SentimentAnalysisSkillTest {
    
    @Inject
    SentimentAnalysisSkill skill;
    
    @Test
    void testPositiveSentiment() {
        SkillContext context = SkillContext.builder()
            .skillId("sentiment-analysis")
            .input("text", "I love this product! It's amazing!")
            .build();
        
        SkillResult result = skill.execute(context)
            .await().indefinitely();
        
        assertTrue(result.isSuccess());
        assertEquals("positive", result.getOutput("sentiment"));
        assertTrue(result.getOutput("confidence", Double.class) > 0.8);
    }
}
```

---

## Tool Integration

### Model Context Protocol (MCP)

```java
@ApplicationScoped
public class MCPToolProvider implements ToolProvider {
    
    @Override
    public ToolSource source() {
        return ToolSource.MCP_SERVER;
    }
    
    @Override
    public Uni<List<ToolDescriptor>> discoverTools() {
        // Discover tools from MCP servers
        return mcpClient.listTools()
            .map(tools -> tools.stream()
                .map(this::toToolDescriptor)
                .toList());
    }
    
    @Override
    public Uni<ToolResult> execute(ToolContext context) {
        return mcpClient.callTool(
            context.getToolId(),
            context.getInputs()
        ).map(response -> ToolResult.success(response));
    }
}
```

### REST API Tools

```java
@ApplicationScoped
public class RESTToolProvider implements ToolProvider {
    
    @Inject
    OpenAPIParser openAPIParser;
    
    @Inject
    RestClient client;
    
    @Override
    public Uni<List<ToolDescriptor>> discoverTools() {
        // Parse OpenAPI spec and extract operations as tools
        return openAPIParser.parse("https://api.openai.com/openapi.yaml")
            .map(spec -> spec.getPaths().entrySet().stream()
                .map(entry -> ToolDescriptor.builder()
                    .id("rest:" + entry.getKey())
                    .name(entry.getKey())
                    .description(entry.getValue().getDescription())
                    .schema(buildSchema(entry.getValue()))
                    .source(ToolSource.REST_API)
                    .build())
                .toList());
    }
    
    @Override
    public Uni<ToolResult> execute(ToolContext context) {
        // Execute HTTP request based on tool context
        return client.request(context)
            .map(response -> ToolResult.success(response.getBody()));
    }
}
```

---

## Memory Management

### Working Memory

```java
@Inject
AgentMemory memory;

// Store in working memory
await(memory.setWorking("current_task", "data-analysis"));
await(memory.setWorking("intermediate_results", results));

// Retrieve from working memory
String task = await(memory.getWorking("current_task", String.class));
List<Result> results = await(memory.getWorking(
    "intermediate_results", 
    new TypeToken<List<Result>>(){}.getType()
));

// Clear working memory
await(memory.clearWorking());
```

### Conversation Memory

```java
// Add message to conversation
Message userMsg = Message.user("What's the weather today?");
await(memory.addMessage("conv-123", userMsg));

Message assistantMsg = Message.assistant("It's sunny with 25°C");
await(memory.addMessage("conv-123", assistantMsg));

// Get conversation history
List<Message> history = await(memory.getConversation("conv-123", 50));

// Clear conversation
await(memory.clearConversation("conv-123"));
```

### Vector Memory (RAG)

```java
// Store embedding
Embedding embedding = Embedding.builder()
    .tenantId("tenant-123")
    .collection("knowledge-base")
    .content("Gollek is an inference engine...")
    .vector(embeddingModel.encode("Gollek is an inference engine..."))
    .metadata(Map.of("source", "docs", "date", "2024-01-01"))
    .build();

await(memory.storeEmbedding("knowledge-base", embedding));

// Search for similar content
VectorSearchResult results = await(memory.searchSimilar(
    "What is Gollek?",
    5  // top-k
));

for (VectorMatch match : results.getMatches()) {
    System.out.println("Match: " + match.getContent());
    System.out.println("Score: " + match.getSimilarity());
}
```

### Episodic Memory

```java
// Store episode
Episode episode = Episode.builder()
    .tenantId("tenant-123")
    .userId("user-456")
    .type("agent-session")
    .summary("Analyzed Q3 sales data")
    .content(fullSessionLog)
    .tags(List.of("sales", "analysis", "q3"))
    .build();

Episode stored = await(memory.storeEpisode(episode));

// Retrieve episodes
List<Episode> episodes = await(memory.retrieveEpisodes(
    "tenant-123",
    Instant.now().minus(30, ChronoUnit.DAYS)
));
```

---

## Orchestration Patterns

### ReAct Orchestrator

```java
@ApplicationScoped
public class ReActOrchestrator implements AgentOrchestrator {
    
    @Override
    public String strategyId() {
        return "react";
    }
    
    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        AgentState state = AgentState.initial(request);
        
        return loop(state)
            .map(finalState -> buildResponse(finalState, request));
    }
    
    private Uni<AgentState> loop(AgentState state) {
        if (isTerminal(state)) {
            return Uni.createFrom().item(state);
        }
        
        return step(state)
            .chain(next -> loop(next));
    }
    
    private Uni<AgentState> step(AgentState state) {
        // Generate thought + action via LLM
        return generateThoughtAndAction(state)
            .chain(parsed -> {
                if (parsed.isFinalAnswer()) {
                    return Uni.createFrom().item(
                        state.withFinalAnswer(parsed.finalAnswer));
                }
                
                // Execute skill/tool
                return executeAction(state, parsed);
            });
    }
}
```

### Plan-and-Execute Orchestrator

```java
@ApplicationScoped
public class PlanAndExecuteOrchestrator implements AgentOrchestrator {
    
    @Inject
    InferenceService inferenceService;
    
    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        // Step 1: Generate plan
        return generatePlan(request)
            .chain(plan -> {
                // Step 2: Execute each step
                return executePlan(plan, request)
                    .map(results -> synthesize(plan, results, request));
            });
    }
    
    private Uni<List<String>> generatePlan(AgentRequest request) {
        String prompt = """
            Break down this task into numbered steps:
            %s
            
            Respond with format:
            1. [step 1]
            2. [step 2]
            ...
            """.formatted(request.getPrompt());
        
        return inferenceService.inferAsync(buildRequest(prompt))
            .map(response -> parsePlan(response.getContent()));
    }
    
    private Uni<List<ExecutionResult>> executePlan(
        List<String> plan, 
        AgentRequest request
    ) {
        return Multi.createFrom().iterable(plan)
            .onItem().transformConcat(step -> executeStep(step, request))
            .collect().asList();
    }
}
```

### Chain-of-Thought Orchestrator

```java
@ApplicationScoped
public class ChainOfThoughtOrchestrator implements AgentOrchestrator {
    
    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        String cotPrompt = """
            Let's think step by step.
            
            Question: %s
            
            Step 1:
            """.formatted(request.getPrompt());
        
        return inferenceService.inferAsync(buildRequest(cotPrompt))
            .map(response -> {
                String reasoning = response.getContent();
                String finalAnswer = extractFinalAnswer(reasoning);
                
                return AgentResponse.builder()
                    .answer(finalAnswer)
                    .reasoning(reasoning)
                    .strategy("chain-of-thought")
                    .build();
            });
    }
}
```

### Reflexion Orchestrator

```java
@ApplicationScoped
public class ReflexionOrchestrator implements AgentOrchestrator {
    
    @Inject
    AgentMemory memory;
    
    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        return attempt(request, 0)
            .chain(result -> {
                if (result.isSatisfactory()) {
                    return Uni.createFrom().item(result.toResponse());
                }
                
                // Critique and reflect
                return critique(result)
                    .chain(reflection -> {
                        // Store reflection for learning
                        await(memory.storeEpisode(reflection.toEpisode()));
                        
                        // Try again with improved approach
                        return attempt(request.withReflection(reflection), result.getAttempt() + 1);
                    });
            });
    }
    
    private Uni<AttemptResult> attempt(AgentRequest request, int attemptNum) {
        // Execute with current approach
        return executeWithApproach(request)
            .map(output -> evaluate(output, request));
    }
    
    private Uni<Reflection> critique(AttemptResult result) {
        String critiquePrompt = """
            Critique this attempt:
            
            Output: %s
            Expected: %s
            
            Identify errors and suggest improvements.
            """.formatted(result.getOutput(), result.getExpected());
        
        return inferenceService.inferAsync(buildRequest(critiquePrompt))
            .map(response -> parseReflection(response.getContent()));
    }
}
```

---

## Observability

### Metrics

```java
// Micrometer metrics exposed at /q/metrics

# Agent execution metrics
agent_execution_total{strategy="react",tenant="enterprise"} 156
agent_execution_duration_seconds{strategy="react"} 2.34
agent_steps_total{strategy="react"} 847
agent_skills_executed_total{skill="inference"} 523

# Memory metrics
agent_memory_working_entries{tenant="enterprise"} 45
agent_memory_conversation_sessions 128
agent_memory_vector_searches_total 1234
agent_memory_vector_search_duration_seconds 0.045

# Skill metrics
skill_execution_total{skill="rag",status="success"} 456
skill_execution_duration_seconds{skill="code-execution"} 1.23
skill_errors_total{skill="http-call"} 12

# Tool metrics
tool_calls_total{source="mcp",tool="filesystem"} 234
tool_calls_total{source="rest",tool="openai-api"} 567
tool_call_duration_seconds{source="cli"} 0.56
```

### Distributed Tracing

```java
// OpenTelemetry tracing with automatic span creation

@WithSpan("agent.execute")
public Uni<AgentResponse> execute(AgentRequest request) {
    Span.current().setAttribute("agent.strategy", request.getStrategy());
    Span.current().setAttribute("agent.max_steps", request.getMaxSteps());
    Span.current().setAttribute("tenant.id", request.getTenantId());
    
    return loop(initialState)
        .map(this::buildResponse);
}

@WithSpan("agent.step")
private Uni<AgentState> step(AgentState state) {
    Span.current().setAttribute("agent.step", state.getStep());
    
    return generateThoughtAndAction(state)
        .chain(parsed -> executeAction(state, parsed));
}

@WithSpan("skill.execute")
public Uni<SkillResult> execute(SkillContext context) {
    Span.current().setAttribute("skill.id", context.getSkillId());
    Span.current().setAttribute("skill.inputs", context.getInputs().toString());
    
    // ... skill execution
}
```

### Logging

```properties
# Structured logging configuration
quarkus.log.level=INFO
quarkus.log.category."tech.kayys.gollek.agent".level=DEBUG
quarkus.log.category."tech.kayys.gollek.agent".format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
quarkus.log.console.json=true
quarkus.log.console.json.pretty-print=true
```

**Example Log Output:**
```json
{
  "timestamp": "2024-03-25T10:15:30.123Z",
  "level": "INFO",
  "logger": "tech.kayys.gollek.agent.core.ReActOrchestrator",
  "message": "ReAct: starting run for tenant=enterprise, maxSteps=15",
  "runId": "run-abc123",
  "strategy": "react",
  "tenantId": "enterprise"
}
```

---

## Security

### Multi-Tenancy

```java
@Interceptor
@TenantScoped
public class TenantInterceptor {
    
    @AroundInvoke
    public Object intercept(InvocationContext context) {
        String tenantId = extractTenantId();
        
        if (tenantId == null) {
            throw new SecurityException("Missing tenant ID");
        }
        
        // Set tenant in context
        TenantContext.setCurrent(tenantId);
        
        try {
            return context.proceed();
        } finally {
            TenantContext.clear();
        }
    }
    
    private String extractTenantId() {
        // Extract from header, JWT, or API key
        RestRequest request = RestRequest.current();
        return request.getHeader("X-Tenant-Id");
    }
}
```

### Skill Access Control

```java
@ApplicationScoped
public class SkillAccessManager {
    
    @Inject
    TenantConfigRepository tenantConfigRepo;
    
    public boolean canAccessSkill(String tenantId, String skillId) {
        TenantConfig config = tenantConfigRepo.findByTenant(tenantId);
        
        if (config.getAllowedSkills().equals("*")) {
            return true;
        }
        
        return config.getAllowedSkills().contains(skillId);
    }
    
    public boolean canUseTool(String tenantId, ToolSource toolSource) {
        TenantConfig config = tenantConfigRepo.findByTenant(tenantId);
        
        return config.getAllowedToolSources().contains(toolSource);
    }
}
```

### Input Validation

```java
public class SkillInputValidator {
    
    public static void validate(SkillDescriptor descriptor, Map<String, Object> inputs) {
        for (SkillInput inputDef : descriptor.inputs()) {
            if (inputDef.required() && !inputs.containsKey(inputDef.name())) {
                throw new SkillValidationException(
                    "Missing required input: " + inputDef.name());
            }
            
            if (inputs.containsKey(inputDef.name())) {
                validateType(inputDef.name(), inputDef.type(), inputs.get(inputDef.name()));
            }
        }
    }
    
    private static void validateType(String name, String expectedType, Object value) {
        // Type validation logic
    }
}
```

---

## Performance Optimization

### Caching

```java
@ApplicationScoped
public class SkillExecutionCache {
    
    @CacheResult(cacheName = "skill-results")
    public Uni<SkillResult> executeWithCache(
        @CacheKey String skillId,
        @CacheKey Map<String, Object> inputs
    ) {
        // Execute skill
    }
    
    @CacheInvalidate(cacheName = "skill-results")
    public void invalidateCache(String skillId, Map<String, Object> inputs) {
        // Cache invalidation
    }
}
```

### Batching

```java
@ApplicationScoped
public class BatchSkillExecutor {
    
    @Inject
    BatchScheduler scheduler;
    
    public Uni<SkillResult> execute(SkillContext context) {
        // Batch multiple skill calls for efficiency
        return scheduler.schedule(context.getSkillId(), context)
            .onItem().transform(batchResult -> batchResult.getResults()
                .get(context.getRequestId()));
    }
}
```

### Connection Pooling

```yaml
# HTTP connection pooling for REST tools
quarkus:
  rest-client:
    connect-timeout: 5000
    read-timeout: 30000
    pool-size: 20
    max-connections: 100
```

---

## Testing

### Unit Tests

```java
@QuarkusTest
class ReActOrchestratorTest {
    
    @Inject
    ReActOrchestrator orchestrator;
    
    @InjectMock
    DefaultSkillRegistry skillRegistry;
    
    @Test
    void testReActExecution() {
        AgentRequest request = AgentRequest.builder()
            .prompt("What is 2 + 2?")
            .maxSteps(5)
            .build();
        
        AgentResponse response = orchestrator.execute(request)
            .await().indefinitely();
        
        assertTrue(response.isSuccessful());
        assertNotNull(response.getAnswer());
        assertTrue(response.getTotalSteps() <= 5);
    }
}
```

### Integration Tests

```java
@QuarkusTest
class AgentIntegrationTest {
    
    @Inject
    AgentOrchestrator orchestrator;
    
    @Inject
    SkillRegistry skillRegistry;
    
    @Test
    void testFullAgentRun() {
        AgentRequest request = AgentRequest.builder()
            .prompt("Search for recent AI news and summarize")
            .strategy("react")
            .skills("web-search", "summarization")
            .maxSteps(10)
            .build();
        
        AgentResponse response = orchestrator.execute(request)
            .await().indefinitely();
        
        assertTrue(response.isSuccessful());
        assertTrue(response.getAnswer().length() > 0);
        assertTrue(response.getTotalSteps() > 0);
    }
    
    @Test
    void testStreaming() {
        AgentRequest request = AgentRequest.builder()
            .prompt("Count from 1 to 5")
            .stream(true)
            .build();
        
        List<AgentEvent> events = orchestrator.stream(request)
            .collect().asList()
            .await().indefinitely();
        
        assertTrue(events.stream()
            .anyMatch(e -> e.getType() == AgentEventType.STARTED));
        assertTrue(events.stream()
            .anyMatch(e -> e.getType() == AgentEventType.FINAL_ANSWER));
    }
}
```

### Load Tests

```java
@PerformanceTest
class AgentLoadTest {
    
    @Test
    @LoadTest(threads=50, duration="5m")
    void testConcurrentAgents() {
        AgentRequest request = AgentRequest.builder()
            .prompt("Simple calculation: 10 * 10")
            .maxSteps(3)
            .build();
        
        orchestrator.execute(request)
            .subscribe().with(response -> {
                assertTrue(response.isSuccessful());
            });
    }
}
```

---

## Troubleshooting

### Agent Not Responding

**Symptoms:** Agent hangs or times out

**Solutions:**
1. Check LLM provider health: `GET /q/health`
2. Verify skill registry: `GET /api/v1/agents/skills`
3. Check memory backend connectivity
4. Review logs for errors: `tail -f logs/gollek.log`
5. Increase timeout: `gollek.agent.default-timeout-seconds`

### Skill Not Found

**Symptoms:** `SkillNotFoundException`

**Solutions:**
1. Verify skill is registered: `skillRegistry.listAll()`
2. Check ServiceLoader configuration
3. Verify plugin JAR in correct directory
4. Check skill ID matches exactly (case-sensitive)
5. Review skill scan packages in config

### Memory Backend Unavailable

**Symptoms:** Connection refused to Redis/Qdrant

**Solutions:**
1. Check backend service status
2. Verify connection URL in config
3. Check network connectivity
4. Fallback to in-memory: `gollek.agent.memory.backend=in-memory`

### Excessive Token Usage

**Symptoms:** High LLM costs, slow execution

**Solutions:**
1. Reduce `max-steps` in config
2. Lower `max-tokens-per-step`
3. Use more specific skill descriptions
4. Implement early termination conditions
5. Cache frequent responses

---

## Resources

### Documentation
- **Main Docs:** https://gollek-ai.github.io/docs/agents
- **Skill Guide:** https://gollek-ai.github.io/docs/skills
- **Tool Integration:** https://gollek-ai.github.io/docs/tools
- **Memory System:** https://gollek-ai.github.io/docs/memory

### Source Code
- **Agent Core:** `inference-gollek/plugins/agent/agent-core/`
- **Built-in Skills:** `inference-gollek/plugins/agent/agent-core/src/main/java/tech/kayys/gollek/agent/skills/`
- **Example Agents:** `inference-gollek/examples/agents/`

### Community
- **GitHub:** https://github.com/gollek-ai/gollek
- **Discussions:** https://github.com/gollek-ai/gollek/discussions
- **Issues:** https://github.com/gollek-ai/gollek/issues

---

**Last Updated:** March 2026  
**Version:** 2.0.0  
**Status:** ✅ Production Ready

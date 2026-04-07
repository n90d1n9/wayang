# Gollek Agent System — SKILL.md

**Version:** 2.0.0
**Platform:** Gollek Inference Engine (Quarkus 3.x / Multi-Tenant)
**Author:** Gollek Team
**Status:** ✅ Production Ready

---

## Overview

The Gollek Agent System is a **skills-based, memory-enabled, tool-driven agentic framework** built on top of the Gollek inference engine. It enables composing, orchestrating, and deploying intelligent agents that can:

- Execute **multi-step reasoning loops** (ReAct, Plan-and-Execute, Chain-of-Thought, Reflexion)
- Call **registered skills and external tools** during inference with dynamic discovery
- Maintain **four-layer memory** (working, conversation, vector, episodic) across sessions
- Route requests across **multiple LLM providers and model formats**
- Operate in **multi-tenant** enterprise environments with isolation and quotas
- Integrate with **external tools** via MCP, REST APIs, CLI commands, and gRPC

---

## Architecture: Skills-Based Agent Design

Every agent capability is encapsulated as a **Skill** — a self-contained, versioned, discoverable unit of work. Skills are described in `skill.json` / `SKILL.md` sidecars, registered at startup, and exposed to the reasoning loop as callable tools.

```
┌──────────────────────────────────────────────────────────┐
│                   Gollek Agent System                    │
│                                                          │
│  ┌──────────────┐   ┌────────────────┐  ┌────────────┐  │
│  │  AgentRequest│──▶│ AgentOrchestra-│──▶│   Memory   │  │
│  └──────────────┘   │     tor        │  │  Manager   │  │
│                     │  (ReAct/P&E)   │  └────────────┘  │
│                     └───────┬────────┘                  │
│                             │ SkillCall                  │
│                     ┌───────▼────────┐                  │
│                     │  SkillRegistry │                  │
│                     │ ┌────────────┐ │                  │
│                     │ │ Inference  │ │                  │
│                     │ │   Skill    │ │  ← Built-in       │
│                     │ ├────────────┤ │                  │
│                     │ │    RAG     │ │  ← Built-in       │
│                     │ ├────────────┤ │                  │
│                     │ │    Code    │ │  ← Built-in       │
│                     │ ├────────────┤ │                  │
│                     │ │  Custom    │ │  ← Plugin         │
│                     │ └────────────┘ │                  │
│                     └────────────────┘                  │
└──────────────────────────────────────────────────────────┘
```

---

## Skill Contract

Every skill must implement `AgentSkill` and provide a descriptor:

```java
@SkillDescriptor(
    id          = "inference",
    name        = "LLM Inference",
    description = "Execute natural language inference via configured LLM providers",
    version     = "1.0.0",
    category    = SkillCategory.REASONING,
    triggers    = {"infer", "generate", "llm", "ask model"},
    inputs      = { @SkillInput(name="prompt", type="string", required=true) },
    outputs     = { @SkillOutput(name="response", type="string") }
)
public class InferenceSkill implements AgentSkill { ... }
```

### Skill Descriptor (skill.json)
```json
{
  "id": "inference",
  "name": "LLM Inference",
  "description": "Execute natural language inference via configured LLM providers",
  "version": "1.0.0",
  "category": "REASONING",
  "triggers": ["infer", "generate", "llm", "ask model"],
  "inputs": [
    { "name": "prompt",   "type": "string",  "required": true  },
    { "name": "model",    "type": "string",  "required": false },
    { "name": "maxTokens","type": "integer", "required": false }
  ],
  "outputs": [
    { "name": "response",    "type": "string"  },
    { "name": "tokensUsed",  "type": "integer" },
    { "name": "durationMs",  "type": "long"    }
  ],
  "config": {
    "timeout": "PT30S",
    "retryable": true,
    "maxRetries": 2
  }
}
```

---

## Built-in Skills Catalog

| ID                  | Category    | Description                                           |
|---------------------|-------------|-------------------------------------------------------|
| `inference`         | REASONING   | Direct LLM inference via Gollek providers             |
| `rag`               | RETRIEVAL   | Retrieval-Augmented Generation with vector store      |
| `code-execution`    | EXECUTION   | Sandboxed code execution (Python/Java/JS)             |
| `web-search`        | RETRIEVAL   | Web search and content extraction                     |
| `data-analysis`     | ANALYTICS   | Tabular data analysis and transformation              |
| `document-qa`       | RETRIEVAL   | QA over uploaded documents                           |
| `summarization`     | REASONING   | Multi-document summarization                          |
| `embedding`         | VECTOR      | Generate vector embeddings via local models           |
| `model-conversion`  | TOOLING     | Convert model formats (SafeTensors→GGUF etc.)         |
| `memory-store`      | MEMORY      | Store/retrieve from long-term agent memory            |
| `http-call`         | INTEGRATION | Make authenticated HTTP API calls                     |
| `sql-query`         | DATA        | Execute SQL queries against configured datasources    |

---

## Orchestration Strategies

### 1. ReAct (Reason + Act)
```
Thought → Action(SkillCall) → Observation → Thought → ... → FinalAnswer
```
Best for: open-ended tasks, tool-heavy reasoning

### 2. Plan-and-Execute
```
Plan[step1, step2, step3] → Execute(step1) → Execute(step2) → Synthesize
```
Best for: structured tasks with known sub-goals

### 3. Chain-of-Thought
```
CoT[reasoning steps] → FinalAnswer
```
Best for: mathematical/logical reasoning, no external tools needed

---

## Memory Architecture

```
┌─────────────────────────────────────────────┐
│              Agent Memory System            │
│                                             │
│  ┌─────────────────┐  ┌──────────────────┐  │
│  │  Working Memory │  │ Conversation     │  │
│  │  (current turn) │  │ Memory (history) │  │
│  └─────────────────┘  └──────────────────┘  │
│  ┌─────────────────┐  ┌──────────────────┐  │
│  │  Vector Memory  │  │ Episodic Memory  │  │
│  │  (semantic)     │  │ (past sessions)  │  │
│  └─────────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────┘
```

---

## Multi-Tenant Agent Configuration

```yaml
gollek:
  agent:
    enabled: true
    default-orchestrator: react
    max-steps: 20
    timeout: PT60S
    
    skills:
      auto-discover: true
      scan-packages:
        - tech.kayys.gollek.agent.skills
        - com.mycompany.skills
      
    memory:
      conversation:
        max-history: 50
        backend: in-memory      # in-memory | redis | postgres
      vector:
        enabled: false
        provider: qdrant
        endpoint: http://localhost:6333
    
    tenants:
      default:
        max-concurrent-agents: 10
        allowed-skills: "*"
        max-steps: 15
      premium:
        max-concurrent-agents: 50
        allowed-skills: "*"
        max-steps: 30
```

---

## Usage — Programmatic

```java
@Inject AgentOrchestrator orchestrator;

AgentRequest request = AgentRequest.builder()
    .prompt("Analyze the CSV file and find top 5 customers by revenue")
    .strategy(OrchestrationStrategy.REACT)
    .skill("data-analysis")
    .skill("summarization")
    .context("file", csvContent)
    .maxSteps(10)
    .tenantId("enterprise-tenant")
    .build();

Uni<AgentResponse> response = orchestrator.execute(request);
```

---

## Usage — REST API

```http
POST /api/v1/agents/run
Content-Type: application/json
X-Tenant-ID: enterprise

{
  "prompt": "Search for recent AI papers and summarize key findings",
  "strategy": "react",
  "skills": ["web-search", "summarization"],
  "maxSteps": 15,
  "stream": true
}
```

---

## Plugin Development Guide

1. Implement `AgentSkill` interface
2. Annotate with `@SkillDescriptor`
3. Add `skill.json` to `META-INF/skills/`
4. Register in `META-INF/services/tech.kayys.gollek.agent.spi.AgentSkill`
5. Deploy as Quarkus extension or fat JAR on classpath

---

## Performance & Observability

- Every skill execution emits Micrometer metrics: `agent.skill.duration`, `agent.skill.errors`
- OpenTelemetry traces span the full reasoning loop with per-step spans
- Agent audit log records all tool calls, inputs, outputs for compliance
- Health endpoint: `GET /q/health/agent` reports skill registry health

---

## Security

- Skills are tenant-scoped — tenants only see allowed skills
- Skill inputs are validated against the descriptor schema before execution
- Code execution runs in an isolated JVM sandbox (no filesystem/network by default)
- All agent traces are signed for audit integrity


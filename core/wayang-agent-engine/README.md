# 🎶 Gamelan CLI

**Agentic AI CLI for local software development** — powered by the [Gollek inference engine](https://github.com/kayys/gollek) and the [agentskills.io](https://agentskills.io/specification) skills system.

Inspired by Claude Code. Built with Quarkus + Java 21.

---

## Three-Tier Agent Architecture

```
User Task
    │
    ▼
OrchestratorSelector ─── auto-selects the right tier
    │
    ├── Tier 1: DirectCallOrchestrator   (single LLM call, no tools)
    │     Use for: classification, summarisation, translation, Q&A
    │
    ├── Tier 2: SingleAgentOrchestrator  (ReAct loop with tools)
    │     Use for: file ops, debugging, refactoring, most coding tasks
    │     Variant: ReflexionOrchestrator (generate → evaluate → revise)
    │
    └── Tier 3: MultiAgentOrchestrator   (parallel workers + synthesis)
          Use for: cross-domain review, parallel specialisation
```

---

## Quick Start

### Prerequisites
- Java 21+, Maven 3.9+
- Gollek inference engine running locally or remotely

### Build & Run
```bash
# Dev mode (hot reload)
mvn quarkus:dev

# Production JVM build
mvn package && java -jar target/quarkus-app/quarkus-run.jar

# Native binary (requires GraalVM)
mvn package -Pnative && ./target/gamelan-cli-runner
```

---

## Key Commands

```bash
# Interactive REPL
gamelan                                      # auto-start REPL
gamelan chat                                 # explicit REPL

# One-shot tasks
gamelan run "fix the NPE in UserService"     # auto-select strategy
gamelan run "classify: is this a bug?" --strategy direct
gamelan run "full code review" --strategy multi
gamelan run "task" --agui                    # AG-UI event stream

# Workflows
gamelan workflow review src/                 # parallel code review
gamelan workflow refactor UserService.java
gamelan workflow document src/api/
gamelan workflow test src/

# File watching
gamelan watch src/ --on-change "run tests for {file}"
gamelan watch . --glob "*.py" --strategy direct --on-change "summarise {file}"

# Skills
gamelan skill list
gamelan skill install ./my-skill/
gamelan skill validate ./my-skill/

# Memory
gamelan memory list
gamelan memory add "test-cmd" "mvn test -pl auth"
gamelan memory search "database"

# Checkpoints
gamelan checkpoint list
gamelan checkpoint resume abc123
gamelan checkpoint delete abc123

# Models
gamelan models list
gamelan models pull qwen2-7b

# Config
gamelan config list
gamelan config set model qwen2-7b
```

---

## REPL Commands

| Command | Description |
|---------|-------------|
| `/strategy [name]` | Show or set orchestration strategy (auto\|direct\|react\|reflexion\|multi) |
| `/model [name]` | Show or switch the active model |
| `/models` | List available local models |
| `/skills` | List loaded skills |
| `/stats` | Token + LLM call usage for this session |
| `/metrics` | Per-tool latency and error metrics |
| `/budget` | Context window usage and advisory |
| `/memory [query]` | Show remembered project facts |
| `/context` | Show detected project context |
| `/agui` | Toggle AG-UI event rendering |
| `/clear` | Clear history and reset counters |
| `/compact` | AI-powered history summarisation |
| `/session` | Session stats |
| `/exit` | Quit (also Ctrl+D) |

---

## Project Context Detection

Gamelan automatically detects at startup:
- **Build system**: Maven, Gradle, npm, Cargo, Go, pip, Mix, CMake
- **Primary language**: by file extension count
- **Framework**: Quarkus, Spring, React, FastAPI, Django, NestJS, etc.
- **Git branch** and last 5 commits
- **README** excerpt (first 800 chars)
- **Directory tree** (top-level, depth 2)

This context is injected into every system prompt so the agent always knows what project it's in.

---

## Persistent Memory

The agent remembers facts across sessions via the `REMEMBER:` protocol:

```
REMEMBER: test-command = mvn test -pl auth-service
REMEMBER: code-style = Google Java Style, 2-space indent
REMEMBER: db-migration-tool = Flyway
```

Memories are stored per-project in `~/.gamelan/memory/<project>.json`.

---

## AG-UI Protocol

Gamelan implements the [AG-UI protocol](https://docs.ag-ui.com) for streaming agent events:

```bash
# CLI — renders AG-UI events in the terminal
gamelan run "task" --agui

# HTTP SSE — wire to a frontend
runner.run(request, "react", event -> response.write(event.toSseFrame()))
```

Events: `RUN_STARTED`, `TEXT_MESSAGE_CONTENT` (per-token), `TOOL_CALL_START/END`, `STATE_SNAPSHOT`, `RUN_FINISHED`.

---

## Bundled Skills (10)

| Skill | Activates when |
|-------|---------------|
| `analyze-code` | code review, explain, investigate |
| `debug-code` | errors, exceptions, test failures |
| `explain-code` | understand, walk through, describe |
| `git-workflow` | git, commit, branch, PR |
| `test-generation` | write tests, add coverage |
| `refactor-code` | refactor, clean up, simplify |
| `api-design` | REST API, endpoints, schema |
| `read-file` | reading/viewing files |
| `write-file` | creating/editing files |
| `run-command` | shell commands, builds, scripts |

---

## Architecture

| Component | Class | Responsibility |
|-----------|-------|----------------|
| **Orchestrator Selector** | `OrchestratorSelector` | Auto-selects the right tier |
| **Direct Orchestrator** | `DirectCallOrchestrator` | Tier 1: single LLM call |
| **Single Agent** | `SingleAgentOrchestrator` | Tier 2: ReAct loop with tools |
| **Reflexion** | `ReflexionOrchestrator` | Tier 2: generate→evaluate→revise |
| **Multi-Agent** | `MultiAgentOrchestrator` | Tier 3: parallel workers |
| **Prompt Builder** | `PromptBuilder` | Assembles 6-layer system prompt |
| **Tool Call Parser** | `ToolCallParser` | Parses `<tool_call>` XML blocks |
| **Tool Executor** | `ToolExecutor` | CDI-dispatches tool calls |
| **Skill Registry** | `SkillRegistry` | Discovers and caches skills |
| **Project Context** | `ProjectContext` | Auto-detects project metadata |
| **Agent Memory** | `AgentMemory` | Cross-session fact persistence |
| **Token Budget** | `TokenBudgetAdvisor` | Context window warnings |
| **Compactor** | `ConversationCompactor` | AI-powered history summarisation |
| **Checkpoints** | `CheckpointManager` | Save/resume long-running tasks |
| **AG-UI Runner** | `AguiAgentRunner` | AG-UI protocol event emission |
| **Workflow Engine** | `GamelanWorkflowEngine` | Parallel/sequential/map-reduce |

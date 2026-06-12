# Agent Hermes Mode

`agent-hermes` adds a Wayang-native mode inspired by Nous Research Hermes Agent:

- persistent memory prompt assembly
- dynamic skill learning and refinement
- MCP/toolset-aware operating posture
- scheduled/background and gateway-ready metadata
- parallel sub-agent posture through the existing `AgentOrchestrator` SPI

The module does not fork or embed Hermes Agent. It keeps Wayang backend-agnostic by
decorating an existing `AgentOrchestrator`, enriching the request, then recording a
learned `SkillDefinition` through the existing configurable skill-management
stores. Database, file, S3-compatible, and hybrid persistence stay owned by
`skill-management`.

## Runtime configuration

Hermes mode reads runtime configuration from MicroProfile Config when available,
or from system properties/environment variables in plain Java runtimes.

Property prefix: `wayang.agent.hermes.`
Environment prefix: `WAYANG_AGENT_HERMES_`

Common keys:

- `skill-learning-enabled`
- `skill-self-improvement-enabled`
- `persistent-memory-enabled`
- `mcp-enabled`
- `gateway-enabled`
- `cron-enabled`
- `sub-agents-enabled`
- `prefer-local-providers`
- `require-tool-calling`
- `min-steps-to-learn`
- `max-skill-procedure-steps`
- `max-sub-agents`
- `memory-entry-limit`
- `preferred-provider`
- `fallback-provider`
- `default-toolsets`
- `gateway-platforms`
- `execution-backends`
- `persistence-hints.definitions`
- `persistence-hints.artifacts`
- `persistence-hints.fallback`

Example:

```properties
wayang.agent.hermes.skill-learning-enabled=true
wayang.agent.hermes.preferred-provider=ollama
wayang.agent.hermes.default-toolsets=skills,memory,mcp,rag
wayang.agent.hermes.persistence-hints.definitions=database
wayang.agent.hermes.persistence-hints.artifacts=s3
wayang.agent.hermes.persistence-hints.fallback=file-system
```

## Runtime capability manifest

`HermesAgentModeConfig.runtimeCapabilities()` exposes the effective mode contract
after config switches are applied. The manifest filters disabled features and
toolsets, exposes gateway platforms/execution backends, and is attached to:

- request context as `hermesCapabilities`
- inference metadata as `capabilities`
- recommended orchestrator parameters as `capabilities`

Use this manifest from gateway, scheduler, sub-agent, and execution-backend
adapters instead of parsing raw config flags.

`HermesSkillPersistenceStrategy` turns flexible persistence hints into a typed
contract for learned skills: definition store, portable artifact store, fallback
store, cloud/object-store providers, and hybrid/database/file capability flags.
Keep storage provider implementation in `skill-management`; Hermes only advertises
and consumes the strategy.

`HermesExecutionPlanner` turns configured execution backends into an advisory
`HermesExecutionPlan` for each request. It respects explicit backend hints,
prefers isolated backends for sandboxed work, routes remote/serverless work when
available, and attaches the selected plan to request context/parameters for
runtime adapters.

`HermesGatewayContextResolver` normalizes gateway metadata from CLI, Telegram,
Discord, Slack, WhatsApp, Signal, and adjacent adapters into one
`HermesGatewayContext`. It preserves platform, channel/thread/conversation,
message, tenant/session/user, support status, and a continuity key for
always-on routing.

`HermesAutomationIntentResolver` turns explicit cron/RRULE/interval metadata and
recurring natural-language prompts into a typed `HermesAutomationIntent`. It is
advisory: scheduler adapters can consume the intent later, while Hermes keeps
cron enablement, task text, timezone, source, and disabled-state reasons visible
in request and inference metadata.

`HermesDelegationPlanner` turns explicit sub-agent/fan-out hints and parallel
workstream prompts into a typed `HermesDelegationPlan`. It recommends isolated
lanes, clamps fan-out to `max-sub-agents`, and leaves concrete sub-agent spawning
to the orchestration layer.

`HermesProviderRoutingResolver` turns provider/model hints, local-provider
preference, API-gateway preference, and high-context needs into a typed
`HermesProviderRoutingPlan`. Provider adapters can consume it while Hermes stays
decoupled from Ollama, vLLM, OpenRouter, Nous Portal, or any future backend.

`HermesMemoryReflectionResolver` turns explicit memory-reflection hints and
memory-oriented prompts into a typed `HermesMemoryReflectionPlan`. It captures
scope, cadence, priority, source, and disabled-state reasons while leaving
long-term memory writes to memory adapters.

`HermesTrajectoryExportResolver` turns explicit export hints and audit/trace
prompts into a typed `HermesTrajectoryExportPlan`. It captures format,
destination, prompt/tool-call inclusion, redaction, source, and disabled-state
reasons while leaving concrete exporters to observability adapters.

`HermesRequestPlanner` aggregates execution, gateway, automation, delegation,
provider routing, memory reflection, and trajectory export plans into one
request-level contract. Prompt assembly and orchestrator metadata consume this
aggregate so new sidecar plans can be added behind a single planning boundary.

## Learning policy

`HermesLearningPolicy` owns the decision to learn, force-learn, or skip a run.
The learning loop remains focused on distilling, validating, and persisting
skills. Skip reasons are explicit, so callers and tests can distinguish failed
runs, below-threshold runs, disabled learning, and user-requested skips.

`HermesLearnedSkillRepository` owns the persistence boundary for learned skills:
validation, portable `SKILL.md` artifact writes, and definition create/update.
It writes the artifact before activating a skill definition so self-learned
skills remain portable across database, file, object-storage, and hybrid stores.

`HermesSkillMarkdownRenderer` owns the agentskills-compatible `SKILL.md`
format, keeping artifact rendering separate from `HermesSkillDistiller`, which
only builds/refines runtime `SkillDefinition` objects.

`HermesSkillPromptRenderer` owns learned-skill procedure and refinement prompt
formatting. The distiller uses it when building runtime skill definitions, so
prompt shape can evolve independently from identity, metadata, and persistence.

`HermesSkillIdentityFactory` owns deterministic learned-skill ids, names, and
descriptions. This keeps future dedupe, collision, and catalog naming policy out
of the distiller.

`HermesSkillReusePolicy` scores existing learned skills before new skill
creation. Exact id matches still refine directly; similar learned skills can be
reused and refined instead of creating noisy near-duplicates.

`HermesLearningSignalFactory` owns extraction of learning signals from completed
agent runs. This keeps `HermesLearningSignal` as normalized data while future
gateway, scheduler, and sub-agent metadata shaping can evolve in one boundary.

Primary references:

- https://hermes-agent.nousresearch.com/docs/
- https://hermes-agent.nousresearch.com/docs/developer-guide/architecture
- https://hermes-agent.nousresearch.com/docs/user-guide/features/memory
- https://hermes-agent.nousresearch.com/docs/user-guide/features/skills
- https://hermes-agent.nousresearch.com/docs/user-guide/features/mcp

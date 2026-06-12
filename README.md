# Wayang Gollek

Wayang Gollek is the backend-agnostic agentic core for the Wayang platform. It
keeps agent contracts, orchestration, skills, tools, memory, RAG, MCP bridges,
guardrails, HITL, vector stores, and runtime adapters in one Maven reactor while
keeping concrete inference and workflow engines behind SDK/SPI boundaries.

## Architecture

The active reactor is organized around these layers:

- `agent/agent-spi`: backend-neutral contracts for agents, inference backends,
  workflow backends, tools, memory, audit, and dynamic skills.
- `agent/agent-core`: core agent services, registries, skill loading,
  tool selection, memory integration, resilience, and security.
- `agent/agent-backend-gollek`: adapter from the agent inference SPI to the
  Gollek SDK.
- `agent/agent-backend-gamelan`: adapter from the workflow SPI to the Gamelan
  SDK.
- `agent/agent-shaker`: harness and packaging support for minimal agent
  artifacts.
- `tools/`: tool SPI, OpenAPI/MCP/UTCP adapters, sandboxed execution, and tool
  runtime modules.
- `rag/`: RAG core, runtime, retrieval/config/embedding/SLO modules, and
  pluggable RAG pipeline extensions.
- `memory/`, `vector/`, `embedding/`, `graph/`: context, storage, retrieval,
  and search substrate.
- `guardrails/`, `hitl/`, `prompt/`: policy, approval, and prompt/runtime
  services.
- `runtime-quarkus`: thin Quarkus wiring over the pure Java core and backend
  adapters.
- `storage/`: Gollek model-storage plugins kept in the reactor but isolated
  behind Gollek SPI dependencies.

## Dependency Policy

Wayang Gollek must remain SDK/SPI-first:

- Use `gamelan-engine-spi`, `gamelan-sdk-client-core`, and
  `gamelan-sdk-executor-core` for workflow integration.
- Do not depend on `gamelan-engine-core` from framework modules.
- Use `gollek.version` for Gollek SDK/SPI coordinates.
- Keep runtime-specific wiring in runtime modules, not in `agent-spi`.

The parent POM enforces the `gamelan-engine-core` ban during Maven validation.

## Current Build Baseline

From this directory:

```bash
mvn -q validate -DskipTests
mvn -q -pl agent/agent-spi,tools/tools-spi -am compile -DskipTests
```

Both commands are expected to pass for the reorganized base. A focused
`agent-core` compile still exposes remaining package migration work from the
older consolidation: several files reference pre-consolidation packages such as
root-level skill SPI types, `agent.core.AgentResponse`, and old tool SPI
packages. See `docs/REORGANIZATION_STATUS.md`.

## Build Editions

The default `wayang-gollek` reactor is the community build. Pro/enterprise
add-ons are opt-in Maven profile modules so they can be tested, packaged, and
licensed separately from the default community surface.

```bash
mvn -q validate
mvn -q -Ppro-enterprise-addons -pl a2ui/a2ui-core,a2ui/a2ui-wayang -am test
```

The `pro-enterprise-addons` profile currently contributes the A2UI plugin
modules. Provider capability discovery advertises the same boundary with
`metadata.activationProfile=pro-enterprise-addons` and
`metadata.defaultCommunity=false`.

## HTTP Diagnostics

A2UI and A2A adapters expose dependency-free HTTP-shaped diagnostics for route
catalogs, binding reports, smoke probes, and readiness checks. A2UI is packaged
as a pro/enterprise add-on plugin and is not part of the default community
reactor. See
`docs/HTTP_DIAGNOSTICS_PROBES.md` for endpoints, configurable A2A paths, probe
pass/fail semantics, golden fixtures, and focused verification commands.

The main adapter slices are:

```bash
mvn -q -Ppro-enterprise-addons -pl a2ui/a2ui-core,a2ui/a2ui-wayang -am test
mvn -q -pl a2a/a2a-core,a2a/a2a-wayang -am test
```

## CLI Command Discovery

The packaged Wayang CLI exposes SDK-owned command metadata for agent shells:

```bash
wayang commands
wayang commands --surface assistant-agent --json
wayang commands --profile low-code-agent --category Runs --index --json
wayang commands --index --json
wayang commands --category "Run Specs"
wayang commands --id run-print-spec-output --json
wayang commands --contract-json-schema-id urn:wayang:contract:wayang.run.planning:v1:run-preview --json
wayang commands --contract-json-schema-id urn:wayang:contract:wayang.platform.catalog:v1:profile-list --index --json
wayang commands --contract-json-schema-id urn:wayang:contract:wayang.standard.alignment:v1:standard-alignment-health --index --json
wayang commands --contract-json-schema-id urn:wayang:contract:wayang.standard.catalog:v1:standards-catalog --index --json
wayang commands --contract-json-schema-id urn:wayang:contract:wayang.skill.catalog:v1:skill-discovery --index --json
wayang commands --contract-json-schema-id urn:wayang:contract:wayang.contract.coverage:v1:contract-command-coverage --index --json
wayang commands --contract-json-schema-id urn:wayang:contract:wayang.command.discovery:v1:commands-discovery --index --json
wayang commands --contract-json-schema-id urn:wayang:contract:wayang.workbench.discovery:v1:workbench-discovery --index --json
wayang workbench --contract-json-schema-id urn:wayang:contract:wayang.run.planning:v1:run-preview --json
wayang contracts --json
wayang contracts --index --json
wayang contracts --envelope run-preview --schema-json
wayang contracts --json-schema-id urn:wayang:contract:wayang.platform.catalog:v1:profile-list --schema-json
wayang contracts --json-schema-id urn:wayang:contract:wayang.standard.alignment:v1:standard-alignment-health --schema-json
wayang contracts --json-schema-id urn:wayang:contract:wayang.standard.catalog:v1:standards-catalog --schema-json
wayang contracts --json-schema-id urn:wayang:contract:wayang.skill.catalog:v1:skill-discovery --schema-json
wayang contracts --json-schema-id urn:wayang:contract:wayang.contract.coverage:v1:contract-command-coverage --schema-json
wayang contracts --json-schema-id urn:wayang:contract:wayang.command.discovery:v1:commands-discovery --schema-json
wayang contracts --json-schema-id urn:wayang:contract:wayang.workbench.discovery:v1:workbench-discovery --schema-json
wayang contracts --domain planning --schema-bundle-json
wayang contracts --check --json
wayang contracts --coverage --json
wayang contracts --command-id run-dry-json --json
wayang contracts --json-schema-id urn:wayang:contract:wayang.run.planning:v1:run-preview --json
wayang contracts --domain lifecycle --json
wayang contracts --schema wayang.run.planning --json
wayang contracts --envelope run-preview --json
wayang standards
wayang standards --json
wayang standards --catalog
wayang standards --catalog --json
wayang workbench --surface assistant-agent --category Runs --id run-session-context --json
wayang workbench --profile low-code-agent --category Runs --json
wayang skills
wayang skills list --surface assistant-agent --source rag --json
wayang skills list --profile low-code-agent --json
wayang skills inspect rag.retrieve --json
wayang skills search rag --surface assistant-agent --json
wayang skills search gamelan --profile low-code-agent --json
wayang run "Plan the next RAG evaluation" --json
wayang run inspect <run-id> --json
wayang run events <run-id> --json
wayang run events <run-id> --state completed --limit 20 --json
wayang run events <run-id> --after-sequence 10 --limit 20 --json
wayang run events <run-id> --follow --json
wayang run events <run-id> --follow --follow-result --json
wayang run events <run-id> --follow --follow-result-only --json
wayang run events <run-id> --follow --follow-result-only --stats --json
wayang run events <run-id> --stats --json
wayang run list --state completed --limit 10 --json
wayang run list --offset 10 --limit 10 --json
wayang run stats --state completed --json
wayang run list --tenant <id> --surface assistant-agent --json
wayang run list --profile low-code-agent --json
wayang run stats --profile low-code-agent --json
wayang run wait <run-id> --timeout-seconds 30 --json
wayang run cancel <run-id> --reason "user stop" --json
wayang --run-store .wayang/runs.properties run list --state completed --json
wayang --run-store .wayang/runs.properties run forget <run-id> --json
```

Use `--surface` to scope recommendations to a product surface, `--profile` to
scope through reusable product defaults, `--category` for one command family,
`--id` when another shell needs one stable command entry, and
`--contract-json-schema-id` when a shell needs the commands that produce one
stable JSON Schema contract. Profile-scoped JSON includes both `profileId` and
`resolvedSurfaceId` so shells can preserve
the product profile choice while rendering surface-compatible actions. Skill
profile filters resolve the profile's default skill bundle, for example
`low-code-agent` resolves to workflow, HITL, and observability skills. The same
query can filter either the command catalog or the full workbench payload. SDK
callers can use `discoverCommands(WorkbenchCommandQuery)`,
`discoverCommandsForProfile(...)`, `discoverCommandsForContractJsonSchemaId(...)`,
`workbench(WorkbenchCommandQuery)`, `workbenchForProfile(...)`, or
`workbenchForContractJsonSchemaId(...)` for the normalized lookup path. For richer shell
integrations, `commandDiscovery(WorkbenchCommandQuery)` also returns the
normalized query, command ids, categories, category summaries, category counts,
match counts, contract JSON Schema id facets, contract summaries, and
per-command contract references for JSON-producing commands.

JSON payloads that are consumed by agent shells are guarded by golden contract
fixtures under `wayang-gollek-cli/src/test/resources/contracts`.
Schema-backed golden payloads are also validated against the published SDK
schemas so fixture drift is caught before product shells consume it.
Use `wayang contracts --json` to discover the SDK-owned JSON contract catalog,
or use `wayang contracts --index --json` when a shell only needs totals,
filters, facets, and command ids. Filter either shape with `--schema`,
`--envelope`, `--command-id`, `--domain`, and `--json-schema-id`. The same
contract-discovery commands are exposed through the SDK command catalog under
the `Contracts` category. Contract descriptors include stable `commandIds`, so
shells can jump from an envelope to matching command catalog entries.
Use `wayang contracts --envelope <envelope> --schema-json` or
`wayang contracts --json-schema-id <schema-id> --schema-json` to render the
Draft 2020-12 JSON Schema document for one matching contract. Use
`wayang contracts --schema-bundle-json` with any contract filter to render a
bundle of Draft 2020-12 JSON Schema documents for all matching contracts.
Contract discovery JSON also includes aggregate `domains`, `domainCounts`,
`jsonSchemaIds`, `commandIds`, and `commandIdCounts` plus structured
`schemaSummaries`, `domainSummaries`, and `envelopeSummaries` for
product-shell filters and summary UIs. Each descriptor includes a stable
`jsonSchemaId` so shells can link directly from a contract entry to the schema
document.
Contract-aware command entries expose a `contracts` array with schema, version,
envelope, and `jsonSchemaId` references, so shells can bind command output
without reconstructing schema URNs. Command discovery and the full workbench
payload can also reverse-filter by that id with
`wayang commands --contract-json-schema-id <schema-id> --json` or
`wayang workbench --contract-json-schema-id <schema-id> --json`.
Platform catalog JSON is schema-backed as
`wayang.platform.catalog` with envelopes for `platform-status`,
`product-catalog`, `profile-list`, and `profile-detail`. It covers
`status --json`, `products --json`, `profiles --json`,
`profiles --surface ... --json`, and `profiles inspect ... --json`.
Skill catalog JSON is schema-backed as `wayang.skill.catalog` with
`skill-discovery` for `skills list/search --json` and `skill-detail` for
`skills inspect --json`. It covers dynamic capabilities from built-in, RAG,
MCP, workflow, memory, and observability sources.
Standard-alignment health JSON is schema-backed as `wayang.standard.alignment`
with `standard-alignment-health` for `standards --json`, covering readiness,
policy, registry drift, recommendations, and provider diagnostics. Standards
catalog JSON remains schema-backed as `wayang.standard.catalog` with
`standards-catalog` for `standards --catalog --json`. It covers pinned external
standard ids, aliases, versions, bindings, spec URLs, and extension attributes
for A2A, A2UI, Agentic Commerce, and future interoperability adapters.
The command discovery JSON envelope is also a first-class contract:
`urn:wayang:contract:wayang.command.discovery:v1:commands-discovery`.
That schema covers `commands --index --json` metadata and the optional
`commands` array returned by detail views.
The full workbench JSON envelope is also schema-backed as
`urn:wayang:contract:wayang.workbench.discovery:v1:workbench-discovery`.
That schema covers platform status, product catalog, command query, command
palette, command entries, and next actions for product shells that bind the
whole workbench surface.
Use `wayang contracts --check --json` in CI to validate those bidirectional
catalog links; the command exits nonzero if drift is detected.
Use `wayang contracts --coverage --json` to inspect command coverage for every
SDK-owned contract. That report is schema-backed as
`urn:wayang:contract:wayang.contract.coverage:v1:contract-command-coverage`
and keeps intentionally commandless readiness envelopes separate from broken
or incomplete command links.
Use `wayang standards` for standard-alignment readiness health, or
`wayang standards --catalog --json` when an agent shell needs the SDK-pinned
standard ids, aliases, versions, bindings, spec URLs, and extension attributes
for A2A, A2UI, Agentic Commerce, or future protocol adapters.
Run lifecycle JSON envelopes use `schema=wayang.run.lifecycle`, `version=1`,
and envelopes such as `run-result`, `run-status`, `run-events`, `run-list`, or
`run-wait`. Their Draft 2020-12 schemas describe concrete result, status,
event timeline, follow, inspection, history, wait, cancel, and forget fields,
including nested handles, cursors, pages, summaries, status rows, event rows,
and metadata maps for product-shell binding.
Run planning JSON envelopes such as `run --preflight --json` and
`run --dry-run --json` use `schema=wayang.run.planning`, `version=1`, and
envelopes such as `run-preflight` or `run-preview` for the same binding style
before a run exists. Their Draft 2020-12 schemas describe readiness, surface
policy assessment, skill assessment, normalized request, context, and parameter
fields instead of only exposing the contract marker.

## SDK Skill Registry

`wayang-gollek-sdk` now owns a small product-facing skill discovery contract:
`AgentSkillDescriptor`, `AgentSkill`, `RegisteredSkill`, `AgentSkillState`,
`AgentSkillQuery`, and `SkillRegistry`. This layer describes capabilities by
stable id, source, lifecycle state, product surfaces, input/output keys, tags,
aliases, and metadata without coupling the SDK to any concrete skill executor.
Runtime adapters can translate these entries into MCP tools, RAG retrievers,
Gamelan workflows, built-in agent skills, or future low-code platform skills.
The local SDK seeds the registry with the default Wayang capability catalog and
the CLI exposes it through `wayang skills`, `wayang skills list`,
`wayang skills inspect`, and `wayang skills search`. SDK callers can use
`skillsForProfile(...)` or `skillDiscoveryForProfile(...)` when they want the
capability bundle from a reusable product profile.

Run results include an SDK-owned lifecycle handle with `runId`, state, strategy,
terminal status, and an SDK-owned `outcome` such as `terminal` or `pending`.
This prepares the API for later submit/status/list run
commands without changing the current immediate-run behavior.
`AgentRunLifecycleService` is the SDK boundary for recording results and
serving status, history, event timelines, inspection, wait, cancel, and forget
flows; `AgentRunStore` implementations stay focused on local persistence.
Local SDK instances keep an in-memory run-status store, and `wayang run status <run-id>`
or `wayang run list --state completed --limit 10` use the same status contract
that future persistent local or remote stores can implement. `wayang run inspect
<run-id>` combines the status snapshot and event timeline for product shells
that need one lifecycle envelope, and exposes a top-level `outcome` derived
from the combined lifecycle data. Status JSON exposes an SDK-owned `outcome`
from `AgentRunOutcomes`, such as `terminal`, `pending`, or `unknown`.
`wayang run events <run-id>` returns the
SDK-owned lifecycle timeline for debugging, UI timelines, and future
streaming/status adapters. Add `--state`, `--type`,
`--after-sequence`, and `--limit` to keep long-running agent timelines bounded
for terminal UIs and product shells. Use `--follow` for a bounded cursor loop
that advances `afterSequence` until a terminal event or `--max-polls`; tune the
interval with `--poll-millis`. Add `--follow-result` when the CLI should append
a final `run-events-follow` envelope after the streamed event-window envelopes.
Use `--follow-result-only` when automation wants one final envelope with the
last event window nested under `lastEvents`; combine it with `--stats` to omit
event rows from that nested envelope. Final follow envelopes expose an
SDK-owned `outcome` from `AgentRunOutcomes`, such as `terminal` or `max-polls`,
plus `terminalState`, `terminalEventType`, and `terminalSequence` at the top
level so product shells can route terminal outcomes without parsing nested
event rows.
SDK products should use
`followRunEvents(runId, AgentRunEventsFollowOptions, Consumer<AgentRunEvents>)`
for the same cursor loop without duplicating CLI polling. Event responses include
an SDK-owned `outcome` such as `terminal`, `pending`, or `empty`, plus
`nextAfterSequence`, so pollers can feed that value back into the next request,
and a nested `cursor` envelope with sequence range and advancement metadata.
They also include returned-window `stateCounts`/`typeCounts` plus sorted
`stateSummaries`/`typeSummaries` arrays for lightweight dashboards, grouped
under a nested `summary` envelope for stable UI binding.
Use `--stats` when a poller needs cursor and summary envelopes without the event
rows.
`wayang run list` can also filter by tenant, session, product surface, and product profile, add
`--offset` for paged history reads, and returns page metadata such as
`windowStart`, `windowEnd`, `previousOffset`, `nextOffset`, and `hasMore` plus
returned-window state, surface, profile, and strategy counts plus sorted facet
summary arrays. History envelopes expose an SDK-owned `outcome` such as
`terminal`, `pending`, `unknown`, or `empty` for page-level routing. The same
pagination fields are grouped under the JSON `page`
object for product shells that prefer one stable page envelope, while counts
and summaries are grouped under `summary`. Use
`wayang run stats` when a dashboard needs the page and summary envelopes without
the run rows.
Use `--run-store <path>` or `WAYANG_RUN_STORE` to persist local run snapshots
across CLI invocations during development. `wayang run forget <run-id>` removes
one locally recorded snapshot from that store; it does not cancel remote work.
Remote SDK mode keeps the same lifecycle methods and delegates run status and
history to remote Wayang API endpoints. `wayang run cancel <run-id>` is the
separate lifecycle verb for requesting cancellation of a non-terminal run, and
`wayang run wait <run-id>` polls status until a run reaches a terminal state or
the timeout expires. Wait JSON exposes an SDK-owned `outcome` from
`AgentRunOutcomes`, such as `terminal`, `timeout`, or `unknown`, matching the
routing style of event follow results. Cancel JSON uses the same outcome
vocabulary for `cancelled`, `not-cancellable`, and `not-found`; forget JSON
uses `forgotten` and `not-found`.

## Active vs Legacy Trees

The active Maven reactor is declared in `pom.xml`. The older top-level
`skills/`, `agent-gollek/`, `gollek-runtime-*`, and `enhancement/` trees are
kept as legacy or experimental code unless they are explicitly reintroduced into
the reactor.

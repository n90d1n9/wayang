# Wayang Reorganization Status

Date: 2026-05-26

## What Changed

- Re-parented the active `wayang-gollek` reactor to the workspace
  `wayang-platform` parent instead of the external Gollek parent.
- Normalized active module parents to `wayang-agentic-parent` or `agent-parent`
  so Maven no longer silently resolves stale parent artifacts from the local
  repository.
- Added local parent-managed versions for Gollek, Gamelan, Lombok, compiler
  plugin, and Jandex plugin coordinates.
- Moved migration reports/scripts and draft notes out of `src/main/java` into
  module docs.
- Removed a duplicate service-provider file that had been copied under
  `src/main/java/src/main/resources`.
- Fixed agent SDK coordinates to use `tech.kayys.gollek:gollek-sdk` with
  `gollek.version`.
- Aligned the `tools/tools-spi` source skeleton with its declared
  `tech.kayys.wayang.tools.spi` package, replacing the stale `golok` source
  path.
- Split runtime skill-to-tool adaptation from manifest skill-to-tool adaptation:
  `skills.adapters.SkillAsToolAdapter` now owns runtime `SkillDefinition`
  adaptation, while `skills.adapters.ManifestSkillToolAdapter` owns filesystem
  `SkillManifest` adaptation. The old singular
  `skills.adapter.SkillAsToolAdapter` remains only as a deprecated compatibility
  wrapper.
- Collapsed the duplicate `tool.parser.SpecFormatRegistry` into a deprecated
  alias of the canonical `tool.registry.SpecFormatRegistry`.
- Collapsed the second parser-module copy of `SpecFormatRegistry` in
  `wayang-tool-parser` into the same deprecated alias, so supported-format
  metadata has one canonical implementation.
- Aligned the remaining agent-core `ToolRegistryTest` import with the active
  `tech.kayys.wayang.tools.spi` package instead of the stale `golok` SPI.
- Normalized the standalone `skills/skill-spi` skeleton:
  - Re-parented it to `skills-parent` instead of the agent parent.
  - Renamed its artifact from the duplicate `agent-spi` to
    `wayang-skill-spi`.
  - Aligned declared packages with the source path:
    `tech.kayys.wayang.skill.spi`.
  - Added a standalone `Skill` runtime contract and kept `AgentSkill` as a
    deprecated compatibility alias.
  - Registered `skill-spi` as the first explicit module under `skills-parent`.
- Migrated the MCP tool wrapper in `wayang-tool-mcp` onto the active
  `tech.kayys.wayang.tools.spi.Tool` contract, leaving the old
  `tech.kayys.wayang.tool.spi.Tool` path as compatibility-only.
- Made `wayang-tool-spi` a self-contained SPI again by removing its dependency
  on `wayang-tool-core`. Legacy/new tool bridge classes now live in
  `wayang-tool-core` under `tech.kayys.wayang.tool.adapter`, where the
  deprecated core-local SPI is available.
- Migrated the UTCP tool wrapper in `wayang-tool-utcp` onto the active
  `tech.kayys.wayang.tools.spi.Tool` contract, matching the MCP wrapper shape.
- Removed stale external Gollek tool dependencies from the legacy
  `golek-tool-execution` and `golek-tool-registry` compatibility modules:
  both now compile against the active Wayang tool SPI and local compatibility
  contracts.
- Isolated `gollek-plugin-tool-execution` from the active tools parent. It
  still targets old Gollek inference/plugin contracts and should return only
  after that bridge is migrated onto the Wayang/Gamelan plugin boundary.
- Re-centered `runtime-quarkus` on the active Wayang core boundary: it now
  imports `agent-core` client/config and `agent-core` backend registry directly,
  and no longer depends on unfinished backend adapter modules. Backend adapters
  can be added to runtime classpaths through the `BackendProvider` ServiceLoader
  boundary once they compile against the active SPI.
- Tightened the active `agent-parent` module set to the compile-ready agent
  line: `agent-spi`, `agent-core`, `agent-orchestration`,
  `agent-backend-gollek`, `agent-backend-gamelan`,
  `agent-integration-gamelan`, `agent-skills-builtin`, `agent-adapter`,
  `agent-mcp`, `skill-management`, `skill-audit`, `agent-api`,
  `skills-cli`, `agent-examples`, and `agent-shaker`.
- Tightened the active `wayang-memory-parent` module set to the compile-ready
  memory line: `memory-core`, `golek-memory-context-engineering`,
  `golek-memory-short`, `golek-memory-working`,
  `golek-memory-episodic`, `golek-memory-semantic`,
  `golek-memory-longterm`, `gollek-plugin-memory-integration`,
  `vector-memory-integration`, `graph-memory-integration`, and the
  `memory-runtime` REST facade. Remaining legacy Gollek memory sources are
  parked under module-local legacy source trees until they move off unavailable
  Gollek SPI artifacts.
- Migrated `rag-graph-integration` onto the active RAG core records and
  `Retriever` SPI, replacing stale builder/getter calls and fixing relationship
  stream mapping so graph metadata is preserved when converting nodes to
  `RagChunk` records.
- Re-centered `rag-core` response generation on the active backend-agnostic
  `agent-spi` `InferenceBackend` contract. Direct Gollek SDK/SPI imports and
  dependencies were removed from `rag-core`; provider and API-key routing now
  travel through `InferenceRequest.metadata()` for backend adapters such as
  `agent-backend-gollek`.
- Consolidated shared built-in RAG plugin behavior into `rag-plugin-api`.
  `RagPluginSupport` now owns deterministic query normalization, tokenization,
  blocked-term parsing/redaction, weight sanitation, and metadata copying, while
  `RagPluginTuningConfig.defaults()` makes normalize, safety, and lexical
  rerank plugins safe to construct outside CDI.
- Re-centered `rag-memory-integration` on a small `RagMemoryRetriever`
  boundary. The vector memory adapter now sits behind `VectorMemoryRetriever`,
  while `RagMemoryIntegration` owns top-k allocation, filter propagation,
  source/count metadata, and memory-to-`RagChunk` conversion.
- Aligned `rag-memory-integration` with the core `RagMetadata` boundary.
  Memory-augmented retrieval now snapshots caller filters and memory-derived
  chunk metadata through the same nullable-safe immutable copy helper as core
  RAG chunks/documents.
- Split native RAG generation mode from provider selection in `rag-runtime`.
  `NativeGenerationMode` now resolves context vs extractive rendering from
  explicit generation metadata, while retaining the old provider-name fallback
  as a compatibility path.
- Consolidated native RAG runtime pipeline assembly behind
  `NativeRagPipelineFactory`. `NativeRagCoreService` now owns orchestration and
  plugin hooks, while the factory owns parser/chunker/indexer/retriever/reranker
  construction and retrieval-only generator wiring.
- Preserved text ingestion source identity in the core parser path:
  `SimpleTextDocumentParser` now maps a non-blank source to `RagDocument.id()`,
  keeping native RAG chunk document references stable while retaining the UUID
  fallback for anonymous documents.
- Corrected native RAG query hook ordering so `afterRetrieve` plugins mutate the
  retrieved context before `NativeGenerationService` renders the answer. Query
  orchestration now uses the runtime factory's retrieval-only pipeline for
  retrieval/rerank, then generates from the plugin-transformed chunks.
- Added `NativeRagQueryContext` as the native runtime's query-normalization
  boundary. It owns null/default retrieval and generation configs, filter
  normalization, plugin execution context construction, and conversion back to
  `RagQuery` after `beforeQuery` plugin mutations.
- Added `RagQueryWorkflowContext` above the native core to normalize public query
  requests. `RagQueryService` now centralizes mode/strategy/config defaults,
  collection-to-native-filter mapping, conversation metadata, and null-safe
  source/chunk handling before delegating to `NativeRagCoreService`.
- Added `RagConversationQuery` as the conversational query normalization
  boundary. `RagQueryWorkflowContext` now delegates session/history metadata,
  null-turn filtering, and history-aware query text rendering to one helper.
- Added `RagWorkflowFilters` as the workflow filter normalization boundary.
  Workflow context and workflow-input mapping now share defensive filter
  copying, collection-list normalization, and native collection filter merging.
- Added `RagIngestionContext` as the document ingestion normalization boundary.
  `DocumentIngestionService` now orchestrates PDF/text/batch flows through one
  metadata, source identity, chunking default, and result/metrics path instead
  of duplicating those concerns per ingestion entry point.
- Added `RagIngestionMetadata` as the ingestion metadata shaping boundary.
  PDF/text ingestion documents now share tenant/source metadata construction,
  caller metadata copying, collection normalization, and source fallback rules.
- Added `RagPluginSelectionConfig` as the plugin-selection config parsing
  boundary. Config strategy resolution, admin inspection, and compatibility
  parser methods now share tenant normalization, strategy-id normalization,
  enabled plugin parsing, wildcard handling, and order parsing.
- Added `RagPluginSelection` as the plugin eligibility and ordering boundary.
  Config selection and admin inspection now share enabled checks, safe
  tenant-support evaluation, normalized active plugin IDs, and order override
  semantics.
- Added `RagPluginHooks` as the plugin hook execution boundary. The manager now
  delegates before-query, after-retrieve, and after-result hook folding while
  preserving null-return and exception fallback behavior in one place.
- Added `RagPluginInspection` and `RagPluginInspector` as the plugin admin
  reporting boundary. Admin status and manager inspection now share active ID
  projection and per-plugin enabled/support/active flag shaping without
  coupling the public status model to a manager nested record.
- Hardened `RagPluginAdminStatus` as the plugin admin response value boundary.
  Admin responses now normalize tenant IDs, defensively copy plugin/status
  lists, and default missing timestamps before leaving the runtime layer.
- Added `RagPluginDiscovery` as the plugin catalog discovery boundary. CDI and
  test plugin sources now share null filtering, order/id sorting, immutable
  result projection, and plugin ID normalization.
- Added `RagPluginStrategyRegistry` as the plugin selection-strategy lookup
  boundary. CDI and test strategy sources now share normalized indexing,
  stable strategy ID listing, default fallback lookup, and known-strategy
  checks while the tenant resolver focuses on resolution and selection.
- Added `RagPluginMetadata` as the plugin API metadata copy boundary.
  Execution context filters and plugin metadata annotations now share
  defensive immutable copying while preserving nullable values that can flow
  through runtime metadata and filter maps.
- Added `RagRuntimeMetadata` as the runtime metadata copy boundary. Workflow
  filters, ingestion metadata, response metadata, and native retrieval result
  metadata now share null-to-empty handling, linked-map copying, string
  metadata sanitization, and defensive immutable projections. `DocumentSource`
  now applies the same string metadata copying at construction.
- Hardened `RagQueryRequest` as the advanced-query input value boundary.
  Request construction now snapshots filters, defaults null filter/collection
  inputs to empty values, and normalizes collection lists before workflow
  mapping receives them.
- Added `AdminApiKeyAccess` as the admin authentication/key-slot boundary.
  Request and response filters now share configured-key detection, primary vs.
  secondary slot resolution, challenge response construction, and slot header
  rendering while retaining the existing filter constants.
- Added `RagQueryResponseAssembler` as the query response-shaping boundary.
  `RagQueryService` now delegates source-document conversion, response metrics,
  config metadata serialization, and safe chunk filtering after native query
  execution.
- Added `RagWorkflowRequestMapper` as the workflow-entry mapping boundary.
  `RagExecutionService` now preserves `RagWorkflowInput` retrieval/generation
  configs, forwards retrieval metadata filters, and keeps the historical
  default collection while delegating through the advanced query path.
- Added `RagRetrievalEvalFilters` as the retrieval-eval filter boundary.
  Eval request filters, dataset defaults, and per-case filters now share
  defensive immutable copying, nullable-value preservation, and explicit
  dataset/request/case override ordering before native retrieval calls.
- Added `NativeRagRetrievalStage` as the shared native retrieval lifecycle
  boundary. `NativeRagCoreService` now runs `beforeQuery`, retrieval, safe chunk
  normalization, and `afterRetrieve` through one path for both query and
  retrieve calls.
- Added `NativeRagChunkingOptions` as the native ingestion option mapper.
  Direct `NativeRagCoreService.ingestText` calls now get runtime chunking
  defaults and sanitized chunk size/overlap before crossing into the core
  chunker SPI.
- Added `NativeRagPipelineSpec` as the native pipeline assembly boundary.
  `NativeRagPipelineFactory` now delegates namespace/model normalization,
  blank generation fallback, and indexer/retriever construction into one
  package-local unit with direct tests.
- Added `RagRuntimeDefaults` as the runtime default vocabulary for native RAG.
  Namespace defaults, workflow collection defaults, collection metadata keys,
  and collection-list normalization now live in one package-local utility
  instead of being repeated as string literals. PDF ingestion now applies the
  normalized collection value after metadata copying so blank metadata cannot
  erase the default collection.
- Added `RagMetadataKeys` in `rag-core` as the shared metadata/filter key
  vocabulary. Core indexing, vector retrieval filters, in-memory vector store
  contract checks, chunk source metadata, and runtime ingestion/response
  assembly now reuse the same key constants and embedding-scope map builders.
- Added `RagMetadata` in `rag-core` as the core metadata copy boundary. Core
  chunks/documents, metadata-key builders, the in-memory vector store, and the
  owned RAG embedding adapter now share defensive immutable copying while
  preserving nullable metadata and filter values.
- Hardened core query/result records against caller-owned metadata maps.
  `RagQuery` filters and `RagResult` metadata now route through `RagMetadata`
  at construction, matching the chunk/document behavior without changing result
  chunk-list semantics.
- Hardened `RetrievalConfig` as an immutable retrieval-settings boundary.
  Metadata filters now use the core `RagMetadata` copy helper and excluded
  fields are defensively snapshotted, preventing caller-side mutation from
  changing retrieval behavior after config construction.
- Added `RagCollections` as the package-local list snapshot helper for core
  RAG value objects. Retrieval config excluded fields, eval datasets, and eval
  query cases now share immutable list copying while eval filter maps continue
  through the nullable-safe `RagMetadata` boundary.
- Hardened `RagResponse` as a core response value boundary. Source-document,
  citation, source-name, and metadata collections are now snapped at
  construction so caller-side mutations cannot change an assembled response.
- Hardened core response support value objects. `SourceDocument` now snapshots
  string metadata through `RagMetadata.copyStrings`, while metadata-built
  `Citation` construction is null-safe and shares the same immutable metadata
  boundary as other core RAG value objects.
- Added workspace-aware Wayang runs. `AgentRunRequest` can now request
  workspace inspection, local SDK runs attach the compact `workspace` context
  into the core `AgentRequest`, and the CLI exposes it with
  `wayang run <task> --workspace <path>`.
- Added SDK-owned harness planning. `planHarness(...)` derives verification
  checks from workspace descriptors, the CLI exposes `wayang harness --path`,
  and the remote SDK forwards the same contract to `/harness/plan`.
- Added harness-aware Wayang runs. `wayang run --harness` now requests planned
  verification checks, local SDK runs attach them to the core `AgentRequest`
  under `harness`, and remote run payloads carry the same harness flags.
- Added machine-readable CLI output for automation. `workspace`, `harness`,
  and `run` now support `--json`, sharing one small CLI JSON serializer instead
  of separate ad hoc renderers.
- Added a named `AgentRunRequest` builder and moved CLI/defaulting paths onto
  it, preserving older constructors while avoiding fragile long argument lists
  as workspace and harness options grow.
- Added product-surface-aware Wayang runs. `AgentRunRequest.surfaceId` defaults
  to `coding-agent`, CLI runs expose `--surface`, local runs carry the surface
  into `AgentRequest.context()`, and remote run payloads send the same
  `surfaceId` field for API-backed products.
- Centralized product-surface policy in `WayangProductCatalog`. Local SDK,
  remote SDK, mapper, and CLI defaults now share the same default surface,
  known-surface listing, lookup, and fail-fast validation instead of repeating
  surface id strings.
- Added SDK-owned `ProductSurfacePolicy` metadata. The catalog now describes
  each product surface's preferred context families, suggested skills,
  required context keys, and routing hints; local/remote run metadata and core
  `AgentRequest.context()` now expose the selected policy under
  `surfacePolicy`.
- Added machine-readable product catalog output. `wayang products --json` now
  emits Wayang product surfaces together with their SDK-owned policies so
  product shells and API adapters can consume the catalog without scraping
  terminal text.
- Added machine-readable workbench output. `wayang workbench --json` now emits
  the SDK-owned workbench model with status, product catalog, command palette,
  structured commands, and next actions as one payload for Tamboui, agent
  shells, and remote launchers.
- Added surface policy preflight. `SurfacePolicyPreflight` and the
  `WayangGollekSdk.assessRunPolicy(...)` default method compare run requests
  against SDK-owned surface policies, reporting readiness, satisfied/missing
  context keys, recommendations, and routing hints. Local/remote run metadata
  and core `AgentRequest.context()` expose the result as
  `surfacePolicyAssessment`.
- Exposed run policy preflight in the CLI. `wayang run <task> --preflight`
  renders only the surface assessment without preparing a run; `--json`
  returns the machine-readable envelope, and the command exits non-zero when
  required surface context is missing.
- Added an SDK-owned run identity/context envelope. `AgentRunRequest` now
  carries `sessionId`, `userId`, and caller context metadata; local/remote run
  metadata, remote run payloads, and the core `AgentRequest` mapper preserve
  those values while keeping SDK-owned reserved context authoritative. The CLI
  exposes this through `wayang run --session`, `--user`, and repeatable
  `--context key=value`.
- Added richer prompt sourcing for Wayang runs. `AgentRunRequest` now carries
  `systemPrompt`, the mapper forwards it to the core `AgentRequest`, remote run
  payloads include it, and the CLI accepts inline prompts, `--prompt-file`, or
  `--stdin` as mutually exclusive prompt sources plus `--system` or
  `--system-file` for system prompts.
- Added SDK-owned run preview/dry-run support. `previewRun(...)` returns an
  `AgentRunPreview` with normalized routing, core context/parameters,
  workspace/harness attachment flags, and surface policy assessment without
  submitting a run. The CLI exposes this as `wayang run --dry-run`, with
  machine-readable output through `--json`.
- Added Java properties run specs for reusable agent invocations.
  `AgentRunSpec.fromProperties(...)` maps saved run defaults into
  `AgentRunRequest`, and the CLI exposes this as `wayang run --spec <file>`.
  Explicit CLI flags override spec defaults while `--context key=value`
  extends or overrides `context.<key>` entries.
- Added run-spec round-tripping. `AgentRunSpec.formatProperties(...)` emits a
  deterministic properties representation of a resolved `AgentRunRequest`, and
  the CLI exposes it through `wayang run --print-spec` without submitting a run.
- Added dedicated run-spec commands. `wayang spec validate --path <file>`
  validates saved specs through the same preview contract as `run --dry-run`,
  while `wayang spec template --surface <id>` emits starter properties from the
  SDK-owned product surface policy.
- Added safe run-spec file output. `wayang run --print-spec --output <file>`
  and `wayang spec template --output <file>` write UTF-8 properties files,
  create missing parent directories, and refuse to overwrite existing files
  unless `--force` is supplied.
- Extracted run-spec file lifecycle into `WayangRunSpecService`. The SDK now
  owns UTF-8 spec reading, writing, template writing, parent-directory creation,
  and overwrite protection while the CLI delegates to that shared service.
- Consolidated the SDK-owned workbench command palette behind
  `WayangWorkbenchCatalog`. Local and remote workbench models now share the
  same spec/run/workspace/harness examples while local appends only the TUI
  command that belongs to the packaged terminal product.
- Added structured workbench commands. `WorkbenchCommand` gives workbench
  entries stable ids, titles, categories, descriptions, surface hints, and
  local-only flags while `WayangWorkbenchModel.commandPalette()` remains
  available for existing text renderers.
- Added surface-filtered workbench command discovery. `WayangWorkbenchCatalog`
  can return general commands plus commands for one product surface, and
  `wayang workbench --surface <id>` exposes that filtered command model for
  coding-agent, assistant-agent, workflow, and future product shells.
- Added a focused command-discovery CLI. `wayang commands` and
  `wayang commands --surface <id> --json` expose the SDK-owned command catalog
  directly for agent shells that do not need the full workbench/status payload.
- Added command-discovery lookup filters. SDK and CLI callers can now narrow
  command metadata by product surface, command category, or stable command id
  before rendering text or compact JSON.
- Lifted composed command discovery into the SDK. `WorkbenchCommandQuery`,
  `WayangCommandDiscoveryService`, and `WayangGollekSdk.discoverCommands(...)`
  now give coding agents, assistant shells, and future low-code products the
  same normalized lookup behavior as the CLI.
- Reused `WorkbenchCommandQuery` for full workbench filtering. `wayang
  workbench` and `WayangGollekSdk.workbench(query)` can now return status,
  product catalog, next actions, and a narrowed command model in one payload.
- Added `WorkbenchCommandDiscovery` as the SDK result object for command
  discovery. CLI JSON now exposes the normalized query, total command count,
  matching count, categories, and stable command ids alongside command entries.
- Added compact command indexing. `wayang commands --index` renders category
  counts and stable command ids without emitting full command descriptions.
- Added structured category summaries for command discovery. SDK and CLI JSON
  now expose each category with its count and stable command ids, so shells do
  not need to reconstruct groupings from flat fields.
- Added golden CLI JSON contract tests. Status JSON, command index JSON, and
  command detail JSON now have exact fixtures under
  `wayang-gollek-cli/src/test/resources/contracts`, with a filtered workbench
  contract test guarding the larger discovery payload shape.
- Made remote command/workbench discovery explicit. `RemoteWayangGollekSdk`
  now overrides `workbench(query)` and `commandDiscovery(query)` using the same
  SDK discovery service while preserving the remote-safe catalog without local
  only commands such as `tui`.
- Started the run lifecycle contract. `AgentRunState` and `AgentRunHandle` are
  now part of `AgentRunResult`, and CLI run output exposes the completed/failed
  lifecycle state while preserving the existing immediate-run behavior.
- Added the first run-status surface. `AgentRunStatus` and `wayang run status
  <run-id>` expose a structured unknown-status response until persistent local
  or remote run storage is introduced.
- Added the first run-status store boundary. `AgentRunStore` and
  `InMemoryAgentRunStore` let local SDK instances record submitted run
  snapshots, while CLI/status callers keep using the same contract that later
  persistent stores can implement.
- Added the first run-history view. `AgentRunHistory` and `wayang run list`
  expose recorded lifecycle snapshots through the same store boundary, giving
  long-lived shells and future persistent backends a stable submit/status/list
  shape.
- Added `AgentRunHistoryQuery` so SDK callers and `wayang run list` can filter
  run history by lifecycle state and bound output with a limit before persistent
  stores are introduced.
- Added `FileAgentRunStore` and the top-level CLI `--run-store`/`WAYANG_RUN_STORE`
  option. Local Wayang runs can now persist lifecycle snapshots across CLI
  invocations while the SDK still defaults to in-memory storage.
- Extended the remote SDK lifecycle seam. `RemoteWayangGollekSdk` and
  `WayangRemoteTransport` now expose `runStatus` and `runHistory`, with HTTP
  transport mappings for remote status/history endpoints.
- Mapped remote run submission responses into the SDK run handle. Remote
  `POST /runs` results now honor response `runId`, `state`, and `strategy`,
  defaulting accepted submissions to `RUNNING` when state is omitted.
- Extracted `RemoteRunLifecycleMapper` from the HTTP transport so remote
  submit/status/history parsing and path building are tested as lifecycle
  mapping rather than mixed into request execution.
- Hardened `CitationService` as the citation assembly boundary. Citation
  generation now tolerates missing responses, blank/null contexts, missing
  metadata rows, and out-of-range citation markers while routing metadata
  through the core immutable metadata helper before constructing citations.
- Added `ResponseGenerationContextMapper` as the generation workflow-input
  parsing boundary. `ResponseGenerationExecutor` now delegates typed/defaulted
  context extraction, context/metadata alignment, conversation-history
  sanitization, scalar parsing, and API-key alias handling instead of relying
  on raw workflow context casts.
- Hardened `GenerationConfig` as an immutable generation-settings boundary.
  Stop sequences now use the shared core list snapshot helper, and additional
  params plus safety settings route through `RagMetadata` so caller-side
  mutation cannot alter generation behavior after construction.
- Hardened `PromptTemplateService` as the prompt assembly boundary. User prompt
  rendering now skips null/blank contexts and malformed conversation turns,
  avoids rendering literal `null`, and preserves original context numbering for
  valid sparse inputs so prompt citations stay aligned with citation assembly.
- Hardened `ConversationTurn` as the conversation-history value boundary.
  Turns now trim role labels, default missing content/timestamps to stable
  values, expose a shared renderability check, and conversation query/prompt
  paths reuse that check instead of duplicating malformed-turn rules.
- Hardened `RagConversationQuery` as the runtime conversational query boundary.
  Conversation queries now trim current questions/session IDs, render blank
  questions without literal nulls, and defensively snapshot metadata plus
  conversation-history lists even for direct package-local construction.
- Hardened `RagQueryWorkflowContext` as the runtime query workflow boundary.
  Tenant/query text, mode/strategy/config defaults, collection normalization,
  and filter copying now live in the record constructor so simple, advanced,
  conversational, and direct package-local construction share one rule set.
- Hardened `RagWorkflowRequestMapper` as the workflow-input bridge. Workflow
  DTO tenant/query text is trimmed at mapping time, null workflow inputs now
  produce a fully defaulted advanced query request, and retrieval metadata
  filters are copied into immutable request filters before entering query
  orchestration.
- Added `RagRuntimeText` as the package-local runtime text normalization helper.
  Workflow request mapping, workflow context construction, conversational query
  shaping, runtime defaults, and plugin tenant override parsing now share the
  same trim/null-to-empty rule instead of carrying local copies. Case-insensitive
  plugin ids, strategy ids, native generation modes, and vector backend ids now
  share the same trim/lowercase path as well.
- Added `RagRuntimeConfigs` as the package-local config defaulting helper. RAG
  workflow context construction, workflow request mapping, native query context
  creation, and response metadata serialization now use one rule for null
  retrieval/generation config defaults.
- Added `RagRuntimeLists` as the package-local list boundary helper. Plugin
  catalog/status/hooks/selection, plugin inspection, ingestion contexts, and
  runtime collection normalization now share the same null-safe immutable list
  snapshot rule.
- Routed remaining RAG runtime metadata boundary snapshots through
  `RagRuntimeMetadata`. Conversational query metadata, plugin tenant override
  maps, and native query filter maps now share the same immutable map copy
  behavior.
- Consolidated plugin id list parsing inside `RagPluginSelectionConfig`. Enabled
  plugin ids and plugin order strings now share one comma/newline tokenizer for
  trimming, blank-token filtering, and normalized plugin ids.
- Added `RagRuntimeMetadata.stringifyValues` as the runtime object-to-string
  metadata projection. Source document assembly now reuses the same defensive,
  null-skipping metadata boundary instead of maintaining a private map copier.
- Added `RagResponseMetadata` as the response metadata serialization boundary.
  `RagQueryResponseAssembler` now delegates RAG mode/search strategy metadata
  and retrieval/generation config maps to one package-local contract with
  named keys and direct tests.
- Added `RagScoredChunks` as the runtime chunk-list normalization boundary.
  Retrieval, generation, and response assembly now share one rule for dropping
  malformed scored chunks and computing average similarity scores.
- Added `RagSourceDocuments` as the runtime source-document projection
  boundary. Response assembly now delegates chunk-to-source conversion,
  metadata stringification, source fallback, and malformed chunk filtering.
- Added `RagResponseMetrics` as the response-metrics shaping boundary.
  `RagQueryResponseAssembler` now delegates metric defaults, reranked-result
  counting, and average similarity calculation to one tested helper.
- Added `RagResponseContent` as the response answer/context normalization
  boundary. Query orchestration now uses `RagScoredChunks` directly for result
  chunk extraction, leaving the assembler focused on composing the response.
- Re-centered `storage` as a Wayang-owned extension boundary with a new
  `wayang-storage-spi` module and provider-neutral
  `tech.kayys.wayang.storage.spi.ModelStorageService` contract.
- Migrated the S3, GCS, and Azure storage providers onto the active storage SPI
  and reintroduced them as `wayang-storage-s3`, `wayang-storage-gcs`, and
  `wayang-storage-azure`. Provider packages now live under
  `tech.kayys.wayang.storage.provider.*`, and shared URI/object-name logic lives
  in `StoragePaths` instead of being repeated in each provider.
- Added `SkillDefinitionStore` as the skill-management persistence boundary.
  `SkillManagementService` now works with configurable registry, filesystem,
  object-storage, custom, or hybrid stores. Filesystem and object storage share
  the same properties codec, so database-backed custom stores can be primary
  while local files or S3-compatible stores such as RustFS/MinIO act as
  fallback backends.
- Added concrete persistence adapters behind that boundary. Skill management
  now includes a `JdbcSkillDefinitionStore`, a storage-SPI bridge for object
  stores, and explicit `JDBC` config support; the storage layer now exposes a
  generic `ObjectStorageService` plus an S3-compatible implementation with
  optional path-style access for RustFS/MinIO endpoints.
- Added `SkillDefinitionStoreConfigs` as the deployable configuration parser
  for skill persistence. Dotted properties, nested maps, and environment-style
  keys can now select `registry`, `filesystem`, `jdbc`, `object-storage`/`s3`/
  `rustfs`, `custom`, or `hybrid` topologies without binding skill-management
  to a specific runtime configuration framework.
- Added `SkillDefinitionStoreSynchronizer` plus typed sync options/results for
  controlled skill persistence migration. Operators can now bootstrap missing
  skills from file/object fallback into a database or cloud primary, run dry
  runs, detect conflicts, or mirror a source store while deleting target
  orphans.
- Added `SkillDefinitionStoreInspector` and service-level `inspectStore()`
  support. Runtime startup and diagnostics can now report configured store
  type, reachability, visible skill counts/IDs, failure summaries, and hybrid
  primary/fallback child status without mutating the store.
- Extracted skill lifecycle state into `SkillLifecycleStateStore`.
  `SkillManagementService` no longer owns enable/disable/deprecate state in a
  private map, and local deployments can use `FileSystemSkillLifecycleStateStore`
  to preserve disabled/deprecated/revision state across restarts.
- Added `JdbcSkillLifecycleStateStore` plus lifecycle state store config/factory
  support. Database-primary deployments can now persist disabled/deprecated
  lifecycle state and revisions beside JDBC-backed skill definitions, while the
  existing in-memory and filesystem lifecycle stores remain available.
- Added `SkillLifecycleStateStoreConfigs` for deployable lifecycle persistence
  wiring. Properties, nested maps, and `WAYANG_SKILLS_LIFECYCLE_STORE_*`
  environment keys can now select in-memory, filesystem, or JDBC lifecycle
  stores, and definition/lifecycle config parsers share common normalization
  helpers.
- Added `SkillLifecycleStateStoreInspector` and shared store-inspection support.
  Definition and lifecycle diagnostics now share store type/error formatting,
  while lifecycle inspection reports readiness, persisted skill IDs, and
  per-status counts without coupling lifecycle state to definition CRUD.
- Added `SkillLifecycleStateResolver` so read paths can apply active lifecycle
  defaults without writing to filesystem/JDBC lifecycle stores. Mutating paths
  still explicitly ensure persisted state before revision/status changes, which
  keeps lifecycle persistence effects intentional and auditable.
- Added `SkillLifecycleStateReconciler` plus typed reconcile options/results so
  startup or admin flows can explicitly inspect missing/orphaned lifecycle
  state, initialize missing rows/files, and prune stale state after skill
  definition migrations without reintroducing read-path persistence side
  effects.
- Added `SkillManagementServiceConfig` and `SkillManagementServiceFactory` as
  the service-level assembly boundary. Runtime bootstraps can now compose
  definition persistence and lifecycle persistence configs into one
  `SkillManagementService` without duplicating factory wiring across product
  surfaces.
- Added `SkillManagementServiceConfigs` as the service-level configuration
  parser. Properties, nested maps, and environment maps now compose definition
  persistence and lifecycle persistence through one deployable config entry
  point while reusing the existing store-specific parsers and aliases.
- Added custom lifecycle state store support. Lifecycle persistence configs and
  factories now support named custom stores, giving Redis/KV/product-specific
  lifecycle backends the same plugin seam already available for skill
  definition stores.
- Added `SkillManagementInspector` and `SkillManagementInspection` as the
  aggregate diagnostics boundary. Runtime startup/admin surfaces can now fetch
  definition-store health, lifecycle-store health, and inspect-only lifecycle
  reconciliation drift through one service-level call.
- Added `SkillLifecycleStateReconcileConfigs` and threaded reconcile policy into
  `SkillManagementServiceConfig`. Operators can now choose inspect-only,
  create-missing, or sync/prune lifecycle repair behavior through properties,
  nested maps, or `WAYANG_SKILLS_LIFECYCLE_RECONCILE_*` environment keys.
- Added `SkillManagementBootstrapper` and `SkillManagementBootstrapResult` as
  the startup assembly runner. Runtime bootstraps can now create the configured
  service, capture initial diagnostics, apply the configured lifecycle
  reconciliation policy, and capture final diagnostics through one explicit
  workflow.
- Added admin-facing skill-management DTOs and `SkillManagementAdminViews`.
  REST, CLI, and runtime shells can now project inspections and bootstrap
  results into stable string/list/map response contracts without exposing
  internal persistence records or health enums directly.
- Hardened skill-management write consistency with explicit compensation and
  `SkillManagementWriteException`. Create, update, and delete now roll back or
  restore definition/lifecycle state when the second persistence step fails,
  making cross-store write failures visible instead of leaving quiet drift.
- Added `SkillManagementMaintenanceRunner` with maintenance plan/result types.
  Operators can now run definition-store bootstrap/mirror sync and lifecycle
  state reconciliation as one explicit migration/repair workflow, including
  dry-run reporting and orphan pruning.
- Added `SkillDefinitionStoreSyncConfigs` and
  `SkillManagementMaintenancePlanConfigs` so definition sync and combined
  maintenance workflows can be selected from properties, nested maps, and
  environment variables. Bootstrap, mirror/repair, inspect-only, and dry-run
  policies now share the same deployable config style as persistence stores.
- Tightened lifecycle status transitions so enable/disable/deprecate no longer
  create an implicit ACTIVE lifecycle row before saving the requested status.
  A failed transition now leaves missing lifecycle state missing instead of
  persisting an accidental default.
- Added admin-facing maintenance result projections. Definition sync changes,
  sync summaries, and combined maintenance reports now map through
  `SkillManagementAdminViews`, giving CLI/REST shells stable maintenance
  response DTOs beside inspection and bootstrap views.
- Split `agent-core` tests into an active `src/test/java` suite and a
  non-compiled `src/legacy-test/java` holding pre-reorganization tests that
  still target removed `core.spi`, old inference DTOs, orchestration, and skill
  adapter APIs. The active suite now validates current memory and tool registry
  boundaries, plus the runtime `SkillAsToolAdapter`, `MCPSkillProvider`,
  `PromptContextProvider`, `SkillMemoryProvider`, `SkillSafetyValidator`,
  `RAGAwareSkillContext`, `VectorSkillIndexer`, `HITLSkillExecutor`, and
  `SkillIntegrationRegistry` contracts between
  `agent-spi` skills, the active Wayang tool SPI, MCP-shaped skill
  discovery/execution, prompt-context enrichment, skill-scoped in-process memory
  snapshots, fail-closed guardrail validation, RAG metadata/result enrichment,
  deterministic semantic skill indexing, human approval/feedback handling, and
  typed integration initialization/factory wiring.
- Hardened `AgentHitlService` as the agent-core HITL workflow state boundary.
  Requests, decisions, escalations, pending lookup, metrics, and memory audit
  writes now share normalized immutable snapshots without placeholder
  `agent-id`/`task-id` fallbacks or blocking reactive calls.
- Hardened `AgentAuditLogger` as the audit event boundary. Memory/file/custom
  sinks now share sanitized immutable event attributes, async recording has a
  real flushable executor path, file sinks tolerate parentless paths, and sink
  failures are contained without breaking agent execution.
- Hardened `AgentTelemetry` as the agent observability boundary. Memory
  operations now emit OpenTelemetry counters/histograms, local immutable stats
  expose deterministic verification without exporter dependencies, and
  inference/tool/span paths normalize null labels and negative durations.
- Hardened `SkillValidator` as the filesystem skill lifecycle validation
  boundary. Parameter schemas now parse from JSON strings, JSON Schema maps,
  simple per-parameter maps, and manifest `metadata.inputSchema` without
  conflating plain `allowed-tools` allowlists with input schemas; malformed
  schemas and malformed YAML frontmatter are reported explicitly.
- Extracted `SkillParameterSchema` as the shared filesystem-skill input schema
  boundary. `SkillValidator` and `ManifestSkillToolAdapter` now use the same
  normalization/projection path, and manifest tool definitions no longer parse
  ordinary `allowed-tools` allowlists as JSON parameter schemas.
- Added `SkillProcessInput` as the filesystem-skill process argument boundary.
  `SkillExecutor` now projects parameters to deterministic CLI arguments,
  serializes structured values as JSON, rejects malformed parameter names
  before launch, and reports rejected input without logging it as an internal
  execution failure.
- Added `SkillProcessRunner` as the filesystem-skill process lifecycle
  boundary. Skill output is captured concurrently, timeouts are enforced before
  waiting for stdout to close, and timed-out process trees are terminated with a
  bounded grace period. Non-zero exits now preserve bounded output context and
  structured process metadata so callers can distinguish process failures from
  validation, timeout, and infrastructure failures.
- Extracted `SkillExecutableResolver` as the filesystem-skill layout boundary.
  `SkillExecutor` now delegates `SKILL.md` and executable entrypoint discovery,
  reports missing manifests/executables as structured `skill_layout` failures,
  and rejects skill names that escape the configured skills directory.
- Added `SkillExecutionResults` as the filesystem-skill result assembly
  boundary. Success/failure result construction, elapsed timing, immutable
  metadata, and canonical failure type keys now live in one package-local
  helper while preserving the existing `SkillExecutor.SkillExecutionResult`
  public shape.
- Added `SkillExecutionOutcome` as the stable filesystem-skill result contract.
  Manifest tool adapters and skill-aware orchestration can now consume execution
  results without depending on the nested executor record, while the existing
  `SkillExecutor.executeSkill` return type remains source-compatible.
- Added `SkillExecutionOutcomes` as the public filesystem-skill outcome factory
  and failure taxonomy surface. Non-executor callers can now create immutable
  outcome objects and use canonical failure type keys without instantiating
  `SkillExecutor.SkillExecutionResult`.
- Extracted `SkillManifestCatalog` as the filesystem-skill loaded-manifest
  catalog boundary. `SkillExecutor` now delegates manifest loading, lookup, and
  immutable loaded-skill listing to the catalog instead of owning a mutable
  manifest map directly.
- Added immutable `SkillManifestSnapshot` and `SkillManifestCatalogChange`
  lifecycle contracts. Manifest reloads now report added, updated, and removed
  skills from stable SHA-256 content fingerprints, while `SkillExecutor`
  exposes `reloadSkills()` without breaking the existing `loadAllSkills()`
  caller shape.
- Added `SkillIntegrationRefreshRequest` as the generic skill lifecycle refresh
  boundary and wired manifest catalog diffs into `SkillIntegrationRegistry`.
  Runtime skill tool adapters and the vector skill index now refresh only
  added/updated skills and retire removed skills without rebuilding every
  integration cache.
- Added `SkillLifecycleRefreshCoordinator` as the bridge between filesystem
  reloads and integration refreshes. `SkillAwareToolOrchestrator` now
  initializes domains from the coordinated reload result, preserving catalog
  diffs even when no integration registry is attached and refreshing tool/vector
  caches when one is available.
- Added `SkillManifestRuntimeSkillMapper` and `SkillManifestRegistrySync` as
  the filesystem-manifest-to-runtime-skill projection boundary. Coordinated
  reloads now register added/updated `SKILL.md` manifests into the runtime
  `SkillRegistry` and unregister removed manifest skills before tool/vector
  integration caches refresh.
- Added `SkillRuntime` and `FilesystemSkillRuntime` as the filesystem-skill
  runtime boundary. `SkillExecutor` now delegates resolved-entrypoint execution,
  parameter-to-command projection, process working directory selection, and
  timeout-bounded process running to the runtime abstraction.
- Added an injectable `SkillExecutor` construction path for catalog, runtime,
  validator, and timeout dependencies. The public filesystem constructor remains
  unchanged, while tests and future runtimes can exercise executor validation and
  result mapping without hardwiring local process execution.
- Centralized skill execution failure taxonomy in `SkillFailureType` and moved
  failure-metadata construction/parsing into `SkillExecutionOutcomes`, keeping
  existing wire-code constants compatible while removing stringly-typed result
  mapping from executor internals.
- Centralized skill execution metadata keys and typed readers in
  `SkillExecutionMetadata` so filesystem, MCP, remote, and test runtimes can
  report/read exit codes, output capture state, timeouts, layout errors, and
  exception details through one stable result contract.
- Centralized common skill metadata keys and typed readers in the agent SPI via
  `SkillMetadataKeys`. MCP resource projection, skill-to-tool metadata,
  vector-skill indexing, manifest domain filtering, and parameter-schema
  validation now share the same version/category/tags/domain/output-format
  vocabulary.
- Centralized skill context payload keys in the agent SPI via
  `SkillContextKeys`. Prompt variables, RAG result enrichment, HITL approval
  and refinement payloads, MCP skill identifiers, tool schema skill fields, and
  skill-scoped memory keys now share one vocabulary while preserving the
  existing camelCase prompt keys and snake_case wire keys.
- Extracted MCP skill payload mapping into `McpSkillPayloads`, leaving
  `MCPSkillProvider` focused on registry coordination and configuration while
  resource and execution-result payload keys stay covered by a focused contract.
- Extracted shared `SkillResult` payload mapping into `SkillResultPayloads` so
  MCP and tool adapters reuse the same status/output/error interpretation while
  preserving each adapter's public wire keys.
- Extracted runtime skill tool descriptor/schema projection into
  `SkillToolDescriptors`. `SkillAsToolAdapter` now delegates tool name,
  description, metadata, default JSON Schema fallback, and metadata-supplied
  input schema handling to the shared adapter helper.
- Aligned `wayang-tool-core` Jackson YAML and Mutiny dependencies with the
  Quarkus BOM instead of pinning incompatible versions locally.
- Quoted the Redis key prefix in `agent-core` configuration so colon-heavy
  values parse cleanly as YAML.
- Reintroduced `agent-orchestration` to the active agent parent after isolating
  legacy Gollek-native loops under `src/legacy-main/java`. The active
  orchestration module now stays backend-agnostic, uses `InferenceTypes`
  messages for conversation memory, and maps canonical Wayang `Tool` SPI
  objects to agent `ToolDefinition` records through `ToolDefinitionMapper`.
- Reintroduced `agent-backend-gollek` to the active agent parent. The active
  adapter now bridges Wayang `InferenceBackend` requests directly to the current
  Gollek SDK/SPI (`GollekSdkFactory`, `tech.kayys.gollek.spi.*`) and keeps the
  stale intermediate `agent.spi.inference` mapper under `src/legacy-main/java`.
- Reintroduced `agent-backend-gamelan` to the active agent parent. The active
  adapter now bridges Wayang `WorkflowBackend` requests to the current Gamelan
  SDK run builders and JavaBean DTO getters, while the stale `createRun`,
  `startRun`, and record-accessor adapter is preserved under
  `src/legacy-main/java`.
- Reintroduced `agent-integration-gamelan` to the active agent parent. The
  integration service now uses the current Gamelan run builders, active
  `agent-core` memory service, and active `tech.kayys.wayang.graph` contracts;
  graph-related classes now live under a source path that matches their
  `gamelan.graph` package.
- Reintroduced `agent-skills-builtin` to the active agent parent. Legacy
  `SkillContext`/`SkillResult` implementations and old Gollek inference
  bindings are preserved under `src/legacy-main/java`; active built-in skills
  now implement the map-based `AgentSkill` contract and depend on
  `InferenceBackend`, `EmbeddingService`, and Vert.x WebClient boundaries.
- Reintroduced `agent-adapter` to the active agent parent as a programmatic,
  SPI-only adapter module. Invalid pre-migration alias-import adapters are
  preserved under `src/legacy-main/java`; active adapters now provide in-memory
  audit/memory utilities and a skill registry facade that maps runtime
  `AgentSkill` instances to `SkillDefinition` and `SkillResult` records without
  registering duplicate CDI beans.
- Reintroduced `skill-management` and `skill-audit` to the active agent parent.
  Legacy Gollek-package repository, validation, versioning, analytics, and RBAC
  sources are preserved under `src/legacy-main/java`; active management now
  provides lifecycle operations over the current `SkillRegistry`, while active
  audit provides independent in-memory audit trail and usage analytics utilities.
- Reintroduced `agent-api` to the active agent parent. The REST resource now
  lives under the matching `tech.kayys.wayang.agent.api` package and targets the
  current `AgentClient`, `AgentRequest`, `AgentResponse`, and `SkillRegistry`
  contracts instead of the removed `agent.core.spi` package.
- Reintroduced `skills-cli` and `agent-examples` to the active agent parent.
  Legacy Gollek-package CLI code and old example agents are preserved under
  `src/legacy-main/java`; active CLI code now wraps `SkillManagementService`
  through Picocli, and examples now exercise the current `AgentClient` with a
  deterministic in-memory inference backend.
- Reintroduced MCP as an explicit `agent-mcp` module. The loose
  `agent/src/main/java/tech/kayys/gollek/agent/mcp` bridge is preserved under
  `agent-mcp/src/legacy-main/java`; active code now adapts discovered MCP tool
  descriptors to map-based `AgentSkill` instances through a small
  client-agnostic `McpToolClient` boundary.
- Added an agent-side MCP invocation/result contract. `McpToolInvocation`
  carries immutable tool arguments plus transport context derived from
  `McpServerConfig`, `McpTransportContext` centralizes HTTP/SSE/stdio-ready
  context keys, and `McpToolCallResult` can now carry immutable transport
  metadata back through `McpSkillAdapter`.
- Added `HttpMcpToolClient` as the first concrete agent-side MCP transport. It
  lists remote tools through JSON-RPC `tools/list`, calls tools through
  `tools/call`, accepts JSON and simple SSE `data:` response bodies, surfaces
  JSON-RPC errors as `McpToolCallResult` failures, and remains behind the
  `McpToolClient` boundary.
- Added a matching tool-side MCP client boundary in `wayang-tool-mcp`. Gamelan MCP
  node execution and the active tool SPI wrapper now call `McpToolClient`
  implementations and surface structured success/failure results instead of
  placeholder responses.
- Wired `wayang-tool-mcp` tool listing and lookup endpoints to the active
  repository-backed tool registry. The MCP REST surface now projects tenant-scoped
  tools from persisted registry state instead of returning hard-coded empty/404
  responses.
- Added `HttpMcpToolClient` as the first concrete tool-side MCP transport. It
  posts JSON-RPC `tools/call` requests to context-provided HTTP endpoints,
  supports JSON and simple server-sent-event response bodies, maps protocol
  errors to `McpToolCallResult` failures, and remains replaceable through the
  `McpToolClient` boundary.
- Added live MCP tool discovery for HTTP servers. `wayang-tool-mcp` now has a
  shared HTTP JSON-RPC helper, a separate `McpToolDiscoveryClient` boundary, an
  `HttpMcpToolDiscoveryClient` implementation that performs `initialize`,
  `notifications/initialized`, and paginated `tools/list`, plus a REST endpoint
  for discovering remote tools without coupling discovery to execution.
- Added durable MCP discovery import. Live-discovered tools can now be upserted
  into the tenant tool registry through a dedicated import service and REST
  endpoint; imported rows preserve the MCP endpoint, headers, timeout, remote
  tool name, schemas, read-only hints, and capabilities so registry-backed
  `McpTool` wrappers can execute with persisted defaults.
- Connected MCP discovery import to the tenant MCP server registry. Import
  requests can now omit explicit endpoint details when a registered HTTP/SSE MCP
  server name is provided; the import service resolves the registry URL, rejects
  missing/disabled/non-HTTP servers with structured failures, and updates the
  registry sync timestamp after a successful import.
- Added scheduled live MCP tool discovery refresh. `wayang-tool-mcp` now scans
  scheduled MCP server registry entries, runs due HTTP/SSE servers through the
  discovery-import path, records `MCP_TOOLS` sync history, exposes a manual
  trigger endpoint, and keeps the live protocol scheduler separate from the
  core OpenAPI/MCP-JSON registry scheduler.
- Added live MCP tool drift detection. Discovery import now compares the latest
  `tools/list` result against existing imported tools for the same server or
  endpoint, disables missing tools as stale, preserves unrelated tools, and
  reports stale tool ids in the import response.
- Reintroduced `memory-runtime` to the active memory parent. The runtime POM no
  longer depends on inactive Gollek memory slice artifacts or old SDKs; it now
  serves as a REST facade over `wayang-memory-core`, with stale slice executor
  tests preserved under `src/legacy-test/java`.
- Reintroduced short-term and working-memory slices to the active memory parent.
  Their artifacts are now Wayang-owned (`wayang-memory-short` and
  `wayang-memory-working`), the stale `tech.kayys.gollek:gollek-spi`
  dependency has been removed, and both modules compile against active
  `memory-core`, Gamelan executor, and Wayang node-provider contracts.
- Reintroduced episodic, semantic, and long-term memory slices to the active
  memory parent. Their artifacts are now Wayang-owned
  (`wayang-memory-episodic`, `wayang-memory-semantic`, and
  `wayang-memory-longterm`), duplicate entity code in episodic memory is parked
  under `src/legacy-main/java`, stale Gollek/schema/vector dependencies are
  removed, and focused tests now cover executor metadata, node definitions,
  semantic category grouping, and long-term vector type mapping.
- Reintroduced `golek-memory-context-engineering` as a Wayang-owned
  compatibility artifact (`wayang-memory-context-engineering`). The duplicate
  `CompressedContext` class is parked under `src/legacy-main/java`; active
  consumers use the canonical context-engineering API from `wayang-memory-core`.
- Reintroduced `gollek-plugin-memory-integration` as a Wayang-owned
  integration artifact (`wayang-memory-integration`). The old Gollek
  `InferencePhasePlugin` source and tests are parked under legacy source roots;
  active code now provides `MemoryIntegrationService`, a framework-neutral
  bridge that enriches Wayang `AgentRequest` and `InferenceRequest` objects
  with retrieved memory context.
- Added a top-level `wayang-gollek-cli` product module. The new CLI keeps
  Wayang separate from Gollek's own inference/training CLI, exposes
  platform status/run/TUI commands, and uses Tamboui behind a Wayang-owned
  terminal renderer boundary instead of making Tamboui the platform contract.
- Added the initial CLI product-surface catalog for Wayang as a reusable
  core agent engine. The CLI now names coding-agent, assistant-agent, low-code
  workflow, and platform-admin surfaces as consumers of the same agents, skills,
  tools, MCP, RAG, memory, workflow, and harness boundaries.
- Routed CLI run requests through `AgentRequest` via `WayangAgentRequestMapper`,
  so the product CLI already crosses the active agent SPI instead of carrying a
  parallel request model that could drift from the core engine.
- Added a Wayang-owned agent workbench model and renderer seam. Plain terminal
  output and Tamboui now consume the same `WayangWorkbenchModel`, keeping the
  CLI future-proof for Gemini CLI/Claude Code style products, CI logs, richer
  terminal dashboards, and API-backed launchers without making Tamboui the core
  platform model.
- Added `wayang-gollek-sdk` as the shared source of truth for Wayang
  agent products, similar to `gollek/sdk` for Gollek inference clients. The SDK
  now owns agent run requests/results, platform status, product surfaces,
  workbench state, provider/factory hooks, and the mapper into core
  `AgentRequest`; the CLI consumes the SDK instead of carrying duplicate agent
  models.
- Shortened the user-facing agent platform name to `Wayang`. The CLI command is
  now `wayang`, with `wayang-gollek` preserved as a compatibility alias; Maven
  artifact and module names remain stable for now.
- Added SDK provider discovery and CLI SDK selection. The local SDK is now
  registered through `ServiceLoader`, and root CLI options can select local or
  remote SDK mode plus default tenant/model values before handing execution to
  subcommands. Remote mode intentionally fails clearly until a remote provider
  module is installed.
- Added `wayang-gollek-sdk-remote`, a separate ServiceLoader remote provider
  module. It supplies an HTTP-backed `WayangGollekSdk` for `GET /status` and
  `POST /runs`, keeps remote transport out of the default CLI jar, and tests the
  API boundary through an in-process HTTP server.
- Added `wayang-gollek-cli-remote`, an executable remote-enabled CLI
  distribution. It shades the standard CLI together with the remote SDK provider
  and merges ServiceLoader metadata, so API-backed installs can use
  `--sdk-mode REMOTE` without bloating the default local-first CLI jar.
- Added SDK-owned workspace inspection for coding-agent context. The local SDK
  now detects workspace existence, git root/branch, build descriptors, package
  managers, Maven modules, and top-level paths; the CLI exposes this through
  `wayang workspace`, and the remote SDK has a matching `/workspace/inspect`
  transport hook.
- Added `WayangA2aJsonRpcDiagnosticsReportContext` as the A2A diagnostics
  report input boundary. Report assembly now normalizes readiness/config/spec
  alignment once, precomputes the shared readiness issue breakdown and summary,
  and passes those prepared values into status, issues, attributes, and
  diagnostic checks without recomputing issue grouping through helper paths.
- Added `WayangA2aTaskSnapshots` as the reusable A2A task snapshot mutation
  boundary. The in-memory task store now delegates status replacement,
  message append, and artifact append projection to the shared helper, keeping
  lifecycle validation, storage, and immutable task reconstruction separated for
  future file/JDBC/object-store task adapters.
- Added `WayangA2aTaskEventCursor` and `WayangA2aTaskEventStreams` as the A2A
  task event replay boundary. Event cursor defaulting, limit clamping, replay
  slicing, and HTTP/JSON-RPC SSE rendering now live outside the in-memory store
  and transport handlers, preparing the same stream contract for future durable
  task stores.
- Added `WayangA2aPushNotificationConfigCommands` as the shared A2A push
  notification config command boundary. HTTP and JSON-RPC handlers now reuse
  one store-backed create/get/list/delete path plus transport-specific list and
  delete result projections while keeping their own error envelopes.
- Added `WayangA2aTaskListView` as the shared A2A task-list projection
  boundary. HTTP and JSON-RPC task list handlers now use one store/query view
  for task map projection, counts, page size, and placeholder cursor metadata
  so future pagination or durable stores do not duplicate envelope logic.
- Added `WayangA2aTaskCancelRequest` as the shared A2A task cancellation
  request boundary. HTTP and JSON-RPC cancel handlers now reuse one task-id,
  reason/message projection, and store-apply path while retaining their
  transport-specific visibility checks and error envelopes.
- Added `WayangA2aTaskSubscriptionRequest` as the shared A2A task event
  subscription boundary. HTTP and JSON-RPC subscribe handlers now reuse one
  task-id, cursor, event-fetch, and terminal-policy model while preserving HTTP
  terminal replay and JSON-RPC terminal rejection behavior.

## Active Reactor Boundary

The active reactor is:

- `agent`
- `graph`
- `guardrails`
- `hitl`
- `memory`
- `prompt`
- `rag`
- `tools`
- `vector`
- `embedding`
- `storage`
- `runtime-quarkus`
- `wayang-gollek-sdk`
- `wayang-gollek-sdk-remote`
- `wayang-gollek-cli`
- `wayang-gollek-cli-remote`

The top-level `skills`, `agent-gollek`, `gollek-runtime-*`, and `enhancement`
directories remain outside the active reactor for now. `skills/skill-spi` has
been normalized as a standalone skills-parent module, but the broader `skills`
tree should still be reintroduced module-by-module through explicit migration.

## Verified

These commands pass:

```bash
mvn -q validate -DskipTests
mvn -q -pl agent/agent-spi,tools/tools-spi -am compile -DskipTests
mvn -q -pl agent/agent-core -am compile -DskipTests
mvn -q -pl agent/agent-core -am test-compile -DskipTests
mvn -q -pl agent/agent-core -am test
mvn -q -f agent/agent-orchestration/pom.xml compile -DskipTests
mvn -q -pl agent/agent-orchestration -am compile -DskipTests
mvn -q -pl agent/agent-orchestration -am test-compile -DskipTests
mvn -q -pl agent/agent-orchestration -am test
mvn -q -f agent/agent-backend-gollek/pom.xml compile -DskipTests
mvn -q -f agent/agent-backend-gollek/pom.xml test
mvn -q -pl agent/agent-backend-gollek -am test
mvn -q -f agent/agent-backend-gamelan/pom.xml compile -DskipTests
mvn -q -f agent/agent-backend-gamelan/pom.xml test
mvn -q -pl agent/agent-backend-gamelan -am test
mvn -q -pl agent/agent-integration-gamelan -am compile -DskipTests
mvn -q -pl agent/agent-integration-gamelan -am test
mvn -q -pl agent/agent-skills-builtin -am compile -DskipTests
mvn -q -pl agent/agent-skills-builtin -am test
mvn -q -f agent/agent-adapter/pom.xml test
mvn -q -pl agent/agent-adapter -am test
mvn -q -pl agent/agent-mcp -am test
mvn -q -f agent/skill-management/pom.xml test
mvn -q -f agent/skill-audit/pom.xml test
mvn -q -pl agent/skill-management -am test
mvn -q -pl agent/skill-audit -am test
mvn -q -pl agent/agent-api -am compile -DskipTests
mvn -q -pl agent/agent-api -am test
mvn -q -pl agent/skills-cli -am test
mvn -q -pl agent/agent-examples -am test
mvn -q -pl agent -am test-compile -DskipTests
mvn -q -pl tools/wayang-tool-core -am compile -DskipTests
mvn -q -pl tools/wayang-tool-parser -am compile -DskipTests
mvn -q -pl tools/wayang-tool-parser -am test-compile -DskipTests
mvn -q -pl tools/tools-spi clean compile -DskipTests
mvn -q -pl tools/wayang-tool-core -am compile -DskipTests
mvn -q -pl tools/wayang-tool-core -am test-compile -DskipTests
mvn -q -pl tools/wayang-tool-mcp -am compile -DskipTests
mvn -q -pl tools/wayang-tool-mcp -am test-compile -DskipTests
mvn -q -pl tools/wayang-tool-utcp -am compile -DskipTests
mvn -q -pl tools/wayang-tool-utcp -am test-compile -DskipTests
mvn -q -pl tools/golek-tool-execution -am compile -DskipTests
mvn -q -pl tools/golek-tool-execution -am test-compile -DskipTests
mvn -q -pl tools/golek-tool-registry -am compile -DskipTests
mvn -q -pl tools/golek-tool-registry -am test-compile -DskipTests
mvn -q -pl tools/wayang-tool-runtime -am compile -DskipTests
mvn -q -f tools/pom.xml compile -DskipTests
mvn -q -f tools/pom.xml test-compile -DskipTests
mvn -q -pl agent/agent-core -am compile -DskipTests
mvn -q -pl agent/agent-shaker -am compile -DskipTests
mvn -q -pl memory/memory-core -am compile -DskipTests
mvn -q -pl memory/golek-memory-context-engineering -am test
mvn -q -pl memory/golek-memory-short -am test
mvn -q -pl memory/golek-memory-working -am test
mvn -q -pl memory/golek-memory-episodic -am test
mvn -q -pl memory/golek-memory-semantic -am test
mvn -q -pl memory/golek-memory-longterm -am test
mvn -q -pl memory/gollek-plugin-memory-integration -am test
mvn -q -pl memory/vector-memory-integration -am compile -DskipTests
mvn -q -pl memory/graph-memory-integration -am compile -DskipTests
mvn -q -pl memory/memory-runtime -am test
mvn -q -pl memory -am test-compile -DskipTests
mvn -q -pl rag/rag-core -am test
mvn -q -pl rag/rag-runtime -am test
mvn -q -pl rag/rag-memory-integration -am test
mvn -q -pl rag/rag-graph-integration -am compile -DskipTests
mvn -q -pl rag -am test-compile -DskipTests
mvn -q -f storage/pom.xml compile -DskipTests
mvn -q -f storage/pom.xml test-compile -DskipTests
mvn -q -pl runtime-quarkus -am compile -DskipTests
mvn -q -pl runtime-quarkus -am test-compile -DskipTests
mvn -q -pl tools/wayang-tool-core -am test
mvn -q compile -DskipTests
mvn -q -pl wayang-gollek-sdk-remote -am test
mvn -q -pl wayang-gollek-cli-remote -am test
mvn -q -pl wayang-gollek-sdk,wayang-gollek-sdk-remote,wayang-gollek-cli,wayang-gollek-cli-remote -am test
mvn -q -pl wayang-gollek-sdk,wayang-gollek-sdk-remote,wayang-gollek-cli,wayang-gollek-cli-remote -am package -DskipTests
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar status --json
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar workbench
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar workspace --path . --max-entries 20
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar run "plan coding agent architecture" --tenant tenant-a --model qwen --workflow planner --skill rag --max-steps 3
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar commands --category "Run Specs"
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar commands --index --json
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar commands --id run-print-spec-output --json
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar workbench --surface assistant-agent --category Runs --id run-session-context --json
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar spec template --surface assistant-agent --output /private/tmp/wayang-spec-template-smoke.properties --force
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar run "output smoke" --surface assistant-agent --print-spec --output /private/tmp/wayang-run-output-smoke.properties --force
rg -n "surfaceId=assistant-agent|skills=memory,rag,mcp" /private/tmp/wayang-spec-template-smoke.properties
rg -n "prompt=output smoke|surfaceId=assistant-agent" /private/tmp/wayang-run-output-smoke.properties
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar --default-tenant tenant-b --default-model model-b run "draft a workflow"
java -jar wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar --sdk-mode REMOTE --endpoint https://wayang.example.test status
java -jar wayang-gollek-cli-remote/target/wayang-gollek-cli-remote-1.0.0-SNAPSHOT.jar --help
java -jar wayang-gollek-cli-remote/target/wayang-gollek-cli-remote-1.0.0-SNAPSHOT.jar --sdk-mode REMOTE status
jar tf wayang-gollek-sdk/target/wayang-gollek-sdk-1.0.0-SNAPSHOT.jar | rg 'META-INF/services|LocalWayangGollekSdkProvider'
jar tf wayang-gollek-sdk-remote/target/wayang-gollek-sdk-remote-1.0.0-SNAPSHOT.jar | rg 'META-INF/services|RemoteWayangGollekSdkProvider|HttpWayangRemoteTransport'
jar tf wayang-gollek-cli-remote/target/wayang-gollek-cli-remote-1.0.0-SNAPSHOT.jar | rg 'META-INF/services/tech.kayys.wayang.gollek.sdk.WayangGollekSdkProvider|RemoteWayangGollekSdkProvider|LocalWayangGollekSdkProvider|WayangGollekCli.class'
mvn -q -f skills/skill-spi/pom.xml compile -DskipTests
mvn -q -f skills/pom.xml -pl skill-spi compile -DskipTests
mvn -q -f skills/pom.xml compile -DskipTests
```

## Module Skeleton Rules

- Source directories should mirror declared Java packages. Stale package paths
  are treated as migration debt even when Maven can compile them.
- Public SPI contracts live in focused SPI modules such as `agent-spi`,
  `tools/tools-spi`, and `storage/storage-spi`; concrete runtime services stay
  in implementation/provider modules such as `agent-core`,
  `tools/wayang-tool-core`, or storage provider adapters.
- Compatibility wrappers may remain temporarily, but they should be marked
  deprecated and delegate to the canonical implementation instead of duplicating
  behavior.
- Adapter names should describe the boundary they cross, for example runtime
  skill definitions vs filesystem skill manifests.

## Remaining Compile Migration

The active parent reactor now compiles with `-DskipTests`. The second pass
focused on:

- Consolidating `agent-core` imports onto `agent-spi`, `agent-spi.skills`,
  `agent-spi.memory`, and the active tool SPI.
- Adding narrow compatibility accessors for migrated records that older
  integration code still calls while the broader API is normalized.
- Rebuilding `tools/tools-spi` so its emitted bytecode matches the active
  `tech.kayys.wayang.tools.spi` package declarations.
- Adding lightweight core response/config types needed by the unified inference
  service and pure Java client.
- Updating Mutiny calls and Gollek SDK provider/model/MCP signatures to the
  active APIs.
- Migrating storage providers from stale Gollek plugin/SPI packages to the
  active Wayang storage SPI and provider module skeleton.
- Isolating stale `agent-core` tests under `src/legacy-test/java` and keeping
  only current-boundary tests in the Maven-compiled test source tree.
- Isolating stale `agent-orchestration` runtime loops under
  `src/legacy-main/java` and keeping the active module focused on
  provider-neutral orchestration utilities.
- Rebuilding the Gollek backend adapter against the current Gollek SDK/SPI
  rather than the removed intermediate inference/provider/tool packages.
- Rebuilding the Gamelan backend adapter against the current Gamelan SDK/SPI
  rather than older workflow client method names and record-style DTO accessors.
- Rebuilding the Gamelan integration module against the current Gamelan SDK,
  active agent memory service, and active Wayang graph module instead of stale
  package names and old fluent client methods.
- Rebuilding built-in agent skills against the active map-based `AgentSkill`
  contract, preserving old context/result-based implementations as legacy
  source and anchoring inference, summarization, embedding, RAG, HTTP, and code
  execution skills on current core boundaries.
- Rebuilding `agent-adapter` as an SPI-only programmatic adapter layer, removing
  stale non-reactor skill-management/audit dependencies and centralizing
  runtime-skill-to-definition/result conversion.
- Rebuilding skill lifecycle support as separated management and audit modules:
  management owns registry-backed CRUD/status/search operations, and audit owns
  audit trail plus usage analytics without depending back on management.
- Rebuilding `agent-api` as a REST layer over active agent execution and skill
  registry contracts, with request mapping and response/health summaries covered
  by resource-level tests.
- Rebuilding `skills-cli` as a narrow command-line lifecycle wrapper over the
  active skill registry/management contracts, with old SKILL.md/Git-oriented
  implementation details kept as legacy source until a dedicated repository
  service boundary exists.
- Rebuilding `agent-examples` as compile-ready, deterministic demonstrations of
  `AgentClient`, `InferenceBackend`, data-driven `SkillDefinition`, and runtime
  `AgentSkill` contracts.
- Rebuilding MCP integration as an explicit agent-side adapter module:
  `agent-mcp` owns server/tool descriptor records, a transport-neutral
  `McpToolClient`, and the `McpSkillBridge` that registers MCP tools as active
  runtime skills.
- Rebuilding `memory-runtime` as a lightweight REST runtime facade over active
  `memory-core` services, removing direct dependencies on specialized memory
  slice modules.
- Rebuilding the short-term and working-memory slice modules as active
  executor/node-provider packages over the shared `AbstractMemoryExecutor`
  base, with focused tests for windowed context, attention-ranked working
  memory, and node definition exposure.
- Rebuilding the episodic, semantic, and long-term memory slice modules as
  active executor/node-provider packages over the same memory-core contracts,
  including tests for event metadata, stable search output, semantic category
  grouping, long-term persistence metadata, and node definition exposure.
- Rebuilding `golek-memory-context-engineering` as a thin compatibility module
  over canonical `memory-core` context-engineering classes instead of compiling
  duplicate DTOs into a second artifact.
- Rebuilding `gollek-plugin-memory-integration` as an active Wayang memory
  integration module over `AgentMemory`, `AgentRequest`, and `InferenceRequest`
  contracts, keeping the Gollek phase-plugin implementation as legacy source.
- Rebuilding RAG response generation over `InferenceBackend`, with tests for
  message construction, provider/API-key metadata handoff, citation generation,
  guardrail sanitization, token accounting, and response caching.
- Consolidating built-in RAG plugin text/metadata helpers behind the
  `rag-plugin-api` support boundary so plugin implementations stay focused on
  pipeline hooks and can be used by CDI, tests, and programmatic catalogs.
- Rebuilding RAG memory integration behind an explicit memory retrieval
  boundary, with tests for weighted top-k allocation, metadata preservation,
  filter handoff, and zero-top-k guard behavior.
- Rebuilding native RAG generation mode selection so provider strings can remain
  provider identifiers while local extractive/context rendering is selected via
  generation metadata and surfaced in response metadata.
- Centralizing native RAG pipeline construction behind a runtime factory so
  future generator/provider adapters can plug into one assembly point instead of
  duplicating retrieval and indexing setup.
- Stabilizing native text ingestion identities by carrying parser source values
  into chunk document IDs for retrieval, citation, and evaluation flows.
- Aligning native RAG query execution with the plugin lifecycle so retrieval
  filters/rerankers/redactors affect the generation context instead of only the
  response chunk list.
- Consolidating native RAG request normalization and plugin-context mapping into
  a package-local runtime context so query and retrieve paths share the same
  defaults, filters, and plugin mutation semantics.
- Consolidating high-level RAG query workflow defaults into a service-local
  context so simple, advanced, and conversational entry points share collection
  normalization, native filter construction, and metadata serialization inputs.
- Consolidating RAG ingestion setup into a package-local context so document
  metadata/source construction, chunking defaults, null handling, and batch
  metrics stay outside the public ingestion orchestration methods.
- Separating RAG query response assembly from query orchestration so response
  metadata, source documents, scoring metrics, and malformed chunk handling have
  one package-local implementation and direct tests.
- Separating RAG workflow input mapping from execution so workflow entry points
  no longer lose retrieval/generation configs when they cross into the native
  query service.
- Consolidating the native RAG retrieval plugin lifecycle so query and retrieve
  execution share the same `beforeQuery` and `afterRetrieve` behavior, including
  null-safe chunk handling.
- Consolidating native ingestion chunking translation so runtime-facing
  `ChunkingConfig` values are defaulted and sanitized before becoming core
  `ChunkingOptions`.
- Separating native RAG pipeline assembly from CDI factory wiring so namespace
  defaults, embedding model defaults, retrieval-only generation fallback, and
  indexer/retriever construction stay in one tested package-local boundary.
- Consolidating native RAG runtime defaults so namespace, default collection,
  collection metadata key, and collection-list normalization remain aligned
  across ingestion, workflow mapping, and pipeline assembly.
- Consolidating RAG metadata/filter key construction in `rag-core` so runtime,
  native indexing, vector retrieval, and vector-store contract checks no longer
  maintain parallel string literals for tenant/source/embedding/chunk metadata.
- Separating RAG response metadata serialization from response assembly so
  public response metadata keys and config serialization stay stable while the
  query orchestration internals continue moving.
- Consolidating scored-chunk sanitation and score math so retrieval plugins,
  native generation, and response metrics cannot drift on malformed chunk
  handling.
- Separating RAG source-document projection from response assembly so metadata
  conversion and source/title fallback remain stable as response shaping keeps
  shrinking.
- Separating RAG response metric shaping from response assembly so score math
  and default metric fields have one local contract.
- Separating RAG response content normalization from response assembly so
  null-safe answer/context defaults and result chunk extraction no longer live
  behind the assembler.
- Separating conversational RAG query rendering from workflow context defaults
  so session/history metadata and prompt text formatting can evolve without
  expanding the workflow context record.
- Consolidating workflow filter handling so request mapping and native query
  execution do not maintain separate filter-copying and collection override
  rules.
- Separating ingestion metadata shaping from ingestion document creation so
  PDF/text source fallback, collection defaults, and defensive metadata copying
  remain aligned.
- Consolidating RAG plugin-selection config parsing so strategy resolution,
  active plugin selection, and admin inspection stay aligned on tenant keys,
  wildcard enabled IDs, and plugin order semantics.
- Separating RAG plugin eligibility from manager orchestration so hook
  execution, active plugin IDs, and admin inspection use the same enabled,
  tenant-support, and ordering rules.
- Separating RAG plugin hook execution from manager orchestration so null-return
  fallback, exception fallback, and defensive retrieve chunk copies stay aligned
  across every hook stage.
- Separating RAG plugin admin inspection from manager orchestration so active
  plugin IDs and status rows are projected through one reporting boundary.
- Consolidating plugin admin status value invariants so tenant normalization,
  list immutability, and observed-at defaults are enforced by the response type.
- Consolidating RAG plugin discovery ordering so CDI-discovered and test-supplied
  plugins cannot drift on null filtering, stable sorting, or ID normalization.
- Separating RAG plugin strategy lookup from tenant strategy resolution so
  future custom selection strategies share one indexing, default fallback, and
  known-strategy path.
- Consolidating runtime metadata copying so workflow filters, ingestion
  documents, response envelopes, and native retrieval results do not hand-roll
  separate null/default/immutability behavior.
- Consolidating document source metadata handling so batch ingestion sources
  cannot retain caller-owned maps or null metadata entries before the ingestion
  metadata shaper runs.
- Consolidating advanced query request invariants so external callers cannot
  mutate filter maps or pass unnormalized collection lists deeper into the
  workflow runtime.
- Separating admin API key slot resolution from JAX-RS filters so future key
  rotation, audit, and response-header behavior have one narrow policy
  boundary.

Recommended next step: move outward from `agent-core`, `agent-orchestration`,
`agent-backend-gollek`, `agent-backend-gamelan`, `agent-integration-gamelan`,
`agent-skills-builtin`, `agent-adapter`, `skill-management`, `skill-audit`,
`agent-mcp`, `agent-api`, `skills-cli`, and `agent-examples` into the remaining
RAG runtime/provider wiring and legacy tool plugin bridges. Any compatibility
methods and wrappers added here should be treated as transitional and collapsed
once callers are migrated.

Known orchestration migration gap: the old ReAct, Reflexion, native tool
calling, and local Gollek SDK loops are preserved under
`agent/agent-orchestration/src/legacy-main/java` and are not compiled. Migrate
their behavior back only through active `agent-spi`, `agent-core`, and
`tools-spi` contracts.

Known Gollek backend migration gap: the stale adapter that targeted removed
`tech.kayys.wayang.agent.spi.inference`, `agent.spi.provider`, and
`agent.spi.tool` packages is preserved under
`agent/agent-backend-gollek/src/legacy-main/java` and is not compiled. Keep new
provider work on the active `InferenceBackend` and Gollek SDK/SPI boundary.

Known Gamelan backend migration gap: the stale adapter that targeted older
Gamelan workflow client methods and record-style response accessors is
preserved under `agent/agent-backend-gamelan/src/legacy-main/java` and is not
compiled. Keep new provider work on the active `WorkflowBackend` and Gamelan
SDK/SPI boundary.

Known built-in skill migration gap: `agent-skills-builtin` compiles and tests
against active contracts. The RAG skill now calls the agent SPI
`RagSkillRetriever` boundary for collection/top-k/filter-aware context retrieval
while keeping caller-supplied context as the fallback path. `rag-runtime`
provides `NativeRagSkillRetriever`, which delegates to the native RAG retrieval
pipeline and projects scored chunks back into skill source documents without
embedding vector-store logic directly in the skill.

Known test migration gap: `agent-core` test compilation now passes, but the
pre-reorganization tests under `agent/agent-core/src/legacy-test/java` are not
part of Maven's active test source tree. The first skill adapter contract has
been migrated back through `SkillAsToolAdapterContractTest`, and the MCP skill
provider contract is now active through `MCPSkillProviderContractTest`. Prompt
context coverage is active again through `PromptContextProviderContractTest`,
and memory-provider coverage is active through `SkillMemoryProviderContractTest`,
with safety guardrail coverage active through `SkillSafetyValidatorContractTest`,
RAG context coverage active through `RAGAwareSkillContextContractTest`, vector
indexing coverage active through `VectorSkillIndexerContractTest`, and HITL
approval/refinement coverage active through `HITLSkillExecutorContractTest`,
with integration registry lifecycle/factory/refresh-diff coverage active through
`SkillIntegrationRegistryContractTest`,
filesystem manifest-to-runtime-skill projection coverage active through
`SkillManifestRuntimeSkillMapperContractTest` and
`SkillManifestRegistrySyncContractTest`,
and agent HITL workflow-state coverage active through
`AgentHitlServiceContractTest`,
with audit event/sink coverage active through `AgentAuditLoggerContractTest`,
and observability memory/span coverage active through
`AgentTelemetryContractTest`,
and skill manifest/parameter-schema validation coverage active through
`SkillValidatorContractTest`, `SkillMetadataKeysContractTest`, and
`SkillContextKeysContractTest`,
with manifest-to-tool schema projection coverage active through
`ManifestSkillToolAdapterContractTest`,
MCP resource/result payload projection active through `McpSkillPayloadsContractTest`,
shared adapter result projection active through `SkillResultPayloadsContractTest`,
runtime skill tool descriptor/schema projection active through
`SkillToolDescriptorsContractTest`,
and filesystem skill process-argument/lifecycle/layout/result-contract/failure-reporting
coverage active through `SkillExecutorContractTest`,
`SkillExecutableResolverContractTest`,
`FilesystemSkillRuntimeContractTest`,
`SkillManifestCatalogContractTest`,
including immutable manifest snapshot and reload-diff checks,
`SkillExecutorInjectionContractTest`,
`SkillExecutionResultsContractTest`,
`SkillExecutionMetadataContractTest`,
`SkillExecutionOutcomesContractTest` failure-taxonomy checks, and
`SkillAwareToolOrchestratorContractTest` domain initialization and lifecycle
refresh coordination checks,
backed by the reusable `TestSkillContexts` and `TestSkillRegistry` fixtures.
Continue moving the remaining legacy adapter, inference, and orchestration
expectations back one boundary at a time.

Known MCP transport gap: `agent-mcp` now has immutable tool invocation/result
contracts, transport context keys, and the first HTTP JSON-RPC `McpToolClient`,
while `wayang-tool-mcp` has the matching tool-side HTTP client. SSE and stdio
client implementations still need to plug into the same boundaries. Keep
protocol transport details outside `agent-core` and wire them through the active
MCP modules.

Known legacy tool-plugin gap: `tools/gollek-plugin-tool-execution` is outside
the active tools parent because it still depends on unavailable
`tech.kayys.gollek` inference/plugin artifacts. Reintroduce it only after the
tool execution plugin surface is mapped to the active Wayang tool SPI and
Gamelan plugin contracts.

Known agent legacy gap: all compile-ready modules under `agent/` are now back in
the active agent parent. Legacy behavior remains parked under module-local
`src/legacy-main` trees where it still depends on old Gollek/Gamelan SDK shapes,
pre-reorganization package names, missing local artifacts, or older skill/core
contracts.

Known memory migration gap: all memory modules are now in the active memory
parent. Legacy Gollek phase-plugin behavior remains parked under
`memory/gollek-plugin-memory-integration/src/legacy-main/java`, and legacy
runtime executor tests remain parked under
`memory/memory-runtime/src/legacy-test/java`.

Recent Wayang CLI lifecycle improvement: local run stores now support forgetting
one recorded snapshot through the SDK store boundary and `wayang run forget
<run-id> --json`. The command is a local-history maintenance operation, not a
remote cancellation primitive, so the workbench catalog exposes it as a local
command while remote lifecycle status/history remain delegated to remote API
endpoints.

Recent Wayang run cancellation improvement: the SDK now has an
`AgentRunCancelResult` contract, a terminal `CANCELLED` run state, local store
cancellation for non-terminal snapshots, and `wayang run cancel <run-id>
--reason <text> --json`. Remote SDK mode maps the same verb to
`POST /runs/{runId}/cancel`, keeping cancellation separate from local
history-forget behavior.

Recent Wayang run waiting improvement: the SDK now owns an `AgentRunWaitOptions`
and `AgentRunWaitResult` polling contract, surfaced through `wayang run wait
<run-id> --timeout-seconds <n> --json`. The wait loop is implemented once on
the SDK interface and uses `runStatus`, so remote mode inherits it through the
existing status endpoint while local mode can return immediately for terminal
snapshots or clearly report timeout/unknown states.

Recent Wayang run history filtering improvement: `AgentRunHistoryQuery` now
includes tenant, session, and surface filters in addition to state and limit.
`wayang run list --tenant <id> --session <id> --surface <id> --json` filters the
local store through the same SDK query object, and remote mode forwards those
filters as query parameters on `GET /runs`.

Recent Wayang run event timeline improvement: `AgentRunEvent` and
`AgentRunEvents` now expose a stable lifecycle timeline above the run store.
Local memory/file stores append events whenever snapshots change, `wayang run
events <run-id> --json` renders the timeline for agent shells, and remote mode
delegates the same contract to `GET /runs/{runId}/events`.

Recent Wayang run event query improvement: `AgentRunEventsQuery` now gives the
timeline contract state, type, and limit filters. Local stores return the latest
matching events while preserving chronological order, `wayang run events
<run-id> --state completed --type run.completed --limit 20 --json` exposes the
same query to CLI users, and remote mode forwards filters as event endpoint
query parameters.

Recent Wayang run event cursor improvement: `AgentRunEventsQuery` now includes
an `afterSequence` cursor. `wayang run events <run-id> --after-sequence 10
--limit 20 --json` lets agent shells poll only newer lifecycle events, and
remote mode forwards the cursor as `afterSequence` on the event endpoint.

Recent Wayang run event cursor response improvement: `AgentRunEvents` now
derives `lastSequence`, `nextAfterSequence`, and `truncated` values from the
returned timeline. CLI JSON exposes those fields so terminal UIs and product
shells can advance polling cursors without scanning every event payload.

Recent Wayang run event summary improvement: `AgentRunEvents` now derives
returned-window `stateCounts` and `typeCounts`. CLI JSON exposes both maps so
agent dashboards can render lifecycle chips and event facets without duplicating
counting logic in every product shell.

Recent Wayang run inspection improvement: `AgentRunInspection` now combines the
status snapshot and filtered event timeline in one SDK envelope. `wayang run
inspect <run-id> --json` exposes that shape for product shells that need a
single lifecycle payload instead of separate status and event calls.

Recent Wayang run history summary improvement: `AgentRunHistory` now derives
`returnedRuns`, `truncated`, `stateCounts`, `surfaceCounts`, and
`strategyCounts` over the returned run window. `wayang run list --json` exposes
those fields so dashboards can summarize lifecycle history without re-counting
status snapshots.

Recent Wayang run history pagination improvement: `AgentRunHistoryQuery` now
includes a zero-based `offset`, and `AgentRunHistory` derives `nextOffset` and
`hasMore`. `wayang run list --offset <n> --limit <n> --json` gives terminal UIs
and product shells a simple paged history contract, while remote mode forwards
the offset as a run-history query parameter.

Recent Wayang run history page metadata improvement: `AgentRunHistory` now also
derives `pageSize`, `windowStart`, `windowEnd`, `previousOffset`, and
`hasPrevious`. CLI JSON and text output expose these values so terminal and
web shells can render page ranges and previous/next controls without repeating
pagination math.

Recent Wayang run history page-envelope improvement: pagination math now lives
in the `AgentRunHistoryPage` SDK value object, and `AgentRunHistory.page()`
exposes that envelope directly. CLI JSON keeps the existing top-level fields
and adds a nested `page` object so product shells can bind one stable page model.

Recent Wayang run event cursor-envelope improvement: lifecycle event cursor math
now lives in the `AgentRunEventsCursor` SDK value object, and
`AgentRunEvents.cursor()` exposes after/next sequence state, sequence range,
remaining returned-window count, and advancement status. CLI JSON keeps the
existing event fields and adds a nested `cursor` object for pollers and
dashboards.

Recent Wayang run lifecycle summary-envelope improvement: returned-window count
maps now have SDK value objects, `AgentRunHistorySummary` and
`AgentRunEventsSummary`. `AgentRunHistory.summary()` and
`AgentRunEvents.summary()` expose stable dashboard envelopes, and CLI JSON keeps
the existing top-level count maps while adding nested `summary` objects.

Recent Wayang run stats command improvement: `wayang run stats` now exposes the
same history query filters as `run list` but returns only the `page` and
`summary` lifecycle envelopes plus top-level counts. This gives dashboards and
scripts a lighter command when they do not need run rows.

Recent skill-management observability improvement: `SkillManagementEvent`,
`SkillManagementEventSink`, and the best-effort recorder now give skill
definition mutations, lifecycle reconciliation, bootstrap, and maintenance runs
a stable event seam. Service factories propagate the sink into created services,
failure events are emitted for store-write rollback paths, and the in-memory
sink gives tests and local diagnostics a lightweight event collector.

Recent skill-management event inspection improvement: queryable event windows
now use `SkillManagementEventQuery`, `SkillManagementEventPage`, and
`SkillManagementEventSummary` to filter by operation, skill, success state, and
limit while exposing matched/returned/truncated metadata. Admin projections map
those windows to stable event DTOs so CLI and dashboard surfaces can render
recent skill operations without binding directly to sink internals.

Recent skill-management event retention improvement: event history now has a
separate read-side `SkillManagementEventReader` contract, while
`SkillManagementEventSink` remains write-only. The in-memory event collector
implements both sides, ignores null events, and keeps a bounded recent-event
window so long-running local diagnostics do not accumulate unbounded history.

Recent skill-management event fan-out improvement: `CompositeSkillManagementEventSink`
and `SkillManagementEventSink.composite(...)` now let one service publish to
multiple event targets, such as local diagnostics, audit, metrics, or persistent
history. The composite is best-effort per target, so one failing sink does not
prevent later sinks from receiving the same operation event.

Recent skill-management filesystem event history improvement:
`FileSystemSkillManagementEventStore` now implements both the sink and reader
contracts with properties-backed event files, query support, and bounded
retention. This gives local and fallback deployments restart-safe skill
operation history without requiring JDBC or object storage.

Recent skill-management event-store configuration improvement:
`SkillManagementEventStoreConfig`, parser helpers, and
`SkillManagementEventStoreFactory` now make event history selectable through the
same service config path as definition and lifecycle stores. Deployments can
choose `none`, bounded `memory`, filesystem-backed history, or a named custom
sink, and `SkillManagementServiceFactory` wires the selected sink automatically.

Recent skill-management service event-history improvement:
`SkillManagementService.eventHistory(...)` now exposes the configured readable
event history through the service itself. Memory, filesystem, and readable
custom event stores can be queried with the same event query/page model, while
write-only sinks safely return an empty history.

Recent skill-management composite event-reader improvement:
`CompositeSkillManagementEventSink` now implements the event reader contract as
well as fan-out writes. It reads from the first healthy readable delegate,
skipping write-only or failing readers, so services using composite audit,
metrics, and history sinks can still expose `eventHistory(...)`.

Recent skill-management JDBC event-history improvement:
`JdbcSkillManagementEventStore` now gives database-primary deployments a
queryable, retention-bounded event sink/reader for skill operations.
Event-store config accepts `jdbc`/`database` aliases, table/schema settings, and
max-event retention, and service factories that receive a `DataSource` wire the
same dependency into definition, lifecycle, and event persistence.

Recent skill-management object-storage event-history improvement:
`ObjectStorageSkillManagementEventStore` now persists queryable event history
through the same object-store seam used by cloud skill definitions. Event-store
config accepts object/S3/RustFS/MinIO aliases plus object-prefix and retention
settings, and service factories that receive an object store can wire skill
definitions and event history to the same cloud storage backend.

Recent skill-management hybrid event-history improvement:
Event-store config now supports `hybrid`/`composite` topologies with explicit
`primary` and `fallback` child stores. The factory maps that deployable config
onto the existing composite sink, giving operators best-effort dual writes and
read fallback across memory, filesystem, JDBC, object-storage, or custom event
history backends.

Recent skill-management event inspection improvement:
`SkillManagementEventStoreInspector` and `SkillManagementEventStoreInspection`
now make queryable event history part of aggregate management diagnostics.
`inspectManagement()` and admin projections report event-history readiness,
matched/returned counts, truncation, operation counts, and skill counts beside
definition and lifecycle store health.

Recent skill-management composite event inspection improvement:
Composite and hybrid event-history diagnostics now expose child store
inspections. Two-way composites are reported as `primary` and `fallback`, each
with its own readiness, count summary, and failure message, while the aggregate
view still reads from the first healthy readable child.

Recent skill-management event-history pruning improvement:
`SkillManagementEventPruner`, prune options, and prune results now give memory,
filesystem, JDBC, object-storage, and composite event-history stores a shared
dry-run/apply maintenance contract. `SkillManagementService.pruneEventHistory`
exposes that backend-neutral pruning path without requiring operators to reach
into store-specific files, tables, or object keys.

Recent skill-management event-prune admin projection improvement:
`SkillManagementAdminEventPruneReport` and `SkillManagementAdminViews.eventPrune`
now expose prune outcomes as stable admin DTOs, including dry-run/apply flags,
retention counts, pruned references, failures, and child results for hybrid or
composite event-history stores.

Recent skill-management maintenance event-prune policy improvement:
Maintenance plans now carry an explicit `SkillManagementEventPrunePolicy`.
`SkillManagementMaintenanceRunner` can compact event history as part of the
same definition-sync/lifecycle-reconcile workflow, while config parsers keep it
disabled by default and support nested `events.prune` retention/dry-run policy.

Recent skill-management event-prune config extraction improvement:
`SkillManagementEventPrunePolicyConfigs` now owns standalone event-history prune
policy parsing for properties, environment variables, and maps. Maintenance
config delegates to that parser for nested `events.prune` blocks, keeping prune
retention/dry-run semantics reusable outside the maintenance runner.

Recent skill-management service maintenance facade improvement:
`SkillManagementService.runMaintenance` now exposes the combined
definition-sync, lifecycle-reconcile, and optional event-prune workflow through
the main service boundary. Admin, scheduler, and REST layers can invoke one
service call without manually assembling a maintenance runner around internal
stores and event sinks.

Recent skill-management object-storage lifecycle improvement:
`ObjectStorageSkillLifecycleStateStore` now gives cloud/S3/RustFS-compatible
deployments lifecycle-state persistence through the same object-store boundary
used by skill definitions and event history. Filesystem and object-storage
lifecycle stores share `SkillLifecycleStatePropertiesCodec`, and lifecycle
store config/factory wiring accepts object-storage prefixes beside memory,
filesystem, JDBC, and custom stores.

Recent skill-management hybrid lifecycle improvement:
`HybridSkillLifecycleStateStore` now gives lifecycle state the same
primary/fallback topology available to skill definitions and event history.
Lifecycle config parses nested `primary`/`fallback` store groups, factories
compose the child stores recursively, and lifecycle inspection/admin views expose
child readiness and counts for the hybrid store.

Recent skill-management object-store boundary cleanup:
`SkillManagementObjectStore` is now the neutral cloud object persistence boundary
for definitions, lifecycle state, and event history. The old
`SkillDefinitionObjectStore` and `StorageServiceSkillDefinitionObjectStore` names
remain as deprecated compatibility aliases, while factories and object-backed
stores depend on the management-wide boundary.

Recent skill-management object-key normalization improvement:
`SkillManagementObjectKeys` now owns object-storage prefix and skill-id key
normalization for definition, lifecycle, and event-history stores. This removes
duplicated prefix/id guards from each object-backed store and keeps S3/RustFS/MinIO
key rules consistent as more object-storage strategies are added.

Recent skill-management filesystem filename normalization improvement:
`SkillManagementFileNames` now owns safe skill-id-to-file mapping for filesystem
definition and lifecycle stores. Local fallback persistence now shares the same
blank, path-separator, and traversal guardrails instead of duplicating filename
rules in each store.

Recent skill-management skill-id storage normalization improvement:
`SkillManagementSkillIds` now owns the shared storage-safe skill-id predicate used
by both object-store keys and filesystem filenames. Backend-specific helpers keep
their contextual error messages while the blank, separator, and traversal checks
live in one reusable place.

Recent skill-management event-reference normalization improvement:
`SkillManagementEventReferences` now owns persistent event-history reference
generation and `.event.properties` detection for filesystem and object-storage
event stores. Both backends now share the same sortable timestamp/UUID naming
scheme, reducing drift between local fallback history and cloud history.

Recent skill-management JDBC event-id normalization improvement:
JDBC event history now also uses `SkillManagementEventReferences` for its
sortable timestamp/UUID event IDs. Database, filesystem, and object-storage
history backends now share one event-reference vocabulary, with storage backends
adding only their file/object extension.

Recent skill-management event-retention normalization improvement:
`SkillManagementEventRetention` now owns bounded history calculations for memory,
filesystem, JDBC, and object-storage event stores. Null prune-option defaults,
oldest-record target selection, and keep-latest math now use one helper while
each backend keeps its own delete mechanics.

Recent skill-management event-capacity normalization improvement:
`SkillManagementEventRetention` now also owns event-history capacity
normalization. Memory, filesystem, JDBC, object-storage, and event-store config
paths share one minimum-capacity/default fallback rule before retention pruning
runs.

Recent skill-management config-key consolidation:
`SkillStoreConfigKeys` now owns shared deployable store config aliases for kind,
filesystem directory, object prefix, JDBC table/schema initialization, custom
store name, and hybrid primary/fallback groups. Definition, lifecycle, and event
store config parsers keep their own backend enum mapping while sharing the same
key vocabulary.

Recent skill-management event-limit config consolidation:
Event store `max-events` and prune-policy keep-latest/retention aliases now use
the shared `SkillStoreConfigKeys` helper. Store and prune parsers still preserve
their own error messages while limit-key lookup and integer parsing live in one
place.

Recent skill-management maintenance-key consolidation:
Maintenance-oriented config parsers now share `mode`/`strategy`/`policy` and
`dryRun`/`dry-run`/`preview`/`planOnly` aliases through `SkillStoreConfigKeys`.
Definition sync, lifecycle reconcile, event prune, and maintenance-plan parsers
still own their mode semantics while key lookup stays consistent.

Recent skill-management maintenance parser consolidation:
Definition sync and lifecycle reconcile config parsers now expose
default-aware normalized-map entry points. Maintenance-plan child parsing
delegates to those policy parsers instead of duplicating boolean override logic
for sync and reconcile settings.

Recent skill-management store-factory support consolidation:
`SkillStoreFactorySupport` now owns custom-store map copying, named custom-store
lookup, and required dependency validation for definition, lifecycle, and event
store factories. The factories now focus on topology construction instead of
repeating dependency and custom-store boilerplate.

Recent skill-management inspection support consolidation:
`SkillStoreInspectionSupport` now also owns inspection target validation,
non-blank sorted skill-id projection, primary/fallback child inspection, and
indexed child naming. Definition, lifecycle, and event inspectors share those
diagnostic helpers while keeping their store-specific health payloads local.

Recent skill-management hybrid merge consolidation:
`HybridSkillStoreSupport` now owns the fallback-then-primary merge rule used by
hybrid definition and lifecycle stores. Hybrid stores share one primary override
policy while keeping their read/write contracts explicit.

Recent skill-management hybrid lookup/remove consolidation:
`HybridSkillStoreSupport` now also owns primary-first fallback lookup and
remove-from-both behavior. Definition and lifecycle hybrid stores share the same
read and removal policy instead of repeating branch logic per store type.

Recent skill-management hybrid read fallback hardening:
Hybrid definition and lifecycle stores now keep local fallback reads available
when primary read/list/snapshot calls fail, while writes still require the
primary backend. This makes database/cloud primary plus file fallback topologies
more useful during primary outages without weakening mutation consistency.

Recent skill-management event-pruner adapter consolidation:
`SkillManagementEventPruner.forSink` now owns event sink to pruner adaptation.
Composite event sinks, maintenance runners, and management services no longer
repeat the same prunable-vs-unsupported fallback check.

Recent skill-management event-reader adapter consolidation:
`SkillManagementEventReader.forSink` and `readableSink` now own event sink to
reader adaptation. Services, composite readable history lookup, and event-store
inspection share one readable-vs-empty/optional boundary instead of repeating
local `instanceof` checks.

Recent skill-management inspection result construction consolidation:
Definition, lifecycle, and event store inspection records now expose named
`ready`/`unavailable` factories. Inspectors gather store data and delegate the
status/result shape to the record layer, avoiding repeated zero-count, empty
summary, and failure-envelope construction.

Recent skill-management admin store-status mapping consolidation:
`SkillManagementAdminViews` now routes definition, lifecycle, and event store
inspection projections through one recursive store-status helper. The public DTO
surface is unchanged, but common admin fields and child mapping are no longer
hand-assembled in each store-specific mapper.

Recent skill-management store-kind alias consolidation:
`SkillStoreConfigKindAliases` now owns shared filesystem, object/cloud, JDBC,
custom, and hybrid backend aliases across definition, lifecycle, and event store
config parsers. S3-compatible, RustFS, MinIO, and primary-fallback naming stay
consistent while each store still keeps its own registry/memory/none semantics.

Recent skill-management filesystem support consolidation:
`SkillManagementFileStoreSupport` now owns common filesystem operations for
file-backed definition, lifecycle, and event stores: directory creation, sorted
regular-file listing, byte/property reads and writes, and delete error wrapping.
File stores keep domain codecs and path rules while sharing fallback file IO
behavior.

Recent skill-management object-store support consolidation:
`SkillManagementObjectStoreSupport` now owns common object-store listing,
extension filtering, sorted key ordering, optional reads, writes, and deletes for
definition, lifecycle, and event cloud stores. Store implementations keep their
domain codecs and key construction while S3-compatible/RustFS/MinIO behavior
shares one thin adapter layer.

Recent skill-management JDBC support consolidation:
`JdbcSkillStoreSupport` now owns shared JDBC schema execution and delete-by-id
operations for definition, lifecycle, and event stores. Domain stores keep their
row mapping and upsert/query logic while common database plumbing and error
wrapping live behind one helper.

Recent skill-management JDBC query consolidation:
`JdbcSkillStoreSupport` now also owns common JDBC read resource management for
single-row and list queries. Definition, lifecycle, and event stores provide row
mappers while sharing connection, prepared-statement, result-set, and SQL error
handling.

Recent skill-management JDBC upsert consolidation:
`JdbcSkillStoreSupport.updateThenInsert` now owns update-first insert fallback
resource management for JDBC definition and lifecycle stores. Store code keeps
parameter binding close to the domain model while sharing upsert execution and
error wrapping.

Recent skill-management JDBC execute-update consolidation:
`JdbcSkillStoreSupport.executeUpdate` now owns bind-and-execute resource
management for single-statement JDBC writes. Event history inserts use the
shared helper, completing the common JDBC connection/statement/error boundary
for schema, reads, deletes, upserts, and insert-style writes.

Recent skill-management skill-id guard consolidation:
`SkillManagementSkillIds.isBlank` now owns shared blank skill-id detection for
definition and lifecycle stores. File, object, JDBC, and in-memory stores use
one guard before read/remove operations while storage-specific normalization
remains separate.

Recent skill-management properties codec support consolidation:
`SkillManagementPropertiesCodecSupport` now owns shared UTF-8 string/byte
conversion plus Java properties load/store error wrapping for persistence
codecs. Definition, lifecycle, event, and JDBC definition paths keep their
domain mapping while sharing one encoding boundary.

Recent skill-management required-property consolidation:
`SkillManagementPropertiesCodecSupport.requiredProperty` now owns shared
required-field validation and missing-property error text for properties-backed
definition, lifecycle, and event codecs. The codecs keep domain-specific parsing
while one helper guards mandatory persisted fields.

Recent skill-management property parsing consolidation:
`SkillManagementPropertiesCodecSupport` now also owns blank-aware line token,
nullable number, nullable instant, and integer-default parsing for properties
backed persistence codecs. Definition and lifecycle codecs reuse the same typed
value rules while keeping their domain models separate.

Recent skill-management property writing consolidation:
`SkillManagementPropertiesCodecSupport` now owns null-skipping property writes,
newline-delimited list writes, prefixed string-map writes, and prefixed scalar
metadata writes for properties-backed codecs. `SkillDefinitionPropertiesCodec`
keeps the skill-definition field mapping while shared write semantics live in
one helper.

Recent skill-management prefixed-property read consolidation:
`SkillManagementPropertiesCodecSupport.prefixedStringProperties` now owns
sorted prefix scanning and prefix stripping for properties-backed codecs.
Definition sub-skill prompts, definition metadata, and event attributes share
one map extraction path while keeping their domain projections separate.

Recent skill-management store capability model:
Added `SkillStoreCapability` and `SkillStoreCapabilities` as a stable
capability vocabulary for definition, lifecycle, and event stores. Store
inspectors now report read/write/delete/list, event query/prune, composite, and
primary-fallback capabilities, and admin store status projections expose those
labels without changing existing health/count semantics.

Recent skill-management config validation result:
Added `SkillStoreConfigValidationResult` and shared validation primitives for
store configuration records. Definition, lifecycle, and event store configs now
validate through a structured result before throwing, giving future admin and
CLI diagnostics a non-throwing validation path while preserving existing
constructor behavior.

Recent skill-management service config validation:
Store config records now expose instance `validate()` helpers, and
`SkillManagementServiceConfig` aggregates definition, lifecycle, and event-store
validation into one result. `SkillManagementServiceConfigs` adds non-throwing
validation helpers for properties, maps, and environment variables so callers
can report configuration problems without catching parser exceptions.

Recent skill-management event-query page consolidation:
`SkillManagementEventPages` now owns query-page assembly for readable event
history stores. Memory, filesystem, JDBC, and object-storage readers share one
filter/default/latest-window path, so future event backends only need to provide
ordered events.

Recent A2UI integration skeleton:
Added a new `a2ui` parent with `wayang-a2ui-core` and `wayang-a2ui-wayang`
modules. Core owns stable A2UI v0.8 message records, JSONL encoding,
server/client capability models, and A2A `DataPart` wrapping; the Wayang adapter
maps A2UI extension metadata and client capabilities into `AgentRequest.context()`
without coupling the protocol model to agent execution internals.

Recent A2UI protocol and surface-mapper improvement:
`A2uiJsonlCodec` now decodes inbound client `userAction` and `error` JSONL or
A2A `DataPart` payloads, giving action routing a typed protocol entry point.
`WayangA2uiSurfaces` adds the first SDK model mapper by rendering
`AgentRunStatus` as a data model update, component tree, and begin-rendering
message stream with a `wayang.run.inspect` action.

Recent A2UI action-routing improvement:
`WayangA2uiActionRouter` now maps inbound A2UI `userAction` events onto Wayang
SDK run lifecycle calls for inspect, wait, and cancel. A separate
`WayangA2uiActionPolicy` owns action allowlists, optional run-id constraints, and
required context checks before any SDK call is made, while action results return
typed A2UI response message streams for the client surface.

Recent A2UI session-boundary improvement:
`WayangA2uiSession` now accepts inbound JSONL, single JSON lines, and A2A
`DataPart` maps/strings, decodes them through `A2uiJsonlCodec`, routes actions
through the policy-gated router, and returns a `WayangA2uiSessionResult` with
typed response messages plus JSONL/DataPart encodings. This gives future A2A,
HTTP, websocket, and MCP transports one thin adapter point instead of each
transport duplicating protocol parsing and routing glue.

Recent A2UI session batch normalization cleanup:
`WayangA2uiSession` now delegates inbound client-message batch normalization to
`RecordCollections.nonNullList` instead of carrying a local null-list
fallback and stream filter. Session routing remains focused on state updates and
action dispatch, while null entries in decoded or externally supplied batches
are skipped through the same shared list policy used by surfaces and diagnostics.

Recent A2UI session-config improvement:
`WayangA2uiSessionConfig` now makes A2UI enablement and action policy
configurable through plain maps and `AgentRequest.context()`. Transports can use
inspect-only, run-lifecycle, disabled, or custom allowlist policies with
optional run-id and required-context guards without each endpoint hand-building
security checks.

Recent A2UI run-history surface improvement:
`WayangA2uiSurfaces.runHistory` now renders `AgentRunHistory` as a summary data
model, row-based component tree, and begin-rendering message. Each run row
includes a `wayang.run.inspect` action, so future A2A, HTTP, websocket, or MCP
UIs can list runs and drill into one run through the existing action router.

Recent A2UI run-history action improvement:
`WayangA2uiActionRouter` now handles `wayang.run.history` without requiring a
single `runId`, maps query context such as limit, offset, tenant, session, and
surface into `AgentRunHistoryQuery`, and returns the run-history A2UI surface.
`WayangA2uiActionPolicy.readOnly()` and the `read-only` session mode expose
inspect/history/events operations without enabling wait or cancel actions.

Recent A2UI run-events improvement:
`WayangA2uiSurfaces.runEvents` now renders `AgentRunEvents` as cursor metadata,
summary counts, event rows, and a refresh action carrying `afterSequence`.
`WayangA2uiActionRouter` handles `wayang.run.events` with run-id scoped query
context for state, type, after-sequence, and limit, returning an A2UI event
timeline surface for polling and inspection UIs.

Recent A2UI composite inspection improvement:
`WayangA2uiSurfaces.runInspection` now combines the run-status surface with the
run-events timeline when `AgentRunInspection` contains recorded lifecycle
events. The inspect action uses this mapper, so richer SDK backends can return a
status-and-timeline response while empty-event inspections keep the existing
status-only shape.

Recent A2UI event navigation improvement:
Run-status surfaces and each run-history row now expose a direct
`wayang.run.events` action next to inspect. A2UI clients can jump from a run
summary to the lifecycle timeline without relying on composite inspection or
transport-specific UI shortcuts.

Recent A2UI policy-shaped surface improvement:
`WayangA2uiSurfaceOptions` now separates generated controls from router
execution policy. Routers derive visible controls and required action context
from `WayangA2uiActionPolicy`, so inspect-only sessions do not render event or
lifecycle buttons, read-only sessions expose inspect/events, and lifecycle
sessions can surface wait/cancel controls for non-terminal runs.

Recent A2UI action feedback improvement:
`WayangA2uiSessionResult` now materializes routed actions with no domain
response into a standard A2UI feedback surface through
`WayangA2uiResultSurfaces`. Rejected actions, disabled sessions, and future
handled-but-empty outcomes now produce JSONL/data-part responses that clients
can render instead of forcing transports to interpret side-channel metadata.

Recent A2UI action-context codec improvement:
`A2uiJsonlCodec` now accepts inbound `userAction.context` as either a plain
object map or an A2UI key/value entry list. Entry-list values resolve
`literalString`, `literalNumber`, and `literalBoolean` bound values into the
normal action context map, improving round-trip compatibility with generated
component action metadata.

Recent A2UI contract fixture improvement:
A small parsed-JSONL contract harness now compares generated Wayang A2UI message
streams against golden fixtures without being brittle to JSON object field
ordering. The first fixtures cover the read-only run-status surface and rejected
action feedback surface, giving transport work a stable protocol baseline.

Recent A2UI transport adapter improvement:
`WayangA2uiTransportAdapter` now provides a transport-neutral exchange boundary
for JSON lines, JSONL streams, JSON DataParts, and DataPart maps. HTTP,
websocket, MCP, and A2A adapters can delegate to one session facade and receive
a consistent A2UI response envelope with JSONL body, DataParts, MIME type, and
handled/rejected counts.

Recent A2UI session continuity improvement:
`WayangA2uiSessionState` now remembers `wayang.run.events` cursors per run and
enriches later event actions that omit `afterSequence`. Explicit client cursors
still win, so polling transports can use server-side continuity without losing
manual rewind or targeted refresh control.

Recent A2UI surface registry improvement:
`WayangA2uiSurfaceRegistry` centralizes model-to-surface rendering for run
status, inspection, history, events, and action feedback. `WayangA2uiActionRouter`
now delegates response rendering through the registry, giving future RAG, MCP,
skill, and harness surfaces one extension point instead of growing
transport-specific mapper calls.

Recent A2UI registry injection improvement:
Custom `WayangA2uiSurfaceRegistry` instances can now be supplied through the
router, session, or transport adapter constructors. This keeps default Wayang
surfaces policy-aware while letting domain adapters override or extend rendering
for future RAG, MCP, skill, and harness-specific views at the boundary.

Recent A2UI registry composition improvement:
`WayangA2uiSurfaceRegistry` now exposes `standardBuilder`, `toBuilder`,
`extend`, `replace`, and removal helpers. Adapters can preserve the default
policy-aware Wayang surfaces while adding domain-specific renderers, or replace
only the one surface they own without rebuilding the whole registry.

Recent A2UI feedback registry improvement:
Session-level empty/rejected action feedback now flows through
`WayangA2uiSurfaceRegistry` using the sequence-aware `WayangA2uiActionFeedback`
model. Custom registries can replace action feedback rendering while the default
path still preserves unique per-action result surface identifiers.

Recent A2UI action-result compatibility improvement:
`WayangA2uiSurfaceRegistry` again renders plain `WayangA2uiActionResult`
models directly while keeping the sequence-aware feedback model for session
fallbacks. Public surface-kind listing now returns distinct kinds, so multiple
model adapters can feed `wayang.action.result` without noisy duplicate catalog
entries.

Recent A2UI surface descriptor improvement:
`WayangA2uiSurfaceDescriptor` now exposes the registry catalog as public
kind/model-type bindings. Adapters can inspect all model renderers behind a
surface kind, such as the paired `WayangA2uiActionFeedback` and
`WayangA2uiActionResult` bindings for `wayang.action.result`, without reaching
into registry internals.

Recent A2UI model lookup improvement:
`WayangA2uiSurfaceRegistry` now supports descriptor lookup by model instance and
model class through `descriptorOf`, `descriptorForModelType`,
`kindForModelType`, `supportsModelType`, and model-type descriptor filters.
The lookup follows the same assignability rules as rendering, so adapters can
inspect base-type renderers before they have concrete model instances.

Recent A2UI registry replacement improvement:
`WayangA2uiSurfaceRegistry.Builder.replace` now replaces the exact
kind/model-type binding instead of clearing sibling renderers under the same
surface kind. Explicit `replaceKind`, `replaceModelType`, and `withoutBinding`
helpers cover broader edits, keeping shared kinds like `wayang.action.result`
safe for targeted adapter overrides.

Recent A2UI surface registry list-normalization cleanup:
`WayangA2uiSurfaceRegistry` now delegates registry renderer snapshots and
custom-renderer message snapshots to `RecordCollections.nonNullList`.
The registry keeps renderer lookup and custom rendering concerns local, while
list immutability and accidental null-message filtering share the same record
normalization policy used by scenarios, diagnostics, and surface data models.

Recent A2UI surface catalog improvement:
`WayangA2uiSurfaceCatalog` now exposes registered surface kinds and renderer
descriptors as both typed Java objects and a transport-friendly map. Registry,
session, and transport adapters can publish surface capabilities for HTTP, MCP,
A2A, or harness integration without each adapter reformatting descriptor
metadata.

Recent A2UI catalog transport improvement:
`WayangA2uiTransportPayloadKind.SURFACE_CATALOG` and
`WayangA2uiTransportRequest.surfaceCatalog()` now route catalog discovery
through the same transport exchange facade as JSONL and DataPart user actions.
Catalog responses use an `application/json` body with no action counts or
DataParts, giving HTTP, MCP, A2A, and harness adapters a stable capability
discovery contract.

Recent A2UI transport metadata improvement:
`WayangA2uiTransportResponse` now carries a defensive, transport-neutral
metadata map. Session responses expose response kind, action/message/DataPart
counts, and handled/rejected totals, while catalog responses expose catalog
counts. `WayangA2uiTransportAdapter.exchange` also stamps the originating
request kind, so HTTP, MCP, A2A, and harness bridges can route responses
without parsing the payload body.

Recent A2UI response envelope improvement:
`WayangA2uiTransportResponse.toMap()` and `toJson()` now export a stable
transport-neutral envelope containing MIME metadata, body encoding, response
body, DataParts, handled/rejected counts, response metadata, and empty state.
HTTP, MCP, A2A, and harness bridges can serialize one canonical response shape
instead of rebuilding envelope fields independently.

Recent A2UI request envelope improvement:
`WayangA2uiTransportRequest` now supports `toMap()`, `toJson()`, `fromMap()`,
and `fromJson()` with defensive DataPart copying and tolerant request-kind
normalization for enum, camel, kebab, and snake forms. Transport bridges can
accept one canonical inbound envelope and pass the decoded request directly
into `WayangA2uiTransportAdapter.exchange`.

Recent A2UI envelope exchange improvement:
`WayangA2uiTransportAdapter` now offers bridge-ready envelope helpers for
map-in/response, JSON-in/response, map-in/map-out, and JSON-in/JSON-out
exchange. HTTP, MCP, A2A, and harness adapters can delegate an entire transport
turn to the A2UI adapter without duplicating request decoding or response
serialization glue.

Recent A2UI transport error envelope improvement:
`WayangA2uiTransportResponse.error` now emits a canonical
`application/problem+json` envelope with `transport-error` metadata, and
`WayangA2uiTransportAdapter` exposes `OrError` envelope helpers for malformed
map or JSON request envelopes. Bridge adapters can choose strict exception
semantics or serialized transport errors without duplicating catch-and-format
logic.

Recent A2UI structured transport error improvement:
`WayangA2uiTransportError` now owns normalized transport problem details with
stable `code` and `message` fields. Error responses embed that structure in
both the problem JSON body and response metadata, and
`WayangA2uiTransportResponse.transportError()` exposes it directly for bridge
adapters that should not parse response bodies.

Recent A2UI transport outcome improvement:
`WayangA2uiTransportOutcome` now classifies response envelopes as `SUCCESS`,
`PARTIAL_SUCCESS`, `ACTION_REJECTED`, `EMPTY`, or `TRANSPORT_ERROR`.
`WayangA2uiTransportResponse.outcome()` derives the classification from
transport errors, handled/rejected counts, and payload emptiness, and exported
response envelopes include the outcome for bridge routing.

Recent A2UI transport field consolidation:
`WayangA2uiTransportFields` now owns the canonical request, response, metadata,
error, count, and response-kind field names for transport envelopes. Request,
response, error, and adapter code use those constants instead of duplicating
string keys, keeping future HTTP, MCP, A2A, and harness adapters aligned on one
wire contract.

Recent A2UI transport JSON consolidation:
`TransportJson` now owns the shared Jackson mapper and map
type-reference for transport envelope JSON. Request and response envelopes use
the helper for encode/decode paths, keeping blank/decode/encode failure handling
consistent while removing duplicate mapper plumbing from the envelope records.

Recent A2UI transport map-copy consolidation:
`TransportMaps` now owns defensive copying for transport envelope
maps, nested maps, lists, metadata, request DataParts, and response DataParts.
Request and response records no longer carry separate recursive copy helpers,
and response DataParts now get the same deep-copy/null-filtering protection as
request envelopes.

Recent A2UI transport content consolidation:
`WayangA2uiTransportContent` now owns canonical transport MIME types and body
encodings for A2UI JSONL, JSON catalogs, and problem JSON errors. Transport
responses use those constants for defaults and factory outputs, keeping future
HTTP, MCP, A2A, and harness adapters aligned on content negotiation values.

Recent A2UI transport metadata consolidation:
`WayangA2uiTransportMetadata` now owns request-kind, session-result,
surface-catalog, transport-error, and metadata-merge builders for A2UI
transport envelopes. Transport responses and adapters delegate metadata shaping
to the helper, keeping bridge adapters aligned on one metadata vocabulary.

Recent A2UI transport envelope consolidation:
`WayangA2uiTransportEnvelope` now owns canonical request and response envelope
map construction, including top-level field order, defensive nested copying,
outcome export, empty-state export, and null body normalization. Request and
response records delegate `toMap()` through that helper so future HTTP, MCP,
A2A, and harness bridges serialize the same envelope shape.

Recent A2UI transport request decoder consolidation:
`WayangA2uiTransportRequestDecoder` now owns inbound request envelope decoding,
including tolerant request-kind normalization, body text conversion, and
defensive DataPart extraction. `WayangA2uiTransportRequest.fromMap()` delegates
to that helper, keeping the request record focused on value normalization and
serialization.

Recent A2UI transport response decoder consolidation:
`WayangA2uiTransportResponseDecoder` now owns inbound response envelope decoding
from maps and JSON, including body/content metadata, defensive DataPart and
metadata extraction, and tolerant numeric count parsing. Transport responses now
offer `fromMap()` and `fromJson()` entry points, completing the bidirectional
envelope contract for HTTP, MCP, A2A, and harness clients.

Recent A2UI transport fixture improvement:
The A2UI contract suite now includes structural JSON fixtures for transport
request envelopes, response envelopes, problem JSON bodies, and default surface
catalog discovery. `A2uiContractAssert` compares fixture JSON as parsed nodes,
pinning the bridge-facing wire contracts without making tests sensitive to
formatting or object key order.

Recent A2UI bridge SPI improvement:
`WayangA2uiBridge`, `WayangA2uiBridgeRequest`, and
`WayangA2uiBridgeResponse` now provide a transport-neutral bridge boundary for
HTTP, MCP, A2A, and harness integrations. `WayangA2uiTransportBridge` adapts the
existing transport adapter into that SPI, preserving typed request/response
envelopes while giving concrete bridges a place to carry transport attributes
and convert malformed envelopes into canonical transport errors.

Recent A2UI bridge harness improvement:
`WayangA2uiBridgeHarness` now replays named `WayangA2uiBridgeScenario`
instances through any bridge implementation and captures ordered
`WayangA2uiBridgeScenarioExchange` results. Scenario results summarize exchange
counts, handled/rejected totals, outcomes, transport errors, attributes, and
response envelopes, giving HTTP, MCP, A2A, and harness adapters a reusable
smoke/contract runner.

Recent A2UI HTTP bridge adapter skeleton:
`WayangA2uiHttpRequest`, `WayangA2uiHttpResponse`, and
`WayangA2uiHttpBridgeAdapter` now provide a dependency-free HTTP-shaped adapter
over the bridge SPI. The skeleton maps `POST /a2ui/exchange` and
`GET /a2ui/surface-catalog` into canonical transport envelopes, returns JSON
response envelopes with outcome/content headers, and emits problem envelopes for
invalid JSON, unsupported methods, and unknown routes.

Recent A2UI HTTP route manifest improvement:
`WayangA2uiHttpRoute` and `WayangA2uiHttpRouteCatalog` now describe the A2UI
HTTP binding surface separately from request dispatch. Framework integrations
can discover operation ids, methods, paths, content types, and body requirements
from one catalog while `WayangA2uiHttpBridgeAdapter` continues to own only
transport-neutral request handling.

Recent A2UI HTTP route discovery endpoint improvement:
`GET /a2ui/route-catalog` now returns the HTTP route manifest as a canonical
A2UI transport response envelope. The manifest includes the exchange, surface
catalog, and route-catalog endpoints, giving future framework adapters a
self-describing bootstrap path without hardcoding route metadata.

Recent A2UI HTTP content-type guard improvement:
`WayangA2uiHttpRequest` now provides case-insensitive header lookup and media
type comparison that tolerates parameters such as charset. `WayangA2uiHttpBridgeAdapter`
uses the route manifest's declared request content type to reject unsupported
`POST /a2ui/exchange` media types with a canonical 415 transport error before
dispatching into the bridge.

Recent A2UI HTTP options and route headers improvement:
Known A2UI HTTP routes now attach `Allow` and `X-Wayang-A2UI-Route-Operation`
headers to successful and route-specific error responses. `OPTIONS` requests to
known routes return a one-route catalog envelope, giving concrete HTTP
frameworks a dependency-free preflight/discovery hook without invoking the SDK.

Recent A2UI HTTP accept negotiation improvement:
`WayangA2uiHttpRequest` now exposes `Accept` handling with exact media type,
type wildcard, full wildcard, and `q=0` filtering. The HTTP bridge adapter uses
each route's declared response content type to return canonical 406
`not_acceptable` envelopes before dispatching when clients explicitly reject
JSON responses.

Recent A2UI HTTP route capability manifest improvement:
`WayangA2uiHttpRoute` now owns allowed-method computation and `Allow` header
rendering. Route catalog bodies include `allowedMethods` and `allowHeader`, and
the HTTP bridge adapter consumes those descriptor values instead of duplicating
method capability logic internally.

Recent A2UI HTTP route guard consolidation:
`WayangA2uiHttpRouteGuard` now centralizes method, response negotiation, and
request content-type validation for HTTP-shaped A2UI routes. The bridge adapter
delegates route-specific 405, 406, and 415 problem responses through the guard,
so future framework bindings can reuse the same policy without copying adapter
branches.

Recent A2UI HTTP catalog dispatch consolidation:
`WayangA2uiHttpRouteCatalog` now resolves routes by operation id as well as by
path/method. `WayangA2uiHttpBridgeAdapter` resolves normal requests through the
catalog and dispatches by route operation, keeping route discovery and runtime
dispatch aligned around the same manifest.

Recent A2UI HTTP operation dispatcher improvement:
`WayangA2uiHttpOperationHandler` and `WayangA2uiHttpOperationDispatcher` now own
operation registration and invocation for HTTP-shaped A2UI routes. The bridge
adapter resolves routes from the catalog and delegates validated dispatch to the
dispatcher, making future A2UI operations pluggable without editing adapter
branching logic.

Recent A2UI HTTP binding audit improvement:
`WayangA2uiHttpBindingReport` now audits route catalog operations against
registered dispatcher handlers. The dispatcher and bridge adapter expose binding
reports that identify missing route handlers and orphan handlers, giving tests
and future runtime adapters a preflight check before HTTP routes are served.

Recent A2UI HTTP binding report endpoint improvement:
`GET /a2ui/binding-report` now exposes the route/handler binding audit as a
canonical A2UI transport response envelope. The default HTTP route catalog and
operation dispatcher include the binding-report route, and contract fixtures pin
the route catalog and binding-report JSON bodies.

Recent RAG skill SPI contract improvement:
`agent-spi` now owns a direct `RagSkillRetrievalContractTest` for the
agent-facing RAG retrieval boundary. The contract pins request normalization,
defensive embedding/filter copies, blank-document filtering, prompt context
rendering, and source-summary metadata before downstream built-in skill or
native runtime adapters consume the SPI.

Recent Wayang run event stats command improvement: `wayang run events <run-id>
--stats` now renders the lifecycle event `cursor` and `summary` envelopes
without event rows. This gives polling UIs and dashboard scripts a lightweight
event status command while the existing `run events` output remains available
for full timeline inspection.

Recent Wayang CLI lifecycle query consolidation: shared picocli mixins now own
event query flags for `run events`/`run inspect` and history query flags for
`run list`/`run stats`. The command behavior stays the same, but future
lifecycle commands can reuse one query-to-SDK path instead of duplicating option
fields.

Recent skill-management artifact identity improvement:
`SkillArtifactKind` and `SkillArtifactReference` now provide a storage-neutral
identity model for dynamic skill definitions, lifecycle state, event history,
packages, resources, RAG indexes, and MCP descriptors. References normalize skill
ids, artifact names, and versions once, then render stable path segments,
relative paths, qualified names, or object keys for file, database, S3, RustFS,
and hybrid artifact stores.

Recent skill-management artifact store SPI improvement:
`SkillArtifact`, `SkillArtifactQuery`, and `SkillArtifactStore` now define a
semantic persistence boundary for dynamic skill packages, resources, RAG indexes,
and MCP descriptors. `InMemorySkillArtifactStore` gives tests and harnesses a
ready implementation with normalized listing filters, deterministic ordering, and
defensive payload copies while file, JDBC, object-store, and hybrid adapters can
plug in behind the same interface.

Recent skill-management object artifact store improvement:
`ObjectStorageSkillArtifactStore` now persists dynamic skill artifacts through
the neutral `SkillManagementObjectStore` contract used by S3-compatible storage,
RustFS, MinIO, and platform object gateways. Payload bytes are stored separately
from a Java-properties manifest, so large skill packages and RAG resources keep
efficient binary storage while listings and reloads retain content type,
metadata, and normalized artifact identity.

Recent skill-management file artifact store improvement:
`FileSystemSkillArtifactStore` now gives dynamic skill artifacts a local durable
fallback using the same per-artifact `content.bin` plus `artifact.properties`
layout as object storage. Recursive manifest listing, deterministic query
ordering, missing-manifest payload fallback, and empty-directory cleanup make the
file strategy suitable as a configurable fallback for hybrid database/object
storage deployments.

Recent skill-management hybrid artifact store improvement:
`HybridSkillArtifactStore` now composes a primary artifact backend with a fallback
backend for dynamic skill packages, resources, RAG indexes, and MCP descriptors.
Reads and listings fall back when the primary is unavailable, merged catalogs
deduplicate primary and fallback references, writes remain primary-authoritative,
and deletes remove matching artifacts from both stores.

Recent skill-management artifact store configuration improvement:
`SkillArtifactStoreConfig`, `SkillArtifactStoreConfigs`, and
`SkillArtifactStoreFactory` now make artifact persistence configurable across
memory, filesystem, S3-compatible object storage such as RustFS or MinIO, custom
stores, and primary/fallback hybrid topologies. Property, environment, and nested
map parsing use the same alias style as definition, lifecycle, and event stores,
so dynamic skill artifacts can switch persistence strategy without changing
callers.

Recent skill-management service artifact config improvement:
`SkillManagementServiceConfig` now carries artifact-store configuration alongside
definition, lifecycle, and event store settings, defaulting to in-memory artifact
persistence for compatibility. `SkillManagementServiceConfigs` parses artifact
settings from properties, environment variables, and nested maps, and service
validation now reports artifact-store configuration errors through the same
non-throwing validation helpers.

Recent skill-management artifact service API improvement:
`SkillManagementService` now exposes configured artifact-store operations for
put, get, list, and delete across dynamic skill packages, resources, RAG indexes,
and MCP descriptors. `SkillManagementServiceFactory` builds the artifact store
from service config, write operations emit artifact events, and existing service
constructors keep an in-memory artifact store by default for compatibility.

Recent skill-management artifact inspection improvement:
`SkillArtifactStoreInspector`, `SkillArtifactStoreInspection`, and
`SkillArtifactStoreHealthStatus` now expose readiness, artifact counts, qualified
artifact references, kind counts, capabilities, and hybrid primary/fallback
children. Aggregate management inspection, admin projections, and service-level
inspection now include artifact-store status alongside definitions, lifecycle
state, and event history.

Recent skill-management artifact integrity improvement:
`SkillArtifact` now exposes stable SHA-256 digests, and artifact manifests
persist both size and digest expectations beside payload metadata. File and
object artifact stores validate persisted manifests when artifacts reload, so
corrupted dynamic skill packages, resources, RAG indexes, or MCP descriptors are
detected before runtime consumers receive them while legacy missing-manifest
payloads still load as fallback data.

Recent skill-management JDBC artifact store improvement:
`JdbcSkillArtifactStore` now gives dynamic skill artifacts a database-backed
persistence adapter alongside memory, filesystem, object storage, and hybrid
stores. Artifact identity is stored in queryable columns, payloads are persisted
as portable Base64 text with integrity manifests, and artifact config/factory
parsing now supports `jdbc`/`database` backends with configurable table names and
schema initialization.

Recent skill-management artifact sync improvement:
`SkillArtifactStoreSynchronizer` now provides bootstrap, mirror, and dry-run
synchronization between artifact backends. Dynamic skill packages, resources,
RAG indexes, and MCP descriptors can be copied into missing fallback stores,
mirrored from database or cloud primary stores, conflict-checked without
overwrite, and pruned from targets when full repair mode is selected.
`SkillArtifactStoreSyncConfigs` parses the same policy from properties,
environment variables, and nested maps.

Recent skill-management maintenance artifact sync improvement:
`SkillManagementMaintenancePlan`, `SkillManagementMaintenanceRunner`, and
`SkillManagementMaintenanceResult` now carry artifact-store synchronization
beside definition sync, lifecycle reconciliation, and event pruning.
`SkillManagementService` exposes artifact sync and artifact-aware maintenance
overloads, while admin maintenance projections report artifact copied, updated,
deleted, conflict, and dry-run counts using qualified artifact references.

Recent skill-management artifact sync observability improvement:
Direct `SkillManagementService.syncArtifacts(...)` runs now emit a single
`SYNC_ARTIFACTS` management event with dry-run, copied, updated, unchanged,
conflict, deleted, changed, and consistency counts. Failed artifact sync attempts
record the same operation with failure metadata, giving operators visibility
into standalone artifact bootstrap or repair jobs outside full maintenance runs.

Recent skill-management configured maintenance source improvement:
`SkillManagementMaintenanceSourceConfig` and
`SkillManagementMaintenanceSourceConfigs` now model optional source definition
and artifact stores for configured maintenance jobs. `SkillManagementServiceFactory`
can run maintenance directly from explicit registry, filesystem, JDBC, object
storage, hybrid, or custom sources, while omitted sources safely reuse the
managed target stores so mirror/repair mode cannot prune data from an empty
default source.

Recent skill-management deployment config improvement:
`SkillManagementDeploymentConfig` and `SkillManagementDeploymentConfigs` now
bundle managed service stores, configured maintenance source stores, and the
maintenance plan into one deployable value. Properties, environment variables,
and nested maps can now drive the full dynamic-skill persistence topology and
maintenance policy together, and `SkillManagementServiceFactory` can execute
maintenance directly from that combined config.

Recent skill-management deployment validation improvement:
Maintenance-plan config now exposes non-throwing validation helpers alongside
store config parsers. Deployment validation combines managed service-store,
maintenance-source, and maintenance-plan validation results so CLI, scheduler,
and REST preflight flows can report multiple configuration issues in one pass
instead of stopping at the first invalid section.

Recent skill-management deployment factory improvement:
`SkillManagementServiceFactory` can now create a `SkillManagementService`
directly from `SkillManagementDeploymentConfig`, matching the deployment-based
maintenance execution path. Service construction consumes only the managed
service-store section, keeping maintenance-source stores isolated from normal
runtime service wiring.

Recent skill-management deployment result improvement:
`SkillManagementDeploymentResult` now captures the service, resolved deployment
config, and maintenance result for one-step deployment flows.
`SkillManagementServiceFactory.deploy(...)` applies configured maintenance and
then exposes a service backed by the same managed target stores, preserving
memory/custom target state that would otherwise be lost if maintenance and
service construction used separate store instances.

Recent skill-management deployment admin projection improvement:
`SkillManagementAdminDeploymentReport` and `SkillManagementAdminViews.deployment`
now expose one-step deployment outcomes through stable admin DTOs. Deployment
responses surface dry-run, changed, and consistency state plus the existing
maintenance report without leaking the runtime `SkillManagementService` object
across CLI, scheduler, or REST boundaries.

Recent skill-management deployment observability improvement:
Deployment runs now emit a `DEPLOYMENT` management event after the nested
maintenance event completes, with dry-run, changed, consistency, definition
change, artifact change, lifecycle repair, and event-prune counts. Failed
deployment attempts record the same operation with failure metadata so scheduled
or admin-triggered deployment jobs have an audit trail even when source-store
resolution fails before maintenance can run.

Recent skill-management event-attribute consolidation:
`SkillManagementEventAttributes` now owns artifact-sync, maintenance, and
deployment event summary maps. Maintenance runners, deployment factories, and
direct artifact synchronization all share one attribute vocabulary, reducing
drift as scheduler, CLI, and admin surfaces consume the same operation history.

Recent skill-management deployment preflight improvement:
`SkillManagementDeploymentPreflightReport` and
`SkillManagementServiceFactory.preflight(...)` now validate a deployable
configuration without running maintenance. The report separates configuration,
managed target-store, and maintenance source-store issues, including missing
registry, object-store, JDBC, or custom-store dependencies that previously only
surfaced when deployment began resolving stores.

Recent skill-management deployment history projection:
`SkillManagementEventQuery.deployments(...)`,
`SkillManagementAdminDeploymentHistoryPage`, and
`SkillManagementAdminDeploymentHistoryEntry` now turn raw `DEPLOYMENT` events
into a stable admin-facing history. CLI, scheduler, and REST surfaces can render
recent deployment success, failure, change, consistency, sync, lifecycle, prune,
and error metadata without parsing event attribute maps themselves.

Recent skill-management deployment capability preflight:
Deployment preflight now includes a dedicated capability validation bucket.
Event-pruning maintenance plans require an event store or override sink with
`prune-events` capability, and custom/hybrid event stores are checked
recursively before deployment starts. This keeps plan-vs-store capability
failures visible to schedulers and admin tools before maintenance mutates any
target store.

Recent skill-management deployment preflight admin projection:
`SkillManagementAdminDeploymentPreflightReport` and
`SkillManagementAdminValidationReport` now expose deployment readiness as stable
admin DTOs. `SkillManagementAdminViews.deploymentPreflight(...)` maps core
configuration, target-store, source-store, and capability validation buckets
into count/message/error summaries so REST, CLI, and scheduler surfaces can
render preflight failures without depending on internal validation records.

Recent skill-management deployment preflight enforcement:
`SkillManagementServiceFactory.deploy(...)` now enforces deployment preflight
before resolving target stores or running maintenance. Non-deployable plans raise
`SkillManagementDeploymentPreflightException`, which carries the full preflight
report for admin handlers. When an event-sink override is available, failed
preflight attempts still emit a deployment failure event; otherwise deployment
fails before mutating managed stores.

Recent skill-management maintenance preflight enforcement:
`SkillManagementServiceFactory.preflight(serviceConfig, sourceConfig, plan)` now
lets maintenance callers validate the same target, source, and capability
buckets without wrapping inputs in a deployment config. Configured maintenance
runs enforce that report before resolving stores, so source dependency failures
or missing prune capabilities fail before definition, artifact, or lifecycle
stores are mutated; override event sinks still receive a maintenance failure
event for auditability.

Recent skill-management maintenance runner capability guard:
`SkillManagementEventPruner` now exposes `supportsPruning()` and the unsupported
adapter reports the missing `prune-events` capability before work starts.
`SkillManagementMaintenanceRunner` enforces that signal before definition sync,
artifact sync, or lifecycle reconciliation, while composite event sinks now
preflight all children so a partially prunable fan-out cannot mutate one event
store and fail on another.

Recent skill-management preflight failure event enrichment:
`SkillManagementEventAttributes` now owns generic failure attributes and
deployment preflight summaries. Factory-level deployment and maintenance
preflight failures emit readiness, total error count, bucket counts, and bucket
messages alongside `errorType`/`error`, so schedulers and admin history views can
classify source-store, target-store, configuration, and capability failures
without parsing exception text.

Recent skill-management deployment history preflight projection:
`SkillManagementAdminDeploymentHistoryEntry` now exposes preflight availability,
readiness, deployability, total error count, bucket error counts, and preflight
message fields from deployment failure events. `SkillManagementAdminViews` maps
those attributes directly so admin, CLI, and scheduler surfaces can render
deployment history with structured preflight context instead of scraping failure
messages.

Recent skill-management deployment history preflight summary:
`SkillManagementAdminDeploymentHistoryPage` now aggregates preflight-aware
deployment counts and per-bucket preflight failure counts. Admin dashboards,
schedulers, and CLI summaries can distinguish configuration, target-store,
source-store, and capability failure trends without scanning every deployment
entry.

Recent skill-management deployment history summary consolidation:
`SkillManagementAdminDeploymentHistoryPage` now derives success, failure,
change, consistency, and preflight bucket counters from its deployment entries.
`SkillManagementAdminViews.deploymentHistory(...)` only supplies the matched
count, truncation state, and entry list, keeping summary math in one DTO boundary
and preventing stale caller-provided counts from drifting.

Recent A2UI HTTP harness improvement:
`WayangA2uiHttpHarness`, `WayangA2uiHttpScenario`,
`WayangA2uiHttpScenarioExchange`, and `WayangA2uiHttpScenarioResult` now replay
dependency-free HTTP-shaped A2UI scenarios through the adapter and summarize
status codes, success/error buckets, transport outcomes, handled/rejected
counts, and decoded response envelopes. Tests now cover successful exchange plus
route diagnostics, JSON-envelope scenario construction, and HTTP problem
responses across invalid JSON, content-type, accept-negotiation, and not-found
paths.

Recent A2UI HTTP scenario report improvement:
`WayangA2uiHttpScenarioReport` now projects HTTP harness results into a stable
automation-friendly map/JSON shape with pass/fail status, status-code buckets,
transport outcome summaries, sanitized request metadata, response headers,
decoded response envelopes, and transport error details. Scenario results expose
`report()`, `toMap()`, and `toJson()` so CLI, REST, and smoke harness surfaces
can publish the same report without knowing the internal record graph.

Recent A2UI HTTP built-in scenario improvement:
`WayangA2uiHttpScenarios` now provides catalog-backed discovery, route-options,
and diagnostics presets for side-effect-free smoke runs. The diagnostics preset
combines surface catalog, route catalog, binding report, and `OPTIONS` probes for
every known route, with scenario/request attributes carrying route count and
route operation metadata into the existing scenario reports.

Recent A2UI HTTP scenario suite improvement:
`WayangA2uiHttpScenarioSuite`, `WayangA2uiHttpScenarioSuiteResult`, and
`WayangA2uiHttpScenarioSuiteReport` now group multiple HTTP scenarios into one
aggregate smoke result. `WayangA2uiHttpHarness.run(suite)` produces suite-level
pass/fail status, scenario counts, exchange totals, error buckets, transport
error state, and nested scenario reports. The built-in smoke suite combines
discovery, route-options, and diagnostics presets so CI, CLI, and REST harnesses
can execute one catalog-backed A2UI HTTP health check.

Recent A2UI scenario factory varargs cleanup:
`RecordCollections.nonNullVarargs` now owns nullable varargs array
normalization for small record factories. HTTP scenarios, HTTP scenario suites,
and bridge scenarios delegate their varargs factories to that helper instead of
duplicating local `Arrays.asList` fallbacks, with focused tests pinning null
arrays and accidental null entries.

Recent A2UI HTTP harness issue projection improvement:
`WayangA2uiHttpScenarioIssue` now extracts compact failure summaries from failed
HTTP scenario exchanges, including scenario id, exchange index, method, path,
status, route operation, outcome, error code, message, and request attributes.
Scenario and suite reports now expose `issueCount` plus flattened `issues`
arrays so automation can render failures without parsing nested transport
envelopes.

Recent A2UI HTTP binding report ordering stability:
`WayangA2uiHttpBindingReport` now orders handler operations by route-catalog
order first and appends sorted orphan handlers afterward. This keeps binding
diagnostic output deterministic even when custom dispatchers are constructed
from unordered maps.

Recent Agentic Commerce module skeleton:
`agentic-commerce/agentic-commerce-core` now introduces framework-neutral
Agentic Commerce Protocol support for Wayang. The first slice models the
Checkout API route catalog, protocol headers, HTTP-shaped requests, route
matching, validation issues, and validation reports for seller-side ACP
adapters. The root build now includes the new module so future runtime,
commerce-agent, and REST bindings can share one contract layer.

Recent Agentic Commerce response validation improvement:
`AgenticCommerceHttpResponse`, `AgenticCommerceResponseValidator`, and
`AgenticCommerceResponseValidationReport` now validate seller responses for ACP
checkout adapters. Response checks cover route success status codes, JSON content
type, non-empty response bodies, required `Request-Id` correlation, and optional
idempotency-key echo consistency. The core module also tracks the current
checkout date version and the `201 Created` create-session success status.

Recent Agentic Commerce checkout DTO improvement:
`AgenticCommerceCheckoutSession`, `AgenticCommerceLineItem`,
`AgenticCommerceTotals`, `AgenticCommerceBuyer`,
`AgenticCommerceFulfillmentDetails`, `AgenticCommerceAddress`,
`AgenticCommerceMessage`, `AgenticCommerceError`, and
`AgenticCommerceCheckoutStatus` now provide dependency-free, protocol-shaped
checkout payload contracts. DTOs serialize to ACP snake_case maps, parse common
camelCase compatibility aliases, normalize status and currency values, preserve
unknown extension fields in metadata maps, and keep integer minor-unit cart
amounts ready for future REST, Wayang adapter, and commerce-agent modules.

Recent Agentic Commerce checkout request DTO improvement:
`AgenticCommerceCreateCheckoutSessionRequest`,
`AgenticCommerceUpdateCheckoutSessionRequest`,
`AgenticCommerceCompleteCheckoutSessionRequest`, and
`AgenticCommerceCancelCheckoutSessionRequest` now model checkout operation
payloads separately from checkout session responses. The request layer adds
checkout items, payment data, intent traces, fulfillment selections, coupons,
discounts, capabilities, authentication results, affiliate attribution, and
risk signals while keeping complex ACP extension areas as defensive maps.
Request DTOs emit protocol snake_case keys and accept common compatibility
aliases such as `items`, `fulfillment_address`, and camelCase field names.

Recent Agentic Commerce checkout HTTP request factory improvement:
`AgenticCommerceJson`, `AgenticCommerceHttpRequestOptions`, and
`AgenticCommerceCheckoutHttpRequests` now turn checkout operation DTOs into
framework-neutral `AgenticCommerceHttpRequest` values. The factory covers
create, retrieve, update, complete, and cancel checkout routes, applies
Bearer/API-Version/Accept/Content-Type/idempotency/request-id headers, escapes
checkout session path segments, carries route operation metadata in request
attributes, and omits cancel bodies unless optional intent-trace payloads are
present. Core map copying now preserves insertion order while remaining
immutable, keeping generated JSON stable without introducing a JSON dependency.

Recent Agentic Commerce checkout HTTP response decoder improvement:
`AgenticCommerceJson` now also reads dependency-free JSON objects, while
`AgenticCommerceCheckoutHttpResponses` and
`AgenticCommerceCheckoutHttpResult` decode checkout HTTP responses into typed
checkout sessions, structured errors, messages, raw body maps, merged validation
issues, and compact result summaries. The decoder supports direct or nested
checkout-session bodies, preserves response validation reports, reports invalid
JSON as a validation issue, and only treats success bodies as errors when an
explicit `error` object or HTTP error status is present.

Recent Agentic Commerce checkout HTTP harness improvement:
`AgenticCommerceCheckoutHttpHarness`, scenario/step/exchange/result records,
and `AgenticCommerceCheckoutHttpResponder` now provide a dependency-free smoke
and contract harness for checkout HTTP adapters. The built-in
`AgenticCommerceCheckoutHttpSmoke` scenario exercises create, retrieve, update,
complete, and cancel flows through the request factory, request validator,
fixture responder, response decoder, and expectation checks. Harness reports
capture request-validation issues, response/decode issues, transport failures,
expected vs. actual status codes, expected vs. actual success state, operation
metadata, and compact machine-readable scenario maps for future CLI, REST, CI,
and Quarkus adapter probes.

Recent Agentic Commerce checkout HTTP expectation improvement:
`AgenticCommerceCheckoutHttpExpectation` and
`AgenticCommerceCheckoutHttpExpectationResult` now validate harness scenario
results without rerunning transport. Expectations can assert valid/successful
state, exchange count, issue count, status-code sequence, operation sequence,
step-id sequence, and whether transport errors are allowed. Scenario results now
expose `validate(...)`, and the built-in smoke expectation gives CLI, REST, CI,
and Quarkus probes one reusable pass/fail contract for the five-step checkout
smoke flow.

Recent Agentic Commerce checkout HTTP smoke runner improvement:
`AgenticCommerceCheckoutHttpSmokeRunner` and
`AgenticCommerceCheckoutHttpSmokeResult` now wrap the built-in checkout smoke
scenario, fixture responder, and smoke expectation into one operational result.
The runner exposes pass/fail state, exit code, successful-exit flag, scenario
and expectation issue counts, exchange counts, route/spec metadata, full
scenario result, and expectation result maps for future CLI, CI, REST, and
Quarkus probes. Custom responders, scenarios, and expectations can reuse the
same runner so accepted-failure probes can pass intentionally without changing
the core harness.

Recent Agentic Commerce checkout HTTP smoke summary improvement:
`AgenticCommerceCheckoutHttpSmokeSummary` and
`AgenticCommerceCheckoutHttpIssueSummary` now condense smoke results into a
small operational view with pass/fail state, exit code, scenario and expectation
ids, exchange count, route count, issue counts, status-code sequence, operation
sequence, step-id sequence, transport-error count, and flattened issue rows
tagged by source, step, and operation. Summaries can be built from live smoke
results, smoke-result maps, or raw JSON payloads, giving CLI, CI, REST, and
Quarkus probes a stable low-noise decision surface without traversing nested
scenario and expectation reports.

Recent Agentic Commerce checkout HTTP smoke probe improvement:
`AgenticCommerceCheckoutHttpSmokeProbeResult` now wraps HTTP-level smoke probe
responses with status code, content type, request id, route operation, decoded
smoke summary, probe issues, final pass/fail state, and exit code. Smoke summary
decoding now accepts both full smoke-result maps and compact summary maps/JSON,
so future REST, CLI, CI, and Quarkus probes can return either shape while
downstream consumers still make one operational decision. Probe validation
reports non-2xx status codes, invalid JSON bodies, missing bodies, and non-JSON
content types as flattened probe issues.

Recent A2UI HTTP harness expectation validation improvement:
`WayangA2uiHttpScenarioExpectation` and
`WayangA2uiHttpScenarioSuiteExpectation` now let callers validate reports against
expected pass/fail state, exchange or scenario count, issue count, transport
error allowance, status codes, outcomes, route operations, and scenario ids.
`WayangA2uiHttpExpectationResult` exposes stable validation issue summaries for
CI, CLI, and REST harness callers, and scenario/suite results now provide
`validate(...)` helpers. The built-in smoke suite also exposes a matching
expectation preset.

Recent A2UI HTTP harness contract fixture improvement:
Contract fixtures now lock the JSON shapes for compact HTTP scenario reports,
scenario issue summaries, suite reports, and expectation validation results.
`WayangA2uiContractTest` compares these fixtures structurally, giving future CLI,
REST, and CI harness integrations stable machine-readable report schemas to
target before those surfaces are wired in.

Recent A2A protocol core module foundation:
Added a new `a2a/a2a-core` module for provider-neutral A2A v1.0 contracts.
The module captures Agent Card discovery records, supported interfaces, skills,
capabilities, messages, parts, tasks, artifacts, send-message envelopes,
streaming event envelopes, and REST route metadata without depending on Wayang
runtime adapters. This keeps protocol contracts separate from future Quarkus,
agent-core, MCP, RAG, and A2UI integration layers.

Recent A2A Wayang adapter foundation:
Added `a2a/a2a-wayang` as the first runtime-facing A2A bridge module. It
projects Wayang `SkillDefinition` and legacy `AgentSkill` catalogs into A2A
Agent Card skills, builds configurable Agent Cards from runtime profiles, and
maps A2A send-message requests plus Wayang `AgentResponse` values across the
existing `agent-spi` boundary. A2UI, MCP, RAG, and security hints remain
configurable Agent Card metadata/extensions instead of hard-coded transport
dependencies.

Recent A2A HTTP binding scaffold:
Added dependency-free HTTP-shaped request/response, route matching, route
guard, operation handler SPI, and operation dispatcher support in
`a2a/a2a-wayang`. The dispatcher now serves public and extended Agent Cards,
extracts templated task/config path parameters, validates Accept and
Content-Type behavior, handles OPTIONS/method/path errors, and delegates
send/task operations to pluggable handlers so Quarkus and task-store layers can
wire in later without owning protocol matching logic.

Recent A2A task lifecycle store foundation:
Added `WayangA2aTaskStore`, task query/event/config records, an
`InMemoryWayangA2aTaskStore`, and store-backed HTTP handlers for task get/list,
cancel, subscribe replay, and push notification config routes. Task lifecycle
state now has a storage-agnostic SPI that preserves history, artifacts, status
events, cancellation, and event replay while leaving database, file, S3, RustFS,
or hybrid persistence adapters to plug in behind the same boundary.

Recent A2A send-message execution foundation:
Added a small `WayangA2aAgentExecutor` boundary, `WayangA2aSendMessageService`,
send-message result records, and HTTP handlers for `SendMessage` and
`SendStreamingMessage`. A2A requests now create submitted tasks, append inbound
message history, mark work in progress, invoke a pluggable Wayang executor,
persist completed/failed status, capture answer artifacts, and expose the same
stored task through the existing task HTTP handlers.

Recent A2A HTTP harness foundation:
Added dependency-free scenario, exchange, result, issue, harness, and built-in
smoke scenario records for A2A HTTP bindings. The harness can now replay Agent
Card discovery, send-message execution, task reads, subscription replay, and
push notification config routes through the dispatcher, then emit compact
machine-readable pass/fail reports with flattened issue summaries for CI, CLI,
and future Quarkus smoke surfaces.

Recent A2UI HTTP smoke runner improvement:
`WayangA2uiHttpSmokeRunner` and `WayangA2uiHttpSmokeResult` now provide a
dependency-free operational smoke entrypoint for CLI, REST, and CI integration.
The runner executes the built-in smoke suite, applies the matching expectation,
and returns one JSON-ready result with pass/fail status, exit code, suite
report, expectation result, and route metadata.

Recent A2UI HTTP smoke endpoint improvement:
`GET /a2ui/smoke` is now part of the default A2UI HTTP route catalog and
dispatcher. The endpoint runs the same built-in smoke suite through the bridge,
returns a transport-envelope `http-smoke-result`, and exposes pass/fail status,
exit code, suite id, scenario count, issue count, and route count metadata for
framework adapters and operational probes.

Recent A2UI HTTP smoke contract fixture improvement:
Contract fixtures now lock the compact `WayangA2uiHttpSmokeResult` JSON shape
and its transport-response metadata projection. Future CLI, REST, CI, and
framework adapters can assert the smoke result body and `http-smoke-result`
metadata without depending on the full live smoke scenario transcript.

Recent A2UI HTTP smoke summary improvement:
`WayangA2uiHttpSmokeSummary` now decodes smoke endpoint responses, transport
responses, or raw smoke-result JSON into a compact consumer view with pass/fail
state, exit code, suite id, scenario count, route count, issue count,
`smokeResult`, and `successfulExit`. Smoke metadata now counts both suite report
issues and expectation-validation issues so operational probes do not miss
expectation-only failures.

Recent A2UI HTTP smoke probe result improvement:
`WayangA2uiHttpSmokeProbeResult` now wraps the HTTP smoke endpoint response with
status code, route operation, `Allow`, transport outcome, decoded smoke summary,
headers, final pass/fail state, and exit code. CLI, REST, CI, and framework
adapters can now make one operational decision from the HTTP layer and decoded
smoke metadata together.

Recent A2UI HTTP smoke issue summary improvement:
`WayangA2uiHttpSmokeSummary` now flattens suite report issues and
expectation-validation issues into one copied `issues` list tagged by source.
Operational probes can now show concrete smoke failure reasons alongside the
existing pass/fail state, exit code, route count, and issue count without
walking the nested smoke result body.

Recent A2UI HTTP smoke summary/probe contract fixture improvement:
Contract fixtures now lock the public JSON shapes for
`WayangA2uiHttpSmokeSummary` and `WayangA2uiHttpSmokeProbeResult`. The fixtures
cover decoded smoke metadata, flattened issues, the original smoke result body,
HTTP status, route operation, `Allow`, transport outcome, headers, final
pass/fail state, and exit code for downstream adapter compatibility checks.

Recent A2UI HTTP failed smoke contract fixture improvement:
Contract fixtures now lock failing `WayangA2uiHttpSmokeSummary` and
`WayangA2uiHttpSmokeProbeResult` shapes, including expectation-validation issue
flattening, rejected transport outcome, failure exit code, and HTTP route
headers.

Recent A2UI HTTP binding report probe improvement:
`WayangA2uiHttpBindingReportProbeResult` now decodes binding-report HTTP
responses into one operational decision object. Probes expose status code,
route operation, `Allow`, transport outcome, content and envelope encoding,
binding-report metadata, route/handler counts, missing/orphan handler lists,
copied body data, headers, and final pass/fail state for CLI, REST, CI, and
framework adapters.

Recent A2UI HTTP incomplete binding report probe contract improvement:
Binding-report probes now flatten missing and orphan handler operations into a
copied `issues` list with an `issueCount`. Contract fixtures now lock both the
complete and incomplete probe shapes, including failed pass state, HTTP route
headers, transport metadata, missing handlers, orphan handlers, and issue
messages.

Recent A2A JSON-RPC transport scaffold:
Added JSON-RPC 2.0 request/response/error envelopes and a JSON-RPC-over-HTTP
dispatcher in `a2a/a2a-wayang`. The dispatcher maps A2A PascalCase methods to
the same send-message service and task store used by the HTTP binding, returns
`application/json` for regular calls, `text/event-stream` for streaming methods,
and emits standard JSON-RPC errors plus A2A task/operation errors.

Recent A2A JSON-RPC smoke harness improvement:
Added dependency-free JSON-RPC scenario, exchange, result, issue, harness, and
built-in smoke scenario records. JSON-RPC smoke runs now encode request
envelopes, route them through the dispatcher, decode JSON and SSE responses, and
mark JSON-RPC `error` envelopes as scenario issues even when the HTTP transport
status is successful.

Recent A2A JSON-RPC smoke runner improvement:
`WayangA2aJsonRpcSmokeRunner` and `WayangA2aJsonRpcSmokeResult` now provide a
compact operational smoke entrypoint for CLI, REST, framework adapters, and CI.
The runner executes the built-in JSON-RPC smoke scenario and returns one
JSON-ready result with pass/fail state, exit code, scenario report, issue
summary, task ids, message id, and exchange count metadata.

Recent A2A JSON-RPC smoke summary improvement:
`WayangA2aJsonRpcSmokeSummary` now decodes live smoke results or raw smoke-result
JSON into a compact consumer view with pass/fail state, exit code, scenario id,
exchange count, issue count, `smokeResult`, `successfulExit`, flattened issues,
attributes, and original body data.

Recent A2A JSON-RPC smoke probe result improvement:
`WayangA2aJsonRpcSmokeProbeResult` now wraps JSON-RPC smoke results in an
HTTP-shaped response and decodes those responses into one operational decision
object. Probes now expose status code, transport success, route operation,
protocol version, content type, decoded summary, headers, final pass/fail state,
and exit code for CLI, REST, CI, and framework adapters.

Recent A2A JSON-RPC smoke contract fixture improvement:
Contract fixtures now lock the public JSON shapes for
`WayangA2aJsonRpcSmokeResult`, `WayangA2aJsonRpcSmokeSummary`, and
`WayangA2aJsonRpcSmokeProbeResult`. The fixtures cover deterministic JSON-RPC
error handling, flattened scenario issues, HTTP-shaped smoke headers, decoded
summary metadata, final pass/fail state, and exit code.

Recent A2A JSON-RPC passing smoke contract fixture improvement:
Contract fixtures now also lock the passing `WayangA2aJsonRpcSmokeResult`,
`WayangA2aJsonRpcSmokeSummary`, and `WayangA2aJsonRpcSmokeProbeResult` shapes.
The fixtures cover a deterministic successful JSON-RPC exchange, empty issue
lists, success exit code, HTTP smoke headers, and decoded summary body data.

Recent A2A JSON-RPC HTTP adapter improvement:
`WayangA2aJsonRpcHttpAdapter` now provides a dependency-free HTTP-shaped surface
for the JSON-RPC binding. The adapter validates endpoint paths, methods,
Content-Type, Accept negotiation, request bodies, streaming response media types,
OPTIONS responses, and smoke probe access before delegating to the JSON-RPC
dispatcher or smoke runner.

Recent A2A JSON-RPC HTTP config improvement:
`WayangA2aJsonRpcHttpConfig` now centralizes runtime-neutral exposure settings
for the JSON-RPC HTTP adapter. It normalizes endpoint and smoke paths, supports
map/builder binding for framework or CLI configuration, exposes a smoke toggle,
and prevents ambiguous endpoint/smoke path collisions.

Recent A2A JSON-RPC binding report improvement:
`WayangA2aJsonRpcBindingReport` now exposes a JSON-ready diagnostic view of the
configured JSON-RPC binding surface. Runtime, CLI, REST, CI, and framework
adapters can inspect endpoint/smoke paths, allowed methods, media types,
streaming methods, protocol version, and config snapshots without issuing a live
A2A request.

Recent A2A JSON-RPC binding report endpoint improvement:
The JSON-RPC HTTP adapter now exposes the binding report through a configurable
`GET /a2a/jsonrpc/binding-report` diagnostic route with `OPTIONS`, Accept
validation, route headers, collision checks, and an enable/disable toggle.
Binding report JSON now includes the diagnostic route and config snapshot fields
so framework adapters can discover endpoint, smoke, and report exposure from one
payload.

Recent A2A JSON-RPC binding report probe improvement:
`WayangA2aJsonRpcBindingReportProbeResult` now decodes binding-report HTTP
responses into one operational decision object. Probes expose status code,
route operation, protocol version, content type, `Allow`, method counts,
streaming methods, endpoint/smoke/report paths, route completeness, copied body
data, headers, and final pass/fail state for CLI, REST, CI, and framework
adapters.

Recent A2A JSON-RPC incomplete binding report probe contract improvement:
Binding-report probes now include a copied `issues` list and `issueCount` for
HTTP, route, content-type, method-count, and missing path failures. Contract
fixtures now lock both complete and incomplete probe shapes, including failed
pass state, missing endpoint/smoke/report paths, empty methods, and diagnostic
issue messages.

Recent A2A JSON-RPC adapter probe convenience improvement:
`WayangA2aJsonRpcHttpAdapter` now exposes direct `smokeResponse`,
`smokeProbe`, `bindingReportResponse`, and `bindingReportProbe` helpers.
`WayangA2aJsonRpcSmokeProbeResult` can also run through the configured HTTP
adapter, letting runtime callers probe the same exposed paths they serve without
manually constructing dependency-free HTTP requests.

Recent A2A JSON-RPC readiness probe improvement:
`WayangA2aJsonRpcReadinessProbeResult` now composes the binding-report probe and
optional smoke probe into one operational readiness decision. It reports
binding-report pass/fail, whether smoke is required, smoke pass/fail, exit code,
issues, and copied probe payloads so CLI, REST, CI, and framework adapters can
check the configured JSON-RPC surface with one helper.

Recent A2A JSON-RPC readiness endpoint improvement:
`GET /a2a/jsonrpc/readiness` is now a configurable JSON-RPC diagnostics route
alongside smoke and binding-report endpoints. It returns the composite readiness
probe as JSON, supports `OPTIONS`, advertises stable route headers, participates
in binding-report discovery, and can be disabled or moved through
`WayangA2aJsonRpcHttpConfig`.

Recent A2A JSON-RPC readiness decode/contract improvement:
A2A JSON-RPC readiness probes now round-trip from maps, raw JSON, and HTTP
endpoint responses. Binding-report and smoke probe records gained matching
map/JSON decoders, while a golden readiness fixture pins the no-smoke-required
payload and live endpoint tests cover the smoke-required branch.

Recent A2A JSON-RPC failed readiness contract improvement:
Readiness diagnostics now include a failed golden fixture for the common
binding-report-unavailable path. The fixture pins the 404 binding-report probe,
flattened readiness issue, empty smoke probe fallback, and HTTP endpoint decode
round trip so operator surfaces can render failed readiness consistently.

Recent A2A JSON-RPC readiness issue summary improvement:
`WayangA2aJsonRpcReadinessIssueSummary` now condenses readiness-level,
binding-report, and smoke issues into one flat operator-facing issue list. The
summary reports per-probe issue counts, supports map/JSON/HTTP response decode,
and has a failed golden fixture for CI, CLI, and REST diagnostic annotations.

Recent A2A JSON-RPC readiness issue summary adapter improvement:
`WayangA2aJsonRpcHttpAdapter` now exposes `readinessIssueSummary()` and
`readinessIssueSummaryResponse()` helpers. The summary response carries stable
JSON content, A2A protocol headers, and a `JsonRpcReadinessIssueSummary`
operation marker so gateway, REST, and CLI layers can publish compact readiness
issues without rewalking nested probe payloads.

Recent A2A JSON-RPC readiness issue summary endpoint improvement:
The compact issue summary is now a first-class configurable diagnostic route at
`GET /a2a/jsonrpc/readiness/issues` with `OPTIONS`, route metadata headers,
binding-report discovery, config map aliases, collision validation, and golden
failed-readiness fixture coverage. This keeps gateway and CLI integrations on
the same route contract as the adapter helpers.

Recent A2A JSON-RPC route catalog improvement:
Added `WayangA2aJsonRpcHttpRoute` and `WayangA2aJsonRpcHttpRouteCatalog` as the
single JSON-ready manifest for configured JSON-RPC HTTP routes. The adapter now
serves configurable `GET /a2a/jsonrpc/route-catalog` with `OPTIONS`, exposes
`routeCatalog()` and `routeCatalogResponse()` helpers, reports the catalog path
from binding diagnostics, and validates route-catalog discovery in readiness
probe contracts.

Recent A2A JSON-RPC route catalog probe improvement:
`WayangA2aJsonRpcRouteCatalogProbeResult` now validates the route catalog
endpoint directly, including HTTP status, route-operation headers, JSON content,
route counts, and required descriptors for endpoint, smoke, catalog,
diagnostics-report, binding-report, readiness, and readiness-issue-summary
routes. Readiness now composes this probe when route-catalog exposure is
enabled, and compact issue summaries report route-catalog pass state and issue
counts separately.

Recent A2A JSON-RPC diagnostics report improvement:
`WayangA2aJsonRpcDiagnosticsReport` now gives CLI, CI, REST, and gateway layers
one compact aggregate payload for the JSON-RPC HTTP surface. The report reuses
readiness, binding-report, route-catalog, and optional smoke probes, projects a
stable check list, preserves flattened readiness issues, captures the runtime
config under `attributes.config`, and has a golden contract fixture for stored
diagnostic output.

Recent A2A JSON-RPC diagnostics endpoint improvement:
`WayangA2aJsonRpcHttpAdapter` now serves the aggregate diagnostics report at
configurable `GET /a2a/jsonrpc/diagnostics` with `OPTIONS`, route metadata
headers, and direct `diagnosticsReportResponse()` support. HTTP config,
binding reports, route catalogs, route-catalog probes, readiness fixtures, and
docs now treat the compact diagnostics report as a first-class published
surface.

Recent A2A JSON-RPC route publication improvement:
Added `WayangA2aJsonRpcHttpPublication` and
`WayangA2aJsonRpcHttpRouteBinding` as the framework-neutral registration view
for enabled JSON-RPC HTTP routes. Runtime modules can now iterate
`adapter.routePublication().bindings()` to register method/path/operation
metadata and delegate both primary methods and `OPTIONS` requests back through
the existing adapter, keeping route catalog, config toggles, diagnostics, and
dispatch behavior in one source of truth.

Recent A2A JSON-RPC spec compliance improvement:
Added `WayangA2aJsonRpcSpecOperation` and
`WayangA2aJsonRpcSpecComplianceReport` as the JSON-ready A2A v1.0 method
mapping guardrail for the JSON-RPC binding. The report derives JSON-RPC, gRPC,
HTTP+JSON/REST, streaming, media-type, and support metadata from
`A2aHttpRouteCatalog.standard()`, verifies that the configured JSON-RPC
endpoint is present in `adapter.routePublication()`, round-trips through JSON,
and is exposed directly as `adapter.specComplianceReport()` for CLI/CI and
framework integration checks.

Recent A2A JSON-RPC spec compliance contract improvement:
Locked `adapter.specComplianceReport()` to the golden
`wayang-jsonrpc-spec-compliance-report.json` fixture and added contract tests
for direct serialization plus JSON round-trip reconstruction. The fixture now
captures the full operation matrix and route-publication snapshot, giving
framework adapters, CLIs, and CI checks a stable external payload to compare
against.

Recent A2A JSON-RPC spec compliance endpoint improvement:
Published the spec-compliance report as configurable
`GET /a2a/jsonrpc/spec-compliance` with `OPTIONS` route metadata and direct
`adapter.specComplianceReportResponse()` support. HTTP config, binding reports,
route catalogs, route-catalog probes, readiness fixtures, and diagnostics
contracts now include `JsonRpcSpecCompliance`, while config path-collision
validation is consolidated behind a surface-list check for easier future
diagnostic endpoints.

Recent A2A JSON-RPC protocol version negotiation improvement:
The JSON-RPC HTTP adapter now honors the standard `A2A-Version` service
parameter header. Route responses emit both `A2A-Version` and the existing
Wayang diagnostic protocol-version header, probes can decode either header, and
unsupported client versions are rejected before execution with the A2A
`VersionNotSupportedError` JSON-RPC code `-32009`. Contract fixtures now lock
the standard header into probe payloads.

Recent A2A Agent Card discovery cache improvement:
The neutral A2A HTTP dispatcher now emits deterministic Agent Card cache
metadata for `/.well-known/agent-card.json` and `/extendedAgentCard`. Agent Card
responses carry `Cache-Control` and SHA-256 based `ETag` headers, and matching
`If-None-Match` requests return `304 Not Modified` while retaining route
metadata headers. Discovery clients and gateways can now cache public/extended
cards without re-downloading unchanged card bodies.

Recent A2A required extension negotiation improvement:
Added a shared extension negotiator for A2A HTTP surfaces. Required extensions
declared in the Agent Card are enforced through the comma-separated
`A2A-Extensions` service parameter before REST handler or JSON-RPC method
execution. Public Agent Card discovery remains open, REST routes return
`extension_support_required`, and JSON-RPC maps the failure to the A2A
`ExtensionSupportRequiredError` code `-32008`.

Recent A2A extended Agent Card authorization improvement:
The neutral HTTP dispatcher now has an optional
`WayangA2aExtendedAgentCardAuthorizer` boundary for `/extendedAgentCard`.
Default constructors preserve open harness behavior, while adapters can require
a bearer token or provide a custom authorizer. Unauthorized requests return
HTTP `401`, `extended_agent_card_unauthorized`, and `WWW-Authenticate` while
retaining route metadata headers.

Recent A2A JSON-RPC extended Agent Card authorization improvement:
The JSON-RPC HTTP adapter now accepts the same extended Agent Card authorizer
used by the neutral REST dispatcher. `GetExtendedAgentCard` requests can be
rejected before method dispatch with HTTP `401`, a JSON-RPC custom
authentication error code `-32010`, and `WWW-Authenticate`, while authorized
requests can still fetch the extended card before sending required extension
opt-ins for operational methods.

Recent A2A capability validation improvement:
Added a shared capability guard for optional A2A operations. REST and JSON-RPC
dispatchers now enforce `capabilities.streaming`,
`capabilities.pushNotifications`, and `capabilities.extendedAgentCard` before
invoking handlers. Push notification failures map to
`PushNotificationNotSupportedError` (`-32003`) for JSON-RPC, streaming and
unsupported extended-card calls map to `UnsupportedOperationError` (`-32004`),
and declared-but-missing extended cards return
`extended_agent_card_not_configured`/`-32007` style failures.

Recent A2A tenant validation improvement:
Added a shared tenant guard for explicit operation tenant hints. When the Agent
Card advertises tenant-scoped interfaces, REST and JSON-RPC dispatchers reject
requests whose `tenant` is not advertised before invoking handlers. Tenantless
cards and tenantless requests remain compatible, REST failures return
`tenant_not_supported`, and JSON-RPC failures map to `Invalid params` (`-32602`).

Recent A2A tenant-scoped task access improvement:
`WayangA2aTaskQuery` now carries an optional tenant filter and matches it
against task metadata. REST task handlers and JSON-RPC task methods hide
tenant-mismatched direct task-id lookups as task-not-found before get, cancel,
subscribe, and push notification config operations, preventing shared stores
from leaking cross-tenant task existence through task IDs.

Recent A2A task lifecycle validation improvement:
Added shared task lifecycle invariants for the local A2A task store. Terminal
tasks now reject cancel/status/history/artifact mutations instead of silently
returning unchanged state, and status updates cannot target
`TASK_STATE_UNSPECIFIED`. REST surfaces map these failures to
`unsupported_operation`; JSON-RPC maps them to `UnsupportedOperationError`
(`-32004`).

Recent A2A SendMessage request validation improvement:
Added a shared SendMessage payload guard for REST and JSON-RPC entry points.
SendMessage and SendStreamingMessage now require inbound `ROLE_USER` messages
and validate message part input modes against `AgentCard.defaultInputModes`
before task creation. Unsupported roles and input modes are rejected before
handler execution so task stores do not record payloads the advertised Agent
Card cannot accept.

Recent A2A SendMessage skill routing validation improvement:
The SendMessage request guard now also validates Wayang `allowedSkills` metadata
against the Agent Card skill IDs before the mapper builds an `AgentRequest`.
REST returns `skill_not_supported` for unknown skill routes and JSON-RPC maps
the same failure to `Invalid params`, preventing clients from routing execution
to unadvertised skills.

Recent A2A selected-skill mode validation improvement:
Added `WayangA2aSkillRouting` to resolve SendMessage `allowedSkills` hints and
skill-scoped input/output mode support from the Agent Card. Request input-mode
checks and configuration output-mode checks now use selected skill modes when
present, falling back to Agent Card defaults only when selected skills omit mode
declarations.

Recent A2A SendMessage preflight consolidation:
Added `WayangA2aSendMessagePreflight` as the dispatcher-level coordinator for
SendMessage request and configuration validation. REST and JSON-RPC dispatchers
now parse valid SendMessage payloads once, reuse the decoded request for
handler execution, and keep validation order explicit while leaving malformed
payloads to the normal request parsing error path.

Recent A2A SendMessage tenant preflight reuse improvement:
HTTP SendMessage dispatch now enriches valid requests with the parsed
`A2aSendMessageRequest` before tenant validation, and JSON-RPC dispatch feeds
the same typed preflight result into tenant validation. `WayangA2aTenantGuard`
reads parsed SendMessage requests before raw payload parsing, so tenant checks,
preflight validation, and handler execution share the same decoded payload.

Recent A2A tenant hint consolidation improvement:
Added `WayangA2aTenantHints` as the shared tenant extraction helper for HTTP
requests, JSON-RPC params, parsed SendMessage requests, and stored tasks. Tenant
guard validation, task scoping, task queries, and execution mapping now use the
same alias and metadata rules.

Recent A2A skill hint consolidation improvement:
Added `WayangA2aSkillHints` as the shared `allowedSkills` extraction helper for
SendMessage metadata. Skill validation and execution mapping now use the same
request/message metadata merge order, de-duplication, and legacy
`wayang.allowedSkills` alias support.

Recent A2A task access consolidation improvement:
Added `WayangA2aTaskAccess` as the shared tenant-scoped task lookup helper.
REST task handlers and the JSON-RPC dispatcher now use the same task visibility
rule before returning, canceling, subscribing to, or editing push config for a
task. Task list queries now delegate tenant matching to the same visibility
rule, so single-task reads and list filtering cannot drift.

Recent A2A push notification key parsing consolidation improvement:
`WayangA2aPushNotificationConfig` now owns push config request key parsing,
including create-time task id lookup, required task/config ids, `configId`/`id`
aliases, and the default config id fallback. JSON-RPC push config handlers use
those shared rules instead of open-coded parameter extraction.

Recent A2A SendMessage identity consolidation improvement:
Added `WayangA2aSendMessageIdentity` as the shared task/context id resolver for
SendMessage execution and smoke scenarios. The service now uses one rule for
message ids, metadata fallback ids, generated task ids, and context fallback,
while HTTP and JSON-RPC smoke scenarios share the explicit message task-id
requirement.

Recent A2A agent response projection consolidation improvement:
Added `WayangA2aAgentResponseProjection` as the shared projection helper for
Wayang `AgentResponse` values exposed through A2A. SendMessage lifecycle
updates and mapper-built A2A responses now share response message id, text, and
metadata rules.

Recent A2A JSON report map coercion consolidation improvement:
Added `WayangA2aJsonReportMaps` as the shared JSON body, child map, list-copy,
text, number, and boolean coercion helper for A2A JSON-RPC report/probe DTOs.
Smoke summaries, smoke probes, binding-report probes, route-catalog probes,
readiness probes, readiness issue summaries, diagnostics reports, and spec
compliance reports now share strict versus lenient body parsing rules without
duplicating private helpers in each record.

Recent A2A response header projection consolidation improvement:
Added `WayangA2aHttpResponseHeaders` as the shared route-operation, Allow, and
protocol-version header projection helper for HTTP-shaped A2A responses.
JSON-RPC smoke, binding-report, and route-catalog probes now share one protocol
version fallback rule instead of carrying duplicate private header readers.

Recent A2A JSON-RPC HTTP response construction consolidation improvement:
Added `WayangA2aJsonRpcHttpResponses` as the shared JSON response/header factory
for JSON-RPC HTTP diagnostics. Binding reports, route catalogs, diagnostics,
spec compliance, readiness, readiness issue summaries, smoke responses, and the
HTTP adapter now share JSON content-type, route-operation, protocol-version,
and optional Allow header construction.

Recent A2A JSON-RPC HTTP request construction consolidation improvement:
Added `WayangA2aJsonRpcHttpRequests` as the shared request factory for JSON-RPC
diagnostic probes. Adapter diagnostic response helpers and binding-report probes
now share JSON `Accept` request construction, while route-catalog matching uses
the same empty route-probe request shape.

Recent A2A JSON-RPC diagnostic route guard consolidation improvement:
Added `WayangA2aJsonRpcDiagnosticRouteGuard` as the shared GET and JSON Accept
validator for JSON-RPC diagnostic HTTP routes. Smoke, binding-report,
route-catalog, diagnostics, spec-compliance, readiness, and readiness issue
summary dispatch paths now share method/accept error construction instead of
duplicating route-specific validation blocks.

Recent A2A JSON-RPC diagnostic route dispatch consolidation improvement:
`WayangA2aJsonRpcDiagnosticRouteGuard` now also owns the successful diagnostic
dispatch/decorate flow. Diagnostic adapter methods provide only route metadata
and a response supplier, while the guard applies route-operation, protocol, and
Allow headers consistently after validation.

Recent A2A JSON-RPC route descriptor consolidation improvement:
Added package-local `WayangA2aJsonRpcHttpRouteDescriptor` as the shared route
metadata source for JSON-RPC endpoint and diagnostic surfaces. Binding reports
and route catalogs now project from the same descriptor list, preserving their
existing JSON shapes while removing parallel path, method, media type, and
enabled-state definitions.

Recent A2A JSON-RPC adapter route dispatch consolidation improvement:
`WayangA2aJsonRpcHttpAdapter` now builds a descriptor-derived dispatch table at
construction time. Endpoint, diagnostic, and OPTIONS routing use descriptor
path, operation, Allow, and route-name metadata, so disabled diagnostic paths
still fall through as 404s while enabled routes share one matching flow.

Recent A2A JSON-RPC HTTP error response consolidation improvement:
`WayangA2aJsonRpcHttpResponses` now owns route-aware JSON error envelope
construction for JSON-RPC HTTP diagnostics. The adapter and diagnostic route
guard delegate error body and header creation through the same factory, keeping
status, content type, operation, protocol version, and Allow projection aligned.

Recent A2A JSON-RPC HTTP request context consolidation improvement:
Added package-local `WayangA2aJsonRpcHttpRequestContext` to parse JSON-RPC HTTP
request bodies once for id, method, and expected response media type. Endpoint
dispatch now reuses the same context for validation, version errors, extension
errors, and extended-card authorization errors instead of reparsing request
bodies across private adapter helpers.

Recent A2A JSON-RPC endpoint route guard improvement:
Added package-local `WayangA2aJsonRpcEndpointRouteGuard` for endpoint HTTP
method, body, content-type, and Accept validation. The JSON-RPC HTTP adapter now
delegates raw endpoint validation through the guard while retaining protocol
version, extension, authorization, and dispatcher orchestration responsibilities.

Recent A2A JSON-RPC endpoint preflight guard improvement:
Added package-local `WayangA2aJsonRpcEndpointPreflightGuard` for endpoint
protocol-version, required-extension, and extended Agent Card authorization
checks. The JSON-RPC HTTP adapter now performs route validation, preflight, and
dispatch as distinct steps while preserving JSON-RPC error ids and route
headers for rejected requests.

Recent A2A JSON-RPC HTTP error envelope factory improvement:
`WayangA2aJsonRpcHttpResponses` now owns JSON-RPC error HTTP response
construction for endpoint preflight failures. Version, extension, and extended
Agent Card authorization rejects share one factory for JSON-RPC id preservation,
content type, protocol headers, operation metadata, and Allow projection.

Recent A2A JSON-RPC adapter route handler consolidation improvement:
`WayangA2aJsonRpcHttpAdapter` now dispatches descriptor keys through switch
expressions instead of a repeated route-key `if` chain and per-diagnostic
wrapper methods. Diagnostic routes share one route guard wrapper and differ only
by their response supplier, keeping endpoint and diagnostic responsibilities
clearer inside the adapter.

Recent A2A JSON-RPC HTTP route table improvement:
Added package-local `WayangA2aJsonRpcHttpRouteTable` to own enabled route
matching, OPTIONS short-circuiting, and JSON-RPC path-not-found responses. The
JSON-RPC HTTP adapter now delegates path lookup and unmatched-path handling to
the route table before executing endpoint or diagnostic route handlers.

Recent A2A JSON-RPC HTTP route handler registry improvement:
Added package-local `WayangA2aJsonRpcHttpRouteHandlers` to own endpoint and
diagnostic route execution after route-table matching. The JSON-RPC HTTP
adapter now keeps configuration and route facade helpers while dispatch
execution lives in a focused registry with direct unit coverage for diagnostics,
unconfigured smoke handling, and endpoint validation delegation.

Recent A2A JSON-RPC HTTP diagnostic handler registry improvement:
Added package-local `WayangA2aJsonRpcHttpDiagnosticHandlers` to own diagnostic
response suppliers by route key. Route execution now branches only between the
JSON-RPC endpoint and diagnostic surfaces, while diagnostic response lookup,
unconfigured smoke errors, and unsupported diagnostic keys have focused tests.

Recent A2A JSON-RPC diagnostic handler coverage improvement:
`WayangA2aJsonRpcHttpDiagnosticHandlers` now reports handler keys, missing
diagnostic route handlers, orphan diagnostic handlers, and completeness against
route descriptors. The JSON-RPC HTTP adapter validates this coverage during
construction, so future diagnostic route additions fail early when handler
wiring drifts from the descriptor catalog.

Recent A2A JSON-RPC binding-report handler coverage improvement:
Added package-local `WayangA2aJsonRpcHttpDiagnosticHandlerCoverage` and exposed
diagnostic handler coverage in the JSON-RPC binding report payload. Binding,
binding-probe, and readiness contract fixtures now include route keys, handler
keys, missing handlers, orphan handlers, and completeness so operator-facing
diagnostics can show route/handler drift directly.

Recent A2A JSON-RPC binding-probe handler coverage improvement:
`WayangA2aJsonRpcBindingReportProbeResult` now decodes diagnostic handler
coverage into top-level probe fields and treats incomplete coverage as a normal
binding-report issue. Readiness, diagnostics, and issue-summary surfaces now
carry route/handler drift as explicit failed probe data rather than opaque body
metadata.

Recent A2A JSON-RPC diagnostic coverage decode consolidation:
`WayangA2aJsonRpcHttpDiagnosticHandlerCoverage` now owns map decoding for
binding-report coverage payloads. Binding-report probes reuse the coverage
value object instead of unpacking route keys, handler keys, and missing/orphan
lists by hand, while missing coverage payloads decode as incomplete.

Recent A2A JSON-RPC binding-report section decode consolidation:
Added package-local `WayangA2aJsonRpcBindingReportSection` for the repeated
path/enabled section shape inside JSON-RPC binding report payloads. Binding
report probes now decode endpoint, smoke, route catalog, diagnostics,
spec-compliance, binding, readiness, and readiness-issue-summary sections
through the same helper while keeping the public probe projection unchanged.

Recent A2A JSON-RPC binding-report section issue consolidation:
`WayangA2aJsonRpcBindingReportSection` now owns missing-path issue projection
for binding report sections. Binding-report probes pass ordered section values
into issue generation instead of duplicating path-empty checks and issue fields
for each diagnostic surface.

Recent A2A JSON-RPC binding-report section set consolidation:
Added package-local `WayangA2aJsonRpcBindingReportSections` to decode the
named binding-report diagnostic section set and preserve the required
missing-path issue order in one place. Binding-report probes now consume this
section set instead of constructing each section and its ordered issue list
locally.

Recent A2A JSON-RPC diagnostic issue factory consolidation:
Added package-local `WayangA2aJsonRpcDiagnosticIssues` as the canonical issue
map factory for JSON-RPC diagnostics. Binding-report probe and section issue
projection now share the same source/code/field/expected/actual/message shape.

Recent A2A JSON-RPC route-catalog issue factory migration:
`WayangA2aJsonRpcRouteCatalogProbeResult` now reuses the shared diagnostic
issue factory for HTTP, route-operation, JSON-content, route-count, and missing
descriptor issues. A focused route-catalog probe test now pins the
`route_descriptor_missing` issue payload while preserving existing contract
fixtures.

Recent A2A JSON-RPC route-catalog descriptor coverage consolidation:
Added package-local `WayangA2aJsonRpcRouteCatalogDescriptorCoverage` to own the
required route-catalog descriptor matrix and ordered missing-descriptor issues.
Route-catalog probes now consume the coverage value object instead of computing
each descriptor flag and missing issue locally.

Recent A2A JSON-RPC probe response check consolidation:
Added package-local `WayangA2aJsonRpcProbeResponseChecks` to centralize
HTTP-success, route-operation, JSON-content, and positive-count issue
projection for diagnostic probes. Binding-report and route-catalog probes now
share these response checks while preserving their probe-specific issue codes
and messages.

Recent A2A JSON-RPC readiness check consolidation:
Added package-local `WayangA2aJsonRpcReadinessProbeCheck` to centralize
readiness probe status rows and compact probe-failure issue projection.
Readiness probes and aggregate diagnostics now share the same check model, so
future readiness signals can be added in one place without drifting JSON
diagnostics output.

Recent A2A JSON-RPC readiness issue breakdown consolidation:
Added package-local `WayangA2aJsonRpcReadinessIssueBreakdown` to own
per-probe issue grouping, flattened issue envelopes, metadata projection, and
per-probe issue counts. `WayangA2aJsonRpcReadinessIssueSummary` now projects
the breakdown instead of mixing aggregation mechanics with summary fields.

Recent A2A JSON-RPC readiness placeholder consolidation:
Added package-local `WayangA2aJsonRpcReadinessProbePlaceholders` to centralize
disabled optional probe null-objects for smoke and route-catalog probes.
`WayangA2aJsonRpcReadinessProbeResult` now names the disabled-probe behavior
instead of embedding long constructor literals in readiness orchestration.

Recent A2A JSON-RPC route surface registry consolidation:
Added package-local `WayangA2aJsonRpcHttpRouteSurface` to centralize JSON-RPC
HTTP route keys, route names, operations, descriptor fields, allow headers, and
publication/required-section ordering. HTTP route descriptors, binding-report
section checks, and route-catalog descriptor coverage now reuse the same route
surface metadata while preserving existing report and fixture ordering.

Recent A2A JSON-RPC config projection consolidation:
`WayangA2aJsonRpcHttpRouteSurface` now also owns route config path/enabled field
names and the canonical config map projection. `WayangA2aJsonRpcHttpConfig`
delegates `toMap()` to the route surface registry, keeping config output aligned
with the centralized JSON-RPC HTTP route surface order.

Recent A2A JSON-RPC config validation consolidation:
`WayangA2aJsonRpcHttpRouteSurface` now owns the enabled-path conflict check for
configured JSON-RPC HTTP surfaces. `WayangA2aJsonRpcHttpConfig` delegates
distinct-path validation to the route surface registry, keeping validation names
and config projection aligned with the same route metadata.

Recent A2A JSON-RPC readiness issue factory migration:
`WayangA2aJsonRpcDiagnosticIssues` now also owns the compact probe-failure issue
shape used by readiness probes. `WayangA2aJsonRpcReadinessProbeResult` delegates
binding-report, route-catalog, and smoke probe failure issue construction to the
shared factory while preserving the readiness JSON fixture shape.

Recent A2A JSON-RPC spec-compliance issue factory migration:
`WayangA2aJsonRpcSpecComplianceReport` now reuses the shared diagnostic issue
factory for endpoint-publication and method-mapping compliance issues. Focused
tests pin both spec-compliance issue projections while the existing contract
fixture continues to cover the passing report shape.

Recent A2A media type negotiation consolidation:
Added `WayangA2aMediaTypes` as the shared media-mode normalization and wildcard
matching helper for A2A SendMessage validation. Request input-mode checks and
configuration output-mode checks now use the same exact/wildcard matching rules,
so `text/*` and `*/*` behave consistently across REST and JSON-RPC surfaces.

Recent A2A SendMessage configuration improvement:
Added a shared SendMessage configuration guard for REST and JSON-RPC A2A entry
points. `acceptedOutputModes` now validates against Agent Card output modes
before execution, `taskPushNotificationConfig` requires the advertised push
notification capability, valid push configs are persisted with the created task,
and `historyLength` trims only the returned task projection while preserving the
stored lifecycle history.

Recent HTTP diagnostics documentation improvement:
Added `docs/HTTP_DIAGNOSTICS_PROBES.md` as the canonical operator-facing guide
for A2UI and A2A HTTP diagnostics. The guide consolidates route paths,
configurable A2A diagnostic settings, probe pass/fail semantics, issue
projection behavior, golden contract fixtures, and focused Maven verification
commands so CLI, REST, CI, readiness, and framework adapters can integrate the
new probe surfaces consistently.

Recent A2UI HTTP readiness probe improvement:
`WayangA2uiHttpReadinessProbeResult` now composes the A2UI binding-report probe
and smoke probe into one readiness decision. The default A2UI HTTP catalog now
exposes `GET /a2ui/readiness` with `OPTIONS`, route metadata headers, transport
metadata, adapter convenience helpers, and a golden readiness fixture so CLI,
REST, CI, and framework adapters can check the full A2UI diagnostic surface with
one probe.

Recent A2UI HTTP readiness decode improvement:
A2UI binding-report, smoke-summary, smoke-probe, and readiness-probe records now
support JSON/map decode factories where needed for endpoint round-trips.
`WayangA2uiHttpReadinessProbeResult.from(response)` decodes the transport-wrapped
`/a2ui/readiness` response back into the same readiness decision, while the
golden readiness fixture now verifies both raw readiness JSON and HTTP endpoint
decode paths.

Recent Agentic Commerce Wayang adapter foundation:
`agentic-commerce/agentic-commerce-wayang` now provides a Wayang-facing
checkout adapter module with connector config, a checkout service, an in-memory
seller connector, and checkout skill projection into `SkillDefinition`. The
adapter stays dependency-light while exposing deterministic local checkout
lifecycle behavior, protocol-aware response headers, and metadata-rich skill
definitions for create, retrieve, update, complete, and cancel checkout
operations.

Recent Agentic Commerce HTTP adapter improvement:
`AgenticCommerceHttpAdapter` now exposes checkout routes through a
dependency-free HTTP-shaped adapter with configurable public base/smoke paths,
OPTIONS responses, Accept negotiation, request validation, route metadata
headers, and connector delegation. The adapter strips the public base path into
core checkout routes, enriches delegated requests with operation/session
attributes, and serves the existing checkout smoke runner through a configurable
smoke endpoint.

Recent Agentic Commerce HTTP binding diagnostics improvement:
`AgenticCommerceHttpBindingReport` now provides a JSON-ready description of the
Wayang Agentic Commerce HTTP surface, including public checkout paths, route
operations, media types, smoke exposure, binding-report exposure, and adapter
config. `AgenticCommerceHttpAdapterConfig` now supports configurable
binding-report paths, and `AgenticCommerceHttpAdapter` serves the report over
HTTP alongside smoke/probe convenience entrypoints.

Recent Agentic Commerce executable skill improvement:
`AgenticCommerceCheckoutAgentSkill` and `AgenticCommerceCheckoutAgentSkills`
now turn the projected checkout skill definitions into runtime `AgentSkill`
instances backed by `AgenticCommerceCheckoutService`. Dynamic skill registries
can execute create, retrieve, update, complete, and cancel checkout operations
with map payloads while receiving the standard runtime skill result contract
plus protocol outputs such as status code, checkout session id, checkout
status, decoded body, and full checkout result metadata.

Recent Agentic Commerce skill registry bootstrap improvement:
`AgenticCommerceCheckoutSkillRegistryInstaller` now installs Agentic Commerce
checkout definitions and runtime skills into any Wayang `SkillRegistry`, either
as a full bundle, definitions-only, runtime-only, or selected skill ids.
`AgenticCommerceSkillRegistration` reports requested, registered, and missing
skill ids with JSON-ready metadata so API, CLI, and bootstrap layers can expose
clean installation status.

Recent Agentic Commerce checkout skill dispatcher improvement:
`AgenticCommerceCheckoutSkillDispatcher` now provides a stable execution facade
over the checkout runtime skills. API, CLI, gateway, and orchestrator layers can
inspect supported skill ids and protocol operations, then execute by skill id,
protocol operation, or a context map containing `skillId`, `skill`, or
`operation`, while receiving the same runtime skill result contract and
deterministic dispatcher diagnostics.

Recent Agentic Commerce Wayang runtime bundle improvement:
`AgenticCommerceWayangRuntime` now bundles the configured connector, checkout
service, HTTP adapter, skill dispatcher, smoke/probe helpers, binding report,
and skill registry installation helpers behind one runtime-neutral entrypoint.
API, CLI, gateway, and bootstrap code can now create an in-memory or configured
runtime without hand-assembling the Agentic Commerce integration pieces.

Recent Agentic Commerce runtime configuration improvement:
`AgenticCommerceConnectorConfig` now supports map/builder based construction
with seller-token aliases, nested headers, metadata attributes, and redacted
diagnostic output. `AgenticCommerceWayangRuntimeConfig` consolidates connector
and HTTP adapter settings into one runtime-neutral bootstrap object that can be
built from nested or flat maps and can create configured in-memory runtimes for
tests, CLI, API, and future persistence-backed bootstrap flows.

Recent Agentic Commerce bootstrap readiness improvement:
`AgenticCommerceWayangBootstrapReport` now gives API, CLI, gateway, and
operator bootstrap code one JSON-ready readiness object that combines runtime
config, connector identity, skill registration, smoke probe, binding report,
and bootstrap issues. `AgenticCommerceWayangRuntime` can now install
definitions/runtime skills and return the report in one call, while tests share
a single `AgenticCommerceTestSkillRegistry` fixture instead of duplicating
registry scaffolding across installer and runtime coverage.

Recent Agentic Commerce bootstrap policy improvement:
`AgenticCommerceWayangBootstrapConfig` now makes bootstrap policy explicit and
JSON-ready, including selected checkout skill ids, definitions/runtime-skill
installation modes, and readiness requirements for skill registration, smoke
probe, and binding routes. Runtime bootstrap methods now accept the policy
object, the bootstrap report includes it, and boolean map parsing is shared
through `AgenticCommerceWayangMaps` instead of being duplicated in adapter
configuration code.

Recent Agentic Commerce discovery manifest improvement:
`AgenticCommerceWayangManifest` now exposes a connector-free JSON discovery
document for Wayang Agentic Commerce support, combining runtime defaults,
bootstrap policy, HTTP binding diagnostics, skill ids, protocol operations, and
compact checkout skill descriptors. Configured runtimes can now emit a manifest
with their HTTP paths and bootstrap policy included, giving API, CLI, docs, and
operator tooling a stable discovery surface without requiring a live seller
connector call.

Recent Agentic Commerce persistence boundary improvement:
`AgenticCommerceWayangPersistenceStore` now defines the storage boundary for
runtime config, bootstrap policy, bootstrap report snapshots, and manifest
snapshots. `FileAgenticCommerceWayangPersistenceStore` provides the local JSON
fallback implementation with fixed filenames and replacement writes, while
runtime config now has a storage projection that preserves connector secrets for
explicit persistence without leaking them through redacted diagnostic maps.

Recent Agentic Commerce hybrid persistence improvement:
`HybridAgenticCommerceWayangPersistenceStore` now composes a primary persistence
store with a fallback store, reading from primary first, falling back on missing
or unavailable primary data, and optionally mirroring successful writes to the
fallback. This gives future DB, S3, RustFS, and other object-store adapters a
clean hybrid strategy while preserving the local file store as the durable
fallback path.

Recent Agentic Commerce persistence configuration improvement:
`AgenticCommerceWayangPersistenceConfig` now turns map-based persistence
settings into concrete stores, supporting file storage aliases, nested hybrid
primary/fallback configs, mirror-write policy, and explicit unsupported backend
errors for future DB, S3, RustFS, and object-store adapters. Bootstrap code can
now parse persistence settings once and call `buildStore()` instead of
hand-wiring file and hybrid constructors.

Recent Agentic Commerce persistence service improvement:
`AgenticCommerceWayangPersistenceService` now provides the operator-facing
facade over persistence stores, resolving runtime/bootstrap config defaults,
building configured runtimes, emitting manifests, and bootstrapping with
snapshot persistence in one workflow. API and CLI layers can now load config,
run bootstrap, and persist runtime config, bootstrap policy, bootstrap reports,
and manifests without duplicating store-specific orchestration.

Recent Agentic Commerce config reload improvement:
`AgenticCommerceWayangConfigSnapshot` now captures the effective runtime and
bootstrap configuration along with persisted/default source metadata and store
status. `AgenticCommerceWayangConfigReloadReport` compares snapshots to report
runtime-config changes, bootstrap-policy changes, source changes, and whether a
runtime rebuild or bootstrap rerun is recommended, giving API, CLI, and operator
layers a safe reload decision surface without hand-diffing config maps.

Recent Agentic Commerce connector factory improvement:
`AgenticCommerceConnectorFactoryConfig` now makes connector selection explicit
and map-configurable, with built-in `in-memory` support plus clear unsupported
connector errors for future HTTP, seller, and hosted connector adapters.
Config snapshots and the persistence service can now build runtimes from a
connector factory policy while still applying the persisted runtime connector
settings, removing scattered direct construction of local demo connectors from
API and CLI bootstrap paths.

Recent Agentic Commerce HTTP connector improvement:
`HttpAgenticCommerceConnector` now provides the first real seller transport
adapter behind the connector factory, using Java's dependency-free HTTP client
to send Agentic Commerce checkout requests to the configured seller `baseUrl`.
The connector merges request headers with configured seller auth/version/header
defaults, captures response headers and transport metadata, and converts
transport failures into protocol-shaped JSON error responses. The connector
factory now supports `http`, `seller-http`, `seller`, and `remote` aliases.

Recent Agentic Commerce connector policy improvement:
`AgenticCommerceConnectorPolicy` now provides a framework-neutral guard for
connector creation, with configurable connector-kind allowlists, HTTP seller host
allowlists, and HTTPS requirements. The connector factory, config snapshots, and
persistence service can now build runtimes under an explicit deployment policy
while retaining permissive defaults for local and in-memory development.

Recent Agentic Commerce persisted connector policy improvement:
`AgenticCommerceWayangRuntimeConfig` now carries `connectorPolicy` alongside the
seller connector and HTTP adapter settings, with clean storage maps for file,
hybrid, database, and future cloud-backed configuration stores. Runtime rebuilds
from config snapshots now apply the persisted policy by default, while explicit
policy overrides still produce runtimes whose manifests report the policy that
was actually enforced.

Recent Agentic Commerce persisted connector factory improvement:
`AgenticCommerceWayangRuntimeConfig` now also carries `connectorFactoryConfig`,
so the selected connector kind (`in-memory`, `http`, `seller-http`, `seller`, or
`remote`) can be loaded from the same persisted runtime configuration as seller
credentials and connector policy. Config snapshots and the persistence service
now expose no-arg runtime builders that honor the saved connector factory by
default, while explicit factory overrides remain available for tests and local
operator workflows.

Recent Agentic Commerce runtime preflight improvement:
`AgenticCommerceRuntimePreflightReport` now gives API, CLI, and admin layers a
single non-network readiness surface before runtime construction. The report
checks connector support, persisted connector policy issues, HTTP smoke/binding
expectations, route availability, source/defaulted config state, and persistence
store metadata, while `AgenticCommerceWayangConfigSnapshot` and the persistence
service now expose preflight helpers directly.

Recent Agentic Commerce runtime profile improvement:
`AgenticCommerceWayangRuntimeProfile` now provides named runtime/bootstrap
profiles for `local`, `seller-http`, `staging`, and `production` deployment
modes. Profiles compose the existing connector factory, seller connector config,
connector policy, HTTP adapter, bootstrap config, and preflight report surfaces,
and the persistence service can now save a profile into the normal runtime and
bootstrap config stores without introducing profile-specific storage paths.

Recent Agentic Commerce connector contract harness improvement:
`AgenticCommerceConnectorContractHarness` now wraps the core checkout HTTP
scenario harness with Wayang connector/runtime metadata, producing a JSON-ready
`AgenticCommerceConnectorContractReport` for the full create/retrieve/update/
complete/cancel lifecycle. The same contract can now run against in-memory
connectors, persisted runtimes, and HTTP seller connectors backed by deterministic
fake seller fixtures, giving future database, S3/RustFS, and hosted connectors a
shared pass/fail contract before integration.

Recent Agentic Commerce connector diagnostics improvement:
`AgenticCommerceConnectorDiagnostics` now exposes a compact, redacted connector
health surface across config snapshots, runtimes, and the persistence service.
It reports connector kind/support, config sources, storage summary, policy
status, redacted auth/header counts, transport host/scheme readiness, preflight
issues, and optional contract summaries without automatically calling remote
sellers from normal `toMap()` diagnostics.

Recent Agentic Commerce object-store persistence improvement:
`ObjectStoreAgenticCommerceWayangPersistenceStore` now persists runtime config,
bootstrap config, bootstrap reports, and manifests through a provider-neutral
object-store client boundary. Persistence config now accepts `object-store`,
`s3`, `s3-compatible`, `minio`, and `rustfs` aliases with bucket/prefix
settings, while app layers inject the concrete S3/RustFS client and can still
compose object storage with file fallback through the existing hybrid store.

Recent Agentic Commerce object-store resolver improvement:
`AgenticCommerceObjectStoreClientResolver` and
`AgenticCommerceObjectStoreClientRegistry` now let app layers route configured
object-store persistence locations to concrete clients by exact
provider/endpoint/bucket, provider plus bucket, endpoint, bucket, provider, or
default client. Hybrid persistence can now combine S3 primary storage with
RustFS or file fallback without forcing every object-store node through the same
client instance.

Recent Agentic Commerce persistence contract harness improvement:
`AgenticCommerceWayangPersistenceContractHarness` now gives file, object-store,
hybrid, database, and future cloud-backed persistence stores a shared write/read
round-trip contract for runtime config, bootstrap config, bootstrap reports, and
manifests. The harness reports structured issue codes instead of failing on the
first store exception, while `AgenticCommerceWayangPersistenceService` exposes
the same contract through its operator-facing facade.

Recent Agentic Commerce persistence transfer improvement:
`AgenticCommerceWayangPersistenceTransfer` now copies persisted runtime config,
bootstrap config, bootstrap reports, and manifests between any two persistence
stores using only the store boundary. The transfer report records copied,
skipped, verified, and failed documents with redacted diagnostics, allowing file
fallbacks, S3/RustFS buckets, hybrid stores, and future database stores to be
backed up or promoted without store-specific migration code.

Recent Agentic Commerce persistence transfer safety improvement:
`AgenticCommerceWayangPersistenceTransferOptions` now gives backup and promotion
flows dry-run planning, no-overwrite protection, and configurable post-copy
verification. Transfer reports expose planned, copied, skipped, blocked, and
failed document counts, while `AgenticCommerceWayangPersistenceService` exposes
the same optioned transfer path through the operator facade.

Recent Agentic Commerce persistence transfer plan improvement:
`AgenticCommerceWayangPersistenceTransferPlan` now gives operators a dedicated
mutation-free preview before applying a persistence transfer. The plan records
source/target store kinds, intended transfer options, copyable documents,
skipped documents, blocked documents, status snapshots, and issue codes, while
`AgenticCommerceWayangPersistenceService.planTransferTo(...)` exposes the same
preview/apply separation through the service facade.

Recent Agentic Commerce in-memory persistence improvement:
`InMemoryAgenticCommerceWayangPersistenceStore` now provides an ephemeral,
zero-I/O implementation of the same persistence boundary used by file,
object-store, hybrid, and future database stores. Persistence config accepts
memory/in-memory/ephemeral aliases, giving previews, tests, embedded runtimes,
and transfer dry runs a first-class store that still round-trips through the
shared persistence contract harness.

Recent Agentic Commerce persistence capability improvement:
`AgenticCommerceWayangPersistenceCapabilities` now normalizes store status into
durability, ephemerality, local-file, object-store, cloud, hybrid, mirror, and
fallback-readiness signals without forcing callers to type-check concrete store
classes. The service, snapshot, preflight, and connector diagnostics maps expose
the same capability summary, giving future database and cache-backed stores a
small status contract to plug into.

Recent Agentic Commerce persistence health improvement:
`AgenticCommerceWayangPersistenceHealthReport` now performs a non-mutating
store health pass that combines capability metadata with status readability,
document availability, load failures, and missing-document warnings. The
persistence service exposes `persistenceHealth()` and includes the same report
in its operator map, so dashboards and migration tools can distinguish an empty
but reachable store from an unreadable or partially failing adapter.

Recent Agentic Commerce persistence document catalog improvement:
`AgenticCommerceWayangPersistenceDocument` and
`AgenticCommerceWayangPersistenceDocuments` now centralize the persisted
document ids, file names, status keys, missing warnings, and load-failure issue
codes for runtime config, bootstrap config, bootstrap report, and manifest
snapshots. File stores, object stores, transfer reports, health reports,
capability summaries, and diagnostics now derive shared document metadata from
one catalog while preserving their existing public constants.

Recent Agentic Commerce persistence document status improvement:
`AgenticCommerceWayangPersistenceDocumentStatus` now gives persistence health
reports a catalog-indexed status entry for every stored runtime document. Each
entry carries availability, loadability, document-level issues, missing
warnings, and known store attributes such as file paths or object keys, while
the legacy aggregate availability booleans and counts remain available for
existing dashboards and adapters.

Recent Agentic Commerce persistence health summary improvement:
`AgenticCommerceWayangPersistenceHealthSummary` now projects health reports into
a compact operator status vocabulary: healthy, degraded, incomplete, and
unavailable. The summary includes missing and failed document ids, document
counts, issue/warning totals, and store capability attributes, giving CLI,
admin, and REST surfaces a stable health headline without parsing the detailed
document list.

Recent Agentic Commerce persistence document health index improvement:
`AgenticCommerceWayangPersistenceDocumentHealthIndex` now gives health reports a
lookup-oriented document status view keyed by persisted document id. Reports can
answer document availability by catalog entry or id, expose missing and failed
document ids, and include a JSON-ready `documentIndex` alongside the ordered
document list so dashboards are not coupled to catalog ordering.

Recent Agentic Commerce persistence health finding improvement:
`AgenticCommerceWayangPersistenceHealthFinding` now projects persistence health
issues and warnings into structured entries with severity, source, code, and
optional document metadata. Health reports still expose the legacy string
issue/warning lists, while also providing finding counts and JSON-ready findings
for dashboards, CLIs, and alerting integrations that need stable severity and
document scope without parsing message strings.

Recent Agentic Commerce persistence health finding catalog improvement:
`AgenticCommerceWayangPersistenceHealthFindingDefinition` and
`AgenticCommerceWayangPersistenceHealthFindings` now define the known
persistence health codes with default severity, source, blocking status, titles,
and remediation hints. Emitted findings enrich their JSON projection from the
catalog while retaining fallback metadata for unknown future codes, giving
operator surfaces stable guidance without scattering code-specific text.

Recent Agentic Commerce persistence provider boundary improvement:
`AgenticCommerceWayangPersistenceStoreProvider`,
`AgenticCommerceWayangPersistenceProviderContext`, and
`AgenticCommerceWayangPersistenceStoreProviders` now route persistence store
construction through a provider registry instead of hard-coded config switches.
The built-in registry covers file, in-memory, hybrid, and object-store stores,
while custom providers can add future database or hosted backends without
changing the persistence config record.

Recent Agentic Commerce database persistence adapter improvement:
`DatabaseAgenticCommerceWayangPersistenceStore` now provides a database-backed
document persistence adapter behind `AgenticCommerceDatabasePersistenceClient`
and `AgenticCommerceDatabasePersistenceClientResolver`. The database config
normalizes database/JDBC/Postgres aliases, table names, and namespaces, while
the default provider registry can build database stores when a client resolver
is supplied. An in-memory database client keeps contract tests deterministic and
gives embedded runtimes a no-dependency preview path.

Recent Agentic Commerce database client registry improvement:
`AgenticCommerceDatabasePersistenceClientRegistry` now resolves database
persistence clients by exact provider/table/namespace, provider+namespace,
table+namespace, namespace, table, provider, or default fallback. This mirrors
the object-store resolver pattern and lets hybrid database configurations route
primary and fallback stores to distinct JDBC/Postgres clients without hard-coded
client selection in persistence config.

Recent skill-management admin preflight DTO consistency improvement:
Admin validation buckets and deployment preflight reports now derive validity,
readiness, deployability, error counts, messages, and aggregate errors from
normalized validation bucket errors. `SkillManagementAdminViews` delegates those
summary invariants to the DTOs, reducing caller-supplied summary drift as new
admin projections and storage capabilities are added.

Recent skill-management admin event-prune DTO consistency improvement:
`SkillManagementAdminEventPruneReport` now derives success, changed state,
scanned/pruned counts, and failure behavior from normalized child reports,
failure text, dry-run/skipped flags, and pruned event references. The admin view
mapper now passes raw prune data into the DTO and lets the value boundary keep
the public prune projection internally consistent.

Recent skill-management admin sync DTO consistency improvement:
Definition and artifact admin sync changes now normalize known action names into
their correct changed flags, while sync status DTOs derive copied, updated,
unchanged, conflict, deleted, and changed counts from normalized change lists.
`SkillManagementAdminViews` now delegates sync summary invariants to those value
boundaries instead of precomputing duplicate totals.

Recent skill-management admin report consistency improvement:
Reconcile, maintenance, deployment, and bootstrap admin reports now derive their
summary booleans from normalized child projections. Reconcile consistency comes
from missing/orphaned versus created/removed lifecycle ids and failure text,
maintenance derives dry-run/changed/consistent from sync, reconcile, and prune
children, deployment mirrors maintenance, and bootstrap mirrors final inspection
readiness plus reconcile changes.

Recent skill-management admin value normalization improvement:
Admin DTO list, text, action, error-message, and summary-count normalization now
flows through `SkillManagementAdminValueSupport`. Validation buckets, preflight
reports, sync reports, event prune reports, reconcile reports, store statuses,
event pages, deployment history pages, and event projections reuse one
package-local normalization boundary instead of duplicating stream/filter/count
logic in each record.

Recent skill-management neutral preflight model improvement:
`SkillManagementPreflightReport` now owns operation-neutral configuration,
target-store, source-store, and capability validation buckets with shared ready,
error, and message aggregation. Deployment preflight wraps that neutral report
for compatibility, service factories expose `preflightValidation(...)` for
maintenance/deployment callers, and preflight failure event attributes read from
the same aggregate validation boundary.

Recent skill-management operation-aware preflight exception improvement:
`SkillManagementPreflightException` now provides the shared operation metadata
and neutral preflight report for failed preflight enforcement. Deployment
failures keep the existing deployment-specific wrapper and report accessor,
while maintenance factory failures now raise `SkillManagementMaintenancePreflightException`
with the same structured validation report and maintenance-specific event/error
semantics.

Recent skill-management runner preflight telemetry improvement:
`SkillManagementEventAttributes` now projects neutral `SkillManagementPreflightReport`
instances directly, and `SkillManagementMaintenanceRunner` records those
structured preflight attributes when runner-level maintenance capability checks
fail. Factory and runner preflight failures now share `preflightReady`,
deployable, bucket error counts, and bucket messages in maintenance failure
events.

Recent skill-management automatic preflight failure enrichment improvement:
`SkillManagementEventAttributes.failure(...)` now detects deployment and neutral
preflight exceptions and merges structured preflight attributes automatically.
Factory and maintenance-runner failure paths no longer need bespoke preflight
attribute plumbing, so future preflight enforcement points get consistent
failure telemetry through the shared event recorder path.

Recent skill-management maintenance-runner preflight boundary improvement:
`SkillManagementMaintenanceRunner` now exposes a side-effect-free
`preflight(...)` report for plan capability readiness and enforces that same
neutral report before store mutation. Unsupported event pruning now surfaces
through the shared capability-validation bucket before definition, artifact,
lifecycle, or event stores are touched.

Recent skill-management maintenance preflight consolidation improvement:
`SkillManagementMaintenancePreflight` now owns maintenance plan normalization
and the shared event-pruning capability rule. Runner-level live pruner checks
and service-factory config/override checks use the same helper, so future
maintenance capabilities can extend one preflight boundary instead of copying
plan-policy branching across orchestration code.

Recent skill-management preflight enforcement consolidation improvement:
`SkillManagementPreflightEnforcer` now owns ready checks, operation-specific
preflight exception creation, and optional context-aware failure event recording.
Maintenance runners and service factories delegate enforcement through that
boundary, leaving orchestration paths focused on assembling reports and running
stores rather than duplicating preflight failure mechanics.

Recent skill-management store-bundle extraction improvement:
`SkillManagementStoreBundleFactory` now owns target store assembly, target/source
store validation, maintenance-source fallback resolution, and event-sink override
capability checks. `SkillManagementServiceFactory` delegates persistence wiring
through the bundle boundary, keeping public service, maintenance, and deployment
orchestration separate from concrete store construction.

Recent skill-management preflight-service extraction improvement:
`SkillManagementPreflightService` now owns deployment preflight report assembly
and neutral validation bucket construction. `SkillManagementServiceFactory`
keeps its public preflight methods as delegates while readiness calculation
stays beside the store-bundle validation boundary instead of inside public
workflow orchestration.

Recent skill-management workflow-runner extraction improvement:
`SkillManagementWorkflowRunner` now owns configured maintenance and deployment
execution, including preflight enforcement, source-store fallback resolution,
operation-context propagation, and deployment event recording. A small
`SkillManagementServiceAssembler` builds services from live store bundles, so
`SkillManagementServiceFactory` now acts as a public facade instead of carrying
workflow execution internals.

Recent skill-management maintenance-store bundle improvement:
`SkillManagementMaintenanceStores` now represents the concrete source, target,
artifact, lifecycle, and event stores for a maintenance run. Store-bundle
assembly owns source fallback and custom source resolution, letting the workflow
runner invoke maintenance through one resolved store value instead of repeating
source/target mapping logic.

Recent skill-management maintenance-runner factory improvement:
`SkillManagementMaintenanceRunnerFactory` now owns construction of maintenance
runners and their shared sync/reconcile components. Configured workflows and the
public `SkillManagementService` maintenance APIs use the same runner factory,
while still allowing callers with a pre-resolved event pruner to preserve
effective pruning capability.

Recent skill-management deployment-history consistency improvement:
Deployment-history admin entries now own event-attribute decoding and
normalization for deployment, preflight, lifecycle, artifact, and event-prune
fields. `SkillManagementAdminViews` delegates entry mapping to that value
boundary, while deployment-history pages reuse shared admin count helpers for
derived summary totals. Malformed or negative numeric attributes now normalize
at the DTO boundary, keeping API/CLI/admin projections stable as deployment
event attributes evolve.

Recent skill-management admin count normalization improvement:
`SkillManagementAdminValueSupport` now centralizes non-negative count handling,
minimum count bounds, count-map sanitization, boolean event attributes, integer
event attributes, and attribute-prefix detection. Event summaries, event pages,
event-prune reports, deployment-history entries/pages, and store statuses now
reuse those helpers, so negative item counts, status counts, or malformed
deployment/preflight event attributes normalize consistently before reaching
API, CLI, and operator projections.

Recent skill-management core value normalization improvement:
`SkillManagementValueSupport` now owns package-level list, text, count, count
map, and event-attribute normalization for skill-management value objects.
Admin value support delegates neutral behavior to that boundary, while event
summaries and event pages use it directly. Core event windows now filter null
entries before matching/summarizing, clamp matched counts consistently, and
sanitize negative summary/count-map values before inspection, admin, CLI, or
API projections consume them.

Recent skill-management prune value normalization improvement:
Event-prune options, retention calculations, and prune results now share
`SkillManagementValueSupport` for non-negative counts, compact reference lists,
blank failure text, and null-child filtering. Prune results clamp scanned counts
to at least pruned counts and pruned counts to at least compacted reference
counts, so composite stores, admin views, CLI output, and API callers receive
internally consistent pruning summaries even when a backend reports partial,
negative, or sparse prune data.

Recent skill-management store-inspection normalization improvement:
`SkillStoreInspectionSupport` now centralizes inspection count clamping,
failure-text normalization, compact id/reference lists, null-child filtering,
string count maps, and lifecycle status-count copying. Definition, artifact,
lifecycle-state, and event-history inspection records now route through those
helpers, so direct inspectors, admin projections, CLI status, and API health
surfaces receive consistent item counts, child lists, capabilities defaults,
and failure text even when an underlying store reports sparse or malformed
inspection data.

Recent skill-management sync-result normalization improvement:
Definition and artifact sync changes/results now reuse `SkillManagementValueSupport`
for null-change filtering, detail text normalization, changed-count aggregation,
and action-count aggregation. Admin sync views continue to delegate count
behavior through the same neutral value boundary, keeping deployment, maintenance,
and operator sync projections stable even when store synchronizers return sparse
or partially malformed result entries.

Recent skill-management admin sync summary consolidation improvement:
Definition-sync and artifact-sync admin status DTOs now share
`SkillManagementAdminSyncSummary` for copied, updated, unchanged, conflict,
deleted, and changed-count derivation. Both sync-change DTO families implement a
small common view contract, so future sync surfaces can reuse the same admin
summary path without duplicating action-count logic.

Recent skill-management sync config parser consolidation improvement:
Definition-store and artifact-store sync config parsing now share
`SkillStoreSyncConfigSupport` and a neutral `SkillStoreSyncPolicy` while keeping
their public option records separate. Store-specific alias profiles preserve
definition aliases such as `update-existing`, `prune-orphans`, and `check`, and
artifact aliases such as `replace-existing` and `prune`, so future sync config
surfaces can extend parsing behavior from one shared policy boundary.

Recent skill-management sync loop consolidation improvement:
Definition-store and artifact-store synchronizers now share `SkillStoreSyncSupport`
for the source-to-target copy, unchanged, update, conflict, delete, and dry-run
control flow. Domain synchronizers retain only store enumeration, equivalence,
mutation, and change-record mapping, reducing drift risk as new persistence
surfaces or sync actions are added.

Recent skill-management lifecycle reconcile plan improvement:
Lifecycle reconciliation now separates mutation-free id diff planning from
state-store mutation through `SkillLifecycleStateReconcilePlan`. Reconcile
results and plans share compact sorted-distinct id normalization, so null,
blank, duplicate, or sparse lifecycle ids are filtered consistently before
maintenance, deployment, admin, CLI, and health projections consume the result.

Recent skill-management capability requirement enforcement improvement:
Event-store capability detection now lives on `SkillStoreCapabilities` and
respects effective pruning support instead of marker-interface presence alone.
Preflight validation uses `SkillStoreCapabilityRequirement` for event-pruning
requirements across configured event stores, custom event stores, and event-sink
overrides, keeping operator-facing capability errors consistent while rejecting
stores that implement pruning but explicitly report it disabled.

Recent skill-management hybrid read-repair improvement:
Hybrid definition, artifact, and lifecycle stores now repair missing primary
entries after a successful fallback read while preserving primary-only write
semantics. The shared `HybridSkillStoreSupport` helper makes repair opt-in and
non-disruptive: repair failures are swallowed so fallback reads still recover,
while mirrored stores keep their existing primary/fallback behavior.

Recent skill-management operation context improvement:
Maintenance and deployment event telemetry now carries a generated
`operationId`, with deployment-triggered maintenance events carrying a
`parentOperationId` that links them to the deployment event. The operation
context is additive to existing result and attribute projections, giving future
rollback hooks, trace views, and grouped operator logs a stable correlation
boundary without changing public result construction.

Recent skill-management artifact attribute projection improvement:
Artifact put/delete event metadata now routes through `SkillManagementEventAttributes`
instead of a local service-only map builder. This keeps artifact event shape
with the rest of the shared projection helpers while leaving service code focused
on orchestration and store error handling.

Recent skill-management lifecycle attribute projection improvement:
Lifecycle transition and reconcile event metadata now also routes through
`SkillManagementEventAttributes`. The service no longer assembles status,
revision, consistency, and reconcile-count maps inline, keeping lifecycle
observability aligned with artifact, sync, maintenance, deployment, and failure
projection ownership.

Recent skill-management bootstrap attribute projection improvement:
Bootstrap readiness event metadata now routes through `SkillManagementEventAttributes`
instead of being assembled directly in `SkillManagementBootstrapper`. Bootstrap
orchestration remains responsible for creating services and running inspection/
reconcile flows, while the shared projection helper owns the public event
attribute shape.

Recent skill-management event-recorder overload improvement:
`SkillManagementEventRecorder` now exposes no-attribute success/failure overloads
and a context-aware no-attribute failure overload. Bootstrap, service,
maintenance, and deployment orchestration no longer pass empty maps just to emit
plain events, keeping event call sites focused on meaningful attributes while
preserving the same recorded event shape.

Recent skill-management skill-revision attribute projection improvement:
Update-skill revision telemetry now routes through `SkillManagementEventAttributes`
instead of assembling a service-local `revision` map inline. A focused service
test pins the update event contract so future definition lifecycle work can reuse
the shared projection helper without silently changing revision metadata.

Recent skill-management artifact-delete attribute projection improvement:
Artifact delete success telemetry now uses a named `artifactDeleted(...)`
projection helper instead of passing ad hoc extra attributes from the service.
Direct `SkillManagementEventAttributes` tests now cover artifact delete metadata
and revision metadata immutability, giving the projection layer its own fast
contract checks before service/admin call paths consume the maps.

Recent skill-management service operation context improvement:
`SkillManagementEventRecorder` now supports context-aware success events, and
normal service/bootstrap operations create root operation contexts for emitted
success and failure telemetry. Create, update, delete, lifecycle transition,
lifecycle reconcile, artifact put/delete, artifact sync, and bootstrap events
now carry `operationId` attributes, while deployment-triggered maintenance keeps
its existing parent/child correlation model.

Recent skill-management event-attribute key vocabulary improvement:
`SkillManagementEventAttributeKeys` now owns the package-local vocabulary for
event attribute maps. Event writers, operation-context attributes, and deployment
history decoding now share the same constants for core telemetry, preflight,
error, artifact, lifecycle, and operation-correlation keys, reducing string drift
as new API, CLI, and admin projections are added.

Recent skill-management event-attribute reader improvement:
`SkillManagementEventAttributeReader` now wraps raw event attribute maps with
normalized boolean, count, text, and prefix accessors. Deployment-history
decoding uses the reader instead of calling value helpers against raw maps
directly, and focused tests cover malformed counts, missing maps, text values,
prefix detection, and immutable reader snapshots.

Recent skill-management artifact-sync runner improvement:
Direct artifact synchronization now runs through `SkillManagementArtifactSyncRunner`,
which owns sync orchestration and success/failure event recording. The public
`SkillManagementService` delegates to the runner, keeping artifact persistence
operations separate from facade wiring while preserving operation-context
telemetry for both successful and failed syncs.

Recent skill-management artifact-mutation runner improvement:
Artifact put/delete workflows now run through `SkillManagementArtifactMutationRunner`,
which owns artifact-store mutations, write-failure shaping, and success/failure
event recording. The public service facade delegates artifact mutations to this
runner, keeping direct artifact persistence concerns separate from service
query/list and maintenance orchestration.

Recent skill-management lifecycle-runner improvement:
Lifecycle state views, status transitions, and reconciliation now run through
`SkillManagementLifecycleRunner`. The public service facade delegates lifecycle
operations to the runner, keeping state persistence decisions and transition/
reconcile telemetry separate from skill definition CRUD and artifact workflows.

Recent skill-management definition-mutation runner improvement:
Skill definition create/update/delete workflows now run through
`SkillManagementDefinitionMutationRunner`, which owns lifecycle-state consistency,
rollback, write-failure shaping, and definition mutation telemetry. The public
service facade now validates definitions and delegates the mutation workflow,
leaving definition CRUD consistency separate from catalog reads and artifact
operations.

Recent skill-management catalog-reader improvement:
Skill definition reads, stable catalog sorting, lifecycle-aware active filtering,
and query/category search now run through `SkillManagementCatalogReader`. The
public service facade delegates catalog reads to the reader, separating read
projections from mutation runners, lifecycle workflow, artifact persistence, and
maintenance orchestration.

Recent skill-management service-runtime bundle improvement:
`SkillManagementServiceRuntime` now owns service component assembly for stores,
inspectors, event hooks, catalog reads, lifecycle workflows, definition
mutations, artifact workflows, and maintenance-runner construction. Public
`SkillManagementService` constructors keep their existing shape but now assign
from the assembled runtime graph instead of manually wiring every component.

Recent skill-management service async boundary consolidation:
`SkillManagementAsync` now owns Mutiny `Uni` item adaptation for synchronous
skill-management workflows. The public `SkillManagementService` facade keeps
its existing API shape while routing create/update/read/lifecycle/event/artifact
and maintenance calls through one async boundary, so future scheduling or
failure-policy changes have a single production seam.

Recent skill-management service default wiring consolidation:
`SkillManagementServiceDefaults` now owns the source-compatible constructor
defaults for registry-backed definitions, default inspectors, in-memory
lifecycle/artifact stores, noop event sinks, and readable event-sink adaptation.
`SkillManagementService` keeps its public constructors while delegating default
dependency choices to one package-local boundary.

Recent skill-management service-factory default wiring consolidation:
`SkillManagementServiceFactoryDefaults` now owns source-compatible factory
defaults for definition/lifecycle/event/artifact store factories, default
inspectors, and object-storage service wrapping. `SkillManagementServiceFactory`
keeps its constructor surface while routing default dependency choices through
one package-local factory boundary.

Recent skill-management service-factory component graph extraction:
`SkillManagementServiceFactoryComponents` now owns the internal factory runtime
graph for store bundles, preflight, service assembly, and maintenance/deployment
workflow execution. `SkillManagementServiceFactory` keeps the public facade API
while delegating internal construction and operation routing to one package-local
component boundary.

Recent skill-management config-resolution consolidation:
`SkillManagementConfigResolution` now owns top-level null-to-default policy for
service configs, maintenance source configs, maintenance plans, and deployment
configs. Bootstrap, deployment/preflight reports, store-bundle creation,
workflow execution, maintenance event attributes, and service/factory overloads
now share one package-local default-resolution boundary.

Recent skill-management runtime-dependency selection extraction:
`SkillManagementRuntimeDependencies` now owns runtime config fallback,
object-storage/JDBC optional dependency normalization, and runtime service-factory
selection. `SkillManagementServiceRuntimeFactory` remains the public runtime
entry point while delegating dependency-selection policy to one package-local
runtime boundary.

Recent skill-management service runtime facade consolidation:
`SkillManagementService` now retains a single `SkillManagementServiceRuntime`
graph instead of copying each assembled runtime component into parallel facade
fields. The public service API is unchanged, while operation methods delegate
through the runtime graph as the single component owner.

Recent skill-management grouped runtime graph consolidation:
`SkillManagementServiceRuntime` now retains grouped runtime sections for stores,
event hooks, inspection, and operations instead of flattening every subcomponent
into the top-level runtime record. Existing package-local accessors are preserved
while the runtime graph keeps clearer separation between store, event,
inspection, and operation concerns.

Recent skill-management service test fixture consolidation:
`TestSkillManagementServices` now owns standard service-test setup for registry
definition stores, default inspectors, in-memory lifecycle/artifact stores, and
event sinks. `SkillManagementServiceTest` uses the builder for repeated full
service-constructor setups while preserving simple constructor-path coverage.

Recent skill-management factory test fixture consolidation:
`TestSkillManagementFactories` now owns standard service-factory test setup for
registry-backed factories, optional object/JDBC dependencies, custom
definition/lifecycle/artifact/event stores, and event-sink overrides.
`SkillManagementServiceFactoryTest` uses the builder for repeated full factory
constructor setups while leaving direct constructor-path coverage intact.

Recent skill-management admin fixture consolidation:
`TestSkillManagementAdminFixtures` now owns stable admin projection inputs for
service placeholders, sync results, maintenance/deployment results,
reconciliation, inspections, and timestamped events. Admin mapper tests keep
their assertion-specific attributes inline while sharing repeated DTO/domain
setup for facade, deployment, event, trace, store, and deployment-history views.

Recent skill-management config-source parsing consolidation:
`SkillStoreConfigParsing` now owns the common properties/environment/map source
normalization path for definition, lifecycle, artifact, and event store
configuration parsers. Each store config family keeps only its store-specific
kind, default, child, JDBC, object-storage, and event-retention rules.

Recent skill-management composed-store parsing consolidation:
`SkillStoreConfigParsing` now also owns the primary/fallback child parsing flow
for hybrid and mirrored store configs. Definition, lifecycle, artifact, and
event store config parsers preserve their own error wording and final config
constructors while sharing the child-prefix validation and recursive parsing
mechanics.

Recent skill-management config validation rule consolidation:
`SkillStoreConfigValidation.Builder` now has conditional directory, text, and
primary/fallback requirements. Definition, lifecycle, artifact, and event store
config records use compact validation rule chains instead of repeating
kind-specific control flow, preserving existing error messages and ordering.

Recent skill-management factory composed-validation consolidation:
`SkillStoreFactorySupport.validatePrimaryFallback(...)` now owns null-safe
primary/fallback validation aggregation for composed stores. Definition,
lifecycle, artifact, and event store factories reuse it for hybrid/mirrored
runtime validation, including event prune-support checks, with focused support
tests covering ordering and missing-child behavior.

Recent skill-management factory composed-creation consolidation:
`SkillStoreFactorySupport.createPrimaryFallback(...)` now owns ordered
primary/fallback child creation for composed stores. Definition, lifecycle,
artifact, and event store factories keep their specific hybrid/mirrored
constructors while sharing the repeated child-store creation mechanics.

Recent skill-management factory composed-constructor consolidation:
`SkillStoreFactorySupport.createPrimaryFallback(...)` now also accepts a
composer function, so factories can create composed child stores and immediately
apply the concrete hybrid/mirrored constructor. Store factories keep constructor
references visible at the call site while sharing pair construction and
composition mechanics.

Recent skill-management definition-validator improvement:
Skill definition validation rules now live in `SkillDefinitionValidator`.
`SkillManagementService` keeps the public `validateSkill(...)` API but delegates
validation and mutation precondition checks to the validator supplied by the
service runtime graph, separating validation policy from the service facade.

Recent skill-management artifact-reader improvement:
Artifact get/list operations now run through `SkillManagementArtifactReader`.
The public service facade delegates artifact reads to the reader supplied by the
runtime graph, separating artifact read-side access from mutation, sync, and
maintenance workflows.

Recent skill-management deployment-history correlation projection improvement:
Deployment-history admin entries now expose normalized `operationId` and
`parentOperationId` fields decoded through `SkillManagementEventAttributeReader`.
Operator/API surfaces can correlate deployment events with nested maintenance
events without reaching back into raw attribute maps, while existing deployment
summary counts remain derived from the same entry list.

Recent skill-management admin-event correlation projection improvement:
Generic admin event entries now also expose normalized `operationId` and
`parentOperationId` fields while preserving the original immutable attributes
map. Event-page and deployment-history projections now share the same typed
attribute decoding path for operation correlation, giving API clients a stable
field contract without losing low-level telemetry details.

Recent skill-management event-attribute correlation reader improvement:
`SkillManagementEventAttributeReader` now exposes operation-correlation
accessors for `operationId` and `parentOperationId`. Admin event and deployment
history projections no longer reach into raw attribute-key constants directly,
keeping the event writer and typed reader as the two clear boundaries around
correlation metadata.

Recent skill-management event-correlation query improvement:
`SkillManagementEventQuery` now supports exact `operationId` and
`parentOperationId` filters with named factories, while preserving the existing
operation, skill, success, and limit query shape. Event history readers can now
fetch a deployment root event or its child maintenance events through the same
page assembly path used by memory, file, JDBC, and object-backed stores.

Recent skill-management operation-trace projection improvement:
`SkillManagementAdminOperationTrace` now provides an admin-facing root/child
event projection for one `operationId`. `SkillManagementAdminViews` can build a
trace from an event page, selecting the matching root event and child events
whose `parentOperationId` references it, with derived success, failure, and
child-event counts for operator diagnostics.

Recent skill-management event-history convenience API improvement:
`SkillManagementService` now exposes `eventHistoryForOperation(...)` and
`eventHistoryForParentOperation(...)` helpers. Callers can fetch a root
operation event or its child events without manually constructing correlation
queries, while the actual filtering rules remain centralized in
`SkillManagementEventQuery`.

Recent skill-management operation-trace page assembly improvement:
`SkillManagementAdminViews.operationTrace(...)` now accepts separate root and
child event pages in addition to the existing combined-page form. This matches
the service-level correlation helpers directly: callers can query the root
operation page and child-operation page independently, then assemble one stable
admin trace without merging raw event lists themselves.

Recent skill-management operation-trace reader improvement:
`SkillManagementOperationTraceReader` now owns the root/child event-history
queries needed to build an admin operation trace. The service runtime wires this
reader beside event history, and the public service facade exposes a thin
`operationTrace(...)` diagnostic helper while keeping correlation filtering
centralized in `SkillManagementEventQuery`.

Recent skill-management operation-trace summary improvement:
`SkillManagementAdminOperationTrace` now includes a derived
`SkillManagementAdminEventSummary` for the normalized root and child events in
the trace. Operators can inspect total, success/failure, operation, and skill
breakdowns without recomputing counts from the raw event list.

Recent skill-management operation-trace health improvement:
`SkillManagementAdminOperationTrace` now exposes derived `healthy`, `failed`,
and `failedChildEvents` fields. Admin/API callers can classify a trace directly
without recomputing health from root availability, failure counts, and child
event status.

Recent skill-management operation-trace status improvement:
`SkillManagementAdminOperationTrace` now exposes a stable `status` value
(`MISSING_OPERATION_ID`, `ROOT_MISSING`, `FAILED`, `HEALTHY`) derived from the
normalized operation id, root event availability, and failure state. API/CLI
callers can branch on one status field instead of duplicating trace health
precedence rules.

Recent skill-management typed trace-status improvement:
`SkillManagementOperationTraceStatus` now owns operation-trace status precedence
rules while `SkillManagementAdminOperationTrace.status()` continues exposing the
stable string value for admin/API consumers. Trace projection and reader tests
now reference the typed status source instead of duplicating literals.

Recent skill-management deployment trace-page improvement:
`SkillManagementOperationTraceReader` can now assemble a bounded page of recent
deployment operation traces, and the service facade exposes it through
`deploymentOperationTraces(...)`. `SkillManagementAdminOperationTracePage`
summarizes matched root events, returned traces, truncation, child-event limit,
and trace status counts for admin/API diagnostics.

Recent skill-management operation-trace query improvement:
`SkillManagementOperationTraceQuery` now centralizes deployment trace-page
options, including operation limit, child-event limit, and optional status
filtering. `deploymentOperationTraces(...)` keeps its simple limit overload and
also accepts the query object for filtered admin/API diagnostics such as
failed-only trace pages.

Recent skill-management operation-trace status parsing improvement:
`SkillManagementOperationTraceStatus` now parses API-friendly status labels such
as `failed`, `root-missing`, and `missing operation id`. The trace query keeps
blank status labels as no filter and rejects unknown labels, while the service
facade exposes a string-status overload for REST/CLI integration.

Recent skill-management operation-trace page metadata improvement:
`SkillManagementAdminOperationTracePage` now separates matched root events,
returned root events, distinct traceable roots, untraceable roots without an
operation id, filtered traces, and returned traces. Deployment trace diagnostics
can now distinguish paging truncation, skipped roots, and status filtering
instead of inferring all three from a single returned count.

Recent skill-management operation-trace mapper split improvement:
Operation-trace projection now lives in `SkillManagementAdminOperationTraceViews`
instead of the broad `SkillManagementAdminViews` facade. Existing
`SkillManagementAdminViews.operationTrace(...)` methods remain as compatibility
delegates, while `SkillManagementOperationTraceReader` depends on the focused
mapper directly.

Recent skill-management event mapper split improvement:
Event-history admin projection now lives in `SkillManagementAdminEventViews`,
covering event pages, individual events, and event summaries. The broad
`SkillManagementAdminViews` facade remains source-compatible through delegates,
and operation-trace projection now depends on the focused event mapper instead
of the broad facade.

Recent skill-management deployment-history mapper split improvement:
Deployment-history admin projection now lives in
`SkillManagementAdminDeploymentHistoryViews`, covering deployment history pages
and deployment history entries. The broad `SkillManagementAdminViews` facade
remains source-compatible through delegates, while mixed event-page filtering is
covered directly on the focused mapper.

Recent skill-management sync mapper split improvement:
Definition and artifact synchronization admin projection now lives in
`SkillManagementAdminSyncViews`. The broad `SkillManagementAdminViews` facade
keeps its existing sync methods as delegates, and raw sync-result mapping is now
covered directly on the focused mapper.

Recent skill-management store mapper split improvement:
Definition, lifecycle, event, and artifact store inspection projection now lives
in `SkillManagementAdminStoreViews`, including child-store recursion,
lifecycle-status count conversion, event-summary projection, and capability
labels. The broad `SkillManagementAdminViews` facade keeps its existing store
methods as delegates.

Recent skill-management event-prune mapper split improvement:
Event-history prune projection now lives in `SkillManagementAdminEventPruneViews`,
including recursive child prune reports. The broad `SkillManagementAdminViews`
facade keeps its existing `eventPrune(...)` method as a delegate, and composite
prune mapping is covered directly on the focused mapper.

Recent skill-management inspection mapper split improvement:
Aggregate inspection, bootstrap, and lifecycle reconciliation projection now
live in `SkillManagementAdminInspectionViews`. The broad
`SkillManagementAdminViews` facade keeps its existing inspection/bootstrap/
reconcile methods as delegates, while aggregate inspection and failure-state
reconciliation mapping are covered directly on the focused mapper.

Recent skill-management deployment-preflight mapper split improvement:
Deployment preflight and validation-bucket projection now live in
`SkillManagementAdminDeploymentViews`. The broad `SkillManagementAdminViews`
facade keeps its existing `deploymentPreflight(...)` and `validation(...)`
methods as delegates, while multi-bucket preflight and null validation mapping
are covered directly on the focused mapper.

Recent skill-management deployment-report mapper completion:
Maintenance and deployment admin report projection now also lives in
`SkillManagementAdminDeploymentViews`. The broad `SkillManagementAdminViews`
facade keeps its existing `maintenance(...)` and `deployment(...)` methods as
delegates, so all deployment-family DTO assembly is behind one focused mapper.

Recent skill-management maintenance-workflow improvement:
Ad hoc maintenance runs now route through `SkillManagementMaintenanceWorkflow`,
which owns source-store validation, managed target-store wiring, event-pruner
selection, and runner creation. The public service facade now delegates both
definition-only and artifact-aware maintenance paths through the runtime graph,
keeping manual maintenance orchestration separate from API-level `Uni` wrapping.

Recent skill-management inspection-reader improvement:
Operational inspection reads now route through `SkillManagementInspectionReader`,
which owns the managed definition, lifecycle, artifact, event, and composite
inspection wiring. `SkillManagementService` delegates store and management
inspection methods through this runtime component, keeping individual inspector
labels and composite-read assembly out of the public facade.

Recent skill-management event-history boundary improvement:
Event-history reads and pruning now route through `SkillManagementEventHistory`,
which owns the query reader, prune-capability reporting, and event-prune
delegation. The public service facade no longer carries raw event reader/pruner
hooks for history APIs, while the runtime graph still composes event history,
inspection, and maintenance from the same resolved event-store capabilities.

Recent skill-management artifact-sync workflow improvement:
Direct artifact synchronization now routes through
`SkillManagementArtifactSyncWorkflow`, which binds caller-provided source
artifacts to the managed target artifact store before delegating execution and
event recording to `SkillManagementArtifactSyncRunner`. The public service
facade no longer carries target artifact-store wiring for sync operations.

Recent skill-management lifecycle snapshot delegation improvement:
Lifecycle snapshots now route through `SkillManagementLifecycleRunner`, keeping
view-state reads, persisted-state snapshots, transitions, and reconciliation
behind the lifecycle boundary. `SkillManagementService` no longer retains raw
definition or lifecycle stores after assembling the runtime graph.

Recent skill-management runtime graph decomposition:
`SkillManagementServiceRuntime` now assembles the service facade through
package-local runtime graph records for stores, event hooks, inspection, and
operations. This keeps store normalization, event history/trace wiring,
inspection readers, mutation runners, artifact workflows, and maintenance
workflow creation separated while preserving the existing public service API.

Recent skill-management test fixture consolidation:
Package-local `TestSkillDefinitionStore` and `TestSkillRegistry` fixtures now
replace duplicated in-memory test implementations across focused runtime,
reader, runner, workflow, bootstrap, and runtime-factory tests. This keeps
component tests smaller while preserving targeted failure fixtures where a test
needs store-specific behavior.

Recent skill-management registry fixture consolidation:
The large service and service-factory test suites now also use the shared
`TestSkillRegistry` fixture instead of carrying their own copied
`SimpleSkillRegistry` implementations. Registry-backed tests now share one
package-local behavior model while suite-specific failure stores remain local.

Recent skill-management skill-definition fixture consolidation:
`TestSkillDefinitions` now owns the common valid skill-definition shapes used by
focused skill-management component tests. Runtime, catalog, mutation, lifecycle,
inspection, maintenance, workflow, bootstrap, and runtime-factory tests reuse the
same base skill builder while keeping specialized service-update definitions
local to the assertions that need them.

Recent skill-management fixture coverage expansion:
Store inspector, lifecycle inspector, hybrid store, synchronizer, maintenance
runner, management inspector, lifecycle reconciler, and service-factory tests now
also delegate valid skill construction through `TestSkillDefinitions`. The
fixture includes metadata and custom-prompt variants so store and sync tests can
preserve their asserted payloads without copying `SkillDefinition` builder setup.

Recent skill-management definition-store fixture expansion:
Plain in-memory definition store copies in store inspector, lifecycle inspector,
hybrid store, synchronizer, management inspector, lifecycle reconciler,
maintenance runner, store-bundle factory, and event-pruner tests now use the
shared `TestSkillDefinitionStore`. Tests that need unavailable read/write
behavior keep only the failure-specific overrides on top of the shared fixture.

Recent skill-management definition-store fixture completion:
Admin projection and persistence-strategy contract tests now also use the shared
`TestSkillDefinitionStore`, removing the remaining local
`InMemorySkillDefinitionStore` copies from skill-management tests. Persistence
tests keep their richer skill-definition payload builder because it exercises
codec and store compatibility fields directly.

Recent skill-management direct-builder consolidation:
Service and validator tests now route their valid skill-definition construction
through `TestSkillDefinitions.builder(...)`, leaving only intentionally invalid
records and the persistence codec compatibility payload as local construction.
This keeps ordinary valid-skill shape changes centralized while preserving
high-signal test-local fixtures for special cases.

Recent skill-management deployment-admin test consolidation:
Deployment-family admin mapper coverage now lives in
`SkillManagementAdminDeploymentViewsTest`, including maintenance/deployment
report normalization, preflight buckets, and validation DTO invariants. The
broader `SkillManagementAdminViewsTest` no longer duplicates those deployment
cases, keeping facade tests from becoming the projection catch-all again.

Recent skill-management event-prune admin test consolidation:
Event-prune admin projection coverage now lives in
`SkillManagementAdminEventPruneViewsTest`, including direct prune-result
mapping, DTO normalization, and composite child-failure projection. The broader
`SkillManagementAdminViewsTest` no longer duplicates prune-specific mapper
assertions.

Recent skill-management admin-facade hardening improvement:
`SkillManagementAdminViews` is now documented and tested as a
source-compatible facade over the focused admin mapper classes. Representative
facade tests compare public entry points with their focused mapper equivalents,
so future admin projection logic stays behind the smaller mapper boundaries
instead of drifting back into the facade.

Recent skill-management operation-trace admin test consolidation:
Operation-trace admin projection coverage now lives in
`SkillManagementAdminOperationTraceViewsTest`, covering same-page and split
root/child event-page mapping directly against the focused mapper. The broad
admin views test no longer carries operation-trace projection assertions.

Recent skill-management event admin test consolidation:
Event-page admin projection coverage now lives only in
`SkillManagementAdminEventViewsTest`, including normalized correlation IDs and
raw attribute preservation. The broad admin views test no longer duplicates the
focused event mapper assertions.

Recent skill-management deployment-history admin test consolidation:
Deployment-history projection coverage now lives in
`SkillManagementAdminDeploymentHistoryViewsTest`, including mixed event-page
filtering, deployment entry normalization, preflight summary counts, and page
summary derivation. The broad admin views test no longer carries deployment
history projection assertions.

Recent skill-management inspection admin test consolidation:
Aggregate inspection, bootstrap, reconciliation, and bootstrap-report admin
projection coverage now lives in `SkillManagementAdminInspectionViewsTest`.
The broad admin views test no longer carries inspection/bootstrap mapper
assertions, keeping those focused on the inspection projection boundary.

Recent skill-management store admin test consolidation:
Hybrid lifecycle/artifact store recursion and store-status normalization
coverage now lives in `SkillManagementAdminStoreViewsTest`. The broad admin
views test no longer carries store projection assertions.

Recent skill-management admin-facade test completion:
Sync DTO summary invariants now live in `SkillManagementAdminSyncViewsTest`,
shared normalization checks live in `SkillManagementAdminValueSupportTest`, and
`SkillManagementAdminViewsFacadeTest` verifies facade delegation across the
focused mapper families. This keeps the public compatibility contract separate
from projection and value-object behavior.

Recent skill-management admin-views test retirement:
The remaining duplicate sync DTO and admin value-support assertions already
live in `SkillManagementAdminSyncViewsTest` and
`SkillManagementAdminValueSupportTest`, so the old broad
`SkillManagementAdminViewsTest` has been removed. Facade compatibility remains
covered by `SkillManagementAdminViewsFacadeTest`.

Recent skill-management admin DTO contract snapshots:
`SkillManagementAdminDtoContractTest` now snapshots the record-component shape
of the operator-facing admin DTOs for inspection, store status, deployment
preflight, deployment history, operation traces, and event pages. This gives API
and CLI consumers an early warning when public field names or types drift.

Recent skill-management package boundary documentation:
The skill-management package now has a package-level architecture note covering
the public service facades, runtime assembly records, runners/readers/workflows,
persistence store boundaries, event-history observability, and focused admin
projection mappers. New behavior has an explicit home before it drifts back into
facade or catch-all classes.

Recent skill-management query-limit consolidation:
`SkillManagementQueryLimits` now owns the shared bounded-window default and max
limit policy for event queries, artifact queries, operation trace queries, and
operation trace pages. Existing public query constants remain aligned with that
shared package-local policy.

Recent skill-management event-query contract coverage:
`SkillManagementEventQueryTest` now directly covers filter normalization,
bounded limit normalization, operation/skill/success matching, correlation-id
matching, and null-event rejection. The service test no longer carries a stray
query-limit assertion.

Recent skill-management operation-trace query contract coverage:
`SkillManagementOperationTraceQueryTest` now directly covers status-filter
matching semantics in addition to limit normalization and API-friendly status
parsing. All-status queries accept any non-null trace, filtered queries accept
only matching trace statuses, and null traces are rejected.

Recent skill-management artifact-query contract coverage:
`SkillArtifactQueryTest` now directly covers the default all-artifacts query
shape, shared limit normalization, null-reference rejection, and optional
kind/name/version filter matching. Artifact listing behavior is now guarded
beside event and operation-trace query contracts instead of relying on service
tests to catch query drift.

Recent Agentic Commerce persistence target descriptor improvement:
`AgenticCommerceWayangPersistenceTargetDescriptor` now gives file, in-memory,
object-store, database, and hybrid persistence a shared target identity map.
Persistence config and concrete store status maps include this descriptor, so
S3/RustFS/JDBC/Postgres/hybrid deployments can expose provider, location,
durability, cloud, database, and nested fallback details without special-case
diagnostics.

Recent Agentic Commerce persistence transfer target reporting improvement:
Persistence transfer reports and dry-run plans now expose source and target
persistence descriptors as first-class maps, alongside the full store status
payloads. Operators can compare file, object-store, database, and hybrid
locations before and after a migration without digging through nested status
maps, while status-failure paths still fall back to storage-kind descriptors.

Recent Agentic Commerce config snapshot target reporting improvement:
Config snapshots, runtime preflight maps, persistence service status maps, and
reload reports now expose normalized persistence-target descriptors. Reload
reports also flag `persistenceTargetChanged` separately from runtime/bootstrap
config changes, so moving state between file, S3/RustFS, database, or hybrid
stores does not get confused with effective runtime configuration drift.

Recent Agentic Commerce persistence health target reporting improvement:
Persistence health reports now expose `persistenceTarget` directly, and health
summaries carry the same target descriptor inside their attributes. Operators
can identify the affected file, in-memory, object-store, database, hybrid, or
fallback target from the health payload without digging through raw store
status, including status-read failure cases.

Recent Agentic Commerce persistence contract target reporting improvement:
Persistence contract reports now expose before/after persistence-target
descriptors in addition to raw store status maps. Contract harness output can
pin round-trip successes or adapter failures to the exact file, object-store,
database, hybrid, or fallback target while keeping secrets out of summaries.

Recent Agentic Commerce persistence target comparison improvement:
`AgenticCommerceWayangPersistenceTargetComparison` now centralizes backend
target drift detection, including storage kind, target kind, provider,
location, durability, cloud, database, and hybrid changes. Config reload
reports, transfer reports, and transfer plans expose these comparison maps so
operators can distinguish runtime config drift from persistence backend moves
without reimplementing map comparisons.

Recent Agentic Commerce persistence config validation improvement:
Persistence configs now expose validation reports with structured error and
warning findings. Validation checks provider-registry support, custom storage
kinds, ephemeral in-memory usage, object-store/database provider ambiguity,
RustFS endpoint gaps, and hybrid fallback risks such as unmirrored writes,
same-target fallback, and ephemeral primary/fallback stores.

Recent Agentic Commerce persistence transfer document status improvement:
Persistence transfer reports and dry-run plans now expose a structured
per-document status table for runtime config, bootstrap config, bootstrap
reports, and manifests. Each entry includes action, copied/skipped/blocked,
copyable, dry-run/planning, failure, and document-scoped issue fields, so CLI
and API consumers can explain migration outcomes without reconstructing them
from aggregate counters and issue strings.

Recent Agentic Commerce persistence transfer summary improvement:
Persistence transfer reports and plans now share
`AgenticCommerceWayangPersistenceTransferSummary`, a compact operator-facing
outcome boundary over the detailed document table. The summary exposes stable
statuses such as complete, preview, partial, skipped, failed, mutation flags,
target-change flags, document-id buckets, and attention reasons for blocked,
skipped, dry-run, planning, and failed transfer paths.

Recent Agentic Commerce persistence transfer finding improvement:
Persistence transfer reports and plans now expose structured transfer findings
beside raw issue strings. `AgenticCommerceWayangPersistenceTransferFindings`
catalogs source/target status failures, document load/save/verify failures,
no-overwrite blocks, skipped source documents, dry-run previews, and planning
previews with severity, source, title, remediation, blocking, document scope,
and attributes for API and CLI renderers.

Recent Agentic Commerce persistence transfer finding index improvement:
`AgenticCommerceWayangPersistenceTransferFindingIndex` now gives transfer
reports and plans a lookup-oriented view over structured findings. Operators
and API consumers can inspect findings by code, severity, source, and document,
with blocking/document-scoped counts and serialized index buckets available
beside the flat finding list.

Recent Agentic Commerce persistence transfer preflight improvement:
`AgenticCommerceWayangPersistenceTransferPreflightReport` now combines source
health, target health, and mutation-free transfer planning into one readiness
payload. `AgenticCommerceWayangPersistenceService.preflightTransferTo(...)`
classifies ready, blocked, source-incomplete, source/target-unavailable,
plan-failed, and no-op transfer states while preserving the underlying health
summaries, plan summary, finding index, target comparison, and attention
reasons without mutating the target store.

Recent Agentic Commerce persistence transfer preflight check improvement:
Transfer preflight reports now expose stage-level checks for source health,
target health, and transfer plan readiness. Each check carries a stable id,
stage, status, pass/ready/blocking flags, issue/warning/finding counts,
attention reasons, and scoped attributes, with aggregate check counts and
`checksById` included in the serialized preflight payload.

Recent A2UI HTTP mount improvement:
`WayangA2uiHttpRouteCatalog` can now mount the default A2UI route surface at a
runtime-specific root such as `/api/a2ui` while preserving route operations,
allowed methods, content metadata, binding reports, smoke probes, and readiness
checks. This gives Quarkus, servlet, CLI, and other concrete adapters a shared
path-rewriting boundary instead of duplicating framework-local route catalogs.

Recent A2UI HTTP endpoint binding improvement:
`WayangA2uiHttpEndpointBinding` now gives concrete HTTP frameworks a small
dependency-free entry point over the mounted bridge adapter. Framework wrappers
can pass raw method/path/body/header/attribute values, while the binding strips
query strings and fragments, normalizes multi-value headers, exposes mounted
route lookup, and delegates to the canonical A2UI bridge response path.

Recent A2UI HTTP route binding improvement:
`WayangA2uiHttpRouteBinding` now exposes one mounted A2UI HTTP route as a
framework-neutral registration handle. Concrete adapters can enumerate
`WayangA2uiHttpEndpointBinding.bindings()` to register operation, path, method,
media type, request-body, allowed-method, and dispatch metadata route by route,
with mismatch protection before delegating to the shared endpoint bridge.

Recent A2UI HTTP endpoint publication improvement:
`WayangA2uiHttpEndpointPublication` now gives mounted A2UI endpoint bindings a
data-only registration manifest for CLI/API diagnostics and framework route
publication. The manifest reports route count, operation order, mounted paths,
per-route metadata, operation/path lookup, and JSON-ready maps without retaining
framework or endpoint objects.

Recent A2UI HTTP endpoint response projection improvement:
`WayangA2uiHttpEndpointResponse` now converts dependency-free A2UI HTTP
responses into framework-friendly status/body/content-type/header-list
projections. Endpoint bindings expose `respond(...)` helpers so concrete
framework adapters can pass raw HTTP request values in and receive
`Map<String, List<String>>` response headers out without duplicating projection
logic.

Recent A2UI HTTP endpoint request projection improvement:
`WayangA2uiHttpEndpointRequest` now gives concrete HTTP frameworks a normalized
request diagnostics surface before dispatch. Endpoint bindings expose
`project(...)` helpers that strip query strings/fragments, normalize headers and
attributes, report known-path versus matched-route state, and include mounted
route operation/`Allow` metadata for framework logs, rejection paths, and tests.

Recent A2UI HTTP endpoint exchange projection improvement:
`WayangA2uiHttpEndpointExchange` now packages request and response projections
for one raw mounted HTTP call. Endpoint bindings expose `exchange(...)` helpers
that report known-path and matched-route state, status, success, transport
outcome, transport-error state, the normalized request, projected response, and
decoded response envelope for framework diagnostics and harness assertions.

Recent A2UI HTTP endpoint diagnostics improvement:
`WayangA2uiHttpEndpointDiagnostics` now runs batch diagnostics over raw mounted
endpoint calls using `WayangA2uiHttpEndpointDiagnosticRequest` inputs and
`WayangA2uiHttpEndpointExchange` captures. The result/report layer summarizes
known paths, matched routes, status buckets, transport outcomes, handled and
rejected counts, decoded response envelopes, and compact issues; `runDefault()`
probes route catalog, binding report, smoke/readiness, and OPTIONS for each
published route.

Recent A2UI HTTP endpoint diagnostics config improvement:
`WayangA2uiHttpEndpointDiagnosticConfig` now makes endpoint diagnostics runtime
configurable. Framework adapters can preserve the default full probe set or
disable smoke/readiness and route OPTIONS probes, keep discovery-only checks,
and apply shared default headers and attributes to every raw diagnostic request
without changing endpoint binding or report code.

Recent A2UI HTTP endpoint diagnostics config parsing improvement:
Endpoint diagnostic config can now be built from property-style maps via
`WayangA2uiHttpEndpointDiagnosticConfig.fromMap(...)`. Runtime binders can pass
`profile`, nested `probes`, header maps, and attribute maps, with top-level
probe keys overriding nested values, giving Quarkus, servlet, CLI, and test
harness adapters one shared parser for endpoint diagnostics behavior.

Recent A2UI HTTP endpoint diagnostic request parsing improvement:
`WayangA2uiHttpEndpointDiagnosticRequest` now supports `fromMap(...)`,
`fromJson(...)`, and `toJson()` for raw mounted endpoint diagnostics. Request
maps can provide `method`, `rawPath` or `path`, `body`, `headers`, and
`attributes`, and `WayangA2uiHttpEndpointDiagnostics.runFromMaps(...)` executes
map lists through the same default-header/default-attribute and exchange report
pipeline.

Recent A2UI HTTP endpoint diagnostic plan improvement:
`WayangA2uiHttpEndpointDiagnosticPlan` now packages diagnostics id, config,
request definitions, and report attributes into one JSON-ready payload for
external harnesses. Endpoint diagnostics can run plan records, plan maps, or
plan JSON; plans without explicit requests use the configured default probes
while preserving plan attributes and per-run diagnostic config.

Recent Agentic Commerce persistence transfer recommendation improvement:
Transfer preflight reports now derive operator recommendations from readiness
status, stage checks, health findings, and transfer plan details. The
recommendation queue exposes actions such as apply transfer, complete source
documents, fix source health, inspect target health, review plan issues, enable
overwrite or clear blocked target documents, review warnings, and no-op
transfer, with priority, blocking flags, check ids, attributes, and serialized
action counts.

Recent Agentic Commerce persistence guarded apply improvement:
`AgenticCommerceWayangPersistenceTransferApplyReport` now wraps transfer apply
attempts with preflight gating. `AgenticCommerceWayangPersistenceService`
exposes guarded `applyTransferTo(...)` methods that mutate the target only when
preflight is ready, or when explicitly forced, while preserving preflight
recommendations, transfer reports, apply status, mutation flags, force flags,
and blocked-by-preflight diagnostics in one payload.

Recent Agentic Commerce persistence transfer audit event improvement:
`AgenticCommerceWayangPersistenceTransferAuditEvent` now projects transfer
preflight, direct copy, and guarded apply reports into a compact event
envelope. The event captures stable type/status fields, success, dry-run,
forced, mutation, blocked flags, document counters, recommendations, attention
reasons, and focused attributes so telemetry, audit logs, CLI summaries, and
API consumers can observe persistence migrations without re-walking nested
health, plan, transfer, and apply payloads.

Recent Agentic Commerce persistence transfer audit trail improvement:
`AgenticCommerceWayangPersistenceTransferAuditTrail` now assembles ordered
audit-event timelines for preflight, direct copy, and guarded apply operations.
Apply trails include the guard preflight event, optional copy event, and final
apply event, while deriving final status, mutation/block/force flags, document
counters, recommendation actions, attention reasons, and serialized latest
event details from the ordered event list.

Recent Agentic Commerce persistence transfer audit trail index improvement:
`AgenticCommerceWayangPersistenceTransferAuditTrailIndex` now gives transfer
audit trails a lookup-oriented event view. Consumers can query events by type
or status, detect successful, blocked, forced, dry-run, and mutated-target
events, and read serialized type/status buckets without rescanning the raw
event list.

Recent Agentic Commerce persistence transfer audit summary improvement:
`AgenticCommerceWayangPersistenceTransferAuditSummary` now condenses audit
trails into an operator-facing outcome vocabulary: ready, complete, preview,
attention, forced, blocked, failed, and no-op. The summary preserves final
trail status while exposing attention/action flags, lifecycle counters,
document counters, event type/status lists, recommendations, attention reasons,
and latest-event attributes for CLI, API, telemetry, and audit sinks.

Recent Agentic Commerce persistence transfer audit decision improvement:
`AgenticCommerceWayangPersistenceTransferAuditDecision` now derives a compact
automation-facing next action from audit summaries. Decisions classify outcomes
into apply, force, retry, inspect, or stop actions, with decision status,
terminal/mutation/force/retry/inspection/approval flags, reasons,
recommendation actions, and document counters for orchestration, runbooks, and
operator UIs.

Recent Agentic Commerce persistence transfer audit sink improvement:
`AgenticCommerceWayangPersistenceTransferAuditSink` now provides a small
write-only boundary for transfer audit trails, with noop, composite, and
bounded in-memory implementations. `AgenticCommerceWayangPersistenceService`
offers sink-aware preflight, transfer, and guarded-apply overloads so logs,
metrics, telemetry, stores, and tests can capture one trail per requested
operation without coupling transfer code to a concrete backend.

Recent Agentic Commerce persistence transfer audit reader improvement:
`AgenticCommerceWayangPersistenceTransferAuditReader`,
`AgenticCommerceWayangPersistenceTransferAuditQuery`, and
`AgenticCommerceWayangPersistenceTransferAuditPage` now make recorded transfer
audit trails queryable by trail type, outcome, decision action, decision
status, attention flag, and bounded latest result. The in-memory sink implements
the reader directly, and composite sinks delegate reads to the first readable
child while preserving best-effort fan-out for writes.

Recent Agentic Commerce persistence transfer audit file-store improvement:
`FileSystemAgenticCommerceWayangPersistenceTransferAuditStore` now records
transfer audit trails to a retained local JSONL journal while keeping a
bounded in-memory reader mirror for live diagnostics. This gives persistence
transfer operations restart-safe file evidence and queryable in-process history
without coupling audit producers to database, object-store, or filesystem
details.

Recent Agentic Commerce persistence transfer audit configuration improvement:
`AgenticCommerceWayangPersistenceTransferAuditConfig` and
`AgenticCommerceWayangPersistenceTransferAuditStoreProviders` now make transfer
audit storage configurable through noop, in-memory, file, object-store, and composite
providers. The provider registry mirrors the existing persistence-store factory
pattern, resolves S3/RustFS-compatible audit clients and database audit clients,
and leaves a clean extension point for hosted audit sinks.

Recent Agentic Commerce persistence transfer audit reload improvement:
filesystem transfer audit stores now reload existing JSONL journals into their
bounded reader mirror on construction and prune retained journal lines to the
configured trail capacity. Audit events and trails also support map-based
rehydration, giving file, object-store, and database audit
backends one shared serialization contract.

Recent Agentic Commerce persistence transfer audit object-store improvement:
`ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore` now persists
retained transfer audit JSONL journals through the existing
`AgenticCommerceObjectStoreClient` boundary, enabling S3/RustFS-compatible
audit storage with restart reload and bounded in-process querying. Audit config
now carries object-store settings, object keys, and client resolver overloads so
cloud audit backends can participate in the same provider registry as memory,
file, noop, and composite sinks.

Recent Agentic Commerce persistence transfer audit database improvement:
`DatabaseAgenticCommerceWayangPersistenceTransferAuditStore` now persists
retained transfer audit JSONL journals through the existing
`AgenticCommerceDatabasePersistenceClient` boundary, enabling JDBC/Postgres-style
audit storage with restart reload and bounded in-process querying. Audit config
now carries database table/namespace settings, audit document names, and database
client resolver overloads so database-backed audit history participates in the
same provider registry as memory, file, object-store, noop, and composite sinks.

Recent Agentic Commerce persistence transfer audit contract-harness improvement:
`AgenticCommerceWayangPersistenceTransferAuditContractHarness` and
`AgenticCommerceWayangPersistenceTransferAuditContractReport` now give transfer
audit stores a reusable retained-history contract. The harness records a
synthetic preflight/copy/apply trail set, verifies bounded latest history,
type/outcome/action queries, null no-op writes, optional reload behavior, and
failure diagnostics so in-memory, filesystem, object-store, and database audit
backends stay behaviorally aligned as new storage providers are added.

Recent Agentic Commerce persistence transfer audit provider-contract improvement:
`AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness` now runs
the retained-history contract through audit configs, provider registries, and
object-store/database client resolvers. Configured memory, file, durable-first
composite, S3/RustFS-compatible object-store, JDBC/Postgres-style database, and
custom future providers can now be checked through the same provider boundary
operators will configure in production.

Recent Agentic Commerce persistence transfer audit diagnostics improvement:
`AgenticCommerceWayangPersistenceTransferAuditDiagnostics` now exposes a compact
operator-facing health surface for configured audit storage. Diagnostics can run
the provider contract, classify audit status as healthy, degraded, disabled, or
unavailable, summarize storage targets for file, memory, composite,
S3/RustFS-compatible object-store, and JDBC/Postgres-style database backends,
and report provider/contract issues without leaking raw audit trails.

Recent Agentic Commerce persistence service audit diagnostics integration:
`AgenticCommerceWayangPersistenceService` now exposes transfer audit diagnostics
beside persistence health and connector diagnostics. The service status map
includes non-mutating default audit diagnostics for CLI/API status calls, while
explicit service methods can run contract-backed diagnostics for configured
file, memory, object-store, database, and composite audit backends using the
same object-store/database resolver hooks as the provider registry.

Recent Agentic Commerce persistence transfer audit recommendation improvement:
`AgenticCommerceWayangPersistenceTransferAuditRecommendation` now turns audit
diagnostics into operator actions. Transfer audit diagnostics report
recommendation counts, blocking recommendation counts, action IDs, and
JSON-ready recommendation details for disabled audit storage, ephemeral
in-memory storage, missing contract checks, durable backends without reload
verification, provider construction failures, and retained-history contract
failures.

Recent Agentic Commerce persistence transfer audit config validation improvement:
`AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport` now gives
transfer audit storage the same preflight-style validation surface as state
persistence config. Audit configs report unsupported storage providers, retained
history capacity below the contract floor, disabled/ephemeral storage warnings,
file journal path/name issues, S3/RustFS object-store provider warnings,
JDBC/Postgres database provider warnings, and composite child durability risks.
Audit config maps and diagnostics now include validation output, and validation
errors produce a blocking `fix_audit_config` recommendation before contract
execution is attempted.

Recent Agentic Commerce persistence transfer audit retention-policy improvement:
`AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy` now centralizes
durable audit journal retention rules. File, object-store, and database audit
stores receive the resolved policy from config/providers, preserve current
`maxTrails` behavior, support optional byte caps that still retain the newest
audit evidence, and reload their query mirrors from the actually retained JSONL
lines. Audit config maps and validation targets now expose the resolved
retention policy so future archive/window policies can land without rewriting
each storage backend.

Recent Agentic Commerce persistence transfer audit retention validation
improvement:
`AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment` now
checks configured retention policies against the audit contract's canonical
JSONL sample lines. Validation reports include the retained-history assessment
and block byte caps that cannot retain the expected latest audit trails, with a
recommended minimum byte floor for operators before contract diagnostics run.

Recent Agentic Commerce persistence transfer audit byte-size config improvement:
`AgenticCommerceWayangByteSizes` now parses deployable byte-size values such as
`64kb`, `64 KiB`, `1.5MiB`, and `unlimited`. Transfer audit retention policies
use the parser for `maxBytes` aliases, so file, object-store, and database audit
byte caps can be configured with human-readable units while retaining the same
normalized numeric policy shape for stores, validation, and diagnostics.

Recent Agentic Commerce persistence transfer audit byte-size display
improvement:
Retention policy and retention assessment maps now include display strings for
configured byte limits and recommended minimum byte floors. Operator-facing
validation output keeps exact numeric `maxBytes` values for automation while
also surfacing labels such as `unlimited`, `1 B`, or `64 KiB` for CLI/API/admin
views.

Recent Agentic Commerce persistence transfer audit byte-size validation
improvement:
`AgenticCommerceWayangByteSizes` now exposes parse reports for validation, and
transfer audit retention policies preserve invalid `maxBytes` input metadata.
Audit config validation reports malformed byte-size strings such as `64xb` as a
blocking `audit_retention_byte_limit_invalid` error instead of silently
defaulting the byte cap to unlimited.

Recent Agentic Commerce persistence transfer audit count validation
improvement:
Transfer audit retention policies now preserve parse metadata for malformed
retained-history count aliases such as `maxTrails`, `maxEvents`, `limit`, and
`capacity`. Audit config validation reports invalid count values as a blocking
`audit_retention_count_invalid` error instead of silently falling back to the
default retention count.

Recent Agentic Commerce persistence transfer audit config remediation
improvement:
Audit diagnostics recommendations now enrich the blocking `fix_audit_config`
action with machine-readable remediation hints. Retention count errors,
malformed byte-size limits, and undersized byte caps report operations,
suggested values, raw invalid inputs, accepted keys, and examples so CLI/API
surfaces can guide operators without reinterpreting validation issue payloads.

Recent Agentic Commerce persistence transfer audit remediation boundary
improvement:
`AgenticCommerceWayangPersistenceTransferAuditConfigRemediation` now owns
validation-issue-to-remediation mapping for transfer audit config. Validation
reports expose remediation counts and maps directly, while diagnostics
recommendations reuse the same boundary instead of duplicating issue-code
switching inside the recommendation model.

Recent Agentic Commerce persistence transfer audit validation vocabulary
improvement:
`AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues` now
centralizes transfer audit config validation issue codes. Validation report
builders, remediation routing, recommendations, and focused tests reuse the
same constants so future storage, retention, and cloud/database rules do not
drift through duplicated string literals.

Recent Agentic Commerce persistence transfer audit contract vocabulary
improvement:
`AgenticCommerceWayangPersistenceTransferAuditContractIssues` and
`AgenticCommerceWayangPersistenceTransferAuditDiagnosticsIssues` now separate
contract harness failure codes from operator diagnostics warnings. Contract
checks, diagnostics, recommendations, and provider-harness tests reuse those
vocabularies, keeping provider-build, record, query, retention, reload, and
not-run warning codes aligned across the transfer-audit surface.

Recent Agentic Commerce persistence transfer audit remediation patch
improvement:
`AgenticCommerceWayangPersistenceTransferAuditConfigPatch` now models safe
config replacement suggestions derived from transfer audit config remediation.
Retention count fixes, retained-history increases, and byte-limit increases
emit patch counts and patch maps inside remediation output and flattened
diagnostic recommendation attributes, while malformed byte-size text remains an
advisory remediation until a single safe replacement is known.

Recent Agentic Commerce persistence transfer audit config schema improvement:
`AgenticCommerceWayangPersistenceTransferAuditConfigSchema` now exposes a
compact machine-readable schema for audit storage kinds, config aliases,
retention policy fields, object-store/database provider settings, validation
issue vocabularies, remediation operations, and patch shapes. The schema can be
built from the default or custom transfer-audit provider registry so operator
docs, CLI/API help, and UI forms stay aligned with the actual parsing and
provider surfaces.

Recent Agentic Commerce persistence transfer audit patch application
improvement:
`AgenticCommerceWayangPersistenceTransferAuditConfigPatch` now applies supported
`replace` patches to config maps using a copy-on-write path updater for
`$.field[.nestedField]` paths. Focused validation tests prove retained-history
and byte-limit remediation patches turn invalid audit configs into valid parsed
configs without mutating the original caller-supplied map.

Recent Agentic Commerce persistence transfer audit validation patch summary
improvement:
Validation reports now expose flattened remediation patches with `patchCount`,
`patches`, and `applyPatchesTo(...)` directly on
`AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport`. Callers
can retrieve or apply all safe config fixes from the validation boundary without
walking each remediation item, while schema report fields advertise the
flattened patch surface.

Recent Agentic Commerce persistence transfer audit patch application report
improvement:
`AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport` now
previews safe remediation patch application with original and patched config
maps plus before/after validation summaries. Validation reports expose
`patchApplicationReport(...)`, and the config schema advertises the application
report fields so CLI/API callers can tell whether safe patches are available,
improve the config, or fully resolve blocking validation errors.

Recent Agentic Commerce persistence transfer audit recommendation patch preview
improvement:
Blocking `fix_audit_config` recommendations now include a compact
`patchApplication` summary with patchability, before/after validity, resolution,
improvement, and validation-code deltas. The full patch application report stays
available from validation reports, while recommendation attributes remain small
enough for CLI/API action lists and schema-driven UI prompts.

Recent Agentic Commerce spec alignment matrix improvement:
`AgenticCommerceSpecAlignmentReport` and
`AgenticCommerceSpecAlignmentRequirement` now provide a machine-readable
alignment snapshot for the pinned Agentic Commerce checkout surface. The report
derives protocol metadata, required HTTP headers, and checkout route expectations
from the existing route catalog/protocol constants, exposing aligned/gap counts,
requirement ids, gap ids, route catalog details, and per-requirement expected vs
actual maps without expanding normal route-catalog payloads.

Recent Agentic Commerce payload alignment matrix improvement:
The Agentic Commerce spec alignment report now also verifies checkout payload
field shapes for create, update, complete, cancel, checkout-session response,
and error bodies. Each payload requirement derives actual field names from
populated DTO `toMap()` output, so accidental camelCase regressions or missing
snake_case protocol fields become visible as alignment gaps instead of only
surfacing in adapter smoke tests.

Recent A2UI spec alignment matrix improvement:
`WayangA2uiSpecAlignmentReport` and `WayangA2uiSpecAlignmentRequirement` now
provide a machine-readable alignment snapshot for the pinned A2UI v0.8 surface.
The report verifies protocol metadata, transport content constants, server and
client message keys, A2A DataPart MIME metadata, and default/mounted HTTP route
semantics, exposing aligned/gap counts and expected-vs-actual maps through the
A2UI route catalog without expanding normal route-catalog payloads.

Recent A2A spec alignment matrix improvement:
`WayangA2aSpecAlignmentReport` and `WayangA2aSpecAlignmentRequirement` now
provide a machine-readable alignment snapshot for the pinned A2A v1.0 JSON-RPC
surface. The report verifies protocol metadata, transport binding constants,
neutral route catalog shape, JSON-RPC method registry coverage, response media
types, and capability-gated method families, with a convenience entry point from
`WayangA2aJsonRpcMethods` for readiness and future publication checks.

Recent A2A Agent Card alignment improvement:
`WayangA2aAgentCardSpecAlignment` now keeps Agent Card payload expectations out
of the main A2A alignment report while contributing top-level field,
component-field, and binding-default requirements. The A2A report now catches
discovery-contract drift in Agent Card field names, nested interface/capability/
skill/signature shapes, and default HTTP+JSON/text mode behavior before client
compatibility depends on route-level smoke tests.

Recent A2A diagnostics alignment snapshot improvement:
`WayangA2aSpecAlignmentSnapshot` now exposes a compact diagnostics view of the
full A2A spec-alignment matrix. JSON-RPC diagnostics include the snapshot under
attributes, add a `specAlignment` check row, and can fail with a dedicated
`spec_alignment_gaps` issue when an injected or future runtime alignment report
contains gaps, while keeping the full requirement payload out of normal
diagnostics responses.

Recent A2A alignment category summary improvement:
`WayangA2aSpecAlignmentCategorySummary` now groups A2A alignment requirements by
protocol, binding, Agent Card, route, and JSON-RPC categories. Full reports and
compact diagnostics snapshots include per-category requirement/aligned/gap
counts plus category-local gap ids, so operators can distinguish route catalog
drift from Agent Card or JSON-RPC drift without expanding every requirement.

Recent A2A alignment category lookup improvement:
A2A alignment reports and compact diagnostics snapshots now expose direct
category lookups, `gapCategorySummaries`, and `gapCategories`, while each
category summary carries an `aligned` flag. Diagnostics consumers can now branch
on failing alignment areas without re-scanning every category summary map.

Recent A2A spec-compliance alignment snapshot improvement:
The JSON-RPC spec-compliance report now embeds the compact A2A alignment
snapshot in its attributes and fails with the shared `spec_alignment_gaps`
diagnostic issue when injected or future runtime alignment gaps are present.
This keeps the spec-compliance endpoint aligned with diagnostics while avoiding
duplication of the full requirement matrix in normal compliance responses.

Recent A2A spec-health facade improvement:
`WayangA2aJsonRpcSpecHealth` now centralizes the live A2A alignment snapshot
used by JSON-RPC diagnostics and spec-compliance reports. The HTTP adapter
delegates spec-health report construction through this facade, keeping adapter
responsibility focused on route dispatch while preserving explicit report
builders for serialization and issue projection.

Recent A2A spec-alignment category checks improvement:
JSON-RPC diagnostics now emit first-class `specAlignment:<category>` check rows
for each compact A2A alignment category. Operators can see protocol, binding,
agent-card, route, and JSON-RPC alignment state directly from the checks list
while the aggregate `specAlignment` row still carries the total gap count.

Recent multi-standard alignment descriptor improvement:
A2A, A2UI, and Agentic Commerce alignment reports now expose a common nested
`standard` descriptor with stable `standardId`, `name`, `version`, `binding`,
and `specUrl` fields. This gives future cross-standard health aggregation a
shared map shape while each module keeps its protocol-specific alignment report
and requirements local.

Recent SDK standard-alignment portfolio improvement:
`wayang-gollek-sdk` now includes neutral standard-alignment descriptor, summary,
and portfolio records that consume plain report maps or diagnostics/spec-
compliance carrier maps with nested `specAlignment` payloads. The portfolio
factory also flattens aggregate `standards`, `alignments`, and report-list
payloads, so gateway health endpoints can roll up several standards from one
map. Duplicate standard summaries are merged by `standardId` with conservative
counts and unioned gaps, so diagnostics and spec-compliance payloads do not
double-count the same standard. A public portfolio builder composes existing
portfolios, summaries, raw reports, carrier maps, and aggregate payloads through
the same parser/normalizer. A2A, A2UI, Agentic Commerce, and future standards
can share this alignment state without creating compile-time dependencies
between protocol modules.

Recent SDK standard registry improvement:
`WayangStandardRegistry` and `WayangStandardDefinition` now provide SDK-local
identity metadata for known standards, including A2A, A2UI, and Agentic
Commerce aliases, versions, bindings, and spec URLs. Alignment descriptor
parsing enriches sparse or alias-based report maps from this registry while
preserving explicit report-provided fields, keeping cross-standard rollups
stable without importing protocol implementation modules.

Recent SDK standard-alignment provenance improvement:
`WayangStandardAlignmentSource` adds lightweight provenance for alignment
summaries. Report-map resolution now carries diagnostics, spec-compliance,
portfolio, explicit `source`, and existing `sources` metadata into summary
`sources`, and duplicate-standard merging unions those sources alongside gaps.
Gateway health rollups can now explain where each standard status came from
without coupling to protocol-specific report classes.

Recent SDK standard-alignment policy improvement:
`WayangStandardAlignmentPolicy` and `WayangStandardAlignmentPolicyAssessment`
now evaluate portfolio readiness against required standards, exact standard
versions, and warning-only gap categories. Required standard ids are
canonicalized through the standard registry, version requirements imply required
standards, blocking gaps and mismatched versions produce actionable
recommendations, and portfolios offer a convenience `assess(policy)` method.
`WayangStandardAlignmentPolicies` adds reusable presets for empty policies,
strict standard gates, registry-pinned standard versions, and all-known-standard
readiness gates so gateways do not copy standard ids or version strings.
`WayangStandardAlignmentPolicyConfig` adds a deployment-facing config bridge for
none/strict/pinned-registry/all-known modes, required standards, warning-only
categories, and version overrides; health reports can now consume that config
directly.
This keeps deployment/readiness decisions outside the neutral rollup model while
giving gateways a reusable policy layer.

Recent SDK standard-alignment health report improvement:
`WayangStandardAlignmentHealthReport` now wraps a portfolio and policy
assessment into a stable JSON-ready gateway payload. The report exposes
`ready`, `ready`/`warning`/`blocked` status, top-level rollup counts, nested
portfolio and policy details, and recommendations. Gateways can publish
multi-standard health without inventing a one-off envelope or depending on
protocol-specific report classes. Registry-pinned health factories now compose
the standard registry presets directly into health envelopes, including
all-known-standard gates for deployment readiness.

Recent protocol standard-alignment adapter improvement:
A2A, A2UI, and Agentic Commerce Wayang integration modules now expose small
standard-alignment adapter facades that convert native spec-alignment reports
into SDK portfolios and health reports. Each adapter supports pinned single-
standard health and deployment-configured health, keeping protocol report
construction local while giving gateways a shared readiness payload shape.

Recent SDK standard registry drift guard improvement:
`WayangStandardRegistryDriftReport` and `WayangStandardRegistryDriftIssue`
compare portfolio standard descriptors against registry metadata, including
name, version, binding, spec URL, and registry-defined descriptor attributes.
Portfolios and health reports expose registry drift summaries, and the protocol
adapter tests now verify A2A, A2UI, and Agentic Commerce descriptors remain
aligned with the shared SDK registry. `WayangStandardRegistryDriftMode` lets
deployment configs ignore, warn on, or block readiness for descriptor drift, and
configured health reports include drift recommendations when the gate is active.

Recent SDK multi-standard health composer improvement:
`WayangStandardAlignmentHealthReports` now composes several protocol portfolios
or raw report maps into one configured health report. Gateways can combine A2A,
A2UI, Agentic Commerce, and future standard adapters with pinned-known or
subset-pinned readiness policies without hand-merging portfolio internals.

Recent Skill Management factory dependency-requirement consolidation:
`SkillStoreFactorySupport` now models runtime store prerequisites with a shared
`DependencyRequirement` descriptor. Definition, lifecycle, artifact, and event
store factories reuse factory-local descriptors for both creation failures and
validation reports, keeping dependency names and error messages aligned while
preserving existing registry, object-store, JDBC, and event-store diagnostics.

Recent Skill Management persistence contract matrix improvement:
`SkillPersistenceContractMatrix` now derives provider, persistence class,
durability, durable-fallback, external/custom, composition, mirroring, and
capability contracts directly from `SkillManagementServiceConfig`. The matrix
normalizes definition, lifecycle, event-history, and artifact store rows,
including nested hybrid and mirrored children, so preflight, CLI/API status,
documentation, and future policy checks can reason about memory, registry,
filesystem, S3/RustFS-compatible object storage, JDBC/database, custom, and
disabled event persistence before stores are constructed.

Recent Skill Management runtime profile improvement:
`SkillManagementServiceProfile`, `SkillManagementServiceProfileOptions`, and
`SkillManagementServiceProfiles` now provide named service-level persistence
profiles for default, local filesystem, S3/RustFS-compatible object storage,
JDBC/database, hybrid object-with-file-fallback, and mirrored object/file
deployments. `SkillManagementServiceConfigs` recognizes
`wayang.skills.profile` plus environment aliases such as
`WAYANG_SKILLS_PROFILE`, applies profile options for base directories, object
prefixes, event retention, and JDBC schema initialization, and still allows
explicit store `kind` groups to override the selected profile.

Recent Skill Management preflight matrix improvement:
`SkillManagementPreflightMatrix` now joins deployment preflight validation with
config-derived persistence contracts. The matrix reports configuration,
target-store, source-store, and capability rows, expands target and explicit
maintenance-source contracts from `SkillPersistenceContractMatrix`, and adds an
event-pruning capability row that carries the configured event-history provider,
capability labels, requirement flag, and preflight errors. Deployment preflight
reports and `SkillManagementServiceFactory` now expose matrix accessors without
changing the existing four-bucket preflight report or admin DTO contract.

Recent Skill Management package skeleton split improvement:
`SkillManagementModuleBoundary` now names the target package skeleton for the
skill-management module: facade, config, contracts, preflight, runtime, store,
workflow, events, admin, and support. Marker `package-info.java` files reserve
the future subpackage destinations while preserving the current flat package
for source compatibility, giving future class moves a tested map instead of a
one-off ad hoc refactor.

Recent Skill Management persistence strategy summary improvement:
`SkillPersistenceStrategySummary` now turns config-derived contract rows into a
compact operational stance: ephemeral, local filesystem, object storage,
database, hybrid fallback, mirrored, custom, mixed durable, or mixed. Service
configs, contract matrices, deployment configs, and deployment preflight reports
expose the summary so operator tooling can inspect file, database, S3/RustFS,
hybrid fallback, and custom-provider persistence posture before stores are
constructed.

Recent Skill Management persistence strategy admin projection improvement:
`SkillManagementAdminPersistenceStrategy`,
`SkillManagementAdminPersistenceRole`, and `SkillManagementAdminPersistenceViews`
now expose the persistence strategy summary as stable admin DTOs with provider,
class, durability, fallback, custom, composite, mirrored, capability, warning,
and recursive child-role details. `SkillManagementAdminViews` delegates service
config, contract-matrix, and summary inputs to the focused mapper, keeping
status/CLI/API rendering out of the core strategy classifier.

Recent Skills CLI persistence status improvement:
`skills status` now renders the admin persistence strategy projection from the
CLI, including the overall strategy, durability flags, role counts, provider
shape, warnings, and nested hybrid/mirrored store branches. The command can
preview named persistence profiles with `--profile` such as default, local,
S3/RustFS-compatible object storage, JDBC/database, hybrid fallback, and
mirrored object/file, or read runtime properties and environment with
`--runtime`.

Recent Skills CLI persistence status JSON improvement:
`skills status --json` now renders the same persistence status projection as
machine-readable JSON through the focused `SkillsPersistenceStatusJson`
serializer. The output includes strategy flags, role counts, warnings,
capabilities, and recursive child branches, keeping automation output aligned
with the admin DTOs without adding JSON rendering logic to the command handler.

Recent Skills CLI persistence status source improvement:
`SkillsPersistenceStatusReport` now wraps CLI persistence status with source
metadata for default config, runtime properties/environment, or named profile
previews. Human output renders `config source`, while `skills status --json`
adds `source`, `profile`, and `runtime` fields before the persistence strategy
payload, making automated checks explicit about which configuration surface was
inspected.

Recent Skills CLI persistence preflight improvement:
`skills status --preflight` now adds deployment preflight readiness to the same
persistence status report. The CLI uses the existing skill-management
deployment preflight path, then renders ready/deployable/error summaries in
human output and a structured `preflight` object in `--json` output with
configuration, target-store, source-store, and capability validation buckets.

Recent Skills CLI status JSON contract hardening:
`SkillsStatusJsonContractTest` now snapshots the exact compact JSON emitted by
`skills status --json` and `skills status --preflight --json` for the default
configuration. This locks the machine-readable field order, source/preflight
envelope, warning list, role counts, role rows, capabilities, and child branch
shape so automation consumers get an explicit contract signal when the CLI
payload changes.

Recent Skills CLI persistence diagnostics improvement:
`skills status --diagnostics` now adds resolved config diagnostics beside the
persistence strategy summary. `SkillsPersistenceConfigDiagnostics` reports the
selected store kind, target path/object prefix/table/custom name, JDBC schema
initialization flag, event retention capacity, lifecycle reconcile mode, and
hybrid/mirrored child branches. The JSON renderer exposes the same diagnostics
object behind `diagnosticsAvailable`, so operators can explain default,
runtime, and named-profile persistence selection without parsing raw config.

Recent Skills CLI persistence profile catalog improvement:
skill-management now exposes canonical persistence profile descriptors with
labels, aliases, and descriptions, and profile alias resolution is derived from
that catalog instead of a separate switch. `skills profiles` renders the catalog
as human-readable text or JSON, including each profile's persistence strategy,
durability, provider shape, role count, warning count, and aliases for default,
local file, S3/RustFS-compatible object storage, JDBC/database, hybrid fallback,
and mirrored object/file deployments.

Recent Skills admin persistence profile projection improvement:
`SkillManagementAdminPersistenceViews` now owns profile catalog projection with
stable `SkillManagementAdminPersistenceProfileCatalog` and
`SkillManagementAdminPersistenceProfile` DTOs. The admin catalog carries
derived profile counts and each profile reuses the existing persistence strategy
projection, while `SkillManagementAdminViews` exposes facade methods for the
whole catalog or one named profile. The skills CLI now renders `skills profiles`
from this admin DTO instead of rebuilding profile summaries locally.

Recent Skills CLI profiles JSON contract hardening:
`SkillsProfilesJsonContractTest` now snapshots the exact compact JSON emitted by
`skills profiles --json`. The contract locks catalog-level counts, profile
ordering, aliases, descriptions, strategy labels, durability flags, provider
shape flags, role counts, and warning counts for default, local filesystem,
object storage/S3/RustFS, JDBC, hybrid fallback, and mirrored profiles.

Recent Skills CLI persistence source selection improvement:
`SkillsPersistenceConfigSource` now owns `skills status` config-source
resolution for default config, named profile previews, and runtime
properties/environment. The status handler consumes this focused value object
instead of embedding source-selection branches, and the CLI now returns a clean
non-zero error when `--profile` and `--runtime` are combined or when a profile
name is unknown.

Recent Skills CLI persistence status service extraction:
`SkillsPersistenceStatusService` now builds `skills status` reports from a
small `SkillsPersistenceStatusRequest`, owning config-source resolution,
admin persistence projection, optional deployment preflight, and optional
resolved config diagnostics. `SkillsCommandHandler` now delegates status report
assembly to this service and focuses on command error handling plus text/JSON
rendering, reducing persistence-specific branching in the command handler.

Recent Skills CLI persistence text renderer extraction:
`SkillsPersistenceStatusText` and `SkillsPersistenceProfileCatalogText` now own
human-readable persistence status and profile catalog output. The command
handler delegates text rendering to these focused renderers, while direct
renderer tests cover diagnostics, preflight summaries, warnings, nested store
rows, profile counts, aliases, and provider-shape summaries.

Recent Skills CLI status JSON variant contract hardening:
`SkillsStatusJsonContractTest` now snapshots richer status JSON variants beyond
the default and default-preflight payloads. Exact contracts cover
`skills status --diagnostics --json` and `skills status --profile hybrid --json`,
locking diagnostics object shape, resolved store rows, source metadata,
hybrid-fallback role nesting, child paths, provider flags, capabilities, and
warning/count fields for automation consumers.

Recent Skills CLI profile inspection command:
`skills profile inspect <profile>` now inspects one named persistence profile or
alias without overloading `skills status`. The command combines the stable admin
profile projection with the existing status service, supports `--json`,
`--diagnostics`, and `--preflight`, and renders through focused text/JSON
inspect renderers. Unknown profile names return the same clean non-zero CLI
error as status/profile source resolution.

Recent Skills CLI durable config validation gate:
`skills config validate --require-durable` now acts as a production persistence
gate. Structural config validity remains separate from gate pass/fail, so the
CLI reports `config valid`, `durability required`, `validation passed`, regular
errors, and policy errors independently in both text and JSON. Ephemeral default
config now exits non-zero only when the durability gate is requested, while
durable profiles such as RustFS/object-storage pass the gate.

Recent Skills CLI runtime config resolution consolidation:
`SkillsPersistenceConfigResolutionService` now centralizes source selection,
validation, diagnostics, and persistence strategy projection for the config
commands. `skills config validate` and `skills config resolve` now adapt that
shared resolution object instead of duplicating source resolution and validation
logic, while `SkillsConfigResolveJsonContractTest` locks the RustFS/object-storage
resolved JSON shape for automation.

Recent Skills CLI runtime config resolve improvement:
`skills config resolve` now renders the effective resolved skill persistence
config for the default config, a named profile, or runtime properties/environment.
The command shares the status config-source resolver, exposes validation state,
diagnostic store rows, lifecycle reconciliation options, persistence strategy,
durability, and warnings in text or JSON, and reuses consolidated diagnostics
renderers so status and resolve stay in sync.

Recent Skills CLI runtime config validation improvement:
`skills config validate` now validates the resolved skill persistence config for
the default config, a named profile, or runtime properties/environment. The
command shares the existing persistence config source resolver with `status`,
returns non-zero for invalid or conflicting sources, and renders compact text or
JSON containing validity, errors, persistence strategy, durability, and warnings.

Recent Skills CLI runtime config group discovery improvement:
`skills config groups` now lists the available runtime config hint groups with
their labels and hint counts, with `--json` for automation. The command is backed
by `SkillManagementRuntimeConfigGroupSummary`, so group discovery, filtered
explain output, and full explain rendering all project from the same management
catalog instead of duplicating group names in the CLI.

Recent Skills CLI runtime config explain group-filter improvement:
`skills config explain` now accepts `--group <name>` so operators and harnesses
can render only one runtime config hint group such as `profile-options`,
`store-overrides`, or `store-option-suffixes`. The same catalog projection feeds
both text and JSON output, and unknown group names fail with a clear error
instead of silently rendering the full catalog.

Recent Skills CLI runtime config sample JSON contract improvement:
`skills config sample <profile> --json` now renders the profile-oriented starter
config as a machine-readable contract containing the resolved profile,
description, property entries, and environment entries. The RustFS/object-storage
alias path is covered by an exact CLI JSON contract so harnesses can consume
sample config without scraping `.properties` or env text output.

Recent Skills CLI runtime config sample improvement:
`SkillManagementRuntimeConfigSamples` now generates profile-oriented starter
runtime config for skill persistence. The new `skills config sample <profile>`
command supports profile aliases such as `rustfs`, `hybrid`, and `db`, and can
render either Java properties or environment-variable form with the shared
profile defaults for file base directories, S3/RustFS object prefixes,
JDBC schema initialization, event retention, and lifecycle reconciliation mode.

Recent Skills CLI runtime config explainability improvement:
`SkillManagementRuntimeConfigHints` now centralizes the operator-facing runtime
configuration surface for skill persistence. The new `skills config explain`
command renders grouped text or JSON for runtime config source precedence,
profile selectors, profile option defaults, role override prefixes, and shared
store option suffixes such as filesystem directories, object prefixes,
JDBC table names, hybrid primary/fallback children, and event retention.

Recent Skills CLI profile inspect JSON contract hardening:
`SkillsProfileInspectJsonContractTest` now snapshots the exact compact JSON
emitted by `skills profile inspect rustfs --json`. The contract locks the
profile-inspection envelope, alias list, description, embedded status metadata,
object-storage strategy fields, durability/provider flags, role rows,
capabilities, counts, warnings, and empty child branches for the S3/RustFS
profile alias path.

Recent A2A JSON-RPC method registry consolidation:
`WayangA2aJsonRpcMethods` now acts as a descriptor registry derived from the
canonical `A2aHttpRouteCatalog.standard()` method mappings. Binding reports,
spec-compliance operation projection, and capability gating now consume the same
method and operation metadata for operation names, REST paths, streaming
response media types, push notification requirements, and extended Agent Card
support.

Recent A2A JSON-RPC method dispatch-table improvement:
`WayangA2aJsonRpcMethodDispatchTable` now separates runtime method lookup from
`WayangA2aJsonRpcDispatcher` execution logic. The dispatcher registers handlers
against the centralized method registry, requires complete coverage at
construction time, exposes the registered method order for diagnostics/tests,
and routes capability checks through the resolved operation metadata instead of
duplicating method-switch behavior.

Recent A2A JSON-RPC method dispatch coverage improvement:
`WayangA2aJsonRpcMethodDispatchCoverage` now provides a JSON-ready completeness
diagnostic for registered protocol methods versus runtime dispatch handlers. The
dispatch table and dispatcher expose this coverage so future harness/reporting
layers can detect missing or orphan method handlers without changing the
existing binding report contract shape.

Recent A2A JSON-RPC adapter dispatch coverage improvement:
`WayangA2aJsonRpcHttpAdapter` now retains its dispatcher and exposes the same
JSON-ready method dispatch coverage at the HTTP adapter boundary. Harnesses and
operator diagnostics can inspect registered-versus-dispatched method coverage
from the adapter without reaching into dispatcher internals or changing binding
report fixtures.

Recent A2A JSON-RPC binding report dispatch coverage improvement:
adapter-generated `WayangA2aJsonRpcBindingReport` payloads now include optional
`methodDispatch` coverage while config-only reports remain backward compatible.
`WayangA2aJsonRpcBindingReportProbeResult` decodes the section when present,
reports registered/dispatch/missing/orphan method sets, and only fails probe
completeness when reported dispatch coverage is incomplete.

Recent A2A JSON-RPC readiness dispatch snapshot improvement:
`WayangA2aJsonRpcReadinessProbeResult` and diagnostics reports now surface a
compact optional method-dispatch snapshot derived from the binding report probe.
Legacy binding-report probes without dispatch coverage remain contract-compatible,
while adapter-generated readiness/diagnostics maps expose reported/complete/pass
state, method counts, and missing/orphan dispatch method names for faster
operator triage.

Recent A2A JSON-RPC diagnostics check-row improvement:
`WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks` now adds an optional
`methodDispatch` row whenever the binding report probe reported dispatch
coverage. The row remains absent for legacy/config-only reports, preserving the
older four-row readiness diagnostics shape, while adapter-generated diagnostics
can show dispatch coverage pass/fail state at the same level as binding,
catalog, and smoke checks.

Recent A2A JSON-RPC readiness issue-classification improvement:
`WayangA2aJsonRpcReadinessIssueBreakdown` now classifies binding-report method
dispatch coverage issues under a `methodDispatch` probe bucket while preserving
the original binding-report issue source. `WayangA2aJsonRpcReadinessIssueSummary`
adds `methodDispatchIssueCount` only when such issues exist, keeping legacy and
config-only summary fixtures stable.

Recent A2A JSON-RPC readiness issue-bucketing consolidation:
binding-report issue classification moved into
`WayangA2aJsonRpcReadinessBindingReportIssueBuckets`, keeping
`WayangA2aJsonRpcReadinessIssueBreakdown` focused on aggregation and envelope
rendering. This gives future binding-report coverage checks a small extension
point instead of growing readiness breakdown conditionals.

Recent A2A JSON-RPC readiness bucket predicate hardening:
method-dispatch issue bucketing now requires both the
`method_dispatch_coverage_incomplete` issue code and the
`methodDispatch.complete` field. This prevents future binding-report issues from
being routed to the method-dispatch bucket by code alone.

Recent A2A JSON-RPC diagnostic-handler issue bucket improvement:
readiness issue summaries now classify concrete binding-report diagnostic
handler coverage failures under a `diagnosticHandlers` probe bucket and expose
`diagnosticHandlerIssueCount` only when that bucket is non-empty. Unreported
coverage from missing/legacy binding reports remains in the generic
`bindingReport` bucket so existing failed-report contract fixtures stay stable.

Recent A2A JSON-RPC diagnostic-handler check-row improvement:
`WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks` now emits an optional
`diagnosticHandlers` row only when the readiness summary contains concrete
diagnostic-handler coverage issues. Healthy/default diagnostics and missing
legacy binding reports keep their existing check-row shape while concrete
handler coverage failures become visible beside route, smoke, dispatch, and
spec-alignment checks.

Recent A2A JSON-RPC coverage check-row consolidation:
optional readiness coverage check rows now flow through
`WayangA2aJsonRpcReadinessCoverageCheckRows`, which owns diagnostic-handler and
method-dispatch row construction. `WayangA2aJsonRpcReadinessProbeCheck` keeps
the top-level diagnostics order while coverage-specific row policy stays in a
small helper that can absorb future coverage buckets.

Recent A2A JSON-RPC binding-report issue bucket rule improvement:
`WayangA2aJsonRpcReadinessBindingReportIssueBuckets` now delegates coverage
issue matching to `WayangA2aJsonRpcReadinessBindingReportIssueBucketRule`.
Diagnostic-handler and method-dispatch bucket predicates are rule definitions
instead of ad hoc branches, giving future binding-report coverage buckets a
small, explicit extension point.

Recent A2A JSON-RPC readiness issue envelope extraction:
`WayangA2aJsonRpcReadinessIssueEnvelope` now owns summary issue envelope
rendering and metadata preservation. `WayangA2aJsonRpcReadinessIssueBreakdown`
stays focused on collecting issue groups and ordering them for summaries, while
the JSON issue shape remains unchanged.

Recent A2A JSON-RPC readiness issue envelope list improvement:
`WayangA2aJsonRpcReadinessIssueEnvelope.wrapAll` now owns list-level envelope
wrapping and empty/null handling. `WayangA2aJsonRpcReadinessIssueBreakdown`
only adds ordered wrapped groups, and the envelope behavior has dedicated tests
for metadata preservation, ordering, and empty input handling.

Recent A2A JSON-RPC readiness issue group extraction:
`WayangA2aJsonRpcReadinessIssueGroups` now owns raw readiness issue group
collection and ordered wrapped summary issue rendering. `WayangA2aJsonRpcReadinessIssueBreakdown`
is reduced to exposing counts and the final summary issue list, keeping grouping
policy separate from the summary record.

Recent A2A JSON-RPC readiness issue group value improvement:
`WayangA2aJsonRpcReadinessIssueGroup` now represents one named readiness issue
group and owns per-group envelope rendering. `WayangA2aJsonRpcReadinessIssueGroups`
keeps only the ordered group catalog and aggregation, so future readiness issue
buckets can be added by extending one ordered list instead of repeating summary
wrapping code.

Recent A2A JSON-RPC readiness issue catalog improvement:
`WayangA2aJsonRpcReadinessIssueCatalog` now centralizes readiness probe names,
summary issue codes, coverage issue fields, and spec-alignment category probe
formatting. Readiness checks, issue grouping, binding-report issue bucketing,
and coverage rows now share that catalog instead of repeating string literals
across small helpers.

Recent A2A JSON-RPC readiness issue summary builder extraction:
`WayangA2aJsonRpcReadinessIssueSummaryBuilder` now owns the assembly of a
readiness probe plus issue breakdown into the operator-facing issue summary.
`WayangA2aJsonRpcReadinessIssueSummary` remains the normalized data envelope
for parsing, JSON rendering, and HTTP responses, keeping construction policy out
of the record itself.

Recent A2A JSON-RPC readiness coverage row dependency cleanup:
`WayangA2aJsonRpcReadinessCoverageCheckRows` now derives coverage-row counts
from `WayangA2aJsonRpcReadinessIssueBreakdown` instead of constructing a full
issue summary. Diagnostic check assembly passes the same precomputed breakdown
into coverage rows and the final readiness row, keeping summary rendering out of
the diagnostics row pipeline.

Recent A2A JSON-RPC readiness diagnostic checks extraction:
`WayangA2aJsonRpcReadinessDiagnosticChecks` now owns diagnostic-check row
ordering, coverage-row insertion, spec-alignment category expansion, and the
final readiness row. `WayangA2aJsonRpcReadinessProbeCheck` remains focused on a
single check row plus failure issue conversion, while diagnostics report
construction calls the dedicated assembler directly.

Recent A2A JSON-RPC diagnostics report builder extraction:
`WayangA2aJsonRpcDiagnosticsReportBuilder` now owns diagnostics report assembly
from readiness, HTTP config, and spec-alignment snapshots. The diagnostics
report record delegates construction through the builder and remains focused on
normalization, parsing, JSON rendering, and HTTP response wrapping.

Recent A2A JSON-RPC diagnostics report detail extraction:
`WayangA2aJsonRpcDiagnosticsReportIssues` now owns readiness issue plus
spec-alignment gap issue aggregation, while
`WayangA2aJsonRpcDiagnosticsReportAttributes` owns diagnostics attribute
construction for config, protocol, binding, spec alignment, and method-dispatch
metadata. `WayangA2aJsonRpcDiagnosticsReportBuilder` stays focused on final
report orchestration.

Recent A2A JSON-RPC diagnostics report status extraction:
`WayangA2aJsonRpcDiagnosticsReportStatus` now owns diagnostics pass/fail and
exit-code policy for readiness plus spec-alignment inputs. The diagnostics
builder delegates status calculation to that value object, keeping future
diagnostics gates out of report assembly code.

Recent A2A JSON-RPC task handler extraction:
`WayangA2aJsonRpcTaskMethodHandlers` now owns task lookup, list, cancel,
subscribe, and task-scoped push notification config JSON-RPC method handlers.
`WayangA2aJsonRpcDispatcher` delegates those method registrations through the
package-local handler group and stays focused on JSON-RPC parsing, preflight,
tenant/capability guards, SendMessage orchestration, and the extended Agent
Card method.

Recent A2A JSON-RPC response factory consolidation:
`WayangA2aJsonRpcHttpResponses` now owns plain JSON-RPC result/error response
construction and event-stream response construction in addition to route-aware
diagnostic responses. The dispatcher and task method handlers delegate envelope
and content-type/header shaping through that helper, removing duplicated
`WayangA2aHttpResponse` construction from transport method code.

Recent A2A JSON-RPC push-config handler extraction:
`WayangA2aJsonRpcPushConfigMethodHandlers` now owns JSON-RPC create, get, list,
and delete handlers for task-scoped push notification configs. The task method
handler composes that package-local group while keeping task lookup, list,
cancel, and subscribe behavior separate from push-config command execution.

Recent A2A JSON-RPC task query handler extraction:
`WayangA2aJsonRpcTaskQueryMethodHandlers` now owns JSON-RPC task get/list
handlers and tenant-aware task lookup for read paths. The task method handler
composes that package-local group ahead of cancel/subscribe and push-config
handlers, reducing the remaining lifecycle handler surface.

Recent A2A JSON-RPC task subscription handler extraction:
`WayangA2aJsonRpcTaskSubscriptionMethodHandlers` now owns JSON-RPC task
subscription handling, including task visibility, terminal-task rejection, and
SSE event-stream response shaping. The task method handler composes the
subscription group separately from task query, cancel, and push-config methods.

Recent A2A JSON-RPC task cancel handler extraction:
`WayangA2aJsonRpcTaskCancelMethodHandlers` now owns JSON-RPC task cancellation,
including task visibility, cancel request projection, task-not-found envelopes,
and lifecycle exception propagation back to the dispatcher. The top-level task
method handler is now a small registrar that composes query, cancel,
subscription, and push-config handler groups.

Recent A2A JSON-RPC task method support consolidation:
`WayangA2aJsonRpcTaskMethodSupport` now owns shared task-scoped JSON-RPC helper
behavior: tenant-aware task lookup, required task parameter extraction, and
task-not-found envelope construction. Query, cancel, subscription, and
push-config method handlers delegate those repeated concerns to the support
boundary and keep their own method logic focused.

Recent A2A JSON-RPC method group coverage improvement:
`WayangA2aJsonRpcMethods` now exposes protocol-ordered functional method
groups, and dispatch coverage reports per-group registered, dispatched,
missing, and orphan method details. Binding-report probes and readiness
diagnostics carry those group maps forward, so send, task query, lifecycle,
subscription, push-config, Agent Card, and unassigned dispatch gaps are visible
without reverse-engineering the flat method list.

Recent A2A JSON-RPC method group readiness issue improvement:
`WayangA2aJsonRpcMethodDispatchIssues` now owns method-dispatch coverage issue
projection. Binding-report probes keep the existing summary dispatch issue and
also emit one issue per incomplete method group, while readiness bucketing and
coverage rows count those group-specific failures in the method-dispatch lane.
Diagnostics can now point directly at fields such as
`methodDispatch.methodGroups.taskQuery.complete`.

Recent A2A JSON-RPC dispatch-table group metadata improvement:
`WayangA2aJsonRpcMethodDispatchTable.Entry` now carries method-group metadata,
and the dispatch table exposes grouped dispatched methods in protocol order.
Dispatch coverage built from the table consumes that grouped dispatch view,
keeping future dynamic or extension handlers closer to their dispatch
registration boundary while preserving the decoded flat-list fallback.

Recent A2A JSON-RPC method dispatch contract fixture improvement:
Focused JSON contract fixtures now cover incomplete method-dispatch coverage
and the corresponding readiness issue projection. The fixtures lock the
`methodGroups` payload and group-specific issue fields such as
`methodDispatch.methodGroups.taskQuery.complete`, protecting diagnostics
consumers from future report-shape drift.

Recent A2A JSON-RPC method preflight policy extraction:
`WayangA2aJsonRpcMethodPreflightPolicy` now owns JSON-RPC method preflight
ordering across parsed SendMessage reuse, tenant validation, capability
validation, and send-message request/configuration validation. The dispatcher
delegates that sequence through the policy result and stays focused on method
lookup, response shaping, and handler execution.

Recent A2A JSON-RPC method handler executor extraction:
`WayangA2aJsonRpcMethodHandlerExecutor` now owns resolved JSON-RPC method
handler execution and exception-to-error mapping for lifecycle, parameter, and
runtime failures. The JSON-RPC dispatcher keeps parse/invalid-request handling,
method lookup, and preflight delegation while method execution failure policy
sits behind a focused boundary, matching the newer HTTP handler executor shape.

Recent A2A JSON-RPC request decoder extraction:
`WayangA2aJsonRpcRequestDecoder` now owns raw JSON-RPC HTTP body decoding,
malformed JSON parse-error responses, request envelope validation, and
invalid-request response shaping with original request id preservation. The
JSON-RPC dispatcher delegates raw-body handling through the decoder and keeps
its request-level path focused on method lookup, preflight, and method
execution.

Recent A2A JSON-RPC core method handler extraction:
`WayangA2aJsonRpcSendMessageMethodHandlers` and
`WayangA2aJsonRpcAgentCardMethodHandlers` now own SendMessage,
SendStreamingMessage, and extended Agent Card JSON-RPC handler assembly. The
JSON-RPC dispatcher no longer stores the task store or send-message service for
private handler lambdas; it builds a dispatch table from focused handler
groups and remains centered on request decoding, method lookup, preflight, and
execution delegation.

Recent A2A JSON-RPC method dispatch table factory extraction:
`WayangA2aJsonRpcMethodDispatchTableFactory` now owns complete JSON-RPC
dispatch-table assembly from SendMessage, task, push-config, and Agent Card
handler groups. The JSON-RPC dispatcher delegates table construction through
the factory and remains focused on request decoding, method lookup, preflight,
and execution delegation, while the factory becomes the stepping stone for
future dynamic operation registration.

Recent A2A JSON-RPC typed request dispatcher extraction:
`WayangA2aJsonRpcRequestDispatcher` now owns typed JSON-RPC request dispatch
after envelope decoding: method lookup, method-not-found responses, method
preflight, and handler execution delegation. The public JSON-RPC dispatcher now
keeps raw JSON decoding and typed-request handoff separate, while method lists
and dispatch coverage still flow through the same diagnostics surface.

Recent A2A JSON-RPC method handler registry extraction:
`WayangA2aJsonRpcMethodHandlerGroup` and
`WayangA2aJsonRpcMethodHandlerRegistry` now represent ordered JSON-RPC handler
contributions before dispatch-table validation. Core SendMessage, task, and
Agent Card handlers publish named groups, and the dispatch-table factory builds
from the registry, giving future dynamic A2A method providers a focused
registration boundary without changing the public JSON-RPC dispatcher.

Recent A2A JSON-RPC method handler provider SPI extraction:
`WayangA2aJsonRpcMethodHandlerProvider` is now the package-local SPI for
JSON-RPC method handler contributors. Core SendMessage, task, and Agent Card
handler groups implement the provider contract, and the registry builder accepts
providers directly, making future extension, skill, MCP, or RAG-backed method
registration additive instead of factory-specific.

Recent A2A JSON-RPC configurable provider assembly improvement:
JSON-RPC dispatch-table and typed-request dispatcher assembly now accepts
additional method handler providers after the core SendMessage, task, and Agent
Card providers. The default constructors still build the same complete core
surface, while package-local extension paths can append or intentionally
override known methods through the registry before dispatch-table validation.

Recent A2A JSON-RPC method handler override policy improvement:
`WayangA2aJsonRpcMethodHandlerRegistry` now assembles handlers with an explicit
override policy instead of relying on implicit map replacement. The default
policy preserves ordered last-provider-wins behavior and records override
metadata, while strict registry assembly can reject duplicate method handlers
for safer extension configurations.

Recent A2A JSON-RPC method registry snapshot diagnostics:
`WayangA2aJsonRpcMethodHandlerRegistrySnapshot` now projects provider assembly
state into JSON-ready diagnostics: handler groups, methods per group, override
policy, and override records. Adapter-backed binding reports include the
optional `methodRegistry` section alongside method dispatch coverage, while
config-only and legacy reports can omit it.

Recent A2A JSON-RPC method registry probe decode improvement:
Binding-report probes now decode the optional `methodRegistry` section from
live binding report bodies and preserve registry group, override policy, and
override record metadata in probe maps. Flattened probe-map round trips support
the same registry fields while legacy binding reports continue to pass without
registry data.

Recent A2A JSON-RPC method registry readiness diagnostics:
Readiness and diagnostics projections now surface reported JSON-RPC method
registry snapshots as optional `methodRegistry` attributes and check rows. The
registry row records provider assembly visibility without adding readiness
issues, while issue accounting remains focused on binding, route, smoke,
diagnostic-handler, and method-dispatch failures.

Recent A2A JSON-RPC method registry test fixture consolidation:
Registry diagnostics tests now share a package-local method-registry fixture
for common provider-group, override, flattened-probe, and snapshot payloads.
This keeps registry payload shape changes centralized while preserving focused
readiness, binding-report, and diagnostics assertions.

Recent A2A JSON-RPC readiness diagnostics check assembly extraction:
`WayangA2aJsonRpcReadinessDiagnosticCheckAssembly` now owns the ordered
composition of base probe rows, coverage rows, spec-alignment rows, and the
overall readiness row. `WayangA2aJsonRpcReadinessDiagnosticChecks` remains the
resolver/delegate, while diagnostics report parts no longer construct the row
assembly directly.

Recent A2A JSON-RPC method handler contributor metadata:
JSON-RPC method handler groups now carry contribution metadata describing the
provider id, module id, capability tags, and priority for the handler source.
Core SendMessage, task, and Agent Card providers publish stable contributor
metadata, and method-registry snapshots surface it per group so future skill,
MCP, RAG, or external A2A method providers can be diagnosed by source.

Recent A2A JSON-RPC method handler registry validation:
`WayangA2aJsonRpcMethodHandlerRegistryValidation` now centralizes guardrails for
handler-provider assembly. Method handler groups must contribute at least one
handler, every contributed method must be part of the supported A2A JSON-RPC
method catalog, and registry assembly rejects duplicate group names before they
make override diagnostics ambiguous.

Recent A2A JSON-RPC core method handler contribution catalog:
`WayangA2aJsonRpcCoreMethodHandlerContributions` now centralizes built-in
handler group names, provider ids, module id, and capability tags for core
SendMessage, task, and Agent Card JSON-RPC providers. Core providers and tests
now reference the catalog instead of repeating identity strings, giving future
skill, MCP, RAG, or external A2A contributors a clearer metadata pattern.

Recent A2A JSON-RPC method registry provider summary diagnostics:
Method-registry snapshots now derive provider-level summaries from group
contribution metadata, including provider ids, module ids, capability tags, and
provider count. Binding-report probe projections flatten the same provider
summary fields for quick diagnostics while still preserving the detailed group
and override sections for deeper inspection.

Recent A2A JSON-RPC method registry provider summary extraction:
`WayangA2aJsonRpcMethodHandlerRegistryProviderSummary` now owns provider-summary
derivation and map projection for method-registry diagnostics. Registry
snapshots keep the same flattened provider fields, but no longer carry the
provider id, module id, and capability-tag extraction logic inline.

Recent A2A HTTP operation preflight policy extraction:
`WayangA2aHttpOperationPreflightPolicy` now owns HTTP operation preflight
ordering across route validation, parsed SendMessage reuse, tenant validation,
capability validation, send-message request/configuration validation, and
required extension negotiation. The HTTP dispatcher delegates through the
policy result and stays focused on route matching, built-in Agent Card
responses, and handler execution.

Recent A2A HTTP Agent Card responder extraction:
`WayangA2aHttpAgentCardResponder` now owns built-in HTTP Agent Card operation
handling, including public-card discovery, extended-card authorization,
extended-to-public fallback, and conditional ETag responses. The HTTP operation
dispatcher delegates Agent Card responses through that boundary so custom
operation routing and handler execution stay separate from card-serving policy.

Recent A2A HTTP operation handler executor extraction:
`WayangA2aHttpOperationHandlerExecutor` now owns custom HTTP operation handler
registration, support checks, handler execution, unsupported-operation
responses, and handler exception-to-response mapping. The HTTP dispatcher keeps
route matching, preflight delegation, Agent Card delegation, and Allow-header
decoration while custom operation execution sits behind a focused boundary.

Recent A2A HTTP route response policy extraction:
`WayangA2aHttpRouteResponsePolicy` now owns matcher-aware HTTP route response
fallbacks, including OPTIONS dispatch, OPTIONS guard errors, method-not-allowed
responses, route-not-found responses, route wrapping, and Allow-header
decoration. `WayangA2aHttpRouteResponses` remains the low-level payload/header
helper while the HTTP operation dispatcher delegates fallback response policy
through the route-response boundary.

Recent Skills CLI durable validation policy extraction:
`SkillsPersistenceConfigValidationPolicy` and
`SkillsPersistenceConfigValidationPolicyResult` now own optional deployment
gates such as `--require-durable`, while
`SkillsPersistenceConfigValidationReport` stays a data envelope. The validation
service accepts policy objects directly, so future production gates can be
added without growing report construction or CLI handlers into policy logic.

Recent Skills CLI validation request envelope improvement:
`SkillsPersistenceConfigValidationRequest` now carries profile/runtime source
selection together with validation policy. `SkillsCommandHandler` and
`SkillsPersistenceConfigValidationService` delegate through that request
boundary, reducing boolean plumbing and keeping future validation options from
spreading through the CLI call chain.

Recent Skills CLI resolve request envelope improvement:
`SkillsPersistenceConfigResolveRequest` now carries profile/runtime source
selection for `skills config resolve`. The CLI command, handler, and resolve
service delegate through the request boundary while preserving the existing
boolean convenience overload, keeping future resolve options from leaking
through method signatures.

Recent Skills CLI config sample service extraction:
`SkillsConfigSampleRequest`, `SkillsConfigSampleReport`, and
`SkillsConfigSampleService` now own sample profile/format resolution for
`skills config sample`. `SkillsCommandHandler` no longer reaches directly into
`SkillManagementRuntimeConfigSamples`; it delegates sample construction to the
small service and stays focused on rendering and exit-code behavior.

Recent Skills CLI runtime config catalog service extraction:
`SkillsConfigCatalogRequest` and `SkillsConfigCatalogService` now own runtime
config hint catalog selection for `skills config explain` and `skills config
groups`. `SkillsCommandHandler` delegates all-vs-selected-group catalog
resolution to the service and keeps only rendering/error output behavior.

Recent Skills CLI persistence profile service extraction:
`SkillsPersistenceProfileCatalogService`,
`SkillsPersistenceProfileInspectRequest`, and
`SkillsPersistenceProfileInspectService` now own profile catalog lookup and
profile inspection composition. `SkillsCommandHandler` delegates profile
descriptor/status assembly to those services and remains focused on rendering
and CLI exit-code behavior.

Recent Skills CLI render support consolidation:
`SkillsCommandRenderSupport` now owns JSON-vs-text report rendering and the
standard `IllegalArgumentException` to stderr/exit-code-1 path. Report-style
commands in `SkillsCommandHandler` delegate through the helper, reducing
duplicate rendering branches while preserving command-specific report services
and success predicates.

Recent Skills CLI status request routing improvement:
`SkillsPersistenceStatusRequest` now has the same defaults/from-options factory
shape as the other CLI request envelopes. `SkillsCommand`, `SkillsCommandHandler`,
`SkillsPersistenceStatusService`, and profile inspection now route status
options through the request boundary instead of manually rebuilding the same
string/boolean tuple.

Recent Skills CLI definition request extraction:
`SkillsDefinitionRequest` now owns CLI skill-definition input normalization for
`register` and `validate`. Registration keeps display-friendly defaults while
validation preserves nullable required fields for validation errors, letting
`SkillsCommandHandler` delegate definition construction instead of carrying
separate helper methods.

Recent Skills CLI definition query service extraction:
`SkillsDefinitionListRequest`, `SkillsDefinitionListReport`,
`SkillsDefinitionInfoRequest`, `SkillsDefinitionInfoReport`, and
`SkillsDefinitionQueryService` now own list/info queries and lifecycle lookup
for skill definitions. Dedicated text renderers keep list/detail output stable
while `SkillsCommandHandler` delegates query composition instead of reaching
directly into `SkillManagementService` for those read paths.

Recent Skills CLI lifecycle command service extraction:
`SkillsLifecycleCommandRequest`, `SkillsLifecycleCommandReport`,
`SkillsLifecycleCommandService`, and `SkillsLifecycleCommandText` now own
enable/disable lifecycle transitions and their text output. `SkillsCommand`
routes lifecycle commands through request objects, and `SkillsCommandHandler`
delegates lifecycle transitions instead of calling `SkillManagementService`
directly.

Recent Skills CLI definition command service extraction:
`SkillsDefinitionCommandService`, `SkillsDefinitionRegistrationReport`,
`SkillsDefinitionValidationReport`, and `SkillsDefinitionCommandText` now own
register/validate mutations and output. `SkillsCommandHandler` no longer calls
`createSkill` or `validateSkill` directly, leaving the handler as a routing and
exit-code boundary while definition command behavior stays testable on its own.

Recent Skills CLI service composition extraction:
`SkillsCommandServices` now owns construction of the command service graph,
including shared persistence status/config services. `SkillsCommandHandler`
receives that composed boundary and routes commands through it, keeping
constructor wiring from growing alongside future CLI features.

Recent Skills CLI info command report extraction:
`SkillsDefinitionInfoCommandReport`, `SkillsDefinitionInfoCommandService`, and
`SkillsDefinitionInfoCommandText` now own found/missing behavior for `skills
info`. `SkillsCommandHandler` delegates the lookup outcome and text rendering
through that boundary instead of carrying private helpers for success and
missing-skill output.

Recent skill-management provider registry extraction:
`SkillStoreProviderRegistry` now provides a reusable leaf-provider lookup for
store factories. `SkillDefinitionStoreFactory` uses it for registry,
filesystem, object-storage, JDBC, and custom definition stores while keeping
hybrid/mirrored composition local, creating a clearer path for the lifecycle,
artifact, and event factories to adopt the same provider-selection boundary.

Recent skill-management lifecycle provider registry adoption:
`SkillLifecycleStateStoreFactory` now uses `SkillStoreProviderRegistry` for
memory, filesystem, object-storage, JDBC, and custom lifecycle stores. Hybrid
and mirrored lifecycle composition remains explicit while the leaf-provider
selection path now matches definition-store persistence wiring.

Recent skill-management artifact provider registry adoption:
`SkillArtifactStoreFactory` now uses `SkillStoreProviderRegistry` for memory,
filesystem, object-storage, JDBC, and custom artifact stores. Artifact
persistence now follows the same leaf-provider path as definition and lifecycle
stores while preserving explicit hybrid/mirrored composition.

Recent skill-management event provider registry adoption:
`SkillManagementEventStoreFactory` now uses `SkillStoreProviderRegistry` for
none, memory, filesystem, object-storage, JDBC, and custom event sinks. Event
history pruning support remains a separate capability check, while core event
store creation and runtime dependency validation now match the other
persistence roles.

Recent skill-management provider registry hardening:
`SkillStoreProviderRegistry` now preserves provider registration order and
rejects duplicate kind registration. Store factories can add future providers
without silently replacing an existing kind or producing unstable provider
catalog ordering.

Recent skill-management event sink factory extraction:
`SkillManagementEventSinkFactory` now owns effective event sink resolution,
including explicit override handling, configured event store validation, and
event-pruning capability validation. `SkillManagementStoreBundleFactory` now
assembles store bundles without carrying repeated event override branches.

Recent skill-management maintenance store factory extraction:
`SkillManagementMaintenanceStoreFactory` now owns maintenance source-store
validation and source-vs-target store resolution for definitions and artifacts.
`SkillManagementStoreBundleFactory` delegates maintenance store assembly to that
focused boundary instead of carrying duplicate source fallback helpers.

Recent A2UI endpoint diagnostics contract fixtures:
The endpoint diagnostics config, raw request, and whole diagnostic plan payloads
now have golden JSON fixtures plus a focused contract test. External CLI, REST,
gateway, and harness adapters can target those fixtures while the runtime code
keeps alias parsing and canonical serialization under the same semantic JSON
assertion path as the rest of A2UI.

Recent A2UI endpoint diagnostic report decoding:
`WayangA2uiHttpEndpointDiagnosticReportDecoder` now owns map/JSON decoding for
stored or remote endpoint diagnostic reports, while the report record exposes
small `fromMap(...)` and `fromJson(...)` delegates. The report payload also has
a golden fixture, so external adapters can validate both diagnostic input plans
and machine-readable output reports without duplicating parsing rules.

Recent A2UI endpoint diagnostic issue extraction:
`WayangA2uiHttpEndpointDiagnosticIssue` now owns unknown-path, route-mismatch,
transport-error, and HTTP-status issue projection for mounted endpoint
diagnostics. `WayangA2uiHttpEndpointDiagnosticReport` delegates issue shaping
to that focused value object, keeping report assembly compact and making issue
policy reusable for future gateway or CLI views.

Recent A2UI endpoint diagnostic issue taxonomy:
`WayangA2uiHttpEndpointDiagnosticIssueCatalog` now centralizes endpoint issue
categories and fallback error-code names. The issue value object delegates
category and fallback-code selection to that catalog, reducing string drift
before diagnostic summaries, runners, and adapter-facing views start reusing
the same issue vocabulary.

Recent A2UI endpoint diagnostic summary:
`WayangA2uiHttpEndpointDiagnosticSummary` now provides a compact pass/fail,
exit-code, count, status/outcome, issue-category, and error-code projection for
mounted endpoint diagnostics. Reports and raw diagnostic results expose
`summary()` helpers, and the summary has JSON/map decoding plus a golden
fixture for CLI, readiness, dashboard, and CI integrations.

Recent A2UI endpoint diagnostic runner facade:
`WayangA2uiHttpEndpointDiagnosticRunner` and
`WayangA2uiHttpEndpointDiagnosticRun` now provide an adapter-facing boundary for
default probes, plan maps, plan JSON, and request-map diagnostics. The facade
delegates execution to the existing diagnostics engine while the run envelope
exposes report JSON, summary JSON, pass/fail, exit code, and a combined
report/summary payload for REST, CLI, gateway, and harness integrations.

Recent A2UI endpoint diagnostic issue contracts:
Unknown-path and route-mismatch endpoint diagnostic issues now have dedicated
golden JSON fixtures, and `WayangA2uiHttpEndpointDiagnosticIssue` exposes
`toJson()` for direct issue-row serialization. This pins the issue-level
payloads separately from aggregate report fixtures for adapter, CLI, and
gateway contract checks.

Recent A2UI endpoint diagnostic run contract:
The adapter-facing `WayangA2uiHttpEndpointDiagnosticRun` envelope now has a
golden JSON fixture that pins the combined `summary` plus `report` payload from
a real mounted endpoint diagnostic run. This gives REST, CLI, gateway, and
harness integrations one stable contract for the facade output.

Recent A2UI transport serialization stability:
`TransportMaps`, `WayangA2uiTransportMetadata`, and
`WayangA2uiTransportError` now preserve canonical linked-map ordering while
still returning immutable projections. Embedded `WayangA2uiTransportResponse`
JSON no longer depends on `Map.copyOf` iteration behavior, and a focused
transport response test guards the problem-json envelope order used by endpoint
diagnostic fixtures.

Recent A2UI endpoint diagnostic map stability:
Endpoint diagnostic config, request, plan, issue, report, summary, run, and
default-attribute merge paths now use the shared immutable linked-map freeze
helper instead of `Map.copyOf`. This keeps the adapter-facing diagnostic JSON
field order stable while preserving defensive snapshots, and the runner test now
guards the combined envelope's `summary` then `report` ordering.

Recent A2UI HTTP probe map stability:
A2UI binding-report probe, smoke summary, smoke probe, and readiness probe
payloads now use the same immutable linked-map freeze helper for top-level maps
and generated issue rows. Contract tests still compare the golden fixtures, and
the readiness fixture path now also guards the authored top-level JSON order.

Recent A2UI HTTP scenario map stability:
A2UI HTTP scenario issues/reports, suite reports, expectation issues/results,
and smoke-result payloads now use the same immutable linked-map freeze helper.
Smoke body JSON keeps stable field order from scenario rows through suite and
expectation summaries, and the contract test guards the smoke result envelope
order.

Recent A2UI contract test skeleton cleanup:
The broad A2UI contract test is now split by responsibility into HTTP,
transport, and surface contract suites, with shared smoke/readiness/binding
fixture construction extracted into `WayangA2uiContractFixtures`. This keeps
golden fixture checks focused while avoiding repeated setup as more adapter
contracts are added.

Recent A2UI HTTP adapter manifest map stability:
A2UI HTTP route, route-catalog, route-binding, endpoint publication,
endpoint request/response/exchange, endpoint header normalization, and
binding-report projections now preserve authored field order through immutable
linked-map snapshots. Contract and publication tests guard the route catalog,
binding report, and publication envelope ordering used by framework adapters.

Recent A2UI surface/session/spec map stability:
The remaining A2UI Wayang surface catalog, surface descriptor, action result
metadata, session config, action policy/context, surface options, agent
extension/context, session cursor, and spec-alignment map snapshots now use
linked immutable copies instead of `Map.copyOf`. Focused tests guard surface
catalog, session config, agent extension/context, and spec-alignment envelope
ordering, leaving the A2UI Wayang package free of `Map.copyOf` iteration drift.

Recent A2A JSON-RPC route publication map stability:
A2A JSON-RPC HTTP route, route descriptor, route catalog, route binding, and
framework publication projections now use the A2A linked-map copy helper instead
of `Map.copyOf`/`Map.of` for authored JSON field order. Contract, descriptor,
and publication tests guard route-catalog, OPTIONS descriptor, and publication
envelope ordering for framework adapters.

Recent A2A scenario and smoke projection map stability:
A2A HTTP and JSON-RPC scenario definitions, exchanges, exchange results,
issues, aggregate results, JSON-RPC smoke results, smoke summaries, and smoke
probe envelopes now freeze authored JSON maps through the A2A linked-map helper.
Harness tests guard scenario, result, smoke, summary, and probe envelope ordering
so operational diagnostics remain predictable for adapters and CLI consumers.

Recent A2A readiness and diagnostics map stability:
A2A JSON-RPC binding reports, binding/route-catalog probes, readiness probes,
readiness issue summaries, diagnostic check rows, coverage snapshots, diagnostic
issue maps, and aggregate diagnostics reports now use the A2A linked-map helper
for authored JSON order. Focused tests guard binding report, coverage,
readiness, issue-envelope, issue-summary, and diagnostics-report envelope order.

Recent A2A spec-alignment map stability:
A2A spec-alignment reports, compact snapshots, category summaries, JSON-RPC
spec operation descriptors, and spec-compliance reports now preserve authored
JSON field order through the A2A linked-map helper. Spec alignment and
spec-compliance tests guard top-level report, snapshot, operation, and
compliance envelope ordering for diagnostics and publication consumers.

Recent skill-management maintenance source resolution cleanup:
`SkillManagementMaintenanceStoreFactory` now resolves maintenance source
configuration once per store bundle assembly and passes that resolved config
through definition and artifact source selection. This keeps fallback behavior
centralized before future source roles or hybrid persistence sources are added.

Recent skill-management preflight report factory extraction:
`SkillManagementPreflightReportFactory` now owns neutral and deployment
preflight report assembly from resolved deployment inputs. The preflight
service is reduced to overload routing, keeping validation bucket construction
in one boundary for future CLI, admin API, and harness projections.

Recent skill-management deployment workflow split:
`SkillManagementDeploymentWorkflow` now owns deployment preflight enforcement,
maintenance correlation, service assembly, and deployment event recording,
while `SkillManagementMaintenanceExecution` owns target-store maintenance
execution. `SkillManagementWorkflowRunner` is reduced to the workflow facade
for configured maintenance and deployment operations.

Recent skill-management preflight default cleanup:
Neutral and deployment preflight reports now expose `empty()` and `orEmpty(...)`
helpers. The enforcer, matrix builder, event attributes, and preflight
exceptions share those defaults instead of hand-building null bucket reports,
which keeps future CLI, admin, and harness fallback behavior consistent.

Recent skill-management operation event recorder boundary:
`SkillManagementEventRecorder` now exposes a reusable operation wrapper that
records success attributes, failure metadata, and operation context around a
runtime action. Deployment and maintenance execution use that boundary instead
of repeating try/success/catch/failure blocks, preparing the mutation runners
for the same event-recording consolidation.

Recent skill-management runner event consolidation:
Bootstrap, direct artifact synchronization, and lifecycle reconciliation now
use the shared operation recorder wrapper for success/failure events. Lifecycle
status transition and artifact mutation paths keep their custom failure
attributes for now, leaving the next consolidation step to add an explicit
failure-attribute overload instead of weakening event payloads.

Recent skill-management custom failure event consolidation:
`SkillManagementEventRecorder` now supports custom failure attributes and
mapped rethrow exceptions around recorded operations. Lifecycle transitions and
artifact mutations use that overload, preserving transition target-status
metadata and artifact write-failure wrapping while removing their local
try/success/catch/failure blocks.

Recent skill-management definition rollback extraction:
`SkillManagementDefinitionMutationRollback` now owns recovery for partially
applied create, update, and delete definition mutations. The definition mutation
runner keeps operation flow and event/write-failure mapping, while rollback
state restoration and suppressed rollback errors live behind a focused helper.

Recent skill-management definition write-failure extraction:
`SkillManagementDefinitionWriteFailure` now owns failure event recording and
write-exception shaping for definition create, update, and delete mutations.
The mutation runner delegates write failure policy to this focused helper,
leaving the runner centered on mutation ordering, rollback, and success events.

Recent A2A JSON-RPC envelope map stability:
A2A JSON-RPC request, response, error, HTTP error-envelope, route-header,
protocol-header, and route-decorated response projections now preserve authored
wire order through the A2A linked-map helper. Focused tests guard emitted JSON
request/result/error envelopes and exact protocol header iteration order so
adapters, harnesses, and downstream clients receive deterministic payloads.

Recent skill-management service factory dependency extraction:
`SkillManagementServiceFactoryDependencies` now owns the normalized dependency
graph behind the public service-factory constructor matrix. The facade keeps
its source-compatible constructors, while registry, JDBC, object-store, custom
store, and explicit factory wiring flow through named assembly paths for future
persistence providers and harness entrypoints.

Recent skill-management preflight event-attribute extraction:
`SkillManagementPreflightEventAttributes` now owns preflight report projection
and preflight-exception failure attributes. `SkillManagementEventAttributes`
remains the compatibility facade, but the preflight bucket/message policy is
isolated for future CLI, admin, deployment, and harness event contracts.

Recent skill-management artifact event-attribute extraction:
`SkillManagementArtifactEventAttributes` now owns artifact sync counts,
artifact reference payloads, and artifact mutation result attributes. The
shared event-attribute facade keeps its existing methods while direct artifact
sync and mutation runners gain a dedicated projection boundary for future
artifact, RAG, package, and MCP descriptor event contracts.

Recent skill-management lifecycle event-attribute extraction:
`SkillManagementLifecycleEventAttributes` now owns lifecycle transition,
revision, reconciliation, and bootstrap readiness projections. The shared
event-attribute facade continues to delegate through the existing methods, but
lifecycle event payload policy is isolated for repair, bootstrap, and harness
contracts.

Recent skill-management maintenance event-attribute extraction:
`SkillManagementMaintenanceEventAttributes` now owns maintenance and deployment
run summary projection, including definition/artifact changes, lifecycle repair
counts, event pruning, and operation context. The shared event-attribute facade
keeps compatibility while deployment and maintenance event contracts have a
dedicated boundary.

Recent skill-management failure event-attribute extraction:
`SkillManagementFailureEventAttributes` now owns operation-context merging and
runtime failure metadata, including preflight failure expansion. The shared
event-attribute facade is reduced to compatibility delegation across artifact,
lifecycle, maintenance, preflight, and failure projection helpers.

Recent A2A task and push projection map stability:
A2A push notification config payloads, push command result maps, send-message
results, task event stream records, agent-response metadata, mapped A2A agent
context, send-message task metadata, and JSON-RPC method binding descriptors now
preserve authored wire order through the A2A linked-map helper. Focused tests
guard public map key order and emitted JSON order for task, push, message, and
method-descriptor projections.

Recent A2A route and task-list projection map stability:
A2A task-list HTTP/JSON-RPC response envelopes, templated HTTP route match
parameters, JSON-RPC smoke scenario list parameters, smoke-probe response
headers, and capability-guard HTTP error metadata now preserve authored order
through linked immutable projections. Focused tests guard task-list JSON order,
route path-parameter order, smoke header order, scenario parameter order, and
capability metadata order at the adapter boundary.

Recent A2A HTTP handler map assembly extraction:
`WayangA2aHttpHandlerMaps` now owns ordered HTTP operation handler map assembly,
permissive public handler-map merging, and strict dispatcher registration copy
semantics. Send-message handlers, task handlers, the public handler composer,
and the HTTP dispatcher now share that boundary instead of duplicating
`LinkedHashMap` construction and immutable-copy behavior.

Recent A2A JSON-RPC handler map assembly extraction:
`WayangA2aJsonRpcMethodHandlerMaps` now owns ordered JSON-RPC method handler map
assembly for send, task query, task cancel, task subscription, push config, and
dispatcher method registration. Method groups now read as protocol method lists,
while the dispatch table remains responsible for descriptor validation and
completeness checks.

Recent skill-management maintenance step executor extraction:
`SkillManagementMaintenanceStepExecutor` now owns the ordered maintenance work:
definition sync, optional artifact sync, lifecycle reconciliation, and event
history pruning. `SkillManagementMaintenanceRunner` keeps constructor/API
compatibility, preflight enforcement, and event recording, leaving the step
sequence isolated for future harness and persistence-strategy extensions.

Recent skill-management maintenance input boundary:
`SkillManagementMaintenanceInputs` now groups source definitions, target
definitions, lifecycle state, and optional artifact stores for maintenance
execution. The runner preserves its public overloads, while the internal
executor consumes one cohesive input object so future hybrid database, file, and
object-storage strategies can be added without widening maintenance call sites.

Recent skill-management maintenance summary boundary:
`SkillManagementMaintenanceSummary` now centralizes aggregate dry-run, changed,
consistency, per-step change counts, conflict counts, lifecycle repair counts,
and event-prune status for maintenance results. Event attributes now consume that
summary instead of re-counting step results, giving admin, harness, and future
diagnostic projections one reusable maintenance summary contract.

Recent skill-management maintenance step diagnostics:
Maintenance results now expose ordered `SkillManagementMaintenanceStepDiagnostic`
entries for definition sync, artifact sync, lifecycle reconciliation, and event
history pruning. Step/status enums keep harness and admin projections independent
from raw result internals while preserving dry-run, skipped, changed, conflict,
inconsistent, and failed step states.

Recent skill-management admin maintenance-step projection:
`SkillManagementAdminMaintenanceStepReport` now maps core maintenance step
diagnostics into stable admin-facing string step/status rows. Maintenance admin
reports include those ordered step rows, while the public admin facade exposes
step and step-list mapping helpers for harness, dashboard, and API consumers.

Recent skill-management maintenance-step event history:
Maintenance and deployment events now encode ordered maintenance step diagnostic
attributes, and deployment history entries reconstruct admin step reports from
those attributes. Historical admin views can now show definition, artifact,
lifecycle, and event-prune step status without re-running maintenance or
depending on raw result objects.

Recent skill-management maintenance-step history summary:
Deployment history pages now derive ordered maintenance step summaries from entry
step reports, including per-step deployment counts, dry-run/skipped/changed
counts, consistency/failure counts, and aggregate change/conflict totals. This
keeps dashboard and harness summary logic behind one admin DTO boundary.

Recent skill-management deployment-history page summary extraction:
`SkillManagementAdminDeploymentHistoryPageSummary` now owns returned, success,
failure, changed, consistency, preflight, and step-summary aggregate derivation
for deployment history pages. The public page DTO stays a stable projection
shape while reusable summary logic lives behind a focused package boundary.

Recent skill-management deployment-history event-window extraction:
`SkillManagementAdminDeploymentHistoryEventWindow` now owns deployment-event
selection from generic event pages, including matched-count and truncation
metadata semantics for deployment-only versus mixed event pages. Deployment
history views now focus on mapping selected events into admin DTO entries.

Recent skill-management deployment-history entry attribute extraction:
Deployment history entry decoding now runs through focused package-private
attribute records for change counts, event-prune state, and preflight metadata.
The public history entry DTO keeps normalization and shape stability, while raw
event-attribute vocabulary is isolated behind reusable decoder boundaries.

Recent skill-management deployment-history aggregate split:
Deployment history page aggregation now composes focused outcome and preflight
summary records before building the public page summary. Success/failure/change
counts and preflight failure buckets have separate testable ownership, keeping
page aggregation extensible without turning the page DTO into a counting hub.

Recent skill-management maintenance-step status classification:
Maintenance step history summaries now classify admin report status strings
through `SkillManagementAdminMaintenanceStepReportStatuses` instead of comparing
raw literals. Failure counts stay tied to the core step-status vocabulary while
admin DTO status strings remain stable for external consumers.

Recent skill-management maintenance-step history grouping:
`SkillManagementAdminMaintenanceStepHistoryGroups` now owns stable grouping of
deployment history step reports, preserving canonical maintenance-step order and
appending custom step ids predictably. Step-history summaries now focus on
turning grouped reports into aggregate counts instead of collecting reports.

Recent skill-management maintenance-step history stats:
`SkillManagementAdminMaintenanceStepHistoryStats` now owns per-step deployment,
dry-run, skipped, changed, consistent, failed, change, and conflict metrics for
grouped maintenance step reports. Step-history summary mapping now composes
grouping plus stats instead of carrying counter logic inline.

Recent skill-management object-storage provider config hints:
Runtime config discovery now includes an object-storage provider group for
S3/RustFS endpoint, bucket, region, access key, secret key, path-style access,
and bucket-wide path prefix settings. Object, hybrid, and mirrored profile
samples now render those provider knobs alongside skill object-prefix settings,
keeping skill persistence configurable without coupling stores to provider SDKs.

Recent S3-compatible storage config typing:
The S3 storage provider now has a typed `S3StorageConfig` record with property
and environment parsers for access key, secret key, bucket, region, endpoint,
path prefix, and path-style access. S3 model and object storage services keep
their compatibility initializers while delegating to the typed config boundary,
making RustFS/S3 setup validation reusable outside service construction.

Recent S3-compatible storage client factory extraction:
`S3StorageClients` now owns AWS SDK client construction from `S3StorageConfig`,
including credentials, region, optional endpoint override, and path-style access.
S3 model and object storage services now focus on storage operations while
sharing one provider setup boundary for AWS S3, RustFS, and MinIO deployments.

Recent provider-neutral object-name mapping extraction:
`ObjectStorageNames` now owns provider path-prefix normalization plus
logical-to-provider object-name mapping for arbitrary object storage services.
The S3 object storage service keeps request handling focused while listed S3
keys are translated back to logical skill-management keys through the tested
storage SPI mapper.

Recent provider-neutral storage path hardening:
`StoragePaths` now has direct SPI-level tests and consistently trims storage
schemes, bucket/container names, prefixes, object names, and owned storage URIs.
Model prefixes default back to `models/` when blank or slash-only, keeping
provider services from carrying their own whitespace and leading-slash fixes.

Recent provider-neutral model-object mapping extraction:
`ModelStorageObjects` now owns model object-name construction, provider
container-scoped storage URI rendering, and owned-URI parsing for S3, GCS, and
Azure model providers. Provider services now focus on SDK requests while shared
model path semantics stay behind a tested SPI boundary.

Recent storage provider lifecycle guard:
`StorageServiceInitialization` now centralizes clear uninitialized-service
failures for storage providers with explicit `initialize(...)` methods. S3
object/model storage plus GCS and Azure model storage guard their SDK clients,
bucket/container names, and shared path mappers before request construction,
with focused tests covering uninitialized operations.

Recent GCS and Azure storage config typing:
GCS and Azure storage providers now mirror the S3 provider's typed config
boundary. `GcsStorageConfig` parses bucket, optional project id, and path prefix
from properties or environment variables, while `AzureStorageConfig` parses
connection string, container, and path prefix. Existing positional initializers
remain as compatibility overloads that delegate through typed config records.

Recent skill-management cloud provider config hints:
The skill-management runtime config catalog now advertises GCS and Azure
object-storage provider keys alongside the existing S3/RustFS defaults. The
object-storage profile samples remain S3/RustFS runnable defaults, while the
catalog exposes alternative GCS bucket/project/prefix and Azure connection
string/container/prefix settings for cloud-provider deployments.

Recent skill-management provider-specific config samples:
Runtime config samples now accept provider-focused aliases such as `gcs`,
`azure`, `hybrid-gcs`, `hybrid-azure`, `mirrored-gcs`, and `mirrored-azure`.
Those aliases still emit canonical persistence profile selectors like
`object-storage`, `hybrid-object-file`, and `mirrored-object-file`, while the
provider-specific sample entries switch to GCS or Azure keys. This keeps
provider choice configurable without duplicating persistence profiles.

Recent skill-management config sample catalog:
`SkillManagementRuntimeConfigSamples` now exposes a discoverable sample
descriptor catalog covering canonical profiles plus provider-focused GCS/Azure
variants. The skills CLI adds `skills config samples` with text and JSON
rendering, so operators can discover sample aliases before rendering a concrete
properties or environment template.

Recent skill-management config sample catalog extraction:
Runtime config sample alias resolution now sits behind
`SkillManagementRuntimeConfigSampleCatalog`, with provider vocabulary and
resolved selections split into package-local value types. The public
`SkillManagementRuntimeConfigSamples` facade now focuses on rendering
properties and environment entries, keeping catalog metadata separate from
sample entry assembly.

Recent skill-management config sample catalog guardrails:
The extracted config sample catalog now has direct package-level tests for
canonical S3/RustFS selection, normalized GCS/Azure aliases, provider-specific
descriptor metadata, and provider defaulting. Cloud-backed profile defaults are
also null-safe, keeping future catalog extensions from depending on callers to
sanitize profile values first.

Recent skill-management object-storage provider readiness:
`SkillManagementObjectStorageProviderConfigAssessment` now classifies runtime
object-storage settings into S3/RustFS, GCS, and Azure provider families and
reports missing or conflicting provider settings without constructing provider
SDK clients. The skills CLI merges those warnings into `config resolve` and
`config validate` only for `--runtime` configs with external providers, leaving
static profile/sample output stable while making live object-storage setup more
actionable.

Recent skill-management provider-readiness catalog extraction:
Object-storage provider family definitions now live behind
`SkillManagementObjectStorageProviderConfigFamilies` and
`SkillManagementObjectStorageProviderConfigFamily`, separating provider-key
metadata from readiness assessment flow. The assessment now composes catalog
entries instead of embedding S3/RustFS, GCS, and Azure required/optional keys,
with focused tests preserving provider order, labels, and incomplete-family
warnings for future provider additions.

Recent skill-management object-storage provider vocabulary consolidation:
`SkillManagementObjectStorageProviderKind` now owns the shared S3/RustFS, GCS,
and Azure ids and labels used by both runtime config samples and provider
readiness checks. Sample providers delegate config names and display labels to
that vocabulary, while readiness families delegate ids, warning labels, and
summary labels, reducing duplicate provider strings across the cloud
persistence setup flow.

Recent skill-management provider sample-entry routing cleanup:
`SkillManagementRuntimeConfigSampleProvider` now owns provider-specific sample
entry selection for S3/RustFS, GCS, and Azure. `SkillManagementRuntimeConfigSamples`
only appends the selected provider's entries, removing the provider switch from
profile sample rendering and keeping future provider additions localized to the
provider vocabulary layer.

Recent skill-management provider-key metadata consolidation:
`SkillManagementObjectStorageProviderConfigKey` now keeps object-storage provider
property names, environment names, defaults, sample descriptions, and
required/optional status together. Runtime config samples and provider-readiness
required/optional property lists now read from the same key descriptors, reducing
drift between operator-facing samples and live object-storage readiness warnings.

Recent skill-management provider-key set extraction:
`SkillManagementObjectStorageProviderConfigKeySet` now groups sample, required,
and optional key ordering per object-storage provider. Runtime sample rendering
and readiness property extraction delegate through the key set instead of
carrying parallel static lists and stale property/env aliases inside the hint
class, keeping provider metadata extension more localized.

Recent skill-management provider-key required-flag consolidation:
Provider key sets now carry sample order plus readiness order only. Required and
optional readiness properties are derived from each
`SkillManagementObjectStorageProviderConfigKey`'s `required` flag, removing
another parallel required/optional list while preserving warning order for
S3/RustFS, GCS, and Azure readiness checks.

Recent skill-management provider-key set guardrails:
`SkillManagementObjectStorageProviderConfigKeySet` now validates duplicate sample
or readiness properties and rejects readiness keys that are not present in the
provider sample key list. Provider metadata mistakes now fail at construction
time instead of silently drifting into runtime config samples or readiness
warnings.

Recent A2A skill-routing preflight metadata stability:
`WayangA2aSkillRouting` now freezes skill-mode lookup maps through linked
immutable copies, preserving Agent Card skill order when selected skills resolve
input and output modes. Send-message request and configuration guards now build
HTTP rejection metadata through ordered local projections, keeping role, skill,
input-mode, and output-mode error payloads stable without centralizing unrelated
validation concerns.

Recent A2A HTTP route response extraction:
`WayangA2aHttpRouteResponses` now owns OPTIONS response payload projection and
Allow-header composition for standard A2A HTTP routes. The operation dispatcher
keeps matching, preflight, and handler execution flow, while route metadata
response shape and header ordering have focused coverage for future route
catalog and harness extensions.

Recent A2A JSON-RPC endpoint preflight response extraction:
`WayangA2aJsonRpcEndpointPreflightResponses` now owns version, extension, and
extended-Agent-Card authorization rejection responses for the JSON-RPC endpoint.
The endpoint preflight guard keeps validation decisions only, while A2A-specific
JSON-RPC error metadata now uses ordered helper maps so version, extension, and
authentication metadata remain stable at the HTTP boundary.

Recent A2A HTTP access rejection metadata stability:
Tenant, required-extension, and extended-Agent-Card authorization HTTP rejections
now build metadata and challenge headers through ordered local projections.
Focused guard-level tests pin tenant, extension, and bearer-auth error payload
order so HTTP preflight and Agent Card access failures remain stable for clients
and harness probes.

Recent A2A scenario fixture definition stability:
`WayangA2aHttpScenarios` and `WayangA2aJsonRpcScenarios` now build smoke
scenario attributes, subscription cursors, task params, and push-config params
through ordered linked projections. HTTP scenario exchange projections also
surface request-level harness attributes separately from exchange metadata, and
focused tests pin fixture order for future docs, probes, and A2UI-facing
scenario consumers.

Recent A2A scenario result projection consolidation:
`WayangA2aScenarioResultProjection` now owns the shared ordered aggregate result
shape for HTTP and JSON-RPC harness runs. Protocol-specific result records keep
their pass/fail semantics and exchange/issue types, while the public scenario
result map order, counts, exchange list, issue list, and attributes projection
are pinned through one focused helper test.

Recent A2A scenario issue projection consolidation:
`WayangA2aScenarioIssueProjection` now owns shared failure-code fallback,
message fallback, and ordered issue-map construction for compact HTTP and
JSON-RPC scenario failures. Protocol-specific issue records still define their
own fields and defaults, while focused tests pin HTTP route failure shape,
JSON-RPC in-band error fallback, and HTTP fallback behavior for JSON-RPC
transport failures.

Recent A2A scenario exchange result projection consolidation:
`WayangA2aScenarioExchangeResultProjection` now owns the shared ordered exchange
result envelope for HTTP and JSON-RPC harness runs. Protocol-specific exchange
result records still own success semantics, request identity, route operation,
decoded error detection, and body selection, while focused tests pin the public
HTTP and JSON-RPC exchange result shapes.

Recent A2A scenario definition projection consolidation:
`WayangA2aScenarioDefinitionProjection` now owns the shared ordered scenario
definition envelope for HTTP and JSON-RPC smoke or contract scenarios.
Protocol-specific scenario records still own typed exchanges and validation,
while focused tests pin definition key order, description normalization,
exchange counts, attributes, and aligned public shapes across both bindings.

Recent A2A scenario exchange definition projection consolidation:
`WayangA2aScenarioExchangeDefinitionProjection` now owns the shared ordered
exchange definition envelope for HTTP and JSON-RPC scenario fixtures. HTTP and
JSON-RPC exchange records still own typed request objects, while focused tests
pin optional request id, path, headers, request attributes, params, and exchange
metadata ordering across both bindings.

Recent A2A JSON-RPC smoke result projection consolidation:
`WayangA2aJsonRpcSmokeResultProjection` now owns ordered smoke-run attributes
and the top-level smoke result envelope. The smoke runner now focuses on
scenario execution, and the smoke result record keeps pass and exit-code
semantics while focused tests pin scenario id, exchange count, scenario result,
and attributes ordering for operational summary and probe consumers.

Recent A2A JSON-RPC smoke summary projection consolidation:
`WayangA2aJsonRpcSmokeSummaryProjection` now owns smoke result parsing,
scenario-issue source decoration, compact-summary body handling, and ordered
summary map construction. `WayangA2aJsonRpcSmokeSummary` stays focused on
normalized value semantics, while focused tests and contract fixtures pin
summary issue fallback, successful-exit state, and public field order for A2UI
and probe consumers.

Recent A2A JSON-RPC smoke probe projection consolidation:
`WayangA2aJsonRpcSmokeProbeProjection` now owns smoke response header
construction and ordered probe map projection. The probe result record keeps
HTTP/probe interpretation semantics while focused tests and contract fixtures
pin smoke-route status, exit-code headers, scenario headers, summary projection,
and public probe field order.

Recent A2A JSON-RPC readiness probe projection consolidation:
`WayangA2aJsonRpcReadinessProbeProjection` now owns readiness envelope
construction, standard readiness attributes, SDK readiness report projection,
and HTTP response wrapping. `WayangA2aJsonRpcReadinessProbeResult` stays focused
on readiness semantics while focused tests and contract fixtures pin conditional
method-dispatch ordering, issue placement, standard attributes, and endpoint
decode stability.

Recent A2A JSON-RPC readiness issue summary projection consolidation:
`WayangA2aJsonRpcReadinessIssueSummaryProjection` now owns issue-summary map
parsing, readiness-probe payload conversion, ordered summary projection, and
HTTP response wrapping. The summary record remains a normalized operator-facing
value envelope, while focused tests and contract fixtures pin optional issue
count placement, route/smoke count ordering, response decode, and failed
readiness fixture stability.

Recent A2A JSON-RPC diagnostics report projection consolidation:
`WayangA2aJsonRpcDiagnosticsReportProjection` now owns diagnostics map parsing,
ordered report projection, SDK diagnostics projection, and HTTP response
wrapping. The diagnostics report record remains a compact normalized aggregate,
while focused tests and contract fixtures pin top-level field order, standard
diagnostics shape, response decode, and fixture stability for CLI, CI, REST,
and gateway consumers.

Recent A2A JSON-RPC spec compliance projection consolidation:
`WayangA2aJsonRpcSpecComplianceReportProjection` now owns compliance map
parsing, ordered report projection, ordered compliance attributes, and HTTP
response wrapping. The spec compliance report record keeps publication/spec
semantics and issue factories, while focused tests and contract fixtures pin
top-level field order, spec-alignment attributes, response decode, and fixture
stability for CLI, CI, REST, and gateway consumers.

Recent A2A JSON-RPC route catalog probe projection consolidation:
`WayangA2aJsonRpcRouteCatalogProbeProjection` now owns route-catalog probe map
parsing and ordered probe envelope projection. The route catalog probe result
record keeps HTTP response interpretation, descriptor completeness checks, and
issue generation, while focused tests and contract fixtures pin field order,
lenient map decoding, JSON round-trip stability, and route descriptor coverage
for readiness, diagnostics, A2UI, REST, and gateway consumers.

Recent A2A JSON-RPC binding report probe projection consolidation:
`WayangA2aJsonRpcBindingReportProbeProjection` now owns binding-report probe map
parsing, derived method-dispatch group fallback, and ordered probe envelope
projection. The binding report probe result record keeps HTTP response
interpretation, diagnostic handler coverage, method-dispatch semantics, and
issue generation, while focused tests and contract fixtures pin optional
method-dispatch placement, lenient map decoding, JSON round-trip stability, and
fixture compatibility for readiness, diagnostics, A2UI, REST, and gateway
consumers.

Recent A2A JSON-RPC binding report projection consolidation:
`WayangA2aJsonRpcBindingReportProjection` now owns ordered binding report map
construction, diagnostic-handler projection, method descriptor projection, and
HTTP response wrapping. The binding report record keeps config normalization,
supported-method validation, method counting, and streaming-method semantics,
while focused tests and contract fixtures pin top-level field order, optional
method-dispatch placement, response metadata, and fixture stability for
readiness, diagnostics, A2UI, REST, and gateway consumers.

Recent A2A JSON-RPC route catalog projection consolidation:
`WayangA2aJsonRpcHttpRouteCatalogProjection` now owns ordered route catalog map
construction and HTTP response wrapping. The route catalog record keeps route
normalization, counting, operation/path lookup, and route matching semantics,
while focused tests and contract fixtures pin top-level field order, response
metadata, configured route projection, adapter compatibility, and fixture
stability for readiness, diagnostics, A2UI, REST, and gateway consumers.

Recent A2A JSON-RPC HTTP route projection consolidation:
`WayangA2aJsonRpcHttpRouteProjection` now owns ordered route catalog entries,
binding-report route entries, OPTIONS payloads, and OPTIONS response wrapping.
Route and route descriptor records keep normalization, enabled/path matching,
allowed-method semantics, and descriptor construction, while focused tests and
contract fixtures pin route entry order, endpoint versus diagnostic report
shapes, OPTIONS metadata, route handler compatibility, and fixture stability
for readiness, diagnostics, A2UI, REST, and gateway consumers.

Recent A2A JSON-RPC HTTP publication projection consolidation:
`WayangA2aJsonRpcHttpPublicationProjection` now owns ordered publication
summaries and published route-binding envelopes. Publication and binding classes
keep enabled-route selection, operation/path lookup, request matching, dispatch,
and mismatch handling, while focused tests and contract fixtures pin published
route order, the `published` registration flag, spec-compliance publication
shape, adapter compatibility, and fixture stability for readiness, diagnostics,
A2UI, REST, and gateway consumers.

Recent A2A JSON-RPC HTTP config projection consolidation:
`WayangA2aJsonRpcHttpConfigProjection` now owns ordered adapter configuration
map projection. The config record keeps normalization, alias parsing, builder
defaults, and duplicate-path validation, while route-surface metadata keeps
canonical route ordering and descriptor construction. Focused tests and
contract fixtures pin config field order, legacy alias decoding, binding-report
config shape, adapter custom-path behavior, and fixture stability for readiness,
diagnostics, A2UI, REST, and gateway consumers.

Recent A2A JSON-RPC diagnostic handler coverage projection consolidation:
`WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection` now owns diagnostic
handler coverage map parsing and ordered coverage projection. The coverage
record keeps route-key derivation, handler comparison, completeness semantics,
and count semantics, while focused tests and contract fixtures pin coverage
field order, status-only fallback, binding-report diagnostic shape, readiness
diagnostics compatibility, and fixture stability for A2UI, REST, and gateway
consumers.

Recent A2A JSON-RPC method dispatch coverage projection consolidation:
`WayangA2aJsonRpcMethodDispatchCoverageProjection` now owns method-dispatch
coverage map parsing, derived method-group fallback for flattened maps, ordered
coverage projection, and method-group map projection. The coverage record keeps
method normalization, registered/dispatch comparison, method-group derivation,
completeness semantics, and count semantics, while focused tests and contract
fixtures pin coverage field order, explicit and derived method groups,
status-only fallback, binding-report probe shape, readiness compatibility, and
fixture stability for A2UI, REST, and gateway consumers.

Recent A2A JSON-RPC method dispatch group coverage projection consolidation:
`WayangA2aJsonRpcMethodDispatchGroupCoverageProjection` now owns method-dispatch
group map parsing and ordered group coverage projection. The group coverage
record keeps group-name fallback, method normalization, group-level
registered/dispatch comparison, completeness semantics, and count semantics,
while focused tests and contract fixtures pin group field order, explicit group
decode, unknown-group fallback, parent coverage projection, binding-report probe
shape, readiness compatibility, and fixture stability for A2UI, REST, and
gateway consumers.

Recent A2UI HTTP route publication projection consolidation:
`HttpRouteProjection` now owns ordered route, route-catalog, and
published binding envelopes, while `HttpPublicationProjection` owns
ordered endpoint publication summaries. Route, catalog, binding, and
publication records keep path mounting, matching, dispatch delegation,
operation/path lookup, and defensive copy semantics, while focused tests and
HTTP contract coverage pin manifest field order, published registration flags,
mounted route projection, publication summary shape, and fixture stability for
A2UI clients and framework bindings.

Recent A2UI HTTP readiness probe projection consolidation:
`HttpReadinessProbeProjection` now owns ordered readiness probe JSON,
native readiness issue envelopes, and shared `WayangReadinessReport` projection
for the A2UI HTTP surface. The readiness record keeps response decoding,
binding/smoke pass semantics, exit-code semantics, optional smoke fallback, and
JSON round-trip entry points, while focused tests and contract fixtures pin
field order, native-versus-shared issue shape, shared readiness probe metadata,
endpoint decoding, and fixture stability for A2UI clients and framework
bindings.

Recent A2UI HTTP binding report probe projection consolidation:
`HttpBindingReportProbeProjection` now owns ordered binding-report
probe JSON and diagnostic issue-envelope construction for missing and orphan
handler operations. The binding-report probe record keeps HTTP response
decoding, transport envelope interpretation, operation counting, content
validation, pass/fail semantics, and lenient map parsing, while focused tests
and contract fixtures pin field order, issue ordering, issue messages,
incomplete binding-report behavior, nested readiness compatibility, and fixture
stability for A2UI clients and framework bindings.

Recent A2UI HTTP smoke probe projection consolidation:
`HttpSmokeProbeProjection` now owns ordered smoke summary JSON,
ordered smoke probe JSON, and suite-versus-expectation issue source attribution
for the A2UI HTTP smoke surface. The smoke summary and probe records keep HTTP
response decoding, result-body interpretation, success/exit-code semantics,
route validation, issue counting, and lenient map parsing, while focused tests
and contract fixtures pin summary/probe field order, default issue source
attribution, failed-smoke behavior, nested readiness compatibility, and fixture
stability for A2UI clients and framework bindings.

Recent A2UI HTTP endpoint diagnostic projection consolidation:
`HttpEndpointDiagnosticProjection` now owns ordered diagnostic report
JSON, diagnostic run envelopes, indexed exchange projection, and derived
diagnostic issue lists for mounted endpoint diagnostics. The diagnostic result,
report, and run records keep counting, pass/fail semantics, summary delegation,
JSON round-trip entry points, and execution-result ownership, while focused
tests and contract fixtures pin report/run field order, exchange indexing,
issue derivation, endpoint diagnostic runner compatibility, and fixture
stability for A2UI clients and framework bindings.

Recent A2UI HTTP endpoint diagnostic summary projection consolidation:
`HttpEndpointDiagnosticProjection` now also owns ordered diagnostic
summary JSON, diagnostic issue JSON, and distinct issue taxonomy extraction for
summary views. The diagnostic summary and issue records keep normalization,
successful-exit semantics, category/error-code derivation, default-message
fallbacks, and JSON round-trip entry points, while focused tests and contract
fixtures pin summary/issue field order, distinct taxonomy ordering, diagnostic
issue projection, and fixture stability for A2UI clients and framework
bindings.

Recent A2UI HTTP endpoint diagnostic plan projection consolidation:
`HttpEndpointDiagnosticPlanProjection` now owns ordered diagnostic
config, plan, and request JSON for external endpoint diagnostic runners. The
config, plan, and request records keep profile parsing, nested probe aliases,
top-level override semantics, path alias decoding, default-request detection,
header/attribute merging, and JSON round-trip entry points, while focused tests
and contract fixtures pin config/plan/request field order, request body
metadata, runner compatibility, and fixture stability for A2UI clients and
framework bindings.

Recent A2UI HTTP endpoint request/response projection consolidation:
`HttpEndpointProjection` now owns ordered framework-facing endpoint
request, response, and exchange JSON for diagnostics and framework adapters.
The endpoint request, response, and exchange records keep route matching,
known-path semantics, request-body requirements, header normalization,
transport-envelope decoding, status/outcome helpers, and response-envelope
accessors, while focused tests and diagnostic contract coverage pin request
optional field order, unknown-route shape, response header metadata, exchange
envelope order, and downstream diagnostic compatibility.

Recent A2A JSON-RPC method handler registry assembly extraction:
`WayangA2aJsonRpcMethodHandlerRegistryAssembly` now owns ordered handler-map
assembly, duplicate-method replacement detection, override policy enforcement,
and override-map projection for contributed method handler groups. The registry
keeps its builder and accessors stable, while focused tests pin handler order,
replacement semantics, strict duplicate rejection, and immutable assembly
collections.

Recent A2A JSON-RPC binding-report probe issue extraction:
`WayangA2aJsonRpcBindingReportProbeIssues` now owns binding-report probe issue
assembly for HTTP response checks, missing method counts, missing required
route paths, diagnostic-handler coverage gaps, and method-dispatch coverage
gaps. The probe result keeps response parsing and projection stable, while
focused tests pin issue taxonomy, actual-value formatting, and legacy binding
report compatibility.

Recent A2A JSON-RPC binding-report probe context extraction:
`WayangA2aJsonRpcBindingReportProbeContext` now owns HTTP response body
decoding, binding-report section parsing, diagnostic-handler coverage parsing,
method-dispatch coverage parsing, method-registry snapshot decoding, and probe
issue wiring. The probe result now assembles from a parsed context, while
focused tests pin nested coverage defaults, registry summary decoding, issue
propagation, and downstream readiness probe compatibility.

Recent A2A spec-alignment requirement catalog extraction:
`WayangA2aSpecAlignmentRequirements` now owns the pinned A2A requirement
catalog for protocol metadata, binding metadata, agent-card requirements,
standard route shapes, JSON-RPC method registry alignment, response media, and
capability gates. `WayangA2aSpecAlignmentReport` now focuses on summarizing and
projecting an already-built requirement list, while focused tests pin
requirement order, missing-route gap behavior, and downstream spec-compliance
compatibility.

Recent A2A spec-alignment category summary consolidation:
`WayangA2aSpecAlignmentCategorySummaries` now owns category summary grouping,
summary lookup, gap-summary filtering, gap category projection, and ordered map
projection for both full reports and compact snapshots. Report and snapshot
projection keep their external JSON shape stable, while focused tests pin
ordered summary construction, nullable summary filtering, gap categories, and
downstream readiness/spec-compliance compatibility.

Recent A2A spec-alignment standard descriptor consolidation:
`WayangA2aSpecAlignmentStandardDescriptor` now owns the standard descriptor
normalization and ordered map projection shared by full spec-alignment reports
and compact snapshots. Existing report constants remain stable for callers,
while focused tests pin descriptor field order, pinned defaults, snapshot
version/binding overrides, and downstream spec-compliance compatibility.

Recent A2A spec-alignment requirement summary extraction:
`WayangA2aSpecAlignmentRequirementSummary` now owns requirement-level aligned
state, total/aligned/gap counts, gap filtering, requirement id projection, and
gap id projection for spec-alignment reports. The report API and JSON contract
remain stable, while focused tests pin count semantics, null filtering, gap
ids, and downstream spec-compliance compatibility.

Recent SDK contract integrity expectation hardening:
`WayangContractIntegrityTest` now derives expected default contract totals,
command totals, and bidirectional link counts from the live contract and
workbench catalogs instead of hardcoded historical counts. The integrity test
continues to verify valid default catalogs and matching link totals, while
remaining stable as new contract descriptors are added.

Recent A2UI surface catalog projection consolidation:
`SurfaceProjection` now owns ordered surface descriptor and surface
catalog JSON for the A2UI model-to-surface registry. The descriptor, catalog,
and registry classes keep kind normalization, model-type support checks,
renderer lookup, catalog filtering, and renderer replacement semantics, while
focused tests and surface/transport/HTTP contract coverage pin descriptor field
order, catalog field order, descriptor ordering, distinct surface kinds, and
surface catalog stability across A2UI transport and HTTP boundaries.

Recent A2UI session policy projection consolidation:
`SessionProjection` now owns ordered session configuration and action
policy JSON for the A2UI runtime boundary. The session config and action policy
records keep mode parsing, allowlist defaults, run-id normalization, required
context matching, enabled/disabled semantics, and policy guard behavior, while
focused tests and bridge/session coverage pin config field order, policy field
order, sorted action/run-id projection, disabled defaults, and transport
stability across A2UI session and bridge harness flows.

Recent A2UI transport envelope projection consolidation:
`TransportProjection` now owns ordered request envelopes, response
envelopes, and transport error body JSON for the A2UI bridge transport layer.
The public `WayangA2uiTransportEnvelope` facade keeps its compatibility surface,
and request/response/error records keep decoding, normalization, outcome
calculation, metadata access, and JSON entry points, while focused tests and
transport/HTTP bridge coverage pin envelope field order, error body order,
facade delegation, defensive-copy behavior, immutability, and fixture stability
for direct and framework-hosted A2UI clients.

Recent A2UI transport metadata projection consolidation:
`TransportMetadataProjection` now owns ordered request metadata,
session-result metadata, catalog metadata, HTTP diagnostic metadata, transport
error metadata, and metadata merge projection for A2UI bridge envelopes. The
public `WayangA2uiTransportMetadata` facade keeps its compatibility surface,
while response records and adapters keep outcome calculation, body encoding,
metadata access, and JSON entry points, with focused tests and bridge coverage
pinning field order, facade delegation, HTTP readiness/smoke/binding metadata,
defensive-copy behavior, null filtering, immutability, and fixture stability.

Recent A2UI HTTP expectation projection consolidation:
`HttpExpectationProjection` now owns ordered expectation-result and
expectation-issue JSON for HTTP harness validation reports. The expectation
records keep target/default normalization, expected-versus-actual string
conversion, pass/fail semantics, issue counting, and JSON entry points, while
focused tests and HTTP contract/smoke coverage pin issue field order, result
field order, nested issue shape, passing-result defaults, and fixture stability
for A2UI HTTP smoke and contract clients.

Recent A2UI HTTP scenario projection consolidation:
`HttpScenarioProjection` now owns ordered scenario-report, suite-report,
scenario-issue, exchange, request, and response JSON for HTTP harness automation
reports. The scenario result/report records keep aggregation, status/outcome
counting, issue derivation, pass/fail semantics, normalization, and JSON entry
points, while focused tests and harness/contract coverage pin report field order,
suite field order, issue field order, nested exchange shapes, route metadata,
response-envelope embedding, and fixture stability for A2UI HTTP smoke clients.

Recent A2UI HTTP binding report projection consolidation:
`HttpBindingReportProjection` now owns ordered route/handler binding
coverage JSON for the A2UI HTTP operation dispatcher. The binding report record
keeps route-catalog comparison, operation normalization, distinct ordering,
missing-handler derivation, orphan-handler derivation, complete-state semantics,
and count helpers, while focused tests and dispatcher/probe/contract coverage
pin report field order, complete and incomplete report shapes, normalized
operation lists, binding-report probe compatibility, and fixture stability.

Recent A2UI HTTP smoke result projection consolidation:
`HttpSmokeResultProjection` now owns ordered smoke-result JSON for
the A2UI HTTP smoke runner. The smoke result record keeps suite/expectation
composition, pass/fail semantics, exit-code derivation, attribute normalization,
and JSON entry points, while focused tests and smoke/contract/transport coverage
pin smoke-result field order, passing and failing shapes, nested suite report
projection, nested expectation projection, response metadata compatibility, and
fixture stability for A2UI smoke clients.

Recent A2UI action metadata consolidation:
`ActionMetadata` now owns ordered metadata maps for inspect, history,
events, wait, and cancel action results routed through the A2UI Wayang bridge.
The action router keeps policy checks, SDK dispatch, response-surface rendering,
context parsing, and unsupported-action handling, while focused tests and
router/session/transport coverage pin metadata field order, history pagination
metadata, event cursor metadata, wait timing metadata, cancel status metadata,
and direct session/transport compatibility.

Recent A2UI spec-alignment requirement projection consolidation:
`SpecAlignmentProjection` now owns ordered requirement JSON for A2UI
spec-alignment reports. The requirement record keeps id/category/title
validation, expected/actual defensive copying, aligned/gap construction, and
message normalization, while focused tests and spec-alignment/route-catalog
coverage pin aligned requirement field order, gap requirement message shape,
message omission for aligned requirements, and pinned A2UI snapshot stability.

Recent A2UI spec-alignment report projection consolidation:
`SpecAlignmentProjection` now also owns ordered top-level
spec-alignment report JSON and the pinned standard descriptor JSON. The report
record keeps pinned A2UI requirement construction, route suffix matching,
message-key checks, route expectation/actual derivation, gap counting, and
alignment semantics, while focused tests and standard-alignment/route/contract
coverage pin report field order, standard descriptor order, route catalog
embedding, requirement projection reuse, and pinned snapshot stability.

Recent A2UI spec-alignment route projection consolidation:
`SpecAlignmentProjection` now also owns ordered route expectation and
actual-route JSON for pinned A2UI spec-alignment requirements, including mounted
path suffix matching. The report record keeps requirement construction, route
lookup, alignment decisions, gap messages, and aggregation semantics, while
focused tests and standard-alignment/route/contract coverage pin expected route
field order, actual mounted route field order, suffix-match behavior, route
catalog embedding, and pinned snapshot stability.

Recent A2UI spec-alignment report default ownership:
`WayangA2uiSpecAlignmentReport.defaultReport()` now owns the canonical pinned
A2UI alignment report built from the default HTTP route catalog, while
`defaults()` remains a compatibility alias. The SDK standard-alignment bridge
now routes portfolio, pinned health, configured health, and null-report
fallbacks through that canonical factory, with focused tests pinning the report
factory and bridge fallback behavior.

Recent A2UI endpoint diagnostic default ownership:
`WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig()` and
`WayangA2uiHttpEndpointDiagnosticPlan.defaultPlan()` now own the canonical
empty/default endpoint diagnostic semantics, while the older `defaults()`
factories remain compatibility aliases. Config and plan decoders, plan
normalization, and endpoint diagnostic constructors now route null/empty
fallbacks through those canonical factories, with focused tests pinning the
record, decoder, and fallback behavior.

Recent A2UI endpoint diagnostic request default ownership:
`WayangA2uiHttpEndpointDiagnosticRequest.defaultRequest()` now owns the
canonical empty diagnostic request shape used by stored/remote request decoders.
Request decoding routes null/empty maps through that factory and preserves raw
request body text instead of trimming payload whitespace, while focused tests
pin the record factory, decoder fallback, alias precedence, and body fidelity.

Recent A2UI endpoint diagnostic plan decoder extraction:
`WayangA2uiHttpEndpointDiagnosticPlanDecoder` now owns stored/remote plan map and
JSON decoding, including external id aliases, flattened probe config keys,
headers aliases, request list decoding, and validation messages. The diagnostic
plan record keeps normalized state, defaults, request counting, JSON entry
points, and projection delegation, while focused tests pin decoder aliases,
record-factory delegation, JSON decoding, and existing plan/projection stability.

Recent A2UI endpoint diagnostic request decoder extraction:
`WayangA2uiHttpEndpointDiagnosticRequestDecoder` now owns stored/remote request
map and JSON decoding, including method normalization inputs, rawPath/path
aliases, body extraction, header and attribute copying, and validation messages.
The diagnostic request record keeps constructor normalization, default request
builders, header/attribute enrichment, JSON entry points, and projection
delegation, while focused tests pin alias precedence, factory delegation, JSON
decoding, and existing request/plan diagnostic stability.

Recent A2UI endpoint diagnostic config decoder extraction:
`WayangA2uiHttpEndpointDiagnosticConfigDecoder` now owns stored/remote config map
decoding, including profile aliases, nested probes, top-level probe overrides,
headers/defaultHeaders aliases, and attributes/defaultAttributes aliases. The
diagnostic config record keeps immutable default state, discovery-only defaults,
header/attribute enrichment, and projection delegation, while focused tests pin
decoder aliases, override precedence, factory delegation, and existing
diagnostic plan/request stability.

Recent A2UI endpoint diagnostic summary decoder extraction:
`WayangA2uiHttpEndpointDiagnosticSummaryDecoder` now owns stored/remote summary
map and JSON decoding, including stringy count parsing, boolean fallback
handling, status-code coercion, issue taxonomy lists, attributes, and validation
messages. The diagnostic summary record keeps report summarization, normalized
state, successful-exit semantics, JSON entry points, and projection delegation,
while focused tests pin stringy summary decoding, factory delegation, JSON
decoding, and existing endpoint diagnostic contract stability.

Recent A2UI binding report probe decoder extraction:
`WayangA2uiHttpBindingReportProbeResultDecoder` now owns stored/remote binding
report probe map and JSON decoding, including stringy HTTP fields, distinct
operation-list parsing, issue copying, metadata/body/header copying, count
fallbacks, and validation messages. The probe result record keeps HTTP response
probing, binding-report envelope derivation, pass/fail semantics, JSON entry
points, and projection delegation, while focused tests pin decoder operation
list normalization, factory delegation, JSON decoding, and readiness/probe
contract stability.

Recent A2UI smoke probe decoder extraction:
`WayangA2uiHttpSmokeProbeResultDecoder` now owns stored/remote smoke probe map
and JSON decoding, including stringy HTTP fields, nested smoke summary decoding,
header copying, boolean/count fallbacks, and validation messages. The smoke
probe record keeps HTTP response probing, route validation, exit-code semantics,
JSON entry points, and projection delegation, while focused tests pin nested
summary decoding, factory delegation, JSON decoding, and smoke/readiness
contract stability.

Recent A2UI readiness probe decoder extraction:
`WayangA2uiHttpReadinessProbeResultDecoder` now owns stored/remote readiness
probe map and JSON decoding, including nested binding-report/smoke probe
decoding, smoke-required parsing, boolean fallback handling, and validation
messages. The readiness probe record keeps HTTP response probing, transport-body
derivation, composite pass/fail semantics, issue projection, shared readiness
report projection, JSON entry points, and projection delegation, while focused
tests pin nested probe decoding, factory delegation, JSON decoding, and
readiness/contract stability.

Recent A2UI smoke summary decoder extraction:
`WayangA2uiHttpSmokeSummaryDecoder` now owns stored summary maps, summary JSON,
transport-backed smoke responses, and raw smoke-result body decoding, including
stringy count/boolean fallbacks, nested suite/expectation issue extraction,
metadata/body copying, and validation messages. The smoke summary record keeps
normalized state, successful-exit semantics, JSON entry points, and projection
delegation, while focused tests pin persisted summary decoding, transport
decoding, raw result decoding, factory delegation, and smoke contract stability.

Recent A2UI session config decoder extraction:
`WayangA2uiSessionConfigDecoder` now owns stored/remote session config map and
JSON decoding, including enabled defaults, mode alias normalization, nested
policy maps, top-level policy fallback merging, run-id allowlists, required
context coercion, and validation messages. The session config record keeps
enablement defaults, named factories, normalized action policy state, JSON entry
points, and projection delegation, while focused tests pin empty defaults,
nested policy decoding, factory delegation, JSON decoding, and session/bridge
stability.

Recent A2UI session config decoder copy cleanup:
`WayangA2uiSessionConfigDecoder` now delegates external config and nested policy
map copying to `TransportMaps` instead of carrying a private shallow
copy helper. Session policy merging remains local to the decoder, while null
external values are filtered consistently with other transport-backed decoders
so top-level fallback policy fields can still fill null nested policy entries.

Recent A2UI transport error decoder extraction:
`WayangA2uiTransportErrorDecoder` now owns stored/remote transport error map and
JSON decoding, including empty/default error fallbacks, canonical field lookup,
record-level text normalization, and validation messages. The transport error
record keeps canonical defaults, factory construction, normalized problem
details, JSON entry points, and projection delegation, while focused tests pin
default decoding, factory delegation, JSON decoding, and transport response
stability.

Recent A2UI decoder primitive consolidation:
`DecodeValues` now owns the shared low-level coercions used by A2UI
decoders for trimmed text, fallback text, boolean fallbacks, and non-negative
integer parsing. Recent smoke summary, smoke probe, readiness probe, session
config, and transport error decoders now delegate those primitive conversions
instead of carrying private duplicates, while focused tests pin fallback and
normalization behavior plus the affected decoder contract suites.

Recent A2UI binding report decoder primitive cleanup:
`WayangA2uiHttpBindingReportProbeResultDecoder` now reuses the shared A2UI
decode primitives for HTTP status, booleans, text fields, and non-negative
count fallbacks while keeping route/handler operation-list tokenization local to
the binding-report probe boundary. This removes another cluster of private
coercion helpers without changing the probe record, projection, or readiness
contract behavior.

Recent A2UI endpoint diagnostic summary primitive cleanup:
`DecodeValues` now also covers lenient nullable integer parsing,
non-negative long parsing, and clamped non-negative int parsing for external
summary counters. `WayangA2uiHttpEndpointDiagnosticSummaryDecoder` delegates
diagnostic ids, pass/fail flags, exit/exchange/issue counts, path/status
counters, transport-error flags, and status-code coercion to the shared helper
while keeping summary list assembly local to the diagnostic summary boundary.

Recent A2UI endpoint diagnostic decoder primitive cleanup:
`WayangA2uiHttpEndpointDiagnosticConfigDecoder`,
`WayangA2uiHttpEndpointDiagnosticRequestDecoder`, and
`WayangA2uiHttpEndpointDiagnosticPlanDecoder` now reuse the shared A2UI decode
primitives for profile/method/body/path/id text normalization and probe boolean
fallback parsing. Their config alias merging, request rawPath/path precedence,
plan config flattening, and request-list decoding remain local to the matching
diagnostic boundary.

Recent A2UI transport decoder primitive cleanup:
`DecodeValues` now includes raw text coercion for transport payloads
that must preserve whitespace. `WayangA2uiTransportRequestDecoder` and
`WayangA2uiTransportResponseDecoder` reuse it for request kinds, request bodies,
response MIME/encoding/body fields, and strict response count parsing while
keeping data-part/metadata copying and field-specific count errors local to the
transport envelope boundary.

Recent A2UI endpoint diagnostic report primitive cleanup:
`WayangA2uiHttpEndpointDiagnosticReportDecoder` now reuses shared A2UI decode
primitives for diagnostics id raw text, string-list raw text, and
transport-error boolean coercion while keeping strict field-specific integer and
count parsing local to the report boundary. This preserves existing numeric
validation messages and record-level report normalization while removing another
private raw-text/boolean helper cluster.

Recent A2UI binding report probe primitive cleanup:
`WayangA2uiHttpBindingReportProbeResult` now reuses shared A2UI decode
primitives for constructor text normalization, response header raw text,
response envelope fallbacks, boolean completion parsing, and non-negative count
fallbacks. Binding-report-specific JSON envelope decoding and route/handler
operation tokenization remain local to the probe result boundary.

Recent A2UI spec-alignment primitive cleanup:
`SpecAlignmentProjection`, `WayangA2uiSpecAlignmentReport`, and
`WayangA2uiSpecAlignmentRequirement` now reuse the shared A2UI decode primitive
for trimmed text normalization in route suffix matching, data-part metadata
checks, requirement validation, and gap-message normalization. Spec-alignment
report shaping, copy semantics, and route expectation projection remain local to
the spec-alignment boundary.

Recent A2UI endpoint publication primitive cleanup:
`WayangA2uiHttpEndpointPublication` now reuses the shared A2UI decode primitive
for operation lookup, route summary values, and raw path normalization before
query/fragment stripping. Publication map copying, mounted route discovery, and
ordered projection remain local to the endpoint publication boundary.

Recent A2UI endpoint diagnostic projection primitive cleanup:
`HttpEndpointDiagnosticProjection` now reuses the shared A2UI decode
primitive for issue category/error-code value extraction while keeping ordered
diagnostic report, run, summary, issue, exchange, and derived-issue projection
local to the diagnostic projection boundary.

Recent A2UI endpoint response primitive cleanup:
`WayangA2uiHttpEndpointResponse` now reuses shared A2UI decode primitives for
content-type fallback normalization, raw response body preservation, scalar
header values, and case-insensitive header-name normalization. Recursive header
collection flattening, default content-type header insertion, and endpoint
response projection remain local to the endpoint response boundary.

Recent A2UI collection decode helper extraction:
`DecodeCollections` now owns the shared distinct token-list coercion
used by binding report probe decoding. `WayangA2uiHttpBindingReportProbeResult`
and `WayangA2uiHttpBindingReportProbeResultDecoder` delegate route, handler,
missing-handler, and orphan-handler operation-list parsing to the helper while
keeping binding-report envelope decoding, count fallback logic, and projection
ownership local to their existing boundaries.

Recent A2UI diagnostic summary collection decode cleanup:
`DecodeCollections` now also owns generic text-value and lenient
integer-value coercion for scalar-or-list payloads. The endpoint diagnostic
summary decoder delegates status code, outcome, issue category, and error-code
collection parsing to the helper while keeping summary counter/boolean parsing,
attribute copying, and JSON validation messages local to the summary decoder.

Recent A2UI diagnostic report map collection cleanup:
`DecodeCollections` now owns single-map-or-map-list coercion for
defensively copied diagnostic payload maps. The endpoint diagnostic report
decoder delegates exchange and issue map decoding to the helper while keeping
strict count parsing, strict status-code parsing, raw outcome coercion, and JSON
validation messages local to the report decoder.

Recent A2UI diagnostic report raw-text collection cleanup:
`DecodeCollections` now also owns raw text collection coercion for
payload values that must preserve whitespace until record-level normalization.
The endpoint diagnostic report decoder delegates outcome list decoding to the
helper while keeping strict count parsing, strict status-code parsing, and JSON
validation messages local to the report decoder.

Recent A2UI record string-list normalization cleanup:
`DecodeCollections` now owns reusable nonblank text-list normalization
for record constructors, including a distinct variant for operation catalogs.
Endpoint diagnostic summaries, HTTP scenario expectations, and HTTP binding
reports delegate their trim/drop-blank list normalization to the helper while
retaining their own count, expectation, ordering, and projection behavior.

Recent A2UI report string-list normalization cleanup:
Endpoint diagnostic reports, HTTP scenario reports, HTTP scenario suite reports,
and HTTP scenario suite expectations now reuse `DecodeCollections`
for exact-match nonblank text-list normalization. Their status-code copying,
map-copying, count clamping, suite validation, and projection behavior remain
local to the owning records.

Recent A2A spec-alignment report projection extraction:
`WayangA2aSpecAlignmentReportProjection` now owns ordered top-level report JSON
for protocol metadata, standard descriptor embedding, requirement metrics,
gap/category projections, route catalog embedding, and requirement rows. The
spec-alignment report keeps summary accessors and delegates full map assembly,
while focused tests pin stable field order, aligned/gap report shapes, gap
category projection, and downstream spec-compliance compatibility.

Recent A2A JSON-RPC route-surface catalog extraction:
`WayangA2aJsonRpcHttpRouteSurfaceCatalog` now owns the singleton JSON-RPC HTTP
route surfaces and canonical publication/binding-report ordering. The route
surface record keeps validation, config-path lookup, distinct-path checks, and
descriptor construction behind the existing API, while focused route/config and
binding-report tests pin route order, descriptor fields, config path rejection,
and downstream report compatibility.

Recent A2A JSON-RPC HTTP config decoder extraction:
`WayangA2aJsonRpcHttpConfigDecoder` now owns map decoding for canonical and
legacy HTTP config aliases plus lenient boolean token parsing. The config record
keeps normalized immutable state, builder defaults, validation, and projection
delegation, while focused decoder/config/route/adapter tests pin alias
compatibility, boolean fallback behavior, ordered projection, and HTTP adapter
path exposure.

Recent A2A JSON-RPC method catalog extraction:
`WayangA2aJsonRpcMethodCatalog` now owns route-catalog-derived method
descriptors, method/operation indexes, streaming method discovery, functional
method-group ordering, and method-to-group lookup. `WayangA2aJsonRpcMethods`
remains the public vocabulary facade with the existing descriptor type and
constants, while focused catalog/method/dispatch/binding-report tests pin
canonical order, index lookup, group completeness, streaming metadata, and
downstream dispatch/report compatibility.

Recent A2A JSON-RPC binding-report probe readiness extraction:
`WayangA2aJsonRpcBindingReportProbeReadiness` now owns the derived binding
report route, JSON content, complete, and passed semantics for binding-report
probe results. The probe result record keeps captured HTTP/body/header state,
diagnostic and dispatch fields, method-registry accessors, and projection
delegation behind the existing API, while focused readiness/probe/projection
and diagnostics tests pin legacy pass behavior, incomplete dispatch handling,
route/content gating, and downstream readiness compatibility.

Recent A2A spec-alignment JSON-RPC requirement extraction:
`WayangA2aSpecAlignmentJsonRpcRequirements` now owns the JSON-RPC method
registry, response media, and capability-gate alignment requirements derived
from the pinned A2A route catalog and the Wayang JSON-RPC method facade. The
top-level requirements assembler keeps protocol, binding, agent-card, and route
assembly order stable, while focused JSON-RPC requirement/spec-report tests pin
requirement order, method counts, streaming media expectations, capability gate
lists, and downstream spec-compliance compatibility. SDK catalog coverage tests
now derive aggregate totals from the catalog/coverage inventories so future
valid contract additions do not require brittle count-only fixture updates.

Recent A2A spec-alignment requirement factory consolidation:
`WayangA2aSpecAlignmentRequirementFactory` now owns the shared aligned-or-gap
construction mechanics used by protocol, binding, route, agent-card, and
JSON-RPC spec-alignment requirement modules. The domain modules now express
expected and actual payloads directly, while focused factory and alignment tests
pin comparison behavior, explicit alignment decisions, and downstream report
compatibility.

Recent A2A spec-alignment route requirement extraction:
`WayangA2aSpecAlignmentRouteRequirements` now owns canonical route requirement
assembly, route-shape projection, route ids, and missing-route gap payloads.
The top-level requirements assembler now preserves report order while delegating
route comparison details to the route module, and focused route tests pin route
ordering, HTTP/JSON-RPC/GRPC shape fields, and missing-route diagnostics.

Recent A2UI readiness probe response decoder extraction:
`HttpReadinessProbeResponseDecoder` now owns HTTP response envelope
unwrapping for readiness probes, including the lenient malformed-body fallback
and raw readiness JSON fallback when a non-envelope object decodes as an empty
transport response. The readiness result record keeps run/fromMap/fromJson,
projection, standard readiness, and pass/exit semantics, while focused response
decoder tests pin transport-envelope, raw JSON, blank, and malformed body
compatibility.

Recent SDK production storage configuration spine:
`WayangStorageBackend`, `WayangObjectStorageConfig`, and `WayangStorageConfig`
now model memory, file, database, object-storage, and hybrid storage choices at
the SDK boundary, including S3/RustFS/MinIO aliases and file fallback paths.
`WayangGollekSdkConfig` keeps the legacy run-store path as a compatibility alias
while exposing typed storage config, and `AgentRunStore.configured` now selects
the current file-backed implementation through the typed effective fallback path.

Recent SDK storage readiness diagnostics:
`WayangStorageReadiness` now turns the typed storage configuration into a shared
Wayang readiness envelope with backend, database, object-storage, and file
fallback probes. The SDK facade exposes storage readiness, local SDK instances
report their configured storage backend, and focused tests pin memory, RustFS,
hybrid database/object-storage/file-fallback, and local SDK readiness behavior.

Recent SDK platform readiness aggregation:
`WayangPlatformReadiness` now aggregates storage readiness, contract integrity,
contract command coverage, and standard-alignment health into one production
readiness envelope. The SDK facade exposes `platformReadiness()`, while focused
tests pin default component ordering, storage failure propagation, and contract
coverage gap diagnostics.

Recent A2UI binding-report probe response decoder extraction:
`HttpBindingReportProbeResponseDecoder` now owns HTTP response
unwrapping, header reads, binding-report body parsing, derived operation lists,
missing/orphan issue projection, and transport metadata fallbacks for binding
report probes. The probe result record keeps normalized immutable state,
run/fromMap/fromJson, route/content/pass checks, and projection delegation,
while focused response decoder tests pin transport-envelope, raw JSON fallback,
blank body, malformed body, and record factory compatibility.

Recent A2UI smoke probe response decoder extraction:
`HttpSmokeProbeResponseDecoder` now owns HTTP response field assembly
for smoke probes, including status, headers, outcome, and summary decoding
delegation. The smoke probe result record keeps immutable normalization,
run/fromMap/fromJson, smoke-route/pass/exit semantics, and projection
delegation, while focused response decoder tests pin transport-envelope
compatibility, record factory delegation, and the existing strict malformed
transport-response errors.

Recent A2UI HTTP response header accessor cleanup:
`WayangA2uiHttpResponse` now exposes a shared raw-text header accessor for exact
header-name lookups with an empty missing-header fallback. Binding-report probe
response decoding, smoke probe response decoding, scenario response projection,
and scenario issue creation now reuse the response accessor instead of carrying
private string-header helpers, while focused tests pin raw value preservation
and missing-header behavior.

Recent A2UI HTTP response body decoder cleanup:
`HttpResponseBodyDecoder` now owns lenient JSON body decoding for HTTP
responses that may be full A2UI transport envelopes or raw JSON bodies. The
readiness and binding-report probe response decoders reuse the shared helper for
blank, malformed, raw-body, and transport-envelope fallback behavior while
keeping their result assembly, pass rules, counters, and issue projection local.
Focused tests pin envelope metadata/body decoding, raw JSON fallback, and empty
body fallback behavior.

Recent A2UI HTTP response body envelope-shape guard:
`HttpResponseBodyDecoder` now detects actual transport envelopes by
their response-envelope fields before copying transport metadata, MIME, encoding,
and outcome values. Raw JSON bodies still decode as payload maps, but they no
longer inherit synthetic transport defaults from the transport response decoder;
focused tests pin the empty transport context for raw-body fallback.

Recent A2UI HTTP response body transport-signature hardening:
`HttpResponseBodyDecoder` now treats common raw payload fields such as
`body` and `metadata` as payload data unless stronger response-envelope
signature fields are present, such as MIME, body encoding, outcome, data parts,
counts, or empty-state markers. Focused tests pin raw payloads that include
top-level `body` and `metadata` fields without assigning synthetic transport
context.

Recent A2UI response decoder test fixture consolidation:
`HttpResponseDecoderTestFixtures` now owns shared JSON HTTP response,
raw body JSON, and transport-envelope JSON builders for response decoder tests.
Readiness, binding-report, and smoke response decoder tests reuse the fixture
instead of carrying repeated response/header/envelope setup, keeping future
decoder behavior changes easier to pin in one place.

Recent A2UI HTTP issue map builder extraction:
`HttpIssueMaps` now owns the small ordered issue-map builders shared by
binding-report probe issues, smoke summary issue copying, and readiness probe
failure/attribute projection. The owning projection modules keep their domain
decisions, pass rules, and issue source selection local, while focused helper and
projection tests pin field order, source fallback, and probe status/route
normalization.

Recent A2UI HTTP report metrics extraction:
`HttpReportMetrics` now owns the ordered HTTP report metric fragments
shared by scenario reports, scenario suite reports, endpoint diagnostic reports,
and endpoint diagnostic summaries. The report projections still own their
identity fields, issue payloads, and endpoint-specific counters, while the helper
keeps outcome counts, transport counts, and status/outcome digests consistent.

Recent A2UI HTTP report metric decoder extraction:
`HttpReportMetricDecoders` now owns strict count and integer-list
coercion for stored or remote HTTP report payloads. Endpoint diagnostic report
decoding is back to field mapping, while the helper preserves owner-specific
validation messages for invalid counts, overflowing integer counts, and
non-numeric status code lists.

Recent A2UI HTTP exchange metric aggregation extraction:
`HttpMetricExchange` defines the small metric-facing protocol shared by
scenario and endpoint diagnostic exchanges, and `HttpExchangeMetrics`
now owns aggregate counts, status-code lists, outcome lists, transport error
checks, and response envelope collection. Scenario and endpoint diagnostic
results delegate shared HTTP metrics while keeping scenario identity and
endpoint-specific known/matched path counts local.

Recent A2UI HTTP scenario suite aggregation extraction:
`HttpScenarioSuiteMetrics` now owns suite-level aggregation over
scenario results and scenario reports, including pass/fail counts, HTTP exchange
totals, transport counters, scenario ids, report maps, and flattened issues.
`WayangA2uiHttpScenarioSuiteResult` now delegates metrics to the helper, while
`WayangA2uiHttpScenarioSuiteReport` delegates report-map and issue flattening.

Recent A2UI transport exchange metric extraction:
`TransportMetricExchange` now defines the transport-level metric view
shared by bridge and HTTP exchanges, with `TransportExchangeMetrics`
owning handled/rejected counts, transport error checks, outcome lists, and
response envelope collection. The HTTP metric helper keeps only HTTP-specific
status and success counts, while bridge scenario results now reuse the shared
transport aggregation path.

Recent A2UI record collection normalization:
`RecordCollections` now owns null-safe list and non-blank JSON payload
normalization for A2UI scenario and result records. Bridge/HTTP scenarios,
scenario suites, endpoint diagnostic plans, and their result records now share
the same constructor cleanup path while preserving the existing request payload
filtering behavior.

Recent A2UI record scalar normalization:
`RecordValues` now owns constructor-time text trimming and defaulting
for harness and diagnostic value records. Scenario IDs, suite IDs, diagnostic
IDs, expectation IDs, issue fields, diagnostic categories, and error-code
defaults now flow through one package-local scalar boundary while decoder
coercion remains isolated in `DecodeValues`.

Recent A2UI record number normalization:
`RecordNumbers` now owns constructor-time numeric clamping for A2UI
records. Scenario/bridge exchange indexes, action feedback sequences, smoke and
binding probe counts, scenario report metrics, diagnostic report metrics,
diagnostic issues, and nullable expectation thresholds now share one
package-local non-negative and one-based normalization policy.

Recent A2UI record snapshot normalization:
`RecordCollections.copyList` now owns immutable list snapshotting for
A2UI record constructors that preserve `List.copyOf` semantics. Action/session
results, HTTP scenario and diagnostic status-code lists, expectation status
codes, and binding-report operation lists now share the same null-to-empty and
null-element-rejecting snapshot path.

Recent A2UI constructor text normalization:
Remaining constructor-local string trimming in action results, smoke probe
results, smoke summaries, binding-report probe results, and spec-alignment
requirements now flows through `RecordValues`. Decoder coercion stays
in `DecodeValues`, keeping parsed input handling separate from record
value normalization.

Recent A2UI record map normalization:
`RecordMaps` now owns constructor-time map policies for A2UI records.
Action-result metadata uses the nullable-value snapshot policy, while
spec-alignment requirements use the string-key/non-null-value policy. The
spec-alignment report now delegates recursive map snapshots to
`TransportMaps` instead of keeping a local duplicate.

Recent A2UI string-context map normalization:
`StringMaps` now owns string-key context map normalization for A2UI
policies, surface options, and session-config decoding. Constructor paths share
the string-value snapshot policy, while the decoder path keeps its
`String.valueOf` coercion for remote/stored config values.

Recent A2UI set normalization:
`RecordCollections` now owns constructor-time set snapshotting and
trim/drop-blank string-set normalization, while `DecodeCollections`
owns comma-separated text-set coercion for remote/stored config inputs. Action
policies, surface options, and session config decoding now share these helpers
instead of carrying local set-copy and stream pipelines.

Recent A2UI projection collection ordering:
`ProjectionCollections` now owns stable string collection ordering for
A2UI projections. Session policy export delegates sorted set projection to the
helper, and binding-report assembly delegates route-catalog-first handler
ordering with sorted orphan handlers instead of keeping a local stream pipeline.

Recent A2UI action-context reader extraction:
`ActionContextReader` now owns typed reads for inbound A2UI
`userAction.context` values, including trimmed text, alias fallback, numeric
coercion, blank-value checks, and immutable context enrichment. The action router
and session continuity state now share this boundary instead of keeping local
context coercion helpers.

Recent A2UI action vocabulary consolidation:
`WayangA2uiActions` now owns default action groups and run-id requirement
classification in addition to stable action names. Action policies, surface
options, and the admission gate now depend on that vocabulary instead of
duplicating action sets or private action taxonomy checks.

Recent A2UI action handler registry extraction:
`WayangA2uiActionHandlers` now owns built-in action lookup and action-to-SDK
dispatch for inspect, history, events, wait, and cancel. `WayangA2uiActionRouter`
now keeps message filtering and policy admission orchestration, then delegates
accepted actions to the handler registry instead of carrying a per-action switch.

Recent A2UI action dispatch extension boundary:
`WayangA2uiActionHandler` is now the public functional boundary for custom
policy-admitted A2UI actions. `WayangA2uiActionHandlers` exposes builder,
standard-builder, replace, remove, and support-list operations, and
`WayangA2uiActionRouter` can accept a prebuilt handler registry so custom
actions can be added without changing the router or built-in SDK dispatch code.

Recent A2UI session profile consolidation:
`WayangA2uiSessionProfiles` now owns named session modes, mode aliases, and
default action-policy resolution for inspect-only, read-only, run-lifecycle, and
custom sessions. `WayangA2uiSessionConfig` keeps its compatibility constants and
factory methods, while config decoding now delegates profile normalization and
base-policy selection to the shared profile boundary.

Recent A2UI action binding diagnostics:
`WayangA2uiActionBindingReport` now compares allowed action-policy names against
registered `WayangA2uiActionHandlers`, reporting missing handler actions and
orphan handler actions in stable order. The handler registry and action router
both expose the report so custom A2UI action wiring can be checked before a
policy-admitted user action reaches runtime dispatch.

Recent A2UI action-query mapping extraction:
`ActionQueries` now owns conversion from inbound action context into
SDK history queries, event queries, wait options, and cancel reasons. The action
router keeps policy validation, SDK dispatch, and response-surface rendering,
while query parsing and alias/default handling sit behind a focused package-local
boundary with direct tests.

Recent A2UI action admission gate extraction:
`ActionGate` now owns pre-dispatch policy checks for allowed action
names, required run ids, allowed run ids, and required policy context. The router
now receives either an accepted run id or a prebuilt rejection result, keeping
admission ordering and rejection messages testable without mixing them into SDK
dispatch logic.

Recent A2UI action response assembly extraction:
`ActionResponses` now owns conversion from SDK lifecycle responses into
handled A2UI action results, including surface rendering, ordered metadata, and
cancel-result status projection. The action router now focuses on policy-gated
dispatch while response assembly is directly tested behind a package-local
boundary.

Recent A2UI surface action-control extraction:
`SurfaceActions` now owns surface action option fallback, A2UI action
construction, and label/button component assembly. `WayangA2uiSurfaces` can keep
focusing on status, history, and event surface layout while action-control wiring
stays behind a focused package-local helper.

Recent A2UI surface identity extraction:
`SurfaceIds` now owns safe surface-id segment normalization and the
standard Wayang A2UI surface id formats for run status, run events, run history
rows, and action-result feedback. Standard and result surface renderers now share
the same id rules instead of carrying duplicate `safeId` implementations.

Recent A2UI surface data-model extraction:
`SurfaceData` now owns run-row, event-row, and count-map data-model
entry construction for Wayang A2UI surfaces. `WayangA2uiSurfaces` delegates
summary maps and run/event collections to that helper, keeping the main renderer
closer to visible layout and message-stream assembly.

Recent A2UI surface data list-normalization cleanup:
`SurfaceData` now delegates run and event collection normalization to
`RecordCollections.nonNullList` instead of carrying local null-list
fallbacks. Run/event data-model assembly now shares the same null-safe list
policy as scenario and diagnostic records, and focused tests pin that accidental
null entries are skipped before surface rows are rendered.

Recent A2UI surface text extraction:
`SurfaceText` now owns history/event summaries, fallback messages, and
run/event row text for Wayang A2UI surfaces. The main surface renderer delegates
text formatting to this package-local helper, leaving it closer to layout and
message-stream assembly.

Recent A2UI run-status surface extraction:
`RunStatusSurface` now owns the single-run status surface layout,
action controls, data model, and begin-rendering message sequence. The public
`WayangA2uiSurfaces.runStatus(...)` facade delegates to that renderer, starting
the larger status/history/events surface split without changing public callers.

Recent A2UI run-events surface extraction:
`RunEventsSurface` now owns event stream layout, refresh-action
context, data model entries, and begin-rendering assembly. The public
`WayangA2uiSurfaces.runEvents(...)` facade delegates to the renderer, continuing
the surface split while keeping registry/session callers stable.

Recent A2UI run-history surface extraction:
`RunHistorySurface` now owns history page layout, per-run row actions,
summary/data model entries, and begin-rendering assembly. The public
`WayangA2uiSurfaces.runHistory(...)` facade delegates to it, leaving the facade
focused on stable API entry points and inspection composition.

Recent A2UI run-inspection surface extraction:
`RunInspectionSurface` now owns inspection composition across the
status and optional events surfaces. The public `WayangA2uiSurfaces` class is
now a thin facade over dedicated status, inspection, events, and history
renderers.

Recent A2UI run action control consolidation:
`RunActionControls` now owns the shared inspect/events/wait/cancel
button assembly for run status surfaces and history rows. Status and history
renderers keep their layout/data-model responsibilities while delegating common
action gating and id/label wiring.

Recent A2UI surface message assembly consolidation:
`SurfaceMessages` now owns the standard data-model update, surface
update, and begin-rendering message sequence. Run status, run events, run
history, and action-result renderers keep their domain data/component assembly
while sharing the common envelope ordering.

Recent A2UI root layout helper consolidation:
`SurfaceLayouts` now owns small root-column layout primitives for
mutable component/child lists and root-column insertion. Run and action-result
renderers keep their surface-specific component content while sharing the common
root-column setup.

Recent A2UI run data-model extraction:
`RunDataModels` now owns top-level data-model entry assembly for run
status, event stream, and history surfaces. The run renderers are now closer to
layout/action composition while delegating published data payload shape to a
focused helper.

Recent A2UI surface architecture documentation:
`docs/A2UI_SURFACE_ARCHITECTURE.md` now documents the surface facade, registry,
renderer, helper, and test ownership map created by the A2UI surface split. The
docs index links the guide so future model-to-surface extensions can follow the
package boundary without rebuilding a giant `WayangA2uiSurfaces` class.

Recent A2UI run surface contract fixture coverage:
`WayangA2uiSurfaceContractTest` now pins parsed JSONL fixtures for read-only
run events, run history, and composite run inspection in addition to the existing
run-status and action-feedback contracts. Future renderer/helper refactors can
change internals while tests keep the published A2UI message order, component
ids, action context, and data-model payload stable.

Recent A2UI run surface test fixture consolidation:
`RunSurfaceFixtures` now owns the shared running/completed run status,
event page, history page, and inspection samples used across A2UI surface,
contract, text, data-model, and action-control tests. The surface tests no
longer repeat constructor-heavy sample setup, reducing fixture drift while
keeping production renderers free of test-only concerns.

Recent A2UI JSONL test support consolidation:
`A2uiJsonlTestSupport` now owns shared test-only streaming and parsed-line
access for generated A2UI message streams. Surface, registry, and action-result
tests reuse it for data-model update, surface update, and begin-rendering JSON
inspection instead of each carrying local `ObjectMapper`, `A2uiJsonlCodec`, and
manual JSONL line splitting.

Recent SDK/CLI platform readiness command exposure:
`status --readiness --json` is now a real workbench command linked to the shared
`wayang.readiness/readiness-aggregate` contract. The CLI status command routes
the option through `sdk.platformReadiness()`, emits the readiness envelope with
the envelope exit code, and keeps text rendering in a small dedicated formatter.
Contract discovery, command coverage, and CLI golden fixtures now pin the
aggregate readiness command while `readiness-report` stays a reusable
commandless component envelope. Storage and platform readiness diagnostics now
use deterministic ordered attribute maps so production readiness JSON remains
stable across JVM runs.

Recent SDK platform readiness provider capability coverage:
`WayangPlatformReadiness` now includes a dedicated
`wayang.provider-capability.readiness` component in the aggregate readiness
report. The component interprets the existing provider capability discovery
surface without mixing discovery and readiness concerns, exposes capability,
provider, module, standard, type, and state facets, and fails fast when the
provider capability catalog is empty or unavailable. SDK tests pin the default
catalog readiness and empty-catalog issue shape, while CLI readiness JSON golden
fixtures now include the provider capability component.

Recent SDK platform readiness dynamic skill coverage:
`WayangPlatformReadiness` now includes a dedicated
`wayang.skill-catalog.readiness` component before provider capability readiness.
The component reuses the existing `AgentSkillDiscovery` envelope, reports skill,
category, source, surface, availability, and state facets, and marks the platform
not ready when the dynamic skill catalog is empty, unmatched, or has no active or
preview skills available for runs. Focused SDK tests pin the default skill
catalog and empty-catalog issue shape, and CLI readiness golden fixtures now
show the six-component production readiness aggregate.

Recent SDK platform readiness component extraction:
Skill-catalog and provider-capability readiness logic now live in focused
`WayangSkillCatalogReadiness` and `WayangProviderCapabilityReadiness` classes,
with `WayangPlatformReadiness` reduced to aggregate orchestration plus contract
and standard checks. Platform readiness constants remain compatibility aliases,
and a small package-local ordered attribute helper keeps readiness JSON stable
without duplicating map construction across component modules.

Recent SDK platform readiness full component split:
Contract integrity, contract command coverage, and standard alignment readiness
now also live in dedicated `WayangContractIntegrityReadiness`,
`WayangContractCoverageReadiness`, and `WayangStandardAlignmentReadiness`
classes. `WayangPlatformReadiness` is now a small aggregate façade that delegates
all component assessments to their owners while preserving the existing public
method and readiness-id constants as aliases. Focused tests exercise the
component classes directly and verify the compatibility aliases.

Recent SDK platform readiness component registry:
Platform readiness aggregation now goes through `WayangPlatformReadinessComponents`
and typed `WayangPlatformReadinessComponent` entries instead of a hard-coded
component list inside the aggregate method. The default registry owns component
ordering, duplicate readiness-id validation, and SDK-to-component assessment
bindings, while `WayangPlatformReadiness` remains a tiny compatibility façade.
Focused tests pin the default component order, ordered report assessment, and
duplicate-id rejection without changing the published readiness JSON envelope.

Recent SDK platform readiness component binding validation:
`WayangPlatformReadinessComponent` now rejects component assessors that return no
report or return a report with a readiness id different from the component id.
This keeps custom readiness registries honest as production profiles evolve:
miswired assessors fail fast instead of silently corrupting aggregate ordering or
component summaries. Focused tests pin null-report and readiness-id mismatch
failure messages.

Recent SDK platform readiness registry execution guard:
`WayangPlatformReadinessComponents.assess(...)` now rejects empty or null
component registries before aggregation. The component-list builder remains a
small reusable normalization helper, while the execution path treats a missing
registry as production misconfiguration instead of allowing a zero-component
readiness aggregate to pass by default. Focused tests pin empty and null
assessment-list failures.

Recent SDK platform readiness failure isolation:
Platform readiness aggregate execution now isolates runtime failures from
individual component assessors into deterministic not-ready component reports.
Direct component assessment remains strict, and aggregate assessment still
rejects null reports or readiness-id mismatches as registry wiring errors. This
lets production readiness output surface a broken subsystem without hiding the
rest of the component results. Focused tests pin runtime-failure report shape and
miswired aggregate rejection.

Recent SDK platform readiness execution diagnostics:
`WayangPlatformReadinessExecution` now owns aggregate-time component execution,
runtime-failure conversion, and optional duration diagnostics instead of leaving
that logic on the component binding record. Normal readiness JSON remains
deterministic by default, while setting
`wayang.readiness.includeDurationMillis=true` can attach non-negative
`durationMillis` metadata for production diagnostics. Focused tests pin the
status metadata and opt-in duration path.

Recent SDK platform readiness profile boundary:
`WayangPlatformReadinessProfile` and `WayangPlatformReadinessProfiles` now model
named readiness component selections separately from component registration and
aggregate execution. The default and production profiles keep the existing
six-component readiness contract, while minimal, contracts, and catalogs profiles
provide focused subsets for future deployment-specific checks. SDK overloads can
assess by profile id or custom profile, and focused tests pin profile selection
and unknown component/profile rejection.

Recent CLI platform readiness profile selection:
`status --readiness-profile <profile-id>` now renders a selected platform
readiness profile and implies readiness mode, while `status --readiness` keeps
the default production aggregate. The workbench and contract catalogs expose the
profile-specific readiness JSON command so command discovery and contract
coverage stay aligned. CLI tests pin minimal-profile JSON, profile-implied text
output, and unknown-profile failure handling.

Recent readiness profile catalog discovery:
`WayangPlatformReadinessProfileDescriptor` and
`WayangPlatformReadinessProfileCatalog` now expose built-in readiness profiles
as stable metadata instead of leaving profile semantics hidden behind CLI help
text. The SDK facade can list descriptors, the platform envelope publishes a
`readiness-profile-list` JSON contract, and `readiness-profiles --json` gives
operators and product shells a discoverable profile catalog with component
bindings. Focused tests pin SDK descriptors, CLI text/JSON rendering, workbench
catalog coverage, and golden schema validation.

Recent readiness aggregate profile traceability:
Platform readiness aggregates now include `readinessProfileId`,
`readinessProfileDefault`, `readinessProfileProduction`, and
`readinessProfileComponentIds` attributes. The generic aggregate factory accepts
context attributes while preserving component-count ownership, so deployment
gates and dashboards can identify which readiness profile produced a report
without inferring it from component lists. CLI text output also surfaces the
profile id, and focused tests pin generic aggregation, platform metadata,
schema discovery, and golden readiness JSON.

Recent readiness profile detail inspection:
`readiness-profiles inspect <profile-id>` now renders one platform readiness
profile as text or JSON without forcing product shells to fetch and filter the
whole profile list. The platform contract catalog publishes a dedicated
`readiness-profile-detail` envelope, and the workbench catalog links the
`readiness-profiles-inspect-json` command for discovery and coverage checks.
Focused tests pin CLI text/JSON behavior, unknown-profile handling, schema
generation, command catalog wiring, and golden payload validation.

Recent readiness profile error diagnostics:
Unknown readiness profile errors now include the available built-in profile ids
from the central SDK resolver. Both `status --readiness-profile ...` and
`readiness-profiles inspect ...` inherit the same message, keeping CLI behavior
consistent while avoiding duplicate validation logic. Focused tests pin the SDK
exception and both CLI failure paths.

Recent readiness profile validation gate:
`WayangPlatformReadinessProfileValidation` now validates the built-in readiness
profile catalog against the central platform readiness component registry,
surfacing duplicate profiles and unknown readiness component bindings as
structured issues. The SDK facade exposes the validation report, and
`readiness-profiles --check` publishes a JSON/text production gate with
workbench catalog, contract catalog, schema, and golden-payload coverage.

Recent readiness profile semantic validation:
The readiness profile validation report now exposes default-profile,
production-profile, covered-readiness, and uncovered-readiness diagnostics.
Validation requires exactly one default profile, exactly one production profile,
and full coverage of known platform readiness components, giving future external
or configurable profile catalogs a stronger production preflight before they
are used by status gates or dashboards.

Recent readiness profile validation policy:
`WayangPlatformReadinessProfileValidationPolicy` now separates validation
strictness from validation execution. Built-in profile checks keep strict
production semantics, while custom catalogs can validate against external known
readiness ids with relaxed or selectively re-enabled requirements. The SDK
facade exposes the policy-aware validation overload so future file, database,
or cloud-backed profile catalogs can reuse the same validator boundary.

Recent readiness profile policy traceability:
Readiness profile validation reports now carry a compact validation-policy
summary with strictness, known readiness count, and active requirement flags.
The CLI text output and JSON contract both expose that summary, making future
custom profile catalogs auditable without requiring operators to infer which
validation rules were enabled from the issue list alone.

Recent readiness profile named validation policies:
Readiness profile validation policies now carry a normalized `policyId`, with
stable built-in ids for strict and relaxed modes plus derived ids for toggled
requirements. The report summary, CLI output, and validation JSON contract all
publish the id, giving production logs and dashboards a stable key for the
policy preset used by built-in or external readiness profile catalogs.

Recent readiness profile validation policy selection:
`WayangPlatformReadinessProfileValidationPolicies` now centralizes supported
policy ids and unknown-policy diagnostics. The SDK facade can validate built-in
readiness profiles by policy id, and `readiness-profiles --check` accepts
`--validation-policy` so operators can run strict, relaxed, or partially strict
profile checks without duplicating validation-policy construction in shells.

Recent readiness profile validation policy discovery:
Validation policies are now discoverable as SDK descriptors and as a dedicated
`readiness-profile-validation-policy-list` platform contract. The
`readiness-profiles policies` command renders human-readable policy metadata,
while `readiness-profiles policies --json` publishes ids, descriptions,
default-policy markers, strictness, and active validation requirements for
operators, dashboards, and future external profile catalogs.

Recent readiness profile registry boundary:
Readiness profiles now have a source and registry layer above the built-in
catalog. The default source remains the built-in SDK catalog, while a
properties-based file source can load versioned external profile documents and
fall back to built-ins when the source is unavailable or fails strict
validation. Registry resolution reports expose the active source, fallback
state, per-source status, selected profiles, and validation report, keeping the
path open for database and S3/RustFS-compatible object-storage sources.

Recent readiness profile registry resolution contract:
`readiness-profiles sources` now exposes the profile registry resolution as a
human-readable report, and `readiness-profiles sources --json` publishes the
same shape as the `readiness-profile-registry-resolution` platform contract.
The command can resolve the default built-in source or test a properties file
with built-in fallback, making active source, fallback state, source validity,
selected profiles, and validation policy visible to operators and automation.

Recent readiness profile registry configuration:
`WayangPlatformReadinessProfileRegistryConfig` now normalizes built-in, file,
database, object-storage/S3/RustFS/MinIO, and hybrid registry modes at the SDK
boundary. `LocalWayangGollekSdk` uses the configured registry for profile
listing, validation, registry-source diagnostics, and bare platform readiness,
so file-backed catalogs can define their own default profile without depending
on the built-in `default` id. The CLI exposes matching root options and
environment variables for registry mode, file path, database URL, object-storage
endpoint/bucket/prefix/provider, built-in fallback, and validation policy.

Recent readiness profile registry failure readiness:
Unavailable or invalid configured readiness profile registries now produce a
blocked `wayang.platform.readiness` report instead of throwing while resolving
the default profile. The new registry-readiness component records active source,
fallback state, validation policy, source probes, and source/validation issues,
so `status --readiness` remains a dependable production gate even when a file,
database, or object-storage catalog is misconfigured.

Recent readiness profile registry config diagnostics:
Readiness profile registry configuration now has explicit diagnostics for
missing required file/database/object-storage fields and unknown validation
policy ids. `LocalWayangGollekSdk` checks those diagnostics before resolving
profile sources, and invalid config now becomes a dedicated readiness component
instead of an SDK construction failure. This keeps CLI root configuration
preflightable and makes bad policy or storage settings visible in readiness JSON.

Recent readiness profile registry config CLI:
`readiness-profiles config` now renders the effective readiness profile registry
configuration diagnostics as text or JSON. Operators can check selected mode,
fallback behavior, validation policy, file/database/object-storage settings, and
config issues directly from the same root options/environment that the SDK uses,
without running a full platform readiness aggregate.

Recent readiness profile registry config contract:
`readiness-profiles config --json` is now published as the
`readiness-profile-registry-config-diagnostics` platform contract with schema,
workbench discovery, command coverage, and golden fixture validation. Automation
can depend on the config diagnostics envelope instead of treating it as an
incidental CLI payload.

Recent readiness profile object-storage source boundary:
Object-storage readiness profile registries now resolve through a dedicated
`WayangPlatformReadinessProfileObjectStorageSource` and pluggable
`WayangPlatformReadinessProfileObjectReader`. The SDK core stays dependency
light while S3/RustFS/MinIO adapters can inject their own reader at
`LocalWayangGollekSdk` construction time. Missing readers and reader failures
remain normal registry source statuses, so configured built-in fallback keeps
production readiness checks deterministic.

Recent readiness profile object-storage service bridge:
`wayang-gollek-sdk-storage` now provides an optional bridge from the neutral
`ObjectStorageService` SPI into the readiness profile object-reader contract.
`WayangReadinessProfileObjectStorageServiceReader` reads the configured logical
profile object key with bounded timeout, UTF-8 defaults, missing-object
diagnostics, and optional object-key/charset overrides. S3/RustFS/MinIO-backed
storage providers can now feed platform readiness profiles without making the
SDK core depend on AWS or provider-specific clients.

Recent readiness profile object-storage key hardening:
Object-storage readiness profile configuration now accepts object-key aliases
(`objectKey`, `key`, `profileObjectKey`, and the existing `keyPrefix`) and
requires a non-blank key for object-storage registries. CLI options now expose
`--readiness-profile-object-key` while preserving the legacy
`--readiness-profile-object-prefix` alias and environment fallback. Invalid
object-storage configs fail during registry diagnostics instead of waiting for
the object reader to discover an empty key at runtime.

Recent readiness profile object-storage service resolver:
`wayang-gollek-sdk-storage` now includes a resolver boundary and immutable
service registry for readiness profile object-storage reads. The bridge reader
can route by `credentialsRef`, fall back to provider ids such as `s3`, `rustfs`,
or `minio`, and finally use a default service when present. Existing
single-service construction remains available, while multi-tenant or
multi-provider deployments can now inject one config-driven reader instead of
rebuilding the SDK for each storage backend.

Recent readiness profile object-storage resolution diagnostics:
`WayangReadinessProfileObjectStorageServiceRegistry` now exposes a
`resolveReport(...)` preflight for object-storage service selection. The report
records credentials reference, provider, selected service id, selection reason,
available service ids, availability, and a human-readable message. The bridge
reader reuses the same resolution path, so diagnostics and object reads cannot
drift as provider, credentials-ref, and default-service routing evolve.

Recent readiness profile S3-compatible reader adapter:
`wayang-gollek-sdk-storage-s3` now provides an optional factory for
S3-compatible readiness profile readers. It maps `WayangObjectStorageConfig`
plus access-key credentials into the existing AWS SDK-backed
`S3ObjectStorageService`, supporting AWS S3, RustFS, and MinIO-style path access
without pulling provider clients into the SDK core or the neutral storage bridge.
Focused tests pin S3/RustFS config translation, default region handling,
credential validation, and reader/service construction without live network IO.

Recent readiness profile S3 credentials resolver:
The optional S3-compatible readiness profile adapter now supports
config-driven credential resolution. `WayangReadinessProfileS3CredentialsRegistry`
routes credentials by `credentialsRef`, provider id, or default credentials, and
publishes a compact resolution report for preflight diagnostics. The reader and
service factory can now accept a resolver directly, letting multi-tenant
RustFS/MinIO/S3 deployments select credentials from config instead of rebuilding
adapter instances per credential set.

Recent readiness profile S3 credential sources:
S3-compatible readiness profile credentials can now be assembled from
redacting credential sources before entering the resolver registry.
`WayangReadinessProfileS3CredentialSource` supports readiness-specific
environment/property aliases, the neutral storage-S3 aliases, AWS environment
fallbacks, and MinIO root-user fallbacks. Sources publish operator diagnostics
with source type, selected keys, presence flags, availability, and messages
without exposing secret values. The registry can build from available sources
while preserving diagnostics for incomplete ones, keeping production bootstrap
observable without leaking credentials.

Recent readiness profile object-storage preflight:
`wayang-gollek-sdk-storage` now exposes
`WayangReadinessProfileObjectStoragePreflight` and an immutable preflight
report for operator checks before SDK bootstrap. The preflight composes registry
configuration diagnostics, object-storage service resolution, bounded object
reads, document byte/character sizing, and readiness-profile registry validation
into one report with `ready`, `exitCode`, profile ids, source diagnostics, and
human-readable issues. This keeps live S3/RustFS/MinIO smoke commands and
runtime health probes on a shared SDK boundary instead of duplicating object
read and profile parsing logic in each integration.

Recent readiness profile S3-compatible preflight:
`wayang-gollek-sdk-storage-s3` now composes redacted S3 credential resolution
with the neutral object-storage preflight. The new
`WayangReadinessProfileS3ObjectStoragePreflight` resolves credentials through
registry or external resolvers, preserves credential-source diagnostics without
secret values, creates the S3-compatible object-storage service, and delegates
object reads plus readiness-profile validation to the shared storage preflight.
The report exposes credential resolution, source diagnostics, service creation,
object/profile diagnostics, issues, `ready`, and `exitCode` for future live
S3/RustFS/MinIO smoke commands and health endpoints.

Recent readiness profile database reader boundary:
The SDK core now supports pluggable database-backed readiness profile sources.
`WayangPlatformReadinessProfileDatabaseReader` keeps JDBC or database-client
dependencies outside the core, while
`WayangPlatformReadinessProfileDatabaseSource` parses the returned properties
document through the same readiness profile document model as file and
object-storage sources. `WayangPlatformReadinessProfileExternalReaders`
composes database and object-storage readers without ambiguous lambda
constructors, and database source locations redact common secret query
parameters before surfacing diagnostics.

Recent readiness profile external reader provider discovery:
The local SDK now discovers optional readiness profile reader providers through
`ServiceLoader`. `WayangPlatformReadinessProfileExternalReaderProvider`
allows database, object-storage, S3/RustFS, or deployment-specific modules to
contribute readers without adding those dependencies to the SDK core.
`WayangPlatformReadinessProfileExternalReaders` now merges explicit and
discovered readers deterministically: explicit constructor readers stay highest
priority, while discovered providers can fill missing database or object-storage
slots. This keeps production wiring configurable and classpath-extensible while
preserving file and built-in fallback behavior.

Recent readiness profile object-storage service provider:
`wayang-gollek-sdk-storage` now includes a concrete ServiceLoader bridge from
initialized provider-neutral `ObjectStorageService` instances into the SDK
readiness profile reader SPI. Deployments can register
`WayangReadinessProfileObjectStorageServiceProvider` implementations that
contribute services by provider id, credential reference, or default id.
`WayangReadinessProfileObjectStorageExternalReaderProvider` adapts the
discovered service registry into `WayangPlatformReadinessProfileExternalReaders`
so local SDK construction can load object-storage readiness profiles without
manual constructor wiring. Duplicate service ids are resolved deterministically
by provider priority, and missing or failing optional providers leave the SDK on
its normal fallback path.

Recent readiness profile object-storage provider diagnostics:
`WayangReadinessProfileObjectStorageServiceProviders` now exposes aggregate
provider diagnostics alongside registry construction. Each provider can publish
a redacted diagnostic map through
`WayangReadinessProfileObjectStorageServiceProvider.diagnostics(...)`, and the
neutral
`WayangReadinessProfileObjectStorageServiceProviderDiagnostics` wrapper adds
provider id, priority, availability, service count, service ids, message, and
details. Explicit `available=false` reports now take precedence over incidental
target service ids, so unavailable provider diagnostics cannot be mistaken for
created services. `WayangReadinessProfileObjectStorageServiceDiscoveryReport`
wraps those provider diagnostics into a stable aggregate report with
`available`, `exitCode`, provider counts, available-provider counts, discovered
service ids, service count, provider maps, and an operator-facing message for
future CLI or API surfaces.

Recent S3-compatible readiness profile service provider:
`wayang-gollek-sdk-storage-s3` now registers
`WayangReadinessProfileS3ObjectStorageServiceProvider` as an
`ObjectStorageService` provider for the neutral storage bridge. The provider
supports `s3`, `aws-s3`, `rustfs`, and `minio` object-storage configs, resolves
credentials from the existing redacted S3 credential source aliases
(environment variables or system properties), and initializes the S3-compatible
storage service only when the readiness profile bucket and object key are
configured. Services are registered by credential reference when present,
otherwise by provider id, preserving the object-storage registry selection
rules while keeping live cloud/RustFS wiring optional and classpath-driven.
`WayangReadinessProfileS3ObjectStorageServiceProviderReport` now exposes the
same provider evaluation as an operator-facing diagnostic map, including
supported-provider checks, object-config completeness, redacted credential
source diagnostics, credential resolution, service creation status, service id,
issues, `available`, and `exitCode`. This makes ServiceLoader fallback paths
observable without requiring a live S3/RustFS read.

Recent A2UI action binding report transport:
Action policy/handler binding diagnostics are now reachable through the
transport boundary via `ACTION_BINDING_REPORT`,
`WayangA2uiTransportRequest.actionBindingReport()`, and
`WayangA2uiTransportAdapter.exchangeActionBindingReport()`. The response uses
the canonical JSON transport envelope, publishes `action-binding-report`
metadata with policy/handler/missing/orphan counts, and remains accessible to
HTTP integrations through the existing `/a2ui/exchange` route without expanding
the HTTP route catalog.

Recent A2UI action binding report decoding:
`WayangA2uiActionBindingReportDecoder` now provides canonical
`fromMap`, `fromJson`, transport-response, and HTTP-response decoding for
action policy/handler diagnostics. The report facade delegates through those
decoders and can round-trip with `toJson()`, keeping remote, stored, and
bridge-sourced reports out of ad hoc map parsing.

Recent A2UI action binding readiness:
A2UI HTTP readiness now includes an action-binding probe alongside route
binding and smoke probes. `WayangA2uiHttpActionBindingProbeResult` exercises the
existing `/a2ui/exchange` transport envelope, verifies the action binding report
response, and fails readiness when the active action policy allows an action
without a registered handler. Orphan handlers remain visible in probe counts
and lists, but do not fail readiness because the action gate prevents
policy-disallowed actions from dispatching.

Recent A2UI HTTP operational diagnostics:
`WayangA2uiHttpOperationalDiagnostics` now gives adapter, CLI, REST, and harness
callers one typed diagnostics object for route binding, action binding, smoke,
readiness, and standard readiness views. The facade is backed by a single
readiness run, so callers get consolidated diagnostics without duplicating probe
traffic or keeping multiple probe results in sync by hand.

Recent A2UI HTTP operational diagnostics summary:
Operational diagnostics now include
`WayangA2uiHttpOperationalDiagnosticsSummary`, a compact JSON-ready summary for
operator surfaces and external adapters. The summary captures pass/fail state,
exit code, issue codes, route binding counts, action binding counts, smoke
counts, and readiness identity, with a golden contract fixture to pin the
payload independently from the full drill-down diagnostics object.

Recent A2UI HTTP header value normalization:
`HttpHeaderValues` now owns recursive coercion for flexible framework
header values, including optionals, iterables, arrays, blanks, and nulls.
Endpoint binding still owns request-header map projection and endpoint response
still owns multi-value response headers, but both now share the same value
flattening rules.

Recent A2UI proportional helper naming:
Package-local common helpers now use direct role names such as
`RecordCollections`, `DecodeValues`, `DecodeCollections`, `StringMaps`,
`ProjectionCollections`, and `HttpHeaderValues`. Public Wayang/A2UI adapters,
records, and integration-facing contracts keep their `WayangA2ui` prefix, so
external API names remain explicit while internal helper names stay readable.

Recent A2UI action helper naming:
Package-local action implementation helpers now follow the same proportional
naming rule: `ActionContextReader`, `ActionQueries`, `ActionGate`,
`ActionResponses`, `ActionMetadata`, and `ActionBindingReportProjection` carry
their direct role names. Public action records, handlers, routers, factories,
and decoders keep `WayangA2uiAction*` names for integration-facing clarity.

Recent A2UI action binding projection move:
`ActionBindingReportProjection` now lives in
`tech.kayys.wayang.a2ui.wayang.action`. The branded action binding report
record keeps its root API name while delegating ordered policy/handler coverage
maps into the action boundary. Boundary placement, coverage, and direct
projection tests now pin the helper as an action-first projection before the
generic projection fallback.

Recent A2UI action metadata projection move:
`ActionMetadata` now lives in `tech.kayys.wayang.a2ui.wayang.action` beside
the other action-local projections. The ordered inspect/history/events/wait/
cancel metadata maps are now owned by the action boundary and pinned by a
direct package-local test plus the boundary coverage checks.

Recent A2UI action response assembly move:
`ActionResponses` now lives in `tech.kayys.wayang.a2ui.wayang.action` beside
`ActionMetadata`. Root action handlers still orchestrate SDK calls, while the
action boundary now owns the conversion from SDK lifecycle responses into
handled `WayangA2uiActionResult` payloads. Boundary placement and response
tests pin this helper as an action-local implementation role.

Recent A2UI action context/query move:
`ActionContextReader` and `ActionQueries` now live in
`tech.kayys.wayang.a2ui.wayang.action`. Session continuity and root action
handlers still call them directly, but inbound user-action context parsing and
SDK query/option construction are now owned by the action boundary. Dedicated
tests moved with the helpers, and boundary coverage pins both as action-local
implementation roles.

Recent A2UI action gate move:
`ActionGate` now lives in `tech.kayys.wayang.a2ui.wayang.action` beside the
context reader it uses. The public root router still owns inbound routing, but
admission-policy evaluation and the accepted/rejected decision shape are now
inside the action boundary. The gate test moved with the helper and boundary
coverage pins it as an action-local implementation role.

Recent A2UI surface id helper move:
`SurfaceIds` now lives in `tech.kayys.wayang.a2ui.wayang.surface`. Root
renderers still assemble the current run/status/history/event views, but stable
surface-id normalization has moved into the surface boundary. Its direct test
moved with it, and boundary coverage now pins the surface package's first
helper as public and local to the surface module.

Recent A2UI surface layout helper move:
`SurfaceLayouts` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
Root renderers still build the current domain surfaces, but mutable component
lists, child lists, and root-column insertion now belong to the surface
boundary. Its direct test moved with it, and boundary coverage pins it beside
`SurfaceIds`.

Recent A2UI surface text helper move:
`SurfaceText` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
Run-history, run-event, and data-model assembly still call it from the current
root renderers, but surface copy, fallback messages, and line formatting now
belong to the surface boundary. Its test moved with local SDK samples so the
surface package does not depend on root test fixtures.

Recent A2UI surface message helper move:
`SurfaceMessages` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
Root renderers still decide which components and data models to emit, but the
standard data-model update, surface update, and begin-rendering sequence now
belongs to the surface boundary. Its direct test moved with the helper, and
boundary coverage pins it beside the other moved surface helpers.

Recent A2UI surface action helper move:
`SurfaceActions` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
Root run surfaces still choose which controls to display, while option
fallback, button assembly, and A2UI action context construction belong to the
surface boundary. Its focused unit test moved with the helper, and boundary
coverage pins it beside the other moved surface helpers.

Recent A2UI surface data helper move:
`SurfaceData` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
Run data-model assembly still chooses top-level status/event/history entries,
while reusable run rows, event rows, and count entries belong to the surface
boundary. Its focused unit test moved with self-contained SDK samples, and
boundary coverage pins it beside the other moved surface helpers.

Recent A2UI surface projection helper move:
`SurfaceProjection` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
Public descriptor and catalog records still expose their `toMap()` methods,
while ordered descriptor/catalog map assembly belongs to the surface boundary.
Its focused unit test moved with the helper, and boundary coverage pins it
beside the other moved surface helpers.

Recent A2UI run data-model helper move:
`RunDataModels` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
Root run renderers still decide which surface to compose, while status/event/
history data-model entry assembly belongs to the surface boundary. Its focused
unit test moved with self-contained SDK samples, and boundary coverage pins it
beside the other moved surface helpers.

Recent A2UI run action-control helper move:
`RunActionControls` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
Root status and history renderers still choose where controls appear, while
shared inspect/events/wait/cancel button selection and labeling belongs to the
surface boundary. Its focused unit test moved with self-contained SDK samples,
and boundary coverage pins it beside the other moved surface helpers.

Recent A2UI run status renderer move:
`RunStatusSurface` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
The public `WayangA2uiSurfaces` facade and inspection composition still
delegate to it, while single-run status layout, data-model wiring, and action
control placement now belong to the surface boundary. Its focused unit test
moved with self-contained SDK samples and a local JSONL parser, and boundary
coverage pins it beside the other moved surface renderers.

Recent A2UI run events renderer move:
`RunEventsSurface` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
The public `WayangA2uiSurfaces` facade and inspection composition still
delegate to it, while event-stream layout, refresh-action wiring, and event
data-model wiring now belong to the surface boundary. Its focused unit test
moved with self-contained SDK samples and a local JSONL parser, and boundary
coverage pins it beside the other moved surface renderers.

Recent A2UI run history renderer move:
`RunHistorySurface` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
The public `WayangA2uiSurfaces` facade still delegates to it, while history
page layout, per-run row composition, action-control placement, and history
data-model wiring now belong to the surface boundary. Its focused unit test
moved with self-contained SDK samples, and moved surface renderer tests now
share a package-local JSONL parser helper.

Recent A2UI run inspection renderer move:
`RunInspectionSurface` now lives in `tech.kayys.wayang.a2ui.wayang.surface`.
The public `WayangA2uiSurfaces` facade still delegates to it, while inspection
composition across status and event surfaces now belongs to the surface
boundary. Its focused unit test moved with self-contained SDK samples and uses
the shared surface JSONL parser helper.

Recent A2UI session profile helper extraction:
`SessionProfiles` now lives in `tech.kayys.wayang.a2ui.wayang.session` and owns
mode aliases, profile action-policy selection, and profile config creation.
The public `WayangA2uiSessionProfiles` class remains as a compatibility facade,
while `WayangA2uiSessionConfig` and config decoding now delegate directly to
the session boundary helper. Boundary coverage now pins the first concrete
session package helper.

Recent A2UI session config decoder boundary move:
`SessionConfigDecoder` now lives in `tech.kayys.wayang.a2ui.wayang.session` and
owns stored or remote session config map and JSON decoding. The public
`WayangA2uiSessionConfigDecoder` class remains as a compatibility facade, while
`WayangA2uiSessionConfig` delegates directly to the session boundary helper.
Boundary coverage now pins the decoder beside `SessionProfiles`.

Recent A2UI session config source abstraction:
`SessionConfigSource` and `SessionConfigSources` now provide a configurable
session config loading boundary for inline JSON, files, classpath resources,
and future adapter-backed stores such as database or S3/RustFS-compatible
object storage. `WayangA2uiSessionConfig.fromSource(...)` and session
constructors can now load through this boundary while defaulting safely when no
source provides config JSON.

Recent A2UI declarative session config source registry:
`SessionConfigSourceRegistry` and `SessionConfigSourceProvider` now turn
declarative source specs into loadable session config sources. The standard
registry supports inline JSON, files, classpath resources, and fallback chains,
while external modules can register provider names such as `database`, `s3`,
or `rustfs` without adding those storage clients to the A2UI core module.

Recent A2UI request-level session config source wiring:
`WayangA2ui` now accepts a `sessionConfigSource` context entry beside direct
`sessionConfig`. Direct config still wins, while source specs can be resolved
through the standard source registry or a caller-supplied registry for
database, S3, RustFS, or tenant-specific providers.

Recent A2UI structured session config source specs:
`SessionConfigSourceSpec` now provides typed factories for inline JSON, file,
classpath, provider-backed, and fallback session config sources. The registry
still accepts plain maps for transport compatibility, while Java callers can
build source specs without hand-assembling source keys.

Recent A2UI session config source load diagnostics:
`SessionConfigLoadResult` and `SessionConfigLoadStatus` now expose whether a
session config source loaded JSON, was missing, or failed to decode. Simple
`load()` and `loadOrDefault()` behavior remains intact, while diagnostics can
report selected provider descriptions for fallback chains.

Recent A2UI request-level session config diagnostics:
`WayangA2ui.sessionConfigLoadResult(...)` now reports direct request config,
source-spec loading, missing config, and source resolution failures without
changing the existing `sessionConfig(...)` optional API. This makes request
context diagnostics visible to callers that need to explain config origin.

Recent A2UI session config load result projection:
`SessionProjection.loadResult(...)` and `SessionConfigLoadResult.toMap()/toJson()`
now provide a deterministic diagnostic envelope with source description, status,
loaded flag, projected config, and message. This keeps operational reporting
stable without duplicating projection code at request or provider call sites.

Recent A2UI session config source spec validation:
`SessionConfigSourceSpec` now validates built-in source specs before load time:
inline specs require JSON, file specs require a path, classpath specs require a
resource, and fallback chains require nested source objects. The registry also
validates raw map specs before provider resolution, while provider-backed specs
remain storage-neutral for database, S3, RustFS, and tenant-specific adapters.

Recent A2UI surface renderer naming:
Package-local surface renderer helpers now use direct rendering roles:
`RunStatusSurface`, `RunEventsSurface`, `RunHistorySurface`,
`RunInspectionSurface`, `RunActionControls`, `RunDataModels`, `SurfaceIds`,
`SurfaceActions`, `SurfaceLayouts`, `SurfaceMessages`, `SurfaceText`,
`SurfaceData`, and `SurfaceProjection`. Public facades such as
`WayangA2uiSurfaces`, `WayangA2uiSurfaceRegistry`, descriptors, catalogs, and
options keep the Wayang/A2UI prefix because they are integration-facing types.

Recent A2UI HTTP response decoder naming:
Package-local HTTP response-envelope decoders now use direct HTTP role names:
`HttpResponseBodyDecoder`, `HttpReadinessProbeResponseDecoder`,
`HttpSmokeProbeResponseDecoder`, `HttpBindingReportProbeResponseDecoder`, and
`HttpActionBindingProbeResponseDecoder`. Public result decoders and HTTP
records keep their `WayangA2uiHttp*` names because callers use them as
integration-facing contracts.

Recent A2UI HTTP projection helper naming:
Package-local HTTP map/JSON projection helpers now use direct HTTP role names,
including `HttpRouteProjection`, `HttpEndpointProjection`,
`HttpEndpointDiagnosticProjection`, `HttpEndpointDiagnosticPlanProjection`,
`HttpReadinessProbeProjection`, `HttpSmokeProbeProjection`,
`HttpBindingReportProbeProjection`, `HttpActionBindingProbeProjection`,
`HttpPublicationProjection`, `HttpExpectationProjection`,
`HttpScenarioProjection`, `HttpBindingReportProjection`,
`HttpSmokeResultProjection`, and `HttpOperationalDiagnosticsProjection`.
Public HTTP records and facades keep the `WayangA2uiHttp*` prefix.

Recent A2UI projection ordering normalization:
`ProjectionCollections` now owns null-entry cleanup for sorted string
projections and reference-order projections. Action and HTTP binding reports
delegate nullable handler lists to that helper instead of carrying local
null/empty guards in their binding comparison code.

Recent A2UI singleton list normalization:
`RecordCollections.singletonOrEmpty` now owns nullable scalar-to-list
normalization. Permissive decode collections and strict HTTP report metric
decoders use the shared helper after they parse integer values, keeping parsing
policy separate from immutable list construction.

Recent A2UI endpoint diagnostic projection normalization:
Endpoint diagnostic exchange and issue projections now share
`RecordCollections.nonNullList` with the diagnostic result model.
Direct projection calls therefore ignore accidental null exchange entries
consistently instead of keeping local null-list fallbacks in the projection.

Recent A2UI metric list normalization:
HTTP exchange, transport exchange, and scenario-suite metric helpers now reuse
`RecordCollections.nonNullList` for nullable aggregate inputs,
including accidental null entries. HTTP report transport digests also snapshot
their status-code and outcome lists through `RecordCollections.copyList`
so caller-owned list mutation cannot drift projected report payloads.

Recent A2UI probe issue list normalization:
Action-binding and binding-report probe issue projections now delegate nullable
diagnostic action/operation lists to `RecordCollections.nonNullList`.
This removes duplicate local null-list adapters and prevents malformed null
entries from becoming issue payloads while preserving projection order.

Recent A2UI HTTP issue helper consolidation:
Issue-value extraction for endpoint diagnostic summaries and operational
diagnostics summaries now lives in `HttpIssueMaps`. This keeps
projection helpers focused on projection shape and prevents operational
diagnostics from depending on endpoint-specific projection internals.

Recent A2UI smoke fallback ownership:
The empty smoke summary and smoke probe fallback now live on
`WayangA2uiHttpSmokeSummary.empty()` and `WayangA2uiHttpSmokeProbeResult.empty()`.
Readiness composition uses those factories instead of constructing nested smoke
fallback state directly, keeping fallback ownership with the smoke probe model.

Recent A2UI readiness fallback ownership:
Empty binding-report and readiness fallback states are now explicit through
`WayangA2uiHttpBindingReportProbeResult.empty()` and
`WayangA2uiHttpReadinessProbeResult.empty()`. Stored or empty operational
diagnostics payloads route through those factories instead of relying on empty
map decoding as an implicit fallback.

Recent A2UI operational diagnostics fallback ownership:
`WayangA2uiHttpOperationalDiagnostics.empty()` now names the empty diagnostics
fallback state directly. Empty-map decoders and null-summary inputs use that
factory, keeping fallback construction centralized at the diagnostics model.

Recent A2UI binding operation ownership:
`WayangA2uiHttpBindingReportProbeResult` now owns route-operation membership
checks through `hasRouteOperation(...)` and `requiresSmokeProbe()`. Readiness
composition asks the binding probe whether the smoke probe is required instead
of inspecting the raw route-operation list itself, preserving separation between
binding-report interpretation and readiness orchestration.

Recent A2UI operational diagnostics summary fallback ownership:
`WayangA2uiHttpOperationalDiagnosticsSummary.empty()` now names the empty
summary state directly. Null summary inputs and empty stored summary maps route
through the same diagnostics-empty projection, so compact operator summaries no
longer drift from the full operational diagnostics fallback.

Recent A2UI endpoint diagnostic fallback ownership:
Endpoint diagnostic reports and summaries now expose explicit `empty()`
factories. Null report summaries and empty stored report/summary maps route
through those factories, preserving the zero-exchange no-issue fallback without
requiring callers or decoders to rely on implicit empty-map construction.

Recent A2UI binding report fallback construction:
`WayangA2uiHttpBindingReportProbeResult.empty()` now constructs the fallback
value directly instead of delegating through empty-map decoding. The decoder
still accepts empty stored maps, but routes them through the named factory so
binding fallback semantics stay owned by the probe result model.

Recent A2UI action binding compatibility fallback:
`WayangA2uiHttpActionBindingProbeResult.compatibilityFallback()` now names the
passed action-binding probe state used for older readiness payloads that do not
include action-binding diagnostics. Readiness composition and decoding use the
new factory, while the older package-local helper remains as a compatibility
alias.

Recent A2UI action binding empty fallback:
`WayangA2uiHttpActionBindingProbeResult.empty()` now names the failed
unpopulated action-binding probe state directly. Empty stored probe maps decode
through that factory, keeping direct probe fallback separate from the passing
readiness compatibility fallback for legacy payloads.

Recent A2UI transport error default ownership:
`WayangA2uiTransportError.defaultError()` now owns the canonical fallback
transport error. Transport error decoding and null error-response construction
route through the named factory instead of reconstructing the default code and
message in adapter helpers.

Recent A2UI session config default ownership:
`WayangA2uiSessionConfig.defaultConfig()` now names the inspect-only fallback
session configuration. Session config decoding, null request-context storage,
and session constructor defaults route through that factory instead of spelling
the inspect-only fallback inline.

Recent A2UI action policy default ownership:
`WayangA2uiActionPolicy.defaultPolicy()` now names the inspect-only action
policy fallback. Action gates, routers, binding reports, session config
construction, and profile default resolution use the named factory instead of
embedding inspect-only as an implicit null/default behavior.

Recent A2UI transport/metric helper naming:
Package-local transport and HTTP metric helpers now use proportional role names:
`TransportJson`, `TransportMaps`, `TransportMetricExchange`,
`TransportExchangeMetrics`, `HttpMetricExchange`, `HttpExchangeMetrics`,
`HttpIssueMaps`, `HttpReportMetrics`, `HttpReportMetricDecoders`, and
`HttpScenarioSuiteMetrics`. Public A2UI contracts keep their branded
`WayangA2ui*` names, while local helper names now stay compact and focused.

Recent A2UI projection helper naming:
The remaining package-local projection helpers now use proportional role names:
`SessionProjection`, `TransportProjection`, `TransportMetadataProjection`, and
`SpecAlignmentProjection`. Public model and contract records still keep their
`WayangA2ui*` API names, while implementation helpers now read as local roles.

Recent A2UI module boundary skeleton:
`WayangA2uiModuleBoundary` now names the intended package split for action,
bridge, HTTP, projection, session, spec, surface, transport, and support
concerns. Package-level descriptors reserve each target package before large
class moves begin, and boundary tests pin the skeleton plus the public-branded
versus package-local helper naming convention.

Recent A2UI boundary placement guardrail:
`WayangA2uiBoundaryPlacement` now classifies the current source-compatible
classes into the target module boundaries before physical package moves begin.
It pins the support-first migration set, makes projection helpers win over
domain words in class names, and keeps HTTP probe/diagnostic models under the
HTTP boundary even when names include action or transport vocabulary.

Recent A2UI boundary coverage guardrail:
`WayangA2uiBoundaryCoverageTest` now scans the current root A2UI package and
requires every source-compatible type to have a target boundary classification.
It also keeps compact helper names package-private until an intentional package
move exposes a boundary, while public types remain on the branded
`WayangA2ui*` API surface.

Recent A2UI support package move:
The first physical A2UI package split moved `DecodeCollections`, `DecodeValues`,
`ProjectionCollections`, `RecordCollections`, `RecordMaps`, `RecordNumbers`,
`RecordValues`, and `StringMaps` into `tech.kayys.wayang.a2ui.wayang.support`.
Those helpers are public from the support boundary so the still-rooted action,
HTTP, projection, session, surface, and transport code can import them during
the staged migration.

Recent A2UI projection package seed:
The projection boundary now contains the first moved projection helpers:
`SessionProjection`, `SpecAlignmentProjection`, and package-local
`ProjectionMaps`. Session config, action policy, and spec-alignment public
records still keep their `WayangA2ui*` API names while delegating ordered map
assembly into `tech.kayys.wayang.a2ui.wayang.projection`. Boundary coverage now
pins the moved projection package shape so later HTTP and transport projection
moves can be staged without widening root-package helper visibility.

Recent A2UI transport helper package seed:
The transport boundary now owns the shared `TransportMaps` and `TransportJson`
helpers under `tech.kayys.wayang.a2ui.wayang.transport`. They are public from
that boundary so root A2UI records, HTTP diagnostics, surface projections, and
tests import transport map-copy and JSON codec behavior explicitly instead of
relying on package-local root access. Boundary coverage now pins those moved
transport helpers while the heavier transport metadata/projection helpers remain
rooted until their HTTP coupling can be split cleanly.

Recent A2UI transport envelope projection move:
`TransportProjection` now lives beside `TransportMaps` and `TransportJson` in
`tech.kayys.wayang.a2ui.wayang.transport`. The branded public envelope and
transport-error records keep the `WayangA2ui*` API while delegating ordered
request, response, and error-body map assembly into the transport boundary.
Boundary placement now treats this transport-owned projection as transport-first
instead of using the generic projection fallback.

Recent A2UI transport metadata projection move:
`TransportMetadataProjection` now also lives in
`tech.kayys.wayang.a2ui.wayang.transport`. The branded
`WayangA2uiTransportMetadata` facade still owns the public entry points while
delegating request, session, surface, action-binding, HTTP diagnostic, error,
and merge metadata maps to the transport boundary. Boundary placement pins
transport-owned metadata projection before the generic projection fallback, and
the focused test keeps package-local HTTP fixtures rooted instead of widening
fixture visibility.

Recent A2UI transport exchange metrics move:
`TransportMetricExchange` and `TransportExchangeMetrics` now live in
`tech.kayys.wayang.a2ui.wayang.transport`. Bridge scenario exchanges and HTTP
metric adapters import the shared transport-facing view explicitly, while
`HttpExchangeMetrics` remains package-local and layers HTTP status aggregation
on top of transport handled/rejected/outcome aggregation. Boundary coverage pins
the moved metric helpers as public transport-boundary helpers.

Recent A2UI HTTP exchange metrics move:
`HttpMetricExchange` and `HttpExchangeMetrics` now live in
`tech.kayys.wayang.a2ui.wayang.http`. HTTP scenario and endpoint exchange
records implement the moved HTTP metric view explicitly, while scenario and
endpoint diagnostic result records delegate status-code, success, error, and
response-envelope aggregation through the HTTP boundary. Boundary coverage now
pins the first moved HTTP package helpers as public HTTP-boundary types.

Recent A2UI HTTP issue map move:
`HttpIssueMaps` now lives in `tech.kayys.wayang.a2ui.wayang.http` beside the
HTTP metric helpers. Binding-report, readiness, smoke, operational diagnostic,
and endpoint diagnostic projections import the issue-map builder explicitly,
keeping ordered HTTP issue payloads in the HTTP boundary instead of relying on
root-package helper access. Boundary placement and coverage tests now pin the
helper as a public HTTP-boundary type.

Recent A2UI HTTP report metric helper move:
`HttpReportMetrics` and `HttpReportMetricDecoders` now live in
`tech.kayys.wayang.a2ui.wayang.http`. Scenario and endpoint diagnostic
projections import the ordered metric fragment helper explicitly, and endpoint
diagnostic report decoding imports the strict metric decoder from the same HTTP
boundary. Boundary placement and coverage tests now pin both helpers as public
HTTP-boundary types.

Recent A2UI HTTP header/body helper move:
`HttpHeaderValues` and `HttpResponseBodyDecoder` now live in
`tech.kayys.wayang.a2ui.wayang.http`. Endpoint binding/response normalization
imports the header coercion helper explicitly, while action-binding,
binding-report, and readiness probe response decoders import the shared lenient
body decoder. Boundary placement and coverage tests now pin both helpers as
public HTTP-boundary types.

Recent A2UI HTTP readiness/smoke response decoder move:
`HttpReadinessProbeResponseDecoder` and `HttpSmokeProbeResponseDecoder` now
live in `tech.kayys.wayang.a2ui.wayang.http`. The branded readiness and smoke
probe result records keep their root API names while importing these HTTP
decoder helpers explicitly. The shared response decoder test fixture moved into
the HTTP test package as well. Action-binding and binding-report response
decoders remain rooted until their probe projection helpers move in a separate
HTTP/projection step.

Recent A2UI HTTP probe projection and decoder move:
`HttpActionBindingProbeProjection`, `HttpBindingReportProbeProjection`,
`HttpActionBindingProbeResponseDecoder`, and
`HttpBindingReportProbeResponseDecoder` now live in
`tech.kayys.wayang.a2ui.wayang.http`. The branded action-binding and
binding-report probe result records keep their root API names while delegating
map projection and HTTP response parsing into the HTTP boundary. Boundary
placement now treats these probe-specific projections as HTTP-first before the
generic projection fallback.

Recent A2UI HTTP scenario suite metrics move:
`HttpScenarioSuiteMetrics` now lives in
`tech.kayys.wayang.a2ui.wayang.http` beside the other HTTP metric helpers.
The branded scenario suite result/report records keep their root API names
while importing suite aggregation explicitly from the HTTP boundary. Boundary
placement and coverage tests now pin the helper as a public HTTP-boundary type.

Recent A2UI HTTP route/endpoint projection move:
`HttpRouteProjection`, `HttpEndpointProjection`, and
`HttpPublicationProjection` now live in
`tech.kayys.wayang.a2ui.wayang.http`. The branded route, route binding, route
catalog, endpoint request/response/exchange, and endpoint publication records
keep their root API names while delegating ordered map projection into the HTTP
boundary. Boundary placement now treats these HTTP-specific projections as
HTTP-first before the generic projection fallback.

Recent readiness profile external reader diagnostics surface:
`WayangPlatformReadinessProfileExternalReaderProviderDiagnostics` and
`WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport` now expose
provider-neutral visibility for optional database/object-storage readiness
profile reader modules. `WayangPlatformApi` publishes the report as a platform
envelope, `readiness-profiles providers` renders it from the CLI, and the
object-storage bridge contributes its nested service discovery report without
making SDK or CLI code depend on S3/RustFS implementation classes.

Recent readiness profile registry preflight surface:
`WayangPlatformReadinessProfileRegistryPreflightReport` now combines registry
config diagnostics, external reader provider discovery, and source resolution
into one production-facing check. `WayangPlatformApi` exposes the preflight
envelope and `readiness-profiles preflight` renders JSON/text for operational
smoke checks. Missing database/object-storage readers fail when no fallback is
available, but become warnings when built-in fallback keeps the registry usable.

Recent secret-safe configuration diagnostics hardening:
`WayangSecretRedactor` now centralizes connection-string redaction for SDK
diagnostic projections. Readiness profile registry config, registry preflight,
database source locations, storage config maps, storage readiness reports, and
CLI `readiness-profiles config` text keep raw connection strings inside runtime
models while redacting inline passwords, tokens, API keys, and URI user-info
passwords from operator-facing JSON/text output.

Recent object-storage credential projection hardening:
`WayangObjectStorageConfig.toMap()` now runs `credentialsRef` through the shared
redactor so normal reference names remain visible while accidental inline S3
credential payloads such as `accessKeyId=... secretAccessKey=...` are redacted
from storage, readiness profile registry, preflight, and CLI JSON diagnostics.

Recent A2UI HTTP expectation projection move:
`HttpExpectationProjection` now lives in
`tech.kayys.wayang.a2ui.wayang.http`. The branded expectation issue/result
records keep their root API names while delegating ordered validation maps into
the HTTP boundary. Boundary placement and coverage now pin it as an
HTTP-specific projection before the generic projection fallback.

Recent A2UI HTTP readiness/smoke probe projection move:
`HttpReadinessProbeProjection` and `HttpSmokeProbeProjection` now live in
`tech.kayys.wayang.a2ui.wayang.http`. The branded readiness probe result,
smoke probe result, smoke summary, and smoke summary decoder keep their root
API names while importing probe projection helpers explicitly from the HTTP
boundary. Boundary placement and coverage now pin both helpers as HTTP-first
projection types before the generic projection fallback.

Recent A2UI HTTP smoke result projection move:
`HttpSmokeResultProjection` now lives in
`tech.kayys.wayang.a2ui.wayang.http`. The branded smoke result record keeps
its root API name while delegating ordered result maps into the HTTP boundary.
Boundary placement and coverage now pin the smoke-result helper as an
HTTP-first projection before the generic projection fallback.

Recent A2UI HTTP scenario projection move:
`HttpScenarioProjection` now lives in
`tech.kayys.wayang.a2ui.wayang.http`. The branded scenario report, suite
report, and issue records keep their root API names while importing scenario
map projection from the HTTP boundary. Boundary placement and coverage now pin
the scenario projection helper as HTTP-first before the generic projection
fallback.

Recent A2UI HTTP binding report projection move:
`HttpBindingReportProjection` now lives in
`tech.kayys.wayang.a2ui.wayang.http`. The branded HTTP binding report record
keeps its root API name while delegating route/handler coverage maps into the
HTTP boundary. Boundary placement and coverage now pin the binding-report
projection as HTTP-first before the generic projection fallback.

Recent A2UI HTTP endpoint diagnostic plan projection move:
`HttpEndpointDiagnosticPlanProjection` now lives in
`tech.kayys.wayang.a2ui.wayang.http`. The branded endpoint diagnostic config,
plan, and request records keep their root API names while delegating ordered
diagnostic plan maps into the HTTP boundary. Boundary placement and coverage
now pin the plan projection as HTTP-first before the generic projection
fallback.

Recent A2UI HTTP endpoint diagnostic projection move:
`HttpEndpointDiagnosticProjection` now lives in
`tech.kayys.wayang.a2ui.wayang.http`. The branded endpoint diagnostic report,
run, summary, and issue records keep their root API names while importing the
shared diagnostic map projection from the HTTP boundary. Boundary placement and
coverage now pin the diagnostic projection as HTTP-first before the generic
projection fallback.

Recent A2UI HTTP operational diagnostics projection move:
`HttpOperationalDiagnosticsProjection` now lives in
`tech.kayys.wayang.a2ui.wayang.http`. The branded operational diagnostics and
summary records keep their root API names while delegating ordered diagnostics
maps into the HTTP boundary. Boundary placement, coverage, and direct
projection tests now pin the helper as an HTTP-first projection.

Recent S3 diagnostics credential redaction hardening:
S3-specific readiness profile diagnostics now reuse `WayangSecretRedactor` for
credential source maps, credential-resolution maps, service-provider reports,
and object-storage preflight reports. Raw credential references remain available
through runtime accessors for registry lookup and service creation, while
operator-facing map/string projections redact inline `accessKeyId`,
`secretAccessKey`, token, password, and URI user-info values.

Recent object-storage diagnostic redaction boundary hardening:
`WayangSecretRedactor` now also redacts nested diagnostic maps and lists.
Neutral object-storage service resolution, service reader failures, provider
diagnostics, discovery reports, and preflight aggregation reuse that boundary so
inline credential-like service ids or read failure strings stay available for
runtime matching but are scrubbed from operator-facing maps, messages, and CLI
payloads. The S3 reader factory now redacts missing-credential exception
messages through the same shared helper.

Recent readiness profile source diagnostics redaction hardening:
Readiness profile source result/status/resolution models now redact source
locations and messages as they are constructed. Database reader failures can
still carry rich failure context for operators, but JDBC passwords, tokens,
URI user-info passwords, and inline credential assignments are scrubbed before
the values reach registry resolution, platform readiness reports, registry
preflight maps, or CLI payloads. The touched source records now include class
descriptions documenting their diagnostic boundary role.

Recent global operator-output redaction boundary hardening:
`WayangReportMaps` now recursively redacts string values in shared readiness
and diagnostics reports, covering nested probes, issues, attributes, aggregate
reports, and protocol adapter diagnostics that use the shared report copier.
Agent run lifecycle/history/event/control/inspection envelopes now route
through `AgentRunEnvelopeMaps`, a null-preserving redacting copier, so run
metadata and messages are scrubbed for JSON/text output without changing the
published lifecycle contract shape or stored runtime metadata.

Recent file run-store persistence hardening:
`FileAgentRunStore` now writes complete properties snapshots through sibling
temporary files and replaces the live store with an atomic move when supported.
The write path fsyncs the temporary snapshot before replacement, makes directory
durability best-effort where the filesystem allows it, and cleans temporary
files on failed moves while keeping persisted run metadata unchanged for later
redacted projection through operator envelopes.

Recent file run-store snapshot boundary hardening:
`AgentRunStoreSnapshotFiles` now owns run-store filesystem mechanics separately
from `FileAgentRunStore`'s run/status property mapping. The snapshot boundary
keeps atomic replacement behavior in one place and quarantines unreadable or
malformed properties snapshots to sibling `.corrupt-*` files, allowing status,
history, and follow-up writes to continue from an empty store instead of failing
every operator command on one bad persistence file.

Recent file run-store version compatibility hardening:
`FileAgentRunStore` now treats explicit unknown snapshot versions as unsupported
instead of silently parsing them as the current format. Missing versions remain
compatible with older properties snapshots, while future versions are preserved
as sibling `.unsupported-version-*` files so operators can inspect the original
payload and the run store can recover with a fresh version-1 snapshot.

Recent file run-store snapshot consistency hardening:
`AgentRunStoreSnapshot` now represents the immutable status/event lists loaded
from one properties snapshot. `FileAgentRunStore` uses that single loaded model
for save, remove, append-event, history, and timeline reads, reducing duplicate
file I/O and avoiding mixed status/event views when a store file changes between
separate reads.

Recent run lifecycle event sequencing hardening:
`AgentRunEventSequences` now owns generated lifecycle event sequence allocation.
Both memory and file run stores generate status events after the highest existing
sequence for the run instead of using event count plus one, preventing duplicate
or regressing sequence numbers after explicit audit/tool events are appended with
their own sequence values.

Recent run timeline ordering hardening:
`AgentRunEventTimelines` now owns read-side event ordering and latest-window
selection. Run event queries sort by sequence before applying the limit, so
out-of-order appended audit/tool events still produce deterministic timelines,
cursors, and follow-event advancement.

Recent file run-store advisory lock hardening:
`AgentRunStoreSnapshotFiles` now exposes a sibling `.lock` file boundary backed
by both a JVM-local monitor and a filesystem `FileLock`. `FileAgentRunStore`
wraps status/event reads and read-modify-write mutations in that boundary, so
separate SDK/CLI instances sharing one local run-store file serialize updates
instead of racing between snapshot read and atomic replacement.

Recent file run-store retention hardening:
`AgentRunStoreRetentionPolicy` now exposes bounded local persistence defaults
of 1000 retained runs and 1000 retained events per run, with an explicit
`unlimited()` mode and an `AgentRunStore.file(path, policy)` factory for custom
deployments. `WayangStorageConfig` can parse and report the same retention
policy from storage maps, and `AgentRunStore.configured(...)` applies it for
file-backed stores and cloud/fallback file stores. `AgentRunStoreRetention` owns
snapshot compaction separately from property serialization, counts both
status-backed and event-only runs, and keeps the newest per-run timeline window
while preserving the existing run-store API shape for ordinary callers.

Recent run-store retention operability hardening:
Storage readiness now includes a dedicated `storage.retention` probe and
operator attributes that expose retained run/event limits, whether each axis is
bounded, and whether the local run history is intentionally unlimited. The root
CLI now accepts `--run-store-max-runs` and
`--run-store-max-events-per-run` with `WAYANG_RUN_STORE_MAX_RUNS` and
`WAYANG_RUN_STORE_MAX_EVENTS_PER_RUN` fallbacks, so retention can be controlled
from production scripts without hand-building SDK configuration.

Recent run-store retention policy boundary cleanup:
`AgentRunStoreRetentionPolicy` now owns retention map parsing, alias handling,
serialization, and bounded/unlimited helpers. `WayangStorageConfig` delegates to
that policy boundary instead of carrying retention-specific parsing helpers,
keeping storage backend selection separate from run-history retention semantics.

Recent run-store retention mode ergonomics improvement:
Retention configuration now supports explicit unlimited/disabled modes in
addition to numeric `0` limits. Storage maps can use scalar `retention=off`,
nested `retention.mode=unlimited`, `retention.unlimited=true`, or
`retention.enabled=false`; the root CLI mirrors this through
`--run-store-retention` and `WAYANG_RUN_STORE_RETENTION` while preserving the
existing numeric limit flags.

Recent run-store retention reporting cleanup:
`AgentRunStoreRetentionPolicy.toMap()` now emits a self-describing operator
shape with `mode`, numeric limits, per-axis bounded flags, and aggregate
bounded/unlimited booleans. Storage config exports and storage readiness probes
reuse that same map, removing duplicate retention-reporting logic and keeping
CLI/readiness JSON aligned with the policy boundary.

Recent run-store retention assessment hardening:
`AgentRunStoreRetentionAssessment` now summarizes one snapshot's retention
decision with total/retained/pruned run, status, and event counts plus retained
and pruned run ids and per-run pruned event counts. `AgentRunStoreRetention`
builds this assessment from the same plan used for compaction, giving future
operator commands and readiness diagnostics a reusable explanation model without
duplicating pruning math.

Recent run-store diagnostics envelope improvement:
`AgentRunStoreDiagnostics` now exposes backend, persistence, snapshot path,
lock path, snapshot version, run/status/event counts, retention policy, and a
nested retention assessment through one reusable SDK model. Memory stores report
their live in-process counts, while file stores inspect the locked properties
snapshot and sibling lock file, giving future CLI/readiness/admin surfaces a
stable production diagnostics boundary without duplicating snapshot parsing.
The in-memory implementation now publishes the same status-backed plus
event-only run counting semantics used by file retention, keeping local tests,
audit-only timelines, and future admin probes consistent across backends.

Recent run-store diagnostics CLI improvement:
`AgentRunLifecycleService`, `WayangGollekSdk`, and `WayangRunApi` now expose the
run-store diagnostics envelope through the shared SDK facade. The CLI adds an
explicit `wayang run store` command with text and JSON output for backend,
path, lock, snapshot, count, and retention diagnostics, including file-backed
stores resolved from `--run-store`. Routine readiness remains lightweight, while
operators can opt into live run-store inspection when they need production
debuggability.

Recent run-store diagnostics contract hardening:
`run store --json` is now a first-class `wayang.run.lifecycle` contract with a
dedicated `run-store` envelope, JSON Schema properties for backend, lock,
snapshot, count, retention policy, and retention assessment fields, and a stable
`run-store-json` local workbench command. The SDK renders diagnostics through
`AgentRunStoreDiagnosticsEnvelopes`, keeping contract metadata out of the plain
diagnostics model while making CLI, TUI, HTTP, and future operator shells share
one schema-backed JSON boundary.

Recent run-store verification hardening:
`AgentRunStoreVerification` and `AgentRunStoreVerificationIssue` now provide a
pass/fail operator report with error and warning counts, exit code, issues, and
the inspected diagnostics snapshot. File-backed stores use a non-mutating
snapshot inspection path for verification, so corrupt or unsupported properties
files can be reported without quarantining or rewriting the snapshot. The CLI
adds `wayang run store --verify` with text and JSON output, returning a non-zero
exit only for blocking verification errors while still surfacing retention
warnings for production scripts.

Recent run-store verification contract hardening:
`run store --verify --json` is now a first-class lifecycle JSON contract with a
stable `run-store-verification` envelope, `run-store-verify-json` local workbench
command, and golden fixture coverage. Verification JSON now carries the same
contract metadata as other run lifecycle surfaces, while its nested diagnostics
object reuses the run-store diagnostics schema shape for backend, snapshot,
count, lock, retention policy, and retention assessment fields.

Recent run-store verification policy hardening:
`AgentRunStoreVerificationPolicy` now separates pass/fail semantics from the
verification report itself, preserving lenient default behavior while allowing
strict automation to fail warning-only reports. `wayang run store --verify
--strict` applies that SDK-owned policy to text output, JSON envelopes, and exit
codes, and the JSON contract now publishes the active policy so production CI,
deploy scripts, and future operator surfaces can distinguish blocking errors
from warnings that were intentionally promoted to failures.

Recent run-store verification policy configuration improvement:
Run-store verification policy parsing now lives in the SDK and accepts stable
operator aliases such as `lenient`, `strict`, and `warnings-as-errors`. The CLI
adds `--verification-policy` plus `WAYANG_RUN_STORE_VERIFICATION_POLICY` fallback
for `wayang run store --verify`, while preserving `--strict` as a concise
one-shot override. This keeps production shell scripts and future product
surfaces aligned on the same policy vocabulary instead of duplicating string
parsing at the edges.

Recent run-store compaction preview improvement:
`AgentRunStoreCompactionPreview` now exposes a non-mutating retention compaction
plan through the SDK, including previewability, planned pruning counts,
verification issues, diagnostics, and the shared retention assessment. The CLI
adds `wayang run store --compact --dry-run` with text and JSON output, and
`run store --compact --dry-run --json` is now a lifecycle contract-backed
operator command. This creates a safe production path for reviewing retention
impact before future mutating repair or compaction commands are introduced.

Recent run-store compaction apply improvement:
`AgentRunStoreCompactionResult` now exposes explicit mutating retention
compaction through the SDK, including applied/skipped status, pruned run/event
counts, backup metadata, before/after diagnostics, verification issues, and an
exit-code hint. File-backed stores compact under the same file lock used for
writes, verify the snapshot before mutation, create a sibling compaction backup,
and skip safely when blocking verification errors or no retention pruning are
present. The CLI adds `wayang run store --compact --apply` with text and JSON
output, and `run store --compact --apply --json` is now a lifecycle
contract-backed workbench command with golden fixture coverage.

Recent run-store compaction backup-retention improvement:
Compaction backups now have their own SDK-owned retention policy, separate from
run/status/event retention. `AgentRunStoreBackupRetentionPolicy` accepts bounded
and unlimited modes, storage config maps can set `backupRetention.maxBackups`,
and the root CLI exposes `--run-store-backup-retention`,
`--run-store-max-backups`, and `WAYANG_RUN_STORE_MAX_BACKUPS`. File-backed
compaction keeps the newest rollback backups and reports retained/pruned backup
counts and paths through the `run-store-compaction` JSON contract.

Recent run-store backup-inventory diagnostics improvement:
Run-store diagnostics now expose the configured compaction-backup retention
policy and a newest-first backup inventory. `run store --json`, verification
diagnostics, compaction previews, and compaction before/after diagnostics all
share `backupRetentionPolicy` and `backupInventory` fields so operator surfaces
can show backup count plus latest/oldest rollback snapshots without duplicating
filesystem scanning rules.

Recent run-store backup-retention verification hardening:
Run-store verification now warns when the current compaction-backup inventory
exceeds the configured backup-retention window. The warning is shared by custom
diagnostics and file-backed stores, participates in strict verification policy,
and leaves backup files untouched until a successful compaction applies the
configured backup pruning rule.

Recent run-store compaction preview backup-plan improvement:
`AgentRunStoreCompactionPreview` now includes the same backup-retention plan
shape as mutating compaction results. Dry-run CLI and JSON output report planned
retained/pruned backup counts and paths without deleting rollback snapshots,
and preview/apply now share the SDK backup-retention calculation instead of
duplicating pruning math in the file-store boundary.

Recent run-store backup-prune failure reporting improvement:
Backup-retention results now expose failed backup prune counts and paths.
File-backed compaction keeps the primary snapshot compaction successful when
best-effort backup cleanup is incomplete, but emits a
`backup-retention.prune-incomplete` warning and includes the failed rollback
snapshot paths in text and JSON output for operator follow-up.

Recent A2UI session config provider validation:
`SessionConfigSourceProvider` now exposes an optional provider-specific
validation hook while remaining lambda-friendly. `SessionConfigSourceRegistry`
applies built-in source-spec validation first, resolves the provider, and then
lets database/S3/RustFS-style adapters reject missing storage-specific fields
before any remote load is attempted. `docs/A2UI_SESSION_CONFIG_SOURCES.md`
documents inline, file, classpath, fallback, database, and object-storage
configuration patterns plus request-level diagnostics.

Recent A2UI session config request-context extraction:
`SessionConfigRequestContext` now owns request-context lookup for direct
`sessionConfig` and dynamic `sessionConfigSource` values. The Wayang facade
delegates session config resolution and load diagnostics to that helper, and
request context values may now be supplied as either decoded maps or JSON
strings. Direct config still takes precedence over dynamic sources, keeping
operator overrides predictable while gateway adapters can pass source JSON
without duplicating decoding logic.

Recent A2UI object-storage session config provider:
`SessionConfigObjectStorageProvider` now provides a dependency-free adapter for
bucket/key-backed session config sources such as S3, RustFS, or compatible
object stores. The reusable provider validates bucket/key source specs,
supports common `container` and `objectKey` aliases, and delegates reads to an
application-owned function so storage SDKs remain outside the A2UI module.

Recent A2UI lookup-backed session config provider:
`SessionConfigLookupProvider` now provides a dependency-free adapter for
database and config-service backed session config lookup. Tenant-scoped
providers validate `tenantId` and default to a `default` profile, while keyed
providers validate explicit lookup keys from `key`, `configKey`, `profile`,
`name`, or `id`. This consolidates database-style source wiring without adding
JDBC, service-client, or product-specific dependencies to A2UI.

Recent A2UI session config source spec factories:
`SessionConfigSourceSpecs` now provides typed factory helpers for common
provider-backed source specs, including database, config-service, S3, RustFS,
custom lookup, and custom object-storage sources. This keeps request and
fallback source construction out of loose ad hoc maps while preserving the
storage-neutral `SessionConfigSourceSpec` value object.

Recent A2UI session config registry composition helpers:
`SessionConfigSourceRegistries` now provides dependency-free composition helpers
for common database, config-service, S3, and RustFS provider wiring. Applications
can obtain ready registries or pre-populated builders, then add custom provider
types without repeating standard adapter registration code at every gateway or
tenant boundary.

Recent A2UI session config source redaction:
`SessionConfigSourceRedactor` now centralizes diagnostic masking for dynamic
session config source specs. `SessionConfigSourceSpec.toDiagnosticMap()` keeps
runtime `toMap()` untouched while masking credential-like fields, tokens, and
service endpoints across nested fallback sources before logs or readiness probes
publish source details.

Recent A2UI session config fallback diagnostics:
Fallback session config loads now keep an ordered attempt trace through
`SessionConfigLoadAttempt`. `SessionConfigSources.firstAvailable(...)`,
`loadFirstResult(...)`, and registry-created fallback sources report which
sources missed, failed, or loaded while preserving the existing behavior where
failures stop the chain and missing sources continue.

Recent A2UI session config source contract fixtures:
Session config source specs now have focused JSON contract fixtures for fallback
source chains, redacted object-storage diagnostics, and fallback load-result
attempt traces. These fixtures stabilize the SDK, gateway, CLI, and operator UI
contract around dynamic database/object-storage/file persistence.

Recent A2UI session config provider capability metadata:
`SessionConfigSourceCapability` now describes required fields, aliases,
diagnostic-safe fields, and provider traits such as lookup, object-storage, and
fallback source support. `SessionConfigSourceRegistry` exposes provider and
source capability maps so SDK, CLI, gateway, and operator UI integrations can
validate dynamic session config source specs before runtime.

Recent A2UI session config source policy:
`SessionConfigSourcePolicy` now lets applications restrict dynamic session
config source types with allow-only or deny lists. The registry enforces the
policy before provider resolution, recurses through fallback chains, and
canonicalizes aliases such as `json`/`inline`, `resource`/`classpath`, and
`chain`/`fallback`/`first-available`.

Recent A2UI session config source diagnostics:
`SessionConfigSourceDiagnostics` now provides a compact operator payload for
dynamic session config sources. It combines redacted source specs, validation
and policy errors, top-level load status, provider capability metadata, registry
source capabilities, and nested load-result attempts so SDK, CLI, gateway, and
admin UI integrations can explain source behavior without duplicating registry
or redaction logic.

Recent A2UI session config diagnostics decoding:
Session config source diagnostics and load-result diagnostics now round-trip
through session-owned decoders. `SessionConfigSourceDiagnostics.fromMap/json`
and `SessionConfigLoadResult.fromMap/json` keep stored, remote, CLI, and admin
UI payload handling out of HTTP-specific decoders, while the policy-rejected
diagnostics fixture locks the public source-diagnostics JSON shape.

Recent A2UI request-context source diagnostics:
`WayangA2ui.sessionConfigSourceDiagnostics(...)` now exposes dynamic
`sessionConfigSource` diagnostics from agent request context. It returns a
redacted optional diagnostics payload when a source is active, reports malformed
source JSON as a failed diagnostic, and stays empty when direct `sessionConfig`
precedes the source override.

Recent A2UI request-level session config diagnostics:
`SessionConfigRequestDiagnostics` now gives SDK, CLI, gateway, and admin UI
callers one envelope for request-context session config resolution. The facade
exposes it through `WayangA2ui.sessionConfigDiagnostics(...)`, including
context/input presence, active input, load status, load result, optional source
diagnostics, JSON decoding, and a direct-config contract fixture.

Recent A2UI request-level session config diagnostics summary:
`SessionConfigRequestDiagnosticsSummary` now projects the request diagnostics
envelope into a compact operator row with pass/fail state, exit code, active
input, status, source type, error counts, attempt count, and key attributes.
`WayangA2ui.sessionConfigDiagnosticsSummary(...)` exposes the summary directly,
and a standalone contract fixture locks the compact JSON shape for CLI,
gateway, readiness, and dashboard integrations.

Recent A2UI request diagnostics summary extraction:
Stored request-diagnostics envelopes can now be read as compact summaries with
`SessionConfigRequestDiagnosticsSummary.fromDiagnosticsMap(...)` and
`fromDiagnosticsJson(...)`. The decoder prefers the embedded `summary` field
for lightweight clients and derives the same shape from legacy full envelopes
that predate the embedded summary block.

Recent A2UI pro/enterprise add-on packaging improvement:
The active `wayang-gollek` community reactor no longer includes the A2UI
modules by default. A2UI now builds through the explicit
`pro-enterprise-addons` Maven profile, and provider capability metadata marks
`a2ui.contracts` with `edition=pro-enterprise`,
`defaultCommunity=false`, and `activationProfile=pro-enterprise-addons` so CLI,
SDK, and product-shell discovery can distinguish add-on capabilities from
community-default modules.

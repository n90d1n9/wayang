## rag-runtime (Native Wayang RAG)

`rag-runtime` is the orchestration/runtime layer that wires Wayang-owned components for ingestion and query flows.

### Scope
- Runtime services: `DocumentIngestionService`, `RagQueryService`, `RagExecutionService`
- Native pipeline bridge: `NativeRagCoreService`
- Vector backend provider: `RagVectorStoreProvider`
- Owned embedding abstractions: `RagEmbeddingModel`, `RagEmbeddingStore`
  - Plugin extension system: `RagPipelinePlugin` + `RagPluginManager`
    - `RagPluginCatalog` handles plugin discovery/sorting.
    - `RagPluginTenantStrategyResolver` handles tenant strategy resolution and active selection.
  - Contracts are published in `rag-plugin-api`
  - Built-ins are packaged as separate artifacts:
    - `rag-plugin-normalize-query`
    - `rag-plugin-safety-filter`
    - `rag-plugin-lexical-rerank`

### Current behavior
- Ingestion path uses PDFBox + owned chunk/index flow from `rag-core`.
- Query path uses owned retrieval and generation (`NativeGenerationService`).
- Vector store backend can be in-memory or pgvector through `VectorStoreFactory`.
- Admin endpoint:
  - `GET /admin/embedding/config` to inspect active embedding config.
  - `POST /admin/embedding/config/reload` to force reload config snapshot.
  - `GET /admin/embedding/schema/{tenantId}` to inspect active tenant embedding schema contract.
  - `GET /admin/embedding/schema/{tenantId}/history?limit=20` to inspect migration audit history.
  - `POST /admin/embedding/schema/{tenantId}/history/compact` to compact audit history (`maxEvents`, `maxAgeDays`, `dryRun`).
  - `GET /admin/embedding/schema/history/compaction/status` to inspect auto-compaction runtime status.
  - `POST /admin/embedding/schema/migrate` to migrate schema contract (`dryRun` supported).
  - Set `rag.runtime.embedding.schema.history.path` to persist migration history as NDJSON across restarts.
  - Optional scheduled auto-compaction:
    - `rag.runtime.embedding.schema.history.compaction.enabled`
    - `rag.runtime.embedding.schema.history.compaction.interval`
    - `rag.runtime.embedding.schema.history.compaction.max-events`
    - `rag.runtime.embedding.schema.history.compaction.max-age-days`
    - `rag.runtime.embedding.schema.history.compaction.dry-run`
    - `rag.runtime.embedding.schema.history.compaction.tenants`
  - Micrometer metrics exposed for compactor:
    - `wayang.rag.embedding.schema.compaction.cycle.count`
    - `wayang.rag.embedding.schema.compaction.tenants_processed.count`
    - `wayang.rag.embedding.schema.compaction.removed.count`
    - `wayang.rag.embedding.schema.compaction.failure.count`
    - `wayang.rag.embedding.schema.compaction.last_cycle.*` gauges
  - `GET /admin/observability/slo` and `GET|PUT|POST /admin/observability/slo/config` for SLO status/config.
  - `GET|PUT|POST /admin/rag/plugins/config` for plugin toggles/order/tuning status, live update, and reload.
    - Supports `selectionStrategy` (`config` default).
    - `PUT` validates tenant override syntax and returns `400` for malformed entries.
    - Validation `400` payload includes: `code`, `field`, `tenantId`, `value`, `message`.
  - Startup fails fast when `rag.runtime.rag.plugins.selection-strategy`,
    `rag.runtime.rag.plugins.tenant-enabled`, or
    `rag.runtime.rag.plugins.tenant-order` is invalid.
  - `GET /admin/rag/plugins?tenantId=...` to inspect discovered plugins and effective active order for a tenant.
    - Response includes tenant strategy resolution (`strategyId`, `matchedTenant*Override`, `effectiveEnabledIds`, `effectiveOrder`).
  - `GET /admin/observability/slo/alerts` to evaluate alert state with severity filter + cooldown suppression.
  - `GET /admin/observability/slo/alerts/snooze` to inspect active alert snooze state.
  - `POST /admin/observability/slo/alerts/snooze` to snooze current alert fingerprint (`scope=all|guardrail`, `durationMs`).
  - `POST /admin/observability/slo/alerts/snooze/clear` to clear active snooze.
  - Optional persistence: `rag.runtime.slo.alert.snooze.path` keeps snooze state across restarts.
  - `POST /admin/eval/retrieval` to run offline retrieval evaluation (Recall@K, MRR, latency p95) from inline dataset or fixture file path.
  - `GET /admin/eval/retrieval/history?tenantId=&datasetName=&limit=20` to inspect stored eval runs.
  - `GET /admin/eval/retrieval/trend?tenantId=&datasetName=&window=20` to compare latest run against previous run.
  - `GET /admin/eval/retrieval/guardrails?tenantId=&datasetName=&window=20` to evaluate regression guardrails against trend deltas.
  - `GET|PUT|POST /admin/eval/retrieval/guardrails/config` to inspect/update/reload guardrail thresholds live.
  - SLO now also evaluates compactor health with:
    - `wayang.rag.slo.compaction-failure-rate`
    - `wayang.rag.slo.compaction-cycle-staleness-ms`
  - SLO now also surfaces retrieval-eval guardrail regressions as breaches
    (`eval_guardrail_*` metrics), so `/admin/observability/slo/alerts` includes them.
  - Each SLO breach includes a `severity` field (`warning` or `critical`).
  - Severity multipliers are configurable:
    - `wayang.rag.slo.severity.warning-multiplier` (default `1.0`)
    - `wayang.rag.slo.severity.critical-multiplier` (default `2.0`)
    - Per-metric overrides:
      - `wayang.rag.slo.severity.warning-by-metric` (e.g. `index_lag_ms=1.2`)
      - `wayang.rag.slo.severity.critical-by-metric` (e.g. `index_lag_ms=2.5`)
  - Alert policy knobs:
    - `wayang.rag.slo.alert.enabled` (default `true`)
    - `wayang.rag.slo.alert.min-severity` (`warning|critical`, default `warning`)
    - `wayang.rag.slo.alert.cooldown-ms` (default `300000`)
  - Retrieval eval request supports:
    - `dataset` (inline query fixtures), or `fixturePath` (`classpath:...` or file path)
    - `matchField`: `documentId` (default), `chunkId`, `source`, or `metadata:<key>`
    - Returns aggregate `recallAtK`, `mrr`, `latencyP95Ms`, and per-case results.
  - Retrieval eval run history:
    - `rag.runtime.eval.retrieval.history.path` for NDJSON persistence across restarts
    - `rag.runtime.eval.retrieval.history.max-events` retention cap (default `1000`)
  - Retrieval eval guardrails:
    - `rag.runtime.eval.retrieval.guardrail.enabled`
    - `rag.runtime.eval.retrieval.guardrail.window-size`
    - `rag.runtime.eval.retrieval.guardrail.recall-drop-max`
    - `rag.runtime.eval.retrieval.guardrail.mrr-drop-max`
    - `rag.runtime.eval.retrieval.guardrail.latency-p95-increase-max-ms`
    - `rag.runtime.eval.retrieval.guardrail.latency-avg-increase-max-ms`
  - Metrics emitted for eval runs and guardrails:
    - `wayang.rag.eval.retrieval.run.count`
    - `wayang.rag.eval.retrieval.query.count`
    - `wayang.rag.eval.retrieval.hit.count`
    - `wayang.rag.eval.retrieval.recall_at_k`
    - `wayang.rag.eval.retrieval.mrr`
    - `wayang.rag.eval.retrieval.latency_p95_ms`
    - `wayang.rag.eval.retrieval.latency_avg_ms`
    - `wayang.rag.eval.retrieval.guardrail.check.count`
    - `wayang.rag.eval.retrieval.guardrail.breach.count`
  - RAG plugin system:
    - Implement `RagPipelinePlugin` as CDI bean to extend hooks:
      - `beforeQuery(context)`
      - `afterRetrieve(context, chunks)`
      - `afterResult(context, result)`
    - Built-in plugins included (external modules):
      - `normalize-query`: trims/collapses whitespace, optional lowercase + max length cap.
      - `safety-filter`: redacts blocked terms in query/answer and removes matching chunks.
      - `lexical-rerank`: reorders retrieved chunks using lexical overlap + original score blend.
    - Plugin controls:
      - `rag.runtime.rag.plugins.selection-strategy` (`config` by default)
      - `rag.runtime.rag.plugins.enabled` (`*` or CSV plugin ids)
      - `rag.runtime.rag.plugins.order` (CSV explicit execution order)
      - `rag.runtime.rag.plugins.tenant-enabled` (`tenant=csv_plugins;tenant2=*`)
      - `rag.runtime.rag.plugins.tenant-order` (`tenant=csv_plugins_in_order;tenant2=...`)
    - Built-in plugin tuning:
      - `rag.runtime.rag.plugins.normalize-query.lowercase`
      - `rag.runtime.rag.plugins.normalize-query.max-query-length`
      - `rag.runtime.rag.plugins.lexical-rerank.original-weight`
      - `rag.runtime.rag.plugins.lexical-rerank.lexical-weight`
      - `rag.runtime.rag.plugins.lexical-rerank.annotate-metadata`
      - `rag.runtime.rag.plugins.safety-filter.blocked-terms`
      - `rag.runtime.rag.plugins.safety-filter.mask`
  - Require header: `x-admin-key` matching `rag.runtime.admin.api-key` or
    `rag.runtime.admin.api-key-secondary` (for key rotation).
  - Response includes `X-Admin-Key-Slot: primary|secondary` for rotation observability.

### Example
```java
DocumentIngestionService ingestion = ...;
ingestion.ingestTextDocuments(
    "tenant-a",
    List.of("Wayang supports native embedding and RAG modules."),
    Map.of("collection", "docs"),
    ChunkingConfig.defaults()
).await().indefinitely();

RagQueryService query = ...;
RagResponse response = query.query("tenant-a", "What modules are supported?", "docs")
    .await().indefinitely();
```

### Notes
- This module is native-first and does not require `dev.langchain4j` for runtime compilation.
- A legacy scratch file exists at `src/main/java/tech/kayys/wayang/rag/zzz` and is not part of compiled sources.

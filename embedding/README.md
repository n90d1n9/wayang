# Wayang Embedding Module

`wayang-embedding-core` provides a reusable embedding layer for modules like RAG and memory.

## What it includes

- `EmbeddingProvider` interface for pluggable providers.
- `EmbeddingService` for provider/model selection and optional L2 normalization.
- Content-hash embedding cache + request dedup (`tenant + provider + model + normalize + textHash`).
- `EmbeddingBatchPipeline` for async bounded-queue batch embedding with retry/backpressure.
- Multiple built-in providers that work without external API calls:
  - `DeterministicHashEmbeddingProvider`: `hash`, `hash-384`, `hash-768`, `hash-1536`, ...
  - `TfIdfHashEmbeddingProvider`: `tfidf`, `tfidf-256`, `tfidf-512`, ...
  - `CharNgramEmbeddingProvider`: `chargram`, `chargram-256`, `chargram-512`, ...

## Quick usage

```java
EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(
        new DeterministicHashEmbeddingProvider(),
        new TfIdfHashEmbeddingProvider(),
        new CharNgramEmbeddingProvider()));

EmbeddingService service = new EmbeddingService(registry, new EmbeddingModuleConfig());
float[] embedding = service.embedOne("hello wayang");

// provider auto-resolves from model name
float[] vector = service.embed(new EmbeddingRequest(
        List.of("risk scoring"),
        "tfidf-512",
        null,
        true)).first();
```

## Notes for other modules

- RAG can call `EmbeddingService.embed(...)` for query/document vectors.
- Memory stores can call `EmbeddingService.embedOne(...)` before persistence/search.
- Provider/model can be overridden per request using `EmbeddingRequest`.
- Tenant strategy is supported via `EmbeddingModuleConfig#setTenantStrategy(...)` and
  `EmbeddingService#embedForTenant(tenantId, request)`.
- Dimension safety checks are enforced: model-declared dimension must match produced vectors.
- `EmbeddingResponse.version()` exposes `embedding_version` for downstream store schema/version control.

## Config from properties/env

- `wayang.embedding.default-provider` or `WAYANG_EMBEDDING_DEFAULT_PROVIDER`
- `wayang.embedding.default-model` or `WAYANG_EMBEDDING_DEFAULT_MODEL`
- `wayang.embedding.version` or `WAYANG_EMBEDDING_VERSION`
- `wayang.embedding.normalize` or `WAYANG_EMBEDDING_NORMALIZE`
- `wayang.embedding.cache.enabled` or `WAYANG_EMBEDDING_CACHE_ENABLED`
- `wayang.embedding.cache.max-entries` or `WAYANG_EMBEDDING_CACHE_MAX_ENTRIES`
- `wayang.embedding.batch.size` or `WAYANG_EMBEDDING_BATCH_SIZE`
- `wayang.embedding.batch.queue-capacity` or `WAYANG_EMBEDDING_BATCH_QUEUE_CAPACITY`
- `wayang.embedding.batch.max-retries` or `WAYANG_EMBEDDING_BATCH_MAX_RETRIES`
- `wayang.embedding.batch.worker-threads` or `WAYANG_EMBEDDING_BATCH_WORKER_THREADS`
- `wayang.embedding.tenant-strategies` or `WAYANG_EMBEDDING_TENANT_STRATEGIES`

In Quarkus/MicroProfile runtime, `EmbeddingModuleConfig` is produced from these
`wayang.embedding.*` keys via CDI.

`EmbeddingService` reads runtime config through a reloadable snapshot
(`EmbeddingConfigRuntime`) so tenant strategy updates are picked up without
recreating the service.

Tenant strategy format (`;` separated):
- `tenant-a=tfidf:tfidf-512`
- `tenant-b|hash|hash-384`

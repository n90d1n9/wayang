# Wayang Native RAG Foundation

This package provides a Wayang-owned RAG foundation.

## Package

`tech.kayys.wayang.rag.core`

## Included

- Core models: `RagDocument`, `RagChunk`, `RagQuery`, `RagScoredChunk`, `RagResult`
- SPI: `DocumentParser`, `Chunker`, `Retriever`, `Reranker`, `Generator`
- Vector layer: `VectorStore<T>`, `VectorSearchHit<T>`, `InMemoryVectorStore<T>`, `PgVectorStore<T>`
- Payload serialization: `PayloadCodec<T>`, `JsonPayloadCodec<T>`
- Store selection: `VectorStoreOptions`, `VectorStoreFactory`
- Default impls:
  - `SimpleTextDocumentParser`
  - `SlidingWindowChunker`
  - `RagIndexer` (uses `EmbeddingService`)
  - `VectorRetriever` (uses `EmbeddingService`)
  - `TopKReranker`
  - `ContextConcatenatingGenerator`
  - `RagPipeline` orchestration

## Goal

Build end-to-end RAG with fully controlled code paths:

1. Parse documents
2. Chunk text
3. Embed with `wayang-embedding-core`
4. Store/search vectors via `VectorStore`
5. Rerank and generate response

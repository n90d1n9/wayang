package tech.kayys.wayang.rag.core.impl;

import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagDocument;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.spi.Chunker;
import tech.kayys.wayang.rag.core.spi.ChunkingOptions;
import tech.kayys.wayang.rag.core.spi.DocumentParser;
import tech.kayys.wayang.rag.core.spi.Generator;
import tech.kayys.wayang.rag.core.spi.Reranker;
import tech.kayys.wayang.rag.core.spi.Retriever;

import java.util.List;
import java.util.Map;

public class RagPipeline {

    private final DocumentParser parser;
    private final Chunker chunker;
    private final RagIndexer indexer;
    private final Retriever retriever;
    private final Reranker reranker;
    private final Generator generator;

    public RagPipeline(
            DocumentParser parser,
            Chunker chunker,
            RagIndexer indexer,
            Retriever retriever,
            Reranker reranker,
            Generator generator) {
        this.parser = parser;
        this.chunker = chunker;
        this.indexer = indexer;
        this.retriever = retriever;
        this.reranker = reranker;
        this.generator = generator;
    }

    public List<RagChunk> ingest(String source, String rawText, Map<String, Object> metadata, ChunkingOptions options) {
        RagDocument document = parser.parse(source, rawText, metadata);
        List<RagChunk> chunks = chunker.chunk(document, options);
        indexer.indexChunks(chunks);
        return chunks;
    }

    public RagResult query(RagQuery query) {
        List<RagScoredChunk> retrieved = retriever.retrieve(query);
        List<RagScoredChunk> reranked = reranker.rerank(query, retrieved, query.topK());
        String answer = generator.generate(query, reranked);
        return new RagResult(query, reranked, answer, Map.of("retrieved", retrieved.size()));
    }
}

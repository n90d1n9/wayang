package tech.kayys.wayang.rag.core.spi;

import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;

public interface Reranker {
    List<RagScoredChunk> rerank(RagQuery query, List<RagScoredChunk> candidates, int topK);
}

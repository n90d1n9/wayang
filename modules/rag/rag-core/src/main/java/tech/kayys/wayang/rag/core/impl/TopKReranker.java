package tech.kayys.wayang.rag.core.impl;

import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.spi.Reranker;

import java.util.Comparator;
import java.util.List;

public class TopKReranker implements Reranker {

    @Override
    public List<RagScoredChunk> rerank(RagQuery query, List<RagScoredChunk> candidates, int topK) {
        if (candidates == null || candidates.isEmpty() || topK <= 0) {
            return List.of();
        }
        return candidates.stream()
                .sorted(Comparator.comparingDouble(RagScoredChunk::score).reversed())
                .limit(topK)
                .toList();
    }
}

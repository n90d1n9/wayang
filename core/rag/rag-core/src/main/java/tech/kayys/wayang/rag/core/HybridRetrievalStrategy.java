package tech.kayys.wayang.rag.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HYBRID RETRIEVAL STRATEGY - INTERNAL IMPLEMENTATION
 */
public class HybridRetrievalStrategy implements RetrievalStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(HybridRetrievalStrategy.class);
    private final DenseRetrievalStrategy denseStrategy;
    private final KeywordRetrievalStrategy keywordStrategy;

    HybridRetrievalStrategy(EmbeddingService embeddingService) {
        this.denseStrategy = new DenseRetrievalStrategy(embeddingService);
        this.keywordStrategy = new KeywordRetrievalStrategy();
    }

    @Override
    public List<ScoredDocument> retrieve(
            String query,
            VectorStore<RagChunk> store,
            RetrievalConfig config) {

        LOG.debug("Hybrid retrieval for query: {}", query);

        // Dense retrieval
        List<ScoredDocument> denseResults = denseStrategy.retrieve(query, store, config);

        // Keyword retrieval
        List<ScoredDocument> keywordResults = keywordStrategy.retrieve(query, store, config);

        // Merge using Reciprocal Rank Fusion
        return mergeWithRRF(denseResults, keywordResults, config.topK());
    }

    private List<ScoredDocument> mergeWithRRF(
            List<ScoredDocument> list1,
            List<ScoredDocument> list2,
            int topK) {

        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, ScoredDocument> docMap = new HashMap<>();
        int k = 60;

        // Process first list
        for (int i = 0; i < list1.size(); i++) {
            ScoredDocument doc = list1.get(i);
            String key = doc.segment().id(); // Using ID as unique key
            rrfScores.put(key, 1.0 / (k + i + 1));
            docMap.put(key, doc);
        }

        // Process second list
        for (int i = 0; i < list2.size(); i++) {
            ScoredDocument doc = list2.get(i);
            String key = doc.segment().id();
            rrfScores.merge(key, 1.0 / (k + i + 1), Double::sum);
            docMap.putIfAbsent(key, doc);
        }

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new ScoredDocument(docMap.get(e.getKey()).segment(), e.getValue()))
                .collect(Collectors.toList());
    }
}
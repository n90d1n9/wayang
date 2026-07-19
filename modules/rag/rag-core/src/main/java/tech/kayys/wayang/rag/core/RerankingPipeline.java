package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RERANKING PIPELINE - FULL IMPLEMENTATION
 */
@ApplicationScoped
class RerankingPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(RerankingPipeline.class);

    public List<ScoredDocument> rerank(
            String query,
            List<ScoredDocument> documents,
            int topK) {

        LOG.debug("Reranking {} documents to top {}", documents.size(), topK);

        // Stage 1: Semantic similarity reranking
        List<ScoredDocument> reranked = rerankBySemantic(query, documents);

        // Stage 2: Sort by combined score
        reranked.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());

        return reranked.stream().limit(topK).collect(Collectors.toList());
    }

    private List<ScoredDocument> rerankBySemantic(String query, List<ScoredDocument> documents) {
        Set<String> queryWords = tokenize(query);

        return documents.stream()
                .map(doc -> {
                    Set<String> docWords = tokenize(doc.segment().text());
                    double semanticScore = calculateJaccardSimilarity(queryWords, docWords);

                    // Combine original score with semantic score
                    double combinedScore = 0.7 * doc.score() + 0.3 * semanticScore;

                    return new ScoredDocument(doc.segment(), combinedScore);
                })
                .collect(Collectors.toList());
    }

    private double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty())
            return 1.0;
        if (set1.isEmpty() || set2.isEmpty())
            return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+"))
                .filter(s -> s.length() > 2)
                .collect(Collectors.toSet());
    }
}
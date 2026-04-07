package tech.kayys.wayang.rag.core;

import java.util.List;
import java.util.Map;

/**
 * Standard retrieval configuration for RAG operations.
 * This class serves as the single source of truth for RAG retrieval parameters.
 */
public class RetrievalConfig {
    private final int topK;
    private final float minSimilarity;
    private final int maxChunkSize;
    private final int chunkOverlap;
    private final boolean enableReranking;
    private final RerankingModel rerankingModel;
    private final boolean enableHybridSearch;
    private final float hybridAlpha;
    private final boolean enableMultiQuery;
    private final int numQueryVariations;
    private final boolean enableMmr;
    private final int mmrLambda;
    private final Map<String, Object> metadataFilters;
    private final List<String> excludedFields;
    private final boolean enableGrouping;
    private final boolean enableDeduplication;

    public RetrievalConfig(int topK, float minSimilarity, int maxChunkSize, int chunkOverlap,
            boolean enableReranking, RerankingModel rerankingModel, boolean enableHybridSearch,
            float hybridAlpha, boolean enableMultiQuery, int numQueryVariations,
            boolean enableMmr, int mmrLambda, Map<String, Object> metadataFilters,
            List<String> excludedFields, boolean enableGrouping, boolean enableDeduplication) {
        this.topK = topK;
        this.minSimilarity = minSimilarity;
        this.maxChunkSize = maxChunkSize;
        this.chunkOverlap = chunkOverlap;
        this.enableReranking = enableReranking;
        this.rerankingModel = rerankingModel;
        this.enableHybridSearch = enableHybridSearch;
        this.hybridAlpha = hybridAlpha;
        this.enableMultiQuery = enableMultiQuery;
        this.numQueryVariations = numQueryVariations;
        this.enableMmr = enableMmr;
        this.mmrLambda = mmrLambda;
        this.metadataFilters = metadataFilters != null ? metadataFilters : Map.of();
        this.excludedFields = excludedFields != null ? excludedFields : List.of();
        this.enableGrouping = enableGrouping;
        this.enableDeduplication = enableDeduplication;
    }

    public static RetrievalConfig defaults() {
        return new RetrievalConfig(5, 0.5f, 512, 50, false, RerankingModel.COHERE_RERANK,
                false, 0.7f, false, 3, false, 0, Map.of(),
                List.of(), false, false);
    }

    public int topK() {
        return topK;
    }

    public float minSimilarity() {
        return minSimilarity;
    }

    public int maxChunkSize() {
        return maxChunkSize;
    }

    public int chunkOverlap() {
        return chunkOverlap;
    }

    public boolean enableReranking() {
        return enableReranking;
    }

    public RerankingModel rerankingModel() {
        return rerankingModel;
    }

    public boolean enableHybridSearch() {
        return enableHybridSearch;
    }

    public float hybridAlpha() {
        return hybridAlpha;
    }

    public boolean enableMultiQuery() {
        return enableMultiQuery;
    }

    public int numQueryVariations() {
        return numQueryVariations;
    }

    public boolean enableMmr() {
        return enableMmr;
    }

    public int mmrLambda() {
        return mmrLambda;
    }

    public Map<String, Object> metadataFilters() {
        return metadataFilters;
    }

    public List<String> excludedFields() {
        return excludedFields;
    }

    public boolean enableGrouping() {
        return enableGrouping;
    }

    public boolean enableDeduplication() {
        return enableDeduplication;
    }

    // Compatibility method for DenseRetrievalStrategy
    public double minScore() {
        return (double) minSimilarity;
    }
}
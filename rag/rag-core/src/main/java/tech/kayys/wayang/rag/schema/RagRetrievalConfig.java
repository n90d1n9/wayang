package tech.kayys.wayang.rag.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import java.util.Map;

/**
 * Strongly-typed DTO for RAG retrieval configuration.
 *
 * Mirrors the fields in {@code tech.kayys.wayang.rag.core.RetrievalConfig}.
 */
public class RagRetrievalConfig {

    @JsonProperty("topK")
    @JsonPropertyDescription("Number of top-K documents to retrieve. Defaults to 5.")
    private Integer topK = 5;

    @JsonProperty("minSimilarity")
    @JsonPropertyDescription("Minimum cosine similarity score (0.0–1.0) for a document to be included. Defaults to 0.5.")
    private Float minSimilarity = 0.5f;

    @JsonProperty("maxChunkSize")
    @JsonPropertyDescription("Maximum token size for each retrieved chunk. Defaults to 512.")
    private Integer maxChunkSize = 512;

    @JsonProperty("chunkOverlap")
    @JsonPropertyDescription("Overlap in tokens between adjacent chunks to preserve context. Defaults to 50.")
    private Integer chunkOverlap = 50;

    @JsonProperty("enableReranking")
    @JsonPropertyDescription("Whether to apply a semantic reranking model after initial retrieval. Defaults to false.")
    private Boolean enableReranking = false;

    @JsonProperty("rerankingModel")
    @JsonPropertyDescription("Reranking model to use when enableReranking is true. E.g. COHERE_RERANK, BGE_RERANKER.")
    private String rerankingModel = "COHERE_RERANK";

    @JsonProperty("enableHybridSearch")
    @JsonPropertyDescription("Whether to combine dense vector search with keyword (BM25) search. Defaults to false.")
    private Boolean enableHybridSearch = false;

    @JsonProperty("hybridAlpha")
    @JsonPropertyDescription("Weight for dense vs keyword results in hybrid mode (0.0 = pure keyword, 1.0 = pure dense). Defaults to 0.7.")
    private Float hybridAlpha = 0.7f;

    @JsonProperty("enableMultiQuery")
    @JsonPropertyDescription("Whether to generate multiple query variations for broader coverage. Defaults to false.")
    private Boolean enableMultiQuery = false;

    @JsonProperty("numQueryVariations")
    @JsonPropertyDescription("Number of extra query variations to generate when enableMultiQuery is true. Defaults to 3.")
    private Integer numQueryVariations = 3;

    @JsonProperty("enableMmr")
    @JsonPropertyDescription("Whether to apply Maximal Marginal Relevance (MMR) to diversify results. Defaults to false.")
    private Boolean enableMmr = false;

    @JsonProperty("mmrLambda")
    @JsonPropertyDescription("Lambda parameter for MMR (0 = max diversity, 1 = max relevance). Defaults to 0.")
    private Integer mmrLambda = 0;

    @JsonProperty("metadataFilters")
    @JsonPropertyDescription("Optional key-value pairs to filter documents by metadata before scoring.")
    private Map<String, Object> metadataFilters;

    @JsonProperty("excludedFields")
    @JsonPropertyDescription("Metadata fields to exclude from retrieved documents in the response.")
    private List<String> excludedFields;

    @JsonProperty("enableGrouping")
    @JsonPropertyDescription("Whether to group retrieved chunks by source document. Defaults to false.")
    private Boolean enableGrouping = false;

    @JsonProperty("enableDeduplication")
    @JsonPropertyDescription("Whether to deduplicate near-identical retrieved chunks. Defaults to false.")
    private Boolean enableDeduplication = false;

    public RagRetrievalConfig() {
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Float getMinSimilarity() {
        return minSimilarity;
    }

    public void setMinSimilarity(Float minSimilarity) {
        this.minSimilarity = minSimilarity;
    }

    public Integer getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(Integer maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public Boolean getEnableReranking() {
        return enableReranking;
    }

    public void setEnableReranking(Boolean enableReranking) {
        this.enableReranking = enableReranking;
    }

    public String getRerankingModel() {
        return rerankingModel;
    }

    public void setRerankingModel(String rerankingModel) {
        this.rerankingModel = rerankingModel;
    }

    public Boolean getEnableHybridSearch() {
        return enableHybridSearch;
    }

    public void setEnableHybridSearch(Boolean enableHybridSearch) {
        this.enableHybridSearch = enableHybridSearch;
    }

    public Float getHybridAlpha() {
        return hybridAlpha;
    }

    public void setHybridAlpha(Float hybridAlpha) {
        this.hybridAlpha = hybridAlpha;
    }

    public Boolean getEnableMultiQuery() {
        return enableMultiQuery;
    }

    public void setEnableMultiQuery(Boolean enableMultiQuery) {
        this.enableMultiQuery = enableMultiQuery;
    }

    public Integer getNumQueryVariations() {
        return numQueryVariations;
    }

    public void setNumQueryVariations(Integer numQueryVariations) {
        this.numQueryVariations = numQueryVariations;
    }

    public Boolean getEnableMmr() {
        return enableMmr;
    }

    public void setEnableMmr(Boolean enableMmr) {
        this.enableMmr = enableMmr;
    }

    public Integer getMmrLambda() {
        return mmrLambda;
    }

    public void setMmrLambda(Integer mmrLambda) {
        this.mmrLambda = mmrLambda;
    }

    public Map<String, Object> getMetadataFilters() {
        return metadataFilters;
    }

    public void setMetadataFilters(Map<String, Object> metadataFilters) {
        this.metadataFilters = metadataFilters;
    }

    public List<String> getExcludedFields() {
        return excludedFields;
    }

    public void setExcludedFields(List<String> excludedFields) {
        this.excludedFields = excludedFields;
    }

    public Boolean getEnableGrouping() {
        return enableGrouping;
    }

    public void setEnableGrouping(Boolean enableGrouping) {
        this.enableGrouping = enableGrouping;
    }

    public Boolean getEnableDeduplication() {
        return enableDeduplication;
    }

    public void setEnableDeduplication(Boolean enableDeduplication) {
        this.enableDeduplication = enableDeduplication;
    }
}

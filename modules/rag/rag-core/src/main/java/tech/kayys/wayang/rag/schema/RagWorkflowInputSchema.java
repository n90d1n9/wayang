package tech.kayys.wayang.rag.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Schema DTO for a RAG workflow input payload.
 */
public class RagWorkflowInputSchema {

    @JsonProperty("tenantId")
    @JsonPropertyDescription("Tenant identifier for multi-tenant deployments.")
    private String tenantId;

    @JsonProperty("query")
    @JsonPropertyDescription("The user question or search query. Required.")
    private String query;

    @JsonProperty("retrievalConfig")
    @JsonPropertyDescription("Retrieval configuration for the RAG pipeline.")
    private RagRetrievalConfig retrievalConfig;

    @JsonProperty("generationConfig")
    @JsonPropertyDescription("Generation (LLM) configuration for the RAG pipeline.")
    private RagGenerationConfig generationConfig;

    public RagWorkflowInputSchema() {
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public RagRetrievalConfig getRetrievalConfig() {
        return retrievalConfig;
    }

    public void setRetrievalConfig(RagRetrievalConfig retrievalConfig) {
        this.retrievalConfig = retrievalConfig;
    }

    public RagGenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(RagGenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }
}

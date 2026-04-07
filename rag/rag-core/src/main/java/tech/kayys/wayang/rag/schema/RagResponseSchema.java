package tech.kayys.wayang.rag.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;
import java.util.Map;

/**
 * Schema DTO for the full RAG response output.
 */
public class RagResponseSchema {

    @JsonProperty("query")
    @JsonPropertyDescription("The original user query.")
    private String query;

    @JsonProperty("answer")
    @JsonPropertyDescription("The generated answer from the LLM, grounded in the retrieved context.")
    private String answer;

    @JsonProperty("sourceDocuments")
    @JsonPropertyDescription("List of source documents retrieved for context.")
    private List<RagSourceDocumentSchema> sourceDocuments;

    @JsonProperty("citations")
    @JsonPropertyDescription("List of citation references within the generated answer.")
    private List<RagCitationSchema> citations;

    @JsonProperty("metrics")
    @JsonPropertyDescription("Performance and quality metrics for this RAG operation.")
    private RagMetricsSchema metrics;

    @JsonProperty("context")
    @JsonPropertyDescription("The combined context text provided to the LLM.")
    private String context;

    @JsonProperty("timestamp")
    @JsonPropertyDescription("ISO-8601 timestamp of when the response was generated.")
    private String timestamp;

    @JsonProperty("metadata")
    @JsonPropertyDescription("Additional metadata about the RAG execution.")
    private Map<String, Object> metadata;

    @JsonProperty("sources")
    @JsonPropertyDescription("List of collection names that were queried.")
    private List<String> sources;

    @JsonProperty("error")
    @JsonPropertyDescription("Error message if the RAG operation failed. Null on success.")
    private String error;

    public RagResponseSchema() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<RagSourceDocumentSchema> getSourceDocuments() {
        return sourceDocuments;
    }

    public void setSourceDocuments(List<RagSourceDocumentSchema> sourceDocuments) {
        this.sourceDocuments = sourceDocuments;
    }

    public List<RagCitationSchema> getCitations() {
        return citations;
    }

    public void setCitations(List<RagCitationSchema> citations) {
        this.citations = citations;
    }

    public RagMetricsSchema getMetrics() {
        return metrics;
    }

    public void setMetrics(RagMetricsSchema metrics) {
        this.metrics = metrics;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

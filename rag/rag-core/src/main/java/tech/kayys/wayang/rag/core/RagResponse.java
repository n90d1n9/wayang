package tech.kayys.wayang.rag.core;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Standard response object for RAG operations.
 */
public class RagResponse {
    private final String query;
    private final String answer;
    private final List<SourceDocument> sourceDocuments;
    private final List<Citation> citations;
    private final RagMetrics metrics;
    private final String context;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    private final List<String> sources;
    private final Optional<String> error;

    public RagResponse(String query, String answer, List<SourceDocument> sourceDocuments,
            List<Citation> citations, RagMetrics metrics, String context,
            Instant timestamp, Map<String, Object> metadata, List<String> sources,
            Optional<String> error) {
        this.query = query;
        this.answer = answer;
        this.sourceDocuments = sourceDocuments != null ? sourceDocuments : List.of();
        this.citations = citations != null ? citations : List.of();
        this.metrics = metrics;
        this.context = context;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.metadata = metadata != null ? metadata : Map.of();
        this.sources = sources != null ? sources : List.of();
        this.error = error != null ? error : Optional.empty();
    }

    public String query() {
        return query;
    }

    public String answer() {
        return answer;
    }

    public List<SourceDocument> sourceDocuments() {
        return sourceDocuments;
    }

    public List<Citation> citations() {
        return citations;
    }

    public RagMetrics metrics() {
        return metrics;
    }

    public String context() {
        return context;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public List<String> sources() {
        return sources;
    }

    public Optional<String> error() {
        return error;
    }
}

package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import io.smallrye.mutiny.Uni;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * RAG-aware skill context provider.
 * 
 * Integrates skills with RAG (Retrieval-Augmented Generation) pipeline,
 * allowing skills to enhance retrieval, ranking, and generation.
 */
public class RAGAwareSkillContext {

    private final SkillContext context;
    private final Map<String, Object> ragMetadata;

    public RAGAwareSkillContext(SkillContext context) {
        this.context = context;
        this.ragMetadata = new HashMap<>();
    }

    /**
     * Add RAG-specific metadata (query, documents, rankings).
     */
    public RAGAwareSkillContext withRAGMetadata(String key, Object value) {
        ragMetadata.put(key, value);
        return this;
    }

    /**
     * Get query being augmented.
     */
    public Optional<String> getQuery() {
        return Optional.ofNullable((String) ragMetadata.get("query"));
    }

    /**
     * Get retrieved documents.
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> getDocuments() {
        Object docs = ragMetadata.get("documents");
        return docs instanceof java.util.List
            ? (java.util.List<String>) docs
            : java.util.List.of();
    }

    /**
     * Enhance RAG results with skill context.
     */
    public Map<String, Object> enrichRAGResults(Map<String, Object> results) {
        Map<String, Object> enriched = new HashMap<>(results);
        
        if (context.metadata() != null) {
            enriched.put("skill_id", context.metadata().name());
            enriched.put("skill_tags", context.metadata().tags());
            enriched.put("skill_version", context.metadata().version());
        }
        
        return enriched;
    }

    /**
     * Compute skill-specific ranking scores for documents.
     */
    public Uni<Map<String, Double>> computeSkillScores() {
        return Uni.createFrom().item(() -> {
            Map<String, Double> scores = new HashMap<>();
            getDocuments().forEach(doc ->
                scores.put(doc, 0.5) // Placeholder scoring
            );
            return scores;
        });
    }

    /**
     * Create RAG-aware skill context asynchronously.
     */
    public Uni<RAGAwareSkillContext> enrich() {
        return Uni.createFrom().item(this);
    }

    /**
     * Get all RAG metadata.
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(ragMetadata);
    }
}

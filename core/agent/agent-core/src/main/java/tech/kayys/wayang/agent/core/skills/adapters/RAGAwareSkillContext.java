package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillContextKeys;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import io.smallrye.mutiny.Uni;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        this.context = Objects.requireNonNull(context, "context");
        this.ragMetadata = new LinkedHashMap<>();
    }

    /**
     * Add RAG-specific metadata (query, documents, rankings).
     */
    public RAGAwareSkillContext withRAGMetadata(String key, Object value) {
        if (hasText(key) && value != null) {
            ragMetadata.put(key.trim(), snapshotValue(value));
        }
        return this;
    }

    /**
     * Get query being augmented.
     */
    public Optional<String> getQuery() {
        Object query = ragMetadata.get(SkillContextKeys.RAG_QUERY);
        if (!(query instanceof String text) || text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(text.trim());
    }

    /**
     * Get retrieved documents.
     */
    public List<String> getDocuments() {
        Object docs = ragMetadata.get(SkillContextKeys.RAG_DOCUMENTS);
        if (!(docs instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(String::trim)
                .filter(RAGAwareSkillContext::hasText)
                .toList();
    }

    /**
     * Enhance RAG results with skill context.
     */
    public Map<String, Object> enrichRAGResults(Map<String, Object> results) {
        Map<String, Object> enriched = copyMap(results);
        if (hasText(context.skillId())) {
            enriched.put(SkillContextKeys.WIRE_SKILL_ID, context.skillId().trim());
        }

        SkillMetadata metadata = context.metadata();
        if (metadata != null) {
            if (hasText(metadata.name())) {
                enriched.put(SkillContextKeys.WIRE_SKILL_NAME, metadata.name());
            }
            enriched.put(SkillContextKeys.WIRE_SKILL_TAGS, List.copyOf(metadata.tags()));
            enriched.put(SkillContextKeys.WIRE_SKILL_VERSION, metadata.version());
        }
        
        return Map.copyOf(enriched);
    }

    /**
     * Compute skill-specific ranking scores for documents.
     */
    public Uni<Map<String, Double>> computeSkillScores() {
        return Uni.createFrom().item(() -> {
            Map<String, Double> scores = new LinkedHashMap<>();
            getDocuments().forEach(doc -> scores.putIfAbsent(doc, 0.5)); // Placeholder scoring
            return Map.copyOf(scores);
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
        return Map.copyOf(ragMetadata);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Object snapshotValue(Object value) {
        if (value instanceof List<?> list) {
            return List.copyOf(list.stream()
                    .filter(Objects::nonNull)
                    .toList());
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copied = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                if (key != null && item != null) {
                    copied.put(key, item);
                }
            });
            return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
        }
        return value;
    }

    private static Map<String, Object> copyMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (hasText(key) && value != null) {
                copied.put(key.trim(), snapshotValue(value));
            }
        });
        return copied;
    }
}

package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import io.smallrye.mutiny.Uni;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vector-aware skill indexer for semantic search and embedding integration.
 * 
 * Indexes skill metadata in vector databases, enabling semantic discovery
 * and similarity-based skill retrieval through embeddings.
 */
public class VectorSkillIndexer {

    private final SkillRegistry skillRegistry;
    private final Map<String, EmbeddingVector> vectorIndex;

    public VectorSkillIndexer(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
        this.vectorIndex = new HashMap<>();
    }

    /**
     * Index all skills with vector embeddings.
     */
    public Uni<Void> indexAllSkills() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                skillRegistry.list().forEach(skill -> {
                    String skillId = skill.id();
                    String text = createIndexableText(skill);
                    EmbeddingVector vector = generateEmbedding(text);
                    vectorIndex.put(skillId, vector);
                });
                System.out.println("[Vector] Indexed " + vectorIndex.size() + " skills");
            });
    }

    /**
     * Search skills by semantic similarity.
     */
    public Uni<List<SkillSimilarityResult>> searchByEmbedding(String query, int topK) {
        return Uni.createFrom().item(() -> {
            EmbeddingVector queryVector = generateEmbedding(query);
            
            return vectorIndex.entrySet()
                .stream()
                .map(entry -> new SkillSimilarityResult(
                    entry.getKey(),
                    computeSimilarity(queryVector, entry.getValue())
                ))
                .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
                .limit(topK)
                .collect(Collectors.toList());
        });
    }

    /**
     * Get vector for skill.
     */
    public SkillVectorRepresentation getSkillVector(String skillId) {
        SkillDefinition skill = skillRegistry.get(skillId).await().indefinitely();
        EmbeddingVector vector = vectorIndex.get(skillId);
        
        return new SkillVectorRepresentation(
            skillId,
            skill.metadata().name(),
            vector != null ? vector.values : new double[0],
            skill.metadata().tags()
        );
    }

    /**
     * Update skill index when skills are added/modified.
     */
    public Uni<Void> updateSkillIndex(SkillDefinition skill) {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                String skillId = skill.id();
                String text = createIndexableText(skill);
                EmbeddingVector vector = generateEmbedding(text);
                vectorIndex.put(skillId, vector);
            });
    }

    /**
     * Create indexable text from skill metadata.
     */
    private String createIndexableText(SkillDefinition skill) {
        SkillMetadata meta = skill.metadata();
        return String.format("%s %s %s %s",
            meta.name(),
            meta.description(),
            String.join(" ", meta.tags()),
            meta.category()
        );
    }

    /**
     * Generate embedding vector (placeholder - would use real embedding model).
     */
    private EmbeddingVector generateEmbedding(String text) {
        double[] values = new double[384]; // Typical embedding size
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.random() * 2 - 1; // Random for demo
        }
        return new EmbeddingVector(values);
    }

    /**
     * Compute cosine similarity between vectors.
     */
    private double computeSimilarity(EmbeddingVector v1, EmbeddingVector v2) {
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        
        for (int i = 0; i < v1.values.length && i < v2.values.length; i++) {
            dotProduct += v1.values[i] * v2.values[i];
            norm1 += v1.values[i] * v1.values[i];
            norm2 += v2.values[i] * v2.values[i];
        }
        
        return norm1 * norm2 == 0 ? 0 : dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Embedding vector representation.
     */
    private record EmbeddingVector(double[] values) {}

    /**
     * Skill similarity result from vector search.
     */
    public record SkillSimilarityResult(String skillId, double similarity) {}

    /**
     * Skill vector representation.
     */
    public record SkillVectorRepresentation(
        String skillId,
        String skillName,
        double[] vector,
        List<String> tags
    ) {}
}

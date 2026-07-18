package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vector-aware skill indexer for semantic search and embedding integration.
 *
 * Indexes skill metadata in vector databases, enabling semantic discovery
 * and similarity-based skill retrieval through embeddings.
 */
public class VectorSkillIndexer {

    private static final Logger LOG = Logger.getLogger(VectorSkillIndexer.class);
    private static final int EMBEDDING_DIMENSIONS = 384;

    private final SkillRegistry skillRegistry;
    private final Map<String, EmbeddingVector> vectorIndex;

    public VectorSkillIndexer(SkillRegistry skillRegistry) {
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
        this.vectorIndex = new ConcurrentHashMap<>();
    }

    /**
     * Index all skills with vector embeddings.
     */
    public Uni<Void> indexAllSkills() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                Map<String, EmbeddingVector> indexed = new LinkedHashMap<>();
                skillRegistry.list().forEach(skill -> {
                    if (skill != null && hasText(skill.id())) {
                        indexed.put(skill.id().trim(), generateEmbedding(createIndexableText(skill)));
                    }
                });
                vectorIndex.clear();
                vectorIndex.putAll(indexed);
                LOG.debugf("Indexed %d skills for vector search", vectorIndex.size());
            });
    }

    /**
     * Search skills by semantic similarity.
     */
    public Uni<List<SkillSimilarityResult>> searchByEmbedding(String query, int topK) {
        return Uni.createFrom().item(() -> {
            if (!hasText(query) || topK <= 0 || vectorIndex.isEmpty()) {
                return List.of();
            }
            EmbeddingVector queryVector = generateEmbedding(query);

            return vectorIndex.entrySet()
                .stream()
                .map(entry -> new SkillSimilarityResult(
                    entry.getKey(),
                    computeSimilarity(queryVector, entry.getValue())
                ))
                .sorted((a, b) -> {
                    int similarity = Double.compare(b.similarity(), a.similarity());
                    return similarity != 0 ? similarity : a.skillId().compareTo(b.skillId());
                })
                .limit(topK)
                .toList();
        });
    }

    /**
     * Get vector for skill.
     */
    public SkillVectorRepresentation getSkillVector(String skillId) {
        String normalizedSkillId = normalizeSkillId(skillId);
        SkillDefinition skill = findSkill(normalizedSkillId);
        EmbeddingVector vector = vectorIndex.get(normalizedSkillId);

        return new SkillVectorRepresentation(
            normalizedSkillId,
            skill == null || !hasText(skill.name()) ? normalizedSkillId : skill.name(),
            vector == null ? new double[0] : vector.values(),
            tagsOf(skill)
        );
    }

    /**
     * Update skill index when skills are added/modified.
     */
    public Uni<Void> updateSkillIndex(SkillDefinition skill) {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                if (skill != null && hasText(skill.id())) {
                    vectorIndex.put(skill.id().trim(), generateEmbedding(createIndexableText(skill)));
                }
            });
    }

    /**
     * Refresh changed skills and retire removed skills without rebuilding the full index.
     */
    public Uni<Void> refreshSkillIndex(List<SkillDefinition> changedSkills, List<String> removedSkillIds) {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                normalizedIds(removedSkillIds).forEach(vectorIndex::remove);
                if (changedSkills != null) {
                    changedSkills.stream()
                            .filter(skill -> skill != null && hasText(skill.id()))
                            .forEach(skill -> vectorIndex.put(
                                    skill.id().trim(),
                                    generateEmbedding(createIndexableText(skill))));
                }
                LOG.debugf("Refreshed vector skill index (changed=%d, removed=%d)",
                        changedSkills == null ? 0 : changedSkills.size(),
                        removedSkillIds == null ? 0 : removedSkillIds.size());
            });
    }

    /**
     * Create indexable text from skill metadata.
     */
    private String createIndexableText(SkillDefinition skill) {
        if (skill == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, skill.id());
        addIfPresent(parts, skill.name());
        addIfPresent(parts, skill.description());
        addIfPresent(parts, skill.category());
        addIfPresent(parts, skill.systemPrompt());
        parts.addAll(tagsOf(skill));
        return String.join(" ", parts);
    }

    private List<String> tagsOf(SkillDefinition skill) {
        if (skill == null || skill.metadata() == null) {
            return List.of();
        }
        return SkillMetadataKeys.tags(skill.metadata());
    }

    /**
     * Generate embedding vector (placeholder - would use real embedding model).
     */
    private EmbeddingVector generateEmbedding(String text) {
        double[] values = new double[EMBEDDING_DIMENSIONS];
        for (String token : tokens(text)) {
            int hash = token.hashCode();
            int index = Math.floorMod(hash, values.length);
            values[index] += (hash & 1) == 0 ? 1.0 : -1.0;
        }
        normalize(values);
        return new EmbeddingVector(values);
    }

    /**
     * Compute cosine similarity between vectors.
     */
    private double computeSimilarity(EmbeddingVector v1, EmbeddingVector v2) {
        double[] values1 = v1.values();
        double[] values2 = v2.values();
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (int i = 0; i < values1.length && i < values2.length; i++) {
            dotProduct += values1[i] * values2[i];
            norm1 += values1[i] * values1[i];
            norm2 += values2[i] * values2[i];
        }

        return norm1 * norm2 == 0 ? 0 : dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private SkillDefinition findSkill(String skillId) {
        if (!hasText(skillId)) {
            return null;
        }
        try {
            return skillRegistry.get(skillId).await().indefinitely();
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static List<String> tokens(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(VectorSkillIndexer::hasText)
                .toList();
    }

    private static void normalize(double[] values) {
        double norm = 0;
        for (double value : values) {
            norm += value * value;
        }
        if (norm == 0) {
            return;
        }
        double scale = Math.sqrt(norm);
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] / scale;
        }
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (hasText(value)) {
            parts.add(value.trim());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeSkillId(String skillId) {
        return hasText(skillId) ? skillId.trim() : "";
    }

    private static List<String> normalizedIds(List<String> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return List.of();
        }
        return skillIds.stream()
                .filter(VectorSkillIndexer::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * Embedding vector representation.
     */
    private record EmbeddingVector(double[] values) {
        private EmbeddingVector {
            values = values == null ? new double[0] : values.clone();
        }

        @Override
        public double[] values() {
            return values.clone();
        }
    }

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
    ) {
        public SkillVectorRepresentation {
            skillId = skillId == null ? "" : skillId;
            skillName = skillName == null ? "" : skillName;
            vector = vector == null ? new double[0] : vector.clone();
            tags = tags == null ? List.of() : List.copyOf(tags);
        }

        @Override
        public double[] vector() {
            return vector.clone();
        }
    }
}

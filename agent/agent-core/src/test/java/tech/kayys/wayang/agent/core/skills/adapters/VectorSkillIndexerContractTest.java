package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.support.TestSkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorSkillIndexerContractTest {

    @Test
    void indexesSkillsAndSearchesDeterministically() {
        VectorSkillIndexer indexer = new VectorSkillIndexer(registry());

        indexer.indexAllSkills().await().indefinitely();

        List<VectorSkillIndexer.SkillSimilarityResult> first = indexer.searchByEmbedding("semantic search", 2)
                .await().indefinitely();
        List<VectorSkillIndexer.SkillSimilarityResult> second = indexer.searchByEmbedding("semantic search", 2)
                .await().indefinitely();

        assertEquals(2, first.size());
        assertEquals(first, second);
        assertEquals("search-skill", first.getFirst().skillId());
        assertTrue(first.getFirst().similarity() >= first.getLast().similarity());
    }

    @Test
    void returnsEmptyResultsForBlankQueryTopKOrEmptyIndex() {
        VectorSkillIndexer indexer = new VectorSkillIndexer(registry());

        assertTrue(indexer.searchByEmbedding("semantic search", 5).await().indefinitely().isEmpty());

        indexer.indexAllSkills().await().indefinitely();

        assertTrue(indexer.searchByEmbedding(" ", 5).await().indefinitely().isEmpty());
        assertTrue(indexer.searchByEmbedding("semantic search", 0).await().indefinitely().isEmpty());
    }

    @Test
    void exposesDefensiveVectorRepresentationWithMetadata() {
        VectorSkillIndexer indexer = new VectorSkillIndexer(registry());
        indexer.indexAllSkills().await().indefinitely();

        VectorSkillIndexer.SkillVectorRepresentation vector = indexer.getSkillVector("search-skill");
        double[] mutated = vector.vector();
        mutated[0] = 999.0;

        assertEquals("search-skill", vector.skillId());
        assertEquals("Search Skill", vector.skillName());
        assertEquals(384, vector.vector().length);
        assertEquals(List.of("search", "semantic"), vector.tags());
        assertNotEquals(999.0, vector.vector()[0]);
        assertThrows(UnsupportedOperationException.class, () -> vector.tags().add("new"));
    }

    @Test
    void updateSkillIndexAddsOrReplacesSingleSkill() {
        VectorSkillIndexer indexer = new VectorSkillIndexer(TestSkillRegistry.of());

        indexer.updateSkillIndex(skill(
                "rag-skill",
                "RAG Skill",
                "retrieves documents and answers questions",
                "retrieval",
                List.of("rag", "retrieval"))).await().indefinitely();

        List<VectorSkillIndexer.SkillSimilarityResult> results = indexer.searchByEmbedding("retrieves documents", 3)
                .await().indefinitely();

        assertEquals(1, results.size());
        assertEquals("rag-skill", results.getFirst().skillId());
    }

    @Test
    void returnsEmptyVectorForMissingSkillWithoutThrowing() {
        VectorSkillIndexer indexer = new VectorSkillIndexer(TestSkillRegistry.of());

        VectorSkillIndexer.SkillVectorRepresentation vector = indexer.getSkillVector("missing");

        assertEquals("missing", vector.skillId());
        assertEquals("missing", vector.skillName());
        assertArrayEquals(new double[0], vector.vector());
        assertTrue(vector.tags().isEmpty());
    }

    @Test
    void requiresRegistry() {
        assertThrows(NullPointerException.class, () -> new VectorSkillIndexer(null));
    }

    private static TestSkillRegistry registry() {
        return TestSkillRegistry.of(
                skill("search-skill", "Search Skill", "performs semantic search", "retrieval",
                        List.of("search", "semantic")),
                skill("analysis-skill", "Analysis Skill", "analyzes data patterns", "analysis",
                        List.of("analysis", "data")),
                skill("generation-skill", "Generation Skill", "generates content", "generation",
                        List.of("generation", "content")));
    }

    private static SkillDefinition skill(
            String id,
            String name,
            String description,
            String category,
            List<String> tags) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(SkillMetadataKeys.KEY_VERSION, SkillMetadataKeys.DEFAULT_VERSION);
        metadata.put(SkillMetadataKeys.KEY_TAGS, tags);
        return SkillDefinition.builder()
                .id(id)
                .name(name)
                .description(description)
                .category(category)
                .systemPrompt("Use " + name)
                .metadata(metadata)
                .build();
    }
}

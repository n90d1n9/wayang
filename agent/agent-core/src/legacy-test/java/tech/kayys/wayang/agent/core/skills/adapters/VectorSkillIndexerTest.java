package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VectorSkillIndexer Tests")
class VectorSkillIndexerTest {

    private VectorSkillIndexer indexer;
    private MockSkillRegistry mockRegistry;

    @BeforeEach
    void setUp() {
        mockRegistry = new MockSkillRegistry();
        indexer = new VectorSkillIndexer(mockRegistry);
    }

    @Test
    @DisplayName("Should index all skills")
    void testIndexAllSkills() {
        indexer.indexAllSkills().await().indefinitely();
        
        // Indexing completed without exception
        assertTrue(true);
    }

    @Test
    @DisplayName("Should search skills by embedding")
    void testSearchByEmbedding() {
        indexer.indexAllSkills().await().indefinitely();
        
        var results = indexer.searchByEmbedding("search query", 5)
            .await().indefinitely();

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 5);
        
        // Results should be sorted by similarity
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).similarity() >= results.get(i + 1).similarity());
        }
    }

    @Test
    @DisplayName("Should return top K results")
    void testTopKResults() {
        indexer.indexAllSkills().await().indefinitely();
        
        var results = indexer.searchByEmbedding("query", 2)
            .await().indefinitely();

        assertTrue(results.size() <= 2);
    }

    @Test
    @DisplayName("Should get skill vector representation")
    void testGetSkillVector() {
        indexer.indexAllSkills().await().indefinitely();
        
        VectorSkillIndexer.SkillVectorRepresentation vector = 
            indexer.getSkillVector("skill-1");

        assertNotNull(vector);
        assertEquals("skill-1", vector.skillId());
        assertFalse(vector.vector().length == 0);
        assertFalse(vector.tags().isEmpty());
    }

    @Test
    @DisplayName("Should update skill index")
    void testUpdateSkillIndex() {
        var skill = new MockSkillDefinition("new-skill", "New Skill", "A new skill");
        
        indexer.updateSkillIndex(skill).await().indefinitely();
        
        // Update completed without exception
        assertTrue(true);
    }

    @Test
    @DisplayName("Should compute cosine similarity correctly")
    void testCosineSimilarity() {
        indexer.indexAllSkills().await().indefinitely();
        
        var results = indexer.searchByEmbedding("test", 1)
            .await().indefinitely();

        assertFalse(results.isEmpty());
        VectorSkillIndexer.SkillSimilarityResult result = results.get(0);
        
        // Similarity should be between -1 and 1 for cosine similarity
        assertTrue(result.similarity() >= -1.0 && result.similarity() <= 1.0);
    }

    @Test
    @DisplayName("Should handle empty search results gracefully")
    void testEmptySearchResults() {
        indexer = new VectorSkillIndexer(new EmptySkillRegistry());
        
        var results = indexer.searchByEmbedding("query", 5)
            .await().indefinitely();

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should preserve skill metadata in vector representation")
    void testSkillMetadataPreservation() {
        indexer.indexAllSkills().await().indefinitely();
        
        VectorSkillIndexer.SkillVectorRepresentation vector = 
            indexer.getSkillVector("skill-1");

        assertNotNull(vector.skillName());
        assertFalse(vector.tags().isEmpty());
        assertTrue(vector.tags().contains("test"));
    }

    // Mock skill registry for testing
    private static class MockSkillRegistry implements tech.kayys.wayang.agent.spi.skills.SkillRegistry {
        @Override
        public List<tech.kayys.wayang.agent.spi.skills.SkillDefinition> list() {
            return List.of(
                new MockSkillDefinition("skill-1", "Search Skill", "Performs semantic search"),
                new MockSkillDefinition("skill-2", "Analysis Skill", "Analyzes data patterns"),
                new MockSkillDefinition("skill-3", "Generation Skill", "Generates content")
            );
        }

        @Override
        public io.smallrye.mutiny.Uni<tech.kayys.wayang.agent.spi.skills.SkillDefinition> get(String skillId) {
            return io.smallrye.mutiny.Uni.createFrom().item(
                new MockSkillDefinition(skillId, "Test Skill", "A test skill")
            );
        }

        @Override
        public io.smallrye.mutiny.Uni<tech.kayys.wayang.agent.spi.skills.SkillResult> executeSkill(
            String skillId, 
            java.util.Map<String, Object> input) {
            return io.smallrye.mutiny.Uni.createFrom().item(
                new tech.kayys.wayang.agent.spi.skills.SkillResult(
                    skillId, "invoc-123",
                    tech.kayys.wayang.agent.spi.skills.SkillResult.Status.SUCCESS,
                    "Success", true
                )
            );
        }
    }

    private static class EmptySkillRegistry implements tech.kayys.wayang.agent.spi.skills.SkillRegistry {
        @Override
        public List<tech.kayys.wayang.agent.spi.skills.SkillDefinition> list() {
            return List.of();
        }

        @Override
        public io.smallrye.mutiny.Uni<tech.kayys.wayang.agent.spi.skills.SkillDefinition> get(String skillId) {
            return io.smallrye.mutiny.Uni.createFrom().failure(new IllegalArgumentException("Not found"));
        }

        @Override
        public io.smallrye.mutiny.Uni<tech.kayys.wayang.agent.spi.skills.SkillResult> executeSkill(
            String skillId, 
            java.util.Map<String, Object> input) {
            return io.smallrye.mutiny.Uni.createFrom().failure(new IllegalArgumentException("Not found"));
        }
    }

    private static class MockSkillDefinition implements tech.kayys.wayang.agent.spi.skills.SkillDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final SkillMetadata metadata;

        MockSkillDefinition(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.metadata = new SkillMetadata(
                id, name, description, "1.0.0", "test",
                List.of("test", "demo"), null
            );
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public SkillMetadata metadata() {
            return metadata;
        }
    }
}

package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RAGAwareSkillContext Tests")
class RAGAwareSkillContextTest {

    private SkillContext mockContext;
    private RAGAwareSkillContext ragContext;

    @BeforeEach
    void setUp() {
        SkillMetadata mockMetadata = new SkillMetadata(
            "search-skill",
            "Search Skill",
            "Skill for RAG-aware searching",
            "1.0.0",
            "search",
            List.of("rag", "search"),
            null
        );

        mockContext = new SkillContext() {
            @Override
            public String skillId() {
                return "search-skill";
            }

            @Override
            public String userId() {
                return "test-user";
            }

            @Override
            public SkillMetadata metadata() {
                return mockMetadata;
            }

            @Override
            public Map<String, Object> variables() {
                return Map.of();
            }

            @Override
            public long timeoutMs() {
                return 5000;
            }
        };

        ragContext = new RAGAwareSkillContext(mockContext);
    }

    @Test
    @DisplayName("Should add RAG metadata via fluent API")
    void testAddRAGMetadata() {
        RAGAwareSkillContext result = ragContext
            .withRAGMetadata("query", "What is AI?")
            .withRAGMetadata("count", 5);

        assertSame(ragContext, result);
    }

    @Test
    @DisplayName("Should retrieve query from metadata")
    void testGetQuery() {
        ragContext.withRAGMetadata("query", "machine learning basics");
        
        var query = ragContext.getQuery();
        
        assertTrue(query.isPresent());
        assertEquals("machine learning basics", query.get());
    }

    @Test
    @DisplayName("Should retrieve documents list")
    void testGetDocuments() {
        List<String> docs = List.of(
            "doc1.txt",
            "doc2.txt",
            "doc3.txt"
        );
        ragContext.withRAGMetadata("documents", docs);
        
        List<String> retrieved = ragContext.getDocuments();
        
        assertEquals(3, retrieved.size());
        assertTrue(retrieved.contains("doc1.txt"));
    }

    @Test
    @DisplayName("Should return empty list when no documents")
    void testGetDocumentsEmpty() {
        List<String> retrieved = ragContext.getDocuments();
        
        assertTrue(retrieved.isEmpty());
    }

    @Test
    @DisplayName("Should enrich RAG results with skill metadata")
    void testEnrichRAGResults() {
        Map<String, Object> results = Map.of(
            "answer", "AI is...",
            "score", 0.95
        );

        Map<String, Object> enriched = ragContext.enrichRAGResults(results);

        assertNotNull(enriched);
        assertTrue(enriched.containsKey("answer"));
        assertTrue(enriched.containsKey("score"));
        assertTrue(enriched.containsKey("skill_id"));
        assertTrue(enriched.containsKey("skill_tags"));
        assertTrue(enriched.containsKey("skill_version"));
        
        assertEquals("Search Skill", enriched.get("skill_id"));
    }

    @Test
    @DisplayName("Should compute skill-specific scores for documents")
    void testComputeSkillScores() {
        List<String> docs = List.of("doc1.txt", "doc2.txt");
        ragContext.withRAGMetadata("documents", docs);
        
        Map<String, Double> scores = ragContext.computeSkillScores().await().indefinitely();

        assertNotNull(scores);
        assertEquals(2, scores.size());
        assertTrue(scores.containsKey("doc1.txt"));
        assertTrue(scores.containsKey("doc2.txt"));
    }

    @Test
    @DisplayName("Should enrich asynchronously")
    void testEnrichAsync() {
        var enriched = ragContext.enrich();
        
        assertNotNull(enriched);
        RAGAwareSkillContext result = enriched.await().indefinitely();
        assertSame(ragContext, result);
    }

    @Test
    @DisplayName("Should get all metadata")
    void testGetAllMetadata() {
        ragContext
            .withRAGMetadata("query", "test query")
            .withRAGMetadata("documents", List.of("doc1.txt", "doc2.txt"))
            .withRAGMetadata("threshold", 0.7);

        Map<String, Object> metadata = ragContext.getAllMetadata();

        assertEquals(3, metadata.size());
        assertTrue(metadata.containsKey("query"));
        assertTrue(metadata.containsKey("documents"));
        assertTrue(metadata.containsKey("threshold"));
    }

    @Test
    @DisplayName("Should handle non-list document values gracefully")
    void testGetDocumentsNonList() {
        ragContext.withRAGMetadata("documents", "not-a-list");
        
        List<String> retrieved = ragContext.getDocuments();
        
        assertTrue(retrieved.isEmpty());
    }
}

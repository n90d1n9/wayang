package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.support.TestSkillContexts;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RAGAwareSkillContextContractTest {

    @Test
    void storesRagMetadataAsSanitizedSnapshot() {
        RAGAwareSkillContext context = context();
        List<Object> documents = new ArrayList<>(List.of(" doc1.txt ", 123, "", "doc2.txt"));

        context.withRAGMetadata(" query ", " machine learning basics ")
                .withRAGMetadata("documents", documents)
                .withRAGMetadata(null, "ignored")
                .withRAGMetadata("ignored", null);
        documents.add("late-doc.txt");

        Map<String, Object> metadata = context.getAllMetadata();
        @SuppressWarnings("unchecked")
        List<Object> storedDocuments = assertInstanceOf(List.class, metadata.get("documents"));

        assertEquals("machine learning basics", context.getQuery().orElseThrow());
        assertEquals(List.of("doc1.txt", "123", "doc2.txt"), context.getDocuments());
        assertFalse(metadata.containsKey("ignored"));
        assertFalse(storedDocuments.contains("late-doc.txt"));
        assertThrows(UnsupportedOperationException.class, () -> metadata.put("new", "value"));
        assertThrows(UnsupportedOperationException.class, () -> storedDocuments.add("new"));
    }

    @Test
    void returnsEmptyQueryAndDocumentsForUnsupportedValues() {
        RAGAwareSkillContext context = context()
                .withRAGMetadata("query", " ")
                .withRAGMetadata("documents", "not-a-list");

        assertTrue(context.getQuery().isEmpty());
        assertTrue(context.getDocuments().isEmpty());
    }

    @Test
    void enrichesResultsWithActiveSkillIdentityAndMetadata() {
        RAGAwareSkillContext context = context();
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("answer", "AI is...");
        results.put("score", 0.95);

        Map<String, Object> enriched = context.enrichRAGResults(results);
        results.put("answer", "mutated");

        assertEquals("AI is...", enriched.get("answer"));
        assertEquals(0.95, enriched.get("score"));
        assertEquals("search-skill", enriched.get("skill_id"));
        assertEquals("Search Skill", enriched.get("skill_name"));
        assertEquals(List.of("rag", "search"), enriched.get("skill_tags"));
        assertEquals("1.0.0", enriched.get("skill_version"));
        assertThrows(UnsupportedOperationException.class, () -> enriched.put("new", "value"));
    }

    @Test
    void handlesNullResultsAndMissingMetadata() {
        RAGAwareSkillContext context = new RAGAwareSkillContext(TestSkillContexts.context("search-skill", null));

        Map<String, Object> enriched = context.enrichRAGResults(null);

        assertEquals(Map.of("skill_id", "search-skill"), enriched);
    }

    @Test
    void computesDeterministicPlaceholderScoresForDocuments() {
        RAGAwareSkillContext context = context()
                .withRAGMetadata("documents", List.of("doc1.txt", "doc1.txt", "doc2.txt"));

        Map<String, Double> scores = context.computeSkillScores().await().indefinitely();

        assertEquals(2, scores.size());
        assertEquals(0.5, scores.get("doc1.txt"));
        assertEquals(0.5, scores.get("doc2.txt"));
        assertThrows(UnsupportedOperationException.class, () -> scores.put("new", 0.1));
    }

    @Test
    void enrichKeepsProviderFluent() {
        RAGAwareSkillContext context = context();

        assertSame(context, context.enrich().await().indefinitely());
    }

    @Test
    void requiresContext() {
        assertThrows(NullPointerException.class, () -> new RAGAwareSkillContext(null));
    }

    private static RAGAwareSkillContext context() {
        SkillMetadata metadata = TestSkillContexts.metadata(
                "Search Skill",
                "Skill for RAG-aware searching",
                "1.0.0",
                "rag",
                "search");
        return new RAGAwareSkillContext(TestSkillContexts.context("search-skill", metadata));
    }
}

package tech.kayys.wayang.agent.spi.skills.rag;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagSkillRetrievalContractTest {

    @Test
    void requestNormalizesAndCopiesMutableInputs() {
        float[] embedding = new float[] { 0.1f, 0.2f };
        Map<String, Object> filters = new HashMap<>();
        filters.put("domain", "docs");
        filters.put("ignored", null);

        RagSkillRetrievalRequest request = new RagSkillRetrievalRequest(
                " tenant-a ",
                " question ",
                " docs ",
                -1,
                embedding,
                filters);
        embedding[0] = 9.0f;
        filters.put("domain", "mutated");

        assertEquals("tenant-a", request.tenantId());
        assertEquals("question", request.query());
        assertEquals("docs", request.collection());
        assertEquals(0, request.topK());
        assertArrayEquals(new float[] { 0.1f, 0.2f }, request.queryEmbedding());
        assertEquals(Map.of("domain", "docs"), request.filters());
        assertThrows(UnsupportedOperationException.class, () -> request.filters().put("other", "value"));

        float[] exposed = request.queryEmbedding();
        exposed[0] = 7.0f;
        assertArrayEquals(new float[] { 0.1f, 0.2f }, request.queryEmbedding());
    }

    @Test
    void resultFiltersBlankDocumentsAndRendersPromptContext() {
        RagSkillRetrievedDocument document = new RagSkillRetrievedDocument(
                " doc-1 ",
                " Manual ",
                " Wayang supports dynamic skills. ",
                " docs://manual ",
                0.9,
                Map.of("page", 1));

        RagSkillRetrievalResult result = new RagSkillRetrievalResult(Arrays.asList(
                null,
                new RagSkillRetrievedDocument("empty", "Empty", " ", "", 0.1, Map.of()),
                document));

        assertEquals(1, result.documents().size());
        assertEquals(document, result.documents().getFirst());
        assertTrue(result.context().contains("Manual:"));
        assertTrue(result.context().contains("Wayang supports dynamic skills."));
        assertEquals("doc-1", document.sourceSummary().get("id"));
        assertEquals("Manual", document.sourceSummary().get("title"));
        assertEquals("docs://manual", document.sourceSummary().get("source"));
        assertEquals(0.9, document.sourceSummary().get("score"));
    }
}

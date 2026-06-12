package tech.kayys.wayang.rag.retrieval;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRetrievalEvalRequestTest {

    @Test
    void copiesFiltersDefensivelyAndPreservesNullableValues() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("collection", "docs");
        filters.put("nullable", null);

        RagRetrievalEvalRequest request = new RagRetrievalEvalRequest(
                "tenant",
                null,
                null,
                null,
                filters,
                null,
                null);
        filters.put("collection", "mutated");

        assertEquals("docs", request.filters().get("collection"));
        assertTrue(request.filters().containsKey("nullable"));
        assertNull(request.filters().get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> request.filters().put("other", "value"));
    }
}

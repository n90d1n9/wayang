package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEvalQueryCaseTest {

    @Test
    void copiesExpectedIdsAndFiltersDefensively() {
        List<String> expectedIds = new ArrayList<>(List.of("doc-1"));
        Map<String, Object> filters = new HashMap<>();
        filters.put("collection", "docs");
        filters.put("nullable", null);

        RagEvalQueryCase queryCase = new RagEvalQueryCase("case-1", "question", expectedIds, filters);
        expectedIds.set(0, "mutated");
        filters.put("collection", "mutated");

        assertEquals(List.of("doc-1"), queryCase.expectedIds());
        assertEquals("docs", queryCase.filters().get("collection"));
        assertTrue(queryCase.filters().containsKey("nullable"));
        assertNull(queryCase.filters().get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> queryCase.expectedIds().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> queryCase.filters().put("other", "value"));
    }
}

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

class RagEvalDatasetTest {

    @Test
    void copiesDefaultFiltersAndQueriesDefensively() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("collection", "docs");
        filters.put("nullable", null);
        List<RagEvalQueryCase> queries = new ArrayList<>(List.of(
                new RagEvalQueryCase("case-1", "question", List.of("doc-1"), Map.of())));

        RagEvalDataset dataset = new RagEvalDataset(
                "fixture",
                "tenant",
                5,
                0.2,
                "documentId",
                filters,
                queries);
        filters.put("collection", "mutated");
        queries.clear();

        assertEquals("docs", dataset.defaultFilters().get("collection"));
        assertTrue(dataset.defaultFilters().containsKey("nullable"));
        assertNull(dataset.defaultFilters().get("nullable"));
        assertEquals(1, dataset.queries().size());
        assertThrows(UnsupportedOperationException.class, () -> dataset.defaultFilters().put("other", "value"));
        assertThrows(UnsupportedOperationException.class, () -> dataset.queries().add(null));
    }
}

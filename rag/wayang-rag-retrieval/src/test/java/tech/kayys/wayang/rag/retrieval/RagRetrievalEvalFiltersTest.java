package tech.kayys.wayang.rag.retrieval;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRetrievalEvalFiltersTest {

    @Test
    void copiesFiltersDefensivelyAndPreservesNullableValues() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("collection", "docs");
        filters.put("nullable", null);

        Map<String, Object> copied = RagRetrievalEvalFilters.copy(filters);
        filters.put("collection", "mutated");

        assertEquals("docs", copied.get("collection"));
        assertTrue(copied.containsKey("nullable"));
        assertNull(copied.get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> copied.put("other", "value"));
    }

    @Test
    void mergesFiltersInDatasetRequestCaseOrder() {
        Map<String, Object> datasetFilters = new HashMap<>();
        datasetFilters.put("collection", "dataset");
        datasetFilters.put("nullable", "dataset");
        Map<String, Object> requestFilters = new HashMap<>();
        requestFilters.put("collection", "request");
        requestFilters.put("request-null", null);
        Map<String, Object> caseFilters = new HashMap<>();
        caseFilters.put("nullable", null);
        caseFilters.put("case", "case-value");

        Map<String, Object> merged = RagRetrievalEvalFilters.merge(datasetFilters, requestFilters, caseFilters);
        datasetFilters.put("collection", "mutated");
        requestFilters.put("collection", "mutated");
        caseFilters.put("case", "mutated");

        assertEquals("request", merged.get("collection"));
        assertEquals("case-value", merged.get("case"));
        assertTrue(merged.containsKey("nullable"));
        assertNull(merged.get("nullable"));
        assertTrue(merged.containsKey("request-null"));
        assertNull(merged.get("request-null"));
        assertThrows(UnsupportedOperationException.class, () -> merged.put("other", "value"));
    }
}

package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagMetadataKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagWorkflowFiltersTest {

    @Test
    void copiesFiltersDefensively() {
        Map<String, Object> input = new HashMap<>();
        input.put("domain", "docs");

        Map<String, Object> copied = RagWorkflowFilters.copy(input);
        input.put("domain", "mutated");

        assertEquals(Map.of("domain", "docs"), copied);
        assertThrows(UnsupportedOperationException.class, () -> copied.put("new", "value"));
    }

    @Test
    void defaultsNullAndEmptyFilters() {
        assertEquals(Map.of(), RagWorkflowFilters.copy(null));
        assertEquals(Map.of(), RagWorkflowFilters.nativeFilters(null, null));
    }

    @Test
    void nativeFiltersAddFirstNormalizedCollection() {
        Map<String, Object> filters = Map.of(
                "domain", "docs",
                RagMetadataKeys.COLLECTION, "old");

        Map<String, Object> nativeFilters = RagWorkflowFilters.nativeFilters(
                filters,
                List.of(" faq ", "archive"));

        assertEquals("docs", nativeFilters.get("domain"));
        assertEquals("faq", nativeFilters.get(RagMetadataKeys.COLLECTION));
    }

    @Test
    void normalizesCollectionsWithoutInjectingDefaults() {
        assertEquals(List.of("docs", "faq"), RagWorkflowFilters.normalizeCollections(List.of(" docs ", "", "faq")));
    }
}

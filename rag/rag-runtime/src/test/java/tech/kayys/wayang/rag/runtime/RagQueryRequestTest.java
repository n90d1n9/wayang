package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagQueryRequestTest {

    @Test
    void normalizesCollectionsAndCopiesFiltersDefensively() {
        List<String> collections = Arrays.asList(" docs ", "", null, "faq");
        Map<String, Object> filters = new HashMap<>();
        filters.put("domain", "manual");

        RagQueryRequest request = new RagQueryRequest(
                "tenant",
                "question",
                RagMode.STANDARD,
                SearchStrategy.HYBRID,
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                collections,
                filters);
        filters.put("domain", "mutated");

        assertEquals(List.of("docs", "faq"), request.collections());
        assertEquals(Map.of("domain", "manual"), request.filters());
        assertThrows(UnsupportedOperationException.class, () -> request.collections().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> request.filters().put("other", "value"));
    }

    @Test
    void defaultsMissingCollectionsAndFiltersToEmptyValues() {
        RagQueryRequest request = new RagQueryRequest(
                "tenant",
                "question",
                null,
                null,
                null,
                null,
                null,
                null);

        assertEquals(List.of(), request.collections());
        assertEquals(Map.of(), request.filters());
    }
}

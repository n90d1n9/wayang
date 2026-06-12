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

class RetrievalConfigTest {

    @Test
    void copiesMetadataFiltersAndExcludedFieldsDefensively() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("collection", "docs");
        filters.put("nullable", null);
        List<String> excludedFields = new ArrayList<>(List.of("embedding"));

        RetrievalConfig config = new RetrievalConfig(
                10,
                0.7f,
                1024,
                128,
                true,
                RerankingModel.COHERE_RERANK,
                true,
                0.4f,
                true,
                2,
                true,
                1,
                filters,
                excludedFields,
                true,
                true);
        filters.put("collection", "mutated");
        excludedFields.set(0, "mutated");

        assertEquals("docs", config.metadataFilters().get("collection"));
        assertTrue(config.metadataFilters().containsKey("nullable"));
        assertNull(config.metadataFilters().get("nullable"));
        assertEquals(List.of("embedding"), config.excludedFields());
        assertThrows(UnsupportedOperationException.class, () -> config.metadataFilters().put("other", "value"));
        assertThrows(UnsupportedOperationException.class, () -> config.excludedFields().add("other"));
    }

    @Test
    void defaultsMissingCollectionsToEmptyImmutableValues() {
        RetrievalConfig config = new RetrievalConfig(
                5,
                0.5f,
                512,
                50,
                false,
                RerankingModel.COHERE_RERANK,
                false,
                0.7f,
                false,
                3,
                false,
                0,
                null,
                null,
                false,
                false);

        assertEquals(Map.of(), config.metadataFilters());
        assertEquals(List.of(), config.excludedFields());
        assertThrows(UnsupportedOperationException.class, () -> config.metadataFilters().put("other", "value"));
        assertThrows(UnsupportedOperationException.class, () -> config.excludedFields().add("other"));
    }
}

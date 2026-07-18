package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagQueryTest {

    @Test
    void copiesFiltersDefensivelyAndPreservesNullableValues() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("collection", "docs");
        filters.put("nullable", null);

        RagQuery query = new RagQuery("question", 5, 0.0, filters);
        filters.put("collection", "mutated");

        assertEquals("docs", query.filters().get("collection"));
        assertTrue(query.filters().containsKey("nullable"));
        assertNull(query.filters().get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> query.filters().put("other", "value"));
    }
}

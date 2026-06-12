package tech.kayys.wayang.rag.plugin.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPluginExecutionContextTest {

    @Test
    void copiesFiltersDefensivelyAndPreservesNullableValues() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("domain", "manual");
        filters.put("nullable", null);

        RagPluginExecutionContext context = new RagPluginExecutionContext(
                "tenant",
                "question",
                3,
                0.7f,
                filters,
                GenerationConfig.defaults(),
                false);
        filters.put("domain", "mutated");

        assertEquals("manual", context.filters().get("domain"));
        assertTrue(context.filters().containsKey("nullable"));
        assertNull(context.filters().get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> context.filters().put("other", "value"));
    }

    @Test
    void withFiltersUsesSameDefensiveCopy() {
        RagPluginExecutionContext context = new RagPluginExecutionContext(
                "tenant",
                "question",
                3,
                0.7f,
                Map.of("domain", "manual"),
                GenerationConfig.defaults(),
                false);
        Map<String, Object> updatedFilters = new HashMap<>();
        updatedFilters.put("nullable", null);

        RagPluginExecutionContext updated = context.withFilters(updatedFilters);
        updatedFilters.put("nullable", "mutated");

        assertTrue(updated.filters().containsKey("nullable"));
        assertNull(updated.filters().get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> updated.filters().put("other", "value"));
    }
}

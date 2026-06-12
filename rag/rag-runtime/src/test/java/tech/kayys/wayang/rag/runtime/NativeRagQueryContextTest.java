package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagMetadataKeys;
import tech.kayys.wayang.rag.core.RetrievalConfig;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeRagQueryContextTest {

    @Test
    void defaultsNullConfigsAndFilters() {
        NativeRagQueryContext context = NativeRagQueryContext.create(
                "tenant",
                "question",
                null,
                null,
                null,
                false);

        assertEquals("tenant", context.pluginContext().tenantId());
        assertEquals("question", context.ragQuery().text());
        assertEquals(RetrievalConfig.defaults().topK(), context.ragQuery().topK());
        assertEquals(RetrievalConfig.defaults().minSimilarity(), context.ragQuery().minScore(), 0.0001);
        assertEquals(Map.of(), context.ragQuery().filters());
        assertEquals(GenerationConfig.defaults().model(), context.generationConfig().model());
        assertFalse(context.pluginContext().retrievalOnly());
    }

    @Test
    void rebuildsRagQueryAfterPluginContextMutation() {
        GenerationConfig generationConfig = GenerationConfig.defaults();
        NativeRagQueryContext context = NativeRagQueryContext.create(
                "tenant",
                "question",
                RetrievalConfig.defaults(),
                generationConfig,
                Map.of(RagMetadataKeys.COLLECTION, "docs"),
                true);

        NativeRagQueryContext updated = context.withPluginContext(
                context.pluginContext()
                        .withQuery("normalized")
                        .withTopK(2)
                        .withMinSimilarity(0.25f)
                        .withFilters(Map.of(RagMetadataKeys.COLLECTION, "faq")));

        assertEquals("normalized", updated.ragQuery().text());
        assertEquals(2, updated.ragQuery().topK());
        assertEquals(0.25, updated.ragQuery().minScore(), 0.0001);
        assertEquals(Map.of(RagMetadataKeys.COLLECTION, "faq"), updated.ragQuery().filters());
        assertSame(generationConfig, updated.generationConfig());
        assertTrue(updated.pluginContext().retrievalOnly());
    }

    @Test
    void copiesInputFiltersDefensivelyForPluginAndQueryContext() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(RagMetadataKeys.COLLECTION, "docs");

        NativeRagQueryContext context = NativeRagQueryContext.create(
                "tenant",
                "question",
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                filters,
                false);
        filters.put(RagMetadataKeys.COLLECTION, "mutated");

        assertEquals("docs", context.pluginContext().filters().get(RagMetadataKeys.COLLECTION));
        assertEquals("docs", context.ragQuery().filters().get(RagMetadataKeys.COLLECTION));
        assertThrows(
                UnsupportedOperationException.class,
                () -> context.pluginContext().filters().put("other", "value"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> context.ragQuery().filters().put("other", "value"));
    }
}

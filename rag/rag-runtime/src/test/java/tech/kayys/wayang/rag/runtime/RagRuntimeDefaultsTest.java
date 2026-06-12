package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagMetadataKeys;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagRuntimeDefaultsTest {

    @Test
    void keepsNamespaceAndCollectionDefaultsAligned() {
        assertEquals("default", RagRuntimeDefaults.DEFAULT_NAMESPACE);
        assertEquals(RagRuntimeDefaults.DEFAULT_NAMESPACE, RagRuntimeDefaults.DEFAULT_COLLECTION);
        assertEquals(RagMetadataKeys.COLLECTION, RagRuntimeDefaults.COLLECTION_METADATA_KEY);
        assertEquals(List.of(RagRuntimeDefaults.DEFAULT_COLLECTION), RagRuntimeDefaults.defaultCollections());
    }

    @Test
    void normalizesRuntimeScopeValues() {
        assertEquals(RagRuntimeDefaults.DEFAULT_NAMESPACE, RagRuntimeDefaults.normalizeNamespace(" "));
        assertEquals("tenant-a", RagRuntimeDefaults.normalizeNamespace(" tenant-a "));
        assertEquals(RagRuntimeDefaults.DEFAULT_COLLECTION, RagRuntimeDefaults.normalizeCollection(null));
        assertEquals("docs", RagRuntimeDefaults.normalizeCollection(" docs "));
    }

    @Test
    void normalizesCollectionListsWithoutInjectingDefaults() {
        assertEquals(List.of(), RagRuntimeDefaults.normalizeCollections(null));
        assertEquals(List.of(), RagRuntimeDefaults.normalizeCollections(List.of("", " ")));
        assertEquals(
                List.of("docs", "faq"),
                RagRuntimeDefaults.normalizeCollections(Arrays.asList(" docs ", null, "faq")));
    }
}

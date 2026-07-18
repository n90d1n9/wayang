package tech.kayys.wayang.rag.core.store;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.vector.faiss.FaissNative;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FaissVectorStoreTest {

    private FaissVectorStore<String> store;

    @BeforeEach
    void setUp() {
        // Use Flat index with L2 metric for testing
        store = new FaissVectorStore<>(2, "Flat", FaissNative.METRIC_L2);
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    @Test
    void shouldSearchWithNamespacing() {
        String ns1 = "collection-1";
        String ns2 = "collection-2";

        store.upsert(ns1, "id-1", new float[] { 1.0f, 0.0f }, "doc-1", Map.of("category", "A"));
        store.upsert(ns2, "id-2", new float[] { 1.0f, 0.0f }, "doc-2", Map.of("category", "B"));

        // Search ns1
        List<VectorSearchHit<String>> hits1 = store.search(ns1, new float[] { 1.0f, 0.0f }, 5, 0.0, Map.of());
        assertEquals(1, hits1.size());
        assertEquals("doc-1", hits1.get(0).payload());

        // Search ns2
        List<VectorSearchHit<String>> hits2 = store.search(ns2, new float[] { 1.0f, 0.0f }, 5, 0.0, Map.of());
        assertEquals(1, hits2.size());
        assertEquals("doc-2", hits2.get(0).payload());
    }

    @Test
    void shouldFilterByMetadata() {
        String ns = "collection-3";
        store.upsert(ns, "id-1", new float[] { 1.0f, 0.0f }, "doc-1", Map.of("tag", "blue"));
        store.upsert(ns, "id-2", new float[] { 1.1f, 0.1f }, "doc-2", Map.of("tag", "red"));

        // Match blue
        List<VectorSearchHit<String>> hitsBlue = store.search(ns, new float[] { 1.0f, 0.0f }, 5, 0.0,
                Map.of("tag", "blue"));
        assertEquals(1, hitsBlue.size());
        assertEquals("id-1", hitsBlue.get(0).id());

        // Match red
        List<VectorSearchHit<String>> hitsRed = store.search(ns, new float[] { 1.0f, 0.0f }, 5, 0.0,
                Map.of("tag", "red"));
        assertEquals(1, hitsRed.size());
        assertEquals("id-2", hitsRed.get(0).id());

        // Match both
        List<VectorSearchHit<String>> hitsAll = store.search(ns, new float[] { 1.0f, 0.0f }, 5, 0.0, Map.of());
        assertEquals(2, hitsAll.size());
    }

    @Test
    void shouldDeleteEntry() {
        String ns = "collection-4";
        store.upsert(ns, "id-1", new float[] { 1.0f, 0.0f }, "doc-1", Map.of());

        List<VectorSearchHit<String>> hitsBefore = store.search(ns, new float[] { 1.0f, 0.0f }, 5, 0.0, Map.of());
        assertEquals(1, hitsBefore.size());

        store.delete(ns, "id-1");

        List<VectorSearchHit<String>> hitsAfter = store.search(ns, new float[] { 1.0f, 0.0f }, 5, 0.0, Map.of());
        assertEquals(0, hitsAfter.size());
    }

    @Test
    void shouldClearNamespace() {
        String ns = "collection-5";
        store.upsert(ns, "id-1", new float[] { 1.0f, 0.0f }, "doc-1", Map.of());

        store.clear(ns);

        List<VectorSearchHit<String>> hits = store.search(ns, new float[] { 1.0f, 0.0f }, 5, 0.0, Map.of());
        assertEquals(0, hits.size());
    }
}

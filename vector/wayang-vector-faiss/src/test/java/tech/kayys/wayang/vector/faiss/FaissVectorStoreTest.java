package tech.kayys.wayang.vector.faiss;

import org.junit.jupiter.api.*;
import tech.kayys.wayang.vector.VectorEntry;
import tech.kayys.wayang.vector.VectorQuery;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FaissVectorStore} — the VectorStore SPI implementation
 * backed by native FAISS via FFM.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FaissVectorStoreTest {

    private static final int DIMENSION = 32;
    private static FaissVectorStore store;

    @BeforeAll
    static void setup() {
        assumeNativeAvailable();
        store = new FaissVectorStore(DIMENSION, "Flat");
    }

    @AfterAll
    static void teardown() {
        if (store != null) {
            store.close();
        }
    }

    // ==================== Store ====================

    @Test
    @Order(1)
    void testStoreEntries() {
        List<VectorEntry> entries = List.of(
                createEntry("entry-1", "Hello world", DIMENSION),
                createEntry("entry-2", "Foo bar", DIMENSION),
                createEntry("entry-3", "Test data", DIMENSION)
        );

        store.store(entries).await().indefinitely();

        assertEquals(3, store.size());
        assertTrue(store.isTrained());
    }

    // ==================== Search ====================

    @Test
    @Order(2)
    void testSearchReturnsResults() {
        VectorQuery query = new VectorQuery(
                randomFloatList(DIMENSION), 2, 0.0f);

        List<VectorEntry> results = store.search(query).await().indefinitely();
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Search should return results");
        assertTrue(results.size() <= 2, "Should respect topK");
    }

    @Test
    @Order(3)
    void testSearchResultsHaveContent() {
        VectorQuery query = new VectorQuery(
                randomFloatList(DIMENSION), 3, 0.0f);

        List<VectorEntry> results = store.search(query).await().indefinitely();
        for (VectorEntry entry : results) {
            assertNotNull(entry.id());
            assertNotNull(entry.content());
            assertNotNull(entry.vector());
            assertEquals(DIMENSION, entry.vector().size());
        }
    }

    @Test
    @Order(4)
    void testSearchWithMinScore() {
        VectorQuery query = new VectorQuery(
                randomFloatList(DIMENSION), 10, 0.99f); // Very high threshold

        List<VectorEntry> results = store.search(query).await().indefinitely();
        // With random vectors and high threshold, may return empty
        assertNotNull(results);
    }

    // ==================== Delete ====================

    @Test
    @Order(5)
    void testDeleteEntries() {
        long before = store.size();
        store.delete(List.of("entry-2")).await().indefinitely();

        // entry-2 should no longer appear in search results
        VectorQuery query = new VectorQuery(
                randomFloatList(DIMENSION), 10, 0.0f);
        List<VectorEntry> results = store.search(query).await().indefinitely();
        for (VectorEntry entry : results) {
            assertNotEquals("entry-2", entry.id(), "Deleted entry should not be in results");
        }
    }

    // ==================== Rebuild ====================

    @Test
    @Order(6)
    void testRebuildIndex() {
        store.rebuildIndex().await().indefinitely();
        assertTrue(store.size() > 0, "Rebuilt index should have entries");
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(10)
    void testStoreDimensionMismatchThrows() {
        List<VectorEntry> badEntries = List.of(
                createEntry("bad", "wrong dim", DIMENSION + 5));

        assertThrows(Exception.class,
                () -> store.store(badEntries).await().indefinitely());
    }

    @Test
    @Order(11)
    void testSearchEmptyVector() {
        VectorQuery query = new VectorQuery(List.of(), 5, 0.0f);
        List<VectorEntry> results = store.search(query).await().indefinitely();
        assertTrue(results.isEmpty());
    }

    // ==================== Close & Cleanup ====================

    @Test
    @Order(20)
    void testCloseStore() {
        FaissVectorStore temp = new FaissVectorStore(DIMENSION, "Flat");
        temp.store(List.of(createEntry("tmp", "data", DIMENSION))).await().indefinitely();
        assertEquals(1, temp.size());
        temp.close();
        // After close, operations should not be possible
    }

    // ==================== Helpers ====================

    private static VectorEntry createEntry(String id, String content, int dim) {
        return new VectorEntry(id, randomFloatList(dim), content, Map.of("source", "test"));
    }

    private static List<Float> randomFloatList(int dim) {
        Float[] arr = new Float[dim];
        for (int i = 0; i < dim; i++) {
            arr[i] = (float) (Math.random() * 2 - 1);
        }
        return List.of(arr);
    }

    private static void assumeNativeAvailable() {
        try {
            FaissNative.lookup();
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError e) {
            Assumptions.assumeTrue(false,
                    "Skipping FAISS tests: native library not available. " +
                    "Run scripts/build-faiss.sh first.");
        }
    }
}

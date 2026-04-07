package tech.kayys.wayang.vector.faiss;

import org.junit.jupiter.api.*;

import java.lang.foreign.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FaissIndex} — the high-level Java wrapper
 * around native FAISS via FFM.
 *
 * These tests require libfaiss_c to be built and available at ~/.wayang/lib/.
 * Run scripts/build-faiss.sh first, or use: mvn test -Pfaiss-native-build
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FaissIndexTest {

    private static final int DIMENSION = 64;
    private static FaissIndex index;

    @BeforeAll
    static void setup() {
        assumeNativeAvailable();
        index = new FaissIndex(DIMENSION, "Flat", FaissNative.METRIC_L2);
    }

    @AfterAll
    static void teardown() {
        if (index != null) {
            index.close();
        }
    }

    // ==================== Index Lifecycle ====================

    @Test
    @Order(1)
    void testIndexCreated() {
        assertNotNull(index);
        assertEquals(DIMENSION, index.getDimension());
        assertEquals("Flat", index.getIndexDescription());
        assertTrue(index.isTrained(), "Flat index should be pre-trained");
        assertEquals(0, index.size());
    }

    // ==================== Adding Vectors ====================

    @Test
    @Order(2)
    void testAddSingleVector() {
        float[] vector = randomVector(DIMENSION);
        index.add("vec-1", vector);
        assertEquals(1, index.size());
    }

    @Test
    @Order(3)
    void testAddBatchVectors() {
        String[] ids = {"vec-2", "vec-3", "vec-4", "vec-5"};
        float[][] vectors = new float[4][DIMENSION];
        for (int i = 0; i < 4; i++) {
            vectors[i] = randomVector(DIMENSION);
        }
        index.addBatch(ids, vectors);
        assertEquals(5, index.size());
    }

    @Test
    @Order(4)
    void testAddDimensionMismatchThrows() {
        float[] wrongDim = new float[DIMENSION + 1];
        assertThrows(IllegalArgumentException.class, () -> index.add("bad", wrongDim));
    }

    // ==================== Searching ====================

    @Test
    @Order(5)
    void testSearchReturnsResults() {
        float[] query = randomVector(DIMENSION);
        List<FaissIndex.SearchResult> results = index.search(query, 3);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find at least one result");
        assertTrue(results.size() <= 3, "Should return at most k results");
    }

    @Test
    @Order(6)
    void testSearchResultsContainIds() {
        float[] query = randomVector(DIMENSION);
        List<FaissIndex.SearchResult> results = index.search(query, 5);
        for (FaissIndex.SearchResult result : results) {
            assertNotNull(result.id());
            assertTrue(result.distance() >= 0, "L2 distance should be non-negative");
            assertTrue(result.score() > 0 && result.score() <= 1, "Score should be in (0,1]");
        }
    }

    @Test
    @Order(7)
    void testSearchEmptyIndexReturnsEmpty() {
        try (FaissIndex emptyIndex = new FaissIndex(DIMENSION, "Flat")) {
            List<FaissIndex.SearchResult> results = emptyIndex.search(randomVector(DIMENSION), 5);
            assertTrue(results.isEmpty());
        }
    }

    @Test
    @Order(8)
    void testBatchSearch() {
        float[][] queries = {randomVector(DIMENSION), randomVector(DIMENSION)};
        List<List<FaissIndex.SearchResult>> results = index.batchSearch(queries, 3);
        assertEquals(2, results.size());
        for (List<FaissIndex.SearchResult> queryResults : results) {
            assertFalse(queryResults.isEmpty());
        }
    }

    // ==================== Self-Search (exact match) ====================

    @Test
    @Order(9)
    void testSearchFindsSelf() {
        // Add a known vector and search for it — it should be the closest
        float[] known = new float[DIMENSION];
        for (int i = 0; i < DIMENSION; i++) known[i] = i * 0.1f;
        index.add("known-vec", known);

        List<FaissIndex.SearchResult> results = index.search(known, 1);
        assertFalse(results.isEmpty());
        assertEquals("known-vec", results.get(0).id());
        assertTrue(results.get(0).distance() < 1e-4, "Self-search distance should be ~0");
    }

    // ==================== Deletion ====================

    @Test
    @Order(10)
    void testMarkForRemoval() {
        index.markForRemoval("vec-1");
        // After marking, the ID mapping is removed but the FAISS index
        // still holds the vector. Search should not return the removed ID.
        float[] query = randomVector(DIMENSION);
        List<FaissIndex.SearchResult> results = index.search(query, 100);
        for (FaissIndex.SearchResult r : results) {
            assertNotEquals("vec-1", r.id(), "Removed ID should not appear in results");
        }
    }

    // ==================== Persistence ====================

    @Test
    @Order(11)
    void testSaveAndLoad() throws Exception {
        Path tmpFile = Files.createTempFile("faiss-test-", ".index");
        try {
            index.save(tmpFile);
            assertTrue(Files.size(tmpFile) > 0, "Saved index file should not be empty");

            try (FaissIndex loaded = new FaissIndex(DIMENSION, "Flat")) {
                loaded.load(tmpFile);
                // The loaded index should have the same ntotal as the native index
                assertTrue(loaded.size() > 0, "Loaded index should have vectors");
            }
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    // ==================== Reset ====================

    @Test
    @Order(12)
    void testReset() {
        try (FaissIndex resetIndex = new FaissIndex(DIMENSION, "Flat")) {
            resetIndex.add("a", randomVector(DIMENSION));
            resetIndex.add("b", randomVector(DIMENSION));
            assertEquals(2, resetIndex.size());

            resetIndex.reset();
            assertEquals(0, resetIndex.size());
        }
    }

    // ==================== AutoCloseable ====================

    @Test
    @Order(13)
    void testCloseAndUseAfterCloseThrows() {
        FaissIndex tempIndex = new FaissIndex(DIMENSION, "Flat");
        tempIndex.add("x", randomVector(DIMENSION));
        tempIndex.close();

        assertThrows(IllegalStateException.class, () -> tempIndex.search(randomVector(DIMENSION), 1));
    }

    // ==================== HNSW Index ====================

    @Test
    @Order(20)
    void testHNSWIndex() {
        try (FaissIndex hnsw = new FaissIndex(DIMENSION, "HNSW32")) {
            assertTrue(hnsw.isTrained());
            for (int i = 0; i < 100; i++) {
                hnsw.add("hnsw-" + i, randomVector(DIMENSION));
            }
            assertEquals(100, hnsw.size());

            List<FaissIndex.SearchResult> results = hnsw.search(randomVector(DIMENSION), 5);
            assertFalse(results.isEmpty());
        }
    }

    // ==================== Inner Product Metric ====================

    @Test
    @Order(21)
    void testInnerProductIndex() {
        try (FaissIndex ipIndex = new FaissIndex(DIMENSION, "Flat", FaissNative.METRIC_INNER_PRODUCT)) {
            for (int i = 0; i < 20; i++) {
                ipIndex.add("ip-" + i, randomVector(DIMENSION));
            }
            assertEquals(20, ipIndex.size());

            List<FaissIndex.SearchResult> results = ipIndex.search(randomVector(DIMENSION), 3);
            assertFalse(results.isEmpty());
        }
    }

    // ==================== Helpers ====================

    private static float[] randomVector(int dim) {
        float[] v = new float[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = (float) (Math.random() * 2 - 1);
        }
        return v;
    }

    /**
     * Skip tests if native FAISS library is not available.
     */
    private static void assumeNativeAvailable() {
        try {
            // Try to trigger the static initializer of FaissNative
            FaissNative.lookup();
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError e) {
            Assumptions.assumeTrue(false,
                    "Skipping FAISS tests: native library not available. " +
                    "Run scripts/build-faiss.sh first. Error: " + e.getMessage());
        }
    }
}

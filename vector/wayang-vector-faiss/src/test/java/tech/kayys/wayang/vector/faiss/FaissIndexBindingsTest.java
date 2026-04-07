package tech.kayys.wayang.vector.faiss;

import org.junit.jupiter.api.*;

import java.lang.foreign.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Low-level tests for {@link FaissIndexBindings} — verifying FFM downcalls
 * directly against the native FAISS C API.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FaissIndexBindingsTest {

    private static final int DIMENSION = 16;

    @BeforeAll
    static void setup() {
        assumeNativeAvailable();
    }

    // ==================== Index Factory ====================

    @Test
    @Order(1)
    void testIndexFactoryFlat() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment index = FaissIndexBindings.indexFactory(
                    arena, DIMENSION, "Flat", FaissNative.METRIC_L2);
            assertNotNull(index);
            assertFalse(index.equals(MemorySegment.NULL));

            assertEquals(DIMENSION, FaissIndexBindings.getDimension(index));
            assertEquals(0, FaissIndexBindings.getNTotal(index));
            assertTrue(FaissIndexBindings.isTrained(index));
            assertEquals(FaissNative.METRIC_L2, FaissIndexBindings.getMetricType(index));

            FaissIndexBindings.free(index);
        }
    }

    @Test
    @Order(2)
    void testIndexFactoryHNSW() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment index = FaissIndexBindings.indexFactory(
                    arena, DIMENSION, "HNSW32", FaissNative.METRIC_L2);
            assertNotNull(index);
            assertTrue(FaissIndexBindings.isTrained(index));
            FaissIndexBindings.free(index);
        }
    }

    @Test
    @Order(3)
    void testIndexFactoryInnerProduct() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment index = FaissIndexBindings.indexFactory(
                    arena, DIMENSION, "Flat", FaissNative.METRIC_INNER_PRODUCT);
            assertEquals(FaissNative.METRIC_INNER_PRODUCT, FaissIndexBindings.getMetricType(index));
            FaissIndexBindings.free(index);
        }
    }

    // ==================== Add + Search ====================

    @Test
    @Order(10)
    void testAddAndSearch() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment index = FaissIndexBindings.indexFactory(
                    arena, DIMENSION, "Flat", FaissNative.METRIC_L2);

            // Add 10 vectors
            int n = 10;
            MemorySegment vectors = arena.allocate(ValueLayout.JAVA_FLOAT, (long) n * DIMENSION);
            for (int i = 0; i < n * DIMENSION; i++) {
                vectors.setAtIndex(ValueLayout.JAVA_FLOAT, i, (float) Math.random());
            }
            FaissIndexBindings.add(index, n, vectors);
            assertEquals(n, FaissIndexBindings.getNTotal(index));

            // Search for k=3 nearest
            int k = 3;
            MemorySegment query = arena.allocate(ValueLayout.JAVA_FLOAT, DIMENSION);
            for (int i = 0; i < DIMENSION; i++) {
                query.setAtIndex(ValueLayout.JAVA_FLOAT, i, (float) Math.random());
            }
            MemorySegment distances = arena.allocate(ValueLayout.JAVA_FLOAT, k);
            MemorySegment labels = arena.allocate(ValueLayout.JAVA_LONG, k);

            FaissIndexBindings.search(index, 1, query, k, distances, labels);

            // Verify results are valid
            for (int i = 0; i < k; i++) {
                long label = labels.getAtIndex(ValueLayout.JAVA_LONG, i);
                float distance = distances.getAtIndex(ValueLayout.JAVA_FLOAT, i);
                assertTrue(label >= 0 && label < n, "Label should be a valid index");
                assertTrue(distance >= 0, "L2 distance should be non-negative");
            }

            // Distances should be in non-decreasing order
            for (int i = 1; i < k; i++) {
                assertTrue(distances.getAtIndex(ValueLayout.JAVA_FLOAT, i) >=
                                distances.getAtIndex(ValueLayout.JAVA_FLOAT, i - 1),
                        "Distances should be sorted");
            }

            FaissIndexBindings.free(index);
        }
    }

    // ==================== Add With IDs ====================

    @Test
    @Order(11)
    void testAddWithIds() {
        try (Arena arena = Arena.ofConfined()) {
            // IDMap2 wraps Flat to support add_with_ids
            MemorySegment index = FaissIndexBindings.indexFactory(
                    arena, DIMENSION, "IDMap2,Flat", FaissNative.METRIC_L2);

            int n = 5;
            MemorySegment vectors = arena.allocate(ValueLayout.JAVA_FLOAT, (long) n * DIMENSION);
            MemorySegment ids = arena.allocate(ValueLayout.JAVA_LONG, n);
            for (int i = 0; i < n; i++) {
                ids.setAtIndex(ValueLayout.JAVA_LONG, i, 100L + i);
                for (int j = 0; j < DIMENSION; j++) {
                    vectors.setAtIndex(ValueLayout.JAVA_FLOAT, (long) i * DIMENSION + j, (float) Math.random());
                }
            }
            FaissIndexBindings.addWithIds(index, n, vectors, ids);
            assertEquals(n, FaissIndexBindings.getNTotal(index));

            // Search and verify IDs are in the 100-104 range
            MemorySegment query = arena.allocate(ValueLayout.JAVA_FLOAT, DIMENSION);
            for (int j = 0; j < DIMENSION; j++) {
                query.setAtIndex(ValueLayout.JAVA_FLOAT, j, (float) Math.random());
            }
            MemorySegment dist = arena.allocate(ValueLayout.JAVA_FLOAT, 3);
            MemorySegment lbl = arena.allocate(ValueLayout.JAVA_LONG, 3);
            FaissIndexBindings.search(index, 1, query, 3, dist, lbl);

            for (int i = 0; i < 3; i++) {
                long label = lbl.getAtIndex(ValueLayout.JAVA_LONG, i);
                assertTrue(label >= 100 && label <= 104, "Labels should be custom IDs: " + label);
            }

            FaissIndexBindings.free(index);
        }
    }

    // ==================== Reset ====================

    @Test
    @Order(12)
    void testReset() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment index = FaissIndexBindings.indexFactory(
                    arena, DIMENSION, "Flat", FaissNative.METRIC_L2);

            MemorySegment vectors = arena.allocate(ValueLayout.JAVA_FLOAT, (long) 5 * DIMENSION);
            for (int i = 0; i < 5 * DIMENSION; i++) {
                vectors.setAtIndex(ValueLayout.JAVA_FLOAT, i, (float) Math.random());
            }
            FaissIndexBindings.add(index, 5, vectors);
            assertEquals(5, FaissIndexBindings.getNTotal(index));

            FaissIndexBindings.reset(index);
            assertEquals(0, FaissIndexBindings.getNTotal(index));

            FaissIndexBindings.free(index);
        }
    }

    // ==================== Reconstruct ====================

    @Test
    @Order(13)
    void testReconstruct() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment index = FaissIndexBindings.indexFactory(
                    arena, DIMENSION, "Flat", FaissNative.METRIC_L2);

            // Add a known vector
            float[] original = new float[DIMENSION];
            for (int i = 0; i < DIMENSION; i++) original[i] = i * 0.5f;
            MemorySegment vec = arena.allocate(ValueLayout.JAVA_FLOAT, DIMENSION);
            vec.copyFrom(MemorySegment.ofArray(original));
            FaissIndexBindings.add(index, 1, vec);

            // Reconstruct it
            MemorySegment recons = arena.allocate(ValueLayout.JAVA_FLOAT, DIMENSION);
            FaissIndexBindings.reconstruct(index, 0, recons);

            for (int i = 0; i < DIMENSION; i++) {
                assertEquals(original[i], recons.getAtIndex(ValueLayout.JAVA_FLOAT, i), 1e-5f,
                        "Reconstructed vector should match original at index " + i);
            }

            FaissIndexBindings.free(index);
        }
    }

    // ==================== I/O ====================

    @Test
    @Order(20)
    void testWriteAndRead() throws Exception {
        java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("faiss-bindings-", ".index");
        try (Arena arena = Arena.ofConfined()) {
            // Create and populate
            MemorySegment index = FaissIndexBindings.indexFactory(
                    arena, DIMENSION, "Flat", FaissNative.METRIC_L2);
            MemorySegment vectors = arena.allocate(ValueLayout.JAVA_FLOAT, (long) 20 * DIMENSION);
            for (int i = 0; i < 20 * DIMENSION; i++) {
                vectors.setAtIndex(ValueLayout.JAVA_FLOAT, i, (float) Math.random());
            }
            FaissIndexBindings.add(index, 20, vectors);

            // Write
            FaissIOBindings.writeIndex(index, tmpFile.toAbsolutePath().toString());
            assertTrue(java.nio.file.Files.size(tmpFile) > 0);

            // Read
            MemorySegment loaded = FaissIOBindings.readIndex(
                    arena, tmpFile.toAbsolutePath().toString(), 0);
            assertEquals(20, FaissIndexBindings.getNTotal(loaded));
            assertEquals(DIMENSION, FaissIndexBindings.getDimension(loaded));

            FaissIndexBindings.free(index);
            FaissIndexBindings.free(loaded);
        } finally {
            java.nio.file.Files.deleteIfExists(tmpFile);
        }
    }

    // ==================== Clone ====================

    @Test
    @Order(21)
    void testCloneIndex() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment index = FaissIndexBindings.indexFactory(
                    arena, DIMENSION, "Flat", FaissNative.METRIC_L2);
            MemorySegment vec = arena.allocate(ValueLayout.JAVA_FLOAT, DIMENSION);
            for (int i = 0; i < DIMENSION; i++) vec.setAtIndex(ValueLayout.JAVA_FLOAT, i, 1.0f);
            FaissIndexBindings.add(index, 1, vec);

            MemorySegment cloned = FaissIOBindings.cloneIndex(arena, index);
            assertEquals(1, FaissIndexBindings.getNTotal(cloned));
            assertEquals(DIMENSION, FaissIndexBindings.getDimension(cloned));

            FaissIndexBindings.free(index);
            FaissIndexBindings.free(cloned);
        }
    }

    // ==================== Error Handling ====================

    @Test
    @Order(30)
    void testInvalidIndexDescriptionThrows() {
        try (Arena arena = Arena.ofConfined()) {
            assertThrows(FaissException.class, () ->
                    FaissIndexBindings.indexFactory(arena, DIMENSION, "INVALID_INDEX_TYPE_999", FaissNative.METRIC_L2));
        }
    }

    // ==================== Helpers ====================

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

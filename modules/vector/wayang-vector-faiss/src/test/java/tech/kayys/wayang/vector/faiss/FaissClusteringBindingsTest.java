package tech.kayys.wayang.vector.faiss;

import org.junit.jupiter.api.*;

import java.lang.foreign.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FaissClusteringBindings} — k-means clustering
 * via FAISS native FFM.
 */
class FaissClusteringBindingsTest {

    private static final int DIMENSION = 8;

    @BeforeAll
    static void setup() {
        assumeNativeAvailable();
    }

    @Test
    void testKmeansClustering() {
        try (Arena arena = Arena.ofConfined()) {
            int n = 100;
            int k = 5;

            // Generate random training data
            MemorySegment data = arena.allocate(ValueLayout.JAVA_FLOAT, (long) n * DIMENSION);
            for (int i = 0; i < n * DIMENSION; i++) {
                data.setAtIndex(ValueLayout.JAVA_FLOAT, i, (float) Math.random());
            }

            // Output centroids
            MemorySegment centroids = arena.allocate(ValueLayout.JAVA_FLOAT, (long) k * DIMENSION);

            float error = FaissClusteringBindings.kmeansClustering(
                    arena, DIMENSION, n, k, data, centroids);

            assertTrue(error >= 0, "Quantization error should be non-negative");

            // Check centroids are populated (not all zeros)
            boolean hasNonZero = false;
            for (int i = 0; i < k * DIMENSION; i++) {
                if (centroids.getAtIndex(ValueLayout.JAVA_FLOAT, i) != 0.0f) {
                    hasNonZero = true;
                    break;
                }
            }
            assertTrue(hasNonZero, "Centroids should be populated after clustering");
        }
    }

    @Test
    void testClusteringObject() {
        try (Arena arena = Arena.ofConfined()) {
            int k = 3;
            MemorySegment clustering = FaissClusteringBindings.newClustering(arena, DIMENSION, k);
            assertNotNull(clustering);

            assertEquals(DIMENSION, FaissClusteringBindings.getD(clustering));
            assertEquals(k, FaissClusteringBindings.getK(clustering));

            FaissClusteringBindings.free(clustering);
        }
    }

    @Test
    void testClusteringWithParams() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment params = FaissClusteringBindings.allocateParams(arena);
            assertNotNull(params);

            int k = 4;
            MemorySegment clustering = FaissClusteringBindings.newClusteringWithParams(
                    arena, DIMENSION, k, params);
            assertNotNull(clustering);
            assertEquals(k, FaissClusteringBindings.getK(clustering));

            FaissClusteringBindings.free(clustering);
        }
    }

    // ==================== Helpers ====================

    private static void assumeNativeAvailable() {
        try {
            FaissNative.lookup();
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError e) {
            Assumptions.assumeTrue(false,
                    "Skipping FAISS tests: native library not available.");
        }
    }
}

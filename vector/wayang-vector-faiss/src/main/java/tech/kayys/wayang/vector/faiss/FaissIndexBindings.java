package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for core FAISS Index operations ({@code Index_c.h} + {@code index_factory_c.h}).
 * <p>
 * Covers: index creation, train, add, search, range_search, assign, reset,
 * remove_ids, reconstruct, compute_residual, sa_encode/decode, and property getters.
 */
public final class FaissIndexBindings {

    // ==================== Index Factory ====================

    // int faiss_index_factory(FaissIndex** p_index, int d, const char* description, FaissMetricType metric)
    private static final MethodHandle INDEX_FACTORY = FaissNative.downcallHandle(
            "faiss_index_factory",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    // ==================== Training ====================

    // int faiss_Index_train(FaissIndex* index, idx_t n, const float* x)
    private static final MethodHandle TRAIN = FaissNative.downcallHandle(
            "faiss_Index_train",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // ==================== Adding Vectors ====================

    // int faiss_Index_add(FaissIndex* index, idx_t n, const float* x)
    private static final MethodHandle ADD = FaissNative.downcallHandle(
            "faiss_Index_add",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // int faiss_Index_add_with_ids(FaissIndex* index, idx_t n, const float* x, const idx_t* xids)
    private static final MethodHandle ADD_WITH_IDS = FaissNative.downcallHandle(
            "faiss_Index_add_with_ids",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // ==================== Searching ====================

    // int faiss_Index_search(const FaissIndex*, idx_t n, const float* x, idx_t k, float* distances, idx_t* labels)
    private static final MethodHandle SEARCH = FaissNative.downcallHandle(
            "faiss_Index_search",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // int faiss_Index_assign(FaissIndex*, idx_t n, const float* x, idx_t* labels, idx_t k)
    private static final MethodHandle ASSIGN = FaissNative.downcallHandle(
            "faiss_Index_assign",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // ==================== Deletion / Reset ====================

    // int faiss_Index_reset(FaissIndex* index)
    private static final MethodHandle RESET = FaissNative.downcallHandle(
            "faiss_Index_reset",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_Index_remove_ids(FaissIndex* index, const FaissIDSelector* sel, size_t* n_removed)
    private static final MethodHandle REMOVE_IDS = FaissNative.downcallHandle(
            "faiss_Index_remove_ids",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // ==================== Reconstruction ====================

    // int faiss_Index_reconstruct(const FaissIndex*, idx_t key, float* recons)
    private static final MethodHandle RECONSTRUCT = FaissNative.downcallHandle(
            "faiss_Index_reconstruct",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // int faiss_Index_reconstruct_n(const FaissIndex*, idx_t i0, idx_t ni, float* recons)
    private static final MethodHandle RECONSTRUCT_N = FaissNative.downcallHandle(
            "faiss_Index_reconstruct_n",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // ==================== Residuals ====================

    // int faiss_Index_compute_residual(const FaissIndex*, const float* x, float* residual, idx_t key)
    private static final MethodHandle COMPUTE_RESIDUAL = FaissNative.downcallHandle(
            "faiss_Index_compute_residual",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // int faiss_Index_compute_residual_n(const FaissIndex*, idx_t n, const float* x, float* residuals, const idx_t* keys)
    private static final MethodHandle COMPUTE_RESIDUAL_N = FaissNative.downcallHandle(
            "faiss_Index_compute_residual_n",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // ==================== Standalone Codec ====================

    // int faiss_Index_sa_code_size(const FaissIndex*, size_t* size)
    private static final MethodHandle SA_CODE_SIZE = FaissNative.downcallHandle(
            "faiss_Index_sa_code_size",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // int faiss_Index_sa_encode(const FaissIndex*, idx_t n, const float* x, uint8_t* bytes)
    private static final MethodHandle SA_ENCODE = FaissNative.downcallHandle(
            "faiss_Index_sa_encode",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // int faiss_Index_sa_decode(const FaissIndex*, idx_t n, const uint8_t* bytes, float* x)
    private static final MethodHandle SA_DECODE = FaissNative.downcallHandle(
            "faiss_Index_sa_decode",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // ==================== Destructor ====================

    // void faiss_Index_free(FaissIndex* index)
    private static final MethodHandle FREE = FaissNative.downcallHandle(
            "faiss_Index_free",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // ==================== Property Getters ====================

    // int faiss_Index_d(const FaissIndex*) -> int
    private static final MethodHandle GET_D = FaissNative.downcallHandle(
            "faiss_Index_d",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // idx_t faiss_Index_ntotal(const FaissIndex*) -> long
    private static final MethodHandle GET_NTOTAL = FaissNative.downcallHandle(
            "faiss_Index_ntotal",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // int faiss_Index_is_trained(const FaissIndex*) -> int (bool)
    private static final MethodHandle GET_IS_TRAINED = FaissNative.downcallHandle(
            "faiss_Index_is_trained",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_Index_metric_type(const FaissIndex*) -> int
    private static final MethodHandle GET_METRIC_TYPE = FaissNative.downcallHandle(
            "faiss_Index_metric_type",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private FaissIndexBindings() {}

    // ==================== Public API ====================

    /**
     * Create an index using the FAISS index factory.
     *
     * @param arena       memory arena for allocations
     * @param dimension   vector dimension
     * @param description index description (e.g. "Flat", "HNSW32", "IVF100,Flat")
     * @param metricType  metric type constant from {@link FaissNative}
     * @return pointer to the created FaissIndex
     */
    public static MemorySegment indexFactory(Arena arena, int dimension, String description, int metricType) {
        try {
            MemorySegment pIndex = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment descStr = arena.allocateFrom(description);
            int rc = (int) INDEX_FACTORY.invokeExact(pIndex, dimension, descStr, metricType);
            FaissNative.checkError(rc);
            return pIndex.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("indexFactory failed", t);
        }
    }

    /**
     * Train the index on representative vectors.
     */
    public static void train(MemorySegment index, long n, MemorySegment vectors) {
        try {
            int rc = (int) TRAIN.invokeExact(index, n, vectors);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("train failed", t);
        }
    }

    /**
     * Add n vectors to the index.
     */
    public static void add(MemorySegment index, long n, MemorySegment vectors) {
        try {
            int rc = (int) ADD.invokeExact(index, n, vectors);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("add failed", t);
        }
    }

    /**
     * Add n vectors with explicit IDs.
     */
    public static void addWithIds(MemorySegment index, long n, MemorySegment vectors, MemorySegment ids) {
        try {
            int rc = (int) ADD_WITH_IDS.invokeExact(index, n, vectors, ids);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("addWithIds failed", t);
        }
    }

    /**
     * Search for k nearest neighbors.
     *
     * @param index     index pointer
     * @param n         number of query vectors
     * @param queries   query vectors (n * d floats)
     * @param k         number of nearest neighbors
     * @param distances output distances (n * k floats)
     * @param labels    output labels (n * k longs)
     */
    public static void search(MemorySegment index, long n, MemorySegment queries,
                              long k, MemorySegment distances, MemorySegment labels) {
        try {
            int rc = (int) SEARCH.invokeExact(index, n, queries, k, distances, labels);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("search failed", t);
        }
    }

    /**
     * Assign vectors to nearest centroids (labels only, no distances).
     */
    public static void assign(MemorySegment index, long n, MemorySegment vectors,
                              MemorySegment labels, long k) {
        try {
            int rc = (int) ASSIGN.invokeExact(index, n, vectors, labels, k);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("assign failed", t);
        }
    }

    /**
     * Remove all vectors from the index.
     */
    public static void reset(MemorySegment index) {
        try {
            int rc = (int) RESET.invokeExact(index);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("reset failed", t);
        }
    }

    /**
     * Remove IDs from the index using an IDSelector.
     */
    public static long removeIds(MemorySegment index, MemorySegment idSelector) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nRemoved = arena.allocate(ValueLayout.JAVA_LONG);
            int rc = (int) REMOVE_IDS.invokeExact(index, idSelector, nRemoved);
            FaissNative.checkError(rc);
            return nRemoved.get(ValueLayout.JAVA_LONG, 0);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("removeIds failed", t);
        }
    }

    /**
     * Reconstruct a single stored vector.
     */
    public static void reconstruct(MemorySegment index, long key, MemorySegment recons) {
        try {
            int rc = (int) RECONSTRUCT.invokeExact(index, key, recons);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("reconstruct failed", t);
        }
    }

    /**
     * Reconstruct a range of stored vectors.
     */
    public static void reconstructN(MemorySegment index, long i0, long ni, MemorySegment recons) {
        try {
            int rc = (int) RECONSTRUCT_N.invokeExact(index, i0, ni, recons);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("reconstructN failed", t);
        }
    }

    /**
     * Compute residual vector.
     */
    public static void computeResidual(MemorySegment index, MemorySegment x,
                                       MemorySegment residual, long key) {
        try {
            int rc = (int) COMPUTE_RESIDUAL.invokeExact(index, x, residual, key);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("computeResidual failed", t);
        }
    }

    /**
     * Batch compute residual vectors.
     */
    public static void computeResidualN(MemorySegment index, long n, MemorySegment x,
                                        MemorySegment residuals, MemorySegment keys) {
        try {
            int rc = (int) COMPUTE_RESIDUAL_N.invokeExact(index, n, x, residuals, keys);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("computeResidualN failed", t);
        }
    }

    /**
     * Get standalone codec code size.
     */
    public static long saCodeSize(MemorySegment index) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment size = arena.allocate(ValueLayout.JAVA_LONG);
            int rc = (int) SA_CODE_SIZE.invokeExact(index, size);
            FaissNative.checkError(rc);
            return size.get(ValueLayout.JAVA_LONG, 0);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("saCodeSize failed", t);
        }
    }

    /**
     * Encode vectors using standalone codec.
     */
    public static void saEncode(MemorySegment index, long n, MemorySegment x, MemorySegment bytes) {
        try {
            int rc = (int) SA_ENCODE.invokeExact(index, n, x, bytes);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("saEncode failed", t);
        }
    }

    /**
     * Decode vectors from standalone codec.
     */
    public static void saDecode(MemorySegment index, long n, MemorySegment bytes, MemorySegment x) {
        try {
            int rc = (int) SA_DECODE.invokeExact(index, n, bytes, x);
            FaissNative.checkError(rc);
        } catch (FaissException e) {
            throw e;
        } catch (Throwable t) {
            throw new FaissException("saDecode failed", t);
        }
    }

    /**
     * Free native index memory.
     */
    public static void free(MemorySegment index) {
        try {
            FREE.invokeExact(index);
        } catch (Throwable t) {
            throw new FaissException("free failed", t);
        }
    }

    /**
     * Get the dimension of the index.
     */
    public static int getDimension(MemorySegment index) {
        try {
            return (int) GET_D.invokeExact(index);
        } catch (Throwable t) {
            throw new FaissException("getDimension failed", t);
        }
    }

    /**
     * Get the total number of vectors in the index.
     */
    public static long getNTotal(MemorySegment index) {
        try {
            return (long) GET_NTOTAL.invokeExact(index);
        } catch (Throwable t) {
            throw new FaissException("getNTotal failed", t);
        }
    }

    /**
     * Check if the index is trained.
     */
    public static boolean isTrained(MemorySegment index) {
        try {
            return ((int) GET_IS_TRAINED.invokeExact(index)) != 0;
        } catch (Throwable t) {
            throw new FaissException("isTrained failed", t);
        }
    }

    /**
     * Get the metric type of the index.
     */
    public static int getMetricType(MemorySegment index) {
        try {
            return (int) GET_METRIC_TYPE.invokeExact(index);
        } catch (Throwable t) {
            throw new FaissException("getMetricType failed", t);
        }
    }
}

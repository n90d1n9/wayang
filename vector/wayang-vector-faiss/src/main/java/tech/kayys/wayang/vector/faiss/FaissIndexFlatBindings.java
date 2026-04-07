package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS IndexFlat operations ({@code IndexFlat_c.h}).
 * Flat (brute-force) exact search indexes.
 */
public final class FaissIndexFlatBindings {

    // int faiss_IndexFlat_new(FaissIndexFlat** p_index)
    private static final MethodHandle NEW = FaissNative.downcallHandle(
            "faiss_IndexFlat_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_IndexFlat_new_with(FaissIndexFlat** p_index, idx_t d, FaissMetricType metric)
    private static final MethodHandle NEW_WITH = FaissNative.downcallHandle(
            "faiss_IndexFlat_new_with",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));

    // int faiss_IndexFlatL2_new(FaissIndexFlatL2** p_index)
    private static final MethodHandle NEW_L2 = FaissNative.downcallHandle(
            "faiss_IndexFlatL2_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_IndexFlatL2_new_with(FaissIndexFlatL2** p_index, idx_t d)
    private static final MethodHandle NEW_L2_WITH = FaissNative.downcallHandle(
            "faiss_IndexFlatL2_new_with",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // int faiss_IndexFlatIP_new(FaissIndexFlatIP** p_index)
    private static final MethodHandle NEW_IP = FaissNative.downcallHandle(
            "faiss_IndexFlatIP_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_IndexFlatIP_new_with(FaissIndexFlatIP** p_index, idx_t d)
    private static final MethodHandle NEW_IP_WITH = FaissNative.downcallHandle(
            "faiss_IndexFlatIP_new_with",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // int faiss_IndexFlat_compute_distance_subset(FaissIndex*, idx_t n, const float* x,
    //     idx_t k, float* distances, const idx_t* labels)
    private static final MethodHandle COMPUTE_DISTANCE_SUBSET = FaissNative.downcallHandle(
            "faiss_IndexFlat_compute_distance_subset",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // int faiss_IndexRefineFlat_new(FaissIndexRefineFlat** p_index, FaissIndex* base_index)
    private static final MethodHandle NEW_REFINE = FaissNative.downcallHandle(
            "faiss_IndexRefineFlat_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private FaissIndexFlatBindings() {}

    /** Create a default flat index. */
    public static MemorySegment newIndex(Arena arena) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW.invokeExact(p));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexFlat_new failed", t); }
    }

    /** Create a flat index with dimension and metric. */
    public static MemorySegment newWithMetric(Arena arena, long d, int metric) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW_WITH.invokeExact(p, d, metric));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexFlat_new_with failed", t); }
    }

    /** Create a L2 flat index. */
    public static MemorySegment newL2(Arena arena) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW_L2.invokeExact(p));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexFlatL2_new failed", t); }
    }

    /** Create a L2 flat index with dimension. */
    public static MemorySegment newL2WithDim(Arena arena, long d) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW_L2_WITH.invokeExact(p, d));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexFlatL2_new_with failed", t); }
    }

    /** Create an inner-product flat index. */
    public static MemorySegment newIP(Arena arena) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW_IP.invokeExact(p));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexFlatIP_new failed", t); }
    }

    /** Create an inner-product flat index with dimension. */
    public static MemorySegment newIPWithDim(Arena arena, long d) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW_IP_WITH.invokeExact(p, d));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexFlatIP_new_with failed", t); }
    }

    /** Compute distances to a subset of vectors. */
    public static void computeDistanceSubset(MemorySegment index, long n, MemorySegment x,
                                             long k, MemorySegment distances, MemorySegment labels) {
        try {
            FaissNative.checkError((int) COMPUTE_DISTANCE_SUBSET.invokeExact(
                    index, n, x, k, distances, labels));
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("computeDistanceSubset failed", t); }
    }

    /** Create a refine-flat index wrapping a base index. */
    public static MemorySegment newRefineFlat(Arena arena, MemorySegment baseIndex) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW_REFINE.invokeExact(p, baseIndex));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexRefineFlat_new failed", t); }
    }
}

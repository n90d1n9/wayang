package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS HNSW index operations ({@code IndexHNSW_c.h}).
 */
public final class FaissHNSWBindings {

    // int faiss_IndexHNSWFlat_new(FaissIndexHNSWFlat** p_index)
    private static final MethodHandle NEW = FaissNative.downcallHandle(
            "faiss_IndexHNSWFlat_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_IndexHNSWFlat_new_with(FaissIndexHNSWFlat** p_index, idx_t d, int M)
    private static final MethodHandle NEW_WITH = FaissNative.downcallHandle(
            "faiss_IndexHNSWFlat_new_with",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));

    private FaissHNSWBindings() {}

    /** Create a default HNSW flat index. */
    public static MemorySegment newIndex(Arena arena) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW.invokeExact(p));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexHNSWFlat_new failed", t); }
    }

    /** Create an HNSW flat index with dimension and M parameter. */
    public static MemorySegment newWithParams(Arena arena, long d, int m) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW_WITH.invokeExact(p, d, m));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexHNSWFlat_new_with failed", t); }
    }
}

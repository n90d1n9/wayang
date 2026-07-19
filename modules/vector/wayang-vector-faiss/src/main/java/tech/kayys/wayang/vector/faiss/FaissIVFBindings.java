package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS IVF index operations ({@code IndexIVF_c.h} + {@code IndexIVFFlat_c.h}).
 */
public final class FaissIVFBindings {

    // Getters/setters for nprobe
    private static final MethodHandle GET_NPROBE = FaissNative.downcallHandle(
            "faiss_IndexIVF_nprobe",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    private static final MethodHandle SET_NPROBE = FaissNative.downcallHandle(
            "faiss_IndexIVF_set_nprobe",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // idx_t faiss_IndexIVF_nlist(const FaissIndexIVF*)
    private static final MethodHandle GET_NLIST = FaissNative.downcallHandle(
            "faiss_IndexIVF_nlist",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // FaissIndex* faiss_IndexIVF_quantizer(const FaissIndexIVF*)
    private static final MethodHandle GET_QUANTIZER = FaissNative.downcallHandle(
            "faiss_IndexIVF_quantizer",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // int faiss_IndexIVF_merge_from(FaissIndexIVF* index, FaissIndexIVF* other, idx_t add_id)
    private static final MethodHandle MERGE_FROM = FaissNative.downcallHandle(
            "faiss_IndexIVF_merge_from",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // int faiss_IndexIVF_make_direct_map(FaissIndexIVF*, int new_maintain_direct_map)
    private static final MethodHandle MAKE_DIRECT_MAP = FaissNative.downcallHandle(
            "faiss_IndexIVF_make_direct_map",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    // int faiss_IndexIVFFlat_new(FaissIndexIVFFlat** p_index)
    private static final MethodHandle IVFFLAT_NEW = FaissNative.downcallHandle(
            "faiss_IndexIVFFlat_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_IndexIVFFlat_new_with(FaissIndexIVFFlat** p_index, FaissIndex* quantizer,
    //     size_t d, size_t nlist)
    private static final MethodHandle IVFFLAT_NEW_WITH = FaissNative.downcallHandle(
            "faiss_IndexIVFFlat_new_with",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));

    // int faiss_IndexIVFFlat_new_with_metric(FaissIndexIVFFlat** p_index, FaissIndex* quantizer,
    //     size_t d, size_t nlist, FaissMetricType metric)
    private static final MethodHandle IVFFLAT_NEW_WITH_METRIC = FaissNative.downcallHandle(
            "faiss_IndexIVFFlat_new_with_metric",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));

    private FaissIVFBindings() {}

    public static long getNprobe(MemorySegment index) {
        try { return (long) GET_NPROBE.invokeExact(index); }
        catch (Throwable t) { throw new FaissException("getNprobe failed", t); }
    }

    public static void setNprobe(MemorySegment index, long nprobe) {
        try { SET_NPROBE.invokeExact(index, nprobe); }
        catch (Throwable t) { throw new FaissException("setNprobe failed", t); }
    }

    public static long getNlist(MemorySegment index) {
        try { return (long) GET_NLIST.invokeExact(index); }
        catch (Throwable t) { throw new FaissException("getNlist failed", t); }
    }

    public static MemorySegment getQuantizer(MemorySegment index) {
        try { return (MemorySegment) GET_QUANTIZER.invokeExact(index); }
        catch (Throwable t) { throw new FaissException("getQuantizer failed", t); }
    }

    public static void mergeFrom(MemorySegment index, MemorySegment other, long addId) {
        try { FaissNative.checkError((int) MERGE_FROM.invokeExact(index, other, addId)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("mergeFrom failed", t); }
    }

    public static void makeDirectMap(MemorySegment index, boolean maintain) {
        try { FaissNative.checkError((int) MAKE_DIRECT_MAP.invokeExact(index, maintain ? 1 : 0)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("makeDirectMap failed", t); }
    }

    public static MemorySegment newIVFFlat(Arena arena) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) IVFFLAT_NEW.invokeExact(p));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexIVFFlat_new failed", t); }
    }

    public static MemorySegment newIVFFlatWith(Arena arena, MemorySegment quantizer, long d, long nlist) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) IVFFLAT_NEW_WITH.invokeExact(p, quantizer, d, nlist));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexIVFFlat_new_with failed", t); }
    }

    public static MemorySegment newIVFFlatWithMetric(Arena arena, MemorySegment quantizer,
                                                      long d, long nlist, int metric) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) IVFFLAT_NEW_WITH_METRIC.invokeExact(p, quantizer, d, nlist, metric));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexIVFFlat_new_with_metric failed", t); }
    }
}

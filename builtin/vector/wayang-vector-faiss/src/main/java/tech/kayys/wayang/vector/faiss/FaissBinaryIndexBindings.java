package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS Binary Index operations
 * ({@code IndexBinary_c.h}, {@code IndexBinaryFlat_c.h}, {@code IndexBinaryIVF_c.h}).
 */
public final class FaissBinaryIndexBindings {

    // ==================== Base BinaryIndex ====================

    private static final MethodHandle FREE = FaissNative.downcallHandle(
            "faiss_IndexBinary_free",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    private static final MethodHandle TRAIN = FaissNative.downcallHandle(
            "faiss_IndexBinary_train",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    private static final MethodHandle ADD = FaissNative.downcallHandle(
            "faiss_IndexBinary_add",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    private static final MethodHandle ADD_WITH_IDS = FaissNative.downcallHandle(
            "faiss_IndexBinary_add_with_ids",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle SEARCH = FaissNative.downcallHandle(
            "faiss_IndexBinary_search",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle RESET = FaissNative.downcallHandle(
            "faiss_IndexBinary_reset",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle REMOVE_IDS = FaissNative.downcallHandle(
            "faiss_IndexBinary_remove_ids",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle RECONSTRUCT = FaissNative.downcallHandle(
            "faiss_IndexBinary_reconstruct",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    private static final MethodHandle GET_D = FaissNative.downcallHandle(
            "faiss_IndexBinary_d",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle GET_NTOTAL = FaissNative.downcallHandle(
            "faiss_IndexBinary_ntotal",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // ==================== BinaryFlat ====================

    private static final MethodHandle FLAT_NEW = FaissNative.downcallHandle(
            "faiss_IndexBinaryFlat_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // ==================== BinaryIVF ====================

    private static final MethodHandle BIVF_GET_NPROBE = FaissNative.downcallHandle(
            "faiss_IndexBinaryIVF_nprobe",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    private static final MethodHandle BIVF_SET_NPROBE = FaissNative.downcallHandle(
            "faiss_IndexBinaryIVF_set_nprobe",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    private static final MethodHandle BIVF_GET_NLIST = FaissNative.downcallHandle(
            "faiss_IndexBinaryIVF_nlist",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    private FaissBinaryIndexBindings() {}

    // ==================== Base API ====================

    public static void free(MemorySegment index) {
        try { FREE.invokeExact(index); }
        catch (Throwable t) { throw new FaissException("IndexBinary_free failed", t); }
    }

    public static void train(MemorySegment index, long n, MemorySegment x) {
        try { FaissNative.checkError((int) TRAIN.invokeExact(index, n, x)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexBinary_train failed", t); }
    }

    public static void add(MemorySegment index, long n, MemorySegment x) {
        try { FaissNative.checkError((int) ADD.invokeExact(index, n, x)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexBinary_add failed", t); }
    }

    public static void addWithIds(MemorySegment index, long n, MemorySegment x, MemorySegment ids) {
        try { FaissNative.checkError((int) ADD_WITH_IDS.invokeExact(index, n, x, ids)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexBinary_add_with_ids failed", t); }
    }

    public static void search(MemorySegment index, long n, MemorySegment x,
                               long k, MemorySegment distances, MemorySegment labels) {
        try { FaissNative.checkError((int) SEARCH.invokeExact(index, n, x, k, distances, labels)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexBinary_search failed", t); }
    }

    public static void reset(MemorySegment index) {
        try { FaissNative.checkError((int) RESET.invokeExact(index)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexBinary_reset failed", t); }
    }

    public static long removeIds(MemorySegment index, MemorySegment selector) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nRemoved = arena.allocate(ValueLayout.JAVA_LONG);
            FaissNative.checkError((int) REMOVE_IDS.invokeExact(index, selector, nRemoved));
            return nRemoved.get(ValueLayout.JAVA_LONG, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexBinary_remove_ids failed", t); }
    }

    public static void reconstruct(MemorySegment index, long key, MemorySegment recons) {
        try { FaissNative.checkError((int) RECONSTRUCT.invokeExact(index, key, recons)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexBinary_reconstruct failed", t); }
    }

    public static int getDimension(MemorySegment index) {
        try { return (int) GET_D.invokeExact(index); }
        catch (Throwable t) { throw new FaissException("IndexBinary_d failed", t); }
    }

    public static long getNTotal(MemorySegment index) {
        try { return (long) GET_NTOTAL.invokeExact(index); }
        catch (Throwable t) { throw new FaissException("IndexBinary_ntotal failed", t); }
    }

    // ==================== BinaryFlat API ====================

    public static MemorySegment newBinaryFlat(Arena arena, long d) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) FLAT_NEW.invokeExact(p, d));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexBinaryFlat_new failed", t); }
    }

    // ==================== BinaryIVF API ====================

    public static long bivfGetNprobe(MemorySegment index) {
        try { return (long) BIVF_GET_NPROBE.invokeExact(index); }
        catch (Throwable t) { throw new FaissException("IndexBinaryIVF_nprobe failed", t); }
    }

    public static void bivfSetNprobe(MemorySegment index, long nprobe) {
        try { BIVF_SET_NPROBE.invokeExact(index, nprobe); }
        catch (Throwable t) { throw new FaissException("IndexBinaryIVF_set_nprobe failed", t); }
    }

    public static long bivfGetNlist(MemorySegment index) {
        try { return (long) BIVF_GET_NLIST.invokeExact(index); }
        catch (Throwable t) { throw new FaissException("IndexBinaryIVF_nlist failed", t); }
    }
}

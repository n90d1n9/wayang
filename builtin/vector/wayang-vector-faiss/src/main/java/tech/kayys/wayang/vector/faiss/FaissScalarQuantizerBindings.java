package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS ScalarQuantizer indexes ({@code IndexScalarQuantizer_c.h}).
 */
public final class FaissScalarQuantizerBindings {

    // int faiss_IndexScalarQuantizer_new(FaissIndexScalarQuantizer** p_index)
    private static final MethodHandle NEW = FaissNative.downcallHandle(
            "faiss_IndexScalarQuantizer_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_IndexScalarQuantizer_new_with(FaissIndexScalarQuantizer**, idx_t d,
    //     FaissScalarQuantizerQT qt, FaissMetricType metric)
    private static final MethodHandle NEW_WITH = FaissNative.downcallHandle(
            "faiss_IndexScalarQuantizer_new_with",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    // int faiss_IndexIVFScalarQuantizer_new(FaissIndexIVFScalarQuantizer** p_index)
    private static final MethodHandle IVF_SQ_NEW = FaissNative.downcallHandle(
            "faiss_IndexIVFScalarQuantizer_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_IndexIVFScalarQuantizer_new_with(FaissIndexIVFScalarQuantizer**,
    //     FaissIndex* quantizer, size_t d, size_t nlist, FaissScalarQuantizerQT qt)
    private static final MethodHandle IVF_SQ_NEW_WITH = FaissNative.downcallHandle(
            "faiss_IndexIVFScalarQuantizer_new_with",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));

    // int faiss_IndexIVFScalarQuantizer_new_with_metric(... + FaissMetricType)
    private static final MethodHandle IVF_SQ_NEW_WITH_METRIC = FaissNative.downcallHandle(
            "faiss_IndexIVFScalarQuantizer_new_with_metric",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    private FaissScalarQuantizerBindings() {}

    /** ScalarQuantizer type constants. */
    public static final int QT_8bit = 0;
    public static final int QT_4bit = 1;
    public static final int QT_8bit_uniform = 2;
    public static final int QT_4bit_uniform = 3;
    public static final int QT_fp16 = 4;
    public static final int QT_8bit_direct = 5;
    public static final int QT_6bit = 6;

    public static MemorySegment newIndex(Arena arena) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW.invokeExact(p));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexScalarQuantizer_new failed", t); }
    }

    public static MemorySegment newWithParams(Arena arena, long d, int qt, int metric) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW_WITH.invokeExact(p, d, qt, metric));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexScalarQuantizer_new_with failed", t); }
    }

    public static MemorySegment newIVFSQ(Arena arena) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) IVF_SQ_NEW.invokeExact(p));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexIVFScalarQuantizer_new failed", t); }
    }

    public static MemorySegment newIVFSQWith(Arena arena, MemorySegment quantizer,
                                              long d, long nlist, int qt) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) IVF_SQ_NEW_WITH.invokeExact(p, quantizer, d, nlist, qt));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexIVFScalarQuantizer_new_with failed", t); }
    }

    public static MemorySegment newIVFSQWithMetric(Arena arena, MemorySegment quantizer,
                                                    long d, long nlist, int qt, int metric) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) IVF_SQ_NEW_WITH_METRIC.invokeExact(
                    p, quantizer, d, nlist, qt, metric));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexIVFScalarQuantizer_new_with_metric failed", t); }
    }
}

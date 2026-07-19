package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS VectorTransform operations ({@code VectorTransform_c.h}).
 * Dimensionality reduction and pre-processing transforms: PCA, OPQ, ITQ, etc.
 */
public final class FaissTransformBindings {

    // ==================== Base VectorTransform ====================

    private static final MethodHandle VT_D_IN = FaissNative.downcallHandle(
            "faiss_VectorTransform_d_in",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle VT_D_OUT = FaissNative.downcallHandle(
            "faiss_VectorTransform_d_out",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle VT_IS_TRAINED = FaissNative.downcallHandle(
            "faiss_VectorTransform_is_trained",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_VectorTransform_train(FaissVectorTransform*, idx_t n, const float* x)
    private static final MethodHandle VT_TRAIN = FaissNative.downcallHandle(
            "faiss_VectorTransform_train",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // float* faiss_VectorTransform_apply(FaissVectorTransform*, idx_t n, const float* x)
    private static final MethodHandle VT_APPLY = FaissNative.downcallHandle(
            "faiss_VectorTransform_apply",
            FunctionDescriptor.of(ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // void faiss_VectorTransform_free(FaissVectorTransform*)
    private static final MethodHandle VT_FREE = FaissNative.downcallHandle(
            "faiss_VectorTransform_free",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // ==================== PCA ====================

    // int faiss_PCAMatrix_new(FaissPCAMatrix**, int d_in, int d_out)
    private static final MethodHandle PCA_NEW = FaissNative.downcallHandle(
            "faiss_PCAMatrix_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    // ==================== OPQ ====================

    // int faiss_OPQMatrix_new(FaissOPQMatrix**, int d, int M)
    private static final MethodHandle OPQ_NEW = FaissNative.downcallHandle(
            "faiss_OPQMatrix_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    // ==================== ITQ ====================

    // int faiss_ITQMatrix_new(FaissITQMatrix**, int d)
    private static final MethodHandle ITQ_MATRIX_NEW = FaissNative.downcallHandle(
            "faiss_ITQMatrix_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    // int faiss_ITQTransform_new(FaissITQTransform**, int d_in, int d_out, int do_pca)
    private static final MethodHandle ITQ_TRANSFORM_NEW = FaissNative.downcallHandle(
            "faiss_ITQTransform_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    // ==================== LinearTransform ====================

    // int faiss_LinearTransform_new(FaissLinearTransform**, int d_in, int d_out, int have_bias)
    private static final MethodHandle LINEAR_NEW = FaissNative.downcallHandle(
            "faiss_LinearTransform_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    // ==================== RemapDimensionsTransform ====================

    // int faiss_RemapDimensionsTransform_new(FaissRemapDimensionsTransform**, int d_in, int d_out, int uniform)
    private static final MethodHandle REMAP_NEW = FaissNative.downcallHandle(
            "faiss_RemapDimensionsTransform_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    private FaissTransformBindings() {}

    // ==================== VectorTransform Base ====================

    public static int dIn(MemorySegment vt) {
        try { return (int) VT_D_IN.invokeExact(vt); }
        catch (Throwable t) { throw new FaissException("VectorTransform_d_in failed", t); }
    }

    public static int dOut(MemorySegment vt) {
        try { return (int) VT_D_OUT.invokeExact(vt); }
        catch (Throwable t) { throw new FaissException("VectorTransform_d_out failed", t); }
    }

    public static boolean isTrained(MemorySegment vt) {
        try { return ((int) VT_IS_TRAINED.invokeExact(vt)) != 0; }
        catch (Throwable t) { throw new FaissException("VectorTransform_is_trained failed", t); }
    }

    public static void train(MemorySegment vt, long n, MemorySegment x) {
        try { FaissNative.checkError((int) VT_TRAIN.invokeExact(vt, n, x)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("VectorTransform_train failed", t); }
    }

    public static MemorySegment apply(MemorySegment vt, long n, MemorySegment x) {
        try { return (MemorySegment) VT_APPLY.invokeExact(vt, n, x); }
        catch (Throwable t) { throw new FaissException("VectorTransform_apply failed", t); }
    }

    public static void free(MemorySegment vt) {
        try { VT_FREE.invokeExact(vt); }
        catch (Throwable t) { throw new FaissException("VectorTransform_free failed", t); }
    }

    // ==================== Constructors ====================

    /** Create a PCA matrix transform. */
    public static MemorySegment newPCA(Arena arena, int dIn, int dOut) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) PCA_NEW.invokeExact(p, dIn, dOut));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("PCAMatrix_new failed", t); }
    }

    /** Create an OPQ matrix transform. */
    public static MemorySegment newOPQ(Arena arena, int d, int M) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) OPQ_NEW.invokeExact(p, d, M));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("OPQMatrix_new failed", t); }
    }

    /** Create an ITQ matrix. */
    public static MemorySegment newITQMatrix(Arena arena, int d) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) ITQ_MATRIX_NEW.invokeExact(p, d));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("ITQMatrix_new failed", t); }
    }

    /** Create an ITQ transform. */
    public static MemorySegment newITQTransform(Arena arena, int dIn, int dOut, boolean doPCA) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) ITQ_TRANSFORM_NEW.invokeExact(p, dIn, dOut, doPCA ? 1 : 0));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("ITQTransform_new failed", t); }
    }

    /** Create a linear transform. */
    public static MemorySegment newLinearTransform(Arena arena, int dIn, int dOut, boolean haveBias) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) LINEAR_NEW.invokeExact(p, dIn, dOut, haveBias ? 1 : 0));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("LinearTransform_new failed", t); }
    }

    /** Create a dimension remapping transform. */
    public static MemorySegment newRemapDimensions(Arena arena, int dIn, int dOut, boolean uniform) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) REMAP_NEW.invokeExact(p, dIn, dOut, uniform ? 1 : 0));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("RemapDimensionsTransform_new failed", t); }
    }
}

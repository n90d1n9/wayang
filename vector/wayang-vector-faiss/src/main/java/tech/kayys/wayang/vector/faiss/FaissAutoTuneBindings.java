package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS AutoTune/ParameterSpace ({@code AutoTune_c.h}).
 * Automatic parameter tuning for FAISS indexes.
 */
public final class FaissAutoTuneBindings {

    // int faiss_ParameterSpace_new(FaissParameterSpace** space)
    private static final MethodHandle NEW = FaissNative.downcallHandle(
            "faiss_ParameterSpace_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // size_t faiss_ParameterSpace_n_combinations(const FaissParameterSpace*)
    private static final MethodHandle N_COMBINATIONS = FaissNative.downcallHandle(
            "faiss_ParameterSpace_n_combinations",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    // int faiss_ParameterSpace_set_index_parameters(const FaissParameterSpace*, FaissIndex*, const char*)
    private static final MethodHandle SET_PARAMS = FaissNative.downcallHandle(
            "faiss_ParameterSpace_set_index_parameters",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // int faiss_ParameterSpace_set_index_parameter(const FaissParameterSpace*, FaissIndex*, const char*, double)
    private static final MethodHandle SET_PARAM = FaissNative.downcallHandle(
            "faiss_ParameterSpace_set_index_parameter",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE));

    // int faiss_ParameterSpace_combination_name(const FaissParameterSpace*, size_t, char*, size_t)
    private static final MethodHandle COMBINATION_NAME = FaissNative.downcallHandle(
            "faiss_ParameterSpace_combination_name",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // void faiss_ParameterSpace_display(const FaissParameterSpace*)
    private static final MethodHandle DISPLAY = FaissNative.downcallHandle(
            "faiss_ParameterSpace_display",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // void faiss_ParameterSpace_free(FaissParameterSpace*)
    private static final MethodHandle FREE = FaissNative.downcallHandle(
            "faiss_ParameterSpace_free",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    private FaissAutoTuneBindings() {}

    /** Create a new ParameterSpace. */
    public static MemorySegment newParameterSpace(Arena arena) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW.invokeExact(p));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("ParameterSpace_new failed", t); }
    }

    /** Get number of parameter combinations. */
    public static long nCombinations(MemorySegment space) {
        try { return (long) N_COMBINATIONS.invokeExact(space); }
        catch (Throwable t) { throw new FaissException("n_combinations failed", t); }
    }

    /** Set index parameters described by a string (e.g. "nprobe=16,efSearch=40"). */
    public static void setIndexParameters(MemorySegment space, MemorySegment index, String params) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pStr = arena.allocateFrom(params);
            FaissNative.checkError((int) SET_PARAMS.invokeExact(space, index, pStr));
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("setIndexParameters failed", t); }
    }

    /** Set a single index parameter. */
    public static void setIndexParameter(MemorySegment space, MemorySegment index,
                                         String name, double value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pName = arena.allocateFrom(name);
            FaissNative.checkError((int) SET_PARAM.invokeExact(space, index, pName, value));
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("setIndexParameter failed", t); }
    }

    /** Get combination name as a string. */
    public static String combinationName(MemorySegment space, long combination) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(1000);
            FaissNative.checkError((int) COMBINATION_NAME.invokeExact(space, combination, buf, 1000L));
            return buf.getString(0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("combinationName failed", t); }
    }

    /** Display parameter space info to stdout. */
    public static void display(MemorySegment space) {
        try { DISPLAY.invokeExact(space); }
        catch (Throwable t) { throw new FaissException("display failed", t); }
    }

    /** Free parameter space resources. */
    public static void free(MemorySegment space) {
        try { FREE.invokeExact(space); }
        catch (Throwable t) { throw new FaissException("free failed", t); }
    }
}

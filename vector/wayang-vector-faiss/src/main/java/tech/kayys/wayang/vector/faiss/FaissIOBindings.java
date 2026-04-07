package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS I/O and clone operations
 * ({@code index_io_c.h} + {@code clone_index_c.h}).
 */
public final class FaissIOBindings {

    // int faiss_write_index_fname(const FaissIndex* idx, const char* fname)
    private static final MethodHandle WRITE_INDEX_FNAME = FaissNative.downcallHandle(
            "faiss_write_index_fname",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // int faiss_read_index_fname(const char* fname, int io_flags, FaissIndex** p_out)
    private static final MethodHandle READ_INDEX_FNAME = FaissNative.downcallHandle(
            "faiss_read_index_fname",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_write_index_binary_fname(const FaissIndexBinary* idx, const char* fname)
    private static final MethodHandle WRITE_BINARY_FNAME = FaissNative.downcallHandle(
            "faiss_write_index_binary_fname",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // int faiss_read_index_binary_fname(const char* fname, int io_flags, FaissIndexBinary** p_out)
    private static final MethodHandle READ_BINARY_FNAME = FaissNative.downcallHandle(
            "faiss_read_index_binary_fname",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_clone_index(const FaissIndex*, FaissIndex** p_out)
    private static final MethodHandle CLONE_INDEX = FaissNative.downcallHandle(
            "faiss_clone_index",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // int faiss_clone_index_binary(const FaissIndexBinary*, FaissIndexBinary** p_out)
    private static final MethodHandle CLONE_BINARY = FaissNative.downcallHandle(
            "faiss_clone_index_binary",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private FaissIOBindings() {}

    /** Write index to a file. */
    public static void writeIndex(MemorySegment index, String filename) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fname = arena.allocateFrom(filename);
            FaissNative.checkError((int) WRITE_INDEX_FNAME.invokeExact(index, fname));
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("writeIndex failed", t); }
    }

    /** Read index from a file. */
    public static MemorySegment readIndex(Arena arena, String filename, int ioFlags) {
        try {
            MemorySegment fname = arena.allocateFrom(filename);
            MemorySegment pOut = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) READ_INDEX_FNAME.invokeExact(fname, ioFlags, pOut));
            return pOut.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("readIndex failed", t); }
    }

    /** Write binary index to a file. */
    public static void writeBinaryIndex(MemorySegment index, String filename) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fname = arena.allocateFrom(filename);
            FaissNative.checkError((int) WRITE_BINARY_FNAME.invokeExact(index, fname));
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("writeBinaryIndex failed", t); }
    }

    /** Read binary index from a file. */
    public static MemorySegment readBinaryIndex(Arena arena, String filename, int ioFlags) {
        try {
            MemorySegment fname = arena.allocateFrom(filename);
            MemorySegment pOut = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) READ_BINARY_FNAME.invokeExact(fname, ioFlags, pOut));
            return pOut.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("readBinaryIndex failed", t); }
    }

    /** Deep-copy (clone) an index. */
    public static MemorySegment cloneIndex(Arena arena, MemorySegment index) {
        try {
            MemorySegment pOut = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) CLONE_INDEX.invokeExact(index, pOut));
            return pOut.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("cloneIndex failed", t); }
    }

    /** Deep-copy (clone) a binary index. */
    public static MemorySegment cloneBinaryIndex(Arena arena, MemorySegment index) {
        try {
            MemorySegment pOut = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) CLONE_BINARY.invokeExact(index, pOut));
            return pOut.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("cloneBinaryIndex failed", t); }
    }
}

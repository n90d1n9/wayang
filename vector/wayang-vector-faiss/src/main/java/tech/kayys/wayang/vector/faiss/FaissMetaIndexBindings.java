package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS MetaIndexes ({@code MetaIndexes_c.h}),
 * IndexReplicas ({@code IndexReplicas_c.h}), IndexShards ({@code IndexShards_c.h}),
 * IndexPreTransform ({@code IndexPreTransform_c.h}), and IndexLSH ({@code IndexLSH_c.h}).
 */
public final class FaissMetaIndexBindings {

    // ==================== IDMap ====================

    private static final MethodHandle IDMAP_NEW = FaissNative.downcallHandle(
            "faiss_IndexIDMap_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle IDMAP_SUB_INDEX = FaissNative.downcallHandle(
            "faiss_IndexIDMap_sub_index",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // ==================== IDMap2 ====================

    private static final MethodHandle IDMAP2_NEW = FaissNative.downcallHandle(
            "faiss_IndexIDMap2_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle IDMAP2_CONSTRUCT_REV_MAP = FaissNative.downcallHandle(
            "faiss_IndexIDMap2_construct_rev_map",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // ==================== Replicas ====================

    private static final MethodHandle REPLICAS_NEW = FaissNative.downcallHandle(
            "faiss_IndexReplicas_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    private static final MethodHandle REPLICAS_ADD = FaissNative.downcallHandle(
            "faiss_IndexReplicas_add_replica",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle REPLICAS_REMOVE = FaissNative.downcallHandle(
            "faiss_IndexReplicas_remove_replica",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // ==================== Shards ====================

    private static final MethodHandle SHARDS_NEW = FaissNative.downcallHandle(
            "faiss_IndexShards_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    private static final MethodHandle SHARDS_ADD = FaissNative.downcallHandle(
            "faiss_IndexShards_add_shard",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle SHARDS_REMOVE = FaissNative.downcallHandle(
            "faiss_IndexShards_remove_shard",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // ==================== PreTransform ====================

    private static final MethodHandle PRETRANSFORM_NEW = FaissNative.downcallHandle(
            "faiss_IndexPreTransform_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle PRETRANSFORM_NEW_WITH = FaissNative.downcallHandle(
            "faiss_IndexPreTransform_new_with",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle PRETRANSFORM_PREPEND = FaissNative.downcallHandle(
            "faiss_IndexPreTransform_prepend_transform",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // ==================== LSH ====================

    private static final MethodHandle LSH_NEW = FaissNative.downcallHandle(
            "faiss_IndexLSH_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));

    private static final MethodHandle LSH_NEW_WITH_OPTIONS = FaissNative.downcallHandle(
            "faiss_IndexLSH_new_with_options",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    private FaissMetaIndexBindings() {}

    // ==================== IDMap API ====================

    /** Create an IDMap wrapping an existing index. */
    public static MemorySegment newIDMap(Arena arena, MemorySegment baseIndex) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) IDMAP_NEW.invokeExact(p, baseIndex));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexIDMap_new failed", t); }
    }

    /** Get the sub-index of an IDMap. */
    public static MemorySegment idMapSubIndex(MemorySegment index) {
        try { return (MemorySegment) IDMAP_SUB_INDEX.invokeExact(index); }
        catch (Throwable t) { throw new FaissException("IndexIDMap_sub_index failed", t); }
    }

    // ==================== IDMap2 API ====================

    /** Create an IDMap2 (with reverse map) wrapping an existing index. */
    public static MemorySegment newIDMap2(Arena arena, MemorySegment baseIndex) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) IDMAP2_NEW.invokeExact(p, baseIndex));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexIDMap2_new failed", t); }
    }

    /** Construct the reverse map for an IDMap2. */
    public static void constructRevMap(MemorySegment index) {
        try { FaissNative.checkError((int) IDMAP2_CONSTRUCT_REV_MAP.invokeExact(index)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("constructRevMap failed", t); }
    }

    // ==================== Replicas API ====================

    /** Create a replicated index. */
    public static MemorySegment newReplicas(Arena arena, long d) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) REPLICAS_NEW.invokeExact(p, d));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexReplicas_new failed", t); }
    }

    /** Add a replica. */
    public static void addReplica(MemorySegment replicas, MemorySegment index) {
        try { FaissNative.checkError((int) REPLICAS_ADD.invokeExact(replicas, index)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("addReplica failed", t); }
    }

    /** Remove a replica. */
    public static void removeReplica(MemorySegment replicas, MemorySegment index) {
        try { FaissNative.checkError((int) REPLICAS_REMOVE.invokeExact(replicas, index)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("removeReplica failed", t); }
    }

    // ==================== Shards API ====================

    /** Create a sharded index. */
    public static MemorySegment newShards(Arena arena, long d) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) SHARDS_NEW.invokeExact(p, d));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexShards_new failed", t); }
    }

    /** Add a shard. */
    public static void addShard(MemorySegment shards, MemorySegment index) {
        try { FaissNative.checkError((int) SHARDS_ADD.invokeExact(shards, index)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("addShard failed", t); }
    }

    /** Remove a shard. */
    public static void removeShard(MemorySegment shards, MemorySegment index) {
        try { FaissNative.checkError((int) SHARDS_REMOVE.invokeExact(shards, index)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("removeShard failed", t); }
    }

    // ==================== PreTransform API ====================

    /** Create a pre-transform index. */
    public static MemorySegment newPreTransform(Arena arena) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) PRETRANSFORM_NEW.invokeExact(p));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexPreTransform_new failed", t); }
    }

    /** Create a pre-transform index wrapping a sub-index. */
    public static MemorySegment newPreTransformWith(Arena arena, MemorySegment subIndex) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) PRETRANSFORM_NEW_WITH.invokeExact(p, subIndex));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexPreTransform_new_with failed", t); }
    }

    /** Prepend a transform to the index. */
    public static void prependTransform(MemorySegment index, MemorySegment transform) {
        try { FaissNative.checkError((int) PRETRANSFORM_PREPEND.invokeExact(index, transform)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("prependTransform failed", t); }
    }

    // ==================== LSH API ====================

    /** Create an LSH index. */
    public static MemorySegment newLSH(Arena arena, long d, int nbits) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) LSH_NEW.invokeExact(p, d, nbits));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexLSH_new failed", t); }
    }

    /** Create an LSH index with options. */
    public static MemorySegment newLSHWithOptions(Arena arena, long d, int nbits, int rotateData, int trainThresholds) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) LSH_NEW_WITH_OPTIONS.invokeExact(p, d, nbits, rotateData, trainThresholds));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("IndexLSH_new_with_options failed", t); }
    }
}

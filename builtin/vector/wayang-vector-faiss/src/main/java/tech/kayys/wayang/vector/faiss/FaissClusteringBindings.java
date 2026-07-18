package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for FAISS Clustering operations ({@code Clustering_c.h}).
 * Provides k-means clustering and related utilities.
 */
public final class FaissClusteringBindings {

    // void faiss_ClusteringParameters_init(FaissClusteringParameters* params)
    private static final MethodHandle PARAMS_INIT = FaissNative.downcallHandle(
            "faiss_ClusteringParameters_init",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // int faiss_Clustering_new(FaissClustering** p_clustering, int d, int k)
    private static final MethodHandle NEW = FaissNative.downcallHandle(
            "faiss_Clustering_new",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    // int faiss_Clustering_new_with_params(FaissClustering**, int d, int k, const FaissClusteringParameters*)
    private static final MethodHandle NEW_WITH_PARAMS = FaissNative.downcallHandle(
            "faiss_Clustering_new_with_params",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // int faiss_Clustering_train(FaissClustering*, idx_t n, const float* x, FaissIndex* index)
    private static final MethodHandle TRAIN = FaissNative.downcallHandle(
            "faiss_Clustering_train",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // void faiss_Clustering_centroids(FaissClustering*, float** centroids, size_t* size)
    private static final MethodHandle CENTROIDS = FaissNative.downcallHandle(
            "faiss_Clustering_centroids",
            FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // void faiss_Clustering_free(FaissClustering*)
    private static final MethodHandle FREE = FaissNative.downcallHandle(
            "faiss_Clustering_free",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // int faiss_kmeans_clustering(size_t d, size_t n, size_t k, const float* x,
    //     float* centroids, float* q_error)
    private static final MethodHandle KMEANS = FaissNative.downcallHandle(
            "faiss_kmeans_clustering",
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // Getter macros for Clustering properties
    private static final MethodHandle GET_D = FaissNative.downcallHandle(
            "faiss_Clustering_d", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
    private static final MethodHandle GET_K = FaissNative.downcallHandle(
            "faiss_Clustering_k", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    /** Layout for FaissClusteringParameters struct. */
    public static final StructLayout PARAMS_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("niter"),
            ValueLayout.JAVA_INT.withName("nredo"),
            ValueLayout.JAVA_INT.withName("verbose"),
            ValueLayout.JAVA_INT.withName("spherical"),
            ValueLayout.JAVA_INT.withName("int_centroids"),
            ValueLayout.JAVA_INT.withName("update_index"),
            ValueLayout.JAVA_INT.withName("frozen_centroids"),
            ValueLayout.JAVA_INT.withName("min_points_per_centroid"),
            ValueLayout.JAVA_INT.withName("max_points_per_centroid"),
            ValueLayout.JAVA_INT.withName("seed"),
            ValueLayout.JAVA_LONG.withName("decode_block_size")
    );

    private FaissClusteringBindings() {}

    /** Initialize clustering parameters with defaults. */
    public static void initParams(MemorySegment params) {
        try { PARAMS_INIT.invokeExact(params); }
        catch (Throwable t) { throw new FaissException("ClusteringParameters_init failed", t); }
    }

    /** Allocate and initialize default clustering parameters. */
    public static MemorySegment allocateParams(Arena arena) {
        MemorySegment params = arena.allocate(PARAMS_LAYOUT);
        initParams(params);
        return params;
    }

    /** Create a new Clustering object. */
    public static MemorySegment newClustering(Arena arena, int d, int k) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW.invokeExact(p, d, k));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("Clustering_new failed", t); }
    }

    /** Create a new Clustering with custom parameters. */
    public static MemorySegment newClusteringWithParams(Arena arena, int d, int k, MemorySegment params) {
        try {
            MemorySegment p = arena.allocate(ValueLayout.ADDRESS);
            FaissNative.checkError((int) NEW_WITH_PARAMS.invokeExact(p, d, k, params));
            return p.get(ValueLayout.ADDRESS, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("Clustering_new_with_params failed", t); }
    }

    /** Train clustering on data. */
    public static void train(MemorySegment clustering, long n, MemorySegment x, MemorySegment index) {
        try { FaissNative.checkError((int) TRAIN.invokeExact(clustering, n, x, index)); }
        catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("Clustering_train failed", t); }
    }

    /** Get centroids and their count. Returns pointer to float array. */
    public static MemorySegment getCentroids(Arena arena, MemorySegment clustering) {
        try {
            MemorySegment pCentroids = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment pSize = arena.allocate(ValueLayout.JAVA_LONG);
            CENTROIDS.invokeExact(clustering, pCentroids, pSize);
            return pCentroids.get(ValueLayout.ADDRESS, 0);
        } catch (Throwable t) { throw new FaissException("Clustering_centroids failed", t); }
    }

    /** Free clustering resources. */
    public static void free(MemorySegment clustering) {
        try { FREE.invokeExact(clustering); }
        catch (Throwable t) { throw new FaissException("Clustering_free failed", t); }
    }

    /**
     * Simplified one-shot k-means clustering.
     *
     * @param arena     memory arena
     * @param d         vector dimension
     * @param n         number of training vectors
     * @param k         number of clusters
     * @param x         training vectors (n * d floats)
     * @param centroids output centroids (k * d floats)
     * @return quantization error
     */
    public static float kmeansClustering(Arena arena, long d, long n, long k,
                                         MemorySegment x, MemorySegment centroids) {
        try {
            MemorySegment qError = arena.allocate(ValueLayout.JAVA_FLOAT);
            FaissNative.checkError((int) KMEANS.invokeExact(d, n, k, x, centroids, qError));
            return qError.get(ValueLayout.JAVA_FLOAT, 0);
        } catch (FaissException e) { throw e; }
        catch (Throwable t) { throw new FaissException("kmeans_clustering failed", t); }
    }

    public static long getD(MemorySegment clustering) {
        try { return (long) GET_D.invokeExact(clustering); }
        catch (Throwable t) { throw new FaissException("Clustering_d failed", t); }
    }

    public static long getK(MemorySegment clustering) {
        try { return (long) GET_K.invokeExact(clustering); }
        catch (Throwable t) { throw new FaissException("Clustering_k failed", t); }
    }
}

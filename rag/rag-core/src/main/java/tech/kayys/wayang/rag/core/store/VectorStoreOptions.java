package tech.kayys.wayang.rag.core.store;

public record VectorStoreOptions(
        String backend,
        String tableName,
        int dimensions,
        boolean ensureExtension,
        boolean createIvfFlatIndex) {

    public static VectorStoreOptions defaults(int dimensions) {
        return new VectorStoreOptions("in-memory", "rag_vectors", dimensions, false, false);
    }
}

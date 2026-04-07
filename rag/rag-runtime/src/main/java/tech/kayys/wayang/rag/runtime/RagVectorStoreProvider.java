package tech.kayys.wayang.rag.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.store.JsonPayloadCodec;
import tech.kayys.wayang.rag.core.store.VectorStore;
import tech.kayys.wayang.rag.core.store.VectorStoreFactory;
import tech.kayys.wayang.rag.core.store.VectorStoreOptions;

import javax.sql.DataSource;

@ApplicationScoped
public class RagVectorStoreProvider {

    @Inject
    RagRuntimeConfig config;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Instance<DataSource> dataSourceInstance;

    private volatile VectorStore<RagChunk> store;

    public VectorStore<RagChunk> getStore() {
        VectorStore<RagChunk> current = store;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (store == null) {
                store = createStore();
            }
            return store;
        }
    }

    private VectorStore<RagChunk> createStore() {
        String backend = normalizeBackend(config.getVectorstoreBackend());
        VectorStoreOptions options = new VectorStoreOptions(
                backend,
                config.getPostgresTable(),
                config.getEmbeddingDimension(),
                true,
                true);

        return VectorStoreFactory.create(
                options,
                dataSourceInstance.isResolvable() ? dataSourceInstance.get() : null,
                new JsonPayloadCodec<>(objectMapper, RagChunk.class),
                objectMapper);
    }

    private String normalizeBackend(String backend) {
        if (backend == null || backend.isBlank()) {
            return "faiss"; // Default to high-performance FAISS
        }
        String normalized = backend.trim().toLowerCase();
        return switch (normalized) {
            case "postgres", "postgresql", "pgvector" -> "pgvector";
            case "memory", "inmemory", "in-memory" -> "in-memory";
            case "faiss" -> "faiss";
            case "redis" -> "redis";
            case "pinecone" -> "pinecone";
            case "chroma" -> "chroma";
            case "qdrant" -> "qdrant";
            case "milvus" -> "milvus";
            default -> normalized;
        };
    }
}

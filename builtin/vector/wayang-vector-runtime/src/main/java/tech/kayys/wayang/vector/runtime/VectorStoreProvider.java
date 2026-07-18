package tech.kayys.wayang.vector.runtime;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import tech.kayys.wayang.vector.VectorStore;
import tech.kayys.wayang.vector.faiss.FaissVectorStore;

/**
 * Factory and producer for VectorStore implementations.
 */
@ApplicationScoped
public class VectorStoreProvider {

    @ConfigProperty(name = "wayang.vector.store.type", defaultValue = "faiss")
    String vectorStoreType;

    private volatile VectorStore vectorStore;

    @Produces
    @ApplicationScoped
    public VectorStore getVectorStore() {
        if (vectorStore == null) {
            synchronized (this) {
                if (vectorStore == null) {
                    vectorStore = createVectorStore(vectorStoreType);
                }
            }
        }
        return vectorStore;
    }

    @ConfigProperty(name = "wayang.vector.faiss.dimension", defaultValue = "768")
    int faissDimension;

    @ConfigProperty(name = "wayang.vector.faiss.index.type", defaultValue = "Flat")
    String faissIndexType;

    @ConfigProperty(name = "wayang.vector.faiss.index.path", defaultValue = "")
    String faissIndexPath;

    private VectorStore createVectorStore(String type) {
        switch (type.toLowerCase()) {
            case "in-memory":
            case "inmemory":
                return new InMemoryVectorStore();
            case "pgvector":
                return new PgVectorStore();
            case "qdrant":
                return new QdrantVectorStore();
            case "milvus":
                return new MilvusVectorStore();
            case "chroma":
                return new ChromaVectorStore();
            case "pinecone":
                return new PineconeVectorStore();
            case "faiss":
                return new FaissVectorStore(faissDimension, faissIndexType,
                        faissIndexPath.isEmpty() ? null : faissIndexPath);
            default:
                throw new IllegalArgumentException("Unknown vector store type: " + type);
        }
    }

    /**
     * Initialize the vector store (e.g., create tables, connect to database).
     */
    public Uni<Void> initialize() {
        // Initialization logic would go here
        return Uni.createFrom().voidItem();
    }
}
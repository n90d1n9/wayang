package tech.kayys.wayang.memory.service;

import tech.kayys.wayang.vector.VectorStore;
import tech.kayys.wayang.vector.runtime.VectorStoreProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating appropriate vector store based on configuration
 */
@ApplicationScoped
public class VectorStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(VectorStoreFactory.class);

    @ConfigProperty(name = "wayang.memory.storage.strategy", defaultValue = "inmemory")
    String storageStrategy;

    @Inject
    Instance<VectorStoreProvider> vectorStoreProviderInstance;

    /**
     * Get configured vector store
     */
    @Produces
    @ApplicationScoped
    public VectorMemoryStore getVectorStore() {
        LOG.info("Using memory storage strategy: {}", storageStrategy);

        if ("inmemory".equalsIgnoreCase(storageStrategy)) {
            return new InMemoryVectorStore();
        } 
        else if ("local".equalsIgnoreCase(storageStrategy)) {
            // Local persistence uses FAISS for vector index and RocksDB for metadata storage
            if (vectorStoreProviderInstance.isUnsatisfied()) {
                LOG.warn("No VectorStoreProvider found for local strategy, falling back to InMemory");
                return new InMemoryVectorStore();
            }
            VectorStore faissStore = vectorStoreProviderInstance.get().getVectorStore();
            if (faissStore instanceof tech.kayys.wayang.vector.faiss.FaissVectorStore) {
                // Initialize RocksDB in a default directory within the Wayang data folder
                String rocksDbPath = System.getProperty("user.home") + "/.wayang/data/memory-rocksdb";
                return new FaissRocksDBMemoryStore((tech.kayys.wayang.vector.faiss.FaissVectorStore) faissStore, rocksDbPath);
            } else {
                LOG.warn("VectorStoreProvider returned non-FAISS store for local strategy, using Adapter");
                return new VectorStoreAdapter(faissStore);
            }
        }
        else if ("external".equalsIgnoreCase(storageStrategy)) {
            if (vectorStoreProviderInstance.isUnsatisfied()) {
                LOG.warn("No VectorStoreProvider found for external strategy, falling back to InMemory");
                return new InMemoryVectorStore();
            }
            VectorStore externalStore = vectorStoreProviderInstance.get().getVectorStore();
            return new VectorStoreAdapter(externalStore);
        }
        else {
            LOG.warn("Unknown storage strategy: {}, falling back to InMemory", storageStrategy);
            return new InMemoryVectorStore();
        }
    }
}
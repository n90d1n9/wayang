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

    @ConfigProperty(name = "gamelan.memory.store.type", defaultValue = "inmemory")
    String storeType;

    @Inject
    Instance<VectorStoreProvider> vectorStoreProviderInstance;

    /**
     * Get configured vector store
     */
    @Produces
    @ApplicationScoped
    public VectorMemoryStore getVectorStore() {
        LOG.info("Using vector store type: {}", storeType);

        if (vectorStoreProviderInstance.isUnsatisfied()) {
            LOG.warn("No VectorStoreProvider found, vector store will be unavailable");
            return null; // or return a mock/dummy
        }
        // Use the vector store provider from the vector module with adapter
        VectorStore vectorStore = vectorStoreProviderInstance.get().getVectorStore();
        return new VectorStoreAdapter(vectorStore);
    }
}
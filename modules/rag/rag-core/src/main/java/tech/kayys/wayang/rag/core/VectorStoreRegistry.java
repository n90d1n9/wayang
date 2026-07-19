package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.rag.core.store.InMemoryVectorStore;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VECTOR STORE REGISTRY - INTERNAL IMPLEMENTATION
 */
@ApplicationScoped
public class VectorStoreRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(VectorStoreRegistry.class);

    private final Map<String, Map<String, VectorStore<RagChunk>>> stores = new ConcurrentHashMap<>();

    @ConfigProperty(name = "gamelan.rag.store.default-type", defaultValue = "in-memory")
    String defaultStoreType;

    public VectorStore<RagChunk> getStore(String tenantId, String storeType) {
        LOG.debug("Getting vector store for tenant: {}, type: {}", tenantId, storeType);

        return stores
                .computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(storeType, k -> createStore(tenantId, storeType));
    }

    private VectorStore<RagChunk> createStore(String tenantId, String storeType) {
        LOG.info("Creating vector store: tenant={}, type={}", tenantId, storeType);

        return switch (storeType.toLowerCase()) {
            case "in-memory" -> new InMemoryVectorStore<>();
            default -> {
                LOG.warn("Unknown store type: {}, using in-memory", storeType);
                yield new InMemoryVectorStore<>();
            }
        };
    }

    public void clearStore(String tenantId, String storeType) {
        LOG.info("Clearing store for tenant: {}, type: {}", tenantId, storeType);
        Map<String, VectorStore<RagChunk>> tenantStores = stores.get(tenantId);
        if (tenantStores != null) {
            tenantStores.remove(storeType);
        }
    }

    public void clearAllStores(String tenantId) {
        LOG.info("Clearing all stores for tenant: {}", tenantId);
        stores.remove(tenantId);
    }
}

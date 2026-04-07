package tech.kayys.wayang.rag.embedding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and managing tenant-specific embedding stores.
 * It handles the lifecycle of vector stores, provides migration capabilities
 * for
 * tenant embedding contracts, and maintains a cache of active store instances.
 */
@ApplicationScoped
public class RagEmbeddingStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RagEmbeddingStoreFactory.class);

    @ConfigProperty(name = "wayang.rag.embedding.model", defaultValue = "hash-1536")
    String embeddingModel;

    @ConfigProperty(name = "wayang.rag.embedding.dimension", defaultValue = "1536")
    int embeddingDimension;

    @ConfigProperty(name = "wayang.rag.embedding.version", defaultValue = "v1")
    String embeddingVersion;

    @Inject
    Instance<VectorStore<RagChunk>> vectorStoreInstance;

    @Inject
    Instance<EmbeddingMetrics> metricsInstance;

    private final Map<String, RagEmbeddingStore> cache = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingSchemaContract> contracts = new ConcurrentHashMap<>();

    public RagEmbeddingStore getStore(String tenantId, RetrievalConfig retrievalConfig) {
        LOG.debug("Getting embedding store for tenant {}", tenantId);
        EmbeddingSchemaContract contract = contracts.computeIfAbsent(tenantId, ignored -> defaultContract());
        return cache.computeIfAbsent(
                tenantId,
                namespace -> new OwnedRagEmbeddingStoreAdapter(
                        namespace,
                        contract.model(),
                        contract.dimension(),
                        contract.version(),
                        resolveVectorStore(),
                        resolveMetrics()));
    }

    public EmbeddingSchemaContract contractForTenant(String tenantId) {
        return contracts.getOrDefault(tenantId, defaultContract());
    }

    public synchronized EmbeddingSchemaContract migrateTenantContract(
            String tenantId,
            String embeddingModel,
            int embeddingDimension,
            String embeddingVersion,
            boolean clearNamespace) {

        EmbeddingSchemaContract target = new EmbeddingSchemaContract(
                embeddingModel,
                embeddingDimension,
                embeddingVersion);
        EmbeddingSchemaContract previous = contractForTenant(tenantId);
        boolean changed = !previous.equals(target);
        if (changed && !clearNamespace) {
            throw new IllegalArgumentException("clearNamespace must be true when embedding contract changes");
        }

        VectorStore<RagChunk> store = resolveVectorStore();
        RagEmbeddingStore existing = cache.remove(tenantId);
        if (clearNamespace) {
            if (existing != null) {
                existing.clear();
            } else {
                store.clear(tenantId);
            }
        }

        contracts.put(tenantId, target);
        cache.put(tenantId, new OwnedRagEmbeddingStoreAdapter(
                tenantId,
                target.model(),
                target.dimension(),
                target.version(),
                store,
                resolveMetrics()));
        return previous;
    }

    private VectorStore<RagChunk> resolveVectorStore() {
        if (vectorStoreInstance.isResolvable()) {
            return vectorStoreInstance.get();
        }
        throw new IllegalStateException("No VectorStore<RagChunk> bean available");
    }

    private EmbeddingMetrics resolveMetrics() {
        return metricsInstance.isResolvable() ? metricsInstance.get() : EmbeddingMetrics.NOOP;
    }

    private EmbeddingSchemaContract defaultContract() {
        String version = embeddingVersion;
        if (version == null || version.isBlank()) {
            version = "v1";
        }
        return new EmbeddingSchemaContract(
                embeddingModel,
                embeddingDimension,
                version);
    }
}

package tech.kayys.wayang.rag.embedding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.embedding.EmbeddingService;

/**
 * Factory for creating embedding and chat models tailored to specific tenants
 * and configurations. It maps requested model names to supported
 * implementations
 * and resolves the necessary metrics collectors for the model instances.
 */
@ApplicationScoped
public class RagModelFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RagModelFactory.class);

    @ConfigProperty(name = "wayang.rag.embedding.dimension", defaultValue = "1536")
    int embeddingDimension;

    @Inject
    EmbeddingService embeddingService;

    @Inject
    Instance<EmbeddingMetrics> metricsInstance;

    public RagEmbeddingModel createEmbeddingModel(String tenantId, String modelName) {
        LOG.debug("Creating embedding model {} for tenant {}", modelName, tenantId);
        String mappedModel = mapModel(modelName);
        EmbeddingMetrics metrics = (metricsInstance != null && metricsInstance.isResolvable()) ? metricsInstance.get()
                : EmbeddingMetrics.NOOP;
        return new OwnedEmbeddingModelAdapter(embeddingService, tenantId, mappedModel, metrics);
    }

    public Object createChatModel(String tenantId, String modelName) {
        LOG.debug("Creating chat model {} for tenant {}", modelName, tenantId);
        return null;
    }

    private String mapModel(String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank()) {
            return "hash-" + embeddingDimension;
        }

        String normalized = requestedModel.trim().toLowerCase();
        if (normalized.startsWith("hash-")
                || normalized.startsWith("tfidf-")
                || normalized.startsWith("chargram-")
                || normalized.equals("hash")
                || normalized.equals("tfidf")
                || normalized.equals("chargram")) {
            return normalized;
        }

        if (normalized.contains("embedding")) {
            return "hash-" + embeddingDimension;
        }
        return "tfidf-" + Math.max(256, embeddingDimension / 2);
    }
}

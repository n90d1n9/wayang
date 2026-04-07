package tech.kayys.wayang.rag.embedding;

import tech.kayys.wayang.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingResponse;
import tech.kayys.wayang.embedding.EmbeddingService;

import java.util.List;
import java.util.Objects;

/**
 * Adapter to bridge Wayang EmbeddingService into rag-runtime owned embedding
 * model.
 */
public class OwnedEmbeddingModelAdapter implements RagEmbeddingModel {

    private final EmbeddingService embeddingService;
    private final String tenantId;
    private final String modelName;
    private final EmbeddingMetrics metrics;

    public OwnedEmbeddingModelAdapter(EmbeddingService embeddingService, String tenantId, String modelName) {
        this(embeddingService, tenantId, modelName, EmbeddingMetrics.NOOP);
    }

    public OwnedEmbeddingModelAdapter(
            EmbeddingService embeddingService,
            String tenantId,
            String modelName,
            EmbeddingMetrics metrics) {
        this.embeddingService = Objects.requireNonNull(embeddingService, "embeddingService must not be null");
        this.tenantId = tenantId;
        this.modelName = Objects.requireNonNull(modelName, "modelName must not be null");
        this.metrics = metrics != null ? metrics : EmbeddingMetrics.NOOP;
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        List<String> input = texts == null || texts.isEmpty() ? List.of("") : texts;
        long started = System.currentTimeMillis();
        try {
            EmbeddingRequest request = new EmbeddingRequest(input, modelName, null, true);
            EmbeddingResponse response = embeddingService.embedForTenant(tenantId, request).await().indefinitely();
            metrics.recordEmbeddingSuccess(modelName, input.size(), System.currentTimeMillis() - started);
            return response.embeddings();
        } catch (RuntimeException ex) {
            metrics.recordEmbeddingFailure(modelName);
            throw ex;
        }
    }

    @Override
    public int dimension() {
        EmbeddingResponse probe = embeddingService.embedForTenant(
                tenantId,
                new EmbeddingRequest(List.of("dimension-probe"), modelName, null, true))
                .await().indefinitely();
        return probe.dimension();
    }

    @Override
    public String modelName() {
        return modelName;
    }
}

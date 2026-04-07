package tech.kayys.wayang.embedding.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.error.ErrorInfo;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.sdk.executor.core.AbstractWorkflowExecutor;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.wayang.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.embedding.node.EmbeddingNodeTypes;

import java.util.List;
import java.util.Map;

/**
 * Node executor for generating vector embeddings.
 */
@ApplicationScoped
@Executor(executorType = EmbeddingNodeTypes.EMBEDDING_GENERATE, supportedNodeTypes = {
        EmbeddingNodeTypes.EMBEDDING_GENERATE }, description = "Generates text embeddings using specified models")
public class EmbeddingNodeExecutor extends AbstractWorkflowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddingNodeExecutor.class);

    @Inject
    EmbeddingService embeddingService;

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Map<String, Object> config = task.context();

        // Configuration from node instance
        String model = config.get("model") != null ? (String) config.get("model") : "default-model";
        String provider = config.get("provider") != null ? (String) config.get("provider") : "default-provider";
        Boolean normalize = config.get("normalize") != null ? (Boolean) config.get("normalize") : false;

        // Input from task data
        Object input = config.get("text");
        if (input == null) {
            return Uni.createFrom().item(SimpleNodeExecutionResult.failure(
                    task.runId(), task.nodeId(), task.attempt(), null, task.token()));
        }

        List<String> inputs;
        if (input instanceof List) {
            inputs = (List<String>) input;
        } else {
            inputs = List.of(input.toString());
        }

        LOG.info("Generating embeddings for {} inputs using model: {}, provider: {}", inputs.size(), model, provider);

        EmbeddingRequest request = new EmbeddingRequest(inputs, model, provider, normalize);

        return embeddingService.embed(request)
                .map(response -> {
                    Map<String, Object> output = Map.of(
                            "embeddings", response.embeddings(),
                            "dimension", response.dimension(),
                            "model", response.model(),
                            "provider", response.provider());
                    return SimpleNodeExecutionResult.success(
                            task.runId(), task.nodeId(), task.attempt(), output, task.token(), java.time.Duration.ZERO);
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Embedding generation failed: {}", throwable.getMessage());
                    return SimpleNodeExecutionResult.failure(
                            task.runId(), task.nodeId(), task.attempt(), ErrorInfo.of(throwable), task.token());
                });
    }
}

package tech.kayys.wayang.vector.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.sdk.executor.core.AbstractWorkflowExecutor;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.wayang.schema.catalog.BuiltinSchemaCatalog;
import tech.kayys.wayang.schema.vector.VectorSearchConfig;
import tech.kayys.wayang.vector.VectorStore;
import tech.kayys.wayang.vector.VectorQuery;
import tech.kayys.gamelan.engine.error.ErrorInfo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import io.smallrye.mutiny.Uni;
import java.time.Duration;

@ApplicationScoped
@Executor(
    executorType = BuiltinSchemaCatalog.VECTOR_SEARCH,
    supportedNodeTypes = { BuiltinSchemaCatalog.VECTOR_SEARCH },
    description = "Searches for vectors in a vector database"
)
public class VectorSearchExecutor extends AbstractWorkflowExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    VectorStoreProvider vectorStoreProvider;

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Map<String, Object> context = task.context();
        VectorSearchConfig config = MAPPER.convertValue(context, VectorSearchConfig.class);

        VectorStore store = vectorStoreProvider.getVectorStore();

        Object rawInput = context.get("input");
        if (rawInput == null) rawInput = context.get("vector"); // Usually we pass the vector here
        
        java.util.List<Float> queryVector = new java.util.ArrayList<>();
        if (rawInput instanceof java.util.List) {
            for (Object num : (java.util.List<?>) rawInput) {
                if (num instanceof Number) {
                    queryVector.add(((Number) num).floatValue());
                } else if (num instanceof String) {
                    queryVector.add(Float.parseFloat((String) num));
                }
            }
        }

        VectorQuery query = new VectorQuery(queryVector, config.getTopK(), 0.0f);
        
        return store.search(query, config.getFilters())
            .map(results -> (NodeExecutionResult) SimpleNodeExecutionResult.success(
                task.runId(), task.nodeId(), task.attempt(), Map.of("results", results), task.token(), Duration.ZERO))
            .onFailure().recoverWithItem(throwable -> SimpleNodeExecutionResult.failure(
                task.runId(), task.nodeId(), task.attempt(), ErrorInfo.of(throwable), task.token()));
    }
}

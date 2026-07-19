package tech.kayys.wayang.vector.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.sdk.executor.core.AbstractWorkflowExecutor;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.wayang.schema.catalog.BuiltinSchemaCatalog;
import tech.kayys.wayang.schema.vector.VectorUpsertConfig;
import tech.kayys.wayang.vector.VectorStore;
import tech.kayys.wayang.vector.VectorEntry;
import tech.kayys.gamelan.engine.error.ErrorInfo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import io.smallrye.mutiny.Uni;
import java.time.Duration;

@ApplicationScoped
@Executor(
    executorType = BuiltinSchemaCatalog.VECTOR_UPSERT,
    supportedNodeTypes = { BuiltinSchemaCatalog.VECTOR_UPSERT },
    description = "Upserts vectors in a vector database"
)
public class VectorUpsertExecutor extends AbstractWorkflowExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    VectorStoreProvider vectorStoreProvider;

    @Override
    @SuppressWarnings("unchecked")
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Map<String, Object> context = task.context();
        VectorUpsertConfig config = MAPPER.convertValue(context, VectorUpsertConfig.class);

        VectorStore store = vectorStoreProvider.getVectorStore();

        // Extract input. We expect a List of VectorEntry records or mapped Maps.
        Object input = context.get("input");
        List<VectorEntry> entries = new ArrayList<>();
        
        if (input instanceof List) {
            for (Object item : (List<?>) input) {
                if (item instanceof VectorEntry) {
                    entries.add((VectorEntry) item);
                } else if (item instanceof Map) {
                    entries.add(MAPPER.convertValue(item, VectorEntry.class));
                }
            }
        }

        if (entries.isEmpty()) {
            return Uni.createFrom().item(SimpleNodeExecutionResult.failure(
                task.runId(), task.nodeId(), task.attempt(), ErrorInfo.of(new IllegalArgumentException("No valid VectorEntries found in input.")), task.token()));
        }

        return store.store(entries)
            .replaceWith(() -> (NodeExecutionResult) SimpleNodeExecutionResult.success(
                task.runId(), task.nodeId(), task.attempt(), 
                Map.of(
                    "status", "success", 
                    "inserted", entries.size(),
                    "collection", config.getCollectionName() != null ? config.getCollectionName() : "default"
                ), 
                task.token(), 
                Duration.ZERO))
            .onFailure().recoverWithItem(throwable -> SimpleNodeExecutionResult.failure(
                task.runId(), task.nodeId(), task.attempt(), ErrorInfo.of(throwable), task.token()));
    }
}

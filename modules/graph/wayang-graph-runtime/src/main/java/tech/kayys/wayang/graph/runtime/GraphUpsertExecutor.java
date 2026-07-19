package tech.kayys.wayang.graph.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.gamelan.engine.error.ErrorInfo;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.sdk.executor.core.AbstractWorkflowExecutor;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.wayang.graph.GraphStore;
import tech.kayys.wayang.graph.Node;
import tech.kayys.wayang.graph.Relationship;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Workflow executor that creates or updates nodes and relationships in the graph store.
 */
@ApplicationScoped
@Executor(
    executorType = "graph-upsert-node",
    supportedNodeTypes = { "graph-upsert-node" },
    description = "Creates or updates nodes and relationships in a graph database"
)
public class GraphUpsertExecutor extends AbstractWorkflowExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    GraphStoreProvider graphStoreProvider;

    @Override
    @SuppressWarnings("unchecked")
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Map<String, Object> context = task.context();
        GraphStore store = graphStoreProvider.getGraphStore();

        return Uni.createFrom().item(() -> {
            List<String> nodeIds = new ArrayList<>();
            List<String> relIds = new ArrayList<>();

            // Process nodes
            Object nodesInput = context.get("nodes");
            if (nodesInput instanceof List<?> nodeList) {
                for (Object item : nodeList) {
                    Node node;
                    if (item instanceof Map<?, ?> map) {
                        node = mapToNode((Map<String, Object>) map);
                    } else {
                        node = MAPPER.convertValue(item, Node.class);
                    }
                    nodeIds.add(store.addNode(node));
                }
            }

            // Process a single node if "node" key is provided
            Object singleNode = context.get("node");
            if (singleNode instanceof Map<?, ?> map) {
                Node node = mapToNode((Map<String, Object>) map);
                nodeIds.add(store.addNode(node));
            }

            // Process relationships
            Object relsInput = context.get("relationships");
            if (relsInput instanceof List<?> relList) {
                for (Object item : relList) {
                    Relationship rel;
                    if (item instanceof Map<?, ?> map) {
                        rel = mapToRelationship((Map<String, Object>) map);
                    } else {
                        rel = MAPPER.convertValue(item, Relationship.class);
                    }
                    relIds.add(store.addRelationship(rel));
                }
            }

            // Process a single relationship
            Object singleRel = context.get("relationship");
            if (singleRel instanceof Map<?, ?> map) {
                Relationship rel = mapToRelationship((Map<String, Object>) map);
                relIds.add(store.addRelationship(rel));
            }

            return (NodeExecutionResult) SimpleNodeExecutionResult.success(
                    task.runId(), task.nodeId(), task.attempt(),
                    Map.of(
                            "status", "success",
                            "nodesCreated", nodeIds.size(),
                            "nodeIds", nodeIds,
                            "relationshipsCreated", relIds.size(),
                            "relationshipIds", relIds
                    ),
                    task.token(), Duration.ZERO);
        }).onFailure().recoverWithItem(throwable ->
                SimpleNodeExecutionResult.failure(
                        task.runId(), task.nodeId(), task.attempt(),
                        ErrorInfo.of(throwable), task.token()));
    }

    @SuppressWarnings("unchecked")
    private Node mapToNode(Map<String, Object> map) {
        Node.Builder builder = Node.builder();
        if (map.containsKey("id")) builder.id(map.get("id").toString());
        if (map.containsKey("label")) builder.label(map.get("label").toString());
        if (map.containsKey("properties") && map.get("properties") instanceof Map<?, ?> props) {
            builder.properties((Map<String, Object>) props);
        }
        if (map.containsKey("metadata") && map.get("metadata") instanceof Map<?, ?> meta) {
            ((Map<String, Object>) meta).forEach(builder::metadata);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Relationship mapToRelationship(Map<String, Object> map) {
        Relationship.Builder builder = Relationship.builder();
        if (map.containsKey("id")) builder.id(map.get("id").toString());
        if (map.containsKey("startNodeId")) builder.startNodeId(map.get("startNodeId").toString());
        if (map.containsKey("endNodeId")) builder.endNodeId(map.get("endNodeId").toString());
        if (map.containsKey("type")) builder.type(map.get("type").toString());
        if (map.containsKey("properties") && map.get("properties") instanceof Map<?, ?> props) {
            ((Map<String, Object>) props).forEach(builder::property);
        }
        return builder.build();
    }
}

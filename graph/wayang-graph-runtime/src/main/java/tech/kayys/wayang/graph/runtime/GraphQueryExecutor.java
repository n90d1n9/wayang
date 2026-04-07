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

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Workflow executor that queries the graph store.
 * Supports querying nodes by label, by property, finding paths,
 * and executing raw Cypher queries (for Neo4j).
 */
@ApplicationScoped
@Executor(executorType = "graph-query-node", supportedNodeTypes = {
        "graph-query-node" }, description = "Queries a graph database for nodes, relationships or paths")
public class GraphQueryExecutor extends AbstractWorkflowExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    GraphStoreProvider graphStoreProvider;

    @Override
    @SuppressWarnings("unchecked")
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Map<String, Object> context = task.context();
        GraphStore store = graphStoreProvider.getGraphStore();

        String queryType = stringOrDefault(context.get("queryType"), "label");
        String label = stringOrDefault(context.get("label"), "");
        String propertyKey = stringOrDefault(context.get("propertyKey"), "");
        Object propertyValue = context.get("propertyValue");
        String cypherQuery = stringOrDefault(context.get("cypher"), "");
        String startNodeId = stringOrDefault(context.get("startNodeId"), "");
        String endNodeId = stringOrDefault(context.get("endNodeId"), "");
        int maxDepth = intOrDefault(context.get("maxDepth"), 3);

        return Uni.createFrom().item(() -> {
            Object results = switch (queryType.toLowerCase()) {
                case "label" -> store.findNodesByLabel(label).stream()
                        .map(this::nodeToMap).collect(Collectors.toList());

                case "property" -> store.findNodesByProperty(label, propertyKey, propertyValue)
                        .stream().map(this::nodeToMap).collect(Collectors.toList());

                case "cypher" -> store.executeCypher(cypherQuery,
                        context.containsKey("parameters")
                                ? (Map<String, Object>) context.get("parameters")
                                : Map.of());

                case "path" -> store.findPaths(startNodeId, endNodeId, maxDepth).stream()
                        .map(path -> path.stream().map(this::nodeToMap)
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList());

                case "node" -> {
                    String nodeId = stringOrDefault(context.get("nodeId"), "");
                    yield store.getNode(nodeId).map(this::nodeToMap).orElse(Map.of());
                }

                case "relationships" -> {
                    String nodeId = stringOrDefault(context.get("nodeId"), "");
                    String dirStr = stringOrDefault(context.get("direction"), "BOTH");
                    GraphStore.Direction dir = GraphStore.Direction.valueOf(dirStr.toUpperCase());
                    yield store.getRelationships(nodeId, dir).stream()
                            .map(r -> Map.of(
                                    "id", (Object) r.getId(),
                                    "type", r.getType(),
                                    "startNodeId", r.getStartNodeId(),
                                    "endNodeId", r.getEndNodeId(),
                                    "properties", r.getProperties()))
                            .collect(Collectors.toList());
                }

                default -> throw new IllegalArgumentException("Unknown queryType: " + queryType);
            };

            return (NodeExecutionResult) SimpleNodeExecutionResult.success(
                    task.runId(), task.nodeId(), task.attempt(),
                    Map.of("results", results), task.token(), Duration.ZERO);
        }).onFailure().recoverWithItem(throwable -> SimpleNodeExecutionResult.failure(
                task.runId(), task.nodeId(), task.attempt(),
                ErrorInfo.of(throwable), task.token()));
    }

    private Map<String, Object> nodeToMap(Node node) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", node.getId());
        map.put("label", node.getLabel());
        map.putAll(node.getProperties());
        return map;
    }

    private String stringOrDefault(Object val, String defaultVal) {
        return val instanceof String s ? s : defaultVal;
    }

    private int intOrDefault(Object val, int defaultVal) {
        if (val instanceof Number n)
            return n.intValue();
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }
}

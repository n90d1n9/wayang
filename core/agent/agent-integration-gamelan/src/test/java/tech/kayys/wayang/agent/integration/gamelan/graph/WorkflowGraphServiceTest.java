package tech.kayys.wayang.agent.integration.gamelan.graph;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import tech.kayys.gamelan.engine.run.RunResponse;
import tech.kayys.wayang.graph.GraphStatistics;
import tech.kayys.wayang.graph.GraphStore;
import tech.kayys.wayang.graph.Node;
import tech.kayys.wayang.graph.Relationship;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowGraphServiceTest {

    @Test
    void storesWorkflowDefinitionUsingActiveGraphContracts() {
        FakeGraphStore graphStore = new FakeGraphStore();
        WorkflowGraphService service = service(graphStore);

        String workflowNodeId = service.storeWorkflowDefinition(
                        "workflow.alpha",
                        "Alpha Workflow",
                        List.of(
                                new WorkflowStepDefinition("step-1", "First", 1, "task"),
                                new WorkflowStepDefinition("step-2", "Second", 2, "task")))
                .await().indefinitely();

        assertThat(graphStore.nodes.get(workflowNodeId).getProperty("workflowId")).isEqualTo("workflow.alpha");
        assertThat(graphStore.findNodesByLabel("WorkflowStep"))
                .extracting(node -> node.getProperty("stepId"))
                .containsExactlyInAnyOrder("step-1", "step-2");
        assertThat(graphStore.relationships)
                .extracting(Relationship::getType)
                .containsExactlyInAnyOrder("HAS_STEP", "HAS_STEP", "FOLLOWS");
    }

    @Test
    void recordsExecutionRunUsingCurrentGamelanRunFields() {
        FakeGraphStore graphStore = new FakeGraphStore();
        WorkflowGraphService service = service(graphStore);
        graphStore.addNode(Node.builder()
                .label("Workflow")
                .property("workflowId", "workflow.alpha")
                .build());
        Instant startedAt = Instant.parse("2026-05-26T01:02:03Z");
        RunResponse response = RunResponse.builder()
                .runId("run-1")
                .workflowId("workflow.alpha")
                .status("COMPLETED")
                .startedAt(startedAt)
                .durationMs(42L)
                .nodesExecuted(2)
                .nodesTotal(3)
                .build();

        String runNodeId = service.recordExecutionRun("agent-a", response).await().indefinitely();

        Node runNode = graphStore.nodes.get(runNodeId);
        assertThat(runNode.getProperty("runId")).isEqualTo("run-1");
        assertThat(runNode.getProperty("agentId")).isEqualTo("agent-a");
        assertThat(runNode.getProperty("startTime")).isEqualTo(startedAt.toString());
        assertThat(runNode.getProperty("nodesExecuted")).isEqualTo(2);
        assertThat(runNode.getProperty("nodesTotal")).isEqualTo(3);
        assertThat(graphStore.relationships)
                .extracting(Relationship::getType)
                .contains("EXECUTED_RUN");
    }

    private WorkflowGraphService service(FakeGraphStore graphStore) {
        WorkflowGraphService service = new WorkflowGraphService();
        service.logger = Logger.getLogger(WorkflowGraphService.class);
        service.graphStore = graphStore;
        return service;
    }

    private static final class FakeGraphStore implements GraphStore {
        private final Map<String, Node> nodes = new LinkedHashMap<>();
        private final List<Relationship> relationships = new ArrayList<>();

        @Override
        public void initialize() {
        }

        @Override
        public void close() {
        }

        @Override
        public String addNode(Node node) {
            if (node.getId() == null || node.getId().isBlank()) {
                node.setId(UUID.randomUUID().toString());
            }
            nodes.put(node.getId(), node);
            return node.getId();
        }

        @Override
        public List<String> addNodes(List<Node> nodes) {
            return nodes.stream().map(this::addNode).toList();
        }

        @Override
        public Optional<Node> getNode(String nodeId) {
            return Optional.ofNullable(nodes.get(nodeId));
        }

        @Override
        public List<Node> findNodesByLabel(String label) {
            return nodes.values().stream()
                    .filter(node -> label.equals(node.getLabel()))
                    .toList();
        }

        @Override
        public List<Node> findNodesByProperty(String label, String property, Object value) {
            return findNodesByLabel(label).stream()
                    .filter(node -> value.equals(node.getProperty(property)))
                    .toList();
        }

        @Override
        public boolean updateNode(String nodeId, Node node) {
            nodes.put(nodeId, node);
            return true;
        }

        @Override
        public boolean deleteNode(String nodeId) {
            return nodes.remove(nodeId) != null;
        }

        @Override
        public String addRelationship(Relationship relationship) {
            if (relationship.getId() == null || relationship.getId().isBlank()) {
                relationship.setId(UUID.randomUUID().toString());
            }
            relationships.add(relationship);
            return relationship.getId();
        }

        @Override
        public List<String> addRelationships(List<Relationship> relationships) {
            return relationships.stream().map(this::addRelationship).toList();
        }

        @Override
        public List<Relationship> getRelationships(String nodeId, Direction direction) {
            return relationships.stream()
                    .filter(relationship -> switch (direction) {
                        case INCOMING -> nodeId.equals(relationship.getEndNodeId());
                        case OUTGOING -> nodeId.equals(relationship.getStartNodeId());
                        case BOTH -> nodeId.equals(relationship.getStartNodeId())
                                || nodeId.equals(relationship.getEndNodeId());
                    })
                    .toList();
        }

        @Override
        public List<Relationship> findRelationshipsByType(String type) {
            return relationships.stream()
                    .filter(relationship -> type.equals(relationship.getType()))
                    .toList();
        }

        @Override
        public boolean deleteRelationship(String relationshipId) {
            return relationships.removeIf(relationship -> relationshipId.equals(relationship.getId()));
        }

        @Override
        public List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters) {
            return List.of();
        }

        @Override
        public List<List<Node>> findPaths(String startNodeId, String endNodeId, int maxDepth) {
            return List.of();
        }

        @Override
        public GraphStatistics getStatistics() {
            return new GraphStatistics();
        }
    }
}

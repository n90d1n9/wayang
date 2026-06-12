package tech.kayys.wayang.memory;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.gamelan.engine.execution.ExecutionToken;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.node.NodeId;
import tech.kayys.gamelan.engine.run.RetryPolicy;
import tech.kayys.gamelan.engine.workflow.WorkflowRunId;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.model.MemoryType;
import tech.kayys.wayang.memory.model.ScoredMemory;
import tech.kayys.wayang.memory.node.MemoryNodeTypes;
import tech.kayys.wayang.memory.service.MemoryStatistics;
import tech.kayys.wayang.memory.service.VectorMemoryStore;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticMemoryExecutorTest {

    @Test
    void contextGroupsEntriesByCategory() {
        FakeVectorMemoryStore store = new FakeVectorMemoryStore();
        store.searchResults = List.of(new ScoredMemory(Memory.builder()
                .id("memory-1")
                .content("Agents can compose skills through typed node contracts")
                .type(MemoryType.SEMANTIC)
                .timestamp(Instant.parse("2026-01-02T03:04:05Z"))
                .metadata(Map.of(
                        "category", "architecture",
                        "concepts", List.of("agent", "skill")))
                .build(), 0.91));
        SemanticMemoryExecutor executor = executor(store);

        var result = executor.execute(task(Map.of(
                "operation", "CONTEXT",
                "agentId", "agent-a",
                "category", "architecture")))
                .await().indefinitely();

        assertThat(result.output())
                .containsEntry("success", true)
                .containsEntry("memoryType", "semantic")
                .containsEntry("count", 1);
        assertThat(groupedByCategory(result.output())).containsKey("architecture");
        assertThat(store.lastFilters)
                .containsEntry("agentId", "agent-a")
                .containsEntry("memoryType", "semantic")
                .containsEntry("category", "architecture");
    }

    @Test
    void exposesSemanticMemoryNodeDefinition() {
        SemanticNodeProvider provider = new SemanticNodeProvider();

        assertThat(provider.nodes()).hasSize(1);
        assertThat(provider.nodes().getFirst().type()).isEqualTo(MemoryNodeTypes.SEMANTIC);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<Map<String, Object>>> groupedByCategory(Map<String, Object> output) {
        return (Map<String, List<Map<String, Object>>>) output.get("groupedByCategory");
    }

    private SemanticMemoryExecutor executor(FakeVectorMemoryStore store) {
        SemanticMemoryExecutor executor = new SemanticMemoryExecutor();
        executor.defaultCategory = "general";
        executor.defaultSearchLimit = 15;
        executor.enableConceptLinking = true;
        executor.vectorMemoryStore = store;
        executor.embeddingService = text -> Uni.createFrom().item(List.of(1.0f, 2.0f, 3.0f));
        return executor;
    }

    private NodeExecutionTask task(Map<String, Object> context) {
        WorkflowRunId runId = WorkflowRunId.generate();
        NodeId nodeId = NodeId.of("semantic-memory-node");
        return new NodeExecutionTask(
                runId,
                nodeId,
                1,
                ExecutionToken.create(runId, nodeId, 1, Duration.ofMinutes(1)),
                context,
                RetryPolicy.none());
    }

    private static final class FakeVectorMemoryStore implements VectorMemoryStore {
        private final List<Memory> stored = new ArrayList<>();
        private List<ScoredMemory> searchResults = List.of();
        private Map<String, Object> lastFilters = Map.of();

        @Override
        public Uni<String> store(Memory memory) {
            stored.add(memory);
            return Uni.createFrom().item(memory.getId());
        }

        @Override
        public Uni<List<String>> storeBatch(List<Memory> memories) {
            stored.addAll(memories);
            return Uni.createFrom().item(memories.stream().map(Memory::getId).toList());
        }

        @Override
        public Uni<List<ScoredMemory>> search(
                float[] queryEmbedding,
                int limit,
                double minSimilarity,
                Map<String, Object> filters) {
            lastFilters = Map.copyOf(filters);
            return Uni.createFrom().item(searchResults);
        }

        @Override
        public Uni<List<ScoredMemory>> hybridSearch(
                float[] queryEmbedding,
                List<String> keywords,
                int limit,
                double semanticWeight) {
            return Uni.createFrom().item(searchResults);
        }

        @Override
        public Uni<Memory> retrieve(String memoryId) {
            return Uni.createFrom().nullItem();
        }

        @Override
        public Uni<List<Memory>> retrieveBatch(List<String> memoryIds) {
            return Uni.createFrom().item(List.of());
        }

        @Override
        public Uni<Memory> updateMetadata(String memoryId, Map<String, Object> metadata) {
            return Uni.createFrom().nullItem();
        }

        @Override
        public Uni<Boolean> delete(String memoryId) {
            return Uni.createFrom().item(false);
        }

        @Override
        public Uni<Long> deleteNamespace(String namespace) {
            return Uni.createFrom().item(0L);
        }

        @Override
        public Uni<MemoryStatistics> getStatistics(String namespace) {
            return Uni.createFrom().item(new MemoryStatistics(namespace, 0, 0, 0, 0, 0, 0.0, null, null));
        }

        @Override
        public Uni<List<Memory>> searchByFilter(Map<String, Object> filters) {
            return Uni.createFrom().item(List.of());
        }
    }
}

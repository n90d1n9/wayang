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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LongTermMemoryExecutorTest {

    @Test
    void storesLongTermMemoryAsSemanticVectorRecord() {
        FakeVectorMemoryStore store = new FakeVectorMemoryStore();
        LongTermMemoryExecutor executor = executor(store);

        var result = executor.execute(task(Map.of(
                "operation", "STORE",
                "agentId", "agent-a",
                "content", "Use typed memory nodes for persistent cross-session knowledge",
                "importance", 0.9)))
                .await().indefinitely();

        Memory memory = store.stored.getFirst();
        assertThat(result.output())
                .containsEntry("success", true)
                .containsEntry("memoryType", "longterm")
                .containsEntry("memoryId", memory.getId())
                .containsEntry("importance", 0.9);
        assertThat(memory.getNamespace()).isEqualTo("agent-a");
        assertThat(memory.getType()).isEqualTo(MemoryType.SEMANTIC);
        assertThat(memory.getMetadata())
                .containsEntry("memoryType", "longterm")
                .containsEntry("agentId", "agent-a")
                .containsEntry("importance", 0.9);
    }

    @Test
    void exposesLongTermMemoryNodeDefinition() {
        LongTermNodeProvider provider = new LongTermNodeProvider();

        assertThat(provider.nodes()).hasSize(1);
        assertThat(provider.nodes().getFirst().type()).isEqualTo(MemoryNodeTypes.LONG_TERM);
    }

    private LongTermMemoryExecutor executor(FakeVectorMemoryStore store) {
        LongTermMemoryExecutor executor = new LongTermMemoryExecutor();
        executor.defaultImportanceThreshold = 0.5;
        executor.decayRate = 0.01;
        executor.defaultSearchLimit = 10;
        executor.defaultMinSimilarity = 0.7f;
        executor.vectorMemoryStore = store;
        executor.embeddingService = text -> Uni.createFrom().item(List.of(1.0f, 2.0f, 3.0f));
        return executor;
    }

    private NodeExecutionTask task(Map<String, Object> context) {
        WorkflowRunId runId = WorkflowRunId.generate();
        NodeId nodeId = NodeId.of("longterm-memory-node");
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

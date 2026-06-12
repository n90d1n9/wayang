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

class EpisodicMemoryExecutorTest {

    @Test
    void storesEventMetadataAndNamespace() {
        FakeVectorMemoryStore store = new FakeVectorMemoryStore();
        EpisodicMemoryExecutor executor = executor(store);

        var result = executor.execute(task(Map.of(
                "operation", "STORE",
                "agentId", "agent-a",
                "content", "kept the release decision with the ops team",
                "eventType", "meeting",
                "participants", List.of("ops", "ai"),
                "location", "war-room",
                "emotionalValence", 0.4)))
                .await().indefinitely();

        Memory memory = store.stored.getFirst();
        assertThat(result.output())
                .containsEntry("success", true)
                .containsEntry("memoryType", "episodic")
                .containsEntry("memoryId", memory.getId())
                .containsEntry("eventType", "meeting");
        assertThat(memory.getType()).isEqualTo(MemoryType.EPISODIC);
        assertThat(memory.getNamespace()).isEqualTo("agent-a:episodic:meeting");
        assertThat(memory.getMetadata())
                .containsEntry("memoryType", "episodic")
                .containsEntry("agentId", "agent-a")
                .containsEntry("eventType", "meeting")
                .containsEntry("location", "war-room");
    }

    @Test
    void searchWithoutQueryKeepsStableOutputShape() {
        EpisodicMemoryExecutor executor = executor(new FakeVectorMemoryStore());

        var result = executor.execute(task(Map.of(
                "operation", "SEARCH",
                "agentId", "agent-a",
                "eventType", "general")))
                .await().indefinitely();

        assertThat(result.output())
                .containsEntry("success", true)
                .containsEntry("query", "")
                .containsEntry("count", 0);
    }

    @Test
    void exposesEpisodicMemoryNodeDefinition() {
        EpisodicNodeProvider provider = new EpisodicNodeProvider();

        assertThat(provider.nodes()).hasSize(1);
        assertThat(provider.nodes().getFirst().type()).isEqualTo(MemoryNodeTypes.EPISODIC);
    }

    private EpisodicMemoryExecutor executor(FakeVectorMemoryStore store) {
        EpisodicMemoryExecutor executor = new EpisodicMemoryExecutor();
        executor.defaultEventType = "general";
        executor.defaultSearchLimit = 20;
        executor.enableTemporalOrdering = true;
        executor.relatedTimeWindowHours = 24;
        executor.vectorMemoryStore = store;
        executor.embeddingService = text -> Uni.createFrom().item(List.of(1.0f, 2.0f, 3.0f));
        return executor;
    }

    private NodeExecutionTask task(Map<String, Object> context) {
        WorkflowRunId runId = WorkflowRunId.generate();
        NodeId nodeId = NodeId.of("episodic-memory-node");
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

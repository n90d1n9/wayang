package tech.kayys.wayang.memory.api;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.memory.dto.SearchRequest;
import tech.kayys.wayang.memory.dto.StoreRequest;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.model.MemoryType;
import tech.kayys.wayang.memory.model.ScoredMemory;
import tech.kayys.wayang.memory.service.ContextEngineeringService;
import tech.kayys.wayang.memory.service.EmbeddingServiceFactory;
import tech.kayys.wayang.memory.service.MemoryStatistics;
import tech.kayys.wayang.memory.service.VectorMemoryStore;
import tech.kayys.wayang.memory.spi.EmbeddingService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryResourceTest {

    @Test
    void storesMemoryUsingConfiguredEmbeddingService() {
        FakeVectorMemoryStore store = new FakeVectorMemoryStore();
        MemoryResource resource = resource(store);

        var response = resource.storeMemory(new StoreRequest(
                "team-a",
                "remember release constraints",
                MemoryType.SEMANTIC,
                0.8,
                Map.of("source", "test")))
                .await().indefinitely();

        assertThat(response.success()).isTrue();
        assertThat(response.memoryId()).isEqualTo("memory-1");
        assertThat(store.stored).hasSize(1);
        assertThat(store.stored.getFirst().getNamespace()).isEqualTo("team-a");
        assertThat(store.stored.getFirst().getEmbedding()).containsExactly(1.0f, 2.0f, 3.0f);
    }

    @Test
    void searchesMemoriesWithDefaultQueryOptions() {
        FakeVectorMemoryStore store = new FakeVectorMemoryStore();
        store.searchResults = List.of(new ScoredMemory(Memory.builder()
                .id("memory-1")
                .content("release constraints")
                .type(MemoryType.EPISODIC)
                .importance(0.7)
                .build(), 0.91));
        MemoryResource resource = resource(store);

        var response = resource.search(new SearchRequest("team-a", "release", null, null))
                .await().indefinitely();

        assertThat(response.success()).isTrue();
        assertThat(response.count()).isEqualTo(1);
        assertThat(response.results().getFirst().id()).isEqualTo("memory-1");
        assertThat(store.lastLimit).isEqualTo(10);
        assertThat(store.lastMinSimilarity).isEqualTo(0.5);
        assertThat(store.lastFilters).containsEntry("namespace", "team-a");
    }

    @Test
    void mapsStorageStatisticsToRuntimeResponse() {
        FakeVectorMemoryStore store = new FakeVectorMemoryStore();
        store.statistics = new MemoryStatistics(
                "team-a",
                7,
                2,
                3,
                1,
                1,
                0.64,
                Instant.now(),
                Instant.now());
        MemoryResource resource = resource(store);

        var response = resource.getStatistics("team-a").await().indefinitely();

        assertThat(response.success()).isTrue();
        assertThat(response.totalMemories()).isEqualTo(7);
        assertThat(response.semanticCount()).isEqualTo(3);
        assertThat(response.avgImportance()).isEqualTo(0.64);
    }

    private MemoryResource resource(FakeVectorMemoryStore store) {
        MemoryResource resource = new MemoryResource();
        resource.memoryStore = store;
        resource.embeddingFactory = new FixedEmbeddingServiceFactory();
        resource.contextService = null;
        return resource;
    }

    private static final class FixedEmbeddingServiceFactory extends EmbeddingServiceFactory {
        private final EmbeddingService embeddingService = text -> Uni.createFrom().item(List.of(1.0f, 2.0f, 3.0f));

        @Override
        public EmbeddingService getEmbeddingService() {
            return embeddingService;
        }
    }

    private static final class FakeVectorMemoryStore implements VectorMemoryStore {
        private final List<Memory> stored = new ArrayList<>();
        private List<ScoredMemory> searchResults = List.of();
        private MemoryStatistics statistics = new MemoryStatistics(
                "default",
                0,
                0,
                0,
                0,
                0,
                0.0,
                null,
                null);
        private int lastLimit;
        private double lastMinSimilarity;
        private Map<String, Object> lastFilters = Map.of();

        @Override
        public Uni<String> store(Memory memory) {
            stored.add(memory);
            return Uni.createFrom().item("memory-" + stored.size());
        }

        @Override
        public Uni<List<String>> storeBatch(List<Memory> memories) {
            memories.forEach(stored::add);
            return Uni.createFrom().item(stored.stream().map(Memory::getId).toList());
        }

        @Override
        public Uni<List<ScoredMemory>> search(
                float[] queryEmbedding,
                int limit,
                double minSimilarity,
                Map<String, Object> filters) {
            lastLimit = limit;
            lastMinSimilarity = minSimilarity;
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
            return Uni.createFrom().item(() -> stored.stream()
                    .filter(memory -> memoryId.equals(memory.getId()))
                    .findFirst()
                    .orElse(null));
        }

        @Override
        public Uni<List<Memory>> retrieveBatch(List<String> memoryIds) {
            return Uni.createFrom().item(stored.stream()
                    .filter(memory -> memoryIds.contains(memory.getId()))
                    .toList());
        }

        @Override
        public Uni<Memory> updateMetadata(String memoryId, Map<String, Object> metadata) {
            return retrieve(memoryId);
        }

        @Override
        public Uni<Boolean> delete(String memoryId) {
            return Uni.createFrom().item(stored.removeIf(memory -> memoryId.equals(memory.getId())));
        }

        @Override
        public Uni<Long> deleteNamespace(String namespace) {
            long before = stored.size();
            stored.removeIf(memory -> namespace.equals(memory.getNamespace()));
            return Uni.createFrom().item(before - stored.size());
        }

        @Override
        public Uni<MemoryStatistics> getStatistics(String namespace) {
            return Uni.createFrom().item(statistics);
        }

        @Override
        public Uni<List<Memory>> searchByFilter(Map<String, Object> filters) {
            return Uni.createFrom().item(stored);
        }
    }
}

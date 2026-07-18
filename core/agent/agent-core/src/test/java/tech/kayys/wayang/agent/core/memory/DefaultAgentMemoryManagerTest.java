package tech.kayys.wayang.agent.core.memory;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.embedding.EmbeddingResponse;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.model.ScoredMemory;
import tech.kayys.wayang.memory.service.VectorMemoryStore;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;

@ExtendWith(MockitoExtension.class)
class DefaultAgentMemoryManagerTest {

    @InjectMocks
    DefaultAgentMemoryManager memoryManager;

    @Mock
    VectorMemoryStore vectorStore;

    @Mock
    EmbeddingService embeddingService;

    @Test
    void testStoreMemory() {
        Mockito.when(embeddingService.embed(any()))
                .thenReturn(Uni.createFrom().item(new EmbeddingResponse(List.of(new float[512]), 512, "tfidf", "tfidf-512")));
        Mockito.when(vectorStore.store(any(Memory.class))).thenReturn(Uni.createFrom().item("mem-123"));

        String id = memoryManager.storeMemory("agent-1", "some content", Map.of("role", "user"))
                .await().indefinitely();

        Assertions.assertEquals("mem-123", id);
        Mockito.verify(vectorStore).store(any(Memory.class));
    }

    @Test
    void testRetrieveContext() {
        Memory mem1 = Memory.builder()
                .id("1")
                .content("history 1")
                .metadata(Map.of())
                .build();
        ScoredMemory scored1 = new ScoredMemory(mem1, 0.9);

        Mockito.when(embeddingService.embed(any()))
                .thenReturn(Uni.createFrom().item(new EmbeddingResponse(List.of(new float[512]), 512, "tfidf", "tfidf-512")));
        Mockito.when(vectorStore.search(any(), anyInt(), anyDouble(), anyMap()))
                .thenReturn(Uni.createFrom().item(List.of(scored1)));

        String context = memoryManager.retrieveContext("agent-1", "query", 5)
                .await().indefinitely();

        Assertions.assertNotNull(context);
        Assertions.assertTrue(context.contains("history 1"));
    }
}

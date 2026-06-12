package tech.kayys.gamelan.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpisodicIndexerTest {

    @Mock EmbeddingService embeddings;
    private EpisodicIndexer indexer;

    @BeforeEach
    void setUp(@TempDir Path tmp) throws Exception {
        indexer = new EpisodicIndexer();
        Field ef = EpisodicIndexer.class.getDeclaredField("embeddings");
        ef.setAccessible(true);
        ef.set(indexer, embeddings);
        // Init with temp dir via reflection
        Field sf = EpisodicIndexer.class.getDeclaredField("episodicStore");
        sf.setAccessible(true);
        sf.set(indexer, new VectorStore(tmp.resolve("ep.vec"), "test"));
    }

    private MemoryHierarchy.Episode ep(String task, boolean success) {
        return new MemoryHierarchy.Episode(
                java.util.UUID.randomUUID().toString(),
                task, "outcome", List.of("read_file"), success, 2000, Instant.now());
    }

    @Test
    void indexedCountZeroInitially() {
        assertThat(indexer.indexedCount()).isEqualTo(0);
    }

    @Test
    void findSimilarEmptyWhenEmbeddingUnavailable() {
        when(embeddings.isAvailable()).thenReturn(false);
        assertThat(indexer.findSimilar("any task", 5)).isEmpty();
    }

    @Test
    void findSimilarEmptyWhenQueryEmbedFails() {
        when(embeddings.isAvailable()).thenReturn(true);
        when(embeddings.embed(anyString())).thenReturn(new float[0]);
        assertThat(indexer.findSimilar("task", 5)).isEmpty();
    }

    @Test
    void indexAsyncDoesNotBlockCaller() {
        when(embeddings.embed(anyString())).thenReturn(new float[0]); // no-op
        // Should return immediately even if embedding is slow
        long t0 = System.currentTimeMillis();
        indexer.indexAsync(ep("task", true));
        assertThat(System.currentTimeMillis() - t0).isLessThan(1000);
    }

    @Test
    void findSimilarSuccessesFiltersCorrectly() {
        when(embeddings.isAvailable()).thenReturn(true);
        when(embeddings.embed(anyString())).thenReturn(new float[]{1f, 0f});
        // Directly add entries to the store (bypassing async)
        invokeIndex(ep("success task", true));
        invokeIndex(ep("failure task", false));

        // Both get indexed; findSimilarSuccesses should return only the success
        List<EpisodicIndexer.EpisodicMatch> successes =
                indexer.findSimilarSuccesses("any", 5);
        successes.forEach(m -> assertThat(m.wasSuccess()).isTrue());
    }

    private void invokeIndex(MemoryHierarchy.Episode ep) {
        try {
            var m = EpisodicIndexer.class.getDeclaredMethod("index", MemoryHierarchy.Episode.class);
            m.setAccessible(true);
            m.invoke(indexer, ep);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

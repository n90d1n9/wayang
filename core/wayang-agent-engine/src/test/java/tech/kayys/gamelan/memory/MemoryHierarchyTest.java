package tech.kayys.gamelan.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.config.GamelanConfig;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the refactored {@link MemoryHierarchy} backed by vector stores.
 *
 * <p>All CDI injections are done via reflection so this runs without a
 * container. SemanticMemoryStore and EpisodicIndexer are mocked to isolate
 * MemoryHierarchy's coordination logic from the vector search internals
 * (which are tested in their own test classes).
 */
@ExtendWith(MockitoExtension.class)
class MemoryHierarchyTest {

    @Mock SemanticMemoryStore    semanticStore;
    @Mock EpisodicIndexer        episodicIndexer;
    @Mock KnowledgeGraphExtractor knowledgeExtractor;
    @Mock EmbeddingService       embeddingService;
    @Mock GamelanConfig          config;

    private MemoryHierarchy memory;

    @BeforeEach
    void setUp(@TempDir Path tmp) throws Exception {
        memory = new MemoryHierarchy() {
            @Override protected Path defaultMemoryDir() { return tmp; }
        };
        inject("semanticStore",       semanticStore);
        inject("episodicIndexer",     episodicIndexer);
        inject("knowledgeExtractor",  knowledgeExtractor);
        inject("embeddingService",    embeddingService);
        inject("config",              config);
        // Manually trigger @PostConstruct
        memory.init();

        when(config.defaultModel()).thenReturn("test-model");
        when(embeddingService.isAvailable()).thenReturn(false);
        when(semanticStore.search(any(), anyInt(), any())).thenReturn(List.of());
        when(episodicIndexer.findSimilar(any(), anyInt())).thenReturn(List.of());
    }

    // ── Layer 2: Episodic ──────────────────────────────────────────────────

    @Test
    void recordEpisodeIncrementsCount() {
        memory.recordEpisode("task", "outcome", List.of(), true, 1000);
        assertThat(memory.episodeCount()).isEqualTo(1);
    }

    @Test
    void recordEpisodeTriggersAsyncIndexing() {
        memory.recordEpisode("task", "outcome", List.of(), true, 1000);
        verify(episodicIndexer, times(1)).indexAsync(any());
    }

    @Test
    void recordEpisodeTriggersKnowledgeExtractionForMeaningfulOutcomes() {
        memory.recordEpisode("task",
                "Used Maven 3.9 to build. Applied patch. Tests pass.", // >20 chars
                List.of(), true, 5000);
        verify(knowledgeExtractor, times(1)).extractAsync(any(), eq("test-model"));
    }

    @Test
    void recordEpisodeSkipsExtractionForBlankOutcome() {
        memory.recordEpisode("task", "", List.of(), true, 1000);
        verify(knowledgeExtractor, never()).extractAsync(any(), any());
    }

    @Test
    void recordEpisodeUsesConfigDefaultModel() {
        when(config.defaultModel()).thenReturn("qwen2-7b");
        memory.recordEpisode("t", "meaningful long outcome text here", List.of(), true, 100);
        verify(knowledgeExtractor).extractAsync(any(), eq("qwen2-7b"));
    }

    @Test
    void recentEpisodesNewestFirst() {
        memory.recordEpisode("task-a", "a", List.of(), true, 1000);
        memory.recordEpisode("task-b", "b", List.of(), true, 2000);
        assertThat(memory.recentEpisodes(5).get(0).task()).isEqualTo("task-b");
    }

    @Test
    void failedEpisodesFiltersCorrectly() {
        memory.recordEpisode("success", "ok", List.of(), true,  1000);
        memory.recordEpisode("failure", "err", List.of(), false, 1000);
        var failures = memory.failedEpisodes(10);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).task()).isEqualTo("failure");
    }

    @Test
    void successRateComputedCorrectly() {
        memory.recordEpisode("t1", "ok", List.of(), true,  100);
        memory.recordEpisode("t2", "ok", List.of(), true,  100);
        memory.recordEpisode("t3", "err",List.of(), false, 100);
        assertThat(memory.successRate()).isCloseTo(2.0/3.0, org.assertj.core.data.Offset.offset(0.01));
    }

    // ── Layer 3: Semantic ──────────────────────────────────────────────────

    @Test
    void learnFactDelegatesToSemanticStore() {
        memory.learnFact("build-tool", "Maven 3.9");
        verify(semanticStore).store(eq("build-tool"), eq("Maven 3.9"), eq("FACT"));
    }

    @Test
    void relevantFactsCallsSemanticSearch() {
        memory.relevantFacts("build the project", 5);
        verify(semanticStore).search(eq("build the project"), eq(5), any());
    }

    // ── Layer 4: Procedural ────────────────────────────────────────────────

    @Test
    void learnProcedureDelegatesToSemanticStore() {
        memory.learnProcedure("when debugging NPE", "1. Check logs 2. Add null guard", "ep-1");
        verify(semanticStore).store(
                eq("when debugging NPE"),
                contains("PROCEDURE"),
                eq("PROCEDURE"));
    }

    // ── Prompt block ───────────────────────────────────────────────────────

    @Test
    void buildPromptBlockEmptyWhenNothingRelevant() {
        assertThat(memory.buildPromptBlock("some task")).isEmpty();
    }

    @Test
    void buildPromptBlockIncludesSemanticFacts() {
        when(semanticStore.search(anyString(), anyInt(), any()))
                .thenReturn(List.of(new SemanticMemoryStore.SemanticFact(
                        "build-tool", "Maven", "FACT", "proj", 0.9f)));
        String block = memory.buildPromptBlock("build the project");
        assertThat(block).contains("Maven");
        assertThat(block).contains("FACT");
    }

    @Test
    void buildPromptBlockIncludesProcedures() {
        when(semanticStore.search(anyString(), anyInt(), any()))
                .thenReturn(List.of(new SemanticMemoryStore.SemanticFact(
                        "test-flow", "PROCEDURE: test-flow → run mvn test",
                        "PROCEDURE", "proj", 0.85f)));
        String block = memory.buildPromptBlock("run the tests");
        assertThat(block).contains("Effective Procedures");
    }

    @Test
    void buildPromptBlockEmptyForBlankTask() {
        assertThat(memory.buildPromptBlock("")).isEmpty();
        assertThat(memory.buildPromptBlock(null)).isEmpty();
    }

    @Test
    void buildPromptBlockIncludesSimilarSuccesses() {
        when(episodicIndexer.findSimilar(anyString(), anyInt()))
                .thenReturn(List.of(new EpisodicIndexer.EpisodicMatch(
                        "ep1", "TASK: refactor UserService\nOUTCOME: done",
                        true, "apply_patch", 5000, 0.88f)));
        String block = memory.buildPromptBlock("refactor UserService");
        assertThat(block).contains("Past Successes");
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private void inject(String name, Object value) throws Exception {
        Field f = MemoryHierarchy.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(memory, value);
    }
}

package tech.kayys.gamelan.memory;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.ConversationMessage;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.consolidation.MemoryConsolidationPipeline;
import tech.kayys.gamelan.memory.hierarchy.*;
import tech.kayys.gamelan.memory.working.*;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Layer VIII: Memory Architecture tests.
 * Covers consolidation pipeline, forgetting curves, and working memory
 * attention scoring and eviction.
 */
@ExtendWith(MockitoExtension.class)
class LayerVIIITest {

    // ── MemoryConsolidationPipeline ───────────────────────────────────────

    @Mock EpisodicMemory   episodic;
    @Mock SemanticMemory   semantic;
    @Mock ProceduralMemory procedural;
    @Mock GollekSdk        sdk;
    @Mock GamelanConfig    config;

    @InjectMocks MemoryConsolidationPipeline pipeline;

    @BeforeEach
    void setUpPipeline() throws Exception {
        when(config.defaultModel()).thenReturn("test-model");
        InferenceResponse resp = mock(InferenceResponse.class);
        when(resp.getContent()).thenReturn("When code fails to compile, check import statements first.");
        when(sdk.createCompletion(any(InferenceRequest.class))).thenReturn(resp);
        when(semantic.allNodes()).thenReturn(java.util.Map.of());
        when(semantic.upsert(any(), any(), any(), anyLong(), anyDouble()))
                .thenReturn(mock(SemanticMemory.KnowledgeNode.class));
    }

    @Test
    void consolidateSkippedWhenEpisodesBelowThreshold() {
        when(episodic.all()).thenReturn(List.of(episode(true), episode(false)));

        MemoryConsolidationPipeline.ConsolidationResult result = pipeline.consolidate(false);

        assertThat(result.ran()).isFalse();
        assertThat(result.summary()).contains("Skipped");
    }

    @Test
    void consolidateRunsWhenForced() {
        when(episodic.all()).thenReturn(List.of(episode(true), episode(false)));

        MemoryConsolidationPipeline.ConsolidationResult result = pipeline.consolidate(true);

        assertThat(result.ran()).isTrue();
        assertThat(result.run()).isNotNull();
    }

    @Test
    void consolidateRunsAutomaticallyAboveThreshold() {
        // Generate 500+ episodes to trigger consolidation
        List<EpisodicMemory.Episode> many = java.util.stream.IntStream.range(0, 500)
                .mapToObj(i -> episode(i % 3 != 0))  // 2/3 success
                .toList();
        when(episodic.all()).thenReturn(many);

        MemoryConsolidationPipeline.ConsolidationResult result = pipeline.consolidate(false);

        assertThat(result.ran()).isTrue();
    }

    @Test
    void consolidationRunRecordsInHistory() {
        when(episodic.all()).thenReturn(List.of(episode(true)));
        pipeline.consolidate(true);
        pipeline.consolidate(true);

        assertThat(pipeline.history()).hasSize(2);
    }

    @Test
    void consolidationRunSummaryIsNonBlank() {
        when(episodic.all()).thenReturn(List.of(episode(true)));
        MemoryConsolidationPipeline.ConsolidationResult result = pipeline.consolidate(true);
        assertThat(result.run().summary()).isNotBlank();
    }

    @Test
    void retentionScoreIsOneForJustCreatedEpisode() {
        EpisodicMemory.Episode fresh = new EpisodicMemory.Episode(
                1L, "task", "result", true,
                List.of("read_file"), 500L, Instant.now(), List.of());
        when(episodic.all()).thenReturn(List.of(fresh));

        double score = pipeline.retentionScore(fresh);

        assertThat(score).isGreaterThan(0.9);  // just created = nearly 1.0
    }

    @Test
    void retentionScoreDecreasesWithAge() {
        // Old episode: recorded 100 hours ago
        EpisodicMemory.Episode old = new EpisodicMemory.Episode(
                2L, "old task", "result", true,
                List.of("apply_patch"), 500L,
                Instant.now().minusSeconds(100 * 3600L), List.of());
        when(episodic.all()).thenReturn(List.of(old));

        double score = pipeline.retentionScore(old);

        assertThat(score).isLessThan(0.9);  // old episode = lower retention
    }

    @Test
    void healthReportContainsAllMetrics() {
        when(episodic.all()).thenReturn(List.of(episode(true), episode(false)));
        when(semantic.allNodes()).thenReturn(java.util.Map.of());
        when(procedural.all()).thenReturn(List.of());

        MemoryConsolidationPipeline.MemoryHealthReport health = pipeline.health();

        assertThat(health.episodeCount()).isEqualTo(2);
        assertThat(health.summary()).isNotBlank();
    }

    @Test
    void repeatedConsolidationsUseSemanticUpsert() throws Exception {
        when(episodic.all()).thenReturn(List.of(
                episodeWithTools(true,  List.of("read_file", "apply_patch")),
                episodeWithTools(true,  List.of("read_file", "apply_patch")),
                episodeWithTools(false, List.of("read_file", "write_file"))));

        pipeline.consolidate(true);

        // Semantic upsert should have been called for recurring patterns
        verify(semantic, atLeastOnce()).upsert(any(), any(), any(), anyLong(), anyDouble());
    }

    // ── WorkingMemoryManager ──────────────────────────────────────────────

    @Mock GamelanConfig wmConfig;
    @InjectMocks WorkingMemoryManager wm;

    @BeforeEach
    void setUpWM() {
        when(wmConfig.tokenBudget()).thenReturn(8000);
    }

    @Test
    void addMessageReturnsIncrementingId() {
        int id1 = wm.add(userMsg("first task"));
        int id2 = wm.add(userMsg("second task"));
        assertThat(id2).isEqualTo(id1 + 1);
    }

    @Test
    void totalTokensAccumulatesEstimates() {
        wm.add(userMsg("hello"));              // ~5 tokens
        wm.add(assistantMsg("world response")); // ~15 tokens
        assertThat(wm.totalTokens()).isGreaterThan(0);
    }

    @Test
    void getContextReturnsAllMessagesWhenUnderBudget() {
        wm.add(systemMsg("You are helpful."));
        wm.add(userMsg("task A"));
        wm.add(assistantMsg("response A"));

        List<ConversationMessage> ctx = wm.getContext("task A", 8000);
        assertThat(ctx).hasSize(3);
    }

    @Test
    void getContextEvictsLowAttentionWhenOverBudget() {
        // Fill with low-importance messages
        for (int i = 0; i < 50; i++) {
            wm.add(userMsg("filler message " + i), WorkingMemoryManager.MessageImportance.LOW);
        }
        // Add a high-importance one
        wm.add(systemMsg("critical context"), WorkingMemoryManager.MessageImportance.CRITICAL);

        // Budget = 500 tokens — forces eviction
        List<ConversationMessage> ctx = wm.getContext("critical task", 500);

        // Should include the critical message
        assertThat(ctx.stream().anyMatch(m -> m.content().contains("critical context"))).isTrue();
        // Should have fewer messages than we added
        assertThat(ctx.size()).isLessThan(51);
    }

    @Test
    void pinnedMessagesAreNeverEvicted() {
        int pinnedId = wm.add(userMsg("pinned important context"),
                WorkingMemoryManager.MessageImportance.LOW);
        wm.pin(pinnedId);

        // Fill with other messages
        for (int i = 0; i < 30; i++) {
            wm.add(userMsg("filler " + i), WorkingMemoryManager.MessageImportance.LOW);
        }

        // Very tight budget
        List<ConversationMessage> ctx = wm.getContext("some task", 200);

        assertThat(ctx.stream().anyMatch(m -> m.content().contains("pinned important context"))).isTrue();
    }

    @Test
    void criticalMessagesScoreHigher() {
        wm.add(userMsg("low importance"), WorkingMemoryManager.MessageImportance.LOW);
        wm.add(systemMsg("CRITICAL system context"), WorkingMemoryManager.MessageImportance.CRITICAL);

        // Tight budget: only 1 message can fit
        List<ConversationMessage> ctx = wm.getContext("task", 100);

        // Critical should survive
        boolean hasCritical = ctx.stream()
                .anyMatch(m -> m.content().contains("CRITICAL"));
        assertThat(hasCritical).isTrue();
    }

    @Test
    void errorMessagesHaveHigherAttention() {
        wm.add(assistantMsg("[ERROR] compilation failed"));
        wm.add(userMsg("some unrelated message"));

        List<ConversationMessage> ctx = wm.getContext("fix the error", 200);

        // Error message should survive in tight budget
        boolean hasError = ctx.stream().anyMatch(m -> m.content().contains("[ERROR]"));
        assertThat(hasError).isTrue();
    }

    @Test
    void compactReplacesLowAttentionWithSummary() {
        int before = wm.messageCount();
        for (int i = 0; i < 10; i++) {
            wm.add(userMsg("old message " + i), WorkingMemoryManager.MessageImportance.LOW);
        }
        int removed = wm.compact("Summary of old conversation.");
        assertThat(removed).isGreaterThan(0);
        // Should have a summary message now
        assertThat(wm.getContext("task", 8000))
                .anyMatch(m -> m.content().contains("Summary"));
    }

    @Test
    void clearResetsAllState() {
        wm.add(userMsg("msg1"));
        wm.add(userMsg("msg2"));
        int pinId = wm.add(systemMsg("pinned"));
        wm.pin(pinId);

        wm.clear();

        assertThat(wm.messageCount()).isEqualTo(0);
        assertThat(wm.totalTokens()).isEqualTo(0);
    }

    @Test
    void statsReportUtilizationRate() {
        wm.add(userMsg("hello world this is a medium length message"));
        WorkingMemoryManager.WorkingMemoryStats stats = wm.stats();

        assertThat(stats.messageCount()).isEqualTo(1);
        assertThat(stats.tokenBudget()).isEqualTo(8000);
        assertThat(stats.utilizationRate()).isBetween(0.0, 1.0);
        assertThat(stats.summary()).isNotBlank();
    }

    @Test
    void lastNExchangesAlwaysRetained() {
        // Fill with old LOW importance messages
        for (int i = 0; i < 20; i++) {
            wm.add(userMsg("old msg " + i), WorkingMemoryManager.MessageImportance.LOW);
        }
        // Add 4 recent messages
        wm.add(userMsg("recent user 1"));
        wm.add(assistantMsg("recent assistant 1"));
        wm.add(userMsg("recent user 2"));
        wm.add(assistantMsg("recent assistant 2 MARKER"));

        // Very tight budget
        List<ConversationMessage> ctx = wm.getContext("recent task", 300);

        assertThat(ctx.stream().anyMatch(m -> m.content().contains("MARKER"))).isTrue();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private EpisodicMemory.Episode episode(boolean success) {
        return episodeWithTools(success, List.of("read_file"));
    }

    private EpisodicMemory.Episode episodeWithTools(boolean success, List<String> tools) {
        return new EpisodicMemory.Episode(
                System.nanoTime(), "analyze code", success ? "done" : "failed",
                success, tools, 1500L, Instant.now(), List.of());
    }

    private ConversationMessage userMsg(String content) {
        return ConversationMessage.user(content);
    }

    private ConversationMessage assistantMsg(String content) {
        return ConversationMessage.assistant(content);
    }

    private ConversationMessage systemMsg(String content) {
        return ConversationMessage.system(content);
    }
}

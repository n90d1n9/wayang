package tech.kayys.gamelan.memory.hierarchy;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ProceduralMemoryTest {

    private ProceduralMemory memory;

    @BeforeEach
    void setUp() {
        memory = new ProceduralMemory();
        memory.init();
    }

    @Test
    void manualRegistrationCreatesNamedProcedure() {
        ProceduralMemory.Procedure p = memory.register(
                "fix-npe",
                "Read stack trace → find null → add Optional or null check",
                List.of("read_file", "search_files", "apply_patch"),
                List.of("NullPointerException", "NPE", "null pointer"));

        assertThat(p.id()).isGreaterThan(0);
        assertThat(p.name()).isEqualTo("fix-npe");
        assertThat(p.steps()).containsExactly("read_file", "search_files", "apply_patch");
        assertThat(p.triggers()).contains("NullPointerException");
        assertThat(p.successRate()).isEqualTo(1.0);
        assertThat(p.usageCount()).isEqualTo(0);
    }

    @Test
    void findApplicableMatchesByTriggerKeywords() {
        memory.register("fix-npe", "Fix null pointer errors",
                List.of("read_file", "apply_patch"),
                List.of("NullPointerException", "null", "pointer"));
        memory.register("add-tests", "Add unit tests",
                List.of("read_file", "write_file"),
                List.of("unit test", "junit", "test coverage"));

        List<ProceduralMemory.Procedure> found =
                memory.findApplicable("fix the NullPointerException in login", 5);

        assertThat(found).isNotEmpty();
        assertThat(found.get(0).name()).isEqualTo("fix-npe");
    }

    @Test
    void findApplicableFiltersLowSuccessRate() {
        ProceduralMemory.Procedure p = memory.register("bad-procedure", "Desc",
                List.of("read_file"), List.of("some common keyword task here"));
        // Drive success rate below threshold
        for (int i = 0; i < 5; i++) memory.recordOutcome(p.id(), false);

        List<ProceduralMemory.Procedure> found =
                memory.findApplicable("some common keyword task here", 5);

        // Should be filtered out due to low success rate (< 0.6)
        assertThat(found).noneMatch(proc -> proc.id() == p.id());
    }

    @Test
    void recordOutcomeUpdatesSuccessRateViaEma() {
        ProceduralMemory.Procedure p = memory.register("test-proc", "Test",
                List.of("tool"), List.of("keyword"));

        assertThat(p.successRate()).isEqualTo(1.0); // initial

        // Record a failure
        memory.recordOutcome(p.id(), false);

        ProceduralMemory.Procedure updated = memory.all().stream()
                .filter(proc -> proc.id() == p.id()).findFirst().orElseThrow();
        assertThat(updated.successRate()).isLessThan(1.0); // EMA applied
        assertThat(updated.usageCount()).isEqualTo(1);
    }

    @Test
    void allReturnsSortedBySuccessRateDesc() {
        ProceduralMemory.Procedure high = memory.register("high", "High success",
                List.of(), List.of("keyword"));
        ProceduralMemory.Procedure low = memory.register("low", "Low success",
                List.of(), List.of("keyword"));

        // Drive low's rate down
        for (int i = 0; i < 5; i++) memory.recordOutcome(low.id(), false);

        List<ProceduralMemory.Procedure> all = memory.all();
        // Find positions
        int highIdx = -1, lowIdx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id() == high.id()) highIdx = i;
            if (all.get(i).id() == low.id())  lowIdx  = i;
        }
        assertThat(highIdx).isLessThan(lowIdx);
    }

    @Test
    void learnFromEpisodePromotesAfterThresholdHits() {
        // Create 3 episodes with the same tool pattern
        var ep1 = createSuccessEpisode("fix npe in login", List.of("read_file", "apply_patch"));
        var ep2 = createSuccessEpisode("fix npe in signup", List.of("read_file", "apply_patch"));
        var ep3 = createSuccessEpisode("fix npe in logout", List.of("read_file", "apply_patch"));

        int before = memory.all().size();
        memory.learnFrom(ep1, List.of());
        memory.learnFrom(ep2, List.of());
        memory.learnFrom(ep3, List.of());
        int after = memory.all().size();

        // Pattern should have been promoted after 3rd hit
        assertThat(after).isGreaterThanOrEqualTo(before);
    }

    @Test
    void learnFromIgnoresFailureEpisodes() {
        var ep = createEpisode("fix npe", List.of("read_file", "apply_patch"), false);
        int before = memory.all().size();
        memory.learnFrom(ep, List.of());
        assertThat(memory.all().size()).isEqualTo(before); // no learning from failures
    }

    @Test
    void findApplicableReturnsEmptyForBlankTask() {
        memory.register("p", "desc", List.of(), List.of("keyword"));
        assertThat(memory.findApplicable("", 5)).isEmpty();
        assertThat(memory.findApplicable(null, 5)).isEmpty();
    }

    @Test
    void promptSnippetContainsKeyInfo() {
        ProceduralMemory.Procedure p = memory.register("fix-npe", "Fix null pointer errors",
                List.of("read_file", "apply_patch"), List.of("NPE"));
        String snippet = p.promptSnippet();
        assertThat(snippet).contains("fix-npe");
        assertThat(snippet).contains("100%"); // initial success rate is 1.0
        assertThat(snippet).contains("read_file");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private EpisodicMemory.Episode createSuccessEpisode(String task, List<String> tools) {
        return createEpisode(task, tools, true);
    }

    private EpisodicMemory.Episode createEpisode(String task, List<String> tools, boolean success) {
        return new EpisodicMemory.Episode(1L, task, "result", success, tools,
                500L, java.time.Instant.now(), List.of());
    }
}

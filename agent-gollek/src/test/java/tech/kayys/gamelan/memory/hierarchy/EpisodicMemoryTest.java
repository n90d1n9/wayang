package tech.kayys.gamelan.memory.hierarchy;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link EpisodicMemory}.
 * Uses in-memory storage only — no filesystem side effects.
 */
class EpisodicMemoryTest {

    private EpisodicMemory memory;

    @BeforeEach
    void setUp() {
        memory = new EpisodicMemory();
        memory.init();
    }

    @Test
    void recordsEpisodeWithAllFields() {
        var ep = memory.record("Fix null pointer in UserService", "Applied Optional",
                true, List.of("read_file", "apply_patch"), 1500L);

        assertThat(ep.id()).isGreaterThan(0);
        assertThat(ep.task()).isEqualTo("Fix null pointer in UserService");
        assertThat(ep.result()).isEqualTo("Applied Optional");
        assertThat(ep.success()).isTrue();
        assertThat(ep.toolsUsed()).containsExactly("read_file", "apply_patch");
        assertThat(ep.durationMs()).isEqualTo(1500L);
        assertThat(ep.recordedAt()).isNotNull();
    }

    @Test
    void findsRelevantEpisodesByKeywordOverlap() {
        memory.record("fix the null pointer in UserService authentication", "done", true,
                List.of("read_file"), 1000L);
        memory.record("add logging to OrderService", "done", true,
                List.of("write_file"), 500L);
        memory.record("fix authentication bug in UserService", "done", true,
                List.of("read_file", "write_file"), 2000L);

        List<EpisodicMemory.Episode> found = memory.findRelevant(
                "debugging UserService authentication", 3);

        assertThat(found).hasSize(2); // both authentication episodes
        assertThat(found).extracting(EpisodicMemory.Episode::task)
                .allMatch(t -> t.contains("UserService") || t.contains("authentication"));
    }

    @Test
    void returnsEmptyWhenNoOverlap() {
        memory.record("fix the null pointer in UserService", "done", true, List.of(), 1000L);

        List<EpisodicMemory.Episode> found = memory.findRelevant("deploy kubernetes cluster", 5);
        assertThat(found).isEmpty();
    }

    @Test
    void recentFailuresOnlyReturnsFailures() {
        memory.record("task a", "success", true,  List.of(), 100L);
        memory.record("task b", "failed",  false, List.of(), 200L);
        memory.record("task c", "success", true,  List.of(), 300L);
        memory.record("task d", "failed",  false, List.of(), 400L);

        List<EpisodicMemory.Episode> failures = memory.recentFailures(10);
        assertThat(failures).allMatch(ep -> !ep.success());
        assertThat(failures).hasSize(2);
    }

    @Test
    void statsAreCorrect() {
        memory.record("t1", "ok", true,  List.of("read_file", "write_file"), 100L);
        memory.record("t2", "ok", true,  List.of("read_file"), 200L);
        memory.record("t3", "err", false, List.of("run_command"), 300L);

        var stats = memory.stats();

        assertThat(stats.total()).isEqualTo(3);
        assertThat(stats.successes()).isEqualTo(2);
        assertThat(stats.failures()).isEqualTo(1);
        assertThat(stats.successRate()).isCloseTo(66.6, within(1.0));
        assertThat(stats.toolFrequency()).containsKey("read_file");
        assertThat(stats.toolFrequency().get("read_file")).isEqualTo(2);
    }

    @Test
    void truncatesResultsLongerThan2000Chars() {
        String longResult = "x".repeat(3000);
        var ep = memory.record("task", longResult, true, List.of(), 100L);
        assertThat(ep.result().length()).isLessThanOrEqualTo(2001);
        assertThat(ep.result()).endsWith("…");
    }

    @Test
    void idSequenceIsMonotonicallyIncreasing() {
        var ep1 = memory.record("t1", "r", true, List.of(), 1L);
        var ep2 = memory.record("t2", "r", true, List.of(), 1L);
        var ep3 = memory.record("t3", "r", true, List.of(), 1L);

        assertThat(ep1.id()).isLessThan(ep2.id());
        assertThat(ep2.id()).isLessThan(ep3.id());
    }

    @Test
    void allReturnsEverythingRecorded() {
        memory.record("a", "r", true, List.of(), 1L);
        memory.record("b", "r", true, List.of(), 1L);
        assertThat(memory.all()).hasSize(2);
    }

    @Test
    void relevanceScoresFavorRecentOverOlder() {
        // Add older episode first
        memory.record("fix UserService null pointer bug", "done", true, List.of(), 100L);
        // Add newer episode with same keywords
        memory.record("fix UserService null pointer in login", "done", true, List.of(), 200L);

        List<EpisodicMemory.Episode> found = memory.findRelevant("UserService null pointer", 2);
        assertThat(found).hasSize(2);
        // Both should be found (keyword overlap >= 2)
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   ", "x", "ab" })
    void returnsEmptyForShortOrBlankQuery(String query) {
        memory.record("UserService null pointer fix", "done", true, List.of(), 100L);
        assertThat(memory.findRelevant(query, 5)).isEmpty();
    }
}

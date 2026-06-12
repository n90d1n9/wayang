package tech.kayys.gamelan.evolution;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import tech.kayys.gamelan.evolution.pareto.ParetoFrontier;
import tech.kayys.gamelan.evolution.trace.ExecutionTraceAnalyzer;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Layer IV evolution primitives: Pareto frontier, trace analysis,
 * and the data model foundation for AVO.
 */
class EvolutionLayerTest {

    // ── ParetoFrontier ────────────────────────────────────────────────────

    private ParetoFrontier frontier;

    @BeforeEach
    void setUp() {
        frontier = new ParetoFrontier("test-skill");
    }

    @Test
    void emptyFrontierAcceptsFirstPoint() {
        var p = point("v1", 0.7, 0.8, 2000, 1000, 5);
        var result = frontier.add(p);
        assertThat(result.accepted()).isTrue();
        assertThat(result.outcome()).isEqualTo(ParetoFrontier.AddResult.AddOutcome.ACCEPTED_FIRST);
        assertThat(frontier.frontierSize()).isEqualTo(1);
    }

    @Test
    void dominatedPointIsRejected() {
        // v1 is better on ALL objectives → v2 is dominated
        frontier.add(point("v1", 0.9, 0.9, 1000, 500, 3));
        var result = frontier.add(point("v2", 0.7, 0.7, 2000, 1000, 5));
        assertThat(result.accepted()).isFalse();
        assertThat(result.outcome()).isEqualTo(ParetoFrontier.AddResult.AddOutcome.DOMINATED);
        assertThat(frontier.frontierSize()).isEqualTo(1); // still just v1
    }

    @Test
    void nonDominatedPointIsAddedToFrontier() {
        // v1: high quality, slow. v2: lower quality but faster — neither dominates the other
        frontier.add(point("v1", 0.9, 0.9, 5000, 2000, 8));
        var result = frontier.add(point("v2", 0.7, 0.8, 1000, 800, 3));
        assertThat(result.accepted()).isTrue();
        assertThat(frontier.frontierSize()).isEqualTo(2);
    }

    @Test
    void newPointThatDominatesExistingPrunesFrontier() {
        frontier.add(point("v1", 0.7, 0.7, 2000, 1000, 5));
        var result = frontier.add(point("v2", 0.9, 0.9, 1000, 500, 3)); // better everywhere
        assertThat(result.outcome()).isEqualTo(ParetoFrontier.AddResult.AddOutcome.DOMINATES_EXISTING);
        assertThat(result.improved()).isTrue();
        assertThat(frontier.frontierSize()).isEqualTo(1); // v1 removed
    }

    @Test
    void multipleDominatedPointsArePruned() {
        frontier.add(point("v1", 0.5, 0.5, 5000, 3000, 10));
        frontier.add(point("v2", 0.6, 0.6, 4000, 2500, 9));
        frontier.add(point("v3", 0.7, 0.7, 3000, 2000, 8));
        // New point dominates all three
        var result = frontier.add(point("vBest", 0.9, 0.9, 500, 200, 2));
        assertThat(result.improved()).isTrue();
        assertThat(frontier.frontierSize()).isEqualTo(1);
    }

    @Test
    void bestWeightedReturnsHighestScoringPoint() {
        frontier.add(point("vA", 0.9, 0.8, 2000, 1000, 5));
        frontier.add(point("vB", 0.6, 0.9, 800, 400, 2));
        frontier.add(point("vC", 0.5, 0.5, 200, 100, 1));

        var best = frontier.bestWeighted(ParetoFrontier.ObjectiveWeights.qualityFirst());
        assertThat(best).isPresent();
        // quality-first weights quality the most → vA should win
        assertThat(best.get().version()).isEqualTo("vA");
    }

    @Test
    void bestOnQualityReturnsHighestQualityPoint() {
        frontier.add(point("vA", 0.9, 0.5, 3000, 1000, 5));
        frontier.add(point("vB", 0.5, 0.9, 1000, 500, 2));

        var best = frontier.bestOn(ParetoFrontier.Objective.QUALITY_SCORE);
        assertThat(best).isPresent();
        assertThat(best.get().version()).isEqualTo("vA");
    }

    @Test
    void bestOnLatencyReturnsLowestLatency() {
        frontier.add(point("vSlow", 0.9, 0.9, 5000, 2000, 5));
        frontier.add(point("vFast", 0.7, 0.7, 500, 300, 3));

        var best = frontier.bestOn(ParetoFrontier.Objective.LATENCY_MS);
        assertThat(best).isPresent();
        assertThat(best.get().version()).isEqualTo("vFast");
    }

    @Test
    void hasImprovedReturnsTrueWhenBetterPointAdded() {
        frontier.add(point("v0-baseline", 0.6, 0.6, 3000, 1500, 6));
        frontier.add(point("v1-improved", 0.85, 0.85, 1000, 500, 3));
        assertThat(frontier.hasImproved("v0-baseline")).isTrue();
    }

    @Test
    void historyContainsAllAddedPoints() {
        frontier.add(point("v1", 0.5, 0.5, 2000, 1000, 5));
        frontier.add(point("v2", 0.6, 0.6, 1500, 800, 4));
        frontier.add(point("v3", 0.4, 0.4, 3000, 2000, 8)); // dominated — rejected
        assertThat(frontier.history()).hasSize(3); // all 3 in history
        assertThat(frontier.frontierSize()).isEqualTo(2); // only v1,v2 on frontier
    }

    @Test
    void reportIsNonBlank() {
        frontier.add(point("v1", 0.8, 0.9, 1500, 700, 4));
        assertThat(frontier.report()).isNotBlank().contains("v1");
    }

    @Test
    void emptyFrontierBestWeightedReturnsEmpty() {
        assertThat(frontier.bestWeighted(ParetoFrontier.ObjectiveWeights.equal())).isEmpty();
        assertThat(frontier.bestOn(ParetoFrontier.Objective.QUALITY_SCORE)).isEmpty();
    }

    // ── Dominance edge cases ───────────────────────────────────────────────

    @Test
    void pointWithIdenticalMetricsIsNotDominated() {
        var p = point("v1", 0.8, 0.8, 1000, 500, 4);
        frontier.add(p);
        // Same metrics → neither dominates
        var result = frontier.add(point("v2", 0.8, 0.8, 1000, 500, 4));
        // v2 doesn't strictly improve on any dimension → ACCEPTED (non-dominated) not DOMINATES
        assertThat(result.accepted()).isTrue();
    }

    @Test
    void paretoWeightsValidationWorks() {
        assertThatCode(() -> ParetoFrontier.ObjectiveWeights.equal().validate()).doesNotThrowAnyException();
        assertThatCode(() -> ParetoFrontier.ObjectiveWeights.qualityFirst().validate()).doesNotThrowAnyException();
        // Invalid weights (don't sum to 1)
        var invalid = new ParetoFrontier.ObjectiveWeights(0.5, 0.5, 0.5, 0.5, 0.5);
        assertThatThrownBy(invalid::validate).isInstanceOf(IllegalArgumentException.class);
    }

    // ── ExecutionTraceAnalyzer ────────────────────────────────────────────

    private ExecutionTraceAnalyzer analyzer;

    @BeforeEach
    void setUpAnalyzer() {
        analyzer = new ExecutionTraceAnalyzer();
    }

    @Test
    void emptyEpisodesReturnsEmptyAnalysis() {
        var result = analyzer.analyze(List.of(), "my-skill");
        assertThat(result.episodeCount()).isEqualTo(0);
        assertThat(result.hasSignals()).isFalse();
    }

    @Test
    void repeatedToolCallIsDetectedAsInefficiency() {
        var ep = episode(true, List.of("read_file","read_file","read_file","apply_patch"));
        var result = analyzer.analyze(List.of(ep), null);
        assertThat(result.inefficiencies()).anyMatch(i ->
                i.type() == ExecutionTraceAnalyzer.InefficiencyType.REPEATED_TOOL_CALL);
    }

    @Test
    void readAfterWriteIsDetected() {
        var ep = episode(true, List.of("read_file","write_file","read_file"));
        var result = analyzer.analyze(List.of(ep), null);
        assertThat(result.inefficiencies()).anyMatch(i ->
                i.type() == ExecutionTraceAnalyzer.InefficiencyType.READ_AFTER_WRITE);
    }

    @Test
    void recurringFailurePatternIsDetected() {
        var f1 = episode(false, List.of("read_file", "write_file"));
        var f2 = episode(false, List.of("read_file", "write_file"));
        var result = analyzer.analyze(List.of(f1, f2), null);
        assertThat(result.failures()).isNotEmpty();
        assertThat(result.failures().get(0).occurrences()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void successPatternExtractedFrom3SuccessfulEpisodes() {
        var s1 = episode(true, List.of("read_file", "apply_patch"));
        var s2 = episode(true, List.of("read_file", "apply_patch"));
        var s3 = episode(true, List.of("read_file", "apply_patch"));
        var result = analyzer.analyze(List.of(s1,s2,s3), null);
        assertThat(result.successes()).anyMatch(sp ->
                sp.toolSequence().equals("read_file→apply_patch") && sp.occurrences() >= 3);
    }

    @Test
    void toolStatsAggregateCorrectly() {
        var s1 = episode(true, List.of("read_file","search_files","apply_patch"));
        var s2 = episode(false, List.of("read_file","write_file"));
        var result = analyzer.analyze(List.of(s1,s2), null);
        assertThat(result.toolStats()).containsKey("read_file");
        assertThat(result.toolStats().get("read_file").totalCalls()).isEqualTo(2);
    }

    @Test
    void recommendStrategiesReturnsNonEmpty() {
        var ep = episode(false, List.of("read_file","read_file","read_file"));
        var analysis = analyzer.analyze(List.of(ep), null);
        var strategies = analyzer.recommendStrategies(analysis);
        assertThat(strategies).isNotEmpty();
    }

    @Test
    void summaryContainsEpisodeCount() {
        var ep = episode(true, List.of("read_file"));
        var result = analyzer.analyze(List.of(ep), "test-skill");
        assertThat(result.summary()).contains("1");
        assertThat(result.summary()).contains("test-skill");
    }

    // ── AVO data types ────────────────────────────────────────────────────

    @Test
    void variationStrategyHasCodeAndDescription() {
        for (var strategy : tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy.values()) {
            assertThat(strategy.code()).isNotBlank();
            assertThat(strategy.description()).isNotBlank();
        }
    }

    @Test
    void variantRecordHasAllFields() {
        var variant = new tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.Variant(
                "test-v1",
                "## Instructions\nDo the work.",
                tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy.PRECISION,
                1, Instant.now());
        assertThat(variant.id()).isEqualTo("test-v1");
        assertThat(variant.instructions()).contains("Instructions");
        assertThat(variant.strategy()).isEqualTo(
                tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy.PRECISION);
        assertThat(variant.generation()).isEqualTo(1);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private ParetoFrontier.ParetoPoint point(String version, double quality, double success,
                                              long latency, int tokens, int tools) {
        return new ParetoFrontier.ParetoPoint(version, quality, success, latency, tokens, tools, "", Instant.now());
    }

    private EpisodicMemory.Episode episode(boolean success, List<String> tools) {
        return new EpisodicMemory.Episode(
                System.nanoTime(), success ? "analyze task" : "fix failing task",
                success ? "success" : "failed: read failed",
                success, tools, 1500L, Instant.now(), List.of());
    }
}

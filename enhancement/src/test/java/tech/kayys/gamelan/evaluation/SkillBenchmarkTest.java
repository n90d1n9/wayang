package tech.kayys.gamelan.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.agent.orchestration.SingleAgentOrchestrator;
import tech.kayys.gamelan.config.GamelanConfig;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillBenchmarkTest {

    @Mock SingleAgentOrchestrator agent;
    @Mock GamelanConfig           config;
    @InjectMocks SkillBenchmark   benchmark;

    private OrchestratorResult ok(String answer) {
        return OrchestratorResult.ok(answer, "react", 1, List.of(), Duration.ofMillis(100));
    }

    // ── BenchmarkTask.evaluate ─────────────────────────────────────────────

    @Test
    void passesWhenAllCriteriaMet() {
        var task = new SkillBenchmark.BenchmarkTask(
                "t1", "test", "prompt",
                List.of("null", "optional"), List.of("wrong"), 5, 10_000);
        var result = ok("Java Optional handles null values gracefully");
        assertThat(task.evaluate(result)).isTrue();
    }

    @Test
    void failsWhenMustContainMissing() {
        var task = new SkillBenchmark.BenchmarkTask(
                "t1", "test", "prompt",
                List.of("required-word"), List.of(), 5, 10_000);
        assertThat(task.evaluate(ok("answer without the keyword"))).isFalse();
    }

    @Test
    void failsWhenMustNotContainPresent() {
        var task = new SkillBenchmark.BenchmarkTask(
                "t1", "test", "prompt",
                List.of(), List.of("forbidden"), 5, 10_000);
        assertThat(task.evaluate(ok("answer with forbidden content"))).isFalse();
    }

    @Test
    void failsWhenStepsExceedMax() {
        var task = new SkillBenchmark.BenchmarkTask(
                "t1", "test", "prompt", List.of(), List.of(), 2, 10_000);
        var result = OrchestratorResult.ok("answer", "react", 5, List.of(), Duration.ofMillis(100));
        assertThat(task.evaluate(result)).isFalse();
    }

    @Test
    void failsWhenAgentFailed() {
        var task = new SkillBenchmark.BenchmarkTask(
                "t1", "test", "prompt", List.of(), List.of(), 0, 0);
        var result = OrchestratorResult.failure("react", "error", Duration.ZERO);
        assertThat(task.evaluate(result)).isFalse();
    }

    // ── SuiteResult ────────────────────────────────────────────────────────

    @Test
    void regressionDetectionWorks() {
        var baseline  = makeSuiteResult(0.9, 9, 10);
        var regressed = makeSuiteResult(0.7, 7, 10); // 20% drop
        assertThat(regressed.hasRegression(baseline)).isTrue();
    }

    @Test
    void noRegressionForSmallDrop() {
        var baseline = makeSuiteResult(0.9, 9, 10);
        var slight   = makeSuiteResult(0.85, 8, 10); // 5% drop — under 10% threshold
        assertThat(slight.hasRegression(baseline)).isFalse();
    }

    @Test
    void noRegressionIfImproved() {
        var baseline  = makeSuiteResult(0.7, 7, 10);
        var improved  = makeSuiteResult(0.9, 9, 10);
        assertThat(improved.hasRegression(baseline)).isFalse();
    }

    @Test
    void summaryContainsPassedCount() {
        var suite = makeSuiteResult(0.8, 8, 10);
        assertThat(suite.summary()).contains("8");
        assertThat(suite.summary()).contains("10");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private SkillBenchmark.SuiteResult makeSuiteResult(double score, int passed, int total) {
        return new SkillBenchmark.SuiteResult(
                "test-suite", List.of(), passed, total, score, 1000L, java.time.Instant.now());
    }
}

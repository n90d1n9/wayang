package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentState;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningPolicyTest {

    @Test
    void forceLearnOverridesSimpleRunButSkipTakesPrecedence() {
        HermesLearningPolicy policy = new HermesLearningPolicy(HermesAgentModeConfig.defaults());

        HermesLearningAssessment forced = policy.assess(signal(true, 1, Map.of(
                HermesAgentMode.PARAM_LEARN_KEY, "yes")));
        HermesLearningAssessment skipped = policy.assess(signal(true, 5, Map.of(
                HermesAgentMode.PARAM_LEARN_KEY, true,
                HermesAgentMode.PARAM_SKIP_LEARN_KEY, "on")));

        assertThat(forced.eligible()).isTrue();
        assertThat(forced.forced()).isTrue();
        assertThat(forced.reason()).isEqualTo("learning explicitly requested");
        assertThat(forced.metrics()).containsKey("qualityScore");
        assertThat(skipped.eligible()).isFalse();
        assertThat(skipped.reason()).isEqualTo("learning explicitly skipped");
    }

    @Test
    void reportsConcreteSkipReasonForFailedOrBelowThresholdRun() {
        HermesLearningPolicy policy = new HermesLearningPolicy(HermesAgentModeConfig.builder()
                .minStepsToLearn(4)
                .build());

        HermesLearningAssessment failed = policy.assess(signal(false, 8, Map.of()));
        HermesLearningAssessment simple = policy.assess(signal(true, 2, Map.of()));

        assertThat(failed.eligible()).isFalse();
        assertThat(failed.reason()).isEqualTo("run was not successful");
        assertThat(simple.eligible()).isFalse();
        assertThat(simple.reason()).isEqualTo("run had 2 step(s), below learning threshold 4");
    }

    @Test
    void usesQualityGateForProceduralReusePotential() {
        HermesLearningPolicy policy = new HermesLearningPolicy(HermesAgentModeConfig.builder()
                .minStepsToLearn(3)
                .build());

        HermesLearningAssessment reusable = policy.assess(proceduralSignal());
        HermesLearningAssessment noisy = policy.assess(signal(true, 4, Map.of("error", "transient failure")));

        assertThat(reusable.eligible()).isTrue();
        assertThat(reusable.reason()).isEqualTo("run met learning quality threshold");
        assertThat(reusable.qualityScore()).isGreaterThanOrEqualTo(reusable.qualityThreshold());
        assertThat(reusable.toMetadata())
                .containsEntry("eligible", true)
                .containsKey("metrics");
        assertThat(noisy.eligible()).isFalse();
        assertThat(noisy.reason()).startsWith("learning quality score ");
        assertThat(noisy.qualityScore()).isLessThan(noisy.qualityThreshold());
        assertThat(noisy.metrics())
                .containsEntry("failureMarkerCount", 1)
                .containsEntry("passes", false);
    }

    private static HermesLearningSignal signal(boolean successful, int steps, Map<String, Object> metadata) {
        return new HermesLearningSignal(
                "req-policy",
                "Policy task",
                "Policy answer",
                successful,
                java.util.stream.IntStream.rangeClosed(1, steps)
                        .mapToObj(index -> new AgentState.ReasoningStep(
                                index,
                                "Step " + index,
                                null,
                                "Observation " + index,
                                1,
                                true))
                        .toList(),
                List.of(),
                metadata,
                Instant.parse("2026-06-02T00:00:00Z"));
    }

    private static HermesLearningSignal proceduralSignal() {
        return new HermesLearningSignal(
                "req-policy",
                "Generate nightly API backup report",
                "Verified report is ready with backup evidence, checks, and next action notes.",
                true,
                java.util.stream.IntStream.rangeClosed(1, 3)
                        .mapToObj(index -> new AgentState.ReasoningStep(
                                index,
                                "Step " + index,
                                new AgentState.AgentAction(
                                        index == 1 ? "rag" : "terminal",
                                        "complete step " + index,
                                        Map.of("query", "q" + index),
                                        Instant.parse("2026-06-02T00:00:00Z")),
                                "Observation " + index,
                                1,
                                true))
                        .toList(),
                List.of("rag", "terminal"),
                Map.of(),
                Instant.parse("2026-06-02T00:00:00Z"));
    }
}

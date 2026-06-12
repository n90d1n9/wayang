package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningStageCatalogTest {

    @Test
    void exposesStableLearningStageGroups() {
        assertThat(HermesLearningStageCatalog.PLANNING_STAGES)
                .containsExactly(
                        "signal-detection",
                        "eligibility-assessment",
                        "skill-distillation",
                        "candidate-validation",
                        "reuse-match");
        assertThat(HermesLearningStageCatalog.EXECUTION_STAGES)
                .containsExactly(
                        "promotion-decision",
                        "skill-persistence",
                        "promotion-receipt");
        assertThat(HermesLearningStageCatalog.OPTIONAL_STAGES)
                .containsExactly("skill-indexing");
        assertThat(HermesLearningStageCatalog.FULL_FLOW)
                .containsExactly(
                        "signal-detection",
                        "eligibility-assessment",
                        "skill-distillation",
                        "candidate-validation",
                        "reuse-match",
                        "promotion-decision",
                        "skill-persistence",
                        "promotion-receipt");
        assertThat(HermesLearningStageCatalog.ALL_STAGES)
                .contains("skill-indexing")
                .doesNotHaveDuplicates();
    }

    @Test
    void detectsKnownLearningStageIds() {
        assertThat(HermesLearningStageCatalog.contains("skill-persistence")).isTrue();
        assertThat(HermesLearningStageCatalog.contains(" promotion-receipt ")).isTrue();
        assertThat(HermesLearningStageCatalog.contains("unknown-stage")).isFalse();
    }
}

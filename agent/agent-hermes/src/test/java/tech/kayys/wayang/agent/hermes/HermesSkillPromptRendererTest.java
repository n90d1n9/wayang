package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentState;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPromptRendererTest {

    @Test
    void rendersProcedureWithConfiguredStepLimitAndVerification() {
        HermesLearningSignal signal = signal(
                List.of(step(1, "rag"), step(2, "terminal"), step(3, "mcp")),
                "Verified release evidence");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .maxSkillProcedureSteps(2)
                .build();

        String prompt = new HermesSkillPromptRenderer().initialPrompt(signal, config);

        assertThat(prompt)
                .contains("## When to Use")
                .contains("Use this skill for tasks similar to: Prepare release report")
                .contains("1. Use `rag` because complete step 1.")
                .contains("2. Use `terminal` because complete step 2.")
                .doesNotContain("Use `mcp`")
                .contains("## Verification")
                .contains("Previous successful outcome: Verified release evidence");
    }

    @Test
    void rendersFallbackProcedureAndRefinementBlock() {
        HermesLearningSignal signal = signal(List.of(), "Done");

        HermesSkillPromptRenderer renderer = new HermesSkillPromptRenderer();

        assertThat(renderer.initialPrompt(signal, HermesAgentModeConfig.defaults()))
                .contains("1. Analyze the request and identify reusable context.")
                .contains("2. Execute the needed tools or skills.")
                .contains("3. Verify the answer before returning it.");
        assertThat(renderer.refinementPrompt(signal))
                .contains("## Latest Refinement")
                .contains("- Request: Prepare release report")
                .contains("- Verification: Done");
    }

    private static HermesLearningSignal signal(List<AgentState.ReasoningStep> steps, String answer) {
        return new HermesLearningSignal(
                "req-prompt",
                "Prepare release report",
                answer,
                true,
                steps,
                List.of(),
                Map.of(),
                Instant.parse("2026-06-02T00:00:00Z"));
    }

    private static AgentState.ReasoningStep step(int number, String skillId) {
        return new AgentState.ReasoningStep(
                number,
                "Step " + number,
                new AgentState.AgentAction(skillId, "complete step " + number, Map.of(), Instant.now()),
                "ok",
                5,
                true);
    }
}

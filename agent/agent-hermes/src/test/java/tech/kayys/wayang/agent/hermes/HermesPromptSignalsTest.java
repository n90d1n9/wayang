package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesPromptSignalsTest {

    @Test
    void infersAutomationSchedulesFromPromptText() {
        assertThat(HermesPromptSignals.inferAutomationSchedule("Generate the report every 2 hours"))
                .contains("every 2 hours");
        assertThat(HermesPromptSignals.inferAutomationSchedule("Run the audit nightly"))
                .contains("nightly");
        assertThat(HermesPromptSignals.inferAutomationSchedule("Run this once"))
                .isEmpty();
    }

    @Test
    void detectsExecutionBackendSignals() {
        assertThat(HermesPromptSignals.suggestsIsolation("Run this in a Docker sandbox")).isTrue();
        assertThat(HermesPromptSignals.suggestsRemote("Deploy over SSH to a VPS")).isTrue();
        assertThat(HermesPromptSignals.suggestsServerless("Use Modal serverless workers")).isTrue();
    }

    @Test
    void infersDelegationSignalsAndLanes() {
        assertThat(HermesPromptSignals.suggestsDelegation(
                "Split this into parallel research, implementation, verification, and docs tracks"))
                .isTrue();
        assertThat(HermesPromptSignals.inferredDelegationLanes(
                "Research, implement, test, review, and document the change"))
                .containsExactly("research", "implementation", "verification", "review", "documentation");
    }

    @Test
    void detectsProviderRoutingSignals() {
        assertThat(HermesPromptSignals.provider("Route this through Nous Portal")).contains("nous-portal");
        assertThat(HermesPromptSignals.suggestsLocalProvider("Use an offline local model")).isTrue();
        assertThat(HermesPromptSignals.suggestsHighContext("This is a long-running multi-step workstream")).isTrue();
        assertThat(HermesPromptSignals.suggestsApiGateway("Use OpenRouter for this run")).isTrue();
    }

    @Test
    void detectsMemoryAndTrajectorySignals() {
        assertThat(HermesPromptSignals.suggestsMemoryReflection("Always remember this important preference")).isTrue();
        assertThat(HermesPromptSignals.inferredMemoryPriority("Never forget this")).isEqualTo("high");
        assertThat(HermesPromptSignals.suggestsTrajectoryExport("Save the execution trace for audit trail")).isTrue();
    }
}

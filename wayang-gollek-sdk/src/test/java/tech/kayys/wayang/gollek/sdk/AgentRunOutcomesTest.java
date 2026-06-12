package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunOutcomesTest {

    @Test
    void exposesStableLifecycleOutcomeWireNames() {
        assertThat(AgentRunOutcomes.knownOutcomeNames())
                .containsExactly(
                        "terminal",
                        "timeout",
                        "max-polls",
                        "forgotten",
                        "cancelled",
                        "not-cancellable",
                        "not-found",
                        "unknown",
                        "empty",
                        "pending");
    }

    @Test
    void normalizesLifecycleOutcomeWireNames() {
        assertThat(AgentRunOutcomes.normalizeOrDefault(" NOT_CANCELLABLE ", AgentRunOutcomes.UNKNOWN))
                .isEqualTo(AgentRunOutcomes.NOT_CANCELLABLE);
        assertThat(AgentRunOutcomes.normalizeOrDefault("mystery", AgentRunOutcomes.TIMEOUT))
                .isEqualTo(AgentRunOutcomes.TIMEOUT);
    }
}

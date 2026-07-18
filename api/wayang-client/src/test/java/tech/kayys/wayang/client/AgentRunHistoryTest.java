package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.history.AgentRunHistory;
import tech.kayys.wayang.agent.run.AgentRunHandle;
import tech.kayys.wayang.agent.run.AgentRunOutcomes;
import tech.kayys.wayang.agent.run.AgentRunState;
import tech.kayys.wayang.agent.run.AgentRunStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunHistoryTest {

    @Test
    void reportsEmptyOutcomeWhenNoRunsAreReturned() {
        AgentRunHistory history = AgentRunHistory.empty("none");

        assertThat(history.outcome()).isEqualTo(AgentRunOutcomes.EMPTY);
    }

    @Test
    void reportsTerminalOutcomeWhenAllReturnedRunsAreTerminal() {
        AgentRunHistory history = new AgentRunHistory(
                List.of(
                        new AgentRunStatus(
                                AgentRunHandle.completed("run-history-1", "strategy-a"),
                                true,
                                "done",
                                Map.of()),
                        new AgentRunStatus(
                                new AgentRunHandle("run-history-2", AgentRunState.CANCELLED, "strategy-a"),
                                true,
                                "cancelled",
                                Map.of())),
                2,
                "done");

        assertThat(history.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
    }

    @Test
    void reportsPendingOutcomeWhenAnyReturnedRunIsNonTerminal() {
        AgentRunHistory history = new AgentRunHistory(
                List.of(
                        new AgentRunStatus(
                                AgentRunHandle.completed("run-history-3", "strategy-a"),
                                true,
                                "done",
                                Map.of()),
                        new AgentRunStatus(
                                new AgentRunHandle("run-history-4", AgentRunState.RUNNING, "strategy-a"),
                                true,
                                "running",
                                Map.of())),
                2,
                "mixed");

        assertThat(history.outcome()).isEqualTo(AgentRunOutcomes.PENDING);
    }

    @Test
    void reportsUnknownOutcomeWhenAnyReturnedRunIsUnknown() {
        AgentRunHistory history = new AgentRunHistory(
                List.of(AgentRunStatus.unknown("run-history-5", "unknown")),
                1,
                "unknown");

        assertThat(history.outcome()).isEqualTo(AgentRunOutcomes.UNKNOWN);
    }
}

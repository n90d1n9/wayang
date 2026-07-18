package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.run.AgentRunHandle;
import tech.kayys.wayang.agent.run.AgentRunOutcomes;
import tech.kayys.wayang.agent.run.AgentRunResult;
import tech.kayys.wayang.agent.run.AgentRunState;
import tech.kayys.wayang.agent.run.AgentRunStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunResultTest {

    @Test
    void derivesCompletedLifecycleHandleForSuccessfulLegacyResults() {
        AgentRunResult result = new AgentRunResult(
                " run-1 ",
                "done",
                true,
                " strategy-a ",
                List.of("step"),
                Map.of());

        assertThat(result.runId()).isEqualTo("run-1");
        assertThat(result.handle().runId()).isEqualTo("run-1");
        assertThat(result.handle().state()).isEqualTo(AgentRunState.COMPLETED);
        assertThat(result.handle().strategy()).isEqualTo("strategy-a");
        assertThat(result.handle().terminal()).isTrue();
        assertThat(result.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
    }

    @Test
    void derivesFailedLifecycleHandleForUnsuccessfulLegacyResults() {
        AgentRunResult result = new AgentRunResult(
                "run-2",
                "failed",
                false,
                "strategy-b",
                List.of(),
                Map.of());

        assertThat(result.handle().state()).isEqualTo(AgentRunState.FAILED);
        assertThat(result.handle().terminal()).isTrue();
        assertThat(result.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
    }

    @Test
    void preservesExplicitLifecycleHandle() {
        AgentRunHandle handle = new AgentRunHandle("run-3", AgentRunState.RUNNING, "strategy-c");

        AgentRunResult result = new AgentRunResult(
                "run-3",
                "running",
                true,
                "strategy-c",
                List.of(),
                Map.of(),
                handle);

        assertThat(result.handle()).isSameAs(handle);
        assertThat(result.handle().terminal()).isFalse();
        assertThat(result.outcome()).isEqualTo(AgentRunOutcomes.PENDING);
    }

    @Test
    void buildsUnknownStatusSnapshot() {
        AgentRunStatus status = AgentRunStatus.unknown("run-4", "No store");

        assertThat(status.known()).isFalse();
        assertThat(status.outcome()).isEqualTo(AgentRunOutcomes.UNKNOWN);
        assertThat(status.handle().runId()).isEqualTo("run-4");
        assertThat(status.handle().state()).isEqualTo(AgentRunState.UNKNOWN);
        assertThat(status.handle().terminal()).isFalse();
        assertThat(status.message()).isEqualTo("No store");
    }

    @Test
    void buildsPendingStatusSnapshotForKnownNonTerminalRuns() {
        AgentRunStatus status = new AgentRunStatus(
                new AgentRunHandle("run-5", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of());

        assertThat(status.outcome()).isEqualTo(AgentRunOutcomes.PENDING);
    }
}

package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.event.AgentRunEvent;
import tech.kayys.wayang.agent.event.AgentRunEvents;
import tech.kayys.wayang.agent.run.AgentRunHandle;
import tech.kayys.wayang.agent.run.AgentRunInspection;
import tech.kayys.wayang.agent.run.AgentRunOutcomes;
import tech.kayys.wayang.agent.run.AgentRunState;
import tech.kayys.wayang.agent.run.AgentRunStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunInspectionTest {

    @Test
    void derivesOutcomeFromKnownStatusFirst() {
        AgentRunInspection inspection = new AgentRunInspection(
                "run-inspect-1",
                new AgentRunStatus(
                        new AgentRunHandle("run-inspect-1", AgentRunState.RUNNING, "strategy-a"),
                        true,
                        "running",
                        Map.of()),
                new AgentRunEvents(
                        "run-inspect-1",
                        List.of(new AgentRunEvent(
                                "run-inspect-1",
                                1,
                                "run.completed",
                                AgentRunState.COMPLETED,
                                "done",
                                Map.of())),
                        "Recorded run events."),
                "Inspected Wayang run lifecycle.");

        assertThat(inspection.outcome()).isEqualTo(AgentRunOutcomes.PENDING);
    }

    @Test
    void fallsBackToEventOutcomeWhenStatusIsUnknown() {
        AgentRunInspection inspection = new AgentRunInspection(
                "run-inspect-2",
                AgentRunStatus.unknown("run-inspect-2", "unknown"),
                new AgentRunEvents(
                        "run-inspect-2",
                        List.of(new AgentRunEvent(
                                "run-inspect-2",
                                1,
                                "run.completed",
                                AgentRunState.COMPLETED,
                                "done",
                                Map.of())),
                        "Recorded run events."),
                "Inspected Wayang run lifecycle.");

        assertThat(inspection.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
    }

    @Test
    void reportsEmptyOutcomeWhenNoLifecycleDataExists() {
        AgentRunInspection inspection = new AgentRunInspection(
                "run-inspect-3",
                AgentRunStatus.unknown("run-inspect-3", "unknown"),
                new AgentRunEvents("run-inspect-3", List.of(), "No events."),
                "No run lifecycle data is recorded for this run id.");

        assertThat(inspection.outcome()).isEqualTo(AgentRunOutcomes.EMPTY);
    }
}

package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.event.AgentRunEvent;
import tech.kayys.wayang.agent.event.AgentRunEventEnvelopes;
import tech.kayys.wayang.agent.event.AgentRunEvents;
import tech.kayys.wayang.agent.event.AgentRunEventsFollowResult;
import tech.kayys.wayang.agent.event.AgentRunEventsQuery;
import tech.kayys.wayang.agent.history.AgentRunHistory;
import tech.kayys.wayang.agent.history.AgentRunHistoryEnvelopes;
import tech.kayys.wayang.agent.history.AgentRunHistoryQuery;
import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.run.AgentRunControlEnvelopes;
import tech.kayys.wayang.agent.run.AgentRunHandle;
import tech.kayys.wayang.agent.run.AgentRunInspection;
import tech.kayys.wayang.agent.run.AgentRunInspectionEnvelopes;
import tech.kayys.wayang.agent.run.AgentRunOutcomes;
import tech.kayys.wayang.agent.run.AgentRunState;
import tech.kayys.wayang.agent.run.AgentRunStatus;
import tech.kayys.wayang.agent.run.AgentRunWaitResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunLifecycleEnvelopesTest {

    @Test
    void eventEnvelopesOwnTimelineStatsAndFollowShapes() {
        AgentRunEvents events = new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.of(null, "", 0L, 10),
                List.of(
                        new AgentRunEvent(
                                "run-1",
                                1,
                                "run.running",
                                AgentRunState.RUNNING,
                                "started",
                                Map.of("phase", "work")),
                        new AgentRunEvent(
                                "run-1",
                                2,
                                "run.completed",
                                AgentRunState.COMPLETED,
                                "done",
                                Map.of())),
                2,
                "ok");

        Map<String, Object> timeline = AgentRunEventEnvelopes.events(events);
        Map<String, Object> stats = AgentRunEventEnvelopes.eventsStats(events);
        Map<String, Object> follow = AgentRunEventEnvelopes.followResult(
                new AgentRunEventsFollowResult(
                        "run-1",
                        AgentRunEventsQuery.all(),
                        AgentRunEventsQuery.of(null, "", 2L, 10),
                        events,
                        false,
                        false,
                        2,
                        25,
                        "done",
                        Map.of("mode", "follow")),
                false);

        assertThat(objectMap(timeline.get("contract")))
                .containsEntry("envelope", AgentRunLifecycleContract.RUN_EVENTS);
        assertThat(list(timeline.get("events"))).hasSize(2);
        assertThat(objectMap(stats.get("contract")))
                .containsEntry("envelope", AgentRunLifecycleContract.RUN_EVENTS_STATS);
        assertThat(stats).doesNotContainKey("events");
        assertThat(objectMap(follow.get("contract")))
                .containsEntry("envelope", AgentRunLifecycleContract.RUN_EVENTS_FOLLOW);
        assertThat(follow)
                .containsEntry("terminal", true)
                .containsEntry("terminalState", "completed")
                .containsEntry("terminalSequence", 2L)
                .containsEntry("nextAfterSequence", 2L);
        assertThat(objectMap(follow.get("lastEvents"))).containsKey("events");
    }

    @Test
    void historyEnvelopesOwnListAndStatsShapes() {
        AgentRunHistory history = new AgentRunHistory(
                AgentRunHistoryQuery.of(null, 10, "tenant-a", "", "coding-agent", "profile-a", 0),
                List.of(
                        new AgentRunStatus(
                                AgentRunHandle.completed("run-2", "react"),
                                true,
                                "done",
                                Map.of("surfaceId", "coding-agent", "profileId", "profile-a"))),
                1,
                "one run");

        Map<String, Object> values = AgentRunHistoryEnvelopes.history(history);
        Map<String, Object> stats = AgentRunHistoryEnvelopes.stats(history);

        assertThat(objectMap(values.get("contract")))
                .containsEntry("envelope", AgentRunLifecycleContract.RUN_LIST);
        assertThat(values)
                .containsEntry("outcome", AgentRunOutcomes.TERMINAL)
                .containsEntry("totalRuns", 1)
                .containsEntry("returnedRuns", 1);
        assertThat(objectMap(values.get("query")))
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("surfaceId", "coding-agent")
                .containsEntry("profileId", "profile-a");
        assertThat(list(values.get("runs"))).singleElement()
                .satisfies(run -> assertThat(objectMap(run)).containsEntry("known", true));
        assertThat(objectMap(stats.get("contract")))
                .containsEntry("envelope", AgentRunLifecycleContract.RUN_STATS);
        assertThat(stats).doesNotContainKey("runs");
    }

    @Test
    void controlAndInspectionEnvelopesOwnLifecycleShapes() {
        AgentRunStatus status = new AgentRunStatus(
                new AgentRunHandle("run-3", AgentRunState.RUNNING, "react"),
                true,
                "running",
                Map.of("surfaceId", "coding-agent"));
        AgentRunWaitResult wait = new AgentRunWaitResult(
                "run-3",
                status,
                false,
                true,
                3,
                50,
                "timed out",
                Map.of("timeoutMillis", 50));
        AgentRunInspection inspection = new AgentRunInspection(
                "run-3",
                status,
                new AgentRunEvents("run-3", List.of(AgentRunEvent.fromStatus(status, 1)), "one event"),
                "inspect");

        Map<String, Object> waitEnvelope = AgentRunControlEnvelopes.waitResult(wait);
        Map<String, Object> statusEnvelope = AgentRunInspectionEnvelopes.statusEnvelope(status);
        Map<String, Object> inspectionEnvelope = AgentRunInspectionEnvelopes.inspection(inspection);

        assertThat(objectMap(waitEnvelope.get("contract")))
                .containsEntry("envelope", AgentRunLifecycleContract.RUN_WAIT);
        assertThat(waitEnvelope)
                .containsEntry("outcome", AgentRunOutcomes.TIMEOUT)
                .containsEntry("timedOut", true)
                .containsEntry("attempts", 3);
        assertThat(objectMap(statusEnvelope.get("contract")))
                .containsEntry("envelope", AgentRunLifecycleContract.RUN_STATUS);
        assertThat(objectMap(inspectionEnvelope.get("contract")))
                .containsEntry("envelope", AgentRunLifecycleContract.RUN_INSPECT);
        assertThat(objectMap(inspectionEnvelope.get("events"))).containsKey("events");
    }

    @Test
    void lifecycleEnvelopesRedactSecretLikeMessagesAndMetadata() {
        AgentRunStatus status = new AgentRunStatus(
                AgentRunHandle.completed("run-secret", "react"),
                true,
                "password=status-secret",
                Map.of("nested", Map.of(
                        "credentials", "accessKeyId=inline-access secretAccessKey=inline-secret")));
        AgentRunEvents events = new AgentRunEvents(
                "run-secret",
                List.of(new AgentRunEvent(
                        "run-secret",
                        1,
                        "run.completed",
                        AgentRunState.COMPLETED,
                        "token=event-token",
                        Map.of("url", "jdbc:postgresql://ops:inline-password@localhost/wayang"))),
                "apiKey=events-key");
        AgentRunHistory history = new AgentRunHistory(
                AgentRunHistoryQuery.all(),
                List.of(status),
                1,
                "secretAccessKey=history-secret");

        String output = AgentRunHistoryEnvelopes.history(history).toString()
                + AgentRunHistoryEnvelopes.stats(history)
                + AgentRunEventEnvelopes.events(events)
                + AgentRunControlEnvelopes.waitResult(new AgentRunWaitResult(
                        "run-secret",
                        status,
                        true,
                        false,
                        1,
                        10,
                        "token=wait-token",
                        Map.of("password", "password=wait-secret")))
                + AgentRunInspectionEnvelopes.inspection(
                        new AgentRunInspection("run-secret", status, events, "apiKey=inspect-key"));

        assertThat(output)
                .contains("<redacted>")
                .doesNotContain("status-secret")
                .doesNotContain("inline-access")
                .doesNotContain("inline-secret")
                .doesNotContain("event-token")
                .doesNotContain("inline-password")
                .doesNotContain("events-key")
                .doesNotContain("history-secret")
                .doesNotContain("wait-token")
                .doesNotContain("wait-secret")
                .doesNotContain("inspect-key");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }
}

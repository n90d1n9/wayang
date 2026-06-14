package tech.kayys.wayang.gollek.sdk.remote;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsCursor;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsSummary;
import tech.kayys.wayang.gollek.sdk.AgentRunForgetResult;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHistorySummary;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteRunLifecycleMapperTest {

    private final RemoteRunLifecycleMapper mapper = new RemoteRunLifecycleMapper(URI.create("http://wayang.test"));

    @Test
    void mapsSubmissionLifecycleFromRemoteBody() {
        RemoteRunLifecycleMapper.RemoteRunSubmission submission = mapper.submission(new RemoteResponse(
                202,
                "{\"runId\":\"remote-1\",\"state\":\"WAITING_FOR_APPROVAL\",\"strategy\":\"remote-strategy\"}"));

        assertThat(submission.runId()).isEqualTo("remote-1");
        assertThat(submission.state()).isEqualTo(AgentRunState.WAITING_FOR_APPROVAL);
        assertThat(submission.strategy()).isEqualTo("remote-strategy");
        assertThat(submission.successful()).isTrue();
        assertThat(submission.handle().terminal()).isFalse();
    }

    @Test
    void defaultsAcceptedSubmissionToRunningWhenStateIsOmitted() {
        RemoteRunLifecycleMapper.RemoteRunSubmission submission = mapper.submission(new RemoteResponse(
                202,
                "{\"runId\":\"remote-2\"}"));

        assertThat(submission.runId()).isEqualTo("remote-2");
        assertThat(submission.state()).isEqualTo(AgentRunState.RUNNING);
        assertThat(submission.strategy()).isEqualTo("wayang-remote-api");
    }

    @Test
    void mapsRemoteStatusAndAddsTransportMetadata() {
        AgentRunStatus status = mapper.status(
                new RemoteResponse(
                        200,
                        "{\"runId\":\"remote-3\",\"state\":\"completed\",\"known\":true,\"message\":\"done\",\"metadata\":{\"tenant\":\"tenant-r\",\"httpStatus\":499,\"endpoint\":\"remote-shadow\"}}"),
                "fallback",
                "fallback message");

        assertThat(status.handle().runId()).isEqualTo("remote-3");
        assertThat(status.handle().state()).isEqualTo(AgentRunState.COMPLETED);
        assertThat(status.message()).isEqualTo("done");
        assertThat(status.metadata())
                .containsEntry("endpoint", "http://wayang.test")
                .containsEntry("httpStatus", 200)
                .containsEntry("tenant", "tenant-r")
                .containsKey("responsePreview");
    }

    @Test
    void mapsHistoryAndBuildsFilteredPath() {
        AgentRunHistoryQuery query = new AgentRunHistoryQuery(
                AgentRunState.WAITING_FOR_APPROVAL,
                7,
                "tenant a",
                "session-a",
                "assistant-agent",
                "low code",
                1);

        assertThat(mapper.historyPath(query))
                .isEqualTo("/runs?limit=7&offset=1&state=waiting-for-approval&tenantId=tenant%20a&sessionId=session-a&surfaceId=assistant-agent&profileId=low%20code");

        AgentRunHistory history = mapper.history(
                new RemoteResponse(
                        200,
                        "{\"totalRuns\":2,\"message\":\"remote history\",\"runs\":[{\"runId\":\"remote-4\",\"state\":\"FAILED\",\"known\":true,\"message\":\"failed\",\"metadata\":{\"surface\":\"assistant-agent\",\"attempt\":2}}]}"),
                query);

        assertThat(history.query()).isEqualTo(query);
        assertThat(history.totalRuns()).isEqualTo(2);
        assertThat(history.returnedRuns()).isEqualTo(1);
        assertThat(history.pageSize()).isEqualTo(7);
        assertThat(history.offset()).isEqualTo(1);
        assertThat(history.windowStart()).isEqualTo(2);
        assertThat(history.windowEnd()).isEqualTo(2);
        assertThat(history.previousOffset()).isZero();
        assertThat(history.hasPrevious()).isTrue();
        assertThat(history.nextOffset()).isEqualTo(2);
        assertThat(history.hasMore()).isFalse();
        assertThat(history.truncated()).isTrue();
        assertThat(history.stateCounts()).containsEntry("failed", 1);
        assertThat(history.summary()).isEqualTo(new AgentRunHistorySummary(
                2,
                1,
                Map.of("failed", 1),
                Map.of("assistant-agent", 1),
                Map.of("wayang-remote-api", 1)));
        assertThat(history.message()).isEqualTo("remote history");
        assertThat(history.runs())
                .singleElement()
                .satisfies(status -> {
                    assertThat(status.handle().runId()).isEqualTo("remote-4");
                    assertThat(status.handle().state()).isEqualTo(AgentRunState.FAILED);
                    assertThat(status.metadata())
                            .containsEntry("endpoint", "http://wayang.test")
                            .containsEntry("httpStatus", 200)
                            .containsEntry("surface", "assistant-agent")
                            .containsEntry("attempt", 2)
                            .containsKey("responsePreview");
                });
    }

    @Test
    void mapsRemoteCancelAndBuildsCancelPath() {
        assertThat(mapper.cancelPath("remote 5")).isEqualTo("/runs/remote%205/cancel");

        AgentRunCancelResult result = mapper.cancel(
                new RemoteResponse(
                        202,
                        "{\"runId\":\"remote-5\",\"state\":\"cancelled\",\"strategy\":\"remote-cancel\",\"cancelled\":true,\"message\":\"stopping\",\"metadata\":{\"tenant\":\"tenant-c\",\"endpoint\":\"remote-shadow\"}}"),
                "fallback",
                "user stop");

        assertThat(result.cancelled()).isTrue();
        assertThat(result.handle().runId()).isEqualTo("remote-5");
        assertThat(result.handle().state()).isEqualTo(AgentRunState.CANCELLED);
        assertThat(result.handle().terminal()).isTrue();
        assertThat(result.message()).isEqualTo("stopping");
        assertThat(result.metadata())
                .containsEntry("endpoint", "http://wayang.test")
                .containsEntry("httpStatus", 202)
                .containsEntry("tenant", "tenant-c")
                .containsEntry("reason", "user stop")
                .containsKey("responsePreview");
    }

    @Test
    void mapsRemoteForgetAndBuildsForgetPath() {
        assertThat(mapper.forgetPath("remote 6")).isEqualTo("/runs/remote%206");

        AgentRunForgetResult result = mapper.forget(
                new RemoteResponse(
                        200,
                        "{\"runId\":\"remote-6\",\"state\":\"completed\",\"strategy\":\"remote-forget\",\"forgotten\":true,\"message\":\"forgotten\",\"metadata\":{\"tenant\":\"tenant-f\",\"deletedBy\":\"remote-api\"}}"),
                "fallback");

        assertThat(result.runId()).isEqualTo("remote-6");
        assertThat(result.forgotten()).isTrue();
        assertThat(result.message()).isEqualTo("forgotten");
        assertThat(result.metadata())
                .containsEntry("endpoint", "http://wayang.test")
                .containsEntry("httpStatus", 200)
                .containsEntry("tenant", "tenant-f")
                .containsEntry("deletedBy", "remote-api")
                .containsEntry("state", "COMPLETED")
                .containsEntry("strategy", "remote-forget")
                .containsKey("responsePreview");
    }

    @Test
    void mapsRemoteEventsAndBuildsEventsPath() {
        AgentRunEventsQuery query = AgentRunEventsQuery.of("completed", "run.completed", 3L, 7);

        assertThat(mapper.eventsPath("remote 7")).isEqualTo("/runs/remote%207/events?limit=50");
        assertThat(mapper.eventsPath("remote 7", query))
                .isEqualTo("/runs/remote%207/events?limit=7&state=completed&type=run.completed&afterSequence=3");

        AgentRunEvents events = mapper.events(
                new RemoteResponse(
                        200,
                        "{\"runId\":\"remote-7\",\"totalEvents\":4,\"message\":\"remote events\",\"events\":[{\"runId\":\"remote-7\",\"sequence\":4,\"type\":\"run.completed\",\"state\":\"COMPLETED\",\"message\":\"done\",\"metadata\":{\"tenant\":\"tenant-e\",\"retry\":1,\"successful\":true}}]}"),
                "fallback",
                query);

        assertThat(events.runId()).isEqualTo("remote-7");
        assertThat(events.query()).isEqualTo(query);
        assertThat(events.totalEvents()).isEqualTo(4);
        assertThat(events.returnedEvents()).isEqualTo(1);
        assertThat(events.cursor()).isEqualTo(new AgentRunEventsCursor(3, 4, 4, 4, 7, 4, 1));
        assertThat(events.cursor().remainingEvents()).isEqualTo(3);
        assertThat(events.cursor().advanced()).isTrue();
        assertThat(events.lastSequence()).isEqualTo(4);
        assertThat(events.nextAfterSequence()).isEqualTo(4);
        assertThat(events.truncated()).isTrue();
        assertThat(events.stateCounts()).containsEntry("completed", 1);
        assertThat(events.typeCounts()).containsEntry("run.completed", 1);
        assertThat(events.summary()).isEqualTo(new AgentRunEventsSummary(
                4,
                1,
                Map.of("completed", 1),
                Map.of("run.completed", 1)));
        assertThat(events.message()).isEqualTo("remote events");
        assertThat(events.events())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.sequence()).isEqualTo(4);
                    assertThat(event.type()).isEqualTo("run.completed");
                    assertThat(event.state()).isEqualTo(AgentRunState.COMPLETED);
                    assertThat(event.metadata())
                            .containsEntry("endpoint", "http://wayang.test")
                            .containsEntry("httpStatus", 200)
                            .containsEntry("tenant", "tenant-e")
                            .containsEntry("retry", 1)
                            .containsEntry("successful", true)
                            .containsKey("responsePreview");
                });
    }
}

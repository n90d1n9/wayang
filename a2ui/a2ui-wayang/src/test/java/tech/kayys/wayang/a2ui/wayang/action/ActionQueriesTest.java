package tech.kayys.wayang.a2ui.wayang.action;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitOptions;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActionQueriesTest {

    @Test
    void buildsHistoryQueryFromActionContextAndAliases() {
        A2uiUserAction action = action(WayangA2uiActions.RUN_HISTORY, Map.of(
                "state", "running",
                "limit", "2",
                "tenantId", " ",
                "tenant", " tenant-a ",
                "session", "session-a",
                "surface", "coding-agent",
                "offset", 3L));

        AgentRunHistoryQuery query = ActionQueries.history(action);

        assertThat(query.state().name()).isEqualTo("RUNNING");
        assertThat(query.limit()).isEqualTo(2);
        assertThat(query.tenantId()).isEqualTo("tenant-a");
        assertThat(query.sessionId()).isEqualTo("session-a");
        assertThat(query.surfaceId()).isEqualTo("coding-agent");
        assertThat(query.offset()).isEqualTo(3);
    }

    @Test
    void buildsEventsQueryFromActionContext() {
        A2uiUserAction action = action(WayangA2uiActions.RUN_EVENTS, Map.of(
                "state", "completed",
                "type", " run.completed ",
                "afterSequence", "7",
                "limit", 10));

        AgentRunEventsQuery query = ActionQueries.events(action);

        assertThat(query.state().name()).isEqualTo("COMPLETED");
        assertThat(query.type()).isEqualTo("run.completed");
        assertThat(query.afterSequence()).isEqualTo(7L);
        assertThat(query.limit()).isEqualTo(10);
    }

    @Test
    void buildsWaitOptionsAndCancelReasonFromActionContext() {
        A2uiUserAction action = action(WayangA2uiActions.RUN_WAIT, Map.of(
                "timeoutSeconds", 2,
                "pollMillis", "25",
                "reason", " no longer needed "));

        AgentRunWaitOptions options = ActionQueries.waitOptions(action);

        assertThat(options.timeoutMillis()).isEqualTo(2_000L);
        assertThat(options.pollMillis()).isEqualTo(25L);
        assertThat(ActionQueries.cancelReason(action)).isEqualTo("no longer needed");
    }

    private static A2uiUserAction action(String name, Map<String, Object> context) {
        return new A2uiUserAction(
                name,
                "main",
                "button",
                Instant.parse("2026-05-31T00:00:00Z"),
                context);
    }
}

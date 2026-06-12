package tech.kayys.wayang.a2ui.wayang.projection;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionPolicy;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadAttempt;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadResult;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SessionProjectionTest {

    @Test
    void projectsOrderedActionPolicyEnvelopeAndRecordDelegates() {
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(
                Set.of(WayangA2uiActions.RUN_WAIT, WayangA2uiActions.RUN_INSPECT),
                Set.of("run-z", "run-a"),
                Map.of("tenantId", "tenant-a"));

        Map<String, Object> values = SessionProjection.actionPolicy(policy);

        assertThat(policy.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly("allowedActions", "allowedRunIds", "requiredContext");
        assertThat((Iterable<String>) values.get("allowedActions"))
                .containsExactly(WayangA2uiActions.RUN_INSPECT, WayangA2uiActions.RUN_WAIT);
        assertThat((Iterable<String>) values.get("allowedRunIds"))
                .containsExactly("run-a", "run-z");
        assertThat((Map<String, Object>) values.get("requiredContext"))
                .containsEntry("tenantId", "tenant-a");
    }

    @Test
    void projectsOrderedSessionConfigEnvelopeAndRecordDelegates() {
        WayangA2uiSessionConfig config = new WayangA2uiSessionConfig(
                false,
                new WayangA2uiActionPolicy(
                        Set.of(WayangA2uiActions.RUN_EVENTS, WayangA2uiActions.RUN_INSPECT),
                        Set.of("run-2", "run-1"),
                        Map.of("tenantId", "tenant-a")));

        Map<String, Object> values = SessionProjection.config(config);

        assertThat(config.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly("enabled", "policy");
        assertThat(values).containsEntry("enabled", false);

        Map<String, Object> policy = (Map<String, Object>) values.get("policy");
        assertThat(policy.keySet()).containsExactly("allowedActions", "allowedRunIds", "requiredContext");
        assertThat((Iterable<String>) policy.get("allowedActions"))
                .containsExactly(WayangA2uiActions.RUN_EVENTS, WayangA2uiActions.RUN_INSPECT);
        assertThat((Iterable<String>) policy.get("allowedRunIds"))
                .containsExactly("run-1", "run-2");
    }

    @Test
    void projectsDisabledConfigWithDefaultInspectPolicy() {
        Map<String, Object> values = SessionProjection.config(WayangA2uiSessionConfig.disabled());

        assertThat(values).containsEntry("enabled", false);
        assertThat((Map<String, Object>) values.get("policy"))
                .containsEntry("allowedActions", java.util.List.of(WayangA2uiActions.RUN_INSPECT))
                .containsEntry("allowedRunIds", java.util.List.of())
                .containsEntry("requiredContext", Map.of());
    }

    @Test
    void projectsSessionConfigLoadResultAndRecordDelegates() {
        SessionConfigLoadResult result = SessionConfigLoadResult.loaded(
                "database:tenant-a",
                WayangA2uiSessionConfig.readOnly());

        Map<String, Object> values = SessionProjection.loadResult(result);

        assertThat(result.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "sourceDescription",
                "status",
                "loaded",
                "config",
                "message");
        assertThat(values)
                .containsEntry("sourceDescription", "database:tenant-a")
                .containsEntry("status", SessionConfigLoadStatus.LOADED.name())
                .containsEntry("loaded", true);
        assertThat((Map<String, Object>) values.get("config"))
                .containsEntry("enabled", true);
        assertThat(result.toJson()).contains("\"sourceDescription\":\"database:tenant-a\"");
    }

    @Test
    void projectsSessionConfigLoadAttemptsWhenPresent() {
        SessionConfigLoadResult result = SessionConfigLoadResult
                .loaded("database:tenant-a", WayangA2uiSessionConfig.readOnly())
                .withAttempts(List.of(
                        SessionConfigLoadAttempt.missing("file:/etc/wayang/session.json"),
                        SessionConfigLoadAttempt.loaded("database:tenant-a")));

        Map<String, Object> values = SessionProjection.loadResult(result);

        assertThat(values.keySet()).containsExactly(
                "sourceDescription",
                "status",
                "loaded",
                "config",
                "message",
                "attempts");
        assertThat((Iterable<Map<String, Object>>) values.get("attempts"))
                .extracting(attempt -> attempt.get("status"))
                .containsExactly(SessionConfigLoadStatus.MISSING.name(), SessionConfigLoadStatus.LOADED.name());
    }
}

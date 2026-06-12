package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiSessionConfigTest {

    @Test
    void defaultConfigNamesInspectOnlyFallback() {
        WayangA2uiSessionConfig config = WayangA2uiSessionConfig.defaultConfig();

        assertThat(config).isEqualTo(WayangA2uiSessionConfig.inspectOnly());
        assertThat(config.enabled()).isTrue();
        assertThat(config.actionPolicy().allowedActions())
                .containsExactly(WayangA2uiActions.RUN_INSPECT);
    }

    @Test
    void parsesRunLifecycleModeAndPolicyGuards() {
        WayangA2uiSessionConfig config = WayangA2uiSessionConfig.fromMap(Map.of(
                "mode", "run-lifecycle",
                "allowedRunIds", "run-1, run-2",
                "requiredContext", Map.of(
                        " tenantId ", "tenant-a",
                        "attempt", 2)));

        assertThat(config.enabled()).isTrue();
        assertThat(config.actionPolicy().allowedActions())
                .containsExactlyInAnyOrder(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS,
                        WayangA2uiActions.RUN_WAIT,
                        WayangA2uiActions.RUN_CANCEL);
        assertThat(config.actionPolicy().allowedRunIds())
                .containsExactlyInAnyOrder("run-1", "run-2");
        assertThat(config.actionPolicy().requiredContext())
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("attempt", "2");
    }

    @Test
    @SuppressWarnings("unchecked")
    void parsesNestedCustomPolicyAndSerializesDeterministically() {
        WayangA2uiSessionConfig config = WayangA2uiSessionConfig.fromMap(Map.of(
                "enabled", "false",
                "policy", Map.of(
                        "mode", "custom",
                        "allowedActions", java.util.List.of(
                                WayangA2uiActions.RUN_WAIT,
                                WayangA2uiActions.RUN_INSPECT),
                        "allowedRunIds", java.util.List.of("run-z", "run-a"))));

        assertThat(config.enabled()).isFalse();
        assertThat(config.actionPolicy().allowedActions())
                .containsExactlyInAnyOrder(WayangA2uiActions.RUN_INSPECT, WayangA2uiActions.RUN_WAIT);
        assertThat(config.toMap())
                .containsEntry("enabled", false)
                .containsKey("policy");
        Map<String, Object> serialized = config.toMap();
        Map<String, Object> serializedPolicy = (Map<String, Object>) serialized.get("policy");
        String json = TransportJson.json(serialized, "Unable to encode session config fixture");

        assertThat(serializedPolicy)
                .containsEntry("allowedRunIds", java.util.List.of("run-a", "run-z"));
        assertThat(json).startsWith("{\"enabled\":");
        assertThat(json.indexOf("\"policy\""))
                .isGreaterThan(json.indexOf("\"enabled\""));
        assertThat(json.indexOf("\"allowedRunIds\""))
                .isGreaterThan(json.indexOf("\"allowedActions\""));
    }

    @Test
    void parsesReadOnlyMode() {
        WayangA2uiSessionConfig config = WayangA2uiSessionConfig.fromMap(Map.of("mode", "read-only"));

        assertThat(config.actionPolicy().allowedActions())
                .containsExactlyInAnyOrder(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS);
    }
}

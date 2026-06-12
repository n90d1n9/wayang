package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiSessionConfigDecoderTest {

    @Test
    void defaultsEmptyConfigToInspectOnly() {
        WayangA2uiSessionConfig config = WayangA2uiSessionConfig.defaultConfig();

        assertThat(config.enabled()).isTrue();
        assertThat(config.actionPolicy().allowedActions())
                .containsExactly(WayangA2uiActions.RUN_INSPECT);
        assertThat(config).isEqualTo(WayangA2uiSessionConfig.inspectOnly());
        assertThat(WayangA2uiSessionConfig.fromMap(Map.of())).isEqualTo(config);
        assertThat(WayangA2uiSessionConfigDecoder.fromMap(Map.of())).isEqualTo(config);
        assertThat(WayangA2uiSessionConfigDecoder.fromMap(null)).isEqualTo(config);
    }

    @Test
    void decodesNestedPolicyWithTopLevelFallbackOverrides() {
        WayangA2uiSessionConfig config =
                WayangA2uiSessionConfigDecoder.fromMap(Map.of(
                        "enabled",
                        "false",
                        "policy",
                        Map.of(
                                "mode",
                                "custom",
                                "allowedActions",
                                List.of(WayangA2uiActions.RUN_WAIT, WayangA2uiActions.RUN_INSPECT)),
                        "allowedRunIds",
                        "run-2, run-1",
                        "requiredContext",
                        Map.of("tenantId", 42)));

        assertThat(config.enabled()).isFalse();
        assertThat(config.actionPolicy().allowedActions())
                .containsExactlyInAnyOrder(WayangA2uiActions.RUN_INSPECT, WayangA2uiActions.RUN_WAIT);
        assertThat(config.actionPolicy().allowedRunIds())
                .containsExactlyInAnyOrder("run-1", "run-2");
        assertThat(config.actionPolicy().requiredContext())
                .containsEntry("tenantId", "42");
    }

    @Test
    void ignoresNullNestedPolicyValuesWhenApplyingTopLevelFallbacks() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("mode", null);
        policy.put("allowedRunIds", null);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", null);
        values.put("policy", policy);
        values.put("mode", "run-lifecycle");
        values.put("allowedRunIds", "run-a, run-b");

        WayangA2uiSessionConfig config = WayangA2uiSessionConfigDecoder.fromMap(values);

        assertThat(config.enabled()).isTrue();
        assertThat(config.actionPolicy().allowedActions())
                .contains(WayangA2uiActions.RUN_CANCEL, WayangA2uiActions.RUN_WAIT);
        assertThat(config.actionPolicy().allowedRunIds())
                .containsExactlyInAnyOrder("run-a", "run-b");
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = Map.of(
                "mode",
                "readonly",
                "enabled",
                true);

        assertThat(WayangA2uiSessionConfig.fromMap(values))
                .isEqualTo(WayangA2uiSessionConfigDecoder.fromMap(values));
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        String json = """
                {
                  "enabled": "true",
                  "policy": {
                    "mode": "run_lifecycle",
                    "allowedRunIds": ["run-a"]
                  }
                }
                """;
        WayangA2uiSessionConfig decoded = WayangA2uiSessionConfigDecoder.fromJson(json);

        assertThat(decoded.enabled()).isTrue();
        assertThat(decoded.actionPolicy().allowedActions())
                .contains(WayangA2uiActions.RUN_CANCEL, WayangA2uiActions.RUN_WAIT);
        assertThat(decoded.actionPolicy().allowedRunIds()).containsExactly("run-a");
        assertThat(WayangA2uiSessionConfig.fromJson(json)).isEqualTo(decoded);
        assertThatThrownBy(() -> WayangA2uiSessionConfigDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI session config JSON must not be blank");
    }
}

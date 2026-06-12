package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigDecoderTest {

    @Test
    void defaultsEmptyConfigToInspectOnly() {
        WayangA2uiSessionConfig config = WayangA2uiSessionConfig.defaultConfig();

        assertThat(SessionConfigDecoder.fromMap(Map.of())).isEqualTo(config);
        assertThat(SessionConfigDecoder.fromMap(null)).isEqualTo(config);
    }

    @Test
    void decodesNestedPolicyWithTopLevelFallbackOverrides() {
        WayangA2uiSessionConfig config =
                SessionConfigDecoder.fromMap(Map.of(
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
        WayangA2uiSessionConfig decoded = SessionConfigDecoder.fromJson(json);

        assertThat(decoded.enabled()).isTrue();
        assertThat(decoded.actionPolicy().allowedActions())
                .contains(WayangA2uiActions.RUN_CANCEL, WayangA2uiActions.RUN_WAIT);
        assertThat(decoded.actionPolicy().allowedRunIds()).containsExactly("run-a");
        assertThat(WayangA2uiSessionConfig.fromJson(json)).isEqualTo(decoded);
        assertThatThrownBy(() -> SessionConfigDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI session config JSON must not be blank");
    }
}

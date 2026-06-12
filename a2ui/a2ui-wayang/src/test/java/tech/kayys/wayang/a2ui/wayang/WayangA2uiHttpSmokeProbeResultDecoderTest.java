package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpSmokeProbeResultDecoderTest {

    @Test
    void decodesStringyProbeMapsAndNestedSummary() {
        WayangA2uiHttpSmokeProbeResult result =
                WayangA2uiHttpSmokeProbeResultDecoder.fromMap(Map.of(
                        "statusCode",
                        "200",
                        "httpSuccessful",
                        "true",
                        "routeOperation",
                        WayangA2uiHttpRoute.OPERATION_SMOKE,
                        "allow",
                        "POST, OPTIONS",
                        WayangA2uiTransportFields.OUTCOME,
                        WayangA2uiTransportOutcome.SUCCESS.name(),
                        "summary",
                        Map.of(
                                WayangA2uiTransportFields.PASSED,
                                "true",
                                WayangA2uiTransportFields.EXIT_CODE,
                                "0",
                                WayangA2uiTransportFields.SUITE_ID,
                                WayangA2uiHttpScenarios.SMOKE_SUITE_ID),
                        "headers",
                        Map.of("Allow", "POST, OPTIONS")));

        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.httpSuccessful()).isTrue();
        assertThat(result.smokeRoute()).isTrue();
        assertThat(result.summary().successfulExit()).isTrue();
        assertThat(result.passed()).isTrue();
        assertThat(result.headers()).containsEntry("Allow", "POST, OPTIONS");
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = Map.of(
                "statusCode",
                500,
                "routeOperation",
                WayangA2uiHttpRoute.OPERATION_SMOKE);

        assertThat(WayangA2uiHttpSmokeProbeResult.fromMap(values))
                .isEqualTo(WayangA2uiHttpSmokeProbeResultDecoder.fromMap(values));
    }

    @Test
    void emptyProbeFactoryProvidesReadinessFallbackShape() {
        WayangA2uiHttpSmokeProbeResult probe = WayangA2uiHttpSmokeProbeResult.empty();

        assertThat(probe.statusCode()).isZero();
        assertThat(probe.httpSuccessful()).isFalse();
        assertThat(probe.routeOperation()).isEmpty();
        assertThat(probe.summary()).isEqualTo(WayangA2uiHttpSmokeSummary.empty());
        assertThat(probe.summary().exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(probe.passed()).isFalse();
        assertThat(probe.toMap())
                .containsEntry(WayangA2uiTransportFields.PASSED, false)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, WayangA2uiHttpSmokeResult.EXIT_FAILURE);
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        WayangA2uiHttpSmokeProbeResult decoded =
                WayangA2uiHttpSmokeProbeResultDecoder.fromJson("""
                        {
                          "statusCode": 200,
                          "httpSuccessful": true,
                          "routeOperation": "a2ui.smoke",
                          "summary": {"passed": true, "exitCode": 0}
                        }
                        """);

        assertThat(decoded.statusCode()).isEqualTo(200);
        assertThat(decoded.summary().successfulExit()).isTrue();
        assertThatThrownBy(() -> WayangA2uiHttpSmokeProbeResultDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP smoke probe result JSON must not be blank");
    }
}

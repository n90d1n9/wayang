package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpBindingReportProbeResultDecoderTest {

    @Test
    void decodesStringyProbeMapsAndOperationLists() {
        WayangA2uiHttpBindingReportProbeResult result =
                WayangA2uiHttpBindingReportProbeResultDecoder.fromMap(Map.ofEntries(
                        Map.entry("statusCode", "200"),
                        Map.entry("httpSuccessful", "true"),
                        Map.entry("routeOperation", WayangA2uiHttpRoute.OPERATION_BINDING_REPORT),
                        Map.entry(WayangA2uiTransportFields.OUTCOME, WayangA2uiTransportOutcome.SUCCESS.name()),
                        Map.entry(WayangA2uiTransportFields.COMPLETE, "false"),
                        Map.entry("routeOperations", "a2ui.exchange a2ui.smoke a2ui.exchange"),
                        Map.entry("handlerOperations", List.of("a2ui.exchange", "a2ui.smoke", "a2ui.smoke")),
                        Map.entry("missingHandlerOperations", "a2ui.smoke"),
                        Map.entry("orphanHandlerOperations", "a2ui.custom"),
                        Map.entry("issues", List.of(Map.of("message", "binding mismatch"))),
                        Map.entry(WayangA2uiTransportFields.METADATA, Map.of("source", "decoder")),
                        Map.entry(WayangA2uiTransportFields.BODY, Map.of("complete", false)),
                        Map.entry("headers", Map.of("Allow", "GET"))));

        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.httpSuccessful()).isTrue();
        assertThat(result.routeOperations()).containsExactly("a2ui.exchange", "a2ui.smoke");
        assertThat(result.handlerOperations()).containsExactly("a2ui.exchange", "a2ui.smoke");
        assertThat(result.missingHandlerOperations()).containsExactly("a2ui.smoke");
        assertThat(result.orphanHandlerOperations()).containsExactly("a2ui.custom");
        assertThat(result.hasRouteOperation(WayangA2uiHttpRoute.OPERATION_EXCHANGE)).isTrue();
        assertThat(result.requiresSmokeProbe()).isTrue();
        assertThat(result.issueCount()).isEqualTo(1);
        assertThat(result.metadata()).containsEntry("source", "decoder");
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = Map.of(
                "statusCode",
                500,
                "routeOperation",
                WayangA2uiHttpRoute.OPERATION_BINDING_REPORT);

        assertThat(WayangA2uiHttpBindingReportProbeResult.fromMap(values))
                .isEqualTo(WayangA2uiHttpBindingReportProbeResultDecoder.fromMap(values));
    }

    @Test
    void emptyProbeFactoryProvidesReadinessFallbackShape() {
        WayangA2uiHttpBindingReportProbeResult probe = WayangA2uiHttpBindingReportProbeResult.empty();

        assertThat(WayangA2uiHttpBindingReportProbeResult.fromMap(Map.of())).isEqualTo(probe);
        assertThat(WayangA2uiHttpBindingReportProbeResultDecoder.fromMap(Map.of())).isEqualTo(probe);
        assertThat(probe.statusCode()).isZero();
        assertThat(probe.httpSuccessful()).isFalse();
        assertThat(probe.routeOperation()).isEmpty();
        assertThat(probe.routeOperations()).isEmpty();
        assertThat(probe.hasRouteOperation(WayangA2uiHttpRoute.OPERATION_SMOKE)).isFalse();
        assertThat(probe.requiresSmokeProbe()).isFalse();
        assertThat(probe.passed()).isFalse();
        assertThat(probe.toMap())
                .containsEntry(WayangA2uiTransportFields.PASSED, false)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 0);
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        WayangA2uiHttpBindingReportProbeResult decoded =
                WayangA2uiHttpBindingReportProbeResultDecoder.fromJson("""
                        {
                          "statusCode": 200,
                          "httpSuccessful": true,
                          "routeOperation": "a2ui.bindingReport",
                          "routeOperations": ["a2ui.exchange"]
                        }
                        """);

        assertThat(decoded.statusCode()).isEqualTo(200);
        assertThat(decoded.routeOperations()).containsExactly("a2ui.exchange");
        assertThatThrownBy(() -> WayangA2uiHttpBindingReportProbeResultDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP binding report probe result JSON must not be blank");
    }
}
